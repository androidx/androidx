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
import androidx.camera.view.PreviewView;
import androidx.camera.view.preview.transform.transformation.ScaleTransformation;
import androidx.camera.view.preview.transform.transformation.Transformation;
import androidx.camera.view.preview.transform.transformation.TranslationTransformation;

final class ScaleTypeTransform {

    private ScaleTypeTransform() {
    }

    /** Converts a {@link PreviewView.ScaleType} to a {@link Transformation} */
    static Transformation getTransformation(@NonNull final View container, @NonNull final View view,
            @NonNull final PreviewView.ScaleType scaleType) {
        final Transformation scale = getScale(container, view, scaleType);

        // Use the current preview scale AND the scale that's about to be applied to it to figure
        // out how to position the preview in its container
        final Pair<Float, Float> scaleXY = new Pair<>(view.getScaleX() * scale.getScaleX(),
                view.getScaleY() * scale.getScaleY());
        final Transformation translation = getScaledTranslation(container, view, scaleType,
                scaleXY);

        return scale.add(translation);
    }

    private static ScaleTransformation getScale(@NonNull final View container,
            @NonNull final View view,
            final PreviewView.ScaleType scaleType) {
        switch (scaleType) {
            case FILL_START:
            case FILL_CENTER:
            case FILL_END:
                return ScaleTransform.fill(container, view);
            case FIT_START:
            case FIT_CENTER:
            case FIT_END:
                return ScaleTransform.fit(container, view);
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
