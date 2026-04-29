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

@file:Suppress("RemoveExplicitTypeArguments")

package androidx.compose.foundation.style

import androidx.compose.animation.core.tween
import androidx.compose.foundation.platform.SynchronizedObject
import androidx.compose.foundation.platform.synchronized
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.coroutines.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

@ExperimentalFoundationStyleApi
class StyleAnimationsTest {
    @Test
    fun can_create_animations() {
        val animations = StyleAnimations()
        assertNotNull(animations)
    }

    @Test
    fun can_animate_contentPaddingStart() = runTest {
        animateDp({ contentPaddingStart(it) }, { contentPaddingStart })
        animateDpFromDefault({ contentPaddingStart(it) }, { contentPaddingStart })
    }

    @Test
    fun can_animate_contentPaddingEnd() = runTest {
        animateDp({ contentPaddingEnd(it) }, { contentPaddingEnd })
        animateDpFromDefault({ contentPaddingEnd(it) }, { contentPaddingEnd })
    }

    @Test
    fun can_animate_contentPaddingTop() = runTest {
        animateDp({ contentPaddingTop(it) }, { contentPaddingTop })
        animateDpFromDefault({ contentPaddingTop(it) }, { contentPaddingTop })
    }

    @Test
    fun can_animate_contentPaddingBottom() = runTest {
        animateDp({ contentPaddingBottom(it) }, { contentPaddingBottom })
        animateDpFromDefault({ contentPaddingBottom(it) }, { contentPaddingBottom })
    }

    @Test
    fun can_animate_externalPaddingStart() = runTest {
        animateDp({ externalPaddingStart(it) }, { externalPaddingStart })
        animateDpFromDefault({ externalPaddingStart(it) }, { externalPaddingStart })
    }

    @Test
    fun can_animate_externalPaddingEnd() = runTest {
        animateDp({ externalPaddingEnd(it) }, { externalPaddingEnd })
        animateDpFromDefault({ externalPaddingEnd(it) }, { externalPaddingEnd })
    }

    @Test
    fun can_animate_externalPaddingTop() = runTest {
        animateDp({ externalPaddingTop(it) }, { externalPaddingTop })
        animateDpFromDefault({ externalPaddingTop(it) }, { externalPaddingTop })
    }

    @Test
    fun can_animate_externalPaddingBottom() = runTest {
        animateDp({ externalPaddingBottom(it) }, { externalPaddingBottom })
        animateDpFromDefault({ externalPaddingBottom(it) }, { externalPaddingBottom })
    }

    @Test
    fun can_animate_borderWidth() = runTest {
        animateDp({ borderWidth(it) }, { borderWidth })
        animateDpFromDefault({ borderWidth(it) }, { borderWidth })
    }

    @Test
    fun can_animate_borderColor() = runTest { animateColor({ borderColor(it) }, { borderColor }) }

    @Test
    fun can_animate_borderBrush() = runTest { animateBrush({ borderBrush(it) }, { borderBrush }) }

    @Test fun can_animate_width() = runTest { animateDp({ width(it) }, { width }) }

    @Test fun can_animate_height() = runTest { animateDp({ height(it) }, { height }) }

    @Test
    fun can_animate_widthFraction() = runTest { animateFloat({ width(it) }, { widthFraction }) }

    @Test
    fun can_animate_heightFraction() = runTest { animateFloat({ height(it) }, { heightFraction }) }

    @Test
    fun can_animate_left() = runTest {
        animateDp({ left(it) }, { left })
        animateDpFromDefault({ left(it) }, { left })
    }

    @Test
    fun can_animate_right() = runTest {
        animateDp({ right(it) }, { right })
        animateDpFromDefault({ right(it) }, { right })
    }

    @Test
    fun can_animate_top() = runTest {
        animateDp({ top(it) }, { top })
        animateDpFromDefault({ top(it) }, { top })
    }

    @Test
    fun can_animate_bottom() = runTest {
        animateDp({ bottom(it) }, { bottom })
        animateDpFromDefault({ bottom(it) }, { bottom })
    }

    @Test fun can_animate_minWidth() = runTest { animateDp({ minWidth(it) }, { minWidth }) }

    @Test fun can_animate_maxWidth() = runTest { animateDp({ maxWidth(it) }, { maxWidth }) }

