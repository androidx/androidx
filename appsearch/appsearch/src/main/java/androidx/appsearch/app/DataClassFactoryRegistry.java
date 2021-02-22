/*
 * Copyright 2020 The Android Open Source Project
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
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.core.util.Preconditions;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry which maintains instances of {@link androidx.appsearch.app.DataClassFactory}.
 * @hide
 */
@AnyThread
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DataClassFactoryRegistry {
    private static final String GEN_CLASS_PREFIX = "$$__AppSearch__";

    private static volatile DataClassFactoryRegistry sInstance = null;

    private final Map<Class<?>, DataClassFactory<?>> mFactories = new HashMap<>();

    private DataClassFactoryRegistry() {}

    /** Returns the singleton instance of {@link DataClassFactoryRegistry}. */
    @NonNull
    public static DataClassFactoryRegistry getInstance() {
        if (sInstance == null) {
            synchronized (DataClassFactoryRegistry.class) {
                if (sInstance == null) {
                    sInstance = new DataClassFactoryRegistry();
                }
            }
        }
        return sInstance;
    }

    /**
     * Gets the {@link DataClassFactory} instance that can convert to and from objects of type
     * {@code T}.
     *
     * @throws AppSearchException if no factory for this data class could be found on the classpath
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> DataClassFactory<T> getOrCreateFactory(@NonNull Class<T> dataClass)
            throws AppSearchException {
        Preconditions.checkNotNull(dataClass);
        DataClassFactory<?> factory;
        synchronized (this) {
            factory = mFactories.get(dataClass);
        }
        if (factory == null) {
            factory = loadFactoryByReflection(dataClass);
            synchronized (this) {
                DataClassFactory<?> racingFactory = mFactories.get(dataClass);
                if (racingFactory == null) {
                    mFactories.put(dataClass, factory);
                } else {
                    // Another thread beat us to it
                    factory = racingFactory;
                }
            }
        }
        return (DataClassFactory<T>) factory;
    }

    /**
     * Gets the {@link DataClassFactory} instance that can convert to and from objects of type
     * {@code T}.
     *
     * @throws AppSearchException if no factory for this data class could be found on the classpath
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> DataClassFactory<T> getOrCreateFactory(@NonNull T dataClass)
            throws AppSearchException {
        Preconditions.checkNotNull(dataClass);
        Class<?> clazz = dataClass.getClass();
        DataClassFactory<?> factory = getOrCreateFactory(clazz);
        return (DataClassFactory<T>) factory;
    }

    private DataClassFactory<?> loadFactoryByReflection(@NonNull Class<?> dataClass)
            throws AppSearchException {
        Package pkg = dataClass.getPackage();
        String simpleName = dataClass.getCanonicalName();
        if (simpleName == null) {
            throw new AppSearchException(
                    AppSearchResult.RESULT_INTERNAL_ERROR,
                    "Failed to find simple name for data class \"" + dataClass
                            + "\". Perhaps it is anonymous?");
        }

        // Creates factory class name under the package.
        // For a class Foo annotated with @AppSearchDocument, we will generated a
        // $$__AppSearch__Foo.class under the package.
        // For an inner class Foo.Bar annotated with @AppSearchDocument, we will generated a
        // $$__AppSearch__Foo$$__Bar.class under the package.
        String packageName = "";
        if (pkg != null) {
            packageName = pkg.getName() + ".";
            simpleName = simpleName.substring(packageName.length()).replace(".", "$$__");
        }
        String factoryClassName = packageName + GEN_CLASS_PREFIX + simpleName;

        Class<?> factoryClass;
        try {
            factoryClass = Class.forName(factoryClassName);
        } catch (ClassNotFoundException e) {
            throw new AppSearchException(
                    AppSearchResult.RESULT_INTERNAL_ERROR,
                    "Failed to find data class converter \"" + factoryClassName
                            + "\". Perhaps the annotation processor was not run or the class was "
                            + "proguarded out?",
                    e);
        }
        Object instance;
        try {
            instance = factoryClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new AppSearchException(
                    AppSearchResult.RESULT_INTERNAL_ERROR,
                    "Failed to construct data class converter \"" + factoryClassName + "\"",
                    e);
        }
        return (DataClassFactory<?>) instance;
    }
}
