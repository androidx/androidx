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
import androidx.graphics.surface.SurfaceControlCompat
import androidx.hardware.USAGE_COMPOSER_OVERLAY

internal class FrontBufferUtils private constructor() {

    companion object {

        internal const val TAG = "FrontBufferUtils"

        /** Flags that are expected to be supported on all [HardwareBuffer] instances */
        @SuppressLint("WrongConstant")
        internal const val BaseFlags =
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or
                HardwareBuffer.USAGE_GPU_COLOR_OUTPUT or
                USAGE_COMPOSER_OVERLAY

        internal fun obtainHardwareBufferUsageFlags(): Long =
            if (!UseCompatSurfaceControl && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                UsageFlagsVerificationHelper.obtainUsageFlagsV33()
            } else {
                BaseFlags
            }

        internal const val UseCompatSurfaceControl = false

        fun configureFrontBufferLayerFrameRate(
            frontBufferSurfaceControl: SurfaceControlCompat,
            frameRate: Float = 1000f,
            transaction: SurfaceControlCompat.Transaction? = null
        ): SurfaceControlCompat.Transaction? {
            var targetTransaction: SurfaceControlCompat.Transaction? = transaction
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (targetTransaction == null) {
                    targetTransaction = SurfaceControlCompat.Transaction()
                }
                targetTransaction.setFrameRate(
                    frontBufferSurfaceControl,
                    frameRate,
                    SurfaceControlCompat.FRAME_RATE_COMPATIBILITY_DEFAULT,
                    SurfaceControlCompat.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS
                )
            }
            return targetTransaction
        }
    }
}

/** Helper class to avoid class verification failures */
@RequiresApi(Build.VERSION_CODES.Q)
internal class UsageFlagsVerificationHelper private constructor() {
    companion object {

        /**
         * Helper method to determine if a particular HardwareBuffer usage flag is supported. Even
         * though the FRONT_BUFFER_USAGE and COMPOSER_OVERLAY flags are introduced in Android T, not
         * all devices may support this flag. So we conduct a capability query with a sample 1x1
         * HardwareBuffer with the provided flag to see if it is compatible
         */
        // Suppressing WrongConstant warnings as we are leveraging a constant with the same value
        // as HardwareBuffer.USAGE_COMPOSER_OVERLAY to avoid SDK checks as the constant has been
        // supported in the NDK for several platform releases.
        // See:
        // developer.android.com/ndk/reference/group/a-hardware-buffer#ahardwarebuffer_usageflags
        @SuppressLint("WrongConstant")
        @RequiresApi(Build.VERSION_CODES.Q)
        internal fun isSupported(flag: Long): Boolean =
            HardwareBuffer.isSupported(
                1, // width
                1, // height
                HardwareBuffer.RGBA_8888, // format
                1, // layers
                FrontBufferUtils.BaseFlags or flag
            )

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun obtainUsageFlagsV33(): Long {
            // First verify if the front buffer usage flag is supported along with the
            // "usage composer overlay" flag that was introduced in API level 33
            return if (isSupported(HardwareBuffer.USAGE_FRONT_BUFFER)) {
                FrontBufferUtils.BaseFlags or HardwareBuffer.USAGE_FRONT_BUFFER
            } else {
                // If the front buffer usage flag is not supported, configure the CPU write flag
                // in order to prevent arm frame buffer compression from causing visual artifacts
                // on certain devices like the Samsung Galaxy Tab S6 lite. See b/365131024
                FrontBufferUtils.BaseFlags or HardwareBuffer.USAGE_CPU_WRITE_OFTEN
            }
        }
    }
}
