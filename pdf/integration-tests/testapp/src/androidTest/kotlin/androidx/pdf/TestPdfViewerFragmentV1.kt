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

package androidx.pdf

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresExtension
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.pdf.idlingresource.PdfIdlingResource
import androidx.pdf.metrics.EventCallback
import androidx.pdf.testapp.R
import androidx.pdf.viewer.fragment.PdfStylingOptions
import androidx.pdf.viewer.fragment.PdfViewerFragmentV1
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.UUID

/**
 * A subclass fragment from [PdfViewerFragmentV1] to include [androidx.test.espresso.IdlingResource]
 * while loading pdf document.
 *
 * TODO(b/386721657) Remove this when PdfViewerFragment is replaced with PdfViewerFragmentV2.
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
internal class TestPdfViewerFragmentV1 : PdfViewerFragmentV1 {

    constructor() : super()

    constructor(pdfStylingOptions: PdfStylingOptions) : super(pdfStylingOptions)

    private var hostView: ConstraintLayout? = null
    private var search: FloatingActionButton? = null
    val pdfLoadingIdlingResource = PdfIdlingResource(PDF_LOAD_RESOURCE_NAME)

    var documentLoaded = false
    var documentError: Throwable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setEventCallback(
            object : EventCallback {
                override fun onPasswordRequested() {
                    pdfLoadingIdlingResource.decrement()
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState) as FrameLayout

        // Inflate the custom layout for this fragment
        hostView = inflater.inflate(R.layout.fragment_host, container, false) as ConstraintLayout
        search = hostView?.findViewById(R.id.host_Search)

        hostView?.let { hostView -> handleInsets(hostView) }

        // Add the default PDF viewer to the custom layout
        hostView?.addView(view)

        // Show/hide the search button based on initial toolbox visibility
        if (isToolboxVisible) search?.show() else search?.hide()

        // Set up search button click listener
        search?.setOnClickListener { isTextSearchActive = true }
        return hostView
    }

    override fun onRequestImmersiveMode(enterImmersive: Boolean) {
        super.onRequestImmersiveMode(enterImmersive)
        if (!enterImmersive) search?.show() else search?.hide()
    }

    override fun onLoadDocumentSuccess() {
        documentLoaded = true
        pdfLoadingIdlingResource.decrement()
    }

    override fun onLoadDocumentError(error: Throwable) {
        documentError = error
        pdfLoadingIdlingResource.decrement()
    }

    companion object {
        // It is vital to keep the resource name unique for each test scenario as it conflicts in
        // parallel runs during increment and decrement making tests flaky.
        private val PDF_LOAD_RESOURCE_NAME = UUID.randomUUID().toString()

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
