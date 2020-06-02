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
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.view.PreviewView;
import androidx.camera.view.preview.transform.transformation.Transformation;

/**
 * Transforms the camera preview using a supported {@link PreviewView.ScaleType}.
 * <p>
 * Holds a reference to the {@link PreviewView.ScaleType} that's being applied to the preview.
 * This attribute is used by both {@link PreviewView} (see {@link PreviewView#getScaleType()} and
 * {@link PreviewView#setScaleType(PreviewView.ScaleType)}) and its implementation (when
 * correcting the preview and updating the {@link PreviewView.ScaleType}).
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class PreviewTransform {

    private static final PreviewView.ScaleType DEFAULT_SCALE_TYPE =
            PreviewView.ScaleType.FILL_CENTER;

    @NonNull
    private PreviewView.ScaleType mScaleType = DEFAULT_SCALE_TYPE;

    @Nullable
    private Transformation mCurrentTransformation;

    private boolean mSensorDimensionFlipNeeded = true;

    private int mDeviceRotation = RotationTransform.ROTATION_AUTOMATIC;

    /** Returns the {@link PreviewView.ScaleType} currently applied to the preview. */
    @NonNull
    public PreviewView.ScaleType getScaleType() {
        return mScaleType;
    }

    /** Sets the {@link PreviewView.ScaleType} to be applied to the preview. */
    public void setScaleType(@NonNull PreviewView.ScaleType scaleType) {
        mScaleType = scaleType;
    }

    /**
     * Returns the current {@link Transformation} applied to the preview, or {@code null} if the
     * preview hasn't started yet.
     */
    @Nullable
    public Transformation getCurrentTransformation() {
        return mCurrentTransformation;
    }

    /** Returns whether the device sensor x and y dimensions need to be flipped. */
    public boolean isSensorDimensionFlipNeeded() {
        return mSensorDimensionFlipNeeded;
    }

    /**
     * Sets whether the target device sensor dimensions need to be flipped when calculating the
     * transform.
     */
    public void setSensorDimensionFlipNeeded(boolean sensorDimensionFlipNeeded) {
        mSensorDimensionFlipNeeded = sensorDimensionFlipNeeded;
    }

    /** Returns current device rotation value. */
    public int getDeviceRotation() {
        return mDeviceRotation;
    }

    /**
     * Sets the device rotation value that will affect the transform calculations.
     */
    public void setDeviceRotation(@ImageOutputConfig.RotationValue int deviceRotation) {
        mDeviceRotation = deviceRotation;
    }

    /** Applies the current {@link PreviewView.ScaleType} on the passed in preview. */
    public void applyCurrentScaleType(@NonNull final View container, @NonNull final View view,
            @NonNull final Size bufferSize) {
        resetPreview(view);
        correctPreview(container, view, bufferSize);
        applyScaleTypeInternal(container, view, mScaleType, mDeviceRotation);
    }

    private void resetPreview(@NonNull View view) {
        final Transformation reset = new Transformation();
        applyTransformation(view, reset);
    }

    /** Corrects the preview. */
    private void correctPreview(@NonNull final View container, @NonNull final View view,
            @NonNull final Size bufferSize) {
        final Transformation correct = PreviewCorrector.getCorrectionTransformation(container, view,
                bufferSize, mSensorDimensionFlipNeeded, mDeviceRotation);
        applyTransformation(view, correct);
    }

    /** Applies the specified {@link PreviewView.ScaleType} on top of the corrected preview. */
    private void applyScaleTypeInternal(@NonNull final View container,
            @NonNull final View view, @NonNull final PreviewView.ScaleType scaleType,
            final int deviceRotation) {
        final Transformation current = getTransformation(view);
        final Transformation transformation = ScaleTypeTransform.getTransformation(container, view,
                scaleType, deviceRotation);
        applyTransformation(view, current.add(transformation));
    }

    /**
     * Applies a {@link Transformation} on the passed in preview while overriding any previous
     * preview {@linkplain Transformation transformations}
     */
    private void applyTransformation(@NonNull final View view,
            @NonNull final Transformation transformation) {
        view.setX(0);
        view.setY(0);
        view.setScaleX(transformation.getScaleX());
        view.setScaleY(transformation.getScaleY());
        view.setTranslationX(transformation.getTransX());
        view.setTranslationY(transformation.getTransY());
        view.setRotation(transformation.getRotation());

        mCurrentTransformation = transformation;
    }
}
