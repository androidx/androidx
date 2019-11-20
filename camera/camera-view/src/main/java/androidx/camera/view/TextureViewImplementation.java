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

import static androidx.camera.view.ScaleTypeTransform.transformCenterCrop;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.Futures;

/**
 * The {@link TextureView} implementation for {@link PreviewView}
 */
public class TextureViewImplementation implements PreviewView.Implementation {
    private static final String TAG = "TextureViewImpl";

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    TextureView mTextureView;

    private SurfaceTextureReleaseBlockingListener mSurfaceTextureListener;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(@NonNull FrameLayout parent) {
        mTextureView = new TextureView(parent.getContext());
        mTextureView.setLayoutParams(
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        // Access the setting of the SurfaceTexture safely through the listener instead of
        // directly on the TextureView
        mSurfaceTextureListener = new SurfaceTextureReleaseBlockingListener(mTextureView);

        parent.addView(mTextureView);
    }

    @NonNull
    @Override
    public Preview.PreviewSurfaceProvider getPreviewSurfaceProvider() {
        return (resolution, surfaceReleaseFuture) -> {
            // Create the SurfaceTexture. Using a FixedSizeSurfaceTexture, because the
            // TextureView might try to change the size of the SurfaceTexture if layout has not
            // yet completed.
            // TODO(b/144807315) Remove when a solution to TextureView calling
            //  setDefaultBufferSize() to a resolution not supported by camera2
            SurfaceTexture surfaceTexture = new FixedSizeSurfaceTexture(0, resolution);
            surfaceTexture.detachFromGLContext();
            Surface surface = new Surface(surfaceTexture);

            Display display = ((WindowManager) mTextureView.getContext().getSystemService(
                            Context.WINDOW_SERVICE)).getDefaultDisplay();

            // Setup the TextureView for the correct transformation
            Matrix matrix = transformCenterCrop(resolution, mTextureView, display.getRotation());
            mTextureView.setTransform(matrix);

            final ViewGroup parent = (ViewGroup) mTextureView.getParent();
            parent.removeView(mTextureView);
            parent.addView(mTextureView);

            // Set the SurfaceTexture safely instead of directly calling
            // mTextureView.setSurfaceTexture(surfaceTexture);
            mSurfaceTextureListener.setSurfaceTextureSafely(surfaceTexture, surfaceReleaseFuture);
            surfaceReleaseFuture.addListener(surface::release, CameraXExecutors.directExecutor());

            return Futures.immediateFuture(surface);
        };
    }
}
