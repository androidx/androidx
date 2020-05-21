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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.view.preview.transform.transformation.TranslationTransformation;

/**
 * Computes the horizontal and vertical translations by which the preview needs to be translated
 * to position it at the start, center or end of its parent.
 * <p>
 * The start represents the top left corner in a left-to-right (LTR) layout, or the top right
 * corner in a right-to-left (RTL) layout.
 * <p>
 * The end represents the bottom right corner in a left-to-right (LTR) layout, or the bottom left
 * corner in a right-to-left (RTL) layout.
 */
final class TranslationTransform {

    private TranslationTransform() {
    }

    /**
     * Computes the horizontal and vertical translations to set on {@code view} to align it to the
     * start of its parent {@code container}.
     * <p>
     * The start represents the top left corner in a left-to-right (LTR) layout, or the top right
     * corner in a right-to-left (RTL) layout.
     */
    static TranslationTransformation start(@NonNull final View view,
            @NonNull final Pair<Float, Float> scaleXY, final int deviceRotation) {
        if (view.getWidth() == 0 || view.getHeight() == 0) {
            return new TranslationTransformation(0, 0);
        }

        // Scaled width and height of the view
        final int scaledWidth = (int) (view.getWidth() * scaleXY.first);
        final int scaledHeight = (int) (view.getHeight() * scaleXY.second);

        final int viewRotationDegrees = (int) RotationTransform.getRotationDegrees(view,
                deviceRotation);
        final boolean isPortrait = viewRotationDegrees == 0 || viewRotationDegrees == 180;

        // Coordinates of the view's center after the `start` translation
        final int targetCenterX;
        final int targetCenterY;
        if (isPortrait) {
            targetCenterX = scaledWidth / 2;
            targetCenterY = scaledHeight / 2;
        } else {
            targetCenterX = scaledHeight / 2;
            targetCenterY = scaledWidth / 2;
        }

        // Current coordinates of the view's center
        final int currentCenterX = view.getWidth() / 2;
        final int currentCenterY = view.getHeight() / 2;

        final int transX = reverseIfRTLLayout(view, targetCenterX - currentCenterX);
        final int transY = targetCenterY - currentCenterY;
        return new TranslationTransformation(transX, transY);
    }

    /**
     * Computes the horizontal and vertical translations to set on {@code view} to center it in its
     * parent {@code container}.
     */
    static TranslationTransformation center(@NonNull final View container,
            @NonNull final View view) {
        if (view.getWidth() == 0 || view.getHeight() == 0) {
            return new TranslationTransformation(0, 0);
        }

        // Coordinates of the view's center after the `center` translation
        final int targetCenterX = container.getWidth() / 2;
        final int targetCenterY = container.getHeight() / 2;

        // Current coordinates of the view's center
        final int currentCenterX = view.getWidth() / 2;
        final int currentCenterY = view.getHeight() / 2;

        final int transX = reverseIfRTLLayout(view, targetCenterX - currentCenterX);
        final int transY = targetCenterY - currentCenterY;
        return new TranslationTransformation(transX, transY);
    }

    /**
     * Computes the horizontal and vertical translations to set on {@code view} to align it to the
     * end of its parent {@code container}.
     * <p>
     * The end represents the bottom right corner in a left-to-right (LTR) layout, or the bottom
     * left corner in a right-to-left (RTL) layout.
     */
    static TranslationTransformation end(@NonNull final View container, @NonNull final View view,
            @NonNull final Pair<Float, Float> scaleXY, final int deviceRotation) {
        if (view.getWidth() == 0 || view.getHeight() == 0) {
            return new TranslationTransformation(0, 0);
        }

        // Coordinates of the bottom right corner of the container
        final int endX = container.getWidth();
        final int endY = container.getHeight();

        // Scaled width and height of the view
        final int scaledWidth = (int) (view.getWidth() * scaleXY.first);
        final int scaledHeight = (int) (view.getHeight() * scaleXY.second);

        final int viewRotationDegrees = (int) RotationTransform.getRotationDegrees(view,
                deviceRotation);
        final boolean isPortrait = viewRotationDegrees == 0 || viewRotationDegrees == 180;

        // Coordinates of the view's center after the `end` translation
        final int targetCenterX;
        final int targetCenterY;
        if (isPortrait) {
            targetCenterX = endX - (scaledWidth / 2);
            targetCenterY = endY - (scaledHeight / 2);
        } else {
            targetCenterX = endX - (scaledHeight / 2);
            targetCenterY = endY - (scaledWidth / 2);
        }

        // Current coordinates of the view's center
        final int currentCenterX = view.getWidth() / 2;
        final int currentCenterY = view.getHeight() / 2;

        final int transX = reverseIfRTLLayout(view, targetCenterX - currentCenterX);
        final int transY = targetCenterY - currentCenterY;
        return new TranslationTransformation(transX, transY);
    }

    /**
     * Reverses a horizontal translation if the {@code view} is in a right-to-left (RTL) layout.
     *
     * @return The passed in horizontal translation if the layout is left-to-right (LTR), or its
     * reverse if the layout is right-to-left (RTL).
     */
    private static int reverseIfRTLLayout(@NonNull final View view, int transX) {
        final boolean isRTLDirection = view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        return isRTLDirection ? -transX : transX;
    }
}
