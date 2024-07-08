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

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.pdf.fetcher.Fetcher
import androidx.pdf.find.FindInFileView
import androidx.pdf.util.ObservableValue.ValueObserver
import androidx.pdf.util.Observables
import androidx.pdf.util.Observables.ExposedValue
import androidx.pdf.viewer.PageIndicator
import androidx.pdf.viewer.PaginatedView
import androidx.pdf.widget.FastScrollView
import androidx.pdf.widget.ZoomView
import androidx.pdf.widget.ZoomView.ContentResizedMode
import androidx.pdf.widget.ZoomView.InitialZoomMode
import androidx.pdf.widget.ZoomView.RotateMode
import androidx.pdf.widget.ZoomView.ZoomScroll

/**
 * A Fragment that renders a PDF document.
 *
 * <p>A [PdfViewerFragment] that can display paginated PDFs. The viewer includes a FAB for
 * annotation support and a search menu. Each page is rendered in its own View. Upon creation, this
 * fragment displays a loading spinner.
 *
 * <p>Rendering is done in 2 passes:
 * <ol>
 * <li>Layout: Request the page data, get the dimensions and set them as measure for the image view.
 * <li>Render: Create bitmap(s) at adequate dimensions and attach them to the page view.
 * </ol>
 *
 * <p>The layout pass is progressive: starts with a few first pages of the document, then reach
 * further as the user scrolls down (and ultimately spans the whole document). The rendering pass is
 * tightly limited to the currently visible pages. Pages that are scrolled past (become not visible)
 * have their bitmaps released to free up memory.
 *
 * @see documentUri
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
open class PdfViewerFragment : Fragment() {

    private var pendingScrollPositionObserver: ValueObserver<ZoomScroll>? = null
    private var viewState: ExposedValue<ViewState> =
        Observables.newExposedValueWithInitialValue(ViewState.NO_VIEW)

    private lateinit var fetcher: Fetcher
    private lateinit var zoomView: ZoomView
    private lateinit var fastScrollView: FastScrollView
    private lateinit var pdfViewer: FrameLayout
    private lateinit var findInFileView: FindInFileView
    private lateinit var paginatedView: PaginatedView
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var pageIndicator: PageIndicator
    private lateinit var fastscrollerPositionObserver: ValueObserver<Int>
    private lateinit var zoomScrollObserver: ValueObserver<ZoomScroll>
    private lateinit var scrollPositionObserverKey: Any
    private lateinit var fastscrollerPositionObserverKey: Any

    /**
     * The URI of the PDF document to display defaulting to `null`.
     *
     * When this property is set, the fragment begins loading the PDF. A loading spinner is
     * displayed until the document is fully loaded. If an error occurs during loading, an error
     * message is displayed, and the detailed exception can be captured by overriding
     * [onLoadDocumentError].
     */
    var documentUri: Uri? = null

    /**
     * Controls the visibility of the "find in file" menu. Defaults to `false`.
     *
     * Set to `true` to display the menu, or `false` to hide it.
     */
    var isTextSearchActive: Boolean = false

    /**
     * Callback invoked when an error occurs while loading the PDF document.
     *
     * Override this method to handle document loading errors. The default implementation displays a
     * generic error message in the loading view.
     *
     * @param throwable [Throwable] that occurred during document loading.
     */
    @Suppress("UNUSED_PARAMETER") fun onLoadDocumentError(throwable: Throwable) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fetcher = Fetcher.build(requireContext(), 1)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        pdfViewer = inflater.inflate(R.layout.pdf_viewer_container, container, false) as FrameLayout
        findInFileView = pdfViewer.findViewById(R.id.search)
        fastScrollView = pdfViewer.findViewById(R.id.fast_scroll_view)
        paginatedView = fastScrollView.findViewById(R.id.pdf_view)

        zoomView = fastScrollView.findViewById(R.id.zoom_view)
        zoomView.setStraightenVerticalScroll(true)

        zoomView
            .setFitMode(ZoomView.FitMode.FIT_TO_WIDTH)
            .setInitialZoomMode(InitialZoomMode.ZOOM_TO_FIT)
            .setRotateMode(RotateMode.KEEP_SAME_VIEWPORT_WIDTH)
            .setContentResizedModeX(ContentResizedMode.KEEP_SAME_RELATIVE)

        // Setting an id so that the View can restore itself. The Id has to be unique and
        // predictable. An alternative that doesn't require id is to rely on this Fragment's
        // onSaveInstanceState().
        zoomView.id = id * 100

        pageIndicator = PageIndicator(requireActivity(), fastScrollView)
        applyReservedSpace()
        zoomView.adjustZoomViewMargins()
        fastscrollerPositionObserver.onChange(null, fastScrollView.scrollerPositionY.get())
        fastscrollerPositionObserverKey =
            fastScrollView.scrollerPositionY.addObserver(fastscrollerPositionObserver)

        // The view system requires the document loaded in order to be properly initialized, so
        // we delay anything view-related until ViewState.VIEW_READY.
        zoomView.visibility = View.GONE

        // TODO: Set fast scroll content model
        // mFastScrollView.setScrollable(this)
        fastScrollView.id = id * 10

        loadingSpinner = fastScrollView.findViewById(R.id.progress_indicator)

        zoomView.zoomScroll().addObserver(zoomScrollObserver)
        if (pendingScrollPositionObserver != null) {
            scrollPositionObserverKey =
                zoomView.zoomScroll().addObserver(pendingScrollPositionObserver)
            pendingScrollPositionObserver = null
        }

        // All views are inflated, update the view state.
        if (viewState.get() == ViewState.NO_VIEW || viewState.get() == ViewState.ERROR) {
            viewState.set(ViewState.VIEW_CREATED)
        }

        return pdfViewer
    }

    private fun applyReservedSpace() {
        if (requireArguments().containsKey(KEY_SPACE_TOP)) {
            zoomView.saveZoomViewBasePadding()
            val left = requireArguments().getInt(KEY_SPACE_LEFT, 0)
            val top = requireArguments().getInt(KEY_SPACE_TOP, 0)
            val right = requireArguments().getInt(KEY_SPACE_RIGHT, 0)
            val bottom = requireArguments().getInt(KEY_SPACE_BOTTOM, 0)

            pageIndicator.view.translationX = -right.toFloat()

            zoomView.setPaddingWithBase(left, top, right, bottom)

            // Adjust the scroll bar to also include the same padding.
            fastScrollView.setScrollbarMarginTop(zoomView.paddingTop)
            fastScrollView.setScrollbarMarginRight(right)
            fastScrollView.setScrollbarMarginBottom(zoomView.paddingBottom)
        }
    }

    companion object {
        private const val KEY_SPACE_LEFT: String = "leftSpace"
        private const val KEY_SPACE_TOP: String = "topSpace"
        private const val KEY_SPACE_BOTTOM: String = "bottomSpace"
        private const val KEY_SPACE_RIGHT: String = "rightSpace"
    }
}
