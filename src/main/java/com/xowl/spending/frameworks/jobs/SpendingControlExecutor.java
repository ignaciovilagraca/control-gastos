package com.xowl.spending.frameworks.jobs;

import com.xowl.spending.adapters.ArgumentsParser;
import com.xowl.spending.entities.Arguments;
import com.xowl.spending.frameworks.config.DependencyInjector;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpendingControlExecutor {
    private final SpendingControlJob job = new SpendingControlJob();

    public void execute() {
        ArgumentsParser parser = DependencyInjector.argumentsParser();
        Arguments arguments = parser.parseFromEnvironment();
        job.execute(arguments);
    }
}
