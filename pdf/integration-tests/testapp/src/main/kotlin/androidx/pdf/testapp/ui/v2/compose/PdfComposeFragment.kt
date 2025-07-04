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

package androidx.pdf.testapp.ui.v2.compose

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.pdf.compose.PdfViewer
import androidx.pdf.compose.PdfViewerState
import androidx.pdf.testapp.databinding.FragmentComposeBinding

/**
 * [Fragment] with a [androidx.compose.runtime.Composable] content view, for testing of [PdfViewer]
 *
 * This does not extend [androidx.pdf.viewer.fragment.PdfViewerFragment] which expects certain
 * [View]s to exist in the layout. This will follow the recommended UDF architecture for Compose-
 * based UIs. The Fragment is simply a container for Compose in an otherwise View-based app.
 */
@SuppressLint("RestrictedApiAndroidX")
class PdfComposeFragment() : Fragment() {
    private lateinit var composeView: ComposeView

    private val viewModel: PdfComposeViewModel by viewModels { PdfComposeViewModel.Factory }

    private val filePicker: ActivityResultLauncher<String> =
        registerForActivityResult(GetContent()) { viewModel.documentUri = it }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = FragmentComposeBinding.inflate(inflater, container, /* attachToParent= */ false)
        view.openPdf.setOnClickListener { _ -> filePicker.launch(MIME_TYPE_PDF) }
        composeView =
            view.composeView.apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    val pdfViewerState = remember { PdfViewerState() }
                    PdfViewer(
                        state = pdfViewerState,
                        pdfDocument =
                            viewModel.loadedDocumentStateFlow.collectAsStateWithLifecycle().value,
                    )
                }
            }

        return view.root
    }

    companion object {
        private const val MIME_TYPE_PDF = "application/pdf"
    }
}
