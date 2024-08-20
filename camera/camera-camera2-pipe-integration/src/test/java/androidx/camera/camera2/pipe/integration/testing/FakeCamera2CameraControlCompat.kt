/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.testing

import androidx.camera.camera2.pipe.integration.compat.Camera2CameraControlCompat
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

@OptIn(ExperimentalCamera2Interop::class)
class FakeCamera2CameraControlCompat : Camera2CameraControlCompat {
    override fun addRequestOption(bundle: CaptureRequestOptions) {
        // No-op
    }

    override fun getRequestOption(): CaptureRequestOptions {
        return CaptureRequestOptions.Builder().build()
    }

    override fun clearRequestOption() {
        // No-op
    }

    override fun cancelCurrentTask() {
        // No-op
    }

    override fun applyAsync(
        requestControl: UseCaseCameraRequestControl?,
        cancelPreviousTask: Boolean
    ): Deferred<Void?> {
        return CompletableDeferred<Void?>(null).apply { complete(null) }
    }
}
