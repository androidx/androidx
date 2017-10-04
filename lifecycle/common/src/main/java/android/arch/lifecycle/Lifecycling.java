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
public class Lifecycling {
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
            Constructor<? extends GenericLifecycleObserver> constructor = getObserverConstuctor(
                    klass);
            return constructor.newInstance(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private static Constructor<? extends GenericLifecycleObserver> getObserverConstuctor(
            Class<?> klass) {
        Constructor<? extends GenericLifecycleObserver> constructor = sCallbackCache.get(klass);
        if (constructor == null) {
            constructor = resolveObserverConstructor(klass);
            sCallbackCache.put(klass, constructor);
        }
        return constructor;
    }

    @Nullable
    private static Constructor<? extends GenericLifecycleObserver> generatedConstructor(
            Class<?> klass) {
        try {
            Package aPackage = klass.getPackage();
            String name = klass.getCanonicalName();
            final String fullPackage = aPackage != null ? aPackage.getName() : "";
            final String adapterName = getAdapterName(fullPackage.isEmpty() ? name :
                    name.substring(fullPackage.length() + 1));

            @SuppressWarnings("unchecked") final Class<? extends GenericLifecycleObserver> aClass =
                    (Class<? extends GenericLifecycleObserver>) Class.forName(
                            fullPackage.isEmpty() ? adapterName : fullPackage + "." + adapterName);
            Constructor<? extends GenericLifecycleObserver> constructor =
                    aClass.getDeclaredConstructor(klass);
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
            return constructor;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoSuchMethodException e) {
            // this should not happen
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private static Constructor<? extends GenericLifecycleObserver> resolveObserverConstructor(
            Class<?> klass) {

        // anonymous class bug:35073837
        if (klass.getCanonicalName() == null) {
            return sREFLECTIVE;
        }

        Constructor<? extends GenericLifecycleObserver> constructor = generatedConstructor(klass);
        if (constructor != null) {
            return constructor;
        }

        boolean hasLifecycleMethods = ClassesInfoCache.sInstance.hasLifecycleMethods(klass);
        if (hasLifecycleMethods) {
            sCallbackCache.put(klass, sREFLECTIVE);
            return sREFLECTIVE;
        }

        int counter = 0;
        Class<?> lifecycleParent = null;
        for (Class<?> intrface : klass.getInterfaces()) {
            if (isLifecycleParent(intrface)) {
                counter++;
                lifecycleParent = intrface;
            }
        }

        Class<?> superclass = klass.getSuperclass();
        if (isLifecycleParent(superclass)) {
            counter++;
            lifecycleParent = superclass;
        }

        if (counter == 1) {
            return getObserverConstuctor(lifecycleParent);
        }
        return sREFLECTIVE;
    }

    private static boolean isLifecycleParent(Class<?> klass) {
        return klass != null && LifecycleObserver.class.isAssignableFrom(klass);
    }

    /**
     * Create a name for an adapter class.
     */
    public static String getAdapterName(String className) {
        return className.replace(".", "_") + "_LifecycleAdapter";
    }
}
