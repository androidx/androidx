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

package androidx.graphics.lowlatency

import android.annotation.SuppressLint
import android.hardware.HardwareBuffer
import android.os.Build
import androidx.annotation.RequiresApi

internal class FrontBufferUtils private constructor() {

    companion object {

        internal const val TAG = "FrontBufferUtils"

        // Leverage the same value as HardwareBuffer.USAGE_COMPOSER_OVERLAY.
        // While this constant was introduced in the SDK in the Android T release, it has
        // been available within the NDK as part of
        // AHardwareBuffer_UsageFlags#AHARDWAREBUFFER_USAGE_COMPOSER_OVERLAY for quite some time.
        // This flag is required for usage of ASurfaceTransaction#setBuffer
        // Use a separate constant with the same value to avoid SDK warnings of accessing the
        // newly added constant in the SDK.
        // See:
        // developer.android.com/ndk/reference/group/a-hardware-buffer#ahardwarebuffer_usageflags
        private const val USAGE_COMPOSER_OVERLAY: Long = 2048L

        /**
         * Flags that are expected to be supported on all [HardwareBuffer] instances
         */
        internal const val BaseFlags =
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or
                HardwareBuffer.USAGE_GPU_COLOR_OUTPUT or
                USAGE_COMPOSER_OVERLAY

        internal fun obtainHardwareBufferUsageFlags(): Long =
            if (!UseCompatSurfaceControl &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                UsageFlagsVerificationHelper.obtainUsageFlagsV33()
            } else {
                BaseFlags
            }

        internal const val UseCompatSurfaceControl = false
    }
}

/**
 * Helper class to avoid class verification failures
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class UsageFlagsVerificationHelper private constructor() {
    companion object {

        /**
         * Helper method to determine if a particular HardwareBuffer usage flag is supported.
         * Even though the FRONT_BUFFER_USAGE and COMPOSER_OVERLAY flags are introduced in
         * Android T, not all devices may support this flag. So we conduct a capability query
         * with a sample 1x1 HardwareBuffer with the provided flag to see if it is compatible
         */
        // Suppressing WrongConstant warnings as we are leveraging a constant with the same value
        // as HardwareBuffer.USAGE_COMPOSER_OVERLAY to avoid SDK checks as the constant has been
        // supported in the NDK for several platform releases.
        // See:
        // developer.android.com/ndk/reference/group/a-hardware-buffer#ahardwarebuffer_usageflags
        @SuppressLint("WrongConstant")
        @RequiresApi(Build.VERSION_CODES.Q)
        @androidx.annotation.DoNotInline
        internal fun isSupported(flag: Long): Boolean =
            HardwareBuffer.isSupported(
                1, // width
                1, // height
                HardwareBuffer.RGBA_8888, // format
                1, // layers
                FrontBufferUtils.BaseFlags or flag
            )

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        @androidx.annotation.DoNotInline
        fun obtainUsageFlagsV33(): Long {
            // First verify if the front buffer usage flag is supported along with the
            // "usage composer overlay" flag that was introduced in API level 33
            // SF Seems to log errors when configuring HardwareBuffer instances with the
            // front buffer usage flag on Cuttlefish, so only include it for actual devices.
            // See b/280866371
            return if (isSupported(HardwareBuffer.USAGE_FRONT_BUFFER) &&
                !Build.MODEL.contains("Cuttlefish")) {
                FrontBufferUtils.BaseFlags or HardwareBuffer.USAGE_FRONT_BUFFER
            } else {
                FrontBufferUtils.BaseFlags
            }
        }
    }
}