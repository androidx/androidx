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
package androidx.glance.wear.parcel.legacy

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.glance.wear.proto.legacy.TileUpdateRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class ProtoParcelableTest {
    private class Wrapper(payload: ByteArray, version: Int) : ProtoParcelable(payload, version) {
        companion object {
            const val VERSION: Int = 1
            val CREATOR: Parcelable.Creator<Wrapper> =
                newCreator<Wrapper>(Wrapper::class.java, ::Wrapper)
        }
    }

    private class WrapperV2(payload: ByteArray, extras: Bundle?, version: Int) :
        ProtoParcelable(payload, extras, version) {
        companion object {
            const val VERSION: Int = 2
            val CREATOR: Parcelable.Creator<WrapperV2?> =
                newCreator<WrapperV2>(WrapperV2::class.java, ::WrapperV2)
        }
    }

    @Test
    fun contentsEqualsAndHashCode() {
        val foo1 = Wrapper(TileUpdateRequest(tile_id = 111).encode(), Wrapper.Companion.VERSION)
        val foo2 = Wrapper(TileUpdateRequest(tile_id = 111).encode(), Wrapper.Companion.VERSION)
        val bar = Wrapper(TileUpdateRequest(tile_id = 222).encode(), Wrapper.Companion.VERSION)
        assertThat(foo1).isEqualTo(foo2)
        assertThat(foo1).isNotEqualTo(bar)
        assertThat(foo1.hashCode()).isEqualTo(foo2.hashCode())
        assertThat(foo1.hashCode()).isNotEqualTo(bar.hashCode())
    }

    @Test
    fun versionEqualsAndHashCode() {
        val foo1 = Wrapper(TileUpdateRequest(tile_id = 111).encode(), Wrapper.Companion.VERSION)
        val foo2 = Wrapper(TileUpdateRequest(tile_id = 222).encode(), /* version= */ 2)

        assertThat(foo1).isNotEqualTo(foo2)
        assertThat(foo1.hashCode()).isNotEqualTo(foo2.hashCode())
    }

    @Suppress("DEPRECATION") // usage of bundleOf
    @Test
    fun extrasEqualsAndHashCode() {
        @Suppress("DEPRECATION") // b/446711972
        val bundle1 = bundleOf("foo1" to 111)
        @Suppress("DEPRECATION") // b/446711972
        val bundle2 = bundleOf("foo1" to 111, "bar2" to "Baz")
        val reqBytes = TileUpdateRequest(tile_id = 222).encode()

        val foo1 = WrapperV2(reqBytes, bundle1, WrapperV2.VERSION)
        val foo2 = WrapperV2(reqBytes, bundle1, WrapperV2.VERSION)
        val bar = WrapperV2(reqBytes, bundle2, WrapperV2.VERSION)

        assertThat(foo1).isEqualTo(foo2)
        assertThat(foo1).isNotEqualTo(bar)
        assertThat(foo1.hashCode()).isEqualTo(foo2.hashCode())
        assertThat(foo1.hashCode()).isNotEqualTo(bar.hashCode())
    }

    @Suppress("DEPRECATION") // usage of bundleOf
    @Test
    fun extrasEqualsAndHashCode_withDifferentKeyOrder() {
        val key1 = "key1"
        val key2 = "key2"
        val val1 = 123
        val val2 = "value2"
        @Suppress("DEPRECATION") // b/446711972
        val bundleA1 = bundleOf(key1 to val1, key2 to val2)
        @Suppress("DEPRECATION") // b/446711972
        val bundleA2 = bundleOf(key2 to val2, key1 to val1)
        @Suppress("DEPRECATION") // b/446711972
        val bundleB = bundleOf("another_key" to "another_value")
        val reqBytes = TileUpdateRequest(tile_id = 222).encode()

        val wrapperA1 = WrapperV2(reqBytes, bundleA1, WrapperV2.VERSION)
        val wrapperA2 = WrapperV2(reqBytes, bundleA2, WrapperV2.VERSION)
        val wrapperB = WrapperV2(reqBytes, bundleB, WrapperV2.VERSION)

        assertThat(wrapperA1).isEqualTo(wrapperA2)
        assertThat(wrapperA1).isNotEqualTo(wrapperB)
        assertThat(wrapperA1.hashCode()).isEqualTo(wrapperA2.hashCode())
        assertThat(wrapperA1.hashCode()).isNotEqualTo(wrapperB.hashCode())
    }

    @Test
    fun toParcelAndBack() {
        val req = TileUpdateRequest(tile_id = 222)
        val wrapper = Wrapper(req.encode(), Wrapper.VERSION)

        val parcel = Parcel.obtain()
        wrapper.writeToParcel(parcel, 0)

        parcel.setDataPosition(0)
        assertThat(Wrapper.CREATOR.createFromParcel(parcel)).isEqualTo(wrapper)
    }

    @Suppress("DEPRECATION") // usage of bundleOf
    @Test
    fun toParcelAndBackV2() {
        val req = TileUpdateRequest(tile_id = 222)
        @Suppress("DEPRECATION") // b/446711972
        val extras = bundleOf("foo1" to 111, "bar2" to "Baz")
        val wrapper = WrapperV2(req.encode(), extras, WrapperV2.VERSION)

        val parcel = Parcel.obtain()
        wrapper.writeToParcel(parcel, 0)

        parcel.setDataPosition(0)
        assertThat(WrapperV2.CREATOR.createFromParcel(parcel)).isEqualTo(wrapper)
    }

    @Test
    fun arrayCreator() {
        assertThat(Wrapper.CREATOR.newArray(123)).hasLength(123)
    }

    @Test
    fun arrayCreatorV2() {
        assertThat(WrapperV2.CREATOR.newArray(123)).hasLength(123)
    }
}
