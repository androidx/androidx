/*
 * Copyright (C) 2016 The Android Open Source Project
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
 *
 */

package com.android.support.lifecycle;

import android.support.annotation.Nullable;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal class to handle lifecycle conversion etc.
 */
class Lifecycling {
    private static Constructor<? extends GenericLifecycleObserver> REFLECTIVE;

    static {
        try {
            REFLECTIVE = ReflectiveGenericLifecycleObserver.class.getConstructor(Object.class);
        } catch (NoSuchMethodException e) {

        }
    }

    private static Map<Class, Constructor<? extends GenericLifecycleObserver>> sCallbackCache =
            new HashMap<>();

    static GenericLifecycleObserver getCallback(Object object) {
        if (object instanceof GenericLifecycleObserver) {
            return (GenericLifecycleObserver) object;
        }
        try {
            final Class<?> klass = object.getClass();
            Constructor<? extends GenericLifecycleObserver> cachedConstructor = sCallbackCache.get(
                    klass);
            if (cachedConstructor != null) {
                return cachedConstructor.newInstance(object);
            }
            cachedConstructor = getGeneratedAdapterConstructor(klass);
            if (cachedConstructor != null) {
                sCallbackCache.put(klass, cachedConstructor);
                if (!cachedConstructor.isAccessible()) {
                    cachedConstructor.setAccessible(true);
                }
                return cachedConstructor.newInstance(object);
            } else {
                sCallbackCache.put(klass, REFLECTIVE);
            }
            return new ReflectiveGenericLifecycleObserver(object);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

    }

    @Nullable
    private static Constructor<? extends GenericLifecycleObserver> getGeneratedAdapterConstructor(
            Class<?> klass) {
        final String fullPackage = klass.getPackage().getName();
        final String adapterName = getAdapterName(klass.getSimpleName());
        try {
            @SuppressWarnings("unchecked")
            final Class<? extends GenericLifecycleObserver> aClass =
                    (Class<? extends GenericLifecycleObserver>) Class.forName(
                            fullPackage + "." + adapterName);
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

    static String getAdapterName(String callbackName) {
        return callbackName.replace(".", "_") + "_LifecycleAdapter";
    }
}
