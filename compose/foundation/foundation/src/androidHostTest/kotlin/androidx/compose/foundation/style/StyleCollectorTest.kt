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

@file:OptIn(ExperimentalFoundationStyleApi::class)

package androidx.compose.foundation.style

import androidx.collection.ScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.compose.animation.core.tween
import androidx.compose.foundation.platform.SynchronizedObject
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.lerp
import kotlin.coroutines.resume
import kotlin.math.absoluteValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

class StyleCollectorTest {
    @Test
    fun can_create_a_collector() {
        val collector = StylePropertyCollector()
        assertNotNull(collector)
    }

    @Test
    fun can_read_default_custom_property() {
        collect(CommonStyle) { assertEquals(Fill(Color.Transparent), customBackground) }
    }

    @Test
    fun can_read_set_custom_property() {
        collect({ customBackground(Color.Blue) }) {
            assertEquals(Fill(Color.Blue), customBackground)
        }
    }

    @Test
    fun can_read_brush_custom_property() {
        val brush = SolidColor(Color.Blue)
        collect({ customBackground(brush) }) { assertEquals(Fill(brush), customBackground) }
    }

    @Test
    fun can_set_custom_float_property() {
        val value = 10f
        collect({ customFloat(value) }) { assertEquals(value, customFloat) }
    }

    @Test
    fun can_set_custom_float_property_with_state() {
        val value = 10f
        val stateValue = 100f
        collect(
            {
                customFloat(value)
                pressed { customFloat(stateValue) }
            },
            MutableStyleState(null).apply { isPressed = true },
        ) {
            assertEquals(stateValue, customFloat)
        }
    }

    @Test
    fun can_read_style_state() {
        collect(
            {
                customBackground(Color.Red)
                pressed { customBackground(Color.Blue) }
            },
            MutableStyleState(null).apply { isPressed = true },
        ) {
            assertEquals(Fill(Color.Blue), customBackground)
        }
    }

    @Test
    fun can_animate_a_float_property() = runTest {
        collectAnimated(customFloatProperty, 0f, 100f) {
            // It must start and end with 0f and have 100f somewhere in it.
            assertEquals(0f, it.first())
            assertTrue(it.contains(100f))
            assertEquals(0f, it.last())

            // It must also a value other than 0f and 100f
            assertTrue(it.toSet().size > 2)
        }
    }

    @Test
    fun can_animate_interrupted_property() = runTest {
        collectAnimated(customFloatProperty, 0f, 100f, duration = 500) {
            assertEquals(0f, it.first())
            var expectedUp = true
            // Assert the values go up then down.
            for (i in 0 until it.size - 2) {
                when {
                    (it[i] - it[i + 1]).absoluteValue < 0.001f -> {}
                    it[i + 1] > it[i] -> assertTrue(expectedUp)
                    else ->
                        if (expectedUp) {
                            expectedUp = false
                        }
                }
            }
        }
    }

    @Test
    fun can_animate_between_color_and_brush() = runTest {
        val color = Fill(Color.Blue)
        val brush = Fill(SolidColor(Color.Red))
        collectAnimated(customBackgroundProperty, color, brush) {
            assertEquals(color, it.first())
            assertEquals(color, it.last())
            assertTrue(it.contains(brush))
            assertTrue(it.toSet().size > 2)
        }
    }

    @Test
    fun can_observe_color_change() {
        validateObserve(
            {
                customBackground(Color.Blue)
                pressed { customBackground(Color.Red) }
            },
            customBackgroundProperty,
        )
    }

    @Test
    fun can_observe_color_set() {
        validateObserve({ pressed { customBackground(Color.Red) } }, customBackgroundProperty)
    }

    @Test
    fun validate_changing_a_property_to_same_values_does_report_change() {
        validateObserve(
            {
                customBackground(Color.Red)
                pressed { customBackground(Color.Red) }
            },
            customBackgroundProperty,
        ) { read, changes ->
            assertFalse(changes.any { it in read })
        }
    }

    @Test
    fun can_observe_float_change() {
        validateObserve(
            {
                customFloat(10f)
                pressed { customFloat(100f) }
            },
            customFloatProperty,
        )
    }

    @Test
    fun can_observe_float_local_change() {
        validateObserve(
            {
                floatLocal(10f)
                pressed { floatLocal(100f) }
            },
            customFloatLocal,
        )
    }

    @Test
    fun can_observer_float_local_unprovided() {
        validateObserve({ pressed { floatLocal(100f) } }, customFloatLocal)
    }

    @Test
    fun can_observe_parent_float_local() {
        validateObserve(
            CommonStyle,
            customFloatLocal,
            parentStyle = {
                floatLocal(10f)
                pressed { floatLocal(100f) }
            },
        )
    }

    @Test
    fun can_observe_parent_float_local_unprovided() {
        validateObserve(
            CommonStyle,
            customFloatLocal,
            parentStyle = { pressed { floatLocal(100f) } },
        )
    }

    @Test
    fun can_update_update_local() {
        collectLocal(
            parentStyle = {
                floatLocal(10f)
                pressed { floatLocal(100f) }
            }
        ) { state ->
            if (state.isPressed) assertEquals(100f, floatLocal) else assertEquals(10f, floatLocal)
        }
    }

    @Test
    fun can_unprovide_local() {
        collectLocal(parentStyle = { pressed { floatLocal(100f) } }) { state ->
            if (state.isPressed) assertEquals(100f, floatLocal) else assertEquals(0f, floatLocal)
        }
    }
}

private val customBackgroundProperty =
    stylePropertyOf("customBackground") { Fill(Color.Transparent) }

private fun StylePropertyProviderScope.customBackground(color: Color) {
    customBackgroundProperty.provide(Fill(color))
}

