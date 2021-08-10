package com.dynatrace.logs.log4j.v2;

import java.util.Map;

public interface IContextDataProvider {
    Map<String, String> provideContextData();
}
