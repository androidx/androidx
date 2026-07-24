/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.testapp.visibility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.xr.scenecore.testapp.common.SpatialMode
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI State for [VisibilityActivity]. */
data class VisibilityUiState(
    val spatialMode: SpatialMode = SpatialMode.FSM,
    val isHideAllChecked: Boolean = false,
    val isParentGltfHidden: Boolean = false,
    val isChildGltf1Hidden: Boolean = false,
    val isChildGltf2Hidden: Boolean = false,
    val isParentPanelHidden: Boolean = false,
    val isChildPanel1Hidden: Boolean = false,
    val isChildPanel2Hidden: Boolean = false,
    val isPanel1PointerHidden: Boolean = false,
    val isActivitySpaceTemporarilyHidden: Boolean = false,
    val isMainPanelTemporarilyHidden: Boolean = false,
)

/**
 * ViewModel for [VisibilityActivity] that preserves visibility settings across activity
 * recreations.
 */
class VisibilityViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VisibilityUiState())
    val uiState: StateFlow<VisibilityUiState> = _uiState.asStateFlow()

    private var hideActivitySpaceJob: Job? = null
    private var hideMainPanelJob: Job? = null

    fun setSpatialMode(spatialMode: SpatialMode) {
        _uiState.update { it.copy(spatialMode = spatialMode) }
    }

    fun setHideAllChecked(isChecked: Boolean) {
        _uiState.update { it.copy(isHideAllChecked = isChecked) }
    }

    fun setParentGltfHidden(isHidden: Boolean) {
        _uiState.update { it.copy(isParentGltfHidden = isHidden) }
    }

    fun setChildGltf1Hidden(isHidden: Boolean) {
        _uiState.update { it.copy(isChildGltf1Hidden = isHidden) }
    }

    fun setChildGltf2Hidden(isHidden: Boolean) {
        _uiState.update { it.copy(isChildGltf2Hidden = isHidden) }
    }

    fun setParentPanelHidden(isHidden: Boolean) {
        _uiState.update { it.copy(isParentPanelHidden = isHidden) }
    }

    fun setChildPanel1Hidden(isHidden: Boolean) {
        _uiState.update { it.copy(isChildPanel1Hidden = isHidden) }
    }

    fun setChildPanel2Hidden(isHidden: Boolean) {
        _uiState.update { it.copy(isChildPanel2Hidden = isHidden) }
    }

    fun setPanel1PointerHidden(isHidden: Boolean) {
        _uiState.update { it.copy(isPanel1PointerHidden = isHidden) }
    }

    fun hideActivitySpaceTemporarily(durationMs: Long = DEFAULT_DELAY_MS) {
        hideActivitySpaceJob?.cancel()
        hideActivitySpaceJob =
            viewModelScope.launch {
                _uiState.update { it.copy(isActivitySpaceTemporarilyHidden = true) }
                delay(durationMs.milliseconds)
                _uiState.update { it.copy(isActivitySpaceTemporarilyHidden = false) }
            }
    }

    fun hideMainPanelTemporarily(durationMs: Long = DEFAULT_DELAY_MS) {
        hideMainPanelJob?.cancel()
        hideMainPanelJob =
            viewModelScope.launch {
                _uiState.update { it.copy(isMainPanelTemporarilyHidden = true) }
                delay(durationMs.milliseconds)
                _uiState.update { it.copy(isMainPanelTemporarilyHidden = false) }
            }
    }

    companion object {
        const val DEFAULT_DELAY_MS: Long = 3000L
    }
}
