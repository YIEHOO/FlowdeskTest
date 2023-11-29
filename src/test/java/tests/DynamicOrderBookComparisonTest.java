package tests;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import io.github.bonigarcia.wdm.WebDriverManager;
import pages.BinancePage;

import java.time.Duration;
import java.util.List;

import static flowdesk.Binance.getOrderBook;

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

        // Use WebDriverManager to set up the WebDriver dynamically
        WebDriverManager.edgedriver().setup();
        driver = new EdgeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

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
            for (WebElement orderBookEntry : orderBookEntries) {
                // Compare the order books
                boolean match = false;
                // Use the appropriate CSS selector based on the type (ask or bid)
                String priceSelector = type.equals("ask") ? ".ask-light" : ".bid-light";
                double price = Double.parseDouble(orderBookEntry.findElement(By.cssSelector(priceSelector)).getText());
                double quantity = Double.parseDouble(orderBookEntry.findElement(By.cssSelector(".text:nth-child(2)")).getText());
                for (String[] strings : expectedOrderBook) {
                    if (price == Double.parseDouble(strings[0]) && quantity == Double.parseDouble(strings[1])) {
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
                test.log(Status.INFO, matchingPercentage + "%");
            } else {
                test.log(Status.PASS, type + "s matching!");
                test.log(Status.INFO, matchingPercentage + "%");
            }

        } catch (Exception e) {
            test.log(Status.FAIL, "An error occurred: " + e.getMessage());
        }
    }

}