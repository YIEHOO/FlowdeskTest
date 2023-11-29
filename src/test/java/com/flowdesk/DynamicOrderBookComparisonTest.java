package com.flowdesk;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.flowdesk.Binance.getOrderBook;

public class DynamicOrderBookComparisonTest {

    private static ExtentReports extent;
    private static ExtentTest test;
    private WebDriver driver;
    private BinancePage binancePage;

    @BeforeClass
    public void setUp() {
        ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter("extent-report.html");
        extent = new ExtentReports();
        extent.attachReporter(htmlReporter);
        test = extent.createTest("DynamicOrderBookComparison", "Test to compare dynamic order books");
        // Start a background thread to periodically flush the report
        Thread flushThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // Adjust the interval as needed (e.g., every 30 seconds)
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                extent.flush();
            }
        });
        flushThread.setDaemon(true);
        flushThread.start();

        // Use WebDriverManager to setup the WebDriver dynamically
        WebDriverManager.edgedriver().setup();
        driver = new EdgeDriver();
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        // Initialize BinancePage
        binancePage = new BinancePage(driver);
    }

    @Test
    public void dynamicOrderBookComparisonTest() {
        String pageUrl = "https://www.binance.com/fr/trade/BNB_BTC?type=spot";
        binancePage.navigateTo(pageUrl);
        String[][] expectedOrderBook;
        int i = 0;

        while (i < 20) {
            try {
                synchronized (DynamicOrderBookComparisonTest.class) {
                    expectedOrderBook = getOrderBook();
                }

                if (expectedOrderBook == null) {
                    test.log(Status.FAIL, "No order book available");
                    break;
                }

                compareOrderBooks(binancePage, expectedOrderBook, "ask");
                compareOrderBooks(binancePage, expectedOrderBook, "bid");
                i++;
            } catch (Exception e) {
                test.log(Status.FAIL, "An error occurred: " + e.getMessage());
            }
        }
    }

    @AfterTest
    public void tearDown() {
        driver.quit();
    }

    private static void compareOrderBooks(BinancePage binancePage, String[][] expectedOrderBook, String type) {
        try {
            WebElement orderBookContainer = binancePage.getOrderBookContainer(type);
            List<WebElement> orderBookEntries = binancePage.getOrderBookEntries(orderBookContainer);

            int totalEntries = orderBookEntries.size();
            int matchingEntries = 0;
            for (int i = 0; i < orderBookEntries.size(); i++) {
                // Compare the order books
                boolean match = false;
                WebElement entry = orderBookEntries.get(i);
                // Use the appropriate CSS selector based on the type (ask or bid)
                String priceSelector = type.equals("ask") ? ".ask-light" : ".bid-light";
                double price = Double.parseDouble(entry.findElement(By.cssSelector(priceSelector)).getText());
                double quantity = Double.parseDouble(entry.findElement(By.cssSelector(".text:nth-child(2)")).getText());
                for (int j = 0; j < expectedOrderBook.length; j++) {
                    if (price == Double.parseDouble(expectedOrderBook[j][0]) && quantity == Double.parseDouble(expectedOrderBook[j][1])) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    matchingEntries++;
                }

            }
            float matchingPercentage = (float)matchingEntries / totalEntries * 100;
            System.out.println("Matching "+type+"s percentage: " + matchingPercentage + "%");
            if (matchingPercentage<70){
                test.log(Status.FAIL, "Too many " + type + "s not matching");
                test.log(Status.INFO, String.valueOf(matchingPercentage + "%"));
            } else {
                test.log(Status.PASS, type + "s matching!");
                test.log(Status.INFO, String.valueOf(matchingPercentage + "%"));
            }

        } catch (Exception e) {
            test.log(Status.FAIL, "An error occurred: " + e.getMessage());
        }
    }

    private static void printOrderBook(String[][] orderBook) {
        for (String[] entry : orderBook) {
            System.out.println("Price: " + entry[0] + ", Quantity: " + entry[1]);
        }
    }

    private static void printOrderBookFromPage(List<WebElement> orderBookEntries, String type) {
        for (WebElement entry : orderBookEntries) {
            // Use the appropriate CSS selector based on the type (ask or bid)
            String priceSelector = type.equals("ask") ? ".ask-light" : ".bid-light";
            String price = entry.findElement(By.cssSelector(priceSelector)).getText();
            String quantity = entry.findElement(By.cssSelector(".text:nth-child(2)")).getText();
            System.out.println("Price: " + price + ", Quantity: " + quantity);
        }
    }
}