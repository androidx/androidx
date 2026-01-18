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
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.pdf.PdfDocument
import androidx.pdf.annotation.drawer.DefaultPdfObjectDrawerFactoryImpl
import androidx.pdf.annotation.drawer.PdfAnnotationDrawerFactory
import androidx.pdf.annotation.drawer.PdfAnnotationDrawerFactoryImpl
import androidx.pdf.annotation.drawer.PdfDocumentAnnotationsDrawerImpl
import androidx.pdf.annotation.drawer.PdfObjectDrawerFactory
import androidx.pdf.annotation.highlights.InProgressHighlightsView
import androidx.pdf.annotation.highlights.InProgressTextHighlightsListener
import androidx.pdf.annotation.models.PdfAnnotation

/**
 * A custom Android [FrameLayout] responsible for drawing a collection of annotations onto a Canvas.
 * Each set of page annotations can have its own transformation matrix. It also supports annotating
 * like text highlighting.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AnnotationsView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private val onAnnotationLocatedListeners = mutableListOf<OnAnnotationLocatedListener>()

    /** The view for displaying in-progress annotations (e.g., wet highlights). */
    private val inProgressHighlightsView: InProgressHighlightsView

    private var annotationsLocator: AnnotationsLocator? = null

    init {
        setWillNotDraw(false)

        inProgressHighlightsView =
            InProgressHighlightsView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                visibility = GONE
            }
        addView(inProgressHighlightsView)
    }

    /**
     * Represents of page annotations that will be rendered on the view. The collection is
     * referenced by the page number (0-indexed).
     */
    public var annotations: SparseArray<PageAnnotationsData> = SparseArray()
        set(value) {
            field = value
            invalidate()
        }

    /**
     * The current interaction mode, determining how touch events are handled for annotations.
     *
     * Set to [AnnotationMode.Select] to enable selecting existing annotations, or
     * [AnnotationMode.Highlight] to create new text highlights. If `null`, touch interactions for
     * annotations are disabled on [androidx.pdf.annotation.AnnotationsView] and its children.
     *
     * This property must only be modified on the UI thread.
     */
    public var interactionMode: AnnotationMode? = null
        set(value) {
            checkMainThread()
            field = value
            if (value is AnnotationMode.Highlight) {
                setHighlighter(value.highlighterConfig)
            } else {
                setHighlighter(null)
            }
        }

    /** Provides page information from view coordinates */
    public var pageInfoProvider: PageInfoProvider? = null
        set(value) {
            field = value
            inProgressHighlightsView.pageInfoProvider = value

            if (value != null) {
                annotationsLocator = AnnotationsLocator(context, pageInfoProvider = value)
            }
        }

    /** Adds a listener for highlight gesture events. */
    public fun addInProgressTextHighlightsListener(listener: InProgressTextHighlightsListener) {
        inProgressHighlightsView.addInProgressTextHighlightsListener(listener)
    }

    /** Adds a listener for annotation hit events. */
    public fun addOnAnnotationLocatedListener(listener: OnAnnotationLocatedListener) {
        if (!onAnnotationLocatedListeners.contains(listener)) {
            onAnnotationLocatedListeners.add(listener)
        }
    }

    /** Removes a listener that was previously added via [addInProgressTextHighlightsListener]. */
    public fun removeInProgressTextHighlightsListener(listener: InProgressTextHighlightsListener) {
        inProgressHighlightsView.removeInProgressTextHighlightsListener(listener)
    }

    /** Removes a listener that was previously added via [addOnAnnotationLocatedListener]. */
    public fun removeOnAnnotationLocatedListener(listener: OnAnnotationLocatedListener) {
        onAnnotationLocatedListeners.remove(listener)
    }

    private var pdfObjectDrawerFactory: PdfObjectDrawerFactory = DefaultPdfObjectDrawerFactoryImpl

    private var annotationDrawerFactory: PdfAnnotationDrawerFactory =
        PdfAnnotationDrawerFactoryImpl(pdfObjectDrawerFactory)

    /**
     * Configures the highlighter.
     *
     * @param config The configuration for the highlighter. Pass null to disable.
     */
    private fun setHighlighter(config: HighlighterConfig?) {
        inProgressHighlightsView.apply {
            if (config != null) {
                pdfDocument = config.pdfDocument
                highlightColor = config.color
                visibility = VISIBLE
            } else {
                pdfDocument = null
                visibility = GONE
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        PdfDocumentAnnotationsDrawerImpl(annotationDrawerFactory).draw(annotations, canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (interactionMode) {
            is AnnotationMode.Select -> {
                val localAnnotationsLocator = annotationsLocator
                if (localAnnotationsLocator != null) {
                    val foundAnnotations =
                        localAnnotationsLocator.findAnnotations(annotations, event)
                    if (foundAnnotations.isNotEmpty()) {
                        onAnnotationLocatedListeners.forEach {
                            val event =
                                LocatedAnnotations(x = event.x, y = event.y, foundAnnotations)
                            it.onAnnotationsLocated(event)
                        }
                        return true
                    }
                }
                false
            }
            is AnnotationMode.Highlight -> {
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
    public data class PageAnnotationsData(
        val keyedAnnotations: List<KeyedPdfAnnotation>,
        val transform: Matrix,
    )

    /**
     * Configuration for the highlighter.
     *
     * @param color The color of the highlighter.
     * @param pdfDocument The [PdfDocument], required for text selection.
     */
    public class HighlighterConfig(
        @ColorInt public val color: Int,
        public val pdfDocument: PdfDocument,
    )

    /** Defines the current interaction mode of the [AnnotationsView]. */
    public abstract class AnnotationMode {
        /** Mode for selecting existing annotations (e.g. erase, drag, scale). */
        public class Select : AnnotationMode()

        /** Mode for creating new highlight annotations. */
        public class Highlight(public val highlighterConfig: HighlighterConfig) : AnnotationMode()
    }

    public companion object {
        private fun checkMainThread() {
            check(Looper.myLooper() == Looper.getMainLooper()) {
                "Property must be set on the main thread"
            }
        }
    }
}

/** Callback interface for annotation hit events. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface OnAnnotationLocatedListener {
    public fun onAnnotationsLocated(locatedAnnotations: LocatedAnnotations)
}
