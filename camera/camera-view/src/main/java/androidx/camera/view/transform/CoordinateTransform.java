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

import static androidx.camera.view.TransformUtils.isAspectRatioMatchingWithRoundingError;

import android.graphics.Matrix;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Logger;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.view.PreviewView;
import androidx.camera.view.TransformExperimental;

/**
 * This class represents the transform from one {@link OutputTransform} to another.
 *
 * <p> This class can be used to map the coordinates of one {@link OutputTransform} to another,
 * given that they are both from the same {@link UseCaseGroup}. {@link OutputTransform} can
 * represent the output of a {@link UseCase} or {@link PreviewView}. For example, mapping the
 * coordinates of detected objects from {@link ImageAnalysis} output to the drawing location in
 * {@link PreviewView}.
 *
 * TODO(b/179827713): add code samples when more {@link OutputTransform} subclasses are available.
 * TODO(b/179827713): unhide this class once all transform utils are done.
 *
 * @hide
 */
@TransformExperimental
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CoordinateTransform {

    private static final String TAG = "CoordinateTransform";
    private static final String MISMATCH_MSG = "The source viewport (%s) does not match the target "
            + "viewport (%s). Please make sure they are from the same UseCaseGroup.";

    private final Matrix mMatrix;

    /**
     * Creates the transform between the {@code source} and the {@code target}.
     *
     * <p> The source and the target must be from the same {@link UseCaseGroup}.
     *
     * @param source the source
     * @see UseCaseGroup
     * @see ViewPort
     */
    public CoordinateTransform(@NonNull OutputTransform source,
            @NonNull OutputTransform target) {
        // TODO(b/137515129): This is a poor way to check if the two outputs are based on
        //  the same viewport. A better way is to add a matrix in use case output that represents
        //  the transform from sensor to surface. But it will require the view artifact to
        //  depend on a new internal API in the core artifact, which we can't do at the
        //  moment because of the version mismatch between view and core.
        if (!isAspectRatioMatchingWithRoundingError(
                source.getViewPortSize(), /* isAccurate1= */ false,
                target.getViewPortSize(), /* isAccurate2= */ false)) {
            // Mismatched aspect ratio means the outputs are not from the same UseCaseGroup
            Logger.w(TAG, String.format(MISMATCH_MSG, source.getViewPortSize(),
                    target.getViewPortSize()));
        }

        // Concatenate the source transform with the target transform.
        mMatrix = new Matrix();
        source.getMatrix().invert(mMatrix);
        mMatrix.postConcat(target.getMatrix());
    }

    /**
     * Gets the transform matrix.
     *
     * @param matrix a {@link android.graphics.Matrix} that represents the transform from source
     *               to target.
     */
    public void getTransform(@NonNull Matrix matrix) {
        matrix.set(mMatrix);
    }

    /**
     * Apply this transform to the array of 2D points, and write the transformed points back into
     * the array
     *
     * @param points The array [x0, y0, x1, y1, ...] of points to transform.
     * @see Matrix#mapPoints(float[])
     */
    public void mapPoints(@NonNull float[] points) {
        mMatrix.mapPoints(points);
    }

    // TODO(b/179827713): add overloading mapPoints method for other data types.
}
