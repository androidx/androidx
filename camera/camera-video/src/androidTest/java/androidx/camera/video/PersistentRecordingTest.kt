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

package androidx.camera.video

import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.test.filters.LargeTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests persistent recording.
 *
 * StreamSharing is not within the scope of this test.
 *
 * @see [PersistentRecordingStreamSharingTest] for persistent recording with StreamSharing enabled.
 */
@LargeTest
@RunWith(Parameterized::class)
class PersistentRecordingTest(
    private val implName: String,
    private var cameraSelector: CameraSelector,
    private val cameraConfig: CameraXConfig,
) : PersistentRecordingTestBase(implName, cameraSelector, cameraConfig) {

    override val testTag: String = "PersistentRecordingTest"
    override val enableStreamSharing: Boolean = false
}
