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
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_UNDEFINED
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.os.Parcelable
import android.os.ext.SdkExtensions
import android.util.AttributeSet
import android.util.Range
import android.util.SparseArray
import android.util.TypedValue
import android.view.ActionMode
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.MainThread
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.animation.addListener
import androidx.core.graphics.toRectF
import androidx.core.os.HandlerCompat
import androidx.core.util.Pools
import androidx.core.util.keyIterator
import androidx.core.util.valueIterator
import androidx.core.view.ViewCompat
import androidx.pdf.PdfDocument
import androidx.pdf.PdfPoint
import androidx.pdf.R
import androidx.pdf.content.ExternalLink
import androidx.pdf.event.PdfTrackingEvent
import androidx.pdf.event.RequestFailureEvent
import androidx.pdf.exceptions.RequestFailedException
import androidx.pdf.formfilling.FormFillingEditTextState
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.selection.ContextMenuComponent
import androidx.pdf.selection.Selection
import androidx.pdf.selection.SelectionActionModeCallback
import androidx.pdf.selection.SelectionMenuManager
import androidx.pdf.selection.SelectionRenderer
import androidx.pdf.selection.SelectionStateManager
import androidx.pdf.selection.SelectionUiSignal
import androidx.pdf.selection.model.ImageSelection
import androidx.pdf.util.Accessibility
import androidx.pdf.util.MathUtils
import androidx.pdf.util.ZoomUtils
import androidx.pdf.view.PdfView.Companion.GESTURE_STATE_IDLE
import androidx.pdf.view.PdfView.Companion.GESTURE_STATE_INTERACTING
import androidx.pdf.view.PdfView.Companion.GESTURE_STATE_SETTLING
import androidx.pdf.view.fastscroll.FastScrollCalculator
import androidx.pdf.view.fastscroll.FastScrollDrawer
import androidx.pdf.view.fastscroll.FastScrollGestureDetector
import androidx.pdf.view.fastscroll.FastScroller
import androidx.pdf.view.fastscroll.getDimensions
import androidx.pdf.view.layout.PageLayoutManager
import com.google.android.material.snackbar.Snackbar
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A [View] for presenting PDF content, represented by [PdfDocument].
 *
 * This View supports zooming, scrolling, and flinging. Zooming is supported via pinch gesture,
 * quick scale gesture, and double tap to zoom in or snap back to fitting the page width inside its
 * bounds. Zoom can be changed using the [zoom] property, which is notably distinct from
 * [View.getScaleX] / [View.getScaleY]. Scroll position is based on the [View.getScrollX] /
 * [View.getScrollY] properties.
 *
 * This inherits [ViewGroup] but does not support adding arbitrary children via [addView] or in a
 * layout.
 */
public open class PdfView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    ViewGroup(context, attrs, defStyle) {

    init {
        setWillNotDraw(false)
    }

    public var fastScrollVerticalThumbDrawable: Drawable =
        requireNotNull(context.getDrawable(R.drawable.fast_scroll_thumb_drawable))
        set(value) {
            field = value
            fastScroller?.fastScrollDrawer?.thumbDrawable = value
            invalidate()
        }

    public var fastScrollPageIndicatorBackgroundDrawable: Drawable =
        requireNotNull(context.getDrawable(R.drawable.page_indicator_background))
        set(value) {
            field = value
            fastScroller?.fastScrollDrawer?.pageIndicatorBackground = value
            invalidate()
        }

    public var fastScrollVerticalThumbMarginEnd: Int =
        context.getDimensions(R.dimen.scroll_thumb_margin_end).toInt()
        set(value) {
            field = value
            fastScroller?.fastScrollDrawer?.thumbMarginEnd = value
            invalidate()
        }

    public var fastScrollPageIndicatorMarginEnd: Int =
        context.getDimensions(R.dimen.page_indicator_right_margin).toInt()
        set(value) {
            field = value
            fastScroller?.fastScrollDrawer?.pageIndicatorMarginEnd = value
            invalidate()
        }

    /**
     * The number of pages displayed side-by-side in a single row.
     *
     * This property controls the layout mode and can be set to either 1 or 2.
     * - 1: Displays one page per row (standard display).
     * - 2: Displays two pages per row (like an open book).
     *
     * @throws IllegalArgumentException if the value is not either 1 or 2.
     */
    @IntRange(from = 1, to = 2)
    public var pagesPerRow: Int = 1
        set(value) {
            checkMainThread()
            require(value == SINGLE_PAGE || value == TWO_PAGE) {
                "pagesPerRow must be either 1 or 2."
            }
            if (field == value) return
            field = value
            pageLayoutManager?.let {
                val lastVisiblePage = fullyVisiblePages.lower
                updateLayoutStrategy()
                // Restore scroll position, prioritizing active selection.
                val firstSelectedBound = currentSelection?.bounds?.firstOrNull()
                if (firstSelectedBound != null) {
                    scrollToPage(firstSelectedBound.pageNum)
                    updateSelectionActionModeVisibility()
                } else {
                    scrollToPage(lastVisiblePage)
                }
            }
        }

    /**
     * The spacing between two horizontally adjacent pages in pixels.
     *
     * Note: This value is only relevant when [pagesPerRow] is set to 2.
     */
    @IntRange(from = 0)
    public var horizontalPageSpacing: Int =
        context.resources.getDimensionPixelSize(R.dimen.pdf_horizontal_page_spacing)
        set(value) {
            checkMainThread()
            val validHorizontalPageSpacing = value.coerceAtLeast(0)
            if (field == validHorizontalPageSpacing) return
            field = validHorizontalPageSpacing
            // horizontal page spacing does not affect single page layout strategy.
            if (pagesPerRow == SINGLE_PAGE) return
            updateLayoutStrategy()
        }

    /** The spacing between vertically adjacent pages in pixels. */
    @IntRange(from = 0)
    public var verticalPageSpacing: Int =
        context.resources.getDimensionPixelSize(R.dimen.pdf_vertical_page_spacing)
        set(value) {
            checkMainThread()
            val validVerticalPageSpacing = value.coerceAtLeast(0)
            if (field == validVerticalPageSpacing) return
            field = validVerticalPageSpacing
            updateLayoutStrategy()
        }

    /** Updates the page layout strategy and triggers a viewport refresh. */
    private fun updateLayoutStrategy() {
        pageLayoutManager?.let {
            it.updateLayoutStrategy(
                pagesPerRow,
                horizontalPageSpacing.toFloat(),
                verticalPageSpacing.toFloat(),
            )
            onViewportChanged()
        }
    }

    /**
     * Controls the vertical alignment of a page within the [PdfView].
     *
     * This attribute aligns a page within the view when the page's height is smaller than the
     * view's height. In this state, scrolling and panning are disabled as the entire page is
     * visible. However, zooming in will re-enable scrolling and panning, aligning page to the top.
     *
     * @see VerticalAlignment
     */
    @VerticalAlignment
    public var verticalAlignment: Int = VERTICAL_ALIGNMENT_CENTER
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    /** Enables / Disables the form- filling feature surface. */
    public var isFormFillingEnabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value

