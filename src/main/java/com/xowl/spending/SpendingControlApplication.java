package com.xowl.spending;

import com.xowl.spending.frameworks.config.DotEnvBootstrap;
import com.xowl.spending.frameworks.jobs.SpendingControlExecutor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SpendingControlApplication {

    static {
        DotEnvBootstrap.load();
    }

    private static final SpendingControlExecutor SPENDING_CONTROL_EXECUTOR = new SpendingControlExecutor();

    public static void main(String[] args) {
        SPENDING_CONTROL_EXECUTOR.execute();
    }
}
