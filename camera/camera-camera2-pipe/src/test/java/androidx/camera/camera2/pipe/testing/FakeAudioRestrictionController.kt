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

package androidx.camera.camera2.pipe.testing

import androidx.camera.camera2.pipe.AudioRestrictionMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.compat.AudioRestrictionController
import javax.inject.Inject

internal class FakeAudioRestrictionController @Inject constructor() : AudioRestrictionController {
    override var globalAudioRestrictionMode: AudioRestrictionMode? =
        AudioRestrictionMode.AUDIO_RESTRICTION_NONE

    override fun updateCameraGraphAudioRestrictionMode(
        cameraGraph: CameraGraph,
        mode: AudioRestrictionMode,
    ) {}

    override fun removeCameraGraph(cameraGraph: CameraGraph) {}

    override fun addListener(listener: AudioRestrictionController.Listener) {}

    override fun removeListener(listener: AudioRestrictionController.Listener) {}
}
