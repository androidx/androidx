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

import static androidx.camera.view.preview.transform.transformation.Transformation.getTransformation;

import android.util.Size;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.view.PreviewView;
import androidx.camera.view.preview.transform.transformation.Transformation;

/**
 * Transforms the camera preview using a supported {@link PreviewView.ScaleType}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class PreviewTransform {

    private PreviewTransform() {
    }

    /** Applies the specified {@link PreviewView.ScaleType} on the passed in preview. */
    public static void applyScaleType(@NonNull final View container, @NonNull final View view,
            @NonNull final Size bufferSize, @NonNull PreviewView.ScaleType scaleType) {
        resetPreview(view);
        correctPreview(container, view, bufferSize);
        applyScaleTypeInternal(container, view, scaleType);
    }

    private static void resetPreview(@NonNull View view) {
        final Transformation reset = new Transformation();
        applyTransformation(view, reset);
    }

    /** Corrects the preview. */
    private static void correctPreview(@NonNull final View container, @NonNull final View view,
            @NonNull final Size bufferSize) {
        final Transformation correct = PreviewCorrector.getCorrectionTransformation(container, view,
                bufferSize);
        applyTransformation(view, correct);
    }

    /** Applies the specified {@link PreviewView.ScaleType} on top of the corrected preview. */
    private static void applyScaleTypeInternal(@NonNull final View container,
            @NonNull final View view, @NonNull final PreviewView.ScaleType scaleType) {
        final Transformation current = getTransformation(view);
        final Transformation transformation = ScaleTypeTransform.getTransformation(container, view,
                scaleType);
        applyTransformation(view, current.add(transformation));
    }

    /**
     * Applies a {@link Transformation} on the passed in preview while overriding any previous
     * preview {@linkplain Transformation transformations}
     */
    private static void applyTransformation(@NonNull final View view,
            @NonNull final Transformation transformation) {
        view.setX(0);
        view.setY(0);
        view.setScaleX(transformation.getScaleX());
        view.setScaleY(transformation.getScaleY());
        view.setTranslationX(transformation.getTransX());
        view.setTranslationY(transformation.getTransY());
        view.setRotation(transformation.getRotation());
    }
}
