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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.LongSparseArray;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helper for creating {@link Typeface}s with exact weight on API 26-27.
 * @hide
 */
@SuppressLint("SoonBlockedPrivateApi")
@RestrictTo(LIBRARY)
@RequiresApi(26)
final class WeightTypefaceApi26 {
    private static final String TAG = "WeightTypeface";

    private static final String NATIVE_INSTANCE_FIELD = "native_instance";
    private static final String NATIVE_CREATE_FROM_TYPEFACE_WITH_EXACT_STYLE_METHOD =
            "nativeCreateFromTypefaceWithExactStyle";

    private static final Field sNativeInstance;
    private static final Method sNativeCreateFromTypefaceWithExactStyle;
    private static final Constructor<Typeface> sConstructor;

    static {
        Field nativeInstance;
        Method nativeCreateFromTypefaceWithExactStyle;
        Constructor<Typeface> constructor;
        try {
            nativeInstance = Typeface.class.getDeclaredField(NATIVE_INSTANCE_FIELD);
            nativeCreateFromTypefaceWithExactStyle = Typeface.class
                    .getDeclaredMethod(NATIVE_CREATE_FROM_TYPEFACE_WITH_EXACT_STYLE_METHOD,
                            long.class, int.class, boolean.class);
            nativeCreateFromTypefaceWithExactStyle.setAccessible(true);
            constructor = Typeface.class.getDeclaredConstructor(long.class);
            constructor.setAccessible(true);
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            Log.e(TAG, e.getClass().getName(), e);
            nativeInstance = null;
            nativeCreateFromTypefaceWithExactStyle = null;
            constructor = null;
        }
        sNativeInstance = nativeInstance;
        sNativeCreateFromTypefaceWithExactStyle = nativeCreateFromTypefaceWithExactStyle;
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
    @Nullable
    static Typeface createWeightStyle(@NonNull Typeface base, int weight, boolean italic) {
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

            typeface = create(
                    nativeCreateFromTypefaceWithExactStyle(baseNativeInstance, weight, italic));
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
            return (long) sNativeCreateFromTypefaceWithExactStyle.invoke(null, nativeInstance,
                    weight, italic);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private static Typeface create(long nativeInstance) {
        try {
            return sConstructor.newInstance(nativeInstance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private WeightTypefaceApi26() {
        // No instances.
    }
}
