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

package androidx.camera.camera2.pipe.testing

import android.view.Surface
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.compat.CameraDeviceWrapper
import androidx.camera.camera2.pipe.compat.CaptureSessionFactory
import androidx.camera.camera2.pipe.compat.CaptureSessionState
import androidx.camera.camera2.pipe.compat.OutputConfigurationWrapper

/**
 * Fake [CaptureSessionFactory] for use in tests. This fake does NOT invoke callbacks.
 */
internal class FakeCaptureSessionFactory(
    private val requiredStreams: Set<StreamId>,
    private val deferrableStreams: Set<StreamId>
) : CaptureSessionFactory {
    private val allStreams = requiredStreams + deferrableStreams

    override fun create(
        cameraDevice: CameraDeviceWrapper,
        surfaces: Map<StreamId, Surface>,
        captureSessionState: CaptureSessionState
    ): Map<StreamId, OutputConfigurationWrapper> {
        check(allStreams.containsAll(surfaces.keys)) {
            "Unexpected streams passed to create! Keys should be one of $allStreams but actual" +
                " keys were ${surfaces.keys}."
        }
        check(surfaces.keys.containsAll(requiredStreams)) {
            "Create was called without providing all required streams ($requiredStreams). Actual" +
                " keys were ${surfaces.keys}."
        }

        val deferredStreams = allStreams - surfaces.keys
        check(deferrableStreams.containsAll(deferredStreams))
        return deferredStreams.associateWith { FakeOutputConfigurationWrapper() }
    }
}