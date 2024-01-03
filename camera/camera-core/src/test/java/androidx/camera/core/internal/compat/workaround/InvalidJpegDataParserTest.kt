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
import android.util.Range
import com.google.common.truth.Truth.assertThat
import java.util.Objects
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

// Invalid data starts from position 18 to position 31.
private val problematicJpegByteArray = listOf(
    0xff, 0xd8, 0xff, 0xe1, 0x00, 0x06, 0x55, 0x55, 0x55, 0x55, 0xff, 0xda, 0x99, 0x99, 0x99, 0x99,
    0xff, 0xd9, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0xff, 0x00, 0x00, 0xe5, 0x92, 0x00, 0x00, 0xe6, 0x01, 0x00
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
    private val model: String,
    private val data: ByteArray,
    private val range: Range<*>?,
    ) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "model={0}, data={1}, range={2}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(arrayOf("SM-A520F", problematicJpegByteArray, Range.create(18, 31)))
            add(arrayOf("SM-A520F", correctJpegByteArray1, null))
            add(arrayOf("SM-A520F", correctJpegByteArray2, null))
            add(arrayOf("SM-A520F", invalidVeryShortData, null))
            add(arrayOf("SM-A520F", invalidNoSosData, null))
            add(arrayOf("SM-A520F", invalidNoEoiData, null))
            add(arrayOf("fake-model", problematicJpegByteArray, null))
            add(arrayOf("fake-model", correctJpegByteArray1, null))
            add(arrayOf("fake-model", correctJpegByteArray2, null))
        }
    }

    @Test
    fun canGetInvalidJpegDataRange() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", model)
        assertThat(
            Objects.equals(
                InvalidJpegDataParser().getInvalidDataRange(data),
                range
            )
        ).isTrue()
    }
}
