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

package androidx.core.util;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.COMPLEX_UNIT_IN;
import static android.util.TypedValue.COMPLEX_UNIT_MM;
import static android.util.TypedValue.COMPLEX_UNIT_PT;
import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.util.TypedValue.COMPLEX_UNIT_SP;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Container for a dynamically typed data value.  Primarily used with
 * {@link android.content.res.Resources} for holding resource values.
 *
 * <p>Used to convert between dimension values like DP and SP to pixels, and vice versa.
 */
public class TypedValueCompat {
    /** The unit of a {@link TypedValue#TYPE_DIMENSION} */
    @IntDef(value = {
            COMPLEX_UNIT_PX,
            COMPLEX_UNIT_DIP,
            COMPLEX_UNIT_SP,
            COMPLEX_UNIT_PT,
            COMPLEX_UNIT_IN,
            COMPLEX_UNIT_MM,
    })
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    public @interface ComplexDimensionUnit {}

    private static final float INCHES_PER_PT = (1.0f / 72);
    private static final float INCHES_PER_MM = (1.0f / 25.4f);

    private TypedValueCompat() {}

    /**
     * Return the complex unit type for the given complex dimension. For example, a dimen type
     * with value 12sp will return {@link TypedValue#COMPLEX_UNIT_SP}.
     *
     * @param complexDimension the dimension, typically {@link TypedValue#data}
     * @return The complex unit type
     */
    @ComplexDimensionUnit
    @SuppressLint("WrongConstant")
    public static int getUnitFromComplexDimension(int complexDimension) {
        return TypedValue.COMPLEX_UNIT_MASK & (complexDimension >> TypedValue.COMPLEX_UNIT_SHIFT);
    }

    /**
     * Converts a pixel value to the given dimension, e.g. PX to DP.
     *
     * <p>This is the inverse of {@link TypedValue#applyDimension(int, float, DisplayMetrics)}
     *
     * @param unitToConvertTo The unit to convert to.
     * @param pixelValue The raw pixels value to convert from.
     * @param metrics Current display metrics to use in the conversion --
     *                supplies display density and scaling information.
     *
     * @return A dimension value equivalent to the given number of pixels
     * @throws IllegalArgumentException if unitToConvertTo is not valid.
     */
    public static float deriveDimension(
            @ComplexDimensionUnit int unitToConvertTo,
            float pixelValue,
            @NonNull DisplayMetrics metrics) {
        if (Build.VERSION.SDK_INT >= 34) {
            return Api34Impl.deriveDimension(unitToConvertTo, pixelValue, metrics);
        }

        switch (unitToConvertTo) {
            case COMPLEX_UNIT_PX:
                return pixelValue;
            case COMPLEX_UNIT_DIP: {
                // Avoid divide-by-zero, and return 0 since that's what the inverse function will do
                if (metrics.density == 0) {
                    return 0;
                }
                return pixelValue / metrics.density;
            }
            case COMPLEX_UNIT_SP:
                // Versions earlier than U don't get the fancy non-linear scaling
                if (metrics.scaledDensity == 0) {
                    return 0;
                }
                return pixelValue / metrics.scaledDensity;
            case COMPLEX_UNIT_PT: {
                if (metrics.xdpi == 0) {
                    return 0;
                }
                return pixelValue / metrics.xdpi / INCHES_PER_PT;
            }
            case COMPLEX_UNIT_IN: {
                if (metrics.xdpi == 0) {
                    return 0;
                }
                return pixelValue / metrics.xdpi;
            }
            case COMPLEX_UNIT_MM: {
                if (metrics.xdpi == 0) {
                    return 0;
                }
                return pixelValue / metrics.xdpi / INCHES_PER_MM;
            }
            default:
                throw new IllegalArgumentException("Invalid unitToConvertTo " + unitToConvertTo);
        }
    }

    /**
     * Converts a density-independent pixels (DP) value to pixels
     *
     * <p>This is a convenience function for
     * {@link TypedValue#applyDimension(int, float, DisplayMetrics)}
     *
     * @param dpValue The value in DP to convert from.
     * @param metrics Current display metrics to use in the conversion --
     *                supplies display density and scaling information.
     *
     * @return A raw pixel value
     */
    public static float dpToPx(float dpValue, @NonNull DisplayMetrics metrics) {
        return TypedValue.applyDimension(COMPLEX_UNIT_DIP, dpValue, metrics);
    }

    /**
     * Converts a pixel value to density-independent pixels (DP)
     *
     * <p>This is a convenience function for {@link #deriveDimension(int, float, DisplayMetrics)}
     *
     * @param pixelValue The raw pixels value to convert from.
     * @param metrics Current display metrics to use in the conversion --
     *                supplies display density and scaling information.
     *
     * @return A dimension value (in DP) representing the given number of pixels.
     */
    public static float pxToDp(float pixelValue, @NonNull DisplayMetrics metrics) {
        return deriveDimension(COMPLEX_UNIT_DIP, pixelValue, metrics);
    }

    /**
     * Converts a scaled pixels (SP) value to pixels
     *
     * <p>This is a convenience function for
     * {@link TypedValue#applyDimension(int, float, DisplayMetrics)}
     *
     * @param spValue The value in SP to convert from.
     * @param metrics Current display metrics to use in the conversion --
     *                supplies display density and scaling information.
     *
     * @return A raw pixel value
     */
    public static float spToPx(float spValue, @NonNull DisplayMetrics metrics) {
        return TypedValue.applyDimension(COMPLEX_UNIT_SP, spValue, metrics);
    }

    /**
     * Converts a pixel value to scaled pixels (SP)
     *
     * <p>This is a convenience function for {@link #deriveDimension(int, float, DisplayMetrics)}
     *
     * @param pixelValue The raw pixels value to convert from.
     * @param metrics Current display metrics to use in the conversion --
     *                supplies display density and scaling information.
     *
     * @return A dimension value (in SP) representing the given number of pixels.
     */
    public static float pxToSp(float pixelValue, @NonNull DisplayMetrics metrics) {
        return deriveDimension(COMPLEX_UNIT_SP, pixelValue, metrics);
    }

    @RequiresApi(34)
    private static class Api34Impl {
        @DoNotInline
        public static float deriveDimension(
                @ComplexDimensionUnit int unitToConvertTo,
                float pixelValue,
                DisplayMetrics metrics
        ) {
            return TypedValue.deriveDimension(unitToConvertTo, pixelValue, metrics);
        }
    }
}
