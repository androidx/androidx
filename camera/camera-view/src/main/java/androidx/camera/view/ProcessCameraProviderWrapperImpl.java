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

import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

/**
 * Implementation of {@link ProcessCameraProviderWrapper} that wraps a round a real
 * {@link ProcessCameraProvider} object.
 */
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
    public void unbind(UseCase @NonNull ... useCases) {
        mProcessCameraProvider.unbind(useCases);
    }

    @Override
    public void unbind(@NonNull SessionConfig sessionConfig) {
        mProcessCameraProvider.unbind(sessionConfig);
    }

    @Override
    public void unbindAll() {
        mProcessCameraProvider.unbindAll();
    }

    @Override
    public @NonNull Camera bindToLifecycle(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull CameraSelector cameraSelector, @NonNull UseCaseGroup useCaseGroup) {
        return mProcessCameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, useCaseGroup);
    }

    @Override
    public @NonNull Camera bindToLifecycle(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull CameraSelector cameraSelector, @NonNull SessionConfig sessionConfig) {
        return mProcessCameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector,
                sessionConfig);
    }

    @VisibleForTesting
    @Override
    public @NonNull ListenableFuture<Void> shutdownAsync() {
        return mProcessCameraProvider.shutdownAsync();
    }

    @Override
    public @NonNull CameraInfo getCameraInfo(CameraSelector cameraSelector) {
        return mProcessCameraProvider.getCameraInfo(cameraSelector);
    }
}
