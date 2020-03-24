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
 * Computes the x and y coordinates of the top left corner of the preview in order to position it
 * at the start (top left), center or end (bottom right) of its parent.
 */
final class TranslationTransform {

    private TranslationTransform() {
    }

    /**
     * Computes the x and y coordinates of the top left corner of {@code view} so that it's
     * aligned to the top left corner of its parent {@code container}.
     */
    static TranslationTransformation start(@NonNull final View view,
            @NonNull final Pair<Float, Float> scaleXY) {
        if (view.getWidth() == 0 || view.getHeight() == 0) {
            return new TranslationTransformation(0, 0);
        }

        // Scaled width and height of the view
        final int scaledWidth = (int) (view.getWidth() * scaleXY.first);
        final int scaledHeight = (int) (view.getHeight() * scaleXY.second);

        final int viewRotationDegrees = (int) RotationTransform.getRotationDegrees(view);
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

        final int transX = targetCenterX - currentCenterX;
        final int transY = targetCenterY - currentCenterY;
        return new TranslationTransformation(transX, transY);
    }

    /**
     * Computes the x and y coordinates of the top left corner of {@code view} so that it's
     * centered in its parent {@code container}.
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

        final int transX = targetCenterX - currentCenterX;
        final int transY = targetCenterY - currentCenterY;
        return new TranslationTransformation(transX, transY);
    }

    /**
     * Computes the x and y coordinates of the top left corner of {@code view} so that it's
     * aligned to the bottom right corner of its parent {@code container}.
     */
    static TranslationTransformation end(@NonNull final View container, @NonNull final View view,
            @NonNull final Pair<Float, Float> scaleXY) {
        if (view.getWidth() == 0 || view.getHeight() == 0) {
            return new TranslationTransformation(0, 0);
        }

        // Coordinates of the bottom right corner of the container
        final int endX = container.getWidth();
        final int endY = container.getHeight();

        // Scaled width and height of the view
        final int scaledWidth = (int) (view.getWidth() * scaleXY.first);
        final int scaledHeight = (int) (view.getHeight() * scaleXY.second);

        final int viewRotationDegrees = (int) RotationTransform.getRotationDegrees(view);
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

        final int transX = targetCenterX - currentCenterX;
        final int transY = targetCenterY - currentCenterY;
        return new TranslationTransformation(transX, transY);
    }
}
