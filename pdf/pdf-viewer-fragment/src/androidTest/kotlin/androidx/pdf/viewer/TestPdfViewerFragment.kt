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

package androidx.pdf.viewer

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.annotation.RequiresExtension
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.fragment.PdfStylingOptions
import androidx.pdf.viewer.fragment.PdfViewerFragment
import androidx.pdf.viewer.fragment.test.R
import androidx.pdf.viewer.idlingresource.PdfCountingIdlingResource
import androidx.pdf.viewer.idlingresource.PdfLambdaIdlingResource
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.UUID

/**
 * A subclass fragment from [PdfViewerFragment] to include [androidx.test.espresso.IdlingResource]
 * while loading pdf document.
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
internal class TestPdfViewerFragment : PdfViewerFragment {

    constructor() : super()

    constructor(pdfStylingOptions: PdfStylingOptions) : super(pdfStylingOptions)

    val pdfLoadingIdlingResource = PdfCountingIdlingResource(newPdfLoadingIdlingResourceName())
    val pdfScrollIdlingResource = PdfCountingIdlingResource(PDF_SCROLL_RESOURCE_NAME)
    val pdfSearchFocusIdlingResource = PdfCountingIdlingResource(PDF_SEARCH_FOCUS_RESOURCE_NAME)
    val pdfSearchViewVisibleIdlingResource =
        PdfCountingIdlingResource(PDF_SEARCH_VIEW_VISIBLE_RESOURCE_NAME)
    val pdfPagesFullyRenderedIdlingResource =
        PdfLambdaIdlingResource(PDF_PAGES_FULLY_RENDERED_RESOURCE_NAME) {
            pdfView.arePagesFullyRendered()
        }
    private var hostView: FrameLayout? = null
    private var search: FloatingActionButton? = null

    var documentLoaded = false
    var documentError: Throwable? = null

    private var gestureStateChangedListener: PdfView.OnGestureStateChangedListener? = null

    fun getPdfViewInstance(): PdfView = pdfView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState) as ConstraintLayout

        // Inflate the custom layout for this fragment
        hostView = inflater.inflate(R.layout.fragment_host, container, false) as FrameLayout
        hostView?.let { hostView -> handleInsets(hostView) }

        // Add the default PDF viewer to the custom layout
        hostView?.addView(view)
        return hostView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        search = hostView?.findViewById(R.id.host_Search)

        // Show/hide the search button based on initial toolbox visibility
        if (isToolboxVisible) search?.show() else search?.hide()

        // Set up search button click listener
        search?.setOnClickListener {
            pdfSearchViewVisibleIdlingResource.increment()
            isTextSearchActive = true
        }

        gestureStateChangedListener =
            object : PdfView.OnGestureStateChangedListener {
                    override fun onGestureStateChanged(newState: Int) {
                        if (newState == PdfView.GESTURE_STATE_IDLE) {
                            pdfScrollIdlingResource.decrement()
                        }
                    }
                }
                .also { pdfView.addOnGestureStateChangedListener(it) }

        pdfSearchView.searchQueryBox.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    pdfSearchFocusIdlingResource.decrement()
                }
            }

        pdfSearchView
            .getViewTreeObserver()
            .addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (pdfSearchView.visibility != View.VISIBLE) {
                            pdfSearchViewVisibleIdlingResource.decrement()
                            pdfSearchView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                }
            )
    }

    fun setIsAnnotationIntentResolvable(value: Boolean) {
        setAnnotationIntentResolvability(value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        gestureStateChangedListener?.let { pdfView.removeOnGestureStateChangedListener(it) }
    }

    override fun onRequestImmersiveMode(enterImmersive: Boolean) {
        super.onRequestImmersiveMode(enterImmersive)
        if (!enterImmersive) {
            isToolboxVisible = true
            search?.show()
        } else {
            isToolboxVisible = false
            search?.hide()
        }
    }

    override fun onLoadDocumentSuccess() {
        documentLoaded = true
        pdfLoadingIdlingResource.decrement()
        pdfPagesFullyRenderedIdlingResource.startPolling()
    }

    override fun onLoadDocumentError(error: Throwable) {
        documentError = error
        pdfLoadingIdlingResource.decrement()
    }

    // Callback invoked when the password dialog is requested (i.e., becomes visible).
    override fun onPasswordRequestedState() {
        pdfLoadingIdlingResource.decrement()
    }

    companion object {
        // Resource name must be unique to avoid conflicts while running multiple test scenarios
        private val PDF_SCROLL_RESOURCE_NAME = "PdfScroll-${UUID.randomUUID()}"
        private val PDF_SEARCH_FOCUS_RESOURCE_NAME = "PdfSearchFocus-${UUID.randomUUID()}"
        private val PDF_SEARCH_VIEW_VISIBLE_RESOURCE_NAME =
            "PdfSearchViewVisible-${UUID.randomUUID()}"
        private val PDF_PAGES_FULLY_RENDERED_RESOURCE_NAME =
            "PdfPagesFullyRendered-${UUID.randomUUID()}"

        private fun newPdfLoadingIdlingResourceName(): String = "PdfLoad-${UUID.randomUUID()}"

        fun handleInsets(hostView: View) {
            ViewCompat.setOnApplyWindowInsetsListener(hostView) { view, insets ->
                // Get the insets for the system bars (status bar, navigation bar)
                val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                // Adjust the padding of the container view to accommodate system windows
                view.setPadding(
                    view.paddingLeft,
                    systemBarsInsets.top,
                    view.paddingRight,
                    systemBarsInsets.bottom,
                )
                insets
            }
        }
    }
}
