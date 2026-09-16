package com.xowl.spending.bot.handler;

import com.xowl.spending.frameworks.config.DotEnvBootstrap;

public class BotHandlerApplication {
    public static void main(String[] args) {
        DotEnvBootstrap.load();
        new BotHandler().run();
    }
}
