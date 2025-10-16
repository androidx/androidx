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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.compose.platform.DefaultDialogManager
import androidx.xr.compose.platform.LocalDialogManager
import androidx.xr.compose.spatial.ApplicationSubspace
import androidx.xr.compose.spatial.SpatialDialog
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.sizeIn
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.session
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider
import androidx.xr.scenecore.scene
import com.android.extensions.xr.ShadowXrExtensions
import com.android.extensions.xr.space.ShadowActivityPanel
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertTrue
import org.junit.Ignore
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
            Subspace {
                SpatialPanel(SubspaceModifier.width(100.dp).testTag("panel")) {
                    Row {
                        Spacer(Modifier.testTag("spacer1").weight(1f))
                        Spacer(Modifier.testTag("spacer2").weight(3f))
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
            Subspace {
                SpatialPanel(SubspaceModifier.width(10.dp).testTag("panel")) {
                    Column {
                        Text(
                            "Hello World long text",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }
                }
            }
        }
        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onNodeWithText("Hello World long text").assertWidthIsEqualTo(10.dp)
    }

    @Test
    fun spatialPanel_composePanel_sizesItself() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    Box(Modifier.width(100.dp).height(100.dp).testTag("contentBox")) {
                        Text("Content")
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(100.dp)
        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(100.dp)
        composeTestRule.onNodeWithTag("contentBox").assertWidthIsEqualTo(100.dp)
    }

    @Test
    fun spatialPanel_composePanel_sizesItselfWithLazyContent() {
        composeTestRule.setContent {
            ApplicationSubspace(modifier = SubspaceModifier.sizeIn(0.dp, 2000.dp, 0.dp, 2000.dp)) {
                SpatialPanel(SubspaceModifier.testTag("panel")) {
                    LazyColumn { items(50) { Box(Modifier.size(100.dp)) } }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(100.dp)
        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(2000.dp)
    }

    @Test
    fun spatialPanel_androidViewBasedPanel_composes() {
        lateinit var view: TextView
        composeTestRule.setContent {
            Subspace {
                SpatialAndroidViewPanel(
                    factory = {
                        TextView(it)
                            .apply { this.text = "Hello AndroidView World" }
                            .also { view = it }
                    },
                    SubspaceModifier.testTag("panel"),
                )
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        assertTrue(view.isAttachedToWindow)
    }

    @Test
    fun spatialPanel_androidViewBasedPanel_sizesItself() {
        composeTestRule.setContent {
            Subspace {
                SpatialAndroidViewPanel(
                    factory = { context ->
                        TextView(context).apply {
                            this.width = 100
                            this.height = 100
                        }
                    },
                    SubspaceModifier.testTag("panel"),
                )
            }
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(100.dp)
        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun mainPanel_renders() {
        val text = "Main Window Text"

        composeTestRule.setContent {
            Text(text)
            Subspace { SpatialMainPanel(SubspaceModifier.testTag("panel")) }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onNodeWithText(text).assertExists()
    }

    @Test
    fun mainPanel_visibilityToggles_enablesAndDisablesEntity() {
        val showPanel = mutableStateOf(true)
        val panelTag = "mainPanel"

        composeTestRule.setContent {
            ApplicationSubspace {
                if (showPanel.value) {
                    SpatialMainPanel(SubspaceModifier.testTag(panelTag).size(100.dp))
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag(panelTag).assertExists()
        val panelNode = composeTestRule.onSubspaceNodeWithTag(panelTag).fetchSemanticsNode()
        val panelEntity = panelNode.semanticsEntity as? PanelEntity
        assertThat(checkNotNull(panelEntity).isEnabled()).isTrue()

        showPanel.value = false

        composeTestRule.onSubspaceNodeWithTag(panelTag).assertDoesNotExist()

        showPanel.value = true

        composeTestRule.onSubspaceNodeWithTag(panelTag).assertExists()
        val panelEntityAfterShowing =
            composeTestRule.onSubspaceNodeWithTag(panelTag).fetchSemanticsNode().semanticsEntity
                as? PanelEntity
        assertThat(checkNotNull(panelEntityAfterShowing).isEnabled()).isTrue()
    }

    @Test
    fun spatialPanel_visibilityToggles_enablesAndDisablesEntity() {
        val showPanel = mutableStateOf(true)
        val panelTag = "spatialPanel"

        composeTestRule.setContent {
            Subspace {
                if (showPanel.value) {
                    SpatialPanel(SubspaceModifier.testTag(panelTag).size(100.dp)) {}
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag(panelTag).assertExists()
        val panelNode = composeTestRule.onSubspaceNodeWithTag(panelTag).fetchSemanticsNode()
        val panelEntity = panelNode.semanticsEntity as? PanelEntity
        assertThat(checkNotNull(panelEntity).isEnabled()).isTrue()

        showPanel.value = false

        composeTestRule.onSubspaceNodeWithTag(panelTag).assertDoesNotExist()

        showPanel.value = true

        composeTestRule.onSubspaceNodeWithTag(panelTag).assertExists()
        val panelNodeAfterShow =
            composeTestRule.onSubspaceNodeWithTag(panelTag).fetchSemanticsNode()
        val panelEntityAfterShow = panelNodeAfterShow.semanticsEntity as? PanelEntity
        assertThat(checkNotNull(panelEntityAfterShow).isEnabled()).isTrue()
    }

    @Test
    fun spatialAndroidViewPanel_visibilityToggles_enablesAndDisablesEntity() {
        val showPanel = mutableStateOf(true)
        val panelTag = "androidViewPanel"

        composeTestRule.setContent {
            Subspace {
                if (showPanel.value) {
                    SpatialAndroidViewPanel(
                        factory = { context -> TextView(context).apply { this.text = "test" } },
                        modifier = SubspaceModifier.testTag(panelTag).size(100.dp),
                    )
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag(panelTag).assertExists()
        val panelNode = composeTestRule.onSubspaceNodeWithTag(panelTag).fetchSemanticsNode()
        val panelEntity = panelNode.semanticsEntity as? PanelEntity
        assertThat(checkNotNull(panelEntity).isEnabled()).isTrue()

        showPanel.value = false

        composeTestRule.onSubspaceNodeWithTag(panelTag).assertDoesNotExist()

        showPanel.value = true

        composeTestRule.onSubspaceNodeWithTag(panelTag).assertExists()
        val panelNodeAfterShow =
            composeTestRule.onSubspaceNodeWithTag(panelTag).fetchSemanticsNode()
        val panelEntityAfterShow = panelNodeAfterShow.semanticsEntity as? PanelEntity
        assertThat(checkNotNull(panelEntityAfterShow).isEnabled()).isTrue()
    }

    @Test
    fun activityPanel_visibilityToggles_enablesAndDisablesEntity() {
        val showPanel = mutableStateOf(true)
        val panelTag = "activityPanel"

        composeTestRule.setContent {
            Subspace {
                if (showPanel.value) {
                    SpatialActivityPanel(
                        intent = Intent(composeTestRule.activity, SpatialPanelActivity::class.java),
                        modifier = SubspaceModifier.testTag(panelTag).size(100.dp),
                    )
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag(panelTag).assertExists()
        val panelNode = composeTestRule.onSubspaceNodeWithTag(panelTag).fetchSemanticsNode()
        val panelEntity = panelNode.semanticsEntity as? PanelEntity
        assertThat(checkNotNull(panelEntity).isEnabled()).isTrue()

        showPanel.value = false

        composeTestRule.onSubspaceNodeWithTag(panelTag).assertDoesNotExist()

        showPanel.value = true

        composeTestRule.onSubspaceNodeWithTag(panelTag).assertExists()
        val panelNodeAfterShow =
            composeTestRule.onSubspaceNodeWithTag(panelTag).fetchSemanticsNode()
        val panelEntityAfterShow = panelNodeAfterShow.semanticsEntity as? PanelEntity
        assertThat(checkNotNull(panelEntityAfterShow).isEnabled()).isTrue()
    }

    @Test
    fun spatialPanel_cornerRadius_dp() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier = SubspaceModifier.width(200.dp).height(300.dp).testTag("panel"),
                    shape = SpatialRoundedCornerShape(CornerSize(32.dp)),
                ) {}
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelEntity = panelNode.semanticsEntity as? PanelEntity
        assertThat(checkNotNull(panelEntity).cornerRadius.meters.toDp()).isEqualTo(32.dp)
    }

    @Test
    fun mainPanel_cornerRadius_dp() {
        composeTestRule.setContent {
            Subspace {
                SpatialMainPanel(
                    modifier = SubspaceModifier.width(200.dp).height(300.dp).testTag("mainPanel"),
                    shape = SpatialRoundedCornerShape(CornerSize(16.dp)),
                )
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("mainPanel").fetchSemanticsNode()
        val panelEntity = panelNode.semanticsEntity as? PanelEntity
        assertThat(checkNotNull(panelEntity).cornerRadius.meters.toDp()).isEqualTo(16.dp)
    }

    @Test
    fun spatialPanel_cornerRadius_percent() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier = SubspaceModifier.width(200.dp).height(300.dp).testTag("panel"),
                    shape = SpatialRoundedCornerShape(CornerSize(50)),
                ) {}
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelEntity = panelNode.semanticsEntity as? PanelEntity
        assertThat(checkNotNull(panelEntity).cornerRadius.meters.toDp()).isEqualTo(100.dp)
    }

    @Test
    @Ignore("This test depends on the implementation of ShadowActivityPanel.")
    fun activityPanel_launchesIntent() {
        composeTestRule.setContent {
            Subspace {
                SpatialActivityPanel(
                    intent = Intent(composeTestRule.activity, SpatialPanelActivity::class.java),
                    modifier = SubspaceModifier.width(200.dp).height(300.dp),
                    shape = SpatialRoundedCornerShape(CornerSize(50)),
                )
            }
        }
        // Since SubspaceTestingActivity uses FakeXrExtensions, the intent is stored in a map
        // instead of being launched.
        val launchIntent =
            ShadowActivityPanel.extract(
                    ShadowXrExtensions.extract(checkNotNull(XrExtensionsProvider.getXrExtensions()))
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
            CompositionLocalProvider(
                LocalDialogManager provides DefaultDialogManager(),
                content = {
                    Subspace {
                        SpatialActivityPanel(
                            intent =
                                Intent(composeTestRule.activity, SpatialPanelActivity::class.java),
                            modifier = SubspaceModifier.width(200.dp).height(300.dp),
                            shape = SpatialRoundedCornerShape(CornerSize(50)),
                        )
                        if (showDialog.value) {
                            SpatialDialog(onDismissRequest = { showDialog.value = false }) {
                                Text("Spatial Dialog")
                            }
                        }
                    }
                },
            )
        }
        val session = composeTestRule.session

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
            CompositionLocalProvider(
                LocalDialogManager provides DefaultDialogManager(),
                content = {
                    Subspace {
                        SpatialActivityPanel(
                            intent =
                                Intent(composeTestRule.activity, SpatialPanelActivity::class.java),
                            modifier = SubspaceModifier.width(200.dp).height(300.dp),
                            shape = SpatialRoundedCornerShape(CornerSize(50)),
                        )
                        if (showDialog.value) {
                            SpatialDialog(onDismissRequest = { showDialog.value = false }) {
                                Text("Spatial Dialog")
                            }
                        }
                    }
                },
            )
        }
        val session = composeTestRule.session

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

    @Test
    fun mainPanel_switchesActivePanel_onStateChange() {
        var activePanelId by mutableStateOf("A")

        @Composable
        fun PanelContainer(id: String) {
            if (activePanelId == id) {
                SpatialMainPanel(SubspaceModifier.testTag("panel-$id"))
            }
        }

        composeTestRule.setContent {
            Subspace {
                PanelContainer("A")
                PanelContainer("B")
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel-A").assertExists()
        composeTestRule.onSubspaceNodeWithTag("panel-B").assertDoesNotExist()

        activePanelId = "B"
        composeTestRule.waitForIdle()
        composeTestRule.onSubspaceNodeWithTag("panel-A").assertDoesNotExist()
        composeTestRule.onSubspaceNodeWithTag("panel-B").assertExists()

        activePanelId = "A"
        composeTestRule.waitForIdle()
        composeTestRule.onSubspaceNodeWithTag("panel-A").assertExists()
        composeTestRule.onSubspaceNodeWithTag("panel-B").assertDoesNotExist()

        activePanelId = "C"
        composeTestRule.waitForIdle()
        composeTestRule.onSubspaceNodeWithTag("panel-A").assertDoesNotExist()
        composeTestRule.onSubspaceNodeWithTag("panel-B").assertDoesNotExist()
    }

    private class SpatialPanelActivity : ComponentActivity() {}
}
