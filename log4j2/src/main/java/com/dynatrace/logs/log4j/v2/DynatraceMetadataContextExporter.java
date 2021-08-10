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


public class DynatraceMetadataContextExporter implements ContextDataProvider {
    private final Map<String, String> dynatraceMetadata;
    private final IContextDataProvider openTelemetryContextProvider;

    // using a log4j logger here can lead to an endless loop, since
    private static final Logger logger = Logger.getLogger(DynatraceMetadataContextExporter.class.getName());


    // export all metadata by default
    public DynatraceMetadataContextExporter() {
        this(DynatraceMetadataEnricherWrapper.getDynatraceMetadata());
    }

    // VisibleForTesting
    DynatraceMetadataContextExporter(Map<String, String> dynatraceMetadata) {
        this.dynatraceMetadata = dynatraceMetadata;
        this.openTelemetryContextProvider = tryLoadOpenTelemetryTraceSupport();
    }

    private IContextDataProvider tryLoadOpenTelemetryTraceSupport() {
        try {
            logger.info("trying to ");
            Class<?> clazz = Class.forName("com.dynatrace.logs.log4j.v2.OpenTelemetrySpanContextDataProvider");
            return (IContextDataProvider) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                InstantiationException | InvocationTargetException e) {
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

