/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.core.os

import android.content.Intent
import android.content.pm.Signature
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class BundleTest {
    @Test
    fun getParcelable() {
        val bundle = Bundle()
        bundle.putParcelable("parcelable", Intent())
        parcelAndUnparcel(bundle)

        assertThat(bundle.getParcelableCompat<Intent>("parcelable"))
            .isInstanceOf(Intent::class.java)
    }

    @Test
    fun getParcelable_returnsNullOnClassMismatch() {
        val bundle = Bundle()
        bundle.putParcelable("parcelable", Intent())
        parcelAndUnparcel(bundle)

        assertThat(bundle.getParcelableCompat<Signature>("parcelable")).isNull()
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun getParcelableArray_post33() {
        if (Build.VERSION.SDK_INT < 34) return
        val bundle = Bundle()
        bundle.putParcelableArray("array", arrayOf(Intent()))
        parcelAndUnparcel(bundle)

        assertThat(bundle.getParcelableArrayCompat<Intent>("array"))
            .isInstanceOf(Array<Intent>::class.java)
    }

    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun getParcelableArray_returnsNullOnClassMismatch_post33() {
        if (Build.VERSION.SDK_INT < 34) return
        val bundle = Bundle()
        bundle.putParcelableArray("array", arrayOf(Intent()))
        parcelAndUnparcel(bundle)

        assertThat(bundle.getParcelableArrayCompat<Signature>("array")).isNull()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 32)
    fun getParcelableArray_pre33() {
        if (Build.VERSION.SDK_INT >= 34) return
        val bundle = Bundle()
        bundle.putParcelableArray("array", arrayOf(Intent()))
        parcelAndUnparcel(bundle)

        assertThat(bundle.getParcelableArrayCompat<Intent>("array"))
            .isInstanceOf(Array<Parcelable>::class.java)

        assertThat(bundle.getParcelableArrayCompat<Intent>("array"))
            .isNotInstanceOf(Array<Intent>::class.java)

        // We do not check clazz Pre-U
        assertThat(bundle.getParcelableArrayCompat<Signature>("array"))
            .isInstanceOf(Array<Parcelable>::class.java)
    }

    @Test
    fun getParcelableArrayList() {
        val bundle = Bundle()
        bundle.putParcelableArrayList("array", arrayListOf(Intent()))
        parcelAndUnparcel(bundle)

        assertThat(bundle.getParcelableArrayListCompat<Intent>("array")?.get(0))
            .isInstanceOf(Intent::class.java)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun getParcelableArrayList_returnsNullOnClassMismatch_post34() {
        if (Build.VERSION.SDK_INT < 34) return
        val bundle = Bundle()
        bundle.putParcelableArrayList("array", arrayListOf(Intent()))
        parcelAndUnparcel(bundle)

        assertThat(bundle.getParcelableArrayListCompat<Signature>("array")).isNull()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun getParcelableArrayList_noTypeCheck_pre34() {
        if (Build.VERSION.SDK_INT >= 34) return
        val bundle = Bundle()
        bundle.putParcelableArrayList("array", arrayListOf(Intent()))
        parcelAndUnparcel(bundle)

        val list = bundle.getParcelableArrayListCompat<Signature>("array") as ArrayList<*>
        assertThat(list[0]).isInstanceOf(Intent::class.java)
    }

    @Test
    fun getSparseParcelableArray() {
        val bundle = Bundle()
        val array = SparseArray<Intent?>()
        array.put(0, Intent())
        bundle.putSparseParcelableArray("array", array)
        parcelAndUnparcel(bundle)

        assertThat(bundle.getSparseParcelableArrayCompat<Intent>("array")?.get(0))
            .isInstanceOf(Intent::class.java)
    }

    @Test
    @SdkSuppress(minSdkVersion = 34)
    fun getSparseParcelableArray_returnsNullOnClassMismatch_post34() {
        if (Build.VERSION.SDK_INT < 34) return
        val bundle = Bundle()
        val array = SparseArray<Intent?>()
        array.put(0, Intent())
        bundle.putSparseParcelableArray("array", array)
        parcelAndUnparcel(bundle)

        assertThat(bundle.getSparseParcelableArrayCompat<Signature>("array")).isNull()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun getSparseParcelableArray_noTypeCheck_pre34() {
        if (Build.VERSION.SDK_INT >= 34) return
        val bundle = Bundle()
        val array = SparseArray<Intent?>()
        array.put(0, Intent())
        bundle.putSparseParcelableArray("array", array)
        parcelAndUnparcel(bundle)

        assertThat(bundle.getSparseParcelableArrayCompat<Intent>("array")?.get(0))
            .isInstanceOf(Intent::class.java)
    }

    private fun parcelAndUnparcel(bundle: Bundle) {
        val p = Parcel.obtain()
        bundle.writeToParcel(p, 0)
        p.setDataPosition(0)
        bundle.readFromParcel(p)
    }

    @Test
    fun getSerializable() {
        val bundle = Bundle()
        val s = "Hello World"
        bundle.putSerializable("serializable", s)
        parcelAndUnparcel(bundle)

        assertThat(bundle.getSerializableCompat<String>("serializable"))
            .isInstanceOf(String::class.java)
    }

    @Test
    fun getSerializable_returnsNullOnClassMismatch() {
        val bundle = Bundle()
        val s = "Hello World"
        bundle.putSerializable("serializable", s)
        parcelAndUnparcel(bundle)

        assertThat(bundle.getSerializableCompat<HashSet<*>>("serializable")).isNull()
    }
}
