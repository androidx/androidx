/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.platform

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.createFakeRuntime
import androidx.xr.compose.testing.createFakeSession
import androidx.xr.compose.testing.disableXr
import androidx.xr.compose.testing.session
import androidx.xr.scenecore.scene
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpatialCapabilitiesTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    private val spatialUiEnabledText = "Spatial UI Enabled"
    private val content3dEnabledText = "3D Content Enabled"
    private val appEnvironmentEnabledText = "App Environment Enabled"
    private val passthroughControlEnabledText = "Passthrough Control Enabled"
    private val spatialAudioEnabledText = "Spatial Audio Enabled"

    @Test
    fun isSpatialUiEnabled_xrNotEnabled_returnsFalse() {
        composeTestRule.disableXr()

        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                Text(spatialUiEnabledText)
            }
        }

        composeTestRule.onNodeWithText(spatialUiEnabledText).assertDoesNotExist()
    }

    @Test
    fun isContent3dEnabled_xrNotEnabled_returnsFalse() {
        composeTestRule.disableXr()

        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isContent3dEnabled) {
                Text(content3dEnabledText)
            }
        }

        composeTestRule.onNodeWithText(content3dEnabledText).assertDoesNotExist()
    }

    @Test
    fun isAppEnvironmentEnabled_xrNotEnabled_returnsFalse() {
        composeTestRule.disableXr()

        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isAppEnvironmentEnabled) {
                Text(appEnvironmentEnabledText)
            }
        }

        composeTestRule.onNodeWithText(appEnvironmentEnabledText).assertDoesNotExist()
    }

    @Test
    fun isPassthroughControlEnabled_xrNotEnabled_returnsFalse() {
        composeTestRule.disableXr()

        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isPassthroughControlEnabled) {
                Text(passthroughControlEnabledText)
            }
        }

        composeTestRule.onNodeWithText(passthroughControlEnabledText).assertDoesNotExist()
    }

    @Test
    fun isSpatialAudioEnabled_xrNotEnabled_returnsFalse() {
        composeTestRule.disableXr()

        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isSpatialAudioEnabled) {
                Text(spatialAudioEnabledText)
            }
        }

        composeTestRule.onNodeWithText(spatialAudioEnabledText).assertDoesNotExist()
    }

    @Test
    fun isSpatialUiEnabled_fullSpaceMode_returnsTrue() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                Text(spatialUiEnabledText)
            }
        }

        composeTestRule.onNodeWithText(spatialUiEnabledText).assertExists()
    }

    @Test
    fun isContent3dEnabled_fullSpaceMode_returnsTrue() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isContent3dEnabled) {
                Text(content3dEnabledText)
            }
        }

        composeTestRule.onNodeWithText(content3dEnabledText).assertExists()
    }

    @Test
    fun isAppEnvironmentEnabled_fullSpaceMode_returnsTrue() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isAppEnvironmentEnabled) {
                Text(appEnvironmentEnabledText)
            }
        }

        composeTestRule.onNodeWithText(appEnvironmentEnabledText).assertExists()
    }

    @Test
    fun isPassthroughControlEnabled_fullSpaceMode_returnsTrue() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isPassthroughControlEnabled) {
                Text(passthroughControlEnabledText)
            }
        }

        composeTestRule.onNodeWithText(passthroughControlEnabledText).assertExists()
    }

    @Test
    fun isSpatialAudioEnabled_fullSpaceMode_returnsTrue() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isSpatialAudioEnabled) {
                Text(spatialAudioEnabledText)
            }
        }

        composeTestRule.onNodeWithText(spatialAudioEnabledText).assertExists()
    }

    @Test
    fun isSpatialUiEnabled_homeSpaceMode_returnsFalse() {
        composeTestRule.session =
            createFakeSession(composeTestRule.activity).apply { scene.requestHomeSpaceMode() }

        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                Text(spatialUiEnabledText)
            }
        }

        composeTestRule.onNodeWithText(spatialUiEnabledText).assertDoesNotExist()
    }

    @Test
    fun isSpatialUiEnabled_homeSpaceMode_requestFullSpaceMode_returnsTrue() {
        val runtime = createFakeRuntime(composeTestRule.activity)
        runtime.requestHomeSpaceMode()
        composeTestRule.session = createFakeSession(composeTestRule.activity, runtime)

        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                Text(spatialUiEnabledText)
            }
            LocalSession.current?.scene?.requestFullSpaceMode()
        }

        composeTestRule.onNodeWithText(spatialUiEnabledText).assertExists()
    }

    @Test
    fun isSpatialUiEnabled_fullSpaceMode_requestHomeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
                Text(spatialUiEnabledText)
            }
            LocalSession.current?.scene?.requestHomeSpaceMode()
        }

        composeTestRule.onNodeWithText(spatialUiEnabledText).assertDoesNotExist()
    }

    @Test
    fun isContent3dEnabled_homeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isContent3dEnabled) {
                Text(content3dEnabledText)
            }
            LocalSession.current?.scene?.requestHomeSpaceMode()
        }

        composeTestRule.onNodeWithText(content3dEnabledText).assertDoesNotExist()
    }

    @Test
    fun isContent3dEnabled_homeSpaceMode_requestFullSpaceMode_returnsTrue() {
        composeTestRule.session =
            createFakeSession(composeTestRule.activity).apply { scene.requestHomeSpaceMode() }

        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isContent3dEnabled) {
                Text(content3dEnabledText)
            }
            LocalSession.current?.scene?.requestFullSpaceMode()
        }

        composeTestRule.onNodeWithText(content3dEnabledText).assertExists()
    }

    @Test
    fun isContent3dEnabled_fullSpaceMode_requestHomeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isContent3dEnabled) {
                Text(content3dEnabledText)
            }
            LocalSession.current?.scene?.requestHomeSpaceMode()
        }

        composeTestRule.onNodeWithText(content3dEnabledText).assertDoesNotExist()
    }

    @Test
    fun isAppEnvironmentEnabled_homeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isAppEnvironmentEnabled) {
                Text(appEnvironmentEnabledText)
            }
            LocalSession.current?.scene?.requestHomeSpaceMode()
        }

        composeTestRule.onNodeWithText(appEnvironmentEnabledText).assertDoesNotExist()
    }

    @Test
    fun isAppEnvironmentEnabled_homeSpaceMode_requestFullSpaceMode_returnsTrue() {
        composeTestRule.session =
            createFakeSession(composeTestRule.activity).apply { scene.requestHomeSpaceMode() }

        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isAppEnvironmentEnabled) {
                Text(appEnvironmentEnabledText)
            }
            LocalSession.current?.scene?.requestFullSpaceMode()
        }

        composeTestRule.onNodeWithText(appEnvironmentEnabledText).assertExists()
    }

    @Test
    fun isAppEnvironmentEnabled_fullSpaceMode_requestHomeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isAppEnvironmentEnabled) {
                Text(appEnvironmentEnabledText)
            }
            LocalSession.current?.scene?.requestHomeSpaceMode()
        }

        composeTestRule.onNodeWithText(appEnvironmentEnabledText).assertDoesNotExist()
    }

    @Test
    fun isPassthroughControlEnabled_homeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isPassthroughControlEnabled) {
                Text(passthroughControlEnabledText)
            }
            LocalSession.current?.scene?.requestHomeSpaceMode()
        }

        composeTestRule.onNodeWithText(passthroughControlEnabledText).assertDoesNotExist()
    }

    @Test
    fun isPassthroughControlEnabled_homeSpaceMode_requestFullSpaceMode_returnsTrue() {
        composeTestRule.session =
            createFakeSession(composeTestRule.activity).apply { scene.requestHomeSpaceMode() }

        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isPassthroughControlEnabled) {
                Text(passthroughControlEnabledText)
            }
            LocalSession.current?.scene?.requestFullSpaceMode()
        }

        composeTestRule.onNodeWithText(passthroughControlEnabledText).assertExists()
    }

    @Test
    fun isPassthroughControlEnabled_fullSpaceMode_requestHomeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isPassthroughControlEnabled) {
                Text(passthroughControlEnabledText)
            }
            LocalSession.current?.scene?.requestHomeSpaceMode()
        }

        composeTestRule.onNodeWithText(passthroughControlEnabledText).assertDoesNotExist()
    }

    @Test
    fun isSpatialAudioEnabled_homeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isSpatialAudioEnabled) {
                Text(spatialAudioEnabledText)
            }
            LocalSession.current?.scene?.requestHomeSpaceMode()
        }

        composeTestRule.onNodeWithText(spatialAudioEnabledText).assertDoesNotExist()
    }

    @Test
    fun isSpatialAudioEnabled_homeSpaceMode_requestFullSpaceMode_returnsTrue() {
        composeTestRule.session =
            createFakeSession(composeTestRule.activity).apply { scene.requestHomeSpaceMode() }

        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isSpatialAudioEnabled) {
                Text(spatialAudioEnabledText)
            }
            LocalSession.current?.scene?.requestFullSpaceMode()
        }

        composeTestRule.onNodeWithText(spatialAudioEnabledText).assertExists()
    }

    @Test
    fun isSpatialAudioEnabled_fullSpaceMode_requestHomeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            if (LocalSpatialCapabilities.current.isSpatialAudioEnabled) {
                Text(spatialAudioEnabledText)
            }
            LocalSession.current?.scene?.requestHomeSpaceMode()
        }

        composeTestRule.onNodeWithText(spatialAudioEnabledText).assertDoesNotExist()
    }
}
