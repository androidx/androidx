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

package android.support.v4.view.animation;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.Build;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/**
 * Helper for creating path-based {@link Interpolator} instances. On API 21 or newer, the
 * platform {@link PathInterpolator} will be used and on older platforms a compatible alternative
 * implementation will be used.
 */
public class PathInterpolatorCompat {

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
    public static Interpolator create(Path path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new PathInterpolator(path);
        }
        return new PathInterpolatorCompatIMPL(path);
    }

    /**
     * Create an {@link Interpolator} for a quadratic Bezier curve. The end points
     * {@code (0, 0)} and {@code (1, 1)} are assumed.
     *
     * @param controlX the x coordinate of the quadratic Bezier control point
     * @param controlY the y coordinate of the quadratic Bezier control point
     * @return the {@link Interpolator} representing the quadratic Bezier curve
     */
    public static Interpolator create(float controlX, float controlY) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new PathInterpolator(controlX, controlY);
        }
        return new PathInterpolatorCompatIMPL(createQuad(controlX, controlY));
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
    public static Interpolator create(float controlX1, float controlY1,
            float controlX2, float controlY2) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new PathInterpolator(controlX1, controlY1, controlX2, controlY2);
        }
        return new PathInterpolatorCompatIMPL(
                createCubic(controlX1, controlY1, controlX2, controlY2));
    }

    /**
     * Create a {@link Path} representing a quadratic Bezier curve. The end points
     * {@code (0, 0)} and {@code (1, 1)} are assumed.
     *
     * @param controlX the x coordinate of the quadratic Bezier control point
     * @param controlY the y coordinate of the quadratic Bezier control point
     * @return the {@link Path} representing the quadratic Bezier curve
     */
    private static Path createQuad(float controlX, float controlY) {
        final Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.quadTo(controlX, controlY, 1.0f, 1.0f);
        return path;
    }

    /**
     * Create a {@link Path} representing a cubic Bezier curve. The end points
     * {@code (0, 0)} and {@code (1, 1)} are assumed.
     *
     * @param controlX1 the x coordinate of the first control point of the cubic Bezier
     * @param controlY1 the y coordinate of the first control point of the cubic Bezier
     * @param controlX2 the x coordinate of the second control point of the cubic Bezier
     * @param controlY2 the y coordinate of the second control point of the cubic Bezier
     * @return the {@link Path} representing the quadratic Bezier curve
     */
    private static Path createCubic(float controlX1, float controlY1,
            float controlX2, float controlY2) {
        final Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.cubicTo(controlX1, controlY1, controlX2, controlY2, 1.0f, 1.0f);
        return path;
    }

    private static class PathInterpolatorCompatIMPL implements Interpolator {

        /**
         * Governs the accuracy of the approximation of the {@link Path}.
         */
        private static final float PRECISION = 0.002f;

        private final float[] mX;
        private final float[] mY;

        public PathInterpolatorCompatIMPL(Path path) {
            final PathMeasure pathMeasure = new PathMeasure(path, false /* forceClosed */);

            final float pathLength = pathMeasure.getLength();
            final int numPoints = (int) (pathLength / PRECISION) + 1;

            mX = new float[numPoints];
            mY = new float[numPoints];

            final float[] position = new float[2];
            for (int i = 0; i < numPoints; ++i) {
                final float distance = (i * pathLength) / (numPoints - 1);
                pathMeasure.getPosTan(distance, position, null /* tangent */);

                mX[i] = position[0];
                mY[i] = position[1];
            }
        }

        @Override
        public float getInterpolation(float t) {
            if (t <= 0.0f) {
                return 0.0f;
            } else if (t >= 1.0f) {
                return 1.0f;
            }

            // Do a binary search for the correct x to interpolate between.
            int startIndex = 0;
            int endIndex = mX.length - 1;
            while (endIndex - startIndex > 1) {
                int midIndex = (startIndex + endIndex) / 2;
                if (t < mX[midIndex]) {
                    endIndex = midIndex;
                } else {
                    startIndex = midIndex;
                }
            }

            final float xRange = mX[endIndex] - mX[startIndex];
            if (xRange == 0) {
                return mY[startIndex];
            }

            final float tInRange = t - mX[startIndex];
            final float fraction = tInRange / xRange;

            final float startY = mY[startIndex];
            final float endY = mY[endIndex];

            return startY + (fraction * (endY - startY));
        }
    }
}
