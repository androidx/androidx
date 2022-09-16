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

package androidx.health.platform.client.proto

import android.content.Intent
import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntentExtTest {

    @Test
    fun byteArrays_notEmpty() {
        val originalArrays = List(1024) { ByteArray(it, Int::toByte) }
        var intent = Intent()
        intent.putByteArraysExtra("key", originalArrays)
        intent = intent.serializeDeserialize()
        val deserializedArrays = intent.getByteArraysExtra("key")

        assertThat(deserializedArrays?.map(ByteArray::toList))
            .containsExactlyElementsIn(originalArrays.map(ByteArray::toList))
    }

    @Test
    fun byteArrays_empty() {
        val originalArrays = emptyList<ByteArray>()
        var intent = Intent()
        intent.putByteArraysExtra("key", originalArrays)
        intent = intent.serializeDeserialize()
        val deserializedArrays = intent.getByteArraysExtra("key")

        assertThat(deserializedArrays).isEmpty()
    }

    @Test
    fun byteArrays_null() {
        val deserializedArrays = Intent().getByteArraysExtra("key")

        assertThat(deserializedArrays).isNull()
    }

    private fun Intent.serializeDeserialize(): Intent {
        val parcel = Parcel.obtain()
        writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val newIntent = Intent()
        newIntent.readFromParcel(parcel)
        parcel.recycle()

        return newIntent
    }
}