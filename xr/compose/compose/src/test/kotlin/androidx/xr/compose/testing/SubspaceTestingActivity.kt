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

package androidx.xr.compose.testing

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.xr.runtime.manifest.FEATURE_XR_API_SPATIAL
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * A specialized [ComponentActivity] designed to provide a Spatial environment for testing
 * [androidx.xr.compose.subspace.SubspaceComposable] content.
 *
 * By default, this activity mocks the system environment to simulate a device that supports
 * [FEATURE_XR_API_SPATIAL], allowing spatial UI hierarchies to be evaluated in unit tests without
 * requiring a physical XR device or emulator.
 *
 * **Example usage:**
 *
 * ```kotlin
 * @get:Rule
 * val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()
 *
 * @Test
 * fun testSpatialContent() {
 *     composeTestRule.setContent {
 *         Subspace {
 *             SpatialPanel(SubspaceModifier.testTag("panel")) {}
 *         }
 *     }
 *
 *     composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
 * }
 *
 * @Test
 * fun testSpatialContentWhenXrIsDisabled() {
 *     composeTestRule.activity.disableXr()
 *
 *     composeTestRule.setContent {
 *         Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
 *     }
 *
 *     composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
 * }
 * ```
 */
public class SubspaceTestingActivity : ComponentActivity() {
    private val _packageManager: PackageManager = mock<PackageManager>()

    init {
        whenever(_packageManager.hasSystemFeature(FEATURE_XR_API_SPATIAL)).thenReturn(true)
    }

    override fun getPackageManager() = _packageManager

    /**
     * Disables the spatial capabilities for this activity by simulating a device that lacks
     * [FEATURE_XR_API_SPATIAL] support.
     *
     * Invoking this method before calling `setContent` allows testing how the application code
     * degrades or behaves when spatial APIs are unavailable.
     *
     * **Edge Cases and State:**
     * - This method modifies the underlying mock [PackageManager] state.
     * - Call this method before building your spatial composition to ensure the correct XR feature
     *   availability is read upon initialization.
     */
    public fun disableXr() {
        whenever(_packageManager.hasSystemFeature(FEATURE_XR_API_SPATIAL)).thenReturn(false)
    }
}
