/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.view;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Matrix;
import android.graphics.Path;
import android.view.DisplayShape;
import android.view.Surface;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.PathParser;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Compatibility class for representing the physical shape of a display.
 *
 * <p>This class can be used to obtain a {@link Path} representing the display's shape,
 * which can be useful for drawing or layout purposes, especially on devices with
 * non-rectangular displays (e.g., circular or displays with rounded corners).</p>
 *
 * <p>Instances of {@link DisplayShapeCompat} can be created in a few ways:
 * <ul>
 *     <li>Using {@link #create(String, float, int, int)} to parse an SVG path data
 *     string. This is suitable for custom or complex display shapes.</li>
 *     <li>Using {@link #create(int, int, boolean, int, int, int, int)} to define a common
 *     shape, such as a circle or a rectangle with specified corner radii.</li>
 * </ul>
 * The {@link Path} representing the shape can be retrieved using {@link #getPath()}.</p>
 */
public final class DisplayShapeCompat {
    private static final String TAG = "DisplayShapeCompat";

    private final Impl mImpl;

    @RequiresApi(34)
    private DisplayShapeCompat(@NonNull DisplayShape platformDisplayShape) {
        mImpl = new Impl34(platformDisplayShape);
    }

    private DisplayShapeCompat(@NonNull String spec, int displayWidth, int displayHeight,
            float physicalPixelDisplaySizeRatio, int rotation, int offsetX,
            int offsetY, float scale) {
        mImpl = new ImplBase(spec, displayWidth, displayHeight,
                physicalPixelDisplaySizeRatio, rotation, offsetX, offsetY, scale);
    }

    /**
     * Converts a platform {@link android.view.DisplayShape} into a {@link DisplayShapeCompat}.
     * This method should only be called on API 34+.
     *
     * @param ds The given platform display shape.
     * @return The compatible version of the display shape, or {@code null} if the input is null.
     */
    @RequiresApi(34)
    @RestrictTo(LIBRARY_GROUP)
    static @Nullable DisplayShapeCompat toDisplayShapeCompat(@Nullable DisplayShape ds) {
        return ds == null ? null : new DisplayShapeCompat(ds);
    }

    /**
     * Converts a {@link DisplayShapeCompat} into a platform {@link android.view.DisplayShape}.
     * This method should only be called on API 34+.
     *
     * @param dsc The given compatible display shape.
     * @return The platform version of the display shape, or {@code null} if the input is null.
     */
    @RequiresApi(34)
    @RestrictTo(LIBRARY_GROUP)
    static @Nullable DisplayShape toPlatformDisplayShape(@Nullable DisplayShapeCompat dsc) {
        if (dsc == null) {
            return null;
        }
        return dsc.mImpl.getPlatformDisplayShape();
    }

    /**
     * Creates a {@link DisplayShapeCompat} from an SVG path specification string.
     *
     * @param spec                          The SVG path data string.
     * @param physicalPixelDisplaySizeRatio The ratio of physical pixels to logical pixels.
     * @param displayWidth                  The width of the display in pixels.
     * @param displayHeight                 The height of the display in pixels.
     * @return A {@link DisplayShapeCompat} instance.
     */
    @NonNull
    @RestrictTo(LIBRARY_GROUP)
    public static DisplayShapeCompat create(@NonNull String spec,
            float physicalPixelDisplaySizeRatio, int displayWidth, int displayHeight) {
        // Backported logic for all API levels.
        return new DisplayShapeCompat(spec, displayWidth, displayHeight,
                physicalPixelDisplaySizeRatio,
                Surface.ROTATION_0, 0, 0, 1f);
    }

    /**
     * Creates a {@link DisplayShapeCompat} representing a rectangle with potentially rounded
     * corners or a circular shape.
     * This is used for API levels below 34.
     *
     * @param displayWidth  The width of the display in pixels.
     * @param displayHeight The height of the display in pixels.
     * @param isCircular    Whether the display is circular
     * @param topLeftRadius       The radius of the top-left corner in pixels.
     * @param topRightRadius      The radius of the top-right corner in pixels.
     * @param bottomRightRadius   The radius of the bottom-right corner in pixels.
     * @param bottomLeftRadius    The radius of the bottom-left corner in pixels.
     * @return A {@link DisplayShapeCompat} instance.
     */
    @NonNull
    @RestrictTo(LIBRARY_GROUP)
    public static DisplayShapeCompat create(
            int displayWidth, int displayHeight, boolean isCircular,
            int topLeftRadius, int topRightRadius,
            int bottomRightRadius, int bottomLeftRadius) {
        String spec = createSpecString(
                displayWidth, displayHeight, isCircular, topLeftRadius, topRightRadius,
                bottomRightRadius, bottomLeftRadius);
        return new DisplayShapeCompat(spec, displayWidth, displayHeight, 1f,
                Surface.ROTATION_0, 0, 0, 1f);
    }

    /**
     * Generates path data string for a display shape.
     * If {@code isCircular} is true, it generates a circular shape.
     * Otherwise, it generates a rectangle with potentially rounded corners.
     */
    private static String createSpecString(int displayWidth, int displayHeight, boolean isCircular,
            int topLeftRadius, int topRightRadius, int bottomRightRadius, int bottomLeftRadius) {

        if (isCircular) {
            final int xRadius = displayWidth / 2;
            final int yRadius = displayHeight / 2;
            return "M0," + yRadius
                    + " A" + xRadius + "," + yRadius + " 0 1,1 " + displayWidth + "," + yRadius
                    + " A" + xRadius + "," + yRadius + " 0 1,1 0," + yRadius + " Z";
        }

        final StringBuilder spec = new StringBuilder();

        final int maxRadius = Math.min(displayWidth / 2, displayHeight / 2);

        final int rTL = Math.min(maxRadius, topLeftRadius);
        final int rTR = Math.min(maxRadius, topRightRadius);
        final int rBR = Math.min(maxRadius, bottomRightRadius);
        final int rBL = Math.min(maxRadius, bottomLeftRadius);

        spec.append("M ").append(rTL).append(",0");

        spec.append(" L ").append(displayWidth - rTR).append(",0");
        if (rTR > 0) {
            spec.append(" A ").append(rTR).append(",").append(rTR).append(" 0 0,1 ").append(
                    displayWidth).append(",").append(rTR);
        }

        spec.append(" L ").append(displayWidth).append(",").append(displayHeight - rBR);
        if (rBR > 0) {
            spec.append(" A ").append(rBR).append(",").append(rBR).append(" 0 0,1 ").append(
                    displayWidth - rBR).append(",").append(displayHeight);
        }

        spec.append(" L ").append(rBL).append(",").append(displayHeight);
        if (rBL > 0) {
            spec.append(" A ").append(rBL).append(",").append(rBL).append(" 0 0,1 ").append(
                    0).append(",").append(
                    displayHeight - rBL);
        }

        if (rTL > 0) {
            spec.append(" L ").append(0).append(",").append(rTL);
            spec.append(" A ").append(rTL).append(",").append(rTL).append(" 0 0,1 ").append(
                    rTL).append(",").append(0);
        }

        spec.append(" Z");
        return spec.toString();
    }

    /**
     * Returns a {@link Path} of the display shape. Returns an empty {@link Path}
     * if the shape information is not available or if parsing fails.
     *
     * @throws IllegalArgumentException If the display spec string cannot be parsed.
     *
     */
    @NonNull
    public Path getPath() {
        return mImpl.getPath();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof DisplayShapeCompat)) return false;
        DisplayShapeCompat that = (DisplayShapeCompat) o;
        return Objects.equals(mImpl, that.mImpl);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mImpl);
    }

    @NonNull
    @Override
    public String toString() {
        return mImpl.toString();
    }

    /**
     * An empty {@link DisplayShapeCompat} instance. This instance will always result
     * in an empty {@link Path} from {@link #getPath()}.
     */
    @RestrictTo(LIBRARY_GROUP)
    static final @NonNull DisplayShapeCompat EMPTY =
            new DisplayShapeCompat("", 0, 0, 1f, Surface.ROTATION_0, 0, 0, 1f);

    private interface Impl {
        @NonNull
        Path getPath();

        @Nullable
        @RequiresApi(34)
        DisplayShape getPlatformDisplayShape();
    }

    private static class ImplBase implements Impl {
        @Nullable
        private final String mDisplayShapeSpec;
        private final float mPhysicalPixelDisplaySizeRatio;
        private final int mDisplayWidth;
        private final int mDisplayHeight;
        private final int mRotation;
        private final int mOffsetX;
        private final int mOffsetY;
        private final float mScale;

        @Nullable
        private Path mCachedPath;

        ImplBase(@NonNull String spec, int displayWidth, int displayHeight,
                float physicalPixelDisplaySizeRatio, int rotation, int offsetX, int offsetY,
                float scale) {
            mDisplayShapeSpec = spec;
            mDisplayWidth = displayWidth;
            mDisplayHeight = displayHeight;
            mPhysicalPixelDisplaySizeRatio = physicalPixelDisplaySizeRatio;
            mRotation = rotation;
            mOffsetX = offsetX;
            mOffsetY = offsetY;
            mScale = scale;
        }

        @NonNull
        @Override
        public Path getPath() {

            if (mCachedPath != null) {
                return mCachedPath;
            }

            if (mDisplayShapeSpec == null || mDisplayShapeSpec.isEmpty()) {
                return new Path();
            }

            Path path;

            try {
                path = PathParser.createPathFromPathData(mDisplayShapeSpec);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(
                        "Failed to parse DisplayShapeCompat path data: " + mDisplayShapeSpec, e);
            }

            if (!path.isEmpty()) {
                Matrix matrix = new Matrix();

                if (mRotation != Surface.ROTATION_0) {
                    float rotateDegrees = 0;
                    float pivotX = 0;
                    float pivotY = 0;
                    switch (mRotation) {
                        case Surface.ROTATION_90:
                            rotateDegrees = 90;
                            pivotX = mDisplayWidth;
                            pivotY = 0;
                            break;
                        case Surface.ROTATION_180:
                            rotateDegrees = 180;
                            pivotX = mDisplayWidth;
                            pivotY = mDisplayHeight;
                            break;
                        case Surface.ROTATION_270:
                            rotateDegrees = 270;
                            pivotX = 0;
                            pivotY = mDisplayHeight;
                            break;
                    }
                    matrix.preRotate(rotateDegrees, pivotX, pivotY);
                }

                if (mPhysicalPixelDisplaySizeRatio != 1f) {
                    matrix.preScale(mPhysicalPixelDisplaySizeRatio, mPhysicalPixelDisplaySizeRatio);
                }

                if (mOffsetX != 0 || mOffsetY != 0) {
                    matrix.postTranslate(mOffsetX, mOffsetY);
                }

                if (mScale != 1f) {
                    matrix.postScale(mScale, mScale);
                }
                path.transform(matrix);
            }

            mCachedPath = path;
            return path;
        }

        @Nullable
        @Override
        @RequiresApi(34)
        public DisplayShape getPlatformDisplayShape() {
            return null;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof ImplBase)) return false;
            ImplBase that = (ImplBase) o;

            return Objects.equals(mDisplayShapeSpec, that.mDisplayShapeSpec)
                    && mDisplayWidth == that.mDisplayWidth
                    && mDisplayHeight == that.mDisplayHeight
                    && mPhysicalPixelDisplaySizeRatio == that.mPhysicalPixelDisplaySizeRatio
                    && mRotation == that.mRotation
                    && mOffsetX == that.mOffsetX
                    && mOffsetY == that.mOffsetY
                    && mScale == that.mScale;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mDisplayShapeSpec, mDisplayWidth, mDisplayHeight,
                    mPhysicalPixelDisplaySizeRatio, mRotation, mOffsetX, mOffsetY, mScale);
        }

        @NonNull
        @Override
        public String toString() {
            return "DisplayShapeCompat{"
                    + " spec=" + (mDisplayShapeSpec != null ? mDisplayShapeSpec.hashCode() : "null")
                    + " displayWidth=" + mDisplayWidth
                    + " displayHeight=" + mDisplayHeight
                    + " physicalPixelDisplaySizeRatio=" + mPhysicalPixelDisplaySizeRatio
                    + " rotation=" + mRotation
                    + " offsetX=" + mOffsetX
                    + " offsetY=" + mOffsetY
                    + " scale=" + mScale + "}";
        }
    }

    @RequiresApi(34)
    private static class Impl34 implements Impl {
        @NonNull
        private final DisplayShape mPlatformDisplayShape;

        Impl34(@NonNull DisplayShape platformDisplayShape) {
            mPlatformDisplayShape = platformDisplayShape;
        }

        @NonNull
        @Override
        public Path getPath() {
            return mPlatformDisplayShape.getPath();
        }

        @Nullable
        @Override
        public DisplayShape getPlatformDisplayShape() {
            return mPlatformDisplayShape;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof Impl34)) return false;
            Impl34 that = (Impl34) o;
            return Objects.equals(mPlatformDisplayShape, that.mPlatformDisplayShape);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(mPlatformDisplayShape);
        }

        @NonNull
        @Override
        public String toString() {
            return "DisplayShapeCompat{mPlatformDisplayShape=" + mPlatformDisplayShape + '}';
        }
    }
}
