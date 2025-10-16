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

package androidx.xr.compose.subspace.layout

import android.content.Intent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.ApplicationSubspace
import androidx.xr.compose.subspace.SpatialActivityPanel
import androidx.xr.compose.subspace.SpatialAndroidViewPanel
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.session
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.scenecore.GroupEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.junit.rules.ExpectedLogMessagesRule

@RunWith(AndroidJUnit4::class)
class CoreEntityTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()
    @get:Rule val expectedLogMessagesRule = ExpectedLogMessagesRule()

    private class SpatialPanelActivity : ComponentActivity() {}

    @Test
    fun coreEntity_coreGroupEntity_shouldThrowIfNotGroupEntity() {
        composeTestRule.setContent {
            val session = assertNotNull(LocalSession.current)
            assertFailsWith<IllegalArgumentException> {
                CoreGroupEntity(session.scene.activitySpace)
            }
        }
    }

    @Test
    @Ignore("b/430291253 - behavior is different in presubmit after moving to targetSdk 35")
    fun coreEntity_size_shouldNotTriggerRecomposition() {
        var size = 100
        var sizeCount = 0
        var mutableSizeCount = 0

        composeTestRule.setContent {
            val coreEntity = remember {
                CoreGroupEntity(
                        GroupEntity.create(
                            session = assertNotNull(composeTestRule.session),
                            name = "Test",
                        )
                    )
                    .apply { this.size = IntVolumeSize(size, size, size) }
            }

            SizeWatcher(coreEntity) { sizeCount++ }
            MutableSizeWatcher(coreEntity) { mutableSizeCount++ }

            Button(
                onClick = {
                    size += 100
                    coreEntity.size = IntVolumeSize(size, size, size)
                }
            ) {
                Text("Increase")
            }
        }

        composeTestRule.onNodeWithText("Increase").performClick()
        composeTestRule.onNodeWithText("Increase").performClick()
        composeTestRule.onNodeWithText("Increase").performClick()
        composeTestRule.onNodeWithText("Increase").performClick()
        composeTestRule.waitForIdle()

        assertThat(sizeCount).isEqualTo(1)
        assertThat(mutableSizeCount).isEqualTo(5)
    }

    @Composable
    private fun SizeWatcher(coreEntity: CoreEntity, onSizeChanged: () -> Unit) {
        check(coreEntity.size != IntVolumeSize.Zero)
        onSizeChanged()
    }

    @Composable
    private fun MutableSizeWatcher(coreEntity: CoreEntity, onSizeChanged: () -> Unit) {
        check(coreEntity.mutableSize != IntVolumeSize.Zero)
        onSizeChanged()
    }

    @Test
    fun coreBasePanelEntity_androidViewPanel_enabledStateFollowsSizeChanges() {
        var size by mutableStateOf(100.dp)
        composeTestRule.setContent {
            ApplicationSubspace {
                SpatialAndroidViewPanel(
                    factory = { View(it) },
                    SubspaceModifier.width(size).height(size).testTag("panel"),
                )
            }
        }

        // Initial non-zero size should be enabled.
        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelEntity = assertNotNull(panelNode.semanticsEntity as? PanelEntity)
        assertThat(panelEntity.isEnabled()).isTrue()

        // Recompose with zero size, should be disabled.
        size = 0.dp
        composeTestRule.waitForIdle()
        assertThat(panelEntity.isEnabled()).isFalse()

        // Recompose with non-zero size, should be enabled again.
        size = 50.dp
        composeTestRule.waitForIdle()
        assertThat(panelEntity.isEnabled()).isTrue()
    }

    @Test
    fun coreBasePanelEntity_spatialPanel_enabledStateFollowsSizeChanges() {
        var size by mutableStateOf(100.dp)
        composeTestRule.setContent {
            ApplicationSubspace {
                SpatialPanel(SubspaceModifier.width(size).height(size).testTag("panel")) {}
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelEntity = assertNotNull(panelNode.semanticsEntity as? PanelEntity)
        assertThat(panelEntity.isEnabled()).isTrue()

        size = 0.dp
        composeTestRule.waitForIdle()
        assertThat(panelEntity.isEnabled()).isFalse()

        size = 50.dp
        composeTestRule.waitForIdle()
        assertThat(panelEntity.isEnabled()).isTrue()
    }

    @Test
    fun coreBasePanelEntity_mainPanel_enabledStateFollowsSizeChanges() {
        var size by mutableStateOf(100.dp)
        composeTestRule.setContent {
            ApplicationSubspace {
                SpatialMainPanel(SubspaceModifier.width(size).height(size).testTag("panel"))
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelEntity = assertNotNull(panelNode.semanticsEntity as? PanelEntity)
        assertThat(panelEntity.isEnabled()).isTrue()

        size = 0.dp
        composeTestRule.waitForIdle()
        assertThat(panelEntity.isEnabled()).isFalse()

        size = 50.dp
        composeTestRule.waitForIdle()
        assertThat(panelEntity.isEnabled()).isTrue()
    }

    @Test
    fun coreBasePanelEntity_activityPanel_enabledStateFollowsSizeChanges() {
        var size by mutableStateOf(100.dp)
        composeTestRule.setContent {
            ApplicationSubspace {
                SpatialActivityPanel(
                    intent = Intent(composeTestRule.activity, SpatialPanelActivity::class.java),
                    SubspaceModifier.width(size).height(size).testTag("panel"),
                )
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelEntity = assertNotNull(panelNode.semanticsEntity as? PanelEntity)
        assertThat(panelEntity.isEnabled()).isTrue()

        size = 0.dp
        composeTestRule.waitForIdle()
        assertThat(panelEntity.isEnabled()).isFalse()

        size = 50.dp
        composeTestRule.waitForIdle()
        assertThat(panelEntity.isEnabled()).isTrue()
    }
}
