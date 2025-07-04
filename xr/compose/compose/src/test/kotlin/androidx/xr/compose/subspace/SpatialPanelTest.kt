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

package androidx.xr.compose.subspace

import android.content.Intent
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.compose.spatial.ApplicationSubspace
import androidx.xr.compose.spatial.SpatialDialog
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.space.ShadowActivityPanel
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SpatialPanel]. */
@RunWith(AndroidJUnit4::class)
class SpatialPanelTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun spatialPanel_internalElementsAreLaidOutProperly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.width(100.dp).testTag("panel")) {
                        // Row with 2 elements, one is 3x as large as the other
                        Row {
                            Spacer(Modifier.testTag("spacer1").weight(1f))
                            Spacer(Modifier.testTag("spacer2").weight(3f))
                        }
                    }
                }
            }
        }
        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onNodeWithTag("spacer1").assertWidthIsEqualTo(25.dp)
        composeTestRule.onNodeWithTag("spacer2").assertWidthIsEqualTo(75.dp)
    }

    @Test
    fun spatialPanel_textTooLong_panelDoesNotGrowBeyondSpecifiedWidth() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    // Panel with 10dp width, way too small for the text we're putting into it
                    SpatialPanel(SubspaceModifier.width(10.dp).testTag("panel")) {
                        // Panel contains a column.
                        Column {
                            Text(
                                "Hello World long text",
                                style = MaterialTheme.typography.headlineLarge,
                            )
                        }
                    }
                }
            }
        }
        // Text element stays 10dp long, even though it needs more space, as the Panel will not grow
        // for the text.
        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onNodeWithText("Hello World long text").assertWidthIsEqualTo(10.dp)
    }

    @Test
    fun spatialPanel_AndroidViewBasedPanelComposes() {
        lateinit var view: TextView
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialAndroidViewPanel(
                        factory = {
                            TextView(it)
                                .apply { text = "Hello AndroidView World" }
                                .also { view = it }
                        },
                        SubspaceModifier.testTag("panel"),
                    )
                }
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertTrue(view.isAttachedToWindow)
    }

    @Test
    fun mainPanel_renders() {
        val text = "Main Window Text"

        composeTestRule.setContent {
            Text(text)
            TestSetup { Subspace { SpatialMainPanel(SubspaceModifier.testTag("panel")) } }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onNodeWithText(text).assertExists()
    }

    @Test
    fun mainPanel_disposes_mainPanelGetsDisabled() {
        val showMainPanel = mutableStateOf(true)

        composeTestRule.setContent {
            TestSetup {
                ApplicationSubspace {
                    if (showMainPanel.value) {
                        SpatialMainPanel(
                            SubspaceModifier.testTag("mainPanel").width(100.dp).height(100.dp)
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val mainPanelNode = composeTestRule.onSubspaceNodeWithTag("mainPanel").fetchSemanticsNode()
        val mainPanelSceneCoreEntity = mainPanelNode.semanticsEntity as? PanelEntity
        assertThat(checkNotNull(mainPanelSceneCoreEntity).isEnabled()).isTrue()

        showMainPanel.value = false
        composeTestRule.waitForIdle()

        val mainPanelSceneCoreEntityAfter = mainPanelNode.semanticsEntity as? PanelEntity
        assertThat(checkNotNull(mainPanelSceneCoreEntityAfter).isEnabled()).isFalse()
    }

    @Test
    fun mainPanel_addedTwice_asserts() {
        val text = "Main Window Text"

        assertThrows(IllegalStateException::class.java) {
            composeTestRule.setContent {
                Text(text)
                TestSetup {
                    Subspace {
                        SpatialMainPanel(SubspaceModifier.testTag("panel"))
                        SpatialMainPanel(SubspaceModifier.testTag("panel2"))
                    }
                }
            }
        }
    }

    @Test
    fun mainPanel_addedTwiceInDifferentSubtrees_asserts() {
        val text = "Main Window Text"

        assertThrows(IllegalStateException::class.java) {
            composeTestRule.setContent {
                Text(text)
                TestSetup {
                    Subspace {
                        SpatialMainPanel(SubspaceModifier.testTag("panel"))
                        SpatialMainPanel(SubspaceModifier.testTag("panel2"))
                    }
                }
            }
        }
    }

    @Test
    fun spatialPanel_cornerRadius_dp() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(
                        modifier = SubspaceModifier.width(200.dp).height(300.dp).testTag("panel"),
                        shape = SpatialRoundedCornerShape(CornerSize(32.dp)),
                    ) {}
                }
            }
        }
        assertThat(getPanelEntity("panel")?.cornerRadius?.meters?.toDp()).isEqualTo(32.dp)
    }

    @Test
    fun mainPanel_cornerRadius_dp() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialMainPanel(
                        modifier =
                            SubspaceModifier.width(200.dp).height(300.dp).testTag("mainPanel"),
                        shape = SpatialRoundedCornerShape(CornerSize(16.dp)),
                    )
                }
            }
        }
        assertThat(getPanelEntity("mainPanel")?.cornerRadius?.meters?.toDp()).isEqualTo(16.dp)
    }

    @Test
    fun spatialPanel_cornerRadius_percent() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(
                        modifier = SubspaceModifier.width(200.dp).height(300.dp).testTag("panel"),
                        shape = SpatialRoundedCornerShape(CornerSize(50)),
                    ) {}
                }
            }
        }

        assertThat(getPanelEntity("panel")?.cornerRadius?.meters?.toDp()).isEqualTo(100.dp)
    }

    @Test
    fun activityPanel_launchesIntent() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialActivityPanel(
                        intent = Intent(composeTestRule.activity, SpatialPanelActivity::class.java),
                        modifier = SubspaceModifier.width(200.dp).height(300.dp),
                        shape = SpatialRoundedCornerShape(CornerSize(50)),
                    )
                }
            }
        }
        // Since SubspaceTestingActivity uses FakeXrExtensions, the intent is stored in a map
        // instead of
        // being launched.
        val launchIntent =
            ShadowActivityPanel.extract(
                    ShadowXrExtensions.extract(composeTestRule.activity.extensions)
                        .getActivityPanelForHost(composeTestRule.activity)
                )
                .launchIntent

        assertThat(launchIntent?.component?.className)
            .isEqualTo(SpatialPanelActivity::class.java.name)
    }

    @Test
    fun activityPanel_scrimAdds() {
        val showDialog = mutableStateOf(false)

        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialActivityPanel(
                        intent = Intent(composeTestRule.activity, SpatialPanelActivity::class.java),
                        modifier = SubspaceModifier.width(200.dp).height(300.dp),
                        shape = SpatialRoundedCornerShape(CornerSize(50)),
                    )
                    if (showDialog.value) {
                        SpatialDialog(onDismissRequest = { showDialog.value = false }) {
                            Text("Spatial Dialog")
                        }
                    }
                }
            }
        }
        val session = composeTestRule.activity.session

        // Verify the initial set of PanelEntities in the scene before the dialog is shown:
        // Activity Panel
        // Main PanelEntity
        assertThat(session?.scene?.getEntitiesOfType(PanelEntity::class.java)?.size).isEqualTo(2)

        showDialog.value = true
        composeTestRule.waitForIdle()

        // Verify the set of PanelEntities after the SpatialDialog is displayed:
        // Activity Panel
        // Main PanelEntity
        // SpatialDialog
        // Activity Scrim Panel
        assertThat(session?.scene?.getEntitiesOfType(PanelEntity::class.java)?.size).isEqualTo(4)
    }

    @Test
    fun activityPanel_scrimRemoves() {
        val showDialog = mutableStateOf(true)

        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialActivityPanel(
                        intent = Intent(composeTestRule.activity, SpatialPanelActivity::class.java),
                        modifier = SubspaceModifier.width(200.dp).height(300.dp),
                        shape = SpatialRoundedCornerShape(CornerSize(50)),
                    )
                    if (showDialog.value) {
                        SpatialDialog(onDismissRequest = { showDialog.value = false }) {
                            Text("Spatial Dialog")
                        }
                    }
                }
            }
        }
        val session = composeTestRule.activity.session

        // Verify the set of PanelEntities before the SpatialDialog is dismissed:
        // SpatialDialog
        // Activity Scrim Panel
        // Activity Panel
        // Main PanelEntity
        assertThat(session?.scene?.getEntitiesOfType(PanelEntity::class.java)?.size).isEqualTo(4)

        showDialog.value = false
        composeTestRule.waitForIdle()

        // Verify the set of PanelEntities after the SpatialDialog is dismissed:
        // Activity Panel
        // Main PanelEntity
        assertThat(session?.scene?.getEntitiesOfType(PanelEntity::class.java)?.size).isEqualTo(2)
    }

    private fun getPanelEntity(tag: String): PanelEntity? {
        return composeTestRule.onSubspaceNodeWithTag(tag).fetchSemanticsNode().semanticsEntity
            as PanelEntity
    }

    private class SpatialPanelActivity : ComponentActivity() {}
}
