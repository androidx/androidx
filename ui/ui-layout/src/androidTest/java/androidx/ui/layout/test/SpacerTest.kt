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
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxSize
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.layout.Center
import androidx.ui.layout.Container
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.FixedSpacer
import androidx.ui.layout.HeightSpacer
import androidx.ui.layout.WidthSpacer
import com.google.common.truth.Truth
import androidx.compose.Composable
import androidx.compose.composer
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
        var size: PxSize? = null
        val width = 40.dp
        val height = 71.dp

        val drawLatch = CountDownLatch(1)
        show {
            Container(constraints = bigConstraints) {
                OnChildPositioned(onPositioned = { position ->
                    size = position.size
                    drawLatch.countDown()
                }) {
                    FixedSpacer(width = width, height = height)
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(height.toIntPx())
            Truth.assertThat(size?.width?.round()).isEqualTo(width.toIntPx())
        }
    }

    @Test
    fun fixedSpacer_Sizes_WithSmallerContainer() {
        var size: PxSize? = null
        val width = 40.dp
        val height = 71.dp

        val drawLatch = CountDownLatch(1)
        val containerWidth = 5.dp
        val containerHeight = 7.dp
        show {
            Center {
                Container(
                    constraints = DpConstraints(
                        maxWidth = containerWidth,
                        maxHeight = containerHeight
                    )
                ) {
                    OnChildPositioned(onPositioned = { position ->
                        size = position.size
                        drawLatch.countDown()
                    }) {
                        FixedSpacer(width = width, height = height)
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(containerHeight.toIntPx())
            Truth.assertThat(size?.width?.round()).isEqualTo(containerWidth.toIntPx())
        }
    }

    @Test
    fun widthSpacer_Sizes() {
        var size: PxSize? = null
        val width = 71.dp

        val drawLatch = CountDownLatch(1)
        show {
            Container(constraints = bigConstraints) {
                OnChildPositioned(onPositioned = { position ->
                    size = position.size
                    drawLatch.countDown()
                }) {
                    WidthSpacer(width = width)
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(0.ipx)
            Truth.assertThat(size?.width?.round()).isEqualTo(width.toIntPx())
        }
    }

    @Test
    fun widthSpacer_Sizes_WithSmallerContainer() {
        var size: PxSize? = null
        val width = 40.dp

        val drawLatch = CountDownLatch(1)
        val containerWidth = 5.dp
        val containerHeight = 7.dp
        show {
            Center {
                Container(
                    constraints = DpConstraints(
                        maxWidth = containerWidth,
                        maxHeight = containerHeight
                    )
                ) {
                    OnChildPositioned(onPositioned = { position ->
                        size = position.size
                        drawLatch.countDown()
                    }) {
                        WidthSpacer(width = width)
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(0.ipx)
            Truth.assertThat(size?.width?.round()).isEqualTo(containerWidth.toIntPx())
        }
    }

    @Test
    fun heightSpacer_Sizes() {
        var size: PxSize? = null
        val height = 7.dp

        val drawLatch = CountDownLatch(1)
        show {
            Container(constraints = bigConstraints) {
                OnChildPositioned(onPositioned = { position ->
                    size = position.size
                    drawLatch.countDown()
                }) {
                    HeightSpacer(height = height)
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(height.toIntPx())
            Truth.assertThat(size?.width?.round()).isEqualTo(0.ipx)
        }
    }

    @Test
    fun heightSpacer_Sizes_WithSmallerContainer() {
        var size: PxSize? = null
        val height = 23.dp

        val drawLatch = CountDownLatch(1)
        val containerWidth = 5.dp
        val containerHeight = 7.dp
        show {
            Center {
                Container(
                    constraints = DpConstraints(
                        maxWidth = containerWidth,
                        maxHeight = containerHeight
                    )
                ) {
                    OnChildPositioned(onPositioned = { position ->
                        size = position.size
                        drawLatch.countDown()
                    }) {
                        HeightSpacer(height = height)
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        withDensity(density) {
            Truth.assertThat(size?.height?.round()).isEqualTo(containerHeight.toIntPx())
            Truth.assertThat(size?.width?.round()).isEqualTo(0.ipx)
        }
    }
}