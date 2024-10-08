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

package androidx.compose.ui.node

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HitTestTouchBoundsExpansionTest {
    @Test
    fun hitTest_expandedBounds() {
        // Density is 1f by default so 1dp equals to 1px in the tests.
        val pointerInputModifier1 =
            FakePointerInputModifierNode(touchBoundsExpansion = TouchBoundsExpansion(1, 2, 3, 4))

        val outerNode =
            LayoutNode(0, 0, 100, 100) {
                childNode(10, 10, 20, 20, pointerInputModifier1.toModifier())
            }

        assertTouchBounds(pointerInputModifier1, outerNode, Rect(9f, 8f, 23f, 24f))
    }

    @Test
    fun hitTest_expandedBounds_rtlLayout() {
        // Density is 1f by default so 1dp equals to 1px in the tests.
        val pointerInputModifier1 =
            FakePointerInputModifierNode(touchBoundsExpansion = TouchBoundsExpansion(1, 2, 3, 4))

        val outerNode =
            LayoutNode(0, 0, 100, 100) {
                childNode(
                    10,
                    10,
                    20,
                    20,
                    pointerInputModifier1.toModifier(),
                    layoutDirection = LayoutDirection.Rtl
                )
            }

        // start is applied on left and end is applied on right.
        assertTouchBounds(pointerInputModifier1, outerNode, Rect(7f, 8f, 21f, 24f))
    }

    @Test
    fun hitTest_expandedBounds_absolute_rtlLayout() {
        // Density is 1f by default so 1dp equals to 1px in the tests.
        val pointerInputModifier1 =
            FakePointerInputModifierNode(
                touchBoundsExpansion = TouchBoundsExpansion.Absolute(1, 2, 3, 4)
            )

        val outerNode =
            LayoutNode(0, 0, 100, 100) {
                childNode(
                    10,
                    10,
                    20,
                    20,
                    pointerInputModifier1.toModifier(),
                    layoutDirection = LayoutDirection.Rtl
                )
            }

        assertTouchBounds(pointerInputModifier1, outerNode, Rect(9f, 8f, 23f, 24f))
    }

    @Test
    fun hitTest_expandedBounds_largerThanParentBounds() {
        // Density is 1f by default so 1dp equals to 1px in the tests.
        val pointerInputModifier1 =
            FakePointerInputModifierNode(touchBoundsExpansion = TouchBoundsExpansion(1, 2, 3, 4))

        val outerNode =
            LayoutNode(0, 0, 20, 20) {
                childNode(10, 10, 20, 20, pointerInputModifier1.toModifier())
            }

        assertTouchBounds(pointerInputModifier1, outerNode, Rect(9f, 8f, 23f, 24f))
    }

    @Test
    fun hitTest_expandedBounds_parentCantInterceptOutOfBoundsChildEvent() {
        // Density is 1f by default so 1dp equals to 1px in the tests.

        val pointerInputModifierParent =
            FakePointerInputModifierNode(interceptOutOfBoundsChildEvents = true)

        val pointerInputModifier =
            FakePointerInputModifierNode(touchBoundsExpansion = TouchBoundsExpansion(1, 2, 3, 4))

        val outerNode =
            LayoutNode(0, 0, 20, 20, pointerInputModifierParent.toModifier()) {
                childNode(30, 30, 40, 40, pointerInputModifier.toModifier())
            }

        // The pointer hits the childNode's expanded touch bounds, the parent can't intercept
        // the event.
        assertThat(outerNode.hitTest(Offset(29f, 28f))).isEqualTo(listOf(pointerInputModifier))

        // The pointer directly hits the childNode, the parent can intercept the event.
        assertThat(outerNode.hitTest(Offset(35f, 35f)))
            .isEqualTo(listOf(pointerInputModifierParent, pointerInputModifier))
    }

    @Test
    fun hitTest_expandedBounds_overlapsWithMinimumTouchTargetBounds_expandedBoundsWin() {
        // Density is 1f by default so 1dp equals to 1px in the tests.

        val pointerInputModifier1 =
            FakePointerInputModifierNode(
                touchBoundsExpansion = TouchBoundsExpansion(10, 10, 10, 10)
            )

        val pointerInputModifier2 = FakePointerInputModifierNode()

        // Both nodes has it's touch bounds expanded by 10.dp.
        val outerNode =
            LayoutNode(0, 0, 20, 20) {
                childNode(0, 0, 10, 10, pointerInputModifier1.toModifier())
                childNode(
                    0,
                    20,
                    10,
                    30,
                    pointerInputModifier2.toModifier(),
                    minimumTouchTargetSize = DpSize(30.dp, 30.dp)
                )
            }

        // The pointer hits the overlapping area, the node with expanded touch bounds has the event.
        assertThat(outerNode.hitTest(Offset(5f, 15f))).isEqualTo(listOf(pointerInputModifier1))
    }

    @Test
    fun hitTest_expandedBounds_overlapsWithDirectHit_DirectHitWin() {
        // Density is 1f by default so 1dp equals to 1px in the tests.

        val pointerInputModifier1 =
            FakePointerInputModifierNode(
                touchBoundsExpansion = TouchBoundsExpansion(10, 10, 10, 10)
            )

        val pointerInputModifier2 = FakePointerInputModifierNode()

        // Both nodes has it's touch bounds expanded by 10.dp.
        val outerNode =
            LayoutNode(0, 0, 20, 20) {
                childNode(0, 0, 10, 10, pointerInputModifier1.toModifier())
                childNode(0, 10, 10, 20, pointerInputModifier2.toModifier())
            }

        // The pointer hits the overlapping area, the node with expanded touch bounds has the event.
        assertThat(outerNode.hitTest(Offset(5f, 15f))).isEqualTo(listOf(pointerInputModifier2))
    }

    @Test
    fun hitTest_expandedBounds_overlaps_shareEvents() {
        // Density is 1f by default so 1dp equals to 1px in the tests.

        val pointerInputModifier1 =
            FakePointerInputModifierNode(
                touchBoundsExpansion = TouchBoundsExpansion(10, 10, 10, 10)
            )

        val pointerInputModifier2 =
            FakePointerInputModifierNode(
                touchBoundsExpansion = TouchBoundsExpansion(10, 10, 10, 10)
            )

        // Both nodes has it's touch bounds expanded by 10.dp.
        val outerNode =
            LayoutNode(0, 0, 20, 20) {
                childNode(0, 0, 10, 10, pointerInputModifier1.toModifier())
                childNode(0, 20, 10, 30, pointerInputModifier2.toModifier())
            }

        // The pointer hits the expanded touch bounds' overlapping area, the pointer is shared.
        // PointerInputModifier2 is hit first because it's attached on the newly added child with
        // higher z index.
        assertThat(outerNode.hitTest(Offset(5f, 15f)))
            .isEqualTo(listOf(pointerInputModifier2, pointerInputModifier1))
    }

    @Test
    fun hitTest_expandedBounds_applyOnPointerInputModifierLevel() {
        // Density is 1f by default so 1dp equals to 1px in the tests.
        val pointerInputModifier1 =
            FakePointerInputModifierNode(touchBoundsExpansion = TouchBoundsExpansion(1, 2, 3, 4))
        val pointerInputModifier2 = FakePointerInputModifierNode()

        val outerNode =
            LayoutNode(0, 0, 20, 20) {
                childNode(
                    10,
                    10,
                    20,
                    20,
                    pointerInputModifier1.toModifier().then(pointerInputModifier2.toModifier())
                )
            }

        // Hit in expanded touch bounds, only pointerInputModifier1 is hit
        assertTouchBounds(pointerInputModifier1, outerNode, Rect(9f, 8f, 23f, 24f))
        val hit = outerNode.hitTest(Offset(15f, 15f))
        // Hit inside the node, both modifiers are hit
        assertThat(hit).isEqualTo(listOf(pointerInputModifier1, pointerInputModifier2))
    }

    private fun assertTouchBounds(
        pointerInputModifierNode: PointerInputModifierNode,
        layoutNode: LayoutNode,
        expectedBounds: Rect
    ) {
        assertThat(layoutNode.hitTest(expectedBounds.topLeft))
            .isEqualTo(listOf(pointerInputModifierNode))

        // Right edge is not inclusive, minus 0.01f here.
        assertThat(layoutNode.hitTest(expectedBounds.topRight - Offset(0.01f, 0f)))
            .isEqualTo(listOf(pointerInputModifierNode))

        // Bottom edge is not inclusive, minus 0.01f here.
        assertThat(layoutNode.hitTest(expectedBounds.bottomLeft - Offset(0f, 0.01f)))
            .isEqualTo(listOf(pointerInputModifierNode))

        // Bottom and right edge are not inclusive, minus 0.01f here.
        assertThat(layoutNode.hitTest(expectedBounds.bottomRight - Offset(0.01f, 0.01f)))
            .isEqualTo(listOf(pointerInputModifierNode))

        // Also assert that the touch is exactly the expected bounds.
        assertThat(layoutNode.hitTest(expectedBounds.topLeft - Offset(0.01f, 0.01f))).isEmpty()
        assertThat(layoutNode.hitTest(expectedBounds.topRight)).isEmpty()
        assertThat(layoutNode.hitTest(expectedBounds.bottomLeft)).isEmpty()
        assertThat(layoutNode.hitTest(expectedBounds.bottomRight)).isEmpty()
    }
}

