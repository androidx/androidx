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

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
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
 * A wrapper interface for {@link ProcessCameraProvider}.
 *
 * <p> This exists to to inject fake {@link ProcessCameraProvider} in unit tests.
 *
 * <p> TODO: remove this class once there is a better way to inject dependencies.
 */
interface ProcessCameraProviderWrapper {

    /**
     * Wrapper method for {@link ProcessCameraProvider#hasCamera}.
     */
    boolean hasCamera(@NonNull CameraSelector cameraSelector) throws CameraInfoUnavailableException;

    /**
     * Wrapper method for {@link ProcessCameraProvider#unbind}.
     */
    void unbind(@NonNull UseCase... useCases);

    /**
     * Wrapper method for {@link ProcessCameraProvider#unbindAll()}.
     */
    void unbindAll();

    /**
     * Wrapper method for {@link ProcessCameraProvider#bindToLifecycle}.
     */
    @SuppressWarnings({"lambdaLast"})
    @MainThread
    @NonNull
    Camera bindToLifecycle(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull CameraSelector cameraSelector,
            @NonNull UseCaseGroup useCaseGroup);

    /**
     * Wrapper method for {@link ProcessCameraProvider#shutdown()}.
     *
     */
    @NonNull
    @VisibleForTesting
    ListenableFuture<Void> shutdown();
}
