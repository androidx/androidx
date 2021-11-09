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

package androidx.camera.camera2.internal;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

/**
 * Camera device id that is composed by Brand, Device, Model and CameraId.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
abstract class CameraDeviceId {
    CameraDeviceId() {
    }

    /**
     * Creates a new instance of CameraDeviceId with the given parameters.
     *
     * <p>Be noticed that all CameraDeviceId related info will be stored in lower case.
     */
    @NonNull
    public static CameraDeviceId create(@NonNull String brand, @NonNull String device,
            @NonNull String model, @NonNull String cameraId) {
        return new AutoValue_CameraDeviceId(brand.toLowerCase(), device.toLowerCase(),
                model.toLowerCase(), cameraId.toLowerCase());
    }

    /** Returns the brand. */
    @NonNull
    public abstract String getBrand();

    /** Returns the device. */
    @NonNull
    public abstract String getDevice();

    /** Returns the model. */
    @NonNull
    public abstract String getModel();

    /** Returns the camera id. */
    @NonNull
    public abstract String getCameraId();
}
