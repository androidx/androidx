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

import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.appsearch.annotation.Document;
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
 */
@AnyThread
public abstract class AppSearchDocumentClassMap {

    private static final String TAG = "AppSearchDocumentClassM";
    private static final Object sLock = new Object();

    /**
     * The cached value of {@link #getGlobalMap()}.
     */
    private static volatile Map<String, List<String>> sGlobalMap = null;

    /**
     * The cached value of {@code Class.forName(className)} for AppSearch document classes.
     */
    private static volatile Map<String, Class<?>> sCachedAppSearchClasses = new ArrayMap<>();

    /**
     * Returns the global map that includes all AppSearch document classes annotated with
     * {@link Document} that are available in the current runtime. It maps from AppSearch's type
     * name specified by {@link Document#name()} to the list of the fully qualified names of the
     * corresponding document classes. The values are lists because it is possible that two
     * document classes are associated with the same AppSearch type name.
     *
     * <p>Note that although this method, under normal circumstances, executes quickly, it
     * performs a synchronous disk read operation in order to build the map, which means it can
     * potentially introduce I/O blocking if executed on the main thread.
     *
     * <p>Since every call to this method should return the same map, the value of this map will
     * be internally cached, so that only the first call will perform disk I/O.
     */
    @NonNull
    @WorkerThread
    public static Map<String, List<String>> getGlobalMap() {
        if (sGlobalMap == null) {
            synchronized (sLock) {
                if (sGlobalMap == null) {
                    sGlobalMap = buildGlobalMapLocked();
                }
            }
        }
        return sGlobalMap;
    }

    /**
     * Looks up the provided map to find a class for {@code schemaName} that is assignable to
     * {@code documentClass}. Returns null if such class is not found.
     */
    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static <T> Class<? extends T> getAssignableClassBySchemaName(
            @NonNull Map<String, List<String>> map, @NonNull String schemaName,
            @NonNull Class<T> documentClass) {
        List<String> classNames = map.get(schemaName);
        if (classNames == null) {
            return null;
        }
        // If there are multiple classes that correspond to the schema name, then we will:
        // 1. skip any classes that are not assignable to documentClass.
        // 2. if there are still multiple candidates, return the first one in the global map.
        for (int i = 0; i < classNames.size(); ++i) {
            String className = classNames.get(i);
            try {
                Class<?> clazz = getAppSearchDocumentClass(className);
                if (documentClass.isAssignableFrom(clazz)) {
                    return clazz.asSubclass(documentClass);
                }
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "Failed to load document class \"" + className + "\". Perhaps the "
                        + "class was proguarded out?");
            }
        }
        return null;
    }

    /**
     * Returns the map from schema type names to the list of the fully qualified names of the
     * corresponding document classes.
     */
    @NonNull
    protected abstract Map<String, List<String>> getMap();

    @NonNull
    private static Class<?> getAppSearchDocumentClass(@NonNull String className)
            throws ClassNotFoundException {
        Class<?> result;
        synchronized (sLock) {
            result = sCachedAppSearchClasses.get(className);
        }
        if (result == null) {
            result = Class.forName(className);
            synchronized (sLock) {
                sCachedAppSearchClasses.put(className, result);
            }
        }
        return result;
    }

    /**
     * Collects all of the instances of the generated {@link AppSearchDocumentClassMap} classes
     * available in the current JVM environment, and calls the {@link #getMap()} method from them to
     * build and return the merged map. The keys are schema type names, and the values are the
     * lists of the corresponding document classes.
     */
    @NonNull
    @GuardedBy("AppSearchDocumentClassMap.sLock")
    private static Map<String, List<String>> buildGlobalMapLocked() {
        ServiceLoader<AppSearchDocumentClassMap> loader = ServiceLoader.load(
                AppSearchDocumentClassMap.class, AppSearchDocumentClassMap.class.getClassLoader());
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
