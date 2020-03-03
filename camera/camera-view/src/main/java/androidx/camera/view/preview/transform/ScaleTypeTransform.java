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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.view.PreviewView;
import androidx.camera.view.preview.transform.transformation.Transformation;
import androidx.camera.view.preview.transform.transformation.TranslationTransformation;

final class ScaleTypeTransform {

    private ScaleTypeTransform() {
    }

    /**
     * Converts a {@link PreviewView.ScaleType} to a {@link Transformation}.
     */
    static Transformation getTransformation(@NonNull final View container, @NonNull final View view,
            @NonNull final Size bufferSize, @NonNull final PreviewView.ScaleType scaleType) {
        final Pair<Float, Float> scaleXY = getScaleXY(container, view, bufferSize, scaleType);
        final Transformation translation = getScaledTranslation(container, view, scaleType,
                scaleXY);
        final float rotation = -RotationTransform.getRotationDegrees(view);
        return new Transformation(scaleXY.first, scaleXY.second, translation.getTransX(),
                translation.getTransY(), rotation);
    }

    private static Pair<Float, Float> getScaleXY(@NonNull final View container,
            @NonNull final View view, @NonNull final Size bufferSize,
            final PreviewView.ScaleType scaleType) {
        switch (scaleType) {
            case FILL_START:
            case FILL_CENTER:
            case FILL_END:
                return ScaleTransform.fill(container, view, bufferSize);
            case FIT_START:
            case FIT_CENTER:
            case FIT_END:
                return ScaleTransform.fit(container, view, bufferSize);
            default:
                throw new IllegalArgumentException("Unknown scale type " + scaleType);
        }
    }

    private static TranslationTransformation getScaledTranslation(@NonNull final View container,
            @NonNull final View view, @NonNull final PreviewView.ScaleType scaleType,
            @NonNull final Pair<Float, Float> scaleXY) {
        switch (scaleType) {
            case FILL_START:
            case FIT_START:
                return TranslationTransform.start(view, scaleXY);
            case FILL_CENTER:
            case FIT_CENTER:
                return TranslationTransform.center(container, view);
            case FILL_END:
            case FIT_END:
                return TranslationTransform.end(container, view, scaleXY);
            default:
                throw new IllegalArgumentException("Unknown scale type " + scaleType);
        }
    }
}
