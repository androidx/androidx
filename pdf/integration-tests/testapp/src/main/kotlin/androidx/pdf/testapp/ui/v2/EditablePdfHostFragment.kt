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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.pdf.PdfWriteHandle
import androidx.pdf.ink.EditablePdfViewerFragment
import androidx.pdf.ink.R
import androidx.pdf.selection.Selection
import androidx.pdf.selection.model.ImageSelection
import androidx.pdf.testapp.R as testR
import androidx.pdf.view.PdfView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import kotlinx.coroutines.launch

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
class EditablePdfHostFragment : EditablePdfViewerFragment() {
    private val viewModel: EditablePdfHostViewModel by viewModels()

    internal var destinationUri: Uri? = null

    private lateinit var backPressedCallback: OnBackPressedCallback

    private var fragmentListener: FragmentListener? = null

    private val discardDialog: AlertDialog by lazy { createDiscardDialog(requireContext()) }

    private lateinit var loadingProgressBar: ProgressBar
    private var imageSelectionActionMode: ActionMode? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentListener) fragmentListener = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView =
            super.onCreateView(inflater, container, savedInstanceState) as? ConstraintLayout
        loadingProgressBar = createProgressBar()
        rootView?.addView(loadingProgressBar)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBackPressedCallback()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveState.collect { state ->
                when (state) {
                    is SaveState.Success -> {
                        isEditModeEnabled = false
                        viewModel.resetSaveState()
                        loadingProgressBar.isVisible = false
                    }

                    is SaveState.Error -> {
                        viewModel.resetSaveState()
                        loadingProgressBar.isVisible = false
                        Snackbar.make(
                            requireView(),
                            getString(
                                testR.string.write_error_message,
                                state.error.message.toString(),
                            ),
                            Snackbar.LENGTH_SHORT,
                        )
                        // Show error dialog or log error
                    }

                    SaveState.Ready -> {
                        // Re-enable the save button
                        fragmentListener?.onSaveComplete()
                    }
                    SaveState.Saving -> {
                        loadingProgressBar.isVisible = true
                    }
                }
            }
        }

        pdfView.isImageSelectionEnabled = true
        pdfView.isFormFillingEnabled = true
        setupPdfViewListeners()
    }

    private fun setupPdfViewListeners() {
        pdfView.addOnSelectionChangedListener(
            object : PdfView.OnSelectionChangedListener {
                override fun onSelectionChanged(newSelection: Selection?) {
                    if (newSelection == null) {
                        imageSelectionActionMode?.finish()
                        imageSelectionActionMode = null
                        return
                    }

                    isTextSearchActive = false
                    when (newSelection) {
                        is ImageSelection -> onImageSelected(newSelection)
                    }
                }
            }
        )
    }

    private fun onImageSelected(imageSelection: ImageSelection) {
        val context = context ?: return
        val callback =
            ImageSelectionActionModeCallback(
                requireContext(),
                imageSelection,
                lifecycleScope,
                pdfView::pdfToViewPoint,
            )
        imageSelectionActionMode =
            requireActivity().startActionMode(callback, ActionMode.TYPE_FLOATING)
    }

    override fun onApplyEditsSuccess(handle: PdfWriteHandle) {
        super.onApplyEditsSuccess(handle)

        destinationUri?.let { dest ->
            val contentResolver = requireActivity().contentResolver
            val pfd: ParcelFileDescriptor? = getParcelFileDescriptorFromUri(contentResolver, dest)
            pfd?.let { viewModel.saveDocument(handle, it) }
        }
    }

    override fun onApplyEditsFailed(error: Throwable) {
        super.onApplyEditsFailed(error)
        Snackbar.make(
            requireView(),
            getString(testR.string.apply_error_message, error.message.toString()),
            Snackbar.LENGTH_SHORT,
        )
        fragmentListener?.onSaveComplete()
    }

    override fun onEnterEditMode() {
        super.onEnterEditMode()
        backPressedCallback.isEnabled = true
        fragmentListener?.onEnterEditMode()
    }

    override fun onExitEditMode() {
        super.onExitEditMode()
        backPressedCallback.isEnabled = false
        fragmentListener?.onExitEditMode()
    }

    override fun onDetach() {
        super.onDetach()
        fragmentListener = null
    }

    private fun setupBackPressedCallback() {
        backPressedCallback =
            object : OnBackPressedCallback(enabled = false) {
                override fun handleOnBackPressed() {
                    if (hasUnsavedChanges) {
                        discardDialog.show()
                    } else {
                        isEditModeEnabled = false
                    }
                }
            }
        // sync state with edit mode after creation
        backPressedCallback.isEnabled = isEditModeEnabled

        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, backPressedCallback)
    }

    private fun createDiscardDialog(context: Context): AlertDialog =
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.discard_changes_dialog_title))
            .setMessage(getString(R.string.discard_changes_dialog_message))
            .setNegativeButton(getString(R.string.keep_editing_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.discard_button)) { dialog, _ ->
                dialog.dismiss()
                isEditModeEnabled = false
            }
            .create()

    private fun getParcelFileDescriptorFromUri(
        contentResolver: ContentResolver,
        uri: Uri,
    ): ParcelFileDescriptor? {
        return try {
            contentResolver.openFileDescriptor(uri, "rw")
        } catch (e: IOException) {
            null
        }
    }

    private fun createProgressBar(): ProgressBar =
        ProgressBar(requireContext()).apply {
            id = View.generateViewId()
            layoutParams =
                ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    startToStart = PARENT_ID
                    endToEnd = PARENT_ID
                    topToTop = PARENT_ID
                    bottomToBottom = PARENT_ID
                }
            visibility = View.GONE
        }

    /**
     * Interface for the host Activity to listen to state changes and events from the
     * [EditablePdfHostFragment].
     */
    internal interface FragmentListener {
        fun onEnterEditMode()

        fun onExitEditMode()

        fun onSaveComplete()
    }
}
