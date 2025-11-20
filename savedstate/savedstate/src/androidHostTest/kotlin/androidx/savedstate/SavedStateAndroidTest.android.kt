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

import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import java.io.FileDescriptor
import java.io.Serializable
import kotlin.test.Test

internal class ParcelableSavedStateTest : RobolectricTest() {

    @Test
    fun getBinder_whenSet_returns() {
        val underTest = savedState { putBinder(KEY_1, BINDER_VALUE_1) }
        val actual = underTest.read { getBinder(KEY_1) }

        assertThat(actual).isEqualTo(BINDER_VALUE_1)
    }

    @Test
    fun getBinder_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getBinder(KEY_1) } }
    }

    @Test
    fun getBinder_whenSet_differentType_returnsDefault() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalArgumentException> { underTest.read { getBinder(KEY_1) } }
    }

    @Test
    fun getBinderOrNull_whenSet_returns() {
        val underTest = savedState { putBinder(KEY_1, BINDER_VALUE_1) }
        val actual = underTest.read { getBinderOrNull(KEY_1) }

        assertThat(actual).isEqualTo(BINDER_VALUE_1)
    }

    @Test
    fun getBinderOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getBinderOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getBinderOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getBinderOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getBinderOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThat(underTest.read { getBinderOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getSize_whenSet_returns() {
        val underTest = savedState { putSize(KEY_1, SIZE_IN_PIXEL_VALUE_1) }
        val actual = underTest.read { getSize(KEY_1) }

        assertThat(actual).isEqualTo(SIZE_IN_PIXEL_VALUE_1)
    }

    @Test
    fun getSize_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getSize(KEY_1) } }
    }

    @Test
    fun getSize_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalArgumentException> { underTest.read { getSize(KEY_1) } }
    }

    @Test
    fun getSizeOrNull_whenSet_returns() {
        val underTest = savedState { putSize(KEY_1, SIZE_IN_PIXEL_VALUE_1) }
        val actual = underTest.read { getSizeOrNull(KEY_1) }

        assertThat(actual).isEqualTo(SIZE_IN_PIXEL_VALUE_1)
    }

    @Test
    fun getSizeOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getSizeOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getSizeOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getSizeOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getSizeOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThat(underTest.read { getSizeOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getSizeF_whenSet_returns() {
        val underTest = savedState { putSizeF(KEY_1, SIZE_IN_FLOAT_VALUE_1) }
        val actual = underTest.read { getSizeF(KEY_1) }

        assertThat(actual).isEqualTo(SIZE_IN_FLOAT_VALUE_1)
    }

    @Test
    fun getSizeF_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> { savedState().read { getSizeF(KEY_1) } }
    }

    @Test
    fun getSizeF_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalArgumentException> { underTest.read { getSizeF(KEY_1) } }
    }

    @Test
    fun getSizeFOrNull_whenSet_returns() {
        val underTest = savedState { putSizeF(KEY_1, SIZE_IN_FLOAT_VALUE_1) }
        val actual = underTest.read { getSizeFOrNull(KEY_1) }

        assertThat(actual).isEqualTo(SIZE_IN_FLOAT_VALUE_1)
    }

    @Test
    fun getSizeFOrNull_whenSetNull_returns() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getSizeFOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getSizeFOrNull_whenNotSet_returnsNull() {
        assertThat(savedState().read { getSizeFOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getSizeFOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThat(underTest.read { getSizeFOrNull(KEY_1) }).isNull()
    }

    @Test
    fun getParcelable_whenSet_returns() {
        val underTest = savedState { putParcelable(KEY_1, PARCELABLE_VALUE_1) }
        val actual = underTest.read { getParcelable<TestParcelable>(KEY_1) }

        assertThat(actual).isEqualTo(PARCELABLE_VALUE_1)
    }

    @Test
    fun getParcelable_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> {
            savedState().read { getParcelable<TestParcelable>(KEY_1) }
        }
    }

    @Test
    fun getParcelable_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalArgumentException> {
            underTest.read { getParcelable<TestParcelable>(KEY_1) }
        }
    }

    @Test
    fun getParcelableOrNull_whenSet_returns() {
        val underTest = savedState { putParcelable(KEY_1, PARCELABLE_VALUE_1) }
        val actual = underTest.read { getParcelableOrNull<TestParcelable>(KEY_1) }

        assertThat(actual).isEqualTo(PARCELABLE_VALUE_1)
    }

    @Test
    fun getParcelableOrNull_whenSetNull_returnsNull() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getParcelableOrNull<TestParcelable>(KEY_1) }).isNull()
    }

    @Test
    fun getParcelableOrNull_whenNotSet_returnsNull() {
        val actual = savedState().read { getParcelableOrNull<TestParcelable>(KEY_1) }
        assertThat(actual).isNull()
    }

    @Test
    fun getParcelableOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        val actual = underTest.read { getParcelableOrNull<TestParcelable>(KEY_1) }
        assertThat(actual).isNull()
    }

    @Test
    fun getParcelableList_whenSet_returns() {
        val expected = List(size = 5) { idx -> TestParcelable(idx) }

        val underTest = savedState { putParcelableList(KEY_1, expected) }
        val actual = underTest.read { getParcelableList<TestParcelable>(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getParcelableList_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> {
            savedState().read { getParcelableList<TestParcelable>(KEY_1) }
        }
    }

    @Test
    fun getParcelableList_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalArgumentException> {
            underTest.read { getParcelableList<TestParcelable>(KEY_1) }
        }
    }

    @Test
    fun getParcelableListOrNull_whenSet_returns() {
        val expected = List(size = 5) { idx -> TestParcelable(idx) }

        val underTest = savedState { putParcelableList(KEY_1, expected) }
        val actual = underTest.read { getParcelableListOrNull<TestParcelable>(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getParcelableListOrNull_whenSetNull_returnsNull() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getParcelableListOrNull<TestParcelable>(KEY_1) }).isNull()
    }

    @Test
    fun getParcelableListOrNull_whenNotSet_returnsNull() {
        val actual = savedState().read { getParcelableListOrNull<TestParcelable>(KEY_1) }
        assertThat(actual).isNull()
    }

    @Test
    fun getParcelableListOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        val actual = underTest.read { getParcelableListOrNull<TestParcelable>(KEY_1) }
        assertThat(actual).isNull()
    }

    @Test
    fun getParcelableArray_whenSet_returns() {
        val expected = Array(size = 5) { idx -> TestParcelable(idx) }

        val underTest = savedState { putParcelableArray(KEY_1, expected) }
        val actual = underTest.read { getParcelableArray<TestParcelable>(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getParcelableArray_ofParcelable_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> {
            savedState().read { getParcelableArray<TestParcelable>(KEY_1) }
        }
    }

    @Test
    fun getParcelableArray_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalArgumentException> {
            underTest.read { getParcelableArray<TestParcelable>(KEY_1) }
        }
    }

    @Test
    fun getParcelableArrayOrNull_whenSet_returns() {
        val expected = Array(size = 5) { idx -> TestParcelable(idx) }

        val underTest = savedState { putParcelableArray(KEY_1, expected) }
        val actual = underTest.read { getParcelableArrayOrNull<TestParcelable>(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getParcelableArrayOrNull_whenSetNull_returnsNull() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getParcelableArrayOrNull<TestParcelable>(KEY_1) }).isNull()
    }

    @Test
    fun getParcelableArrayOrNull_whenNotSet_returnsNull() {
        val actual = savedState().read { getParcelableArrayOrNull<TestParcelable>(KEY_1) }
        assertThat(actual).isNull()
    }

    @Test
    fun getParcelableArrayOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        val actual = underTest.read { getParcelableArrayOrNull<TestParcelable>(KEY_1) }
        assertThat(actual).isNull()
    }

    @Test
    fun getSparseParcelableArray_whenSet_returns() {
        val expected = SPARSE_PARCELABLE_ARRAY

        val underTest = savedState { putSparseParcelableArray(KEY_1, expected) }
        val actual = underTest.read { getSparseParcelableArray<TestParcelable>(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getSparseParcelableArray_ofParcelable_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> {
            savedState().read { getSparseParcelableArray<TestParcelable>(KEY_1) }
        }
    }

    @Test
    fun getSparseParcelableArray_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalArgumentException> {
            underTest.read { getSparseParcelableArray<TestParcelable>(KEY_1) }
        }
    }

    @Test
    fun getSparseParcelableArrayOrNull_whenSet_returns() {
        val expected = SPARSE_PARCELABLE_ARRAY

        val underTest = savedState { putSparseParcelableArray(KEY_1, expected) }
        val actual = underTest.read { getSparseParcelableArrayOrNull<TestParcelable>(KEY_1) }

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getSparseParcelableArrayOrNull_whenSetNull_returnsNull() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getSparseParcelableArrayOrNull<TestParcelable>(KEY_1) })
            .isNull()
    }

    @Test
    fun getSparseParcelableArrayOrNull_whenNotSet_returnsNull() {
        val actual = savedState().read { getSparseParcelableArrayOrNull<TestParcelable>(KEY_1) }
        assertThat(actual).isNull()
    }

    @Test
    fun getSparseParcelableArrayOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        val actual = underTest.read { getSparseParcelableArrayOrNull<TestParcelable>(KEY_1) }
        assertThat(actual).isNull()
    }

    @Test
    fun getSerializable_whenSet_returns() {
        val underTest = savedState { putJavaSerializable(KEY_1, SERIALIZABLE_VALUE_1) }
        val actual = underTest.read { getJavaSerializable<TestSerializable>(KEY_1) }

        assertThat(actual).isEqualTo(SERIALIZABLE_VALUE_1)
    }

    @Test
    fun getSerializable_whenNotSet_throws() {
        assertThrows<IllegalArgumentException> {
            savedState().read { getJavaSerializable<TestSerializable>(KEY_1) }
        }
    }

    @Test
    fun getSerializable_whenSet_differentType_throws() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        assertThrows<IllegalArgumentException> {
            underTest.read { getJavaSerializable<TestSerializable>(KEY_1) }
        }
    }

    @Test
    fun getJavaSerializableOrNull_whenSet_returns() {
        val underTest = savedState { putJavaSerializable(KEY_1, SERIALIZABLE_VALUE_1) }
        val actual = underTest.read { getJavaSerializableOrNull<TestSerializable>(KEY_1) }

        assertThat(actual).isEqualTo(SERIALIZABLE_VALUE_1)
    }

    @Test
    fun getJavaSerializableOrNull_whenSetNull_returnsNull() {
        val underTest = savedState { putNull(KEY_1) }
        assertThat(underTest.read { getJavaSerializableOrNull<TestSerializable>(KEY_1) }).isNull()
    }

    @Test
    fun getJavaSerializableOrNull_whenNotSet_returnsNull() {
        val actual = savedState().read { getJavaSerializableOrNull<TestSerializable>(KEY_1) }
        assertThat(actual).isNull()
    }

    @Test
    fun getJavaSerializableOrNull_whenSet_differentType_returnsNull() {
        val underTest = savedState { putInt(KEY_1, Int.MAX_VALUE) }

        val actual = underTest.read { getJavaSerializableOrNull<TestSerializable>(KEY_1) }
        assertThat(actual).isNull()
    }

    private companion object {
        const val KEY_1 = "KEY_1"
        val SIZE_IN_PIXEL_VALUE_1 = Size(/* width= */ Int.MIN_VALUE, /* height */ Int.MIN_VALUE)
        val SIZE_IN_FLOAT_VALUE_1 =
            SizeF(/* width= */ Float.MIN_VALUE, /* height */ Float.MIN_VALUE)
        val BINDER_VALUE_1 = TestBinder(value = Int.MIN_VALUE)
        val PARCELABLE_VALUE_1 = TestParcelable(value = Int.MIN_VALUE)
        val SERIALIZABLE_VALUE_1 = TestSerializable(value = Int.MIN_VALUE)
        val SPARSE_PARCELABLE_ARRAY =
            SparseArray<TestParcelable>(/* initialCapacity= */ 5).apply {
                repeat(times = 5) { idx -> put(idx, TestParcelable(idx)) }
            }
    }

    internal data class TestBinder(val value: Int) : IBinder {
        override fun getInterfaceDescriptor() = error("")

        override fun pingBinder() = error("")

        override fun isBinderAlive() = error("")

        override fun queryLocalInterface(descriptor: String) = error("")

        override fun dump(fd: FileDescriptor, args: Array<out String>?) = error("")

        override fun dumpAsync(fd: FileDescriptor, args: Array<out String>?) = error("")

        override fun transact(code: Int, data: Parcel, reply: Parcel?, flags: Int) = error("")

        override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) = error("")

        override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int) = error("")
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

    internal data class TestSerializable(val value: Int) : Serializable
}
