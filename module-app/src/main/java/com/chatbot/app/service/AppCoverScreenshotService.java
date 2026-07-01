package com.chatbot.app.service;

import com.chatbot.app.config.AppStorageProperties;
import com.chatbot.app.config.SeleniumProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppCoverScreenshotService {

    private final SeleniumProperties seleniumProperties;
    private final AppStorageProperties storageProperties;

    public boolean isEnabled() {
        return seleniumProperties.isEnabled();
    }

    public boolean capturePreview(Long appId, String previewPath) {
        if (!isEnabled()) {
            return false;
        }
        String base = seleniumProperties.getPreviewBaseUrl().replaceAll("/$", "");
        String url = base + previewPath;
        Path out = coverPngPath(appId);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--window-size=" + seleniumProperties.getViewportWidth()
                + "," + seleniumProperties.getViewportHeight());

        ChromeDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.get(url);
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.createDirectories(out.getParent());
            Files.write(out, png);
            log.info("Selenium cover screenshot saved: {}", out);
            return true;
        } catch (Exception e) {
            log.warn("Selenium screenshot failed for app {}: {}", appId, e.getMessage());
            return false;
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public Path coverPngPath(Long appId) {
        return Path.of(storageProperties.getBasePath(), "covers", appId + ".png");
    }
}
