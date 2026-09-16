package com.xowl.spending.usecases;

import com.xowl.spending.entities.Arguments;
import com.xowl.spending.entities.Spending;
import com.xowl.spending.frameworks.dolar.DolarApiResponse;
import com.xowl.spending.frameworks.http.CustomHttpClient;
import com.xowl.spending.usecases.browser.BrowserTool;
import com.xowl.spending.usecases.exceptions.JobExecutionException;
import com.xowl.spending.usecases.messenger.InstantMessenger;
import com.xowl.spending.usecases.repository.SpendingRepository;
import com.xowl.spending.usecases.webdriver.WebDriverSupplier;
import com.google.gson.Gson;
import java.net.http.HttpResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;

@Slf4j
public class SpendingControl {

    private final static String SOMETHING_WENT_WRONG_MESSAGE = "Something went wrong with Selenium run: %s";
    private final static String dolarApi = "https://dolarapi.com/v1/dolares/cripto";
    private final SpendingRepository spendingRepository;
    private final WebDriverSupplier webDriverSupplier;
    private final InstantMessenger instantMessenger;
    private final BrowserTool browserTool;
    private final CustomHttpClient customHttpClient;
    private final Gson gson;

    public SpendingControl(SpendingRepository spendingRepository, WebDriverSupplier webDriverSupplier, InstantMessenger instantMessenger, BrowserTool browserTool, CustomHttpClient customHttpClient, Gson gson) {
        this.spendingRepository = spendingRepository;
        this.webDriverSupplier = webDriverSupplier;
        this.instantMessenger = instantMessenger;
        this.browserTool = browserTool;
        this.customHttpClient = customHttpClient;
        this.gson = gson;
    }

    public void start(Arguments arguments) {
        log.info("Starting job execution");

        List<Spending> gastos;
        WebDriver driver = webDriverSupplier.supply();
        try {
            gastos = browserTool.findUpdates(driver, arguments.getAuthentication());
        } catch (Exception e) {
            log.error(e.getMessage());
            instantMessenger.sendErrorMessage(String.format(SOMETHING_WENT_WRONG_MESSAGE, e.getMessage()), arguments.getErrorsBotToken());
            throw new JobExecutionException(e);
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception quitEx) {
                    log.warn("Driver quit failed (non-fatal): {}", quitEx.getMessage());
                }
            }
        }

        HttpResponse<String> response = customHttpClient.get(dolarApi);
        if (response.statusCode() > 299) {
            throw new JobExecutionException(new RuntimeException("failed to call dollar api"));
        }
        DolarApiResponse parsedResponse = gson.fromJson(response.body(), DolarApiResponse.class);

        int lastSize = spendingRepository.getSpendingListSize();
        instantMessenger.sendSuccessMessage(gastos, parsedResponse.venta, lastSize, arguments.getGastosBotToken());
        spendingRepository.saveSpendingList(gastos);

        log.info("Ending job execution");
    }
}
