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
 * A registry which maintains instances of {@link DocumentClassFactory}.
 * @exportToFramework:hide
 */
@AnyThread
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DocumentClassFactoryRegistry {
    private static final String GEN_CLASS_PREFIX = "$$__AppSearch__";

    private static volatile DocumentClassFactoryRegistry sInstance = null;

    private final Map<Class<?>, DocumentClassFactory<?>> mFactories = new HashMap<>();

    private DocumentClassFactoryRegistry() {}

    /** Returns the singleton instance of {@link DocumentClassFactoryRegistry}. */
    @NonNull
    public static DocumentClassFactoryRegistry getInstance() {
        if (sInstance == null) {
            synchronized (DocumentClassFactoryRegistry.class) {
                if (sInstance == null) {
                    sInstance = new DocumentClassFactoryRegistry();
                }
            }
        }
        return sInstance;
    }

    /**
     * Gets the {@link DocumentClassFactory} instance that can convert to and from objects of type
     * {@code T}.
     *
     * @throws AppSearchException if no factory for this document class could be found on the
     * classpath
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> DocumentClassFactory<T> getOrCreateFactory(@NonNull Class<T> documentClass)
            throws AppSearchException {
        Preconditions.checkNotNull(documentClass);
        DocumentClassFactory<?> factory;
        synchronized (this) {
            factory = mFactories.get(documentClass);
        }
        if (factory == null) {
            factory = loadFactoryByReflection(documentClass);
            synchronized (this) {
                DocumentClassFactory<?> racingFactory = mFactories.get(documentClass);
                if (racingFactory == null) {
                    mFactories.put(documentClass, factory);
                } else {
                    // Another thread beat us to it
                    factory = racingFactory;
                }
            }
        }
        return (DocumentClassFactory<T>) factory;
    }

    /**
     * Gets the {@link DocumentClassFactory} instance that can convert to and from objects of type
     * {@code T}.
     *
     * @throws AppSearchException if no factory for this document class could be found on the
     * classpath
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> DocumentClassFactory<T> getOrCreateFactory(@NonNull T documentClass)
            throws AppSearchException {
        Preconditions.checkNotNull(documentClass);
        Class<?> clazz = documentClass.getClass();
        DocumentClassFactory<?> factory = getOrCreateFactory(clazz);
        return (DocumentClassFactory<T>) factory;
    }

    private DocumentClassFactory<?> loadFactoryByReflection(@NonNull Class<?> documentClass)
            throws AppSearchException {
        Package pkg = documentClass.getPackage();
        String simpleName = documentClass.getCanonicalName();
        if (simpleName == null) {
            throw new AppSearchException(
                    AppSearchResult.RESULT_INTERNAL_ERROR,
                    "Failed to find simple name for document class \"" + documentClass
                            + "\". Perhaps it is anonymous?");
        }

        // Creates factory class name under the package.
        // For a class Foo annotated with @Document, we will generated a
        // $$__AppSearch__Foo.class under the package.
        // For an inner class Foo.Bar annotated with @Document, we will generated a
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
            // If the current class or interface has only one parent interface/class, then try to
            // look at the unique parent.
            Class<?> superClass = documentClass.getSuperclass();
            Class<?>[] superInterfaces = documentClass.getInterfaces();
            if (superClass == Object.class) {
                superClass = null;
            }
            int numParent = superInterfaces.length;
            if (superClass != null) {
                numParent += 1;
            }

            if (numParent == 1) {
                if (superClass != null) {
                    return loadFactoryByReflection(superClass);
                } else {
                    return loadFactoryByReflection(superInterfaces[0]);
                }
            }

            String errorMessage = "Failed to find document class converter \"" + factoryClassName
                    + "\". Perhaps the annotation processor was not run or the class was "
                    + "proguarded out?";
            if (numParent > 1) {
                errorMessage += " Or, this class may not have been annotated with @Document, and "
                        + "there is an ambiguity to determine a unique @Document annotated parent "
                        + "class/interface.";
            }

            throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR, errorMessage, e);
        }
        Object instance;
        try {
            instance = factoryClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new AppSearchException(
                    AppSearchResult.RESULT_INTERNAL_ERROR,
                    "Failed to construct document class converter \"" + factoryClassName + "\"",
                    e);
        }
        return (DocumentClassFactory<?>) instance;
    }
}
