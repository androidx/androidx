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

package androidx.ui.foundation

import android.os.Build
import androidx.compose.Composable
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.AbsoluteAlignment
import androidx.ui.core.Alignment
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.globalPosition
import androidx.ui.core.onPositioned
import androidx.ui.core.testTag
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.preferredSize
import androidx.ui.layout.rtl
import androidx.ui.layout.size
import androidx.ui.layout.wrapContentSize
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.unit.Density
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(JUnit4::class)
class BoxTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val contentTag = "Box"

    @Test
    fun box_testPadding_all() {
        var childSize: IntPxSize? = null
        val size = 100.dp
        val padding = 20.dp
        composeTestRule.setContent {
            SemanticsParent {
                Box(Modifier.preferredSize(size), padding = padding) {
                    Box(Modifier.fillMaxSize().onPositioned { childSize = it.size })
                }
            }
        }
        with(composeTestRule.density) {
            val paddingSide = padding.toIntPx()
            Truth.assertThat(childSize!!.width).isEqualTo(size.toIntPx() - paddingSide * 2)
            Truth.assertThat(childSize!!.height).isEqualTo(size.toIntPx() - paddingSide * 2)
        }
    }

    @Test
    fun box_testPadding_separate() {
        var childSize: IntPxSize? = null
        var childPosition: PxPosition? = null
        val size = 100.dp
        val start = 17.dp
        val top = 2.dp
        val end = 5.dp
        val bottom = 8.dp
        composeTestRule.setContent {
            SemanticsParent {
                Box(
                    Modifier.preferredSize(size),
                    paddingStart = start,
                    paddingEnd = end,
                    paddingTop = top,
                    paddingBottom = bottom
                ) {
                    Box(Modifier.fillMaxSize().onPositioned {
                        childSize = it.size
                        childPosition = it.localToGlobal(PxPosition.Origin)
                    })
                }
            }
        }
        with(composeTestRule.density) {
            Truth.assertThat(childSize!!.width).isEqualTo(
                size.toIntPx() - start.toIntPx() - end.toIntPx()
            )
            Truth.assertThat(childSize!!.height)
                .isEqualTo(size.toIntPx() - top.toIntPx() - bottom.toIntPx())
            Truth.assertThat(childPosition!!)
                .isEqualTo(PxPosition(start.toIntPx(), top.toIntPx()))
        }
    }

    @Test
    fun box_testPadding_rtl() {
        var childSize: IntPxSize? = null
        var childPosition: PxPosition? = null
        val size = 100.dp
        val start = 17.dp
        val top = 2.dp
        val end = 5.dp
        val bottom = 8.dp
        composeTestRule.setContent {
            SemanticsParent {
                Box(
                    Modifier.preferredSize(size).rtl,
                    paddingStart = start,
                    paddingEnd = end,
                    paddingTop = top,
                    paddingBottom = bottom
                ) {
                    Box(Modifier.fillMaxSize().onPositioned {
                        childSize = it.size
                        childPosition = it.localToGlobal(PxPosition.Origin)
                    })
                }
            }
        }
        with(composeTestRule.density) {
            Truth.assertThat(childSize!!.width).isEqualTo(
                size.toIntPx() - start.toIntPx() - end.toIntPx()
            )
            Truth.assertThat(childSize!!.height)
                .isEqualTo(size.toIntPx() - top.toIntPx() - bottom.toIntPx())
            Truth.assertThat(childPosition!!)
                .isEqualTo(PxPosition(end.toIntPx(), top.toIntPx()))
        }
    }

    @Test
    fun box_testPadding_concreteOverride() {
        var childSize: IntPxSize? = null
        val size = 100.dp
        val padding = 10.dp
        val left = 17.dp
        val top = 2.dp
        val bottom = 8.dp
        composeTestRule.setContent {
            SemanticsParent {
                Box(
                    Modifier.preferredSize(size),
                    padding = padding,
                    paddingStart = left,
                    paddingTop = top,
                    paddingBottom = bottom
                ) {
                    Box(Modifier.fillMaxSize().onPositioned { childSize = it.size })
                }
            }
        }
        with(composeTestRule.density) {
            Truth.assertThat(childSize!!.width).isEqualTo(
                size.toIntPx() - left.toIntPx() - padding.toIntPx()
            )
            Truth.assertThat(childSize!!.height)
                .isEqualTo(size.toIntPx() - top.toIntPx() - bottom.toIntPx())
        }
    }

    @Test
    fun box_testLayout_multipleChildren() {
        val size = 100.dp
        val childSize = 20.dp
        var childPosition1: PxPosition? = null
        var childPosition2: PxPosition? = null
        var childPosition3: PxPosition? = null
        composeTestRule.setContent {
            SemanticsParent {
                Box(
                    modifier = Modifier.preferredSize(size),
                    gravity = Alignment.TopCenter
                ) {
                    Box(Modifier.size(childSize).onPositioned {
                        childPosition1 = it.globalPosition
                    })
                    Box(Modifier.size(childSize).onPositioned {
                        childPosition2 = it.globalPosition
                    })
                    Box(Modifier.size(childSize).onPositioned {
                        childPosition3 = it.globalPosition
                    })
                }
            }
        }
        with(composeTestRule.density) {
            Truth.assertThat(childPosition1).isEqualTo(
                PxPosition(
                    (size.toIntPx() - childSize.toIntPx()) / 2,
                    0.ipx
                )
            )
            Truth.assertThat(childPosition2).isEqualTo(
                PxPosition(
                    (size.toIntPx() - childSize.toIntPx()) / 2,
                    childSize.toIntPx()
                )
            )
            Truth.assertThat(childPosition3).isEqualTo(
                PxPosition(
                    (size.toIntPx() - childSize.toIntPx()) / 2,
                    childSize.toIntPx() * 2
                )
            )
        }
    }

    @Test
    fun box_testLayout_absoluteAlignment() {
        val size = 100.dp
        val childSize = 20.dp
        var childPosition: PxPosition? = null
        composeTestRule.setContent {
            SemanticsParent {
                Box(
                    modifier = Modifier.preferredSize(size).rtl,
                    gravity = AbsoluteAlignment.TopLeft
                ) {
                    Box(Modifier.size(childSize).onPositioned {
                        childPosition = it.globalPosition
                    })
                }
            }
        }
        Truth.assertThat(childPosition).isEqualTo(PxPosition(0.ipx, 0.ipx))
    }

    @Test
    fun box_testBackground() {
        composeTestRule.setContent {
            SemanticsParent {
                Box(
                    Modifier.preferredSize(50.dp),
                    backgroundColor = Color.Red
                )
            }
        }
        val bitmap = findByTag(contentTag).captureToBitmap()
        bitmap.assertShape(composeTestRule.density, RectangleShape, Color.Red, Color.Red)
    }

    @Test
    fun box_testBackground_doesntAffectPadding() {
        val size = 50.dp
        val padding = 10.dp
        composeTestRule.setContent {
            SemanticsParent {
                Box(
                    Modifier.preferredSize(size),
                    backgroundColor = Color.Red,
                    padding = padding
                ) {
                    Box(Modifier.fillMaxSize(), backgroundColor = Color.Blue)
                }
            }
        }
        with(composeTestRule.density) {
            val bitmap = findByTag(contentTag).captureToBitmap()
            bitmap.assertShape(
                density = composeTestRule.density,
                shape = RectangleShape,
                shapeColor = Color.Blue,
                backgroundColor = Color.Red,
                shapeSizeX = (size.toPx() - padding.toPx() - padding.toPx()),
                shapeSizeY = (size.toPx() - padding.toPx() - padding.toPx())
            )
        }
    }

    @Test
    fun box_testBackground_shape() {
        val size = 50.dp
        val padding = 10.dp
        composeTestRule.setContent {
            SemanticsParent {
                Box(
                    Modifier.preferredSize(size),
                    backgroundColor = Color.Red,
                    padding = padding
                ) {
                    Box(Modifier.fillMaxSize(), backgroundColor = Color.Blue, shape = CircleShape)
                }
            }
        }
        with(composeTestRule.density) {
            val bitmap = findByTag(contentTag).captureToBitmap()
            bitmap.assertShape(
                density = composeTestRule.density,
                shape = CircleShape,
                shapeColor = Color.Blue,
                backgroundColor = Color.Red,
                shapeSizeX = (size.toPx() - padding.toPx() - padding.toPx()),
                shapeSizeY = (size.toPx() - padding.toPx() - padding.toPx()),
                shapeOverlapPixelCount = 2.0f
            )
        }
    }

    @Test
    fun box_testBorder() {
        val size = 50.dp
        val borderSize = 10.dp
        composeTestRule.setContent {
            SemanticsParent {
                Box(
                    Modifier.preferredSize(size),
                    backgroundColor = Color.Blue,
                    border = Border(borderSize, Color.Red)
                )
            }
        }
        with(composeTestRule.density) {
            val bitmap = findByTag(contentTag).captureToBitmap()
            bitmap.assertShape(
                density = composeTestRule.density,
                shape = RectangleShape,
                shapeColor = Color.Blue,
                backgroundColor = Color.Red,
                shapeSizeX = (size.toPx() - borderSize.toPx() * 2),
                shapeSizeY = (size.toPx() - borderSize.toPx() * 2),
                shapeOverlapPixelCount = 2.0f
            )
        }
    }

    @Test
    fun box_testBorder_respectsShape() {
        val size = 50.dp
        val borderSize = 10.dp
        composeTestRule.setContent {
            SemanticsParent {
                Box(
                    Modifier.preferredSize(size),
                    backgroundColor = Color.Red
                ) {
                    Box(
                        Modifier.fillMaxSize(),
                        backgroundColor = Color.Blue,
                        shape = CircleShape,
                        border = Border(borderSize, Color.Blue)
                    )
                }
            }
        }
        with(composeTestRule.density) {
            val bitmap = findByTag(contentTag).captureToBitmap()
            bitmap.assertShape(
                density = composeTestRule.density,
                shape = CircleShape,
                shapeColor = Color.Blue,
                backgroundColor = Color.Red,
                shapeOverlapPixelCount = 2.0f
            )
        }
    }

    @Test
    fun box_testBorder_addsPadding() {
        var childSize: IntPxSize? = null
        val size = 50.dp
        val borderSize = 10.dp
        composeTestRule.setContent {
            SemanticsParent {
                Box(Modifier.preferredSize(size), border = Border(borderSize, Color.Red)) {
                    Box(Modifier.fillMaxSize().onPositioned { childSize = it.size })
                }
            }
        }
        with(composeTestRule.density) {
            Truth.assertThat(childSize!!.width)
                .isEqualTo(size.toIntPx() - borderSize.toIntPx() * 2)
            Truth.assertThat(childSize!!.height)
                .isEqualTo(size.toIntPx() - borderSize.toIntPx() * 2)
        }
    }

    @Composable
    private fun SemanticsParent(children: @Composable Density.() -> Unit) {
        Stack(Modifier
            .testTag(contentTag)
            .wrapContentSize(Alignment.TopStart)
        ) {
            DensityAmbient.current.children()
        }
    }
}
