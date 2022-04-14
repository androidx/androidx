/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.platform.client.impl.data

import android.os.Parcel
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import com.google.protobuf.BytesValue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProtoParcelableTest {

    @Test
    fun storeInPlace() {
        // Small enough that it will be stored in place.
        val protoParcelable =
            TestProtoParcelable(BytesValue.of(ByteString.copyFrom(byteArrayOf(1, 2, 3, 4, 5))))

        assertThat(parcelAndRead(protoParcelable).proto).isEqualTo(protoParcelable.proto)
    }

    @Test
    fun storeInSharedMemory() {
        // Big enough that it will be stored in shared memory.
        val protoParcelable =
            TestProtoParcelable(
                BytesValue.of(ByteString.copyFrom(ByteArray(1_000_000) { i -> i.toByte() }))
            )

        assertThat(parcelAndRead(protoParcelable).proto).isEqualTo(protoParcelable.proto)
    }
}

private fun parcelAndRead(
    protoParcelable: ProtoParcelable<BytesValue>
): ProtoParcelable<BytesValue> {
    val parcel = Parcel.obtain()
    protoParcelable.writeToParcel(parcel, protoParcelable.describeContents())

    // now read it back out and create the result
    parcel.setDataPosition(0)

    @Suppress("UNCHECKED_CAST") // Safe to use in test
    val creator =
        protoParcelable.javaClass.getField("CREATOR").get(protoParcelable) as
            Parcelable.Creator<TestProtoParcelable>
    return creator.createFromParcel(parcel)
}

private class TestProtoParcelable(override val proto: BytesValue) : ProtoParcelable<BytesValue>() {
    @JvmField
    val CREATOR: Parcelable.Creator<TestProtoParcelable> = newCreator {
        val proto = BytesValue.parseFrom(it)
        TestProtoParcelable(proto)
    }
}
