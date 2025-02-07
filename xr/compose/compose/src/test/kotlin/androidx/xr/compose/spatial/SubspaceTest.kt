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

package androidx.xr.compose.spatial

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.SceneManager
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
import androidx.xr.compose.testing.createFakeRuntime
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubspaceTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun subspace_xrEnabled_contentIsCreated() {
        composeTestRule.setContent {
            TestSetup { Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} } }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
    }

    @Test
    fun subspace_nonXr_contentIsNotCreated() {
        composeTestRule.setContent {
            TestSetup(isXrEnabled = false) {
                Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
    }

    @Test
    fun subspace_applicationSubspace_contentIsParentedToActivitySpace() {
        composeTestRule.setContent {
            TestSetup { Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} } }
        }

        val node = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panel = node.semanticsEntity
        val subspaceBox = panel?.getParent()
        assertThat(subspaceBox?.getParent())
            .isEqualTo(composeTestRule.activity.session.activitySpace)
    }

    @Test
    fun subspace_nestedSubspace_contentIsParentedToContainingPanel() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel")) {
                        Subspace { SpatialPanel(SubspaceModifier.testTag("innerPanel")) {} }
                    }
                }
            }
        }

        val outerPanelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val outerPanelEntity = outerPanelNode.semanticsEntity
        val innerPanelNode =
            composeTestRule.onSubspaceNodeWithTag("innerPanel").fetchSemanticsNode()
        val innerPanelEntity = innerPanelNode.semanticsEntity
        val subspaceBoxEntity = innerPanelEntity?.getParent()
        val subspaceLayoutEntity = subspaceBoxEntity?.getParent()
        val subspaceRootEntity = subspaceLayoutEntity?.getParent()
        val subspaceRootContainerEntity = subspaceRootEntity?.getParent()
        val parentPanel = subspaceRootContainerEntity?.getParent()
        assertThat(parentPanel).isNotNull()
        assertThat(parentPanel).isEqualTo(outerPanelEntity)
    }

    @Test
    fun subspace_isDisposed() {
        var showSubspace by mutableStateOf(true)

        composeTestRule.setContent {
            TestSetup {
                if (showSubspace) {
                    Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount()).isEqualTo(1)
        showSubspace = false
        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
        assertThat(SceneManager.getSceneCount()).isEqualTo(0)
    }

    @Test
    fun subspace_onlyOneSceneExists_afterSpaceModeChanges() {
        val runtime = createFakeRuntime(composeTestRule.activity)

        composeTestRule.setContent {
            TestSetup(runtime = runtime) {
                Subspace { SpatialPanel(SubspaceModifier.testTag("panel")) {} }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount()).isEqualTo(1)
        runtime.requestHomeSpaceMode()
        composeTestRule.onSubspaceNodeWithTag("panel").assertDoesNotExist()
        assertThat(SceneManager.getSceneCount()).isEqualTo(0)
        runtime.requestFullSpaceMode()
        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertThat(SceneManager.getSceneCount()).isEqualTo(1)
    }
}
