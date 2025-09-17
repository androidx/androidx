/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.runtime.mock.EmptyApplier
import androidx.compose.runtime.mock.Linear
import androidx.compose.runtime.mock.MockViewValidator
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.View
import androidx.compose.runtime.mock.ViewApplier
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.validate
import androidx.compose.runtime.mock.view
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.coroutines.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield

@OptIn(ExperimentalCoroutinesApi::class)
@Stable
class PausableCompositionTests {
    @Test
    fun canCreateARootPausableComposition() = runTest {
        val recomposer = Recomposer(coroutineContext)
        val pausableComposition = PausableComposition(EmptyApplier(), recomposer)
        pausableComposition.dispose()
        recomposer.cancel()
        recomposer.close()
    }

    @Test
    fun canCreateANestedPausableComposition() = compositionTest {
        compose {
            val parent = rememberCompositionContext()
            DisposableEffect(Unit) {
                val pausableComposition = PausableComposition(EmptyApplier(), parent)
                onDispose { pausableComposition.dispose() }
            }
        }
    }

    @Test
    fun canRecordAComposition() = compositionTest {
        // This just tests the recording mechanism used in the tests below.
        val recording = recordTest {
            compose { A() }

            validate { this.A() }
        }

        // Legend for the recording:
        //  +N: Enter N for functions A, B, C, D, (where A:1 is the first lambda in A())
        //  -N: Exit N
        //  *N: Calling N (e.g *B is recorded before B() is called).
        //  ^n: calling remember for some value

        // Here we expect the normal, synchronous, execution as the recorded composition is not
        // pausable. That is if we see a *B that should immediately followed by a B+ its content and
        // a B-.
        assertEquals(
            recording,
            "+A, ^z, ^Y, *B, +B, *Linear, +A:1, *C, +C, ^x, *Text, -C, *D, +D, +D:1, *C, +C, " +
                "^x, *Text, -C, *C, +C, ^x, *Text, -C, *C, +C, ^x, *Text, -C, -D:1, -D, -A:1, " +
                "-B, -A",
        )
    }

    @Test
    fun canPauseContent() = compositionTest {
        val awaiter = Awaiter()
        var receivedIteration = 0
        val recording = recordTest {
            compose {
                PausableContent(
                    normalWorkflow {
                        receivedIteration = iteration
                        awaiter.done()
                    }
                ) {
                    A()
                }
            }
            awaiter.await()
        }
        validate { this.PausableContent { this.A() } }
        assertEquals(9, receivedIteration)

        // Same Legend as canRecordAComposition
        // Here we expect all functions to exit before the content of the function is executed
        // because the above will pause at every pause point. If we see a B* we should not receive
        // a B+ until after the caller finishes. (e.g. A-).
        assertEquals(
            recording,
            "+A, ^z, ^Y, *B, -A, +B, *Linear, -B, +A:1, *C, *D, -A:1, +C, " +
                "^x, *Text, -C, +D, -D, +D:1, *C, *C, *C, -D:1, +C, ^x, *Text, -C, +C, ^x, *Text, " +
                "-C, +C, ^x, *Text, -C",
        )
    }

    @Test
    fun canPauseReusableContent() = compositionTest {
        val awaiter = Awaiter()
        var receivedIteration = 0
        val recording = recordTest {
            compose {
                PausableContent(
                    reuseWorkflow {
                        receivedIteration = iteration
                        awaiter.done()
                    }
                ) {
                    A()
                }
            }
            awaiter.await()
        }
        validate { this.PausableContent { this.A() } }
        assertEquals(9, receivedIteration)
        // Same Legend as canRecordAComposition
        // Here we expect the result to be the same as if we were inserting new content as in
        // canPauseContent
        assertEquals(
            "+A, ^z, ^Y, *B, -A, +B, *Linear, -B, +A:1, *C, *D, -A:1, +C, " +
                "^x, *Text, -C, +D, -D, +D:1, *C, *C, *C, -D:1, +C, ^x, *Text, -C, +C, ^x, *Text, " +
                "-C, +C, ^x, *Text, -C",
            recording,
        )
    }

