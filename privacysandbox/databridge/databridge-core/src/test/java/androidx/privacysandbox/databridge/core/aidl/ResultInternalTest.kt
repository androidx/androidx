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
import org.robolectric.annotation.Config

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class ResultInternalTest(
    private val keyName: String,
    private val exceptionName: String?,
    private val exceptionString: String?,
    private val valueInternal: ValueInternal?,
) {

    @Rule @JvmField val expect = Expect.create()

    @Test
    fun testResultInternal() {
        val original = ResultInternal(keyName, exceptionName, exceptionString, valueInternal)
        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = ResultInternal.CREATOR.createFromParcel(parcel)
        expect.that(restored.keyName).isEqualTo(original.keyName)
        expect.that(restored.exceptionName).isEqualTo(original.exceptionName)
        expect.that(restored.exceptionMessage).isEqualTo(original.exceptionMessage)

        expect.that(restored.valueInternal?.type).isEqualTo(original.valueInternal?.type)
        expect
            .that(restored.valueInternal?.isValueNull)
            .isEqualTo(original.valueInternal?.isValueNull)
        expect.that(restored.valueInternal?.value).isEqualTo(original.valueInternal?.value)
        // This verifies there are no bytes left to be read on the Parcel.
        parcel.enforceNoDataAvail()
        parcel.recycle()
    }

    companion object {
        @ParameterizedRobolectricTestRunner.Parameters(
            name =
                "{index}: keyName={0}, exceptionName={1}, exceptionMessage={2}, valueInternal={3}"
        )
        @JvmStatic
        fun data(): Collection<Array<Any?>> {
            return listOf(
                arrayOf("intKey", null, null, null),
                arrayOf("intKey", "ClassCastException", "Int cannot be casted to String", null),
                arrayOf("intKey", null, null, ValueInternal("INT", false, value = 1)),
                arrayOf("stringKey", null, null, ValueInternal("STRING", value = "stringValue")),
            )
        }
    }
}
