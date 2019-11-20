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

package androidx.camera.camera2.impl;

import androidx.annotation.NonNull;
import androidx.camera.core.TorchState;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Implementation of torch control used within {@link Camera2CameraControl}.
 */
// TODO(b/143514107): implement TorchControl
final class TorchControl {

    private final Camera2CameraControl mCamera2CameraControl;
    private final MutableLiveData<Integer> mTorchState;

    TorchControl(@NonNull Camera2CameraControl camera2CameraControl) {
        mCamera2CameraControl = camera2CameraControl;
        mTorchState = new MutableLiveData<>(TorchState.OFF);
    }

    // TODO(b/143514107): return a functional ListenableFuture
    ListenableFuture<Void> enableTorch(boolean enabled) {
        setLiveDataValue(mTorchState, enabled ? TorchState.ON : TorchState.OFF);
        return Futures.immediateFuture(null);
    }

    @NonNull
    LiveData<Integer> getTorchState() {
        return mTorchState;
    }

    private <T> void setLiveDataValue(@NonNull MutableLiveData<T> liveData, T value) {
        if (Threads.isMainThread()) {
            liveData.setValue(value);
        } else {
            liveData.postValue(value);
        }
    }
}
