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

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.DynamicRangeProfiles
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.core.checkApi
import androidx.camera.core.DynamicRange

/**
 * Helper for accessing features in DynamicRangeProfiles in a backwards compatible fashion.
 */
@RequiresApi(21)
class DynamicRangeProfilesCompat internal constructor(
    private val mImpl: DynamicRangeProfilesCompatImpl
) {
    /**
     * Returns a set of supported [DynamicRange] that can be referenced in a single
     * capture request.
     *
     * For example if a particular 10-bit output capable device returns (STANDARD,
     * HLG10, HDR10) as result from calling [getSupportedDynamicRanges] and
     * [DynamicRangeProfiles.getProfileCaptureRequestConstraints]
     * returns (STANDARD, HLG10) when given an argument
     * of STANDARD. This means that the corresponding camera device will only accept and process
     * capture requests that reference outputs configured using HDR10 dynamic range or
     * alternatively some combination of STANDARD and HLG10. However trying to queue capture
     * requests to outputs that reference both HDR10 and STANDARD/HLG10 will result in
     * IllegalArgumentException.
     *
     * The list will be empty in case there are no constraints for the given dynamic range.
     *
     * @param dynamicRange The dynamic range that will be checked for constraints
     * @return non-modifiable set of dynamic ranges
     * @throws IllegalArgumentException If the dynamic range argument is not within the set
     * returned by [getSupportedDynamicRanges].
     */
    fun getDynamicRangeCaptureRequestConstraints(
        dynamicRange: DynamicRange
    ): Set<DynamicRange> {
        return mImpl.getDynamicRangeCaptureRequestConstraints(dynamicRange)
    }

    /**
     * Returns a set of supported dynamic ranges.
     *
     * @return a non-modifiable set of dynamic ranges.
     */
    fun getSupportedDynamicRanges(): Set<DynamicRange> {
        return mImpl.getSupportedDynamicRanges()
    }

    /**
     * Checks whether a given dynamic range is suitable for latency sensitive use cases.
     *
     * Due to internal lookahead logic, camera outputs configured with some dynamic range
     * profiles may experience additional latency greater than 3 buffers. Using camera outputs
     * with such dynamic ranges for latency sensitive use cases such as camera preview is not
     * recommended. Dynamic ranges that have such extra streaming delay are typically utilized for
     * scenarios such as offscreen video recording.
     *
     * @param dynamicRange The dynamic range to check for extra latency
     * @return `true` if the given profile is not suitable for latency sensitive use cases,
     * `false` otherwise.
     * @throws IllegalArgumentException If the dynamic range argument is not within the set
     * returned by [getSupportedDynamicRanges].
     */
    fun isExtraLatencyPresent(dynamicRange: DynamicRange): Boolean {
        return mImpl.isExtraLatencyPresent(dynamicRange)
    }

    /**
     * Returns the underlying framework
     * [DynamicRangeProfiles].
     *
     * @return the underlying [DynamicRangeProfiles] or
     * `null` if the device doesn't support 10 bit dynamic range.
     */
    @RequiresApi(33)
    fun toDynamicRangeProfiles(): DynamicRangeProfiles? {
        checkApi(
            33, "DynamicRangesCompat can only be " +
                "converted to DynamicRangeProfiles on API 33 or higher."
        )
        return mImpl.unwrap()
    }

    internal interface DynamicRangeProfilesCompatImpl {
        fun getDynamicRangeCaptureRequestConstraints(
            dynamicRange: DynamicRange
        ): Set<DynamicRange>

        fun getSupportedDynamicRanges(): Set<DynamicRange>

        fun isExtraLatencyPresent(dynamicRange: DynamicRange): Boolean
        fun unwrap(): DynamicRangeProfiles?
    }

    companion object {
        /**
         * Returns a [DynamicRangeProfilesCompat] using the capabilities derived from the provided
         * characteristics.
         *
         * @param cameraMetadata the metaData used to derive dynamic range information.
         * @return a [DynamicRangeProfilesCompat] object.
         */
        fun fromCameraMetaData(
            cameraMetadata: CameraMetadata
        ): DynamicRangeProfilesCompat {
            var rangesCompat: DynamicRangeProfilesCompat? = null
            if (Build.VERSION.SDK_INT >= 33) {
                rangesCompat = toDynamicRangesCompat(
                    cameraMetadata[CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES]
                )
            }
            return rangesCompat ?: DynamicRangeProfilesCompatBaseImpl.COMPAT_INSTANCE
        }

        /**
         * Creates an instance from a framework [DynamicRangeProfiles]
         * object.
         *
         * @param dynamicRangeProfiles a [DynamicRangeProfiles].
         * @return an equivalent [DynamicRangeProfilesCompat] object.
         */
        @RequiresApi(33)
        fun toDynamicRangesCompat(
            dynamicRangeProfiles: DynamicRangeProfiles?
        ): DynamicRangeProfilesCompat? {
            if (dynamicRangeProfiles == null) {
                return null
            }
            checkApi(
                33, "DynamicRangeProfiles can only " +
                    "be converted to DynamicRangesCompat on API 33 or higher."
            )
            return DynamicRangeProfilesCompat(
                DynamicRangeProfilesCompatApi33Impl(
                    dynamicRangeProfiles
                )
            )
        }
    }
}
