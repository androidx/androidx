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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RestrictTo
import androidx.pdf.PdfDocument
import androidx.pdf.R
import androidx.pdf.util.AnnotationUtils
import androidx.pdf.util.Intents.startActivity
import androidx.pdf.util.Uris
import com.google.android.material.floatingactionbutton.FloatingActionButton

@RestrictTo(RestrictTo.Scope.LIBRARY)
public open class ToolBoxView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ViewGroup(context, attrs, defStyleAttr) {

    private val editButton: FloatingActionButton
    private var pdfDocument: PdfDocument? = null
    private var editClickListener: OnClickListener? = null

    /** A callback to get the current page number. */
    private var onCurrentPageRequested: (() -> Int)? = null

    /** Gives the visibility of the toolbox view from edit fab. */
    public val toolboxVisibility: Int
        get() = editButton.visibility

    init {
        inflate(context, R.layout.tool_box_view, this)
        editButton = findViewById(R.id.edit_fab)

        editButton.setOnClickListener { editClickListener?.onClick(this) ?: handleEditFabClick() }
    }

    /**
     * Sets a callback to get the current page number from the [PdfView].
     *
     * @param callback A callback to get the current page number.
     */
    public fun setOnCurrentPageRequested(callback: (() -> Int)?) {
        this.onCurrentPageRequested = callback
    }

    /**
     * Sets the [PdfDocument] that the [ToolBoxView] should use.
     *
     * @param pdfDocument The [PdfDocument] that the [ToolBoxView] should use.
     */
    public fun setPdfDocument(pdfDocument: PdfDocument?) {
        this.pdfDocument = pdfDocument
    }

    /**
     * Sets the drawable that should be used for the edit icon.
     *
     * @param drawable The drawable that should be used for the edit icon.
     */
    public fun setEditIconDrawable(drawable: Drawable?) {
        editButton.setImageDrawable(drawable)
    }

    /**
     * Sets a [OnClickListener] that will be called when the edit FAB is clicked.
     *
     * @param listener The [OnClickListener] that will be called when the edit FAB is clicked.
     */
    public fun setOnEditClickListener(listener: OnClickListener) {
        editClickListener = listener
    }

    private fun handleEditFabClick() {
        val document = pdfDocument ?: return

        if (!AnnotationUtils.resolveAnnotationIntent(context, document.uri)) {
            hideEditFabAndShowToast()
            return
        }

        pdfDocument?.let {
            try {
                val intent = createAnnotationIntent(it.uri)
                startActivity(context, "", intent)
            } catch (error: Exception) {
                when (error) {
                    is NullPointerException,
                    is ActivityNotFoundException -> hideEditFabAndShowToast()
                    else -> throw error
                }
            }
        }
    }

    private fun createAnnotationIntent(uri: Uri): Intent {
        return AnnotationUtils.getAnnotationIntent(uri).apply {
            setData(uri)
            putExtra(EXTRA_PDF_FILE_NAME, Uris.extractName(uri, context.contentResolver))
            val pageNum = onCurrentPageRequested?.invoke() ?: 0
            putExtra(EXTRA_STARTING_PAGE, pageNum)
        }
    }

    private fun hideEditFabAndShowToast() {
        editButton.hide()
        Toast.makeText(
                context,
                context?.resources?.getString(R.string.cannot_edit_pdf),
                Toast.LENGTH_SHORT,
            )
            .show()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val child = getChildAt(0)
        if (child == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        // subtract paddings for calculating available width for child views
        val width = maxOf(0, MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight)
        val height = maxOf(0, MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom)
        // Create measure spec
        child.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.getMode(widthMeasureSpec)),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.getMode(heightMeasureSpec)),
        )
        // Set measurements
        setMeasuredDimension(
            child.measuredWidth + paddingLeft + paddingRight,
            child.measuredHeight + paddingTop + paddingBottom,
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {

        var child = getChildAt(0) ?: return // Return if there's no child
        if (child.visibility == GONE) return // Return if the child is not visible
        child.layout(
            paddingLeft,
            paddingTop,
            (right - paddingRight) - left,
            (bottom - paddingBottom) - top,
        )
    }

    /** Hides the ToolBoxView with a fade-out animation. */
    public fun hide() {
        // When we expand toolbox view by adding new views, we need to update the hide animation
        editButton.hide()
    }

    /** Shows the ToolBoxView with a fade-in animation. */
    public fun show() {
        // When we expand toolbox view by adding new views, we need to update the show animation
        editButton.show()
    }

    public companion object {
        public const val EXTRA_PDF_FILE_NAME: String =
            "androidx.pdf.viewer.fragment.extra.PDF_FILE_NAME"
        public const val EXTRA_STARTING_PAGE: String =
            "androidx.pdf.viewer.fragment.extra.STARTING_PAGE"
    }
}
