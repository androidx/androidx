/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.IntSize
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HitTestSharePointerInputWithSiblingTest {
    @Test
    fun hitTest_sharePointerWithSibling() {
        val pointerInputModifier1 = FakePointerInputModifierNode(sharePointerWithSiblings = true)
        val pointerInputModifier2 = FakePointerInputModifierNode(sharePointerWithSiblings = true)

        val outerNode = LayoutNode(0, 0, 1, 1) {
            childNode(0, 0, 1, 1, pointerInputModifier1.toModifier())
            childNode(0, 0, 1, 1, pointerInputModifier2.toModifier())
        }

        val hit = mutableListOf<Modifier.Node>()

        outerNode.hitTest(Offset(0f, 0f), hit)

        assertThat(hit).isEqualTo(listOf(pointerInputModifier2, pointerInputModifier1))
    }

    @Test
    fun hitTest_sharePointerWithSibling_utilFirstNodeNotSharing() {
        val pointerInputModifier1 = FakePointerInputModifierNode(sharePointerWithSiblings = true)
        val pointerInputModifier2 = FakePointerInputModifierNode(sharePointerWithSiblings = false)
        val pointerInputModifier3 = FakePointerInputModifierNode(sharePointerWithSiblings = true)

        val outerNode = LayoutNode(0, 0, 1, 1) {
            childNode(0, 0, 1, 1, pointerInputModifier1.toModifier())
            childNode(0, 0, 1, 1, pointerInputModifier2.toModifier())
            childNode(0, 0, 1, 1, pointerInputModifier3.toModifier())
        }

        val hit = mutableListOf<Modifier.Node>()

        outerNode.hitTest(Offset(0f, 0f), hit)

        assertThat(hit).isEqualTo(listOf(pointerInputModifier3, pointerInputModifier2))
    }

    @Test
    fun hitTest_sharePointerWithSibling_whenParentDisallowShare() {
        val pointerInputModifier1 = FakePointerInputModifierNode(sharePointerWithSiblings = false)
        val pointerInputModifier2 = FakePointerInputModifierNode(sharePointerWithSiblings = true)
        val pointerInputModifier3 = FakePointerInputModifierNode(sharePointerWithSiblings = true)

        val outerNode = LayoutNode(0, 0, 1, 1) {
            childNode(0, 0, 1, 1, pointerInputModifier1.toModifier()) {
                childNode(0, 0, 1, 1, pointerInputModifier2.toModifier())
                childNode(0, 0, 1, 1, pointerInputModifier3.toModifier())
            }
        }

        val hit = mutableListOf<Modifier.Node>()

        outerNode.hitTest(Offset(0f, 0f), hit)

        // The parent node doesn't share pointer events, the two children can still share events.
        assertThat(hit).isEqualTo(
            listOf(pointerInputModifier1, pointerInputModifier3, pointerInputModifier2)
        )
    }

    @Test
    fun hitTest_sharePointerWithSiblingTrue_shareWithCousin() {
        val pointerInputModifier1 = FakePointerInputModifierNode(sharePointerWithSiblings = true)
        val pointerInputModifier2 = FakePointerInputModifierNode(sharePointerWithSiblings = true)

        val outerNode = LayoutNode(0, 0, 1, 1) {
            childNode(0, 0, 1, 1, pointerInputModifier1.toModifier())
            childNode(0, 0, 1, 1) {
                childNode(0, 0, 1, 1, pointerInputModifier2.toModifier())
            }
        }

        val hit = mutableListOf<Modifier.Node>()

        outerNode.hitTest(Offset(0f, 0f), hit)

        assertThat(hit).isEqualTo(listOf(pointerInputModifier2, pointerInputModifier1))
    }

    @Test
    fun hitTest_parentDisallowShare_notShareWithCousin() {
        val pointerInputModifier1 = FakePointerInputModifierNode(sharePointerWithSiblings = true)
        val pointerInputModifier2 = FakePointerInputModifierNode(sharePointerWithSiblings = false)
        val pointerInputModifier3 = FakePointerInputModifierNode(sharePointerWithSiblings = true)

        val outerNode = LayoutNode(0, 0, 1, 1) {
            childNode(0, 0, 1, 1, pointerInputModifier1.toModifier())
            childNode(0, 0, 1, 1, pointerInputModifier2.toModifier()) {
                childNode(0, 0, 1, 1, pointerInputModifier3.toModifier())
            }
        }

        val hit = mutableListOf<Modifier.Node>()

        outerNode.hitTest(Offset(0f, 0f), hit)

        // PointerInputModifier1 can't receive events because pointerInputModifier2 doesn't share.
        assertThat(hit).isEqualTo(listOf(pointerInputModifier2, pointerInputModifier3))
    }

    @Test
    fun hitTest_sharePointerWithCousin_untilFirstNodeNotSharing() {
        val pointerInputModifier1 = FakePointerInputModifierNode(sharePointerWithSiblings = true)
        val pointerInputModifier2 = FakePointerInputModifierNode(sharePointerWithSiblings = false)
        val pointerInputModifier3 = FakePointerInputModifierNode(sharePointerWithSiblings = true)

        val outerNode = LayoutNode(0, 0, 1, 1) {
            childNode(0, 0, 1, 1, pointerInputModifier1.toModifier())
            childNode(0, 0, 1, 1) {
                childNode(0, 0, 1, 1, pointerInputModifier2.toModifier())
            }
            childNode(0, 0, 1, 1) {
                childNode(0, 0, 1, 1, pointerInputModifier3.toModifier())
            }
        }

        val hit = mutableListOf<Modifier.Node>()

        outerNode.hitTest(Offset(0f, 0f), hit)

        assertThat(hit).isEqualTo(listOf(pointerInputModifier3, pointerInputModifier2))
    }
}

private fun LayoutNode(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    block: LayoutNode.() -> Unit
): LayoutNode {
    val root = LayoutNode(left, top, right, bottom).apply {
        attach(MockOwner())
    }

    block.invoke(root)
    return root
}

private fun LayoutNode.childNode(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    modifier: Modifier = Modifier,
    block: LayoutNode.() -> Unit = {}
): LayoutNode {
    val layoutNode = LayoutNode(left, top, right, bottom, modifier)
    add(layoutNode)
    layoutNode.onNodePlaced()
    block.invoke(layoutNode)
    return layoutNode
}

private fun FakePointerInputModifierNode.toModifier(): Modifier {
    return object : ModifierNodeElement<FakePointerInputModifierNode>() {
        override fun create(): FakePointerInputModifierNode = this@toModifier

        override fun update(node: FakePointerInputModifierNode) { }

        override fun hashCode(): Int {
            return if (this@toModifier.sharePointerWithSiblings) 1 else 0
        }

        override fun equals(other: Any?): Boolean {
           return this@toModifier.sharePointerWithSiblings
        }
    }
}

private class FakePointerInputModifierNode(
    var sharePointerWithSiblings: Boolean = false
) : Modifier.Node(), PointerInputModifierNode {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {}

    override fun onCancelPointerInput() {}

    override fun sharePointerInputWithSiblings(): Boolean = this.sharePointerWithSiblings
}

private fun LayoutNode.hitTest(
    pointerPosition: Offset,
    hitPointerInputFilters: MutableList<Modifier.Node>,
    isTouchEvent: Boolean = false
) {
    val hitTestResult = HitTestResult()
    hitTest(pointerPosition, hitTestResult, isTouchEvent)
    hitPointerInputFilters.addAll(hitTestResult)
}

private fun LayoutNode.onNodePlaced() = measurePassDelegate.onNodePlaced()
