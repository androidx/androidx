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

package androidx.pdf.view

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.pdf.models.FormWidgetInfo

/**
 * Parcelable class which serves as the model to store page level form widget metadata along with
 * the edit history state. This class preserves the state across config changes and process death by
 * saving and restoring relevant data related to form filling.
 */
@SuppressLint("BanParcelableUsage")
internal class PdfFormFillingState(val numPages: Int) : Parcelable {

    /** Stores the list of form widgets present in each page */
    private val pageFormWidgetInfos = Array<List<FormWidgetInfo>?>(numPages) { null }

    /** Stores the list of edits applied to each page */
    private val pageEditHistory = Array(numPages) {}

    init {
        require(numPages >= 0) { "Empty PDF" }
    }

    constructor(parcel: Parcel) : this(parcel.readInt()) {
        readFormWidgetInfosFromParcel(parcel)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(numPages)
        writeFormWidgetInfosToParcel(parcel)
    }

    fun addPageFormWidgetInfos(pageNum: Int, formWidgetInfos: List<FormWidgetInfo>?) {
        require(pageNum in 0 until numPages) { "Page number out of range" }
        pageFormWidgetInfos[pageNum] = formWidgetInfos
    }

    fun getPageFormWidgetInfos(pageNum: Int): List<FormWidgetInfo>? {
        return pageFormWidgetInfos[pageNum]?.toList()
    }

    override fun describeContents(): Int {
        return 0
    }

    private fun writeFormWidgetInfosToParcel(dest: Parcel) {
        for (formWidgetInfos: List<FormWidgetInfo>? in pageFormWidgetInfos) {
            if (formWidgetInfos == null) {
                dest.writeInt(-1)
            } else {
                dest.writeInt(formWidgetInfos.size)
                dest.writeTypedList(formWidgetInfos)
            }
        }
    }

    private fun readFormWidgetInfosFromParcel(parcel: Parcel) {
        for (i in 0 until numPages) {
            if (parcel.readInt() == -1) {
                pageFormWidgetInfos[i] = null
            } else {
                val list = mutableListOf<FormWidgetInfo>()
                parcel.readTypedList(list, FormWidgetInfo.CREATOR)
                pageFormWidgetInfos[i] = list
            }
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PdfFormFillingState> =
            object : Parcelable.Creator<PdfFormFillingState> {
                override fun createFromParcel(parcel: Parcel): PdfFormFillingState? {
                    return PdfFormFillingState(parcel)
                }

                override fun newArray(size: Int): Array<out PdfFormFillingState?>? {
                    return arrayOfNulls(size)
                }
            }
    }
}
