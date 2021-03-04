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

import static androidx.camera.view.TransformUtils.min;
import static androidx.camera.view.TransformUtils.rectToSize;
import static androidx.camera.view.TransformUtils.rectToVertices;
import static androidx.camera.view.transform.OutputTransform.getNormalizedToBuffer;

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
        Matrix matrix = new Matrix();

        // Map the viewport to output.
        float[] cropRectVertices = rectToVertices(getCropRect(imageProxy));
        float[] outputVertices = getRotatedVertices(cropRectVertices,
                getRotationDegrees(imageProxy));
        matrix.setPolyToPoly(cropRectVertices, 0, outputVertices, 0, 4);

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
     * Rotates the crop rect with given degrees.
     *
     * <p> Rotate the vertices, then align the top left corner to (0, 0).
     *
     * <pre>
     *         (0, 0)                          (0, 0)
     * Before  +-----Surface-----+     After:  a--------------------b
     *         |                 |             |          ^         |
     *         |  d-crop rect-a  |             |          |         |
     *         |  |           |  |             d--------------------c
     *         |  |           |  |
     *         |  |    -->    |  |    Rotation:        <-----+
     *         |  |           |  |                       270°|
     *         |  |           |  |                           |
     *         |  c-----------b  |
     *         +-----------------+
     * </pre>
     */
    static float[] getRotatedVertices(float[] cropRectVertices, int rotationDegrees) {
        // Rotate the vertices. The pivot point doesn't matter since we are gong to align it to
        // the origin afterwards.
        float[] vertices = cropRectVertices.clone();
        Matrix matrix = new Matrix();
        matrix.setRotate(rotationDegrees);
        matrix.mapPoints(vertices);

        // Align the rotated vertices to origin. The transformed output always starts at (0, 0).
        float left = min(vertices[0], vertices[2], vertices[4], vertices[6]);
        float top = min(vertices[1], vertices[3], vertices[5], vertices[7]);
        for (int i = 0; i < vertices.length; i += 2) {
            vertices[i] -= left;
            vertices[i + 1] -= top;
        }
        return vertices;
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
