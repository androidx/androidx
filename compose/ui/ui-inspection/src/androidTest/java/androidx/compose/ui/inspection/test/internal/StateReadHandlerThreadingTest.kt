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

package androidx.compose.ui.inspection.test.internal

import android.annotation.SuppressLint
import androidx.annotation.GuardedBy
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.IntState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.IdentifiableRecomposeScope
import androidx.compose.ui.inspection.inspector.InlineClassConverter
import androidx.compose.ui.inspection.recompositions.StateReadHandler
import androidx.compose.ui.inspection.test.rules.JvmtiRule
import androidx.compose.ui.inspection.test.util.UnitApplier
import androidx.compose.ui.inspection.util.AnchorMap
import androidx.inspection.DefaultArtTooling
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StateReadSettings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private const val RECOMPOSITIONS = 40

/**
 * Parameter changes must be recorded correctly when compositions run on background threads, when a
 * sub-composition is composed during the composition of its parent, and when multiple recomposers
 * compose concurrently.
 *
 * The CompositionObserver callbacks of a composition are made on the thread doing the composition.
 * A composition may run on a background thread, a composition may create a sub-composition during
 * its own composition, and multiple recomposers may each compose on their own thread.
 *
 * Each scenario runs with unknown and with known keys. Studio registers the keys of the composables
 * it has received (GetComposablesCommand), and the parameter changes of composables with a known
 * key and with an unknown key are found differently.
 */
@LargeTest
@OptIn(ComposeToolingApi::class)
class StateReadHandlerThreadingTest {
    @get:Rule val jvmti = JvmtiRule()

    private val anchorMap = AnchorMap()
    private val recorder = CompositionRecorder(anchorMap)
    private lateinit var handler: StateReadHandler

    @SuppressLint("RestrictedApi")
    @Before
    fun before() {
        handler =
            StateReadHandler(DefaultArtTooling(INSPECTOR_ID), anchorMap, InlineClassConverter())
        observeAll(includeParameterChanges = true)
    }

    /**
     * Observe state reads of all composables, and reset the recomposition counts when
     * [keepRecomposeCounts] is false.
     */
    private fun observeAll(
        includeParameterChanges: Boolean,
        keepRecomposeCounts: Boolean = false,
    ) {
        val settings =
            StateReadSettings.newBuilder()
                .setAll(
                    StateReadSettings.All.newBuilder()
                        .setIncludeParameterChanges(includeParameterChanges)
                )
                .build()
        handler.changeCollectionMode(
            includeRecomposeCounts = true,
            keepRecomposeCounts = keepRecomposeCounts,
            settings = settings,
        )
    }

    /** Observe state reads and parameter changes of the composables with [anchorHashes]. */
    private fun observeById(vararg anchorHashes: Int, keepRecomposeCounts: Boolean = true) {
        val settings =
            StateReadSettings.newBuilder()
                .setById(
                    StateReadSettings.ById.newBuilder()
                        .addAllComposableToObserve(anchorHashes.asList())
                        .setIncludeParameterChanges(true)
                )
                .build()
        handler.changeCollectionMode(
            includeRecomposeCounts = true,
            keepRecomposeCounts = keepRecomposeCounts,
            settings = settings,
        )
    }

    @After
    fun after() {
        handler.dispose()
    }

    @Test fun compositionOnBackgroundThread() = compositionOnBackgroundThread(knownKeys = false)

    @Test
    fun compositionOnBackgroundThreadWithKnownKeys() =
        compositionOnBackgroundThread(knownKeys = true)

    @Test
    fun subCompositionComposedDuringParentComposition() =
        subCompositionComposedDuringParentComposition(knownKeys = false)

    @Test
    fun subCompositionComposedDuringParentCompositionWithKnownKeys() =
        subCompositionComposedDuringParentComposition(knownKeys = true)

    @Test
    fun recomposersComposingConcurrently() = recomposersComposingConcurrently(knownKeys = false)

    @Test
    fun recomposersComposingConcurrentlyWithKnownKeys() =
        recomposersComposingConcurrently(knownKeys = true)

