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

package androidx.pdf.viewer.loader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.view.View
import android.view.View.VISIBLE
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.fragment.app.FragmentManager
import androidx.pdf.R
import androidx.pdf.ViewState
import androidx.pdf.data.DisplayData
import androidx.pdf.data.PdfStatus
import androidx.pdf.data.Range
import androidx.pdf.find.FindInFileView
import androidx.pdf.models.Dimensions
import androidx.pdf.models.GotoLink
import androidx.pdf.models.LinkRects
import androidx.pdf.models.MatchRects
import androidx.pdf.models.PageSelection
import androidx.pdf.util.Observables.ExposedValue
import androidx.pdf.util.PaginationUtils
import androidx.pdf.util.Preconditions
import androidx.pdf.util.ThreadUtils
import androidx.pdf.util.TileBoard
import androidx.pdf.viewer.LayoutHandler
import androidx.pdf.viewer.LoadingView
import androidx.pdf.viewer.PageMosaicView
import androidx.pdf.viewer.PageViewFactory
import androidx.pdf.viewer.PaginatedView
import androidx.pdf.viewer.PdfPasswordDialog
import androidx.pdf.viewer.PdfSelectionModel
import androidx.pdf.viewer.SearchModel
import androidx.pdf.viewer.SelectedMatch
import androidx.pdf.widget.FastScrollView
import androidx.pdf.widget.ZoomView
import androidx.pdf.widget.ZoomView.ZoomScroll
import com.google.android.material.snackbar.Snackbar

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PdfLoaderCallbacksImpl(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private var fastScrollView: FastScrollView,
    private var zoomView: ZoomView,
    private var paginatedView: PaginatedView,
    private var loadingView: LoadingView,
    private var findInFileView: FindInFileView,
    private var isTextSearchActive: Boolean,
    private var viewState: ExposedValue<ViewState>,
    private val fragmentContainerView: View?,
    private val onRequestPassword: (Boolean) -> Unit,
    private val onDocumentLoaded: () -> Unit,
    private val onDocumentLoadFailure: (Throwable) -> Unit
) : PdfLoaderCallbacks {
    private val pageElevationInPixels: Int = PaginationUtils.getPageElevationInPixels(context)

    public var selectionModel: PdfSelectionModel? = null
    public var searchModel: SearchModel? = null
    public var layoutHandler: LayoutHandler? = null
    public var fileName: String? = null
    public var pageViewFactory: PageViewFactory? = null
    public var pdfLoader: PdfLoader? = null
    public var uri: Uri? = null
    public var onScreen: Boolean = false

    private fun currentPasswordDialog(fm: FragmentManager): PdfPasswordDialog? {
        val passwordDialog = fm.findFragmentByTag(PASSWORD_DIALOG_TAG)
        return passwordDialog as PdfPasswordDialog?
    }

    private fun dismissPasswordDialog() {
        currentPasswordDialog(fragmentManager)?.dismiss()
    }

    private fun handleError(status: PdfStatus) {
        viewState.set(ViewState.ERROR)

        val thrown =
            when (status) {
                PdfStatus.FILE_ERROR ->
                    RuntimeException(context.resources.getString(R.string.file_error))
                PdfStatus.PAGE_BROKEN ->
                    RuntimeException(context.resources.getString(R.string.page_broken))
                PdfStatus.NEED_MORE_DATA ->
                    RuntimeException(context.resources.getString(R.string.needs_more_data))
                else -> RuntimeException(context.resources.getString(R.string.pdf_error))
            }

        onDocumentLoadFailure(thrown)
    }

    @UiThread
    public fun hideSpinner() {
        loadingView.visibility = View.GONE
    }

    private fun lookAtSelection(selection: SelectedMatch?) {
        if (selection == null || selection.isEmpty) {
            return
        }

        if (selection.page >= paginatedView.paginationModel.size) {
            layoutHandler!!.layoutPages(selection.page + 1)
            return
        }

        val rect = selection.pageMatches.getFirstRect(selection.selected)
        val x: Int = paginatedView.paginationModel.getLookAtX(selection.page, rect.centerX())
        val y: Int = paginatedView.paginationModel.getLookAtY(selection.page, rect.centerY())
        zoomView.centerAt(x.toFloat(), y.toFloat())

        (pageViewFactory!!.getOrCreatePageView(
                selection.page,
                pageElevationInPixels,
                paginatedView.paginationModel.getPageSize(selection.page)
            ) as PageMosaicView)
            .setOverlay(selection.overlay)
    }

    public fun loadPageAssets(position: ZoomScroll) {
        // Change the resolution of the bitmaps only when a gesture is not in progress.
        if (position.stable || zoomView.stableZoom == 0f) {
            zoomView.stableZoom = position.zoom
        }

        zoomView.let {
            paginatedView.paginationModel.setViewArea(it.visibleAreaInContentCoords)
            paginatedView.refreshPageRangeInVisibleArea(position, it.height)
            paginatedView.handleGonePages(/* clearViews= */ false)
            paginatedView.loadInvisibleNearPageRange(it.stableZoom)
        }

        // The step (4) below requires page Views to be created and laid out. So we create them here
        // and set this flag if that operation needs to wait for a layout pass.
        val requiresLayoutPass: Boolean = paginatedView.createPageViewsForVisiblePageRange()

        // 4. Refresh tiles and/or full pages.
        if (position.stable) {
            // Perform a full refresh on all visible pages
            viewState.get()?.let {
                zoomView.let { it1 ->
                    paginatedView.refreshVisiblePages(requiresLayoutPass, it, it1.stableZoom)
                }
            }
            paginatedView.handleGonePages(/* clearViews= */ true)
        } else if (zoomView.stableZoom == position.zoom) {
            // Just load a few more tiles in case of tile-scroll
            viewState.get()?.let { paginatedView.refreshVisibleTiles(requiresLayoutPass, it) }
        }

        paginatedView.pageRangeHandler.visiblePages?.let {
            layoutHandler!!.maybeLayoutPages(it.last)
        }
    }

    private fun isPageCreated(pageNum: Int): Boolean {
        return pageNum < paginatedView.paginationModel.size &&
            paginatedView.getViewAt(pageNum) != null
    }

    private fun getPage(pageNum: Int): PageViewFactory.PageView? {
        return paginatedView.getViewAt(pageNum)
    }

    override fun requestPassword(incorrect: Boolean) {
        onRequestPassword(onScreen)

        if (viewState.get() != ViewState.NO_VIEW) {
            var passwordDialog = currentPasswordDialog(fragmentManager)
            if (passwordDialog == null) {
                passwordDialog = PdfPasswordDialog()
                passwordDialog.setListener(
                    object : PdfPasswordDialog.PasswordDialogEventsListener {
                        override fun onPasswordTextChange(password: String) {
                            pdfLoader?.applyPassword(password)
                        }

                        override fun onDialogCancelled() {
                            val retryCallback = Runnable { requestPassword(false) }
                            val snackbar =
                                fragmentContainerView?.let {
                                    Snackbar.make(
                                        it,
                                        R.string.password_not_entered,
                                        Snackbar.LENGTH_INDEFINITE
                                    )
                                }
                            val mResolveClickListener =
                                View.OnClickListener { _: View? -> retryCallback.run() }
                            snackbar?.setAction(R.string.retry_button_text, mResolveClickListener)
                            snackbar?.show()
                        }
                    }
                )

                passwordDialog.show(fragmentManager, PASSWORD_DIALOG_TAG)
            }

            if (incorrect) {
                passwordDialog.retry()
            }
        }
    }

    override fun documentLoaded(numPages: Int, data: DisplayData) {
        if (numPages <= 0) {
            documentNotLoaded(PdfStatus.PDF_ERROR)
            return
        }

        onDocumentLoaded()
        hideSpinner()

        // Assume we see at least the first page
        paginatedView.pageRangeHandler.maxPage = 1
        if (viewState.get() != ViewState.NO_VIEW) {
            if (uri != null && data.uri == uri) {
                paginatedView.paginationModel.setMaxPages(-1)
            }

            paginatedView.paginationModel.initialize(numPages)

            // Add pagination model to the view
            paginatedView.model = paginatedView.paginationModel
            paginatedView.let { paginatedView.paginationModel.addObserver(it) }

            fastScrollView.setPaginationModel(paginatedView.paginationModel)

            dismissPasswordDialog()

            layoutHandler!!.maybeLayoutPages(1)
            searchModel?.setNumPages(numPages)
        }

        if (isTextSearchActive) {
            findInFileView.setFindInFileView(true)
            findInFileView.setVisibility(VISIBLE)
        }
    }

    override fun documentNotLoaded(status: PdfStatus) {
        if (viewState.get() != ViewState.NO_VIEW) {
            dismissPasswordDialog()
            when (status) {
                PdfStatus.NONE,
                PdfStatus.LOADED,
                PdfStatus.REQUIRES_PASSWORD ->
                    Preconditions.checkArgument(
                        false,
                        "Document not loaded but status " + status.number
                    )
                PdfStatus.PDF_ERROR -> {
                    handleError(status)
                }
                PdfStatus.FILE_ERROR,
                PdfStatus.PAGE_BROKEN,
                PdfStatus.NEED_MORE_DATA -> {
                    handleError(status)
                }
            }
        }
    }

    override fun pageBroken(page: Int) {
        if (viewState.get() != ViewState.NO_VIEW) {
            if (page < paginatedView.paginationModel.numPages) {
                (pageViewFactory!!.getOrCreatePageView(
                        page,
                        pageElevationInPixels,
                        paginatedView.paginationModel.getPageSize(page)
                    ) as PageMosaicView)
                    .setFailure(context.resources.getString(R.string.error_on_page, page + 1))
                // TODO: Track render error.
            }
        }
    }

    override fun setPageDimensions(pageNum: Int, dimensions: Dimensions) {
        if (viewState.get() != ViewState.NO_VIEW) {

            paginatedView.paginationModel.addPage(pageNum, dimensions)

            layoutHandler!!.pageLayoutReach = paginatedView.paginationModel.size

            if (
                searchModel!!.query().get() != null &&
                    searchModel!!.selectedMatch().get() != null &&
                    searchModel!!.selectedMatch().get()!!.page == pageNum &&
                    pageViewFactory != null
            ) {
                // lookAtSelection is posted to run once layout has finished:
                ThreadUtils.postOnUiThread {
                    if (viewState.get() != ViewState.NO_VIEW) {
                        lookAtSelection(searchModel!!.selectedMatch().get())
                    }
                }
            }

            viewState.get()?.let { layoutHandler!!.processCallbacksInQueue(it, pageNum) }

            // The new page might actually be visible on the screen, so we need
            // to fetch assets:
            val position: ZoomScroll = zoomView.zoomScroll().get()!!
            val newRange: Range =
                paginatedView.pageRangeHandler.computeVisibleRange(
                    position.scrollY,
                    position.zoom,
                    zoomView.height,
                    true
                )
            if (newRange.isEmpty) {
                layoutHandler!!.maybeLayoutPages(newRange.last)
            } else if (newRange.contains(pageNum)) {
                // The new page is visible, fetch its assets.
                loadPageAssets(zoomView.zoomScroll().get()!!)
            }
        }
    }

    override fun setPageBitmap(pageNum: Int, bitmap: Bitmap) {
        // We announce that the viewer is ready as soon as a bitmap is loaded
        // (not before).
        if (viewState.get() == ViewState.VIEW_CREATED) {
            zoomView.visibility = View.VISIBLE
            viewState.set(ViewState.VIEW_READY)
        }
        if (viewState.get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
            getPage(pageNum)?.getPageView()?.setPageBitmap(bitmap)
        }
    }

    override fun setTileBitmap(pageNum: Int, tileInfo: TileBoard.TileInfo, bitmap: Bitmap) {
        if (viewState.get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
            getPage(pageNum)?.getPageView()?.setTileBitmap(tileInfo, bitmap)
        }
    }

    override fun setPageText(pageNum: Int, text: String) {
        if (viewState.get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
            getPage(pageNum)?.getPageView()?.setPageText(text)
        }
    }

    override fun setSearchResults(query: String, pageNum: Int, matches: MatchRects) {
        if (viewState.get() != ViewState.NO_VIEW && query == searchModel!!.query().get()) {

            searchModel!!.updateMatches(query, pageNum, matches)
            if (isPageCreated(pageNum)) {
                getPage(pageNum)
                    ?.getPageView()
                    ?.setOverlay(searchModel!!.getOverlay(query, pageNum, matches))
            }
        }
    }

    override fun setSelection(pageNum: Int, selection: PageSelection) {
        if (viewState.get() == ViewState.NO_VIEW) {
            return
        }
        searchModel!!.setQuery(null, -1)
        selectionModel?.setSelection(selection)
    }

    override fun setPageUrlLinks(pageNum: Int, links: LinkRects) {
        if (viewState.get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
            getPage(pageNum)?.setPageUrlLinks(links)
        }
    }

    override fun setPageGotoLinks(pageNum: Int, links: MutableList<GotoLink>) {
        if (viewState.get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
            getPage(pageNum)?.setPageGotoLinks(links)
        }
    }

    override fun setInvalidRects(pageNum: Int, invalidRects: MutableList<Rect>) {
        if (viewState.get() != ViewState.NO_VIEW && isPageCreated(pageNum)) {
            if (invalidRects.isEmpty()) {
                return
            }
            paginatedView.getViewAt(pageNum)?.getPageView()?.requestRedrawAreas(invalidRects)
        }
    }

    private companion object {
        private const val PASSWORD_DIALOG_TAG = "password-dialog"
    }
}
