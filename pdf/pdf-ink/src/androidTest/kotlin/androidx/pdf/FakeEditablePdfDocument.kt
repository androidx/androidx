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

package androidx.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Size
import android.util.SparseArray
import androidx.annotation.OpenForTesting
import androidx.annotation.RequiresExtension
import androidx.pdf.PdfDocument.Companion.LINEARIZATION_STATUS_UNKNOWN
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfObject
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageGotoLinkContent
import androidx.pdf.content.PdfPageLinkContent
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.content.SelectionBoundary
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.models.ListItem
import java.util.UUID
import java.util.concurrent.Executor
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@OpenForTesting
internal open class FakeEditablePdfDocument(
    internal val pages: List<Point?> = listOf(),
    override val formType: Int = PDF_FORM_TYPE_NONE,
    @Deprecated(
        "Deprecated, Use linearizationStatus instead",
        replaceWith = ReplaceWith("linearizationStatus"),
    )
    override val isLinearized: Boolean = false,
    override val renderParams: RenderParams = RenderParams(RenderParams.RENDER_MODE_FOR_DISPLAY),
    private val searchResults: SparseArray<List<PageMatchBounds>> = SparseArray(),
    override val uri: Uri = Uri.parse("content://test.app/document.pdf"),
    private val pageLinks: Map<Int, PdfDocument.PdfPageLinks> = mapOf(),
    private val textContents: List<PdfPageTextContent> = emptyList(),
    private val pageFormWidgetInfos: Map<Int, List<FormWidgetInfo>> = mapOf(),
    initialEdits: List<PdfAnnotation> = emptyList(),
    override val linearizationStatus: Int = LINEARIZATION_STATUS_UNKNOWN,
) : EditablePdfDocument() {
    override val pageCount: Int = pages.size

    @get:Synchronized @set:Synchronized internal var layoutReach: Int = 0

    private val bitmapRequestsLock = Any()
    private val _bitmapRequests = mutableMapOf<Int, SizeParams>()
    internal val bitmapRequests
        get() = _bitmapRequests

    private val _formWidgetRequests = mutableSetOf<Int>()
    internal val formWidgetRequests: Set<Int>
        get() = _formWidgetRequests.toSet()

    internal var editHistory: MutableList<FormEditInfo> = mutableListOf()

    // Store annotations keyed by Page Number
    private val edits = mutableMapOf<Int, MutableList<KeyedPdfAnnotation>>()

    init {
        // Initialize with any provided edits
        initialEdits.forEach { annotation ->
            val pageEdits = edits.getOrPut(annotation.pageNum) { mutableListOf() }
            pageEdits.add(KeyedPdfAnnotation(UUID.randomUUID().toString(), annotation))
        }
    }

    override fun getPageBitmapSource(pageNumber: Int): PdfDocument.BitmapSource {
        return FakeBitmapSource(pageNumber)
    }

    private fun logFormWidgetRequest(pageNum: Int) {
        _formWidgetRequests.add(pageNum)
    }

    override suspend fun getFormWidgetInfos(pageNum: Int, types: Long): List<FormWidgetInfo> {
        logFormWidgetRequest(pageNum)
        if (types == PdfDocument.FORM_WIDGET_INCLUDE_ALL_TYPES)
            return pageFormWidgetInfos[pageNum] ?: emptyList()

        return pageFormWidgetInfos[pageNum]?.filter {
            (1 shl it.widgetType).toLong() and types != 0L
        } ?: emptyList()
    }

    override suspend fun getTopPageObjectAtPosition(pageNum: Int, point: PointF): PdfObject? {
        return null
    }

    override suspend fun applyEdit(record: FormEditInfo) {
        return
    }

    override suspend fun getPageLinks(pageNumber: Int): PdfDocument.PdfPageLinks {
        return pageLinks[pageNumber] ?: PdfDocument.PdfPageLinks(emptyList(), emptyList())
    }

    // --- Annotation Implementations ---

    override suspend fun getAnnotationsForPage(pageNum: Int): List<KeyedPdfAnnotation> {
        return edits[pageNum] ?: emptyList()
    }

    override suspend fun applyEdits(editsDraft: EditsDraft): List<String> {
        val results = mutableListOf<String>()

        for (operation in editsDraft.operations) {
            when (operation) {
                is InsertDraftEditOperation -> {
                    val id = UUID.randomUUID().toString()
                    val pageEdits = edits.getOrPut(operation.annotation.pageNum) { mutableListOf() }
                    pageEdits.add(KeyedPdfAnnotation(id, operation.annotation))
                    results.add(id)
                }
                is UpdateDraftEditOperation -> {
                    val pageNum = operation.annotation.pageNum
                    val pageEdits = edits[pageNum]
                    val index = pageEdits?.indexOfFirst { it.key == operation.id } ?: -1
                    if (index != -1) {
                        pageEdits!![index] = KeyedPdfAnnotation(operation.id, operation.annotation)
                        results.add(operation.id)
                    } else {
                        // Simulate failure or ignore if not found in fake
                        // Real service might throw, but fake can be lenient or throw
                    }
                }
                is RemoveDraftEditOperation -> {
                    val pageEdits = edits[operation.pageNum]
                    val removed = pageEdits?.removeIf { it.key == operation.id } ?: false
                    if (removed) {
                        results.add(operation.id)
                    }
                }
            }
        }
        return results
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    override suspend fun getPageContent(pageNumber: Int): PdfDocument.PdfPageContent {
        if (pageNumber in pages.indices && pageNumber < textContents.size) {
            return PdfDocument.PdfPageContent(
                textContents = listOf(textContents[pageNumber]),
                imageContents = emptyList(),
            )
        }
        return PdfDocument.PdfPageContent(textContents = emptyList(), imageContents = emptyList())
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    override suspend fun getSelectionBounds(
        pageNumber: Int,
        start: PointF,
        stop: PointF,
    ): PageSelection {
        val selectedTextContents =
            if (textContents.isEmpty()) {
                listOf(PdfPageTextContent(listOf(RectF(0f, 0f, 10f, 10f)), "test"))
            } else {
                listOf(textContents[pageNumber])
            }
        return PageSelection(
            pageNumber,
            SelectionBoundary(0),
            SelectionBoundary(0),
            selectedTextContents,
        )
    }

    override suspend fun getSelectAllSelectionBounds(pageNumber: Int): PageSelection? {
        return PageSelection(
            pageNumber,
            SelectionBoundary(0),
            SelectionBoundary(Int.MAX_VALUE),
            listOf(textContents[pageNumber]),
        )
    }

    override suspend fun searchDocument(
        query: String,
        pageRange: IntRange,
    ): SparseArray<List<PageMatchBounds>> {
        return searchResults
    }

    override suspend fun getPageInfos(pageRange: IntRange): List<PdfDocument.PageInfo> {
        return pageRange.map { getPageInfo(it) }
    }

    override suspend fun getPageInfos(
        pageRange: IntRange,
        pageInfoFlags: Long,
    ): List<PdfDocument.PageInfo> {
        return listOf()
    }

    override suspend fun getPageInfo(pageNumber: Int): PdfDocument.PageInfo {
        return getPageInfo(pageNumber, PdfDocument.PAGE_INFO_EXCLUDE_FORM_WIDGETS)
    }

    override suspend fun getPageInfo(pageNumber: Int, pageInfoFlags: Long): PdfDocument.PageInfo {
        layoutReach = maxOf(pageNumber, layoutReach)
        val size = pages[pageNumber]
        if (size == null) {
            throw kotlinx.coroutines.CancellationException()
        }
        if (pageInfoFlags and PdfDocument.PAGE_INFO_INCLUDE_FORM_WIDGET != 0L) {
            return PdfDocument.PageInfo(
                pageNum = pageNumber,
                height = size.y,
                width = size.x,
                formWidgetInfos = getFormWidgetInfos(pageNumber),
            )
        }
        return PdfDocument.PageInfo(pageNumber, size.y, size.x)
    }

    override fun close() {}

    private inner class FakeBitmapSource(override val pageNumber: Int) : PdfDocument.BitmapSource {

        override suspend fun getBitmap(scaledPageSizePx: Size, tileRegion: Rect?): Bitmap {
            logRequest(scaledPageSizePx, tileRegion)
            val size =
                if (tileRegion != null) Size(tileRegion.width(), tileRegion.height())
                else scaledPageSizePx
            val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            bitmap.apply {
                val colorRng = Random(System.currentTimeMillis())
                eraseColor(
                    Color.argb(
                        255,
                        colorRng.nextInt(256),
                        colorRng.nextInt(256),
                        colorRng.nextInt(256),
                    )
                )
            }
            return bitmap
        }

        private fun logRequest(scaledPageSizePx: Size, tileRegion: Rect?) {
            synchronized(bitmapRequestsLock) {
                val requestedSize = _bitmapRequests[pageNumber]
                if (tileRegion == null) {
                    _bitmapRequests[pageNumber] = FullBitmap(scaledPageSizePx)
                } else if (requestedSize != null && requestedSize is Tiles) {
                    requestedSize.withTile(tileRegion)
                } else {
                    _bitmapRequests[pageNumber] =
                        Tiles(scaledPageSizePx).apply { withTile(tileRegion) }
                }
            }
        }

        override fun close() {}
    }

    override fun addOnEditsAppliedListener(executor: Executor, listener: OnEditsAppliedListener) {
        TODO("Not yet implemented")
    }

    override fun removeOnEditsAppliedListener(listener: OnEditsAppliedListener) {
        TODO("Not yet implemented")
    }

    override fun addOnPdfContentInvalidatedListener(
        executor: Executor,
        listener: PdfDocument.OnPdfContentInvalidatedListener,
    ) {}

    override fun removeOnPdfContentInvalidatedListener(
        listener: PdfDocument.OnPdfContentInvalidatedListener
    ) {}

    override fun createWriteHandle(): PdfWriteHandle {
        return object : PdfWriteHandle {
            override suspend fun writeTo(destination: ParcelFileDescriptor) {}

            override fun close() {}
        }
    }

    companion object {
        const val URI_WITH_VALID_SCHEME = "https://www.example.com"
        const val VALID_PAGE_NUMBER = 4

        fun newInstance(): FakeEditablePdfDocument =
            FakeEditablePdfDocument(
                pages = List(10) { Point(100, 200) },
                formType = PdfDocument.PDF_FORM_TYPE_ACRO_FORM,
                textContents =
                    List(10) { index ->
                        PdfPageTextContent(
                            bounds = listOf(RectF(0f, 0f, 100f, 200f)),
                            text = "Sample text for page ${index + 1}",
                        )
                    },
                pageLinks =
                    mapOf(
                        0 to
                            PdfDocument.PdfPageLinks(
                                gotoLinks =
                                    listOf(
                                        PdfPageGotoLinkContent(
                                            bounds = listOf(RectF(25f, 30f, 75f, 50f)),
                                            destination =
                                                PdfPageGotoLinkContent.Destination(
                                                    pageNumber = VALID_PAGE_NUMBER,
                                                    xCoordinate = 10f,
                                                    yCoordinate = 40f,
                                                    zoom = 1f,
                                                ),
                                        )
                                    ),
                                externalLinks =
                                    listOf(
                                        PdfPageLinkContent(
                                            bounds = listOf(RectF(25f, 60f, 75f, 80f)),
                                            uri = Uri.parse(URI_WITH_VALID_SCHEME),
                                        )
                                    ),
                            )
                    ),
                pageFormWidgetInfos =
                    mapOf(
                        0 to
                            listOf(
                                FormWidgetInfo.createRadioButton(
                                    widgetIndex = 0,
                                    widgetRect = Rect(50, 500, 100, 600),
                                    textValue = "false",
                                    accessibilityLabel = "Radio",
                                    isReadOnly = false,
                                )
                            ),
                        1 to
                            listOf(
                                FormWidgetInfo.createListBox(
                                    widgetIndex = 0,
                                    widgetRect = Rect(50, 400, 100, 550),
                                    textValue = "Banana",
                                    accessibilityLabel = "ListBox",
                                    listItems =
                                        listOf(ListItem("Apple", false), ListItem("Banana", false)),
                                    isMultiSelect = true,
                                    isReadOnly = true,
                                )
                            ),
                    ),
            )
    }
}

/** Represents the size and scale of a [Bitmap] requested from [PdfDocument.BitmapSource] */
internal sealed class SizeParams(val scaledPageSizePx: Size)

/** Represents a full page [Bitmap] requested from [PdfDocument.BitmapSource] */
internal class FullBitmap(scaledPageSizePx: Size) : SizeParams(scaledPageSizePx)

/** Represents a set of tile region [Bitmap] requested from [PdfDocument.BitmapSource] */
internal class Tiles(scaledPageSizePx: Size) : SizeParams(scaledPageSizePx) {
    private val _tiles = mutableListOf<Rect>()

    fun withTile(region: Rect) = _tiles.add(region)
}

/** Represents a PDF with no form fields */
internal const val PDF_FORM_TYPE_NONE = 0

/** Represents a PDF with form fields specified using the AcroForm spec */
internal const val PDF_FORM_TYPE_ACRO_FORM = 1

/** Represents a PDF with form fields specified using the entire XFA spec */
internal const val PDF_FORM_TYPE_XFA_FULL = 2

/** Represents a PDF with form fields specified using the XFAF subset of the XFA spec */
internal const val PDF_FORM_TYPE_XFA_FOREGROUND = 3

/**
 * Laying out pages involves waiting for multiple coroutines that are started sequentially. It is
 * not possible to use TestScheduler alone to wait for a certain amount of layout to happen. This
 * uses a polling loop to wait for a certain number of pages to be laid out, up to [timeoutMillis]
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun FakeEditablePdfDocument.waitForLayout(
    untilPage: Int,
    timeoutMillis: Long = 1000,
) {
    // Jump to Dispatchers.Default, as TestDispatcher will skip delays and timeouts
    withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(timeoutMillis) {
            while (layoutReach < untilPage) {
                delay(100)
            }
        }
    }
}

/**
 * Rendering pages involves waiting for multiple coroutines that are started sequentially. It is not
 * possible to use TestScheduler alone to wait for a certain amount of rendering to happen. This
 * uses a polling loop to wait for a certain number of pages to be rendered, up to [timeoutMillis]
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun FakeEditablePdfDocument.waitForRender(
    untilPage: Int,
    timeoutMillis: Long = 1000,
) {
    // Jump to Dispatchers.Default, as TestDispatcher will skip delays and timeouts
    withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(timeoutMillis) {
            while (!bitmapRequests.containsKeys(0..untilPage)) {
                delay(100)
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun FakeEditablePdfDocument.waitForFormDataFetch(
    untilPage: Int,
    timeoutMillis: Long = 1000,
) {
    withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(timeoutMillis) {
            while (!(0..untilPage).all { pageNum -> formWidgetRequests.contains(pageNum) }) {
                delay(100)
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun FakeEditablePdfDocument.waitForApplyEdit(
    expectedNumEdits: Int,
    timeoutMillis: Long = 1000,
) {
    withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(timeoutMillis) {
            while (editHistory.size < expectedNumEdits) {
                delay(100)
            }
        }
    }
}

/** Returns true if every value in [keys] is a key in this [Map] */
private fun <V> Map<Int, V>.containsKeys(keys: IntRange): Boolean {
    for (key in keys) {
        if (key in this) continue
        return false
    }
    return true
}
