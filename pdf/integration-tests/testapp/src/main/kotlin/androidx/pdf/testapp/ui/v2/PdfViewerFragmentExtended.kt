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

package androidx.pdf.testapp.ui.v2

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.annotation.RequiresExtension
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.OperationCanceledException
import androidx.lifecycle.lifecycleScope
import androidx.pdf.PdfDocument
import androidx.pdf.content.ExternalLink
import androidx.pdf.featureflag.PdfFeatureFlags
import androidx.pdf.testapp.R
import androidx.pdf.testapp.ui.FeatureFlagListener
import androidx.pdf.testapp.ui.FeatureFlagNames.FORM_FILLING
import androidx.pdf.testapp.ui.FeatureFlagNames.THUMBNAIL_PREVIEW
import androidx.pdf.testapp.ui.OpCancellationHandler
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.fragment.PdfViewerFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This fragment extends PdfViewerFragment to provide a custom layout and handle immersive mode. It
 * adds a FloatingActionButton for search functionality and manages its visibility based on the
 * immersive mode state. It also includes a toggleable vertical thumbnail preview.
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class PdfViewerFragmentExtended : PdfViewerFragment(), FeatureFlagListener {

    private lateinit var hostView: ConstraintLayout
    private lateinit var pdfContainerFrame: FrameLayout
    private var searchFAB: FloatingActionButton? = null
    private var twoPageLayoutFAB: FloatingActionButton? = null
    private var lastClickedLinkUri: String? = null
    private var activePdfDocument: PdfDocument? = null

    private lateinit var pdfThumbnailToggleButton: ImageButton
    private lateinit var pdfThumbnailRecyclerView: RecyclerView
    private lateinit var thumbnailAdapter: ThumbnailAdapter
    private var lastScrolledThumbnailIndex = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val pdfContentView = super.onCreateView(inflater, container, savedInstanceState) as View

        hostView = inflater.inflate(R.layout.fragment_host, container, false) as ConstraintLayout

        pdfContainerFrame = hostView.findViewById(R.id.pdf_container_frame)
        pdfContainerFrame.addView(pdfContentView, 0)

        searchFAB = hostView.findViewById(R.id.host_Search)
        twoPageLayoutFAB = hostView.findViewById(R.id.two_page_layout)
        pdfThumbnailToggleButton = hostView.findViewById(R.id.pdf_thumbnail_toggle_button)
        pdfThumbnailRecyclerView = hostView.findViewById(R.id.pdf_thumbnail_recycler_view)

        setupThumbnailView()

        searchFAB?.setOnClickListener { isTextSearchActive = true }
        twoPageLayoutFAB?.setOnClickListener {
            pdfView.pagesPerRow =
                when (pdfView.pagesPerRow) {
                    PdfView.SINGLE_PAGE -> PdfView.TWO_PAGE
                    else -> PdfView.SINGLE_PAGE
                }
        }

        return hostView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lastClickedLinkUri = savedInstanceState?.getString(LAST_CLICKED_LINK_URI)
        lastClickedLinkUri?.let { showCustomLinkHandlerDialog(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activePdfDocument = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        lastClickedLinkUri?.let { outState.putString(LAST_CLICKED_LINK_URI, it) }
    }

    private fun setupThumbnailView() {
        if (!::thumbnailAdapter.isInitialized) {
            thumbnailAdapter = ThumbnailAdapter { goToPage(it) }
            pdfThumbnailRecyclerView.adapter = thumbnailAdapter
            pdfThumbnailRecyclerView.layoutManager = LinearLayoutManager(requireContext())

            pdfThumbnailToggleButton.setOnClickListener {
                pdfThumbnailRecyclerView.visibility =
                    if (pdfThumbnailRecyclerView.visibility == View.VISIBLE) View.GONE
                    else View.VISIBLE
            }
        }

        // Start hidden if feature flag is off
        val enabled = PdfFeatureFlags.isThumbnailPreviewEnabled
        pdfThumbnailToggleButton.visibility = if (enabled) View.VISIBLE else View.GONE
        pdfThumbnailRecyclerView.visibility = View.GONE
    }

    override fun onLoadDocumentSuccess(document: PdfDocument) {
        super.onLoadDocumentSuccess(document)
        activePdfDocument = document
        if (PdfFeatureFlags.isThumbnailPreviewEnabled) {
            thumbnailAdapter.clearThumbnails()
            generateThumbnails(document)
            pdfView.removeOnViewportChangedListener(thumbnailViewportListener)
            pdfView.addOnViewportChangedListener(thumbnailViewportListener)
        }
    }

    private val thumbnailViewportListener =
        object : PdfView.OnViewportChangedListener {
            override fun onViewportChanged(
                firstVisiblePage: Int,
                visiblePagesCount: Int,
                pageLocations: SparseArray<RectF>,
                zoomLevel: Float,
            ) {
                if (!::thumbnailAdapter.isInitialized) return

                var currentPage: Int? = null

                // Look through all visible pages and find the first one
                // whose Rect is fully within the PdfView's height
                for (i in 0 until pageLocations.size()) {
                    val pageIndex = pageLocations.keyAt(i)
                    val rect = pageLocations.valueAt(i)

                    if (rect.top >= 0 && rect.bottom <= pdfView.height) {
                        currentPage = pageIndex
                        break
                    }
                }

                // If no fully visible page was found, fall back to PdfView's firstVisiblePage
                val resolvedPage = currentPage ?: firstVisiblePage

                // Only update thumbnails if the visible page actually changed
                if (resolvedPage != lastScrolledThumbnailIndex) {
                    lastScrolledThumbnailIndex = resolvedPage
                    thumbnailAdapter.updateSelectedPage(resolvedPage) // highlight correct thumbnail
                    scrollThumbnailToVisible(resolvedPage) // scroll strip if needed
                }
            }
        }

    private fun scrollThumbnailToVisible(currentPage: Int) {
        val layoutManager = pdfThumbnailRecyclerView.layoutManager as LinearLayoutManager
        val firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition()
        val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()

        if (currentPage < firstVisible || currentPage > lastVisible) {
            val scroller =
                object : LinearSmoothScroller(pdfThumbnailRecyclerView.context) {
                    override fun getVerticalSnapPreference() = SNAP_TO_START
                }
            scroller.targetPosition = currentPage
            layoutManager.startSmoothScroll(scroller)
        }
    }

    private fun generateThumbnails(document: PdfDocument) {
        lifecycleScope.launch(Dispatchers.Default) {
            val thumbnails = mutableListOf<Bitmap>()
            // Defer get calls to fetch on scroll to address perf issue for large PDF b/485486734.
            for (i in 0 until document.pageCount) {
                val bitmap =
                    document.getPageBitmapSource(i).getBitmap(getDynamicThumbnailSize(), null)
                thumbnails.add(bitmap)
            }

            withContext(Dispatchers.Main) {
                if (::thumbnailAdapter.isInitialized) thumbnailAdapter.submitList(thumbnails)
            }
        }
    }

    fun goToPage(pageIndex: Int) {
        thumbnailAdapter.updateSelectedPage(pageIndex)
        pdfView.scrollToPage(pageIndex)
    }

    override fun onRequestImmersiveMode(enterImmersive: Boolean) {
        super.onRequestImmersiveMode(enterImmersive)
        searchFAB?.visibility = if (enterImmersive) View.GONE else View.VISIBLE
        twoPageLayoutFAB?.visibility = if (enterImmersive) View.GONE else View.VISIBLE
        // Toggle thumbnail button visibility only if the feature is enabled
        if (PdfFeatureFlags.isThumbnailPreviewEnabled) {
            pdfThumbnailToggleButton.visibility = if (enterImmersive) View.GONE else View.VISIBLE
        }
    }

    override fun onLoadDocumentError(error: Throwable) {
        super.onLoadDocumentError(error)
        activePdfDocument = null
        if (error is OperationCanceledException) {
            (activity as? OpCancellationHandler)?.handleCancelOperation()
        }
    }

    override fun onLinkClicked(externalLink: ExternalLink): Boolean {
        return if (PdfFeatureFlags.isCustomLinkHandlingEnabled) {
            lastClickedLinkUri = externalLink.uri.toString()
            lastClickedLinkUri?.let(::showCustomLinkHandlerDialog)
            true
        } else super.onLinkClicked(externalLink)
    }

    private fun showCustomLinkHandlerDialog(linkUri: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Custom Link Handler")
            .setMessage("Intercepted link:\n$linkUri")
            .setPositiveButton("OK") { _, _ -> lastClickedLinkUri = null }
            .show()
    }

    override fun onFeatureFlagUpdated(flagName: String, enabled: Boolean) {
        when (flagName) {
            THUMBNAIL_PREVIEW -> {
                pdfThumbnailToggleButton.visibility = if (enabled) View.VISIBLE else View.GONE
                if (!enabled) {
                    pdfThumbnailRecyclerView.visibility = View.GONE
                    pdfView.removeOnViewportChangedListener(thumbnailViewportListener)
                } else {
                    activePdfDocument?.let {
                        if (thumbnailAdapter.itemCount == 0) {
                            generateThumbnails(it)
                        }
                        pdfView.removeOnViewportChangedListener(thumbnailViewportListener)
                        pdfView.addOnViewportChangedListener(thumbnailViewportListener)
                    }
                }
            }
            FORM_FILLING -> {
                pdfView.isFormFillingEnabled = enabled
            }
        }
    }

    private fun getDynamicThumbnailSize(): Size {
        val width = resources.getDimensionPixelSize(R.dimen.thumbnail_width)
        val height = (width * 1.5f).toInt()
        return Size(width, height)
    }

    companion object {
        private const val LAST_CLICKED_LINK_URI = "last_clicked_link_uri"
    }
}
