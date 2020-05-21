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

package androidx.ui.layout.test

import android.os.Build
import androidx.compose.state
import androidx.test.filters.SmallTest
import androidx.ui.core.Alignment
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.core.globalPosition
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Box
import androidx.ui.layout.Stack
import androidx.ui.layout.offset
import androidx.ui.layout.offsetPx
import androidx.ui.layout.preferredWidth
import androidx.ui.layout.rtl
import androidx.ui.layout.size
import androidx.ui.layout.wrapContentSize
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import androidx.ui.unit.round
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.roundToInt

@SmallTest
@RunWith(JUnit4::class)
class LayoutOffsetTest : LayoutTest() {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun before() {
        // b/151728444
        Assume.assumeFalse(
            Build.MODEL.contains("Nexus 5") && Build.VERSION.SDK_INT == Build.VERSION_CODES.M
        )
    }

    @Test
    fun positionIsModified() = with(density) {
        val offsetX = 10.dp
        val offsetY = 20.dp
        var positionX = 0.ipx
        var positionY = 0.ipx
        composeTestRule.setContent {
            Stack(
                Modifier.testTag("stack")
                    .wrapContentSize(Alignment.TopStart)
                    .offset(offsetX, offsetY)
                    .onPositioned { coordinates: LayoutCoordinates ->
                        positionX = coordinates.globalPosition.x.px.round()
                        positionY = coordinates.globalPosition.y.px.round()
                    }
            ) {
            }
        }

        findByTag("stack").assertExists()
        runOnIdleCompose {
            assertEquals(offsetX.toIntPx(), positionX)
            assertEquals(offsetY.toIntPx(), positionY)
        }
    }

    @Test
    fun positionIsModified_rtl() = with(density) {
        val containerWidth = 30.dp
        val boxSize = 1.ipx
        val offsetX = 10.dp
        val offsetY = 20.dp
        var positionX = 0.ipx
        var positionY = 0.ipx
        composeTestRule.setContent {
            Stack(
                Modifier.testTag("stack")
                    .rtl
                    .wrapContentSize(Alignment.TopEnd)
                    .preferredWidth(containerWidth)
                    .wrapContentSize(Alignment.TopStart)
                    .offset(offsetX, offsetY)
                    .onPositioned { coordinates: LayoutCoordinates ->
                        positionX = coordinates.globalPosition.x.px.round()
                        positionY = coordinates.globalPosition.y.px.round()
                    }
            ) {
                // TODO(popam): this box should not be needed after b/154758475 is fixed.
                Box(Modifier.size(boxSize.toDp()))
            }
        }

        findByTag("stack").assertExists()
        runOnIdleCompose {
            assertEquals(containerWidth.toIntPx() - offsetX.toIntPx() - boxSize, positionX)
            assertEquals(offsetY.toIntPx(), positionY)
        }
    }

    @Test
    fun positionIsModified_px() = with(density) {
        val offsetX = 10f
        val offsetY = 20f
        var positionX = 0f
        var positionY = 0f
        composeTestRule.setContent {
            Stack(
                Modifier.testTag("stack")
                    .wrapContentSize(Alignment.TopStart)
                    .offsetPx(state { offsetX }, state { offsetY })
                    .onPositioned { coordinates: LayoutCoordinates ->
                        positionX = coordinates.globalPosition.x
                        positionY = coordinates.globalPosition.y
                    }
            ) {
            }
        }

        findByTag("stack").assertExists()
        runOnIdleCompose {
            Assert.assertEquals(offsetX, positionX)
            Assert.assertEquals(offsetY, positionY)
        }
    }

    @Test
    fun positionIsModified_px_rtl() = with(density) {
        val containerWidth = 30.dp
        val boxSize = 1f
        val offsetX = 10f
        val offsetY = 20f
        var positionX = 0f
        var positionY = 0f
        composeTestRule.setContent {
            Stack(
                Modifier.testTag("stack")
                    .rtl
                    .wrapContentSize(Alignment.TopEnd)
                    .preferredWidth(containerWidth)
                    .wrapContentSize(Alignment.TopStart)
                    .offsetPx(state { offsetX }, state { offsetY })
                    .onPositioned { coordinates: LayoutCoordinates ->
                        positionX = coordinates.globalPosition.x
                        positionY = coordinates.globalPosition.y
                    }
            ) {
                // TODO(popam): this box should not be needed after b/154758475 is fixed.
                Box(Modifier.size(boxSize.toDp()))
            }
        }

        findByTag("stack").assertExists()
        runOnIdleCompose {
            Assert.assertEquals(
                containerWidth.toIntPx().value - offsetX.roundToInt() - boxSize,
                positionX
            )
            Assert.assertEquals(offsetY, positionY)
        }
    }
}
