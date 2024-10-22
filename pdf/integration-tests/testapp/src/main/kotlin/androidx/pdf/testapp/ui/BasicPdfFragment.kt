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

package androidx.pdf.testapp.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.pdf.testapp.R
import androidx.pdf.testapp.databinding.BasicPdfFragmentBinding
import com.google.android.material.button.MaterialButton

@SuppressLint("RestrictedApiAndroidX")
@RestrictTo(RestrictTo.Scope.LIBRARY)
class BasicPdfFragment : Fragment(), OpCancellationHandler {

    private var pdfViewerFragment: HostFragment? = null
    private var isPdfViewInitialized = false

    @VisibleForTesting
    var filePicker: ActivityResultLauncher<String> =
        registerForActivityResult(GetContent()) { uri: Uri? ->
            uri?.let {
                if (!isPdfViewInitialized) {
                    setPdfView()
                    isPdfViewInitialized = true
                }
                pdfViewerFragment?.documentUri = uri
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (pdfViewerFragment == null) {
            pdfViewerFragment =
                childFragmentManager.findFragmentByTag(PDF_VIEWER_FRAGMENT_TAG) as HostFragment?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val pdfInteraction = BasicPdfFragmentBinding.inflate(inflater, container, false)
        val getContentButton: MaterialButton = pdfInteraction.openPdf
        val searchButton: MaterialButton = pdfInteraction.searchButton

        getContentButton.setOnClickListener { filePicker.launch(MIME_TYPE_PDF) }
        searchButton.setOnClickListener { setFindInFileViewVisible() }

        return pdfInteraction.root
    }

    private fun setPdfView() {
        val fragmentManager: FragmentManager = childFragmentManager

        // Fragment initialization
        pdfViewerFragment = HostFragment()

        // Replace an existing fragment in a container with an instance of a new fragment
        fragmentManager
            .beginTransaction()
            .replace(R.id.pdf_fragment_container_view, pdfViewerFragment!!, PDF_VIEWER_FRAGMENT_TAG)
            .commit()

        fragmentManager.executePendingTransactions()
    }

    private fun setFindInFileViewVisible() {
        if (pdfViewerFragment != null) {
            pdfViewerFragment!!.isTextSearchActive = true
        }
    }

    override fun handleCancelOperation() {
        // Remove PdfViewerFragment from fragment manager
        childFragmentManager.findFragmentByTag(PDF_VIEWER_FRAGMENT_TAG)?.let {
            childFragmentManager.beginTransaction().remove(it).commit()
        }
        // Mark isPdfViewInitialized to false, so next time it will replace new fragment
        isPdfViewInitialized = false
    }

    companion object {
        private const val MIME_TYPE_PDF = "application/pdf"
        private const val PDF_VIEWER_FRAGMENT_TAG = "pdf_viewer_fragment_tag"
    }
}
