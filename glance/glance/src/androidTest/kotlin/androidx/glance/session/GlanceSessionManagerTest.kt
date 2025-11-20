/*
 * Copyright 2023 The Android Open Source Project
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
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceModifier
import androidx.glance.session.SessionWorker.Companion.TimeoutExitReason
import androidx.glance.text.EmittableText
import androidx.glance.text.Text
import androidx.lifecycle.asFlow
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalStdlibApi::class, ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class GlanceSessionManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext!!

    private val testScope = TestScope()
    private val testSession = TestSession()

    private lateinit var workerState: StateFlow<List<WorkInfo>>
    private val workerStateScope = CoroutineScope(Job())

    private val additionalTime = 200.milliseconds
    private val idleTimeout = 200.milliseconds
    private val initialTimeout = 500.milliseconds
    private val timeouts =
        TimeoutOptions(
            additionalTime = additionalTime,
            idleTimeout = idleTimeout,
            initialTimeout = initialTimeout,
            timeSource = { testScope.currentTime },
        )

    @Before
    fun before() = runBlocking {
        val config =
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(SynchronousExecutor())
                .setWorkerFactory(
                    object : WorkerFactory() {
                        override fun createWorker(
                            appContext: Context,
                            workerClassName: String,
                            workerParameters: WorkerParameters,
                        ): ListenableWorker {
                            assertThat(workerClassName)
                                .isEqualTo(SessionWorker::class.qualifiedName)
                            return SessionWorker(
                                appContext,
                                workerParameters,
                                GlanceSessionManager,
                                timeouts,
                                testScope.coroutineContext[CoroutineDispatcher]!!,
                            )
                        }
                    }
                )
                .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workerState =
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(testSession.key)
                .asFlow()
                .stateIn(workerStateScope)
    }

    @After
    fun after() {
        testScope.cancel()
        workerStateScope.cancel()
        WorkManager.getInstance(context).cancelAllWork()
        // TODO(b/242026176): remove this once WorkManager allows closing the test database.
        WorkManagerImpl.getInstance(context).workDatabase.close()
    }

    @Test
    fun startingSessionRunsComposition() =
        testScope.runTest {
            startSession()

            val text = assertIs<EmittableText>(testSession.uiTree.receive().children.single())
            assertThat(text.text).isEqualTo("Hello World")

            GlanceSessionManager.runWithLock { assertNotNull(getSession(testSession.key)).close() }
            waitForWorkerSuccess()
        }

    @Test
    fun sessionInitialTimeout() =
        testScope.runTest {
            startSession()

            // Timeout starts after first successful composition
            testSession.uiTree.receive()
            val timeout = testTimeSource.measureTime { waitForWorkerTimeout() }
            assertThat(timeout).isEqualTo(initialTimeout)
        }

    @Test
    fun sessionDoesNotTimeoutBeforeFirstComposition() =
        testScope.runTest {
            startSession()

            // The session is not subject to a timeout before the composition has been processed
            // successfully for the first time.
            delay(initialTimeout * 5)
            GlanceSessionManager.runWithLock {
                assertThat(isSessionRunning(context, testSession.key)).isTrue()
            }

            testSession.uiTree.receive()
            val timeout = testTimeSource.measureTime { waitForWorkerTimeout() }
            assertThat(timeout).isEqualTo(initialTimeout)
        }

    @Test
    fun sessionAddsTimeOnExternalEvents() =
        testScope.runTest {
            startSession()

            // Timeout starts after first successful composition, and is incremented every time an
            // event is received.
            testSession.uiTree.receive()
            val timeout =
                testTimeSource.measureTime {
                    delay(initialTimeout - 1.milliseconds)
                    GlanceSessionManager.runWithLock { testSession.sendEmptyEvent() }
                    waitForWorkerTimeout()
                }
            assertThat(timeout).isEqualTo(initialTimeout + additionalTime)
        }

    @Test
    fun sessionDoesNotAddTimeOnExternalEventsIfThereIsEnoughTimeLeft() =
        testScope.runTest {
            startSession()

            // Timeout starts after first successful composition. There is still enough time when
            // events
            // arrive, so the deadline will not be extended.
            testSession.uiTree.receive()
            val timeout =
                testTimeSource.measureTime {
                    repeat(5) { testSession.sendEmptyEvent() }
                    waitForWorkerTimeout()
                }
            assertThat(timeout).isEqualTo(initialTimeout)
        }

    @Test
    fun sessionRestarts_IfItTimesOutWithPendingEvents() =
        testScope.runTest {
            startSession()

            testSession.uiTree.receive()

            // Send an event that sends another event, and block until the timeout is reached so
            // that
            // the second event cannot be processed by this session.
            testSession.sendRunnableEvent {
                testSession.sendEmptyEvent()
                delay(INFINITE)
            }

            // When restarting, the worker state remains RUNNING, so we run out the timer on the
            // first
            // session so that it restarts.
            delay(initialTimeout + 1.milliseconds)

            assertThat(testSession.isOpen).isFalse()

            // Verify that a new session is running, with the event that was unprocessed.
            GlanceSessionManager.runWithLock {
                val newSession = assertNotNull(getSession(testSession.key))
                assertThat(isSessionRunning(context, testSession.key)).isTrue()
                assertThat(newSession).isNotSameInstanceAs(testSession)
                assertThat(newSession.receiveAllPendingEvents())
                    .containsExactly(TestSession.EmptyEvent)
            }
        }

    @Test
    fun sessionDoesNotRestart_IfItTimesOutWithNoPendingEvents() =
        testScope.runTest {
            startSession()

            testSession.uiTree.receive()

            // Send event that blocks until the timeout is reached.
            testSession.sendRunnableEvent { delay(INFINITE) }
            waitForWorkerTimeout()

            assertThat(testSession.isOpen).isFalse()

            // Verify that a new session has not been enqueued.
            GlanceSessionManager.runWithLock {
                assertThat(isSessionRunning(context, testSession.key)).isFalse()
                assertThat(getSession(testSession.key)).isNull()
            }
        }

    @Test
    fun sessionDoesNotRestart_IfItStopsDueToError() =
        testScope.runTest {
            startSession()

            testSession.uiTree.receive()

            // Trigger an error, and wait for the worker to close
            testSession.triggerCompositionError()
            waitForWorkerFailure()

            assertThat(testSession.isOpen).isFalse()

            // Verify that a new session has not been enqueued.
            GlanceSessionManager.runWithLock {
                assertThat(isSessionRunning(context, testSession.key)).isFalse()
                assertThat(getSession(testSession.key)).isNull()
            }
        }

    @Test
    fun sessionDoesNotRestart_IfItIsClosed() =
        testScope.runTest {
            startSession()

            testSession.uiTree.receive()

            GlanceSessionManager.runWithLock { closeSession(testSession.key) }
            waitForWorkerSuccess()

            // Verify that a new session has not been enqueued.
            GlanceSessionManager.runWithLock {
                assertThat(isSessionRunning(context, testSession.key)).isFalse()
                assertThat(getSession(testSession.key)).isNull()
            }
        }

    private suspend fun startSession() {
        GlanceSessionManager.runWithLock {
            android.util.Log.e("WIDGET", "0.1")
            startSession(context, testSession)
            android.util.Log.e("WIDGET", "0.2")
            waitForWorkerStart()
            android.util.Log.e("WIDGET", "0.3")
            assertThat(isSessionRunning(context, testSession.key)).isTrue()
        }
    }

    private suspend fun waitForWorkerState(vararg state: State) =
        workerState.first { it.singleOrNull()?.state in state }.single()

    private suspend fun waitForWorkerStart() = waitForWorkerState(State.RUNNING)

    private suspend fun waitForWorkerSuccess() = waitForWorkerState(State.SUCCEEDED)

    private suspend fun waitForWorkerTimeout() =
        waitForWorkerSuccess().also {
            assertThat(it.outputData.getBoolean(TimeoutExitReason, false)).isTrue()
        }

    private suspend fun waitForWorkerFailure() = waitForWorkerState(State.FAILED)

    private suspend fun waitForWorkerStateChange(): WorkInfo {
        val currentValue = workerState.value
        return workerState.first { it != currentValue }.single()
    }
}

class TestSession : Session("session-123") {
    val uiTree = Channel<EmittableWithChildren>(capacity = Channel.RENDEZVOUS)
    private var triggerError by mutableStateOf(false)

    suspend fun sendEmptyEvent() = sendEvent(EmptyEvent)

    suspend fun sendRunnableEvent(block: suspend () -> Unit) = sendEvent(RunnableEvent(block))

    fun triggerCompositionError() {
        triggerError = true
    }

    override fun createRootEmittable(): EmittableWithChildren = RootEmittable()

    override fun provideGlance(context: Context) =
        @Composable {
            Text("Hello World")
            if (triggerError) {
                throw Throwable("There is an error in my composition!")
            }
        }

    override suspend fun processEmittableTree(
        context: Context,
        root: EmittableWithChildren,
    ): Boolean {
        uiTree.send(root)
        return true
    }

    override suspend fun processEvent(context: Context, event: Any) {
        if (event is RunnableEvent) event.block.invoke()
    }

    override suspend fun recreateWithEvents(events: List<Any>) =
        TestSession().apply { events.forEach { sendEvent(it) } }

    private class RootEmittable(override var modifier: GlanceModifier = GlanceModifier) :
        EmittableWithChildren() {
        override fun copy() = RootEmittable(modifier).also { it.children.addAll(children) }
    }

    object EmptyEvent

    class RunnableEvent(val block: suspend () -> Unit)
}

private suspend fun <T> Flow<T?>.firstNotNull() = first { it != null }!!
