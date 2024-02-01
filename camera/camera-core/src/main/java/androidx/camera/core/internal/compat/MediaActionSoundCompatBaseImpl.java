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

package androidx.camera.core.internal.compat;

import androidx.annotation.RequiresApi;

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class MediaActionSoundCompatBaseImpl {

    // The camera ID used to query the camera information that is the same for all cameras.
    private static final int SAMPLE_CAMERA_ID = 0;

    // For API level less than 33, deprecated Camera1 APIs are used as workarounds.
    @SuppressWarnings("deprecation")
    static boolean mustPlayShutterSound() {
        if (android.hardware.Camera.getNumberOfCameras() < 1) {
            // Return false if there's no camera on the device.
            return false;
        }
        try {
            android.hardware.Camera.CameraInfo cameraInfo =
                    new android.hardware.Camera.CameraInfo();
            // Whether the shutter sound is allowed to be disabled should be the same for all
            // cameras on the device.
            android.hardware.Camera.getCameraInfo(SAMPLE_CAMERA_ID, cameraInfo);
            return !cameraInfo.canDisableShutterSound;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // Class should not be instantiated.
    private MediaActionSoundCompatBaseImpl() {
    }
}
