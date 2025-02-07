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
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.CoreContentlessEntity
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.createFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.setSubspaceContent
import androidx.xr.scenecore.ContentlessEntity
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.Session
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SubspaceLayoutNode]. */
@RunWith(AndroidJUnit4::class)
class SubspaceLayoutNodeTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    private lateinit var testSession: Session

    @Before
    fun setUp() {
        testSession = createFakeSession(composeTestRule.activity)
    }

    @Test
    fun subspaceLayoutNode_shouldParentNodesProperly() {
        val parentEntity = ContentlessEntity.create(testSession, "ParentEntity")
        composeTestRule.setSubspaceContent {
            EntityLayout(entity = parentEntity) {
                EntityLayout(
                    entity = ContentlessEntity.create(testSession, "ChildEntity"),
                    modifier = SubspaceModifier.testTag("Child"),
                )
            }
        }

        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("Child")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.getParent()
            )
            .isEqualTo(parentEntity)
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
            coreEntity = CoreContentlessEntity(entity),
        ) { _, _ ->
            layout(0, 0, 0) {}
        }
    }
}
