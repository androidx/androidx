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
package androidx.compose.foundation

import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.LayoutDirection.Rtl
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
class ScrollingContainerTest {
    @get:Rule val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun testInspectorValue() {
        rule.setContent {
            val modifier =
                Modifier.scrollingContainer(
                    rememberScrollState(),
                    orientation = Horizontal,
                    enabled = true,
                    reverseScrolling = false,
                    flingBehavior = null,
                    interactionSource = null,
                    overscrollEffect = null,
                    bringIntoViewSpec = null
                ) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("scrollingContainer")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "state",
                    "orientation",
                    "enabled",
                    "reverseScrolling",
                    "flingBehavior",
                    "interactionSource",
                    "overscrollEffect",
                    "bringIntoViewSpec"
                )
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun clipUpdatesWhenOrientationChanges() {
        var orientation by mutableStateOf(Horizontal)
        rule.setContent {
            val scrollState = rememberScrollState(20)
            Box(Modifier.size(60.dp).testTag("container").background(Color.Gray)) {
                Box(
                    Modifier.padding(20.dp)
                        .fillMaxSize()
                        .scrollingContainer(
                            state = scrollState,
                            orientation = orientation,
                            enabled = true,
                            reverseScrolling = false,
                            flingBehavior = null,
                            interactionSource = null,
                            overscrollEffect = null
                        )
                ) {
                    repeat(4) { Box(Modifier.size(20.dp).drawOutsideOfBounds()) }
                }
            }
        }

        rule
            .onNodeWithTag("container")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Red,
                backgroundColor = Color.Gray,
                horizontalPadding = 20.dp,
                verticalPadding = 0.dp
            )

        rule.runOnIdle { orientation = Vertical }

        rule
            .onNodeWithTag("container")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Red,
                backgroundColor = Color.Gray,
                horizontalPadding = 0.dp,
                verticalPadding = 20.dp
            )
    }

    @Test
    fun layoutDirectionChange_updatesScrollDirection() {
        val size = with(rule.density) { 100.toDp() }
        var scrollAmount = 0f
        val scrollState = ScrollableState {
            scrollAmount = (scrollAmount + it).coerceIn(0f, 10f)
            it
        }
        var layoutDirection by mutableStateOf(Ltr)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box {
                    Box(
                        Modifier.size(size)
                            .testTag("container")
                            .scrollingContainer(
                                state = scrollState,
                                orientation = Horizontal,
                                enabled = true,
                                reverseScrolling = false,
                                flingBehavior = null,
                                interactionSource = null,
                                overscrollEffect = null
                            )
                    )
                }
            }
        }

        rule.onNodeWithTag("container").performTouchInput { swipeLeft() }

        rule.runOnIdle {
            assertThat(scrollAmount).isEqualTo(10f)
            layoutDirection = Rtl
        }

        rule.onNodeWithTag("container").performTouchInput { swipeLeft() }

        // Now that layout direction changed, we should go back to 0
        rule.runOnIdle { assertThat(scrollAmount).isEqualTo(0f) }
    }

    private fun Modifier.drawOutsideOfBounds() = drawBehind {
        val inflate = 20.dp.roundToPx().toFloat()
        drawRect(
            Color.Red,
            Offset(-inflate, -inflate),
            Size(size.width + inflate * 2, size.height + inflate * 2)
        )
    }
}
