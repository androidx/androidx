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
import androidx.work.R
import androidx.work.WorkManager
import androidx.work.WorkManagerInitializer
import androidx.work.WorkQuery
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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

    companion object {
        private const val TEST_TIMEOUT_SECONDS = 30L
    }

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
        private val configuration: Configuration,
    ) : ContextWrapper(base), Configuration.Provider {
        val configCallCount = AtomicInteger(0)

        override val workManagerConfiguration: Configuration
            get() {
                configCallCount.incrementAndGet()
                return configuration
            }

        override fun getApplicationContext(): Context = this
    }

    @Before
    fun setUp() {
        initializationException = null
        realContext = ApplicationProvider.getApplicationContext()
        assertTrue(realContext.resources.getBoolean(R.bool.workmanager_test_configuration))
        shadowUserManager = shadowOf(realContext.getSystemService(UserManager::class.java))
        shadowUserManager.setUserUnlocked(false)

        taskExecutor = Executors.newSingleThreadExecutor()
        configuration =
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setTaskExecutor(taskExecutor)
                .setExecutor(SynchronousExecutor())
                .setGreedySchedulerEnabled(false)
                .setInitializationExceptionHandler { initializationException = it }
                .build()
        contextWithConfiguration = ContextWithConfiguration(realContext, configuration)
    }

    @After
    fun tearDown() {
        try {
            try {
                @Suppress("DEPRECATION")
                WorkManagerImpl.getInstance()?.let { wm ->
                    try {
                        drainWorkManager(wm)
                    } finally {
                        wm.close()
                    }
                }
                shadowOf(Looper.getMainLooper()).idle()
                taskExecutor.submit {}.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                taskExecutor.shutdown()
                assertTrue(taskExecutor.awaitTermination(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            } finally {
                taskExecutor.shutdownNow()
            }
        } finally {
            WorkManagerImpl.resetInstanceForTesting()
        }
    }

    private fun drainWorkManager(wm: WorkManagerImpl) {
        do {
            shadowOf(Looper.getMainLooper()).idle()
            val latch = CountDownLatch(1)
            wm.workTaskExecutor.executeOnTaskThread { latch.countDown() }
            assertTrue(latch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        } while (
            wm.workTaskExecutor.serialTaskExecutor.hasPendingTasks() ||
                !shadowOf(Looper.getMainLooper()).isIdle
        )
    }

    @Test
    fun testGetInstance_inDirectBoot_withConfigurationProvider_doesNotCrash() {
        shadowUserManager.setUserUnlocked(false)

        val retrievedInstance = WorkManager.getInstance(contextWithConfiguration)
        assertFalse(WorkManager.isInitialized())
        @Suppress("DEPRECATION") assertSame(retrievedInstance, WorkManager.getInstance())
        assertNull(initializationException)

        // Unlocking and calling a public API lazily initializes the underlying WorkManagerImpl
        shadowUserManager.setUserUnlocked(true)
        val request = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        retrievedInstance.enqueue(request).result.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertTrue(WorkManager.isInitialized())
        @Suppress("DEPRECATION") assertSame(retrievedInstance, WorkManager.getInstance())
        assertNull(initializationException)
    }

    @Test
    fun testGetInstance_inDirectBoot_withoutConfigurationProvider_defersExceptionUntilApiCall() {
        shadowUserManager.setUserUnlocked(false)

        val retrievedInstance = WorkManager.getInstance(realContext)
        assertFalse(WorkManager.isInitialized())
        @Suppress("DEPRECATION") assertSame(retrievedInstance, WorkManager.getInstance())

        val request = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        val exception =
            assertThrows(IllegalStateException::class.java) { retrievedInstance.enqueue(request) }
        assertTrue(exception.message!!.contains("direct boot"))
        assertTrue(exception.cause is IllegalStateException)
        assertTrue(exception.cause!!.message!!.contains("WorkManager is not initialized properly"))
        assertFalse(WorkManager.isInitialized())
    }

    @Test
    fun testGetInstance_unlocked_withoutConfigurationProvider_throwsImmediately() {
        shadowUserManager.setUserUnlocked(true)
        assertThrows(IllegalStateException::class.java) { WorkManager.getInstance(realContext) }
        assertFalse(WorkManager.isInitialized())
        @Suppress("DEPRECATION")
        assertThrows(IllegalStateException::class.java) { WorkManager.getInstance() }

        // A subsequent call with a valid Configuration.Provider context succeeds because the
        // failed unlocked call did not leave an uninitialized wrapper cached in instance.
        val wm = WorkManager.getInstance(contextWithConfiguration)
        assertTrue(WorkManager.isInitialized())
        assertSame(configuration, wm.configuration)
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
        wm.enqueue(request).result.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertNull(initializationException)
    }

    @Test
    fun testInitialize_afterDeferredGetInstance_bindsExistingWrapperToDefaultImpl() {
        shadowUserManager.setUserUnlocked(false)
        val deferredWrapper = WorkManager.getInstance(realContext)
        assertFalse(WorkManager.isInitialized())

        shadowUserManager.setUserUnlocked(true)
        WorkManager.initialize(contextWithConfiguration, configuration)

        assertTrue(WorkManager.isInitialized())
        assertSame(deferredWrapper, WorkManager.getInstance(contextWithConfiguration))
        assertSame(configuration, deferredWrapper.configuration)

        val request = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        deferredWrapper.enqueue(request).result.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertNull(initializationException)
    }

    @Test
    fun testApiCall_inDirectBoot_reportsInitializationException() {
        shadowUserManager.setUserUnlocked(false)

        val wm = WorkManager.getInstance(contextWithConfiguration)
        assertFalse(WorkManager.isInitialized())

        val request = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        wm.enqueue(request).result.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertTrue(initializationException is IllegalStateException)
        assertTrue(initializationException!!.message!!.contains("direct boot"))
    }

    @Test
    fun testApiCall_initializedAfterUnlock_succeeds() {
        shadowUserManager.setUserUnlocked(true)

        val wm = WorkManager.getInstance(contextWithConfiguration)
        assertTrue(WorkManager.isInitialized())

        val request = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        wm.enqueue(request).result.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
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
        val requests =
            List(threadsCount) { OneTimeWorkRequest.Builder(TestWorker::class.java).build() }
        val expectedWorkIds = requests.map { it.id }
        assertEquals(threadsCount, expectedWorkIds.distinct().size)

        val readyLatch = CountDownLatch(threadsCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadsCount)
        val executors = Executors.newFixedThreadPool(threadsCount)
        val caughtExceptions = CopyOnWriteArrayList<Throwable>()

        try {
            for (request in requests) {
                executors.submit {
                    readyLatch.countDown()
                    startLatch.await()
                    try {
                        wm.enqueue(request).result.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    } catch (t: Throwable) {
                        caughtExceptions.add(t)
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            assertTrue(readyLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            startLatch.countDown()
            assertTrue(doneLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            executors.shutdown()
            assertTrue(executors.awaitTermination(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        } finally {
            executors.shutdownNow()
        }

        assertTrue(
            "Expected no exceptions, but caught: $caughtExceptions",
            caughtExceptions.isEmpty(),
        )
        assertNull(initializationException)
        assertTrue(WorkManager.isInitialized())
        assertEquals(1, contextWithConfiguration.configCallCount.get())

        val singletonInstance = WorkManager.getInstance(contextWithConfiguration)
        val recordedWorkInfos =
            singletonInstance
                .getWorkInfos(WorkQuery.fromIds(expectedWorkIds))
                .get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        assertEquals(expectedWorkIds.toSet(), recordedWorkInfos.map { it.id }.toSet())
    }

    @Test
    fun testSetDelegate_rotatesWrapperAndPreservesBoundDelegateOnExistingWrapper() {
        shadowUserManager.setUserUnlocked(true)
        val config1 =
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setTaskExecutor(SynchronousExecutor())
                .setExecutor(SynchronousExecutor())
                .build()
        val config2 =
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .setTaskExecutor(SynchronousExecutor())
                .setExecutor(SynchronousExecutor())
                .build()

        val impl1 =
            TestWorkManagerImpl(realContext, config1, WorkManagerTaskExecutor(config1.taskExecutor))
        val impl2 =
            TestWorkManagerImpl(realContext, config2, WorkManagerTaskExecutor(config2.taskExecutor))
        try {
            // A deferred wrapper created while locked binds in-place to the first delegate set via
            // setDelegate(impl1), and stays bound to impl1 even if setDelegate(impl2) runs before
            // the first wrapper is invoked.
            shadowUserManager.setUserUnlocked(false)
            val deferredWm = WorkManager.getInstance(realContext)
            shadowUserManager.setUserUnlocked(true)

            WorkManagerImpl.setDelegate(impl1)
            @Suppress("DEPRECATION") val wmFromNoArg = WorkManager.getInstance()
            val wm1 = WorkManager.getInstance(realContext)
            assertSame(deferredWm, wm1)
            assertSame(wmFromNoArg, wm1)

            // Updating the delegate rotates the active singleton wrapper while keeping wm1 bound
            // to impl1.
            WorkManagerImpl.setDelegate(impl2)
            val wm2 = WorkManager.getInstance(realContext)
            assertNotSame(wm1, wm2)
            assertSame(config1, wm1.configuration)
            assertSame(config2, wm2.configuration)

            val request = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
            wm1.enqueue(request).result.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            assertNotNull(
                impl1.getWorkInfoById(request.id).get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            )
            assertNull(
                impl2.getWorkInfoById(request.id).get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            )
        } finally {
            try {
                WorkManagerImpl.setDelegate(null)
            } finally {
                try {
                    try {
                        drainWorkManager(impl1)
                    } finally {
                        impl1.close()
                    }
                } finally {
                    try {
                        drainWorkManager(impl2)
                    } finally {
                        impl2.close()
                    }
                }
            }
        }
    }

    @Test
    fun testSetDelegate_thenNull_restoresWrapperOverDefaultImpl() {
        shadowUserManager.setUserUnlocked(true)
        WorkManager.initialize(contextWithConfiguration, configuration)
        val defaultWm = WorkManager.getInstance(contextWithConfiguration)
        assertSame(configuration, defaultWm.configuration)

        val overrideConfig =
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .setTaskExecutor(SynchronousExecutor())
                .setExecutor(SynchronousExecutor())
                .build()
        val overrideImpl =
            TestWorkManagerImpl(
                realContext,
                overrideConfig,
                WorkManagerTaskExecutor(overrideConfig.taskExecutor),
            )
        try {
            WorkManagerImpl.setDelegate(overrideImpl)
            val delegatedWm = WorkManager.getInstance(contextWithConfiguration)
            assertNotSame(defaultWm, delegatedWm)
            assertSame(overrideConfig, delegatedWm.configuration)

            WorkManagerImpl.setDelegate(null)
            val restoredWm = WorkManager.getInstance(contextWithConfiguration)
            assertNotSame(delegatedWm, restoredWm)
            assertSame(configuration, restoredWm.configuration)

            // Calling setDelegate(null) again when already bound to defaultImpl reuses the wrapper.
            WorkManagerImpl.setDelegate(null)
            assertSame(restoredWm, WorkManager.getInstance(contextWithConfiguration))
        } finally {
            try {
                WorkManagerImpl.setDelegate(null)
            } finally {
                try {
                    drainWorkManager(overrideImpl)
                } finally {
                    overrideImpl.close()
                }
            }
        }
    }

    @Test
    fun testGetInstance_withNullApplicationContext_whenDefaultInitialized_succeeds() {
        shadowUserManager.setUserUnlocked(true)
        WorkManager.initialize(contextWithConfiguration, configuration)

        val contextWithNullAppContext =
            object : ContextWrapper(realContext) {
                override fun getApplicationContext(): Context? = null
            }

        val wm = WorkManager.getInstance(contextWithNullAppContext)
        assertSame(configuration, wm.configuration)
    }

    @Test
    fun testGetInstance_inDirectBoot_withNullApplicationContext_thenInitialize_succeeds() {
        shadowUserManager.setUserUnlocked(false)
        val contextWithNullAppContext =
            object : ContextWrapper(realContext) {
                override fun getApplicationContext(): Context? = null
            }

        val wm = WorkManager.getInstance(contextWithNullAppContext)
        assertFalse(WorkManager.isInitialized())

        shadowUserManager.setUserUnlocked(true)
        WorkManager.initialize(contextWithConfiguration, configuration)

        assertTrue(WorkManager.isInitialized())
        assertSame(wm, WorkManager.getInstance(contextWithConfiguration))
        assertSame(configuration, wm.configuration)
    }

    @Test
    fun testGetInstance_afterUnlock_resolvesWithCallerContextAndBindsExistingWrapper() {
        shadowUserManager.setUserUnlocked(false)

        val wm = WorkManager.getInstance(realContext)
        assertFalse(WorkManager.isInitialized())

        shadowUserManager.setUserUnlocked(true)
        val resolved = WorkManager.getInstance(contextWithConfiguration)
        assertSame(wm, resolved)
        assertTrue(WorkManager.isInitialized())
        assertSame(configuration, wm.configuration)
    }

    @Test
    fun testResetAfterDeferredGetInstance_staleWrapperFailsAndNewWrapperWorks() {
        shadowUserManager.setUserUnlocked(false)
        val stale = WorkManager.getInstance(contextWithConfiguration)
        WorkManagerImpl.resetInstanceForTesting()
        shadowUserManager.setUserUnlocked(true)

        val request = OneTimeWorkRequest.Builder(TestWorker::class.java).build()
        assertThrows(IllegalStateException::class.java) { stale.enqueue(request) }
        assertFalse(WorkManager.isInitialized())

        val fresh = WorkManager.getInstance(contextWithConfiguration)
        assertNotSame(stale, fresh)
        assertTrue(WorkManager.isInitialized())
    }

    @Test
    fun testWorkManagerInitializer_afterDeferredGetInstance_bindsExistingWrapper() {
        shadowUserManager.setUserUnlocked(false)
        val deferredWrapper = WorkManager.getInstance(realContext)
        assertFalse(WorkManager.isInitialized())

        shadowUserManager.setUserUnlocked(true)
        val initializedWm = WorkManagerInitializer().create(realContext)

        assertTrue(WorkManager.isInitialized())
        assertSame(deferredWrapper, initializedWm)
        assertSame(deferredWrapper, WorkManager.getInstance(realContext))
        assertSame(initializedWm.configuration, deferredWrapper.configuration)
    }

    @Test
    fun testConcurrentGetInstance_inDirectBoot_returnsSameDeferredWrapperWithoutInitializing() {
        shadowUserManager.setUserUnlocked(false)

        val threadsCount = 5
        val readyLatch = CountDownLatch(threadsCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadsCount)
        val executors = Executors.newFixedThreadPool(threadsCount)
        val returnedInstances = CopyOnWriteArrayList<WorkManager>()
        val caughtExceptions = CopyOnWriteArrayList<Throwable>()

        try {
            repeat(threadsCount) {
                executors.submit {
                    readyLatch.countDown()
                    startLatch.await()
                    try {
                        returnedInstances.add(WorkManager.getInstance(contextWithConfiguration))
                    } catch (t: Throwable) {
                        caughtExceptions.add(t)
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            assertTrue(readyLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            startLatch.countDown()
            assertTrue(doneLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            executors.shutdown()
            assertTrue(executors.awaitTermination(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS))
        } finally {
            executors.shutdownNow()
        }

        assertTrue(
            "Expected no exceptions, but caught: $caughtExceptions",
            caughtExceptions.isEmpty(),
        )
        assertEquals(threadsCount, returnedInstances.size)
        assertEquals(1, returnedInstances.distinct().size)
        assertFalse(WorkManager.isInitialized())
        assertEquals(0, contextWithConfiguration.configCallCount.get())
        assertNull(initializationException)
    }

    @Test
    fun testGetInstance_withNullUserManager_whenUninitialized_throwsIllegalStateException() {
        val contextWithNullUserManager =
            object : ContextWrapper(realContext) {
                override fun getApplicationContext(): Context? = null

                override fun getSystemService(name: String): Any? =
                    if (Context.USER_SERVICE == name) null else super.getSystemService(name)
            }

        assertThrows(IllegalStateException::class.java) {
            WorkManager.getInstance(contextWithNullUserManager)
        }
        assertFalse(WorkManager.isInitialized())

        shadowUserManager.setUserUnlocked(true)
        val wm = WorkManager.getInstance(contextWithConfiguration)
        assertTrue(WorkManager.isInitialized())
        assertSame(configuration, wm.configuration)
    }
}
