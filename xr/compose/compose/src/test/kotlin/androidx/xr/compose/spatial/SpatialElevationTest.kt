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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.session
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpatialElevationTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    private val parentTestTag = "parent"

    @Test
    fun spatialElevation_mainContent_isComposed() {
        composeTestRule.setContent {
            SpatialElevation {
                Box(modifier = Modifier.size(100.dp).testTag("MainContent")) {
                    Text("Main Content")
                }
            }
        }

        composeTestRule.onAllNodesWithTag("MainContent").onLast().assertIsDisplayed()
    }

    @Test
    fun spatialElevation_popup_doesNotThrowError() {
        composeTestRule.setContent { SpatialElevation { Popup { Text("Popup") } } }

        composeTestRule.onAllNodesWithText("Popup").onLast().assertIsDisplayed()
    }

    @Test
    fun spatialElevation_xrNotSupported_doesNotThrowError() {
        composeTestRule.activity.disableXr()

        composeTestRule.setContent { SpatialElevation { Popup { Text("Popup") } } }

        composeTestRule.onNodeWithText("Popup").assertExists()
    }

    @Test
    fun spatialElevation_homeSpaceMode_doesNotElevate() {
        composeTestRule.configureFakeSession().scene.requestHomeSpaceMode()

        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) { SpatialElevation { Text("Main Content") } }
        }

        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertTextContains("Main Content")
    }

    @Test
    fun spatialElevation_fullSpaceMode_doesElevate() {
        composeTestRule.setContent {
            Box(Modifier.testTag(parentTestTag)) { SpatialElevation { Text("Main Content") } }
        }

        // The placeholder content should not be displayed
        composeTestRule.onNodeWithTag(parentTestTag).onChild().assertIsNotDisplayed()
        // The placeholder and the elevated content both exist
        composeTestRule.onAllNodesWithText("Main Content").assertCountEquals(2)
    }

    @Test
    fun spatialElevation_elevated_panelSizeMatchesContentSize() {
        composeTestRule.setContent {
            Box(Modifier.size(1000.dp))
            SpatialElevation { Box(Modifier.size(100.dp)) { Text("Main Content") } }
        }

        composeTestRule.onAllNodesWithText("Main Content").onLast().assertIsDisplayed()
        val entities = composeTestRule.session?.scene?.getEntitiesOfType(PanelEntity::class.java)
        checkNotNull(entities).single { !it.isMainPanelEntity && it.sizeInPixels.width == 100 }
    }

    @Test
    fun spatialElevation_elevatedPanel_noXYOffsetIfParentViewIsSameSize() {
        composeTestRule.setContent {
            Box(Modifier.size(100.dp))
            SpatialElevation(elevation = 10.dp) {
                Box(Modifier.size(100.dp)) { Text("Main Content") }
            }
        }

        composeTestRule.onAllNodesWithText("Main Content").onLast().assertIsDisplayed()
        val entities =
            checkNotNull(composeTestRule.session?.scene?.getEntitiesOfType(PanelEntity::class.java))
        val panel = checkNotNull(entities).single { it.sizeInPixels.width == 100 }
        assertThat(panel).isNotEqualTo(composeTestRule.session?.scene?.mainPanelEntity)
        assertThat(panel.getPose().translation.x).isEqualTo(0f)
        assertThat(panel.getPose().translation.y).isEqualTo(0f)
        assertThat(panel.getPose().translation.z).isNotEqualTo(0f)
    }

    @Test
    fun spatialElevation_elevatedPanel_contentIsOnlyDisplayedOnce() {
        composeTestRule.setContent {
            Box(Modifier.size(100.dp))
            SpatialElevation(elevation = 10.dp) {
                Box(Modifier.size(100.dp)) { Text("Main Content") }
            }
        }

        composeTestRule.onAllNodesWithText("Main Content").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("Main Content").onFirst().assertIsNotDisplayed()
        composeTestRule.onAllNodesWithText("Main Content").onLast().assertIsDisplayed()
    }

    @Test
    fun spatialElevation_scrolledOffScreen_setsAlphaToZero() {
        val scrollState = ScrollState(0)

        composeTestRule.setContent {
            Column(modifier = Modifier.size(100.dp).verticalScroll(scrollState)) {
                SpatialElevation { Box(Modifier.size(100.dp).testTag("ElevatedContent")) }
                Box(Modifier.size(1000.dp))
            }
        }

        val panel =
            checkNotNull(composeTestRule.session?.scene?.getEntitiesOfType(PanelEntity::class.java))
                .single { !it.isMainPanelEntity }

        assertThat(panel.getAlpha()).isEqualTo(1f)

        runBlocking { scrollState.scrollTo(500) }
        composeTestRule.waitForIdle()

        assertThat(panel.getAlpha()).isEqualTo(0f)
    }
}
