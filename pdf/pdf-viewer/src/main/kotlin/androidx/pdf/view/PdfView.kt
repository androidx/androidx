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

import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Looper
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Range
import android.view.ActionMode
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.toRectF
import androidx.core.os.HandlerCompat
import androidx.core.view.ViewCompat
import androidx.pdf.PdfDocument
import androidx.pdf.R
import androidx.pdf.util.Accessibility
import androidx.pdf.util.MathUtils
import androidx.pdf.util.ZoomUtils
import androidx.pdf.view.fastscroll.FastScrollCalculator
import androidx.pdf.view.fastscroll.FastScrollDrawer
import androidx.pdf.view.fastscroll.FastScrollGestureDetector
import androidx.pdf.view.fastscroll.FastScroller
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * A [View] for presenting PDF content, represented by [PdfDocument].
 *
 * This View supports zooming, scrolling, and flinging. Zooming is supported via pinch gesture,
 * quick scale gesture, and double tap to zoom in or snap back to fitting the page width inside its
 * bounds. Zoom can be changed using the [zoom] property, which is notably distinct from
 * [View.getScaleX] / [View.getScaleY]. Scroll position is based on the [View.getScrollX] /
 * [View.getScrollY] properties.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public open class PdfView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    View(context, attrs, defStyle) {

    public val fastScrollVerticalThumbDrawable: Drawable?
    private val fastScrollVerticalTrackDrawable: Drawable?
    private val fastScrollPageIndicatorBackgroundDrawable: Drawable?

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PdfView)
        fastScrollVerticalThumbDrawable =
            typedArray.getDrawable(R.styleable.PdfView_fastScrollVerticalThumbDrawable)
        fastScrollVerticalTrackDrawable =
            typedArray.getDrawable(R.styleable.PdfView_fastScrollVerticalTrackDrawable)
        fastScrollPageIndicatorBackgroundDrawable =
            typedArray.getDrawable(R.styleable.PdfView_fastScrollPageIndicatorBackgroundDrawable)
        typedArray.recycle()
    }

    /** Supply a [PdfDocument] to process the PDF content for rendering */
    public var pdfDocument: PdfDocument? = null
        set(value) {
            checkMainThread()
            value?.let {
                if (field == value) return
                field = it
                reset()
                onDocumentSet()
            }
        }

    /** The maximum scaling factor that can be applied to this View using the [zoom] property */
    // TODO(b/376299551) - Make maxZoom configurable via XML attribute
    public var maxZoom: Float = DEFAULT_MAX_ZOOM

    /** The minimum scaling factor that can be applied to this View using the [zoom] property */
    // TODO(b/376299551) - Make minZoom configurable via XML attribute
    public var minZoom: Float = DEFAULT_MIN_ZOOM

    /**
     * The zoom level of this view, as a factor of the content's natural size with when 1 pixel is
     * equal to 1 PDF point. Will always be clamped within ([minZoom], [maxZoom])
     */
    public var zoom: Float = DEFAULT_INIT_ZOOM
        set(value) {
            checkMainThread()
            field = value
            onZoomChanged()
        }

    /**
     * A set of areas to be highlighted. Each [Highlight] may be a different color. Setting this
     * property overrides any previous highlights, there is no merging behavior of new and previous
     * values.
     */
    public var highlights: List<Highlight> = listOf()
        set(value) {
            checkMainThread()
            val localPageManager =
                pageManager
                    ?: throw IllegalStateException("Can't highlightAreas without PdfDocument")
            localPageManager.setHighlights(value)
        }

    private val visiblePages: Range<Int>
        get() = pageLayoutManager?.visiblePages?.value ?: Range(0, 0)

    private val fullyVisiblePages: Range<Int>
        get() = pageLayoutManager?.fullyVisiblePages?.value ?: Range(0, 0)

    /** The first page in the viewport, including partially-visible pages. 0-indexed. */
    public val firstVisiblePage: Int
        get() = visiblePages.lower

    /** The number of pages visible in the viewport, including partially visible pages */
    public val visiblePagesCount: Int
        get() = if (pdfDocument != null) visiblePages.upper - visiblePages.lower + 1 else 0

    /** Listener interface for handling clicks on links in a PDF document. */
    public interface LinkClickListener {
        /**
         * Called when a link in the PDF is clicked.
         *
         * @param uri The URI associated with the link.
         */
        public fun onLinkClicked(uri: Uri)
    }

    /** The listener that is notified when a link in the PDF is clicked. */
    public var linkClickListener: LinkClickListener? = null

    /** The [ActionMode.Callback2] for selection */
    public var selectionActionModeCallback: DefaultSelectionActionModeCallback =
        DefaultSelectionActionModeCallback(this)

    /** The currently selected PDF content, as [Selection] */
    public val currentSelection: Selection?
        get() {
            return selectionStateManager?.selectionModel?.value?.selection
        }

    @VisibleForTesting
    public val currentPageIndicatorLabel: String
        get() = fastScroller?.fastScrollDrawer?.currentPageIndicatorLabel ?: ""

    /** Listener interface to receive updates when the [currentSelection] changes */
    public interface OnSelectionChangedListener {
        /** Called when the [Selection] has changed */
        public fun onSelectionChanged(
            previousSelection: Selection?,
            newSelection: Selection?,
        )
    }

    private var onSelectionChangedListeners = mutableListOf<OnSelectionChangedListener>()

    /**
     * The [CoroutineScope] used to make suspending calls to [PdfDocument]. The size of the fixed
     * thread pool is arbitrary and subject to tuning.
     */
    private val backgroundScope: CoroutineScope =
        CoroutineScope(Executors.newFixedThreadPool(5).asCoroutineDispatcher() + SupervisorJob())

    private var pageLayoutManager: PageLayoutManager? = null
    private var pageManager: PageManager? = null
    private var visiblePagesCollector: Job? = null
    private var layoutInfoCollector: Job? = null
    private var pageSignalCollector: Job? = null
    private var selectionStateCollector: Job? = null

    private var deferredScrollPage: Int? = null
    private var deferredScrollPosition: PdfPoint? = null

    /** Used to restore saved state */
    private var stateToRestore: PdfViewSavedState? = null
    private var awaitingFirstLayout: Boolean = true
    private var scrollPositionToRestore: PointF? = null
    private var zoomToRestore: Float? = null
    @VisibleForTesting internal var isInitialZoomDone: Boolean = false

    /**
     * Flag to indicate if we need to override fast scroll visibility. This is true when the search
     * is visible
     */
    private var shouldOverrideFastScrollVisibility: Boolean = false

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var isFastScrollerVisible: Boolean = false
        private set

    /**
     * The width of the PdfView before the last layout change (e.g., before rotation). Used to
     * preserve the zoom level when the device is rotated.
     */
    private var oldWidth: Int? = width

    private var fastScroller: FastScroller? = null
    private var fastScrollGestureDetector: FastScrollGestureDetector? = null

    private val gestureHandler = ZoomScrollGestureHandler()
    private val gestureTracker = GestureTracker(context).apply { delegate = gestureHandler }

    private val scroller = RelativeScroller(context)
    /** Whether we are in a fling movement. This is used to detect the end of that movement */
    private var isFling = false

    /**
     * Returns true if neither zoom nor scroll are actively changing. Does not account for
     * externally-driven changes in position (e.g. a animating scrollY or zoom)
     */
    private val positionIsStable: Boolean
        get() {
            val zoomIsChanging = gestureTracker.matches(GestureTracker.Gesture.ZOOM)
            val scrollIsChanging =
                gestureTracker.matches(
                    GestureTracker.Gesture.DRAG,
                    GestureTracker.Gesture.DRAG_X,
                    GestureTracker.Gesture.DRAG_Y
                ) || isFling
            return !zoomIsChanging && !scrollIsChanging
        }

    // To avoid allocations during drawing
    private val visibleAreaRect = Rect()

    private val fastScrollGestureHandler =
        object : FastScrollGestureDetector.FastScrollGestureHandler {
            override fun onFastScrollDetected(scrollY: Float) {
                fastScroller?.let {
                    val updatedY =
                        it.viewScrollPositionFromFastScroller(scrollY, zoom, height, contentHeight)
                    scrollTo(scrollX, updatedY)
                    invalidate()
                }
            }
        }

    @VisibleForTesting internal var accessibilityPageHelper: AccessibilityPageHelper? = null
    @VisibleForTesting
    internal var isTouchExplorationEnabled: Boolean =
        Accessibility.get().isTouchExplorationEnabled(context)

    private var selectionStateManager: SelectionStateManager? = null
    private val selectionRenderer = SelectionRenderer(context)
    private var selectionActionMode: ActionMode? = null
    private var gestureInProgress = false

    /**
     * Scrolls to the 0-indexed [pageNum], optionally animating the scroll
     *
     * This View cannot scroll to a page until it knows its dimensions. If [pageNum] is distant from
     * the currently-visible page in a large PDF, there may be some delay while dimensions are being
     * loaded from the PDF.
     */
    @Suppress("UNUSED_PARAMETER")
    public fun scrollToPage(pageNum: Int) {
        checkMainThread()
        val localPageLayoutManager =
            pageLayoutManager
                ?: throw IllegalStateException("Can't scrollToPage without PdfDocument")
        require(pageNum < (pdfDocument?.pageCount ?: Int.MIN_VALUE)) {
            "Page $pageNum not in document"
        }

        if (localPageLayoutManager.reach >= pageNum) {
            gotoPage(pageNum)
        } else {
            localPageLayoutManager.increaseReach(pageNum)
            deferredScrollPage = pageNum
            deferredScrollPosition = null
        }
    }

    /**
     * Scrolls to [position], optionally animating the scroll
     *
     * This View cannot scroll to a page until it knows its dimensions. If [position] is distant
     * from the currently-visible page in a large PDF, there may be some delay while dimensions are
     * being loaded from the PDF.
     */
    @Suppress("UNUSED_PARAMETER")
    public fun scrollToPosition(position: PdfPoint) {
        checkMainThread()
        val localPageLayoutManager =
            pageLayoutManager
                ?: throw IllegalStateException("Can't scrollToPage without PdfDocument")

        if (position.pageNum >= (pdfDocument?.pageCount ?: Int.MIN_VALUE)) {
            return
        }

        if (localPageLayoutManager.reach >= position.pageNum) {
            gotoPoint(position)
        } else {
            localPageLayoutManager.increaseReach(position.pageNum)
            deferredScrollPosition = position
            deferredScrollPage = null
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun overrideFastScrollerVisibility(isVisible: Boolean) {
        shouldOverrideFastScrollVisibility = !isVisible
        if (isVisible) {
            maybeShowFastScroller()
        } else {
            maybeHideFastScroller()
        }
    }

    /**
     * Adds the specified listener to the list of listeners that will be notified of selection
     * change events.
     *
     * @param listener listener to notify when selection change events occur
     * @see removeOnSelectionChangedListener
     */
    public fun addOnSelectionChangedListener(listener: OnSelectionChangedListener) {
        onSelectionChangedListeners.add(listener)
    }

    /**
     * Removes the specified listener from the list of listeners that will be notified of selection
     * change events.
     *
     * @param listener listener to remove
     */
    public fun removeOnSelectionChangedListener(listener: OnSelectionChangedListener) {
        onSelectionChangedListeners.remove(listener)
    }

    private fun dispatchSelectionChanged(old: Selection?, new: Selection?) {
        for (listener in onSelectionChangedListeners) {
            listener.onSelectionChanged(old, new)
        }
    }

    private fun gotoPage(pageNum: Int) {
        checkMainThread()
        val localPageLayoutManager =
            pageLayoutManager
                ?: throw IllegalStateException("Can't scrollToPage without PdfDocument")
        check(pageNum <= localPageLayoutManager.reach) { "Can't gotoPage that's not laid out" }

        val pageRect =
            localPageLayoutManager.getPageLocation(pageNum, getVisibleAreaInContentCoords())
        // Zoom should match the width of the page
        val zoom =
            ZoomUtils.calculateZoomToFit(
                viewportWidth.toFloat(),
                viewportHeight.toFloat(),
                pageRect.width().toFloat(),
                1f
            )
        val x = round((pageRect.left + pageRect.width() / 2f) * zoom - (viewportWidth / 2f))
        val y = round((pageRect.top + pageRect.height() / 2f) * zoom - (viewportHeight / 2f))

        // Set zoom to fit the width of the page, then scroll to the center of the page
        this.zoom = zoom
        scrollTo(x.toInt(), y.toInt())
    }

    /** Clears the current selection, if one exists. No-op if there is no current [Selection] */
    public fun clearSelection() {
        selectionStateManager?.clearSelection()
    }

    private fun gotoPoint(position: PdfPoint) {
        checkMainThread()
        val localPageLayoutManager =
            pageLayoutManager
                ?: throw IllegalStateException("Can't scrollToPage without PdfDocument")
        check(position.pageNum <= localPageLayoutManager.reach) {
            "Can't gotoPoint on page that's not laid out"
        }

        val pageRect =
            localPageLayoutManager.getPageLocation(
                position.pageNum,
                getVisibleAreaInContentCoords()
            )
        // Zoom should match the width of the page
        val zoom =
            ZoomUtils.calculateZoomToFit(
                viewportWidth.toFloat(),
                viewportHeight.toFloat(),
                pageRect.width().toFloat(),
                1f
            )

        val x = round((pageRect.left + pageRect.width() / 2f) * zoom - (viewportWidth / 2f))
        val y = round((pageRect.top + position.pagePoint.y) * zoom - (viewportHeight / 2f))

        // Set zoom to fit the width of the page, then scroll to the requested point on the page
        this.zoom = zoom
        scrollTo(x.toInt(), y.toInt())
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return accessibilityPageHelper?.dispatchHoverEvent(event) == true ||
            super.dispatchHoverEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return accessibilityPageHelper?.dispatchKeyEvent(event) == true ||
            super.dispatchKeyEvent(event)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        accessibilityPageHelper?.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val localPaginationManager = pageLayoutManager ?: return
        canvas.save()
        canvas.scale(zoom, zoom)
        val selectionModel = selectionStateManager?.selectionModel
        for (i in visiblePages.lower..visiblePages.upper) {
            val pageLoc = localPaginationManager.getPageLocation(i, getVisibleAreaInContentCoords())
            pageManager?.drawPage(i, canvas, pageLoc)
            selectionModel?.value?.let {
                selectionRenderer.drawSelectionOnPage(
                    model = it,
                    pageNum = i,
                    canvas,
                    pageLoc,
                    zoom
                )
            }
        }
        canvas.restore()

        val documentPageCount = pdfDocument?.pageCount ?: 0
        if (documentPageCount > 1 && !shouldOverrideFastScrollVisibility) {
            fastScroller?.drawScroller(
                canvas,
                scrollY,
                zoom,
                height,
                /* visibleArea= */ getVisibleAreaInContentCoords(),
                fullyVisiblePages,
                contentHeight
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var handled = event?.let { fastScrollGestureDetector?.handleEvent(it, width) } ?: false
        handled = handled || maybeDragSelectionHandle(event)
        handled = handled || event?.let { gestureTracker.feed(it) } ?: false
        return handled || super.onTouchEvent(event)
    }

    private fun maybeShowFastScroller() {
        if (!shouldOverrideFastScrollVisibility) {
            isFastScrollerVisible = true
            fastScroller?.show { postInvalidate() }
        }
    }

    private fun maybeHideFastScroller() {
        isFastScrollerVisible = false
        fastScroller?.hide { postInvalidate() }
    }

    private fun maybeDragSelectionHandle(event: MotionEvent?): Boolean {
        if (event == null) return false
        val touchPoint =
            pageLayoutManager?.getPdfPointAt(
                PointF(toContentX(event.x), toContentY(event.y)),
                getVisibleAreaInContentCoords()
            )
        return selectionStateManager?.maybeDragSelectionHandle(event.action, touchPoint, zoom) ==
            true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onViewportChanged()
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        // TODO(b/390003204): Prevent showing of the scrubber when the document only been
        //  translated on the x-axis
        if (t != oldt) {
            maybeShowFastScroller()
        }
        onViewportChanged()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            val localScrollPositionToRestore = scrollPositionToRestore
            if (awaitingFirstLayout && localScrollPositionToRestore != null) {
                var newZoom = (zoomToRestore ?: zoom) * (width.toFloat() / (oldWidth ?: width))
                newZoom = MathUtils.clamp(newZoom, minZoom, maxZoom)
                this.zoom = newZoom
                scrollToRestoredPosition(localScrollPositionToRestore, zoom)
            }
        }
        awaitingFirstLayout = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        stopCollectingData()
        awaitingFirstLayout = true
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) startCollectingData() else stopCollectingData()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopCollectingData()
        awaitingFirstLayout = true
        pageManager?.cleanup()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val state = PdfViewSavedState(superState)
        state.zoom = zoom
        state.isInitialZoomDone = isInitialZoomDone
        state.viewWidth = width
        state.contentCenterX = toContentX(viewportWidth.toFloat() / 2f)
        state.contentCenterY = toContentY(viewportHeight.toFloat() / 2f)
        // Keep scroll at top if previously at top.
        if (scrollY <= 0) {
            state.contentCenterY = 0F
        }
        state.documentUri = pdfDocument?.uri
        state.paginationModel = pageLayoutManager?.paginationModel
        state.selectionModel = selectionStateManager?.selectionModel?.value
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is PdfViewSavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        stateToRestore = state
        if (pdfDocument != null) {
            maybeRestoreState()
        }
    }

    override fun computeScroll() {
        // Cause OverScroller to compute the new position
        if (scroller.computeScrollOffset()) {
            scroller.apply(this)
            postInvalidateOnAnimation()
        } else if (isFling) {
            isFling = false
            // We hide the action mode during a fling, so reveal it when the fling is over
            updateSelectionActionModeVisibility()
            // Once the fling has ended, prompt the page manager to start fetching data for pages
            // that we don't fetch during a fling
            maybeUpdatePageVisibility()
        }
    }

    override fun scrollBy(x: Int, y: Int) {
        // This is precisely the implementation of View.scrollBy; this is defensive in case the
        // View implementation changes given we assume all scrolling flows through scrollTo
        scrollTo(scrollX + x, scrollY + y)
    }

    override fun scrollTo(x: Int, y: Int) {
        val cappedX = x.coerceIn(0..computeHorizontalScrollRange())
        val cappedY = y.coerceIn(minVerticalScrollPosition..computeVerticalScrollRange())
        super.scrollTo(cappedX, cappedY)
    }

    override fun computeHorizontalScrollRange(): Int {
        // Note we provide scroll = 0 here, as we shouldn't consider the current scroll position
        // to compute the maximum scroll position. Scroll position is absolute, not relative
        val contentWidthPx = toViewCoord(contentWidth.toFloat(), zoom, scroll = 0)
        return if (contentWidthPx < width) 0 else (contentWidthPx - width).roundToInt()
    }

    private val minVerticalScrollPosition: Int
        get() {
            // Note we provide scroll = 0 here, as we shouldn't consider the current scroll position
            // to compute the maximum scroll position. Scroll position is absolute, not relative
            val contentHeightPx = toViewCoord(contentHeight.toFloat(), zoom, scroll = 0)
            return if (contentHeightPx < height) {
                // Center vertically
                -(height - contentHeightPx).roundToInt() / 2
            } else {
                0
            }
        }

    override fun computeVerticalScrollRange(): Int {
        // Note we provide scroll = 0 here, as we shouldn't consider the current scroll position
        // to compute the maximum scroll position. Scroll position is absolute, not relative
        val contentHeightPx = toViewCoord(contentHeight.toFloat(), zoom, scroll = 0)
        return if (contentHeightPx < height) {
            // Center vertically
            -(height - contentHeightPx).roundToInt() / 2
        } else {
            (contentHeightPx - height).roundToInt()
        }
    }

    @VisibleForTesting
    internal fun getDefaultZoom(): Float {
        if (contentWidth == 0 || viewportWidth == 0) return DEFAULT_INIT_ZOOM
        val widthZoom = viewportWidth.toFloat() / contentWidth
        return MathUtils.clamp(widthZoom, minZoom, maxZoom)
    }

    /**
     * Returns true if we are able to restore a previous state from savedInstanceState
     *
     * We are not be able to restore our previous state if it pertains to a different document, or
     * if it is missing critical data like page layout information.
     */
    private fun maybeRestoreState(): Boolean {
        val localStateToRestore = stateToRestore ?: return false
        val localPdfDocument = pdfDocument ?: return false
        if (
            localPdfDocument.uri != localStateToRestore.documentUri ||
                !localStateToRestore.hasEnoughStateToRestore
        ) {
            stateToRestore = null
            return false
        }
        pageLayoutManager =
            PageLayoutManager(
                    localPdfDocument,
                    backgroundScope,
                    DEFAULT_PAGE_PREFETCH_RADIUS,
                    paginationModel = requireNotNull(localStateToRestore.paginationModel)
                )
                .apply { onViewportChanged(scrollY, height, zoom) }
        selectionStateManager =
            SelectionStateManager(
                localPdfDocument,
                backgroundScope,
                resources.getDimensionPixelSize(R.dimen.text_select_handle_touch_size),
                localStateToRestore.selectionModel
            )

        val positionToRestore =
            PointF(localStateToRestore.contentCenterX, localStateToRestore.contentCenterY)
        if (awaitingFirstLayout) {
            scrollPositionToRestore = positionToRestore
            zoomToRestore = localStateToRestore.zoom
            oldWidth = localStateToRestore.viewWidth
            isInitialZoomDone = localStateToRestore.isInitialZoomDone
        } else {
            scrollToRestoredPosition(positionToRestore, localStateToRestore.zoom)
        }
        setAccessibility()

        stateToRestore = null
        return true
    }

    private fun scrollToRestoredPosition(position: PointF, zoom: Float) {
        this.zoom = zoom
        val scrollX = round(position.x * zoom - viewportWidth / 2f).toInt()
        val scrollY = round(position.y * zoom - viewportHeight / 2f).toInt()
        scrollTo(scrollX, scrollY)
        scrollPositionToRestore = null
        zoomToRestore = null
    }

    /**
     * Launches a tree of coroutines to collect data from helper classes while we're attached to a
     * visible window
     */
    @MainThread
    private fun startCollectingData() {
        val mainScope =
            CoroutineScope(HandlerCompat.createAsync(handler.looper).asCoroutineDispatcher())
        pageLayoutManager?.let { manager ->
            // Don't let two copies of this run concurrently
            val layoutInfoToJoin = layoutInfoCollector?.apply { cancel() }
            layoutInfoCollector =
                mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    // Prevent 2 copies from running concurrently
                    layoutInfoToJoin?.join()
                    launch {
                        manager.dimensions.collect { onPageDimensionsReceived(it.first, it.second) }
                    }
                    launch { manager.visiblePages.collect { maybeUpdatePageVisibility() } }
                }
            // Don't let two copies of this run concurrently
            val visiblePagesToJoin = visiblePagesCollector?.apply { cancel() }
            visiblePagesCollector =
                mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    manager.visiblePages.collect {
                        // Prevent 2 copies from running concurrently
                        visiblePagesToJoin?.join()
                        maybeUpdatePageVisibility()
                    }
                }
        }
        pageManager?.let { manager ->
            val pageSignalsToJoin = pageSignalCollector?.apply { cancel() }
            pageSignalCollector =
                mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    // Prevent 2 copies from running concurrently
                    pageSignalsToJoin?.join()
                    launch { manager.invalidationSignalFlow.collect { invalidate() } }
                    launch {
                        manager.pageTextReadyFlow.collect { pageNum ->
                            accessibilityPageHelper?.onPageTextReady(pageNum)
                        }
                    }
                }
        }
        selectionStateManager?.let { manager ->
            val selectionToJoin = selectionStateCollector?.apply { cancel() }
            selectionStateCollector =
                mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    // Prevent 2 copies from running concurrently
                    selectionToJoin?.join()
                    launch { manager.selectionUiSignalBus.collect { onSelectionUiSignal(it) } }
                    var prevSelection = currentSelection
                    launch {
                        manager.selectionModel.collect { newModel ->
                            dispatchSelectionChanged(prevSelection, newModel?.selection)
                            prevSelection = newModel?.selection
                        }
                    }
                }
        }
    }

    private fun stopCollectingData() {
        layoutInfoCollector?.cancel()
        visiblePagesCollector?.cancel()
        pageSignalCollector?.cancel()
        selectionStateCollector?.cancel()
    }

    private fun onSelectionUiSignal(signal: SelectionUiSignal) {
        when (signal) {
            is SelectionUiSignal.PlayHapticFeedback -> {
                performHapticFeedback(signal.level)
            }
            is SelectionUiSignal.Invalidate -> {
                invalidate()
            }
            is SelectionUiSignal.ToggleActionMode -> {
                if (signal.show && selectionActionMode == null && currentSelection != null) {
                    startActionMode(selectionActionModeCallback, ActionMode.TYPE_FLOATING)
                } else if (!signal.show) {
                    selectionActionMode?.finish()
                    selectionActionMode = null
                }
            }
        }
    }

    /** Start using the [PdfDocument] to present PDF content */
    // Display.width and height are deprecated in favor of WindowMetrics, but in this case we
    // actually want to use the size of the display and not the size of the window.
    @Suppress("deprecation")
    private fun onDocumentSet() {
        val localPdfDocument = pdfDocument ?: return
        /* We use the maximum pixel dimension of the display as the maximum pixel dimension for any
        single Bitmap we render, i.e. the threshold for tiled rendering. This is an arbitrary,
        but reasonable threshold to use that does not depend on volatile state like the current
        screen orientation or the current size of our application's Window. */
        val maxBitmapDimensionPx = max(context.display.width, context.display.height)

        pageManager =
            PageManager(
                localPdfDocument,
                backgroundScope,
                DEFAULT_PAGE_PREFETCH_RADIUS,
                Point(maxBitmapDimensionPx, maxBitmapDimensionPx),
                isTouchExplorationEnabled
            )

        if (
            fastScrollVerticalThumbDrawable != null &&
                fastScrollVerticalTrackDrawable != null &&
                fastScrollPageIndicatorBackgroundDrawable != null
        ) {

            val fastScrollCalculator = FastScrollCalculator(context)
            val fastScrollDrawer =
                FastScrollDrawer(
                    context,
                    localPdfDocument,
                    fastScrollVerticalThumbDrawable,
                    fastScrollVerticalTrackDrawable,
                    fastScrollPageIndicatorBackgroundDrawable
                )

            val localFastScroller = FastScroller(fastScrollDrawer, fastScrollCalculator)
            fastScroller = localFastScroller
            fastScrollGestureDetector =
                FastScrollGestureDetector(localFastScroller, fastScrollGestureHandler)
            maybeShowFastScroller()
        }

        // We'll either create our layout and selection managers from restored state, or
        // instantiate new ones
        if (!maybeRestoreState()) {
            pageLayoutManager =
                PageLayoutManager(localPdfDocument, backgroundScope, DEFAULT_PAGE_PREFETCH_RADIUS)
                    .apply { onViewportChanged(scrollY, height, zoom) }
            selectionStateManager =
                SelectionStateManager(
                    localPdfDocument,
                    backgroundScope,
                    resources.getDimensionPixelSize(R.dimen.text_select_handle_touch_size)
                )
            setAccessibility()
        }

        // If not, we'll start doing this when we _are_ attached to a visible window
        if (isAttachedToVisibleWindow) {
            startCollectingData()
        }
    }

    private val View.isAttachedToVisibleWindow
        get() = isAttachedToWindow && windowVisibility == VISIBLE

    /**
     * Compute what content is visible from the current position of this View. Generally invoked on
     * position or size changes.
     */
    private fun onZoomChanged() {
        onViewportChanged()
        // Don't fetch new Bitmaps while the user is actively zooming, to avoid jank and rendering
        // churn
        if (positionIsStable) maybeUpdatePageVisibility()
    }

    private fun onViewportChanged() {
        pageLayoutManager?.onViewportChanged(scrollY, height, zoom)
        if (positionIsStable) maybeUpdatePageVisibility()
        accessibilityPageHelper?.invalidateRoot()
        updateSelectionActionModeVisibility()
    }

    /**
     * Shows or hides the selection action mode, as appropriate. If the current selection is visible
     * and a gesture is not in progress, the action mode will be shown. Otherwise, it will be
     * hidden.
     */
    private fun updateSelectionActionModeVisibility() {
        // isFlinging != gestureInProgress. isFlinging refers to the animation that continues after
        // the gesture stops
        if (selectionIsVisible() && !gestureInProgress && !isFling) {
            selectionStateManager?.maybeShowActionMode()
            selectionActionMode?.invalidateContentRect()
        } else {
            selectionStateManager?.maybeHideActionMode()
        }
    }

    private fun selectionIsVisible(): Boolean {
        // If we don't have a selection or any way to understand the layout of our pages, the
        // selection is not visible
        val localSelection = currentSelection ?: return false
        val localPageLayoutManager = pageLayoutManager ?: return false

        val viewport = getVisibleAreaInContentCoords()
        val firstPage = localSelection.bounds.minOf { it.pageNum }
        val lastPage = localSelection.bounds.maxOf { it.pageNum }
        // Top and bottom edge must be on the first and last page, respectively
        // If we can't locate any edge of the selection, we consider it invisible
        val topEdge =
            localSelection.bounds
                .filter { it.pageNum == firstPage }
                .minByOrNull { it.pageRect.top }
                ?.let { localPageLayoutManager.getViewRect(it, viewport) }
                ?.top ?: return false
        val bottomEdge =
            localSelection.bounds
                .filter { it.pageNum == lastPage }
                .maxByOrNull { it.pageRect.bottom }
                ?.let { localPageLayoutManager.getViewRect(it, viewport) }
                ?.bottom ?: return false
        // The left or right edge may be on any page
        val leftEdge =
            localSelection.bounds
                .minByOrNull { it.pageRect.left }
                ?.let { localPageLayoutManager.getViewRect(it, viewport) }
                ?.left ?: return false
        val rightEdge =
            localSelection.bounds
                .maxByOrNull { it.pageRect.right }
                ?.let { localPageLayoutManager.getViewRect(it, viewport) }
                ?.right ?: return false

        return RectF(viewport).intersects(leftEdge, topEdge, rightEdge, bottomEdge)
    }

    private fun reset() {
        // Stop any in progress fling when we open a new document
        scroller.forceFinished(true)
        scrollTo(0, 0)
        pageManager?.cleanup()
        zoom = DEFAULT_INIT_ZOOM
        pageManager = null
        pageLayoutManager = null
        backgroundScope.coroutineContext.cancelChildren()
        stopCollectingData()
    }

    private fun maybeUpdatePageVisibility() {
        val visiblePageAreas =
            pageLayoutManager?.getVisiblePageAreas(visiblePages, getVisibleAreaInContentCoords())
                ?: return
        pageManager?.updatePageVisibilities(visiblePageAreas, zoom, positionIsStable)
    }

    /** React to a page's dimensions being made available */
    private fun onPageDimensionsReceived(pageNum: Int, size: Point) {
        val pageLocation =
            if (visiblePages.contains(pageNum)) {
                pageLayoutManager?.getPageLocation(pageNum, getVisibleAreaInContentCoords())
            } else {
                null
            }
        pageManager?.addPage(pageNum, size, zoom, isFling, pageLocation)
        // Learning the dimensions of a page can change our understanding of the content that's in
        // the viewport
        pageLayoutManager?.onViewportChanged(scrollY, height, zoom)

        // We use scrollY to center content smaller than the viewport. This triggers the initial
        // centering if it's needed. It doesn't override any restored state because we're scrolling
        // to the current scroll position.
        if (pageNum == 0) {
            if (!isInitialZoomDone) {
                this.zoom = getDefaultZoom()
                isInitialZoomDone = true
            }
            scrollTo(scrollX, scrollY)
        }

        val localDeferredPosition = deferredScrollPosition
        val localDeferredPage = deferredScrollPage
        if (localDeferredPosition != null && localDeferredPosition.pageNum <= pageNum) {
            gotoPoint(localDeferredPosition)
            deferredScrollPosition = null
        } else if (localDeferredPage != null && localDeferredPage <= pageNum) {
            gotoPage(pageNum)
            deferredScrollPage = null
        }
    }

    /** Set the zoom, using the given point as a pivot point to zoom in or out of */
    internal fun zoomTo(zoom: Float, pivotX: Float, pivotY: Float) {
        // TODO(b/376299551) - Restore to developer-configured initial zoom value once that API is
        // implemented
        val newZoom = if (Float.NaN.equals(zoom)) DEFAULT_INIT_ZOOM else zoom
        val deltaX = scrollDeltaNeededForZoomChange(this.zoom, newZoom, pivotX, scrollX)
        val deltaY = scrollDeltaNeededForZoomChange(this.zoom, newZoom, pivotY, scrollY)

        this.zoom = newZoom
        scrollBy(deltaX, deltaY)
    }

    private fun scrollDeltaNeededForZoomChange(
        oldZoom: Float,
        newZoom: Float,
        pivot: Float,
        scroll: Int,
    ): Int {
        // Find where the given pivot point would move to when we change the zoom, and return the
        // delta.
        val contentPivot = toContentCoord(pivot, oldZoom, scroll)
        val movedZoomViewPivot: Float = toViewCoord(contentPivot, newZoom, scroll)
        return (movedZoomViewPivot - pivot).toInt()
    }

    /**
     * Computes the part of the content visible within the outer part of this view (including this
     * view's padding) in co-ordinates of the content.
     */
    internal fun getVisibleAreaInContentCoords(): Rect {
        visibleAreaRect.set(
            toContentX(0F).toInt(),
            toContentY(0F).toInt(),
            toContentX(viewportWidth.toFloat() + paddingRight + paddingLeft).toInt(),
            toContentY(viewportHeight.toFloat() + paddingBottom + paddingTop).toInt(),
        )
        return visibleAreaRect
    }

    /**
     * Initializes and sets the accessibility delegate for the PdfView.
     *
     * This method creates an instance of [AccessibilityPageHelper] if both [.pageLayoutManager] and
     * [.pageManager] are initialized, and sets it as the accessibility delegate for the view using
     * [ViewCompat.setAccessibilityDelegate].
     */
    private fun setAccessibility() {
        if (pageLayoutManager != null && pageManager != null) {
            accessibilityPageHelper =
                AccessibilityPageHelper(this, pageLayoutManager!!, pageManager!!)
            ViewCompat.setAccessibilityDelegate(this, accessibilityPageHelper)
        }
    }

    /** The height of the viewport, minus padding */
    private val viewportHeight: Int
        get() = bottom - top - paddingBottom - paddingTop

    /** The width of the viewport, minus padding */
    private val viewportWidth: Int
        get() = right - left - paddingRight - paddingLeft

    /** Converts an X coordinate in View space to an X coordinate in content space */
    internal fun toContentX(viewX: Float): Float {
        return toContentCoord(viewX, zoom, scrollX)
    }

    /** Converts a Y coordinate in View space to a Y coordinate in content space */
    internal fun toContentY(viewY: Float): Float {
        return toContentCoord(viewY, zoom, scrollY)
    }

    /**
     * Converts a one-dimensional coordinate in View space to a one-dimensional coordinate in
     * content space
     */
    private fun toContentCoord(viewCoord: Float, zoom: Float, scroll: Int): Float {
        return (viewCoord + scroll) / zoom
    }

    private val contentWidth: Int
        get() = pageLayoutManager?.paginationModel?.maxWidth ?: 0

    private val contentHeight: Int
        get() = pageLayoutManager?.paginationModel?.totalEstimatedHeight ?: 0

    /** The default [ActionMode.Callback2] for selection */
    public open class DefaultSelectionActionModeCallback(private val pdfView: PdfView) :
        ActionMode.Callback2() {
        @CallSuper
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            pdfView.selectionActionMode = mode

            // Inflate the menu resource providing context menu items.
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.context_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        @CallSuper
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            if (item.itemId == R.id.action_selectAll) {
                // We can't select all if we don't know what page the selection is on, or if
                // we don't know the size of that page
                val page = pdfView.currentSelection?.bounds?.first()?.pageNum ?: return false
                val pageSize = pdfView.pageLayoutManager?.getPageSize(page) ?: return false
                pdfView.selectionStateManager?.selectAllTextOnPageAsync(page, pageSize)
                return true
            } else if (item.itemId == R.id.action_copy) {
                // We can't copy the current selection if no text is selected
                val text = (pdfView.currentSelection as? TextSelection)?.text ?: return false
                copyToClipboard(text)
                pdfView.clearSelection()
                return true
            }
            return false
        }

        private fun copyToClipboard(text: String) {
            val context = pdfView.context
            val manager = context.getSystemService(ClipboardManager::class.java)
            val clip = ClipData.newPlainText(context.getString(R.string.clipboard_label), text)
            manager.setPrimaryClip(clip)
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            // No-op
        }

        override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
            // If we don't know about page layout, defer to the default implementation
            val localPageLayoutManager =
                pdfView.pageLayoutManager ?: return super.onGetContentRect(mode, view, outRect)
            val viewport = pdfView.getVisibleAreaInContentCoords()
            val viewportF = viewport.toRectF()
            val firstSelection = pdfView.currentSelection?.bounds?.first()
            val lastSelection = pdfView.currentSelection?.bounds?.last()

            // Try to position the context menu near the first selection if it's visible
            if (firstSelection != null) {
                // Copy bounds to avoid mutating the real data
                val boundsInView = localPageLayoutManager.getViewRect(firstSelection, viewport)
                if (
                    boundsInView?.let {
                        viewportF.intersects(it.left, it.top, it.right, it.bottom)
                    } == true
                ) {
                    outRect.set(pdfView.toViewRect(boundsInView))
                    return
                }
            }

            // Else, try to position the context menu near the last selection if it's visible
            if (lastSelection != null) {
                // Copy bounds to avoid mutating the real data
                val boundsInView = localPageLayoutManager.getViewRect(lastSelection, viewport)
                if (
                    boundsInView?.let {
                        viewportF.intersects(it.left, it.top, it.right, it.bottom)
                    } == true
                ) {
                    outRect.set(pdfView.toViewRect(boundsInView))
                    return
                }
            }

            // Else, center the context menu in view
            val centerX = (pdfView.x + pdfView.width / 2).roundToInt()
            val centerY = (pdfView.x + pdfView.height / 2).roundToInt()
            outRect.set(centerX, centerY, centerX + 1, centerY + 1)
        }
    }

    private fun toViewRect(contentRect: RectF): Rect {
        return Rect(
            toViewCoord(contentRect.left, zoom, scrollX).roundToInt(),
            toViewCoord(contentRect.top, zoom, scrollY).roundToInt(),
            toViewCoord(contentRect.right, zoom, scrollX).roundToInt(),
            toViewCoord(contentRect.bottom, zoom, scrollY).roundToInt(),
        )
    }

    /** Adjusts the position of [PdfView] in response to gestures detected by [GestureTracker] */
    private inner class ZoomScrollGestureHandler : GestureTracker.GestureHandler() {

        /**
         * The multiplier to convert from a scale gesture's delta span, in pixels, to scale factor.
         *
         * [ScaleGestureDetector] returns scale factors proportional to the ratio of `currentSpan /
         * prevSpan`. This is problematic because it results in scale factors that are very large
         * for small pixel spans, which is particularly problematic for quickScale gestures, where
         * the span pixel values can be small, but the ratio can yield very large scale factors.
         *
         * Instead, we use this to ensure that pinching or quick scale dragging a certain number of
         * pixels always corresponds to a certain change in zoom. The equation that we've found to
         * work well is a delta span of the larger screen dimension should result in a zoom change
         * of 2x.
         */
        private val linearScaleSpanMultiplier: Float =
            2f / maxOf(resources.displayMetrics.heightPixels, resources.displayMetrics.widthPixels)

        /** The maximum scroll distance used to determine if the direction is vertical. */
        private val maxScrollWindow =
            (resources.displayMetrics.density * MAX_SCROLL_WINDOW_DP).toInt()

        /** The smallest scroll distance that can switch mode to "free scrolling". */
        private val minScrollToSwitch =
            (resources.displayMetrics.density * MIN_SCROLL_TO_SWITCH_DP).toInt()

        /** Remember recent scroll events so we can examine the general direction. */
        private val scrollQueue: Queue<PointF> = LinkedList()

        /** Are we correcting vertical scroll for the current gesture? */
        private var straightenCurrentVerticalScroll = true

        private var totalX = 0f
        private var totalY = 0f

        private val totalScrollLength
            // No need for accuracy of correct hypotenuse calculation
            get() = abs(totalX) + abs(totalY)

        override fun onGestureStart() {
            // Stop any in-progress fling when a new gesture begins
            scroller.forceFinished(true)
            selectionStateManager?.maybeHideActionMode()
            gestureInProgress = true
            // We should hide the action mode during a gesture
            updateSelectionActionModeVisibility()
        }

        override fun onGestureEnd(gesture: GestureTracker.Gesture?) {
            // Update page visibility after scroll / zoom gestures end, because we avoid fetching
            // certain data while those gestures are in progress
            if (gesture in ZOOM_OR_SCROLL_GESTURES) maybeUpdatePageVisibility()
            totalX = 0f
            totalY = 0f
            straightenCurrentVerticalScroll = true
            scrollQueue.clear()
            gestureInProgress = false
            // We should reveal the action mode after a gesture
            updateSelectionActionModeVisibility()
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float,
        ): Boolean {
            var dx = Math.round(distanceX)
            val dy = Math.round(distanceY)

            if (straightenCurrentVerticalScroll) {
                // Remember a window of recent scroll events.
                scrollQueue.offer(PointF(distanceX, distanceY))
                totalX += distanceX
                totalY += distanceY

                // Only consider scroll direction for a certain window of scroll events.
                while (totalScrollLength > maxScrollWindow && scrollQueue.size > 1) {
                    // Remove the oldest scroll event - it is too far away to determine scroll
                    // direction.
                    val oldest = scrollQueue.poll()
                    oldest?.let {
                        totalY -= oldest.y
                        totalX -= oldest.x
                    }
                }

                if (
                    totalScrollLength > minScrollToSwitch &&
                        abs((totalY / totalX).toDouble()) < SCROLL_CORRECTION_RATIO
                ) {
                    straightenCurrentVerticalScroll = false
                } else {
                    // Ignore the horizontal component of the scroll.
                    dx = 0
                }
            }

            scrollBy(dx, dy)
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // Assume a fling in a roughly vertical direction was meant to be exactly vertical.
            val myVelocityX =
                if (velocityY / velocityX > SCROLL_CORRECTION_RATIO) {
                    0
                } else {
                    velocityX
                }

            isFling = true
            scroller.fling(
                scrollX,
                scrollY,
                -myVelocityX.toInt(),
                -velocityY.toInt(),
                /* minX= */ minVerticalScrollPosition,
                computeHorizontalScrollRange(),
                minVerticalScrollPosition,
                computeVerticalScrollRange(),
            )

            postInvalidateOnAnimation() // Triggers computeScroll()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            val pageLayoutManager = pageLayoutManager ?: return super.onLongPress(e)
            val touchPoint =
                pageLayoutManager.getPdfPointAt(
                    PointF(toContentX(e.x), toContentY(e.y)),
                    getVisibleAreaInContentCoords()
                ) ?: return super.onLongPress(e)

            selectionStateManager?.maybeSelectWordAtPoint(touchPoint)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val currentZoom = zoom

            val newZoom =
                ZoomUtils.calculateZoomForDoubleTap(
                    viewportWidth,
                    viewportHeight,
                    contentWidth,
                    currentZoom,
                    minZoom,
                    maxZoom,
                )
            if (newZoom == 0f) {
                // viewport not initialized yet maybe?
                return false
            }

            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 200 // Slightly shorter duration for snappier feel
                addUpdateListener { animator ->
                    val animatedValue = animator.animatedValue as Float
                    val value = currentZoom + (newZoom - currentZoom) * animatedValue
                    zoomTo(value, e.x, e.y)
                }
                start()
            }

            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val rawScaleFactor = detector.scaleFactor
            val deltaSpan = abs(detector.currentSpan - detector.previousSpan)
            val scaleDelta = deltaSpan * linearScaleSpanMultiplier
            val linearScaleFactor = if (rawScaleFactor >= 1f) 1f + scaleDelta else 1f - scaleDelta
            val newZoom = (zoom * linearScaleFactor).coerceIn(minZoom, maxZoom)

            zoomTo(newZoom, detector.focusX, detector.focusY)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            selectionStateManager?.clearSelection()
            val pageLayoutManager = pageLayoutManager ?: return super.onSingleTapConfirmed(e)
            val touchPoint =
                pageLayoutManager.getPdfPointAt(
                    PointF(toContentX(e.x), toContentY(e.y)),
                    getVisibleAreaInContentCoords()
                ) ?: return super.onSingleTapConfirmed(e)

            pageManager?.getLinkAtTapPoint(touchPoint)?.let { links ->
                if (handleGotoLinks(links, touchPoint.pagePoint)) return true
                if (handleExternalLinks(links, touchPoint.pagePoint)) return true
            }
            return super.onSingleTapConfirmed(e)
        }

        private fun handleGotoLinks(
            links: PdfDocument.PdfPageLinks,
            pdfCoordinates: PointF
        ): Boolean {
            links.gotoLinks.forEach { gotoLink ->
                if (gotoLink.bounds.any { it.contains(pdfCoordinates.x, pdfCoordinates.y) }) {
                    val destination =
                        PdfPoint(
                            pageNum = gotoLink.destination.pageNumber,
                            pagePoint =
                                PointF(
                                    gotoLink.destination.xCoordinate,
                                    gotoLink.destination.yCoordinate
                                )
                        )

                    scrollToPosition(destination)
                    return true
                }
            }
            return false
        }

        private fun handleExternalLinks(
            links: PdfDocument.PdfPageLinks,
            pdfCoordinates: PointF
        ): Boolean {
            links.externalLinks.forEach { externalLink ->
                if (externalLink.bounds.any { it.contains(pdfCoordinates.x, pdfCoordinates.y) }) {
                    var uri = externalLink.uri
                    linkClickListener?.onLinkClicked(uri)
                        ?: run {
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                    return true
                }
            }
            return false
        }
    }

    public companion object {
        public const val DEFAULT_INIT_ZOOM: Float = 1.0f
        public const val DEFAULT_MAX_ZOOM: Float = 25.0f
        public const val DEFAULT_MIN_ZOOM: Float = 0.5f

        /** The ratio of vertical to horizontal scroll that is assumed to be vertical only */
        private const val SCROLL_CORRECTION_RATIO = 1.5f

        /** The maximum scroll distance used to determine if the direction is vertical */
        private const val MAX_SCROLL_WINDOW_DP = 70

        /** The smallest scroll distance that can switch mode to "free scrolling" */
        private const val MIN_SCROLL_TO_SWITCH_DP = 30

        private const val DEFAULT_PAGE_PREFETCH_RADIUS: Int = 2

        private val ZOOM_OR_SCROLL_GESTURES =
            setOf(
                GestureTracker.Gesture.ZOOM,
                GestureTracker.Gesture.DRAG,
                GestureTracker.Gesture.DRAG_X,
                GestureTracker.Gesture.DRAG_Y
            )

        private fun checkMainThread() {
            check(Looper.myLooper() == Looper.getMainLooper()) {
                "Property must be set on the main thread"
            }
        }

        internal fun toViewCoord(contentCoord: Float, zoom: Float, scroll: Int): Float {
            return (contentCoord * zoom) - scroll
        }
    }
}
