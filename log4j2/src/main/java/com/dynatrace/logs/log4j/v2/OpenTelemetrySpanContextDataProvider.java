package com.dynatrace.logs.log4j.v2;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class OpenTelemetrySpanContextDataProvider implements IContextDataProvider {
    public static final String TRACE_ID = "trace_id";
    public static final String SPAN_ID = "span_id";
    public static final String TRACE_FLAGS = "trace_flags";


    public OpenTelemetrySpanContextDataProvider() {
        // this is required, so instantiating this class throws if OpenTelemetry is not installed.
        final Span ignored = Span.getInvalid();
    }

    @Override
    public Map<String, String> provideContextData() {
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
