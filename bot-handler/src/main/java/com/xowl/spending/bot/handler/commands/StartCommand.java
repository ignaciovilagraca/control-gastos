package com.xowl.spending.bot.handler.commands;

import com.google.gson.Gson;
import com.xowl.spending.bot.handler.TelegramUpdateResponse;
import com.xowl.spending.frameworks.http.CustomHttpClient;
import com.xowl.spending.frameworks.telegram.TelegramMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StartCommand implements BotCommand {
    private static final String SEND_MESSAGE_URL = "https://api.telegram.org/bot%s/sendMessage";
    private final CustomHttpClient httpClient;
    private final Gson gson;

    public StartCommand(CustomHttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    @Override
    public String getCommand() {
        return "/start";
    }

    @Override
    public void execute(TelegramUpdateResponse.Update update, String token) {
        long chatId = update.getMessage().getChat().getId();
        log.info("Received /start command from chatId: " + chatId);
        String helpMessage = "Available commands:\n" +
                "/run - Ejecuta el job usando variables CONTROL_GASTOS_* (.env)\n" +
                "/start - Show this message";
        sendMessage(chatId, helpMessage, token);
    }

    private void sendMessage(long chatId, String text, String token) {
        String url = String.format(SEND_MESSAGE_URL, token);
        TelegramMessage message = new TelegramMessage(text, String.valueOf(chatId), "");
        httpClient.post(url, gson.toJson(message));
    }
}