    /**
     * Parameter changes are only tracked while they are included, and can be turned on and off
     * while observing state reads.
     */
    @Test
    fun parameterChangesTurnedOnWhileObserving(): Unit = runBlocking {
        observeAll(includeParameterChanges = false)
        val count = mutableIntStateOf(0)
        BackgroundComposition { Item("item", count.intValue, recorder) }
            .use { composition ->
                composition.recompose(count) { recorder.lastValue("item") }
                val anchorHash = recorder.anchorHash("item")
                val reads = handler.getReadsAndRemove(anchorHash, 1, RECOMPOSITIONS, true)
                assertWithMessage("Parameter changes while turned off: $reads")
                    .that(reads.flatMap { it.parameterChanges })
                    .isEmpty()

                observeAll(includeParameterChanges = true, keepRecomposeCounts = true)
                composition.recompose(count) { recorder.lastValue("item") }
                val reads2 =
                    handler.getReadsAndRemove(
                        anchorHash,
                        RECOMPOSITIONS + 1,
                        2 * RECOMPOSITIONS,
                        true,
                    )
                val changes = reads2.associate { read ->
                    read.recomposition to read.parameterChanges.map { it.name to it.value }
                }
                for (recomposition in (RECOMPOSITIONS + 1)..(2 * RECOMPOSITIONS)) {
                    assertWithMessage("Recomposition $recomposition: $changes")
                        .that(changes[recomposition])
                        .isEqualTo(listOf("value" to recomposition))
                }
            }
    }

    /**
     * Turning parameter changes off and back on during a single composition pass must keep the
     * scope stack balanced, so parameter changes after it are still attributed to the right scope.
     */
    @Test
    fun parameterChangesToggledDuringCompositionPass(): Unit = runBlocking {
        val count = mutableIntStateOf(0)
        val toggleOff = { value: Int ->
            if (value >= 1) {
                observeAll(includeParameterChanges = false, keepRecomposeCounts = true)
            }
        }
        val toggleOn = { value: Int ->
            if (value >= 1) {
                observeAll(includeParameterChanges = true, keepRecomposeCounts = true)
            }
        }
        BackgroundComposition {
            Before(count.intValue, toggleOff) {
                Before(count.intValue, toggleOn) { Item("toggled", count.intValue, recorder) }
                Item("after", count.intValue, recorder)
            }
        }
            .use { composition ->
                recorder.registerKeys("toggled", "after")
                composition.recompose(count, recompositions = 5) { recorder.lastValue("after") }
                val afterHash = recorder.anchorHash("after")
                val reads = handler.getReadsAndRemove(afterHash, 1, 5, true)
                val changes = reads.associate { read ->
                    read.recomposition to read.parameterChanges.map { it.name to it.value }
                }
                for (recomposition in 1..5) {
                    assertWithMessage("Recomposition $recomposition of after: $changes")
                        .that(changes[recomposition])
                        .isEqualTo(listOf("value" to recomposition))
                }
            }
    }

    /**
     * When observing by ID, only the observed composables record state reads and parameter changes,
     * and a composable removed from the observed set during a composition pass must not record
     * anything for that pass.
     */
    @Test
    fun observeByIdChangedDuringCompositionPass(): Unit = runBlocking {
        val count = mutableIntStateOf(0)
        val other = mutableIntStateOf(0)
        var hash2 = 0
        val observeOnlySecond = { value: Int ->
            if (value == 2) {
                observeById(hash2)
            }
        }
        BackgroundComposition {
            ReadingItem("item1", count.intValue, other, recorder)
            Before(count.intValue, observeOnlySecond) {
                ReadingItem("item2", count.intValue, other, recorder)
                ReadingItem("unobserved", count.intValue, other, recorder)
            }
        }
            .use { composition ->
                val hash1 = recorder.anchorHash("item1")
                hash2 = recorder.anchorHash("item2")
                val hashUnobserved = recorder.anchorHash("unobserved")
                observeById(hash1, hash2, keepRecomposeCounts = false)
                composition.recompose(count, recompositions = 1) {
                    recorder.lastValue("unobserved")
                }

                val reads1 = handler.getReadsAndRemove(hash1, 1, 1, true)
                assertWithMessage("item1 recomposition 1: $reads1")
                    .that(
                        reads1.map { it.recomposition to it.parameterChanges.map { p -> p.value } }
                    )
                    .containsExactly(1 to listOf(1))

                // In recomposition 2, item1 is composed while observed, then removed from the
                // observed set before onEndComposition, so its parameter changes must not be
                // recorded.
                composition.recompose(count, recompositions = 1) {
                    recorder.lastValue("unobserved")
                }
                val reads1After = handler.getReadsAndRemove(hash1, 1, 2, true)
                assertWithMessage("item1 recomposition 2 after removal from BY_ID: $reads1After")
                    .that(reads1After)
                    .isEmpty()
                val reads2 = handler.getReadsAndRemove(hash2, 1, 2, true)
                assertWithMessage("item2 reads: $reads2")
                    .that(
                        reads2.map { it.recomposition to it.parameterChanges.map { p -> p.value } }
                    )
                    .containsExactly(1 to listOf(1), 2 to listOf(2))
                assertWithMessage("unobserved reads")
                    .that(handler.getReadsAndRemove(hashUnobserved, 1, 2, true))
                    .isEmpty()

                // Observing by ID with an unknown anchor ID must stop observing instead of falling
                // back to observing all composables (b/571066385):
                observeById(-1)
                composition.recompose(count, recompositions = 1) {
                    recorder.lastValue("unobserved")
                }
                assertWithMessage("item2 reads when BY_ID has only unknown IDs")
                    .that(handler.getReadsAndRemove(hash2, 1, 3, true))
                    .isEmpty()
            }
    }

