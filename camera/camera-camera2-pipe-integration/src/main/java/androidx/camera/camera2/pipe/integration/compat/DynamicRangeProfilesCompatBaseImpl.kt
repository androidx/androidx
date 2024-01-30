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

package androidx.camera.camera2.pipe.integration.compat

import android.hardware.camera2.params.DynamicRangeProfiles
import androidx.annotation.RequiresApi
import androidx.camera.core.DynamicRange
import androidx.core.util.Preconditions

@RequiresApi(21)
internal class DynamicRangeProfilesCompatBaseImpl :
    DynamicRangeProfilesCompat.DynamicRangeProfilesCompatImpl {
    override fun getDynamicRangeCaptureRequestConstraints(
        dynamicRange: DynamicRange
    ): Set<DynamicRange> {
        Preconditions.checkArgument(
            DynamicRange.SDR == dynamicRange,
            "DynamicRange is not supported: $dynamicRange"
        )
        return SDR_ONLY
    }

    override fun getSupportedDynamicRanges(): Set<DynamicRange> {
        return SDR_ONLY
    }

    override fun isExtraLatencyPresent(dynamicRange: DynamicRange): Boolean {
        Preconditions.checkArgument(
            DynamicRange.SDR == dynamicRange,
            "DynamicRange is not supported: $dynamicRange"
        )
        return false
    }

    override fun unwrap(): DynamicRangeProfiles? {
        return null
    }

    companion object {
        val COMPAT_INSTANCE: DynamicRangeProfilesCompat =
            DynamicRangeProfilesCompat(DynamicRangeProfilesCompatBaseImpl())
        private val SDR_ONLY = setOf(DynamicRange.SDR)
    }
}
