/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraState;
import androidx.camera.core.CameraState.StateError;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.CameraStateRegistry;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Objects;

/** State machine that computes the camera's public state from its internal state. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class CameraStateMachine {

    private static final String TAG = "CameraStateMachine";

    @NonNull
    private final CameraStateRegistry mCameraStateRegistry;
    @NonNull
    private final MutableLiveData<CameraState> mCameraStates;

    CameraStateMachine(@NonNull final CameraStateRegistry cameraStateRegistry) {
        mCameraStateRegistry = cameraStateRegistry;
        mCameraStates = new MutableLiveData<>();
        mCameraStates.postValue(CameraState.create(CameraState.Type.CLOSED));
    }

    /**
     * Computes a new {@linkplain CameraState public camera state} to emit to the user based on the
     * camera's {@linkplain CameraInternal.State internal state} and potentially an error it
     * encountered.
     */
    public void updateState(@NonNull final CameraInternal.State newInternalState,
            @Nullable final StateError stateError) {
        final CameraState newPublicState;
        switch (newInternalState) {
            case PENDING_OPEN:
                newPublicState = onCameraPendingOpen();
                break;
            case OPENING:
                newPublicState = CameraState.create(CameraState.Type.OPENING, stateError);
                break;
            case OPEN:
                newPublicState = CameraState.create(CameraState.Type.OPEN, stateError);
                break;
            case CLOSING:
            case RELEASING:
                newPublicState = CameraState.create(CameraState.Type.CLOSING, stateError);
                break;
            case CLOSED:
            case RELEASED:
                newPublicState = CameraState.create(CameraState.Type.CLOSED, stateError);
                break;
            default:
                throw new IllegalStateException(
                        "Unknown internal camera state: " + newInternalState);
        }

        Logger.d(TAG, "New public camera state " + newPublicState + " from " + newInternalState
                + " and " + stateError);

        // Only emit the new public state if it's different than the current public state
        final CameraState currentPublicState = mCameraStates.getValue();
        if (!Objects.equals(currentPublicState, newPublicState)) {
            Logger.d(TAG, "Publishing new public camera state " + newPublicState);
            mCameraStates.postValue(newPublicState);
        }
    }

    /**
     * If the camera is waiting to be open while another camera is closing inside CameraX, its
     * public state should be {@link CameraState.Type#OPENING}. Otherwise, it should be
     * {@link CameraState.Type#PENDING_OPEN}.
     *
     * <p>Keeping the public state as {@link CameraState.Type#OPENING} when another camera is
     * closing inside CameraX improves the developer's experience. The
     * {@link CameraStateRegistry} will notify the camera that it can resume opening when the
     * other camera finishes closing.
     */
    private CameraState onCameraPendingOpen() {
        return mCameraStateRegistry.isCameraClosing() ? CameraState.create(CameraState.Type.OPENING)
                : CameraState.create(CameraState.Type.PENDING_OPEN);
    }

    /** Returns a {@link LiveData} that emits the camera's public states. */
    @NonNull
    public LiveData<CameraState> getStateLiveData() {
        return mCameraStates;
    }
}
