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

package com.dynatrace.metric.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DynatraceMetadataEnricherWrapper {

    private DynatraceMetadataEnricherWrapper() {
    }
    
    public static Map<String, String> getDynatraceMetadata() {
        Map<String, String> dimensions = new HashMap<>();
        try {
            for (Dimension dim : DynatraceMetadataEnricher.getDynatraceMetadata()) {
                dimensions.put(dim.getKey(), dim.getValue());
            }
        } catch (Exception e) {
            return Collections.emptyMap();
        }

        return dimensions;
    }
}