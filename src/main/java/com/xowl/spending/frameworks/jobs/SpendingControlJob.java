package com.xowl.spending.frameworks.jobs;

import com.xowl.spending.entities.Arguments;
import com.xowl.spending.frameworks.config.DependencyInjector;
import com.xowl.spending.usecases.SpendingControl;

public class SpendingControlJob {
    private final SpendingControl spendingControl = DependencyInjector.spendingControl();

    public void execute(Arguments arguments) {
        spendingControl.start(arguments);
    }
}
