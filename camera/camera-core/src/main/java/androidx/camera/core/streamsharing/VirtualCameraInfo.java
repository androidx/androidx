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

import static androidx.camera.core.impl.utils.TransformUtils.within360;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.ForwardingCameraInfo;
import androidx.camera.core.impl.ImageOutputConfig;

import java.util.UUID;

/**
 * A {@link CameraInfoInternal} that returns info of the virtual camera.
 */
public class VirtualCameraInfo extends ForwardingCameraInfo {

    private final String mVirtualCameraId;
    private int mVirtualCameraRotationDegrees;

    VirtualCameraInfo(@NonNull CameraInfoInternal cameraInfoInternal) {
        super(cameraInfoInternal);
        // Generate a unique ID for the virtual camera.
        mVirtualCameraId =
                "virtual-" + cameraInfoInternal.getCameraId() + "-" + UUID.randomUUID().toString();
    }

    /**
     * Override the parent camera ID.
     */
    @NonNull
    @Override
    public String getCameraId() {
        return mVirtualCameraId;
    }

    /**
     * Sets the rotation applied by this virtual camera.
     */
    void setVirtualCameraRotationDegrees(int virtualCameraRotationDegrees) {
        mVirtualCameraRotationDegrees = virtualCameraRotationDegrees;
    }

    @Override
    public int getSensorRotationDegrees() {
        return getSensorRotationDegrees(Surface.ROTATION_0);
    }

    @Override
    public int getSensorRotationDegrees(@ImageOutputConfig.RotationValue int relativeRotation) {
        // The child UseCase calls this method to get the remaining rotation degrees, which is the
        // original rotation minus the rotation applied by the virtual camera.
        return within360(
                super.getSensorRotationDegrees(relativeRotation) - mVirtualCameraRotationDegrees);
    }
}
