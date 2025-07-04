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

package androidx.privacysandbox.databridge.core.aidl

import android.os.Build
import android.os.Parcel
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Expect
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
class ValueInternalParametrizedTest(
    private val type: String,
    private val isValueNull: Boolean,
    private val value: Any?,
) {

    @Rule @JvmField val expect = Expect.create()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun testValueInternal() {
        val original = ValueInternal(type, isValueNull, value)
        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = ValueInternal.CREATOR.createFromParcel(parcel)
        expect.that(restored.type).isEqualTo(original.type)
        expect.that(restored.isValueNull).isEqualTo(original.isValueNull)
        expect.that(restored.value).isEqualTo(original.value)
        // This verifies there are no bytes left to be read on the Parcel.
        parcel.enforceNoDataAvail()
        parcel.recycle()
    }

    companion object {
        @ParameterizedRobolectricTestRunner.Parameters(
            name = "{index}: type={0}, isValueNull={1}, value={2}"
        )
        @JvmStatic
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf("INT", false, 123),
                arrayOf("LONG", false, 1234567890123L),
                arrayOf("FLOAT", false, 12.34f),
                arrayOf("DOUBLE", false, 123.456),
                arrayOf("BOOLEAN", false, true),
                arrayOf("BOOLEAN", false, false),
                arrayOf("STRING", false, "stringValue"),
                arrayOf("STRING_SET", false, setOf("string1", "string2", "string3")),
                arrayOf("BYTE_ARRAY", false, byteArrayOf(1, 2, 3)),
                arrayOf("STRING", true, null),
                arrayOf("INT", true, null),
                arrayOf("BOOLEAN", true, null),
            )
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
class ValueInternalWithExceptionTest {
    /**
     * Tests that attempting to parcel a [ValueInternal] with an unsupported type correctly throws
     * an [IllegalArgumentException].
     */
    @Test(expected = IllegalArgumentException::class)
    fun unsupportedType_throwsExceptionOnWrite() {
        val unsupported = ValueInternal("UNSUPPORTED_CUSTOM_TYPE", false, Any())
        val parcel = Parcel.obtain()
        try {
            unsupported.writeToParcel(parcel, 0)
        } finally {
            parcel.recycle()
        }
    }

    /**
     * Tests that attempting to unparcel a [ValueInternal] where the type string is unsupported
     * correctly throws an [IllegalArgumentException].
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test(expected = IllegalArgumentException::class)
    fun unsupportedType_throwsExceptionOnRead() {
        val parcel = Parcel.obtain()
        // Simulate writing an unsupported type string directly into the parcel
        parcel.writeString("UNSUPPORTED_CUSTOM_TYPE")
        parcel.writeBoolean(
            false
        ) // isValueNull = false, as this case doesn't involve a null value.

        parcel.setDataPosition(0) // Rewind for reading
        try {
            ValueInternal.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }
}
