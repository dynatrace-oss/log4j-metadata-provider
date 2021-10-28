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

import java.util.HashMap;
import java.util.Map;

/**
 * This wrapper class is used to access package-local functionality of the Dynatrace metadata
 * utilities libraries, and is intended for internal use only. It is used to access Dynatrace
 * metadata through the Dynatrace metrics util library.
 */
public class DynatraceMetadataEnricherWrapper {

    private DynatraceMetadataEnricherWrapper() {
    }

    /**
     * Get all Dynatrace metadata dimensions as a map of key-value pairs.
     * @return a map containing all found Dynatrace metadata dimensions.
     */
    public static Map<String, String> getDynatraceMetadata() {
        Map<String, String> dimensions = new HashMap<>();
        for (Dimension dim : DynatraceMetadataEnricher.getDynatraceMetadata()) {
            dimensions.put(dim.getKey(), dim.getValue());
        }
        return dimensions;
    }
}
