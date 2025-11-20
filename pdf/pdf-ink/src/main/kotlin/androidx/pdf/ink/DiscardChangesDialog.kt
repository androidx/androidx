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

package androidx.pdf.ink

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** A [DialogFragment] to confirm whether unsaved changes should be discarded. */
internal class DiscardChangesDialog() : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .apply {
                setTitle(getString(R.string.discard_changes_dialog_title))
                setMessage(getString(R.string.discard_changes_dialog_message))
                setNegativeButton(getString(R.string.keep_editing_button)) { dialog, _ ->
                    dialog.dismiss()
                }
                setPositiveButton(getString(R.string.discard_button)) { dialog, _ ->
                    parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle.EMPTY)
                    dialog.dismiss()
                }
            }
            .create()
    }

    companion object {
        const val TAG = "DiscardChangesDialog"
        const val REQUEST_KEY = "discardChangesRequest"
    }
}
