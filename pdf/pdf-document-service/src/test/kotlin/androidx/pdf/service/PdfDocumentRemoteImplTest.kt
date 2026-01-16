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

package androidx.pdf.service

import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresExtension
import androidx.pdf.DraftEditResult
import androidx.pdf.InsertDraftEditOperation
import androidx.pdf.PdfLoadingStatus
import androidx.pdf.RenderParams
import androidx.pdf.RenderParams.Companion.RENDER_MODE_FOR_DISPLAY
import androidx.pdf.adapter.FakePdfDocumentRenderer
import androidx.pdf.adapter.FakePdfDocumentRendererFactory
import androidx.pdf.annotation.createStampAnnotationWithPath
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileOutputStream
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PdfDocumentRemoteImplTest {

    private lateinit var fakePfd: ParcelFileDescriptor
    private lateinit var tempFile: File

    @Before
    fun setUp() {
        tempFile = File.createTempFile("test_pdf", ".pdf")

        // Write some dummy data so it's not empty (optional, but good practice)
        FileOutputStream(tempFile).use { it.write("fake pdf content".toByteArray()) }

        fakePfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    @After
    fun tearDown() {
        // Clean up resources
        fakePfd.close()
        tempFile.delete()
    }

    @Test
    fun openPdfDocument_success_returnsSuccessStatus() {
        // Arrange
        val fakeRenderer = FakePdfDocumentRenderer()
        val factory = FakePdfDocumentRendererFactory(rendererToReturn = fakeRenderer)
        val remote = PdfDocumentRemoteImpl(factory)

        // Act
        val result = remote.openPdfDocument(fakePfd, "password")

        // Assert
        assertThat(result).isEqualTo(PdfLoadingStatus.SUCCESS.ordinal)
    }

    @Test
    fun openPdfDocument_securityException_returnsWrongPasswordStatus() {
        // Arrange
        val factory = FakePdfDocumentRendererFactory(exceptionToThrow = SecurityException("Locked"))
        val remote = PdfDocumentRemoteImpl(factory)

        // Act
        val result = remote.openPdfDocument(fakePfd, "wrong_pass")

        // Assert
        assertThat(result).isEqualTo(PdfLoadingStatus.WRONG_PASSWORD.ordinal)
    }

    @Test
    fun openPdfDocument_illegalArgument_returnsPdfErrorStatus() {
        // Arrange
        val factory =
            FakePdfDocumentRendererFactory(exceptionToThrow = IllegalArgumentException("Bad PDF"))
        val remote = PdfDocumentRemoteImpl(factory)

        // Act
        val result = remote.openPdfDocument(fakePfd, null)

        // Assert
        assertThat(result).isEqualTo(PdfLoadingStatus.PDF_ERROR.ordinal)
    }

    @Test
    fun numPages_returnsValueFromRenderer() {
        // Arrange
        val fakeRenderer = FakePdfDocumentRenderer(pageCount = 42)
        val remote = createRemoteWithRenderer(fakeRenderer)

        // Act
        val pages = remote.numPages()

        // Assert
        assertThat(pages).isEqualTo(42)
    }

    @Test
    fun getPageDimensions_returnsDimensionsFromPage() {
        // Arrange
        val fakeRenderer = FakePdfDocumentRenderer()
        val remote = createRemoteWithRenderer(fakeRenderer)

        // Act
        val dims = remote.getPageDimensions(0)

        // Assert
        assertThat(dims).isNotNull()
        assertThat(dims!!.width).isEqualTo(100)
        assertThat(dims.height).isEqualTo(100)
    }

    @Test
    fun getPageBitmap_rendersPage() {
        // Arrange
        val fakeRenderer = FakePdfDocumentRenderer()
        val remote = createRemoteWithRenderer(fakeRenderer)

        // Act
        // Request a bitmap
        val bitmap = remote.getPageBitmap(0, 100, 200, RenderParams(RENDER_MODE_FOR_DISPLAY))

        // Assert
        assertThat(bitmap).isNotNull()
        assertThat(bitmap.width).isEqualTo(100)
        assertThat(bitmap.height).isEqualTo(200)

        // Verify the fake page was actually rendered
        val page = fakeRenderer.fakePagesMap[0]!!
        assertThat(page.renderBitmapCalled).isTrue()
    }

    @Test
    fun applyDraftEdits_delegatesToProcessor_returnsSuccess() {
        // Arrange
        val fakeRenderer = FakePdfDocumentRenderer()
        val remote = createRemoteWithRenderer(fakeRenderer)

        val annotation = createStampAnnotationWithPath(pageNum = 0, pathSize = 10)
        val op = InsertDraftEditOperation(annotation)

        // Act
        val result = remote.applyDraftEdits(listOf(op))

        // Assert
        assertThat(result).isInstanceOf(DraftEditResult.Success::class.java)
        val success = result as DraftEditResult.Success
        assertThat(success.ids).containsExactly("1000")
    }

    @Test
    fun closePdfDocument_closesRenderer() {
        // Arrange
        val fakeRenderer = FakePdfDocumentRenderer()
        val remote = createRemoteWithRenderer(fakeRenderer)

        // Act
        remote.closePdfDocument()

        // Assert
        assertThat(fakeRenderer.isClosed).isTrue()
    }

    // --- Helper to setup initialized remote ---
    private fun createRemoteWithRenderer(renderer: FakePdfDocumentRenderer): PdfDocumentRemoteImpl {
        val factory = FakePdfDocumentRendererFactory(rendererToReturn = renderer)
        val remote = PdfDocumentRemoteImpl(factory)
        remote.openPdfDocument(fakePfd, null) // Initialize internal state
        return remote
    }
}
