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
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentTransaction
import androidx.pdf.testapp.ui.FeatureFlagListener
import androidx.pdf.testapp.ui.FeaturePreferencesDialog
import androidx.pdf.testapp.ui.v2.EditablePdfHostFragment
import androidx.pdf.testapp.ui.v2.PdfViewerFragmentExtended
import androidx.pdf.testapp.ui.v2.StyledPdfViewerFragment
import androidx.pdf.viewer.fragment.PdfViewerFragment
import com.google.android.material.button.MaterialButton

// TODO(b/386721657): Remove this activity once the switch to V2 completes

@SuppressLint("RestrictedApiAndroidX")
@Suppress("NewApi")
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class MainActivityV2 : AppCompatActivity() {

    private lateinit var pdfViewerFragment: PdfViewerFragment

    private lateinit var searchButton: MaterialButton
    private lateinit var openPdfButton: MaterialButton
    private lateinit var undoPdfButton: MaterialButton
    private lateinit var redoPdfButton: MaterialButton
    private lateinit var preferenceButton: ImageButton

    private val settingsDialog: FeaturePreferencesDialog by lazy {
        FeaturePreferencesDialog(this, listener = pdfViewerFragment as? FeatureFlagListener)
    }

    // TODO(b/461991220) : Add the save button in toolbar for EditablePdfViewerFragment.
    private lateinit var savePdfButton: MaterialButton

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    @VisibleForTesting
    private var filePicker: ActivityResultLauncher<String> =
        registerForActivityResult(GetContent()) { uri: Uri? ->
            uri?.let { pdfViewerFragment.documentUri = uri }
        }

    private val createDocumentLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument(MIME_TYPE_PDF)) { uri: Uri?
            ->
            if (uri != null) {
                val editableFragment = pdfViewerFragment as? EditablePdfHostFragment
                editableFragment?.let { fragment ->
                    fragment.destinationUri = uri
                    savePdfButton.isEnabled = false

                    if (!fragment.isApplyEditsInProgress) {
                        fragment.applyDraftEdits()
                    }
                }
            }
        }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            // We're creating the activity for the first time
            pdfViewerFragment = getFragmentForCurrentConfiguration()
            setPdfView()
        } else {
            // We're restoring from a previous session
            pdfViewerFragment =
                supportFragmentManager.findFragmentByTag(PDF_VIEWER_FRAGMENT_TAG)
                    as PdfViewerFragment
        }

        setupViews(pdfViewerFragment)
        handleWindowInsets()

        // Ensure WindowInsetsCompat are passed to content views without being consumed by the decor
        // view.
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    private fun setupViews(pdfViewerFragment: PdfViewerFragment) {
        openPdfButton = findViewById(R.id.launch_button)
        searchButton = findViewById(R.id.search_pdf_button)
        preferenceButton = findViewById(R.id.preference_button)
        savePdfButton = findViewById(R.id.save_pdf_button)
        undoPdfButton = findViewById(R.id.undo_pdf_button)
        redoPdfButton = findViewById(R.id.redo_pdf_button)

        if (pdfViewerFragment is EditablePdfHostFragment) {
            savePdfButton.visibility = View.VISIBLE
            undoPdfButton.visibility = View.VISIBLE
            redoPdfButton.visibility = View.VISIBLE
            pdfViewerFragment.onSaveCompletion = { savePdfButton.isEnabled = true }
        } else {
            savePdfButton.visibility = View.GONE
            undoPdfButton.visibility = View.GONE
            redoPdfButton.visibility = View.GONE
        }

        openPdfButton.setOnClickListener { filePicker.launch(MIME_TYPE_PDF) }

        searchButton.setOnClickListener { pdfViewerFragment.isTextSearchActive = true }

        preferenceButton.setOnClickListener { view -> settingsDialog.show() }
        savePdfButton.setOnClickListener { createDocumentLauncher.launch(SAMPLE_PDF_NAME) }
    }

    private fun setPdfView() {
        pdfViewerFragment.let {
            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            transaction.replace(
                R.id.fragment_container_view,
                pdfViewerFragment,
                PDF_VIEWER_FRAGMENT_TAG,
            )
            transaction.commitAllowingStateLoss()
            supportFragmentManager.executePendingTransactions()
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    private fun getFragmentForCurrentConfiguration(): PdfViewerFragment {
        val fragmentType = getFragmentTypeFromIntent()

        return when (fragmentType) {
            FragmentType.BASIC_FRAGMENT -> PdfViewerFragmentExtended()
            FragmentType.STYLED_FRAGMENT -> StyledPdfViewerFragment.newInstance()
            FragmentType.EDITABLE_FRAGMENT -> {
                EditablePdfHostFragment()
            }
        }
    }

    private fun getFragmentTypeFromIntent(): FragmentType {
        return intent.extras?.let {
            BundleCompat.getSerializable(it, FRAGMENT_TYPE_KEY, FragmentType::class.java)
        } ?: FragmentType.BASIC_FRAGMENT
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
                systemBarsInsets.bottom,
            )

            insets
        }
    }

    companion object {
        private const val MIME_TYPE_PDF = "application/pdf"
        private const val PDF_VIEWER_FRAGMENT_TAG = "pdf_viewer_fragment_tag"
        internal const val FRAGMENT_TYPE_KEY = "fragmentTypeKey"
        private const val SAMPLE_PDF_NAME = "Sample.pdf"

        internal enum class FragmentType {
            BASIC_FRAGMENT,
            STYLED_FRAGMENT,
            EDITABLE_FRAGMENT,
        }
    }
}
