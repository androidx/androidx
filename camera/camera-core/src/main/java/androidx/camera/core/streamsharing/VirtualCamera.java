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

package androidx.camera.core.streamsharing;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Observable;
import androidx.camera.core.impl.UseCaseConfigFactory;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collection;
import java.util.Set;

/**
 * A virtual implementation of {@link CameraInternal}.
 *
 * <p> This class manages children {@link UseCase} and connects/disconnects them to the
 * parent {@link StreamSharing}. It also forwards parent camera properties/events to the children.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class VirtualCamera implements CameraInternal {

    private static final String UNSUPPORTED_MESSAGE = "Operation not supported by VirtualCamera.";

    @SuppressWarnings("UnusedVariable")
    @NonNull
    private final Set<UseCase> mChildren;
    @SuppressWarnings("UnusedVariable")
    @NonNull
    private final UseCaseConfigFactory mUseCaseConfigFactory;
    @NonNull
    private final CameraInternal mParentCamera;

    /**
     * @param parentCamera         the parent {@link CameraInternal} instance. For example, the
     *                             real camera.
     * @param children             the children {@link UseCase}.
     * @param useCaseConfigFactory the factory for configuring children {@link UseCase}.
     */
    VirtualCamera(@NonNull CameraInternal parentCamera,
            @NonNull Set<UseCase> children,
            @NonNull UseCaseConfigFactory useCaseConfigFactory) {
        mParentCamera = parentCamera;
        mUseCaseConfigFactory = useCaseConfigFactory;
        mChildren = children;
    }

    // --- API for StreamSharing ---

    // TODO(b/264936250): Add methods for interacting with the StreamSharing UseCase.

    @NonNull
    Set<UseCase> getChildren() {
        return mChildren;
    }

    // --- Handle children state change ---

    // TODO(b/264936250): Handle children state changes.

    @Override
    public void onUseCaseActive(@NonNull UseCase useCase) {

    }

    @Override
    public void onUseCaseInactive(@NonNull UseCase useCase) {

    }

    @Override
    public void onUseCaseUpdated(@NonNull UseCase useCase) {

    }

    @Override
    public void onUseCaseReset(@NonNull UseCase useCase) {

    }

    // --- Forward parent camera properties and events ---

    @NonNull
    @Override
    public CameraControlInternal getCameraControlInternal() {
        return mParentCamera.getCameraControlInternal();
    }

    @NonNull
    @Override
    public CameraInfoInternal getCameraInfoInternal() {
        return mParentCamera.getCameraInfoInternal();
    }

    @NonNull
    @Override
    public Observable<State> getCameraState() {
        return mParentCamera.getCameraState();
    }

    // --- Unused overrides ---

    @Override
    public void open() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @NonNull
    @Override
    public ListenableFuture<Void> release() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public void attachUseCases(@NonNull Collection<UseCase> useCases) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public void detachUseCases(@NonNull Collection<UseCase> useCases) {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }
}
