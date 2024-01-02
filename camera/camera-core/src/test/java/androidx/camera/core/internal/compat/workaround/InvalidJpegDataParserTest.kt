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

package androidx.camera.core.internal.compat.workaround

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

// Correct Jpeg byte array 1 that has additional segment data in the end of the file
private val correctJpegByteArray1 = listOf(
    0xff, 0xd8, 0xff, 0xe1, 0x00, 0x06, 0x55, 0x55, 0x55, 0x55, 0xff, 0xda, 0x99, 0x99, 0x99, 0x99,
    0xff, 0xd9, 0xff, 0x00, 0x00, 0xe5, 0x92, 0x00, 0x00, 0xe6, 0x01, 0x00
).map { it.toByte() }.toByteArray()

// Correct Jpeg byte array 2 that doesn't have additional segment data in the end of the file
private val correctJpegByteArray2 = listOf(
    0xff, 0xd8, 0xff, 0xe1, 0x00, 0x06, 0x55, 0x55, 0x55, 0x55, 0xff, 0xda, 0x99, 0x99, 0x99, 0x99,
    0xff, 0xd9
).map { it.toByte() }.toByteArray()

// Problematic data with one segment of redundant 0 padding data.
private val problematicJpegByteArray = listOf(
    0xff, 0xd8, 0xff, 0xe1, 0x00, 0x06, 0x55, 0x55, 0x55, 0x55, 0xff, 0xda, 0x99, 0x99, 0x99, 0x99,
    0xff, 0xd9, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0xff, 0x00, 0x00, 0xe5, 0x92, 0x00, 0x00, 0xe6, 0x01, 0x00
).map { it.toByte() }.toByteArray()

// Problematic data with two segments of redundant 0 padding data.
private val problematicJpegByteArray2 = listOf(
    0xff, 0xd8, 0xff, 0xe1, 0x00, 0x06, 0x55, 0x55, 0x55, 0x55, 0xff, 0xda, 0x99, 0x99, 0x99, 0x99,
    0xff, 0xd9, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0xff, 0x00, 0x00, 0xe5, 0x92, 0x00, 0x00, 0xe6, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
).map { it.toByte() }.toByteArray()

// Invalid very short data
private val invalidVeryShortData = listOf(
    0xff, 0xd8
).map { it.toByte() }.toByteArray()

// Invalid data without SOS byte
private val invalidNoSosData = listOf(
    0xff, 0xd8, 0xff, 0xe1, 0x00, 0x06, 0x55, 0x55, 0x55, 0x55, 0xff, 0x00, 0x99, 0x99, 0x99, 0x99,
    0xff, 0xd9, 0xff, 0x00, 0x00, 0xe5, 0x92, 0x00, 0x00, 0xe6, 0x01, 0x00
).map { it.toByte() }.toByteArray()

// Invalid data without EOI byte
private val invalidNoEoiData = listOf(
    0xff, 0xd8, 0xff, 0xe1, 0x00, 0x06, 0x55, 0x55, 0x55, 0x55, 0xff, 0xda, 0x99, 0x99, 0x99, 0x99,
    0xff, 0x00, 0xff, 0x00, 0x00, 0xe5, 0x92, 0x00, 0x00, 0xe6, 0x01, 0x00
).map { it.toByte() }.toByteArray()

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class InvalidJpegDataParserTest(
    private val brand: String,
    private val model: String,
    private val data: ByteArray,
    private val validDataLength: Int,
) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "brand={0}, model={1}, data={2}, length={3}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(arrayOf("SAMSUNG", "SM-A520F", problematicJpegByteArray, 18))
            add(arrayOf("SAMSUNG", "SM-A520F", problematicJpegByteArray2, 18))
            add(arrayOf("SAMSUNG", "SM-A520F", correctJpegByteArray1, 18))
            add(arrayOf("SAMSUNG", "SM-A520F", correctJpegByteArray2, 18))
            add(arrayOf("SAMSUNG", "SM-A520F", invalidVeryShortData, 2))
            add(arrayOf("SAMSUNG", "SM-A520F", invalidNoSosData, 28))
            add(arrayOf("SAMSUNG", "SM-A520F", invalidNoEoiData, 28))
            add(arrayOf("fake-brand", "fake-model", problematicJpegByteArray, 42))
            add(arrayOf("fake-brand", "fake-model", problematicJpegByteArray2, 64))
            add(arrayOf("fake-brand", "fake-model", correctJpegByteArray1, 28))
            add(arrayOf("fake-brand", "fake-model", correctJpegByteArray2, 18))
        }
    }

    @Test
    fun canGetValidJpegDataLength() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", brand)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model)
        assertThat(InvalidJpegDataParser().getValidDataLength(data)).isEqualTo(validDataLength)
    }
}
