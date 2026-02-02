/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.session

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.layout.Box
import androidx.glance.layout.EmittableBox
import androidx.glance.text.EmittableText
import androidx.glance.text.Text
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionWorkerTest {
    private val sessionManager = TestSessionManager()
    private lateinit var context: Context
    private lateinit var worker: SessionWorker

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        worker =
            TestListenableWorkerBuilder<SessionWorker>(context)
                .setInputData(workDataOf(sessionManager.keyParam to SESSION_KEY))
                .setWorkerFactory(
                    object : WorkerFactory() {
                        override fun createWorker(
                            appContext: Context,
                            workerClassName: String,
                            workerParameters: WorkerParameters
                        ) = SessionWorker(appContext, workerParameters, sessionManager)
                    }
                )
                .build()
    }

    @Test
    fun createSessionWorker() = runTest {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }
        sessionManager.scope.startSession(context)
        sessionManager.scope.closeSession()
    }

    @Test
    fun sessionWorkerRunsComposition() = runTest {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }

        val root =
            sessionManager.scope
                .startSession(context) { Box { Text("Hello World") } }
                .first()
                .getOrThrow()
        val box = assertIs<EmittableBox>(root.children.single())
        val text = assertIs<EmittableText>(box.children.single())
        assertThat(text.text).isEqualTo("Hello World")
        sessionManager.scope.closeSession()
    }

    @Test
    fun sessionWorkerCallsProvideGlance(): Unit = runTest {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }
        sessionManager.scope.startSession(context).first()
        val session = assertIs<TestSession>(sessionManager.scope.getSession(SESSION_KEY))
        assertThat(session.provideGlanceCalled).isEqualTo(1)
        sessionManager.scope.closeSession()
    }

    @Test
    fun sessionWorkerStateChangeTriggersRecomposition() = runTest {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }

        val state = mutableStateOf("Hello World")
        val uiFlow = sessionManager.scope.startSession(context) { Text(state.value) }
        uiFlow.first().getOrThrow().let { root ->
            val text = assertIs<EmittableText>(root.children.single())
            assertThat(text.text).isEqualTo("Hello World")
        }

        state.value = "Hello Earth"
        uiFlow.first().getOrThrow().let { root ->
            val text = assertIs<EmittableText>(root.children.single())
            assertThat(text.text).isEqualTo("Hello Earth")
        }
        sessionManager.scope.closeSession()
    }

    @Test
    fun sessionWorkerReceivesActions() = runTest {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }

        val state = mutableStateOf("Hello World")
        val uiFlow = sessionManager.scope.startSession(context) { Text(state.value) }
        uiFlow.first().getOrThrow().let { root ->
            val text = assertIs<EmittableText>(root.children.single())
            assertThat(text.text).isEqualTo("Hello World")
        }
        val session = assertIs<TestSession>(sessionManager.scope.getSession(SESSION_KEY))
        session.sendEvent { state.value = "Hello Earth" }
        uiFlow.first().getOrThrow().let { root ->
            val text = assertIs<EmittableText>(root.children.single())
            assertThat(text.text).isEqualTo("Hello Earth")
        }
        sessionManager.scope.closeSession()
    }

    @Test
    fun sessionWorkerCancelsProcessingWhenRecomposerStateChanges() = runTest {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }

        val state = mutableStateOf("Hello World")
        val uiFlow =
            sessionManager.scope.startDelayedProcessingSession(context) { Text(state.value) }
        uiFlow.first().getOrThrow().let { root ->
            val text = assertIs<EmittableText>(root.children.single())
            assertThat(text.text).isEqualTo("Hello World")
        }

        // Changing the value triggers recomposition, which should cancel the currently running call
        // to processEmittableTree.
        state.value = "Hello Earth"
        uiFlow.first().getOrThrow().let { root ->
            val text = assertIs<EmittableText>(root.children.single())
            assertThat(text.text).isEqualTo("Hello Earth")
        }

        val session = assertIs<TestSession>(sessionManager.scope.getSession(SESSION_KEY))
        assertThat(session.processEmittableTreeCancelCount).isEqualTo(1)
        sessionManager.scope.closeSession()
    }

    @Test
    fun sessionWorkerCatchesCompositionError() = runTest {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }

        val cause = Throwable()
        val exception = Exception("message", cause)
        val result =
            sessionManager.scope.startSession(context) { throw exception }.first().exceptionOrNull()
        assertThat(result).hasCauseThat().isEqualTo(cause)
        assertThat(result).hasMessageThat().isEqualTo("message")
    }

    @Test
    fun sessionWorkerCatchesRecompositionError() = runTest {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }

        val runError = mutableStateOf(false)
        val cause = Throwable()
        val exception = Exception("message", cause)
        val resultFlow =
            sessionManager.scope.startSession(context) {
                if (runError.value) {
                    throw exception
                } else {
                    Text("Hello World")
                }
            }

        resultFlow.first().getOrThrow().let { root ->
            val text = assertIs<EmittableText>(root.children.single())
            assertThat(text.text).isEqualTo("Hello World")
        }

        runError.value = true
        val result = resultFlow.first().exceptionOrNull()
        // Errors thrown on recomposition are wrapped in an identical outer exception with the
        // original exception as the `cause`.
        assertThat(result).hasCauseThat().isEqualTo(exception)
        assertThat(result?.cause?.cause).isEqualTo(cause)
        assertThat(result).hasMessageThat().isEqualTo("message")
    }

    @Test
    fun sessionWorkerCatchesSideEffectError() = runTest {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }

        val cause = Throwable()
        val exception = Exception("message", cause)
        val result =
            sessionManager.scope
                .startSession(context) { SideEffect { throw exception } }
                .first()
                .exceptionOrNull()
        assertThat(result).hasCauseThat().isEqualTo(cause)
        assertThat(result).hasMessageThat().isEqualTo("message")
    }

    @Test
    fun sessionWorkerCatchesLaunchedEffectError() = runTest {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }

        val cause = Throwable()
        val exception = Exception("message", cause)
        val result =
            sessionManager.scope
                .startSession(context) { LaunchedEffect(true) { throw exception } }
                .first()
                .exceptionOrNull()
        assertThat(result).hasCauseThat().isEqualTo(cause)
        assertThat(result).hasMessageThat().isEqualTo("message")
    }

    @Test
    fun sessionWorkerDoesNotLeakEffectJobOnCancellation() = runTest {
        val workerJob = launch {
            var wasCancelled = false
            try {
                worker.doWork()
            } catch (e: CancellationException) {
                wasCancelled = true
                assertThat(worker.effectJob?.isCancelled).isTrue()
            } finally {
                assertThat(wasCancelled).isTrue()
            }
        }

        sessionManager.scope.startSession(context).first()
        workerJob.cancel()
    }

    @Test
    fun sessionWorkerDoesNotLeakEffectJobOnClose() = runTest {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
            assertThat(worker.effectJob?.isCancelled).isTrue()
        }

        sessionManager.scope.startSession(context).first()
        sessionManager.scope.closeSession()
    }

    @Test
    fun sessionWorkerClosesWithTheLock() = runTest {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }

        val runError = mutableStateOf(false)
        val resultFlow =
            sessionManager.runWithLock {
                this as TestSessionManager.TestSessionManagerScope
                startSession(context) {
                    Text("Hello")
                    if (runError.value) throw Throwable()
                }
            }
        resultFlow.first { it.isSuccess }

        // Start the error within the lock
        sessionManager.runWithLock {
            runError.value = true
            resultFlow.first { it.isFailure }
            // Composition is now cancelled due to error. However, the worker should not be able to
            // close the session channel until it has the lock. yield() here; the worker will run
            // until it suspends to wait for the lock.
            yield()
            val session = checkNotNull(getSession(SESSION_KEY))
            assertThat(session.isOpen).isTrue()
        }

        // Now that we've let go of the lock, yield() again to make sure the worker can resume
        // from waiting for the lock and close the session.
        yield()
        sessionManager.runWithLock {
            val session = checkNotNull(getSession(SESSION_KEY))
            assertThat(session.isOpen).isFalse()
        }
    }
}

