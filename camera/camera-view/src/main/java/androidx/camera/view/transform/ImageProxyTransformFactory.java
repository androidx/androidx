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

package androidx.camera.view.transform;

import static androidx.camera.view.TransformUtils.getNormalizedToBuffer;
import static androidx.camera.view.TransformUtils.getRectToRect;
import static androidx.camera.view.TransformUtils.is90or270;
import static androidx.camera.view.TransformUtils.rectToSize;

import android.graphics.Matrix;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.TransformExperimental;

/**
 * Factory for extracting transform info from {@link ImageProxy}.
 *
 * TODO(b/179827713): unhide this class once all transform utils are done.
 *
 * @hide
 */
@TransformExperimental
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ImageProxyTransformFactory {

    private final boolean mUseCropRect;
    private final boolean mUseRotationDegrees;

    ImageProxyTransformFactory(boolean useCropRect, boolean useRotationDegrees) {
        mUseCropRect = useCropRect;
        mUseRotationDegrees = useRotationDegrees;
    }

    /**
     * Extracts the transform from the given {@link ImageProxy}.
     *
     * <p> This method returns a {@link OutputTransform} that represents the
     * transform applied to the buffer of a {@link ImageProxy} based on factory settings.  An
     * {@link ImageProxy} can be the output of {@link ImageAnalysis} or in-memory
     * {@link ImageCapture}.
     */
    @NonNull
    public OutputTransform getOutputTransform(@NonNull ImageProxy imageProxy) {
        // Map the viewport to output.
        int rotationDegrees = getRotationDegrees(imageProxy);
        RectF source = getCropRect(imageProxy);
        RectF target = getRotatedCropRect(source, rotationDegrees);
        Matrix matrix = getRectToRect(source, target, rotationDegrees);

        // Map the normalized space to viewport.
        matrix.preConcat(getNormalizedToBuffer(imageProxy.getCropRect()));

        return new OutputTransform(matrix, rectToSize(imageProxy.getCropRect()));
    }

    /**
     * Gets the crop rect based on factory settings.
     */
    private RectF getCropRect(@NonNull ImageProxy imageProxy) {
        if (mUseCropRect) {
            return new RectF(imageProxy.getCropRect());
        }
        // The default crop rect is the full buffer.
        return new RectF(0, 0, imageProxy.getWidth(), imageProxy.getHeight());
    }

    /**
     * Gets the rotation degrees based on factory settings.
     */
    private int getRotationDegrees(@NonNull ImageProxy imageProxy) {
        if (mUseRotationDegrees) {
            return imageProxy.getImageInfo().getRotationDegrees();
        }
        // The default is no rotation.
        return 0;
    }

    /**
     * Rotates the rect and align it to (0, 0).
     */
    static RectF getRotatedCropRect(RectF rect, int rotationDegrees) {
        if (is90or270(rotationDegrees)) {
            return new RectF(0, 0, rect.height(), rect.width());
        }
        return new RectF(0, 0, rect.width(), rect.height());
    }

    /**
     * Builder of {@link ImageProxyTransformFactory}.
     */
    public static class Builder {

        private boolean mUseCropRect = false;
        private boolean mUseRotationDegrees = false;

        /**
         * Whether to use the crop rect of the {@link ImageProxy}.
         *
         * <p> By default, the value is false and the factory uses the {@link ImageProxy}'s
         * entire buffer. Only set this value if the coordinates to be transformed respect the
         * crop rect. For example, top-left corner of the crop rect is (0, 0).
         */
        @NonNull
        public Builder setUseCropRect(boolean useCropRect) {
            mUseCropRect = useCropRect;
            return this;
        }

        /**
         * Whether to use the rotation degrees of the {@link ImageProxy}.
         *
         * <p> By default, the value is false and the factory uses a rotation degree of 0. Only
         * set this value if the coordinates to be transformed respect the rotation degrees. For
         * example, if rotation is 90°, (0, 0) should map to (0, height) on the buffer.
         */
        @NonNull
        public Builder setUseRotationDegrees(boolean useRotationDegrees) {
            mUseRotationDegrees = useRotationDegrees;
            return this;
        }

        // TODO(b/179827713): Add support for mirroring.

        /**
         * Builds the {@link ImageProxyTransformFactory} object.
         */
        @NonNull
        public ImageProxyTransformFactory build() {
            return new ImageProxyTransformFactory(mUseCropRect, mUseRotationDegrees);
        }
    }
}
