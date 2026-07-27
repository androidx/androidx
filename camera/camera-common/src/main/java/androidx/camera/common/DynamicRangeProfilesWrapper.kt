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

package androidx.camera.common

import java.lang.Class

/**
 * Provides compatibility-focused access to [android.hardware.camera2.params.DynamicRangeProfiles].
 *
 * @see android.hardware.camera2.params.DynamicRangeProfiles
 */
public interface DynamicRangeProfilesWrapper : UnsafeWrapper {

    /**
     * Returns a set of supported dynamic range profiles.
     *
     * @return An unmodifiable set of supported dynamic range profiles.
     * @see android.hardware.camera2.params.DynamicRangeProfiles.getSupportedProfiles
     */
    public val supportedProfiles: Set<@DynamicRangeProfile Long>

    /**
     * Returns a set of supported dynamic range profiles that can be referenced in a single capture
     * request along with the given profile.
     *
     * A profile is always compatible with itself, and is included in the returned set.
     *
     * @param profile The dynamic range profile to query compatibility for.
     * @return An unmodifiable set of supported dynamic range profiles.
     * @see android.hardware.camera2.params.DynamicRangeProfiles.getProfileCaptureRequestConstraints
     */
    public fun getCompatibleProfiles(
        @DynamicRangeProfile profile: Long
    ): Set<@DynamicRangeProfile Long>

    /**
     * Checks whether a given dynamic range profile incurs extra latency.
     *
     * Profiles that incur extra latency may not be suitable for latency-sensitive use cases, such
     * as real-time previews.
     *
     * @param profile The dynamic range profile to check.
     * @return `true` if the profile incurs extra latency, `false` otherwise.
     * @see android.hardware.camera2.params.DynamicRangeProfiles.isExtraLatencyPresent
     */
    public fun hasExtraLatency(@DynamicRangeProfile profile: Long): Boolean

    public companion object {
        /**
         * The standard 8-bit dynamic range profile.
         *
         * @see android.hardware.camera2.params.DynamicRangeProfiles.STANDARD
         */
        public const val STANDARD: Long = 1L

        /**
         * The 10-bit HLG (Hybrid Log-Gamma) dynamic range profile.
         *
         * @see android.hardware.camera2.params.DynamicRangeProfiles.HLG10
         */
        public const val HLG10: Long = 2L

        /**
         * The 10-bit HDR10 dynamic range profile.
         *
         * @see android.hardware.camera2.params.DynamicRangeProfiles.HDR10
         */
        public const val HDR10: Long = 4L

        /**
         * The 10-bit HDR10+ dynamic range profile.
         *
         * @see android.hardware.camera2.params.DynamicRangeProfiles.HDR10_PLUS
         */
        public const val HDR10_PLUS: Long = 8L

        /**
         * The 10-bit Dolby Vision HDR reference profile.
         *
         * @see android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF
         */
        public const val DOLBY_VISION_10B_HDR_REF: Long = 16L

        /**
         * The 10-bit Dolby Vision HDR reference profile, power-optimized.
         *
         * @see android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF_PO
         */
        public const val DOLBY_VISION_10B_HDR_REF_PO: Long = 32L

        /**
         * The 10-bit Dolby Vision HDR OEM profile.
         *
         * @see android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM
         */
        public const val DOLBY_VISION_10B_HDR_OEM: Long = 64L

        /**
         * The 10-bit Dolby Vision HDR OEM profile, power-optimized.
         *
         * @see android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM_PO
         */
        public const val DOLBY_VISION_10B_HDR_OEM_PO: Long = 128L

        /**
         * The 8-bit Dolby Vision HDR reference profile.
         *
         * @see android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF
         */
        public const val DOLBY_VISION_8B_HDR_REF: Long = 256L

        /**
         * The 8-bit Dolby Vision HDR reference profile, power-optimized.
         *
         * @see android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF_PO
         */
        public const val DOLBY_VISION_8B_HDR_REF_PO: Long = 512L

        /**
         * The 8-bit Dolby Vision HDR OEM profile.
         *
         * @see android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM
         */
        public const val DOLBY_VISION_8B_HDR_OEM: Long = 1024L

        /**
         * The 8-bit Dolby Vision HDR OEM profile, power-optimized.
         *
         * @see android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM_PO
         */
        public const val DOLBY_VISION_8B_HDR_OEM_PO: Long = 2048L
    }
}

/**
 * An implementation of [DynamicRangeProfilesWrapper] that reports support for only the standard
 * 8-bit profile.
 */
internal object UnsupportedDynamicRangeProfiles : DynamicRangeProfilesWrapper {
    override val supportedProfiles: Set<@DynamicRangeProfile Long> =
        setOf(DynamicRangeProfilesWrapper.STANDARD)

    override fun getCompatibleProfiles(
        @DynamicRangeProfile profile: Long
    ): Set<@DynamicRangeProfile Long> {
        if (profile == DynamicRangeProfilesWrapper.STANDARD) {
            return setOf(DynamicRangeProfilesWrapper.STANDARD)
        }
        return emptySet()
    }

    override fun hasExtraLatency(@DynamicRangeProfile profile: Long): Boolean = false

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? {
        if (type.isInstance(this)) {
            return this as T
        }
        return null
    }
}
