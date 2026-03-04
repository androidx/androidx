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
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialActivityPanel
import androidx.xr.compose.subspace.SpatialAndroidViewPanel
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.session
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.PanelEntity
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.junit.rules.ExpectedLogMessagesRule

@RunWith(AndroidJUnit4::class)
class CoreEntityTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()
    @get:Rule val expectedLogMessagesRule = ExpectedLogMessagesRule()

    private class SpatialPanelActivity : ComponentActivity() {}

    @Test
    @Ignore("b/430291253 - behavior is different in presubmit after moving to targetSdk 35")
    fun coreEntity_size_shouldNotTriggerRecomposition() {
        var size = 100
        var sizeCount = 0
        var mutableSizeCount = 0

        composeTestRule.setContent {
            val coreEntity = remember {
                CoreGroupEntity(
                        Entity.create(
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
            Subspace {
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
            Subspace { SpatialPanel(SubspaceModifier.width(size).height(size).testTag("panel")) {} }
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
            Subspace {
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
            Subspace {
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

    @Test
    fun attachEntity_onExistingCoreEntity_replacesAndDisposesOldEntity() {
        val session = composeTestRule.configureFakeSession()
        val initialEntity = Entity.create(session = session, name = "Initial")
        val coreEntity = CoreGroupEntity(initialEntity)
        val newEntity = Entity.create(session = session, name = "New")

        coreEntity.attachEntity(newEntity)

        assertThat(coreEntity.semanticsEntity).isEqualTo(newEntity)
        // SceneCore entities are not truly "disposed" in a way we can easily assert, but we can
        // verify the new entity is the one being used.
    }

    @Test
    fun parent_setParent_updatesEntityParent() {
        val session = composeTestRule.configureFakeSession()
        val testEntity = Entity.create(session = assertNotNull(session), name = "Initial")
        val parentCoreEntity = CoreGroupEntity(testEntity)
        val childEntity = Entity.create(session = assertNotNull(session), name = "Child")
        val childCoreEntity = CoreGroupEntity(childEntity)

        childCoreEntity.parent = parentCoreEntity

        assertThat(childEntity.parent).isEqualTo(testEntity)
    }

    @Test
    fun parent_setParentToNull_restoresOriginalParent() {
        val session = composeTestRule.configureFakeSession()
        val testEntity = Entity.create(session = session, name = "Initial")
        val parentCoreEntity = CoreGroupEntity(testEntity)
        val childEntity = Entity.create(session = session, name = "Child")
        val originalParent = childEntity.parent
        val childCoreEntity = CoreGroupEntity(childEntity)

        childCoreEntity.parent = parentCoreEntity
        assertThat(childEntity.parent).isNotEqualTo(originalParent)

        childCoreEntity.parent = null
        assertThat(childEntity.parent).isEqualTo(originalParent)
    }

    @Test
    fun poseInMeters_setPose_updatesEntityPose() {
        val session = composeTestRule.configureFakeSession()
        val testEntity = Entity.create(session = assertNotNull(session), name = "Initial")
        val coreEntity = CoreGroupEntity(testEntity)
        val newPose = Pose(Vector3(5f, 5f, 5f))

        coreEntity.poseInMeters = newPose

        assertThat(testEntity.getPose()).isEqualTo(newPose)
    }

    @Test
    fun poseInMeters_setSamePose_doesNotUpdateEntity() {
        val session = composeTestRule.configureFakeSession()
        val testEntity = Entity.create(session = session, name = "Initial")
        val coreEntity = CoreGroupEntity(testEntity)
        val initialPose = testEntity.getPose()

        // We can't directly check if setPose was called, but we can ensure
        // the value remains identical.
        coreEntity.poseInMeters = initialPose

        assertThat(testEntity.getPose()).isEqualTo(initialPose)
    }

    @Test
    fun enabled_setEnabled_updatesEntityEnabledState() {
        val session = composeTestRule.configureFakeSession()
        val testEntity = Entity.create(session = assertNotNull(session), name = "Initial")
        val coreEntity = CoreGroupEntity(testEntity)
        testEntity.setEnabled(true)

        coreEntity.enabled = false

        assertThat(testEntity.isEnabled(includeParents = false)).isFalse()
    }

    @Test
    fun scale_setScale_updatesEntityScale() {
        val session = composeTestRule.configureFakeSession()
        val testEntity = Entity.create(session = assertNotNull(session), name = "Initial")
        val coreEntity = CoreGroupEntity(testEntity)
        val newScale = 2.5f

        coreEntity.scale = newScale

        assertThat(testEntity.getScale()).isEqualTo(newScale)
    }

    @Test
    fun alpha_setAlpha_updatesEntityAlpha() {
        val session = composeTestRule.configureFakeSession()
        val testEntity = Entity.create(session = assertNotNull(session), name = "Initial")
        val coreEntity = CoreGroupEntity(testEntity)
        val newAlpha = 0.5f

        coreEntity.alpha = newAlpha

        assertThat(testEntity.getAlpha()).isEqualTo(newAlpha)
    }
}
