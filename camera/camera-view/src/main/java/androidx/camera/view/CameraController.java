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

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.util.Size;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalUseCaseGroup;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.lifecycle.ProcessCameraProvider;

/**
 * The abstract base camera controller class.
 *
 * <p> The controller is a high level API manages the entire CameraX stack. This base class is
 * responsible for 1) initializing camera stack and 2) creating use cases based on user inputs.
 * Subclass this class to bind the use cases to camera.
 */
abstract class CameraController {

    private static final String TAG = "CameraController";

    // TODO(b/148791439): Temporary. Remove once camera selection is implemented.
    static final CameraSelector CAMERA_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    // CameraController and PreviewView hold reference to each other. The 2-way link is managed
    // by PreviewView.
    @Nullable
    Preview mPreview;

    // Size of the PreviewView. Used for creating ViewPort.
    @Nullable
    private Size mPreviewSize;

    // The latest bound camera.
    @Nullable
    Camera mCamera;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable
    ProcessCameraProvider mCameraProvider;

    CameraController(@NonNull Context context) {
        // Wait for camera to be initialized before binding use cases.
        Futures.addCallback(
                ProcessCameraProvider.getInstance(context),
                new FutureCallback<ProcessCameraProvider>() {

                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(@Nullable ProcessCameraProvider provider) {
                        mCameraProvider = provider;
                        mCamera = startCamera();
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        // TODO(b/148791439): fail gracefully and notify caller.
                        throw new RuntimeException("CameraX failed to initialize.", t);
                    }

                }, CameraXExecutors.mainThreadExecutor());
    }

    /**
     * Implemented by children to refresh after {@link UseCase} is changed.
     */
    @Nullable
    abstract Camera startCamera();

    // Preview use case.

    /**
     * Internal API used by {@link PreviewView} notify changes.
     *
     * TODO(b/148791439): add LayoutDirection
     */
    @SuppressLint("MissingPermission")
    @MainThread
    void attachPreviewSurface(Preview.SurfaceProvider surfaceProvider, int width, int height) {
        Threads.checkMainThread();
        if (width == 0 || height == 0) {
            return;
        }
        Size newPreviewSize = new Size(width, height);
        if (newPreviewSize.equals(mPreviewSize) && mPreview != null) {
            // If the Surface size hasn't changed, reuse the UseCase with the new SurfaceProvider.
            mPreview.setSurfaceProvider(surfaceProvider);
            return;
        }
        if (mPreview != null && mCameraProvider != null) {
            mCameraProvider.unbind(mPreview);
        }
        mPreview = createPreview(surfaceProvider, newPreviewSize);
        mPreviewSize = newPreviewSize;
        mCamera = startCamera();
    }

    /**
     * Clear {@link PreviewView} to remove the UI reference.
     */
    @MainThread
    void clearPreviewSurface() {
        Threads.checkMainThread();
        if (mCameraProvider != null) {
            // Preview is required. Unbind everything if Preview is down.
            mCameraProvider.unbindAll();
        }
        mPreviewSize = null;
        mPreview = null;
        mCamera = null;
    }

    @MainThread
    Preview createPreview(Preview.SurfaceProvider surfaceProvider, Size previewSize) {
        Threads.checkMainThread();
        Preview preview = new Preview.Builder()
                .setTargetResolution(previewSize)
                .build();
        preview.setSurfaceProvider(surfaceProvider);
        return preview;
    }

    // TODO(b/148791439): Support ImageCapture.

    // TODO(b/148791439): Support VideoCapture as @Experimental.

    // TODO(b/148791439): Allow user to select camera.

    // TODO(b/148791439): Handle rotation so the output is always in gravity orientation.

    // TODO(b/148791439): Support CameraControl

    /**
     * Creates {@link UseCaseGroup} from all the use cases.
     *
     * <p> Preview is required. If it is null, then controller is not ready. Return null and ignore
     * other use cases.
     */
    @UseExperimental(markerClass = ExperimentalUseCaseGroup.class)
    protected UseCaseGroup createUseCaseGroup() {
        UseCaseGroup.Builder builder = new UseCaseGroup.Builder();
        if (mPreview == null) {
            Log.d(TAG, "PreviewView is not ready.");
            return null;
        }
        builder.addUseCase(mPreview);

        // TODO(b/148791439): add all the use cases.
        // TODO(b/148791439): set ViewPort if mPreviewSize/ LayoutDirection is not null.
        return builder.build();
    }
}
