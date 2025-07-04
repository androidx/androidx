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

package androidx.pdf.view

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.NonNull
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.toRectF
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import androidx.pdf.PdfPoint
import androidx.pdf.R
import androidx.pdf.content.PdfPageGotoLinkContent
import androidx.pdf.content.PdfPageLinkContent
import androidx.pdf.util.ExternalLinks
import androidx.pdf.util.FormWidgetContentDescriptionFactory
import androidx.pdf.util.buildPageIndicatorLabel
import androidx.pdf.view.fastscroll.FastScroller
import kotlin.math.roundToInt

/**
 * Accessibility delegate for PdfView that provides a virtual view hierarchy for pages.
 *
 * This helper class allows accessibility services to interact with individual pages as virtual
 * views, enabling navigation and content exploration.
 */
internal class PdfViewAccessibilityManager(
    private val pdfView: PdfView,
    private val pageMetadataLoader: PageMetadataLoader,
    private val pageManager: PageManager,
    private val formWidgetInteractionHandler: FormWidgetInteractionHandler,
    private val getFastScroller: () -> FastScroller?,
) : ExploreByTouchHelper(pdfView) {

    private val gotoLinks: MutableMap<Int, LinkWrapper<PdfPageGotoLinkContent>> = mutableMapOf()
    private val urlLinks: MutableMap<Int, LinkWrapper<PdfPageLinkContent>> = mutableMapOf()
    // Maps a virtualViewId to a pair representing (pageNum , widgetIndex)
    private val formWidgetInfos: MutableMap<Int, Pair<Int, Int>> = mutableMapOf()
    private val totalPages = pdfView.pdfDocument?.pageCount ?: 0
    private var isLinksLoaded = false
    private var isFormWidgetInfoLoaded = false

    private val fastScrollVerticalThumbDrawableId = FAST_SCROLLER_OFFSET + 1
    private val fastScrollPageIndicatorBackgroundDrawableId = FAST_SCROLLER_OFFSET + 2

    public override fun getVirtualViewAt(x: Float, y: Float): Int {
        val visiblePages = pageMetadataLoader.visiblePages

        if (
            pdfView.lastFastScrollerVisibility &&
                getFastScroller()?.isPointOnThumb(x, y, pdfView.width) == true
        ) {
            return fastScrollVerticalThumbDrawableId
        }

        if (
            pdfView.lastFastScrollerVisibility &&
                getFastScroller()
                    ?.isPointOnIndicator(
                        pdfView.context,
                        pageMetadataLoader.fullyVisiblePages,
                        x,
                        y,
                        totalPages,
                        pdfView.width,
                        pdfView.scrollX,
                    ) == true
        ) {
            return fastScrollPageIndicatorBackgroundDrawableId
        }

        val contentX = pdfView.toContentX(x)
        val contentY = pdfView.toContentY(y)

        if (!isLinksLoaded) {
            loadPageLinks()
        }

        gotoLinks.entries
            .find { (_, wrapper) -> wrapper.linkBounds.contains(contentX, contentY) }
            ?.let {
                return it.key
            }

        urlLinks.entries
            .find { (_, wrapper) -> wrapper.linkBounds.contains(contentX, contentY) }
            ?.let {
                return it.key
            }

        if (!isFormWidgetInfoLoaded) {
            loadFormWidgetInfos()
        }

        formWidgetInfos.entries
            .find { (_, pair) ->
                pageManager.pages[pair.first]
                    .formWidgetIndexToInfoMap
                    ?.get(pair.second)
                    ?.widgetRect
                    ?.contains(contentX.roundToInt(), contentY.roundToInt()) == true
            }
            ?.let {
                return it.key
            }

        // Check if the coordinates fall within the visible page bounds
        return (visiblePages.lower..visiblePages.upper).firstOrNull { page ->
            pageMetadataLoader
                .getPageLocation(page, pdfView.getVisibleAreaInContentCoords())
                .contains(contentX, contentY)
        } ?: HOST_ID
    }

    public override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
        val visiblePages = pageMetadataLoader.visiblePages
        loadPageLinks()
        loadFormWidgetInfos()

        virtualViewIds.apply {
            addAll(visiblePages.lower..visiblePages.upper)
            addAll(gotoLinks.keys)
            addAll(urlLinks.keys)
            addAll(formWidgetInfos.keys)
            if (isFastScrollerStateValid()) {
                add(fastScrollVerticalThumbDrawableId)
                add(fastScrollPageIndicatorBackgroundDrawableId)
            }
        }
    }

    public override fun onPopulateNodeForVirtualView(
        virtualViewId: Int,
        @NonNull node: AccessibilityNodeInfoCompat,
    ) {
        if (!isLinksLoaded) loadPageLinks()
        if (!isFormWidgetInfoLoaded) loadFormWidgetInfos()

        when {
            virtualViewId == fastScrollVerticalThumbDrawableId -> populateFastScrollThumbNode(node)
            virtualViewId == fastScrollPageIndicatorBackgroundDrawableId ->
                populateFastScrollPageIndicatorNode(node)
            virtualViewId < totalPages -> populateNodeForPage(virtualViewId, node)
            else -> {
                // Populate node for GoTo links and URL links
                gotoLinks[virtualViewId]?.let { populateGotoLinkNode(it, node) }
                urlLinks[virtualViewId]?.let { populateUrlLinkNode(it, node) }
                formWidgetInfos[virtualViewId]?.let { populateFormWidgetNode(it, node) }
            }
        }
    }

    private fun populateFastScrollThumbNode(node: AccessibilityNodeInfoCompat) {
        if (!isFastScrollerStateValid()) {
            node.contentDescription = ""
            node.setBoundsInScreen(Rect())
            node.isFocusable = false
            return
        }

        val thumbBounds = getFastScroller()?.getThumbScreenBounds() ?: Rect()
        node.apply {
            contentDescription = pdfView.context.getString(R.string.fast_scroller_thumb)
            setBoundsInScreenFromBoundsInParent(node, thumbBounds)
            isFocusable = pdfView.lastFastScrollerVisibility
        }
    }

    private fun populateFastScrollPageIndicatorNode(node: AccessibilityNodeInfoCompat) {
        if (!isFastScrollerStateValid()) {
            node.contentDescription = ""
            node.setBoundsInScreen(Rect())
            node.isFocusable = false
            return
        }

        val indicatorBounds = getFastScroller()?.getIndicatorScreenBounds() ?: Rect()
        val currentLabel =
            buildPageIndicatorLabel(
                pdfView.context,
                pageMetadataLoader.fullyVisiblePages,
                totalPages,
                R.string.desc_page_single,
                R.string.desc_page_single,
            )
        node.apply {
            contentDescription = currentLabel
            setBoundsInScreenFromBoundsInParent(node, indicatorBounds)
            isFocusable = pdfView.lastFastScrollerVisibility
        }
    }

    /**
     * Checks and sets the AccessibilityNodeInfoCompat to an invalid state if the fast scroller
     * state is invalid.
     *
     * @return True if the fast scroller state is valid, false otherwise.
     */
    private fun isFastScrollerStateValid(): Boolean =
        pdfView.lastFastScrollerVisibility && pdfView.positionIsStable

    override fun onPerformActionForVirtualView(
        virtualViewId: Int,
        action: Int,
        arguments: Bundle?,
    ): Boolean {
        if (action != AccessibilityNodeInfo.ACTION_CLICK) return false

        formWidgetInfos[virtualViewId]?.let { pair ->
            val pageNum = pair.first
            val formWidgetIndex = pair.second
            pageManager.pages[pageNum].formWidgetIndexToInfoMap?.get(formWidgetIndex)?.let {
                formWidgetInfo ->
                if (formWidgetInfo.readOnly) return true

                val pdfTouchPoint =
                    PdfPoint(
                        pageNum,
                        PointF(
                            formWidgetInfo.widgetRect.centerX().toFloat(),
                            formWidgetInfo.widgetRect.centerY().toFloat(),
                        ),
                    )
                formWidgetInteractionHandler.handleInteraction(pdfTouchPoint, formWidgetInfo)
                return true
            }
        }
        // This view does not handle any actions.
        return false
    }

    private fun populateNodeForPage(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
        val pageText = pageManager.pages[virtualViewId]?.pageText
        val pageBounds =
            pageMetadataLoader.getPageLocation(
                virtualViewId,
                pdfView.getVisibleAreaInContentCoords(),
            )

        node.apply {
            contentDescription =
                pageText?.let { getContentDescriptionForPage(pdfView.context, virtualViewId, it) }
                    ?: getDefaultDesc(pdfView.context, virtualViewId)

            setBoundsInScreenFromBoundsInParent(
                node,
                scalePageBounds(RectF(pageBounds), pdfView.zoom),
            )
            isFocusable = true
        }
    }

    private fun populateGotoLinkNode(
        linkWrapper: LinkWrapper<PdfPageGotoLinkContent>,
        node: AccessibilityNodeInfoCompat,
    ) {
        val bounds = scalePageBounds(linkWrapper.linkBounds, pdfView.zoom)

        node.apply {
            contentDescription =
                pdfView.context.getString(
                    R.string.desc_goto_link,
                    linkWrapper.content.destination.pageNumber + 1,
                )
            setBoundsInScreenFromBoundsInParent(this, bounds)
            isFocusable = true
        }
    }

    private fun populateUrlLinkNode(
        linkWrapper: LinkWrapper<PdfPageLinkContent>,
        node: AccessibilityNodeInfoCompat,
    ) {
        val bounds = scalePageBounds(linkWrapper.linkBounds, pdfView.zoom)
        node.apply {
            contentDescription =
                ExternalLinks.getDescription(linkWrapper.content.uri.toString(), pdfView.context)
            setBoundsInScreenFromBoundsInParent(node, bounds)
            isFocusable = true
        }
    }

    private fun populateFormWidgetNode(
        formWidgetInfoPair: Pair<Int, Int>,
        node: AccessibilityNodeInfoCompat,
    ) {
        val pageNum = formWidgetInfoPair.first
        val widgetIndex = formWidgetInfoPair.second
        pageManager.pages[pageNum].formWidgetIndexToInfoMap?.get(widgetIndex)?.let { formWidgetInfo
            ->
            val bounds =
                scalePageBounds(
                    getPageAdjustedBounds(pageNum, formWidgetInfo.widgetRect.toRectF()),
                    pdfView.zoom,
                )
            node.apply {
                contentDescription =
                    FormWidgetContentDescriptionFactory.getContentDescription(
                        formWidgetInfo,
                        pdfView.context,
                    )
                setBoundsInScreenFromBoundsInParent(node, bounds)
                isFocusable = true
            }
            if (!formWidgetInfo.readOnly) {
                node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
            }
        }
    }

    /**
     * Calculates the adjusted bounds relative to the full content of the PDF.
     *
     * @param pageNumber The 0-indexed page number.
     * @param bounds The bounds on the page.
     * @return The adjusted bounds in the content coordinate system.
     */
    fun getPageAdjustedBounds(pageNumber: Int, bounds: RectF): RectF {
        val pageBounds =
            pageMetadataLoader.getPageLocation(pageNumber, pdfView.getVisibleAreaInContentCoords())
        return RectF(
            bounds.left + pageBounds.left,
            bounds.top + pageBounds.top,
            bounds.right + pageBounds.left,
            bounds.bottom + pageBounds.top,
        )
    }

    /**
     * Scales the bounds of a page based on the current zoom level.
     *
     * @param bounds The original bounds to scale.
     * @param zoom The zoom level.
     * @return The scaled bounds as a Rect.
     */
    @VisibleForTesting
    fun scalePageBounds(bounds: RectF, zoom: Float): Rect {
        return Rect(
            (bounds.left * zoom).roundToInt(),
            (bounds.top * zoom).roundToInt(),
            (bounds.right * zoom).roundToInt(),
            (bounds.bottom * zoom).roundToInt(),
        )
    }

    /**
     * Loads the links for the visible pages.
     *
     * This method fetches the GoTo links and URL links for the currently visible pages and stores
     * them in the corresponding maps.
     */
    fun loadPageLinks() {
        val visiblePages = pageMetadataLoader.visiblePages

        // Clear existing links and fetch new ones for the visible pages
        gotoLinks.clear()
        urlLinks.clear()

        var cumulativeId = totalPages

        (visiblePages.lower..visiblePages.upper).forEach { pageIndex ->
            pageManager.pages[pageIndex]?.links?.let { links ->
                links.gotoLinks.forEach { link ->
                    gotoLinks[cumulativeId] =
                        LinkWrapper(
                            pageIndex,
                            link,
                            getPageAdjustedBounds(pageIndex, link.bounds.first()),
                        )
                    cumulativeId++
                }
                links.externalLinks.forEach { link ->
                    urlLinks[cumulativeId] =
                        LinkWrapper(
                            pageIndex,
                            link,
                            getPageAdjustedBounds(pageIndex, link.bounds.first()),
                        )
                    cumulativeId++
                }
            }
        }
        isLinksLoaded = true
    }

    fun loadFormWidgetInfos() {
        formWidgetInfos.clear()
        var currentAvailableVirtualViewId = FORM_WIDGET_VIRTUAL_VIEW_ID_OFFSET
        val visiblePages = pageMetadataLoader.visiblePages
        (visiblePages.lower..visiblePages.upper).forEach { pageIndex ->
            pageManager.pages[pageIndex]?.formWidgetInfos?.let { formWidgetInfos ->
                formWidgetInfos.forEach { formWidgetInfo ->
                    this.formWidgetInfos[currentAvailableVirtualViewId] =
                        pageIndex to formWidgetInfo.widgetIndex
                    currentAvailableVirtualViewId++
                }
            }
        }

        isFormWidgetInfoLoaded = true
    }

    /**
     * Updates accessibility node with extracted page text when ready.
     *
     * @param pageNum 0-indexed page number.
     */
    fun onPageTextReady(pageNum: Int) {
        val pageText = pageManager.pages.get(pageNum)?.pageText

        if (pageText != null) {
            // Update accessibility node with new text.
            invalidateVirtualView(pageNum)
        }
    }

    companion object {
        internal const val FAST_SCROLLER_OFFSET: Int = 1000001
        internal const val FORM_WIDGET_VIRTUAL_VIEW_ID_OFFSET: Int = 10000001

        /**
         * Builds the content description for a page.
         *
         * @param context The context for accessing resources.
         * @param pageText The extracted text content of the page, or null if not loaded.
         * @param pageNum The 0-indexed page number.
         * @return The content description string.
         */
        private fun getContentDescriptionForPage(
            context: Context,
            pageNum: Int,
            pageText: String?,
        ): String {
            return when {
                pageText == null -> getDefaultDesc(context, pageNum)
                pageText.trim().isEmpty() -> context.getString(R.string.desc_empty_page)
                else -> context.getString(R.string.desc_page_with_text, pageNum + 1, pageText)
            }
        }

        /**
         * Gets the default content description for a page. This is used as a placeholder while the
         * actual content description is loading.
         *
         * @param context The context for accessing resources.
         * @param pageNum The 0-indexed page number.
         * @return The default content description string.
         */
        private fun getDefaultDesc(context: Context, pageNum: Int): String {
            return context.getString(R.string.desc_page, pageNum + 1)
        }
    }
}

/**
 * A wrapper class for links in the PDF document.
 *
 * @param T The type of link content (GotoLink or URL link).
 * @param pageNumber The 0-indexed page number where the link is located.
 * @param content The link's content (GotoLink or URL link).
 * @param linkBounds The link's bounds in the full PDF's content coordinates.
 */
private data class LinkWrapper<T>(val pageNumber: Int, val content: T, val linkBounds: RectF)
