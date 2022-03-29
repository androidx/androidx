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

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.LongSparseArray;
import androidx.core.content.res.FontResourcesParserCompat;

import java.lang.reflect.Field;

/**
 * Helper for creating {@link Typeface}s with exact weight on API 14-20.
 * May be used on newer platforms as a fallback method in case private API isn't available.
 * @hide
 */
@RestrictTo(LIBRARY)
final class WeightTypefaceApi14 {
    private static final String TAG = "WeightTypeface";

    private static final String NATIVE_INSTANCE_FIELD = "native_instance";

    private static final Field sNativeInstance;

    static {
        Field nativeInstance;
        try {
            nativeInstance = Typeface.class.getDeclaredField(NATIVE_INSTANCE_FIELD);
            nativeInstance.setAccessible(true); // package-private until API 21
        } catch (Exception e) {
            Log.e(TAG, e.getClass().getName(), e);
            nativeInstance = null;
        }
        sNativeInstance = nativeInstance;
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
    static Typeface createWeightStyle(@NonNull TypefaceCompatBaseImpl compat,
            @NonNull Context context, @NonNull Typeface base, int weight, boolean italic) {
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

            typeface = getBestFontFromFamily(compat, context, base, weight, italic);
            if (typeface == null) {
                typeface = platformTypefaceCreate(base, weight, italic);
            }
            innerCache.put(key, typeface);
        }
        return typeface;
    }

    private static Typeface platformTypefaceCreate(Typeface base, int weight, boolean italic) {
        boolean isBold = weight >= 600;
        int style = 0;
        if (!isBold && !italic) {
            // !bold && !italic
            style = Typeface.NORMAL;
        } else if (!isBold) {
            // !bold && italic
            style = Typeface.ITALIC;
        } else if (!italic) {
            // bold && !italic
            style = Typeface.BOLD;
        } else {
            // bold && italic
            style = Typeface.BOLD_ITALIC;
        }
        return Typeface.create(base, style);
    }

    /**
     * @see {@code TypefaceCompat#getBestFontFromFamily(Context, Typeface, int)}
     */
    @Nullable
    private static Typeface getBestFontFromFamily(@NonNull TypefaceCompatBaseImpl compat,
            @NonNull Context context, @NonNull Typeface base, int weight, boolean italic) {
        final FontResourcesParserCompat.FontFamilyFilesResourceEntry family =
                compat.getFontFamily(base);
        if (family == null) {
            return null; // Base wasn't loaded using TypefaceCompat.
        }

        return compat.createFromFontFamilyFilesResourceEntry(context, family,
                context.getResources(), weight, italic);
    }

    private static long getNativeInstance(@NonNull Typeface typeface) {
        try {
            final Number num = (Number) sNativeInstance.get(typeface); // int until API 21
            return num.longValue();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private WeightTypefaceApi14() {
        // No instances.
    }
}
