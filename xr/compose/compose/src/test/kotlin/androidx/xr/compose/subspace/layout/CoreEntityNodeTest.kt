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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.ApplicationSubspace
import androidx.xr.compose.subspace.SpatialAndroidViewPanel
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.unit.IntVolumeSize
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

    private class ModifyCoreEntityElement(private var modify: CoreEntityScope.() -> Unit) :
        SubspaceModifierNodeElement<ModifyCoreEntityNode>() {
        override fun create(): ModifyCoreEntityNode = ModifyCoreEntityNode(modify)

        override fun update(node: ModifyCoreEntityNode) {
            node.modify = modify
        }

        override fun hashCode(): Int {
            return modify.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ModifyCoreEntityElement) return false

            return modify === other.modify
        }
    }

    private class ModifyCoreEntityNode(var modify: CoreEntityScope.() -> Unit) :
        SubspaceModifier.Node(), CoreEntityNode {
        override fun CoreEntityScope.modifyCoreEntity() {
            modify()
        }
    }

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun testRenderedSize_shouldBeApplied() {
        composeTestRule.setContent {
            TestSetup {
                ApplicationSubspace {
                    SpatialAndroidViewPanel(
                        factory = { View(it) },
                        SubspaceModifier.modifyCoreEntity {
                                setRenderedSize(IntVolumeSize(100, 100, 0))
                            }
                            .modifyCoreEntity { setOrAppendScale(4f) }
                            .testTag("panel"),
                    )
                }
            }
        }

        val panelNode = composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode()
        val panelSceneCoreEntity = panelNode.semanticsEntity as PanelEntity?
        assertNotNull(panelSceneCoreEntity)
        assertThat(panelSceneCoreEntity.sizeInPixels.width).isEqualTo(100)
        assertThat(panelSceneCoreEntity.sizeInPixels.height).isEqualTo(100)
    }
}
