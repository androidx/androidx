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

package androidx.camera.camera2.internal.compat;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.SessionConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat;
import androidx.core.util.Preconditions;

@RequiresApi(28)
class CameraDeviceCompatApi28Impl extends CameraDeviceCompatApi24Impl {

    CameraDeviceCompatApi28Impl(@NonNull CameraDevice cameraDevice) {
        super(Preconditions.checkNotNull(cameraDevice), /*implParams=*/null);
    }

    @Override
    public void createCaptureSession(@NonNull SessionConfigurationCompat config)
            throws CameraAccessExceptionCompat {
        SessionConfiguration sessionConfig = (SessionConfiguration) config.unwrap();
        Preconditions.checkNotNull(sessionConfig);

        try {
            mCameraDevice.createCaptureSession(sessionConfig);
        } catch (CameraAccessException e) {
            throw CameraAccessExceptionCompat.toCameraAccessExceptionCompat(e);
        }
    }
}