internal fun LayoutNode(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    modifier: Modifier = Modifier,
    minimumTouchTargetSize: DpSize = DpSize.Zero,
    block: LayoutNode.() -> Unit
): LayoutNode {
    val root =
        LayoutNode(left, top, right, bottom, modifier, minimumTouchTargetSize).apply {
            attach(MockOwner())
        }

    block.invoke(root)
    return root
}

internal fun LayoutNode.childNode(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
    modifier: Modifier = Modifier,
    minimumTouchTargetSize: DpSize = DpSize.Zero,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    block: LayoutNode.() -> Unit = {}
): LayoutNode {
    val layoutNode =
        LayoutNode(left, top, right, bottom, modifier, minimumTouchTargetSize, layoutDirection)
    add(layoutNode)
    layoutNode.onNodePlaced()
    block.invoke(layoutNode)
    return layoutNode
}

internal fun FakePointerInputModifierNode.toModifier(): Modifier {
    return object : ModifierNodeElement<FakePointerInputModifierNode>() {
        override fun create(): FakePointerInputModifierNode = this@toModifier

        override fun update(node: FakePointerInputModifierNode) {}

        override fun hashCode(): Int {
            return touchBoundsExpansion.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (other !is FakePointerInputModifierNode) return false
            return touchBoundsExpansion == other.touchBoundsExpansion
        }
    }
}

internal class FakePointerInputModifierNode(
    override var touchBoundsExpansion: TouchBoundsExpansion = TouchBoundsExpansion.None,
    val interceptOutOfBoundsChildEvents: Boolean = false,
) : Modifier.Node(), PointerInputModifierNode {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {}

    override fun onCancelPointerInput() {}

    override fun interceptOutOfBoundsChildEvents(): Boolean {
        return interceptOutOfBoundsChildEvents
    }
}

internal fun LayoutNode.hitTest(
    pointerPosition: Offset,
    isTouchEvent: Boolean = true
): List<Modifier.Node> {
    val hitTestResult = HitTestResult()
    hitTest(pointerPosition, hitTestResult, isTouchEvent)
    return hitTestResult
}

private fun LayoutNode.onNodePlaced() = measurePassDelegate.onNodePlaced()
