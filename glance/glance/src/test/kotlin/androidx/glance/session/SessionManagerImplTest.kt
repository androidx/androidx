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

@file:OptIn(ExperimentalCoroutinesApi::class)

package androidx.glance.session

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.concurrent.futures.await
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceComposable
import androidx.test.core.app.ApplicationProvider
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SessionManagerImplTest {
    private lateinit var context: Context
    private lateinit var sessionManager: SessionManagerImpl
    private val workManagerProxy =
        object : WorkManagerProxy {
            val enqueueCalls = mutableListOf<Pair<String, OneTimeWorkRequest>>()
            val isRunningCalls = mutableListOf<String>()

            override suspend fun enqueueUniqueWork(
                context: Context,
                uniqueWorkName: String,
                existingWorkPolicy: ExistingWorkPolicy,
                workRequest: OneTimeWorkRequest,
            ) {
                enqueueCalls.add(uniqueWorkName to workRequest)
                WorkManagerProxy.Default.enqueueUniqueWork(
                    context,
                    uniqueWorkName,
                    existingWorkPolicy,
                    workRequest,
                )
            }

            override suspend fun workerIsRunningOrEnqueued(
                context: Context,
                uniqueWorkName: String,
            ): Boolean {
                isRunningCalls.add(uniqueWorkName)
                return WorkManagerProxy.Default.workerIsRunningOrEnqueued(context, uniqueWorkName)
            }
        }
    private val inputDataFactory: InputDataFactory = { session ->
        workDataOf("KEY" to session.key, "foo" to "bar")
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        sessionManager =
            SessionManagerImpl(
                workerClass = TestWorker::class.java,
                workManagerProxy = workManagerProxy,
                inputDataFactory = inputDataFactory,
            )
    }

    @After
    fun cleanUp() {
        WorkManager.getInstance(context).cancelAllWork()
        // TODO(b/242026176): remove this once WorkManager allows closing the test
        // database.
        WorkManagerImpl.getInstance(context).workDatabase.close()
    }

    @Test
    fun startSession() = runTest {
        sessionManager.runWithLock {
            val key = newKey()
            val session = createTestSession(key)

            assertThat(isSessionRunning(context, key)).isFalse()

            startSession(context, session)

            assertWithMessage(getDebugMessage(session))
                .that(isSessionRunning(context, key))
                .isTrue()
            assertThat(getSession(key)).isSameInstanceAs(session)
        }
    }

    @Test
    fun closeSession() = runTest {
        sessionManager.runWithLock {
            val key = newKey()
            val session = createTestSession(key)

            startSession(context, session)
            assertWithMessage(getDebugMessage(session))
                .that(isSessionRunning(context, key))
                .isTrue()
            closeSession(key)
            assertThat(isSessionRunning(context, key)).isFalse()
            assertThat(getSession(key)).isNull()
        }
    }

    @Test
    fun closedSessionIsNotRunning() = runTest {
        sessionManager.runWithLock {
            val key = newKey()
            val session = createTestSession(key)

            assertThat(isSessionRunning(context, key)).isFalse()
            startSession(context, session)
            assertWithMessage(getDebugMessage(session))
                .that(isSessionRunning(context, key))
                .isTrue()
            session.close()
            assertThat(isSessionRunning(context, key)).isFalse()
        }
    }

    @Test
    fun runWithLockIsMutuallyExclusive() = runTest {
        val firstRan = AtomicBoolean(false)
        launch {
            sessionManager.runWithLock {
                yield()
                firstRan.set(true)
            }
        }
        // Because test dispatchers are single threaded and do not run background `launch`es until
        // we suspend, we yield here to allow the first transaction to run. This resumes after the
        // yield above, while the first transaction still has the lock but is suspended.
        yield()
        // This call to runWithLock should suspend until the first transaction finishes, then run
        // the block. If it is not mutually exclusive, it will run right away and firstRan will not
        // be true.
        sessionManager.runWithLock { assertThat(firstRan.get()).isTrue() }
    }

    @Test
    fun usesWorkManagerProxy() = runTest {
        sessionManager.runWithLock {
            val key = newKey()
            val session = createTestSession(key)
            startSession(context, session)
            assertWithMessage(getDebugMessage(session))
                .that(isSessionRunning(context, key))
                .isTrue()

            assertThat(workManagerProxy.enqueueCalls.size).isEqualTo(2)
            assertThat(workManagerProxy.isRunningCalls.size).isEqualTo(1)
        }
    }

    @Test
    fun usesWorkerClass() = runTest {
        sessionManager.runWithLock {
            val key = newKey()
            val session = createTestSession(key)
            startSession(context, session)
            assertWithMessage(getDebugMessage(session))
                .that(isSessionRunning(context, key))
                .isTrue()

            val (_, workRequest) = workManagerProxy.enqueueCalls.single { it.first == key }
            assertThat(workRequest.workSpec.workerClassName)
                .isEqualTo(TestWorker::class.java.canonicalName)
        }
    }

    @Test
    fun usesInputDataFactory() = runTest {
        sessionManager.runWithLock {
            val key = newKey()
            val session = createTestSession(key)
            startSession(context, session)
            assertWithMessage(getDebugMessage(session))
                .that(isSessionRunning(context, key))
                .isTrue()

            val (_, workRequest) = workManagerProxy.enqueueCalls.single { it.first == key }
            assertThat(workRequest.workSpec.input.getString("KEY")).isEqualTo(key)
            assertThat(workRequest.workSpec.input.getString("foo")).isEqualTo("bar")
        }
    }

    private suspend fun getDebugMessage(session: Session): String {
        val sessionWorkStateInfo =
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(session.key)
                .await()
                .joinToString("\n") { w -> "work for session ${session.key} in state: ${w.state}" }
        return "$sessionWorkStateInfo\nsession.isOpen: ${session.isOpen} "
    }

    companion object {
        // Unique key for each test for better tracking of results.
        fun newKey() = UUID.randomUUID().toString()

        fun createTestSession(key: String): Session {
            return object : Session(key) {
                override fun createRootEmittable(): EmittableWithChildren {
                    TODO("Not yet implemented")
                }

                override fun provideGlance(
                    context: Context
                ): @Composable @GlanceComposable () -> Unit {
                    TODO("Not yet implemented")
                }

                override suspend fun processEmittableTree(
                    context: Context,
                    root: EmittableWithChildren,
                ): Boolean {
                    TODO("Not yet implemented")
                }

                override suspend fun processEvent(context: Context, event: Any) {
                    TODO("Not yet implemented")
                }

                override suspend fun recreateWithEvents(events: List<Any>): Session {
                    TODO("Not yet implemented")
                }
            }
        }
    }
}

class TestWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        suspendCoroutine<Unit> {}
        return Result.success()
    }
}
