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

import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * The SurfaceView implementation for {@link CameraPreviewView}.
 */
@RequiresApi(21)
final class SurfaceViewImplementation extends PreviewViewImplementation {

    private static final String TAG = "SurfaceViewImpl";

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    @Nullable private SurfaceView mSurfaceView;

    SurfaceViewImplementation(@NonNull FrameLayout parent,
            @NonNull PreviewTransformation previewTransform) {
        super(parent, previewTransform);
    }

    @Nullable
    @Override
    View getPreview() {
        return mSurfaceView;
    }
}
