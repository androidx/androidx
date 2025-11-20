/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.graphics;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.GuardedBy;
import androidx.annotation.RestrictTo;
import androidx.collection.LongSparseArray;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helper for creating {@link Typeface}s with exact weight on API 21-25.
 */
@SuppressLint("SoonBlockedPrivateApi")
@RestrictTo(LIBRARY)
final class WeightTypefaceApi21 {
    private static final String TAG = "WeightTypeface";

    private static final String NATIVE_INSTANCE_FIELD = "native_instance";
    private static final String NATIVE_CREATE_FROM_TYPEFACE_METHOD = "nativeCreateFromTypeface";
    private static final String NATIVE_CREATE_WEIGHT_ALIAS_METHOD = "nativeCreateWeightAlias";

    private static final Field sNativeInstance;
    private static final Method sNativeCreateFromTypeface;
    private static final Method sNativeCreateWeightAlias;
    private static final Constructor<Typeface> sConstructor;

    static {
        Field nativeInstance;
        Method nativeCreateFromTypeface;
        Method nativeCreateWeightAlias;
        Constructor<Typeface> constructor;
        try {
            nativeInstance = Typeface.class.getDeclaredField(NATIVE_INSTANCE_FIELD);
            nativeCreateFromTypeface = Typeface.class
                    .getDeclaredMethod(NATIVE_CREATE_FROM_TYPEFACE_METHOD, long.class, int.class);
            nativeCreateFromTypeface.setAccessible(true);
            nativeCreateWeightAlias = Typeface.class
                    .getDeclaredMethod(NATIVE_CREATE_WEIGHT_ALIAS_METHOD, long.class, int.class);
            nativeCreateWeightAlias.setAccessible(true);
            constructor = Typeface.class.getDeclaredConstructor(long.class);
            constructor.setAccessible(true);
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            Log.e(TAG, e.getClass().getName(), e);
            nativeInstance = null;
            nativeCreateFromTypeface = null;
            nativeCreateWeightAlias = null;
            constructor = null;
        }
        sNativeInstance = nativeInstance;
        sNativeCreateFromTypeface = nativeCreateFromTypeface;
        sNativeCreateWeightAlias = nativeCreateWeightAlias;
        sConstructor = constructor;
    }

    /**
     * Returns true if all the necessary methods were found.
     */
    private static boolean isPrivateApiAvailable() {
        return sNativeInstance != null;
    }

    /**
     * Cache for Typeface objects for weight variant. Currently max size is 3.
     */
    @GuardedBy("sWeightCacheLock")
    private static final LongSparseArray<SparseArray<Typeface>> sWeightTypefaceCache =
            new LongSparseArray<>(3);
    private static final Object sWeightCacheLock = new Object();

    /**
     * @return Valid typeface, or {@code null} if private API is not available
     */
    static @Nullable Typeface createWeightStyle(@NonNull Typeface base, int weight,
            boolean italic) {
        if (!isPrivateApiAvailable()) {
            return null;
        }

        final int key = (weight << 1) | (italic ? 1 : 0);

        Typeface typeface;
        synchronized (sWeightCacheLock) {
            final long baseNativeInstance = getNativeInstance(base);
            SparseArray<Typeface> innerCache = sWeightTypefaceCache.get(baseNativeInstance);
            if (innerCache == null) {
                innerCache = new SparseArray<>(4);
                sWeightTypefaceCache.put(baseNativeInstance, innerCache);
            } else {
                typeface = innerCache.get(key);
                if (typeface != null) {
                    return typeface;
                }
            }

            if (italic == base.isItalic()) {
                typeface = create(
                        nativeCreateWeightAlias(baseNativeInstance, weight));
            } else {
                typeface = create(
                        nativeCreateFromTypefaceWithExactStyle(baseNativeInstance, weight, italic));
            }
            innerCache.put(key, typeface);
        }
        return typeface;
    }

    private static long getNativeInstance(@NonNull Typeface typeface) {
        try {
            return sNativeInstance.getLong(typeface);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("BanUncheckedReflection")
    @SuppressWarnings("ConstantConditions")
    private static long nativeCreateFromTypefaceWithExactStyle(long nativeInstance, int weight,
            boolean italic) {
        try {
            // First create typeface with correct normal/italic style, then adjust weight.
            // Don't use public API, bypass style cache. We'll cache the weight instance instead.
            final int style = italic ? Typeface.ITALIC : Typeface.NORMAL;
            nativeInstance = (long) sNativeCreateFromTypeface.invoke(null, nativeInstance, style);
            return (long) sNativeCreateWeightAlias.invoke(null, nativeInstance, weight);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("BanUncheckedReflection")
    @SuppressWarnings("ConstantConditions")
    private static long nativeCreateWeightAlias(long nativeInstance, int weight) {
        try {
            return (long) sNativeCreateWeightAlias.invoke(null, nativeInstance, weight);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static @Nullable Typeface create(long nativeInstance) {
        try {
            return sConstructor.newInstance(nativeInstance);
        } catch (IllegalAccessException e) {
            return null;
        } catch (InstantiationException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
    }

    private WeightTypefaceApi21() {
        // No instances.
    }
}
