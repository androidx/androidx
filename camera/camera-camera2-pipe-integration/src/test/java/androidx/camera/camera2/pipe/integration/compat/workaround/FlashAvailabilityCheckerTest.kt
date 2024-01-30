/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.camera2.pipe.CameraExtensionMetadata
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.integration.compat.workaround.FlashAvailabilityCheckerTest.TestCameraMetadata.Mode
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import com.google.common.truth.Truth
import java.nio.BufferUnderflowException
import kotlin.reflect.KClass
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class FlashAvailabilityCheckerTest(
    private val manufacturer: String,
    private val model: String,
    private val cameraProperties: CameraProperties
) {
    @Before
    fun setup() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", manufacturer)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model)
    }

    @Test
    fun isFlashAvailable_doesNotThrow_whenRethrowDisabled() {
        cameraProperties.isFlashAvailable()
    }

    @Test
    fun isFlashAvailable_throwsForUnexpectedDevice() {
        Assume.assumeTrue(Build.MODEL == "unexpected_throwing_device")
        Assert.assertThrows(BufferUnderflowException::class.java) {
            cameraProperties.isFlashAvailable(/*rethrowOnError=*/true)
        }
    }

    @Test
    fun isFlashAvailable_returnsFalse_whenFlashAvailableReturnsNull() {
        Assume.assumeTrue(Build.MODEL == "null_returning_device")

        Truth.assertThat(cameraProperties.isFlashAvailable()).isFalse()
    }

    private class TestCameraMetadata(
        private val mode: Mode = Mode.DEFAULT,
        private val characteristics: Map<CameraCharacteristics.Key<*>, Any?> = emptyMap(),
        val metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
        cameraId: CameraId = CameraId("0"),
        override val keys: Set<CameraCharacteristics.Key<*>> = emptySet(),
        override val requestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
        override val resultKeys: Set<CaptureResult.Key<*>> = emptySet(),
        override val sessionKeys: Set<CaptureRequest.Key<*>> = emptySet(),
        val physicalMetadata: Map<CameraId, CameraMetadata> = emptyMap(),
        override val physicalRequestKeys: Set<CaptureRequest.Key<*>> = emptySet(),
    ) : CameraMetadata {
        enum class Mode {
            ALWAYS_NULL,
            THROW_BUFFER_UNDERFLOW_EXCEPTION,
            DEFAULT,
        }

        override fun <T> get(key: CameraCharacteristics.Key<T>): T? {
            @Suppress("UNCHECKED_CAST")
            return when (mode) {
                Mode.ALWAYS_NULL -> null
                Mode.THROW_BUFFER_UNDERFLOW_EXCEPTION -> throw BufferUnderflowException()
                else -> characteristics[key] as T?
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> get(key: Metadata.Key<T>): T? = metadata[key] as T?

        override fun <T> getOrDefault(key: CameraCharacteristics.Key<T>, default: T): T =
            get(key) ?: default

        override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T = get(key) ?: default

        override val camera: CameraId = cameraId
        override val isRedacted: Boolean = false

        override val physicalCameraIds: Set<CameraId> = physicalMetadata.keys
        override val supportedExtensions: Set<Int>
            get() = TODO("b/299356087 - Add support for fake extension metadata")

        override suspend fun getPhysicalMetadata(cameraId: CameraId): CameraMetadata =
            physicalMetadata[cameraId]!!

        override fun awaitPhysicalMetadata(cameraId: CameraId): CameraMetadata =
            physicalMetadata[cameraId]!!

        override suspend fun getExtensionMetadata(extension: Int): CameraExtensionMetadata {
            TODO("b/299356087 - Add support for fake extension metadata")
        }

        override fun awaitExtensionMetadata(extension: Int): CameraExtensionMetadata {
            TODO("b/299356087 - Add support for fake extension metadata")
        }

        override fun <T : Any> unwrapAs(type: KClass<T>): T? {
            TODO("Not yet implemented")
        }
    }

    companion object {
        private const val FAKE_OEM = "fake_oem"
        private val flashAvailabilityTrueProvider = FakeCameraProperties(
            metadata = TestCameraMetadata(
                characteristics = mapOf(CameraCharacteristics.FLASH_INFO_AVAILABLE to true)
            )
        )
        private val bufferUnderflowProvider = FakeCameraProperties(
            metadata = TestCameraMetadata(mode = Mode.THROW_BUFFER_UNDERFLOW_EXCEPTION)
        )
        private val flashAvailabilityNullProvider = FakeCameraProperties(
            metadata = TestCameraMetadata(mode = Mode.ALWAYS_NULL)
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "manufacturer={0}, model={1}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(arrayOf("sprd", "LEMP", bufferUnderflowProvider))
            add(arrayOf("sprd", "DM20C", bufferUnderflowProvider))
            add(arrayOf(FAKE_OEM, "unexpected_throwing_device", bufferUnderflowProvider))
            add(arrayOf(FAKE_OEM, "not_a_real_device", flashAvailabilityTrueProvider))
            add(arrayOf(FAKE_OEM, "null_returning_device", flashAvailabilityNullProvider))
        }
    }
}