    @Test
    fun canPauseReusingContent() = compositionTest {
        val awaiter = Awaiter()
        var recording = ""
        val workflow: Workflow = {
            // Create the content
            setContentWithReuse()
            resumeTillComplete { false }
            apply()

            // Reuse the content
            recording = recordTest {
                setContentWithReuse()
                resumeTillComplete { true }
                apply()
            }
            awaiter.done()
        }

        compose { PausableContent(workflow) { A() } }
        awaiter.await()
        // Same Legend as canRecordAComposition
        // Here we expect the result to be the same as if we were inserting new content as in
        // canPauseContent
        assertArrayEquals(
            ("+A, ^z, ^Y, *B, -A, +B, *Linear, -B, +A:1, *C, *D, -A:1, +C, " +
                    "^x, *Text, -C, +D, -D, +D:1, *C, *C, *C, -D:1, +C, ^x, *Text, -C, +C, ^x, *Text, " +
                    "-C, +C, ^x, *Text, -C")
                .splitRecording(),
            recording.splitRecording(),
        )
    }

    @Test
    fun applierOnlyCalledInApply() = compositionTest {
        val awaiter = Awaiter()
        var applier: ViewApplier? = null

        val workflow = workflow {
            setContent()

            assertFalse(applier?.called == true, "Applier was called during set content")

            resumeTillComplete { false }

            assertFalse(applier?.called == true, "Applier was called during resume")

            apply()

            assertTrue(applier?.called == true, "Applier wasn't called")

            awaiter.done()
        }

        compose {
            PausableContent(workflow, { view -> ViewApplier(view).also { applier = it } }) { A() }
        }
        awaiter.await()
    }

    @Test
    fun rememberOnlyCalledInApply() = compositionTest {
        val awaiter = Awaiter()
        var onRememberCalled = false

        val workflow = workflow {
            setContent()
            assertFalse(onRememberCalled, "onRemember called during set content")

            resumeTillComplete {
                assertFalse(onRememberCalled, "onRemember called during resume")
                true
            }
            assertFalse(onRememberCalled, "onRemember called before resume returned")

            apply()

            assertTrue(onRememberCalled, "onRemember was not called in apply")

            awaiter.done()
        }

        fun rememberedObject(name: String) =
            object : RememberObserver {
                val name = name

                override fun onRemembered() {
                    onRememberCalled = true
                    report("+$name")
                }

                override fun onForgotten() {
                    report("-$name")
                }

                override fun onAbandoned() {
                    report("!$name")
                }
            }

        val recording = recordTest {
            compose {
                PausableContent(workflow) {
                    val a = remember { rememberedObject("a") }
                    report("C(${a.name})")
                    B {
                        val b = remember { rememberedObject("b") }
                        report("C(${b.name})")
                        B {
                            val c = remember { rememberedObject("c") }
                            report("C(${c.name})")
                            C()
                            val d = remember { rememberedObject("d") }
                            report("C(${d.name})")
                            D()
                        }
                    }
                }
            }

            awaiter.await()
        }
        // Same Legend as canRecordAComposition except the addition of the C(N) added above and
        // +a, +b, etc. which records when the remembered object are sent the on-remember. This
        // ensures that all onRemember calls are made after the composition has completed.
        assertEquals(
            "C(a), +B, *Linear, -B, C(b), +B, *Linear, -B, C(c), C(d), +C, ^x, *Text, -C, +D, " +
                "-D, +D:1, *C, *C, *C, -D:1, +C, ^x, *Text, -C, +C, ^x, *Text, -C, +C, ^x, *Text, " +
                "-C, +a, +b, +c, +d",
            recording,
        )
    }

