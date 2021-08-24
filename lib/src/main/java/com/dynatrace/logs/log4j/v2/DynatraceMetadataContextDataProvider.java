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

package com.dynatrace.logs.log4j.v2;

import com.dynatrace.metric.util.DynatraceMetadataEnricherWrapper;
import org.apache.logging.log4j.core.util.ContextDataProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


public class DynatraceMetadataContextDataProvider implements ContextDataProvider {
    private final Map<String, String> dynatraceMetadata;
    private final IContextDataRetriever openTelemetryContextProvider;

    // using a log4j logger here can lead to an endless loop, therefore use java.util.logging.
    private static final Logger logger = Logger.getLogger(DynatraceMetadataContextDataProvider.class.getName());

    // export all metadata by default
    public DynatraceMetadataContextDataProvider() {
        this(DynatraceMetadataContextDataProvider.tryLoadOpenTelemetryTraceSupport(),
                DynatraceMetadataEnricherWrapper.getDynatraceMetadata());
    }

    // VisibleForTesting
    DynatraceMetadataContextDataProvider(
            IContextDataRetriever openTelemetryContextProvider,
            Map<String, String> dynatraceMetadata
    ) {
        this.dynatraceMetadata = dynatraceMetadata;
        this.openTelemetryContextProvider = openTelemetryContextProvider;
    }

    private static IContextDataRetriever tryLoadOpenTelemetryTraceSupport() {
        try {
            logger.info("trying to load OpenTelemetry support...");
            Class<?> clazz = Class.forName("com.dynatrace.logs.log4j.v2.OpenTelemetrySpanContextDataRetriever");
            final IContextDataRetriever instance = (IContextDataRetriever) clazz.getDeclaredConstructor().newInstance();
            logger.info("OpenTelemetry support successfully loaded.");
            return instance;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                InstantiationException | InvocationTargetException e) {
            logger.warning("OpenTelemetry support could not be loaded. " +
                    "This is normal if no OpenTelemetry dependency is installed. " +
                    "Log Context will not be enriched with OpenTelemetry metadata.");
            return null;
        }
    }

    @Override
    public Map<String, String> supplyContextData() {
        Map<String, String> contextData = new HashMap<>();

        if (this.openTelemetryContextProvider != null) {
            contextData.putAll(this.openTelemetryContextProvider.provideContextData());
        }

        if (!dynatraceMetadata.isEmpty()) {
            contextData.putAll(dynatraceMetadata);
        }
        return contextData;
    }
}

