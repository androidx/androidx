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
package androidx.ui.material

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.test.filters.MediumTest

import androidx.ui.core.Density
import androidx.ui.core.Dp
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.PxPosition
import androidx.ui.core.TestTag
import androidx.ui.core.dp
import androidx.ui.core.withDensity
import androidx.ui.foundation.Clickable
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.layout.Padding
import androidx.ui.layout.Row
import androidx.ui.layout.Wrap
import androidx.ui.material.ripple.CurrentRippleTheme
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.ripple.RippleEffect
import androidx.ui.material.ripple.RippleEffectFactory
import androidx.ui.material.ripple.RippleSurfaceOwner
import androidx.ui.material.ripple.RippleTheme
import androidx.ui.material.surface.Card
import androidx.ui.painting.Canvas
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.vectormath64.Matrix4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(JUnit4::class)
class RippleEffectTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rippleEffectMatrixHasOffsetFromSurface() {
        val latch = CountDownLatch(1)
        var matrix: Matrix4? = null

        val padding = 10.dp
        composeTestRule.setMaterialContent {
            RippleCallback(onRippleDrawn = {
                matrix = it
                latch.countDown()
            }) {
                Card {
                    Padding(padding = padding) {
                        TestTag(tag = "ripple") {
                            RippleButton()
                        }
                    }
                }
            }
        }

        findByTag("ripple")
            .doClick()

        // wait for drawEffect to be called
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        // verify matrix contains the expected padding
        assertNotNull(matrix)
        val paddingFloat = withDensity(composeTestRule.density) {
            padding.toIntPx().value.toFloat()
        }
        val expectedMatrix = Matrix4.translationValues(
            paddingFloat,
            paddingFloat,
            0f
        )
        assertEquals(expectedMatrix, matrix)
    }

    @Test
    fun rippleEffectMatrixHasTheClickedChildCoordinates() {
        val latch = CountDownLatch(1)
        var matrix: Matrix4? = null

        val size = 10.dp
        composeTestRule.setMaterialContent {
            RippleCallback(onRippleDrawn = {
                matrix = it
                latch.countDown()
            }) {
                Card {
                    Wrap {
                        Row {
                            RippleButton(size)
                            TestTag(tag = "ripple") {
                                RippleButton(size)
                            }
                            RippleButton(size)
                        }
                    }
                }
            }
        }

        findByTag("ripple")
            .doClick()

        // wait for drawEffect to be called
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        // verify matrix contains the expected padding
        assertNotNull(matrix)
        val offsetFloat = withDensity(composeTestRule.density) { size.toIntPx().value.toFloat() }
        val expectedMatrix = Matrix4.translationValues(
            offsetFloat,
            0f,
            0f
        )
        assertEquals(expectedMatrix, matrix)
    }

    private fun RippleButton(size: Dp? = null) {
        Ripple(bounded = false) {
            Clickable(onClick = {}) {
                Container(width = size, height = size) {}
            }
        }
    }

    @Composable
    private fun RippleCallback(
        onRippleDrawn: (Matrix4) -> Unit,
        @Children children: @Composable() () -> Unit
    ) {
        val theme = RippleTheme(testRippleEffect(onRippleDrawn)) { Color(0) }
        CurrentRippleTheme.Provider(value = theme, children = children)
    }

    private fun testRippleEffect(onDraw: (Matrix4) -> Unit): RippleEffectFactory =
        object : RippleEffectFactory {
            override fun create(
                rippleSurface: RippleSurfaceOwner,
                coordinates: LayoutCoordinates,
                touchPosition: PxPosition,
                color: Color,
                density: Density,
                radius: Dp?,
                bounded: Boolean,
                onRemoved: (() -> Unit)?
            ): RippleEffect {
                return object : RippleEffect(rippleSurface, coordinates, color, onRemoved) {

                    init {
                        rippleSurface.addEffect(this)
                    }

                    override fun drawEffect(canvas: Canvas, transform: Matrix4) {
                        onDraw(transform)
                    }
                }
            }
        }
}