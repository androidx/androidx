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
import static androidx.camera.view.TransformUtils.rectToVertices;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.view.TransformExperimental;

/**
 * The transform of a {@link ImageProxy}.
 *
 * <p> {@link ImageProxy} can be the output of {@link ImageAnalysis} or in-memory
 * {@link ImageCapture}. This class represents the transform applied to the raw buffer of a
 * {@link ImageProxy}.
 *
 * TODO(b/179827713): unhide this class once all transform utils are done.
 *
 * @hide
 */
@TransformExperimental
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ImageProxyTransform extends OutputTransform {

    ImageProxyTransform(@NonNull Matrix matrix, @NonNull Rect viewPortRect) {
        super(matrix, new Size(viewPortRect.width(), viewPortRect.height()));
    }

    /**
     * Builder of {@link ImageProxyTransform}.
     */
    public static class Builder extends OutputTransform.Builder {

        private final Rect mViewPortRect;
        private int mRotationDegrees;
        private Rect mCropRect;

        /**
         * @param imageProxy the {@link ImageProxy} that the transform applies to.
         */
        public Builder(@NonNull ImageProxy imageProxy) {
            mViewPortRect = imageProxy.getCropRect();
            mCropRect = new Rect(0, 0, imageProxy.getWidth(), imageProxy.getHeight());
            mRotationDegrees = 0;
        }

        /**
         * Sets the crop rect.
         *
         * <p> Only sets this value if the coordinates to be transformed respect the crop
         * rect, for example, the origin of the coordinates system is the (top, left) of the crop
         * rect.
         */
        @NonNull
        public Builder setCropRect(@NonNull Rect cropRect) {
            mCropRect = cropRect;
            return this;
        }

        /**
         * Sets the rotation degrees.
         *
         * <p> Only sets this value if the coordinates to be transformed respect the rotation
         * degrees.
         */
        @NonNull
        public Builder setRotationDegrees(int rotationDegrees) {
            mRotationDegrees = rotationDegrees;
            return this;
        }

        // TODO(b/179827713): Support mirroring.

        /**
         * Builds the {@link ImageProxyTransform} object.
         */
        @NonNull
        public ImageProxyTransform build() {
            Matrix matrix = new Matrix();

            // Map the viewport to output.
            float[] cropRectVertices = rectToVertices(new RectF(mCropRect));
            float[] outputVertices = getRotatedVertices(cropRectVertices, mRotationDegrees);
            matrix.setPolyToPoly(cropRectVertices, 0, outputVertices, 0, 4);

            // Map the normalized space to viewport.
            matrix.preConcat(getNormalizedToBuffer(mViewPortRect));

            return new ImageProxyTransform(matrix, mViewPortRect);
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
    }
}
