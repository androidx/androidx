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

package androidx.compose.runtime

import android.view.Choreographer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("UNUSED_VARIABLE")
@MediumTest
@RunWith(AndroidJUnit4::class)
class SideEffectTests : BaseComposeTest() {

    @get:Rule override val activityRule = makeTestActivityRule()

    /** Test that side effects run in order of appearance each time the composable is recomposed. */
    @Test
    fun testSideEffectsRunInOrder() {
        val results = mutableListOf<Int>()
        var resultsAtComposition: List<Int>? = null
        var scope: RecomposeScope? = null
        compose {
                SideEffect { results += 1 }
                SideEffect { results += 2 }
                resultsAtComposition = results.toList()
                scope = currentRecomposeScope
            }
            .then {
                assertEquals(listOf(1, 2), results, "side effects were applied")
                assertEquals(
                    emptyList(),
                    resultsAtComposition,
                    "side effects weren't applied until after composition",
                )
                scope?.invalidate() ?: error("missing recompose function")
            }
            .then {
                assertEquals(listOf(1, 2, 1, 2), results, "side effects applied a second time")
            }
    }

    /**
     * Test that side effects run after lifecycle observers enter the composition, even if their
     * remembrance happens after the SideEffect call appears.
     */
    @Test
    fun testSideEffectsRunAfterLifecycleObservers() {
        class MyObserver : RememberObserver {
            var isPresent: Boolean = false
                private set

            override fun onRemembered() {
                isPresent = true
            }

            override fun onForgotten() {
                isPresent = false
            }

            override fun onAbandoned() {
                fail("Unexpected call to onAbandoned")
            }
        }

        val myObserverOne = MyObserver()
        val myObserverTwo = MyObserver()
        var wasObserverOnePresent = false
        var wasObserverTwoPresent = false

        compose {
                val one = remember { myObserverOne }
                SideEffect {
                    wasObserverOnePresent = myObserverOne.isPresent
                    wasObserverTwoPresent = myObserverTwo.isPresent
                }
                val two = remember { myObserverTwo }
            }
            .then {
                assertTrue(wasObserverOnePresent, "observer one present for side effect")
                assertTrue(wasObserverTwoPresent, "observer two present for side effect")
            }
    }

    @Test
    fun testSideEffectsWithKeys() {
        var results = ""
        var resultsAtComposition = ""
        var scope: RecomposeScope? = null
        var key1 = "A"
        var key2 = "B"
        var key3 = "C"
        var key4 = "D"
        var keys = arrayOf(key1, key2, key3, key4)
        compose {
                SideEffect { results += 1 }
                SideEffect(key1) { results += 2 }
                SideEffect(key1, key2) { results += 3 }
                SideEffect(key1, key2, key3) { results += 4 }
                SideEffect(key1, key2, key3, key4) { results += 5 }
                SideEffect(*keys) { results += 6 }
                resultsAtComposition = results
                scope = currentRecomposeScope
            }
            .then {
                assertEquals("123456", results, "side effects were applied")
                assertEquals(
                    "",
                    resultsAtComposition,
                    "side effects weren't applied until after composition",
                )
                scope?.invalidate() ?: error("missing recompose function")
            }
            .then {
                assertEquals(
                    "123456" + "1",
                    results,
                    "side effects applied a second time (no key changes)",
                )
                key1 = "a"
                keys = arrayOf(key1, key2, key3, key4)
                scope?.invalidate() ?: error("missing recompose function")
            }
            .then {
                assertEquals(
                    "1234561" + "123456",
                    results,
                    "side effects applied a second time (key1 changed)",
                )
                key2 = "b"
                keys = arrayOf(key1, key2, key3, key4)
                scope?.invalidate() ?: error("missing recompose function")
            }
            .then {
                assertEquals(
                    "1234561123456" + "13456",
                    results,
                    "side effects applied a second time (key2 changed)",
                )
                key3 = "b"
                keys = arrayOf(key1, key2, key3, key4)
                scope?.invalidate() ?: error("missing recompose function")
            }
            .then {
                assertEquals(
                    "123456112345613456" + "1456",
                    results,
                    "side effects applied a second time (key3 changed)",
                )
                key4 = "b"
                keys = arrayOf(key1, key2, key3, key4)
                scope?.invalidate() ?: error("missing recompose function")
            }
            .then {
                assertEquals(
                    "1234561123456134561456" + "156",
                    results,
                    "side effects applied a second time (key4 changed)",
                )
                keys = arrayOf("x", "y")
                scope?.invalidate() ?: error("missing recompose function")
            }
            .then {
                assertEquals(
                    "1234561123456134561456156" + "16",
                    results,
                    "side effects applied a second time (keys array changed)",
                )
                keys = arrayOf("x", "y", "z")
                scope?.invalidate() ?: error("missing recompose function")
            }
            .then {
                assertEquals(
                    "123456112345613456145615616" + "16",
                    results,
                    "side effects applied a second time (keys array changed)",
                )
            }
    }

