/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.lifecycle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal class to handle lifecycle conversion etc.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class Lifecycling {
    private static Constructor<? extends GenericLifecycleObserver> sREFLECTIVE;

    static {
        try {
            sREFLECTIVE = ReflectiveGenericLifecycleObserver.class
                    .getDeclaredConstructor(Object.class);
        } catch (NoSuchMethodException ignored) {

        }
    }

    private static Map<Class, Constructor<? extends GenericLifecycleObserver>> sCallbackCache =
            new HashMap<>();

    @NonNull
    static GenericLifecycleObserver getCallback(Object object) {
        if (object instanceof FullLifecycleObserver) {
            return new FullLifecycleObserverAdapter((FullLifecycleObserver) object);
        }

        if (object instanceof GenericLifecycleObserver) {
            return (GenericLifecycleObserver) object;
        }
        //noinspection TryWithIdenticalCatches
        try {
            final Class<?> klass = object.getClass();
            Constructor<? extends GenericLifecycleObserver> cachedConstructor = sCallbackCache.get(
                    klass);
            if (cachedConstructor != null) {
                return cachedConstructor.newInstance(object);
            }
            cachedConstructor = getGeneratedAdapterConstructor(klass);
            if (cachedConstructor != null) {
                if (!cachedConstructor.isAccessible()) {
                    cachedConstructor.setAccessible(true);
                }
            } else {
                cachedConstructor = sREFLECTIVE;
            }
            sCallbackCache.put(klass, cachedConstructor);
            return cachedConstructor.newInstance(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private static Constructor<? extends GenericLifecycleObserver> getGeneratedAdapterConstructor(
            Class<?> klass) {
        Package aPackage = klass.getPackage();
        final String fullPackage = aPackage != null ? aPackage.getName() : "";

        String name = klass.getCanonicalName();
        // anonymous class bug:35073837
        if (name == null) {
            return null;
        }
        final String adapterName = getAdapterName(fullPackage.isEmpty() ? name :
                name.substring(fullPackage.length() + 1));
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends GenericLifecycleObserver> aClass =
                    (Class<? extends GenericLifecycleObserver>) Class.forName(
                            fullPackage.isEmpty() ? adapterName : fullPackage + "." + adapterName);
            return aClass.getDeclaredConstructor(klass);
        } catch (ClassNotFoundException e) {
            final Class<?> superclass = klass.getSuperclass();
            if (superclass != null) {
                return getGeneratedAdapterConstructor(superclass);
            }
        } catch (NoSuchMethodException e) {
            // this should not happen
            throw new RuntimeException(e);
        }
        return null;
    }

    static String getAdapterName(String className) {
        return className.replace(".", "_") + "_LifecycleAdapter";
    }
}
