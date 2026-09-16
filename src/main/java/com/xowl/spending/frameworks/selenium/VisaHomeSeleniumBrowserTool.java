package com.xowl.spending.frameworks.selenium;

import com.xowl.spending.adapters.SpendingParser;
import com.xowl.spending.entities.Authentication;
import com.xowl.spending.entities.Spending;
import com.xowl.spending.usecases.browser.BrowserTool;
import com.xowl.spending.usecases.otp.OtpReceiver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class VisaHomeSeleniumBrowserTool implements BrowserTool {

    private static final String LOGIN_URL = "https://mis-tarjetas.prismamediosdepago.com";

    private static final By VER_ULTIMOS_MOVIMIENTOS = By.xpath(
            "//*[self::input or self::button or self::a][@value='Ver últimos movimientos' or "
                    + "contains(normalize-space(.),'Ver últimos movimientos')]");

    private static final By AUTORIZACIONES_PENDIENTES = By.xpath(
            "//a[contains(normalize-space(.),'Autorizaciones pendientes')]");

    private static final List<By> OTP_INPUT_CANDIDATES = Arrays.asList(
            By.cssSelector("[data-testid*='otp' i]"),
            By.cssSelector("input[autocomplete='one-time-code']"),
            By.cssSelector("input[inputmode='numeric']"),
            By.xpath("//input[(@type='text' or @type='tel') and (contains(@name,'code') or contains(@id,'code'))]"));

    /** Prisma OTP step: prefer explicit "Verificar" before generic submit (avoids wrong button). */
    private static final List<By> OTP_SUBMIT_CANDIDATES = Arrays.asList(
            By.xpath("//button[contains(normalize-space(.),'Verificar')]"),
            By.xpath("//*[@role='button'][contains(normalize-space(.),'Verificar')]"),
            By.xpath("//input[@type='submit' and contains(@value,'Verificar')]"),
            By.cssSelector("[data-testid*='verify' i]"),
            By.cssSelector("[data-testid*='confirm' i]"),
            By.xpath("//button[contains(normalize-space(.),'Confirmar') or contains(normalize-space(.),'Continuar')]"),
            By.cssSelector("[data-testid*='submit' i]"),
            By.cssSelector("button[type='submit']"));

    /** Error modal shown by the Next.js login when credentials are rejected (no OTP mail is sent). */
    private static final By LOGIN_REJECTED_MODAL = By.xpath(
            "//*[contains(text(),'No pudimos validar tus datos') or contains(text(),'Credenciales inválidas')]");

    /** Shown after OTP verification; often same test id as the first login submit. */
    private static final List<By> INGRESAR_AFTER_OTP_CANDIDATES = Arrays.asList(
            By.cssSelector("[data-testid='sign-in-credentials']"),
            By.xpath("//button[contains(normalize-space(.),'Ingresar')]"),
            By.xpath("//*[@role='button'][contains(normalize-space(.),'Ingresar')]"),
            By.xpath("//input[@type='submit' and contains(@value,'Ingresar')]"));

    private final SpendingParser spendingParser;
    private final Optional<OtpReceiver> otpReceiver;

    public VisaHomeSeleniumBrowserTool(SpendingParser spendingParser, Optional<OtpReceiver> otpReceiver) {
        this.spendingParser = spendingParser;
        this.otpReceiver = otpReceiver;
    }

    public List<Spending> findUpdates(WebDriver driver, Authentication authentication) throws InterruptedException {
        WebDriverWait webDriverWait = new WebDriverWait(driver, Duration.ofSeconds(30));

        login(driver, authentication);

        TimeUnit.SECONDS.sleep(5);

        driver.manage().window().maximize();

        List<WebElement> activeModals = driver.findElements(By.xpath(
                "//div[contains(concat(' ', normalize-space(@class), ' '), ' modal ') and contains(concat(' ', normalize-space(@class), ' '), ' in ')]"
                        + "//a[contains(concat(' ', normalize-space(@class), ' '), ' modal-titlebar-close ')]"));

        if (!activeModals.isEmpty()) {
            Collections.reverse(activeModals);
            for (WebElement closeAnchor : activeModals) {
                closeModalSafely(driver, closeAnchor);
            }
            TimeUnit.SECONDS.sleep(5);
        }

        webDriverWait.until(ExpectedConditions.elementToBeClickable(VER_ULTIMOS_MOVIMIENTOS));

        List<WebElement> cards = driver.findElements(By.className("card-item"));
        List<Spending> spendingList = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            List<Spending> defaultCardSpendingList = handleLastMovements(driver);
            spendingList.addAll(defaultCardSpendingList);

            if (cards.size() > 1 && i + 1 < cards.size()) {
                clickSafely(driver, driver.findElement(By.id("cardsDropDown")));

                TimeUnit.SECONDS.sleep(5);

                clickSafely(driver, driver.findElements(By.className("card-item")).get(i + 1));

                TimeUnit.SECONDS.sleep(5);
            }
        }

        return spendingList;
    }

    private List<Spending> handleLastMovements(WebDriver driver) throws InterruptedException {
        clickSafely(driver, driver.findElement(VER_ULTIMOS_MOVIMIENTOS));

        TimeUnit.SECONDS.sleep(5);

        List<WebElement> pagos = driver.findElements(By.xpath("//a[contains(text(),'Pagos')]"));

        if (!pagos.isEmpty()) {
            List<WebElement> tarjetas = driver.findElements(By.xpath("//a[contains(text(),'Tarjeta :')]"));
            for (WebElement tarjeta : tarjetas) {
                clickSafely(driver, tarjeta);
            }
            TimeUnit.SECONDS.sleep(5);
        }

        List<String> rows = getRows(driver);

        List<Spending> gastos = rows.stream().map(spendingParser::parseSpending).collect(Collectors.toList());

        clickSafely(driver, driver.findElement(AUTORIZACIONES_PENDIENTES));

        TimeUnit.SECONDS.sleep(5);

        rows = getRows(driver);

        List<Spending> autorizaciones = rows.stream().map(spendingParser::parseAuth).collect(Collectors.toList());

        return Stream.concat(gastos.stream(), autorizaciones.stream()).collect(Collectors.toList());
    }

    /**
     * Centers the element before clicking so fixed headers/footers don't cover it, and falls back to a
     * JavaScript click if something still intercepts it.
     */
    private static void clickSafely(WebDriver driver, WebElement el) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block:'center',inline:'nearest'});", el);
        TimeUnit.MILLISECONDS.sleep(150);
        try {
            el.click();
        } catch (ElementClickInterceptedException e) {
            js.executeScript("arguments[0].click();", el);
        }
    }

    /** Hidden or animating leftovers matched by the modal XPath must not kill the whole run. */
    private static void closeModalSafely(WebDriver driver, WebElement closeAnchor) {
        try {
            if (!closeAnchor.isDisplayed()) {
                return;
            }
            closeAnchor.click();
        } catch (WebDriverException e) {
            try {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", closeAnchor);
            } catch (WebDriverException ignored) {
            }
        }
    }

    private void login(WebDriver driver, Authentication authentication) throws InterruptedException {
        try {
            driver.get(LOGIN_URL);
        } catch (WebDriverException e) {
            // On slow devices (Pi) the page-load deadline can fire — or the driver HTTP call can
            // time out — even with EAGER. Stop loading and let the readyState wait below decide.
            try {
                ((JavascriptExecutor) driver).executeScript("window.stop();");
            } catch (WebDriverException ignored) {
            }
        }

        WebDriverWait pageWait = new WebDriverWait(driver, Duration.ofMinutes(3));
        pageWait.until(d -> {
            Object state = ((JavascriptExecutor) d).executeScript("return document.readyState");
            // Do not require "complete" — third-party resources can keep readyState at interactive indefinitely.
            return "complete".equals(state) || "interactive".equals(state);
        });

        // Next.js login hydrates client-side; on slow devices (e.g. Raspberry Pi) the form appears after readyState.
        TimeUnit.SECONDS.sleep(5);

        waitForLoginForm(driver);

        if (!driver.findElements(By.id("documentNumber")).isEmpty()) {
            loginNextJs(driver, authentication);
        } else {
            loginLegacyJsf(driver, authentication);
        }

        WebDriverWait afterLogin = new WebDriverWait(driver, Duration.ofSeconds(90));
        afterLogin.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.className("card-item")),
                ExpectedConditions.presenceOfElementLocated(By.id("cardsDropDown")),
                ExpectedConditions.elementToBeClickable(VER_ULTIMOS_MOVIMIENTOS)));
    }

    /**
     * Waits for either login form variant; on timeout reloads once (Next.js hydration sometimes
     * hangs on slow devices) and, if the form still does not appear, fails with a description of
     * what the page actually showed instead of an opaque wait-condition message.
     */
    private void waitForLoginForm(WebDriver driver) throws InterruptedException {
        WebDriverWait formWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        try {
            formWait.until(VisaHomeSeleniumBrowserTool::loginFormPresent);
        } catch (TimeoutException first) {
            try {
                driver.navigate().refresh();
            } catch (WebDriverException ignored) {
            }
            TimeUnit.SECONDS.sleep(5);
            try {
                formWait.until(VisaHomeSeleniumBrowserTool::loginFormPresent);
            } catch (TimeoutException second) {
                throw new IllegalStateException(
                        "Login form never appeared (even after reload). " + describePageState(driver), second);
            }
        }
    }

    private static boolean loginFormPresent(WebDriver driver) {
        return !driver.findElements(By.id("documentNumber")).isEmpty()
                || !driver.findElements(By.id("loginFrm:docNumber")).isEmpty();
    }

    private static String describePageState(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object readyState = js.executeScript("return document.readyState");
            Object body = js.executeScript(
                    "return document.body ? document.body.innerText.replace(/\\s+/g,' ').substring(0,300) : '<no body>'");
            return "url=" + driver.getCurrentUrl() + ", title=" + driver.getTitle()
                    + ", readyState=" + readyState + ", body=" + body
                    + ", screenshot=" + saveDebugScreenshot(driver);
        } catch (WebDriverException e) {
            return "page state unavailable: " + e.getMessage();
        }
    }

    private static String saveDebugScreenshot(WebDriver driver) {
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path path = Paths.get(System.getProperty("user.home"),
                    "visahome-login-timeout-" + System.currentTimeMillis() + ".png");
            Files.write(path, png);
            return path.toString();
        } catch (Exception e) {
            return "<failed: " + e.getMessage() + ">";
        }
    }

    private void loginNextJs(WebDriver driver, Authentication authentication) throws InterruptedException {
        WebElement docType = driver.findElement(By.id("documentType"));
        new Select(docType).selectByValue("DNI");

        WebElement docNumber = driver.findElement(By.id("documentNumber"));
        docNumber.clear();
        docNumber.sendKeys(authentication.getDocumentNumber());

        driver.findElement(By.id("username")).sendKeys(authentication.getNickname());
        driver.findElement(By.id("password")).sendKeys(authentication.getPassword());

        Instant submittedAt = Instant.now();
        driver.findElement(By.cssSelector("[data-testid='sign-in-credentials']")).click();

        if (otpReceiver.isPresent()) {
            waitAndApplyEmailOtp(driver, otpReceiver.get(), submittedAt);
        }
    }

    private void waitAndApplyEmailOtp(WebDriver driver, OtpReceiver receiver, Instant notBefore)
            throws InterruptedException {
        WebDriverWait otpUiWait = new WebDriverWait(driver, Duration.ofSeconds(55));
        WebElement otpInput;
        try {
            otpInput = otpUiWait.until(d -> {
                if (!d.findElements(LOGIN_REJECTED_MODAL).isEmpty()) {
                    throw new IllegalStateException(
                            "VisaHome rejected the login (Credenciales inválidas): check CONTROL_GASTOS_* credentials");
                }
                return findDisplayedOtpInput(d);
            });
        } catch (TimeoutException e) {
            return;
        }

        Optional<String> code;
        try {
            code = receiver.pollAfter(notBefore, Duration.ofMinutes(6));
        } catch (IOException e) {
            throw new IllegalStateException("Gmail API error while polling for OTP", e);
        }
        if (code.isEmpty()) {
            throw new IllegalStateException("No email OTP received within timeout (Gmail API)");
        }

        otpInput.clear();
        otpInput.sendKeys(code.get());
        clickFirstClickable(
                driver,
                OTP_SUBMIT_CANDIDATES,
                20_000,
                "Could not find a clickable button to submit the email OTP (e.g. Verificar)");
        clickFirstClickable(
                driver,
                INGRESAR_AFTER_OTP_CANDIDATES,
                20_000,
                "Could not find Ingresar after email OTP verification");
        TimeUnit.SECONDS.sleep(3);
    }

    private static WebElement findDisplayedOtpInput(WebDriver driver) {
        return OTP_INPUT_CANDIDATES.stream()
                .flatMap(by -> driver.findElements(by).stream())
                .filter(WebElement::isDisplayed)
                .findFirst()
                .orElse(null);
    }

    private static void clickFirstClickable(WebDriver driver, List<By> candidates, long timeoutMs, String notFoundMessage)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (By by : candidates) {
                for (WebElement el : driver.findElements(by)) {
                    if (!el.isDisplayed() || !el.isEnabled()) {
                        continue;
                    }
                    clickSafely(driver, el);
                    return;
                }
            }
            Thread.sleep(300);
        }
        throw new IllegalStateException(notFoundMessage);
    }

    private void loginLegacyJsf(WebDriver driver, Authentication authentication) {
        driver.findElement(By.id("loginFrm:docNumber")).sendKeys(authentication.getDocumentNumber());
        driver.findElement(By.id("loginFrm:password")).sendKeys(authentication.getPassword());
        driver.findElement(By.id("loginFrm:nickname")).sendKeys(authentication.getNickname());
        driver.findElement(By.id("loginFrm:button")).click();
    }

    private List<String> getRows(WebDriver driver) {
        return driver.findElements(By.xpath("//tr")).stream()
                .map(WebElement::getText)
                .filter(s -> !s.isEmpty() && !s.contains("SU PAGO EN"))
                .collect(Collectors.toList());
    }
}
