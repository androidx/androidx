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

package androidx.camera.camera2.impl.compat;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraDevice;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/**
 * Helper for accessing features in {@link CameraDevice} in a backwards compatible fashion.
 *
 * @hide Will be unhidden once some methods are implemented
 */
@RestrictTo(Scope.LIBRARY)
@TargetApi(21)
public final class CameraDeviceCompat {

    /**
     * Standard camera operation mode.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public static final int SESSION_OPERATION_MODE_NORMAL =
            0; // ICameraDeviceUser.NORMAL_MODE;

    /**
     * Constrained high-speed operation mode.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public static final int SESSION_OPERATION_MODE_CONSTRAINED_HIGH_SPEED =
            1; // ICameraDeviceUser.CONSTRAINED_HIGH_SPEED_MODE;
}
