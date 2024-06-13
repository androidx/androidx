/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.draganddrop.DragAndDropManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.modifier.ModifierLocalManager
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.InteropView
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

// Adopted copy of compose/ui/ui/src/androidAndroidTest/kotlin/androidx/compose/ui/platform/DepthSortedSetTest.kt
@OptIn(ExperimentalTestApi::class)
class DepthSortedSetTest {

    @Test
    fun sortedByDepth() = runSkikoComposeUiTest {
        val owner = DepthTestOwner()
        val root = LayoutNode()
        root.attach(owner)
        val child1 = LayoutNode()
        root.add(child1)
        val child2 = LayoutNode()
        child1.add(child2)
        val child3 = LayoutNode()
        child2.add(child3)

        val set = DepthSortedSet(extraAssertions = true)
        set.add(child2)
        set.add(child3)
        set.add(root)
        set.add(child1)

        assertEquals(root, set.pop())
        assertEquals(child1, set.pop())
        assertEquals(child2, set.pop())
        assertEquals(child3, set.pop())
        assertTrue(set.isEmpty())
    }

    @Test
    fun sortedByDepthWithItemsOfTheSameDepth() = runSkikoComposeUiTest {
        val owner = DepthTestOwner()
        val root = LayoutNode()
        root.attach(owner)
        val child1 = LayoutNode()
        root.add(child1)
        val child2 = LayoutNode()
        root.add(child2)
        val child3 = LayoutNode()
        child1.add(child3)

        val set = DepthSortedSet(extraAssertions = true)
        set.add(child1)
        set.add(child3)
        set.add(child2)
        set.add(root)

        assertEquals(root, set.pop())
        val result = set.pop()
        if (result === child1) {
            assertEquals(child2, set.pop())
        } else {
            assertEquals(child2, result)
            assertEquals(child1, set.pop())
        }
        assertEquals(child3, set.pop())
        assertTrue(set.isEmpty())
    }

    @Test
    fun modifyingSetWhileWeIterate() = runSkikoComposeUiTest {
        val owner = DepthTestOwner()
        val root = LayoutNode()
        root.attach(owner)
        val child1 = LayoutNode()
        root.add(child1)
        val child2 = LayoutNode()
        child1.add(child2)
        val child3 = LayoutNode()
        child2.add(child3)

        val set = DepthSortedSet(extraAssertions = true)
        set.add(child1)
        set.add(child3)
        var expected: LayoutNode? = child1

        set.popEach {
            assertEquals(expected, it)
            if (expected === child1) {
                set.add(child2)
                set.add(root)
                // now we expect root
                expected = root
            } else if (expected === root) {
                // remove child3 so we will never reach it
                set.remove(child3)
                // now we expect the last item
                expected = child2
            } else {
                assertEquals(child2, it)
                // no other items expected, child3 was removed already
                expected = null
            }
        }
        // assert we iterated the whole set
        assertEquals(null, expected)
    }

    @Test
    fun addingNotAttachedNodeThrows() {
        val set = DepthSortedSet(extraAssertions = true)
        assertFailsWith<IllegalStateException> {
            set.add(LayoutNode())
        }
    }

    @Test
    fun modifyingDepthAfterAddingThrows() = runSkikoComposeUiTest {
        val owner = DepthTestOwner()
        val root = LayoutNode()
        root.attach(owner)
        val child1 = LayoutNode()
        root.add(child1)
        val child2 = LayoutNode()
        child1.add(child2)

        val set = DepthSortedSet(extraAssertions = true)
        set.add(child2)

        // change depth of child2
        child1.removeAt(0, 1)
        root.add(child2)
        // now it is on the same level as child1

        assertTrue(set.isNotEmpty())
        // throws because we changed the depth
        assertFailsWith<IllegalStateException> {
            set.pop()
        }
    }

