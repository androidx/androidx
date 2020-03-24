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

import android.graphics.Point;
import android.util.Pair;
import android.util.Size;
import android.view.Display;
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
     * @param container  Preview container
     * @param preview    Preview view (a {@link android.view.TextureView} or
     *                   {@link android.view.SurfaceView})
     * @param bufferSize Camera output size
     */
    @NonNull
    static PreviewCorrectionTransformation getCorrectionTransformation(
            @NonNull final View container, @NonNull final View preview,
            @NonNull final Size bufferSize) {
        final int rotation = (int) RotationTransform.getRotationDegrees(preview);
        final Pair<Float, Float> scaleXY = getCorrectionScale(container, preview,
                bufferSize);
        return new PreviewCorrectionTransformation(scaleXY.first, scaleXY.second, -rotation);
    }

    /**
     * Computes the scales on both the x and y axes so that the preview can be corrected.
     *
     * @param container  Preview container
     * @param preview    Preview view (a {@link android.view.TextureView} or
     *                   {@link android.view.SurfaceView})
     * @param bufferSize Camera output size
     * @return The scales on both the x and y axes so that the preview can be corrected.
     */
    private static Pair<Float, Float> getCorrectionScale(@NonNull final View container,
            @NonNull final View preview, @NonNull final Size bufferSize) {

        // Scaling only makes sense when none of the dimensions are equal to zero. In the
        // opposite case, a default scale of 1 is returned,
        if (container.getWidth() == 0 || container.getHeight() == 0 || preview.getWidth() == 0
                || preview.getHeight() == 0 || bufferSize.getWidth() == 0
                || bufferSize.getHeight() == 0) {
            return new Pair<>(1F, 1F);
        }

        final int bufferWidth;
        final int bufferHeight;
        if (isNaturalPortrait(preview)) {
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

    /**
     * Determines whether the current device is a natural portrait-oriented device
     *
     * <p>
     * Using the current app's window to determine whether the device is a natural
     * portrait-oriented device doesn't work in all scenarios, one example of this is multi-window
     * mode.
     * Taking a natural portrait-oriented device in multi-window mode, rotating it 90 degrees (so
     * that it's in landscape), with the app open, and its window's width being smaller than its
     * height. Using the app's width and height would determine that the device isn't
     * naturally portrait-oriented, where in fact it is, which is why it is important to use the
     * size of the device instead.
     * </p>
     *
     * @param view A {@link View} used to get the current {@link Display}.
     * @return Whether the device is naturally portrait-oriented.
     */
    private static boolean isNaturalPortrait(@NonNull final View view) {
        final Display display = view.getDisplay();
        if (display == null) {
            return true;
        }

        final Point deviceSize = new Point();
        display.getRealSize(deviceSize);

        final int width = deviceSize.x;
        final int height = deviceSize.y;
        final int rotationDegrees = (int) RotationTransform.getRotationDegrees(view);
        return ((rotationDegrees == 0 || rotationDegrees == 180) && width < height) || (
                (rotationDegrees == 90 || rotationDegrees == 270) && width >= height);
    }
}