    @Test
    fun testSideEffectsWithStateKeys() {
        var results = ""
        var resultsAtComposition = ""
        var key1 by mutableStateOf("A")
        var key2 by mutableStateOf("B")
        var key3 by mutableStateOf("C")
        var key4 by mutableStateOf("D")
        var keys by mutableStateOf(arrayOf(key1, key2, key3, key4))
        compose {
                SideEffect { results += 1 }
                SideEffect(key1) { results += 2 }
                SideEffect(key1, key2) { results += 3 }
                SideEffect(key1, key2, key3) { results += 4 }
                SideEffect(key1, key2, key3, key4) { results += 5 }
                SideEffect(*keys) { results += 6 }
                resultsAtComposition = results
            }
            .then {
                assertEquals("123456", results, "side effects were applied")
                assertEquals(
                    "",
                    resultsAtComposition,
                    "side effects weren't applied until after composition",
                )
                key1 = "a"
                keys = arrayOf(key1, key2, key3, key4)
            }
            .then {
                assertEquals(
                    "123456" + "123456",
                    results,
                    "side effects applied a second time (key1 changed)",
                )
                key2 = "b"
                keys = arrayOf(key1, key2, key3, key4)
            }
            .then {
                assertEquals(
                    "123456123456" + "13456",
                    results,
                    "side effects applied a second time (key2 changed)",
                )
                key3 = "b"
                keys = arrayOf(key1, key2, key3, key4)
            }
            .then {
                assertEquals(
                    "12345612345613456" + "1456",
                    results,
                    "side effects applied a second time (key3 changed)",
                )
                key4 = "b"
                keys = arrayOf(key1, key2, key3, key4)
            }
            .then {
                assertEquals(
                    "123456123456134561456" + "156",
                    results,
                    "side effects applied a second time (key4 changed)",
                )
                keys = arrayOf("x", "y")
            }
            .then {
                assertEquals(
                    "123456123456134561456156" + "16",
                    results,
                    "side effects applied a second time (keys array changed)",
                )
                keys = arrayOf("x", "y", "z")
            }
            .then {
                assertEquals(
                    "12345612345613456145615616" + "16",
                    results,
                    "side effects applied a second time (keys array changed)",
                )
            }
    }

    @Test
    fun testSideEffectVarargKeyRemoved() {
        var keys by mutableStateOf(arrayOf(1, 2, 3))
        var invocations = 0
        compose { SideEffect(*keys) { invocations++ } }
            .then {
                assertEquals(
                    invocations,
                    1,
                    "SideEffect should execute once after initial composition",
                )
                keys = arrayOf(1, 2)
            }
            .then {
                assertEquals(
                    invocations,
                    2,
                    "SideEffect should execute when dropping trailing keys",
                )
                keys = arrayOf()
            }
            .then {
                assertEquals(invocations, 3, "SideEffect should execute when removing all keys")
            }
    }

