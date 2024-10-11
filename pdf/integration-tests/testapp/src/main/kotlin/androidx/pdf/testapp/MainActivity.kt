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
import android.content.Intent
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
import androidx.pdf.testapp.ui.HostFragment
import androidx.pdf.testapp.ui.OpCancellationHandler
import com.google.android.material.button.MaterialButton

@SuppressLint("RestrictedApiAndroidX")
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MainActivity : AppCompatActivity(), OpCancellationHandler {

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
        setContentView(R.layout.activity_main)

        if (pdfViewerFragment == null) {
            pdfViewerFragment =
                supportFragmentManager.findFragmentByTag(PDF_VIEWER_FRAGMENT_TAG) as HostFragment?
        }

        val getContentButton: MaterialButton = findViewById(R.id.launch_button)
        val searchButton: MaterialButton = findViewById(R.id.search_button)
        val doublePDFButton: MaterialButton = findViewById(R.id.double_pdf)

        getContentButton.setOnClickListener { filePicker.launch(MIME_TYPE_PDF) }
        searchButton.setOnClickListener { setFindInFileViewVisible() }
        doublePDFButton.setOnClickListener {
            startActivity(Intent(this, MultipleFragmentsActivity::class.java))
        }

        handleWindowInsets()
    }

    private fun setPdfView() {
        val fragmentManager: FragmentManager = supportFragmentManager

        // Fragment initialization
        pdfViewerFragment = HostFragment()

        // Replace an existing fragment in a container with an instance of a new fragment
        fragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container_view, pdfViewerFragment!!, PDF_VIEWER_FRAGMENT_TAG)
            .commitAllowingStateLoss()

        fragmentManager.executePendingTransactions()
    }

    private fun setFindInFileViewVisible() {
        if (pdfViewerFragment != null) {
            pdfViewerFragment!!.isTextSearchActive = true
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

    override fun handleCancelOperation() {
        // Remove PdfViewerFragment from fragment manager
        supportFragmentManager.findFragmentByTag(PDF_VIEWER_FRAGMENT_TAG)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
        // Mark isPdfViewInitialized to false, so next time it will replace new fragment
        isPdfViewInitialized = false
    }

    companion object {
        private const val MIME_TYPE_PDF = "application/pdf"
        private const val PDF_VIEWER_FRAGMENT_TAG = "pdf_viewer_fragment_tag"
    }
}
