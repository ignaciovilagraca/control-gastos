package com.xowl.spending.usecases.browser;

import com.xowl.spending.entities.Authentication;
import com.xowl.spending.entities.Spending;
import java.util.List;
import org.openqa.selenium.WebDriver;

public interface BrowserTool {
    List<Spending> findUpdates(WebDriver driver, Authentication authentication) throws InterruptedException;
}
