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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Different implementations of {@link CameraCaptureCallback}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CameraCaptureCallbacks {
    private CameraCaptureCallbacks() {
    }

    /** Returns a camera capture callback which does nothing. */
    @NonNull
    public static CameraCaptureCallback createNoOpCallback() {
        return new NoOpCameraCaptureCallback();
    }

    /** Returns a camera capture callback which calls a list of other callbacks. */
    @NonNull
    static CameraCaptureCallback createComboCallback(
            @NonNull List<CameraCaptureCallback> callbacks) {
        if (callbacks.isEmpty()) {
            return createNoOpCallback();
        } else if (callbacks.size() == 1) {
            return callbacks.get(0);
        }
        return new ComboCameraCaptureCallback(callbacks);
    }

    /** Returns a camera capture callback which calls a list of other callbacks. */
    @NonNull
    public static CameraCaptureCallback createComboCallback(
            @NonNull CameraCaptureCallback... callbacks) {
        return createComboCallback(Arrays.asList(callbacks));
    }

    static final class NoOpCameraCaptureCallback extends CameraCaptureCallback {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureResult cameraCaptureResult) {
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureFailure failure) {
        }
    }

    /**
     * A CameraCaptureCallback which contains a list of CameraCaptureCallback and will propagate
     * received callback to the list.
     */
    public static final class ComboCameraCaptureCallback extends CameraCaptureCallback {
        private final List<CameraCaptureCallback> mCallbacks = new ArrayList<>();

        ComboCameraCaptureCallback(@NonNull List<CameraCaptureCallback> callbacks) {
            for (CameraCaptureCallback callback : callbacks) {
                // A no-op callback doesn't do anything, so avoid adding it to the final list.
                if (!(callback instanceof NoOpCameraCaptureCallback)) {
                    mCallbacks.add(callback);
                }
            }
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureResult cameraCaptureResult) {
            for (CameraCaptureCallback callback : mCallbacks) {
                callback.onCaptureCompleted(cameraCaptureResult);
            }
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureFailure failure) {
            for (CameraCaptureCallback callback : mCallbacks) {
                callback.onCaptureFailed(failure);
            }
        }

        @Override
        public void onCaptureCancelled() {
            for (CameraCaptureCallback callback : mCallbacks) {
                callback.onCaptureCancelled();
            }
        }

        @NonNull
        public List<CameraCaptureCallback> getCallbacks() {
            return mCallbacks;
        }
    }
}
