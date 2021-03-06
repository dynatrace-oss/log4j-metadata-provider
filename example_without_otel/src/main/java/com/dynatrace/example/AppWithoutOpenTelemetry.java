/*
 * Copyright 2021 Dynatrace LLC
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.dynatrace.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AppWithoutOpenTelemetry {
    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) {
        logger.info("This app does not use OpenTelemetry.");
    }
}
