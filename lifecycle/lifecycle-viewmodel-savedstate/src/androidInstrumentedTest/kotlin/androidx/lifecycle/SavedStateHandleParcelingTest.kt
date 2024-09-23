/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.lifecycle

import android.os.Bundle
import android.os.Bundle.CREATOR
import android.os.Parcel
import android.os.Parcel.obtain
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle.Companion.createHandle
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import java.util.Arrays.equals
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SavedStateHandleParcelingTest {
    @UiThreadTest
    @Test
    fun test() {
        val handle = SavedStateHandle()
        handle.getLiveData<String>("livedata").value = "para"
        handle["notlive"] = 261
        handle["array"] = intArrayOf(2, 3, 9)
        val savedState = handle.savedStateProvider().saveState()
        val parcel = obtain()
        savedState.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val newBundle = CREATOR.createFromParcel(parcel)
        val newHandle: SavedStateHandle = createHandle(newBundle, null)
        assertThat<String>(newHandle["livedata"], `is`("para"))
        assertThat<Int>(newHandle["notlive"], `is`(261))
        assertThat(equals(newHandle["array"], intArrayOf(2, 3, 9)), `is`(true))
    }

    @UiThreadTest
    @Test
    fun testParcelable() {
        val handle = SavedStateHandle()
        handle["custom"] = CustomTestParcelable("test")
        handle["customArray"] = arrayOf(CustomTestParcelable("test"), CustomTestParcelable("test2"))
        val savedState = handle.savedStateProvider().saveState()
        val parcel = obtain()
        savedState.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val newBundle = CREATOR.createFromParcel(parcel)
        val newHandle: SavedStateHandle = createHandle(newBundle, null)
        assertThat<CustomTestParcelable>(newHandle["custom"], `is`(CustomTestParcelable("test")))
        assertThat(
            newHandle
                .get<Array<Parcelable>>("customArray")
                .contentEquals(
                    arrayOf(CustomTestParcelable("test"), CustomTestParcelable("test2"))
                ),
            `is`(true)
        )
    }

    @UiThreadTest
    @Test
    fun testRemoveFromDefault() {
        val defaultState = Bundle()
        defaultState.putString("string", "default")
        val handle = createHandle(null, defaultState)
        assertThat(handle.contains("string"), `is`(true))
        handle.remove<Any>("string")
        assertThat(handle.contains("string"), `is`(false))
        val savedState = handle.savedStateProvider().saveState()
        val newHandle = createHandle(savedState, defaultState)
        assertThat(newHandle.contains("string"), `is`(false))
    }
}

/** [CustomTestParcelable] that helps testing bundled custom parcels */
data class CustomTestParcelable(val name: String?) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString())

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<CustomTestParcelable> {
        override fun createFromParcel(parcel: Parcel) = CustomTestParcelable(parcel)

        override fun newArray(size: Int): Array<CustomTestParcelable?> = arrayOfNulls(size)
    }
}
