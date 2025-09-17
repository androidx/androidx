/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.collection.mutableIntSetOf
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.scale
import androidx.compose.ui.semantics.SemanticsActions.ScrollBy
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.test.assertTrue
import org.junit.ComparisonFailure
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RectListIntegrationTest {

    @get:Rule val rule = createComposeRule()

    @Test
    @SmallTest
    fun testSingleBox() {
        rule.setContent { Box(Modifier.testTag("foo").size(10.dp)) }

        rule.onNodeWithTag("foo").assertRectDp(0.dp, 0.dp, 10.dp, 10.dp)
    }

    @Test
    @SmallTest
    fun testNestedBox() {
        rule.setContent {
            Box(Modifier.padding(10.dp)) { Box(Modifier.testTag("foo").size(10.dp)) }
        }

        rule.onNodeWithTag("foo").assertRectDp(10.dp, 10.dp, 20.dp, 20.dp)
    }

    @Test
    @SmallTest
    fun testUpdatePosition() {
        var toggle by mutableStateOf(false)

        rule.setContent {
            Box(Modifier.padding(if (toggle) 50.dp else 10.dp)) {
                Box(Modifier.testTag("foo").size(10.dp))
            }
        }

        rule.onNodeWithTag("foo").assertRectDp(10.dp, 10.dp, 20.dp, 20.dp)

        rule.runOnIdle { toggle = !toggle }

        rule.onNodeWithTag("foo").assertRectDp(50.dp, 50.dp, 60.dp, 60.dp)
    }

    @Test
    @SmallTest
    fun testUpdateTextPadding() {
        var padding by mutableIntStateOf(0)

        rule.setContent {
            Box(Modifier.background(Color.Yellow).size(200.dp)) {
                Text("Row", Modifier.testTag("text").padding(padding.dp).size(40.dp))
            }
        }

        rule.onNodeWithTag("text").assertRectDp(0.dp, 0.dp, 40.dp, 40.dp)

        rule.runOnIdle { padding += 10 }

        rule.onNodeWithTag("text").assertRectDp(0.dp, 0.dp, 60.dp, 60.dp)
    }

    @Test
    @SmallTest
    fun testPaddings() {
        rule.setContent {
            Box(Modifier.padding(20.dp)) {
                Box(Modifier.padding(20.dp)) {
                    Box(Modifier.padding(60.dp)) { Box(Modifier.testTag("test").size(100.dp)) }
                }
            }
        }

        rule.onNodeWithTag("test").assertRectDp(100.dp, 100.dp, 200.dp, 200.dp)
    }

    @Test
    @SmallTest
    fun testPaddingsTwo() {
        rule.setContent {
            Box(Modifier.padding(4.dp)) { Box(Modifier.testTag("test").padding(4.dp).size(4.dp)) }
        }

        rule.onNodeWithTag("test").assertRectDp(4.dp, 4.dp, 16.dp, 16.dp)
    }

    @Test
    @SmallTest
    fun testPaddingsThree() {
        var padding by mutableIntStateOf(20)
        rule.setContent {
            Box(Modifier.padding(padding.dp)) {
                Box(Modifier.padding(20.dp)) {
                    Box(Modifier.padding(60.dp)) { Box(Modifier.testTag("test").size(100.dp)) }
                }
            }
        }

        rule.onNodeWithTag("test").assertRectDp(100.dp, 100.dp, 200.dp, 200.dp)
        padding += 10
        rule.onNodeWithTag("test").assertRectDp(110.dp, 110.dp, 210.dp, 210.dp)
    }

    @Test
    @SmallTest
    fun testPaddingsFour() {
        var padding by mutableIntStateOf(20)
        rule.setContent {
            Box(Modifier.offset { IntOffset(padding.dp.roundToPx(), padding.dp.roundToPx()) }) {
                Box(Modifier.padding(20.dp)) {
                    Box(Modifier.padding(60.dp)) { Box(Modifier.testTag("test").size(100.dp)) }
                }
            }
        }

        rule.onNodeWithTag("test").assertRectDp(100.dp, 100.dp, 200.dp, 200.dp)
        padding += 10
        rule.onNodeWithTag("test").assertRectDp(110.dp, 110.dp, 210.dp, 210.dp)
    }

    @Test
    @SmallTest
    fun testUpdateSize() {
        var toggle by mutableStateOf(false)

        rule.setContent { Box { Box(Modifier.testTag("foo").size(if (toggle) 50.dp else 10.dp)) } }

        rule.onNodeWithTag("foo").assertRectDp(0.dp, 0.dp, 10.dp, 10.dp)

        rule.runOnIdle { toggle = !toggle }

        rule.onNodeWithTag("foo").assertRectDp(0.dp, 0.dp, 50.dp, 50.dp)
    }

    @Test
    @SmallTest
    fun testUpdateTranslation() {
        var toggle by mutableStateOf(false)

        rule.setContent {
            Box(
                Modifier.offset {
                    if (toggle) IntOffset(10.dp.roundToPx(), 10.dp.roundToPx()) else IntOffset.Zero
                }
            ) {
                Box(Modifier.testTag("foo").size(10.dp))
            }
        }

        rule.onNodeWithTag("foo").assertRectDp(0.dp, 0.dp, 10.dp, 10.dp)

        rule.runOnIdle { toggle = !toggle }

        rule.onNodeWithTag("foo").assertRectDp(10.dp, 10.dp, 20.dp, 20.dp)
    }

    @Test
    @SmallTest
    fun testRemovingNodeRemovesRect() {
        var toggle by mutableStateOf(false)

        rule.setContent {
            if (!toggle) {
                Box(Modifier.testTag("foo").size(10.dp))
            }
        }

        val node = rule.onNodeWithTag("foo")

        node.assertRectDp(0.dp, 0.dp, 10.dp, 10.dp)

        val semanticsNode = node.fetchSemanticsNode()
        val owner = semanticsNode.layoutNode.owner as? AndroidComposeView
        val rectList = owner?.rectManager?.rects ?: error("Could not find rect list")

        val nodeId = semanticsNode.id

        rule.runOnIdle {
            assertTrue(nodeId in rectList)
            toggle = !toggle
        }

        rule.runOnIdle { assertFalse(nodeId in rectList) }
    }

    @Test
    @SmallTest
    fun removesLayoutNodeWithInputModifier_forEachGesturableIntersectionReflectsOnlyInputInUI() {
        var toggle by mutableStateOf(true)

        rule.setContent {
            Box(Modifier.size(20.dp)) {
                if (toggle) {
                    Box(
                        Modifier.testTag("inputTag").size(10.dp).pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    )
                }
            }
        }

        val node = rule.onNodeWithTag("inputTag")

        node.assertRectDp(0.dp, 0.dp, 10.dp, 10.dp)

        val semanticsNode = node.fetchSemanticsNode()
        val owner = semanticsNode.layoutNode.owner as? AndroidComposeView
        val rectList = owner?.rectManager?.rects ?: error("Could not find rect list")

        val nodeId = semanticsNode.id

        rule.runOnIdle {
            assertTrue(nodeId in rectList)

            var idFound = false
            var count = 0
            rectList.forEachGesturableIntersection(l = 0, r = 5, t = 0, b = 5) { id ->
                count++
                if (id == nodeId) {
                    idFound = true
                }
            }
            assertTrue(idFound)
            assertThat(count).isEqualTo(1)

            // Remove LayoutNode with a pointer input
            toggle = false
        }

        rule.runOnIdle {
            assertFalse(nodeId in rectList)
            var idFound = false
            var count = 0
            rectList.forEachGesturableIntersection(l = 0, r = 5, t = 0, b = 5) { id ->
                count++
                if (id == nodeId) {
                    idFound = true
                }
            }
            assertFalse(idFound)
            assertThat(count).isEqualTo(0)
        }
    }

    @Test
    @SmallTest
    fun addsLayoutNodeWithInputModifier_forEachGesturableIntersectionReflectsOnlyInputInUI() {
        var toggle by mutableStateOf(false)

        rule.setContent {
            Box(Modifier.testTag("parentTag").size(20.dp)) {
                if (toggle) {
                    Box(
                        Modifier.testTag("inputTag").size(10.dp).pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    )
                }
            }
        }

        val parentNode = rule.onNodeWithTag("parentTag")

        parentNode.assertRectDp(0.dp, 0.dp, 20.dp, 20.dp)

        val semanticsParentNode = parentNode.fetchSemanticsNode()
        val ownerParentNode = semanticsParentNode.layoutNode.owner as? AndroidComposeView
        val rectListParentNode =
            ownerParentNode?.rectManager?.rects ?: error("Could not find input node's rect list")

        val parentNodeId = semanticsParentNode.id

        rule.runOnIdle {
            assertTrue(parentNodeId in rectListParentNode)

            var idFound = false
            var count = 0
            rectListParentNode.forEachGesturableIntersection(l = 0, r = 5, t = 0, b = 5) { id ->
                count++
                if (id == parentNodeId) {
                    idFound = true
                }
            }
            assertFalse(idFound)
            assertThat(count).isEqualTo(0)

            // Add pointer input to existing LayoutNode
            toggle = true
        }

        rule.waitForIdle()

        val inputNode = rule.onNodeWithTag("inputTag")
        inputNode.assertRectDp(0.dp, 0.dp, 10.dp, 10.dp)

        val semanticsInputNode = inputNode.fetchSemanticsNode()
        val ownerInputNode = semanticsInputNode.layoutNode.owner as? AndroidComposeView
        val rectListInputNode =
            ownerInputNode?.rectManager?.rects ?: error("Could not find input node's rect list")

        val inputNodeId = semanticsInputNode.id

        rule.runOnIdle {
            assertTrue(parentNodeId in rectListParentNode)
            assertTrue(inputNodeId in rectListInputNode)

            var idFound = false
            var count = 0
            rectListInputNode.forEachGesturableIntersection(l = 0, r = 5, t = 0, b = 5) { id ->
                count++
                if (id == inputNodeId) {
                    idFound = true
                }
            }
            assertTrue(idFound)
            assertThat(count).isEqualTo(1)

            // Remove pointer input modifier from LayoutNode
            toggle = false
        }

        rule.runOnIdle {
            assertTrue(parentNodeId in rectListParentNode)
            assertFalse(inputNodeId in rectListInputNode)

            var idFound = false
            var count = 0
            rectListInputNode.forEachGesturableIntersection(l = 0, r = 5, t = 0, b = 5) { id ->
                count++
                if (id == inputNodeId) {
                    idFound = true
                }
            }
            assertFalse(idFound)
            assertThat(count).isEqualTo(0)
        }
    }

    @Test
    @SmallTest
    fun removesAndAddsInputModifierNode_forEachGesturableIntersectionReflectsOnlyInputInUI() {
        var activateDynamicPointerInput by mutableStateOf(true)

        rule.setContent {
            Box(
                Modifier.testTag("inputTag")
                    .size(10.dp)
                    .dynamicPointerInputModifier(
                        enabled = activateDynamicPointerInput,
                        key = "unique_key_123",
                        onPress = {},
                    )
            )
        }

        val node = rule.onNodeWithTag("inputTag")

        node.assertRectDp(0.dp, 0.dp, 10.dp, 10.dp)

        val semanticsNode = node.fetchSemanticsNode()
        val owner = semanticsNode.layoutNode.owner as? AndroidComposeView
        val rectList = owner?.rectManager?.rects ?: error("Could not find rect list")

        val nodeId = semanticsNode.id

        rule.runOnIdle {
            assertTrue(nodeId in rectList)

            var idFound = false
            var count = 0
            rectList.forEachGesturableIntersection(l = 0, r = 5, t = 0, b = 5) { id ->
                count++
                if (id == nodeId) {
                    idFound = true
                }
            }
            assertTrue(idFound)
            assertThat(count).isEqualTo(1)

            // Remove pointer input Modifier.Node (NOT the LayoutNode)
            activateDynamicPointerInput = false
        }

        rule.runOnIdle {
            assertTrue(nodeId in rectList)
            var idFound = false
            var count = 0
            rectList.forEachGesturableIntersection(l = 0, r = 5, t = 0, b = 5) { id ->
                count++
                if (id == nodeId) {
                    idFound = true
                }
            }
            assertFalse(idFound)
            assertThat(count).isEqualTo(0)
        }

        rule.runOnIdle { activateDynamicPointerInput = true }

        rule.runOnIdle {
            assertTrue(nodeId in rectList)

            var idFound = false
            var count = 0
            rectList.forEachGesturableIntersection(l = 0, r = 5, t = 0, b = 5) { id ->
                count++
                if (id == nodeId) {
                    idFound = true
                }
            }
            assertTrue(idFound)
            assertThat(count).isEqualTo(1)
        }
    }

    @Test
    @SmallTest
    fun testScrolling() {
        rule.setContent {
            val scrollState = rememberScrollState()
            Column(Modifier.testTag("scroller").verticalScroll(scrollState)) {
                for (i in 0 until 4) {
                    Box(Modifier.testTag("foo$i").width(200.dp).height(400.dp)) {
                        Box(Modifier.size(10.dp))
                        Box(Modifier.size(10.dp))
                        Box(Modifier.size(10.dp))
                        Box(Modifier.size(10.dp))
                    }
                }
            }
        }

        rule.onNodeWithTag("foo0").assertRectDp(0.dp, 0.dp, 200.dp, 400.dp)

        rule.onNodeWithTag("foo3").assertRectDp(0.dp, 1200.dp, 200.dp, 1600.dp)

        val scrollBy =
            rule.onNodeWithTag("scroller").fetchSemanticsNode().config[ScrollBy].action
                ?: error("No scrollByAction found")

        val scrollDistance = with(rule.density) { 100.dp.toPx() }

        scrollBy(0f, scrollDistance)

        rule.onNodeWithTag("foo0").assertRectDp(0.dp, -100.dp, 200.dp, 300.dp)

        rule.onNodeWithTag("foo3").assertRectDp(0.dp, 1100.dp, 200.dp, 1500.dp)
    }

    @Composable
    fun ColorStripe(red: Int, green: Int, blue: Int) {
        Canvas(Modifier.size(45.dp, 500.dp)) {
            drawRect(Color(red = red, green = green, blue = blue))
        }
    }

    @Test
    @SmallTest
    fun testScrollingWeirdness() {
        rule.setContent {
            val scrollState = rememberScrollState()
            Column(Modifier.verticalScroll(scrollState)) {
                Box(Modifier.testTag("foo").size(100.dp))
                Box(Modifier.testTag("bar").size(100.dp))
            }
        }

        rule.onNodeWithTag("foo").assertRectDp(0.dp, 0.dp, 100.dp, 100.dp)

        rule.onNodeWithTag("bar").assertRectDp(0.dp, 100.dp, 100.dp, 200.dp)
    }

    @Test
    @SmallTest
    fun testRotatedBox() {
        rule.setContent {
            Box(
                Modifier.testTag("outer").graphicsLayer {
                    translationX = 100.dp.toPx()
                    rotationZ = 45f
                }
            ) {
                Box(Modifier.testTag("inner").size(100.dp))
            }
        }

        rule.onNodeWithTag("outer").assertRectDp(0.dp, 0.dp, 100.dp, 100.dp)
        rule.onNodeWithTag("inner").assertRectDp(79.dp, -21.dp, 220.dp, 121.dp)
    }

    @Test
    @SmallTest
    fun testRotatedChildBoxOffset() {
        rule.setContent {
            Column(Modifier.testTag("outer").graphicsLayer { rotationZ = 45f }) {
                Box(Modifier.height(100.dp))
                Box(Modifier.testTag("inner").size(100.dp))
            }
        }

        rule.onNodeWithTag("outer").assertRectDp(0.dp, 0.dp, 100.dp, 200.dp)
        rule.onNodeWithTag("inner").assertRectDp(-56.dp, 65.dp, 85.dp, 206.dp)
    }

    @Test
    @SmallTest
    fun testRotatedChildBoxOffsetUpdated() {
        var rotation by mutableFloatStateOf(0f)
        rule.setContent {
            Column(Modifier.testTag("outer").graphicsLayer { rotationZ = rotation }) {
                Box(Modifier.height(100.dp))
                Box(Modifier.testTag("inner").size(100.dp))
            }
        }

        rule.onNodeWithTag("outer").assertRectDp(0.dp, 0.dp, 100.dp, 200.dp)
        rule.onNodeWithTag("inner").assertRectDp(0.dp, 100.dp, 100.dp, 200.dp)

        rule.runOnIdle { rotation = 45f }

        rule.onNodeWithTag("outer").assertRectDp(0.dp, 0.dp, 100.dp, 200.dp)
        rule.onNodeWithTag("inner").assertRectDp(-56.dp, 65.dp, 85.dp, 206.dp)
    }

    @Test
    @SmallTest
    fun testRotatedGrandchildBoxOffset() {
        rule.setContent {
            Column(Modifier.testTag("outer").graphicsLayer { rotationZ = 45f }) {
                Box(Modifier.height(100.dp))
                Box { Box(Modifier.testTag("inner").size(100.dp)) }
            }
        }

        rule.onNodeWithTag("outer").assertRectDp(0.dp, 0.dp, 100.dp, 200.dp)
        rule.onNodeWithTag("inner").assertRectDp(-56.dp, 65.dp, 85.dp, 206.dp)
    }

    @Test
    @SmallTest
    fun testRotatedGrandchildBoxOffsetUpdated() {
        var rotation by mutableFloatStateOf(0f)
        rule.setContent {
            Column(Modifier.testTag("outer").graphicsLayer { rotationZ = rotation }) {
                Box(Modifier.height(100.dp))
                Box { Box(Modifier.testTag("inner").size(100.dp)) }
            }
        }

        rule.onNodeWithTag("outer").assertRectDp(0.dp, 0.dp, 100.dp, 200.dp)
        rule.onNodeWithTag("inner").assertRectDp(0.dp, 100.dp, 100.dp, 200.dp)

        rule.runOnIdle { rotation = 45f }

        rule.onNodeWithTag("outer").assertRectDp(0.dp, 0.dp, 100.dp, 200.dp)
        rule.onNodeWithTag("inner").assertRectDp(-56.dp, 65.dp, 85.dp, 206.dp)
    }

    @Test
    @SmallTest
    fun testTranslatedAndRotatedBox() {
        var toggle by mutableStateOf(true)
        rule.setContent {
            Box(
                Modifier.testTag("outer").padding(10.dp).graphicsLayer {
                    translationX = if (toggle) 0f else 100.dp.toPx()
                    rotationZ = if (toggle) 0f else 45f
                }
            ) {
                Box(Modifier.testTag("inner").size(10.dp))
            }
        }

        rule.onNodeWithTag("outer").assertRectDp(0.dp, 0.dp, 30.dp, 30.dp)
        rule.onNodeWithTag("inner").assertRectDp(10.dp, 10.dp, 20.dp, 20.dp)
        toggle = !toggle
        rule.onNodeWithTag("inner").assertRectDp(108.dp, 8.dp, 122.dp, 22.dp)
    }

    @Test
    @SmallTest
    fun testScaledBox() {
        rule.setContent {
            Box(Modifier.testTag("outer").padding(10.dp).scale(2f)) {
                Box(Modifier.testTag("inner").size(10.dp))
            }
        }

        rule.onNodeWithTag("outer").assertRectDp(0.dp, 0.dp, 40.dp, 40.dp)
        rule.onNodeWithTag("inner").assertRectDp(5.dp, 5.dp, 25.dp, 25.dp)
    }

    @Test
    @SmallTest
    fun testScrollingNestedLayout() {
        rule.setContent {
            val scrollState = rememberScrollState()
            Column(Modifier.testTag("scroller").verticalScroll(scrollState)) {
                for (i in 0 until 8) {
                    Box(
                        Modifier.background(if (i % 2 == 0) Color.Yellow else Color.LightGray)
                            .size(200.dp)
                    ) {
                        Text("Row $i", Modifier.testTag("text$i").width(100.dp).height(20.dp))
                    }
                }
            }
        }

        rule.onNodeWithTag("text0").assertRectDp(0.dp, 0.dp, 100.dp, 20.dp)

        rule.onNodeWithTag("text1").assertRectDp(0.dp, 200.dp, 100.dp, 220.dp)

        rule.onNodeWithTag("text2").assertRectDp(0.dp, 400.dp, 100.dp, 420.dp)

        val scrollBy =
            rule.onNodeWithTag("scroller").fetchSemanticsNode().config[ScrollBy].action
                ?: error("No scrollByAction found")

        val scrollDistance = with(rule.density) { 100.dp.toPx() }

        scrollBy(0f, scrollDistance)

        rule.onNodeWithTag("text2").assertRectDp(0.dp, 300.dp, 100.dp, 320.dp)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    @SmallTest
    fun testLazyColumn() {
        rule.setContent {
            LazyColumn(Modifier.testTag("lazy")) {
                items(200) { Box(Modifier.testTag("foo$it").height(100.dp).width(100.dp)) }
            }
        }

        rule.waitUntilExactlyOneExists(hasTestTag("foo1"))
        rule.waitUntilDoesNotExist(hasTestTag("foo20"))

        rule.onNodeWithTag("foo0").assertRectDp(0.dp, 0.dp, 100.dp, 100.dp)
        rule.onNodeWithTag("foo1").assertRectDp(0.dp, 100.dp, 100.dp, 200.dp)

        val scrollBy =
            rule.onNodeWithTag("lazy").fetchSemanticsNode().config[ScrollBy].action
                ?: error("No scrollByAction found")

        val scrollDistance = with(rule.density) { 2000.dp.toPx() }

        scrollBy(0f, scrollDistance)

        rule.waitUntilDoesNotExist(hasTestTag("foo0"))
        rule.waitUntilExactlyOneExists(hasTestTag("foo20"))

        rule.onNodeWithTag("foo20").assertRectTopWithinRange(-4.dp, 4.dp)
    }

    @Test
    @SmallTest
    fun testLazyColumn_isConsistentAfterScroll_0() {
        val lazyListState = LazyListState()

        val rootSizePx = 600f
        val viewPortCount = 4
        val pages = 3

        val listItemHeight = rootSizePx / viewPortCount

        setLazyColumnExample(
            lazyListState = lazyListState,
            rootSizePx = rootSizePx,
            viewPortCount = viewPortCount,
            pages = pages,
        )

        with(rule.density) {
            // These specific calls lead to a reproducible pattern, without a fix, **all**
            // visible items would have missing Rects
            var itemIndex = 2
            repeat(3) {
                rule.runOnIdle {
                    lazyListState.requestScrollToItem(itemIndex)
                    itemIndex += 2
                }
            }
            rule.waitForIdle()

            // Assert the visual state of the List, itemIndex should be 6 and at the top
            rule
                .onNodeWithTag("Item-6")
                .assertRectDp(
                    left = 0.dp,
                    top = 0.dp,
                    right = rootSizePx.toDp(),
                    bottom = listItemHeight.toDp(),
                )

            rule.onNodeWithTag("Item-6").assertRectCount(5)
            rule.onNodeWithTag("Item-7").assertRectCount(5)
            rule.onNodeWithTag("Item-8").assertRectCount(5)
            rule.onNodeWithTag("Item-9").assertRectCount(5)
        }
    }

    @Test
    @MediumTest
    fun testLazyColumn_isConsistentAfterScroll_1() {
        val lazyListState = LazyListState()

        val rootSizePx = 600f
        val viewPortCount = 4
        val pages = 3

        val listItemHeight = rootSizePx / viewPortCount

        setLazyColumnExample(
            lazyListState = lazyListState,
            rootSizePx = rootSizePx,
            viewPortCount = viewPortCount,
            pages = pages,
        )

        with(rule.density) {
            // These specific calls lead to a reproducible pattern, without a fix, some of the
            // visible items will have missing Rects
            var itemIndex = pages * viewPortCount - viewPortCount
            rule.runOnIdle {
                // Scroll to last visible set of items
                lazyListState.requestScrollToItem(itemIndex)
            }
            repeat(2) {
                // Scroll two items back
                rule.runOnIdle { lazyListState.requestScrollToItem(--itemIndex) }
            }

            repeat(2) {
                // Scroll two items forward
                rule.runOnIdle { lazyListState.requestScrollToItem(++itemIndex) }
            }
            rule.waitForIdle()

            // Assert the visual state of the List, first visible should be the
            // lastIndex - viewportCount
            rule
                .onNodeWithTag("Item-8")
                .assertRectDp(
                    left = 0.dp,
                    top = 0.dp,
                    right = rootSizePx.toDp(),
                    bottom = listItemHeight.toDp(),
                )

            rule.onNodeWithTag("Item-8").assertRectCount(5)
            rule.onNodeWithTag("Item-9").assertRectCount(5)
            // Originally observed issues on 10 (1 visible) and 11 (0 visible)
            rule.onNodeWithTag("Item-10").assertRectCount(5)
            rule.onNodeWithTag("Item-11").assertRectCount(5)
        }
    }

    @Test
    @SmallTest
    fun testLayoutPlacingWithOffsetAndScale() {
        rule.setContent {
            Layout(content = { Box(Modifier.testTag("inner").size(10.dp)) }) {
                measurables,
                constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val offset = 10.dp.roundToPx()
                    placeable.placeWithLayer(offset, offset) {
                        scaleX = 2f
                        scaleY = 2f
                    }
                }
            }
        }

        rule.onNodeWithTag("inner").assertRectDp(5.dp, 5.dp, 25.dp, 25.dp)
    }

    @Test
    @SmallTest
    fun testLayoutPlacingWithTranslateOnLayer() {
        rule.setContent {
            Layout(content = { Box(Modifier.testTag("inner").size(10.dp)) }) {
                measurables,
                constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val offset = 5.dp.roundToPx()
                    placeable.placeWithLayer(offset, offset) {
                        translationX = 10.dp.toPx()
                        translationY = 20.dp.toPx()
                    }
                }
            }
        }

        rule.onNodeWithTag("inner").assertRectDp(15.dp, 25.dp, 25.dp, 35.dp)
    }

    /**
     * Lazy Column example that can be used to reproduce issues related to re-using LayoutNodes
     * where each item has the same Layout.
     */
    private fun setLazyColumnExample(
        lazyListState: LazyListState,
        rootSizePx: Float,
        viewPortCount: Int,
        pages: Int,
    ) {
        val listItemHeight = rootSizePx / viewPortCount

        with(rule.density) {
            @Composable
            fun LazyItemScope.MyItem(index: Int) {
                Row(
                    Modifier.testTag("Item-$index")
                        .fillParentMaxWidth()
                        .height(listItemHeight.toDp())
                ) {
                    Column(Modifier.fillMaxHeight().weight(0.7f, true)) {
                        Box(
                            Modifier.fillMaxWidth()
                                .height((listItemHeight / 2f).toDp())
                                .drawBehind {
                                    val alpha = (index + 1f) / (viewPortCount * pages)
                                    drawRect(Color.Blue.copy(alpha = alpha))
                                }
                        )
                        Box(
                            Modifier.fillMaxWidth()
                                .height((listItemHeight / 2f).toDp())
                                .background(Color.Cyan)
                        )
                    }
                    Box(Modifier.fillMaxHeight().weight(0.3f, true).background(Color.LightGray))
                }
            }

            rule.setContent {
                LazyColumn(state = lazyListState, modifier = Modifier.size(rootSizePx.toDp())) {
                    items(viewPortCount * pages) { MyItem(it) }
                }
            }
            rule.waitForIdle()
        }
    }

    internal fun SemanticsNodeInteraction.assertRectDp(left: Dp, top: Dp, right: Dp, bottom: Dp) =
        withRect { l, t, r, b ->
            if (
                !approxEquals(left, l) ||
                    !approxEquals(top, t) ||
                    !approxEquals(right, r) ||
                    !approxEquals(bottom, b)
            ) {
                val actualL = convertToDp(l)
                val actualT = convertToDp(t)
                val actualR = convertToDp(r)
                val actualB = convertToDp(b)

                val expectDpString = "[$left, $top, $right, $bottom]"
                val actualDpString = "[$actualL, $actualT, $actualR, $actualB]"

                throw ComparisonFailure(
                    "expected <$expectDpString> but was: <$actualDpString>",
                    expectDpString,
                    actualDpString,
                )
            }
        }

    internal fun SemanticsNodeInteraction.assertRectTopWithinRange(min: Dp, max: Dp) =
        withRect { _, t, _, _ ->
            val topDp = convertToDp(t)

            if (topDp < min || topDp > max) {
                error("top was $topDp but was expected to be between [$min, $max]")
            }
        }

    inline internal fun SemanticsNodeInteraction.withRect(
        crossinline block: Density.(l: Int, t: Int, r: Int, b: Int) -> Unit
    ) {
        val node = fetchSemanticsNode()
        val owner = node.layoutNode.owner as? AndroidComposeView
        val rectList = owner?.rectManager?.rects ?: error("Could not find rect list")

        with(rule.density) {
            val found = rectList.withRect(node.id) { l, t, r, b -> block(l, t, r, b) }

            if (!found) {
                error("Node with ${node.id} not found in rectlist")
            }
        }
    }

    /** Counts Rects from this node down the hierarchy in the RectList. */
    private fun SemanticsNodeInteraction.assertRectCount(expectedCount: Int) {
        val node = fetchSemanticsNode()
        val owner = node.layoutNode.owner
        val rectList = owner?.rectManager?.rects ?: error("Could not find rect list")
        val layoutNodes = owner.layoutNodes
        val nodeId = node.layoutNode.semanticsId

        val ids = mutableIntSetOf()

        rectList.forEachRect { id, _, _, _, _ ->
            if (id == nodeId) {
                ids.add(id)
            } else {
                layoutNodes[id]?.parent?.semanticsId?.let { parentId ->
                    if (ids.contains(parentId)) {
                        ids.add(id)
                    }
                }
            }
        }
        assertEquals(expectedCount, ids.count())
    }

    private fun Density.convertToDp(px: Int): Dp {
        return (px / density).roundToInt().dp
    }

    private fun Density.approxEquals(dp: Dp, px: Int): Boolean {
        val lower = floor((dp.value - 1f) * density).toInt()
        val upper = ceil((dp.value + 1f) * density).toInt()
        return px in lower..upper
    }

    // TODO: assert on number of times insert/update/move called

    // Helper functions for next several tests
    private fun Modifier.dynamicPointerInputModifier(
        enabled: Boolean,
        key: Any? = Unit,
        onEnter: () -> Unit = {},
        onMove: () -> Unit = {},
        onPress: () -> Unit = {},
        onRelease: () -> Unit = {},
        onExit: () -> Unit = {},
    ) =
        if (enabled) {
            pointerInput(key) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter -> {
                                onEnter()
                            }
                            PointerEventType.Press -> {
                                onPress()
                            }
                            PointerEventType.Move -> {
                                onMove()
                            }
                            PointerEventType.Release -> {
                                onRelease()
                            }
                            PointerEventType.Exit -> {
                                onExit()
                            }
                        }
                    }
                }
            }
        } else this
}
