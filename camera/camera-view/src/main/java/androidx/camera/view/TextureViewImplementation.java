/*
 * Copyright 2019 The Android Open Source Project
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

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewUtil;

/**
 * The {@link TextureView} implementation for {@link PreviewView}
 */
public class TextureViewImplementation implements PreviewView.Implementation {

    private static final String TAG = "TextureViewImpl";

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    TextureView mTextureView;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(@NonNull FrameLayout parent) {
        mTextureView = new TextureView(parent.getContext());
        mTextureView.setLayoutParams(
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
        parent.addView(mTextureView);
    }

    @NonNull
    @Override
    public Preview.PreviewSurfaceCallback getPreviewSurfaceCallback() {
        return PreviewUtil.createPreviewSurfaceCallback(new PreviewUtil.SurfaceTextureCallback() {
            @Override
            public void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture) {
                Log.d(TAG, "onSurfaceTextureReady");
                final ViewGroup parent = (ViewGroup) mTextureView.getParent();
                parent.removeView(mTextureView);
                parent.addView(mTextureView);
                mTextureView.setSurfaceTexture(surfaceTexture);
            }

            @Override
            public void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture) {
                Log.d(TAG, "onSafeToRelease");
                surfaceTexture.release();
            }
        });
    }
}
