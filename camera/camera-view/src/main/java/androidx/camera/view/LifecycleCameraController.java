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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A controller that provides most of the CameraX features.
 *
 * <p> This a high level controller that provides most of the CameraX core features
 * in a single class. It handles camera initialization, creates and configures {@link UseCase}s,
 * and bind them to a {@link LifecycleOwner} when ready. It also listens to device motion sensor
 * and set the target rotation for the use cases.
 *
 * <p> Code sample:
 * <pre><code>
 *     // Setup.
 *     CameraController controller = new LifecycleCameraController(context);
 *     controller.bindToLifecycle(lifecycleOwner);
 *     PreviewView previewView = findViewById(R.id.preview_view);
 *     previewView.setController(controller);
 *
 *     // Use case features
 *     controller.takePicture(...);
 *
 *     // Camera control features
 *     controller.setZoomRatio(.5F);
 * </code></pre>
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class LifecycleCameraController extends CameraController {

    private static final String TAG = "CamLifecycleController";

    @Nullable
    private LifecycleOwner mLifecycleOwner;

    public LifecycleCameraController(@NonNull Context context) {
        super(context);
    }

    @VisibleForTesting
    LifecycleCameraController(@NonNull Context context,
            @NonNull ListenableFuture<ProcessCameraProviderWrapper> cameraProviderFuture) {
        super(context, cameraProviderFuture);
    }

    /**
     * Sets the {@link LifecycleOwner} to be bound with the controller.
     *
     * <p> The state of the lifecycle will determine when the cameras are open, started, stopped
     * and closed. When the {@link LifecycleOwner}'s state is start or greater, the controller
     * receives camera data. It stops once the {@link LifecycleOwner} is destroyed.
     *
     * @throws IllegalStateException If the provided camera selector is unable to resolve a
     *                               camera to be used for the given use cases.
     * @see ProcessCameraProvider#bindToLifecycle
     */
    @SuppressLint("MissingPermission")
    @MainThread
    public void bindToLifecycle(@NonNull LifecycleOwner lifecycleOwner) {
        Threads.checkMainThread();
        mLifecycleOwner = lifecycleOwner;
        startCameraAndTrackStates();
    }

    /**
     * Clears the previously set {@link LifecycleOwner} and stops the camera.
     *
     * @see ProcessCameraProvider#unbindAll
     */
    @MainThread
    public void unbind() {
        Threads.checkMainThread();
        mLifecycleOwner = null;
        mCamera = null;
        if (mCameraProvider != null) {
            mCameraProvider.unbindAll();
        }
    }

    /**
     * Unbind and rebind all use cases to {@link LifecycleOwner}.
     *
     * @return null if failed to start camera.
     * @throws IllegalStateException for invalid {@link UseCase} combinations.
     * @throws RuntimeException      for invalid {@link CameraEffect} combinations.
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    @Override
    @Nullable
    Camera startCamera() {
        if (mLifecycleOwner == null) {
            Log.d(TAG, "Lifecycle is not set.");
            return null;
        }
        if (mCameraProvider == null) {
            Log.d(TAG, "CameraProvider is not ready.");
            return null;
        }

        UseCaseGroup useCaseGroup = createUseCaseGroup();
        if (useCaseGroup == null) {
            // Use cases can't be created.
            return null;
        }
        try {
            return mCameraProvider.bindToLifecycle(mLifecycleOwner, mCameraSelector, useCaseGroup);
        } catch (IllegalArgumentException e) {
            // Catches the invalid use case combination exception and throw a more readable one.
            String errorMessage =
                    "The selected camera does not support the enabled use cases. Please "
                            + "disable use case and/or select a different camera. e.g. "
                            + "#setVideoCaptureEnabled(false)";
            throw new IllegalStateException(errorMessage, e);
        }
    }

    /**
     */
    @VisibleForTesting
    @SuppressWarnings("FutureReturnValueIgnored")
    void shutDownForTests() {
        if (mCameraProvider != null) {
            mCameraProvider.shutdown();
        }
    }
}
