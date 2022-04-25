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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraCaptureSession.CaptureCallback;

import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureCallbacks.ComboCameraCaptureCallback;

import java.util.ArrayList;
import java.util.List;

/** An utility class to convert {@link CameraCaptureCallback} to camera2 {@link CaptureCallback}. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class CaptureCallbackConverter {

    private CaptureCallbackConverter() {
    }

    /**
     * Converts {@link CameraCaptureCallback} to {@link CaptureCallback}.
     *
     * @param cameraCaptureCallback The camera capture callback.
     * @return The capture session callback.
     */
    static CaptureCallback toCaptureCallback(CameraCaptureCallback cameraCaptureCallback) {
        if (cameraCaptureCallback == null) {
            return null;
        }
        List<CaptureCallback> list = new ArrayList<>();
        toCaptureCallback(cameraCaptureCallback, list);
        return list.size() == 1
                ? list.get(0)
                : Camera2CaptureCallbacks.createComboCallback(list);
    }

    /**
     * Converts {@link CameraCaptureCallback} to one or more {@link CaptureCallback} and put them
     * into the input capture callback list.
     *
     * <p>There are several known types of {@link CameraCaptureCallback}s. Convert the callback
     * according to the corresponding rule.
     *
     * @param cameraCaptureCallback The camera capture callback.
     * @param captureCallbackList   The output capture session callback list.
     */
    static void toCaptureCallback(
            CameraCaptureCallback cameraCaptureCallback,
            List<CaptureCallback> captureCallbackList) {
        if (cameraCaptureCallback instanceof ComboCameraCaptureCallback) {
            // Recursively convert callback inside the combo callback.
            ComboCameraCaptureCallback comboCallback =
                    (ComboCameraCaptureCallback) cameraCaptureCallback;
            for (CameraCaptureCallback callback : comboCallback.getCallbacks()) {
                toCaptureCallback(callback, captureCallbackList);
            }
        } else if (cameraCaptureCallback instanceof CaptureCallbackContainer) {
            // Get the actual callback inside the CaptureCallbackContainer.
            CaptureCallbackContainer callbackContainer =
                    (CaptureCallbackContainer) cameraCaptureCallback;
            captureCallbackList.add(callbackContainer.getCaptureCallback());
        } else {
            // Create a CaptureCallbackAdapter.
            captureCallbackList.add(new CaptureCallbackAdapter(cameraCaptureCallback));
        }
    }
}
