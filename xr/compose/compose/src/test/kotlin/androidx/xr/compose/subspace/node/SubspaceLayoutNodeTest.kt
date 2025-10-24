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

package androidx.xr.compose.subspace.node

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.AndroidComposeSpatialElement
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GroupEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SubspaceLayoutNode]. */
@RunWith(AndroidJUnit4::class)
class SubspaceLayoutNodeTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun subspaceLayoutNode_shouldParentNodesProperly() {
        var parentEntity: Entity? = null

        composeTestRule.setContent {
            Subspace {
                val session = checkNotNull(LocalSession.current)
                parentEntity = GroupEntity.create(session, "ParentEntity")
                EntityLayout(entity = parentEntity) {
                    EntityLayout(
                        entity = GroupEntity.create(session, "ChildEntity"),
                        modifier = SubspaceModifier.testTag("Child"),
                    )
                }
            }
        }

        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("Child")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.parent
            )
            .isEqualTo(parentEntity)
    }

    @Test
    fun subspaceLayoutNode_verifyRootDepthIsZero() {
        val rootNode = SubspaceLayoutNode()

        assertThat(rootNode.depth).isEqualTo(0)
    }

    @Test
    fun subspaceLayoutNode_verifyChildrenDepth() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val grandParentNode = SubspaceLayoutNode()
        val parentNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()

        rootNode.attach(owner)
        rootNode.insertAt(0, grandParentNode)
        grandParentNode.insertAt(0, parentNode)
        parentNode.insertAt(0, childNode)

        assertThat(grandParentNode.depth).isEqualTo(1)
        assertThat(parentNode.depth).isEqualTo(2)
        assertThat(childNode.depth).isEqualTo(3)
    }

    @Test
    fun subspaceLayoutNode_verifyDetachingChangesDepth() {
        val owner = AndroidComposeSpatialElement()
        val rootNode = SubspaceLayoutNode()
        val parentNode = SubspaceLayoutNode()
        val childNode = SubspaceLayoutNode()

        rootNode.attach(owner)
        rootNode.insertAt(0, parentNode)
        parentNode.insertAt(0, childNode)

        assertThat(parentNode.depth).isEqualTo(1)
        assertThat(childNode.depth).isEqualTo(2)

        rootNode.removeAt(0, 1)
        parentNode.attach(owner)

        assertThat(parentNode.depth).isEqualTo(0)
        assertThat(childNode.depth).isEqualTo(1)
    }

    @Composable
    @SubspaceComposable
    private fun EntityLayout(
        modifier: SubspaceModifier = SubspaceModifier,
        entity: Entity,
        content: @Composable @SubspaceComposable () -> Unit = {},
    ) {
        SubspaceLayout(
            content = content,
            modifier = modifier,
            coreEntity = CoreGroupEntity(entity),
        ) { _, _ ->
            layout(0, 0, 0) {}
        }
    }
}
