package com.dynatrace.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AppWithoutOpenTelemetry {
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {

        logger.info("This app does not use OpenTelemetry.");
    }
}
