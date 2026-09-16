package com.xowl.spending.frameworks.webdriver;

import com.xowl.spending.usecases.webdriver.WebDriverSupplier;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;

public class ChromeWebDriverSupplier implements WebDriverSupplier {

    public WebDriver supply() {
        WebDriver driver;
        Optional<Path> chromeBinary = resolveChromeBinary();
        Optional<Path> systemChromedriver = resolveSystemChromedriver();
        ChromeOptions options = buildChromeOptions(chromeBinary);

        if (systemChromedriver.isPresent()) {
            ChromeDriverService service = new ChromeDriverService.Builder()
                    .usingDriverExecutable(systemChromedriver.get().toFile())
                    .build();
            driver = new ChromeDriver(service, options);
        } else {
            WebDriverManager.chromedriver().capabilities(options).setup();
            driver = new ChromeDriver(options);
        }

        // Must stay below Selenium's internal 3-minute HTTP read timeout, otherwise driver.get()
        // dies with a raw java.util.concurrent.TimeoutException instead of a page-load timeout.
        driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofMinutes(2));

        return driver;
    }

    private static ChromeOptions buildChromeOptions(Optional<Path> chromeBinary) {
        ChromeOptions options = new ChromeOptions();
        chromeBinary.ifPresent(p -> options.setBinary(p.toString()));
        // EAGER: return after DOMInteractive so driver.get() does not wait forever on "load" (slow Pi / hanging pixels).
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--remote-allow-origins=*");
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);
        String arch = System.getProperty("os.arch", "");
        String linuxArch = arch.contains("aarch64") || arch.contains("arm") ? "aarch64" : "x86_64";
        options.addArguments(
                "--user-agent=Mozilla/5.0 (X11; Linux " + linuxArch + ") AppleWebKit/537.36 (KHTML, like Gecko)"
                        + " Chrome/126.0.0.0 Safari/537.36");
        return options;
    }

    private static Optional<Path> resolveChromeBinary() {
        List<String> candidates =
                Arrays.asList(
                        "/usr/bin/chromium-browser",
                        "/usr/bin/chromium",
                        "/snap/bin/chromium",
                        "/usr/bin/google-chrome-stable",
                        "/usr/bin/google-chrome");

        for (String candidate : candidates) {
            Path p = Paths.get(candidate);
            if (Files.isExecutable(p)) {
                return Optional.of(p);
            }
        }

        return Optional.empty();
    }

    private static Optional<Path> resolveSystemChromedriver() {
        List<String> candidates =
                Arrays.asList(
                        "/usr/bin/chromedriver",
                        "/usr/lib/chromium-browser/chromedriver",
                        "/usr/lib/chromium/chromedriver",
                        "/snap/bin/chromium.chromedriver");

        for (String candidate : candidates) {
            Path p = Paths.get(candidate);
            if (Files.isExecutable(p)) {
                return Optional.of(p);
            }
        }

        return Optional.empty();
    }
}
