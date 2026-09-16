package com.xowl.spending.bot.handler;

import com.google.gson.Gson;
import com.xowl.spending.bot.handler.commands.BotCommand;
import com.xowl.spending.bot.handler.commands.RunCommand;
import com.xowl.spending.bot.handler.commands.StartCommand;
import com.xowl.spending.adapters.ArgumentsParser;
import com.xowl.spending.frameworks.config.EnvVariables;
import com.xowl.spending.frameworks.http.CustomHttpClient;
import com.xowl.spending.frameworks.jobs.SpendingControlExecutor;
import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BotHandler {
    private static final String GET_UPDATES_URL = "https://api.telegram.org/bot%s/getUpdates?offset=%d";
    private static final SpendingControlExecutor EXECUTOR = new SpendingControlExecutor();
    private static final CustomHttpClient HTTP_CLIENT = new CustomHttpClient();
    private static final Gson GSON = new Gson();
    private static final Map<String, BotCommand> COMMANDS = new HashMap<>();
    private static final BotCommand DEFAULT_COMMAND;

    static {
        registerCommand(new RunCommand(EXECUTOR, HTTP_CLIENT, GSON));
        DEFAULT_COMMAND = new StartCommand(HTTP_CLIENT, GSON);
        registerCommand(DEFAULT_COMMAND);
    }

    private static void registerCommand(BotCommand command) {
        COMMANDS.put(command.getCommand(), command);
    }

    private long lastUpdateId = 0;
    private static final long RETRY_INTERVAL_MS = 3600000; // 1 hour

    public void run() {
        while (!ArgumentsParser.isEnvironmentConfigured()) {
            log.error("Missing CONTROL_GASTOS_* environment variables (.env). Retrying in 1 hour...");
            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                log.error("Bot handler initialization interrupted", e);
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.info("Environment configured; starting bot handler.");

        String gastosBotToken = EnvVariables.get(ArgumentsParser.ENV_TELEGRAM_GASTOS_TOKEN).trim();

        log.info("Bot handler started. Polling for messages...");

        while (true) {
            try {
                pollUpdates(gastosBotToken);
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error("Bot handler interrupted", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error polling updates", e);
            }
        }
    }

    private void pollUpdates(String token) {
        String url = String.format(GET_UPDATES_URL, token, lastUpdateId + 1);
        HttpResponse<String> response = HTTP_CLIENT.get(url);

        if (response.statusCode() == 200) {
            TelegramUpdateResponse updateResponse = GSON.fromJson(response.body(), TelegramUpdateResponse.class);
            if (updateResponse.isOk() && updateResponse.getResult() != null) {
                for (TelegramUpdateResponse.Update update : updateResponse.getResult()) {
                    lastUpdateId = update.getUpdateId();
                    handleUpdate(update, token);
                }
            }
        } else {
            log.error("Failed to poll updates: " + response.body());
        }
    }

    private void handleUpdate(TelegramUpdateResponse.Update update, String token) {
        if (update.getMessage() != null && update.getMessage().getText() != null) {
            String text = update.getMessage().getText();
            BotCommand command = COMMANDS.getOrDefault(text, DEFAULT_COMMAND);
            command.execute(update, token);
        }
    }
}
