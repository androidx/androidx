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
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.pdf.PdfWriteHandle
import androidx.pdf.ink.EditablePdfViewerFragment
import androidx.pdf.ink.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException
import kotlinx.coroutines.launch

@SuppressLint("RestrictedApiAndroidX")
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
class EditablePdfHostFragment : EditablePdfViewerFragment() {
    private val viewModel: EditablePdfHostViewModel by viewModels()

    internal var destinationUri: Uri? = null

    private lateinit var backPressedCallback: OnBackPressedCallback

    private var fragmentListener: FragmentListener? = null

    private val discardDialog: AlertDialog by lazy { createDiscardDialog(requireContext()) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentListener) fragmentListener = context
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
                    }

                    is SaveState.Error -> {
                        viewModel.resetSaveState()
                        // Show error dialog or log error
                    }

                    SaveState.Ready -> {
                        // Re-enable the save button
                        fragmentListener?.onSaveComplete()
                    }
                    SaveState.Saving -> {
                        // No-op or show loading indicator
                    }
                }
            }
        }
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
