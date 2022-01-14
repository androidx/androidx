/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.previewview;

import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Wraps the underlying handling of the {@link android.view.Surface} used for preview, which is
 * done using either a {@link android.view.TextureView} (see {@link TextureViewImplementation})
 * or a {@link android.view.SurfaceView} (see {@link SurfaceViewImplementation}).
 */
@RequiresApi(21)
abstract class PreviewViewImplementation {

    @NonNull
    FrameLayout mParent;

    @SuppressWarnings("unused")
    @NonNull
    private final PreviewTransformation mPreviewTransform;

    @Nullable
    abstract View getPreview();

    PreviewViewImplementation(@NonNull FrameLayout parent,
            @NonNull PreviewTransformation previewTransform) {
        mParent = parent;
        mPreviewTransform = previewTransform;
    }

    /**
     * Invoked when the preview needs to be adjusted, either because the layout bounds of the
     * preview's container {@link CameraPreviewView} have changed, or the
     * {@link CameraPreviewView.ScaleType} has changed.
     * <p>
     * Corrects and adjusts the preview using the latest {@link CameraPreviewView.ScaleType} and
     * display properties such as the display orientation and size.
     */
    void redrawPreview() {}
}
