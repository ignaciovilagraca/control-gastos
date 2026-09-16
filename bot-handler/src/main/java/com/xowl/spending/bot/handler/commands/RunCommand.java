package com.xowl.spending.bot.handler.commands;

import com.google.gson.Gson;
import com.xowl.spending.bot.handler.TelegramUpdateResponse;
import com.xowl.spending.frameworks.http.CustomHttpClient;
import com.xowl.spending.adapters.ArgumentsParser;
import com.xowl.spending.frameworks.jobs.SpendingControlExecutor;
import com.xowl.spending.frameworks.telegram.TelegramMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunCommand implements BotCommand {
    private static final String SEND_MESSAGE_URL = "https://api.telegram.org/bot%s/sendMessage";
    private final SpendingControlExecutor executor;
    private final CustomHttpClient httpClient;
    private final Gson gson;

    public RunCommand(SpendingControlExecutor executor, CustomHttpClient httpClient, Gson gson) {
        this.executor = executor;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    @Override
    public String getCommand() {
        return "/run";
    }

    @Override
    public void execute(TelegramUpdateResponse.Update update, String token) {
        long chatId = update.getMessage().getChat().getId();
        log.info("Received /run command from chatId: " + chatId);

        if (!ArgumentsParser.isEnvironmentConfigured()) {
            sendMessage(
                    chatId,
                    "Error: faltan variables CONTROL_GASTOS_* en el entorno (.env). Revisá documento, password, usuario y tokens de Telegram.",
                    token);
            return;
        }

        sendMessage(chatId, "Running spending control job (variables de entorno)...", token);
        try {
            executor.execute();
        } catch (Exception e) {
            log.error("Error executing job", e);
            sendMessage(chatId, "Error executing job: " + e.getMessage(), token);
        }
    }

    private void sendMessage(long chatId, String text, String token) {
        String url = String.format(SEND_MESSAGE_URL, token);
        TelegramMessage message = new TelegramMessage(text, String.valueOf(chatId), "");
        httpClient.post(url, gson.toJson(message));
    }
}
