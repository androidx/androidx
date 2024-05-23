/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.activity;


import static androidx.camera.testing.impl.SurfaceTextureProvider.createSurfaceTextureProvider;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Size;
import android.view.TextureView;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.internal.CameraUseCaseAdapter;
import androidx.camera.testing.R;
import androidx.camera.testing.impl.CameraUtil;
import androidx.camera.testing.impl.CameraXUtil;
import androidx.camera.testing.impl.SurfaceTextureProvider;
import androidx.test.espresso.idling.CountingIdlingResource;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

/** An Activity which starts CameraX preview for testing.
 *
 * <p>This Activity is intended to test the behavior of CameraX when an Activity with an
 * active camera session goes into the background and returns to the foreground.
 * Please retain active UseCases even when the Activity is paused. The camera is expected to resume
 * automatically when the Activity returns to the foreground.
 */
public class CameraXTestActivity extends AppCompatActivity {

    private static final String TAG = "CameraXTestActivity";
    private static final int FRAMES_UNTIL_VIEW_IS_READY = 5;

    @Nullable
    private Preview mPreview;
    @Nullable
    private String mCameraId = null;
    @Nullable
    private CameraUseCaseAdapter mCameraUseCaseAdapter = null;
    final CountingIdlingResource mPreviewReady = new CountingIdlingResource("PreviewReady");
    final CountingIdlingResource mResumeIdlingCounting =
            new CountingIdlingResource("ActivityResume");

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
        Logger.i(TAG, "Got UseCase: " + mPreview);
        if (!mResumeIdlingCounting.isIdleNow()) {
            mResumeIdlingCounting.decrement();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumeIdlingCounting.increment();
    }

    private void enablePreview() {
        for (int i = 0; i < FRAMES_UNTIL_VIEW_IS_READY; i++) {
            mPreviewReady.increment();
        }

        if (mCameraUseCaseAdapter != null) {
            Logger.d(TAG, "Preview already enabled");
            return;
        }

        int lensFacing;
        if (CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK)) {
            lensFacing = CameraSelector.LENS_FACING_BACK;
        } else if (CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
            lensFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            throw new IllegalArgumentException("Cannot find camera to use");
        }
        final CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(
                lensFacing).build();

        mPreview = new Preview.Builder()
                .setTargetName("Preview")
                .build();

        final TextureView textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(
                new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture,
                            int width, int height) {
                        Logger.d(TAG, "SurfaceTexture available");
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture,
                            int width, int height) {
                        Logger.d(TAG, "SurfaceTexture size changed " + width + "x" + height);
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(
                            @NonNull SurfaceTexture surfaceTexture) {
                        Logger.d(TAG, "SurfaceTexture destroyed");
                        return true;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
                        // Wait until surface texture receives enough updates.
                        if (!mPreviewReady.isIdleNow()) {
                            mPreviewReady.decrement();
                        }
                    }
                });

        mPreview.setSurfaceProvider(
                createSurfaceTextureProvider(new SurfaceTextureCallbackImpl(textureView)));

        try {
            final CameraX cameraX = CameraXUtil.getOrCreateInstance(this, null).get();
            final CameraInternal camera =
                    cameraSelector.select(cameraX.getCameraRepository().getCameras());
            mCameraUseCaseAdapter = new CameraUseCaseAdapter(camera,
                    cameraX.getCameraFactory().getCameraCoordinator(),
                    cameraX.getCameraDeviceSurfaceManager(),
                    cameraX.getDefaultConfigFactory());
            mCameraUseCaseAdapter.addUseCases(Collections.singleton(mPreview));
        } catch (InterruptedException | ExecutionException e) {
            Logger.e(TAG, "Error when adding UseCases: " + e.getMessage(), e);
            return;
        } catch (CameraUseCaseAdapter.CameraException e) {
            Logger.e(TAG, "CameraException when adding UseCases: " + e.getMessage(), e);
            mCameraUseCaseAdapter = null;
            mPreview = null;
            return;
        }

        mCameraId = CameraUtil.getCameraIdWithLensFacing(cameraSelector.getLensFacing());
    }

    @Nullable
    public String getCameraId() {
        return mCameraId;
    }

    @VisibleForTesting
    @NonNull
    public CountingIdlingResource getPreviewReady() {
        return mPreviewReady;
    }

    @VisibleForTesting
    @NonNull
    public CountingIdlingResource getActivityResumeIdlingCounting() {
        return mResumeIdlingCounting;
    }

    /** Unbind UseCases if any */
    @VisibleForTesting
    public void cleanup() {
        if (mPreview != null && mCameraUseCaseAdapter != null) {
            mCameraUseCaseAdapter.removeUseCases(Collections.singleton(mPreview));
            mPreview = null;
            mCameraUseCaseAdapter = null;
        }
    }

    private static final class SurfaceTextureCallbackImpl implements
            SurfaceTextureProvider.SurfaceTextureCallback {
        private final TextureView mTextureView;

        SurfaceTextureCallbackImpl(@NonNull TextureView textureView) {
            mTextureView = textureView;
        }

        @Override
        public void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture,
                @NonNull Size resolution) {
            ViewGroup viewGroup = (ViewGroup) mTextureView.getParent();
            viewGroup.removeView(mTextureView);
            viewGroup.addView(mTextureView, resolution.getWidth(),
                    resolution.getHeight());
            mTextureView.setSurfaceTexture(surfaceTexture);
        }

        @Override
        public void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture) {
            surfaceTexture.release();
        }
    }
}
