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

package androidx.xr.compose.testapp

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialGltfModel
import androidx.xr.compose.subspace.SpatialGltfModelSource
import androidx.xr.compose.subspace.SpatialGltfModelState
import androidx.xr.compose.subspace.SpatialGltfModelStatus
import androidx.xr.compose.subspace.rememberSpatialGltfModelState
import androidx.xr.testutils.XrDeviceConfig
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for [SpatialGltfModel] verifying that 3D glTF assets render correctly within a
 * Subspace.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SpatialGltfModelTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<EmptyActivity>()

    /**
     * Verifies that a glTF asset loads and renders successfully.
     *
     * This test serves as a critical integration check for asynchronous session initialization.
     * Historically, initializing the Session on a background thread could cause thread-boundary
     * crashes in apps using impress features (such as glTF model loaders). Verifying successful
     * glTF rendering ensures that asynchronous session creation remains fully thread-safe and
     * stable.
     *
     * TODO: b/519600136 - Look into testing Session initialization directly.
     */
    @Test
    @XrDeviceTest(include = [XrDeviceConfig.PHYSICAL_DEVICE])
    fun spatialGltfModel_loadsSuccessfully() {
        var state: SpatialGltfModelState? = null

        composeTestRule.setContent {
            Subspace {
                val rememberedState =
                    rememberSpatialGltfModelState(
                        source =
                            SpatialGltfModelSource.fromPath(
                                Paths.get("models", "Dragon_Evolved.gltf")
                            )
                    )
                state = rememberedState
                SpatialGltfModel(state = rememberedState)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            val currentState = state
            currentState != null &&
                (currentState.status is SpatialGltfModelStatus.Loaded ||
                    currentState.status is SpatialGltfModelStatus.Failed)
        }

        assertThat(state?.status).isEqualTo(SpatialGltfModelStatus.Loaded)
    }
}
