package com.xowl.spending.frameworks.telegram;

import com.xowl.spending.entities.BotToken;
import com.xowl.spending.entities.Moneda;
import com.xowl.spending.entities.Spending;
import com.xowl.spending.frameworks.exceptions.TelegramApiException;
import com.xowl.spending.frameworks.http.CustomHttpClient;
import com.xowl.spending.usecases.messenger.InstantMessenger;
import com.xowl.spending.utils.NumberFormatter;
import com.google.gson.Gson;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TelegramBot implements InstantMessenger {
    private final CustomHttpClient httpClient;
    private final String url;
    private final String chatId;
    private final Double monthlySalaryUsd;
    private final Gson gson;

    public TelegramBot(CustomHttpClient httpClient, String url, String chatId, Double monthlySalaryUsd, Gson gson) {
        this.httpClient = httpClient;
        this.url = url;
        this.chatId = chatId;
        this.monthlySalaryUsd = monthlySalaryUsd;
        this.gson = gson;
    }

    @Override
    public void sendSuccessMessage(List<Spending> current, Double dollar, Integer lastSize, BotToken botToken) {
        if (current.size() == lastSize) {
            return;
        }

        String topSpendingMessage = "*GASTOS:*";
        callGastosBot(topSpendingMessage, botToken.getToken());

        current.stream()
                .filter(c -> c.comprobante().isPresent())
                .sorted(Comparator.comparing(Spending::monto))
                .map(Spending::print)
                .map(this::sanitizeMessage)
                .forEach(m -> callGastosBot(m, botToken.getToken()));

        Double total = current.stream()
                .filter(s -> s.moneda() == Moneda.ARS)
                .mapToDouble(Spending::monto)
                .sum();
        String totalString = NumberFormatter.format(total);
        String message = "*ACUMULADO ARS:* " + totalString;
        callGastosBot(message, botToken.getToken());

        Double totalUsd = current.stream()
                .filter(s -> s.moneda() == Moneda.USD)
                .mapToDouble(Spending::monto)
                .sum();
        String totalUsdString = NumberFormatter.format(totalUsd);
        String usdMessage = "*ACUMULADO USD:* " + totalUsdString;
        callGastosBot(usdMessage, botToken.getToken());

        if (monthlySalaryUsd != null && monthlySalaryUsd > 0) {
            DecimalFormat df = new DecimalFormat("#.00%");
            String formatted = df.format((totalUsd + total / dollar) / monthlySalaryUsd);
            String percentageMessage = "*PORCENTAJE SUELDO:* " + formatted;
            callGastosBot(percentageMessage, botToken.getToken());
        }
    }

    @Override
    public void sendErrorMessage(String message, BotToken botToken) {
        callTelegramApi(message, botToken.getToken(), "");
    }

    private void callGastosBot(String message, String token) {
        callTelegramApi(message, token, "Markdown");
    }

    private void callTelegramApi(String message, String token, String parseMode) {
        TelegramMessage telegramMessage = new TelegramMessage(message, chatId, parseMode);
        String body = gson.toJson(telegramMessage);

        HttpResponse<String> response = httpClient.post(String.format(url, token), body);

        if (response.statusCode() > 299) {
            sendErrorMessage(response, message, parseMode, token);
            throw new TelegramApiException("Something went wrong while calling Telegram API.");
        }
    }

    private void sendErrorMessage(HttpResponse<String> response, String message, String parseMode, String token) {
        TelegramErrorResponse telegramErrorResponse = gson.fromJson(response.body(), TelegramErrorResponse.class);

        String errorMessage = "Something went wrong while sending message.\nError: "
                + telegramErrorResponse.getDescription();

        if ("Markdown".equals(parseMode)) {
            errorMessage += "\nMessage: " + message;
        }

        log.error(errorMessage);

        TelegramMessage telegramMessage = new TelegramMessage(errorMessage, chatId, "");
        String body = gson.toJson(telegramMessage);

        HttpResponse<String> errorMessageResponse = httpClient.post(String.format(url, token), body);

        if (errorMessageResponse.statusCode() > 299) {
            log.error(errorMessageResponse.body());
        }
    }

    private String sanitizeMessage(String message) {
        message = message.replace("*", "\\*");
        message = message.replace("_", "\\_");
        return message;
    }
}
