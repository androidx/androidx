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

package androidx.camera.camera2.internal.compat.workaround

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.common.truth.Truth.assertThat
import java.nio.BufferUnderflowException
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

private const val FAKE_OEM = "fake_oem"

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class FlashAvailabilityCheckerTest(
    private val manufacturer: String,
    private val model: String,
    private val characteristicsProvider: CameraCharacteristicsProvider
) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "manufacturer={0}, model={1}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(arrayOf("sprd", "LEMP", BufferUnderflowProvider()))
            add(arrayOf("sprd", "DM20C", BufferUnderflowProvider()))
            add(arrayOf(FAKE_OEM, "unexpected_throwing_device", BufferUnderflowProvider()))
            add(arrayOf(FAKE_OEM, "not_a_real_device", FlashAvailabilityTrueProvider()))
            add(arrayOf(FAKE_OEM, "null_returning_device", FlashAvailabilityNullProvider()))
        }
    }

    @Before
    fun setup() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", manufacturer)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model)
    }

    @Test
    fun isFlashAvailable_doesNotThrow_whenRethrowDisabled() {
        FlashAvailabilityChecker.isFlashAvailable(characteristicsProvider)
    }

    @Test
    fun isFlashAvailable_throwsForUnexpectedDevice() {
        assumeTrue(Build.MODEL == "unexpected_throwing_device")
        assertThrows(BufferUnderflowException::class.java) {
            FlashAvailabilityChecker.isFlashAvailable(/*rethrowOnError=*/true,
                characteristicsProvider
            )
        }
    }

    @Test
    fun isFlashAvailable_returnsFalse_whenFlashAvailableReturnsNull() {
        assumeTrue(Build.MODEL == "null_returning_device")

        assertThat(FlashAvailabilityChecker.isFlashAvailable(characteristicsProvider)).isFalse()
    }
}

@RequiresApi(21)
private class FlashAvailabilityTrueProvider : CameraCharacteristicsProvider {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> get(key: CameraCharacteristics.Key<T>): T? = when (key) {
        CameraCharacteristics.FLASH_INFO_AVAILABLE -> true as T?
        else -> null
    }
}

@RequiresApi(21)
private class BufferUnderflowProvider : CameraCharacteristicsProvider {
    override fun <T : Any?> get(key: CameraCharacteristics.Key<T>): T =
        throw BufferUnderflowException()
}

@RequiresApi(21)
private class FlashAvailabilityNullProvider : CameraCharacteristicsProvider {
    override fun <T : Any?> get(key: CameraCharacteristics.Key<T>): T? = null
}
