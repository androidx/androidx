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

import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat

/**
 * Fake CameraGraph configuration that can be used for more complicated tests that need a realistic
 * configuration for tests.
 */
internal class FakeCameraGraphConfig {
    private val camera1 = CameraId("TestCamera-1")
    private val camera2 = CameraId("TestCamera-2")

    val fakeMetadata = FakeCameraMetadata(
        mapOf(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
        ),
        cameraId = camera1
    )

    val streamConfig1 = CameraStream.Config.create(
        size = Size(100, 100),
        format = StreamFormat.YUV_420_888
    )
    val streamConfig2 = CameraStream.Config.create(
        size = Size(123, 321),
        format = StreamFormat.YUV_420_888,
        camera = camera1
    )
    val streamConfig3 = CameraStream.Config.create(
        size = Size(200, 200),
        format = StreamFormat.YUV_420_888,
        camera = camera2,
        outputType = OutputStream.OutputType.SURFACE_TEXTURE
    )
    val sharedOutputConfig = OutputStream.Config.create(
        size = Size(200, 200),
        format = StreamFormat.YUV_420_888,
        camera = camera1
    )
    val sharedStreamConfig1 = CameraStream.Config.create(sharedOutputConfig)
    val sharedStreamConfig2 = CameraStream.Config.create(sharedOutputConfig)

    val graphConfig = CameraGraph.Config(
        camera = camera1,
        streams = listOf(
            streamConfig1,
            streamConfig2,
            streamConfig3,
            sharedStreamConfig1,
            sharedStreamConfig2
        ),
        streamSharingGroups = listOf(listOf(streamConfig1, streamConfig2))
    )
}