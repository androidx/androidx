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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.xr.scenecore.testapp.common.SpatialMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class VisibilityViewModelTest {

    @Test
    fun initialState_hasDefaultValues() {
        val viewModel = VisibilityViewModel()
        val state = viewModel.uiState.value

        assertThat(state.spatialMode).isEqualTo(SpatialMode.FSM)
        assertThat(state.isHideAllChecked).isFalse()
        assertThat(state.isParentGltfHidden).isFalse()
        assertThat(state.isChildGltf1Hidden).isFalse()
        assertThat(state.isChildGltf2Hidden).isFalse()
        assertThat(state.isParentPanelHidden).isFalse()
        assertThat(state.isChildPanel1Hidden).isFalse()
        assertThat(state.isChildPanel2Hidden).isFalse()
        assertThat(state.isPanel1PointerHidden).isFalse()
        assertThat(state.isActivitySpaceTemporarilyHidden).isFalse()
        assertThat(state.isMainPanelTemporarilyHidden).isFalse()
    }

    @Test
    fun toggleEntityVisibilities_updatesUiState() {
        val viewModel = VisibilityViewModel()

        viewModel.setParentGltfHidden(true)
        viewModel.setChildGltf1Hidden(true)
        viewModel.setChildGltf2Hidden(true)
        viewModel.setParentPanelHidden(true)
        viewModel.setChildPanel1Hidden(true)
        viewModel.setChildPanel2Hidden(true)
        viewModel.setPanel1PointerHidden(true)

        val state = viewModel.uiState.value
        assertThat(state.isParentGltfHidden).isTrue()
        assertThat(state.isChildGltf1Hidden).isTrue()
        assertThat(state.isChildGltf2Hidden).isTrue()
        assertThat(state.isParentPanelHidden).isTrue()
        assertThat(state.isChildPanel1Hidden).isTrue()
        assertThat(state.isChildPanel2Hidden).isTrue()
        assertThat(state.isPanel1PointerHidden).isTrue()
    }

    @Test
    fun setHideAllChecked_updatesUiStateWithoutChangingIndividualToggles() {
        val viewModel = VisibilityViewModel()

        viewModel.setParentGltfHidden(true)
        viewModel.setChildGltf1Hidden(false)
        viewModel.setHideAllChecked(true)

        val state = viewModel.uiState.value
        assertThat(state.isHideAllChecked).isTrue()
        // Individual toggles remain unchanged in state
        assertThat(state.isParentGltfHidden).isTrue()
        assertThat(state.isChildGltf1Hidden).isFalse()
    }

    @Test
    fun setSpatialMode_updatesSpatialModeInState() {
        val viewModel = VisibilityViewModel()

        viewModel.setSpatialMode(SpatialMode.HSM)
        assertThat(viewModel.uiState.value.spatialMode).isEqualTo(SpatialMode.HSM)
    }
}
