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
import androidx.pdf.PdfPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FormEditRecordTest {

    @Test
    fun formEditRecord_setIndicesConstructor_createsInstanceWithCorrectType() {
        val pageNumber = 1
        val widgetIndex = 2
        val selectedIndices = intArrayOf(1, 2, 3)
        val record = FormEditInfo.createSetIndices(pageNumber, widgetIndex, selectedIndices)

        assertEquals(FormEditInfo.EDIT_TYPE_SET_INDICES, record.type)
        assertEquals(pageNumber, record.pageNumber)
        assertEquals(widgetIndex, record.widgetIndex)
        for (i in 0 until record.selectedIndexCount) {
            assertEquals(selectedIndices[i], record.getSelectedIndexAt(i))
        }
        assertEquals(null, record.clickPoint)
        assertEquals(null, record.text)
    }

    @Test(expected = IllegalArgumentException::class)
    fun formEditRecord_setIndicesConstructor_negativePageNumber_throwsException() {
        FormEditInfo.createSetIndices(-1, 0, intArrayOf(1, 2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun formEditRecord_setIndicesConstructor_negativeWidgetIndex_throwsException() {
        FormEditInfo.createSetIndices(0, -1, intArrayOf(1, 2))
    }

    @Test
    fun formEditRecord_setTextConstructor_createsInstanceWithCorrectType() {
        val pageNumber = 1
        val widgetIndex = 2
        val text = "Test Text"
        val record = FormEditInfo.createSetText(pageNumber, widgetIndex, text)

        assertEquals(FormEditInfo.EDIT_TYPE_SET_TEXT, record.type)
        assertEquals(pageNumber, record.pageNumber)
        assertEquals(widgetIndex, record.widgetIndex)
        assertEquals(text, record.text)
        assertEquals(null, record.clickPoint)
        assertEquals(0, record.selectedIndexCount)
    }

    @Test
    fun formEditRecord_clickConstructor_createsInstanceWithCorrectType() {
        val pageNumber = 1
        val widgetIndex = 2
        val clickPoint = PdfPoint(pageNumber, 10f, 20f)
        val record = FormEditInfo.createClick(widgetIndex, clickPoint)

        assertEquals(FormEditInfo.EDIT_TYPE_CLICK, record.type)
        assertEquals(pageNumber, record.pageNumber)
        assertEquals(widgetIndex, record.widgetIndex)
        assertEquals(clickPoint, record.clickPoint)
        assertEquals(null, record.text)
        assertEquals(0, record.selectedIndexCount)
    }

    @Test
    fun formEditRecord_parcelable_setIndices_equals() {
        val pageNumber = 1
        val widgetIndex = 2
        val selectedIndices = intArrayOf(1, 2, 3)
        val originalRecord = FormEditInfo.createSetIndices(pageNumber, widgetIndex, selectedIndices)

        val parcel = Parcel.obtain()
        originalRecord.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val createdRecord = FormEditInfo.CREATOR.createFromParcel(parcel)

        assertEquals(originalRecord, createdRecord)
        parcel.recycle()
    }

    @Test
    fun formEditRecord_parcelable_setText_equals() {
        val pageNumber = 1
        val widgetIndex = 2
        val text = "Test Text"
        val originalRecord = FormEditInfo.createSetText(pageNumber, widgetIndex, text)

        val parcel = Parcel.obtain()
        originalRecord.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val createdRecord = FormEditInfo.CREATOR.createFromParcel(parcel)
        assertEquals(originalRecord, createdRecord)
        parcel.recycle()
    }

    @Test
    fun formEditRecord_parcelable_click_equals() {
        val pageNumber = 1
        val widgetIndex = 2
        val clickPoint = PdfPoint(pageNumber, 10f, 20f)
        val originalRecord = FormEditInfo.createClick(widgetIndex, clickPoint)

        val parcel = Parcel.obtain()
        originalRecord.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val createdRecord = FormEditInfo.CREATOR.createFromParcel(parcel)

        assertEquals(originalRecord, createdRecord)
        parcel.recycle()
    }

    @Test
    fun formEditRecord_equals_sameObject_returnsTrue() {
        val record = FormEditInfo.createClick(1, PdfPoint(2, 10f, 20f))
        assertEquals(record, record)
    }

    @Test
    fun formEditRecord_equals_sameValues_setIndices_returnsTrue() {
        val record1 = FormEditInfo.createSetIndices(1, 2, intArrayOf(1, 2))
        val record2 = FormEditInfo.createSetIndices(1, 2, intArrayOf(1, 2))
        assertEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_sameValues_setText_returnsTrue() {
        val record1 = FormEditInfo.createSetText(1, 2, "text")
        val record2 = FormEditInfo.createSetText(1, 2, "text")
        assertEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_sameValues_click_returnsTrue() {
        val record1 = FormEditInfo.createClick(1, PdfPoint(2, 10f, 20f))
        val record2 = FormEditInfo.createClick(1, PdfPoint(2, 10f, 20f))
        assertEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentPageNumber_returnsFalse() {
        val record1 = FormEditInfo.createClick(1, PdfPoint(2, 10f, 20f))
        val record2 = FormEditInfo.createClick(1, PdfPoint(1, 10f, 20f))
        assertNotEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentWidgetIndex_returnsFalse() {
        val record1 = FormEditInfo.createClick(2, PdfPoint(1, 10f, 20f))
        val record2 = FormEditInfo.createClick(1, PdfPoint(1, 10f, 20f))
        assertNotEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentType_returnsFalse() {
        val record1 = FormEditInfo.createClick(2, PdfPoint(1, 10f, 20f))
        val record2 = FormEditInfo.createSetText(1, 2, "text")
        assertNotEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentClickPoint_returnsFalse() {
        val record1 = FormEditInfo.createClick(2, PdfPoint(1, 10f, 20f))
        val record2 = FormEditInfo.createClick(2, PdfPoint(1, 30f, 40f))
        assertNotEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentSelectedIndices_returnsFalse() {
        val record1 = FormEditInfo.createSetIndices(1, 2, intArrayOf(1, 2))
        val record2 = FormEditInfo.createSetIndices(1, 2, intArrayOf(3, 4))
        assertNotEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentText_returnsFalse() {
        val record1 = FormEditInfo.createSetText(1, 2, "text1")
        val record2 = FormEditInfo.createSetText(1, 2, "text2")
        assertNotEquals(record1, record2)
    }

    @Test
    fun formEditRecord_equals_differentClass_returnsFalse() {
        val record = FormEditInfo.createSetText(1, 2, "text1")
        val other = "Not a FormEditInfo"
        assertNotEquals(record, other)
    }

    @Test
    fun formEditRecord_equals_null_returnsFalse() {
        val record = FormEditInfo.createSetText(1, 2, "text1")
        assertNotEquals(record, null)
    }

    @Test
    fun formEditRecord_hashCode_equalObjects_equalHashCodes_setIndices() {
        val record1 = FormEditInfo.createSetIndices(1, 2, intArrayOf(1, 2))
        val record2 = FormEditInfo.createSetIndices(1, 2, intArrayOf(1, 2))
        assertEquals(record1.hashCode(), record2.hashCode())
    }

    @Test
    fun formEditRecord_hashCode_equalObjects_equalHashCodes_setText() {
        val record1 = FormEditInfo.createSetText(1, 2, "text")
        val record2 = FormEditInfo.createSetText(1, 2, "text")
        assertEquals(record1.hashCode(), record2.hashCode())
    }

    @Test
    fun formEditRecord_hashCode_equalObjects_equalHashCodes_click() {
        val record1 = FormEditInfo.createClick(2, PdfPoint(1, 10f, 20f))
        val record2 = FormEditInfo.createClick(2, PdfPoint(1, 10f, 20f))
        assertEquals(record1.hashCode(), record2.hashCode())
    }

    @Test
    fun formEditRecord_hashCode_differentObjects_differentHashCodes() {
        val record1 = FormEditInfo.createClick(2, PdfPoint(2, 10f, 20f))
        val record2 = FormEditInfo.createSetText(2, 3, "text")
        assertNotEquals(record1.hashCode(), record2.hashCode())
    }

    @Test
    fun formEditRecord_describeContents_returnsZero() {
        val record = FormEditInfo.createClick(2, PdfPoint(1, 10f, 20f))
        assertEquals(0, record.describeContents())
    }

    @Test
    fun formEditRecord_createFromParcel_nullClickPoint_createsInstanceWithNullClickPoint() {
        val selectedIndices = intArrayOf(1, 2)
        val sampleText = "text"
        val parcel = Parcel.obtain()
        parcel.writeInt(1)
        parcel.writeInt(2)
        parcel.writeInt(FormEditInfo.EDIT_TYPE_CLICK)
        parcel.writeIntArray(selectedIndices)
        parcel.writeString(sampleText)
        parcel.writeInt(-1) // clickPoint is null
        parcel.setDataPosition(0)

        val createdRecord = FormEditInfo.CREATOR.createFromParcel(parcel)

        assertEquals(1, createdRecord.pageNumber)
        assertEquals(2, createdRecord.widgetIndex)
        assertEquals(FormEditInfo.EDIT_TYPE_CLICK, createdRecord.type)
        assertEquals(null, createdRecord.clickPoint)
        assertEquals(selectedIndices.size, createdRecord.selectedIndexCount)
        for (i in 0 until selectedIndices.size) {
            assertEquals(selectedIndices[i], createdRecord.getSelectedIndexAt(i))
        }
        assertEquals(sampleText, createdRecord.text)
        parcel.recycle()
    }

    @Test
    fun formEditRecord_newArray_returnsArrayOfCorrectSize() {
        val size = 5
        val array = FormEditInfo.CREATOR.newArray(size)
        assertEquals(size, array.size)
    }
}
