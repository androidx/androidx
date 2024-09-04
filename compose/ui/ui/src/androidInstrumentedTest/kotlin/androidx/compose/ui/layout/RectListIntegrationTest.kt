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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    fun testScaledBox() {
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
    fun testScaledBoxUpdate() {
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

    internal fun SemanticsNodeInteraction.assertRectDp(
        left: Dp,
        top: Dp,
        right: Dp,
        bottom: Dp,
    ) = withRect { l, t, r, b ->
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
                actualDpString
            )
        }
    }

    internal fun SemanticsNodeInteraction.assertRectTopWithinRange(
        min: Dp,
        max: Dp,
    ) = withRect { _, t, _, _ ->
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

    private fun Density.convertToDp(px: Int): Dp {
        return (px / density).roundToInt().dp
    }

    private fun Density.approxEquals(dp: Dp, px: Int): Boolean {
        val lower = floor((dp.value - 1f) * density).toInt()
        val upper = ceil((dp.value + 1f) * density).toInt()
        return px in lower..upper
    }
    // TODO: assert on number of times insert/update/move called
}
