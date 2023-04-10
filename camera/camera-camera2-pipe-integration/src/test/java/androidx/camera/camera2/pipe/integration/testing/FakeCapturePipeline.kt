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

import android.hardware.camera2.CameraDevice
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.integration.impl.CapturePipeline
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class FakeCapturePipeline(
    override var template: Int = CameraDevice.TEMPLATE_PREVIEW,
) : CapturePipeline {

    override suspend fun submitStillCaptures(
        requests: List<Request>,
        captureMode: Int,
        flashType: Int,
        flashMode: Int
    ): List<Deferred<Void?>> {
        return requests.map {
            CompletableDeferred(null)
        }
    }
}