/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.constraintlayout.compose.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMotionApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
internal class MotionLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    /**
     * Tests that [MotionLayoutScope.customFontSize] works as expected.
     *
     * See custom_text_size_scene.json5
     */
    @Test
    fun testCustomTextSize() {
        var animateToEnd by mutableStateOf(false)
        rule.setContent {
            val progress by animateFloatAsState(targetValue = if (animateToEnd) 1.0f else 0f)
            CustomTextSize(
                modifier = Modifier.size(200.dp),
                progress = progress
            )
        }
        rule.waitForIdle()

        var usernameSize = rule.onNodeWithTag("username").getUnclippedBoundsInRoot().size

        // TextSize is 18sp at the start. Since getting the resulting dimensions of the text is not
        // straightforward, the values were obtained by running the test
        assertEquals(55.dp.value, usernameSize.width.value, absoluteTolerance = 0.5f)
        assertEquals(25.dp.value, usernameSize.height.value, absoluteTolerance = 0.5f)

        animateToEnd = true
        rule.waitForIdle()

        usernameSize = rule.onNodeWithTag("username").getUnclippedBoundsInRoot().size

        // TextSize is 12sp at the end. Results in approx. 66% of the original text height
        assertEquals(35.dp.value, usernameSize.width.value, absoluteTolerance = 0.5f)
        assertEquals(17.dp.value, usernameSize.height.value, absoluteTolerance = 0.5f)
    }

    @Test
    fun testCustomKeyFrameAttributes() {
        val progress: MutableState<Float> = mutableStateOf(0f)
        rule.setContent {
            MotionLayout(
                motionScene = MotionScene {
                    val element = createRefFor("element")
                    defaultTransition(
                        from = constraintSet {
                            constrain(element) {
                                customColor("color", Color.White)
                                customDistance("distance", 0.dp)
                                customFontSize("fontSize", 0.sp)
                                customInt("int", 0)
                            }
                        },
                        to = constraintSet {
                            constrain(element) {
                                customColor("color", Color.Black)
                                customDistance("distance", 10.dp)
                                customFontSize("fontSize", 20.sp)
                                customInt("int", 30)
                            }
                        }
                    ) {
                        keyAttributes(element) {
                            frame(50) {
                                // Also tests interpolating to a transparent color
                                customColor("color", Color(0x00ff0000))
                                customDistance("distance", 20.dp)
                                customFontSize("fontSize", 30.sp)
                                customInt("int", 40)
                            }
                        }
                    }
                },
                progress = progress.value,
                modifier = Modifier.size(200.dp)
            ) {
                val props = customProperties(id = "element")
                Column(Modifier.layoutId("element")) {
                    Text(
                        text = "1) Color: #${props.color("color").toHexString()}"
                    )
                    Text(
                        text = "2) Distance: ${props.distance("distance")}"
                    )
                    Text(
                        text = "3) FontSize: ${props.fontSize("fontSize")}"
                    )
                    Text(
                        text = "4) Int: ${props.int("int")}"
                    )

                    // Missing properties
                    Text(
                        text = "5) Color: #${props.color("a").toHexString()}"
                    )
                    Text(
                        text = "6) Distance: ${props.distance("b")}"
                    )
                    Text(
                        text = "7) FontSize: ${props.fontSize("c")}"
                    )
                    Text(
                        text = "8) Int: ${props.int("d")}"
                    )
                }
            }
        }
        rule.waitForIdle()

        progress.value = 0.25f
        rule.waitForIdle()
        rule.onNodeWithText("1) Color: #7fffbaba").assertExists()
        rule.onNodeWithText("2) Distance: 10.0.dp").assertExists()
        rule.onNodeWithText("3) FontSize: 15.0.sp").assertExists()
        rule.onNodeWithText("4) Int: 20").assertExists()

        // Undefined custom properties
        rule.onNodeWithText("5) Color: #0").assertExists()
        rule.onNodeWithText("6) Distance: Dp.Unspecified").assertExists()
        rule.onNodeWithText("7) FontSize: NaN.sp").assertExists()
        rule.onNodeWithText("8) Int: 0").assertExists()

        progress.value = 0.75f
        rule.waitForIdle()
        rule.onNodeWithText("1) Color: #7fba0000").assertExists()
        rule.onNodeWithText("2) Distance: 15.0.dp").assertExists()
        rule.onNodeWithText("3) FontSize: 25.0.sp").assertExists()
        rule.onNodeWithText("4) Int: 35").assertExists()

        // Undefined custom properties
        rule.onNodeWithText("5) Color: #0").assertExists()
        rule.onNodeWithText("6) Distance: Dp.Unspecified").assertExists()
        rule.onNodeWithText("7) FontSize: NaN.sp").assertExists()
        rule.onNodeWithText("8) Int: 0").assertExists()
    }

    @Test
    fun testMotionLayout_withParentIntrinsics() = with(rule.density) {
        val constraintSet = ConstraintSet {
            val (one, two) = createRefsFor("one", "two")
            val horChain = createHorizontalChain(one, two, chainStyle = ChainStyle.Packed(0f))
            constrain(horChain) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
            constrain(one) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            }
            constrain(two) {
                width = Dimension.preferredWrapContent
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            }
        }

        val rootBoxWidth = 200
        val box1Size = 40
        val box2Size = 70

        var rootSize = IntSize.Zero
        var mlSize = IntSize.Zero
        var box1Position = IntOffset.Zero
        var box2Position = IntOffset.Zero

        rule.setContent {
            Box(
                modifier = Modifier
                    .width(rootBoxWidth.toDp())
                    .height(IntrinsicSize.Max)
                    .background(Color.LightGray)
                    .onGloballyPositioned {
                        rootSize = it.size
                    }
            ) {
                MotionLayout(
                    start = constraintSet,
                    end = constraintSet,
                    transition = Transition {},
                    progress = 0f, // We're not testing the animation
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color.Yellow)
                        .onGloballyPositioned {
                            mlSize = it.size
                        }
                ) {
                    Box(
                        Modifier
                            .size(box1Size.toDp())
                            .background(Color.Green)
                            .layoutId("one")
                            .onGloballyPositioned {
                                box1Position = it
                                    .positionInRoot()
                                    .round()
                            })
                    Box(
                        Modifier
                            .size(box2Size.toDp())
                            .background(Color.Red)
                            .layoutId("two")
                            .onGloballyPositioned {
                                box2Position = it
                                    .positionInRoot()
                                    .round()
                            })
                }
            }
        }

        val expectedSize = IntSize(rootBoxWidth, box2Size)
        val expectedBox1Y = ((box2Size / 2f) - (box1Size / 2f)).roundToInt()
        rule.runOnIdle {
            assertEquals(expectedSize, rootSize)
            assertEquals(expectedSize, mlSize)
            assertEquals(IntOffset(0, expectedBox1Y), box1Position)
            assertEquals(IntOffset(box1Size, 0), box2Position)
        }
    }

    @Test
    fun testTransitionChange_hasCorrectStartAndEnd() = with(rule.density) {
        val rootWidthPx = 200
        val rootHeightPx = 50

        val scene = MotionScene {
            val circleRef = createRefFor("circle")
            val aCSetRef = constraintSet {
                constrain(circleRef) {
                    width = rootHeightPx.toDp().asDimension()
                    height = rootHeightPx.toDp().asDimension()
                    centerVerticallyTo(parent)
                    start.linkTo(parent.start)
                }
            }
            val bCSetRef = constraintSet {
                constrain(circleRef) {
                    width = Dimension.fillToConstraints
                    height = rootHeightPx.toDp().asDimension()
                    centerTo(parent)
                }
            }
            val cCSetRef = constraintSet(extendConstraintSet = aCSetRef) {
                constrain(circleRef) {
                    clearHorizontal()
                    end.linkTo(parent.end)
                }
            }
            transition(
                from = aCSetRef,
                to = bCSetRef,
                name = "part1"
            ) {}
            transition(
                from = bCSetRef,
                to = cCSetRef,
                name = "part2"
            ) {}
        }

        val progress = mutableStateOf(0f)
        var bounds = IntRect.Zero

        rule.setContent {
            MotionLayout(
                motionScene = scene,
                progress = if (progress.value < 0.5) progress.value * 2 else progress.value * 2 - 1,
                transitionName = if (progress.value < 0.5f) "part1" else "part2",
                modifier = Modifier.size(width = rootWidthPx.toDp(), height = rootHeightPx.toDp())
            ) {
                Box(
                    modifier = Modifier
                        .layoutId("circle")
                        .background(Color.Red)
                        .onGloballyPositioned {
                            bounds = it
                                .boundsInParent()
                                .roundToIntRect()
                        }
                )
            }
        }

        rule.runOnIdle {
            assertEquals(
                expected = IntRect(IntOffset(0, 0), IntSize(rootHeightPx, rootHeightPx)),
                actual = bounds
            )
        }

        // Offset attributed to the default non-linear interpolator
        val offset = 25

        progress.value = 0.25f
        rule.runOnIdle {
            assertEquals(
                expected = IntRect(
                    offset = IntOffset(0, 0),
                    size = IntSize(rootWidthPx / 2 + offset, rootHeightPx)
                ),
                actual = bounds
            )
        }

        progress.value = 0.75f
        rule.runOnIdle {
            assertEquals(
                expected = IntRect(
                    offset = IntOffset(rootWidthPx / 2 - offset, 0),
                    size = IntSize(rootWidthPx / 2 + offset, rootHeightPx)
                ),
                actual = bounds
            )
        }
    }

    @Test
    fun testStartAndEndBoundsModifier() = with(rule.density) {
        val rootSizePx = 100
        val boxHeight = 10
        val boxWidthStartPx = 10
        val boxWidthEndPx = 70

        val boxId = "box"

        var startBoundsOfBox = Rect.Zero
        var endBoundsOfBox = Rect.Zero
        var globallyPositionedBounds = IntRect.Zero

        var boundsProvidedCount = 0

        val progress = mutableStateOf(0f)

        rule.setContent {
            MotionLayout(
                motionScene = MotionScene {
                    val box = createRefFor(boxId)
                    defaultTransition(
                        from = constraintSet {
                            constrain(box) {
                                width = boxWidthStartPx.toDp().asDimension()
                                height = boxHeight.toDp().asDimension()

                                top.linkTo(parent.top)
                                centerHorizontallyTo(parent)
                            }
                        },
                        to = constraintSet {
                            constrain(box) {
                                width = boxWidthEndPx.toDp().asDimension()
                                height = boxHeight.toDp().asDimension()

                                centerHorizontallyTo(parent)
                                bottom.linkTo(parent.bottom)
                            }
                        }
                    )
                },
                progress = progress.value,
                modifier = Modifier.size(rootSizePx.toDp())
            ) {
                Box(
                    modifier = Modifier
                        .layoutId(boxId)
                        .background(Color.Red)
                        .onStartEndBoundsChanged(boxId) { startBounds, endBounds ->
                            boundsProvidedCount++
                            startBoundsOfBox = startBounds
                            endBoundsOfBox = endBounds
                        }
                        .onGloballyPositioned {
                            globallyPositionedBounds = it
                                .boundsInParent()
                                .roundToIntRect()
                        }
                )
            }
        }
        rule.waitForIdle()

        rule.runOnIdle {
            // Values should only be assigned once, to prove that they are stable
            assertEquals(1, boundsProvidedCount)
            assertEquals(
                expected = IntRect(
                    offset = IntOffset((rootSizePx - boxWidthStartPx) / 2, 0),
                    size = IntSize(boxWidthStartPx, boxHeight)
                ),
                actual = globallyPositionedBounds
            )
            assertEquals(
                globallyPositionedBounds,
                startBoundsOfBox.roundToIntRect()
            )
        }

        progress.value = 1f

        rule.runOnIdle {
            // Values should only be assigned once, to prove that they are stable
            assertEquals(1, boundsProvidedCount)
            assertEquals(
                expected = IntRect(
                    offset = IntOffset((rootSizePx - boxWidthEndPx) / 2, rootSizePx - boxHeight),
                    size = IntSize(boxWidthEndPx, boxHeight)
                ),
                actual = globallyPositionedBounds
            )
            assertEquals(
                globallyPositionedBounds,
                endBoundsOfBox.roundToIntRect()
            )
        }
    }

    @Test
    fun testStaggeredAndCustomWeights() = with(rule.density) {
        val rootSizePx = 100
        val boxSizePx = 10
        val progress = mutableStateOf(0f)
        val staggeredValue = mutableStateOf(0.31f)
        val weights = mutableStateListOf(Float.NaN, Float.NaN, Float.NaN)

        val ids = IntArray(3) { it }
        val positions = mutableMapOf<Int, IntOffset>()

        rule.setContent {
            MotionLayout(
                motionScene = remember {
                    derivedStateOf {
                        MotionScene {
                            val refs = ids.map { createRefFor(it) }.toTypedArray()
                            defaultTransition(
                                from = constraintSet {
                                    createVerticalChain(*refs, chainStyle = ChainStyle.Packed(0.0f))
                                    refs.forEachIndexed { index, ref ->
                                        constrain(ref) {
                                            staggeredWeight = weights[index]
                                        }
                                    }
                                },
                                to = constraintSet {
                                    createVerticalChain(*refs, chainStyle = ChainStyle.Packed(0.0f))
                                    constrain(*refs) {
                                        end.linkTo(parent.end)
                                    }
                                }
                            ) {
                                maxStaggerDelay = staggeredValue.value
                            }
                        }
                    }
                }.value,
                progress = progress.value,
                modifier = Modifier.size(rootSizePx.toDp())
            ) {
                for (id in ids) {
                    Box(
                        Modifier
                            .size(boxSizePx.toDp())
                            .layoutId(id)
                            .onGloballyPositioned {
                                positions[id] = it
                                    .positionInParent()
                                    .round()
                            })
                }
            }
        }

        // Set the progress to just before the stagger value (0.31f)
        progress.value = 0.3f

        rule.runOnIdle {
            assertEquals(0, positions[0]!!.x)
            assertNotEquals(0, positions[1]!!.x)
            assertNotEquals(0, positions[2]!!.x)

            // Widget 2 has higher weight since it's laid out further towards the bottom
            assertTrue(positions[2]!!.x > positions[1]!!.x)
        }

        // Invert the staggering order
        staggeredValue.value = -(staggeredValue.value)

        rule.runOnIdle {
            assertNotEquals(0, positions[0]!!.x)
            assertNotEquals(0, positions[1]!!.x)
            assertEquals(0, positions[2]!!.x)

            // While inverted, widget 0 has the higher weight
            assertTrue(positions[0]!!.x > positions[1]!!.x)
        }

        // Set the widget in the middle to have the lowest weight
        weights[0] = 3f
        weights[1] = 1f
        weights[2] = 2f

        // Set the staggering order back to normal
        staggeredValue.value = -(staggeredValue.value)

        rule.runOnIdle {
            assertNotEquals(0, positions[0]!!.x)
            assertEquals(0, positions[1]!!.x)
            assertNotEquals(0, positions[2]!!.x)

            // Widget 0 has higher weight, starts earlier
            assertTrue(positions[0]!!.x > positions[2]!!.x)
        }
    }

    @Test
    fun testRemeasureOnContentChanged() {
        val progress = mutableStateOf(0f)
        val textContent = mutableStateOf("Foo")

        rule.setContent {
            WithConsistentTextStyle {
                MotionLayout(
                    modifier = Modifier
                        .size(300.dp)
                        .background(Color.LightGray),
                    motionScene = MotionScene {
                        // Text at wrap_content, animated from top of the layout to the bottom
                        val textRef = createRefFor("text")
                        defaultTransition(
                            from = constraintSet {
                                constrain(textRef) {
                                    centerHorizontallyTo(parent)
                                    centerVerticallyTo(parent, 0f)
                                }
                            },
                            to = constraintSet {
                                constrain(textRef) {
                                    centerHorizontallyTo(parent)
                                    centerVerticallyTo(parent, 1f)
                                }
                            }
                        )
                    },
                    progress = progress.value
                ) {
                    Text(
                        text = textContent.value,
                        fontSize = 10.sp,
                        modifier = Modifier.layoutTestId("text")
                    )
                }
            }
        }

        rule.waitForIdle()
        var actualTextSize = rule.onNodeWithTag("text").getUnclippedBoundsInRoot()
        assertEquals(18, actualTextSize.width.value.roundToInt())
        assertEquals(14, actualTextSize.height.value.roundToInt())

        progress.value = 0.5f
        rule.waitForIdle()
        actualTextSize = rule.onNodeWithTag("text").getUnclippedBoundsInRoot()
        assertEquals(18, actualTextSize.width.value.roundToInt())
        assertEquals(14, actualTextSize.height.value.roundToInt())

        textContent.value = "FooBar"
        rule.waitForIdle()
        actualTextSize = rule.onNodeWithTag("text").getUnclippedBoundsInRoot()
        assertEquals(36, actualTextSize.width.value.roundToInt())
        assertEquals(14, actualTextSize.height.value.roundToInt())
    }

    @Test
    fun testOnSwipe_withLimitBounds() = with(rule.density) {
        val rootSizePx = 300
        val boxSizePx = 30
        val boxId = "box"
        var boxPosition = IntOffset.Zero

        rule.setContent {
            MotionLayout(
                motionScene = remember {
                    createCornerToCornerMotionScene(
                        boxId = boxId,
                        boxSizePx = boxSizePx
                    ) { boxRef ->
                        onSwipe = OnSwipe(
                            anchor = boxRef,
                            side = SwipeSide.End,
                            direction = SwipeDirection.End,
                            limitBoundsTo = boxRef
                        )
                    }
                },
                progress = 0f,
                modifier = Modifier
                    .layoutTestId("MyMotion")
                    .size(rootSizePx.toDp())
            ) {
                Box(
                    Modifier
                        .background(Color.Red)
                        .layoutTestId(boxId)
                        .onGloballyPositioned {
                            boxPosition = it
                                .positionInParent()
                                .round()
                        }
                )
            }
        }
        rule.waitForIdle()
        val motionSemantic = rule.onNodeWithTag("MyMotion")
        motionSemantic
            .assertExists()
            // The first swipe will completely miss the Box, so it shouldn't move
            .performSwipe(
                from = {
                    Offset(left + boxSizePx / 2, centerY)
                },
                to = {
                    Offset(right * 0.9f, centerY)
                }
            )
        // Wait a frame for the Touch Up animation to start
        rule.mainClock.advanceTimeByFrame()
        // Then wait for it to end
        rule.waitForIdle()
        // Box didn't move since the swipe didn't start within the box
        assertEquals(IntOffset.Zero, boxPosition)

        motionSemantic
            .assertExists()
            // The second swipe will start within the Box
            .performSwipe(
                from = {
                    Offset(left + boxSizePx / 2, top + boxSizePx / 2)
                },
                to = {
                    Offset(right * 0.9f, centerY)
                }
            )
        // Wait a frame for the Touch Up animation to start
        rule.mainClock.advanceTimeByFrame()
        // Then wait for it to end
        rule.waitForIdle()
        // Box moved to end
        assertEquals(IntOffset(rootSizePx - boxSizePx, rootSizePx - boxSizePx), boxPosition)
    }

    @Test
    fun testInvalidationStrategy_onObservedStateChange() = with(rule.density) {
        val rootSizePx = 200
        val progress = mutableStateOf(0f)
        val textContent = mutableStateOf("Foo")
        val optimizeCorrectly = mutableStateOf(false)
        val textId = "text"

        rule.setContent {
            WithConsistentTextStyle {
                MotionLayout(
                    motionScene = remember {
                        MotionScene {
                            val textRef = createRefFor(textId)

                            defaultTransition(
                                from = constraintSet {
                                    constrain(textRef) {
                                        centerTo(parent)
                                    }
                                },
                                to = constraintSet {
                                    constrain(textRef) {
                                        centerTo(parent)
                                    }
                                }
                            )
                        }
                    },
                    progress = progress.value,
                    modifier = Modifier.size(rootSizePx.toDp()),
                    invalidationStrategy = remember(optimizeCorrectly.value) {
                        if (optimizeCorrectly.value) {
                            InvalidationStrategy {
                                textContent.value
                            }
                        } else {
                            InvalidationStrategy {
                                // Do not invalidate on recomposition
                            }
                        }
                    }
                ) {
                    Text(
                        text = textContent.value,
                        fontSize = 10.sp,
                        modifier = Modifier.layoutTestId(textId)
                    )
                }
            }
        }

        rule.waitForIdle()
        var actualTextSize = rule.onNodeWithTag(textId).getUnclippedBoundsInRoot()
        assertEquals(18, actualTextSize.width.value.roundToInt())
        assertEquals(14, actualTextSize.height.value.roundToInt())

        textContent.value = "Foo\nBar"

        // Because we are optimizing "incorrectly" the text layout remains unchanged
        rule.waitForIdle()
        actualTextSize = rule.onNodeWithTag(textId).getUnclippedBoundsInRoot()
        assertEquals(18, actualTextSize.width.value.roundToInt())
        assertEquals(14, actualTextSize.height.value.roundToInt())

        textContent.value = "Foo"
        optimizeCorrectly.value = true

        // We change the text back and update the optimization strategy to be correct, text should
        // be the same as in its initial state
        rule.waitForIdle()
        actualTextSize = rule.onNodeWithTag(textId).getUnclippedBoundsInRoot()
        assertEquals(18, actualTextSize.width.value.roundToInt())
        assertEquals(14, actualTextSize.height.value.roundToInt())

        textContent.value = "Foo\nBar"

        // With the appropriate optimization strategy, the layout is invalidated when the text
        // changes
        rule.waitForIdle()
        actualTextSize = rule.onNodeWithTag(textId).getUnclippedBoundsInRoot()
        assertEquals(18, actualTextSize.width.value.roundToInt())
        assertEquals(25, actualTextSize.height.value.roundToInt())
    }

    @Test
    fun testOnSwipe_withDragScale() = with(rule.density) {
        val rootSizePx = 300
        val boxSizePx = 30
        val boxId = "box"
        val dragScale = 3f
        var boxPosition = IntOffset.Zero

        rule.setContent {
            MotionLayout(
                motionScene = remember {
                    createCornerToCornerMotionScene(
                        boxId = boxId,
                        boxSizePx = boxSizePx
                    ) { boxRef ->
                        onSwipe = OnSwipe(
                            anchor = boxRef,
                            side = SwipeSide.Middle,
                            direction = SwipeDirection.Down,
                            onTouchUp = SwipeTouchUp.ToStart,
                            dragScale = dragScale
                        )
                    }
                },
                progress = 0f,
                modifier = Modifier
                    .layoutTestId("MyMotion")
                    .size(rootSizePx.toDp())
            ) {
                Box(
                    Modifier
                        .background(Color.Red)
                        .layoutTestId(boxId)
                        .onGloballyPositioned {
                            boxPosition = it
                                .positionInParent()
                                .round()
                        }
                )
            }
        }
        rule.waitForIdle()
        val motionSemantic = rule.onNodeWithTag("MyMotion")

        motionSemantic
            .assertExists()
            .performSwipe(
                from = {
                    Offset(center.x, top + (boxSizePx / 2f))
                },
                to = {
                    // Move only half-way, with a dragScale of 1f, it would be forced to
                    // return to the start position
                    val off = ((bottom - (boxSizePx / 2f)) - (top + (boxSizePx / 2f))) * 0.5f
                    Offset(center.x, (top + (boxSizePx / 2f)) + off)
                }
            )
        // Wait a frame for the Touch Up animation to start
        rule.mainClock.advanceTimeByFrame()
        // Then wait for it to end
        rule.waitForIdle()
        // Box is at the ending position because of the increased dragScale
        assertEquals(IntOffset(rootSizePx - boxSizePx, rootSizePx - boxSizePx), boxPosition)
    }

    private fun Color.toHexString(): String = toArgb().toUInt().toString(16)
}

