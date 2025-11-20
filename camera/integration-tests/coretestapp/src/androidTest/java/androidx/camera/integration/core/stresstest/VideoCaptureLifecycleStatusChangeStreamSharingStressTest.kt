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

package androidx.camera.integration.core.stresstest

import androidx.camera.core.CameraXConfig
import androidx.test.filters.LargeTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class VideoCaptureLifecycleStatusChangeStreamSharingStressTest(
    implName: String,
    cameraConfig: CameraXConfig,
    cameraId: String,
) : VideoCaptureLifecycleStatusChangeStressTestBase(implName, cameraConfig, cameraId) {

    override val enableStreamSharing: Boolean = true
}
