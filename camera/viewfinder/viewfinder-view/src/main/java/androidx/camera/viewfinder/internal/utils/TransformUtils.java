/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.viewfinder.internal.utils;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Size;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.viewfinder.CameraViewfinder;
import androidx.camera.viewfinder.internal.transform.TransformationInfo;

/**
 * Utility class for transform.
 *
 * <p> The vertices representation uses a float array to represent a rectangle with arbitrary
 * rotation and rotation-direction. It could be otherwise represented by a triple of a
 * {@link RectF}, a rotation degrees integer and a boolean flag for the rotation-direction
 * (clockwise v.s. counter-clockwise).
 *
 * TODO(b/179827713): merge this with androidx.camera.core.internal.utils.ImageUtil.
 */
public class TransformUtils {

    // Normalized space (-1, -1) - (1, 1).
    public static final RectF NORMALIZED_RECT = new RectF(-1, -1, 1, 1);

    private TransformUtils() {
    }

    /**
     * Converts {@link Surface} rotation to rotation degrees: 90, 180, 270 or 0.
     */
    public static int surfaceRotationToRotationDegrees(int rotationValue) {
        switch (rotationValue) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                throw new IllegalStateException("Unexpected rotation value " + rotationValue);
        }
    }

    /**
     * Returns true if the rotation degrees is 90 or 270.
     */
    public static boolean is90or270(int rotationDegrees) {
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            return true;
        }
        if (rotationDegrees == 0 || rotationDegrees == 180) {
            return false;
        }
        throw new IllegalArgumentException("Invalid rotation degrees: " + rotationDegrees);
    }

    /**
     * Converts a {@link Size} to a float array of vertices.
     */
    @NonNull
    public static float[] sizeToVertices(@NonNull Size size) {
        return new float[]{0, 0, size.getWidth(), 0, size.getWidth(), size.getHeight(), 0,
                size.getHeight()};
    }

    /**
     * Checks if aspect ratio matches while tolerating rounding error.
     *
     * <p> One example of the usage is comparing the viewport-based crop rect from different use
     * cases. The crop rect is rounded because pixels are integers, which may introduce an error
     * when we check if the aspect ratio matches. For example, when {@link CameraViewfinder}'s
     * width/height are prime numbers 601x797, the crop rect from other use cases cannot have a
     * matching aspect ratio even if they are based on the same viewport. This method checks the
     * aspect ratio while tolerating a rounding error.
     *
     * @param size1       the rounded size1
     * @param isAccurate1 if size1 is accurate. e.g. it's true if it's the PreviewView's
     *                    dimension which viewport is based on
     * @param size2       the rounded size2
     * @param isAccurate2 if size2 is accurate.
     */
    public static boolean isAspectRatioMatchingWithRoundingError(
            @NonNull Size size1, boolean isAccurate1, @NonNull Size size2, boolean isAccurate2) {
        // The crop rect coordinates are rounded values. Each value is at most .5 away from their
        // true values. So the width/height, which is the difference of 2 coordinates, are at most
        // 1.0 away from their true value.
        // First figure out the possible range of the aspect ratio's true value.
        float ratio1UpperBound;
        float ratio1LowerBound;
        if (isAccurate1) {
            ratio1UpperBound = (float) size1.getWidth() / size1.getHeight();
            ratio1LowerBound = ratio1UpperBound;
        } else {
            ratio1UpperBound = (size1.getWidth() + 1F) / (size1.getHeight() - 1F);
            ratio1LowerBound = (size1.getWidth() - 1F) / (size1.getHeight() + 1F);
        }
        float ratio2UpperBound;
        float ratio2LowerBound;
        if (isAccurate2) {
            ratio2UpperBound = (float) size2.getWidth() / size2.getHeight();
            ratio2LowerBound = ratio2UpperBound;
        } else {
            ratio2UpperBound = (size2.getWidth() + 1F) / (size2.getHeight() - 1F);
            ratio2LowerBound = (size2.getWidth() - 1F) / (size2.getHeight() + 1F);
        }
        // Then we check if the true value range overlaps.
        return ratio1UpperBound >= ratio2LowerBound && ratio2UpperBound >= ratio1LowerBound;
    }

    /**
     * Gets the transform from one {@link Rect} to another with rotation degrees.
     *
     * <p> Following is how the source is mapped to the target with a 90° rotation. The rect
     * &lt;a, b, c, d&gt; is mapped to &lt;a', b', c', d'&gt;.
     *
     * <pre>
     *  a----------b               d'-----------a'
     *  |  source  |    -90°->     |            |
     *  d----------c               |   target   |
     *                             |            |
     *                             c'-----------b'
     * </pre>
     */
    @NonNull
    public static Matrix getRectToRect(
            @NonNull RectF source, @NonNull RectF target, int rotationDegrees) {
        // Map source to normalized space.
        Matrix matrix = new Matrix();
        matrix.setRectToRect(source, NORMALIZED_RECT, Matrix.ScaleToFit.FILL);
        // Add rotation.
        matrix.postRotate(rotationDegrees);
        // Restore the normalized space to target's coordinates.
        matrix.postConcat(getNormalizedToBuffer(target));
        return matrix;
    }

    /**
     * Creates {@link TransformationInfo} by resolution, display, front camera and sensor
     * orientation.
     *
     * @param surfaceResolution The surface resolution.
     * @param display The view {@link Display}.
     * @param isFrontCamera The front or rear camera information.
     * @param sensorOrientation THe sensor orientation.
     * @return {@link TransformationInfo}.
     */
    @NonNull
    public static TransformationInfo createTransformInfo(
            @NonNull Size surfaceResolution,
            @NonNull Display display,
            boolean isFrontCamera,
            int sensorOrientation) {
        // For Camera2, cropRect is equal to full size of surface and targetRotation is default
        // display rotation.
        // For CameraX, targetRotation can be set by the user.
        Rect cropRect = new Rect(0, 0,
                surfaceResolution.getWidth(), surfaceResolution.getHeight());
        // TODO: verify the viewfinder working correctly when not in a locked portrait orientation,
        //  for both the PERFORMANCE and the COMPATIBLE mode
        int relativeRotationDegrees =
                CameraOrientationUtil.surfaceRotationToDegrees(display.getRotation());
        return TransformationInfo.of(cropRect,
                CameraOrientationUtil.getRelativeImageRotation(
                        relativeRotationDegrees,
                        sensorOrientation,
                        !isFrontCamera),
                display.getRotation());
    }

    /**
     * Gets the transform from a normalized space (-1, -1) - (1, 1) to the given rect.
     */
    @NonNull
    private static Matrix getNormalizedToBuffer(@NonNull RectF viewPortRect) {
        Matrix normalizedToBuffer = new Matrix();
        normalizedToBuffer.setRectToRect(NORMALIZED_RECT, viewPortRect, Matrix.ScaleToFit.FILL);
        return normalizedToBuffer;
    }
}
