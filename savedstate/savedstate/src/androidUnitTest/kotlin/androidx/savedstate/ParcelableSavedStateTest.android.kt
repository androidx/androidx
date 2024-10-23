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

package androidx.savedstate

import android.os.Parcel
import android.os.Parcelable
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import kotlin.test.Test

internal class ParcelableSavedStateTest : RobolectricTest() {

    @Test
    fun getParcelable_whenSet_returns() {
        val underTest = savedState { putParcelable(KEY_1, PARCELABLE_VALUE_1) }
        val actual = underTest.read { getParcelable<TestParcelable>(KEY_1) }

        assertThat(actual).isEqualTo(PARCELABLE_VALUE_1)
    }

    @Test
    fun getParcelable_whenNotSet_throws() {
        assertThrows<IllegalStateException> {
            savedState().read { getParcelable<TestParcelable>(KEY_1) }
        }
    }

    @Test
    fun getParcelable_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalStateException> {
            underTest.read { getParcelable<TestParcelable>(KEY_1) }
        }
    }

    @Test
    fun getParcelableOrElse_whenSet_returns() {
        val underTest = savedState { putParcelable(KEY_1, PARCELABLE_VALUE_1) }
        val actual = underTest.read { getParcelableOrElse(KEY_1) { PARCELABLE_VALUE_2 } }

        assertThat(actual).isEqualTo(PARCELABLE_VALUE_1)
    }

    @Test
    fun getParcelableOrElse_whenNotSet_returnsElse() {
        val actual = savedState().read { getParcelableOrElse(KEY_1) { PARCELABLE_VALUE_1 } }

        assertThat(actual).isEqualTo(PARCELABLE_VALUE_1)
    }

    @Test
    fun getParcelableOrElse_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getParcelableOrElse(KEY_1) { PARCELABLE_VALUE_1 } }

        assertThat(actual).isEqualTo(PARCELABLE_VALUE_1)
    }

    @Test
    fun getParcelableList_whenSet_returns() {
        val expected = List(size = 5) { idx -> TestParcelable(idx) }

        val underTest = savedState { putParcelableList(KEY_1, expected) }
        val actual = underTest.read { getParcelableList<TestParcelable>(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getList_ofParcelable_whenNotSet_throws() {
        assertThrows<IllegalStateException> {
            savedState().read { getParcelableList<TestParcelable>(KEY_1) }
        }
    }

    @Test
    fun getList_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalStateException> {
            underTest.read { getParcelableList<TestParcelable>(KEY_1) }
        }
    }

    @Test
    fun getListOrElse_ofParcelable_whenSet_returns() {
        val expected = List(size = 5) { idx -> TestParcelable(idx) }

        val underTest = savedState { putParcelableList(KEY_1, expected) }
        val actual =
            underTest.read { getParcelableListOrElse<TestParcelable>(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getListOrElse_ofParcelable_whenNotSet_returnsElse() {
        val actual =
            savedState().read { getParcelableListOrElse<TestParcelable>(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(emptyList<TestParcelable>())
    }

    @Test
    fun getListOrElse_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }
        val actual = underTest.read { getParcelableListOrElse(KEY_1) { emptyList() } }

        assertThat(actual).isEqualTo(emptyList<Parcelable>())
    }

    private companion object {
        const val KEY_1 = "KEY_1"
        val PARCELABLE_VALUE_1 = TestParcelable(value = Int.MIN_VALUE)
        val PARCELABLE_VALUE_2 = TestParcelable(value = Int.MAX_VALUE)
    }

    internal data class TestParcelable(val value: Int) : Parcelable {

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
