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

package androidx.pdf.annotation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Looper
import android.util.AttributeSet
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.MainThread
import androidx.pdf.annotation.AnnotationsView.AnnotationMode.Highlight
import androidx.pdf.annotation.AnnotationsView.AnnotationMode.Select
import androidx.pdf.annotation.content.KeyedPdfAnnotation
import androidx.pdf.annotation.content.PdfAnnotation
import androidx.pdf.annotation.drawer.DefaultPdfObjectDrawerFactoryImpl
import androidx.pdf.annotation.drawer.PdfAnnotationDrawerFactory
import androidx.pdf.annotation.drawer.PdfAnnotationDrawerFactoryImpl
import androidx.pdf.annotation.drawer.PdfDocumentAnnotationsDrawerImpl
import androidx.pdf.annotation.drawer.PdfObjectDrawerFactory
import androidx.pdf.annotation.highlights.InProgressHighlightsView

/**
 * A custom Android [ViewGroup] responsible for drawing a collection of annotations onto a Canvas.
 * Each set of page annotations can have its own transformation matrix. It also supports annotating
 * like text highlighting.
 *
 * This inherits [ViewGroup] but does not support adding arbitrary children via [addView] or in a
 * layout.
 */
public class AnnotationsView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ViewGroup(context, attrs, defStyleAttr) {

    private val onAnnotationLocatedListeners = mutableListOf<OnAnnotationLocatedListener>()

    /** The view for displaying in-progress annotations (e.g., wet highlights). */
    private val inProgressHighlightsView: InProgressHighlightsView

    private var annotationsLocator: AnnotationsLocator? = null
    private var annotations: SparseArray<PageAnnotationsData> = SparseArray()

    /** Provides page information from view coordinates */
    internal var pageInfoProvider: PageInfoProvider

    private var textBoundsProvider: TextBoundsProvider? = null

