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
 */
@TransformExperimental
public final class OutputTransform {

    @NonNull
    final Matrix mMatrix;
    @NonNull
    final Size mViewPortSize;

    /**
     * @param matrix       The mapping from a normalized viewport space (-1, -1) - (1, 1) to
     *                     the transformed output. e.g. the (-1, -1) maps to the (top, left) of
     *                     the viewport and (1, 1) maps to the (bottom, right) of the
     *                     viewport.
     * @param viewPortSize The aspect ratio of the viewport. This is not used to calculate the
     *                     transform. This is only used for mitigating the user mistake of not
     *                     using a {@link UseCaseGroup}. By comparing the viewport to that of the
     *                     other {@link OutputTransform}, we can at least make sure that they
     *                     have the same aspect ratio, and warn developers if not. Viewports with
     *                     different aspect ratios cannot be from the same {@link UseCaseGroup}.
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

}
