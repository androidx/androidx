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

import android.os.Build
import androidx.animation.AnimationClockObservable
import androidx.animation.AnimationClockObserver
import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.clickable
import androidx.ui.foundation.drawBackground
import androidx.ui.geometry.Offset
import androidx.ui.geometry.Size
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.drawscope.DrawScope
import androidx.ui.graphics.drawscope.clipRect
import androidx.ui.layout.Row
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSize
import androidx.ui.material.ripple.RippleEffect
import androidx.ui.material.ripple.RippleEffectFactory
import androidx.ui.material.ripple.RippleTheme
import androidx.ui.material.ripple.RippleThemeAmbient
import androidx.ui.material.ripple.ripple
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.runOnUiThread
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(JUnit4::class)
class RippleTest {

    private val contentTag = "ripple"

    @get:Rule
    val composeTestRule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun rippleDrawsCorrectly() {
        composeTestRule.setMaterialContent {
            DrawRectRippleCallback {
                Box(
                    modifier = Modifier.drawBackground(Color.Blue).testTag(contentTag),
                    gravity = ContentGravity.Center
                ) {
                    Box(
                        Modifier.preferredSize(10.dp)
                            .ripple()
                    )
                }
            }
        }

        findByTag(contentTag)
            .doClick()

        val bitmap = findByTag(contentTag).captureToBitmap()
        with(composeTestRule.density) {
            bitmap.assertShape(
                density = composeTestRule.density,
                backgroundColor = Color.Blue,
                shape = RectangleShape,
                shapeSizeX = 10.dp.toPx(),
                shapeSizeY = 10.dp.toPx(),
                shapeColor = Color.Red
            )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun rippleUsesCorrectSize() {
        composeTestRule.setMaterialContent {
            DrawRectRippleCallback {
                Box(
                    modifier = Modifier.drawBackground(Color.Blue).testTag(contentTag),
                    gravity = ContentGravity.Center
                ) {
                    Box(
                        Modifier.preferredSize(30.dp)
                            .padding(5.dp)
                            .ripple()
                            // this padding should not affect the size of the ripple
                            .padding(5.dp)
                    )
                }
            }
        }

        findByTag(contentTag)
            .doClick()

        val bitmap = findByTag(contentTag).captureToBitmap()
        with(composeTestRule.density) {
            bitmap.assertShape(
                density = composeTestRule.density,
                backgroundColor = Color.Blue,
                shape = RectangleShape,
                shapeSizeX = 20.dp.toPx(),
                shapeSizeY = 20.dp.toPx(),
                shapeColor = Color.Red
            )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun unboundedIsNotClipped() {
        composeTestRule.setMaterialContent {
            DrawRectRippleCallback {
                Box(
                    modifier = Modifier.drawBackground(Color.Blue).testTag(contentTag),
                    gravity = ContentGravity.Center
                ) {
                    Box(Modifier.preferredSize(10.dp).ripple(bounded = false))
                }
            }
        }

        findByTag(contentTag)
            .doClick()

        val bitmap = findByTag(contentTag).captureToBitmap()
        bitmap.assertShape(
            density = composeTestRule.density,
            backgroundColor = Color.Red,
            shape = RectangleShape,
            shapeSizeX = 0.0f,
            shapeSizeY = 0.0f,
            shapeColor = Color.Red
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun rippleDrawnAfterContent() {
        composeTestRule.setMaterialContent {
            DrawRectRippleCallback {
                Box(
                    modifier = Modifier.drawBackground(Color.Blue).testTag(contentTag),
                    gravity = ContentGravity.Center
                ) {
                    Box(
                        Modifier.preferredSize(10.dp)
                            .ripple()
                            .drawBackground(Color.Blue)
                    )
                }
            }
        }

        findByTag(contentTag)
            .doClick()

        val bitmap = findByTag(contentTag).captureToBitmap()
        with(composeTestRule.density) {
            bitmap.assertShape(
                density = composeTestRule.density,
                backgroundColor = Color.Blue,
                shape = RectangleShape,
                shapeSizeX = 10.0.dp.toPx(),
                shapeSizeY = 10.0.dp.toPx(),
                shapeColor = Color.Red
            )
        }
    }

    @Test
    fun twoEffectsDrawnAndDisposedCorrectly() {
        val drawLatch = CountDownLatch(2)
        val disposeLatch = CountDownLatch(2)
        var emit by mutableStateOf(true)

        composeTestRule.setMaterialContent {
            RippleCallback(
                onDraw = { drawLatch.countDown() },
                onDispose = { disposeLatch.countDown() }
            ) {
                Card {
                    if (emit) {
                        Row {
                            RippleButton(modifier = Modifier.testTag(contentTag))
                        }
                    }
                }
            }
        }

        // create two effects
        findByTag(contentTag)
            .doClick()
        findByTag(contentTag)
            .doClick()

        // wait for drawEffect to be called
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))

        runOnUiThread { emit = false }

        // wait for dispose to be called
        assertTrue(disposeLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun animationIsDisposedCorrectly() {
        var emit by mutableStateOf(true)
        var effectCreated = false
        var rippleAnimationTime = 0L

        val factory = object : RippleEffectFactory {
            override fun create(
                size: PxSize,
                startPosition: PxPosition,
                density: Density,
                radius: Dp?,
                clipped: Boolean,
                clock: AnimationClockObservable,
                onAnimationFinished: (RippleEffect) -> Unit
            ): RippleEffect {
                clock.subscribe(object : AnimationClockObserver {
                    override fun onAnimationFrame(frameTimeMillis: Long) {
                        rippleAnimationTime = frameTimeMillis
                    }
                })
                effectCreated = true
                return object : RippleEffect {
                    override fun DrawScope.draw(color: Color) {}
                    override fun finish(canceled: Boolean) {}
                }
            }
        }

        composeTestRule.clockTestRule.pauseClock()

        composeTestRule.setMaterialContent {
            Providers(
                RippleThemeAmbient provides RippleThemeAmbient.current.copy(
                    factory = factory
                )
            ) {
                Card {
                    if (emit) {
                        Row {
                            RippleButton(modifier = Modifier.testTag(contentTag))
                        }
                    }
                }
            }
        }

        // create an effect
        findByTag(contentTag)
            .doClick()

        // wait for drawEffect to be called
        assertThat(effectCreated).isTrue()
        assertThat(rippleAnimationTime).isEqualTo(0)

        // we got a first frame
        composeTestRule.clockTestRule.advanceClock(100)
        assertThat(rippleAnimationTime).isNotEqualTo(0)
        var prevValue = rippleAnimationTime

        // animation is working and we are getting the next frames
        composeTestRule.clockTestRule.advanceClock(100)
        assertThat(rippleAnimationTime).isGreaterThan(prevValue)
        prevValue = rippleAnimationTime

        runOnIdleCompose {
            emit = false
        }

        runOnIdleCompose {
            // wait for the dispose to be applied
        }

        composeTestRule.clockTestRule.advanceClock(100)
        // asserts our animation is disposed and not reacting on a new timestamp
        assertThat(rippleAnimationTime).isEqualTo(prevValue)
    }

    @Test
    fun rippleColorAndOpacityAreTakenFromTheme() {
        val drawLatch = CountDownLatch(1)
        val color = Color.Yellow
        val opacity = 0.2f
        composeTestRule.setMaterialContent {
            RippleCallback(
                defaultColor = { color },
                opacityCallback = { opacity },
                onDraw = { actualColor ->
                    assertEquals(color.copy(alpha = opacity), actualColor)
                    drawLatch.countDown()
                }
            ) {
                RippleButton(modifier = Modifier.testTag(contentTag))
            }
        }

        findByTag(contentTag)
            .doClick()

        // wait for drawEffect to be called
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun rippleOpacityIsTakenFromTheme() {
        val drawLatch = CountDownLatch(1)
        val color = Color.Green
        val opacity = 0.2f
        composeTestRule.setMaterialContent {
            RippleCallback(
                opacityCallback = { opacity },
                onDraw = { actualColor ->
                    assertEquals(color.copy(alpha = opacity), actualColor)
                    drawLatch.countDown()
                }
            ) {
                RippleButton(modifier = Modifier.testTag(contentTag), color = color)
            }
        }

        findByTag(contentTag)
            .doClick()

        // wait for drawEffect to be called
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun disabledRippleDoesntCreateEffects() {
        val createdLatch = CountDownLatch(1)

        composeTestRule.setMaterialContent {
            RippleCallback(
                onEffectCreated = { createdLatch.countDown() }
            ) {
                Card {
                    RippleButton(modifier = Modifier.testTag(contentTag), enabled = false)
                }
            }
        }

        // create two effects
        findByTag(contentTag)
            .doClick()

        // assert no effects has been created
        assertFalse(createdLatch.await(500, TimeUnit.MILLISECONDS))
    }

    @Test
    fun rippleColorChangeWhileAnimatingAppliedCorrectly() {
        var drawLatch = CountDownLatch(1)
        var actualColor = Color.Transparent
        var colorState by mutableStateOf(Color.Yellow)
        composeTestRule.setMaterialContent {
            RippleCallback(
                defaultColor = { colorState },
                onDraw = { color ->
                    actualColor = color
                    drawLatch.countDown()
                }
            ) {
                RippleButton(modifier = Modifier.testTag(contentTag))
            }
        }

        findByTag(contentTag)
            .doClick()

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertEquals(Color.Yellow, actualColor)

        drawLatch = CountDownLatch(1)
        runOnUiThread {
            colorState = Color.Green
        }

        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertEquals(Color.Green, actualColor)
    }

    @Composable
    private fun RippleButton(
        modifier: Modifier = Modifier,
        size: Dp = 10.dp,
        color: Color = Color.Unset,
        enabled: Boolean = true
    ) {
        Box(modifier
            .ripple(bounded = false, color = color, enabled = enabled)
            .clickable(indication = null) {}) {
            Box(Modifier.preferredSize(size))
        }
    }

    @Composable
    fun DrawRectRippleCallback(children: @Composable () -> Unit) {
        RippleCallback(
            onDraw = {
                drawRect(
                    Color.Red,
                    topLeft = Offset(-100000f, -100000f),
                    size = Size(200000f, 200000f)
                )
            },
            children = children
        )
    }

    @Composable
    private fun RippleCallback(
        onDraw: DrawScope.(Color) -> Unit = { _ -> },
        onDispose: () -> Unit = {},
        onEffectCreated: () -> Unit = {},
        defaultColor: @Composable () -> Color = { Color(0) },
        opacityCallback: @Composable () -> Float = { 1f },
        children: @Composable () -> Unit
    ) {
        val theme = RippleTheme(
            testRippleEffect(onDraw, onDispose, onEffectCreated),
            defaultColor,
            opacityCallback
        )
        Providers(RippleThemeAmbient provides theme, children = children)
    }

    private fun testRippleEffect(
        onDraw: DrawScope.(Color) -> Unit,
        onDispose: () -> Unit,
        onEffectCreated: () -> Unit
    ): RippleEffectFactory =
        object : RippleEffectFactory {
            override fun create(
                size: PxSize,
                startPosition: PxPosition,
                density: Density,
                radius: Dp?,
                clipped: Boolean,
                clock: AnimationClockObservable,
                onAnimationFinished: (RippleEffect) -> Unit
            ): RippleEffect {
                onEffectCreated()
                return object : RippleEffect {
                    override fun DrawScope.draw(color: Color) {
                        if (clipped) {
                            clipRect {
                                this@draw.onDraw(color)
                            }
                        } else {
                            this@draw.onDraw(color)
                        }
                    }

                    override fun finish(canceled: Boolean) {
                    }

                    override fun dispose() {
                        onDispose()
                    }
                }
            }
        }
}
