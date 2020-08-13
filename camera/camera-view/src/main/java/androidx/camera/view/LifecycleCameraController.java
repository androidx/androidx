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
import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;
import androidx.annotation.experimental.UseExperimental;
import androidx.camera.core.Camera;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.lifecycle.ExperimentalUseCaseGroupLifecycle;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

/**
 * The  controller that manages CameraX stack.
 *
 * <p> The controller is a high level API manages the entire CameraX stack including camera
 * readiness, Android UI lifecycle and use cases.
 *
 * <p> The controller is bound with the given {@link LifecycleOwner}. It starts use cases by
 * binding it to a {@link LifecycleOwner}, and cleans up once the {@link Lifecycle} is destroyed.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class LifecycleCameraController extends CameraController {

    private static final String TAG = "CamLifecycleController";

    @Nullable
    private LifecycleOwner mLifecycleOwner;

    public LifecycleCameraController(@NonNull Context context) {
        super(context);
    }

    /**
     * Sets the {@link LifecycleOwner} to be bound when ready.
     */
    @SuppressLint("MissingPermission")
    @MainThread
    public void bindToLifecycle(@NonNull LifecycleOwner lifecycleOwner) {
        Threads.checkMainThread();
        mLifecycleOwner = lifecycleOwner;
        startCamera();
    }

    /**
     * Unbinds all use cases.
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
     */
    @UseExperimental(markerClass = ExperimentalUseCaseGroupLifecycle.class)
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
        return mCameraProvider.bindToLifecycle(mLifecycleOwner, CAMERA_SELECTOR, useCaseGroup);
    }
}
