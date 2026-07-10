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

package androidx.xr.glimmer.stack

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultStackItemKeyTest {

    @Test
    fun equality_sameIndex_areEqual() {
        val key1 = DefaultStackItemKey(1)
        val key2 = DefaultStackItemKey(1)

        assertThat(key1).isEqualTo(key2)
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode())
    }

    @Test
    fun equality_differentIndex_areNotEqual() {
        val key1 = DefaultStackItemKey(1)
        val key2 = DefaultStackItemKey(2)

        assertThat(key1).isNotEqualTo(key2)
        assertThat(key1.hashCode()).isNotEqualTo(key2.hashCode())
    }

    @Test
    fun typeSafety_isNotEqualToUserKeys() {
        val key = DefaultStackItemKey(1)

        assertThat(key).isNotEqualTo(1)
        assertThat(key).isNotEqualTo("1")
        assertThat(key).isNotEqualTo(null)
        assertThat(key).isNotEqualTo(Any())
    }

    @Test
    fun parcelable_writeAndRead_restoresData() {
        val originalKey = DefaultStackItemKey(42)

        val restoredKey = parcelAndUnparcel(originalKey)

        assertThat(restoredKey).isEqualTo(originalKey)
        assertThat(restoredKey).isNotSameInstanceAs(originalKey)
    }

    /** Simulates the Bundle save/restore lifecycle. */
    private fun parcelAndUnparcel(key: DefaultStackItemKey): DefaultStackItemKey {
        val parcel = Parcel.obtain()
        return try {
            key.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            DefaultStackItemKey.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }
}