    init {
        setWillNotDraw(false)

        pageInfoProvider = PageInfoProvider()
        inProgressHighlightsView =
            InProgressHighlightsView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                visibility = GONE
            }
        inProgressHighlightsView.pageInfoProvider = pageInfoProvider
        annotationsLocator = AnnotationsLocator(context, pageInfoProvider)
        addViewInternal(inProgressHighlightsView)
    }

    override fun addView(child: View?) {
        throw UnsupportedOperationException("External views cannot be added to AnnotationsView")
    }

    override fun addView(child: View?, index: Int) {
        throw UnsupportedOperationException("External views cannot be added to AnnotationsView")
    }

    override fun addView(child: View?, params: LayoutParams?) {
        throw UnsupportedOperationException("External views cannot be added to AnnotationsView")
    }

    override fun addView(child: View?, index: Int, params: LayoutParams?) {
        throw UnsupportedOperationException("External views cannot be added to AnnotationsView")
    }

    @MainThread
    private fun addViewInternal(child: View) {
        checkMainThread()
        if (addViewInLayout(child, -1, child.layoutParams)) {
            // Ensure that the UI is updated after the child view has been added.
            requestLayout()
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        if (inProgressHighlightsView.visibility != GONE) {
            measureChild(inProgressHighlightsView, widthMeasureSpec, heightMeasureSpec)
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        inProgressHighlightsView.layout(
            paddingLeft,
            paddingTop,
            right - left - paddingRight,
            bottom - top - paddingBottom,
        )
    }

    /**
     * The current interaction mode, determining how touch events are handled for annotations.
     *
     * Set to [AnnotationMode.Select] to enable selecting existing annotations, or
     * [AnnotationMode.Highlight] to create new text highlights. If `null`, touch interactions for
     * annotations are disabled on [AnnotationsView] and its children.
     *
     * This property must only be modified on the UI thread.
     */
    public var interactionMode: AnnotationMode? = null
        set(value) {
            checkMainThread()
            field = value
            if (value is Highlight) {
                setHighlighter(value)
            } else {
                setHighlighter(null)
            }
        }

    /**
     * Updates both the content and the layout positioning of the annotations in a single atomic
     * operation.
     *
     * @param pdfViewportState The latest layout snapshot (zoom, scroll, and pagebounds) from the
     *   PDF renderer.
     * @param annotations A [SparseArray] indexed with page num containing the list of
     *   [KeyedPdfAnnotation] objects to be rendered.
     */
    @MainThread
    public fun updateDisplayState(
        pdfViewportState: PdfViewportState,
        annotations: SparseArray<List<KeyedPdfAnnotation>>,
    ) {
        checkMainThread()
        pageInfoProvider.setPageBounds(pdfViewportState.pageBounds)
        pageInfoProvider.setZoom(pdfViewportState.zoom)
        this.annotations = extractVisiblePageAnnotations(pdfViewportState, annotations)
        invalidate()
    }

    /**
     * Sets the [TextBoundsProvider] used to retrieve text boundary information during highlighting.
     *
     * @param textBoundsProvider The provider implementation to be used for text boundary lookups.
     */
    public fun setTextBoundsProvider(textBoundsProvider: TextBoundsProvider) {
        this.textBoundsProvider = textBoundsProvider
        inProgressHighlightsView.textBoundsProvider = textBoundsProvider
    }

    /**
     * Sets a listener to be notified when this view claims or abandons the current gesture stream.
     *
     * @param listener The listener to receive gesture coordination signals, or null to clear.
     */
    public fun setOnGestureClaimListener(listener: OnGestureClaimListener?) {
        inProgressHighlightsView.setOnGestureClaimListener(listener)
    }

    /**
     * Adds a listener for events related to the finalized creation or failure of annotations.
     *
     * Registered listeners will be notified when an in-progress interaction successfully produces a
     * new [PdfAnnotation], or if an error occurs during the process.
     *
     * @param listener The listener to be added to the registry.
     */
    public fun addOnAnnotationEditListener(listener: OnAnnotationEditListener) {
        inProgressHighlightsView.addOnAnnotationEditListener(listener)
    }

    /** Adds a listener for annotation hit events. */
    public fun addOnAnnotationLocatedListener(listener: OnAnnotationLocatedListener) {
        if (!onAnnotationLocatedListeners.contains(listener)) {
            onAnnotationLocatedListeners.add(listener)
        }
    }

    /** Removes a listener that was previously added via [addOnAnnotationEditListener]. */
    public fun removeOnAnnotationEditListener(listener: OnAnnotationEditListener) {
        inProgressHighlightsView.removeOnAnnotationEditListener(listener)
    }

    /** Removes a listener that was previously added via [addOnAnnotationLocatedListener]. */
    public fun removeOnAnnotationLocatedListener(listener: OnAnnotationLocatedListener) {
        onAnnotationLocatedListeners.remove(listener)
    }

    private var pdfObjectDrawerFactory: PdfObjectDrawerFactory = DefaultPdfObjectDrawerFactoryImpl

    private var annotationDrawerFactory: PdfAnnotationDrawerFactory =
        PdfAnnotationDrawerFactoryImpl(pdfObjectDrawerFactory)

    private fun extractVisiblePageAnnotations(
        pdfViewportState: PdfViewportState,
        annotations: SparseArray<List<KeyedPdfAnnotation>>,
    ): SparseArray<PageAnnotationsData> {
        val newAnnotationsData = SparseArray<PageAnnotationsData>()
        val firstVisiblePage = pdfViewportState.firstVisiblePage
        val lastVisiblePage = firstVisiblePage + pdfViewportState.visiblePagesCount - 1

        for (pageNum in firstVisiblePage..lastVisiblePage) {
            val pageAnnotations = annotations.get(pageNum) ?: emptyList()
            val transform = pageInfoProvider.getPageInfo(pageNum)?.pageToViewTransform ?: Matrix()
            newAnnotationsData.put(pageNum, PageAnnotationsData(pageAnnotations, transform))
        }
        return newAnnotationsData
    }

    /**
     * Configures the highlighter.
     *
     * @param highlightMode The configuration for the highlighter. Pass null to disable.
     */
    private fun setHighlighter(highlightMode: Highlight?) {
        inProgressHighlightsView.apply {
            if (highlightMode != null) {
                highlightColor = highlightMode.color
                visibility = VISIBLE
            } else {
                visibility = GONE
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        PdfDocumentAnnotationsDrawerImpl(annotationDrawerFactory).draw(annotations, canvas)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false
        return when (interactionMode) {
            is Select -> {
                val localAnnotationsLocator = annotationsLocator
                if (localAnnotationsLocator != null) {
                    val foundAnnotations =
                        localAnnotationsLocator.findAnnotations(annotations, event)
                    if (foundAnnotations.isNotEmpty()) {
                        onAnnotationLocatedListeners.forEach {
                            it.onAnnotationsLocated(x = event.x, y = event.y, foundAnnotations)
                        }
                        return true
                    }
                }
                false
            }
            is Highlight -> {
                if (inProgressHighlightsView.visibility == VISIBLE) {
                    return inProgressHighlightsView.onTouchEvent(event)
                }
                super.onTouchEvent(event)
            }
            else -> super.onTouchEvent(event)
        }
    }

    /**
     * Holds all annotations for a single PDF page and their transformation matrix.
     *
     * @property keyedAnnotations List of [PdfAnnotation]s on the page.
     * @property transform [Matrix] to apply when drawing these annotations.
     */
    internal data class PageAnnotationsData(
        val keyedAnnotations: List<KeyedPdfAnnotation>,
        val transform: Matrix,
    )

    /** Defines the current interaction mode of the [AnnotationsView]. */
    public abstract class AnnotationMode internal constructor() {
        /** Mode for selecting existing annotations (e.g. erase, drag, scale). */
        public object Select : AnnotationMode()

        /** Mode for creating new highlight annotations. */
        public class Highlight(@get:ColorInt @param:ColorInt public val color: Int) :
            AnnotationMode()
    }

    /**
     * Callback interface for gesture coordination events.
     *
     * These signals allow the host to coordinate the touch event stream between different
     * simultaneous interactions.
     */
    public interface OnGestureClaimListener {
        /**
         * Called when the view 'claims' the current gesture stream.
         *
         * The host should typically use this signal to cancel any other 'shadow' or simultaneous
         * interactions that are currently tracking this gesture.
         */
        public fun onGestureClaimed()

        /**
         * Called when the view 'abandons' its interest in the current gesture.
         *
         * The host can use this signal to allow other interactions to continue exclusively.
         */
        public fun onGestureAbandoned()
    }

    /** Callback interface for events related to the creation and modification of annotations. */
    public interface OnAnnotationEditListener {

        /**
         * Called when an in-progress interaction successfully produces a finalized [PdfAnnotation].
         *
         * @param annotation The finalized [PdfAnnotation] object containing the metadata generated
         *   by the user's interaction.
         */
        public fun onAnnotationCreated(annotation: PdfAnnotation)

        /**
         * Called when a failure occurs during the creation or modification of an annotation.
         *
         * @param throwable The underlying cause of the failure.
         */
        public fun onAnnotationError(throwable: Throwable)
    }

    /** Callback interface for annotation hit events. */
    public fun interface OnAnnotationLocatedListener {
        /**
         * Called when one or more annotations are successfully located at a specific touch
         * location.
         *
         * @param x The x-coordinate of the touch event in view coordinates.
         * @param y The y-coordinate of the touch event in view coordinates.
         * @param annotations The list of [KeyedPdfAnnotation] objects found at the (x, y) location,
         *   typically ordered by visual stacking order (Z-index) (top-bottom).
         */
        public fun onAnnotationsLocated(x: Float, y: Float, annotations: List<KeyedPdfAnnotation>)
    }

    public companion object {
        private fun checkMainThread() {
            check(Looper.myLooper() == Looper.getMainLooper()) {
                "Property must be set on the main thread"
            }
        }
    }
}
