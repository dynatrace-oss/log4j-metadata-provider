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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class OpenTelemetrySpanContextDataRetriever implements ContextDataRetriever {
    public static final String TRACE_ID = "trace.id";
    public static final String SPAN_ID = "span.id";

    public OpenTelemetrySpanContextDataRetriever() {
        // this is required, so instantiating this class throws if OpenTelemetry is not installed.
        final Span ignored = Span.getInvalid();
    }

    /**
     * Reads trace id and span id from the current span context.
     * @return a map containing trace.id and span.id if a span context is active, or an empty map
     * otherwise.
     */
    @Override
    public Map<String, String> retrieveContextData() {
        SpanContext spanContext = Span.current().getSpanContext();
        if (!spanContext.isValid()) {
            return Collections.emptyMap();
        }

        Map<String, String> contextData = new HashMap<>();
        contextData.put(TRACE_ID, spanContext.getTraceId());
        contextData.put(SPAN_ID, spanContext.getSpanId());

        return contextData;
    }
}
