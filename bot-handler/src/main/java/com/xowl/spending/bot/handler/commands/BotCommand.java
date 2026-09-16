package com.xowl.spending.bot.handler.commands;

import com.xowl.spending.bot.handler.TelegramUpdateResponse;

public interface BotCommand {
    String getCommand();
    void execute(TelegramUpdateResponse.Update update, String token);
}
