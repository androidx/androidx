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
import android.hardware.camera2.CaptureRequest
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat

/**
 * Fake CameraGraph configuration that can be used for more complicated tests that need a realistic
 * configuration for tests.
 */
@RequiresApi(21)
internal object FakeGraphConfigs {
    private val camera1 = CameraId("TestCamera-1")
    private val camera2 = CameraId("TestCamera-2")

    val fakeMetadata =
        FakeCameraMetadata(
            mapOf(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK
            ),
            cameraId = camera1
        )
    val fakeMetadata2 =
        FakeCameraMetadata(
            mapOf(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT
            ),
            cameraId = camera2
        )
    val fakeCameraBackend =
        FakeCameraBackend(
            mapOf(fakeMetadata.camera to fakeMetadata, fakeMetadata2.camera to fakeMetadata2)
        )

    val streamConfig1 =
        CameraStream.Config.create(size = Size(100, 100), format = StreamFormat.YUV_420_888)
    val streamConfig2 =
        CameraStream.Config.create(
            size = Size(123, 321), format = StreamFormat.YUV_420_888, camera = camera1
        )
    val streamConfig3 =
        CameraStream.Config.create(
            size = Size(200, 200),
            format = StreamFormat.YUV_420_888,
            camera = camera2,
            outputType = OutputStream.OutputType.SURFACE_TEXTURE
        )
    val streamConfig4 =
        CameraStream.Config.create(
            size = Size(200, 200),
            format = StreamFormat.YUV_420_888,
            camera = camera2,
            outputType = OutputStream.OutputType.SURFACE_TEXTURE,
            mirrorMode = OutputStream.MirrorMode.MIRROR_MODE_H
        )
    val streamConfig5 =
        CameraStream.Config.create(
            size = Size(200, 200),
            format = StreamFormat.YUV_420_888,
            camera = camera2,
            outputType = OutputStream.OutputType.SURFACE_TEXTURE,
            mirrorMode = OutputStream.MirrorMode.MIRROR_MODE_AUTO,
            timestampBase = OutputStream.TimestampBase.TIMESTAMP_BASE_MONOTONIC
        )
    val streamConfig6 =
        CameraStream.Config.create(
            size = Size(200, 200),
            format = StreamFormat.YUV_420_888,
            camera = camera2,
            outputType = OutputStream.OutputType.SURFACE_TEXTURE,
            mirrorMode = OutputStream.MirrorMode.MIRROR_MODE_AUTO,
            timestampBase = OutputStream.TimestampBase.TIMESTAMP_BASE_DEFAULT,
            dynamicRangeProfile = OutputStream.DynamicRangeProfile.PUBLIC_MAX
        )

    val streamConfig7 =
        CameraStream.Config.create(
            size = Size(200, 200),
            format = StreamFormat.YUV_420_888,
            camera = camera2,
            outputType = OutputStream.OutputType.SURFACE_TEXTURE,
            mirrorMode = OutputStream.MirrorMode.MIRROR_MODE_AUTO,
            timestampBase = OutputStream.TimestampBase.TIMESTAMP_BASE_DEFAULT,
            dynamicRangeProfile = OutputStream.DynamicRangeProfile.STANDARD,
            streamUseCase = OutputStream.StreamUseCase.VIDEO_RECORD
        )

    val streamConfig8 =
        CameraStream.Config.create(
            size = Size(200, 200),
            format = StreamFormat.UNKNOWN,
            camera = camera2,
            outputType = OutputStream.OutputType.SURFACE_TEXTURE,
            mirrorMode = OutputStream.MirrorMode.MIRROR_MODE_AUTO,
            timestampBase = OutputStream.TimestampBase.TIMESTAMP_BASE_DEFAULT,
            dynamicRangeProfile = OutputStream.DynamicRangeProfile.STANDARD,
            streamUseHint = OutputStream.StreamUseHint.VIDEO_RECORD
        )

    val sharedOutputConfig =
        OutputStream.Config.create(
            size = Size(200, 200), format = StreamFormat.YUV_420_888, camera = camera1
        )
    val sharedStreamConfig1 = CameraStream.Config.create(sharedOutputConfig)
    val sharedStreamConfig2 = CameraStream.Config.create(sharedOutputConfig)

    val graphConfig =
        CameraGraph.Config(
            camera = camera1,
            streams =
            listOf(
                streamConfig1,
                streamConfig2,
                streamConfig3,
                streamConfig4,
                streamConfig5,
                streamConfig6,
                streamConfig7,
                streamConfig8,
                sharedStreamConfig1,
                sharedStreamConfig2
            ),
            streamSharingGroups = listOf(listOf(streamConfig1, streamConfig2)),
            defaultParameters = mapOf(CaptureRequest.JPEG_THUMBNAIL_QUALITY to 24),
            requiredParameters = mapOf(CaptureRequest.JPEG_THUMBNAIL_QUALITY to 42)
        )
}
