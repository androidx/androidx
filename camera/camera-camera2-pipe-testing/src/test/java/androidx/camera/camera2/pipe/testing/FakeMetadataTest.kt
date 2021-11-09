/*
 * Copyright 2020 The Android Open Source Project
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
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.MetadataTransform
import androidx.camera.camera2.pipe.Request
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.robolectric.annotation.Config

@RunWith(JUnit4::class)
public class MetadataTest {
    @Test
    public fun testMetadataCanRetrieveValues() {
        val metadata = FakeMetadata(mapOf(FakeMetadata.TEST_KEY to 42))

        assertThat(metadata[FakeMetadata.TEST_KEY]).isNotNull()
        assertThat(metadata[FakeMetadata.TEST_KEY_ABSENT]).isNull()

        assertThat(metadata.getOrDefault(FakeMetadata.TEST_KEY, 84))
            .isEqualTo(42)
        assertThat(metadata.getOrDefault(FakeMetadata.TEST_KEY_ABSENT, 84))
            .isEqualTo(84)
    }
}

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CameraMetadataTest {
    @Test
    fun cameraMetadataIsNotEqual() {
        val metadata1 = FakeCameraMetadata(
            mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT),
            mapOf(FakeMetadata.TEST_KEY to 42)
        )
        val metadata2 = FakeCameraMetadata(
            mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT),
            mapOf(FakeMetadata.TEST_KEY to 42)
        )

        assertThat(metadata1).isNotEqualTo(metadata2)
        assertThat(metadata1.camera).isNotEqualTo(metadata2.camera)
    }

    @Test
    public fun canRetrieveCameraCharacteristicsOrCameraMetadataViaInterface() {
        val metadata = FakeCameraMetadata(
            mapOf(
                CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT
            ),
            mapOf(FakeMetadata.TEST_KEY to 42)
        )

        assertThat(metadata[FakeMetadata.TEST_KEY]).isNotNull()
        assertThat(metadata[FakeMetadata.TEST_KEY_ABSENT]).isNull()

        assertThat(metadata[CameraCharacteristics.LENS_FACING]).isNotNull()
        assertThat(metadata[CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES]).isNull()
    }
}

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class RequestMetadataTest {

    @Test
    public fun canRetrieveCaptureRequestOrCameraMetadataViaInterface() {
        val requestMetadata = FakeRequestMetadata(
            requestParameters = mapOf(CaptureRequest.JPEG_QUALITY to 95),
            request = Request(
                streams = listOf(),
                parameters = mapOf(CaptureRequest.JPEG_QUALITY to 20),
                extras = mapOf(FakeMetadata.TEST_KEY to 42)
            )
        )

        assertThat(requestMetadata[CaptureRequest.JPEG_QUALITY]).isEqualTo(95)
        assertThat(requestMetadata[CaptureRequest.COLOR_CORRECTION_MODE]).isNull()
        assertThat(requestMetadata[FakeMetadata.TEST_KEY]).isEqualTo(42)
        assertThat(requestMetadata[FakeMetadata.TEST_KEY_ABSENT]).isNull()

        assertThat(requestMetadata.request[CaptureRequest.JPEG_QUALITY]).isEqualTo(20)
        assertThat(requestMetadata.request[CaptureRequest.COLOR_CORRECTION_MODE]).isNull()
        assertThat(requestMetadata.request[FakeMetadata.TEST_KEY]).isEqualTo(42)
        assertThat(requestMetadata.request[FakeMetadata.TEST_KEY_ABSENT]).isNull()
    }
}

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class FrameMetadataTest {
    @Test
    public fun canRetrieveCaptureRequestOrCameraMetadataViaInterface() {
        val metadata = FakeFrameMetadata(
            resultMetadata = mapOf(CaptureResult.JPEG_QUALITY to 95),
            extraResultMetadata = mapOf(FakeMetadata.TEST_KEY to 42)
        )

        assertThat(metadata[FakeMetadata.TEST_KEY]).isNotNull()
        assertThat(metadata[FakeMetadata.TEST_KEY_ABSENT]).isNull()

        assertThat(metadata[CaptureResult.JPEG_QUALITY]).isNotNull()
        assertThat(metadata[CaptureResult.COLOR_CORRECTION_MODE]).isNull()
    }
}

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class MetadataTransformTest {
    private val metadata = FakeCameraMetadata(
        mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT),
        mapOf(FakeMetadata.TEST_KEY to 42)
    )

    private val requestMetadata = FakeRequestMetadata(
        requestParameters = mapOf(CaptureRequest.JPEG_QUALITY to 95),
        request = Request(
            streams = listOf(),
            parameters = mapOf(CaptureRequest.JPEG_QUALITY to 20),
            extras = mapOf(FakeMetadata.TEST_KEY to 42)
        )
    )

    private val fakeFrameInfo = FakeFrameInfo(
        requestMetadata = requestMetadata
    )

    @Test
    public fun defaultMetadataTransformIsNoOp() {
        val transform = MetadataTransform()
        val overrides = transform.transformFn.computeOverridesFor(
            fakeFrameInfo,
            CameraId("Fake"),
            listOf()
        )

        assertThat(overrides).isEmpty()
    }

    @Test
    public fun canCreateAndInvokeMetadataTransform() {
        val transform = MetadataTransform(
            transformFn = object : MetadataTransform.TransformFn {
                override fun computeOverridesFor(
                    result: FrameInfo,
                    camera: CameraId,
                    related: List<FrameInfo?>
                ): Map<*, Any?> {
                    return mapOf(FakeMetadata.TEST_KEY to result.frameNumber.value)
                }
            }
        )
        val overrides = transform.transformFn.computeOverridesFor(
            FakeFrameInfo(
                metadata = FakeFrameMetadata(
                    frameNumber = FrameNumber(128)
                )
            ),
            CameraId("Fake"),
            listOf()
        )

        assertThat(overrides).hasSize(1)
        assertThat(overrides[FakeMetadata.TEST_KEY]).isEqualTo(128)
    }

    @Test
    public fun canUseCameraMetadataForTransforms() {
        val transform = MetadataTransform(
            transformFn = object : MetadataTransform.TransformFn {
                override fun computeOverridesFor(
                    result: FrameInfo,
                    camera: CameraId,
                    related: List<FrameInfo?>
                ): Map<*, Any?> {
                    return mapOf(FakeMetadata.TEST_KEY to metadata[FakeMetadata.TEST_KEY])
                }
            }
        )
        val overrides = transform.transformFn.computeOverridesFor(
            fakeFrameInfo,
            CameraId("Fake"),
            listOf()
        )

        assertThat(overrides).hasSize(1)
        assertThat(overrides[FakeMetadata.TEST_KEY]).isEqualTo(42)
    }
}
