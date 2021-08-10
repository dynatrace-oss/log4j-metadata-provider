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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AppWithOpenTelemetry {
    private static final Logger logger = LogManager.getLogger();
    private static Tracer tracer;

    public static Tracer getTracer() {
        if (tracer == null) {
            tracer = GlobalOpenTelemetry.getTracer("log4j-context-provider-demo", "0.0.1");
        }
        return tracer;
    }

    public static void main(String[] args) {
        logger.info("test");
        // use the LoggingSpanExporter provided here in order to log spans (uses java.util.logging).
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(new LoggingSpanExporter()).build())
                .build();

        // Set up the global OpenTelemetry instance.
        OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();

        // Acquire a Tracer:
        Span span = getTracer()
                .spanBuilder("span1")
                .setAttribute("span_att", "att_value")
                .startSpan();

        // Set the span as the current scope, otherwise Span context information cannot be retrieved
        // by the log4j exporter.
        try (Scope ignored = span.makeCurrent()) {
            logger.info("Inside the outer scope, before calling method");
            methodGettingParentSpanFromCurrentContext();
            methodGettingParentSpan(Span.current());
            logger.info("Inside the outer scope, after calling method");
        }

        logger.info("Outside the scope");
        span.end();

        try {
            // wait for a few seconds for the LoggingSpanExporter to export traces.
            // This will simply export the traces as log lines
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void methodGettingParentSpanFromCurrentContext() {
        Span span = getTracer().spanBuilder("methodGettingParentSpanFromCurrentContext").startSpan();
        try (Scope ignored = span.makeCurrent()) {
            logger.info("Inside methodGettingParentSpanFromCurrentContext");
        } finally {
            span.end();
        }
    }

    public static void methodGettingParentSpan(Span parentSpan) {
        Span span = getTracer().spanBuilder("methodGettingParentSpan").setParent(Context.current().with(parentSpan)).startSpan();
        try (Scope ignored = span.makeCurrent()) {
            logger.info("Inside methodGettingParentSpan");
        } finally {
            span.end();
        }
    }
}
