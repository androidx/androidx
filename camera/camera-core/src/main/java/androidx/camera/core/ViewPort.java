/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core;

import android.util.Rational;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The field of view of one or many {@link UseCase}s.
 *
 * <p> The {@link ViewPort} defines a FOV which is used by CameraX to calculate output crop rects.
 * For use cases associated with the same {@link ViewPort} in a {@link UseCaseGroup}, the output
 * crop rect will be mapped to the same camera sensor area. Usually {@link ViewPort} is
 * configured to optimize for the {@link Preview} use case, with the aspect ratio and rotation
 * set to match that of the {@link Preview} viewfinder.
 *
 * <p> For {@link ImageAnalysis} and in-memory {@link ImageCapture}, the crop rect is
 * {@link ImageProxy#getCropRect()}; for on-disk {@link ImageCapture}, the image is cropped before
 * saving; for {@link Preview}, the crop rect is {@link SurfaceRequest#getCropRect()}. Caller
 * should transform the output in a way that only the area defined by the crop rect is visible
 * to end users. Once the crop rect is applied, all the use cases will produce the same image
 * with possible different resolutions.
 */
@ExperimentalUseCaseGroup
public final class ViewPort {

    /**
     * LayoutDirection that defines the start and end of the {@link ScaleType}.
     *
     * @hide
     * @see android.util.LayoutDirection
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef({android.util.LayoutDirection.LTR, android.util.LayoutDirection.RTL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LayoutDirection {
    }

    /**
     * Scale types used to calculate the crop rect for a {@link UseCase}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef({FILL_START, FILL_CENTER, FILL_END, FIT_START, FIT_CENTER, FIT_END})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScaleType {
    }

    /**
     * Generate a crop rect that once applied, it scales the output while maintaining its aspect
     * ratio, so it fills the entire {@link ViewPort}, and align it to the start of the
     * {@link ViewPort}, which is the top left corner in a left-to-right (LTR) layout, or the top
     * right corner in a right-to-left (RTL) layout.
     * <p>
     * This may cause the output to be cropped if the output aspect ratio does not match that of
     * the {@link ViewPort}.
     */
    public static final int FILL_START = 0;

    /**
     * Generate a crop rect that once applied, it scales the output while maintaining its aspect
     * ratio, so it fills the entire {@link ViewPort} and center it.
     * <p>
     * This may cause the output to be cropped if the output aspect ratio does not match that of
     * the {@link ViewPort}.
     */
    public static final int FILL_CENTER = 1;

    /**
     * Generate a crop rect that once applied, it scales the output while maintaining its aspect
     * ratio, so it fills the entire {@link ViewPort}, and align it to the end of the
     * {@link ViewPort}, which is the bottom right corner in a left-to-right (LTR) layout, or the
     * bottom left corner in a right-to-left (RTL) layout.
     * <p>
     * This may cause the output to be cropped if the output aspect ratio does not match that of
     * the {@link ViewPort}.
     */
    public static final int FILL_END = 2;

    /**
     * Generate a crop rect that once applied, it scales the output while maintaining its aspect
     * ratio, so it is entirely contained within the {@link ViewPort}, and align it to the start
     * of the {@link ViewPort}, which is the top left corner in a left-to-right (LTR) layout, or
     * the top right corner in a right-to-left (RTL) layout.
     * <p>
     * Both dimensions of the {@link ViewPort} crop rect will be equal or less than the
     * corresponding dimensions of the output.
     */
    public static final int FIT_START = 3;

    /**
     * Generate a crop rect that once applied, it scales the output while maintaining its aspect
     * ratio, so it is entirely contained within the {@link ViewPort} and center it.
     * <p>
     * Both dimensions of the {@link ViewPort} will be equal or less than the corresponding
     * dimensions of the output.
     */
    public static final int FIT_CENTER = 4;

    /**
     * Generate a crop rect that once applied, it scales the output while maintaining its aspect
     * ratio, so it is entirely contained within the {@link ViewPort}, and align it to the end of
     * the {@link ViewPort}, which is the bottom right corner in a left-to-right (LTR) layout, or
     * the bottom left corner in a right-to-left (RTL) layout.
     * <p>
     * Both dimensions of the {@link ViewPort} will be equal or less than the corresponding
     * dimensions of the output.
     */
    public static final int FIT_END = 5;

    @ScaleType
    private int mScaleType;

    @NonNull
    private Rational mAspectRatio;

    @ImageOutputConfig.RotationValue
    private int mRotation;

    @LayoutDirection
    private int mLayoutDirection;

    ViewPort(@ScaleType int scaleType, @NonNull Rational aspectRatio,
            @ImageOutputConfig.RotationValue int rotation, @LayoutDirection int layoutDirection) {
        mScaleType = scaleType;
        mAspectRatio = aspectRatio;
        mRotation = rotation;
        mLayoutDirection = layoutDirection;
    }

    /**
     * Gets the aspect ratio of the {@link ViewPort}.
     */
    @NonNull
    public Rational getAspectRatio() {
        return mAspectRatio;
    }

    /**
     * Gets the rotation of the {@link ViewPort}.
     */
    @ImageOutputConfig.RotationValue
    public int getRotation() {
        return mRotation;
    }

    /**
     * Gets the scale type of the {@link ViewPort}.
     */
    @ScaleType
    public int getScaleType() {
        return mScaleType;
    }

    /**
     * Gets the layout direction of the {@link ViewPort}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @ScaleType
    public int getLayoutDirection() {
        return mLayoutDirection;
    }

    /**
     * Builder for {@link ViewPort}.
     */
    @ExperimentalUseCaseGroup
    public static class Builder {

        private static final int DEFAULT_LAYOUT_DIRECTION = android.util.LayoutDirection.LTR;
        @ScaleType
        private static final int DEFAULT_SCALE_TYPE = FILL_CENTER;

        @ScaleType
        private int mScaleType = DEFAULT_SCALE_TYPE;

        private final Rational mAspectRatio;

        @ImageOutputConfig.RotationValue
        private final int mRotation;

        @LayoutDirection
        private int mLayoutDirection = DEFAULT_LAYOUT_DIRECTION;

        /**
         * Creates {@link ViewPort.Builder} with aspect ratio and rotation.
         *
         * @param aspectRatio desired aspect ratio of crop rect obtained from
         *                    {@link SurfaceRequest#getCropRect()} and/or
         *                    {@link ImageProxy#getCropRect()}, if the scale type is FILL_*. This
         *                    is usually the width/height of the preview viewfinder that displays
         *                    the camera feed.
         * @param rotation    Similar to {@link ImageCapture#setTargetRotation(int)}, the rotation
         *                    value is one of four valid values: {@link Surface#ROTATION_0},
         *                    {@link Surface#ROTATION_90}, {@link Surface#ROTATION_180},
         *                    {@link Surface#ROTATION_270}. If the viewport is based on
         *                    {@link Preview}, it is usually set with the value of
         *                    {@link Display#getRotation()}.
         */
        public Builder(@NonNull Rational aspectRatio,
                @ImageOutputConfig.RotationValue int rotation) {
            mAspectRatio = aspectRatio;
            mRotation = rotation;
        }

        /**
         * Sets the {@link ScaleType} of the {@link ViewPort}.
         *
         * <p> The value is used by {@link UseCase} to calculate the
         * {@link UseCase#getViewPortCropRect()}.
         *
         * <p> The default value is {@link #FILL_CENTER} if not set.
         *
         * @hide
         */
        @NonNull
        public Builder setScaleType(@ScaleType int scaleType) {
            mScaleType = scaleType;
            return this;
        }

        /**
         * Sets the layout direction of the {@link ViewPort}.
         *
         * <p> The {@link LayoutDirection} decides the start and the end of the crop rect if
         * the {@link ScaleType} is one of the following types: {@link #FILL_END},
         * {@link #FILL_START},{@link #FIT_START} or {@link #FIT_END}.
         *
         * <p> The default value is {@link android.util.LayoutDirection#LTR} if not set.
         *
         * @hide
         */
        @NonNull
        public Builder setLayoutDirection(@LayoutDirection int layoutDirection) {
            mLayoutDirection = layoutDirection;
            return this;
        }

        /**
         * Builds the {@link ViewPort}.
         */
        @NonNull
        public ViewPort build() {
            Preconditions.checkNotNull(mAspectRatio, "The crop aspect ratio must be set.");
            return new ViewPort(mScaleType, mAspectRatio, mRotation, mLayoutDirection);
        }
    }
}
