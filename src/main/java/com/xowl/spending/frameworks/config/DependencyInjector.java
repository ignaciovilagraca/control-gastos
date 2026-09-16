package com.xowl.spending.frameworks.config;

import com.xowl.spending.adapters.ArgumentsParser;
import com.xowl.spending.adapters.SpendingParser;
import com.xowl.spending.frameworks.http.CustomHttpClient;
import com.xowl.spending.frameworks.repository.SpendingHibernateRepository;
import com.xowl.spending.frameworks.gmail.GmailApiOtpReceiver;
import com.xowl.spending.frameworks.selenium.VisaHomeSeleniumBrowserTool;
import com.xowl.spending.frameworks.telegram.TelegramBot;
import com.xowl.spending.frameworks.webdriver.ChromeWebDriverSupplier;
import com.xowl.spending.usecases.SpendingControl;
import com.xowl.spending.usecases.otp.OtpReceiver;
import com.google.gson.Gson;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

public class DependencyInjector {

    public static final String ENV_TELEGRAM_CHAT_ID = "CONTROL_GASTOS_TELEGRAM_CHAT_ID";
    public static final String ENV_MONTHLY_SALARY_USD = "CONTROL_GASTOS_MONTHLY_SALARY_USD";

    private static final String url = "https://api.telegram.org/bot%s/sendMessage";

    public static SpendingControl spendingControl() {
        return new SpendingControl(
                new SpendingHibernateRepository(),
                new ChromeWebDriverSupplier(),
                new TelegramBot(new CustomHttpClient(), url, chatId(), monthlySalaryUsd(), new Gson()),
                new VisaHomeSeleniumBrowserTool(new SpendingParser(), gmailOtpReceiver()),
                new CustomHttpClient(),
                new Gson()
        );
    }

    private static Optional<OtpReceiver> gmailOtpReceiver() {
        try {
            return GmailApiOtpReceiver.fromEnvironment().map(r -> r);
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Gmail API OTP is misconfigured (check CONTROL_GASTOS_GMAIL_CLIENT_ID/SECRET/REFRESH_TOKEN)", e);
        }
    }

    /** Which Telegram conversation the spendings are sent to. */
    private static String chatId() {
        return EnvVariables.required(ENV_TELEGRAM_CHAT_ID);
    }

    /**
     * The salary the running total is measured against, in dollars.
     *
     * Optional, and absent by default: it is the one number here that says
     * something about the person running this rather than about their card, and
     * a repository is the wrong place for it. Without it the bot still reports
     * every charge and both totals, and simply omits the percentage line.
     */
    private static Double monthlySalaryUsd() {
        String raw = EnvVariables.get(ENV_MONTHLY_SALARY_USD);
        if (EnvVariables.isBlank(raw)) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(ENV_MONTHLY_SALARY_USD + " is not a number: " + raw, e);
        }
    }

    public static ArgumentsParser argumentsParser() {
        return new ArgumentsParser();
    }
}
