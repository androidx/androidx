/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: b/577549468
 * - Description: On Xiaomi 17 Pro Max, camera streams configured with the
 *   `DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM` profile (without any OEM-specific session
 *   configuration) produce heavily over-saturated (orange/red) frames when the stream feeds a video
 *   encoder, while the output buffers are still tagged as BT.2020 HLG. The same over-saturation is
 *   seen with both the Dolby Vision encoder and the HEVC encoder, whereas an `HLG10` stream into
 *   the same Dolby Vision encoder (`c2.qti.dv.encoder`, profile 8.4, which expects HLG input)
 *   records correct colors. The workaround configures the camera stream with `HLG10` for
 *   [androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT], so Dolby Vision recording keeps
 *   working. It applies to all cameras on the device (both the back and front cameras advertise the
 *   profile). Dynamic range constraints and extra-latency queries still use the Dolby Vision
 *   profile; on this device `HLG10` and `DOLBY_VISION_10B_HDR_OEM` are both unconstrained, so only
 *   add devices whose two profiles have identical constraints.
 * - Device(s): Xiaomi 17 Pro Max (model 2509FPN0BC)
 */
@SuppressLint("CameraXQuirksClassDetector")
public object DolbyVisionStreamIncorrectColorQuirk : Quirk {

    @JvmStatic
    public fun load(): Boolean =
        Device.isXiaomiDevice() && "2509FPN0BC".equals(Build.MODEL, ignoreCase = true)
}