    @Suppress("ListIterator")
    @Test
    fun pausable_testRemember_RememberForgetOrder() = compositionTest {
        var order = 0
        val objects = mutableListOf<Any>()
        val newRememberObject = { name: String ->
            object : RememberObserver, Counted, Ordered, Named {
                    override var name = name
                    override var count = 0
                    override var rememberOrder = -1
                    override var forgetOrder = -1

                    override fun onRemembered() {
                        assertEquals(-1, rememberOrder, "Only one call to onRemembered expected")
                        rememberOrder = order++
                        count++
                    }

                    override fun onForgotten() {
                        assertEquals(-1, forgetOrder, "Only one call to onForgotten expected")
                        forgetOrder = order++
                        count--
                    }

                    override fun onAbandoned() {
                        assertEquals(0, count, "onAbandoned called after onRemembered")
                    }
                }
                .also { objects.add(it) }
        }

        @Suppress("UNUSED_PARAMETER") fun used(v: Any) {}

        @Composable
        fun Tree() {
            used(remember { newRememberObject("L0B") })
            Linear {
                used(remember { newRememberObject("L1B") })
                Linear {
                    used(remember { newRememberObject("L2B") })
                    Linear {
                        used(remember { newRememberObject("L3B") })
                        Linear { used(remember { newRememberObject("Leaf") }) }
                        used(remember { newRememberObject("L3A") })
                    }
                    used(remember { newRememberObject("L2A") })
                }
                used(remember { newRememberObject("L1A") })
            }
            used(remember { newRememberObject("L0A") })
        }

        val awaiter = Awaiter()
        val workFlow = normalWorkflow { awaiter.done() }

        compose { PausableContent(workFlow) { Tree() } }
        awaiter.await()

        // Legend:
        //   L<N><B|A>: where N is the nesting level and B is before the children and
        //     A is after the children.
        //   Leaf: the object remembered in the middle.
        // This is asserting that the remember order is the same as it would have been had the
        //   above composition was not paused.
        assertEquals(
            "L0B, L1B, L2B, L3B, Leaf, L3A, L2A, L1A, L0A",
            objects
                .mapNotNull { it as? Ordered }
                .sortedBy { it.rememberOrder }
                .joinToString { (it as Named).name },
            "Expected enter order",
        )
    }

    @Test // b/404058957
    fun pausableComposition_reuseDeactivateOrder_100() = compositionTest {
        val awaiter = Awaiter()
        var active by mutableStateOf(true)
        var text by mutableStateOf("Value")
        val workFlow = workflow {
            setContent()

            resumeTillComplete { true }

            repeat(100) {
                active = false
                advance()

                resumeTillComplete { true }

                active = true
                advance()

                resumeTillComplete { true }
            }

            apply()

            text = "Changed Value"
            advance()

            awaiter.done()
        }

        compose { PausableContent(workFlow) { ReusableContentHost(active) { Text(text) } } }

        awaiter.await()
    }

    @Test // b/404058957
    fun pausableComposition_reuseDeactivateOrder() = compositionTest {
        val awaiter = Awaiter()
        var active by mutableStateOf(true)
        var text by mutableStateOf("Value")
        val workFlow = workflow {
            setContent()

            resumeTillComplete { true }

            active = false
            advance()

            resumeTillComplete { true }

            active = true
            advance()

            resumeTillComplete { true }

            apply()

            text = "Changed Value"
            advance()

            awaiter.done()
        }

        compose { PausableContent(workFlow) { ReusableContentHost(active) { Text(text) } } }

        awaiter.await()
    }

    @Test
    fun pausableComposition_throwInResume() =
        runTest(expected = IllegalStateException::class) {
            val recomposer = Recomposer(coroutineContext)
            val pausableComposition = PausableComposition(EmptyApplier(), recomposer)

            try {
                val handle = pausableComposition.setPausableContent { error("Test error") }
                handle.resume { false }
                handle.apply()
            } finally {
                recomposer.cancel()
                recomposer.close()
            }
        }

    @Test
    fun pausableComposition_throwInApply() =
        runTest(expected = IllegalStateException::class) {
            val recomposer = Recomposer(coroutineContext)
            val pausableComposition = PausableComposition(EmptyApplier(), recomposer)

            try {
                val handle =
                    pausableComposition.setPausableContent {
                        DisposableEffect(Unit) { throw IllegalStateException("test") }
                    }
                handle.resume { false }
                handle.apply()
            } finally {
                recomposer.cancel()
                recomposer.close()
            }
        }

