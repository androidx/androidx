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

package androidx.pdf.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import androidx.pdf.R
import androidx.pdf.featureflag.PdfFeatureFlags

/**
 * A [ViewGroup] that hosts [PdfView] for adding overlays on it using the [ViewGroup.addView]
 * method.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PdfContentLayout(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {
    private var _pdfView: PdfView

    public val pdfView: PdfView
        get() = _pdfView

    /**
     * Controls whether annotation interaction is currently enabled.
     *
     * When set to `true`, [PdfContentLayout] is allowed to intercept touch events and route it to
     * required child. When `false`, touch interception is disable and touch events are passed to
     * the child views(e.g. [PdfView] for scrolling/zooming).
     */
    public var isAnnotationInteractionEnabled: Boolean = false

    init {
        LayoutInflater.from(context).inflate(R.layout.pdf_content_layout, this, true)
        _pdfView = findViewById(R.id.pdfView)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        _pdfView.enableDefaultFastScrollerRendering = false
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (PdfFeatureFlags.isMultiTouchScrollEnabled && _pdfView.isAccessibilityEnabled) {
            _pdfView.fastScrollVisibility = PdfView.FastScrollVisibility.ALWAYS_SHOW
        } else {
            _pdfView.fastScrollVisibility = PdfView.FastScrollVisibility.AUTO_HIDE
        }
        _pdfView.drawFastScroller(canvas)
    }

    override fun onDescendantInvalidated(child: View, target: View) {
        super.onDescendantInvalidated(child, target)
        invalidate()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) {
            return super.onInterceptTouchEvent(ev)
        }
        // Intercept touch events only if annotation interaction is enabled
        return isAnnotationInteractionEnabled
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pdfView.enableDefaultFastScrollerRendering = true
    }
}
