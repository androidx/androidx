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

package androidx.pdf.adapter

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PdfDocumentRendererTest {

    @Test
    fun openPage_whenClosed_throwsRendererClosedException() {
        val renderer = FakePdfDocumentRenderer()

        renderer.close()

        assertThrows(RendererClosedException::class.java) {
            renderer.openPage(pageNum = 0, useCache = true)
        }
    }

    @Test
    fun withPage_whenClosed_catchesRendererClosedExceptionAndReturnsNull() {
        val renderer = FakePdfDocumentRenderer()

        renderer.close()

        val result = renderer.withPage(pageNum = 0) { page -> page.width }
        assertThat(result).isNull()
    }

    @Test
    fun withPage_whenBlockThrowsOtherIllegalStateException_doesNotSwallowException() {
        val renderer = FakePdfDocumentRenderer()

        assertThrows(IllegalStateException::class.java) {
            renderer.withPage(pageNum = 0) {
                throw IllegalStateException("Unrelated error inside block")
            }
        }
    }

    @Test
    fun close_whileUncachedPageIsOpen_doesNotThrowAndSubsequentReleaseIsSafe() {
        val renderer = FakePdfDocumentRenderer()

        // Open an uncached page (useCache = false) that is not tracked in pageCache.
        val uncachedPage = renderer.openPage(pageNum = 0, useCache = false)

        // Closing the renderer while uncachedPage is still open is valid and does not throw
        // IllegalStateException("Current page not closed") because multipage PdfRenderer on
        // Android V+ / SDK Ext >= 13 does not require pages to be closed before closing the
        // document (native PDFium tears down the document and all native page structures).
        renderer.close()

        // 3. Releasing uncachedPage after renderer.close() must early-return via `if (isClosed)
        // return`
        // inside releasePage() WITHOUT calling uncachedPage.close().
        renderer.releasePage(uncachedPage, pageNum = 0)
    }
}
