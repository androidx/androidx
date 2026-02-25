/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.pdf.service.connect.FakePdfServiceConnection
import androidx.pdf.utils.TestUtils
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM, codeName = "VanillaIceCream")
@RunWith(AndroidJUnit4::class)
class SandboxedPdfLoaderTest {
    @Test
    fun openDocumentUri_notConnected_connectsAndLoadsDocument() = runTest {
        var isServiceConnected = false
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = SandboxedPdfLoader(context, Dispatchers.Main)
        loader.testingConnection =
            FakePdfServiceConnection(
                context,
                isConnected = false,
                onServiceConnected = { isServiceConnected = true },
            )
        val uri = TestUtils.openFile(context, PDF_DOCUMENT)

        val document = loader.openDocument(uri)

        val expectedPageCount = 3
        assertThat(isServiceConnected).isTrue()
        assertThat(document.uri == uri).isTrue()
        assertThat(document.pageCount == expectedPageCount).isTrue()
        assertThat(document.linearizationStatus)
            .isEqualTo(PdfDocument.LINEARIZATION_STATUS_NOT_LINEARIZED)
        document.close()
    }

    @Test
    fun openDocumentFd_notConnected_connectsAndLoadsDocument() = runTest {
        var isServiceConnected = false
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = SandboxedPdfLoader(context, Dispatchers.Main)
        loader.testingConnection =
            FakePdfServiceConnection(
                context,
                isConnected = false,
                onServiceConnected = { isServiceConnected = true },
            )
        val pfd = TestUtils.openFileDescriptor(context, PDF_DOCUMENT)

        val document = loader.openDocument(FAKE_URI_1, pfd)

        val expectedPageCount = 3
        assertThat(isServiceConnected).isTrue()
        assertThat(document.uri == FAKE_URI_1).isTrue()
        assertThat(document.pageCount == expectedPageCount).isTrue()
        assertThat(document.linearizationStatus)
            .isEqualTo(PdfDocument.LINEARIZATION_STATUS_NOT_LINEARIZED)
        document.close()
    }

    @Test(expected = IllegalStateException::class)
    fun openDocumentUri_connectedAndNullBinder_throwsIllegalStateException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = SandboxedPdfLoader(context, Dispatchers.Main)
        loader.testingConnection = FakePdfServiceConnection(context, isConnected = true)