private const val SESSION_KEY = "123"

class TestSessionManager : SessionManager {
    val scope = TestSessionManagerScope()
    private val mutex = Mutex()

    override suspend fun <T> runWithLock(block: suspend SessionManagerScope.() -> T): T =
        mutex.withLock { scope.block() }

    class TestSessionManagerScope : SessionManagerScope {
        private val sessions = mutableMapOf<String, Session>()

        suspend fun startSession(
            context: Context,
            content: @GlanceComposable @Composable () -> Unit = {}
        ) =
            MutableSharedFlow<kotlin.Result<EmittableWithChildren>>().also { flow ->
                startSession(context, TestSession(resultFlow = flow, content = content))
            }

        suspend fun startDelayedProcessingSession(
            context: Context,
            content: @GlanceComposable @Composable () -> Unit = {}
        ) =
            MutableSharedFlow<kotlin.Result<EmittableWithChildren>>().also { flow ->
                startSession(
                    context,
                    TestSession(
                        resultFlow = flow,
                        content = content,
                        processEmittableTreeHasInfiniteDelay = true,
                    )
                )
            }

        suspend fun closeSession() {
            closeSession(SESSION_KEY)
        }

        override suspend fun startSession(context: Context, session: Session) {
            sessions[session.key] = session
        }

        override suspend fun closeSession(key: String) {
            sessions[key]?.close()
        }

        override suspend fun isSessionRunning(context: Context, key: String): Boolean {
            TODO("Not yet implemented")
        }

        override fun getSession(key: String): Session? = sessions[key]
    }
}

class TestSession(
    key: String = SESSION_KEY,
    val resultFlow: MutableSharedFlow<kotlin.Result<EmittableWithChildren>>? = null,
    val content: @GlanceComposable @Composable () -> Unit = {},
    var processEmittableTreeHasInfiniteDelay: Boolean = false,
) : Session(key) {
    override fun createRootEmittable() =
        object : EmittableWithChildren() {
            override var modifier: GlanceModifier = GlanceModifier

            override fun copy() = this

            override fun toString() = "EmittableRoot(children=[\n${childrenToString()}\n])"
        }

    var provideGlanceCalled = 0

    override fun provideGlance(context: Context): @Composable @GlanceComposable () -> Unit {
        provideGlanceCalled++
        return content
    }

    var processEmittableTreeCancelCount = 0

    override suspend fun processEmittableTree(
        context: Context,
        root: EmittableWithChildren
    ): Boolean {
        resultFlow?.emit(kotlin.Result.success(root))
        try {
            if (processEmittableTreeHasInfiniteDelay) {
                delay(Duration.INFINITE)
            }
        } catch (e: CancellationException) {
            processEmittableTreeCancelCount++
        }
        return true
    }

    suspend fun sendEvent(block: () -> Unit) = sendEvent(block as Any)

    override suspend fun processEvent(context: Context, event: Any) {
        require(event is Function0<*>)
        event.invoke()
    }

    override suspend fun onCompositionError(context: Context, throwable: Throwable) {
        resultFlow?.emit(kotlin.Result.failure(throwable))
    }
}
