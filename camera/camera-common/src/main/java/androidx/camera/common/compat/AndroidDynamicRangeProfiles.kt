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

package androidx.camera.common.compat

import android.hardware.camera2.params.DynamicRangeProfiles as Camera2DynamicRangeProfiles
import androidx.annotation.RequiresApi
import androidx.camera.common.DynamicRangeProfile
import androidx.camera.common.DynamicRangeProfilesWrapper
import java.lang.Class

@RequiresApi(33)
@Suppress("WrongConstant")
internal class AndroidDynamicRangeProfiles(
    private val dynamicRangeProfiles: Camera2DynamicRangeProfiles
) : DynamicRangeProfilesWrapper {

    override val supportedProfiles: Set<@DynamicRangeProfile Long>
        get() = dynamicRangeProfiles.supportedProfiles

    override fun getCompatibleProfiles(
        @DynamicRangeProfile profile: Long
    ): Set<@DynamicRangeProfile Long> {
        return dynamicRangeProfiles.getProfileCaptureRequestConstraints(profile)
    }

    override fun hasExtraLatency(@DynamicRangeProfile profile: Long): Boolean {
        return dynamicRangeProfiles.isExtraLatencyPresent(profile)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type.isInstance(dynamicRangeProfiles) -> dynamicRangeProfiles as T
            else -> null
        }
}