    /**
     * A composition pass that is still running when observation stops must not record anything.
     *
     * The runtime keeps calling the observer it resolved at the start of a composition pass, even
     * after the observer is removed. Observation is stopped here from inside a composition pass,
     * before a composable reads a state.
     */
    @Test
    fun observationStoppedDuringCompositionPass(): Unit = runBlocking {
        val count = mutableIntStateOf(0)
        val other = mutableIntStateOf(0)
        val stopObserving = { value: Int ->
            if (value == 1) {
                handler.changeCollectionMode(
                    includeRecomposeCounts = true,
                    keepRecomposeCounts = true,
                    settings = StateReadSettings.getDefaultInstance(),
                )
            }
        }
        BackgroundComposition {
            Before(count.intValue, stopObserving) {
                ReadingItem("item", count.intValue, other, recorder)
            }
        }
            .use { composition ->
                recorder.registerKeys("item")
                composition.recompose(count, recompositions = 1) { recorder.lastValue("item") }
                val anchorHash = recorder.anchorHash("item")
                val reads = handler.getReadsAndRemove(anchorHash, 1, RECOMPOSITIONS, true)
                assertWithMessage("Recorded after observation stopped: $reads")
                    .that(reads.flatMap { it.reads + it.parameterChanges })
                    .isEmpty()
            }
    }

    private fun compositionOnBackgroundThread(knownKeys: Boolean): Unit = runBlocking {
        val count = mutableIntStateOf(0)
        BackgroundComposition { Item("item", count.intValue, recorder) }
            .use { composition ->
                if (knownKeys) recorder.registerKeys("item")
                composition.recompose(count) { recorder.lastValue("item") }
                assertParameterChanges("item") { recomposition -> recomposition }
            }
    }

    private fun subCompositionComposedDuringParentComposition(knownKeys: Boolean): Unit =
        runBlocking {
            val count = mutableIntStateOf(0)
            BackgroundComposition { Outer(count.intValue, recorder) }
                .use { composition ->
                    if (knownKeys) recorder.registerKeys("outer", "inner")
                    composition.recompose(count) { recorder.lastValue("inner") }
                    assertParameterChanges("outer") { recomposition -> recomposition }
                    assertParameterChanges("inner") { recomposition -> recomposition }
                }
        }

    private fun recomposersComposingConcurrently(knownKeys: Boolean): Unit = runBlocking {
        val count1 = mutableIntStateOf(0)
        val count2 = mutableIntStateOf(1000)
        BackgroundComposition { Item("item1", count1.intValue, recorder) }
            .use { composition1 ->
                BackgroundComposition { Item("item2", count2.intValue, recorder) }
                    .use { composition2 ->
                        if (knownKeys) recorder.registerKeys("item1", "item2")
                        // A failure in this coroutine fails the test, instead of crashing the
                        // instrumentation process as an uncaught exception on a thread would.
                        val recomposer2 =
                            async(Dispatchers.IO) {
                                composition2.recompose(count2) { recorder.lastValue("item2") }
                            }
                        composition1.recompose(count1) { recorder.lastValue("item1") }
                        recomposer2.await()
                        assertParameterChanges("item1") { recomposition -> recomposition }
                        assertParameterChanges("item2") { recomposition -> 1000 + recomposition }
                    }
            }
    }

    /**
     * Check that the parameter changes for the composable [name] are the expected value for each
     * recomposition. This must be called before the composition is disposed.
     */
    private fun assertParameterChanges(name: String, expectedValue: (recomposition: Int) -> Int) {
        val anchorHash = recorder.anchorHash(name)
        val count = handler.getCounts(anchorHash)?.count
        val reads = handler.getReadsAndRemove(anchorHash, 1, RECOMPOSITIONS, true)
        val changes = reads.associate { read ->
            read.recomposition to read.parameterChanges.map { it.name to it.value }
        }
        for (recomposition in 1..RECOMPOSITIONS) {
            assertWithMessage("Recomposition $recomposition of $name (count: $count): $changes")
                .that(changes[recomposition])
                .isEqualTo(listOf("value" to expectedValue(recomposition)))
        }
    }

    /** A composition on a background thread. */
    private class BackgroundComposition(content: @Composable () -> Unit) : AutoCloseable {
        private val executor = Executors.newSingleThreadExecutor()
        private val dispatcher = executor.asCoroutineDispatcher()
        private val clock = BroadcastFrameClock()
        private val job = Job()
        private val scope = CoroutineScope(dispatcher + clock + job)
        private val recomposer = Recomposer(scope.coroutineContext)
        private val composition = Composition(UnitApplier(), recomposer)

