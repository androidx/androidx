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

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.pdf.PdfViewerFragment
import androidx.pdf.R
import androidx.pdf.ViewState
import androidx.pdf.data.PdfStatus
import androidx.pdf.data.Range
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
import androidx.pdf.util.Toaster
import androidx.pdf.viewer.LayoutHandler
import androidx.pdf.viewer.LoadingView
import androidx.pdf.viewer.PageIndicator
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

@RestrictTo(RestrictTo.Scope.LIBRARY)
class PdfLoaderCallbacksImpl(
    private var pdfViewerFragment: PdfViewerFragment,
    private var fastScrollView: FastScrollView,
    private var zoomView: ZoomView,
    private var paginatedView: PaginatedView,
    private var loadingView: LoadingView,
    private var annotationButton: FloatingActionButton,
    private var pageIndicator: PageIndicator,
    private var viewState: ExposedValue<ViewState>,
) : PdfLoaderCallbacks {

    private val fragmentActivity: FragmentActivity? = pdfViewerFragment.activity
    private val pageElevationInPixels: Int =
        PaginationUtils.getPageElevationInPixels(pdfViewerFragment.requireContext())

    var selectionModel: PdfSelectionModel? = null
    var searchModel: SearchModel? = null
    var layoutHandler: LayoutHandler? = null
    var fileName: String? = null
    var pageViewFactory: PageViewFactory? = null

    private fun currentPasswordDialog(fm: FragmentManager?): PdfPasswordDialog? {
        if (fm != null) {
            val passwordDialog = fm.findFragmentByTag(PASSWORD_DIALOG_TAG)
            if (passwordDialog is PdfPasswordDialog) {
                return passwordDialog
            }
        }
        return null
    }

    private fun dismissPasswordDialog() {
        currentPasswordDialog(fragmentActivity?.supportFragmentManager)?.dismiss()
    }

    fun handleError() {
        viewState.set(ViewState.ERROR)
    }

    @UiThread
    fun hideSpinner() {
        loadingView.visibility = View.GONE
    }

    private fun showFastScrollView() {
        fastScrollView.setVisible()
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

    fun loadPageAssets(position: ZoomScroll) {
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
        // TODO: Implement isShowing method
        //        if (!isShowing()) {
        //            // This would happen if the service decides to start while we're in
        //            // the background.
        //            // The dialog code below would then crash. We can't just bypass it
        //            // because then we'd
        //            // have
        //            // a started service with no loaded PDF and no means to load it. The
        //            // best way is to
        //            // just
        //            // kill the service which will restart on the next onStart.
        //            mPdfLoader?.disconnect()
        //            return
        //        }

        if (viewState.get() != ViewState.NO_VIEW) {
            val fm: FragmentManager? = fragmentActivity?.supportFragmentManager

            var passwordDialog = currentPasswordDialog(fm)
            if (passwordDialog == null) {
                passwordDialog = PdfPasswordDialog()
                passwordDialog.setListener(
                    object : PdfPasswordDialog.PasswordDialogEventsListener {
                        override fun onPasswordTextChange(password: String) {
                            // pdfLoader?.applyPassword(password)
                        }

                        override fun onDialogCancelled() {
                            val retryCallback = Runnable { requestPassword(false) }
                            val snackbar =
                                pdfViewerFragment.view?.let {
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
                if (fm != null) {
                    passwordDialog.show(fm, PASSWORD_DIALOG_TAG)
                }
            }

            if (incorrect) {
                passwordDialog.retry()
            }
        }
    }

    override fun documentLoaded(numPages: Int) {
        if (numPages <= 0) {
            documentNotLoaded(PdfStatus.PDF_ERROR)
            return
        }

        pdfViewerFragment.documentLoaded = true
        hideSpinner()

        // Assume we see at least the first page
        paginatedView.pageRangeHandler.maxPage = 1
        if (viewState.get() != ViewState.NO_VIEW) {

            paginatedView.paginationModel.initialize(numPages)

            // Add pagination model to the view
            paginatedView.model = paginatedView.paginationModel
            paginatedView.let { paginatedView.paginationModel.addObserver(it) }

            dismissPasswordDialog()

            layoutHandler!!.maybeLayoutPages(1)
            pageIndicator.setNumPages(numPages)
            searchModel?.setNumPages(numPages)
        }

        if (pdfViewerFragment.shouldRedrawOnDocumentLoaded) {
            pdfViewerFragment.shouldRedrawOnDocumentLoaded = false
        }

        if (pdfViewerFragment.isAnnotationIntentResolvable) {
            annotationButton.visibility = View.VISIBLE
        }
    }

    override fun documentNotLoaded(status: PdfStatus) {
        if (viewState.get() != ViewState.NO_VIEW) {
            dismissPasswordDialog()
            if (pdfViewerFragment.arguments?.getBoolean(KEY_QUIT_ON_ERROR) == true) {
                fragmentActivity?.finish()
            }
            when (status) {
                PdfStatus.NONE,
                PdfStatus.FILE_ERROR -> handleError()
                PdfStatus.PDF_ERROR ->
                    pdfViewerFragment.context?.let {
                        Toaster.LONG.popToast(it, R.string.error_file_format_pdf, fileName)
                    }
                PdfStatus.LOADED,
                PdfStatus.REQUIRES_PASSWORD ->
                    Preconditions.checkArgument(
                        false,
                        "Document not loaded but status " + status.number
                    )
                PdfStatus.PAGE_BROKEN,
                PdfStatus.NEED_MORE_DATA -> {}
            }
        }
    }

    override fun pageBroken(page: Int) {
        if (viewState.get() != ViewState.NO_VIEW) {
            (pageViewFactory!!.getOrCreatePageView(
                    page,
                    pageElevationInPixels,
                    paginatedView.paginationModel.getPageSize(page)
                ) as PageMosaicView)
                .setFailure(pdfViewerFragment.getString(R.string.error_on_page, page + 1))
            fragmentActivity?.let { Toaster.LONG.popToast(it, R.string.error_on_page, page + 1) }
            // TODO: Track render error.
        }
    }

    override fun setPageDimensions(pageNum: Int, dimensions: Dimensions) {
        if (viewState.get() != ViewState.NO_VIEW) {

            paginatedView.paginationModel.addPage(pageNum, dimensions)

            layoutHandler!!.pageLayoutReach = paginatedView.paginationModel.size

            if (
                searchModel!!.query().get() != null &&
                    searchModel!!.selectedMatch().get() != null &&
                    searchModel!!.selectedMatch().get()!!.page == pageNum
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
                // During fast-scroll, we mostly don't need to fetch assets, but
                // make sure we keep pushing layout bounds far enough, and update
                // page numbers as we "scroll" down.
                if (pageIndicator.setRangeAndZoom(newRange, zoomView.stableZoom, false)) {
                    showFastScrollView()
                }
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

    companion object {
        private const val PASSWORD_DIALOG_TAG = "password-dialog"
        private const val KEY_QUIT_ON_ERROR = "quitOnError"
    }
}