    @Test fun can_animate_minHeight() = runTest { animateDp({ minHeight(it) }, { minHeight }) }

    @Test fun can_animate_maxHeight() = runTest { animateDp({ maxHeight(it) }, { maxHeight }) }

    @Test
    fun can_animate_alpha() = runTest {
        animateFloat({ alpha(it) }, { alpha })
        animateFloatFromDefault({ alpha(it) }, { alpha }, end = 0f, assumeDefault = 1f)
    }

    @Test
    fun can_animate_scaleX() = runTest {
        animateFloat({ scaleX(it) }, { scaleX })
        animateFloatFromDefault({ scaleX(it) }, { scaleX }, end = 0f, assumeDefault = 1f)
    }

    @Test
    fun can_animate_scaleY() = runTest {
        animateFloat({ scaleY(it) }, { scaleY })
        animateFloatFromDefault({ scaleX(it) }, { scaleX }, end = 0f, assumeDefault = 1f)
    }

    @Test
    fun can_animate_translationX() = runTest {
        animateFloat({ translationX(it) }, { translationX })
        animateFloatFromDefault({ translationX(it) }, { translationX })
    }

    @Test
    fun can_animate_translationY() = runTest {
        animateFloat({ translationY(it) }, { translationY })
        animateFloatFromDefault({ translationY(it) }, { translationY })
    }

    @Test
    fun can_animate_rotationX() = runTest {
        animateFloat({ rotationX(it) }, { rotationX })
        animateFloatFromDefault({ rotationX(it) }, { rotationX })
    }

    @Test
    fun can_animate_rotationY() = runTest {
        animateFloat({ rotationY(it) }, { rotationY })
        animateFloatFromDefault({ rotationY(it) }, { rotationY })
    }

    @Test
    fun can_animate_rotationZ() = runTest {
        animateFloat({ rotationZ(it) }, { rotationZ })
        animateFloatFromDefault({ rotationZ(it) }, { rotationZ })
    }

    @Test
    fun can_animate_clip() = runTest { animate({ clip(it) }, { clip }, start = true, end = false) }

    @Test fun can_animate_zIndex() = runTest { animateFloat({ zIndex(it) }, { zIndex }) }

    @Test
    fun can_animate_colorFilter() = runTest {
        animate(
            { colorFilter(it) },
            { colorFilter },
            start = ColorFilter.tint(Color.Red),
            end = ColorFilter.tint(Color.Blue),
        )
    }

    @Test
    fun can_animate_backgroundColor() = runTest {
        animateColor({ background(it) }, { backgroundColor })
    }

    @Test
    fun can_animate_backgroundBrush() = runTest {
        animateBrush({ background(it) }, { backgroundBrush })
    }

    @Test
    fun can_animate_foregroundColor() = runTest {
        animateColor({ foreground(it) }, { foregroundColor })
    }

    @Test
    fun can_animate_foregroundBrush() = runTest {
        animateBrush({ foreground(it) }, { foregroundBrush })
    }

    @Test
    fun can_animate_shape() = runTest {
        animate({ shape(it) }, { shape }, RectangleShape, CircleShape)
    }

    @Test
    fun can_animate_dropShadow() = runTest { animateShadow({ dropShadow(it) }, { dropShadow }) }

    @Test
    fun can_animate_innerShadow() = runTest { animateShadow({ innerShadow(it) }, { innerShadow }) }

    @Test
    fun can_animate_contentColor() = runTest {
        animateColor({ contentColor(it) }, { contentColor })
    }

    @Test
    fun can_animate_contentBrush() = runTest {
        animateBrush({ contentBrush(it) }, { contentBrush })
    }

    @Test
    fun can_animate_textDecoration() = runTest {
        animate(
            { textDecoration(it) },
            { textDecoration },
            TextDecoration.None,
            TextDecoration.LineThrough,
        )
    }

    @Test
    fun can_animate_fontFamily() = runTest {
        animate({ fontFamily(it) }, { fontFamily!! }, FontFamily.Default, FontFamily.Cursive)
    }

    @Test
    fun can_animate_textMotion() = runTest {
        animate(
            { textMotion(it) },
            { textMotion },
            start = TextMotion.Static,
            end = TextMotion.Animated,
        )
    }

