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

import android.os.DeadObjectException
import androidx.pdf.PdfDocument
import androidx.pdf.exceptions.RequestFailedException
import androidx.pdf.exceptions.RequestMetadata
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.util.FORM_WIDGET_INFO_REQUEST_NAME
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * This class is responsible for loading form widget metadata from the [PdfDocument]. It fetches
 * [FormWidgetInfo] for a given page and updates the [PdfFormFillingState] with the loaded
 * information.
 */
internal class FormWidgetMetadataLoader(
    private val pdfDocument: PdfDocument,
    private val formFillingState: PdfFormFillingState,
    private val errorFlow: MutableSharedFlow<Throwable>,
) {
    suspend fun loadFormWidgetInfos(pageNum: Int): List<FormWidgetInfo>? {
        try {
            val formWidgetInfos = pdfDocument.getFormWidgetInfos(pageNum)
            formFillingState.addPageFormWidgetInfos(pageNum, formWidgetInfos)
            return formWidgetInfos
        } catch (e: DeadObjectException) {
            val exception =
                RequestFailedException(
                    requestMetadata =
                        RequestMetadata(
                            requestName = FORM_WIDGET_INFO_REQUEST_NAME,
                            pageRange = pageNum..pageNum,
                        ),
                    throwable = e,
                )
            errorFlow.emit(exception)
        }
        return null
    }
}
