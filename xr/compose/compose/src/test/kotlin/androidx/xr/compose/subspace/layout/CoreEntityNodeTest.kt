/*
 * Copyright 2025 The Android Open Source Project
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

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialAndroidViewPanel
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.scenecore.PanelEntity
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoreEntityNodeTest {

    /**
     * Applies a modification to the [CoreEntity] associated with this subspace layout.
     *
     * This provides a generic way to apply changes to the underlying entity for testing purposes.
     *
     * @param modify The function literal to execute on the [CoreEntityScope].
     */
    private fun SubspaceModifier.modifyCoreEntity(
        modify: CoreEntityScope.() -> Unit
    ): SubspaceModifier = this.then(ModifyCoreEntityElement(modify))

    private data class ModifyCoreEntityElement(private var modify: CoreEntityScope.() -> Unit) :
        SubspaceModifierNodeElement<ModifyCoreEntityNode>() {
        override fun create(): ModifyCoreEntityNode = ModifyCoreEntityNode(modify)

        override fun update(node: ModifyCoreEntityNode) {
            node.modify = modify
        }
    }

    private class ModifyCoreEntityNode(var modify: CoreEntityScope.() -> Unit) :
        SubspaceModifier.Node(), CoreEntityNode {
        override fun CoreEntityScope.modifyCoreEntity() {
            modify()
        }
    }

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun coreEntityNode_alpha_shouldBeApplied() {
        composeTestRule.setContent {
            Subspace {
                SpatialAndroidViewPanel(
                    factory = { View(it) },
                    SubspaceModifier.modifyCoreEntity { setOrAppendAlpha(0.5f) }.testTag("panel"),
                )
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelSceneCoreEntity = panelNode.semanticsEntity as PanelEntity?
        assertNotNull(panelSceneCoreEntity)
        assertThat(panelSceneCoreEntity.getAlpha()).isEqualTo(0.5f)
    }

    @Test
    fun coreEntityNode_alpha_shouldAppendExisting() {
        composeTestRule.setContent {
            Subspace {
                SpatialAndroidViewPanel(
                    factory = { View(it) },
                    SubspaceModifier.modifyCoreEntity { setOrAppendAlpha(0.5f) }
                        .modifyCoreEntity { setOrAppendAlpha(0.5f) }
                        .testTag("panel"),
                )
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelSceneCoreEntity = panelNode.semanticsEntity as PanelEntity?
        assertNotNull(panelSceneCoreEntity)
        assertThat(panelSceneCoreEntity.getAlpha()).isEqualTo(0.25f)
    }

    @Test
    fun coreEntityNode_alpha_shouldAppendExistingWithGap() {
        composeTestRule.setContent {
            Subspace {
                SpatialAndroidViewPanel(
                    factory = { View(it) },
                    SubspaceModifier.modifyCoreEntity { setOrAppendAlpha(0.5f) }
                        .testTag("panel")
                        .modifyCoreEntity { setOrAppendScale(4f) }
                        .modifyCoreEntity { setOrAppendAlpha(0.5f) },
                )
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelSceneCoreEntity = panelNode.semanticsEntity as PanelEntity?
        assertNotNull(panelSceneCoreEntity)
        assertThat(panelSceneCoreEntity.getAlpha()).isEqualTo(0.25f)
    }

    @Test
    fun coreEntityNode_alpha_shouldClampAppendedResult() {
        composeTestRule.setContent {
            Subspace {
                SpatialAndroidViewPanel(
                    factory = { View(it) },
                    SubspaceModifier.modifyCoreEntity { setOrAppendAlpha(0.5f) }
                        .modifyCoreEntity { setOrAppendAlpha(4f) }
                        .testTag("panel"),
                )
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelSceneCoreEntity = panelNode.semanticsEntity as PanelEntity?
        assertNotNull(panelSceneCoreEntity)
        assertThat(panelSceneCoreEntity.getAlpha()).isEqualTo(1f)
    }

    @Test
    fun coreEntityNode_scale_shouldBeApplied() {
        composeTestRule.setContent {
            Subspace {
                SpatialAndroidViewPanel(
                    factory = { View(it) },
                    SubspaceModifier.modifyCoreEntity { setOrAppendScale(4f) }.testTag("panel"),
                )
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelSceneCoreEntity = panelNode.semanticsEntity as PanelEntity?
        assertNotNull(panelSceneCoreEntity)
        assertThat(panelSceneCoreEntity.getScale()).isEqualTo(4f)
    }

    @Test
    fun coreEntityNode_scale_shouldAppendExisting() {
        composeTestRule.setContent {
            Subspace {
                SpatialAndroidViewPanel(
                    factory = { View(it) },
                    SubspaceModifier.modifyCoreEntity { setOrAppendScale(4f) }
                        .modifyCoreEntity { setOrAppendScale(0.5f) }
                        .testTag("panel"),
                )
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelSceneCoreEntity = panelNode.semanticsEntity as PanelEntity?
        assertNotNull(panelSceneCoreEntity)
        assertThat(panelSceneCoreEntity.getScale()).isEqualTo(2f)
    }

    @Test
    fun coreEntityNode_scale_shouldAppendExistingWithGap() {
        composeTestRule.setContent {
            Subspace {
                SpatialAndroidViewPanel(
                    factory = { View(it) },
                    SubspaceModifier.modifyCoreEntity { setOrAppendScale(4f) }
                        .testTag("panel")
                        .modifyCoreEntity { setOrAppendAlpha(0.5f) }
                        .modifyCoreEntity { setOrAppendScale(0.5f) },
                )
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelSceneCoreEntity = panelNode.semanticsEntity as PanelEntity?
        assertNotNull(panelSceneCoreEntity)
        assertThat(panelSceneCoreEntity.getScale()).isEqualTo(2f)
    }

    @Test
    fun coreEntityNode_scale_shouldOnlyApplyWhenCalled() {
        var alpha by mutableFloatStateOf(0.5f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(
                    SubspaceModifier.testTag("box").modifyCoreEntity { setOrAppendAlpha(alpha) }
                ) {}
            }
        }

        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("box")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.getAlpha()
            )
            .isWithin(0.01f)
            .of(0.5f)

        composeTestRule
            .onSubspaceNodeWithTag("box")
            .fetchSemanticsNode()
            .semanticsEntity
            ?.setScale(0.4f)

        assertThat(composeTestRule.onSubspaceNodeWithTag("box").fetchSemanticsNode().scale)
            .isWithin(0.01f)
            .of(0.4f)

        alpha = 1f

        assertThat(composeTestRule.onSubspaceNodeWithTag("box").fetchSemanticsNode().scale)
            .isWithin(0.01f)
            .of(0.4f)
        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("box")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.getAlpha()
            )
            .isWithin(0.01f)
            .of(1f)
    }

    @Test
    fun coreEntityNode_alpha_shouldOnlyApplyWhenCalled() {
        var scale by mutableFloatStateOf(0.5f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(
                    SubspaceModifier.testTag("box").modifyCoreEntity { setOrAppendScale(scale) }
                ) {}
            }
        }

        assertThat(composeTestRule.onSubspaceNodeWithTag("box").fetchSemanticsNode().scale)
            .isWithin(0.01f)
            .of(0.5f)

        composeTestRule
            .onSubspaceNodeWithTag("box")
            .fetchSemanticsNode()
            .semanticsEntity
            ?.setAlpha(0.4f)

        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("box")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.getAlpha()
            )
            .isWithin(0.01f)
            .of(0.4f)

        scale = 1f

        assertThat(composeTestRule.onSubspaceNodeWithTag("box").fetchSemanticsNode().scale)
            .isWithin(0.01f)
            .of(1f)
        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("box")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.getAlpha()
            )
            .isWithin(0.01f)
            .of(0.4f)
    }
}
