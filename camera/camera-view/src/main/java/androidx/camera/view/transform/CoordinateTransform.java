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
import android.graphics.PointF;
import android.graphics.RectF;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Logger;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.view.PreviewView;
import androidx.camera.view.TransformExperimental;
import androidx.core.util.Preconditions;

/**
 * This class represents the transform from one {@link OutputTransform} to another.
 *
 * <p> This class can be used to map the coordinates of one {@link OutputTransform} to another,
 * given that they are associated with the same {@link ViewPort}. {@link OutputTransform} can
 * represent the output of a {@link UseCase} or {@link PreviewView}. For example, mapping the
 * coordinates of detected objects from {@link ImageAnalysis} output to the drawing location in
 * {@link PreviewView}.
 *
 * <pre><code>
 * // imageProxy the output of an ImageAnalysis.
 * OutputTransform source = ImageProxyTransformFactory().getOutputTransform(imageProxy);
 * OutputTransform target = previewView.getOutputTransform();
 *
 * // Build the transform from ImageAnalysis to PreviewView
 * CoordinateTransform coordinateTransform = new CoordinateTransform(source, target);
 *
 * // Detect face in ImageProxy and transform the coordinates to PreviewView.
 * // The value of faceBox can be used to highlight the face in PreviewView.
 * RectF faceBox = detectFaceInImageProxy(imageProxy);
 * coordinateTransform.mapRect(faceBox);
 *
 * </code></pre>
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@TransformExperimental
public final class CoordinateTransform {

    private static final String TAG = "CoordinateTransform";
    private static final String MISMATCH_MSG = "The source viewport (%s) does not match the target "
            + "viewport (%s). Please make sure they are associated with the same Viewport.";

    private final Matrix mMatrix;

    /**
     * Creates the transform between the {@code source} and the {@code target}.
     *
     * <p> The source and the target must be associated with the same {@link ViewPort}.
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
            // Mismatched aspect ratio means the outputs are not associated with the same Viewport.
            Logger.w(TAG, String.format(MISMATCH_MSG, source.getViewPortSize(),
                    target.getViewPortSize()));
        }

        // Concatenate the source transform with the target transform.
        mMatrix = new Matrix();
        Preconditions.checkState(source.getMatrix().invert(mMatrix),
                "The source transform cannot be inverted");
        mMatrix.postConcat(target.getMatrix());
    }

    /**
     * Copies the current transform to the specified {@link Matrix}.
     *
     * @param outMatrix a {@link android.graphics.Matrix} in which to copy the current transform.
     */
    public void transform(@NonNull Matrix outMatrix) {
        outMatrix.set(mMatrix);
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

    /**
     * Apply this transform to the {@link PointF}, and write the transformed points back into
     * the array
     *
     * @param point The point to transform.
     */
    public void mapPoint(@NonNull PointF point) {
        float[] pointArray = new float[]{point.x, point.y};
        mMatrix.mapPoints(pointArray);
        point.x = pointArray[0];
        point.y = pointArray[1];
    }

    /**
     * Apply this transform to the rectangle, and write the transformed rectangle back into it.
     * This is accomplished by transforming the 4 corners of rect, and then setting it to the
     * bounds of those points.
     *
     * @param rect The rectangle to transform.
     */
    public void mapRect(@NonNull RectF rect) {
        mMatrix.mapRect(rect);
    }
}
