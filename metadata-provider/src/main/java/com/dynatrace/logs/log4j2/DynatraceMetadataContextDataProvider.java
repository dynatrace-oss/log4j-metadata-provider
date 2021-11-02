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

package com.dynatrace.logs.log4j2;

import com.dynatrace.metric.util.DynatraceMetadataEnricherWrapper;
import org.apache.logging.log4j.core.util.ContextDataProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


public class DynatraceMetadataContextDataProvider implements ContextDataProvider {
    private final Map<String, String> dynatraceMetadata;
    private final ContextDataRetriever openTelemetryContextProvider;

    // using a log4j logger here can lead to an endless loop, therefore use java.util.logging.
    private static final Logger logger = Logger.getLogger(DynatraceMetadataContextDataProvider.class.getName());

    // export all metadata by default
    public DynatraceMetadataContextDataProvider() {
        this(DynatraceMetadataContextDataProvider.tryLoadOpenTelemetryTraceSupport(),
                DynatraceMetadataEnricherWrapper.getDynatraceMetadata());
    }

    // VisibleForTesting
    DynatraceMetadataContextDataProvider(
            ContextDataRetriever openTelemetryContextProvider,
            Map<String, String> dynatraceMetadata
    ) {
        this.dynatraceMetadata = dynatraceMetadata;
        this.openTelemetryContextProvider = openTelemetryContextProvider;
    }

    /**
     * Attempts to load OpenTelemetry support by reflection. The OpenTelemetryContextDataRetriever
     * will crash upon instantiation if no dependency to the opentelemetry-api package is on the
     * classpath.
     *
     * @return A {@link OpenTelemetrySpanContextDataRetriever} if the OpenTelemetry dependency is
     * satisfied, and null otherwise.
     */
    private static ContextDataRetriever tryLoadOpenTelemetryTraceSupport() {
        try {
            logger.finer("trying to load OpenTelemetry support...");
            Class<?> clazz = Class.forName("com.dynatrace.logs.log4j2.OpenTelemetrySpanContextDataRetriever");
            final ContextDataRetriever instance = (ContextDataRetriever) clazz.getDeclaredConstructor().newInstance();
            logger.finer("OpenTelemetry support successfully loaded.");
            return instance;
        } catch (Exception ignored) {
            logger.fine("OpenTelemetry support could not be loaded. " +
                    "This is normal if no OpenTelemetry dependency is installed. " +
                    "Log Context will not be enriched with OpenTelemetry metadata.");
            return null;
        }
    }

    @Override
    public Map<String, String> supplyContextData() {
        Map<String, String> contextData = new HashMap<>();

        if (this.openTelemetryContextProvider != null) {
            contextData.putAll(this.openTelemetryContextProvider.retrieveContextData());
        }

        if (!dynatraceMetadata.isEmpty()) {
            contextData.putAll(dynatraceMetadata);
        }
        return contextData;
    }
}

