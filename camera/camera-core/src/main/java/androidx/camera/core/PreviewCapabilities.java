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

package androidx.camera.core;

import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * PreviewCapabilities is used to query preview stream capabilities on the device.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface PreviewCapabilities {

    /**
     * Returns if preview stabilization is supported on the device.
     *
     * @return true if
     * {@link CaptureRequest#CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION} is supported,
     * otherwise false.
     *
     * @see CaptureRequest#CONTROL_VIDEO_STABILIZATION_MODE
     */
    boolean isStabilizationSupported();


    /** An empty implementation. */
    @NonNull
    PreviewCapabilities EMPTY = new PreviewCapabilities() {
        @Override
        public boolean isStabilizationSupported() {
            return false;
        }
    };
}
