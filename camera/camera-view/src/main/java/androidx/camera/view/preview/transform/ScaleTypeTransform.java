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
import android.view.Surface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.camera.view.PreviewView;
import androidx.camera.view.preview.transform.transformation.ScaleTransformation;
import androidx.camera.view.preview.transform.transformation.Transformation;
import androidx.camera.view.preview.transform.transformation.TranslationTransformation;

final class ScaleTypeTransform {

    private ScaleTypeTransform() {
    }

    /**
     * Converts a {@link PreviewView.ScaleType} to a {@link Transformation}.
     *
     * @param container      Preview container
     * @param view           Preview view (a {@link android.view.TextureView} or
     *                       {@link android.view.SurfaceView})
     * @param scaleType      The desired {@link PreviewView.ScaleType}.
     * @param deviceRotation If the app is not running in remote display mode, set the parameter
     *                       as {@link RotationTransform#ROTATION_AUTOMATIC}. Then, the rotation
     *                       value queried from the preview will be used to do the transformation
     *                       calculations. If the app is running in remote display mode, the
     *                       device rotation value needs to be provided to make the result be
     *                       rotated into correct orientation. The device rotation should be
     *                       obtained from {@link android.view.OrientationEventListener} and
     *                       needs to be converted into {@link Surface#ROTATION_0},
     *                       {@link Surface#ROTATION_90}, {@link Surface#ROTATION_180}, or
     *                       {@link Surface#ROTATION_270}.
     */
    static Transformation getTransformation(@NonNull final View container, @NonNull final View view,
            @NonNull final PreviewView.ScaleType scaleType, final int deviceRotation) {
        final Transformation scale = getScale(container, view, scaleType, deviceRotation);

        // Use the current preview scale AND the scale that's about to be applied to it to figure
        // out how to position the preview in its container
        final Pair<Float, Float> scaleXY = new Pair<>(view.getScaleX() * scale.getScaleX(),
                view.getScaleY() * scale.getScaleY());
        final Transformation translation = getScaledTranslation(container, view, scaleType,
                scaleXY, deviceRotation);

        return scale.add(translation);
    }

    private static ScaleTransformation getScale(@NonNull final View container,
            @NonNull final View view, @NonNull final PreviewView.ScaleType scaleType,
            final int deviceRotation) {
        switch (scaleType) {
            case FILL_START:
            case FILL_CENTER:
            case FILL_END:
                return ScaleTransform.fill(container, view, deviceRotation);
            case FIT_START:
            case FIT_CENTER:
            case FIT_END:
                return ScaleTransform.fit(container, view, deviceRotation);
            default:
                throw new IllegalArgumentException("Unknown scale type " + scaleType);
        }
    }

    private static TranslationTransformation getScaledTranslation(@NonNull final View container,
            @NonNull final View view, @NonNull final PreviewView.ScaleType scaleType,
            @NonNull final Pair<Float, Float> scaleXY, final int deviceRotation) {
        switch (scaleType) {
            case FILL_START:
            case FIT_START:
                return TranslationTransform.start(view, scaleXY, deviceRotation);
            case FILL_CENTER:
            case FIT_CENTER:
                return TranslationTransform.center(container, view);
            case FILL_END:
            case FIT_END:
                return TranslationTransform.end(container, view, scaleXY, deviceRotation);
            default:
                throw new IllegalArgumentException("Unknown scale type " + scaleType);
        }
    }
}
