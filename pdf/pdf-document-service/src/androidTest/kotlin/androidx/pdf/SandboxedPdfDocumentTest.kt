/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Size
import androidx.annotation.RequiresExtension
import androidx.pdf.annotation.models.ImagePdfObject
import androidx.pdf.annotation.processor.BatchPdfAnnotationsProcessor
import androidx.pdf.annotation.processor.BatchPdfAnnotationsProcessor.Companion.parcelSizeInBytes
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.service.connect.FakePdfServiceConnection
import androidx.pdf.service.connect.PdfServiceConnection
import androidx.pdf.utils.AnnotationUtilsTest.Companion.isRequiredSdkExtensionAvailable
import androidx.pdf.utils.TestUtils
import androidx.pdf.utils.createStampAnnotationWithPath
import androidx.pdf.utils.getSampleStampAnnotation
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.io.File
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.fail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM, codeName = "VanillaIceCream")
@RunWith(AndroidJUnit4::class)
class SandboxedPdfDocumentTest {

    @Test
    fun getPageInfo_validPageNumber_returnsValidPageInfo() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val pageNumber = 0

            val pageInfo = document.getPageInfo(pageNumber)

            val expectedHeight = 792
            val expectedWidth = 612
            assertThat(pageInfo.pageNum == pageNumber).isTrue()
            assertThat(pageInfo.height == expectedHeight).isTrue()
            assertThat(pageInfo.width == expectedWidth).isTrue()
        }
    }

    @Test
    fun getPageInfo_validDimension_onCorruptedPage() = runTest {
        withDocument(PDF_DOCUMENT_PARTIALLY_CORRUPTED_FILE) { document ->
            val pageNumber = 5

            val pageInfo = document.getPageInfo(pageNumber)

            val expectedHeight = 400
            val expectedWidth = 400
            assertThat(pageInfo.pageNum == pageNumber).isTrue()
            assertThat(pageInfo.height == expectedHeight).isTrue()
            assertThat(pageInfo.width == expectedWidth).isTrue()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun getPageInfo_invalidPage_throwsIllegalArgumentException() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val pageNumber = 4

            document.getPageInfo(pageNumber)
        }
    }

    @Test
    fun getPageInfos_partialPageRange_returnsValidPageInfos() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val pageRange = 1..2

            val pageInfos = document.getPageInfos(pageRange)

            val expectedHeight = 792
            val expectedWidth = 612
            val pageIterator = pageInfos.iterator()

            assertThat(pageInfos.size == 2).isTrue()
            for (index: Int in pageRange) {
                assertThat(pageIterator.hasNext()).isTrue()
                val pageInfo = pageIterator.next()
                assertThat(pageInfo.pageNum == index).isTrue()
                assertThat(pageInfo.height == expectedHeight).isTrue()
                assertThat(pageInfo.width == expectedWidth).isTrue()
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun getPageInfos_invalidPageRange_throwsIllegalArgumentException() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val invalidPageRange = 2..4

            document.getPageInfos(invalidPageRange)
        }
    }

    @Test
    fun searchDocument_singlePageSearch_returnsSparseArrayOfResults() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val query = "lorem"
            val pageRange = 0..0

            val results = document.searchDocument(query, pageRange)

            val expectedTotalPageResults = 1
            val expectedFirstPageResults = 2

            assertThat(results.size() == expectedTotalPageResults).isTrue()
            assertThat(results[0].size == expectedFirstPageResults).isTrue()
        }
    }

    @Test
    fun searchDocument_partialDocumentSearch_returnsSparseArrayOfResults() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val query = "lorem"
            val pageRange = 1..2

            val results = document.searchDocument(query, pageRange)

            val expectedTotalPageResults = 2
            val expectedSecondPageResults = 1
            val expectedThirdPageResults = 1

            assertThat(results.size() == expectedTotalPageResults).isTrue()
            assertThat(results[0] == null).isTrue()
            assertThat(results[1].size == expectedSecondPageResults).isTrue()
            assertThat(results[2].size == expectedThirdPageResults).isTrue()
        }
    }

    @Test
    fun searchDocument_fullDocumentSearch_returnsSparseArrayOfResults() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val query = "lorem"
            val pageRange = 0..2

            val results = document.searchDocument(query, pageRange)

            val expectedTotalPageResults = 3
            val expectedFirstPageResults = 2
            val expectedSecondPageResults = 1
            val expectedThirdPageResults = 1

            assertThat(results.size() == expectedTotalPageResults).isTrue()
            assertThat(results[0].size == expectedFirstPageResults).isTrue()
            assertThat(results[1].size == expectedSecondPageResults).isTrue()
            assertThat(results[2].size == expectedThirdPageResults).isTrue()
        }
    }

    @Test
    fun searchDocument_whenCancelled_throwsCancellationExceptionAndStops() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val query = "lorem"
            val pageRange = 0..2

            // Mechanism to signal that the coroutine has actually started
            val searchStarted = MutableStateFlow(false)

            val job = launch {
                try {
                    searchStarted.value = true

                    document.searchDocument(query, pageRange)

                    // Fail the test if we reach this line!
                    fail("Expected CancellationException was not thrown")
                } catch (e: Exception) {
                    // Verify it is specifically a CancellationException
                    assertThat(e).isInstanceOf(kotlinx.coroutines.CancellationException::class.java)
                }
            }

            // Wait for the coroutine to actually start running
            searchStarted.first { it }

            // Yield to allow the searchDocument to progress slightly (hit a suspension point)
            yield()

            // Now cancel
            job.cancelAndJoin()
        }
    }

    @Test
    fun searchDocument_fullDocumentSearch_withSinglePageResults() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val query = "pages are all the same size"
            val pageRange = 0..2

            val results = document.searchDocument(query, pageRange)

            // Assert sparse array doesn't contain empty result lists
            assertThat(results.size()).isEqualTo(1)
            // Assert single result on first page
            assertThat(results[0].size).isEqualTo(1)
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    @Test
    fun getSelectionBounds_returnsPageSelection() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val pageNumber = 0
            val start = PointF(100f, 100f)
            val stop = PointF(120f, 100f)

            val selection = document.getSelectionBounds(pageNumber, start, stop)

            val expectedSelectedText = "F i"
            assertThat(selection != null).isTrue()
            assertThat(selection!!.page == pageNumber).isTrue()
            assertThat(selection.selectedContents.size == 1).isTrue()
            val selectedText = selection.selectedContents[0] as? PdfPageTextContent
            assertThat(selectedText).isNotNull()
            assertThat(selectedText?.text).isEqualTo(expectedSelectedText)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun getSelectionBounds_invalidPageNumber_throwsIllegalArgumentException() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val pageNumber = -1
            val start = PointF(100f, 100f)
            val stop = PointF(200f, 200f)

            document.getSelectionBounds(pageNumber, start, stop)
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    @Test
    fun getSelectionBounds_emptySelection_returnsNull() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val pageNumber = 0
            val start = PointF(100f, 100f)
            val stop = PointF(100f, 100f) // Empty selection

            val selection = document.getSelectionBounds(pageNumber, start, stop)

            assertThat(selection == null).isTrue()
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    @Test
    fun getSelectAllSelectionBounds() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val pageNumber = 0

            val selection = document.getSelectAllSelectionBounds(pageNumber)?.selectedContents
            val expectedSelection = document.getPageContent(pageNumber)?.textContents

            assertNotNull(selection)
            assertNotNull(expectedSelection)
            assertThat(selection?.size == expectedSelection?.size).isTrue()
            for (index: Int in 0..selection!!.size - 1) {
                val selectedText = selection[index] as? PdfPageTextContent
                assertThat(selectedText).isNotNull()
                assertThat(selectedText?.text == expectedSelection!![index].text).isTrue()
            }
        }
    }

    @Test
    fun getPageContent_validPageNumber_returnsPageContentWithTextAndImages() = runTest {
        withDocument(PDF_DOCUMENT_WITH_TEXT_AND_IMAGE) { document ->
            val pageNumber = 0
            val expectedImageContentSize = 1
            val expectedAltText = "Social Security Administration Logo"
            val expectedTextContentSize = 1

            val pageContent = document.getPageContent(pageNumber)

            assertThat(pageContent != null).isTrue()
            assertThat(pageContent!!.textContents.size == expectedTextContentSize).isTrue()
            assertThat(pageContent.imageContents.size == expectedImageContentSize).isTrue()
            assertThat(pageContent.imageContents[0].altText == expectedAltText).isTrue()
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 19)
    @Test
    fun getPageTopObject_validImageObject_fetchLargeImage() = runTest {
        if (!isRequiredSdkExtensionAvailable(19)) return@runTest

        withDocument(PDF_DOCUMENT_WITH_TEXT_AND_IMAGE) { document ->
            val pageNumber = 0
            val point = PointF(500f, 500f)

            val topObject = document.getTopPageObjectAtPosition(pageNumber, point)

            assertNotNull(topObject)
            assertThat(topObject is ImagePdfObject).isTrue()

            (topObject as ImagePdfObject).let { topObject ->
                assertNotNull(topObject.bitmap)
                assertThat(topObject.bitmap.byteCount).isEqualTo(14960000)
                assertThat(topObject.bounds.top).isNotEqualTo(topObject.bounds.bottom)
                assertThat(topObject.bounds.left).isNotEqualTo(topObject.bounds.right)
            }
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 19)
    @Test
    fun getPageTopObject_validImageObject_fetchMediumImage() = runTest {
        if (!isRequiredSdkExtensionAvailable(19)) return@runTest

        withDocument(PDF_DOCUMENT_WITH_IMAGE) { document ->
            val pageNumber = 0
            val point = PointF(150f, 300f)

            val topObject = document.getTopPageObjectAtPosition(pageNumber, point)
            assertNotNull(topObject)
            assertThat(topObject is ImagePdfObject).isTrue()

            (topObject as ImagePdfObject).let { topObject ->
                assertNotNull(topObject.bitmap)
                assertThat(topObject.bitmap.byteCount).isEqualTo(1839280)
                assertThat(topObject.bounds.top).isNotEqualTo(topObject.bounds.bottom)
                assertThat(topObject.bounds.left).isNotEqualTo(topObject.bounds.right)
            }
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 19)
    @Test
    fun getPageTopObject_validImageObject_fetchSmallImage() = runTest {
        if (!isRequiredSdkExtensionAvailable(19)) return@runTest

        withDocument(PDF_DOCUMENT_WITH_LINKS) { document ->
            val pageNumber = 0
            val point = PointF(60f, 50f)

            val topObject = document.getTopPageObjectAtPosition(pageNumber, point)
            assertNotNull(topObject)
            assertThat(topObject is ImagePdfObject).isTrue()

            (topObject as ImagePdfObject).let { topObject ->
                assertNotNull(topObject.bitmap)
                assertThat(topObject.bitmap.byteCount).isEqualTo(23800)
                assertThat(topObject.bounds.top).isNotEqualTo(topObject.bounds.bottom)
                assertThat(topObject.bounds.left).isNotEqualTo(topObject.bounds.right)
            }
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 19)
    @Test
    fun getPageTopObject_validImageObject_notPresent() = runTest {
        if (!isRequiredSdkExtensionAvailable(19)) return@runTest

        withDocument(PDF_DOCUMENT_WITH_LINKS) { document ->
            val pageNumber = 0
            val point1 = PointF(500f, 500f)

            val topObject1 = document.getTopPageObjectAtPosition(pageNumber, point1)
            assertNull(topObject1)

            val point2 = PointF(300f, 300f)

            val topObject2 = document.getTopPageObjectAtPosition(pageNumber, point2)
            assertNull(topObject2)
        }
    }

    @Test
    fun getPageContent_pageWithOnlyText_returnsPageContentWithText() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val pageNumber = 0

            val pageContent = document.getPageContent(pageNumber)

            assertThat(pageContent != null).isTrue()
            assertThat(pageContent!!.textContents.isNotEmpty()).isTrue()
            assertThat(pageContent.imageContents.isEmpty()).isTrue()
        }
    }

    @Test
    fun getPageLinks_validPageNumber_returnsPageLinksWithGotoAndExternalLinks() = runTest {
        withDocument(PDF_DOCUMENT_WITH_LINKS) { document ->
            val pageNumber = 0

            val pageLinks = document.getPageLinks(pageNumber)

            assertThat(pageLinks.gotoLinks.isNotEmpty()).isTrue()
            assertThat(pageLinks.externalLinks.isNotEmpty()).isTrue()
        }
    }

    @Test
    fun getBitmap_fullPage_returnsValidBitmap() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val pageNumber = 0
            val scaledPageSizePx = Size(500, 600)

            val bitmapSource = document.getPageBitmapSource(pageNumber)
            val bitmap = bitmapSource.getBitmap(scaledPageSizePx, tileRegion = null)

            assertThat(bitmap.width == scaledPageSizePx.width).isTrue()
            assertThat(bitmap.height == scaledPageSizePx.height).isTrue()
            assertFalse(bitmap.checkIsAllWhite())
            // TODO(b/377922353): Update this test for a more accurate bitmap comparison
        }
    }

    @Test
    fun getBitmap_tileRegion_returnsValidBitmap() = runTest {
        withDocument(PDF_DOCUMENT) { document ->
            val pageNumber = 0
            val scaledPageSizePx = Size(500, 600)
            val tileRegion = Rect(100, 100, 300, 400) // Example tile region

            val bitmapSource = document.getPageBitmapSource(pageNumber)
            bitmapSource.getBitmap(scaledPageSizePx, tileRegion = null)
            val bitmap = bitmapSource.getBitmap(scaledPageSizePx, tileRegion)

            assertThat(bitmap.width == tileRegion.width()).isTrue()
            assertThat(bitmap.height == tileRegion.height()).isTrue()
            assertFalse(bitmap.checkIsAllWhite())
            // TODO(b/377922353): Update this test for a more accurate bitmap comparison
        }
    }

    @Test
    fun write_modifiedFormFields_returnsModifiedDocument() = runTest {
        val document = openDocument("click_form.pdf")
        val pageNum = 0
        val editableFormWidget =
            document.getFormWidgetInfos(pageNum).find {
                !it.isReadOnly && it.widgetType == FormWidgetInfo.WIDGET_TYPE_CHECKBOX
            }
        requireNotNull(editableFormWidget)

        // assert that the check-box is unselected
        assertThat(editableFormWidget.textValue).isEqualTo("false")

        val editRecord =
            FormEditInfo.createClick(
                widgetIndex = editableFormWidget.widgetIndex,
                clickPoint =
                    PdfPoint(
                        pageNum,
                        editableFormWidget.widgetRect.centerX().toFloat(),
                        editableFormWidget.widgetRect.centerY().toFloat(),
                    ),
            )

        // Apply edit to select the check-box
        document.applyEdit(editRecord)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val editedPdfFile = File(context.cacheDir, "edited_test_pdf.pdf")
        var pfd: ParcelFileDescriptor? = null
        try {
            if (!editedPdfFile.exists()) {
                editedPdfFile.createNewFile()
            }
            pfd = ParcelFileDescriptor.open(editedPdfFile, ParcelFileDescriptor.MODE_READ_WRITE)

            val pdfWriteHandle = document.createWriteHandle()
            pdfWriteHandle.writeTo(pfd)
            pdfWriteHandle.close()

            document.close()

            val editedDocumentUri = Uri.fromFile(editedPdfFile)

            val editedDocument =
                SandboxedPdfLoader(context, Dispatchers.Main).openDocument(editedDocumentUri)
            val editedFormWidget =
                editedDocument.getFormWidgetInfos(pageNum).find {
                    it.widgetIndex == editableFormWidget.widgetIndex
                }
            // assert that the check-box is selected in the edited pdf.
            assertThat(editedFormWidget?.textValue).isEqualTo("true")
            editedDocument.close()
        } finally {
            pfd?.close()
            editedPdfFile.delete()
        }
    }

    @Test
    fun applyEdits_emptyAnnotations_returnsEmptyResult() = runTest {
        if (!isRequiredSdkExtensionAvailable()) return@runTest

        withEditableDocument(PDF_DOCUMENT) { editablePdfDocument ->
            val emptyDraft = MutableEditsDraft().toEditsDraft()

            val result = editablePdfDocument.applyEdits(emptyDraft)

            assertThat(result).isEmpty()
        }
    }

    @Test
    fun applyEdits_addAnnotations_singleBatch_returnsSuccess() = runTest {
        if (!isRequiredSdkExtensionAvailable()) return@runTest

        withEditableDocument(PDF_DOCUMENT) { editablePdfDocument ->
            val pageNum = 1
            val numAnnots = 2
            val draft = MutableEditsDraft()

            repeat(numAnnots) { draft.insert(getSampleStampAnnotation(pageNum)) }

            val totalPayloadSize = draft.operations.sumOf { it.parcelSizeInBytes() }
            val result = editablePdfDocument.applyEdits(draft.toEditsDraft())

            assertThat(totalPayloadSize < BatchPdfAnnotationsProcessor.MAX_BATCH_SIZE_IN_BYTES)
                .isTrue()
            assertThat(result.size).isEqualTo(numAnnots)
        }
    }

    // This is a long running test the payload is approx 1MB and it takes time to propagate all the
    // the annotations over IPC.
    @Test
    fun applyEdits_addAnnotations_multipleBatches_returnsSuccess() = runTest {
        if (!isRequiredSdkExtensionAvailable()) return@runTest

        withEditableDocument(PDF_DOCUMENT) { editablePdfDocument ->
            val numAnnots = 20
            val draft = createDraftWithLargeAnnotations(numAnnots)

            val totalPayloadSize = draft.operations.sumOf { it.parcelSizeInBytes() }

            val result = editablePdfDocument.applyEdits(draft.toEditsDraft())

            assertThat(totalPayloadSize > BatchPdfAnnotationsProcessor.MAX_BATCH_SIZE_IN_BYTES)
                .isTrue()
            assertThat(result.size).isEqualTo(numAnnots)
        }
    }

    @Test
    fun applyEdits_addAnnotations_singleInvalidAnnotation_throwsException() = runTest {
        if (!isRequiredSdkExtensionAvailable()) return@runTest

        withEditableDocument(PDF_DOCUMENT) { editablePdfDocument ->
            val pageNum = 1
            val draft = MutableEditsDraft()

            draft.insert(getSampleStampAnnotation(pageNum))
            // Insert invalid annotation
            draft.insert(getSampleStampAnnotation(pageNum = -1))

            val totalPayloadSize = draft.operations.sumOf { it.parcelSizeInBytes() }
            assertThat(totalPayloadSize < BatchPdfAnnotationsProcessor.MAX_BATCH_SIZE_IN_BYTES)
                .isTrue()

            val thrownException =
                assertFailsWith<PdfEditApplyException> {
                    editablePdfDocument.applyEdits(draft.toEditsDraft())
                }

            assertThat(thrownException.failureIndex).isEqualTo(0)
            assertThat(thrownException.appliedEditIds.size).isEqualTo(0)
            assertThat(thrownException.error.message).isEqualTo("Invalid page index")
        }
    }

    // This is a long running test the payload is approx 1MB and it takes time to propagate all the
    // the annotations over IPC.
    @Test
    fun applyEdits_addAnnotations_multipleBatches__singleInvalidAnnotation_throwsException() =
        runTest {
            if (!isRequiredSdkExtensionAvailable()) return@runTest

            withEditableDocument(PDF_DOCUMENT) { editablePdfDocument ->
                val numAnnots = 19
                val draft = createDraftWithLargeAnnotations(numAnnots)
                // Insert invalid annotation
                draft.insert(getSampleStampAnnotation(pageNum = -1))

                val totalPayloadSize = draft.operations.sumOf { it.parcelSizeInBytes() }
                assertThat(totalPayloadSize > BatchPdfAnnotationsProcessor.MAX_BATCH_SIZE_IN_BYTES)
                    .isTrue()

                val thrownException =
                    assertFailsWith<PdfEditApplyException> {
                        editablePdfDocument.applyEdits(draft.toEditsDraft())
                    }

                assertThat(thrownException.failureIndex).isEqualTo(0)
                assertThat(thrownException.appliedEditIds.size).isEqualTo(0)
                assertThat(thrownException.error.message).isEqualTo("Invalid page index")
            }
        }

    @Test
    fun addOnEditsAppliedListener_singleListener_isNotified() = runTest {
        if (!isRequiredSdkExtensionAvailable()) return@runTest

        val appliedEdits = mutableListOf<BatchPdfAnnotationsProcessor.AppliedEdit>()
        val listener =
            object : EditablePdfDocument.OnEditsAppliedListener {
                override fun onEditApplied(pageNum: Int, editId: String) {
                    appliedEdits.add(BatchPdfAnnotationsProcessor.AppliedEdit(pageNum, editId))
                }
            }

        withEditableDocument(PDF_DOCUMENT) { editablePdfDocument ->
            var pageNum = 0
            val numAnnots = 2
            val draft = MutableEditsDraft()

            repeat(numAnnots) { draft.insert(getSampleStampAnnotation(pageNum++)) }

            editablePdfDocument.addOnEditsAppliedListener(executor = Runnable::run, listener)
            editablePdfDocument.applyEdits(draft.toEditsDraft())

            assertThat(appliedEdits.size).isEqualTo(numAnnots)
            assertThat(appliedEdits[0].pageNum).isEqualTo(0)
            assertThat(appliedEdits[1].pageNum).isEqualTo(1)

            // Clean up
            editablePdfDocument.removeOnEditsAppliedListener(listener)
        }
    }

    @Test
    fun addOnEditsAppliedListener_multipleListeners_sameNotification() = runTest {
        if (!isRequiredSdkExtensionAvailable()) return@runTest

        val appliedEdits1 = mutableListOf<BatchPdfAnnotationsProcessor.AppliedEdit>()
        val appliedEdits2 = mutableListOf<BatchPdfAnnotationsProcessor.AppliedEdit>()

        val listener1 =
            object : EditablePdfDocument.OnEditsAppliedListener {
                override fun onEditApplied(pageNum: Int, editId: String) {
                    appliedEdits1.add(BatchPdfAnnotationsProcessor.AppliedEdit(pageNum, editId))
                }
            }
        val listener2 =
            object : EditablePdfDocument.OnEditsAppliedListener {
                override fun onEditApplied(pageNum: Int, editId: String) {
                    appliedEdits2.add(BatchPdfAnnotationsProcessor.AppliedEdit(pageNum, editId))
                }
            }

        withEditableDocument(PDF_DOCUMENT) { editablePdfDocument ->
            var pageNum = 0
            val numAnnots = 2
            val draft = MutableEditsDraft()

            repeat(numAnnots) { draft.insert(getSampleStampAnnotation(pageNum++)) }

            editablePdfDocument.addOnEditsAppliedListener(executor = Runnable::run, listener1)
            editablePdfDocument.addOnEditsAppliedListener(executor = Runnable::run, listener2)
            editablePdfDocument.applyEdits(draft.toEditsDraft())

            assertThat(appliedEdits1.size).isEqualTo(appliedEdits2.size)
            assertThat(appliedEdits1[0]).isEqualTo(appliedEdits2[0])
            assertThat(appliedEdits1[1]).isEqualTo(appliedEdits2[1])

            // Clean up
            editablePdfDocument.removeOnEditsAppliedListener(listener1)
            editablePdfDocument.removeOnEditsAppliedListener(listener2)
        }
    }

    @Test
    fun removeOnEditsAppliedListener_singleListener_isEmpty() = runTest {
        if (!isRequiredSdkExtensionAvailable()) return@runTest

        val appliedEdits = mutableListOf<BatchPdfAnnotationsProcessor.AppliedEdit>()
        val listener =
            object : EditablePdfDocument.OnEditsAppliedListener {
                override fun onEditApplied(pageNum: Int, editId: String) {
                    appliedEdits.add(BatchPdfAnnotationsProcessor.AppliedEdit(pageNum, editId))
                }
            }

        withEditableDocument(PDF_DOCUMENT) { editablePdfDocument ->
            var pageNum = 0
            val numAnnots = 2
            val draft = MutableEditsDraft()

            repeat(numAnnots) { draft.insert(getSampleStampAnnotation(pageNum++)) }

            editablePdfDocument.addOnEditsAppliedListener(executor = Runnable::run, listener)
            editablePdfDocument.removeOnEditsAppliedListener(listener)
            editablePdfDocument.applyEdits(draft.toEditsDraft())

            assertThat(appliedEdits).isEmpty()
        }
    }

    @Test
    fun documentClosesConnection_whenAllHandlesAreClosed() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var isServiceConnected = false

        val fakeConnection =
            FakePdfServiceConnection(
                context,
                isConnected = false,
                onServiceConnected = { isServiceConnected = true },
                onServiceDisconnected = { isServiceConnected = false },
            )
        val document =
            openDocument(PDF_DOCUMENT, fakeServiceConnection = fakeConnection)
                as SandboxedPdfDocument

        val handle1 = document.createWriteHandle()
        val handle2 = document.createWriteHandle()

        // Close one handle, connection should remain open.
        handle1.close()
        assertThat(isServiceConnected).isTrue()

        // Close the document itself, connection should still remain open as one handle is alive.
        document.close()
        assertThat(isServiceConnected).isTrue()

        // Close the final handle, now the connection should be disconnected.
        handle2.close()
        assertThat(isServiceConnected).isFalse()
    }

    data class AppliedEdit(public val pageNum: Int, public val editId: String)

    companion object {
        private const val PDF_DOCUMENT = "sample.pdf"
        private const val PDF_DOCUMENT_WITH_LINKS = "sample_links.pdf"
        private const val PDF_DOCUMENT_PARTIALLY_CORRUPTED_FILE = "partially_corrupted.pdf"
        private const val PDF_DOCUMENT_WITH_TEXT_AND_IMAGE = "alt_text.pdf"

        private const val PDF_DOCUMENT_WITH_IMAGE = "acro_js.pdf"

        internal suspend fun withDocument(filename: String, block: suspend (PdfDocument) -> Unit) {
            val document = openDocument(filename)
            try {
                block(document)
            } catch (exception: Exception) {
                throw exception
            } finally {
                runTest { document.close() }
            }
        }

        internal suspend fun withEditableDocument(
            filename: String,
            block: suspend (EditablePdfDocument) -> Unit,
        ) {
            val document = openDocument(filename)
            try {
                block(document)
            } catch (exception: Exception) {
                throw exception
            } finally {
                runTest { document.close() }
            }
        }

        private suspend fun openDocument(
            filename: String,
            fakeServiceConnection: PdfServiceConnection? = null,
        ): EditablePdfDocument {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val loader = SandboxedPdfLoader(context, Dispatchers.Main)

            fakeServiceConnection?.let { loader.testingConnection = it }
            val uri = TestUtils.openFile(context, filename)

            val document = loader.openDocument(uri)
            assertThat(document is EditablePdfDocument).isTrue()

            return document as EditablePdfDocument
        }

        private fun Bitmap.checkIsAllWhite(): Boolean {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (getPixel(x, y) != Color.WHITE) {
                        return false
                    }
                }
            }
            return true
        }

        private fun createDraftWithLargeAnnotations(count: Int): MutableEditsDraft {
            val draft = MutableEditsDraft()
            repeat(count) { draft.insert(createStampAnnotationWithPath(0, it * 100)) }
            return draft
        }
    }
}
