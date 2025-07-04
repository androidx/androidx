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

package androidx.pdf.testapp.ui.v2

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresExtension
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.OperationCanceledException
import androidx.pdf.content.ExternalLink
import androidx.pdf.testapp.ConfigurationProvider
import androidx.pdf.testapp.R
import androidx.pdf.testapp.ui.OpCancellationHandler
import androidx.pdf.viewer.fragment.PdfViewerFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * This fragment extends PdfViewerFragment to provide a custom layout and handle immersive mode. It
 * adds a FloatingActionButton for search functionality and manages its visibility based on the
 * immersive mode state.
 */
@Suppress("RestrictedApiAndroidX")
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class PdfViewerFragmentExtended : PdfViewerFragment() {
    private var hostView: FrameLayout? = null
    private var search: FloatingActionButton? = null
    private var configProvider: ConfigurationProvider? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is ConfigurationProvider) configProvider = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val pdfContainer =
            super.onCreateView(inflater, container, savedInstanceState) as ConstraintLayout

        // Inflate the custom layout for this fragment.
        hostView = inflater.inflate(R.layout.fragment_host, container, false) as FrameLayout
        search = hostView?.findViewById(R.id.host_Search)

        // Add the default PDF viewer to the custom layout
        hostView?.addView(pdfContainer)

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

    override fun onLoadDocumentError(error: Throwable) {
        super.onLoadDocumentError(error)
        when (error) {
            is OperationCanceledException ->
                (activity as? OpCancellationHandler)?.handleCancelOperation()
        }
    }

    override fun onLinkClicked(externalLink: ExternalLink): Boolean {
        return if (configProvider?.behaviourFlags?.customLinkHandlingEnabled == true) {
            AlertDialog.Builder(requireContext())
                .setTitle("Custom Link Handler")
                .setMessage("Intercepted link:\n${externalLink.uri}")
                .setPositiveButton("OK", null)
                .show()
            true
        } else {
            false
        }
    }
}