    @Test
    fun pausableComposition_throwIfReusedAfterCancel() =
        runTest(expected = IllegalStateException::class) {
            val recomposer = Recomposer(coroutineContext)
            val pausableComposition = PausableComposition(EmptyApplier(), recomposer)

            try {
                val handle = pausableComposition.setPausableContent { Text("Some text") }
                handle.cancel()
                val handle2 = pausableComposition.setPausableContent { Text("Some other text") }
                handle2.resume { false }
                handle2.apply()
            } finally {
                pausableComposition.dispose()
                recomposer.cancel()
                recomposer.close()
            }
        }

    @Test
    fun pausableComposition_isAppliedReturnsCorrectValue() = runTest {
        val recomposer = Recomposer(coroutineContext)
        val pausableComposition = PausableComposition(EmptyApplier(), recomposer)

        try {
            val handle =
                pausableComposition.setPausableContent { DisposableEffect(Unit) { onDispose {} } }
            assertFalse(handle.isApplied)
            handle.resume { false }
            assertFalse(handle.isApplied)
            handle.apply()
            assertTrue(handle.isApplied)
        } finally {
            recomposer.cancel()
            recomposer.close()
        }
    }

    @Test
    fun pausableComposition_isCancelledReturnsCorrectValue() = runTest {
        val recomposer = Recomposer(coroutineContext)
        val pausableComposition = PausableComposition(EmptyApplier(), recomposer)

        try {
            val handle =
                pausableComposition.setPausableContent { DisposableEffect(Unit) { onDispose {} } }
            assertFalse(handle.isCancelled)
            handle.resume { false }
            assertFalse(handle.isCancelled)
            handle.cancel()
            assertTrue(handle.isCancelled)
        } finally {
            recomposer.cancel()
            recomposer.close()
        }
    }

    @Test
    fun pausableComposition_diagnosticExceptionInApply() = compositionTest {
        val awaiter = Awaiter()

        var applyException: Exception? = null
        val w = workflow {
            setContent()
            resumeTillComplete { false }

            try {
                apply()
            } catch (e: Exception) {
                applyException = e
            }
            awaiter.done()
        }

        compose {
            PausableContent(w) {
                ComposeNode<View, ViewApplier>(
                    factory = { View().also { it.name = "Crash" } },
                    update = { init { error("Test") } },
                    content = {},
                )
            }
        }

        awaiter.await()
        assertEquals("ComposePausableCompositionException", applyException!!::class.simpleName)
    }

    @Test // b/424797313
    fun rememberObserverCount() = compositionTest {
        val awaiter = Awaiter()
        val workFlow = workflow {
            setContent()

            resumeTillComplete { false }

            cancel()

            composition.dispose()

            awaiter.done()
        }

        val rememberObserver =
            object : RememberObserver {
                var rememberCount = 0
                var forgottenCount = 0
                var abandonedCount = 0

                override fun onRemembered() {
                    rememberCount++
                }

                override fun onForgotten() {
                    forgottenCount++
                }

                override fun onAbandoned() {
                    abandonedCount++
                }
            }

        compose { PausableContent(workFlow) { remember<RememberObserver> { rememberObserver } } }

        awaiter.await()

        assertEquals(0, rememberObserver.rememberCount)
        assertEquals(0, rememberObserver.forgottenCount)
        assertEquals(1, rememberObserver.abandonedCount)
    }

    @Test
    fun deactivateAnotherComposition() = compositionTest {
        val awaiter = Awaiter()
        var state by mutableStateOf(false)
        var composition: ReusableComposition? = null

        val workflow = workflow {
            composition = this.composition

            setContent()
            resumeTillComplete { false }
            apply()

            state = true
            Snapshot.sendApplyNotifications()
            advance()

            setContent()
            resumeTillComplete { false }
            apply()

            awaiter.done()
        }

        compose {
            Text("$state")
            SideEffect {
                if (state) {
                    composition?.deactivate()
                }
            }

            PausableContent(workflow) { Text("$state") }
        }

        awaiter.await()
    }

