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

package androidx.camera.view;

import android.util.Size;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Preview;
import androidx.camera.view.preview.transform.PreviewTransform;

/**
 * Wraps the underlying handling of the {@link android.view.Surface} used for preview, which is
 * done using either a {@link android.view.TextureView} (see {@link TextureViewImplementation})
 * or a {@link android.view.SurfaceView} (see {@link SurfaceViewImplementation}).
 */
abstract class PreviewViewImplementation {

    private static final PreviewView.ScaleType DEFAULT_SCALE_TYPE =
            PreviewView.ScaleType.FILL_CENTER;

    @Nullable
    Size mResolution;

    @Nullable
    FrameLayout mParent;

    @NonNull
    private PreviewView.ScaleType mScaleType = DEFAULT_SCALE_TYPE;

    abstract void initializePreview();

    @Nullable
    abstract View getPreview();

    /** Gets the {@link Preview.SurfaceProvider} to be used with {@link Preview}. */
    @NonNull
    abstract Preview.SurfaceProvider getSurfaceProvider();

    /**
     * Initializes the parent view
     *
     * @param parent the containing parent {@link FrameLayout}.
     */
    void init(@NonNull FrameLayout parent) {
        mParent = parent;
    }

    /** Returns the {@link PreviewView.ScaleType} currently applied to the preview. */
    @NonNull
    PreviewView.ScaleType getScaleType() {
        return mScaleType;
    }

    @Nullable
    public Size getResolution() {
        return mResolution;
    }

    /**
     * Invoked when the display properties have changed.
     *
     * <p>Corrects and adjusts the preview using the latest display properties such as the display
     * orientation.
     */
    void onDisplayChanged() {
        applyCurrentScaleType();
    }

    /** Invoked after a {@link android.view.Surface} has been provided to the camera for preview. */
    void onSurfaceProvided() {
        applyCurrentScaleType();
    }

    /**
     * Applies a {@link PreviewView.ScaleType} to the preview.
     *
     * @see PreviewView#setScaleType(PreviewView.ScaleType)
     */
    void setScaleType(@NonNull final PreviewView.ScaleType scaleType) {
        mScaleType = scaleType;
        applyCurrentScaleType();
    }

    private void applyCurrentScaleType() {
        final View preview = getPreview();
        if (mParent != null && preview != null && mResolution != null) {
            PreviewTransform.applyScaleType(mParent, preview, mResolution, mScaleType);
        }
    }
}
