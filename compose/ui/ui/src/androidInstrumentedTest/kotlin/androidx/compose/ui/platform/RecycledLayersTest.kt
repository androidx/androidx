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
package androidx.compose.ui.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isIdentity
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.NodeCoordinator
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.OwnedLayer
import androidx.compose.ui.node.requireCoordinator
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RecycledLayersTest {
    @get:Rule val rule = createComposeRule()

    /**
     * When drawn content is moved between ComposeViews, the graphics layers should not be shared.
     */
    @Test
    fun layersKeptInComposeRoot() {
        class InvalidatingNode : Modifier.Node(), DrawModifierNode {
            var layer: OwnedLayer? = null

            override fun ContentDrawScope.draw() {
                layer = findLayer(requireCoordinator(Nodes.Any))
            }

            private fun findLayer(nodeCoordinator: NodeCoordinator): OwnedLayer? {
                return nodeCoordinator.layer ?: nodeCoordinator.wrappedBy?.layer
            }
        }

        var node: InvalidatingNode? = null

        class InvalidatingElement : ModifierNodeElement<InvalidatingNode>() {
            override fun create(): InvalidatingNode = InvalidatingNode().also { node = it }

            override fun hashCode(): Int = 0

            override fun equals(other: Any?): Boolean {
                return true
            }

            override fun update(node: InvalidatingNode) {}
        }

        var showDialog by mutableStateOf(false)
        rule.setContent {
            val content = remember {
                movableContentOf {
                    Box(
                        Modifier.graphicsLayer { rotationZ = 90f }
                            .size(50.dp)
                            .then(InvalidatingElement())
                    )
                }
            }
            Box(Modifier.fillMaxSize()) {
                if (!showDialog) {
                    content()
                } else {
                    Dialog(onDismissRequest = {}) { content() }
                }
            }
        }
        rule.waitForIdle()
        assertThat(node).isNotNull()
        val firstLayer = node?.layer
        assertThat(firstLayer).isNotNull()
        assertThat(firstLayer!!.underlyingMatrix.isIdentity()).isFalse()
        val firstLayerMatrixValues = firstLayer.underlyingMatrix.values.copyOf()

        showDialog = true
        rule.waitForIdle()

        val secondLayer = node?.layer
        assertThat(secondLayer).isNotNull()
        assertThat(secondLayer).isNotSameInstanceAs(firstLayer)
        assertThat(secondLayer!!.underlyingMatrix.values.contentEquals(firstLayerMatrixValues))
            .isTrue()

        showDialog = false
        rule.waitForIdle()

        val thirdLayer = node?.layer
        assertThat(thirdLayer).isNotNull()
        // It is likely to be the same instance as firstLayer, but only by coincidence -- it has
        // been recycled. We don't want to rely on the recycling behavior here.
        assertThat(thirdLayer).isNotSameInstanceAs(secondLayer)
        assertThat(thirdLayer!!.underlyingMatrix.values.contentEquals(firstLayerMatrixValues))
            .isTrue()
    }
}
