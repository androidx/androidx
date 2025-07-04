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

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.pdf.testapp.R
import androidx.pdf.testapp.databinding.BasicPdfFragmentBinding
import androidx.pdf.testapp.ui.OpCancellationHandler
import androidx.pdf.viewer.fragment.PdfViewerFragment
import com.google.android.material.button.MaterialButton

@SuppressLint("RestrictedApiAndroidX")
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ViewerFragment : Fragment(), OpCancellationHandler {

    private var pdfViewerFragment: PdfViewerFragment? = null
    private var isPdfViewInitialized = false

    @VisibleForTesting
    var filePicker: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { setDocumentUri(uri) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pdfViewerFragment =
            childFragmentManager.findFragmentByTag(PDF_VIEWER_FRAGMENT_TAG) as PdfViewerFragment?
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val pdfInteraction = BasicPdfFragmentBinding.inflate(inflater, container, false)
        val getContentButton: MaterialButton = pdfInteraction.openPdf
        val searchButton: MaterialButton = pdfInteraction.searchButton

        getContentButton.setOnClickListener { filePicker.launch(MIME_TYPE_PDF) }
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            searchButton.setOnClickListener { setFindInFileViewVisible() }
        }
        return pdfInteraction.root
    }

    private fun sendIntentToOpenPdf(uri: Uri) {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
            }
        val chooser = Intent.createChooser(intent, "Open PDF")
        startActivity(chooser)
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    private fun setPdfView() {
        val fragmentManager: FragmentManager = childFragmentManager

        // Fragment initialization
        val fragmentType =
            arguments?.let {
                BundleCompat.getSerializable<FragmentType>(
                    it,
                    FRAGMENT_TYPE_KEY,
                    FragmentType::class.java,
                )
            }

        pdfViewerFragment =
            when (fragmentType) {
                FragmentType.BASIC_FRAGMENT -> PdfViewerFragmentExtended()
                FragmentType.STYLED_FRAGMENT -> StyledPdfViewerFragment.newInstance()
                else -> PdfViewerFragmentExtended()
            }

        // Replace an existing fragment in a container with an instance of a new fragment
        fragmentManager
            .beginTransaction()
            .replace(R.id.pdf_fragment_container_view, pdfViewerFragment!!, PDF_VIEWER_FRAGMENT_TAG)
            .commitNow()

        fragmentManager.executePendingTransactions()
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    private fun setFindInFileViewVisible() {
        if (pdfViewerFragment != null) {
            pdfViewerFragment!!.isTextSearchActive = true
        }
    }

    @VisibleForTesting
    public fun setDocumentUri(uri: Uri) {
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            if (!isPdfViewInitialized) {
                setPdfView()
                isPdfViewInitialized = true
            }
            pdfViewerFragment?.documentUri = uri
        } else {
            /**
             * Send an intent to other apps who support opening PDFs in case PdfViewer library is
             * not supported due to SdkExtension limitations.
             */
            sendIntentToOpenPdf(uri)
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
        private const val FRAGMENT_TYPE_KEY = "fragmentTypeKey"

        enum class FragmentType {
            BASIC_FRAGMENT,
            STYLED_FRAGMENT,
        }

        fun newInstance(fragmentType: FragmentType = FragmentType.BASIC_FRAGMENT): ViewerFragment {
            val fragment = ViewerFragment()
            val args = Bundle().also { it.putSerializable(FRAGMENT_TYPE_KEY, fragmentType) }
            fragment.arguments = args

            return fragment
        }
    }
}
