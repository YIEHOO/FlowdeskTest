package com.flowdesk;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@ClientEndpoint
public class Binance {

    // A map to store the local order book
    private static Map<Double, Double> orderBook = new ConcurrentHashMap<>();

    // A variable to store the last update id from the snapshot
    private static long lastUpdateId = 0;

    // A variable to store the previous event's u
    private static long prevU = 0;

    // A method to get a depth snapshot from the API
    public static void getDepthSnapshot() {
        try {
            // Create a HTTP client and send a GET request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.binance.com/api/v3/depth?symbol=BNBBTC&limit=5000"))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse the response as a JSON object
            JSONObject json = new JSONObject(response.body());

            // Get the last update id from the snapshot
            lastUpdateId = json.getLong("lastUpdateId");

            // Get the bids and asks from the snapshot
            JSONArray bids = json.getJSONArray("bids");
            JSONArray asks = json.getJSONArray("asks");

            // Update the local order book with the bids and asks
            for (int i = 0; i < bids.length(); i++) {
                JSONArray bid = bids.getJSONArray(i);
                double price = Double.parseDouble(bid.getString(0));
                double quantity = bid.getDouble(1);
                orderBook.put(price, quantity);
            }
            for (int i = 0; i < asks.length(); i++) {
                JSONArray ask = asks.getJSONArray(i);
                double price = Double.parseDouble(ask.getString(0));
                double quantity = ask.getDouble(1);
                orderBook.put(price, quantity);
            }

            // Print the local order book size
            System.out.println("Order book size: " + orderBook.size());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // A method to process an event from the stream
    public static void processEvent(JSONObject event) throws JSONException {
        // Get the event's U, u, b and a
        long U = event.getLong("U");
        long u = event.getLong("u");
        JSONArray b = event.getJSONArray("b");
        JSONArray a = event.getJSONArray("a");

        // Check if the event is valid
        if (u <= lastUpdateId) {
            // Drop the event
            System.out.println("Dropped event: " + event);
            return;
        }
        if (prevU == 0) {
            // The first processed event
            if (U <= lastUpdateId + 1 && u >= lastUpdateId + 1) {
                // Valid event
                System.out.println("First event: " + event);
            } else {
                // Invalid event
                System.out.println("Invalid first event: " + event);
                return;
            }
        } else {
            // Subsequent events
            if (U == prevU + 1) {
                // Valid event
                System.out.println("Next event: " + event);
            } else {
                // Invalid event
                System.out.println("Invalid next event: " + event);
                return;
            }
        }

        // Update the previous event's u
        prevU = u;

        // Update the local order book with the event's b and a
        for (int i = 0; i < b.length(); i++) {
            JSONArray bid = b.getJSONArray(i);
            double price = Double.parseDouble(bid.getString(0));
            double quantity = bid.getDouble(1);
            if (quantity == 0) {
                // Remove the price level
                orderBook.remove(price);
            } else {
                // Update the price level
                orderBook.put(price, quantity);
            }
        }
        for (int i = 0; i < a.length(); i++) {
            JSONArray ask = a.getJSONArray(i);
            double price = Double.parseDouble(ask.getString(0));
            double quantity = ask.getDouble(1);
            if (quantity == 0) {
                // Remove the price level
                orderBook.remove(price);
            } else {
                // Update the price level
                orderBook.put(price, quantity);
            }
        }

        // Print the local order book size
        System.out.println("Order book size: " + orderBook.size());
    }

    // A method to open a stream to the websocket
    public static void openStream() {
        try {
            // Create a websocket container and connect to the endpoint
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            Session session = container.connectToServer(Binance.class, URI.create("wss://stream.binance.com:9443/ws/bnbbtc@depth"));
            session.close();

        } catch (IOException | DeploymentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // A method to handle the messages from the stream
    @OnMessage
    public void onMessage(String message) throws JSONException {
        // Parse the message as a JSON object
        JSONObject event = new JSONObject(message);

        // Process the event
        processEvent(event);
    }

    // The main method
    public static String[][] getOrderBook() {
        // Get a depth snapshot
        getDepthSnapshot();

        // Open a stream
        openStream();

        String[][] orderBookArray = new String[orderBook.size()][2];
        int i = 0;

        for (Map.Entry<Double, Double> entry : orderBook.entrySet()) {
            orderBookArray[i][0] = String.valueOf(entry.getKey());
            orderBookArray[i][1] = String.valueOf(entry.getValue());
            i++;
        }

        return orderBookArray;
    }
}
