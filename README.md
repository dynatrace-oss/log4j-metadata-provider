# Dynatrace Metadata provider for Log4j

This utility allows for adding and formatting Dynatrace and OpenTelemetry metadata to Log4j output.
It relies on implementing the `ContextDataProvider` interface, which is available since Log4j
v2.13.2. More information can be found
on [the Log4j website](https://logging.apache.org/log4j/2.x/manual/extending.html).

## Requirements

* Java 8 or later
* Log4j 2, version 2.13.2 or later
  * ⚠ If you use any version <= 2.14.1 pay attention to the Log4j vulnerability [CVE-2021-44228](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44228) and its mitigation, ideally update to 2.15.+
* OpenTelemetry (tested with 1.4, but we suggest using the latest 1.+ version)

## Installation

In order to add Metadata to Log4j log lines, include the following dependencies to your `build.gradle`:

```groovy
// Depend on log4j itself, this is probably already in your project.
// The mechanism that is used in this library works from v2.13.2.
// If you use any version <= 2.14.1 pay attention to the Log4j vulnerability CVE-2021-44228 and its mitigation.
implementation("org.apache.logging.log4j:log4j-api:2.15.+")
implementation("org.apache.logging.log4j:log4j-core:2.15.+")

runtimeOnly("com.dynatrace.logs.log4j2:metadata-provider:0.2.1")
```

As this library is not published on Maven Central, a source dependency is required.
This will download the required code from Github.
To set it up, add the following to your `settings.gradle`:

```groovy
sourceControl {
    gitRepository("https://github.com/dynatrace-oss/log4j-metadata-provider.git") {
        producesModule("com.dynatrace.logs.log4j2:metadata-provider")
    }
}
```

Then, add a `log4j2.xml` configuration file to the classpath of your project (
e.g. `src/main/resources`, as shown in the example applications [here](./example_with_otel/)
and [here](./example_without_otel/)). There, the log output format can be configured, for example
[as JSON](#json-based-logging) or in [a line format](#line-based-logging).

## Properties available from OpenTelemetry

For OpenTelemetry, Trace ID and Span ID are read from the currently active span. They can be used in
the pattern using the `%X{var_name}` or `$${ctx:var_name}` syntax:

- Trace Id: `%X{dt.trace_id}` / `$${ctx:dt.trace_id}`
- Span Id: `%X{dt.span_id}` / `$${ctx:dt.span_id}`

In order to get access to these properties, a dependency to the OpenTelemetry project is required:

```groovy
implementation("io.opentelemetry:opentelemetry-api:1.4.+")
implementation("io.opentelemetry:opentelemetry-sdk:1.4.+")
```

Furthermore, a Span Context has to be activated before the properties become available.
See [the example project](./example_with_otel) for more details.

## Properties available from Dynatrace OneAgent

If there is a OneAgent running on the host, Dynatrace metadata is added to the context.
Available properties are:

- Host ID: `%X{dt.entity.host}` / `$${ctx:dt.entity.host}`
- Process group instance: `%X{dt.entity.process_group_instance}` / `$${ctx:dt.entity.process_group_instance}`

See the [Dynatrace documentation](https://www.dynatrace.com/support/help/how-to-use-dynatrace/metrics/metric-ingestion/ingestion-methods/enrich-metrics/)
for more information on the underlying feature used by this library and its limitations.

## Configuration

Configure your log output via the `log4j2.xml` file.

### JSON based-logging

When exporting logs as JSON, an additional dependency to Jackson databind is required:

```groovy
runtimeOnly('com.fasterxml.jackson.core:jackson-databind:2.12.+')
```

Then, the JSON export can be configured via the `log4j2.xml` file:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Properties>
        <Property name="dt.trace_id">
            <!-- dt.trace_id default. Leave empty to only add the dt.trace_id property to the json if it exists. -->
        </Property>
        <Property name="dt.span_id">
            <!-- dt.span_id default. Leave empty to only add the dt.span_id property to the json if it exists. -->
        </Property>
        <Property name="dt.entity.process_group_instance"></Property>
        <Property name="dt.entity.host"></Property>
    </Properties>
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <JsonLayout>
                <!-- It's also possible to specify key value pairs explicitly. They will be added to the JSON object (at the top level). -->
                <KeyValuePair key="dt.trace_id" value="$${ctx:dt.trace_id}"/>
                <KeyValuePair key="dt.span_id" value="$${ctx:dt.span_id}"/>
                <KeyValuePair key="dt.entity.process_group_instance" value="$${ctx:dt.entity.process_group_instance}"/>
                <KeyValuePair key="dt.entity.host" value="$${ctx:dt.entity.host}"/>
            </JsonLayout>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="Console" level="All"/>
        </Root>
    </Loggers>
</Configuration>
```

Note that only the `$${ctx:var_name}` syntax is supported here. This configuration would lead to a
JSON like this:

```json
{
  "instant" : {
    "epochSecond" : 1629820222,
    "nanoOfSecond" : 6000000
  },
  "thread" : "main",
  "level" : "INFO",
  "loggerName" : "com.dynatrace.example.AppWithOpenTelemetry",
  "message" : "Inside the outer scope, after calling method",
  "endOfBatch" : false,
  "loggerFqcn" : "org.apache.logging.log4j.spi.AbstractLogger",
  "threadId" : 1,
  "threadPriority" : 5,
  "dt.trace_id" : "a5abda71c8de2e36df499cc12f8b2e8d",
  "dt.span_id" : "87aa73b5514f4963",
  "dt.entity.process_group_instance" : "PROCESS_GROUP_INSTANCE-27204EFED3D8466E",
  "dt.entity.host" : "HOST-A0FE2A03244B9728"
}
```

If properties cannot be resolved from the context, it might look like this (with
default properties set to the empty string, as configured above):

```json
{
  "instant": {
    "epochSecond": 1629124120,
    "nanoOfSecond": 111349755
  },
  "thread": "main",
  "level": "INFO",
  "loggerName": "com.dynatrace.example.AppWithOpenTelemetry",
  "message": "Outside the scope",
  "endOfBatch": false,
  "loggerFqcn": "org.apache.logging.log4j.spi.AbstractLogger",
  "threadId": 1,
  "threadPriority": 5
}
```

#### Details

The `<Properties>` section sets up defaults for `dt.trace_id`, `dt.span_id`, as well as the Dynatrace metadata. It is possible to set a
default by adding the respective string in the `<Property>` tag. Otherwise, when using the lookup
notation (e.g., `$${ctx:dt.span_id}`), if the property does not exist, the exported JSON will contain
the line: `"dt.span_id": "${ctx:dt.span_id}"`, without the replaced values. When using the Properties
section as shown above, the default is set to an empty string and if the looked up property is not in the context, it will be omitted in the
exported JSON. An example of missing `dt.trace_id` and `dt.span_id` with no default might look like this:

```json
{
  ...
  "threadPriority": 5,
  "dt.trace_id": "${ctx:dt.trace_id}",
  "dt.span_id": "${ctx:dt.span_id}"
}
```

#### Export all properties in the context

It is also possible to export all properties available in the context by using:

```xml

<JsonLayout properties="true">
    <!-- Optional KeyValue pairs specified explicitly, but can also be left empty. -->
</JsonLayout>
```

All available context values will be exported as a map of key value pairs named `"contextMap"`:

```json
{
  "instant" : {
    "epochSecond" : 1629820418,
    "nanoOfSecond" : 385000000
  },
  "thread" : "main",
  "level" : "INFO",
  "loggerName" : "com.dynatrace.example.AppWithOpenTelemetry",
  "message" : "Inside the outer scope, after calling method",
  "endOfBatch" : false,
  "loggerFqcn" : "org.apache.logging.log4j.spi.AbstractLogger",
  "contextMap" : {
    "dt.entity.host" : "HOST-A0FE2A03244B9728",
    "dt.entity.process_group_instance" : "PROCESS_GROUP_INSTANCE-27204EFED3D8466E",
    "dt.trace_id" : "cf0d4ed5b7242c8644cb463e736a756a",
    "dt.span_id" : "9b7a04e5f7b6d348"
  },
  "threadId" : 1,
  "threadPriority" : 5
}
```

### Line-based logging

Alternatively, it is possible to log the data using a line-based format, which can similarly be
configured using the `log4j2.xml` file.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <!-- Log to the Console: -->
        <Console name="Console" target="SYSTEM_OUT">
            <!-- Specify the pattern layout in which log lines are serialized -->
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} dt.trace_id=%X{dt.trace_id} dt.span_id=%X{dt.span_id} dt.entity.process_group_instance=%X{dt.entity.process_group_instance} dt.entity.host=%X{dt.entity.host} - %msg%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <!-- set the minimum log level for the console exporter to info. -->
        <Root level="info">
            <AppenderRef ref="Console" level="All"/>
        </Root>
    </Loggers>
</Configuration>
```

This would lead to a log line like this:

```text
17:55:59.598 [main] INFO  com.dynatrace.example.AppWithOpenTelemetry dt.trace_id=507172d8c54c56b62905f750af3acf19 dt.span_id=7552f126d64a099d dt.entity.process_group_instance=PROCESS_GROUP_INSTANCE-27204EFED3D8466E dt.entity.host=HOST-A0FE2A03244B9728 - Inside the outer scope, after calling method
```

#### Add all properties

When specifying `%X` in the pattern, all available Context variables will be added in the form
of `key=value` pairs inside curly braces.

For example, a `PatternLayout` like this:

```xml

<PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} %X - %msg%n"/>
```

could lead to a log line like this if the logger is called while an OpenTelemetry trace context is
active:

```text
17:57:01.415 [main] INFO  com.dynatrace.example.AppWithOpenTelemetry {dt.entity.host=HOST-A0FE2A03244B9728, dt.entity.process_group_instance=PROCESS_GROUP_INSTANCE-27204EFED3D8466E, dt.span_id=ab39518a744bb7b7, dt.trace_id=e9e9b2a543a60abba7585e5f0f1ad5ae} - Inside the outer scope, after calling method
```

#### Access individual properties

If the name of the property is known, it is also possible to access its value directly
using `%X{property_name}` or with `$${ctx:property_name}`. More information on lookups can be found
in the [Log4j documentation](https://logging.apache.org/log4j/2.x/manual/lookups.html).

```xml

<PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} dt.trace_id=%X{dt.trace_id} - %msg%n"/>
```

Would lead to a log line like this:

```text
16:46:02.324 [main] INFO  com.dynatrace.example.AppWithOpenTelemetry dt.trace_id=eac920d37a4b60cc93a4ed86448ab66f - Inside the outer scope, after calling method
```

Note, that in this example, the property name string (`dt.trace_id`) is explicitly specified in front
of the `%X{dt.trace_id}` directive. Otherwise, only the property value would be printed.

## Examples

> The examples use two different logging frameworks: The log4j context provider uses
`java.util.logging`, as self-referencing `log4j` can lead to difficulties configuring as well as
endless loops upon setting up. The OpenTelemetry example uses a special kind of trace exporter,
which also relies on `java.util.logging`. This exporter is used, since for the purpose of this
example, there is no backend required. Therefore, the span information might be printed multiple
times, but formatted differently. If using only the log4j output, these additional logs can be
ignored.

### Example with OpenTelemetry

This [example](./example_with_otel) demonstrates how both Dynatrace and OpenTelemetry metadata are added to the logs.
Check out the `log4j2.xml` configuration file to see how the properties are configured.
Dynatrace metadata is only added when a OneAgent is running on the host.

### Example without OpenTelemetry

This [example](./example_without_otel) demonstrates how the logs are still enriched with Dynatrace metadata,
even when OpenTelemetry is not installed in the application.
Dynatrace metadata is only added when a OneAgent is running on the host.
