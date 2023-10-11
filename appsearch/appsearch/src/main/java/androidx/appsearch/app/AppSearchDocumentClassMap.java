/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// @exportToFramework:skipFile()
package androidx.appsearch.app;

import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * A class that maintains the map from schema type names to the fully qualified names of the
 * corresponding document classes.
 *
 * <p>Do not extend this class directly. AppSearch's annotation processor will automatically
 * generate necessary subclasses to hold the map.
 */
@AnyThread
public abstract class AppSearchDocumentClassMap {

    /**
     * The cached value of {@link #getMergedMap()}.
     */
    private static volatile Map<String, List<String>> sMergedMap = null;

    /**
     * Collects all of the instances of the generated {@link AppSearchDocumentClassMap} classes
     * available in the current JVM environment, and calls the {@link #getMap()} method from them to
     * build, cache and return the merged map. The keys are schema type names, and the values are
     * the lists of the corresponding document classes.
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static Map<String, List<String>> getMergedMap() {
        if (sMergedMap == null) {
            synchronized (AppSearchDocumentClassMap.class) {
                if (sMergedMap == null) {
                    sMergedMap = buildMergedMapLocked();
                }
            }
        }
        return sMergedMap;
    }

    /**
     * Returns the map from schema type names to the list of the fully qualified names of the
     * corresponding document classes.
     */
    @NonNull
    protected abstract Map<String, List<String>> getMap();

    @NonNull
    @GuardedBy("AppSearchDocumentClassMap.class")
    private static Map<String, List<String>> buildMergedMapLocked() {
        ServiceLoader<AppSearchDocumentClassMap> loader = ServiceLoader.load(
                AppSearchDocumentClassMap.class);
        Map<String, List<String>> result = new ArrayMap<>();
        for (AppSearchDocumentClassMap appSearchDocumentClassMap : loader) {
            Map<String, List<String>> documentClassMap = appSearchDocumentClassMap.getMap();
            for (Map.Entry<String, List<String>> entry : documentClassMap.entrySet()) {
                String schemaName = entry.getKey();
                // A single schema name can be mapped to more than one document classes because
                // document classes can choose to have arbitrary schema names. The most common
                // case is when there are multiple AppSearch packages that define the same schema
                // name. It is necessary to keep track all of the mapped document classes to prevent
                // from losing any information.
                List<String> documentClassNames = result.get(schemaName);
                if (documentClassNames == null) {
                    documentClassNames = new ArrayList<>();
                    result.put(schemaName, documentClassNames);
                }
                documentClassNames.addAll(entry.getValue());
            }
        }

        for (String schemaName : result.keySet()) {
            result.put(schemaName,
                    Collections.unmodifiableList(Objects.requireNonNull(result.get(schemaName))));
        }
        return Collections.unmodifiableMap(result);
    }
}
