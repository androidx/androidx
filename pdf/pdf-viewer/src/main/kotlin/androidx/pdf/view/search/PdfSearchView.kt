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

package androidx.pdf.view.search

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.pdf.R

/**
 * A [View] that provides a search UI for searching through a PDF's content.
 *
 * This view acts as a composite view for the search box, previous/next buttons, and an indicator
 * for displaying the match count status.
 *
 * It exposes primary widgets as an API surface, which can be used to customize the appearance and
 * behavior of the view.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PdfSearchView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    ViewGroup(context, attrs, defStyle) {

    /** Query box for search query represented by AppCompatEditText */
    public val searchQueryBox: EditText

    /** Match status count TextView */
    public val matchStatusTextView: TextView

    /** Previous button to iterate search results in reverse direction */
    public val findPrevButton: ImageButton

    /** Next button to iterate search results in forward direction */
    public val findNextButton: ImageButton

    /** Close button to dismiss pdf search view */
    public val closeButton: ImageButton

    init {
        // Inflate the layout
        View.inflate(context, R.layout.pdf_search_view, this)

        // Assign views
        searchQueryBox = findViewById(R.id.searchQueryBox)
        matchStatusTextView = findViewById(R.id.matchStatusTextView)
        findPrevButton = findViewById(R.id.findPrevButton)
        findNextButton = findViewById(R.id.findNextButton)
        closeButton = findViewById(R.id.closeButton)
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

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        getChildAt(0).also { child ->
            // delegate layout to root view group
            child.layout(
                paddingLeft,
                paddingTop,
                right - left - paddingRight,
                bottom - top - paddingBottom,
            )
        }
    }
}
