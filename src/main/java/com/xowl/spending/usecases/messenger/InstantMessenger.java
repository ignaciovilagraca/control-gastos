package com.xowl.spending.usecases.messenger;

import com.xowl.spending.entities.BotToken;
import com.xowl.spending.entities.Spending;
import java.util.List;

public interface InstantMessenger {
    void sendSuccessMessage(List<Spending> current, Double dollar, Integer lastSize, BotToken botToken);

    void sendErrorMessage(String message, BotToken botToken);
}
