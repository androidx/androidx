/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: b/359425774
 * - Description: This quirk addresses an under-exposure issue observed in the preview when binding
 *   with VideoCapture on TCL devices. To maintain consistency with preview without VideoCapture,
 *   the workaround enforces the use of `TEMPLATE_PREVIEW`. Although the issue was initially
 *   reported on the front camera of model 9465G, the workaround is applied universally to:
 *     1. All TCL devices: Further investigation (b/359425774#comment23) revealed that other TCL
 *        devices encounter the same problem.
 *     2. All cameras: This ensures a consistent user experience across all cameras on the same
 *        device.
 * - Device(s): TCL devices.
 */
@SuppressLint("CameraXQuirksClassDetector")
public object PreviewUnderExposureQuirk : Quirk {

    @JvmStatic
    public fun load(): Boolean {
        return isTclDevice
    }

    private val isTclDevice: Boolean = Build.BRAND.equals("TCL", ignoreCase = true)
}
