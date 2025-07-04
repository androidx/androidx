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
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.createFakeRuntime
import androidx.xr.scenecore.scene
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpatialCapabilitiesTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun isSpatialUiEnabled_xrNotEnabled_returnsFalse() {
        composeTestRule.setContent {
            TestSetup(isXrEnabled = false) {
                Text(text = "${LocalSpatialCapabilities.current.isSpatialUiEnabled}")
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isContent3dEnabled_xrNotEnabled_returnsFalse() {
        composeTestRule.setContent {
            TestSetup(isXrEnabled = false) {
                Text(text = "${LocalSpatialCapabilities.current.isContent3dEnabled}")
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isAppEnvironmentEnabled_xrNotEnabled_returnsFalse() {
        composeTestRule.setContent {
            TestSetup(isXrEnabled = false) {
                Text(text = "${LocalSpatialCapabilities.current.isAppEnvironmentEnabled}")
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isPassthroughControlEnabled_xrNotEnabled_returnsFalse() {
        composeTestRule.setContent {
            TestSetup(isXrEnabled = false) {
                Text(text = "${LocalSpatialCapabilities.current.isPassthroughControlEnabled}")
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isSpatialAudioEnabled_xrNotEnabled_returnsFalse() {
        composeTestRule.setContent {
            TestSetup(isXrEnabled = false) {
                Text(text = "${LocalSpatialCapabilities.current.isSpatialAudioEnabled}")
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isSpatialUiEnabled_fullSpaceMode_returnsTrue() {
        composeTestRule.setContent {
            TestSetup { Text(text = "${LocalSpatialCapabilities.current.isSpatialUiEnabled}") }
        }

        composeTestRule.onNodeWithText("${true}").assertExists()
    }

    @Test
    fun isContent3dEnabled_fullSpaceMode_returnsTrue() {
        composeTestRule.setContent {
            TestSetup { Text(text = "${LocalSpatialCapabilities.current.isContent3dEnabled}") }
        }

        composeTestRule.onNodeWithText("${true}").assertExists()
    }

    @Test
    fun isAppEnvironmentEnabled_fullSpaceMode_returnsTrue() {
        composeTestRule.setContent {
            TestSetup { Text(text = "${LocalSpatialCapabilities.current.isAppEnvironmentEnabled}") }
        }

        composeTestRule.onNodeWithText("${true}").assertExists()
    }

    @Test
    fun isPassthroughControlEnabled_fullSpaceMode_returnsTrue() {
        composeTestRule.setContent {
            TestSetup {
                Text(text = "${LocalSpatialCapabilities.current.isPassthroughControlEnabled}")
            }
        }

        composeTestRule.onNodeWithText("${true}").assertExists()
    }

    @Test
    fun isSpatialAudioEnabled_fullSpaceMode_returnsTrue() {
        composeTestRule.setContent {
            TestSetup { Text(text = "${LocalSpatialCapabilities.current.isSpatialAudioEnabled}") }
        }

        composeTestRule.onNodeWithText("${true}").assertExists()
    }

    @Test
    fun isSpatialUiEnabled_homeSpaceMode_returnsFalse() {
        val runtime = createFakeRuntime(composeTestRule.activity)
        runtime.requestHomeSpaceMode()

        composeTestRule.setContent {
            TestSetup(runtime = runtime) {
                Text("${LocalSpatialCapabilities.current.isSpatialUiEnabled}")
                LocalSession.current?.scene?.requestHomeSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isSpatialUiEnabled_homeSpaceMode_requestFullSpaceMode_returnsTrue() {
        val runtime = createFakeRuntime(composeTestRule.activity)
        runtime.requestHomeSpaceMode()

        composeTestRule.setContent {
            TestSetup(runtime = runtime) {
                Text("${LocalSpatialCapabilities.current.isSpatialUiEnabled}")
                LocalSession.current?.scene?.requestFullSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${true}").assertExists()
    }

    @Test
    fun isSpatialUiEnabled_fullSpaceMode_requestHomeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            TestSetup {
                Text("${LocalSpatialCapabilities.current.isSpatialUiEnabled}")
                LocalSession.current?.scene?.requestHomeSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isContent3dEnabled_homeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            TestSetup {
                Text("${LocalSpatialCapabilities.current.isContent3dEnabled}")
                LocalSession.current?.scene?.requestHomeSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isContent3dEnabled_homeSpaceMode_requestFullSpaceMode_returnsTrue() {
        val runtime = createFakeRuntime(composeTestRule.activity)
        runtime.requestHomeSpaceMode()

        composeTestRule.setContent {
            TestSetup(runtime = runtime) {
                Text("${LocalSpatialCapabilities.current.isContent3dEnabled}")
                LocalSession.current?.scene?.requestFullSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${true}").assertExists()
    }

    @Test
    fun isContent3dEnabled_fullSpaceMode_requestHomeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            TestSetup {
                Text("${LocalSpatialCapabilities.current.isContent3dEnabled}")
                LocalSession.current?.scene?.requestHomeSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isAppEnvironmentEnabled_homeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            TestSetup {
                Text(text = "${LocalSpatialCapabilities.current.isAppEnvironmentEnabled}")
                LocalSession.current?.scene?.requestHomeSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isAppEnvironmentEnabled_homeSpaceMode_requestFullSpaceMode_returnsTrue() {
        val runtime = createFakeRuntime(composeTestRule.activity)
        runtime.requestHomeSpaceMode()

        composeTestRule.setContent {
            TestSetup(runtime = runtime) {
                Text(text = "${LocalSpatialCapabilities.current.isAppEnvironmentEnabled}")
                LocalSession.current?.scene?.requestFullSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${true}").assertExists()
    }

    @Test
    fun isAppEnvironmentEnabled_fullSpaceMode_requestHomeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            TestSetup {
                Text(text = "${LocalSpatialCapabilities.current.isAppEnvironmentEnabled}")
                LocalSession.current?.scene?.requestHomeSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isPassthroughControlEnabled_homeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            TestSetup {
                Text(text = "${LocalSpatialCapabilities.current.isPassthroughControlEnabled}")
                LocalSession.current?.scene?.requestHomeSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isPassthroughControlEnabled_homeSpaceMode_requestFullSpaceMode_returnsTrue() {
        val runtime = createFakeRuntime(composeTestRule.activity)
        runtime.requestHomeSpaceMode()

        composeTestRule.setContent {
            TestSetup(runtime = runtime) {
                Text(text = "${LocalSpatialCapabilities.current.isPassthroughControlEnabled}")
                LocalSession.current?.scene?.requestFullSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${true}").assertExists()
    }

    @Test
    fun isPassthroughControlEnabled_fullSpaceMode_requestHomeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            TestSetup {
                Text(text = "${LocalSpatialCapabilities.current.isPassthroughControlEnabled}")
                LocalSession.current?.scene?.requestHomeSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isSpatialAudioEnabled_homeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            TestSetup {
                Text(text = "${LocalSpatialCapabilities.current.isSpatialAudioEnabled}")
                LocalSession.current?.scene?.requestHomeSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }

    @Test
    fun isSpatialAudioEnabled_homeSpaceMode_requestFullSpaceMode_returnsTrue() {
        val runtime = createFakeRuntime(composeTestRule.activity)
        runtime.requestHomeSpaceMode()

        composeTestRule.setContent {
            TestSetup(runtime = runtime) {
                Text(text = "${LocalSpatialCapabilities.current.isSpatialAudioEnabled}")
                LocalSession.current?.scene?.requestFullSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${true}").assertExists()
    }

    @Test
    fun isSpatialAudioEnabled_fullSpaceMode_requestHomeSpaceMode_returnsFalse() {
        composeTestRule.setContent {
            TestSetup {
                Text(text = "${LocalSpatialCapabilities.current.isSpatialAudioEnabled}")
                LocalSession.current?.scene?.requestHomeSpaceMode()
            }
        }

        composeTestRule.onNodeWithText("${false}").assertExists()
    }
}
