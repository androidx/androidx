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

import android.os.Build
import androidx.pdf.utils.TestUtils
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@RunWith(AndroidJUnit4::class)
class PdfDocumentRendererAdapterTest {

    @Test
    fun close_whileUncachedPageIsOpen_doesNotThrowAndSubsequentReleaseIsSafe() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        TestUtils.openFileDescriptor(context, "sample.pdf").use { pfd ->
            val adapter = PdfDocumentRendererAdapter(pfd, password = null)

            // 1. Open an uncached page (useCache = false) directly from framework PdfRenderer.
            val uncachedPage = adapter.openPage(pageNum = 0, useCache = false)

            // 2. Closing the adapter while uncachedPage is still open does NOT throw
            // IllegalStateException("Current page not closed") because multi-page PdfRenderer
            // on Android V+ (and SDK Extension >= 13) removed throwIfPageOpened(). Native PDFium
            // tears down the document and frees all associated native page structures.
            // It also sets PdfRenderer's internal mPdfDocument reference to null.
            adapter.close()

            // 3. Releasing uncachedPage after adapter.close() must return early via `if (isClosed)
            // return`
            // inside releasePage() WITHOUT calling uncachedPage.close(). If uncachedPage.close()
            // were called after mPdfDocument became null, Android's PdfRenderer.Page.close() would
            // throw NullPointerException("PdfDocumentProxy cannot be null").
            adapter.releasePage(uncachedPage, pageNum = 0)
        }
    }

    @Test
    fun withPage_whenAdapterClosedMidExecution_completesAndReleasesWithoutCrash() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        TestUtils.openFileDescriptor(context, "sample.pdf").use { pfd ->
            val adapter = PdfDocumentRendererAdapter(pfd, password = null)

            val result =
                adapter.withPage(pageNum = 0) { page ->
                    val width = page.width
                    // Simulate another thread closing the document while withPage is mid-execution
                    // with an uncached page open. When withPage exits into its `finally` block and
                    // calls releasePage(page, 0), `releasePage` safely no-ops instead of calling
                    // page.close() on the destroyed PdfRenderer.
                    adapter.close()
                    width
                }

            assertThat(result).isGreaterThan(0)
        }
    }

    @Test
    fun openPage_afterClose_throwsRendererClosedException() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        TestUtils.openFileDescriptor(context, "sample.pdf").use { pfd ->
            val adapter = PdfDocumentRendererAdapter(pfd, password = null)
            adapter.close()

            assertThrows(RendererClosedException::class.java) {
                adapter.openPage(pageNum = 0, useCache = true)
            }
        }
    }

    @Test
    fun close_whileUncachedPageIsOpen_throwsIllegalStateExceptionOnDirectPageClose() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        TestUtils.openFileDescriptor(context, "sample.pdf").use { pfd ->
            val adapter = PdfDocumentRendererAdapter(pfd, password = null)

            // Open an uncached page (useCache = false) that is not tracked in pageCache.
            val uncachedPage = adapter.openPage(pageNum = 0, useCache = false)

            // Close the adapter (which calls PdfRenderer.close() and sets mPdfDocument = null).
            adapter.close()

            // Directly calling close() on the uncached page throws IllegalStateException
            // ("Document already closed") sequentially (or NullPointerException("PdfDocumentProxy
            // cannot be null") if racing concurrently) in Android's PdfRenderer.Page.close().
            assertThrows(IllegalStateException::class.java) { uncachedPage.close() }
        }
    }

    @androidx.annotation.RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    @Test
    fun preVAdapter_close_whileUncachedPageIsOpen_doesNotThrowAndSubsequentReleaseIsSafe() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        TestUtils.openFileDescriptor(context, "sample.pdf").use { pfd ->
            val adapter = PdfDocumentRendererPreVAdapter(pfd, password = null)

            // Open an uncached page (useCache = false) from real framework PdfRendererPreV
            val uncachedPage = adapter.openPage(pageNum = 0, useCache = false)

            // Closing the adapter while uncachedPage is open succeeds without throwing
            adapter.close()

            // Releasing uncachedPage via releasePage() after adapter.close() is a safe no-op
            adapter.releasePage(uncachedPage, pageNum = 0)
        }
    }
}