        val uri = TestUtils.openFile(context, PDF_DOCUMENT)
        runTest { loader.openDocument(uri) }
    }

    @Test(expected = IllegalStateException::class)
    fun openDocumentFd_connectedAndNullBinder_throwsIllegalStateException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = SandboxedPdfLoader(context, Dispatchers.Main)
        loader.testingConnection = FakePdfServiceConnection(context, isConnected = true)

        val pfd = TestUtils.openFileDescriptor(context, PDF_DOCUMENT)
        runTest { loader.openDocument(FAKE_URI_1, pfd) }
    }

    @Test(expected = PdfPasswordException::class)
    fun openDocumentUri_passwordProtected_throwsPdfPasswordException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = SandboxedPdfLoader(context, Dispatchers.Main)

        val uri = TestUtils.openFile(context, PASSWORD_PROTECTED_DOCUMENT)
        runTest { loader.openDocument(uri) }
    }

    @Test(expected = PdfPasswordException::class)
    fun openDocumentFd_passwordProtected_throwsPdfPasswordException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = SandboxedPdfLoader(context, Dispatchers.Main)

        val pfd = TestUtils.openFileDescriptor(context, PASSWORD_PROTECTED_DOCUMENT)

        runTest { loader.openDocument(FAKE_URI_1, pfd) }
    }

    @Test(expected = IllegalStateException::class)
    fun openDocumentUri_corruptedDocument_throwsIllegalStateException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = SandboxedPdfLoader(context, Dispatchers.Main)

        val pfd = TestUtils.openFileDescriptor(context, CORRUPTED_DOCUMENT)

        runTest { loader.openDocument(FAKE_URI_1, pfd) }
    }

    @Test(expected = IllegalStateException::class)
    fun openDocumentFd_corruptedDocument_throwsIllegalStateException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = SandboxedPdfLoader(context, Dispatchers.Main)

        val uri = TestUtils.openFile(context, CORRUPTED_DOCUMENT)

        runTest { loader.openDocument(uri) }
    }

    /**
     * Verify that 2 documents can be opened using the same loader, without corrupting the initial
     * document's internal state. See b/380140417
     */
    @Test
    fun openTwoDocumentsUri_sharedLoader() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uri1 = TestUtils.openFile(context, "sample.pdf")
        val uri2 = TestUtils.openFile(context, "alt_text.pdf")
        val sharedLoader = SandboxedPdfLoader(context, Dispatchers.Main)

        // Grab some data from document1
        val document1 = sharedLoader.openDocument(uri1)
        assertThat(document1.pageCount).isEqualTo(3)
        val doc1Page3Info = document1.getPageInfo(2)
        val doc1Page1Text = document1.getPageContent(0)?.textContents?.get(0)?.text

        // Load document2, make a basic assertion to verify it is indeed a different PDF document
        val document2 = sharedLoader.openDocument(uri2)
        assertThat(document2.pageCount).isEqualTo(1)

        // Make sure we receive the same data from document1 as before, i.e. that loading document2
        // did not in any way corrupt document1
        assertThat(document1.getPageContent(0)?.textContents?.get(0)?.text).isEqualTo(doc1Page1Text)
        assertThat(document1.getPageInfo(2).height).isEqualTo(doc1Page3Info.height)
        assertThat(document1.getPageInfo(2).width).isEqualTo(doc1Page3Info.width)

        document1.close()
        document2.close()
    }

    @Test
    fun test_initPdfSession_success() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handle = SandboxedPdfLoader.startInitialization(context)

        // Assert a non-null handle returned for maintaining pdf session
        assertNotNull(handle)
        // Clean up
        handle.close()
    }

    @Test
    fun startInitialization_multipleCallsReturnIndependentHandles() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handle1 = SandboxedPdfLoader.startInitialization(context)
        assertNotNull(handle1)

        val handle2 = SandboxedPdfLoader.startInitialization(context)
        assertNotNull(handle2)

        assertNotSame(handle1, handle2)

        // Ensure cleanup for both
        handle1.close()
        handle2.close()
    }

    @Test
    fun test_callingCloseImmediatelyAfterInit() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val handle = SandboxedPdfLoader.startInitialization(context)
        // Call close immediately from a separate thread; asserting no exception is thrown
        withContext(Dispatchers.IO) { handle.close() }
    }

    @Test
    fun openTwoDocumentsFd_sharedLoader() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pfd1 = TestUtils.openFileDescriptor(context, "sample.pdf")
        val pfd2 = TestUtils.openFileDescriptor(context, "alt_text.pdf")
        val sharedLoader = SandboxedPdfLoader(context, Dispatchers.Main)

        // Grab some data from document1
        val document1 = sharedLoader.openDocument(FAKE_URI_1, pfd1)
        assertThat(document1.pageCount).isEqualTo(3)
        val doc1Page3Info = document1.getPageInfo(2)
        val doc1Page1Text = document1.getPageContent(0)?.textContents?.get(0)?.text

        // Load document2, make a basic assertion to verify it is indeed a different PDF document
        val document2 = sharedLoader.openDocument(FAKE_URI_2, pfd2)
        assertThat(document2.pageCount).isEqualTo(1)

        // Make sure we receive the same data from document1 as before, i.e. that loading document2
        // did not in any way corrupt document1
        assertThat(document1.getPageContent(0)?.textContents?.get(0)?.text).isEqualTo(doc1Page1Text)
        assertThat(document1.getPageInfo(2).height).isEqualTo(doc1Page3Info.height)
        assertThat(document1.getPageInfo(2).width).isEqualTo(doc1Page3Info.width)
    }

    @Test
    fun openDocumentWithUri_assertDefaultRenderParams() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uri = TestUtils.openFile(context, "sample.pdf")
        val sharedLoader = SandboxedPdfLoader(context, Dispatchers.Main)
        val document = sharedLoader.openDocument(uri)
        assertThat(document.renderParams)
            .isEqualTo(RenderParams(RenderParams.RENDER_MODE_FOR_DISPLAY))
    }

    @Test
    fun openDocumentWithPfd_assertDefaultRenderParams() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pfd = TestUtils.openFileDescriptor(context, "sample.pdf")
        val sharedLoader = SandboxedPdfLoader(context, Dispatchers.Main)
        val document = sharedLoader.openDocument(FAKE_URI_1, pfd)
        assertThat(document.renderParams)
            .isEqualTo(RenderParams(RenderParams.RENDER_MODE_FOR_DISPLAY))
    }

    @Test
    fun openDocumentWithPfd_assertRenderParamsOnDocument() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pfd = TestUtils.openFileDescriptor(context, "sample.pdf")
        val sharedLoader = SandboxedPdfLoader(context, Dispatchers.Main)
        val customRenderParams =
            RenderParams(
                renderMode = RenderParams.RENDER_MODE_FOR_PRINT,
                renderFlags = RenderParams.FLAG_RENDER_HIGHLIGHT_ANNOTATIONS,
                renderFormContentMode = RenderParams.RENDER_FORM_CONTENT_ENABLED,
            )
        val document = sharedLoader.openDocument(FAKE_URI_1, pfd, renderParams = customRenderParams)
        assertThat(document.renderParams).isEqualTo(customRenderParams)
    }

    companion object {
        private const val PDF_DOCUMENT = "sample.pdf"
        private const val PASSWORD_PROTECTED_DOCUMENT = "sample-protected.pdf"
        private const val CORRUPTED_DOCUMENT = "corrupted.pdf"

        // We deliberately use fake URIs in the file descriptor versions of these tests to validate
        // the file descriptor API's behavior of using the URI only as a unique identifier.
        private val FAKE_URI_1 = Uri.parse("content://who.cares/not_a.pdf")
        private val FAKE_URI_2 = Uri.parse("http://this_is.not/even_a_scheme_we_support.html")
    }
}
