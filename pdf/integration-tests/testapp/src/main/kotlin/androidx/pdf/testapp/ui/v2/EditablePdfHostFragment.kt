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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.annotation.RequiresExtension
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.pdf.PdfWriteHandle
import androidx.pdf.ink.EditablePdfViewerFragment
import java.io.IOException
import kotlinx.coroutines.launch

@SuppressLint("RestrictedApiAndroidX")
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
class EditablePdfHostFragment : EditablePdfViewerFragment() {
    private val viewModel: EditablePdfHostViewModel by viewModels()

    internal var destinationUri: Uri? = null
    internal var onSaveCompletion: () -> Unit = {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                        onSaveCompletion()
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
        onSaveCompletion()
    }

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
}