    @Test
    fun testDisposableEffectExecutionOrder() {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Unmountable() {
            log("Unmountable:start")
            DisposableEffect(Unit) {
                log("DisposableEffect")
                onDispose { log("onDispose") }
            }
            log("Unmountable:end")
        }

        compose {
                log("compose:start")
                if (mount) {
                    Unmountable()
                }
                log("compose:end")
            }
            .then { _ ->
                assertEquals(
                    listOf(
                        "compose:start",
                        "Unmountable:start",
                        "Unmountable:end",
                        "compose:end",
                        "DisposableEffect",
                    ),
                    logHistory,
                )
                mount = false
            }
            .then { _ ->
                assertEquals(
                    listOf(
                        "compose:start",
                        "Unmountable:start",
                        "Unmountable:end",
                        "compose:end",
                        "DisposableEffect",
                        "compose:start",
                        "compose:end",
                        "onDispose",
                    ),
                    logHistory,
                )
            }
    }

    @Test
    fun testDisposableEffectRelativeOrdering() {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Unmountable() {
            DisposableEffect(Unit) {
                log("DisposableEffect:a2")
                onDispose { log("onDispose:a2") }
            }
            DisposableEffect(Unit) {
                log("DisposableEffect:b2")
                onDispose { log("onDispose:b2") }
            }
        }

        compose {
                DisposableEffect(NeverEqualObject) {
                    log("DisposableEffect:a1")
                    onDispose { log("onDispose:a1") }
                }
                if (mount) {
                    Unmountable()
                }
                DisposableEffect(NeverEqualObject) {
                    log("DisposableEffect:b1")
                    onDispose { log("onDispose:b1") }
                }
            }
            .then { _ ->
                assertEquals(
                    listOf(
                        "DisposableEffect:a1",
                        "DisposableEffect:a2",
                        "DisposableEffect:b2",
                        "DisposableEffect:b1",
                    ),
                    logHistory,
                )
                mount = false
                log("recompose")
            }
            .then { _ ->
                assertEquals(
                    listOf(
                        "DisposableEffect:a1",
                        "DisposableEffect:a2",
                        "DisposableEffect:b2",
                        "DisposableEffect:b1",
                        "recompose",
                        "onDispose:b1",
                        "onDispose:b2",
                        "onDispose:a2",
                        "onDispose:a1",
                        "DisposableEffect:a1",
                        "DisposableEffect:b1",
                    ),
                    logHistory,
                )
            }
    }