    @Test
    fun can_animate_textIndent() = runTest {
        animate({ textIndent(it) }, { textIndent!! }, TextIndent(), TextIndent(5.sp, 10.sp))
    }

    @Test fun can_animate_fontSize() = runTest { animateTextUnit({ fontSize(it) }, { fontSize }) }

    @Test
    fun can_animate_lineHeight() = runTest { animateTextUnit({ lineHeight(it) }, { lineHeight }) }

    @Test
    fun can_animate_letterSpacing() = runTest {
        animateTextUnit({ letterSpacing(it) }, { letterSpacing })
    }

    @Test
    fun can_animate_baselineShift() = runTest {
        animate(
            { baselineShift(it) },
            { baselineShift },
            BaselineShift.None,
            BaselineShift.Subscript,
        )
    }

    @Test
    fun can_animate_fontWeight() = runTest {
        animate({ fontWeight(it) }, { fontWeight }, FontWeight.Normal, FontWeight.Bold)
    }

    @Test
    fun can_animate_fontStyle() = runTest {
        animate({ fontStyle(it) }, { fontStyle }, FontStyle.Normal, FontStyle.Italic)
    }

    @Test
    fun can_animate_textAlign() = runTest {
        animate({ textAlign(it) }, { textAlign }, TextAlign.Unspecified, TextAlign.Center)
    }

    @Test
    fun can_animate_textDirection() = runTest {
        animate(
            { textDirection(it) },
            { textDirection },
            TextDirection.Unspecified,
            TextDirection.Rtl,
        )
    }

    @Test
    fun can_animate_lineBreak() = runTest {
        animate({ lineBreak(it) }, { lineBreak }, LineBreak.Simple, LineBreak.Paragraph)
    }

    @Test
    fun can_animate_hyphens() = runTest {
        animate({ hyphens(it) }, { hyphens }, Hyphens.None, Hyphens.Auto)
    }

    @Test
    fun can_animate_fontSynthesis() = runTest {
        animate({ fontSynthesis(it) }, { fontSynthesis }, FontSynthesis.Weight, FontSynthesis.Style)
    }

    @Test
    fun can_animate_brush_out() = runTest {
        val state = MutableStyleState(null).also { it.isPressed = true }
        animateOut(
            {
                background(WhiteBrush)
                pressed { animate { background(BlackBrush) } }
            },
            { backgroundBrush },
            state,
            { state.isPressed = false },
        ) {
            assertTrue(it.size > 2)
            // Assert it starts and ends at WhiteBrush
            assertEquals(WhiteBrush, it.first())
            assertEquals(WhiteBrush, it.last())

            // Assert it animated to BlackBrush
            assertTrue(it.contains(BlackBrush))
        }
    }
}

@ExperimentalFoundationStyleApi
private suspend fun <T> TestScope.animate(
    style: Style,
    collect: StyleProperties.() -> T,
    state: StyleState? = null,
    duration: Int = 1000,
    interval: Int = 50,
    block: suspend TestScope.(List<T>) -> Unit,
) {
    val resolvedStyle = ResolvedStyle()
    val clock = TestFrameClock(this)
    val result = mutableListOf<T>()
    withContext(clock) {
        resolvedStyle.buildForTesting(style, Density(100f), state, this)
        for (frameTimeMillis in 0..duration step interval) {
            clock.frame(frameTimeMillis * 1_000_000L)
        }
        clock.runUntil(duration)

        clock.frameUntilStopped { result.add(collect(resolvedStyle.resolve())) }
        resolvedStyle.closeForTesting()
    }
    block(result)
}

@ExperimentalFoundationStyleApi
private suspend fun <T> TestScope.animateOut(
    style: Style,
    collect: StyleProperties.() -> T,
    state: StyleState? = null,
    out: () -> Unit = {},
    duration: Int = 1000,
    interval: Int = 50,
    block: suspend TestScope.(List<T>) -> Unit,
) {
    val resolvedStyle = ResolvedStyle()
    val clock = TestFrameClock(this)
    val result = mutableListOf<T>()
    withContext(clock) {
        resolvedStyle.buildForTesting(style, Density(100f), state, this)
        for (frameTimeMillis in 0..duration * 2 step interval) {
            clock.frame(frameTimeMillis * 1_000_000L)
        }
        clock.runUntil(duration * 2)

        clock.frameUntil {
            result.add(collect(resolvedStyle.resolve()))
            it >= (duration * 1_000_000L)
        }
        out()
        resolvedStyle.buildForTesting(style, Density(100f), state, this)
        clock.frameUntilStopped { result.add(collect(resolvedStyle.resolve())) }
        resolvedStyle.closeForTesting()
    }
    block(result)
}

