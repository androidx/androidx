/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.pdf.testapp

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.annotation.RestrictTo
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.pdf.viewer.fragment.PdfViewerFragment
import com.google.android.material.button.MaterialButton

@SuppressLint("RestrictedApiAndroidX")
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MainActivity : AppCompatActivity() {

    private var pdfViewerFragment: PdfViewerFragment? = null

    private val filePicker =
        registerForActivityResult(GetContent()) { uri: Uri? ->
            uri?.let { pdfViewerFragment?.documentUri = uri }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (pdfViewerFragment == null) {
            pdfViewerFragment =
                supportFragmentManager.findFragmentByTag(PDF_VIEWER_FRAGMENT_TAG)
                    as PdfViewerFragment?
        }

        val getContentButton: MaterialButton = findViewById(R.id.launch_button)
        val searchButton: MaterialButton = findViewById(R.id.search_button)

        getContentButton.setOnClickListener { filePicker.launch(MIME_TYPE_PDF) }
        searchButton.setOnClickListener { setFindInFileViewVisible() }
        if (savedInstanceState == null) {
            setPdfView()
        }
    }

    private fun setPdfView() {
        val fragmentManager: FragmentManager = supportFragmentManager

        // Fragment initialization
        pdfViewerFragment = PdfViewerFragment()
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()

        // Replace an existing fragment in a container with an instance of a new fragment
        transaction.replace(
            R.id.fragment_container_view,
            pdfViewerFragment!!,
            PDF_VIEWER_FRAGMENT_TAG
        )
        transaction.commitAllowingStateLoss()
        fragmentManager.executePendingTransactions()
    }

    private fun setFindInFileViewVisible() {
        if (pdfViewerFragment != null) {
            pdfViewerFragment!!.isTextSearchActive = true
        }
    }

    companion object {
        private const val MIME_TYPE_PDF = "application/pdf"
        private const val PDF_VIEWER_FRAGMENT_TAG = "pdf_viewer_fragment_tag"
    }
}
