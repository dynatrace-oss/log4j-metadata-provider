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

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DynatraceMetadataContextDataProviderTest {
    private static Tracer tracer;
    private static final InMemorySpanExporter inMemorySpanExporter = InMemorySpanExporter.create();
    private static final ContextDataRetriever openTelemetrySupport =
            new OpenTelemetrySpanContextDataRetriever();

    @BeforeAll
    static void setupTests() {
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(inMemorySpanExporter))
                .build();

        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();

        tracer = openTelemetrySdk.getTracer("test-tracer");
    }

    @BeforeEach
    void setup() {
        inMemorySpanExporter.reset();
    }

    @Test
    void testContextEmptyWhenUsingNoSpan() {
        DynatraceMetadataContextDataProvider exporter = new DynatraceMetadataContextDataProvider(
                openTelemetrySupport, Collections.emptyMap());
        assertThat(exporter.supplyContextData()).isEmpty();
    }

    @Test
    void testContextContainsDynatraceMetadata() {
        Map<String, String> dynatraceMetadata = new HashMap<>();
        dynatraceMetadata.put("dt.some.meta", "some_value");
        dynatraceMetadata.put("dt.some.other.meta", "some_other_value");
        DynatraceMetadataContextDataProvider exporter = new DynatraceMetadataContextDataProvider(
                openTelemetrySupport, dynatraceMetadata);
        assertThat(exporter.supplyContextData()).containsExactlyInAnyOrderEntriesOf(dynatraceMetadata);
    }

    @Test
    void testContextIsAddedWhenUsingSpan() {
        DynatraceMetadataContextDataProvider exporter = new DynatraceMetadataContextDataProvider(
                openTelemetrySupport, Collections.emptyMap());
        Map<String, String> contextData = Collections.emptyMap();

        Span span = tracer.spanBuilder("span1").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            contextData = exporter.supplyContextData();
        } finally {
            span.end();
        }

        List<SpanData> finishedSpanItems = inMemorySpanExporter.getFinishedSpanItems();
        assertThat(finishedSpanItems).hasSize(1);

        Map<String, String> expected = new HashMap<>();
        expected.put("dt.span_id", finishedSpanItems.get(0).getSpanId());
        expected.put("dt.trace_id", finishedSpanItems.get(0).getTraceId());

        assertThat(contextData).containsExactlyInAnyOrderEntriesOf(expected);
    }

    @Test
    void testContextIsNotUsedWhenUsingSpanButNoScope() {
        DynatraceMetadataContextDataProvider exporter = new DynatraceMetadataContextDataProvider(
                openTelemetrySupport, Collections.emptyMap());
        Map<String, String> contextData = Collections.emptyMap();

        Span span = tracer.spanBuilder("span1").startSpan();
        // in this case, the current span context is not set, therefore, context data is empty.
        contextData = exporter.supplyContextData();
        span.end();

        // the span is still valid, however.
        List<SpanData> finishedSpanItems = inMemorySpanExporter.getFinishedSpanItems();
        assertThat(finishedSpanItems).hasSize(1);

        assertThat(contextData).isEmpty();
    }

    @Test
    void testContextContainsAllWhenUsingDynatraceAndOpenTelemetryMetadata() {
        Map<String, String> dynatraceMetadata = new HashMap<>();
        dynatraceMetadata.put("dt.some.meta", "some_value");
        dynatraceMetadata.put("dt.some.other.meta", "some_other_value");

        DynatraceMetadataContextDataProvider exporter = new DynatraceMetadataContextDataProvider(
                openTelemetrySupport, dynatraceMetadata);
        Map<String, String> contextData = Collections.emptyMap();

        Span span = tracer.spanBuilder("span1").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            contextData = exporter.supplyContextData();
        } finally {
            span.end();
        }

        List<SpanData> finishedSpanItems = inMemorySpanExporter.getFinishedSpanItems();
        assertThat(finishedSpanItems).hasSize(1);

        Map<String, String> expected = new HashMap<>();
        expected.put("dt.span_id", finishedSpanItems.get(0).getSpanId());
        expected.put("dt.trace_id", finishedSpanItems.get(0).getTraceId());
        expected.putAll(dynatraceMetadata);

        assertThat(contextData).containsExactlyInAnyOrderEntriesOf(expected);
    }

    @Test
    void testDynatraceMetadataWouldOverwriteOpenTelemetryIfTheySharedKeys() {
        Map<String, String> dynatraceMetadata = new HashMap<>();
        dynatraceMetadata.put("dt.trace_id", "some_value");
        dynatraceMetadata.put("dt.some.meta", "some_other_value");

        DynatraceMetadataContextDataProvider exporter = new DynatraceMetadataContextDataProvider(openTelemetrySupport, dynatraceMetadata);
        Map<String, String> contextData = Collections.emptyMap();

        Span span = tracer.spanBuilder("span1").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            contextData = exporter.supplyContextData();
        } finally {
            span.end();
        }

        List<SpanData> finishedSpanItems = inMemorySpanExporter.getFinishedSpanItems();
        assertThat(finishedSpanItems).hasSize(1);

        Map<String, String> expected = new HashMap<>();
        expected.put("dt.span_id", finishedSpanItems.get(0).getSpanId());
        expected.putAll(dynatraceMetadata);

        assertThat(contextData).containsExactlyInAnyOrderEntriesOf(expected);
        // This is already asserted by the above, but makes it more clear to humans.
        assertThat(contextData).containsEntry("dt.trace_id", "some_value");
    }
}