@ExperimentalFoundationStyleApi
private suspend fun <T> TestScope.animate(
    set: StyleScope.(value: T) -> Unit,
    collect: StyleProperties.() -> T,
    start: T,
    end: T,
) {
    animate(
        style = {
            set(start)
            pressed { animate(tween(900)) { set(end) } }
        },
        collect,
        state = MutableStyleState(null).apply { isPressed = true },
    ) { values ->
        assertEquals(start, values.first())
        assertEquals(end, values.last())
        assertTrue(values.size > 2)
    }
}

@ExperimentalFoundationStyleApi
private suspend fun TestScope.animateDp(
    set: StyleScope.(value: Dp) -> Unit,
    collect: StyleProperties.() -> Float,
    start: Dp = 10.dp,
    end: Dp = 100.dp,
) {
    // Animate from a specified value
    animate(
        style = {
            set(start)
            pressed { animate { set(end) } }
        },
        collect,
        state = MutableStyleState(null).apply { isPressed = true },
    ) { pixels ->
        assertEquals(start.value * 100f, pixels.first())
        assertEquals(end.value * 100f, pixels.last())
        assertTrue(pixels.size > 2)
    }

    // Animate from the default value
}

@ExperimentalFoundationStyleApi
private suspend fun TestScope.animateDpFromDefault(
    set: StyleScope.(value: Dp) -> Unit,
    collect: StyleProperties.() -> Float,
    end: Dp = 100.dp,
) {
    animate(
        style = { pressed { animate { set(end) } } },
        collect,
        state = MutableStyleState(null).apply { isPressed = true },
    ) { pixels ->
        // Assert some animation occurs
        assertTrue(pixels.size > 2)
        assertNotNull(pixels.firstOrNull { it > 0f && it < end.value * 100f })

        // Assert we land where we were supposed to.
        assertEquals(end.value * 100f, pixels.last())
    }
}

@ExperimentalFoundationStyleApi
private suspend fun TestScope.animateColor(
    set: StyleScope.(value: Color) -> Unit,
    collect: StyleProperties.() -> Color,
    start: Color = Color.Black,
    end: Color = Color.White,
) {
    animate(
        style = {
            set(start)
            pressed { animate { set(end) } }
        },
        collect,
        state = MutableStyleState(null).apply { isPressed = true },
    ) { colors ->
        assertEquals(start, colors.first())
        assertEquals(end, colors.last())
        assertTrue(colors.size > 2)
    }
}

@ExperimentalFoundationStyleApi
private suspend fun TestScope.animateFloatFromDefault(
    set: StyleScope.(value: Float) -> Unit,
    collect: StyleProperties.() -> Float,
    end: Float = 1f,
    assumeDefault: Float = 0f,
) {
    animate(
        style = { pressed { animate { set(end) } } },
        collect,
        state = MutableStyleState(null).apply { isPressed = true },
    ) { pixels ->
        // Assert some animation occurs
        assertTrue(pixels.size > 2)
        if (assumeDefault > end) {
            assertNotNull(pixels.firstOrNull { it > end && it < assumeDefault })
        } else {
            assertNotNull(pixels.firstOrNull { it > assumeDefault && it < end })
        }

        // Assert we land where we were supposed to.
        assertEquals(end, pixels.last())
    }
}

private val WhiteBrush = SolidColor(Color.White)
private val BlackBrush = SolidColor(Color.Black)

@ExperimentalFoundationStyleApi
private suspend fun TestScope.animateBrush(
    set: StyleScope.(value: Brush) -> Unit,
    collect: StyleProperties.() -> Brush?,
    start: Brush = BlackBrush,
    end: Brush = WhiteBrush,
) {
    animate(
        style = {
            set(start)
            pressed { animate { set(end) } }
        },
        { collect()!! },
        state = MutableStyleState(null).apply { isPressed = true },
    ) { brushes ->
        assertEquals(start, brushes.first())
        assertEquals(end, brushes.last())
        assertTrue(brushes.size > 2)
    }
}