@OptIn(ExperimentalMotionApi::class)
@Composable
private fun CustomTextSize(modifier: Modifier, progress: Float) {
    val context = LocalContext.current
    WithConsistentTextStyle {
        MotionLayout(
            motionScene = MotionScene(
                content = context
                    .resources
                    .openRawResource(R.raw.custom_text_size_scene)
                    .readBytes()
                    .decodeToString()
            ),
            progress = progress,
            modifier = modifier
        ) {
            val profilePicProperties = customProperties(id = "profile_pic")
            Box(
                modifier = Modifier
                    .layoutTestId("box")
                    .background(Color.DarkGray)
            )
            Image(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = profilePicProperties.color("background"),
                        shape = CircleShape
                    )
                    .layoutTestId("profile_pic")
            )
            Text(
                text = "Hello",
                fontSize = customFontSize("username", "textSize"),
                modifier = Modifier.layoutTestId("username"),
                color = profilePicProperties.color("background")
            )
        }
    }
}

/**
 * Provides composition locals that help making Text produce consistent measurements across multiple
 * devices.
 *
 * Be aware that this makes it so that 1.dp = 1px. So the layout will look significantly different
 * than expected.
 */
@Composable
private fun WithConsistentTextStyle(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalDensity provides Density(1f, 1f),
        LocalTextStyle provides TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            platformStyle = PlatformTextStyle(includeFontPadding = true)
        ),
        content = content
    )
}

private fun Density.createCornerToCornerMotionScene(
    boxId: String,
    boxSizePx: Int,
    transitionContent: TransitionScope.(boxRef: ConstrainedLayoutReference) -> Unit
) = MotionScene {
    val boxRef = createRefFor(boxId)

    defaultTransition(
        from = constraintSet {
            constrain(boxRef) {
                width = boxSizePx.toDp().asDimension()
                height = boxSizePx.toDp().asDimension()

                top.linkTo(parent.top)
                start.linkTo(parent.start)
            }
        },
        to = constraintSet {
            constrain(boxRef) {
                width = boxSizePx.toDp().asDimension()
                height = boxSizePx.toDp().asDimension()

                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            }
        }
    ) {
        transitionContent(boxRef)
    }
}
