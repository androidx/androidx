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

package androidx.pdf.testapp.ui

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
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.annotation.RequiresExtension
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.pdf.testapp.databinding.FragmentXmlStyledPdfBinding
import androidx.pdf.viewer.fragment.PdfViewerFragment
import com.google.android.material.button.MaterialButton

@SuppressLint("RestrictedApiAndroidX")
class XmlStyledPdfFragment : Fragment() {

    private lateinit var binding: FragmentXmlStyledPdfBinding

    @VisibleForTesting
    var filePicker: ActivityResultLauncher<String> =
        registerForActivityResult(GetContent()) { uri: Uri? -> uri?.let { setDocumentUri(uri) } }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentXmlStyledPdfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val getContentButton: MaterialButton = binding.openPdf

        getContentButton.setOnClickListener { filePicker.launch(MIME_TYPE_PDF) }
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            binding.searchButton.setOnClickListener { setFindInFileViewVisible() }
        }
    }

    private fun setDocumentUri(uri: Uri) {
        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            binding.pdfStyledFragment.getFragment<PdfViewerFragment>().documentUri = uri
        } else {
            /**
             * Send an intent to other apps who support opening PDFs in case PdfViewer library is
             * not supported due to SdkExtension limitations.
             */
            sendIntentToOpenPdf(uri)
        }
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
    private fun setFindInFileViewVisible() {
        binding.pdfStyledFragment.getFragment<PdfViewerFragment>().isTextSearchActive = true
    }

    companion object {
        private const val MIME_TYPE_PDF = "application/pdf"
    }
}