@ExperimentalFoundationStyleApi
private suspend fun TestScope.animateFloat(
    set: StyleScope.(value: Float) -> Unit,
    collect: StyleProperties.() -> Float,
    start: Float = 0f,
    end: Float = 1f,
) {
    animate(
        style = {
            set(start)
            pressed { animate { set(end) } }
        },
        collect,
        state = MutableStyleState(null).apply { isPressed = true },
    ) { brushes ->
        assertEquals(start, brushes.first())
        assertEquals(end, brushes.last())
        assertTrue(brushes.size > 2)
    }
}

private val BlackShadow = Shadow(5.dp, Color.Black)
private val WhiteShadow = Shadow(10.dp, Color.White)

@ExperimentalFoundationStyleApi
private suspend fun TestScope.animateShadow(
    set: StyleScope.(value: Shadow) -> Unit,
    collect: StyleProperties.() -> Any?,
    start: Shadow = BlackShadow,
    end: Shadow = WhiteShadow,
) = animate<Shadow>(set, { collect()!! as Shadow }, start, end)

@ExperimentalFoundationStyleApi
private suspend fun TestScope.animateTextUnit(
    set: StyleScope.(value: TextUnit) -> Unit,
    collect: StyleProperties.() -> TextUnit,
    start: TextUnit = 0.sp,
    end: TextUnit = 10.sp,
) = animate<TextUnit>(set, collect, start, end)

@ExperimentalFoundationStyleApi
private class TestFrameClock(private val coroutineScope: CoroutineScope) : MonotonicFrameClock {
    private val frameCh = Channel<Long>(Channel.UNLIMITED)
    private val lock = SynchronizedObject()
    private val frameAwaiters = mutableListOf<FrameAwaiter<*>>()
    private var awaiter: Awaiter? = null
    private var stopped = false

    fun runUntil(duration: Int) {
        start()
        coroutineScope.launch {
            while (!stopped) {
                withFrameNanos {
                    if (it >= duration * 1_000_000) {
                        stop()
                    }
                }
            }
        }
    }

    fun start() {
        coroutineScope.launch {
            while (!stopped) {
                val newAwaiter = Awaiter()
                synchronized(lock) {
                    awaiter?.done()
                    awaiter = newAwaiter
                }
                newAwaiter.await()
                if (stopped) break
                val frameTime = frameCh.receive()
                val toRun =
                    synchronized(lock) {
                        val list = frameAwaiters.toList()
                        frameAwaiters.clear()
                        list
                    }
                toRun.map { it.runFrame(frameTime) }.forEach { it() }
            }
            awaiter?.done()
        }
    }

    fun stop() {
        stopped = true
        awaiter?.resume()
    }

    private class FrameAwaiter<R>(
        private val onFrame: (Long) -> R,
        private val continuation: CancellableContinuation<R>,
    ) {
        fun runFrame(frameTimeNanos: Long): () -> Unit {
            val result = runCatching { onFrame(frameTimeNanos) }
            return { continuation.resumeWith(result) }
        }
    }

    private class Awaiter {
        private var continuation: CancellableContinuation<Unit>? = null
        private var done = false

        suspend fun await() {
            if (!done) {
                suspendCancellableCoroutine { continuation = it }
            }
        }

        fun resume() {
            val current = continuation
            continuation = null
            current?.resume(Unit)
        }

        fun done() {
            done = true
            resume()
        }
    }

    suspend fun frame(frameTimeNanos: Long) {
        frameCh.send(frameTimeNanos)
        awaiter?.resume()
    }

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
        suspendCancellableCoroutine { co ->
            synchronized(lock) { frameAwaiters.add(FrameAwaiter(onFrame, co)) }
            awaiter?.resume()
        }

    suspend fun frameUntilStopped(onFrame: (Long) -> Unit) {
        while (!stopped) {
            withFrameNanos(onFrame)
        }
    }

    suspend fun frameUntil(onFrame: (Long) -> Boolean) {
        var run = true
        while (run && !stopped) {
            withFrameNanos { run = !onFrame(it) }
        }
    }
}

@ExperimentalFoundationStyleApi
private fun ResolvedStyle.resolve() = StyleProperties().also { resolveInto(PhaseFlagMask, it) }
