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

package androidx.camera.view.preview.transform;

import android.util.Pair;
import android.util.Size;
import android.view.Surface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.view.preview.transform.transformation.PreviewCorrectionTransformation;

/** Corrects a camera preview by scaling and/or rotating it so that it matches the display. */
final class PreviewCorrector {

    private PreviewCorrector() {
    }

    /**
     * Corrects a camera preview by scaling and rotating it.
     *
     * @param container                 Preview container
     * @param preview                   Preview view (a {@link android.view.TextureView} or
     *                                  {@link android.view.SurfaceView})
     * @param bufferSize                Camera output size
     * @param sensorDimensionFlipNeeded True if the sensor x and y dimensions need to be flipped.
     * @param deviceRotation            If the app is not running in remote display mode, set the
     *                                  parameter as {@link RotationTransform#ROTATION_AUTOMATIC}.
     *                                  Then, the rotation value queried from the preview will be
     *                                  used to do the transformation calculations. If the app is
     *                                  running in remote display mode, the device rotation value
     *                                  needs to be provided to make the result be rotated into
     *                                  correct orientation. The device rotation should be obtained
     *                                  from {@link android.view.OrientationEventListener} and
     *                                  needs to be converted into {@link Surface#ROTATION_0},
     *                                  {@link Surface#ROTATION_90}, {@link Surface#ROTATION_180}
     *                                  , or {@link Surface#ROTATION_270}.
     */
    @NonNull
    static PreviewCorrectionTransformation getCorrectionTransformation(
            @NonNull final View container, @NonNull final View preview,
            @NonNull final Size bufferSize, final boolean sensorDimensionFlipNeeded,
            final int deviceRotation) {
        final int rotation = (int) RotationTransform.getRotationDegrees(preview, deviceRotation);
        final Pair<Float, Float> scaleXY = getCorrectionScale(container, preview,
                bufferSize, sensorDimensionFlipNeeded);
        return new PreviewCorrectionTransformation(scaleXY.first, scaleXY.second, -rotation);
    }

    /**
     * Computes the scales on both the x and y axes so that the preview can be corrected.
     *
     * @param container                 Preview container
     * @param preview                   Preview view (a {@link android.view.TextureView} or
     *                                  {@link android.view.SurfaceView})
     * @param bufferSize                Camera output size
     * @param sensorDimensionFlipNeeded True if the sensor x and y dimensions need to be flipped.
     * @return The scales on both the x and y axes so that the preview can be corrected.
     */
    private static Pair<Float, Float> getCorrectionScale(@NonNull final View container,
            @NonNull final View preview, @NonNull final Size bufferSize,
            final boolean sensorDimensionFlipNeeded) {

        // Scaling only makes sense when none of the dimensions are equal to zero. In the
        // opposite case, a default scale of 1 is returned,
        if (container.getWidth() == 0 || container.getHeight() == 0 || preview.getWidth() == 0
                || preview.getHeight() == 0 || bufferSize.getWidth() == 0
                || bufferSize.getHeight() == 0) {
            return new Pair<>(1F, 1F);
        }

        final int bufferWidth;
        final int bufferHeight;
        if (sensorDimensionFlipNeeded) {
            bufferWidth = bufferSize.getHeight();
            bufferHeight = bufferSize.getWidth();
        } else {
            bufferWidth = bufferSize.getWidth();
            bufferHeight = bufferSize.getHeight();
        }

        // Scale the buffers back to the original output size.
        float scaleX = bufferWidth / (float) preview.getWidth();
        float scaleY = bufferHeight / (float) preview.getHeight();
        return new Pair<>(scaleX, scaleY);
    }
}
