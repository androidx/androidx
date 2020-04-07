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

package androidx.camera.testing.activity;


import static androidx.camera.testing.SurfaceTextureProvider.createSurfaceTextureProvider;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.R;
import androidx.camera.testing.SurfaceTextureProvider;
import androidx.test.espresso.idling.CountingIdlingResource;

/** An activity which starts CameraX preview for testing. */
public class CameraXTestActivity extends AppCompatActivity {

    private static final String TAG = "CameraXTestActivity";
    private static final int FRAMES_UNTIL_VIEW_IS_READY = 5;
    @Nullable
    private Preview mPreview;
    @Nullable
    public String mCameraId = null;
    @CameraSelector.LensFacing
    public int mLensFacing = CameraSelector.LENS_FACING_BACK;

    @VisibleForTesting
    public final CountingIdlingResource mPreviewReady = new CountingIdlingResource("PreviewReady");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        setContentView(R.layout.activity_camera_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        enablePreview();
        Log.i(TAG, "Got UseCase: " + mPreview);
    }

    void enablePreview() {
        for (int i = 0; i < FRAMES_UNTIL_VIEW_IS_READY; i++) {
            mPreviewReady.increment();
        }

        if (CameraX.isBound(mPreview)) {
            Log.d(TAG, "Preview already bound");
            return;
        }

        if (!CameraUtil.hasCameraWithLensFacing(mLensFacing)) {
            try {
                mLensFacing = CameraX.getDefaultLensFacing();
            } catch (IllegalStateException e) {
                throw new IllegalArgumentException("Cannot find camera to use", e);
            }
        }

        mPreview = new Preview.Builder()
                .setTargetName("Preview")
                .build();
        TextureView textureView = findViewById(R.id.textureView);
        mPreview.setSurfaceProvider(createSurfaceTextureProvider(
                new SurfaceTextureProvider.SurfaceTextureCallback() {
                    @Override
                    public void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture,
                            @NonNull Size resolution) {
                        ViewGroup viewGroup = (ViewGroup) textureView.getParent();
                        viewGroup.removeView(textureView);
                        viewGroup.addView(textureView);
                        textureView.setSurfaceTexture(surfaceTexture);
                    }

                    @Override
                    public void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture) {
                        surfaceTexture.release();
                    }
                }));


        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(mLensFacing).build();
        try {
            CameraX.bindToLifecycle(this, cameraSelector, mPreview);
        } catch (IllegalArgumentException e) {
            mPreview = null;
            return;
        }

        mCameraId = CameraX.getCameraWithCameraSelector(
                cameraSelector).getCameraInfoInternal().getCameraId();

        textureView.setSurfaceTextureListener(
                new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(
                            SurfaceTexture surfaceTexture, int i, int i1) {
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(
                            SurfaceTexture surfaceTexture, int i, int i1) {
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                        return true;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                        // Wait until surface texture receives enough updates.
                        if (!mPreviewReady.isIdleNow()) {
                            mPreviewReady.decrement();
                        }
                    }
                });
    }

}
