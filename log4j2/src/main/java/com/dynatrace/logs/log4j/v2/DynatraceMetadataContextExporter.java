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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.apache.logging.log4j.core.util.ContextDataProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DynatraceMetadataContextExporter implements ContextDataProvider {
    private final Map<String, String> dynatraceMetadata;
    private final boolean provideOpenTelemetryMetadata;

    public static final String TRACE_ID = "trace_id";
    public static final String SPAN_ID = "span_id";
    public static final String TRACE_FLAGS = "trace_flags";

    // export all metadata by default
    public DynatraceMetadataContextExporter() {
        // TODO: figure out a way to configure what metadata should be exported. Maybe a .properties file?
        this(true, DynatraceMetadataEnricherWrapper.getDynatraceMetadata());
    }

    // VisibleForTesting
    DynatraceMetadataContextExporter(boolean provideOpenTelemetryMetadata, Map<String, String> dynatraceMetadata) {
        this.provideOpenTelemetryMetadata = provideOpenTelemetryMetadata;
        this.dynatraceMetadata = dynatraceMetadata;
    }

    @Override
    public Map<String, String> supplyContextData() {
        Map<String, String> contextData = new HashMap<>();
        if (this.provideOpenTelemetryMetadata) {
            contextData.putAll(getSpanContextData());
        }
        if (!dynatraceMetadata.isEmpty()) {
            contextData.putAll(dynatraceMetadata);
        }
        return contextData;
    }

    private Map<String, String> getSpanContextData() {
        SpanContext spanContext = Span.current().getSpanContext();
        if (!spanContext.isValid()) {
            return Collections.emptyMap();
        }

        Map<String, String> contextData = new HashMap<>();
        contextData.put(TRACE_ID, spanContext.getTraceId());
        contextData.put(SPAN_ID, spanContext.getSpanId());
        contextData.put(TRACE_FLAGS, spanContext.getTraceFlags().asHex());

        return contextData;
    }
}

