package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class BinancePage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    public BinancePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public void navigateTo(String url) {
        driver.get(url);
    }

    public WebElement getOrderBookContainer(String type) {
        String xpath = type.equals("ask") ?
                "(//div[@class='orderbook-list-container'])[1]" :
                "(//div[@class='orderbook-list-container'])[2]";
        return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
    }

    public List<WebElement> getOrderBookEntries(WebElement orderBookContainer) {
        return orderBookContainer.findElements(By.className("row-content"));
    }
}