private fun StylePropertyProviderScope.customBackground(brush: Brush) {
    customBackgroundProperty.provide(Fill(brush))
}

private val StylePropertyAccessorScope.customBackground: Fill
    get() = customBackgroundProperty.value

private val customFloatProperty = stylePropertyOf("customFloatProperty", ::lerp) { 0f }

private fun StylePropertyProviderScope.customFloat(value: Float) {
    customFloatProperty.provide(value)
}

private val StylePropertyAccessorScope.customFloat: Float
    get() = customFloatProperty.value

private val customFloatLocal = styleLocalOf("customFloatLocal", ::lerp) { 0f }

private fun StylePropertyProviderScope.floatLocal(value: Float) {
    customFloatLocal.provide(value)
}

private val StylePropertyAccessorScope.floatLocal: Float
    get() = customFloatLocal.value

private fun collect(
    resolver: StyleResolver,
    coroutineScope: CoroutineScope? = null,
    block: StylePropertyAccessorScope.() -> Unit = {},
) {
    val resolverNode = StyleResolverNode(resolver, coroutineScope)
    resolver.bind(resolverNode)
    resolver.actual().collectForTests(Density(10f))
    block(resolver.accessorScope)
}

private fun collect(
    style: CommonStyle,
    styleState: MutableStyleState? = null,
    coroutineScope: CoroutineScope? = null,
    block: StylePropertyAccessorScope.() -> Unit,
) {
    val resolver = StyleResolver(style, styleState ?: MutableStyleState(null))
    collect(resolver, coroutineScope, block)
}

private fun collectLocal(
    style: CommonStyle = CommonStyle,
    styleState: MutableStyleState? = null,
    parentStyle: CommonStyle = CommonStyle,
    coroutineScope: CoroutineScope? = null,
    block: StylePropertyAccessorScope.(state: StyleState) -> Unit,
) {
    val state = styleState ?: MutableStyleState(null)
    val density = Density(100f)
    val parentResolver = StyleResolver(parentStyle, state)
    val parentNode = StyleResolverNode(parentResolver, coroutineScope)
    parentResolver.bind(parentNode)

    val childResolver = StyleResolver(style, state)
    val childNode = StyleResolverNode(childResolver, coroutineScope)
    childResolver.bind(childNode)

    fun resolve() {
        parentResolver.actual().collectForTests(density) { null }
        childResolver.actual().collectForTests(density) {
            parentResolver.accessorScope.getOrNull(it)
        }
        childResolver.accessorScope.block(state)
    }

    // Resolve the initial state
    resolve()

    // Resolver for pressed
    state.isPressed = true
    resolve()

    // Resolver for no longer pressed
    state.isPressed = false
    resolve()
}

private suspend fun <T> TestScope.collectAnimated(
    property: ProvidableStyleProperty<T>,
    initial: T,
    target: T,
    duration: Int = 2000,
    interval: Int = 50,
    block: (List<T>) -> Unit,
) {
    val state = MutableStyleState(null)
    val resolver =
        StyleResolver(
            {
                property.provide(initial)
                pressed { animate(tween(450)) { property.provide(target) } }
            },
            state,
        )
    val clock = TestFrameClock(this)
    val density = Density(10f)
    val result = mutableListOf<T>()
    fun collect() {
        with(resolver.accessorScope) { result.add(property.value) }
    }
    withContext(clock) {
        val resolverNode = StyleResolverNode(resolver, this)
        resolver.bind(resolverNode)
        resolver.actual().collectForTests(density)
        collect()
        state.isPressed = true
        resolver.actual().collectForTests(density)
        for (frameTimeMillis in 0..duration step interval) {
            clock.frame(frameTimeMillis * 1_000_000L)
        }
        clock.runUntil(duration)

        clock.frameUntil {
            collect()
            it > (duration * 1_000_000L) / 2L
        }

        state.isPressed = false
        resolver.actual().collectForTests(density)
        clock.frameUntilStopped { collect() }
        collect()
        resolver.dispose()
    }
    block(result)
}

private fun <T> validateObserve(
    style: CommonStyle,
    property: StyleProperty<T>,
    parentStyle: CommonStyle = CommonStyle,
    validate: (ScatterSet<Any>, ScatterSet<Any>) -> Unit = { read, changes ->
        // By default, validate that an object we read changed.
        assertTrue(changes.any { it in read })
    },
) {
    val read = mutableScatterSetOf<Any>()
    val changes = mutableScatterSetOf<Any>()
    val density = Density(100f)

    // Use the same state for both the parent and child
    val state = MutableStyleState(null)

    // Set up a parent resolver
    val parentResolver = StyleResolver(parentStyle, state)
    val parentNode = StyleResolverNode(parentResolver, null)
    parentResolver.bind(parentNode)

    // Set up the child style resolver
    val resolver = StyleResolver(style, state)
    val node = StyleResolverNode(resolver, null)
    resolver.bind(node)

    parentResolver.actual().collectForTests(density)
    resolver.actual().collectForTests(density) { parentResolver.accessorScope.getOrNull(it) }
    state.isPressed = true
    Snapshot.notifyObjectsInitialized()

    // Collect all the objects read when resolving the property
    Snapshot.observe(readObserver = { read += it }) {
        with(resolver.accessorScope) { assertNotNull(property.value) }
    }

    // Collect all the objects changed after applying
    val handle = Snapshot.registerApplyObserver { set, _ -> changes += set }
    try {
        parentResolver.actual().collectForTests(density)
        resolver.actual().collectForTests(density) { parentResolver.accessorScope.getOrNull(it) }
        Snapshot.sendApplyNotifications()
    } finally {
        handle.dispose()
    }

    validate(read, changes)
}

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
