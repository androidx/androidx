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
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.UseCase;
import androidx.camera.view.TransformExperimental;

/**
 * Factory for extracting transform info from {@link ImageProxy}.
 *
 * <p> This class is for extracting a {@link OutputTransform} from an {@link ImageProxy} object. The
 * {@link OutputTransform} represents the transform being applied to the original camera buffer,
 * which can be used by {@link CoordinateTransform} to transform coordinates between
 * {@link UseCase}s.
 *
 * @see OutputTransform
 * @see CoordinateTransform
 */
@TransformExperimental
public final class ImageProxyTransformFactory {

    private boolean mUsingCropRect;
    private boolean mUsingRotationDegrees;

    public ImageProxyTransformFactory() {
    }

    /**
     * Whether to use the crop rect of the {@link ImageProxy}.
     *
     * <p> By default, the value is false and the factory uses the {@link ImageProxy}'s
     * entire buffer. Only set this value if the coordinates to be transformed respect the
     * crop rect. For example, top-left corner of the crop rect is (0, 0).
     */
    public void setUsingCropRect(boolean usingCropRect) {
        mUsingCropRect = usingCropRect;
    }

    /**
     * Whether the factory respects the value of {@link ImageProxy#getCropRect()}.
     *
     * <p>By default, the value is false.
     */
    public boolean isUsingCropRect() {
        return mUsingCropRect;
    }

    /**
     * Whether to use the rotation degrees of the {@link ImageProxy}.
     *
     * <p> By default, the value is false and the factory uses a rotation degree of 0. Only
     * set this value to true if the coordinates to be transformed is after the rotation degree is
     * applied. For example, if the {@link ImageInfo#getRotationDegrees()} is 90 degrees, (0, 0) in
     * the original buffer should be mapped to (height, 0) in the rotated image. Set this value
     * to true if the input coordinates are based on the original image, and false if the
     * coordinates are based on the rotated image.
     */
    public void setUsingRotationDegrees(boolean usingRotationDegrees) {
        mUsingRotationDegrees = usingRotationDegrees;
    }

    /**
     * Whether the factory respects the value of {@link ImageInfo#getRotationDegrees()}.
     *
     * <p>By default, the value is false.
     */
    public boolean isUsingRotationDegrees() {
        return mUsingRotationDegrees;
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
        if (mUsingCropRect) {
            return new RectF(imageProxy.getCropRect());
        }
        // The default crop rect is the full buffer.
        return new RectF(0, 0, imageProxy.getWidth(), imageProxy.getHeight());
    }

    /**
     * Gets the rotation degrees based on factory settings.
     */
    private int getRotationDegrees(@NonNull ImageProxy imageProxy) {
        if (mUsingRotationDegrees) {
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
}
