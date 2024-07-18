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
import androidx.camera.camera2.pipe.integration.internal.DynamicRangeConversions
import androidx.camera.core.DynamicRange
import java.util.Collections

@RequiresApi(33)
internal class DynamicRangeProfilesCompatApi33Impl(
    private val dynamicRangeProfiles: DynamicRangeProfiles
) : DynamicRangeProfilesCompat.DynamicRangeProfilesCompatImpl {

    override fun getDynamicRangeCaptureRequestConstraints(
        dynamicRange: DynamicRange
    ): Set<DynamicRange> {
        val dynamicRangeProfile = dynamicRangeToFirstSupportedProfile(dynamicRange)
        require(dynamicRangeProfile != null) {
            "DynamicRange is not supported: $dynamicRange"
        }
        return profileSetToDynamicRangeSet(
            dynamicRangeProfiles.getProfileCaptureRequestConstraints(dynamicRangeProfile)
        )
    }

    override fun getSupportedDynamicRanges() = profileSetToDynamicRangeSet(
        dynamicRangeProfiles.supportedProfiles
    )

    override fun isExtraLatencyPresent(dynamicRange: DynamicRange): Boolean {
        val dynamicRangeProfile = dynamicRangeToFirstSupportedProfile(dynamicRange)
        require(
            dynamicRangeProfile != null
        ) {
            "DynamicRange is not supported: $dynamicRange"
        }
        return dynamicRangeProfiles.isExtraLatencyPresent(dynamicRangeProfile)
    }

    override fun unwrap() = dynamicRangeProfiles

    private fun dynamicRangeToFirstSupportedProfile(dynamicRange: DynamicRange) =
        DynamicRangeConversions.dynamicRangeToFirstSupportedProfile(
            dynamicRange,
            dynamicRangeProfiles
        )

    private fun profileToDynamicRange(profile: Long): DynamicRange {
        val result = DynamicRangeConversions.profileToDynamicRange(
            profile
        )
        require(result != null) {
            "Dynamic range profile cannot be converted to a DynamicRange object: $profile"
        }
        return result
    }

    private fun profileSetToDynamicRangeSet(profileSet: Set<Long>): Set<DynamicRange> {
        if (profileSet.isEmpty()) {
            return emptySet()
        }
        val dynamicRangeSet: MutableSet<DynamicRange> = mutableSetOf()
        for (profile in profileSet) {
            dynamicRangeSet.add(profileToDynamicRange(profile))
        }
        return Collections.unmodifiableSet(dynamicRangeSet)
    }
}
