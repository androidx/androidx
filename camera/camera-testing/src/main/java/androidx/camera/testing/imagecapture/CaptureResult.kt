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

package androidx.camera.testing.imagecapture

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.camera.testing.fakes.FakeCameraCaptureResult
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.imagecapture.CaptureResult.Companion.CAPTURE_STATUS_CANCELLED
import androidx.camera.testing.imagecapture.CaptureResult.Companion.CAPTURE_STATUS_FAILED
import androidx.camera.testing.imagecapture.CaptureResult.Companion.CAPTURE_STATUS_SUCCESSFUL

/**
 * The capture result info used for a fake image capture completion.
 *
 * If [captureStatus] is [CAPTURE_STATUS_SUCCESSFUL], [cameraCaptureResult] is guaranteed to be
 * non-null (`FakeCameraCaptureResult()` is used by default if user didn't provide any).
 *
 * If [captureStatus] is [CAPTURE_STATUS_FAILED] or [CAPTURE_STATUS_CANCELLED],
 * [cameraCaptureResult] is usually null.
 *
 * @see FakeCameraControl.submitCaptureResult
 */
public class CaptureResult
private constructor(
    public val captureStatus: @CaptureStatus Int,
    public val cameraCaptureResult: FakeCameraCaptureResult? = null
) {
    /**
     * The capture result status used in fake image capture completion.
     *
     * @see CaptureResult
     */
    @Target(AnnotationTarget.TYPE)
    @IntDef(CAPTURE_STATUS_SUCCESSFUL, CAPTURE_STATUS_FAILED, CAPTURE_STATUS_CANCELLED)
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public annotation class CaptureStatus

    public companion object {
        /** Represents a successful [CaptureStatus]. */
        public const val CAPTURE_STATUS_SUCCESSFUL: Int = 0

        /** Represents a failed [CaptureStatus]. */
        public const val CAPTURE_STATUS_FAILED: Int = 1

        /** Represents a canceled [CaptureStatus]. */
        public const val CAPTURE_STATUS_CANCELLED: Int = 2

        /** Represents a successful [CaptureResult]. */
        @JvmStatic
        @JvmOverloads
        public fun successfulResult(
            fakeCameraCaptureResult: FakeCameraCaptureResult = FakeCameraCaptureResult()
        ): CaptureResult =
            CaptureResult(
                captureStatus = CAPTURE_STATUS_SUCCESSFUL,
                cameraCaptureResult = fakeCameraCaptureResult
            )

        /** Represents a failed [CaptureResult]. */
        @JvmStatic
        public fun failedResult(): CaptureResult =
            CaptureResult(captureStatus = CAPTURE_STATUS_FAILED)

        /** Represents a cancelled [CaptureResult]. */
        @JvmStatic
        public fun cancelledResult(): CaptureResult =
            CaptureResult(
                captureStatus = CAPTURE_STATUS_CANCELLED,
            )
    }
}
