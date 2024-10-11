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

package androidx.pdf.testapp

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.pdf.viewer.fragment.PdfViewerFragment
import com.google.android.material.button.MaterialButton

@SuppressLint("RestrictedApiAndroidX")
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MultipleFragmentsActivity : AppCompatActivity() {

    private var pdfViewerFragment1: PdfViewerFragment? = null
    private var pdfViewerFragment2: PdfViewerFragment? = null
    private var isPdfViewInitialized1 = false
    private var isPdfViewInitialized2 = false

    @VisibleForTesting
    var filePicker1: ActivityResultLauncher<String> =
        registerForActivityResult(GetContent()) { uri: Uri? ->
            uri?.let {
                if (!isPdfViewInitialized1) {
                    pdfViewerFragment1 = PdfViewerFragment()
                    setPdfView(pdfViewerFragment1, R.id.fragment_container_view1)
                    isPdfViewInitialized1 = true
                }
                pdfViewerFragment1?.documentUri = uri
            }
        }

    @VisibleForTesting
    var filePicker2: ActivityResultLauncher<String> =
        registerForActivityResult(GetContent()) { uri: Uri? ->
            uri?.let {
                if (!isPdfViewInitialized2) {
                    pdfViewerFragment2 = PdfViewerFragment()
                    setPdfView(pdfViewerFragment2, R.id.fragment_container_view2)
                    isPdfViewInitialized2 = true
                }
                pdfViewerFragment2?.documentUri = uri
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiple_fragment)

        if (pdfViewerFragment1 == null) {
            pdfViewerFragment1 =
                supportFragmentManager.findFragmentByTag(PDF_VIEWER_FRAGMENT_TAG)
                    as PdfViewerFragment?
        }
        if (pdfViewerFragment2 == null) {
            pdfViewerFragment2 =
                supportFragmentManager.findFragmentByTag(PDF_VIEWER_FRAGMENT_TAG)
                    as PdfViewerFragment?
        }

        val getContentButton1: MaterialButton = findViewById(R.id.launch_button1)
        val getContentButton2: MaterialButton = findViewById(R.id.launch_button2)
        val searchButton1: MaterialButton = findViewById(R.id.search_button1)
        val searchButton2: MaterialButton = findViewById(R.id.search_button2)

        getContentButton1.setOnClickListener { filePicker1.launch(MIME_TYPE_PDF) }
        getContentButton2.setOnClickListener { filePicker2.launch(MIME_TYPE_PDF) }
        searchButton1.setOnClickListener { setFindInFileViewVisible(pdfViewerFragment1) }
        searchButton2.setOnClickListener { setFindInFileViewVisible(pdfViewerFragment2) }

        handleWindowInsets()
    }

    private fun setPdfView(pdfViewerFragment: PdfViewerFragment?, containerID: Int) {
        val fragmentManager: FragmentManager = supportFragmentManager

        val transaction: FragmentTransaction = fragmentManager.beginTransaction()
        // Replace an existing fragment in a container with an instance of a new fragment
        transaction.replace(containerID, pdfViewerFragment!!, PDF_VIEWER_FRAGMENT_TAG)
        transaction.commitAllowingStateLoss()
        fragmentManager.executePendingTransactions()
    }

    private fun setFindInFileViewVisible(pdfViewerFragment: PdfViewerFragment?) {
        if (pdfViewerFragment != null) {
            pdfViewerFragment.isTextSearchActive = true
        }
    }

    private fun handleWindowInsets() {
        val pdfContainerView: View = findViewById(R.id.pdf_container_view)

        ViewCompat.setOnApplyWindowInsetsListener(pdfContainerView) { view, insets ->
            // Get the insets for the system bars (status bar, navigation bar)
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Adjust the padding of the container view to accommodate system windows
            view.setPadding(
                view.paddingLeft,
                systemBarsInsets.top,
                view.paddingRight,
                systemBarsInsets.bottom
            )

            WindowInsetsCompat.CONSUMED
        }
    }

    companion object {
        private const val MIME_TYPE_PDF = "application/pdf"
        private const val PDF_VIEWER_FRAGMENT_TAG = "pdf_viewer_fragment_tag"
    }
}
