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

import android.graphics.Point
import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FormEditRecordTest {

    @Test
    fun formEditRecord_setIndicesConstructor_createsInstanceWithCorrectType() {
        val pageNumber = 1
        val widgetIndex = 2
        val selectedIndices = intArrayOf(1, 2, 3)
        val record = FormEditRecord(pageNumber, widgetIndex, selectedIndices)

        assertEquals(FormEditRecord.EDIT_TYPE_SET_INDICES, record.type)
        assertEquals(pageNumber, record.pageNumber)
        assertEquals(widgetIndex, record.widgetIndex)
        assertTrue(selectedIndices.contentEquals(record.selectedIndices))
        assertEquals(null, record.clickPoint)
        assertEquals(null, record.text)
    }

    @Test(expected = IllegalArgumentException::class)
    fun formEditRecord_setIndicesConstructor_negativePageNumber_throwsException() {
        FormEditRecord(-1, 0, intArrayOf(1, 2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun formEditRecord_setIndicesConstructor_negativeWidgetIndex_throwsException() {
        FormEditRecord(0, -1, intArrayOf(1, 2))
    }

    @Test
    fun formEditRecord_setTextConstructor_createsInstanceWithCorrectType() {
        val pageNumber = 1
        val widgetIndex = 2
        val text = "Test Text"
        val record = FormEditRecord(pageNumber, widgetIndex, text)

        assertEquals(FormEditRecord.EDIT_TYPE_SET_TEXT, record.type)
        assertEquals(pageNumber, record.pageNumber)
        assertEquals(widgetIndex, record.widgetIndex)
        assertEquals(text, record.text)
        assertEquals(null, record.clickPoint)
        assertEquals(null, record.selectedIndices)
    }

    @Test
    fun formEditRecord_clickConstructor_createsInstanceWithCorrectType() {
        val pageNumber = 1
        val widgetIndex = 2
        val clickPoint = Point(10, 20)
        val record = FormEditRecord(pageNumber, widgetIndex, clickPoint)

        assertEquals(FormEditRecord.EDIT_TYPE_CLICK, record.type)
        assertEquals(pageNumber, record.pageNumber)
        assertEquals(widgetIndex, record.widgetIndex)
        assertEquals(clickPoint, record.clickPoint)
        assertEquals(null, record.text)
        assertEquals(null, record.selectedIndices)
    }

    @Test
    fun formEditRecord_parcelable_setIndices_equals() {
        val pageNumber = 1
        val widgetIndex = 2
        val selectedIndices = intArrayOf(1, 2, 3)
        val originalRecord = FormEditRecord(pageNumber, widgetIndex, selectedIndices)

        val parcel = Parcel.obtain()
        originalRecord.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val createdRecord = FormEditRecord.CREATOR.createFromParcel(parcel)

        assertEquals(originalRecord, createdRecord)
        parcel.recycle()
    }

    @Test
    fun formEditRecord_parcelable_setText_equals() {
        val pageNumber = 1
        val widgetIndex = 2
        val text = "Test Text"
        val originalRecord = FormEditRecord(pageNumber, widgetIndex, text)

        val parcel = Parcel.obtain()
        originalRecord.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val createdRecord = FormEditRecord.CREATOR.createFromParcel(parcel)
        assertEquals(originalRecord, createdRecord)
        parcel.recycle()
    }

    @Test
    fun formEditRecord_parcelable_click_equals() {
        val pageNumber = 1
        val widgetIndex = 2
        val clickPoint = Point(10, 20)
        val originalRecord = FormEditRecord(pageNumber, widgetIndex, clickPoint)

        val parcel = Parcel.obtain()
        originalRecord.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val createdRecord = FormEditRecord.CREATOR.createFromParcel(parcel)

        assertEquals(originalRecord, createdRecord)
        parcel.recycle()
    }

    @Test
    fun formEditRecord_equals_sameObject_returnsTrue() {
        val record = FormEditRecord(1, 2, Point(10, 20))
        assertEquals(record, record)
    }

    @Test
    fun formEditRecord_equals_sameValues_setIndices_returnsTrue() {
        val record1 = FormEditRecord(1, 2, intArrayOf(1, 2))
        val record2 = FormEditRecord(1, 2, intArrayOf(1, 2))
        assertEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_sameValues_setText_returnsTrue() {
        val record1 = FormEditRecord(1, 2, "text")
        val record2 = FormEditRecord(1, 2, "text")
        assertEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_sameValues_click_returnsTrue() {
        val record1 = FormEditRecord(1, 2, Point(10, 20))
        val record2 = FormEditRecord(1, 2, Point(10, 20))
        assertEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentPageNumber_returnsFalse() {
        val record1 = FormEditRecord(1, 2, Point(10, 20))
        val record2 = FormEditRecord(2, 2, Point(10, 20))
        assertNotEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentWidgetIndex_returnsFalse() {
        val record1 = FormEditRecord(1, 2, Point(10, 20))
        val record2 = FormEditRecord(1, 3, Point(10, 20))
        assertNotEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentType_returnsFalse() {
        val record1 = FormEditRecord(1, 2, Point(10, 20))
        val record2 = FormEditRecord(1, 2, "text")
        assertNotEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentClickPoint_returnsFalse() {
        val record1 = FormEditRecord(1, 2, Point(10, 20))
        val record2 = FormEditRecord(1, 2, Point(30, 40))
        assertNotEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentSelectedIndices_returnsFalse() {
        val record1 = FormEditRecord(1, 2, intArrayOf(1, 2))
        val record2 = FormEditRecord(1, 2, intArrayOf(3, 4))
        assertNotEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentText_returnsFalse() {
        val record1 = FormEditRecord(1, 2, "text1")
        val record2 = FormEditRecord(1, 2, "text2")
        assertNotEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentClass_returnsFalse() {
        val record = FormEditRecord(1, 2, "text1")
        val other = "Not a FormEditRecord"
        assertNotEquals(record, other)
    }

    @Test
    fun formEditRecord_equals_null_returnsFalse() {
        val record = FormEditRecord(1, 2, "text1")
        assertNotEquals(record, null)
    }

    @Test
    fun formEditRecord_hashCode_equalObjects_equalHashCodes_setIndices() {
        val record1 = FormEditRecord(1, 2, intArrayOf(1, 2))
        val record2 = FormEditRecord(1, 2, intArrayOf(1, 2))
        assertEquals(record1.hashCode(), record2.hashCode())
    }

    @Test
    fun formEditRecord_hashCode_equalObjects_equalHashCodes_setText() {
        val record1 = FormEditRecord(1, 2, "text")
        val record2 = FormEditRecord(1, 2, "text")
        assertEquals(record1.hashCode(), record2.hashCode())
    }

    @Test
    fun formEditRecord_hashCode_equalObjects_equalHashCodes_click() {
        val record1 = FormEditRecord(1, 2, Point(10, 20))
        val record2 = FormEditRecord(1, 2, Point(10, 20))
        assertEquals(record1.hashCode(), record2.hashCode())
    }

    @Test
    fun formEditRecord_hashCode_differentObjects_differentHashCodes() {
        val record1 = FormEditRecord(1, 2, Point(10, 20))
        val record2 = FormEditRecord(2, 3, "text")
        assertNotEquals(record1.hashCode(), record2.hashCode())
    }

    @Test
    fun formEditRecord_describeContents_returnsZero() {
        val record = FormEditRecord(1, 2, Point(10, 20))
        assertEquals(0, record.describeContents())
    }

    @Test
    fun formEditRecord_createFromParcel_nullClickPoint_createsInstanceWithNullClickPoint() {
        val parcel = Parcel.obtain()
        parcel.writeInt(1)
        parcel.writeInt(2)
        parcel.writeInt(FormEditRecord.EDIT_TYPE_CLICK)
        parcel.writeParcelable(null, 0)
        parcel.writeIntArray(intArrayOf(1, 2))
        parcel.writeString("text")
        parcel.setDataPosition(0)

        val createdRecord = FormEditRecord.CREATOR.createFromParcel(parcel)

        assertEquals(1, createdRecord.pageNumber)
        assertEquals(2, createdRecord.widgetIndex)
        assertEquals(FormEditRecord.EDIT_TYPE_CLICK, createdRecord.type)
        assertEquals(null, createdRecord.clickPoint)
        assertEquals(
            intArrayOf(1, 2).contentToString(),
            createdRecord.selectedIndices?.contentToString(),
        )
        assertEquals("text", createdRecord.text)
        parcel.recycle()
    }

    @Test
    fun formEditRecord_newArray_returnsArrayOfCorrectSize() {
        val size = 5
        val array = FormEditRecord.CREATOR.newArray(size)
        assertEquals(size, array.size)
    }
}
