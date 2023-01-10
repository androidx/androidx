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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.math.roundToInt
import kotlin.test.assertEquals
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
                                customColor("color", Color.Red)
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
        rule.onNodeWithText("1) Color: #ffffbaba").assertExists()
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
        rule.onNodeWithText("1) Color: #ffba0000").assertExists()
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
                                box1Position = it.positionInRoot().round()
                            })
                    Box(
                        Modifier
                            .size(box2Size.toDp())
                            .background(Color.Red)
                            .layoutId("two")
                            .onGloballyPositioned {
                                box2Position = it.positionInRoot().round()
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

    private fun Color.toHexString(): String = toArgb().toUInt().toString(16)
}

@OptIn(ExperimentalMotionApi::class)
@Composable
private fun CustomTextSize(modifier: Modifier, progress: Float) {
    val context = LocalContext.current
    CompositionLocalProvider(
        LocalDensity provides Density(1f, 1f),
        LocalTextStyle provides TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal
        )
    ) {
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