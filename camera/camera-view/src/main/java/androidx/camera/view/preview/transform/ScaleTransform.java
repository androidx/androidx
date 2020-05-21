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

import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.view.preview.transform.transformation.ScaleTransformation;

/**
 * Computes the scale on both the x and y axes to uniformly scale up or down a view inside its
 * container, so that it entirely fills it, or is entirely container within it.
 */
final class ScaleTransform {

    private ScaleTransform() {
    }

    /**
     * Computes the scale on both the x and y axes so that the view can uniformly fill its
     * container.
     */
    static ScaleTransformation fill(@NonNull final View container, @NonNull final View view,
            final int deviceRotation) {
        return computeScale(container, view, Math::max, deviceRotation);
    }

    /**
     * Computes the scale on both the x and y axes so that the view can uniformly fit inside its
     * container.
     */
    static ScaleTransformation fit(@NonNull final View container, @NonNull final View view,
            final int deviceRotation) {
        return computeScale(container, view, Math::min, deviceRotation);
    }

    private static ScaleTransformation computeScale(@NonNull final View container,
            @NonNull final View view, @NonNull final FloatBiFunction function,
            final int deviceRotation) {
        // Scaling only makes sense when none of the dimensions are equal to zero. In the
        // opposite case, a default scale of 1 is returned,
        if (container.getWidth() == 0 || container.getHeight() == 0 || view.getWidth() == 0
                || view.getHeight() == 0) {
            return new ScaleTransformation(1);
        }

        final int rotationDegrees = (int) RotationTransform.getRotationDegrees(view,
                deviceRotation);
        float bufferRotatedWidth;
        float bufferRotatedHeight;
        if (rotationDegrees == 0 || rotationDegrees == 180) {
            bufferRotatedWidth = view.getWidth() * view.getScaleX();
            bufferRotatedHeight = view.getHeight() * view.getScaleY();
        } else {
            bufferRotatedWidth = view.getHeight() * view.getScaleY();
            bufferRotatedHeight = view.getWidth() * view.getScaleX();
        }

        final float scale = function.apply(container.getWidth() / bufferRotatedWidth,
                container.getHeight() / bufferRotatedHeight);
        return new ScaleTransformation(scale);
    }

    private interface FloatBiFunction {
        float apply(float a, float b);
    }
}
