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

package androidx.pdf.adapter

import android.os.ParcelFileDescriptor
import androidx.pdf.PdfDocument.Companion.LINEARIZATION_STATUS_NOT_LINEARIZED

class FakePdfDocumentRenderer(
    override val linearizationStatus: Int = LINEARIZATION_STATUS_NOT_LINEARIZED,
    override val pageCount: Int = 10,
    override val formType: Int = 0,
) : PdfDocumentRenderer {
    val fakePagesMap = mutableMapOf<Int, FakePdfPage>()

    var isClosed = false

    init {
        for (pageNum in 0 until pageCount) fakePagesMap[pageNum] = FakePdfPage(pageNum, 100, 100)
    }

    override fun openPage(pageNum: Int, useCache: Boolean): PdfPage {
        return fakePagesMap[pageNum] ?: throw IndexOutOfBoundsException()
    }

    override fun releasePage(page: PdfPage?, pageNum: Int) {}

    override fun write(destination: ParcelFileDescriptor, removePasswordProtection: Boolean) {
        TODO("Not yet implemented")
    }

    override fun close() {
        isClosed = true
    }
}
