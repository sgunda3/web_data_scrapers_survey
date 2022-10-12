package com.reddit;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.twitter.Tweet;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;


/**
 * @author Vinodh, Sandhya, Ganesh
 * <p>
 * Selenium script to login to reddit and collect posts along with comments,votes, posttitle and posturl for a given list of keywords.
 */

public class Application {

    static String userName = "";
    static String passWord = "";
    static String redditBaseURL = "https://reddit.com/";
    static String login = "login";
    static String home = "home";
    static int totalScrolls = 10;

    public static void main(String[] args) throws InterruptedException, IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

        ChromeOptions options = new ChromeOptions();
        options.addArguments("start-maximized");
        options.addArguments("enable-automation");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-browser-side-navigation");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-notifications");
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable( LogType.PERFORMANCE, Level.ALL );
        options.setCapability( "goog:loggingPrefs", logPrefs );
        options.setExperimentalOption("w3c", false);
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\91709\\Downloads\\chromedriver_win32\\chromedriver.exe");
        WebDriver driver = new ChromeDriver(options);
        login(driver);
        Thread.sleep(3000);
        driver.get(redditBaseURL + "r/ronaldo/");
        getPostsFromSubreddit("r/ronaldo/", driver, 10);
//		Thread.sleep(20000);
        List<LogEntry> logs = driver.manage().logs().get(LogType.PERFORMANCE).getAll();
//        for(LogEntry log:logs) {
//            for(String key : log.toJson().keySet())
//                System.out.println(log.toJson().get(key));
//        }
        JavascriptExecutor executor = (JavascriptExecutor) driver;
        long value = (long) executor.executeScript("return window.performance.memory.usedJSHeapSize");
        long valueInMB = value / (1024 * 1024);
        System.out.println("Heap Size: "+valueInMB);
        driver.quit();
    }
    public static void getPostsFromSubreddit(String subreddit, WebDriver driver, int totalScrolls) throws InterruptedException, IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {
        PrintStream stream
                = new PrintStream(System.out);
        Thread.sleep(5000);
        List<RedditPost> redditPosts = new ArrayList<>();
        String lastID = "";
        while (totalScrolls > 0) {
            String url = redditBaseURL + subreddit;
            if (!lastID.equals("")) {
                url += "?count=10&after=" + lastID;
            } else {
                url += "?count=10";
            }

            driver.get(url);
            Thread.sleep(5000);
            List<WebElement> postDivs = driver.findElements(By.className("scrollerItem"));
            int idx = 0;
            for (WebElement postDiv : postDivs) {
                if (idx >= 10) {
                    break;
                }
                String id = postDiv.getAttribute("id");
                String postTitle = postDiv.findElement(By.xpath(".//div/div/div/a/div/h3")).getText();
                System.out.println("postTitle "+postTitle);
                postTitle = postTitle.replace(",","");
                String postUrl = postDiv.findElements(By.xpath(".//div/div/div/a")).get(1).getAttribute("href");
                String votes = postDiv.findElement(By.xpath(".//div/div/div")).getText();

                votes = votes.replace("\n","");
                if(!votes.toLowerCase().contentEquals("vote")){
                    if(votes.length()>=2){
                        votes = votes.substring((votes.length()/2), votes.length());
                    }
                }
                String postedBy = postDiv.findElement(By.xpath(".//div/div/div/div/div/a")).getText();
                String comments = postDiv.findElement(By.xpath(".//div/div/div/a/span")).getText();
                comments = comments.replace("\n","");
                System.out.println(comments);
                if(comments.length() % 2==1){
                    StringBuffer sb = new StringBuffer(comments);
                    sb = sb.reverse();
                    comments= sb.toString();
                }
                if(!comments.toLowerCase().contentEquals("comments")){
                    if(comments.length()>=2 && comments.length() %2 ==0){
                        comments = comments.substring((comments.length()/2), comments.length());
                    }
                }

                String postedAt = postDiv.findElements(By.xpath(".//div/div/div/div/a")).get(1).getText();

                RedditPost redditPost = new RedditPost(postedBy, postedAt, comments, postTitle, postUrl, votes);
                stream.println(redditPost);
//                System.out.println();
                if(!redditPost.getPostTitle().contentEquals("")){
                    redditPosts.add(redditPost);
                    lastID = id;
                    idx++;
                }

            }
            totalScrolls--;
        }

        CustomMappingStrategy<RedditPost> mappingStrategy = new CustomMappingStrategy<>();
        mappingStrategy.setType(RedditPost.class);

        String fileName = new StringBuilder().append(subreddit.replace("r/", "").replace("/", "")).append(".csv").toString();
        Writer writer = new FileWriter(fileName);
        StatefulBeanToCsv<RedditPost> csvwriter = new StatefulBeanToCsvBuilder<RedditPost>(writer)
                .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .withOrderedResults(true)
                .withMappingStrategy(mappingStrategy)
                .build();
        csvwriter.write(redditPosts);
        writer.close();
    }

    /**
     * Login the user to Reddit
     *
     * @param driver
     * @throws InterruptedException
     */

    public static void login(WebDriver driver) throws InterruptedException {
        String twitterLoginURL = new StringBuilder(redditBaseURL).append(login).toString();
        driver.get(twitterLoginURL);
        driver.manage().window().maximize();
        Thread.sleep(1000);
        WebElement userTextField = driver.findElement(By.id("loginUsername"));
        userTextField.sendKeys(userName);

        WebElement PassTextField = driver.findElement(By.id("loginPassword"));
        PassTextField.sendKeys(passWord);

        driver.findElement(By.className("AnimatedForm__submitButton")).click();
    }
    /**
     * Scroll the screen.
     *
     * @param driver
     */
    public static void scroll(WebDriver driver) {
        JavascriptExecutor jse = (JavascriptExecutor) driver;
        jse.executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }
}
