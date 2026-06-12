/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.animation

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EnterExitTransitionConfigTest {

    @Test
    fun testFadeInConfig() {
        val spec = tween<Float>(durationMillis = 300)
        val enter = fadeIn(animationSpec = spec, initialAlpha = 0.5f)
        val config = enter.config

        assertNotNull(config.fade)
        assertEquals(0.5f, config.fade!!.alpha)
        assertEquals(spec, config.fade!!.animationSpec)

        assertNull(config.slide)
        assertNull(config.changeSize)
        assertNull(config.scale)
        assertNull(config.veil)
    }

    @Test
    fun testFadeOutConfig() {
        val spec = spring<Float>()
        val exit = fadeOut(animationSpec = spec, targetAlpha = 0.2f)
        val config = exit.config

        assertNotNull(config.fade)
        assertEquals(0.2f, config.fade!!.alpha)
        assertEquals(spec, config.fade!!.animationSpec)
    }

    @Test
    fun testSlideInConfig() {
        val spec = tween<IntOffset>()
        val offsetLambda: (IntSize) -> IntOffset = { IntOffset(it.width / 2, 0) }
        val enter = slideIn(animationSpec = spec, initialOffset = offsetLambda)
        val config = enter.config

        assertNotNull(config.slide)
        assertEquals(spec, config.slide!!.animationSpec)
        assertEquals(offsetLambda, config.slide!!.slideOffset)
    }

    @Test
    fun testSlideOutConfig() {
        val spec = spring<IntOffset>()
        val offsetLambda: (IntSize) -> IntOffset = { IntOffset(0, it.height / 2) }
        val exit = slideOut(animationSpec = spec, targetOffset = offsetLambda)
        val config = exit.config

        assertNotNull(config.slide)
        assertEquals(spec, config.slide!!.animationSpec)
        assertEquals(offsetLambda, config.slide!!.slideOffset)
    }

    @Test
    fun testChangeSizeConfig() {
        val spec = spring<IntSize>()
        val sizeLambda: (IntSize) -> IntSize = { IntSize(it.width / 4, it.height / 4) }
        val enter =
            expandIn(
                animationSpec = spec,
                expandFrom = Alignment.BottomEnd,
                clip = false,
                initialSize = sizeLambda,
            )
        val config = enter.config

        assertNotNull(config.changeSize)
        assertEquals(Alignment.BottomEnd, config.changeSize!!.alignment)
        assertEquals(sizeLambda, config.changeSize!!.size)
        assertEquals(spec, config.changeSize!!.animationSpec)
        assertEquals(false, config.changeSize!!.clip)
    }

    @Test
    fun testShrinkConfig() {
        val spec = tween<IntSize>()
        val sizeLambda: (IntSize) -> IntSize = { IntSize(0, 0) }
        val exit =
            shrinkOut(
                animationSpec = spec,
                shrinkTowards = Alignment.Center,
                clip = true,
                targetSize = sizeLambda,
            )
        val config = exit.config

        assertNotNull(config.changeSize)
        assertEquals(Alignment.Center, config.changeSize!!.alignment)
        assertEquals(sizeLambda, config.changeSize!!.size)
        assertEquals(spec, config.changeSize!!.animationSpec)
        assertEquals(true, config.changeSize!!.clip)
    }

    @Test
    fun testScaleConfig() {
        val spec = tween<Float>()
        val enter =
            scaleIn(
                animationSpec = spec,
                initialScale = 0.8f,
                transformOrigin = TransformOrigin(0.1f, 0.2f),
            )
        val config = enter.config

        assertNotNull(config.scale)
        assertEquals(0.8f, config.scale!!.scale)
        assertEquals(TransformOrigin(0.1f, 0.2f), config.scale!!.transformOrigin)
        assertEquals(spec, config.scale!!.animationSpec)
    }

    @Test
    fun testScaleOutConfig() {
        val spec = spring<Float>()
        val exit =
            scaleOut(
                animationSpec = spec,
                targetScale = 0.5f,
                transformOrigin = TransformOrigin.Center,
            )
        val config = exit.config

        assertNotNull(config.scale)
        assertEquals(0.5f, config.scale!!.scale)
        assertEquals(TransformOrigin.Center, config.scale!!.transformOrigin)
        assertEquals(spec, config.scale!!.animationSpec)
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun testVeilConfig() {
        val spec = tween<Color>()
        val enter =
            unveilIn(animationSpec = spec, initialColor = Color.Red, matchParentSize = false)
        val config = enter.config

        assertNotNull(config.veil)
        assertEquals(Color.Red, config.veil!!.initialColor)
        assertEquals(Color.Red.copy(alpha = 0f), config.veil!!.targetColor)
        assertEquals(spec, config.veil!!.animationSpec)
        assertEquals(false, config.veil!!.matchParentSize)
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Test
    fun testVeilOutConfig() {
        val spec = spring<Color>()
        val exit = veilOut(animationSpec = spec, targetColor = Color.Blue, matchParentSize = true)
        val config = exit.config

        assertNotNull(config.veil)
        assertEquals(Color.Blue.copy(alpha = 0f), config.veil!!.initialColor)
        assertEquals(Color.Blue, config.veil!!.targetColor)
        assertEquals(spec, config.veil!!.animationSpec)
        assertEquals(true, config.veil!!.matchParentSize)
    }

    @Test
    fun testCombinedConfig() {
        val fadeSpec = tween<Float>(100)
        val scaleSpec = spring<Float>()
        val enter = fadeIn(animationSpec = fadeSpec) + scaleIn(animationSpec = scaleSpec)
        val config = enter.config

        assertNotNull(config.fade)
        assertNotNull(config.scale)
        assertEquals(fadeSpec, config.fade!!.animationSpec)
        assertEquals(scaleSpec, config.scale!!.animationSpec)
        assertNull(config.slide)
    }
}
