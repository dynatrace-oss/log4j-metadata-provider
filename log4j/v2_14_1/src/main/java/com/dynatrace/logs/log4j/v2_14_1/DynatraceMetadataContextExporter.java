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

package com.dynatrace.logs.log4j.v2_14_1;

import com.dynatrace.metric.util.DynatraceMetadataEnricherWrapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.apache.logging.log4j.core.util.ContextDataProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DynatraceMetadataContextExporter implements ContextDataProvider {
    private final boolean provideOpenTelemetryMetadata;
    private final boolean provideDynatraceMetadata;

    public static final String TRACE_ID = "trace_id";
    public static final String SPAN_ID = "span_id";
    public static final String TRACE_FLAGS = "trace_flags";

    // export all metadata by default
    public DynatraceMetadataContextExporter() {
        this(true, true);
    }

    // allow to turn off opentelemetry metadata explicitly, Dynatrace metadata is still exported.
    public DynatraceMetadataContextExporter(boolean provideOpenTelemetryMetadata) {
        this(provideOpenTelemetryMetadata, true);
    }

    // explicitly set both kinds of metadata.
    public DynatraceMetadataContextExporter(boolean provideOpenTelemetryMetadata, boolean provideDynatraceMetadata) {
        this.provideOpenTelemetryMetadata = provideOpenTelemetryMetadata;
        this.provideDynatraceMetadata = provideDynatraceMetadata;
    }

    @Override
    public Map<String, String> supplyContextData() {
        Map<String, String> contextData = new HashMap<>();

        if (this.provideOpenTelemetryMetadata) {
            contextData.putAll(getSpanContextData());
        }

        if (this.provideDynatraceMetadata) {
            contextData.putAll(DynatraceMetadataEnricherWrapper.getDynatraceMetadata());
        }

        return contextData;
    }

    private Map<String, String> getSpanContextData() {
        Span currentSpan = Span.current();
        if (!currentSpan.getSpanContext().isValid()) {
            return Collections.emptyMap();
        }

        Map<String, String> contextData = new HashMap<>();
        SpanContext spanContext = currentSpan.getSpanContext();
        contextData.put(TRACE_ID, spanContext.getTraceId());
        contextData.put(SPAN_ID, spanContext.getSpanId());
        contextData.put(TRACE_FLAGS, spanContext.getTraceFlags().asHex());
        return contextData;
    }

}

