# Dynatrace Metadata provider for Log4j

This utility allows for adding and formatting Dynatrace and OpenTelemetry metadata to Log4j output.
It relies on implementing the `ContextDataProvider` interface, which is available since Log4j v2.13.2.
More information can be found on [the Log4j website](https://logging.apache.org/log4j/2.x/manual/extending.html).

## Installation

In order to add Metadata to Log4j log lines, include the following dependency:

```groovy
runtimeOnly("log4j-metadata-provider:log4j2")
```

Then, add a `log4j2.xml` configuration file to the classpath of your project (e.g. `src/main/resources`, as shown in [the example application](example)).
There, the `<PatternLayout>` tag can be used to specify the log line layout.

## Configuration

Configure your logs via the `log4j.xml` file:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
    <Appenders>
        <!-- Log to the Console: -->
        <Console name="Console" target="SYSTEM_OUT">
            <!-- Specify the pattern layout in which log lines are serialized -->
            <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} trace_id=%X{trace_id} span_id=%X{span_id} trace_flags=%X{trace_flags} - %msg%n"/>
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

### Available pattern accessors

There are multiple ways of specifying metadata in the log output pattern.

#### Add all properties

When specifying `%X` in the pattern, all available Context variables will be added in the form of `key=value` pairs inside curly braces.

For example, if Dynatrace metadata contains a property like

```properties
dt.some.property=some_prop_value
```

a `PatternLayout` like this:

```xml
<PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} %X - %msg%n"/>
```

cloud lead to a log line like this:

```text
16:54:10.581 [main] INFO  com.dynatrace.example.App {dt.some.property=some_prop_value} - Your log message would appear here.
```

#### Access individual properties

If the name of the property is known, it is also possible to access its value directly using `%X{property_name}` or with `${ctx:property_name}`.
More information on lookups can be found [here](https://logging.apache.org/log4j/2.x/manual/lookups.html).

```xml
<PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} dt.some.property=%X{dt.some.property} - %msg%n"/>
```

Would lead to a log line like this:

```text
16:57:10.449 [main] INFO  com.dynatrace.example.App dt.some.property=some_prop_value - Your log message would appear here.
```

Note, that in this example, the property name string (`dt.some.property`) is explicitly specified in front of the `%X{dt.some.property}` directive.
Otherwise, only the property value would be printed.

#### Properties available from OpenTelemetry

For OpenTelemetry, trace id, span id, and trace flags are read from the current span context.
They can be used in the pattern using the `%X{var_name}` syntax:

- Trace Id: `%X{trace_id}`
- Span Id: `%X{span_id}`
- Trace flags: `%X{trace_flags}`

If a Span context is active, and a message is logged with `%X` specified in the `<PatternLayout>`, the resulting log line could look like this (see [this section on printing all context items](#add-all-properties)):
Note that the attributes are enclosed in curly braces, separated by a comma and a space.

```text
17:14:40.584 [main] INFO  com.dynatrace.example.App {dt.some.property=some_prop_value, span_id=daf44afb5e37500a, trace_flags=01, trace_id=4446b5923aa0b22ab7da0648ae17dd33} - Your log message would appear here.
```

It is also possible to specifically name all the properties explicitly:

```xml
<PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} trace_id=%X{trace_id} span_id=%X{span_id} trace_flags=%X{trace_flags} dt.some.property=%X{dt.some.property} - %msg%n"/>
```

would serialize as:

```text
17:19:43.333 [main] INFO  com.dynatrace.example.App trace_id=9c50ab1d03d1f5bf0d44e8067c4a885a span_id=dbbc65db11f6c27e trace_flags=01 dt.some.property=some_prop_value - Your log message would appear here.
```
