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
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceComposable
import androidx.test.core.app.ApplicationProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SessionManagerImplTest {
    private val key = "KEY"
    private val session = object : Session(key) {
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
            root: EmittableWithChildren
        ): Boolean {
            TODO("Not yet implemented")
        }

        override suspend fun processEvent(context: Context, event: Any) {
            TODO("Not yet implemented")
        }
    }
    private lateinit var context: Context
    private lateinit var sessionManager: SessionManagerImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        sessionManager = SessionManagerImpl(TestWorker::class.java)
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
        assertThat(sessionManager.isSessionRunning(context, key)).isFalse()
        sessionManager.startSession(context, session)
        assertThat(sessionManager.isSessionRunning(context, key)).isTrue()
        assertThat(sessionManager.getSession(key)).isSameInstanceAs(session)
    }

    @Test
    fun closeSession() = runTest {
        sessionManager.startSession(context, session)
        assertThat(sessionManager.isSessionRunning(context, key)).isTrue()
        sessionManager.closeSession(key)
        assertThat(sessionManager.isSessionRunning(context, key)).isFalse()
        assertThat(sessionManager.getSession(key)).isNull()
    }
}

class TestWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        suspendCoroutine<Unit> {}
        return Result.success()
    }
}
