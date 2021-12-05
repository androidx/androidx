/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.view.animation;

import android.graphics.Path;
import android.os.Build;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Helper for creating path-based {@link Interpolator} instances. On API 21 or newer, the
 * platform implementation will be used and on older platforms a compatible alternative
 * implementation will be used.
 */
public final class PathInterpolatorCompat {

    private PathInterpolatorCompat() {
        // prevent instantiation
    }

    /**
     * Create an {@link Interpolator} for an arbitrary {@link Path}. The {@link Path}
     * must begin at {@code (0, 0)} and end at {@code (1, 1)}. The x-coordinate along the
     * {@link Path} is the input value and the output is the y coordinate of the line at that
     * point. This means that the Path must conform to a function {@code y = f(x)}.
     * <p/>
     * The {@link Path} must not have gaps in the x direction and must not
     * loop back on itself such that there can be two points sharing the same x coordinate.
     *
     * @param path the {@link Path} to use to make the line representing the {@link Interpolator}
     * @return the {@link Interpolator} representing the {@link Path}
     */
    @NonNull
    public static Interpolator create(@NonNull Path path) {
        if (Build.VERSION.SDK_INT >= 21) {
            return Api21Impl.createPathInterpolator(path);
        }
        return new PathInterpolatorApi14(path);
    }

    /**
     * Create an {@link Interpolator} for a quadratic Bezier curve. The end points
     * {@code (0, 0)} and {@code (1, 1)} are assumed.
     *
     * @param controlX the x coordinate of the quadratic Bezier control point
     * @param controlY the y coordinate of the quadratic Bezier control point
     * @return the {@link Interpolator} representing the quadratic Bezier curve
     */
    @NonNull
    public static Interpolator create(float controlX, float controlY) {
        if (Build.VERSION.SDK_INT >= 21) {
            return Api21Impl.createPathInterpolator(controlX, controlY);
        }
        return new PathInterpolatorApi14(controlX, controlY);
    }

    /**
     * Create an {@link Interpolator} for a cubic Bezier curve.  The end points
     * {@code (0, 0)} and {@code (1, 1)} are assumed.
     *
     * @param controlX1 the x coordinate of the first control point of the cubic Bezier
     * @param controlY1 the y coordinate of the first control point of the cubic Bezier
     * @param controlX2 the x coordinate of the second control point of the cubic Bezier
     * @param controlY2 the y coordinate of the second control point of the cubic Bezier
     * @return the {@link Interpolator} representing the cubic Bezier curve
     */
    @NonNull
    public static Interpolator create(float controlX1, float controlY1,
            float controlX2, float controlY2) {
        if (Build.VERSION.SDK_INT >= 21) {
            return Api21Impl.createPathInterpolator(controlX1, controlY1, controlX2, controlY2);
        }
        return new PathInterpolatorApi14(controlX1, controlY1, controlX2, controlY2);
    }

    @RequiresApi(21)
    static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static PathInterpolator createPathInterpolator(Path path) {
            return new PathInterpolator(path);
        }

        @DoNotInline
        static PathInterpolator createPathInterpolator(float controlX, float controlY) {
            return new PathInterpolator(controlX, controlY);
        }

        @DoNotInline
        static PathInterpolator createPathInterpolator(float controlX1, float controlY1,
                float controlX2, float controlY2) {
            return new PathInterpolator(controlX1, controlY1, controlX2, controlY2);
        }
    }
}
