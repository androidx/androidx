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

package androidx.camera.view;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Implementation of {@link ProcessCameraProviderWrapper} that wraps a round a real
 * {@link ProcessCameraProvider} object.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class ProcessCameraProviderWrapperImpl implements ProcessCameraProviderWrapper {

    private final ProcessCameraProvider mProcessCameraProvider;

    ProcessCameraProviderWrapperImpl(ProcessCameraProvider processCameraProvider) {
        mProcessCameraProvider = processCameraProvider;
    }

    @Override
    public boolean hasCamera(@NonNull CameraSelector cameraSelector)
            throws CameraInfoUnavailableException {
        return mProcessCameraProvider.hasCamera(cameraSelector);
    }

    @Override
    public void unbind(@NonNull UseCase... useCases) {
        mProcessCameraProvider.unbind(useCases);
    }

    @Override
    public void unbindAll() {
        mProcessCameraProvider.unbindAll();
    }

    @NonNull
    @Override
    public Camera bindToLifecycle(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull CameraSelector cameraSelector, @NonNull UseCaseGroup useCaseGroup) {
        return mProcessCameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup);
    }

    @VisibleForTesting
    @NonNull
    @Override
    public ListenableFuture<Void> shutdown() {
        return mProcessCameraProvider.shutdown();
    }
}
