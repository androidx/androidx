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
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresExtension
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.pdf.PdfWriteHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@SuppressLint("RestrictedApiAndroidX")
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
class EditablePdfHostViewModel : ViewModel() {
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Ready)
    val saveState = _saveState.asStateFlow()

    fun saveDocument(handle: PdfWriteHandle, pfd: ParcelFileDescriptor) {
        if (_saveState.value == SaveState.Saving) return

        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                handle.write(pfd)
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
}

sealed interface SaveState {
    data object Ready : SaveState

    data object Saving : SaveState

    data object Success : SaveState

    data class Error(val error: Throwable) : SaveState
}