    // TODO Is there a way to mock it in K/N?
    internal class DepthTestOwner : Owner {
        override val rootForTest: RootForTest get() = throw IllegalStateException()
        override val hapticFeedBack: HapticFeedback get() = throw IllegalStateException()
        override val inputModeManager: InputModeManager get() = throw IllegalStateException()
        override val clipboardManager: ClipboardManager get() = throw IllegalStateException()
        override val accessibilityManager: AccessibilityManager get() = throw IllegalStateException()
        override val graphicsContext: GraphicsContext
            get() = TODO("Not yet implemented")
        override val textToolbar: TextToolbar get() = throw IllegalStateException()
        @ExperimentalComposeUiApi
        override val autofillTree: AutofillTree get() = throw IllegalStateException()
        @ExperimentalComposeUiApi
        override val autofill: Autofill? get() = throw IllegalStateException()
        override val density: Density get() = throw IllegalStateException()
        override val textInputService: TextInputService get() = throw IllegalStateException()
        override val softwareKeyboardController get() = throw IllegalStateException()
        override suspend fun textInputSession(
            session: suspend PlatformTextInputSessionScope.() -> Nothing
        ) = throw IllegalStateException()
        override fun screenToLocal(positionOnScreen: Offset): Offset = throw IllegalStateException()
        override fun localToScreen(localPosition: Offset): Offset = throw IllegalStateException()
        override fun localToScreen(localTransform: Matrix) = throw IllegalStateException()
        override val dragAndDropManager: DragAndDropManager get() = throw IllegalStateException()
        override val pointerIconService: PointerIconService get() = throw IllegalStateException()
        override val focusOwner: FocusOwner get() = throw IllegalStateException()
        override val windowInfo: WindowInfo get() = throw IllegalStateException()
        @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
        override val fontLoader: Font.ResourceLoader get() = throw IllegalStateException()
        override val fontFamilyResolver: FontFamily.Resolver get() = throw IllegalStateException()
        override val layoutDirection: LayoutDirection get() = throw IllegalStateException()
        override var showLayoutBounds = false @InternalCoreApi set
        override fun onRequestMeasure(layoutNode: LayoutNode, affectsLookahead: Boolean, forceRequest: Boolean, scheduleMeasureAndLayout: Boolean) = Unit
        override fun onRequestRelayout(layoutNode: LayoutNode, affectsLookahead: Boolean, forceRequest: Boolean) = Unit
        override fun requestOnPositionedCallback(layoutNode: LayoutNode) = Unit
        override fun onAttach(node: LayoutNode) = Unit
        override fun onDetach(node: LayoutNode) = Unit
        override fun calculatePositionInWindow(localPosition: Offset): Offset = throw IllegalStateException()
        override fun calculateLocalPosition(positionInWindow: Offset): Offset = throw IllegalStateException()
        override fun requestFocus(): Boolean = throw IllegalStateException()
        override fun measureAndLayout(sendPointerUpdate: Boolean) = throw IllegalStateException()
        override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) = throw IllegalStateException()
        override fun forceMeasureTheSubtree(layoutNode: LayoutNode, affectsLookahead: Boolean) = throw IllegalStateException()
        override fun createLayer(
            drawBlock: (canvas: Canvas, parentLayer: GraphicsLayer?) -> Unit,
            invalidateParentLayer: () -> Unit,
            explicitLayer: GraphicsLayer?
        ): OwnedLayer = throw IllegalStateException()
        override fun onSemanticsChange() = throw IllegalStateException()
        override fun onLayoutChange(layoutNode: LayoutNode) = throw IllegalStateException()

        @InternalComposeUiApi
        override fun onInteropViewLayoutChange(view: InteropView) = throw IllegalStateException()

        override fun getFocusDirection(keyEvent: KeyEvent): FocusDirection? = throw IllegalStateException()
        override val measureIteration: Long get() = throw IllegalStateException()
        override val viewConfiguration: ViewConfiguration get() = throw IllegalStateException()
        override val snapshotObserver: OwnerSnapshotObserver get() = throw IllegalStateException()
        override val modifierLocalManager: ModifierLocalManager get() = throw IllegalStateException()
        override val coroutineContext: CoroutineContext get() = throw IllegalStateException()
        override fun registerOnEndApplyChangesListener(listener: () -> Unit) = throw IllegalStateException()
        override fun onEndApplyChanges() = throw IllegalStateException()
        override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) = throw IllegalStateException()

        // That's what actually needed
        override val sharedDrawScope = LayoutNodeDrawScope()
        override val root: LayoutNode get() = LayoutNode()
    }
}
