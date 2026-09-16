package com.xowl.spending.adapters;

import com.xowl.spending.entities.Arguments;
import com.xowl.spending.entities.Authentication;
import com.xowl.spending.entities.BotToken;
import com.xowl.spending.frameworks.config.EnvVariables;

public class ArgumentsParser {

    public static final String ENV_DOCUMENT_NUMBER = "CONTROL_GASTOS_DOCUMENT_NUMBER";
    public static final String ENV_VISA_PASSWORD = "CONTROL_GASTOS_VISA_PASSWORD";
    public static final String ENV_VISA_NICKNAME = "CONTROL_GASTOS_VISA_NICKNAME";
    public static final String ENV_TELEGRAM_GASTOS_TOKEN = "CONTROL_GASTOS_TELEGRAM_GASTOS_TOKEN";
    public static final String ENV_TELEGRAM_ERRORES_TOKEN = "CONTROL_GASTOS_TELEGRAM_ERRORES_TOKEN";

    public Arguments parseFromEnvironment() {
        String documentNumber = required(ENV_DOCUMENT_NUMBER);
        String password = required(ENV_VISA_PASSWORD);
        String nickname = required(ENV_VISA_NICKNAME);
        String gastosBotTokenString = required(ENV_TELEGRAM_GASTOS_TOKEN);
        String errorsBotTokenString = required(ENV_TELEGRAM_ERRORES_TOKEN);

        Authentication authentication = Authentication.builder()
                .documentNumber(documentNumber)
                .password(password)
                .nickname(nickname)
                .build();
        BotToken gastosBotToken = BotToken.builder().token(gastosBotTokenString).build();
        BotToken errorsBotToken = BotToken.builder().token(errorsBotTokenString).build();

        return Arguments.builder()
                .authentication(authentication)
                .gastosBotToken(gastosBotToken)
                .errorsBotToken(errorsBotToken)
                .build();
    }

    public static boolean isEnvironmentConfigured() {
        return !EnvVariables.isBlank(EnvVariables.get(ENV_DOCUMENT_NUMBER))
                && !EnvVariables.isBlank(EnvVariables.get(ENV_VISA_PASSWORD))
                && !EnvVariables.isBlank(EnvVariables.get(ENV_VISA_NICKNAME))
                && !EnvVariables.isBlank(EnvVariables.get(ENV_TELEGRAM_GASTOS_TOKEN))
                && !EnvVariables.isBlank(EnvVariables.get(ENV_TELEGRAM_ERRORES_TOKEN));
    }

    private static String required(String key) {
        String v = EnvVariables.get(key);
        if (EnvVariables.isBlank(v)) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return v.trim();
    }
}
