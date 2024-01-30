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

package androidx.camera.camera2.internal.util;

import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.Camera2CameraControlImpl;
import androidx.camera.camera2.internal.Camera2CameraInfoImpl;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInfoInternal;

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class TestUtil {
    public static Camera2CameraControlImpl getCamera2CameraControlImpl(
            CameraControl cameraControl) {
        if (cameraControl instanceof CameraControlInternal) {
            CameraControlInternal impl =
                    ((CameraControlInternal) cameraControl).getImplementation();
            return (Camera2CameraControlImpl) impl;
        }
        throw new IllegalArgumentException(
                "Can't get Camera2CameraControlImpl from the CameraControl");
    }

    public static Camera2CameraInfoImpl getCamera2CameraInfoImpl(CameraInfo cameraInfo) {
        if (cameraInfo instanceof CameraInfoInternal) {
            CameraInfoInternal impl = ((CameraInfoInternal) cameraInfo).getImplementation();
            return (Camera2CameraInfoImpl) impl;
        }
        throw new IllegalArgumentException(
                "Can't get Camera2CameraInfoImpl from the CameraInfo");
    }
}
