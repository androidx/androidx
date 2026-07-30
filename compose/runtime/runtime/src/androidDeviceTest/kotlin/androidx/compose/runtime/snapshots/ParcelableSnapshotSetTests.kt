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
package androidx.compose.runtime.snapshots

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.runtime.mutableStateSetOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParcelableSnapshotStateSetTests {
    @Test
    fun saveAndRestoreEmptySnapshotStateSet() {
        val set = mutableStateSetOf<Int>()
        val restored = recreateViaParcel(set)
        assertTrue(restored.isEmpty())
    }

    @Test
    fun saveAndRestoreSingleElementSnapshotStateSet() {
        val set = mutableStateSetOf("hello")
        val restored = recreateViaParcel(set)
        assertEquals(setOf("hello"), restored)
    }

    @Test
    fun saveAndRestoreMultipleElementsSnapshotStateSet() {
        val set = mutableStateSetOf(1, 2, 3, 4, 5)
        val restored = recreateViaParcel(set)
        assertEquals(setOf(1, 2, 3, 4, 5), restored)
    }

    @Test
    fun saveAndRestoreSnapshotStateSetAfterModifications() {
        val set = mutableStateSetOf("a", "b", "c")
        set.remove("b")
        set.add("d")
        val restored = recreateViaParcel(set)
        assertEquals(setOf("a", "c", "d"), restored)
    }

    @Test
    fun writeToParcelAndReadFromCreatorDirectly() {
        val set = mutableStateSetOf("x", "y", "z")
        val parcel = Parcel.obtain()
        set.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = SnapshotStateSet.CREATOR.createFromParcel(parcel)
        assertEquals(setOf("x", "y", "z"), restored)
    }

    private inline fun <reified T> recreateViaParcel(value: T): T {
        val parcel =
            Parcel.obtain().apply {
                writeParcelable(value as Parcelable, 0)
                setDataPosition(0)
            }
        @Suppress("DEPRECATION")
        return parcel.readParcelable<Parcelable>(javaClass.classLoader) as T
    }
}
