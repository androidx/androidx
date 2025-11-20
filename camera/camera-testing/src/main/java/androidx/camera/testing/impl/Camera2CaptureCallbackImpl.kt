/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.testing.impl

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

public class Camera2CaptureCallbackImpl : CameraCaptureSession.CaptureCallback() {
    public data class Verification(
        val condition:
            (captureRequest: CaptureRequest, captureResult: TotalCaptureResult) -> Boolean,
        val isVerified: CompletableDeferred<Unit>,
    )

    private var pendingVerifications = mutableListOf<Verification>()

    /** Returns a [Deferred] representing if verification has been completed */
    public fun verify(
        condition: (captureRequest: CaptureRequest, captureResult: TotalCaptureResult) -> Boolean =
            { _, _ ->
                false
            }
    ): Deferred<Unit> =
        CompletableDeferred<Unit>().apply {
            val verification = Verification(condition, this)
            pendingVerifications.add(verification)

            invokeOnCompletion { pendingVerifications.remove(verification) }
        }

    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult,
    ) {
        pendingVerifications.forEach {
            if (it.condition(request, result)) {
                it.isVerified.complete(Unit)
            }
        }
    }
}
