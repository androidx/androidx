/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.test.filters.MediumTest
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.onPositioned
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Spacer
import androidx.ui.layout.Stack
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import com.google.common.truth.Truth
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(JUnit4::class)
class SpacerTest : LayoutTest() {

    private val bigConstraints = DpConstraints(
        maxWidth = 5000.dp,
        maxHeight = 5000.dp
    )

    @Test
    fun fixedSpacer_Sizes() {
        var size: IntPxSize? = null
        val width = 40.dp
        val height = 71.dp

        val drawLatch = CountDownLatch(1)
        show {
            Container(constraints = bigConstraints) {
                Spacer(
                    Modifier.preferredSize(width = width, height = height)
                        .onPositioned { position: LayoutCoordinates ->
                            size = position.size
                            drawLatch.countDown()
                        }
                )
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        with(density) {
            Truth.assertThat(size?.height).isEqualTo(height.toIntPx())
            Truth.assertThat(size?.width).isEqualTo(width.toIntPx())
        }
    }

    @Test
    fun fixedSpacer_Sizes_WithSmallerContainer() {
        var size: IntPxSize? = null
        val width = 40.dp
        val height = 71.dp

        val drawLatch = CountDownLatch(1)
        val containerWidth = 5.dp
        val containerHeight = 7.dp
        show {
            Stack {
                Container(
                    constraints = DpConstraints(
                        maxWidth = containerWidth,
                        maxHeight = containerHeight
                    )
                ) {
                    Spacer(
                        Modifier.preferredSize(width = width, height = height)
                            .onPositioned { position: LayoutCoordinates ->
                                size = position.size
                                drawLatch.countDown()
                            }
                    )
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        with(density) {
            Truth.assertThat(size?.height).isEqualTo(containerHeight.toIntPx())
            Truth.assertThat(size?.width).isEqualTo(containerWidth.toIntPx())
        }
    }

    @Test
    fun widthSpacer_Sizes() {
        var size: IntPxSize? = null
        val width = 71.dp

        val drawLatch = CountDownLatch(1)
        show {
            Container(constraints = bigConstraints) {
                Spacer(Modifier.preferredWidth(width).onPositioned { position: LayoutCoordinates ->
                    size = position.size
                    drawLatch.countDown()
                })
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        with(density) {
            Truth.assertThat(size?.height).isEqualTo(0.ipx)
            Truth.assertThat(size?.width).isEqualTo(width.toIntPx())
        }
    }

    @Test
    fun widthSpacer_Sizes_WithSmallerContainer() {
        var size: IntPxSize? = null
        val width = 40.dp

        val drawLatch = CountDownLatch(1)
        val containerWidth = 5.dp
        val containerHeight = 7.dp
        show {
            Stack {
                Container(
                    constraints = DpConstraints(
                        maxWidth = containerWidth,
                        maxHeight = containerHeight
                    )
                ) {
                    Spacer(Modifier.preferredWidth(width)
                        .onPositioned { position: LayoutCoordinates ->
                            size = position.size
                            drawLatch.countDown()
                        })
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        with(density) {
            Truth.assertThat(size?.height).isEqualTo(0.ipx)
            Truth.assertThat(size?.width).isEqualTo(containerWidth.toIntPx())
        }
    }

    @Test
    fun heightSpacer_Sizes() {
        var size: IntPxSize? = null
        val height = 7.dp

        val drawLatch = CountDownLatch(1)
        show {
            Container(constraints = bigConstraints) {
                Spacer(Modifier.preferredHeight(height)
                    .onPositioned { position: LayoutCoordinates ->
                        size = position.size
                        drawLatch.countDown()
                    })
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        with(density) {
            Truth.assertThat(size?.height).isEqualTo(height.toIntPx())
            Truth.assertThat(size?.width).isEqualTo(0.ipx)
        }
    }

    @Test
    fun heightSpacer_Sizes_WithSmallerContainer() {
        var size: IntPxSize? = null
        val height = 23.dp

        val drawLatch = CountDownLatch(1)
        val containerWidth = 5.dp
        val containerHeight = 7.dp
        show {
            Stack {
                Container(
                    constraints = DpConstraints(
                        maxWidth = containerWidth,
                        maxHeight = containerHeight
                    )
                ) {
                    Spacer(Modifier.preferredHeight(height)
                        .onPositioned { position: LayoutCoordinates ->
                            size = position.size
                            drawLatch.countDown()
                        })
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        with(density) {
            Truth.assertThat(size?.height).isEqualTo(containerHeight.toIntPx())
            Truth.assertThat(size?.width).isEqualTo(0.ipx)
        }
    }
}