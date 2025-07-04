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

package androidx.pdf.models

import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ListItemTest {

    @Test
    fun listItem_constructor_validInputs_createsInstance() {
        val label = "Test Label"
        val selected = true
        val listItem = ListItem(label, selected)

        assertEquals(label, listItem.label)
        assertEquals(selected, listItem.selected)
    }

    @Test
    fun listItem_parcelable_writeToParcelAndCreateFromParcel_equals() {
        val label = "Test Label"
        val selected = true
        val originalListItem = ListItem(label, selected)

        val parcel = Parcel.obtain()
        originalListItem.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val createdListItem = ListItem.CREATOR.createFromParcel(parcel)

        assertEquals(originalListItem, createdListItem)
        parcel.recycle()
    }

    @Test
    fun listItem_equals_sameObject_returnsTrue() {
        val listItem = ListItem("Test", true)
        assertEquals(listItem, listItem)
    }

    @Test
    fun listItem_equals_sameValues_returnsTrue() {
        val listItem1 = ListItem("Test", true)
        val listItem2 = ListItem("Test", true)
        assertEquals(listItem1, listItem2)
    }

    @Test
    fun listItem_equals_differentLabel_returnsFalse() {
        val listItem1 = ListItem("Test1", true)
        val listItem2 = ListItem("Test2", true)
        assertNotEquals(listItem1, listItem2)
    }

    @Test
    fun listItem_equals_differentSelected_returnsFalse() {
        val listItem1 = ListItem("Test", true)
        val listItem2 = ListItem("Test", false)
        assertNotEquals(listItem1, listItem2)
    }

    @Test
    fun listItem_equals_differentClass_returnsFalse() {
        val listItem = ListItem("Test", true)
        val other = "Not a ListItem"
        assertNotEquals(listItem, other)
    }

    @Test
    fun listItem_equals_null_returnsFalse() {
        val listItem = ListItem("Test", true)
        assertNotEquals(listItem, null)
    }

    @Test
    fun listItem_hashCode_equalObjects_equalHashCodes() {
        val listItem1 = ListItem("Test", true)
        val listItem2 = ListItem("Test", true)
        assertEquals(listItem1.hashCode(), listItem2.hashCode())
    }

    @Test
    fun listItem_hashCode_differentObjects_differentHashCodes() {
        val listItem1 = ListItem("Test1", true)
        val listItem2 = ListItem("Test2", false)
        assertNotEquals(listItem1.hashCode(), listItem2.hashCode())
    }

    @Test
    fun listItem_describeContents_returnsZero() {
        val listItem = ListItem("Test", true)
        assertEquals(0, listItem.describeContents())
    }

    @Test
    fun listItem_createFromParcel_nullLabel_createsInstanceWithEmptyLabel() {
        val parcel = Parcel.obtain()
        parcel.writeString(null)
        parcel.writeBoolean(true)
        parcel.setDataPosition(0)

        val createdListItem = ListItem.CREATOR.createFromParcel(parcel)

        assertEquals("", createdListItem.label)
        assertEquals(true, createdListItem.selected)
        parcel.recycle()
    }

    @Test
    fun listItem_newArray_returnsArrayOfCorrectSize() {
        val size = 5
        val array = ListItem.CREATOR.newArray(size)
        assertEquals(size, array?.size)
    }
}
