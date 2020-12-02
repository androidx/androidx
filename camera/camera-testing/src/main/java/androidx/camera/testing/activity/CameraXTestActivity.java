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
import androidx.camera.testing.CameraUtil;
import androidx.camera.testing.R;
import androidx.camera.testing.SurfaceTextureProvider;
import androidx.test.espresso.idling.CountingIdlingResource;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutionException;

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

    CameraUseCaseAdapter mCameraUseCaseAdapter = null;

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
        Logger.i(TAG, "Got UseCase: " + mPreview);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null && mCameraUseCaseAdapter != null) {
            mCameraUseCaseAdapter.removeUseCases(Collections.singleton(mPreview));
            mPreview = null;
            mCameraUseCaseAdapter = null;
        }
    }

    void enablePreview() {
        for (int i = 0; i < FRAMES_UNTIL_VIEW_IS_READY; i++) {
            mPreviewReady.increment();
        }

        if (mCameraUseCaseAdapter != null) {
            Logger.d(TAG, "Preview already enabled");
            return;
        }

        if (CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_BACK)) {
            mLensFacing = CameraSelector.LENS_FACING_BACK;
        } else if (CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
            mLensFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            throw new IllegalArgumentException("Cannot find camera to use");
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
            CameraX cameraX = CameraX.getOrCreateInstance(this).get();
            LinkedHashSet<CameraInternal> cameras =
                    cameraSelector.filter(cameraX.getCameraRepository().getCameras());
            mCameraUseCaseAdapter = new CameraUseCaseAdapter(cameras,
                    cameraX.getCameraDeviceSurfaceManager(), cameraX.getDefaultConfigFactory());
            mCameraUseCaseAdapter.addUseCases(Collections.singleton(mPreview));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return;
        } catch (CameraUseCaseAdapter.CameraException e) {
            mCameraUseCaseAdapter = null;
            mPreview = null;
            return;
        }

        mCameraId = CameraX.getCameraWithCameraSelector(
                cameraSelector).getCameraInfoInternal().getCameraId();

        textureView.setSurfaceTextureListener(
                new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(
                            @NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(
                            @NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(
                            @NonNull SurfaceTexture surfaceTexture) {
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
    }

}
