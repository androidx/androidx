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

package androidx.work.impl

import android.content.Context
import android.content.ContextWrapper
import android.os.Looper
import android.os.UserManager
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.SynchronousExecutor
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowUserManager

@Config(manifest = Config.NONE, minSdk = 24)
@RunWith(RobolectricTestRunner::class)
class WorkManagerDirectBootTest {

    private lateinit var realContext: Context
    private lateinit var contextWithConfiguration: ContextWithConfiguration
    private lateinit var shadowUserManager: ShadowUserManager
    private lateinit var taskExecutor: ExecutorService
    private lateinit var configuration: Configuration
    @Volatile private var initializationException: Throwable? = null

    class TestWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result = Result.success()
    }

    private open class ContextWithConfiguration(
        base: Context,
        override val workManagerConfiguration: Configuration,
    ) : ContextWrapper(base), Configuration.Provider {
        override fun getApplicationContext(): Context = this
    }

    @Before
    fun setUp() {
        initializationException = null
        realContext = ApplicationProvider.getApplicationContext()
        shadowUserManager = shadowOf(realContext.getSystemService(UserManager::class.java))
        shadowUserManager.setUserUnlocked(false)

        taskExecutor = Executors.newSingleThreadExecutor()
        configuration =
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setTaskExecutor(taskExecutor)
                .setExecutor(SynchronousExecutor())
                .setInitializationExceptionHandler { initializationException = it }
                .build()
        contextWithConfiguration = ContextWithConfiguration(realContext, configuration)
    }

    @After
    fun tearDown() {
        try {
            @Suppress("DEPRECATION")
            WorkManagerImpl.getInstance()?.let { wm ->
                do {
                    shadowOf(Looper.getMainLooper()).idle()
                    val latch = CountDownLatch(1)
                    wm.workTaskExecutor.executeOnTaskThread { latch.countDown() }
                    assertTrue(latch.await(5, TimeUnit.SECONDS))
                } while (
                    wm.workTaskExecutor.serialTaskExecutor.hasPendingTasks() ||
                        !shadowOf(Looper.getMainLooper()).isIdle
                )
                wm.close()
            }
            shadowOf(Looper.getMainLooper()).idle()
            taskExecutor.submit {}.get(5, TimeUnit.SECONDS)
            taskExecutor.shutdown()
            assertTrue(taskExecutor.awaitTermination(5, TimeUnit.SECONDS))
        } finally {
            WorkManagerWrapper.resetInstanceForTesting()
            WorkManagerImpl.resetInstanceForTesting()
        }
    }

    @Test
    fun testGetInstance_inDirectBoot_withConfigurationProvider_doesNotCrash() {
        shadowUserManager.setUserUnlocked(false)

        val retrievedInstance = WorkManager.getInstance(contextWithConfiguration)
        assertFalse(WorkManager.isInitialized())
        @Suppress("DEPRECATION") assertEquals(retrievedInstance, WorkManager.getInstance())
        assertNull(initializationException)

        // Unlocking and calling a public API lazily initializes the underlying WorkManagerImpl
        shadowUserManager.setUserUnlocked(true)
        val request = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        retrievedInstance.enqueue(request).result.get(5, TimeUnit.SECONDS)
        assertTrue(WorkManager.isInitialized())
        assertNull(initializationException)
    }

    @Test
    fun testGetInstanceNoArg_whenUninitialized_throwsException() {
        @Suppress("DEPRECATION")
        assertThrows(IllegalStateException::class.java) { WorkManager.getInstance() }
    }

    @Test
    fun testInitialize_afterUnlock_succeeds() {
        shadowUserManager.setUserUnlocked(true)

        WorkManager.initialize(contextWithConfiguration, configuration)
        assertTrue(WorkManager.isInitialized())
        @Suppress("DEPRECATION") val wm = WorkManager.getInstance()
        assertEquals(configuration, wm.configuration)

        val request = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        wm.enqueue(request).result.get(5, TimeUnit.SECONDS)
        assertNull(initializationException)
    }

    @Test
    fun testApiCall_inDirectBoot_throwsException() {
        shadowUserManager.setUserUnlocked(false)

        val wm = WorkManager.getInstance(contextWithConfiguration)
        assertFalse(WorkManager.isInitialized())

        val request = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        wm.enqueue(request).result.get(5, TimeUnit.SECONDS)
        assertTrue(initializationException is IllegalStateException)
        assertTrue(initializationException!!.message!!.contains("direct boot"))
    }

    @Test
    fun testApiCall_initializedAfterUnlock_succeeds() {
        shadowUserManager.setUserUnlocked(true)

        val wm = WorkManager.getInstance(contextWithConfiguration)
        assertTrue(WorkManager.isInitialized())

        val request = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        wm.enqueue(request).result.get(5, TimeUnit.SECONDS)
        assertNull(initializationException)
    }

    @Test
    fun testConcurrentPostUnlockAccess_initializesExactlyOnce() {
        shadowUserManager.setUserUnlocked(false)

        val wm = WorkManager.getInstance(contextWithConfiguration)
        assertFalse(WorkManager.isInitialized())

        // Unlock user
        shadowUserManager.setUserUnlocked(true)

        val threadsCount = 5
        val readyLatch = CountDownLatch(threadsCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadsCount)
        val executors = Executors.newFixedThreadPool(threadsCount)
        val caughtExceptions = CopyOnWriteArrayList<Throwable>()

        for (i in 0 until threadsCount) {
            val request = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
            executors.submit {
                readyLatch.countDown()
                startLatch.await()
                try {
                    wm.enqueue(request).result.get(5, TimeUnit.SECONDS)
                } catch (t: Throwable) {
                    caughtExceptions.add(t)
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS))
        startLatch.countDown()
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS))
        executors.shutdown()
        assertTrue(executors.awaitTermination(5, TimeUnit.SECONDS))

        assertTrue(
            "Expected no exceptions, but caught: $caughtExceptions",
            caughtExceptions.isEmpty(),
        )
        assertNull(initializationException)
        assertTrue(WorkManager.isInitialized())
    }
}
