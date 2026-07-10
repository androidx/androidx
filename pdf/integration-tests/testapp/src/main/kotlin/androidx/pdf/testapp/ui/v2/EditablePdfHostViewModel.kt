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

import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresExtension
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.pdf.PdfWriteHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
class EditablePdfHostViewModel(private val state: SavedStateHandle) : ViewModel() {
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Ready)
    val saveState = _saveState.asStateFlow()

    private val _isDiscardDialogShown =
        MutableStateFlow(state.get<Boolean>(DISCARD_DIALOG_SHOWN_KEY) ?: false)
    val isDiscardDialogShown = _isDiscardDialogShown.asStateFlow()

    fun showDiscardDialog(isShown: Boolean) {
        _isDiscardDialogShown.value = isShown
        state[DISCARD_DIALOG_SHOWN_KEY] = isShown
    }

    fun saveDocument(handle: PdfWriteHandle, pfd: ParcelFileDescriptor) {
        if (_saveState.value == SaveState.Saving) return

        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                handle.writeTo(pfd)
                handle.close()
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e)
            } finally {
                pfd.close()
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Ready
    }

    companion object {
        private const val DISCARD_DIALOG_SHOWN_KEY = "discard_dialog_shown"
    }
}

sealed interface SaveState {
    data object Ready : SaveState

    data object Saving : SaveState

    data object Success : SaveState

    data class Error(val error: Throwable) : SaveState
}
