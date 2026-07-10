/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.testing

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.bundleOf
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import kotlin.test.Test

internal class AndroidViewModelScenarioTest : RobolectricTest() {

    @Test
    fun recreate_tooLargeException() {
        val scenario = viewModelScenario { TestViewModel(handle = createSavedStateHandle()) }
        scenario.viewModel.handle["key"] = ByteArray(size = 1024 * 1024 + 1) // 1 MB is the max.

        assertThrows<IllegalStateException> { scenario.recreate() }
    }

    @Test
    fun recreate_bundle() {
        val expectedParcelable = TestParcelable(value = 1)
        val scenario = viewModelScenario { TestViewModel(handle = createSavedStateHandle()) }
        scenario.viewModel.handle["key"] =
            @Suppress("DEPRECATION") // bundleOf is deprecated
            bundleOf("key" to expectedParcelable)

        scenario.recreate()

        val actualBundle = scenario.viewModel.handle.get<Bundle>("key")!!
        @Suppress("DEPRECATION") val actualParcelable = actualBundle["key"]!!
        assertThat(actualParcelable).isNotSameInstanceAs(expectedParcelable)
        assertThat(actualParcelable).isEqualTo(expectedParcelable)
    }

    @Test
    fun recreate_parcelable() {
        val expectedParcelable = TestParcelable(value = 1)
        val scenario = viewModelScenario { TestViewModel(handle = createSavedStateHandle()) }
        scenario.viewModel.handle["key"] = expectedParcelable

        scenario.recreate()

        val actualParcelable = scenario.viewModel.handle.get<TestParcelable>("key")!!
        assertThat(actualParcelable).isNotSameInstanceAs(expectedParcelable)
        assertThat(actualParcelable).isEqualTo(expectedParcelable)
    }

    private class TestViewModel(val handle: SavedStateHandle = SavedStateHandle()) : ViewModel()

    private data class TestParcelable(val value: Int) : Parcelable {

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(value)
        }

        companion object {
            @Suppress("unused")
            @JvmField
            val CREATOR =
                object : Parcelable.Creator<TestParcelable> {
                    override fun createFromParcel(source: Parcel) =
                        TestParcelable(value = source.readInt())

                    override fun newArray(size: Int) = arrayOfNulls<TestParcelable>(size)
                }
        }
    }
}