    @Test
    fun markInvalidFromBackgroundThread() = compositionTest {
        val awaiter = Awaiter()
        val workflow = workflow {
            setContent()
            resumeTillComplete { false }

            repeat(1000) {
                val job = launch(Dispatchers.Default) { repeat(10) { launch { invalidate() } } }
                job.join()

                resumeTillComplete { false }
            }
            apply()
            awaiter.done()
        }

        compose { PausableContent(workflow) { Text("Some composable") } }

        awaiter.await()
    }

    @Test
    fun tryPausingTheSameScopeTwice() = compositionTest {
        val awaiter = Awaiter()
        var textComposed = false
        var text by mutableStateOf("blah")
        val workflow = workflow {
            setContent()
            resumeTillComplete { false }
            apply()
            composition.deactivate()

            setContent()
            resumeOnce { textComposed }

            text = "text"
            advance()

            resumeOnce { true }

            resumeTillComplete { false }
            apply()

            awaiter.done()
        }

        compose {
            PausableContent(workflow) {
                textComposed = true
                DefaultText(text)
            }
        }

        awaiter.await()
    }

    @Test
    fun resumeOnBackgroundThread() = compositionTest {
        val awaiter = Awaiter()
        var text by mutableStateOf("Some text")
        var running by mutableStateOf(false)
        var counter = 0
        val mutatorJob =
            launch(Dispatchers.Default) {
                while (running) {
                    text = "Some text $counter"
                    counter++
                    yield()
                }
            }

        val workflow = workflow {
            setContent()

            val resumeJob =
                launch(Dispatchers.Default) {
                    while (!isComplete) {
                        resumeOnce { true }
                    }
                }

            resumeJob.join()
            resumeTillComplete { false }
            apply()
            awaiter.done()
        }

        compose {
            W { Text(text) }

            PausableContent(workflow) { W { repeat(1000) { W { Text(text) } } } }
            W { Text(text) }
        }

        repeat(100) {
            advance(ignorePendingWork = true)
            delay(1)
        }

        running = false
        mutatorJob.join()
        awaiter.await()
    }
}

fun String.splitRecording() = split(", ")

typealias Workflow = suspend PausableContentWorkflowScope.() -> Unit

fun workflow(workflow: Workflow): Workflow = workflow

fun reuseWorkflow(done: Workflow = {}) = workflow {
    setContentWithReuse()
    resumeTillComplete { true }
    apply()
    done()
}

fun normalWorkflow(done: Workflow = {}) = workflow {
    setContent()
    resumeTillComplete { true }
    apply()
    done()
}

private interface TestRecorder {
    fun log(message: String)

    fun logs(): String

    fun clear()
}

private var recorder: TestRecorder =
    object : TestRecorder {
        override fun log(message: String) {}

        override fun logs(): String = ""

        override fun clear() {}
    }

private inline fun recordTest(block: () -> Unit): String {
    val result = mutableListOf<String>()
    val oldRecorder = recorder
    recorder =
        object : TestRecorder {
            override fun log(message: String) {
                result.add(message)
            }

            override fun logs() = result.joinToString()

            override fun clear() {
                result.clear()
            }
        }
    block()
    recorder = oldRecorder
    return result.joinToString()
}

private fun report(message: String) {
    recorder.log(message)
}

private inline fun report(message: String, block: () -> Unit) {
    report("+$message")
    block()
    report("-$message")
}

@Composable
private fun A() {
    report("A") {
        report("^z")
        val z = remember { 0 }
        report("^Y")
        val y = remember { 1 }
        Text("A: $z $y")
        report("*B")
        B {
            report("A:1") {
                report("*C")
                C()
                report("*D")
                D()
            }
        }
    }
}

private fun MockViewValidator.PausableContent(content: MockViewValidator.() -> Unit) {
    this.view("PausableContentHost") { this.view("PausableContent", content) }
}

private fun MockViewValidator.A() {
    Text("A: 0 1")
    this.B {
        this.C()
        this.D()
    }
}

@Composable
private fun B(content: @Composable () -> Unit) {
    report("B") {
        report("*Linear")
        Linear(content)
    }
}

private fun MockViewValidator.B(content: MockViewValidator.() -> Unit) {
    this.Linear(content)
}

@Composable
private fun C() {
    report("C") {
        report("^x")
        val x = remember { 3 }
        report("*Text")
        Text("C: $x")
    }
}

