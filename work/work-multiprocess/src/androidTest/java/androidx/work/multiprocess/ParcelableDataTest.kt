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

package androidx.work.multiprocess

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.Data
import androidx.work.multiprocess.parcelable.ParcelConverters
import androidx.work.multiprocess.parcelable.ParcelableData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public class ParcelableDataTest {

    @Test
    @SmallTest
    public fun testParcelableData() {
        if (Build.VERSION.SDK_INT <= 27) {
            // Exclude <= API 27, from tests because it causes a SIGSEGV.
            return
        }

        val data = createData()
        val parcelableData = ParcelableData(data)
        val parcelled: ParcelableData =
            ParcelConverters.unmarshall(
                ParcelConverters.marshall(parcelableData),
                ParcelableData.CREATOR
            )
        assertEquals(data, parcelled.data)
    }

    private fun createData(): Data {
        val map = mutableMapOf<String, Any?>()
        map["byte"] = 1.toByte()
        map["boolean"] = false
        map["int"] = 1
        map["long"] = 10L
        map["float"] = 99f
        map["double"] = 99.0
        map["string"] = "two"
        map["byte array"] = byteArrayOf(1, 2, 3)
        map["boolean array"] = booleanArrayOf(true, false, true)
        map["int array"] = intArrayOf(1, 2, 3)
        map["long array"] = longArrayOf(1L, 2L, 3L)
        map["float array"] = floatArrayOf(1f, 2f, 3f)
        map["double array"] = doubleArrayOf(1.0, 2.0, 3.0)
        map["string array"] = listOf("a", "b", "c").toTypedArray()
        map["null"] = null
        val dataBuilder = Data.Builder()
        dataBuilder.putAll(map)
        return dataBuilder.build()
    }
}
