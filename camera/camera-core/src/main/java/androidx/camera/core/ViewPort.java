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
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * The field of view of one or many {@link UseCase}s.
 *
 * <p> The {@link ViewPort} defines a FOV which is used by CameraX to calculate output crop rects.
 * For use cases associated with the same {@link ViewPort} in a {@link UseCaseGroup}, the output
 * crop rect will be mapped to the same camera sensor area. Usually {@link ViewPort} is
 * configured to optimize for {@link Preview} so that {@link ImageAnalysis} and
 * {@link ImageCapture} produce the same crop rect in a WYSIWYG way.
 *
 * <p> If the {@link ViewPort} is used with a {@link ImageCapture} and
 * {@link ImageCapture#takePicture(
 *ImageCapture.OutputFileOptions, Executor, ImageCapture.OnImageSavedCallback)} is called,
 * the image may be cropped before saving to disk which introduces an additional
 * latency. To avoid the latency and get the uncropped image, please use the in-memory method
 * {@link ImageCapture#takePicture(Executor, ImageCapture.OnImageCapturedCallback)}.
 *
 * <p> For {@link ImageAnalysis} and in-memory {@link ImageCapture}, the output crop rect is
 * {@link ImageProxy#getCropRect()}; for on-disk {@link ImageCapture}, the image is cropped before
 * saving; for {@link Preview}, the crop rect is
 * {@link SurfaceRequest.TransformationInfo#getCropRect()}. Caller should transform the output in
 * a way that only the area defined by the crop rect is visible to end users. Once the crop rect
 * is applied, all the use cases will produce the same image with possibly different resolutions.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class ViewPort {

    /**
     * LayoutDirection that defines the start and end of the {@link ScaleType}.
     *
     * @hide
     * @see android.util.LayoutDirection
     */
    @IntDef({android.util.LayoutDirection.LTR, android.util.LayoutDirection.RTL})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface LayoutDirection {
    }

    /**
     * Scale types used to calculate the crop rect for a {@link UseCase}.
     *
     * @hide
     */
    @IntDef({FILL_START, FILL_CENTER, FILL_END, FIT})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
     * Generate the max possible crop rect ignoring the aspect ratio. For {@link ImageAnalysis}
     * and {@link ImageCapture}, the output will be an image defined by the crop rect.
     *
     * <p> For {@link Preview}, further calculation is needed to to fit the crop rect into the
     * viewfinder. Code sample below is a simplified version assuming {@link Surface}
     * orientation is the same as the camera sensor orientation, the viewfinder is a
     * {@link SurfaceView} and the viewfinder's pixel width/height is the same as the size
     * request by CameraX in {@link SurfaceRequest#getResolution()}. For more complicated
     * scenarios, please check out the source code of PreviewView in androidx.camera.view artifact.
     *
     * <p> First, calculate the transformation to fit the crop rect in the center of the viewfinder:
     *
     * <pre>{@code
     *   val transformation = Matrix()
     *   transformation.setRectToRect(
     *       cropRect, new RectF(0, 0, viewFinder.width, viewFinder.height, ScaleToFit.CENTER))
     * }</pre>
     *
     * <p> Then apply the transformation to the viewfinder:
     *
     * <pre>{@code
     *   val transformedRect = RectF(0, 0, viewFinder.width, viewFinder.height)
     *   transformation.mapRect(surfaceRect)
     *   viewFinder.pivotX = 0
     *   viewFinder.pivotY = 0
     *   viewFinder.translationX = transformedRect.left
     *   viewFinder.translationY = transformedRect.top
     *   viewFinder.scaleX = surfaceRect.width/transformedRect.width
     *   viewFinder.scaleY = surfaceRect.height/transformedRect.height
     * }</pre>
     */
    public static final int FIT = 3;

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
     */
    @LayoutDirection
    public int getLayoutDirection() {
        return mLayoutDirection;
    }

    /**
     * Builder for {@link ViewPort}.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static final class Builder {

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
         * <p> To create a {@link ViewPort} that is based on the {@link Preview} use
         * case, the aspect ratio should be the dimension of the {@link View} and
         * the rotation should be the value of {@link Preview#getTargetRotation()}:
         *
         * <pre>{@code
         * val aspectRatio = Rational(viewFinder.width, viewFinder.height)
         * val viewport = ViewPort.Builder(aspectRatio, preview.getTargetRotation()).build()
         * }</pre>
         *
         * <p> In a scenario where {@link Preview} is not used, for example, face detection in
         * {@link ImageAnalysis} and taking pictures with {@link ImageCapture} when faces are
         * found, the {@link ViewPort} should be created with the aspect ratio and rotation of the
         * {@link ImageCapture} use case.
         *
         * @param aspectRatio aspect ratio of the output crop rect if the scale type
         *                    is FILL_START, FILL_CENTER or FILL_END. This is usually the
         *                    width/height of the preview viewfinder that displays the camera
         *                    feed. The value is ignored if the scale type is FIT.
         * @param rotation    The rotation value is one of four valid values:
         *                    {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
         *                    {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}.
         */
        public Builder(@NonNull Rational aspectRatio,
                @ImageOutputConfig.RotationValue int rotation) {
            mAspectRatio = aspectRatio;
            mRotation = rotation;
        }

        /**
         * Sets the scale type of the {@link ViewPort}.
         *
         * <p> The value is used by {@link UseCase} to calculate the crop rect.
         *
         * <p> The default value is {@link #FILL_CENTER} if not set.
         */
        @NonNull
        public Builder setScaleType(@ScaleType int scaleType) {
            mScaleType = scaleType;
            return this;
        }

        /**
         * Sets the layout direction of the {@link ViewPort}.
         *
         * <p> The layout direction decides the start and the end of the crop rect if
         * the scale type is {@link #FILL_END} or {@link #FILL_START}.
         *
         * <p> The default value is {@link android.util.LayoutDirection#LTR} if not set.
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
