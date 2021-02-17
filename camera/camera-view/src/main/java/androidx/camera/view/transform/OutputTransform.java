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

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.view.TransformExperimental;

/**
 * Represents the transform applied to the output of a {@link UseCase}.
 *
 * <p> Represents the rotation, cropping and/or mirroring applied to the raw buffer of a
 * {@link UseCase} output.
 *
 * TODO(b/179827713): unhide this class once all transform utils are done.
 *
 * @hide
 */
@TransformExperimental
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OutputTransform {

    // Normalized space that maps to the viewport rect.
    private static final RectF NORMALIZED_RECT = new RectF(0, 0, 1, 1);

    @NonNull
    final Matrix mMatrix;
    @NonNull
    final Size mViewPortSize;

    /**
     * @param matrix       The mapping from a normalized viewport space (0, 0) - (1, 1) to
     *                     the transformed output. e.g. the (0, 0) maps to the (top, left) of
     *                     the viewport and (1, 1) maps to the (bottom, right) of the
     *                     viewport.
     * @param viewPortSize The aspect ratio of the viewport. This is not used in transform
     *                     computation. This is only used for mitigating the user mistake of not
     *                     using a {@link UseCaseGroup}. By comparing the viewport to that of the
     *                     other {@link OutputTransform}, we can at least make sure that they
     *                     have the same aspect ratio. Viewports with different aspect ratios
     *                     cannot be from the same {@link UseCaseGroup}.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public OutputTransform(@NonNull Matrix matrix, @NonNull Size viewPortSize) {
        mMatrix = matrix;
        mViewPortSize = viewPortSize;
    }

    @NonNull
    Matrix getMatrix() {
        return mMatrix;
    }

    @NonNull
    Size getViewPortSize() {
        return mViewPortSize;
    }

    /**
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static Matrix getNormalizedToBuffer(@NonNull Rect viewPortRect) {
        return getNormalizedToBuffer(new RectF(viewPortRect));
    }

    /**
     * Gets the transform from a normalized space (0, 0) - (1, 1) to viewport rect.
     */
    @NonNull
    static Matrix getNormalizedToBuffer(@NonNull RectF viewPortRect) {
        Matrix normalizedToBuffer = new Matrix();
        normalizedToBuffer.setRectToRect(NORMALIZED_RECT, viewPortRect, Matrix.ScaleToFit.FILL);
        return normalizedToBuffer;
    }
}
