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
import java.util.Collections

/**
 * Parcelable class which serves as the model to store page level form widget metadata along with
 * the edit history state. This class preserves the state across config changes and process death by
 * saving and restoring relevant data related to form filling.
 */
@SuppressLint("BanParcelableUsage")
internal class PdfFormFillingState(val numPages: Int) : Parcelable {

    /** Stores the list of form widgets present in each page */
    private val pageFormWidgetInfos = Array<List<FormWidgetInfo>?>(numPages) { null }

    /** Stores the detected hint texts for form widgets on each page. */
    private val hintTexts = arrayOfNulls<MutableMap<Int, String>>(numPages)

    init {
        require(numPages >= 0) { "Empty PDF" }
    }

    constructor(parcel: Parcel) : this(parcel.readInt()) {
        readFormWidgetInfosFromParcel(parcel)
        readHintTextsFromParcel(parcel)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(numPages)
        writeFormWidgetInfosToParcel(parcel)
        writeHintTextsToParcel(parcel)
    }

    fun addPageFormWidgetInfos(pageNum: Int, formWidgetInfos: List<FormWidgetInfo>?) {
        require(pageNum in 0 until numPages) { "Page number out of range" }
        pageFormWidgetInfos[pageNum] = formWidgetInfos
    }

    fun getPageFormWidgetInfos(pageNum: Int): List<FormWidgetInfo> {
        require(pageNum in 0 until numPages) { "Page number out of range" }
        return pageFormWidgetInfos[pageNum]?.toList() ?: emptyList()
    }

    fun addHintText(pageNum: Int, widgetIndex: Int, text: String) {
        require(pageNum in 0 until numPages) { "Page number out of range" }
        val pageMap =
            synchronized(hintTexts) {
                hintTexts[pageNum]
                    ?: Collections.synchronizedMap(mutableMapOf<Int, String>()).also {
                        hintTexts[pageNum] = it
                    }
            }
        pageMap[widgetIndex] = text
    }

    fun getHintText(pageNum: Int, widgetIndex: Int): String? {
        require(pageNum in 0 until numPages) { "Page number out of range" }
        return synchronized(hintTexts) { hintTexts[pageNum]?.get(widgetIndex) }
    }

    fun hasHintsForPage(pageNum: Int): Boolean {
        if (pageNum !in 0 until numPages) return false
        return synchronized(hintTexts) { hintTexts[pageNum] != null }
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

    private fun writeHintTextsToParcel(dest: Parcel) {
        synchronized(hintTexts) {
            for (i in 0 until numPages) {
                val pageHints = hintTexts[i]
                if (pageHints.isNullOrEmpty()) {
                    dest.writeInt(-1)
                } else {
                    synchronized(pageHints) {
                        dest.writeInt(pageHints.size)
                        for ((widgetIndex, text) in pageHints) {
                            dest.writeInt(widgetIndex)
                            dest.writeString(text)
                        }
                    }
                }
            }
        }
    }

    private fun readHintTextsFromParcel(parcel: Parcel) {
        for (i in 0 until numPages) {
            val widgetCount = parcel.readInt()
            if (widgetCount == -1) {
                hintTexts[i] = null
            } else {
                val pageMap = Collections.synchronizedMap(mutableMapOf<Int, String>())
                repeat(widgetCount) {
                    val widgetIndex = parcel.readInt()
                    val text = parcel.readString()
                    if (text != null) {
                        pageMap[widgetIndex] = text
                    }
                }
                hintTexts[i] = pageMap
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
