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

@file:OptIn(ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import android.os.Looper
import androidx.collection.buildIntSet
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric test of [RecordingCanvas]. */
@OptIn(ExperimentalCoroutinesApi::class)
@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
@Repeat(5)
class CaptureRemoteDocumentTest {
    @get:Rule val repeatRule = RepeatRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
    }

    @Test
    fun captureDocument() =
        runTest(UnconfinedTestDispatcher()) {
            val document: ByteArray =
                captureSingleRemoteDocument(context) {
                        RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Red.rc))
                    }
                    .bytes

            val remoteComposeDocument = document.toCoreDocument()

            assertTrue(remoteComposeDocument.docInfo.mNumberOfOps > 0)
        }

    @Test
    fun captureDocumentWithCustomProfile() =
        runTest(UnconfinedTestDispatcher()) {
            val defaultOps =
                Operations.getOperations(
                        CoreDocument.DOCUMENT_API_LEVEL,
                        RcProfiles.PROFILE_ANDROIDX,
                    )
                    ?.keySet()
                    .orEmpty()
            val customOps = buildIntSet {
                defaultOps.forEach { add(it) }
                add(Operations.DRAW_TEXT_ON_CIRCLE)
            }
            val customProfile =
                createProfile(
                    profileFlags = RcProfiles.PROFILE_ANDROID_NATIVE,
                    supportedOperations = customOps,
                )
            val document: ByteArray =
                captureSingleRemoteDocument(context, profile = customProfile) {
                        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                            val redPaint = RemotePaint { color = Color.Red.rc }
                            drawRect(paint = redPaint)
                            val bluePaint = RemotePaint { color = Color.Blue.rc }
                            drawCircle(
                                paint = bluePaint,
                                center = RemoteOffset(width / 2f, height / 2f),
                                radius = width / 4f,
                            )
                            val textPaint = RemotePaint {
                                isAntiAlias = true
                                color = Color.LightGray.rc
                                textSize = 12f.rf
                            }

                            drawTextOnCircle(
                                text = "10:09".rs,
                                centerX = width / 2f,
                                centerY = height / 2f,
                                radius = width / 2f,
                                startAngle = 0f.rf,
                                warpRadiusOffset = 0f.rf,
                                paint = textPaint,
                            )
                        }
                    }
                    .bytes

            assertTrue(document.isNotEmpty())
        }

    @Test
    fun captureDocument_withHostDensity() =
        runTest(UnconfinedTestDispatcher()) {
            var capturedDensity: RemoteDensity? = null
            captureSingleRemoteDocument(context = context, remoteDensity = RemoteDensity.Host) {
                capturedDensity = LocalRemoteComposeCreationState.current.remoteDensity
                RemoteBox(modifier = RemoteModifier.fillMaxSize())
            }

            assertNotNull(capturedDensity)
            // Assert that it is not a constant
            assertNull(capturedDensity?.density?.constantValueOrNull)
        }

    @Test
    fun constantCacheKey_doesNotRetainRemoteStateInstance() {
        var weakRef: WeakReference<RemoteString>? = null
        val creationState =
            RemoteComposeCreationState(
                creationDisplayInfo = createCreationDisplayInfo(context),
                profile = RcPlatformProfiles.ANDROIDX,
                writerEvents = null,
                layoutDirection = LayoutDirection.Ltr,
            )

        fun createAndRegister() {
            val str = RemoteString("TestConstant")
            weakRef = WeakReference(str)
            str.getIdForCreationState(creationState)
        }

        createAndRegister()
        System.gc()

        assertNull(weakRef?.get())
    }

    /**
     * Case 1: Calling from a raw background thread (e.g. an IPC Binder thread without a Looper) and
     * without any [ContinuationInterceptor] in the coroutine context (`interceptor == null`).
     * Verifies that capture falls back to a single-threaded default dispatcher, provides a
     * thread-safe [LocalLifecycleOwner], runs effects/frames to completion, and encodes the updated
     * UI state into the resulting [CoreDocument].
     */
    @Test
    fun captureSingleRemoteDocument_onRawBinderThreadWithoutInterceptor_capturesLifecycleAndState() {
        val lifecycleEvents = CopyOnWriteArrayList<Lifecycle.Event>()
        val defaultObserverResumed = AtomicBoolean(false)
        val defaultObserverDestroyed = AtomicBoolean(false)
        val capturedInterceptor = AtomicReference<ContinuationInterceptor?>(null)
        val capturedLifecycle = AtomicReference<Lifecycle?>(null)
        val capturedDocument = AtomicReference<CapturedDocument?>(null)
        val failure = AtomicReference<Throwable?>(null)
        val latch = CountDownLatch(1)

        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "test-binder-thread")
        }
        try {
            val future = executor.submit {
                // Confirm we are on a raw background thread with no Looper attached.
                assertNull(Looper.myLooper())

                val suspendBlock: suspend () -> Unit = {
                    capturedDocument.set(
                        captureSingleRemoteDocument(context) {
                            val lifecycle = LocalLifecycleOwner.current.lifecycle
                            val lifecycleState by lifecycle.currentStateAsState()
                            capturedLifecycle.set(lifecycle)
                            var label by remember { mutableStateOf("Initial") }

                            LaunchedEffect(lifecycle) {
                                capturedInterceptor.set(
                                    currentCoroutineContext()[ContinuationInterceptor]
                                )
                                lifecycle.addObserver(
                                    object : DefaultLifecycleObserver {
                                        override fun onResume(owner: LifecycleOwner) {
                                            defaultObserverResumed.set(true)
                                        }

                                        override fun onDestroy(owner: LifecycleOwner) {
                                            defaultObserverDestroyed.set(true)
                                        }
                                    }
                                )
                                lifecycle.addObserver(
                                    LifecycleEventObserver { _, event ->
                                        lifecycleEvents.add(event)
                                    }
                                )
                                withFrameNanos { label = "Binder:${lifecycleState.name}" }
                            }

                            RemoteBox(
                                modifier = RemoteModifier.fillMaxSize().background(Color.Red.rc)
                            ) {
                                RemoteText(label.rs)
                            }
                        }
                    )
                }

                suspendBlock.startCoroutine(
                    object : Continuation<Unit> {
                        override val context = EmptyCoroutineContext

                        override fun resumeWith(result: Result<Unit>) {
                            failure.set(result.exceptionOrNull())
                            latch.countDown()
                        }
                    }
                )
            }

            future.get(10, TimeUnit.SECONDS)
            assertTrue(latch.await(10, TimeUnit.SECONDS))
            assertNull("Expected capture on raw thread to succeed without error", failure.get())

            assertNotNull(capturedInterceptor.get())
            assertTrue(capturedInterceptor.get() is CoroutineDispatcher)
            assertTrue(defaultObserverResumed.get())
            assertTrue(defaultObserverDestroyed.get())
            assertEquals(
                listOf(
                    Lifecycle.Event.ON_CREATE,
                    Lifecycle.Event.ON_START,
                    Lifecycle.Event.ON_RESUME,
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP,
                    Lifecycle.Event.ON_DESTROY,
                ),
                lifecycleEvents.toList(),
            )
            assertEquals(Lifecycle.State.DESTROYED, capturedLifecycle.get()?.currentState)

            // Adding an observer after destroy() must not dispatch any events.
            val lateEvent = AtomicReference<Lifecycle.Event?>(null)
            capturedLifecycle
                .get()
                ?.addObserver(LifecycleEventObserver { _, event -> lateEvent.set(event) })
            assertNull(lateEvent.get())

            assertDocumentContainsText(capturedDocument.get()!!.toCoreDocument(), "Binder:RESUMED")
        } finally {
            executor.shutdownNow()
            executor.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    /**
     * Case 2: Calling on the Android main looper thread (`Looper.myLooper() ==
     * Looper.getMainLooper()`). Verifies that the main looper branch preserves the caller's
     * coroutine context and captures dynamic state changes driven by frame callbacks.
     */
    @Test
    fun captureSingleRemoteDocument_onMainLooperThread_preservesContextAndCapturesState() =
        runTest {
            assertEquals(Looper.getMainLooper(), Looper.myLooper())

            val baseInterceptor = currentCoroutineContext()[ContinuationInterceptor]!!
            val trackingMainLooperInterceptor =
                object :
                    AbstractCoroutineContextElement(ContinuationInterceptor),
                    ContinuationInterceptor {
                    val interceptedCount = AtomicInteger(0)

                    override fun <T> interceptContinuation(
                        continuation: Continuation<T>
                    ): Continuation<T> {
                        interceptedCount.incrementAndGet()
                        return baseInterceptor.interceptContinuation(continuation)
                    }
                }

            val capturedInterceptor = AtomicReference<ContinuationInterceptor?>(null)
            val document =
                withContext(trackingMainLooperInterceptor) {
                    captureSingleRemoteDocument(context) {
                        var status by remember { mutableStateOf("Pending") }
                        val lifecycleState by
                            LocalLifecycleOwner.current.lifecycle.currentStateAsState()

                        LaunchedEffect(Unit) {
                            capturedInterceptor.set(
                                currentCoroutineContext()[ContinuationInterceptor]
                            )
                            withFrameNanos { status = "MainLooper:${lifecycleState.name}" }
                        }

                        RemoteBox(
                            modifier = RemoteModifier.fillMaxSize().background(Color.Blue.rc)
                        ) {
                            RemoteText(status.rs)
                        }
                    }
                }

            assertSame(trackingMainLooperInterceptor, capturedInterceptor.get())
            assertTrue(trackingMainLooperInterceptor.interceptedCount.get() > 0)
            assertDocumentContainsText(document.toCoreDocument(), "MainLooper:RESUMED")
        }

    /**
     * Case 3: Calling with a [MainCoroutineDispatcher] (`interceptor is MainCoroutineDispatcher`)
     * off the main looper thread. Verifies that the [MainCoroutineDispatcher] is preserved without
     * wrapping in `limitedParallelism(1)` and renders state driven by effects and lifecycle
     * observers.
     */
    @Test
    fun captureSingleRemoteDocument_withMainCoroutineDispatcher_preservesMainDispatcherAndCaptures() =
        runTest {
            withContext(Dispatchers.Default) {
                assertNull(Looper.myLooper())

                val delegateDispatcher = Dispatchers.Default.limitedParallelism(1)
                val testMainDispatcher =
                    object : MainCoroutineDispatcher() {
                        override val immediate: MainCoroutineDispatcher = this

                        override fun dispatch(context: CoroutineContext, block: Runnable) {
                            delegateDispatcher.dispatch(context, block)
                        }
                    }

                val capturedInterceptor = AtomicReference<ContinuationInterceptor?>(null)
                val document =
                    withContext(testMainDispatcher) {
                        captureSingleRemoteDocument(context) {
                            var text by remember { mutableStateOf("Initial") }
                            val lifecycleState by
                                LocalLifecycleOwner.current.lifecycle.currentStateAsState()

                            LaunchedEffect(Unit) {
                                capturedInterceptor.set(
                                    currentCoroutineContext()[ContinuationInterceptor]
                                )
                                withFrameNanos {
                                    text = "MainDispatcher:${lifecycleState.name}"
                                }
                            }

                            RemoteBox(
                                modifier = RemoteModifier.fillMaxSize().background(Color.Green.rc)
                            ) {
                                RemoteText(text.rs)
                            }
                        }
                    }

                assertSame(testMainDispatcher, capturedInterceptor.get())
                assertDocumentContainsText(document.toCoreDocument(), "MainDispatcher:RESUMED")
            }
        }

    /**
     * Case 4: Calling on a multi-threaded background [CoroutineDispatcher] (`Dispatchers.Default`).
     * Verifies that:
     * - The dispatcher is replaced with a `limitedParallelism(1)` view so concurrent child
     *   coroutines launched on the recomposer context never execute concurrently across threads,
     * - Self-removing [LifecycleEventObserver]s work safely off the main thread without throwing
     *   [IndexOutOfBoundsException] or [IllegalStateException],
     * - The captured [CoreDocument] reflects all state updates from background coroutines.
     */
    @Test
    fun captureSingleRemoteDocument_withMultiThreadedBackgroundDispatcher_serializesFramesAndCaptures() =
        runTest {
            val capturedInterceptor = AtomicReference<ContinuationInterceptor?>(null)
            val observedSelfRemoveEvent = AtomicReference<Lifecycle.Event?>(null)
            val activeDispatcherCoroutines = AtomicInteger(0)
            val maxConcurrentDispatcherCoroutines = AtomicInteger(0)

            val document =
                withContext(Dispatchers.Default) {
                    captureSingleRemoteDocument(context) {
                        val lifecycle = LocalLifecycleOwner.current.lifecycle
                        var completedTasks by remember { mutableStateOf(0) }

                        LaunchedEffect(Unit) {
                            capturedInterceptor.set(
                                currentCoroutineContext()[ContinuationInterceptor]
                            )

                            lateinit var selfRemovingObserver: LifecycleEventObserver
                            selfRemovingObserver = LifecycleEventObserver { _, event ->
                                observedSelfRemoveEvent.set(event)
                                if (event == Lifecycle.Event.ON_PAUSE) {
                                    lifecycle.removeObserver(selfRemovingObserver)
                                }
                            }
                            lifecycle.addObserver(selfRemovingObserver)

                            // Launch multiple coroutines executing work directly on the
                            // recomposer dispatcher across yield points; with
                            // limitedParallelism(1),
                            // at most 1 coroutine body may execute on a thread at any instant.
                            repeat(4) {
                                launch {
                                    val concurrent = activeDispatcherCoroutines.incrementAndGet()
                                    maxConcurrentDispatcherCoroutines.updateAndGet { max ->
                                        maxOf(max, concurrent)
                                    }
                                    activeDispatcherCoroutines.decrementAndGet()
                                    yield()
                                    withFrameNanos { completedTasks++ }
                                }
                            }
                        }

                        RemoteBox(
                            modifier = RemoteModifier.fillMaxSize().background(Color.Red.rc)
                        ) {
                            RemoteText("TasksCompleted:$completedTasks".rs)
                        }
                    }
                }

            assertNotNull(capturedInterceptor.get())
            assertTrue(capturedInterceptor.get() is CoroutineDispatcher)
            assertNotSame(Dispatchers.Default, capturedInterceptor.get())
            assertEquals(1, maxConcurrentDispatcherCoroutines.get())
            assertEquals(Lifecycle.Event.ON_PAUSE, observedSelfRemoveEvent.get())
            assertDocumentContainsText(document.toCoreDocument(), "TasksCompleted:4")
        }

    /**
     * Case 5a: Calling on an unconfined [CoroutineDispatcher] (`Dispatchers.Unconfined`, where
     * `isDispatchNeeded` returns `false`) off the main looper thread. Verifies that
     * `captureSingleRemoteDocument` does not call `limitedParallelism(1)` (which throws
     * [UnsupportedOperationException] on [Dispatchers.Unconfined]) and captures the composed
     * document properly.
     */
    @Test
    fun captureSingleRemoteDocument_withUnconfinedDispatcher_doesNotThrowAndCaptures() = runTest {
        withContext(Dispatchers.Default) {
            assertNull(Looper.myLooper())

            val capturedInterceptor = AtomicReference<ContinuationInterceptor?>(null)
            val document =
                withContext(Dispatchers.Unconfined) {
                    captureSingleRemoteDocument(context) {
                        var label by remember { mutableStateOf("Start") }
                        LaunchedEffect(Unit) {
                            capturedInterceptor.set(
                                currentCoroutineContext()[ContinuationInterceptor]
                            )
                            withFrameNanos { label = "Unconfined:OK" }
                        }
                        RemoteBox(
                            modifier = RemoteModifier.fillMaxSize().background(Color.Yellow.rc)
                        ) {
                            RemoteText(label.rs)
                        }
                    }
                }

            assertSame(Dispatchers.Unconfined, capturedInterceptor.get())
            assertDocumentContainsText(document.toCoreDocument(), "Unconfined:OK")
        }
    }

    /**
     * Case 5b: Calling with a non-[CoroutineDispatcher] [ContinuationInterceptor] (such as
     * `ApplyingContinuationInterceptor` in Compose UI tests) off the main looper thread. Verifies
     * that the custom interceptor is preserved and intercepts continuations during capture.
     */
    @Test
    fun captureSingleRemoteDocument_withNonDispatcherContinuationInterceptor_preservesInterceptorAndCaptures() =
        runTest {
            withContext(Dispatchers.Default) {
                val baseInterceptor = currentCoroutineContext()[ContinuationInterceptor]!!
                val interceptedCount = AtomicInteger(0)
                val customInterceptor =
                    object :
                        AbstractCoroutineContextElement(ContinuationInterceptor),
                        ContinuationInterceptor {
                        override fun <T> interceptContinuation(
                            continuation: Continuation<T>
                        ): Continuation<T> {
                            interceptedCount.incrementAndGet()
                            return baseInterceptor.interceptContinuation(continuation)
                        }
                    }

                val capturedInterceptor = AtomicReference<ContinuationInterceptor?>(null)
                val document =
                    withContext(customInterceptor) {
                        captureSingleRemoteDocument(context) {
                            var label by remember { mutableStateOf("Init") }
                            LaunchedEffect(Unit) {
                                capturedInterceptor.set(
                                    currentCoroutineContext()[ContinuationInterceptor]
                                )
                                withFrameNanos { label = "CustomInterceptor:OK" }
                            }
                            RemoteBox(
                                modifier = RemoteModifier.fillMaxSize().background(Color.Cyan.rc)
                            ) {
                                RemoteText(label.rs)
                            }
                        }
                    }

                assertSame(customInterceptor, capturedInterceptor.get())
                assertTrue(interceptedCount.get() > 0)
                assertDocumentContainsText(document.toCoreDocument(), "CustomInterceptor:OK")
            }
        }

    /**
     * Focused test verifying that when a coroutine mutates snapshot state *after* resuming from
     * [withFrameNanos] (so the mutation executes in a continuation dispatched after
     * `BroadcastFrameClock.sendFrame` has already returned and `Recomposer` initially transitioned
     * to `Idle`), the drain-and-apply loop observes the pending write and captures the updated
     * state in the rendered document.
     */
    @Test
    fun captureSingleRemoteDocument_stateWriteAfterWithFrameNanos_isCapturedBeforeIdleExit() =
        runTest {
            val document =
                withContext(Dispatchers.Default) {
                    captureSingleRemoteDocument(context) {
                        var status by remember { mutableStateOf("BeforeFrame") }

                        LaunchedEffect(Unit) {
                            withFrameNanos {}
                            // Mutate state strictly after withFrameNanos returns
                            status = "AfterFrameWrite"
                        }

                        RemoteBox(
                            modifier = RemoteModifier.fillMaxSize().background(Color.Magenta.rc)
                        ) {
                            RemoteText(status.rs)
                        }
                    }
                }

            assertDocumentContainsText(document.toCoreDocument(), "AfterFrameWrite")
        }

    /**
     * Focused test verifying that if a Composable/effect continuously mutates state on every frame
     * so recomposition never reaches quiescence, [captureSingleRemoteDocument] throws an
     * [IllegalStateException] naming unstable recomposition rather than hanging indefinitely.
     */
    @Test
    fun captureSingleRemoteDocument_withUnstableRecompositionLoop_throwsDiagnosticException() =
        runTest {
            val exception =
                assertThrows(IllegalStateException::class.java) {
                    runBlocking {
                        captureSingleRemoteDocument(context) {
                            var frameCounter by remember { mutableStateOf(0) }
                            LaunchedEffect(frameCounter) {
                                withFrameNanos {}
                                frameCounter++
                            }
                            RemoteBox(
                                modifier = RemoteModifier.fillMaxSize().background(Color.Red.rc)
                            ) {
                                RemoteText("Frame:$frameCounter".rs)
                            }
                        }
                    }
                }
            assertTrue(
                "Expected diagnostic message mentioning unstable recomposition, got: ${exception.message}",
                exception.message!!.contains("unstable recomposition"),
            )
        }

    /**
     * Focused test verifying that [SnapshotWriteMonitor] reference-counts concurrent active
     * sessions and disposes its global write observer handle + background job when the last session
     * releases.
     */
    @Test
    fun snapshotWriteMonitor_refCountsConcurrentSessionsAndDisposesWhenIdle() {
        assertEquals(0, SnapshotWriteMonitor.activeSessionCount)
        assertFalse(SnapshotWriteMonitor.isRunning)

        SnapshotWriteMonitor.acquire()
        assertEquals(1, SnapshotWriteMonitor.activeSessionCount)
        assertTrue(SnapshotWriteMonitor.isRunning)

        SnapshotWriteMonitor.acquire()
        assertEquals(2, SnapshotWriteMonitor.activeSessionCount)
        assertTrue(SnapshotWriteMonitor.isRunning)

        SnapshotWriteMonitor.release()
        assertEquals(1, SnapshotWriteMonitor.activeSessionCount)
        assertTrue(SnapshotWriteMonitor.isRunning)

        SnapshotWriteMonitor.release()
        assertEquals(0, SnapshotWriteMonitor.activeSessionCount)
        assertFalse(SnapshotWriteMonitor.isRunning)
    }

    /**
     * Case 6: Streaming [captureRemoteDocument] (`Flow<ByteArray>`) on a background dispatcher with
     * lifecycle observation and dynamic state updates. Verifies that:
     * - [LocalLifecycleOwner] is in [Lifecycle.State.RESUMED] while collecting on a background
     *   dispatcher,
     * - Recomposition emits updated [CoreDocument] snapshots when state changes,
     * - Cancelling collection destroys the [LocalLifecycleOwner], dispatches `ON_DESTROY`, and
     *   releases [SnapshotWriteMonitor] back to 0 active sessions.
     */
    @Test
    fun captureRemoteDocumentFlow_onBackgroundDispatcher_emitsRecomposedDocumentsAndDestroysLifecycle() =
        runTest {
            val state = mutableStateOf("Step1")
            val lifecycleEvents = CopyOnWriteArrayList<Lifecycle.Event>()
            val emittedDocs = CopyOnWriteArrayList<CoreDocument>()

            val flow =
                captureRemoteDocument(
                    context = context,
                    creationDisplayInfo = createCreationDisplayInfo(context),
                    coroutineContext = Dispatchers.Default,
                ) {
                    val lifecycle = LocalLifecycleOwner.current.lifecycle
                    val lifecycleState by lifecycle.currentStateAsState()
                    LaunchedEffect(lifecycle) {
                        lifecycle.addObserver(
                            LifecycleEventObserver { _, event -> lifecycleEvents.add(event) }
                        )
                    }
                    RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Red.rc)) {
                        RemoteText("${state.value}:${lifecycleState.name}".rs)
                    }
                }

            val collectJob =
                launch(Dispatchers.Default) {
                    flow.collect { bytes -> emittedDocs.add(bytes.toCoreDocument()) }
                }

            withContext(Dispatchers.Default) {
                withTimeout(5.seconds) {
                    while (emittedDocs.none { it.textValues().contains("Step1:RESUMED") }) {
                        delay(1)
                    }
                }
            }
            assertTrue(emittedDocs.any { it.textValues().contains("Step1:RESUMED") })
            assertTrue(SnapshotWriteMonitor.isRunning)

            state.value = "Step2"
            withContext(Dispatchers.Default) {
                withTimeout(5.seconds) {
                    while (emittedDocs.none { it.textValues().contains("Step2:RESUMED") }) {
                        delay(1)
                    }
                }
            }
            assertTrue(emittedDocs.any { it.textValues().contains("Step2:RESUMED") })

            collectJob.cancelAndJoin()
            assertEquals(0, SnapshotWriteMonitor.activeSessionCount)
            assertFalse(SnapshotWriteMonitor.isRunning)
            assertTrue(lifecycleEvents.contains(Lifecycle.Event.ON_RESUME))
            assertTrue(lifecycleEvents.contains(Lifecycle.Event.ON_DESTROY))
        }

    /**
     * Regression test for updates being silently dropped when the collector cannot observe
     * [Recomposer.State] transitions.
     *
     * `Recomposer.currentState` is a conflated `StateFlow`. Driving document regeneration off
     * `filter { it == Idle }` therefore relies on the collector actually observing the transient
     * `Idle -> Recomposing -> Idle` cycle. A collector that is suspended for the whole cycle only
     * ever sees the value `Idle`, which is unchanged, so no regeneration happens and the update is
     * lost.
     *
     * This test makes that window deterministic by parking the collector inside `collect {}` while
     * the state write happens, rather than relying on CPU contention to deschedule it.
     */
    @Test
    fun captureRemoteDocumentFlow_stateChangedWhileCollectorSuspended_stillEmitsUpdate() = runTest {
        val state = mutableStateOf("Before")
        val emittedDocs = CopyOnWriteArrayList<CoreDocument>()
        val firstEmissionReceived = CompletableDeferred<Unit>()
        val releaseCollector = CompletableDeferred<Unit>()

        val flow =
            captureRemoteDocument(
                context = context,
                creationDisplayInfo = createCreationDisplayInfo(context),
                coroutineContext = Dispatchers.Default,
            ) {
                RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Red.rc)) {
                    RemoteText(state.value.rs)
                }
            }

        val collectJob =
            launch(Dispatchers.Default) {
                flow.collect { bytes ->
                    val isFirst = emittedDocs.isEmpty()
                    emittedDocs.add(bytes.toCoreDocument())
                    if (isFirst) {
                        firstEmissionReceived.complete(Unit)
                        // Hold the collector here so it cannot observe any recomposer state
                        // transition triggered by the write below.
                        releaseCollector.await()
                    }
                }
            }

        withContext(Dispatchers.Default) {
            withTimeout(10.seconds) { firstEmissionReceived.await() }
        }

        state.value = "After"
        releaseCollector.complete(Unit)

        withContext(Dispatchers.Default) {
            withTimeout(10.seconds) {
                while (emittedDocs.none { it.textValues().contains("After") }) {
                    delay(1)
                }
            }
        }

        assertTrue(
            "Expected an emission containing \"After\", got ${emittedDocs.map { it.textValues() }}",
            emittedDocs.any { it.textValues().contains("After") },
        )

        collectJob.cancelAndJoin()
    }

    /**
     * Case 7: Multithreaded integration test exercising concurrent `captureSingleRemoteDocument`
     * calls, concurrent `captureRemoteDocument` streaming flows, cross-thread global snapshot
     * writes, and self-removing one-shot `onCreate` lifecycle observers across
     * `Dispatchers.Default`.
     */
    @Test
    fun captureRemoteDocument_concurrentMultiThreadedCapturesAndStateWrites_areThreadSafe() =
        runTest {
            val workerCount = 4
            val sharedCounter = mutableStateOf(0)
            val workerOnCreateCounts = AtomicIntegerArray(workerCount)
            val oneShotOnCreateResumeCount = AtomicInteger(0)

            withContext(Dispatchers.Default) {
                val writerJob = launch {
                    repeat(20) { step ->
                        sharedCounter.value = step + 1
                        delay(1)
                    }
                }

                val singleCaptureJobs =
                    (0 until workerCount).map { workerId ->
                        launch {
                            val doc =
                                captureSingleRemoteDocument(context) {
                                    val lifecycle = LocalLifecycleOwner.current.lifecycle
                                    val lifecycleState by lifecycle.currentStateAsState()
                                    var workerState by remember { mutableStateOf("Init-$workerId") }

                                    LaunchedEffect(Unit) {
                                        lateinit var oneShotObserver: DefaultLifecycleObserver
                                        oneShotObserver =
                                            object : DefaultLifecycleObserver {
                                                override fun onCreate(owner: LifecycleOwner) {
                                                    workerOnCreateCounts.incrementAndGet(workerId)
                                                    lifecycle.removeObserver(oneShotObserver)
                                                }

                                                override fun onResume(owner: LifecycleOwner) {
                                                    oneShotOnCreateResumeCount.incrementAndGet()
                                                }
                                            }
                                        lifecycle.addObserver(oneShotObserver)

                                        withFrameNanos {}
                                        workerState = "Worker-$workerId:${lifecycleState.name}"
                                    }

                                    RemoteBox(
                                        modifier =
                                            RemoteModifier.fillMaxSize().background(Color.Red.rc)
                                    ) {
                                        RemoteText("$workerState:${sharedCounter.value}".rs)
                                    }
                                }

                            val coreDoc = doc.toCoreDocument()
                            assertTrue(
                                "Expected Worker-$workerId:RESUMED in ${coreDoc.textValues()}",
                                coreDoc.textValues().any {
                                    it.startsWith("Worker-$workerId:RESUMED:")
                                },
                            )
                        }
                    }

                val streamingJobs =
                    (0 until 2).map { streamId ->
                        launch {
                            val streamState = mutableStateOf("StreamInit-$streamId")
                            val flow =
                                captureRemoteDocument(
                                    context = context,
                                    creationDisplayInfo = createCreationDisplayInfo(context),
                                    coroutineContext = Dispatchers.Default,
                                ) {
                                    val lifecycleState by
                                        LocalLifecycleOwner.current.lifecycle.currentStateAsState()
                                    RemoteBox(
                                        modifier =
                                            RemoteModifier.fillMaxSize().background(Color.Blue.rc)
                                    ) {
                                        RemoteText(
                                            "${streamState.value}:${lifecycleState.name}:${sharedCounter.value}"
                                                .rs
                                        )
                                    }
                                }

                            val initialDoc =
                                withTimeout(5.seconds) {
                                    flow
                                        .map { it.toCoreDocument() }
                                        .first { doc ->
                                            doc.textValues().any {
                                                it.startsWith("StreamInit-$streamId:RESUMED:")
                                            }
                                        }
                                }

                            assertTrue(
                                initialDoc.textValues().any {
                                    it.startsWith("StreamInit-$streamId:RESUMED:")
                                }
                            )

                            streamState.value = "StreamUpdated-$streamId"

                            val updatedDoc =
                                withTimeout(5.seconds) {
                                    flow
                                        .map { it.toCoreDocument() }
                                        .first { doc ->
                                            doc.textValues().any {
                                                it.startsWith("StreamUpdated-$streamId:RESUMED:")
                                            }
                                        }
                                }

                            assertTrue(
                                updatedDoc.textValues().any {
                                    it.startsWith("StreamUpdated-$streamId:RESUMED:")
                                }
                            )
                        }
                    }

                writerJob.join()
                singleCaptureJobs.forEach { it.join() }
                streamingJobs.forEach { it.join() }
            }

            assertEquals(0, SnapshotWriteMonitor.activeSessionCount)
            assertFalse(SnapshotWriteMonitor.isRunning)
            for (workerId in 0 until workerCount) {
                assertTrue(
                    "Expected worker $workerId to observe at least one onCreate callback",
                    workerOnCreateCounts.get(workerId) >= 1,
                )
            }
            assertEquals(
                "One-shot observer that removed itself in onCreate must not receive onResume",
                0,
                oneShotOnCreateResumeCount.get(),
            )
        }

    @Test
    fun captureDocument_withMainImmediateInsideRunTest_doesNotDeadlock() = runTest {
        val doc =
            withContext(Dispatchers.Main.immediate) {
                    captureSingleRemoteDocument(context) {
                        var state by remember { mutableStateOf("Init") }
                        LaunchedEffect(Unit) {
                            withFrameNanos {}
                            state = "AfterFrame"
                        }
                        RemoteBox(modifier = RemoteModifier.fillMaxSize()) { RemoteText(state.rs) }
                    }
                }
                .toCoreDocument()

        assertDocumentContainsText(doc, "AfterFrame")
    }

    private fun ByteArray.toCoreDocument(): CoreDocument {
        val bytes = this
        return CoreDocument().apply {
            ByteArrayInputStream(bytes).use { stream ->
                initFromBuffer(RemoteComposeBuffer.fromInputStream(stream))
            }
        }
    }

    private fun CapturedDocument.toCoreDocument(): CoreDocument = bytes.toCoreDocument()

    private fun CoreDocument.textValues(): Collection<String> = mTextData.values

    private fun assertDocumentContainsText(doc: CoreDocument, expected: String) {
        assertTrue(
            "Expected CoreDocument text data to contain '$expected', got ${doc.textValues()}",
            doc.textValues().contains(expected),
        )
    }
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Repeat(val times: Int = 1)

class RepeatRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        val repeat =
            description.getAnnotation(Repeat::class.java)
                ?: description.testClass.getAnnotation(Repeat::class.java)
        val times = repeat?.times ?: 1
        return if (times > 1) {
            object : Statement() {
                override fun evaluate() {
                    for (i in 0 until times) {
                        base.evaluate()
                    }
                }
            }
        } else {
            base
        }
    }
}