        init {
            scope.launch { recomposer.runRecomposeAndApplyChanges() }
            runBlocking {
                // Wait until the inspector observes the running recomposer on the main thread.
                // A composition that is being composed while the inspector starts observing its
                // recomposer is not reported to the inspector.
                withTimeout(5_000) { recomposer.currentState.first { it == Recomposer.State.Idle } }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                withContext(dispatcher) {
                    composition.setContent {
                        // Parameter values are only available with parameter information:
                        currentComposer.collectParameterInformation()
                        content()
                    }
                }
            }
            // Wait until the inspector sets the observer on the composition on the main thread:
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        }

        /**
         * Change the [count] [RECOMPOSITIONS] times and wait for each recomposition, until
         * [composedValue] returns the new value.
         */
        suspend fun recompose(
            count: MutableIntState,
            recompositions: Int = RECOMPOSITIONS,
            composedValue: () -> Int?,
        ) {
            val start = count.intValue
            for (index in 1..recompositions) {
                val expected = start + index
                Snapshot.withMutableSnapshot { count.intValue = expected }
                withTimeout(5_000) {
                    while (composedValue() != expected) {
                        clock.sendFrame(System.nanoTime())
                        delay(1)
                    }
                }
            }
        }

        override fun close() {
            runBlocking {
                withContext(dispatcher) {
                    composition.dispose()
                    recomposer.cancel()
                }
            }
            job.cancel()
            scope.cancel()
            executor.shutdown()
        }
    }
}

/**
 * Thread-safe recorder of the anchor, [CompositionData], and last value composed for test
 * composables by name.
 */
@OptIn(ComposeToolingApi::class)
internal class CompositionRecorder(private val anchorMap: AnchorMap) {
    private val lock = Any()

    @GuardedBy("lock") private val anchors = mutableMapOf<String, Any>()
    @GuardedBy("lock") private val compositionData = mutableMapOf<String, CompositionData>()
    @GuardedBy("lock") private val lastValues = mutableMapOf<String, Int>()

    fun record(name: String, value: Int, data: CompositionData, anchor: Any?) {
        synchronized(lock) {
            compositionData[name] = data
            anchor?.let { anchors[name] = it }
            lastValues[name] = value
        }
    }

    fun lastValue(name: String): Int? = synchronized(lock) { lastValues[name] }

    /** Return the anchor hash for [name], registering its group key in [anchorMap]. */
    fun anchorHash(name: String): Int = anchorMap[anchor(name), key(name)]

    /**
     * Register the keys of the composables with [names], like Studio does for known composables.
     */
    fun registerKeys(vararg names: String) {
        for (name in names) {
            anchorHash(name)
        }
    }

    private fun anchor(name: String): Any = synchronized(lock) { anchors.getValue(name) }

    private fun key(name: String): Int {
        val data = synchronized(lock) { compositionData.getValue(name) }
        return data.find(anchor(name))?.key as Int
    }
}

/** A composable with a parameter, that records its anchor after it is composed. */
@OptIn(ComposeToolingApi::class)
@Composable
internal fun Item(name: String, value: Int, recorder: CompositionRecorder) {
    val data = currentComposer.compositionData
    val scope = currentRecomposeScope
    SideEffect {
        recorder.record(name, value, data, (scope as IdentifiableRecomposeScope).identity)
    }
    Value(value)
}

/** A composable that composes a sub-composition during its own composition. */
@Composable
internal fun Outer(value: Int, recorder: CompositionRecorder) {
    Item("outer", value, recorder)
    val context = rememberCompositionContext()
    val subComposition = remember { Composition(UnitApplier(), context) }
    // This composes the sub-composition now, while this composition is being composed:
    subComposition.setContent { Item("inner", value, recorder) }
    DisposableEffect(subComposition) { onDispose { subComposition.dispose() } }
}

@Composable internal fun Value(@Suppress("UNUSED_PARAMETER") value: Int) {}

internal const val INSPECTOR_ID = "layoutinspector.compose.inspection"

/**
 * A composable that calls [beforeContent] with [value] during its composition, before composing
 * [content].
 */
@Composable
private fun Before(value: Int, beforeContent: (Int) -> Unit, content: @Composable () -> Unit) {
    beforeContent(value)
    content()
}

/** Like [Item], but also reads [state] in its own scope. */
@OptIn(ComposeToolingApi::class)
@Composable
private fun ReadingItem(name: String, value: Int, state: IntState, recorder: CompositionRecorder) {
    val read = state.intValue
    val data = currentComposer.compositionData
    val scope = currentRecomposeScope
    SideEffect {
        recorder.record(name, value, data, (scope as IdentifiableRecomposeScope).identity)
    }
    Value(value + read)
}