private fun MockViewValidator.C() {
    this.Text("C: 3")
}

@Composable
private fun D() {
    report("D") {
        Linear {
            report("D:1") {
                repeat(3) {
                    report("*C")
                    C()
                }
            }
        }
    }
}

@Composable
private fun W(content: @Composable () -> Unit) {
    Linear(content)
}

private fun MockViewValidator.D() {
    this.Linear { repeat(3) { this.C() } }
}

interface PausableContentWorkflowScope {
    val iteration: Int
    val isComplete: Boolean
    val applied: Boolean
    val composition: PausableComposition

    fun setContent(): PausedComposition

    fun setContentWithReuse(): PausedComposition

    fun resumeTillComplete(shouldPause: () -> Boolean)

    fun resumeOnce(shouldPause: () -> Boolean)

    fun apply()

    fun cancel()

    fun invalidate()
}

fun PausableContentWorkflowScope.run(shouldPause: () -> Boolean = { true }) {
    setContent()
    resumeTillComplete(shouldPause)
    apply()
}

class PausableContentWorkflowDriver(
    override val composition: PausableComposition,
    private val content: @Composable () -> Unit,
    private var host: View?,
    private var contentView: View?,
) : PausableContentWorkflowScope {
    private var pausedComposition: PausedComposition? = null
    override var iteration = 0
    override val applied: Boolean
        get() = host == null && pausedComposition == null

    override val isComplete: Boolean
        get() = host == null || pausedComposition?.isComplete == true

    override fun setContent(): PausedComposition {
        checkPrecondition(pausedComposition == null)
        return composition.setPausableContent(content).also { pausedComposition = it }
    }

    override fun setContentWithReuse(): PausedComposition {
        checkPrecondition(pausedComposition == null)
        return composition.setPausableContentWithReuse(content).also { pausedComposition = it }
    }

    override fun resumeTillComplete(shouldPause: () -> Boolean) {
        val pausedComposition = pausedComposition
        checkPrecondition(pausedComposition != null)
        while (!pausedComposition.isComplete) {
            pausedComposition.resume(shouldPause)
            iteration++
        }
    }

    override fun resumeOnce(shouldPause: () -> Boolean) {
        val pausedComposition = pausedComposition
        checkPrecondition(pausedComposition != null)
        pausedComposition.resume(shouldPause)
    }

    override fun apply() {
        val pausedComposition = pausedComposition
        checkPrecondition(pausedComposition != null && pausedComposition.isComplete)
        pausedComposition.apply()
        this.pausedComposition = null
        val host = host
        val contentView = contentView
        if (host != null && contentView != null) {
            host.children.add(contentView)
            this.host = null
            this.contentView = null
        }
    }

    override fun cancel() {
        val pausedComposition = pausedComposition
        checkPrecondition(pausedComposition != null)
        pausedComposition.cancel()
    }

    override fun invalidate() {
        (pausedComposition as? PausedCompositionImpl)?.markIncomplete()
    }
}

@Composable
private fun PausableContent(
    workflow: suspend PausableContentWorkflowScope.() -> Unit = { run() },
    createApplier: (view: View) -> Applier<View> = { ViewApplier(it) },
    content: @Composable () -> Unit,
) {
    val host = View().also { it.name = "PausableContentHost" }
    val pausableContent = View().also { it.name = "PausableContent" }
    ComposeNode<View, ViewApplier>(factory = { host }, update = {})
    val parent = rememberCompositionContext()
    val composition =
        remember(parent) { PausableComposition(createApplier(pausableContent), parent) }
    LaunchedEffect(content as Any) {
        val scope = PausableContentWorkflowDriver(composition, content, host, pausableContent)
        scope.workflow()
    }
    DisposableEffect(Unit) { onDispose { composition.dispose() } }
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

val LocalColor = compositionLocalOf { -1 }

@Composable
fun DefaultText(
    text: String,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    color: Int = LocalColor.current,
) {
    assertEquals(1, minLines)
    assertEquals(Int.MAX_VALUE, maxLines)
    assertEquals(-1, color)
    Text(text)
}