    @Test
    fun testDisposableEffectKeyChange() {
        var x = 0
        var key = 123
        lateinit var scope: RecomposeScope

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
                scope = currentRecomposeScope
                DisposableEffect(key) {
                    val y = x++
                    log("DisposableEffect:$y")
                    onDispose { log("dispose:$y") }
                }
            }
            .then { _ ->
                log("recompose")
                scope.invalidate()
            }
            .then { _ ->
                assertEquals(listOf("DisposableEffect:0", "recompose"), logHistory)
                log("recompose (key -> 345)")
                key = 345
                scope.invalidate()
            }
            .then { _ ->
                assertEquals(
                    listOf(
                        "DisposableEffect:0",
                        "recompose",
                        "recompose (key -> 345)",
                        "dispose:0",
                        "DisposableEffect:1",
                    ),
                    logHistory,
                )
            }
    }

    @Test
    fun testLaunchedEffect() {
        var counter by mutableStateOf(0)

        // Used as a signal that LaunchedEffect will await
        val ch = Channel<Unit>(Channel.CONFLATED)
        compose {
                LaunchedEffect(ch) {
                    counter++
                    ch.receive()
                    counter++
                    ch.receive()
                    counter++
                }
            }
            .then {
                assertEquals(1, counter)
                ch.trySend(Unit)
            }
            .then {
                assertEquals(2, counter)
                ch.trySend(Unit)
            }
            .then { assertEquals(3, counter) }
    }

    @Test
    fun testAwaitFrameFromLaunchedEffect() {
        var choreographerTime by mutableStateOf(Long.MIN_VALUE)
        var awaitFrameTime by mutableStateOf(Long.MAX_VALUE)
        compose {
                LaunchedEffect(Unit) { withFrameNanos { awaitFrameTime = it } }
                DisposableEffect(true) {
                    Choreographer.getInstance().postFrameCallback { frameTimeNanos ->
                        choreographerTime = frameTimeNanos
                    }
                    onDispose {}
                }
            }
            .then {
                assertNotEquals(
                    choreographerTime,
                    Long.MIN_VALUE,
                    "Choreographer callback never ran",
                )
                assertNotEquals(
                    awaitFrameTime,
                    Long.MAX_VALUE,
                    "awaitFrameNanos callback never ran",
                )
                assertEquals(
                    choreographerTime,
                    awaitFrameTime,
                    "expected same values from choreographer post and awaitFrameNanos",
                )
            }
    }

    @Test
    fun testLaunchedEffectRunsAfter() {
        var onCommitRan = false
        var launchRanAfter = false
        compose {
                // Confirms that these run "out of order" with respect to one another because
                // the launch runs dispatched.
                LaunchedEffect(Unit) { launchRanAfter = onCommitRan }
                SideEffect { onCommitRan = true }
            }
            .then {
                assertTrue(launchRanAfter, "expected LaunchedEffect to run after later onCommit")
            }
    }

    @OptIn(InternalComposeApi::class)
    @Test
    fun testCoroutineScopesHaveCorrectFrameClock() {
        var recomposerClock: MonotonicFrameClock? = null
        var LaunchedEffectClock: MonotonicFrameClock? = null
        var rememberCoroutineScopeFrameClock: MonotonicFrameClock? = null

        compose {
                recomposerClock = currentComposer.applyCoroutineContext[MonotonicFrameClock]
                LaunchedEffect(Unit) { LaunchedEffectClock = coroutineContext[MonotonicFrameClock] }
                val rememberedScope = rememberCoroutineScope()
                SideEffect {
                    rememberCoroutineScopeFrameClock =
                        rememberedScope.coroutineContext[MonotonicFrameClock]
                }
            }
            .then {
                assertNotNull(recomposerClock, "Recomposer frameClock")
                assertSame(recomposerClock, LaunchedEffectClock, "LaunchedEffect clock")
                assertSame(
                    recomposerClock,
                    rememberCoroutineScopeFrameClock,
                    "rememberCoroutineScope clock",
                )
            }
    }

    @Test
    fun testRememberUpdatedStateRecomposition() {
        @Composable
        fun MyComposable(arg: String, inCh: ReceiveChannel<Unit>, outCh: SendChannel<String>) {
            val currentArg by rememberUpdatedState(arg)

            // This block closes over currentArg and is long-lived; it is important that the
            // value used be updated by recomposition of MyComposable
            LaunchedEffect(inCh, outCh) {
                inCh.receive()
                outCh.send(currentArg)
            }
        }

        var myComposableArg by mutableStateOf("hello")
        val pleaseSend = Channel<Unit>()
        val output = Channel<String>()

        compose { MyComposable(myComposableArg, pleaseSend, output) }
            .then { myComposableArg = "world" }
            .then {
                val offerSucceeded = pleaseSend.trySend(Unit).isSuccess
                assertTrue(offerSucceeded, "task wasn't awaiting send signal")
            }
            .then {
                val receivedResult = output.tryReceive().getOrNull()
                assertEquals("world", receivedResult)
            }
    }

    /**
     * Always compares as unequal to itself (and everything else) to force DisposableEffect to
     * recompose on every recomposition
     */
    @Suppress("EqualsOrHashCode")
    private object NeverEqualObject {
        override fun equals(other: Any?) = false

        override fun hashCode() = 42
    }
}