            if (value) {
                formWidgetMetadataLoader?.let { loader ->
                    pageManager?.maybeLoadFormWidgetMetadata(loader)
                }
            } else {
                // remove any existing edit text
                formFillingEditText = null
            }
            invalidate()
        }

    /**
     * Enable or disable the image-selection feature surface.
     *
     * **Important:** Enabling this feature (setting to `true`) requires the device to run Android
     * S+ (API Level 31 or above) with SDK Extension version 19 or higher. This requirement is due
     * to dependencies on platform APIs introduced in that sdk-extension.
     */
    @set:RequiresExtension(extension = Build.VERSION_CODES.S, version = 19)
    @get:RequiresExtension(extension = Build.VERSION_CODES.S, version = 19)
    public var isImageSelectionEnabled: Boolean = false
        set(value) {
            if (field == value) return
            val isApiReqSatisfied = isSdkExtensionGreaterEqualTo(19)
            if (!isApiReqSatisfied) return

            field = value
            selectionStateManager?.isImageSelectionEnabled = value
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    /**
     * Used to determine whether a promotional tooltip for form-filling will be displayed or not
     * when the user opens a PDF which is a form and form-filling is enabled.
     */
    public var isFormFillingTooltipEnabled: Boolean = false

    /**
     * The maximum scaling factor that can be applied to this View using the [zoom] property. This
     * value is a multiplier relative to the content's natural size, where '1.0' represents 100%
     * (1x) zoom (similarly '2.5' represents 250% (2.5x) zoom).
     *
     * The value is automatically clamped to stay within the defined range.
     */
    @get:FloatRange(from = MIN_PERMISSIBLE_ZOOM.toDouble(), to = MAX_PERMISSIBLE_ZOOM.toDouble())
    public var maxZoom: Float = MAX_PERMISSIBLE_ZOOM
        set(
            @FloatRange(
                from = MIN_PERMISSIBLE_ZOOM.toDouble(),
                to = MAX_PERMISSIBLE_ZOOM.toDouble(),
            )
            value
        ) {
            field = min(value, MAX_PERMISSIBLE_ZOOM)
        }

    /**
     * The minimum scaling factor that can be applied to this View using the [zoom] property. This
     * value is a multiplier relative to the content's natural size, where '1.0' represents 100%
     * (1x) zoom (similarly '0.5' represents 50% (0.5x) zoom).
     *
     * The value is automatically clamped to stay within the defined range.
     */
    @get:FloatRange(from = MIN_PERMISSIBLE_ZOOM.toDouble(), to = MAX_PERMISSIBLE_ZOOM.toDouble())
    public var minZoom: Float = MIN_PERMISSIBLE_ZOOM
        set(
            @FloatRange(
                from = MIN_PERMISSIBLE_ZOOM.toDouble(),
                to = MAX_PERMISSIBLE_ZOOM.toDouble(),
            )
            value
        ) {
            field = max(value, MIN_PERMISSIBLE_ZOOM)
        }

    // After the pagination model has loaded and the first set of pages are made visible (or if
    // the view is not attached to a window, we fetch all the dimensions to optimize subsequent
    // rendering.
    private var startedFetchingAllDimensions = false

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PdfView)
        if (typedArray.hasValue(R.styleable.PdfView_fastScrollVerticalThumbDrawable)) {
            val drawable =
                typedArray.getDrawable(R.styleable.PdfView_fastScrollVerticalThumbDrawable)
            if (drawable != null) fastScrollVerticalThumbDrawable = drawable
        }
        if (typedArray.hasValue(R.styleable.PdfView_fastScrollPageIndicatorBackgroundDrawable)) {
            val drawable =
                typedArray.getDrawable(
                    R.styleable.PdfView_fastScrollPageIndicatorBackgroundDrawable
                )
            if (drawable != null) fastScrollPageIndicatorBackgroundDrawable = drawable
        }
        if (typedArray.hasValue(R.styleable.PdfView_fastScrollPageIndicatorMarginEnd)) {
            fastScrollPageIndicatorMarginEnd =
                typedArray.getDimensionPixelSize(
                    R.styleable.PdfView_fastScrollPageIndicatorMarginEnd,
                    fastScrollPageIndicatorMarginEnd,
                )
        }
        if (typedArray.hasValue(R.styleable.PdfView_fastScrollVerticalThumbMarginEnd)) {
            fastScrollVerticalThumbMarginEnd =
                typedArray.getDimensionPixelSize(
                    R.styleable.PdfView_fastScrollVerticalThumbMarginEnd,
                    fastScrollVerticalThumbMarginEnd,
                )
        }
        if (typedArray.hasValue(R.styleable.PdfView_pdfHorizontalPageSpacing)) {
            horizontalPageSpacing =
                typedArray.getDimensionPixelSize(
                    R.styleable.PdfView_pdfHorizontalPageSpacing,
                    horizontalPageSpacing,
                )
        }
        if (typedArray.hasValue(R.styleable.PdfView_isFormFillingEnabled)) {
            isFormFillingEnabled =
                typedArray.getBoolean(R.styleable.PdfView_isFormFillingEnabled, false)
        }

        if (
            typedArray.hasValue(R.styleable.PdfView_isImageSelectionEnabled) &&
                isSdkExtensionGreaterEqualTo(19)
        ) {
            isImageSelectionEnabled =
                typedArray.getBoolean(R.styleable.PdfView_isImageSelectionEnabled, false)
        }
        if (typedArray.hasValue(R.styleable.PdfView_minZoom)) {
            minZoom = typedArray.getFloat(R.styleable.PdfView_minZoom, minZoom)
        }
        if (typedArray.hasValue(R.styleable.PdfView_maxZoom)) {
            maxZoom = typedArray.getFloat(R.styleable.PdfView_maxZoom, maxZoom)
        }
        if (typedArray.hasValue(R.styleable.PdfView_pdfPagesPerRow)) {
            pagesPerRow = typedArray.getInt(R.styleable.PdfView_pdfPagesPerRow, SINGLE_PAGE)
        }
        if (typedArray.hasValue(R.styleable.PdfView_verticalAlignment)) {
            verticalAlignment =
                typedArray.getInt(R.styleable.PdfView_verticalAlignment, VERTICAL_ALIGNMENT_CENTER)
        }
        if (typedArray.hasValue(R.styleable.PdfView_pdfVerticalPageSpacing)) {
            verticalPageSpacing =
                typedArray.getDimensionPixelSize(
                    R.styleable.PdfView_pdfVerticalPageSpacing,
                    verticalPageSpacing,
                )
        }
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

    /**
     * The zoom level of this view, as a factor of the content's natural size with when 1 pixel is
     * equal to 1 PDF point. Will always be clamped within ([minZoom], [maxZoom])
     */
    public var zoom: Float = DEFAULT_INIT_ZOOM
        set(value) {
            checkMainThread()
            field = value
            onViewportChanged()
        }

    private var appliedHighlights: List<Highlight> = listOf()
        set(value) {
            checkMainThread()
            val localPageManager =
                pageManager
                    ?: throw IllegalStateException("Can't highlightAreas without PdfDocument")
            localPageManager.setHighlights(value)
        }

    private val visiblePages: Range<Int>
        get() = pageLayoutManager?.visiblePages ?: Range(0, 0)

    private val fullyVisiblePages: Range<Int>
        get() = pageLayoutManager?.fullyVisiblePages ?: Range(0, 0)

    /** The first page in the viewport, including partially-visible pages. 0-indexed. */
    public val firstVisiblePage: Int
        get() = visiblePages.lower

    /** The number of pages visible in the viewport, including partially visible pages */
    public val visiblePagesCount: Int
        get() = if (pdfDocument != null) visiblePages.upper - visiblePages.lower + 1 else 0

    /**
     * The current state of the PDF view with respect to external inputs, e.g. user touch. Returns
     * one of [GESTURE_STATE_IDLE], [GESTURE_STATE_INTERACTING], or [GESTURE_STATE_SETTLING]
     */
    @GestureState
    public var gestureState: Int = GESTURE_STATE_IDLE
        @MainThread private set

    /**
     * A [Pools.Pool] of [Rect] for dispatching to [OnViewportChangedListener] to avoid excessive
     * per-frame allocations
     */
    private val pageLocationsPool = Pools.SimplePool<RectF>(maxPoolSize = 100)

    /**
     * Listener interface to receive callbacks when the PdfView starts and stops being affected by
     * an external input like user touch.
     */
    public interface OnGestureStateChangedListener {
        /**
         * Callback when the PdfView starts and stops being affected by an external input like user
         * touch. [newState] will be one of [GESTURE_STATE_IDLE], [GESTURE_STATE_INTERACTING], or
         * [GESTURE_STATE_SETTLING]
         */
        @MainThread public fun onGestureStateChanged(@GestureState newState: Int)
    }

    private val onGestureStateChangedListeners = mutableListOf<OnGestureStateChangedListener>()

    /**
     * Listener interface to receive changes to the viewport, i.e. the window of visible PDF content
     */
    public interface OnViewportChangedListener {
        /**
         * Called when there has been a change in visible PDF content
         *
         * @param firstVisiblePage the first page that's visible in the View, even if partially so
         * @param visiblePagesCount the number of pages that are visible in the View, including
         *   partially visible pages
         * @param pageLocations the location of each page in View coordinates. At extremely low zoom
         *   levels, only the first 100 page locations will be provided, and the rest can be
         *   obtained from [pdfToViewPoint]. [Rect] instances are recycled; make a copy of any you
         *   wish to make use of beyond the scope of this method.
         * @param zoomLevel the current zoom level
         */
        @MainThread
        public fun onViewportChanged(
            firstVisiblePage: Int,
            visiblePagesCount: Int,
            pageLocations: SparseArray<RectF>,
            zoomLevel: Float,
        )
    }

    private val onViewportChangedListeners = mutableListOf<OnViewportChangedListener>()

    /** Listener interface for handling form edits on a PDF Document. */
    public interface OnFormWidgetInfoUpdatedListener {
        /**
         * Called when a user interacts with a form widget which leads to the change in state of the
         * widget i.e. [FormWidgetInfo]
         *
         * @param formEditInfo The edit to be applied to the [PdfDocument] Note: In order to
         *   correctly update the state the formEditInfo at the document the [formEditInfo] must be
         *   applied to the document via [androidx.pdf.EditablePdfDocument.applyEdit].
         */
        public fun onFormWidgetInfoUpdated(formEditInfo: FormEditInfo)
    }

    private val onFormWidgetInfoUpdatedListeners = mutableListOf<OnFormWidgetInfoUpdatedListener>()

    /**
     * Adds the specified listener to the list of listeners that is notified when any form widget is
     * updated due to an edit action on a widget e.g. click on a radio button.
     *
     * @param listener The listener to add
     */
    public fun addOnFormWidgetInfoUpdatedListener(listener: OnFormWidgetInfoUpdatedListener) {
        onFormWidgetInfoUpdatedListeners.add(listener)
    }

    /**
     * Removes the specified listener from the list of listeners that is notified when any form
     * widget is updated due to an edit action on the widget e.g. click on a radio button.
     *
     * @param listener The listener to remove
     */
    public fun removeOnFormWidgetInfoUpdatedListener(listener: OnFormWidgetInfoUpdatedListener) {
        onFormWidgetInfoUpdatedListeners.remove(listener)
    }

    /** Listener interface for handling clicks on links in a PDF document. */
    public interface LinkClickListener {
        /**
         * Called when a link in the PDF is clicked.
         *
         * @param externalLink The ExternalLink associated with the link.
         * @return True if the link click was handled, false to use the default behavior.
         */
        @MainThread public fun onLinkClicked(externalLink: ExternalLink): Boolean
    }

    /** The listener that is notified when a link in the PDF is clicked. */
    private var linkClickListener: LinkClickListener? = null

    /** The [ActionMode.Callback2] for selection */
    private var selectionActionModeCallback: SelectionActionModeCallback? = null

    /** Interface to customize the set of actions in the selection menu */
    public interface SelectionMenuItemPreparer {
        /**
         * Customize the text selection menu, by adding items to or removing items from
         * [components].
         */
        @MainThread
        public fun onPrepareSelectionMenuItems(components: MutableList<ContextMenuComponent>)
    }

    internal val selectionMenuItemPreparers = mutableListOf<SelectionMenuItemPreparer>()

    /**
     * Adds the specified listener to the chain of [SelectionMenuItemPreparer]. The listener will be
     * invoked in the order they've been added.
     *
     * @param selectionMenuItemPreparer: The [SelectionMenuItemPreparer] to add to the chain.
     * @see removeSelectionMenuItemPreparer
     */
    public fun addSelectionMenuItemPreparer(selectionMenuItemPreparer: SelectionMenuItemPreparer) {
        selectionMenuItemPreparers.add(selectionMenuItemPreparer)
    }

    /**
     * Removes the specified listener from the chain of [SelectionMenuItemPreparer].
     *
     * @param selectionMenuItemPreparer: The [SelectionMenuItemPreparer] to remove from the chain.
     */
    public fun removeSelectionMenuItemPreparer(
        selectionMenuItemPreparer: SelectionMenuItemPreparer
    ) {
        selectionMenuItemPreparers.remove(selectionMenuItemPreparer)
    }

    /**
     * The currently selected PDF content, as [Selection], or 'null' if no content is currently
     * selected.
     */
    public val currentSelection: Selection?
        get() {
            return selectionStateManager?.selectionModel?.value?.documentSelection?.selection
        }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var requestFailedListener: EventListener? = null

    @VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val currentPageIndicatorLabel: String
        get() = fastScroller?.fastScrollDrawer?.currentPageIndicatorLabel ?: ""

    /** Listener interface to receive updates when the [currentSelection] changes */
    public interface OnSelectionChangedListener {
        /** Called when the [Selection] has changed */
        @MainThread public fun onSelectionChanged(newSelection: Selection?)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface EventListener {
        public fun onEvent(event: PdfTrackingEvent)
    }

    private var onSelectionChangedListeners = mutableListOf<OnSelectionChangedListener>()

    /**
     * Listener interface to receive a callback on the UI thread when the content of the PDF
     * document has been loaded for the first time.
     *
     * <p>
     * This callback is invoked on successful document load on current view instance and The state
     * for this listener resets when view recreates (eg. due to configuration changes) or when a new
     * document is set via the [pdfDocument] property. This callback indicates that the document is
     * ready for user interaction and can be used for hiding loading indicators (like progress bars)
     * or for logging initial page load metrics. If a fatal error prevents the content of the PDF
     * from loading (e.g., file corruption), this listener will not be called.
     */
    public fun interface OnFirstContentLoadListener {
        /** Called when the content of the document has been loaded for the first time. */
        @MainThread public fun onFirstContentLoad()
    }

    private val onFirstContentLoadListeners = mutableListOf<OnFirstContentLoadListener>()

    /** Listener interface to receive update when page bitmaps are either fetched or cleared. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface OnBitmapUpdatedListener {
        /** Called when the bitmap has been fetched and is available */
        @MainThread public fun onBitmapFetched(pageNum: Int)

        /** Called when the bitmap has been cleared and is no longer available */
        @MainThread public fun onBitmapCleared(pageNum: Int)
    }

    private var onBitmapUpdatedListener: OnBitmapUpdatedListener? = null

    /**
     * Sets the listener that is notified when the bitmap is updated to the ready state or when the
     * bitmap is cleared for a specified page. Passing null will remove the listener.
     *
     * @param listener The listener to set, or null to clear the listener.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun setOnBitmapUpdatedListener(listener: OnBitmapUpdatedListener?) {
        this.onBitmapUpdatedListener = listener
    }

    /**
     * The [CoroutineScope] used to make suspending calls to [PdfDocument]. The size of the fixed
     * thread pool is arbitrary and subject to tuning.
     */
    internal var backgroundScope: CoroutineScope =
        CoroutineScope(Executors.newFixedThreadPool(5).asCoroutineDispatcher() + SupervisorJob())

    private lateinit var mainDispatcher: CoroutineDispatcher

    internal var pageLayoutManager: PageLayoutManager? = null
        private set

    private var pageManager: PageManager? = null
    private val selectionMenuManager: SelectionMenuManager = SelectionMenuManager(context)
    private var formWidgetInteractionHandler: FormWidgetInteractionHandler? = null
    private var formWidgetMetadataLoader: FormWidgetMetadataLoader? = null
    private var layoutInfoCollector: Job? = null
    private var pageSignalCollector: Job? = null
    private var selectionStateCollector: Job? = null
    private var errorStateCollector: Job? = null
    private var formEditInfoCollector: Job? = null
    private var selectionMenuJob: Job? = null

    private var deferredScrollTarget: DeferredScrollTarget? = null
    private val onScrollDeferred: (DeferredScrollTarget) -> Unit = { deferredScrollTarget = it }

    private var lastOrientation: Int = resources.configuration.orientation

    /** Used to restore saved state */
    private var stateToRestore: PdfViewSavedState? = null
    private var awaitingFirstLayout: Boolean = true
    private var scrollPositionToRestore: PointF? = null
    private var zoomToRestore: Float? = null
    private val errorFlow = MutableSharedFlow<Throwable>()
    private val errorSnackbar: Snackbar by lazy {
        Snackbar.make(
            this,
            context.getString(R.string.error_cannot_open_pdf),
            Snackbar.LENGTH_SHORT,
        )
    }

    /** Used to track is the first page is rendered. */
    private var isFirstPageRendered: Boolean = false

    /**
     * We use this flag to avoid recomputing the visible content during composite zoom+scroll
     * operations until we've applied both zoom *and* scroll
     */
    private var deferViewportUpdate: Boolean = false

    private var formFillingEditText: FormFillingEditText? = null
        set(value) {
            checkMainThread()
            if (field == value) return
            removeView(field?.editText)
            field = value
            addFormFillingEditText()
        }

    private val formFillingEditTextBoundaryWidth: Int =
        resources.getDimensionPixelSize(R.dimen.form_widget_edit_text_boundary_width)

    /**
     * Used to determine whether the form edit state restoration is in progress. If true, we block
     * the interaction with the form widgets to prevent any further edits till the restoration is
     * complete.
     */
    private var isFormEditStateBeingRestored = false

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public enum class FastScrollVisibility {
        AUTO_HIDE,
        ALWAYS_SHOW,
        ALWAYS_HIDE,
    }

    /**
     * Indicates whether the fast scroller's visibility is managed externally.
     *
     * If `true`, the [androidx.pdf.view.PdfView] will not automatically change the visibility of
     * the fast scroller in response to actions like scrolling or zooming.
     *
     * This allows an external source to manage the visibility.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var fastScrollVisibility: FastScrollVisibility = FastScrollVisibility.AUTO_HIDE
        set(value) {
            field = value
            if (value == FastScrollVisibility.ALWAYS_SHOW) fastScroller?.show { postInvalidate() }
            else if (value == FastScrollVisibility.ALWAYS_HIDE) fastScroller?.hide()
        }

    /**
     * Controls whether the fast scroller renders by default. If `false`, [drawFastScroller] must be
     * called explicitly to render it.
     */
    internal var enableDefaultFastScrollerRendering: Boolean = true

    // Stores width set from onSizeChanged or while restoring state
    private var oldWidth: Int? = null

    @VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var fastScroller: FastScroller? = null
    private var fastScrollGestureDetector: FastScrollGestureDetector? = null

    private val gestureHandler = ZoomScrollGestureHandler()
    private val gestureTracker = GestureTracker(context).apply { delegate = gestureHandler }

    private val externalInputManager = PdfViewExternalInputManager(this)

    private val scroller = RelativeScroller(context)
    private val scrollDelegate = PdfViewScroller(this)
    /** Whether we are in a fling movement. This is used to detect the end of that movement */
    private var isFling = false

    private var doubleTapAnimator: ValueAnimator? = null
    internal var lastFastScrollerVisibility: Boolean = false

    private var prevScrollY: Int = 0

    @VisibleForTesting
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var isAutoScrollingEnabled: Boolean = true

    private var isAutoScrolling = false
    private var prevDragEvent: MotionEvent? = null

    @VisibleForTesting internal var notifyFirstContentLoad: Boolean = true

    @VisibleForTesting internal var isAnyBitmapAvailable: Boolean = false

    /**
     * Returns true if neither zoom nor scroll are actively changing. Does not account for
     * externally-driven changes in position (e.g. a animating scrollY or zoom)
     */
    internal val positionIsStable: Boolean
        get() {
            val zoomIsChanging = gestureTracker.matches(GestureTracker.Gesture.ZOOM)
            val scrollIsChanging =
                gestureTracker.matches(
                    GestureTracker.Gesture.DRAG,
                    GestureTracker.Gesture.DRAG_X,
                    GestureTracker.Gesture.DRAG_Y,
                ) ||
                    isFling ||
                    doubleTapAnimator?.isRunning == true ||
                    fastScrollGestureDetector?.trackingFastScrollGesture == true
            return !zoomIsChanging && !scrollIsChanging
        }

    // To avoid allocations during drawing
    private val visibleAreaRect = RectF()

    private val fastScrollGestureHandler =
        object : FastScrollGestureDetector.FastScrollGestureHandler {
            override fun onFastScrollStart() {
                dispatchGestureStateChanged(newState = GESTURE_STATE_INTERACTING)
                // We should hide the action mode during fast scroll.
                updateSelectionActionModeVisibility()
            }

            override fun onFastScrollEnd() {
                dispatchGestureStateChanged(newState = GESTURE_STATE_IDLE)
                // We should reveal the action mode after fast scroll ends.
                updateSelectionActionModeVisibility()
            }

            override fun onFastScrollDetected(eventY: Float) {
                fastScroller?.let {
                    val updatedY =
                        it.viewScrollPositionFromFastScroller(
                            scrollY = eventY,
                            viewHeight = height,
                            estimatedFullHeight = calculateEstimatedFullHeight(),
                            paddingRect = paddingRect,
                        )
                    scrollTo(scrollX, updatedY)
                    invalidate()
                }
            }
        }

    private fun calculateEstimatedFullHeight(): Float = toViewCoord(contentHeight, zoom, scroll = 0)

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun arePagesFullyRendered(): Boolean {
        // If no document is set, there are no pages to render. In that case, we assume all pages
        // are rendered. For testing purposes, the idling resource can be registered before
        // the document is loaded during test setup. Consequently, this method may be called
        // before the document is ready, and it must return true to avoid a timeout;
        // otherwise, the idling resource might never become idle.

        return pageManager?.areAllVisiblePagesFullyRendered(
            visiblePages,
            zoom,
            pageLayoutManager?.visiblePageAreas,
        ) ?: true
    }

    @VisibleForTesting internal var pdfViewAccessibilityManager: PdfViewAccessibilityManager? = null

    internal var isAccessibilityEnabled: Boolean =
        Accessibility.get().isAccessibilityEnabled(context)
        set(value) {
            field = value
            pageManager?.isAccessibilityEnabled = value
        }

    private var accessibilityManager: AccessibilityManager =
        Accessibility.getAccessibilityManager(context)

    internal val accessibilityStateChangeHandler =
        AccessibilityManager.AccessibilityStateChangeListener { isEnabled ->
            isAccessibilityEnabled = isEnabled
            setAccessibility()
        }

    private var selectionStateManager: SelectionStateManager? = null
    private val selectionRenderer = SelectionRenderer(context)

    // True if the zoom was calculated before the layouting completed and needs to be recalculated
    private var pendingZoomRecalculation = false

    /**
     * Selects all text on the specified page asynchronously.
     *
     * @param pageNum The number of the page to select text from.
     */
    internal fun selectAllTextOnPage(pageNum: Int) {
        selectionStateManager?.selectAllTextOnPageAsync(pageNum)
    }

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
        scrollDelegate.scrollToPage(pageNum, onScrollDeferred)
    }

    /**
     * Scrolls to [position], aligns vertically to center, optionally animating the scroll
     *
     * This View cannot scroll to a page until it knows its dimensions. If [position] is distant
     * from the currently-visible page in a large PDF, there may be some delay while dimensions are
     * being loaded from the PDF.
     */
    @Suppress("UNUSED_PARAMETER")
    public fun scrollToPosition(position: PdfPoint) {
        checkMainThread()
        scrollDelegate.scrollToPosition(position, ScrollAlignment.CENTRE, onScrollDeferred)
    }

    /**
     * Scrolls to [position], optionally animating the scroll
     *
     * This View cannot scroll to a page until it knows its dimensions. If [position] is distant
     * from the currently-visible page in a large PDF, there may be some delay while dimensions are
     * being loaded from the PDF.
     *
     * @param alignment The vertical alignment of the scroll position.
     */
    internal fun scrollToPosition(position: PdfPoint, @ScrollAlignmentDef alignment: Int) {
        scrollDelegate.scrollToPosition(position, alignment, onScrollDeferred)
    }

    /**
     * Registers [listener] as the callback to be invoked when an external link in the PDF is
     * clicked. Supply `null` to clear any current listener.
     */
    public fun setLinkClickListener(listener: LinkClickListener?) {
        linkClickListener = listener
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

    /**
     * Adds the specified listener to the list of listeners that will be notified on first content
     * load.
     *
     * @param listener The listener to add.
     */
    public fun addOnFirstContentLoadListener(listener: OnFirstContentLoadListener) {
        onFirstContentLoadListeners.add(listener)
    }

    /**
     * Removes the specified listener from the list of listeners that will be notified on first
     * content load.
     *
     * @param listener The listener to remove.
     */
    public fun removeOnFirstContentLoadListener(listener: OnFirstContentLoadListener) {
        onFirstContentLoadListeners.remove(listener)
    }

    /**
     * Adds the specified listener to the list of listeners that will be notified of changes in
     * state with respect to this PdfView being affected by an external input, e.g. user touch.
     *
     * @param listener listener to notify when interaction state change events occur
     * @see removeOnGestureStateChangedListener
     */
    public fun addOnGestureStateChangedListener(listener: OnGestureStateChangedListener) {
        onGestureStateChangedListeners.add(listener)
    }

    /**
     * Removes the specified listener from the list of listeners that will be notified of changes in
     * state with respect to this PdfView being affected by an external input, e.g. user touch.
     *
     * @param listener listener to remove
     */
    public fun removeOnGestureStateChangedListener(listener: OnGestureStateChangedListener) {
        onGestureStateChangedListeners.remove(listener)
    }

    /**
     * Fast scroll gestures and corresponding state changes are handled by separate logic than most
     * other gesture events. This method dispatches state changes except during active fast scroll,
     * i.e. to avoid noise from spurious non-fast-scroll gestures detected during a fast scroll
     * sequence.
     */
    private fun dispatchGestureStateChangedUnlessFastScroll(@GestureState newState: Int) {
        if (fastScrollGestureDetector?.trackingFastScrollGesture == false) {
            dispatchGestureStateChanged(newState)
        }
    }

    private fun dispatchGestureStateChanged(@GestureState newState: Int) {
        require(newState in VALID_GESTURE_STATES) {
            "Invalid state change from $gestureState to $newState"
        }
        if (newState == gestureState) {
            return
        }
        gestureState = newState
        for (listener in onGestureStateChangedListeners) {
            listener.onGestureStateChanged(newState)
        }
    }

    /**
     * Applies a set of [Highlight] to be drawn over this PDF. Each [Highlight] may be a different
     * color. This overrides any previous highlights, there is no merging of new and previous
     * values. [highlights] are defensively copied and the list or its contents may be modified
     * after providing it here.
     */
    public fun setHighlights(highlights: List<Highlight>) {
        this.appliedHighlights = ArrayList(highlights.map { highlight -> highlight.copy() })
    }

    private fun dispatchSelectionChanged(new: Selection?) {
        for (listener in onSelectionChangedListeners) {
            listener.onSelectionChanged(new)
        }
    }

    /**
     * Adds the specified listener to the list of listeners that will be notified of viewport change
     * events.
     *
     * @param listener listener to add
     */
    public fun addOnViewportChangedListener(listener: OnViewportChangedListener) {
        onViewportChangedListeners.add(listener)
    }

    /**
     * Removes the specified listener from the list of listeners that will be notified of viewport
     * change events.
     *
     * @param listener listener to remove
     */
    public fun removeOnViewportChangedListener(listener: OnViewportChangedListener) {
        onViewportChangedListeners.remove(listener)
    }

    /**
     * Returns the [PdfPoint] corresponding to [viewPoint] in View coordinates, or null if no PDF
     * content has been laid out at [viewPoint]
     */
    public fun viewToPdfPoint(viewPoint: PointF): PdfPoint? {
        return viewToPdfPoint(viewPoint.x, viewPoint.y)
    }

    /**
     * Returns the [PdfPoint] corresponding to ([x], [y])in View coordinates, or null if no PDF
     * content has been laid out at that point.
     */
    public fun viewToPdfPoint(x: Float, y: Float): PdfPoint? {
        return pageLayoutManager?.getPdfPointAt(
            toContentX(x),
            toContentY(y),
            getVisibleAreaInContentCoords(),
            scanAllPages = true,
        )
    }

    /**
     * Returns the View coordinate location of [pdfPoint], or null if that PDF content has not been
     * laid out yet.
     */
    public fun pdfToViewPoint(pdfPoint: PdfPoint): PointF? {
        return pdfToViewPoint(pdfPoint, accountForScroll = true)
    }

    /**
     * Returns the View coordinate location of [pdfPoint], or null if that PDF content has not been
     * laid out yet.
     *
     * @param accountForScroll true to offset the final position by ([scrollX], [scrollY])
     */
    private fun pdfToViewPoint(pdfPoint: PdfPoint, accountForScroll: Boolean): PointF? {
        val pageLocation =
            pageLayoutManager?.getPageLocation(pdfPoint.pageNum, getVisibleAreaInContentCoords())
                ?: return null
        val ret =
            PointF(
                toViewCoord(
                    pageLocation.left + pdfPoint.x,
                    zoom,
                    scroll = if (accountForScroll) scrollX else 0,
                ),
                toViewCoord(
                    pageLocation.top + pdfPoint.y,
                    zoom,
                    scroll = if (accountForScroll) scrollY else 0,
                ),
            )
        return ret
    }

    /** Clears the current selection, if one exists. No-op if there is no current [Selection] */
    public fun clearCurrentSelection() {
        selectionStateManager?.clearCurrentSelection()
    }

    override fun dispatchHoverEvent(event: MotionEvent?): Boolean {
        return event?.let { pdfViewAccessibilityManager?.dispatchHoverEvent(it) } == true ||
            super.dispatchHoverEvent(event)
    }

    /**
     * Prioritizes standard keyboard shortcuts over accessibility-specific handling of certain keys.
     * This ensures a consistent experience for all users, where shortcuts like D-pad scrolling work
     * irrespective of whether accessibility is enabled or not, as both [externalInputManager] and
     * [pdfViewAccessibilityManager] are capable of handling certain key events.
     */
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.dispatchKeyEvent(event)

        // Accessibility services can register to filter key events and handle their own shortcuts
        // before the event reaches the application. If a key event is received here, it means the
        // accessibility service has chosen to not handle it. Therefore, we can safely let the
        // ExternalInputManager handle the key event.
        return (externalInputManager.handleKeyEvent(event)) ||
            pdfViewAccessibilityManager?.dispatchKeyEvent(event) ?: false ||
            super.dispatchKeyEvent(event)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        pdfViewAccessibilityManager?.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val localPageLayoutManager = pageLayoutManager ?: return
        canvas.save()
        if (clipToPadding) {
            canvas.clipRect(
                scrollX + paddingLeft.toFloat(),
                scrollY + paddingTop.toFloat(),
                scrollX + (width.toFloat() - paddingRight),
                scrollY + (height.toFloat() - paddingBottom),
            )
        }
        // View itself translates the Canvas by scroll position, so we don't have to, but
        // we need to adjust the translation for padding since it's not automatically accounted for.
        canvas.translate(paddingLeft.toFloat(), paddingTop.toFloat())
        canvas.scale(zoom, zoom)
        val selectionModel = selectionStateManager?.selectionModel
        for (i in visiblePages.lower..visiblePages.upper) {
            // Scroll and zoom are applied to the Canvas, so we draw to the Canvas using content
            // coordinates
            val pageLoc = localPageLayoutManager.getPageLocation(i, getVisibleAreaInContentCoords())
            pageManager?.drawPage(i, canvas, pageLoc)
            selectionModel?.value?.let {
                selectionRenderer.drawSelectionOnPage(
                    model = it,
                    pageNum = i,
                    canvas,
                    pageLoc,
                    zoom,
                )
            }

            if (isAnyBitmapAvailable && notifyFirstContentLoad) {
                post { onFirstContentLoadListeners.forEach { it.onFirstContentLoad() } }
                notifyFirstContentLoad = false
                isFirstPageRendered = true
            }
        }
        canvas.restore()

        // Fast scroller is non-content and shouldn't be affected by zoom. It's drawn after
        // restoring the Canvas to its unscaled state
        if (enableDefaultFastScrollerRendering) {
            drawFastScroller(canvas, Point(scrollX, scrollY))
        }
    }

    /** Draws the fast scroller UI in view coordinates. */
    internal fun drawFastScroller(canvas: Canvas, scrollOffset: Point = Point(0, 0)) {
        canvas.save()
        // Adjust the canvas based on current scroll position to draw fast scroller in view
        // coordinates.
        canvas.translate(scrollOffset.x.toFloat(), scrollOffset.y + paddingTop.toFloat())

        val documentPageCount = pdfDocument?.pageCount ?: 0
        if (documentPageCount > 1) {
            fastScroller?.drawScroller(
                canvas = canvas,
                scrollY = scrollY,
                viewWidth = width,
                viewHeight = height,
                visiblePages = fullyVisiblePages,
                estimatedFullHeight = calculateEstimatedFullHeight(),
                paddingRect = paddingRect,
            )
        }
        canvas.restore()
    }

    private val paddingRect: Rect
        get() = Rect(paddingLeft, paddingTop, paddingRight, paddingBottom)

    private val onPdfContentInvalidatedListener =
        object : PdfDocument.OnPdfContentInvalidatedListener {
            override fun onPdfContentInvalidated(pageNumber: Int, dirtyAreas: List<Rect>) {
                val localPageLayoutManager = pageLayoutManager ?: return
                pageManager?.maybeInvalidateAreas(
                    pageNum = pageNumber,
                    visibleArea = localPageLayoutManager.visiblePageAreas[pageNumber],
                    currentZoom = zoom,
                    areasToUpdate = dirtyAreas.map { it.toRectF() },
                )
                formWidgetMetadataLoader?.let { loader ->
                    pageManager?.maybeUpdateFormWidgetMetadata(pageNumber, loader)
                }
            }
        }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        return event?.let { externalInputManager.handleMouseEvent(event) } ?: false ||
            super.onGenericMotionEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Needs to be set so that any ancestor of PdfView does not consume the touch event before
        // the library does. This particularly creates an issue with zoom and scroll when integrated
        // with the ViewPager library.
        parent?.requestDisallowInterceptTouchEvent(true)

        var handled =
            event?.let { fastScrollGestureDetector?.handleEvent(it, parent, width, paddingTop) }
                ?: false
        handled = handled || event?.let { externalInputManager.handleMouseEvent(event) } ?: false
        handled = handled || maybeDragSelection(event)
        handled =
            handled ||
                event?.let { gestureTracker.feed(it, parent, isContentAtHorizontalEdges()) }
                    ?: false

        if (!handled) {
            parent?.requestDisallowInterceptTouchEvent(false)
        }

        return handled || super.onTouchEvent(event)
    }

    override fun addView(child: View?) {
        throw UnsupportedOperationException("PdfView does not accept children.")
    }

    override fun addView(child: View?, index: Int) {
        throw UnsupportedOperationException("PdfView does not accept children.")
    }

    override fun addView(child: View?, width: Int, height: Int) {
        throw UnsupportedOperationException("PdfView does not accept children.")
    }

    override fun addView(child: View?, params: LayoutParams?) {
        throw UnsupportedOperationException("PdfView does not accept children.")
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        throw UnsupportedOperationException("PdfView does not accept children.")
    }

    private fun isContentAtHorizontalEdges(): Boolean {
        val leftContentEdgePx = -scrollX
        val rightContentEdgePx =
            toViewCoord(contentWidth, zoom, scrollX).toInt() - paddingRight - paddingLeft

        return leftContentEdgePx == 0 || rightContentEdgePx == viewportWidth
    }

    private fun maybeShowFastScroller() {
        // Forced visibility takes precedence
        if (fastScrollVisibility != FastScrollVisibility.AUTO_HIDE) return

        fastScroller?.show { postInvalidate() }
    }

    private fun maybeHideFastScroller() {
        // Forced visibility takes precedence
        if (fastScrollVisibility != FastScrollVisibility.AUTO_HIDE) return

        fastScroller?.hide()
    }

    internal fun maybeDragSelection(event: MotionEvent?, isSourceMouse: Boolean = false): Boolean {
        if (event == null) return false
        val touchPoint =
            pageLayoutManager?.getPdfPointAt(
                toContentX(event.x),
                toContentY(event.y),
                getVisibleAreaInContentCoords(),
            )

        if (event.action == MotionEvent.ACTION_UP) {
            isAutoScrolling = false
            parent?.requestDisallowInterceptTouchEvent(false)
        }

        prevDragEvent = event

        if (
            selectionStateManager?.maybeDragSelection(
                event.action,
                touchPoint,
                zoom,
                isSourceMouse,
            ) == true
        ) {
            val shouldAutoScroll = event.action == MotionEvent.ACTION_MOVE && !isAutoScrolling
            if (isAutoScrollingEnabled && shouldAutoScroll) {
                startAutoScrolling()
            }
            return true
        }
        return false
    }

    private fun startAutoScrolling() {
        isAutoScrolling = true
        handler?.post(
            object : Runnable {
                override fun run() {
                    if (isAutoScrolling) {
                        // Perform the scroll.
                        scrollAsYouSelect()
                        handler?.postDelayed(
                            this,
                            AUTO_SCROLL_DELAY_IN_MILLIS,
                        ) // Adjust delay for smoother/faster scroll
                    }
                }
            }
        )
    }

    private fun scrollAsYouSelect() {
        prevDragEvent?.let { event ->
            if (event.y > height * SCROLL_SELECTION_TOLERANCE_RATIO) {
                scrollBy(0, AUTO_SCROLL_BY_VALUE)
            } else if (event.y < height * (1 - SCROLL_SELECTION_TOLERANCE_RATIO)) {
                scrollBy(0, -AUTO_SCROLL_BY_VALUE)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        /**
         * For activities which doesn't recreate upon orientation changes, restore path for
         * [androidx.pdf.view.PdfView] will not kick-in. We need to manually store the current
         * scroll position which then will be restored in [onLayout].
         */
        if (newConfig?.orientation != lastOrientation) {
            val contentCenterX = toContentX(paddingLeft + viewportWidth.toFloat() / 2f)
            // Keep scroll at top if previously at top.
            val contentCenterY =
                if (scrollY <= 0) 0F else toContentY(paddingTop + viewportHeight.toFloat() / 2f)
            scrollPositionToRestore = PointF(contentCenterX, contentCenterY)

            lastOrientation = newConfig?.orientation ?: ORIENTATION_UNDEFINED
        }
        setupFastScroller()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Ignore oldw if we're just added to view hierarchy.
        if (oldw != 0) oldWidth = oldw
        onViewportChanged()
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        // TODO(b/390003204): Prevent showing of the scrubber when the document only been
        //  translated on the x-axis
        if (t != oldt) {
            maybeShowFastScroller()
        }
        manageActionModeOnScroll()
        onViewportChanged()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (pendingZoomRecalculation) {
            this.zoom = getFitToWidthZoom()
            pendingZoomRecalculation = false
        }

        if (changed || awaitingFirstLayout) maybeAdjustZoomAndScroll()

        awaitingFirstLayout = false
        // As view dimensions are finalized we need to update the action mode visibility if needed.
        updateSelectionActionModeVisibility()
        layoutFormFillingEditTextIfPresent()
    }

    private fun layoutFormFillingEditTextIfPresent() {
        formFillingEditText?.let {
            val widgetRect = it.formWidget.widgetRect
            val topLeftCorner =
                pdfToViewPoint(
                    PdfPoint(it.pageNum, widgetRect.left.toFloat(), widgetRect.top.toFloat()),
                    accountForScroll = false,
                )
            val bottomRightCorner =
                pdfToViewPoint(
                    PdfPoint(it.pageNum, widgetRect.right.toFloat(), widgetRect.bottom.toFloat()),
                    accountForScroll = false,
                )
            if (topLeftCorner == null || bottomRightCorner == null) {
                removeView(it.editText)
                formFillingEditText = null
                return
            }
            it.editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, it.fontSize * zoom)
            it.editText.layout(
                topLeftCorner.x.roundToInt() - formFillingEditTextBoundaryWidth,
                topLeftCorner.y.roundToInt() - formFillingEditTextBoundaryWidth,
                bottomRightCorner.x.roundToInt() + formFillingEditTextBoundaryWidth,
                bottomRightCorner.y.roundToInt() + formFillingEditTextBoundaryWidth,
            )
        }
    }

    private fun maybeAdjustZoomAndScroll() {
        val localScrollPosition = scrollPositionToRestore
        val localOldWidth = oldWidth
        /**
         * We only want to adjust zoom if we're restoring from a saved state or PdfView's size has
         * changed, i.e. we'll have a valid [oldWidth] to use.
         *
         * For view init scenario, [getFitToWidthZoom] should be enough to fit to width.
         */
        if (localOldWidth != null) {
            // Either we're restoring or view size has changed; adjust zoom by factor of w / oldW.
            val factor = width.toFloat() / localOldWidth
            val resolvedZoom = zoomToRestore ?: zoom

            // Calculate new zoom, clamped between min and max zoom possible.
            val newZoom = (resolvedZoom * factor).coerceIn(minZoom, maxZoom)
            this.zoom = newZoom
            zoomToRestore = null

            /**
             * If view isn't recreated, we won't have a scroll position from bundle. In this case,
             * adjust view's current scrollX and scrollY according to change in zoom.
             */
            if (localScrollPosition == null) {
                val newScrollX = (scrollX * (newZoom / resolvedZoom)).roundToInt()
                val newScrollY = (scrollY * (newZoom / resolvedZoom)).roundToInt()
                scrollTo(newScrollX, newScrollY)
            }
        }

        // The view is recreated, and we have a position to restore from bundle
        if (localScrollPosition != null) {
            scrollToRestoredPosition(localScrollPosition, this.zoom)
            scrollPositionToRestore = null
        }

        oldWidth = null
    }

    private fun getFormFillingEditTextState(): FormFillingEditTextState? {
        return formFillingEditText?.let {
            FormFillingEditTextState(it.editText.text.toString(), it.pageNum, it.formWidget)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        stopCollectingData()
        awaitingFirstLayout = true
        mainDispatcher = HandlerCompat.createAsync(handler.looper).asCoroutineDispatcher()

        accessibilityManager.addAccessibilityStateChangeListener(accessibilityStateChangeHandler)
        // PageManager is being reset on onDetachToWindow we should make sure we set it back.
        maybeUpdatePageVisibility()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            startCollectingData()
        } else {
            stopCollectingData()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopCollectingData()
        onSelectionUiSignal(SelectionUiSignal.ToggleActionMode(show = false))
        awaitingFirstLayout = true
        pageManager?.cleanup()

        pdfDocument?.removeOnPdfContentInvalidatedListener(onPdfContentInvalidatedListener)
        accessibilityManager.removeAccessibilityStateChangeListener(accessibilityStateChangeHandler)
        removeCallbacks(showSelectionActionModeRunnable)
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val state = PdfViewSavedState(superState)
        state.zoom = zoom
        state.viewWidth = width
        state.contentCenterX = toContentX(paddingLeft + viewportWidth.toFloat() / 2f)
        state.contentCenterY = toContentY(paddingTop + viewportHeight.toFloat() / 2f)
        // Keep scroll at top if previously at top.
        if (scrollY <= 0) {
            state.contentCenterY = 0F
        }
        state.isFormFillingEnabled = isFormFillingEnabled
        state.isFormFillingTooltipEnabled = isFormFillingTooltipEnabled
        if (isSdkExtensionGreaterEqualTo(19)) {
            state.isImageSelectionEnabled = isImageSelectionEnabled
        }
        state.pagesPerRow = pagesPerRow
        state.horizontalPageSpacing = horizontalPageSpacing
        state.verticalPageSpacing = verticalPageSpacing
        state.documentUri = pdfDocument?.uri
        state.paginationModel = pageLayoutManager?.paginationModel
        state.layoutStrategy = pageLayoutManager?.layoutStrategy
        state.pdfFormFillingState = pageLayoutManager?.pdfFormFillingState
        state.selectionModel = selectionStateManager?.selectionModel?.value
        state.pdfFormFillingEditTextState = getFormFillingEditTextState()

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
            dispatchGestureStateChangedUnlessFastScroll(newState = GESTURE_STATE_IDLE)
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

    /**
     * Manages the visibility of the selection action mode during scrolling. The action mode is
     * immediately hidden when scrolling starts and a delayed runnable is posted to potentially show
     * it again after scrolling has settled.
     */
    private fun manageActionModeOnScroll() {
        // Immediately hide the action mode as soon as scrolling begins.
        hideActionMode()
        // Always remove any pending show runnables. This prevents the action mode
        // from flickering or reappearing during continuous scrolling.
        removeCallbacks(showSelectionActionModeRunnable)
        // Post a runnable to potentially show the action mode after a delay.
        // This ensures the action mode only reappears after scrolling has settled.
        postDelayed(showSelectionActionModeRunnable, ACTION_MODE_REAPPEAR_DELAY_MS)
    }

    override fun computeHorizontalScrollRange(): Int {
        // Note we provide scroll = 0 here, as we shouldn't consider the current scroll position
        // to compute the maximum scroll position. Scroll position is absolute, not relative
        val contentWidthPx = toViewCoord(contentWidth, zoom, scroll = 0)
        return if (contentWidthPx < width) 0 else (contentWidthPx - width).roundToInt()
    }

    private val minVerticalScrollPosition: Int
        get() {
            // Note we provide scroll = 0 here, as we shouldn't consider the current scroll position
            // to compute the maximum scroll position. Scroll position is absolute, not relative
            val contentHeightPx = toViewCoord(contentHeight, zoom, scroll = 0)
            return if (verticalAlignment == VERTICAL_ALIGNMENT_TOP || contentHeightPx > height) {
                0
            } else {
                // Center vertically
                -(height - contentHeightPx).roundToInt() / 2
            }
        }

    override fun computeVerticalScrollRange(): Int {
        // Note we provide scroll = 0 here, as we shouldn't consider the current scroll position
        // to compute the maximum scroll position. Scroll position is absolute, not relative
        val contentHeightPx =
            toViewCoord(contentHeight, zoom, scroll = 0) + paddingTop + paddingBottom
        return if (contentHeightPx < height && verticalAlignment == VERTICAL_ALIGNMENT_TOP) {
            0
        } else if (contentHeightPx < height) {
            // Center vertically
            -(height - contentHeightPx).roundToInt() / 2
        } else {
            (contentHeightPx - height).roundToInt()
        }
    }

    internal fun getFitToWidthZoom(): Float {
        if (contentWidth == 0f || viewportWidth <= 0) {
            if (awaitingFirstLayout) pendingZoomRecalculation = true
            return DEFAULT_INIT_ZOOM
        }
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
        // Restore layout properties
        pagesPerRow = localStateToRestore.pagesPerRow
        horizontalPageSpacing = localStateToRestore.horizontalPageSpacing
        verticalPageSpacing = localStateToRestore.verticalPageSpacing

        pageLayoutManager =
            PageLayoutManager(
                    localPdfDocument,
                    backgroundScope,
                    topPageMarginPx = context.getDimensions(R.dimen.top_page_margin),
                    pagesPerRow = pagesPerRow,
                    horizontalPageSpacingPx = horizontalPageSpacing.toFloat(),
                    verticalPageSpacingPx = verticalPageSpacing.toFloat(),
                    paginationModel = requireNotNull(localStateToRestore.paginationModel),
                    layoutStrategy = requireNotNull(localStateToRestore.layoutStrategy),
                    pdfFormFillingState = requireNotNull(localStateToRestore.pdfFormFillingState),
                    errorFlow = errorFlow,
                    isFormFillingEnabled = { isFormFillingEnabled },
                )
                .apply { onViewportChanged() }
        selectionStateManager =
            SelectionStateManager(
                pdfDocument = localPdfDocument,
                backgroundScope = backgroundScope,
                handleTouchTargetSizePx =
                    resources.getDimensionPixelSize(R.dimen.text_select_handle_touch_size),
                errorFlow = errorFlow,
                pageLayoutManager = pageLayoutManager,
                pageManager = pageManager,
                initialSelection = localStateToRestore.selectionModel,
                isImageSelectionEnabled = localStateToRestore.isImageSelectionEnabled,
            )

        val positionToRestore =
            PointF(localStateToRestore.contentCenterX, localStateToRestore.contentCenterY)

        if (awaitingFirstLayout) {
            scrollPositionToRestore = positionToRestore
            zoomToRestore = localStateToRestore.zoom
            oldWidth = localStateToRestore.viewWidth
        } else {
            scrollToRestoredPosition(positionToRestore, localStateToRestore.zoom)
        }

        isFormFillingEnabled = localStateToRestore.isFormFillingEnabled
        isFormFillingTooltipEnabled = localStateToRestore.isFormFillingTooltipEnabled
        if (isSdkExtensionGreaterEqualTo(19)) {
            isImageSelectionEnabled = localStateToRestore.isImageSelectionEnabled
        }
        setAccessibility()

        restoreFormFillingEditText()

        stateToRestore = null
        return true
    }

    private fun restoreFormFillingEditText() {
        val localStateToRestore = stateToRestore ?: return
        val formFillingEditTextState = localStateToRestore.pdfFormFillingEditTextState
        if (formFillingEditTextState != null) {
            formWidgetInteractionHandler?.handleInteractionWithTextWidget(
                formFillingEditTextState.pageNumber,
                formFillingEditTextState.formWidgetInfo!!,
                formFillingEditTextState.currentText,
            )
        }
    }

    private fun scrollToRestoredPosition(position: PointF, zoom: Float) {
        this.zoom = zoom
        val scrollX = (position.x * zoom - viewportWidth / 2f).roundToInt()
        val scrollY = (position.y * zoom - viewportHeight / 2f).roundToInt()
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
        val mainScope = CoroutineScope(mainDispatcher)
        pageLayoutManager?.let { manager ->
            // Don't let two copies of this run concurrently
            val layoutInfoToJoin = layoutInfoCollector?.apply { cancel() }
            layoutInfoCollector =
                mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    // Prevent 2 copies from running concurrently
                    layoutInfoToJoin?.join()
                    launch { manager.pageInfos.collect { onPageMetaDataReceived(it) } }
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
                        manager.bitmapUpdatedFlow.collect { pageBitmapState ->
                            when (pageBitmapState) {
                                is PageBitmapState.PageBitmapReady -> {
                                    onBitmapUpdatedListener?.onBitmapFetched(
                                        pageBitmapState.pageNum
                                    )
                                    isAnyBitmapAvailable = true
                                }
                                is PageBitmapState.PageBitmapCleared -> {
                                    onBitmapUpdatedListener?.onBitmapCleared(
                                        pageBitmapState.pageNum
                                    )
                                }
                            }
                        }
                    }

                    launch {
                        manager.pageTextReadyFlow.collect { pageNum ->
                            pdfViewAccessibilityManager?.onPageTextReady(pageNum)
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
                    launch {
                        manager.selectionModel.collect { newModel ->
                            dispatchSelectionChanged(newModel?.documentSelection?.selection)
                        }
                    }
                }
        }

        formWidgetInteractionHandler?.let { handler ->
            val formEditActionToJoin = formEditInfoCollector?.apply { cancel() }
            formEditInfoCollector =
                mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    formEditActionToJoin?.join()
                    launch {
                        handler.formWidgetUpdates.collect {
                            onFormWidgetInfoUpdatedListeners.forEach { listener ->
                                listener.onFormWidgetInfoUpdated(it)
                            }
                        }
                    }
                }
        }

        val errorsToJoin = errorStateCollector?.apply { cancel() }
        errorStateCollector =
            mainScope.launch(start = CoroutineStart.UNDISPATCHED) {
                // Prevent 2 copies from running concurrently
                errorsToJoin?.join()
                errorFlow.collect { error ->
                    val localError =
                        if (error is RequestFailedException)
                            error.copy(isFirstPageRendered = isFirstPageRendered)
                        else error

                    if (localError is RequestFailedException && localError.showError)
                        showErrorInSnackbar(localError)

                    requestFailedListener?.onEvent(RequestFailureEvent(localError))
                }
            }
    }

    private fun stopCollectingData() {
        layoutInfoCollector?.cancel()
        pageSignalCollector?.cancel()
        selectionStateCollector?.cancel()
        formEditInfoCollector?.cancel()
        errorStateCollector?.cancel()
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
                if (signal.show && currentSelection != null) {
                    showActionMode()
                } else {
                    hideActionMode()
                }
            }
        }
    }

    private fun hideActionMode() {
        selectionMenuJob?.cancel()
        selectionActionModeCallback?.close()
    }

    private fun showActionMode() {
        val localCurrentSelection = currentSelection ?: return
        // Populate the menu for non-image selections if the menu is currently empty
        if (
            currentSelection !is ImageSelection && selectionActionModeCallback?.actionMode == null
        ) {
            val previousJob = selectionMenuJob
            selectionMenuJob =
                backgroundScope.launch {
                    previousJob?.cancelAndJoin()
                    val menuItems =
                        selectionMenuManager.getSelectionMenuItems(localCurrentSelection)
                    selectionActionModeCallback =
                        SelectionActionModeCallback(this@PdfView, menuItems)
                    withContext(mainDispatcher) {
                        startActionMode(selectionActionModeCallback, ActionMode.TYPE_FLOATING)
                    }
                }
        }
    }

    private fun showErrorInSnackbar(error: Throwable) {
        // prevent multiple, rapid error indicators from being displayed to the user in quick
        // succession
        if (errorSnackbar.isShown) return

        val errorMsg =
            when (error) {
                // TODO(b/404836992): Fix strings after confirmation from UXW
                is RequestFailedException -> context.getString(R.string.error_cannot_open_pdf)
                else -> context.getString(R.string.error_cannot_open_pdf)
            }
        errorSnackbar.setText(errorMsg)
        errorSnackbar.show()
    }

    private fun setupFastScroller() {
        val localPdfDocument = pdfDocument ?: return

        val fastScrollCalculator = FastScrollCalculator(context)
        val fastScrollDrawer =
            FastScrollDrawer(
                context,
                localPdfDocument,
                fastScrollVerticalThumbDrawable,
                fastScrollPageIndicatorBackgroundDrawable,
                fastScrollVerticalThumbMarginEnd,
                fastScrollPageIndicatorMarginEnd,
            )

        val localFastScroller = FastScroller(fastScrollDrawer, fastScrollCalculator)
        fastScroller = localFastScroller
        fastScrollGestureDetector =
            FastScrollGestureDetector(localFastScroller, fastScrollGestureHandler)

        /* Invalidate the virtual views within the accessibility hierarchy when the fast scroller auto-hides. */
        fastScroller?.visibilityChangeListener = { isVisible ->
            if (lastFastScrollerVisibility != isVisible) {
                lastFastScrollerVisibility = isVisible
                if (!isVisible) {
                    pdfViewAccessibilityManager?.invalidateRoot()
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
        // No pages to render, return without processing document further.
        if (localPdfDocument.pageCount <= 0) return
        /* We use the maximum pixel dimension of the display as the maximum pixel dimension for any
        single Bitmap we render, i.e. the threshold for tiled rendering. This is an arbitrary,
        but reasonable threshold to use that does not depend on volatile state like the current
        screen orientation or the current size of our application's Window. */
        val maxBitmapDimensionPx = max(context.display.width, context.display.height)

        pageManager =
            PageManager(
                localPdfDocument,
                backgroundScope,
                Point(maxBitmapDimensionPx, maxBitmapDimensionPx),
                errorFlow,
                isAccessibilityEnabled,
            )

        formWidgetInteractionHandler =
            FormWidgetInteractionHandler(context, backgroundScope) { formFillingEditText ->
                this.formFillingEditText = formFillingEditText
            }

        localPdfDocument.addOnPdfContentInvalidatedListener(
            context.mainExecutor,
            onPdfContentInvalidatedListener,
        )

        setupFastScroller()
        // set initial visibility of fast scroller
        maybeHideFastScroller()

        // We'll either create our layout and selection managers from restored state, or
        // instantiate new ones
        if (!maybeRestoreState()) {
            pageLayoutManager =
                PageLayoutManager(
                        localPdfDocument,
                        backgroundScope,
                        topPageMarginPx = context.getDimensions(R.dimen.top_page_margin),
                        pagesPerRow = pagesPerRow,
                        horizontalPageSpacingPx = horizontalPageSpacing.toFloat(),
                        verticalPageSpacingPx = verticalPageSpacing.toFloat(),
                        errorFlow = errorFlow,
                        isFormFillingEnabled = { isFormFillingEnabled },
                    )
                    .apply { onViewportChanged() }

            val isImageSelectionAvailable =
                isSdkExtensionGreaterEqualTo(19) && isImageSelectionEnabled
            selectionStateManager =
                SelectionStateManager(
                    pdfDocument = localPdfDocument,
                    backgroundScope = backgroundScope,
                    handleTouchTargetSizePx =
                        resources.getDimensionPixelSize(R.dimen.text_select_handle_touch_size),
                    errorFlow = errorFlow,
                    pageLayoutManager = pageLayoutManager,
                    pageManager = pageManager,
                    isImageSelectionEnabled = isImageSelectionAvailable,
                )
            setAccessibility()
        }

        /* PageMetadataLoader must have been initialized either with the restored state
        or with the default state (if [maybeRestoreState] return false) */
        pageLayoutManager?.let { pageLayoutManager ->
            formWidgetMetadataLoader =
                FormWidgetMetadataLoader(
                    localPdfDocument,
                    pageLayoutManager.pdfFormFillingState,
                    errorFlow,
                )
        }

        // If not, we'll start doing this when we _are_ attached to a visible window
        if (isAttachedToVisibleWindow) {
            startCollectingData()
        } else {
            // Fetch the page dimensions upfront.
            startedFetchingAllDimensions = true
            pageLayoutManager?.fetchAllPageDimensionsInBgGradually()
        }
    }

    private val View.isAttachedToVisibleWindow
        get() = isAttachedToWindow && windowVisibility == VISIBLE

    private fun onViewportChanged() {
        if (deferViewportUpdate) return
        val prevVisiblePages = visiblePages
        // If the viewport didn't actually change, short-circuit all of the downstream work
        if (
            pageLayoutManager?.onViewportChanged(getVisibleAreaInContentCoords()) != true &&
                prevScrollY == scrollY
        )
            return
        prevScrollY = scrollY
        dispatchViewportChanged()
        // Avoid fetching Bitmaps during active gestures like zoom and scroll, except to render
        // net new pages
        if (positionIsStable || visiblePages != prevVisiblePages) {
            maybeUpdatePageVisibility()
        }
        pdfViewAccessibilityManager?.invalidateRoot()
        formFillingEditText?.editText?.let { requestLayout() }
    }

    private fun addFormFillingEditText() {
        formFillingEditText?.let {
            addViewInLayout(it.editText, 0, it.editText.layoutParams)
            requestLayout()
            it.editText.requestFocus()
            it.editText.post {
                val imm: InputMethodManager =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(formFillingEditText?.editText, 0)
            }
            adjustScroll(it.pageNum, it.formWidget)
        }
    }

    /**
     * Adjusts the scroll to bring the top left of the edit text which overlays the [formWidget] to
     * the center of the view.
     */
    private fun adjustScroll(pageNum: Int, formWidget: FormWidgetInfo) {
        val widgetTopLeftViewCoordinates =
            pdfToViewPoint(
                PdfPoint(
                    pageNum,
                    formWidget.widgetRect.left.toFloat(),
                    formWidget.widgetRect.top.toFloat(),
                )
            )
        if (widgetTopLeftViewCoordinates == null) return

        val xScrollOffset = (widgetTopLeftViewCoordinates.x - (width / 2)).roundToInt()
        val yScrollOffset = (widgetTopLeftViewCoordinates.y - (height / 2)).roundToInt()
        scrollBy(xScrollOffset, yScrollOffset)
    }

    private fun dispatchViewportChanged() {
        // If we don't have a page layout manager, we have no viewport to report
        val localPageLayoutManager = pageLayoutManager ?: return
        val pageLocations = localPageLayoutManager.pageLocations

        // Copy each page location into the SparseArray dispatched to listeners, i.e. to avoid
        // developers mutating our source of truth. Use a Pool to populate the SparseArray
        // dispatched to listeners, i.e. to avoid excessive Rect allocations at low zoom levels
        val dispatchedLocations = SparseArray<RectF>(pageLocations.size())
        for (page in pageLocations.keyIterator()) {
            val dispatchedLocation = pageLocationsPool.acquire() ?: RectF()
            dispatchedLocation.set(pageLocations.get(page))
            dispatchedLocations.put(page, dispatchedLocation.asViewRectF())
        }

        for (listener in onViewportChangedListeners) {
            listener.onViewportChanged(
                firstVisiblePage,
                visiblePagesCount,
                dispatchedLocations,
                zoom,
            )
        }

        // Release copied Rects that we've dispatched to our listener. Our API documentation
        // specifies the page location Rects will be recycled and shouldn't be used beyond the
        // scope of the listener method.
        for (location in dispatchedLocations.valueIterator()) {
            pageLocationsPool.release(location)
        }
    }

    private val showSelectionActionModeRunnable = Runnable { updateSelectionActionModeVisibility() }

    /**
     * Shows or hides the selection action mode, as appropriate. If the current selection is visible
     * and a gesture is not in progress, the action mode will be shown. Otherwise, it will be
     * hidden.
     */
    private fun updateSelectionActionModeVisibility() {
        if (isSelectionVisible() && gestureState == GESTURE_STATE_IDLE) {
            selectionActionModeCallback?.actionMode?.invalidateContentRect()
            selectionStateManager?.maybeShowActionMode()
        } else {
            selectionStateManager?.maybeHideActionMode()
        }
    }

    private fun isSelectionVisible(): Boolean {
        // If we don't have a selection or any way to understand the layout of our pages, the
        // selection is not visible
        val localSelection = currentSelection ?: return false
        val localPageLayoutManager = pageLayoutManager ?: return false

        // Get the area of the screen currently visible in content coordinates
        val viewport = getVisibleAreaInContentCoords()

        // Iterate over all bounding boxes that make up the selection
        for (contentRect in localSelection.bounds) {
            // Convert content coordinates (contentRect) to content view coordinates.
            val contentViewRect =
                localPageLayoutManager.getContentViewRect(contentRect, viewport) ?: continue

            // Check for intersection between the selection's bound and the viewport
            if (
                viewport.intersects(
                    contentViewRect.left,
                    contentViewRect.top,
                    contentViewRect.right,
                    contentViewRect.bottom,
                )
            ) {
                return true
            }
        }
        return false
    }

    private fun reset() {
        // Stop any in progress fling when we open a new document
        scroller.forceFinished(true)
        pageManager?.cleanup()
        pageManager = null
        pageLayoutManager = null
        formFillingEditText = null
        startedFetchingAllDimensions = false
        backgroundScope.coroutineContext.cancelChildren()
        pdfDocument?.removeOnPdfContentInvalidatedListener(onPdfContentInvalidatedListener)
        stopCollectingData()
        isFirstPageRendered = false
        notifyFirstContentLoad = true
        isAnyBitmapAvailable = false

        // Reset zoom and scroll after clearing pageMetadata loader, otherwise they can trigger
        // onViewportChanged callback with outdated information.
        scrollTo(0, 0)
        zoom = DEFAULT_INIT_ZOOM
    }

    private fun maybeUpdatePageVisibility() {
        val localPageLayoutManager = pageLayoutManager ?: return
        val visiblePageAreas = localPageLayoutManager.visiblePageAreas
        pageManager?.updatePageVisibilities(
            visiblePageAreas,
            zoom,
            positionIsStable,
            localPageLayoutManager.layingOutPages,
        )

        if (!startedFetchingAllDimensions) {
            startedFetchingAllDimensions = true
            pageLayoutManager?.fetchAllPageDimensionsInBgGradually()
        }
    }

    /** Sets the initial zoom to fit the content width and centers the view. */
    private fun setInitialZoomScroll() {
        // Only set default zoom if zoom is still the initial value
        if (zoom == DEFAULT_INIT_ZOOM) {
            this.zoom = getFitToWidthZoom()
        }
        // We use scrollY to center content smaller than the viewport. This triggers the initial
        // centering if it's needed. It doesn't override any restored state because we're scrolling
        // to the current scroll position.
        scrollTo(scrollX, scrollY)
    }

    /** React to a page's metadata being made available */
    private fun onPageMetaDataReceived(pageInfo: PdfDocument.PageInfo) {
        val pageNum = pageInfo.pageNum
        val size = Point(pageInfo.width, pageInfo.height)
        val formWidgetInfos = pageInfo.formWidgetInfos

        val localPageLayoutManager = pageLayoutManager ?: return
        val visiblePageArea = localPageLayoutManager.visiblePageAreas.get(pageNum)
        pageManager?.addPage(
            pageNum,
            size,
            zoom,
            positionIsStable,
            visiblePageArea,
            localPageLayoutManager.layingOutPages,
            PdfFormFillingConfig(
                { isFormFillingEnabled },
                context.getColor(R.color.form_fields_highlight_color),
            ),
            formWidgetInfos,
        )
        // Learning the dimensions of a page can change our understanding of the content that's in
        // the viewport
        onViewportChanged()

        // Trigger initial zoom/scroll when the dimensions for all the pages in first row depending
        // upon the layout strategy are loaded. minOf correctly handles documents with fewer pages
        // than the row capacity.
        val localPdfDocument = pdfDocument ?: return
        if (pageNum == minOf(localPdfDocument.pageCount - 1, pagesPerRow - 1)) {
            setInitialZoomScroll()
        }

        when (val target = deferredScrollTarget) {
            is DeferredScrollTarget.ToPage -> {
                if (target.pageNum <= pageInfo.pageNum) {
                    scrollDelegate.scrollToPage(target.pageNum) {}
                    deferredScrollTarget = null
                }
            }
            is DeferredScrollTarget.ToPosition -> {
                if (target.position.pageNum <= pageInfo.pageNum) {
                    scrollDelegate.scrollToPosition(target.position, ScrollAlignment.CENTRE) {}
                    deferredScrollTarget = null
                }
            }
            null -> {
                // No deferred scroll, do nothing.
            }
        }
    }

    /** Set the zoom, using the given point as a pivot point to zoom in or out of */
    internal fun zoomTo(zoom: Float, pivotX: Float, pivotY: Float) {
        // TODO(b/376299551) - Restore to developer-configured initial zoom value once that API is
        // implemented
        val newZoom = if (Float.NaN.equals(zoom)) DEFAULT_INIT_ZOOM else zoom
        val deltaX = scrollDeltaNeededForZoomChange(this.zoom, newZoom, pivotX, scrollX)
        val deltaY = scrollDeltaNeededForZoomChange(this.zoom, newZoom, pivotY, scrollY)

        deferViewportUpdate = true
        this.zoom = newZoom
        scrollBy(deltaX, deltaY)
        deferViewportUpdate = false
        onViewportChanged()
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
    internal fun getVisibleAreaInContentCoords(): RectF {
        visibleAreaRect.set(
            toContentX(0F),
            toContentY(0F),
            toContentX(viewportWidth.toFloat() + paddingRight + paddingLeft),
            toContentY(viewportHeight.toFloat() + paddingBottom + paddingTop),
        )
        return visibleAreaRect
    }

    /**
     * Initializes and sets the accessibility delegate for the PdfView.
     *
     * This method creates an instance of [PdfViewAccessibilityManager] if both [.pageLayoutManager]
     * and [.pageManager] are initialized, and sets it as the accessibility delegate for the view
     * using [ViewCompat.setAccessibilityDelegate].
     */
    private fun setAccessibility() {
        if (isAccessibilityEnabled && pageLayoutManager != null && pageManager != null) {
            pdfViewAccessibilityManager =
                PdfViewAccessibilityManager(
                    this,
                    pageLayoutManager!!,
                    pageManager!!,
                    formWidgetInteractionHandler!!,
                ) {
                    fastScroller
                }
        } else {
            pdfViewAccessibilityManager = null
        }
        ViewCompat.setAccessibilityDelegate(this, pdfViewAccessibilityManager)
    }

    internal fun commitFormFillingEditText() {
        formFillingEditText?.let { formWidgetInteractionHandler?.finishTextEditing(it) }
    }

    private val shouldShowFormFillingTooltip: Boolean
        get() = isFormFillingTooltipEnabled && isFormFillingEnabled && isPdfValidForm

    private val isPdfValidForm: Boolean
        get() = pdfDocument?.formType != PdfDocument.PDF_FORM_TYPE_NONE

    private fun getFirstVisibleAndEditableFormWidget(): Pair<Int, FormWidgetInfo>? {
        val localPageMetadataLoader = pageLayoutManager ?: return null
        val visiblePageAreas = localPageMetadataLoader.visiblePageAreas

        visiblePageAreas.keyIterator().forEach { pageNum ->
            val editableFormWidgetsInPage =
                pageManager?.pages[pageNum]?.formWidgetInfos?.filter { !it.isReadOnly }

            editableFormWidgetsInPage?.forEach { widget ->
                if (visiblePageAreas.get(pageNum).contains(widget.widgetRect.toRectF())) {
                    return Pair(pageNum, widget)
                }
            }
        }
        return null
    }

    /** The height of the viewport, minus padding */
    internal val viewportHeight: Int
        get() = bottom - top - paddingBottom - paddingTop

    /** The width of the viewport, minus padding */
    internal val viewportWidth: Int
        get() = right - left - paddingRight - paddingLeft

    /**
     * Converts an X coordinate in View space (scaled) to an X coordinate in content space
     * (unscaled)
     */
    internal fun toContentX(viewX: Float): Float {
        return toContentCoord(viewX, zoom, scrollX)
    }

    /**
     * Converts a Y coordinate in View space (scaled) to a Y coordinate in content space (unscaled)
     */
    internal fun toContentY(viewY: Float): Float {
        return toContentCoord(viewY, zoom, scrollY - paddingTop)
    }

    internal fun toViewX(contentX: Float): Float {
        return toViewCoord(contentX, zoom, scrollX)
    }

    internal fun toViewY(contentY: Float): Float {
        return toViewCoord(contentY, zoom, scrollY - paddingTop)
    }

    internal val contentWidth: Float
        get() = pageLayoutManager?.maxContentWidth ?: 0f

    internal val contentHeight: Float
        get() = pageLayoutManager?.contentHeight ?: 0f

    /** Returns a new [Rect] representing [contentRect] in View coordinates */
    internal fun toViewRect(contentRect: RectF): Rect =
        toViewRect(contentRect.left, contentRect.top, contentRect.right, contentRect.bottom)

    private fun toViewRect(left: Number, top: Number, right: Number, bottom: Number): Rect {
        return Rect(
            toViewX(left.toFloat()).roundToInt(),
            toViewY(top.toFloat()).roundToInt(),
            toViewX(right.toFloat()).roundToInt(),
            toViewY(bottom.toFloat()).roundToInt(),
        )
    }

    /** Converts an existing [RectF] in content coordinates to View coordinates */
    private fun RectF.asViewRectF(): RectF {
        this.set(
            toViewCoord(left, zoom, scrollX),
            toViewCoord(top, zoom, scrollY),
            toViewCoord(right, zoom, scrollX),
            toViewCoord(bottom, zoom, scrollY),
        )
        return this
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(VERTICAL_ALIGNMENT_TOP, VERTICAL_ALIGNMENT_CENTER)
    public annotation class VerticalAlignment

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(GESTURE_STATE_IDLE, GESTURE_STATE_INTERACTING, GESTURE_STATE_SETTLING)
    public annotation class GestureState

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
            dispatchGestureStateChangedUnlessFastScroll(newState = GESTURE_STATE_INTERACTING)
            // We should hide the action mode during a gesture
            updateSelectionActionModeVisibility()
        }

        override fun onGestureEnd(gesture: GestureTracker.Gesture?) {
            // Update page visibility after scroll / zoom gestures end, because we avoid fetching
            // certain data while those gestures are in progress
            if (gesture in ZOOM_OR_SCROLL_GESTURES) maybeUpdatePageVisibility()
            val newState =
                if (gesture !in ANIMATED_GESTURES) {
                    GESTURE_STATE_IDLE
                } else {
                    GESTURE_STATE_SETTLING
                }
            dispatchGestureStateChangedUnlessFastScroll(newState)
            totalX = 0f
            totalY = 0f
            straightenCurrentVerticalScroll = true
            scrollQueue.clear()
            // We should reveal the action mode after a gesture
            updateSelectionActionModeVisibility()
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float,
        ): Boolean {
            var dx = distanceX.roundToInt()
            val dy = distanceY.roundToInt()

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
            velocityY: Float,
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
            val localPageLayoutManager = pageLayoutManager ?: return super.onLongPress(e)
            val touchPoint =
                localPageLayoutManager.getPdfPointAt(
                    toContentX(e.x),
                    toContentY(e.y),
                    getVisibleAreaInContentCoords(),
                ) ?: return super.onLongPress(e)

            selectionStateManager?.maybeSelectContentAtPoint(touchPoint)
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
            doubleTapAnimator?.cancel()

            doubleTapAnimator =
                ValueAnimator.ofFloat(0f, 1f).apply {
                    // Slightly shorter duration for snappier feel
                    duration = DOUBLE_TAP_ANIMATION_DURATION_MS
                    addUpdateListener { animator ->
                        val animatedValue = animator.animatedValue as Float
                        val value = currentZoom + (newZoom - currentZoom) * animatedValue
                        zoomTo(value, e.x, e.y)
                    }
                    // We avoid pinging pages with new zoom states and fetching new bitmaps during
                    // animations. Update pages with the final zoom state when the animation ends
                    addListener(
                        onCancel = {
                            dispatchGestureStateChangedUnlessFastScroll(
                                newState = GESTURE_STATE_IDLE
                            )
                            maybeUpdatePageVisibility()
                        },
                        onEnd = {
                            dispatchGestureStateChangedUnlessFastScroll(
                                newState = GESTURE_STATE_IDLE
                            )
                            updateSelectionActionModeVisibility()
                            maybeUpdatePageVisibility()
                        },
                    )

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
            commitFormFillingEditText()
            selectionStateManager?.clearCurrentSelection()
            val localPageLayoutManager = pageLayoutManager ?: return super.onSingleTapConfirmed(e)
            val touchPoint =
                localPageLayoutManager.getPdfPointAt(
                    toContentX(e.x),
                    toContentY(e.y),
                    getVisibleAreaInContentCoords(),
                ) ?: return super.onSingleTapConfirmed(e)

            pageManager?.getPageLinks(touchPoint.pageNum)?.let { links ->
                val touchPointOnPage = PointF(touchPoint.x, touchPoint.y)
                if (handleGotoLinks(links, touchPointOnPage)) return true
                if (handleExternalLinks(links, touchPointOnPage)) return true
            }

            if (isFormFillingEnabled && !isFormEditStateBeingRestored) {
                pageManager?.getWidgetAtTapPoint(touchPoint)?.let { widgets ->
                    if (handleTapOnFormWidget(widgets, touchPoint)) return true
                }
            }

            return super.onSingleTapConfirmed(e)
        }

        private fun handleGotoLinks(
            links: PdfDocument.PdfPageLinks,
            pdfCoordinates: PointF,
        ): Boolean {
            links.gotoLinks.forEach { gotoLink ->
                if (gotoLink.bounds.any { it.contains(pdfCoordinates.x, pdfCoordinates.y) }) {
                    val destination =
                        PdfPoint(
                            pageNum = gotoLink.destination.pageNumber,
                            pagePoint =
                                PointF(
                                    gotoLink.destination.xCoordinate,
                                    gotoLink.destination.yCoordinate,
                                ),
                        )

                    scrollToPosition(destination)
                    return true
                }
            }
            return false
        }

        private fun handleExternalLinks(
            links: PdfDocument.PdfPageLinks,
            pdfCoordinates: PointF,
        ): Boolean {
            links.externalLinks.forEach { externalLink ->
                if (externalLink.bounds.any { it.contains(pdfCoordinates.x, pdfCoordinates.y) }) {
                    return openExternalLink(externalLink.uri)
                }
            }
            return false
        }

        private fun handleTapOnFormWidget(
            formWidgetInfos: List<FormWidgetInfo>,
            touchPoint: PdfPoint,
        ): Boolean {
            formWidgetInfos.forEach { formWidgetInfo ->
                // TODO: b/410008790 Implement business logic to perform action on form widget
                if (
                    formWidgetInfo.widgetRect.contains(
                        touchPoint.x.roundToInt(),
                        touchPoint.y.roundToInt(),
                    )
                ) {
                    formWidgetInteractionHandler?.handleInteraction(touchPoint, formWidgetInfo)
                    return true
                }
            }
            return false
        }
    }

    internal fun openExternalLink(uri: Uri): Boolean {
        val externalLink = ExternalLink(uri)
        if (linkClickListener?.onLinkClicked(externalLink) == true) {
            return true
        } else {
            try {
                val intent = Intent(Intent.ACTION_VIEW, externalLink.uri)
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
                return false
            }
        }
    }

    public companion object {
        /** The PdfView is not currently being affected by an outside input, e.g. user touch */
        public const val GESTURE_STATE_IDLE: Int = 0

        /** The PdfView is currently being affected by an outside input, e.g. user touch */
        public const val GESTURE_STATE_INTERACTING: Int = 1

        /**
         * The PdfView is currently animating to a final position while not under outside control,
         * e.g. settling on a final position following a fling gesture.
         */
        public const val GESTURE_STATE_SETTLING: Int = 2

        /** Represents the configuration for displaying a single page per row (standard display). */
        @RestrictTo(RestrictTo.Scope.LIBRARY) public const val SINGLE_PAGE: Int = 1

        /** Represents the configuration for displaying two pages per row (like an open book). */
        @RestrictTo(RestrictTo.Scope.LIBRARY) public const val TWO_PAGE: Int = 2

        /**
         * Vertically aligns the page to the top of the PdfView.
         *
         * This alignment is used when the page is scaled to fit the view's width, and the resulting
         * page height is less than the view's height. The top of the page will be positioned at the
         * top of the [PdfView]. If the scaled page height is greater than the height of [PdfView],
         * the top of the first page is always at the top of the view by default, and this alignment
         * will have no effect.
         */
        public const val VERTICAL_ALIGNMENT_TOP: Int = 0

        /**
         * Vertically aligns the page to the center of the PdfView.
         *
         * This alignment is used when the page is scaled to fit the view's width, and the resulting
         * page height is less than the view's height. The top of the page will be positioned at the
         * center of the [PdfView]. If the scaled page height is greater than the height of
         * [PdfView], the top of the first page is always at the top of the view by default, and
         * this alignment will have no effect.
         */
        public const val VERTICAL_ALIGNMENT_CENTER: Int = 1

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val DEFAULT_INIT_ZOOM: Float = 1.0f
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val MAX_PERMISSIBLE_ZOOM: Float = 25.0f
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val MIN_PERMISSIBLE_ZOOM: Float = 0.5f

        /** The ratio of vertical to horizontal scroll that is assumed to be vertical only */
        private const val SCROLL_CORRECTION_RATIO = 1.5f

        /** The maximum scroll distance used to determine if the direction is vertical */
        private const val MAX_SCROLL_WINDOW_DP = 70

        /** The smallest scroll distance that can switch mode to "free scrolling" */
        private const val MIN_SCROLL_TO_SWITCH_DP = 30

        /** The duration of the double tap to zoom animation, in milliseconds */
        private const val DOUBLE_TAP_ANIMATION_DURATION_MS = 200L

        /** The amount of delay between two scroll events */
        private const val AUTO_SCROLL_DELAY_IN_MILLIS = 5L

        /** The amount of delay for actionMode to show after scroll event */
        private const val ACTION_MODE_REAPPEAR_DELAY_MS = 500L

        /**
         * The tolerance in percentage to control how close the touch point needs to be to the
         * bottom or top of the viewport for scroll-during-selection to start.
         */
        private const val SCROLL_SELECTION_TOLERANCE_RATIO = 0.85f

        /** The amount to control how much it scrolls while selection */
        private const val AUTO_SCROLL_BY_VALUE = 20

        private val ZOOM_OR_SCROLL_GESTURES =
            setOf(
                GestureTracker.Gesture.ZOOM,
                GestureTracker.Gesture.DRAG,
                GestureTracker.Gesture.DRAG_X,
                GestureTracker.Gesture.DRAG_Y,
                GestureTracker.Gesture.FLING,
            )

        private val ANIMATED_GESTURES =
            setOf(GestureTracker.Gesture.FLING, GestureTracker.Gesture.DOUBLE_TAP)

        private val VALID_GESTURE_STATES =
            setOf(GESTURE_STATE_IDLE, GESTURE_STATE_INTERACTING, GESTURE_STATE_SETTLING)

        private fun checkMainThread() {
            check(Looper.myLooper() == Looper.getMainLooper()) {
                "Property must be set on the main thread"
            }
        }

        /**
         * Converts a one-dimensional coordinate in View space (scaled, offset by scroll position)
         * to a one-dimensional coordinate in content space (unscaled).
         *
         * In both coordinate spaces the origin is at the top left corner of the page with the
         * positive X direction being left and the positive Y direction being down.
         */
        internal fun toContentCoord(viewCoord: Float, zoom: Float, scroll: Int): Float {
            return (viewCoord + scroll) / zoom
        }

        /**
         * Converts a one-dimensional coordinate in content space (unscaled) to a View coordinate
         * (scaled, offset by scroll position)
         *
         * In both coordinate spaces the origin is at the top left corner of the page with the
         * positive X direction being left and the positive Y direction being down.
         */
        internal fun toViewCoord(contentCoord: Float, zoom: Float, scroll: Int): Float {
            return (contentCoord * zoom) - scroll
        }
    }
}

private fun isSdkExtensionGreaterEqualTo(sdkExtension: Int): Boolean {
    return SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= sdkExtension
}
