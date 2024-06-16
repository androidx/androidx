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

package androidx.camera.core

import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.camera.core.RetryPolicy.DEFAULT
import androidx.camera.core.RetryPolicy.DEFAULT_RETRY_TIMEOUT_IN_MILLIS
import androidx.camera.core.RetryPolicy.ExecutionState
import androidx.camera.core.RetryPolicy.NEVER
import androidx.camera.core.RetryPolicy.RETRY_UNAVAILABLE_CAMERA
import androidx.camera.core.RetryPolicy.RetryConfig
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.CameraDeviceSurfaceManager
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraFactory.Provider
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.utils.ContextUtilTest
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeCameraFactory
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.internal.os.HandlerExecutor
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.shadows.ShadowVirtualDeviceManager
import org.robolectric.versioning.AndroidVersions

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@OptIn(ExperimentalCoroutinesApi::class)
class CameraXInitRetryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val handler = Handler(Looper.getMainLooper()) // Same to the looper of TestScope
    private val handlerExecutor = HandlerExecutor(handler)
    private lateinit var shadowPackageManager: ShadowPackageManager
    private var repeatingJob: Deferred<Unit>? = null

    @Before
    fun setUp() {
        // This test asserts both the type of the exception thrown, and the type of the cause of the
        // exception thrown in many cases. The Kotlin stacktrace recovery feature is useful for
        // debugging, but it inserts exceptions into the `cause` chain and interferes with this
        // test.
        System.setProperty("kotlinx.coroutines.stacktrace.recovery", false.toString())
        shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA, true)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_FRONT, true)
    }

    @After
    fun tearDown() {
        repeatingJob?.cancel()
    }

    @Test
    fun initializationSucceedsOnValidEnvironment() = runTest {
        // Arrange.
        val executionStateMutableList = mutableListOf<ExecutionState>()
        val policy = RetryPolicy { executionState: ExecutionState ->
            executionStateMutableList.add(executionState)
            return@RetryPolicy DEFAULT.onRetryDecisionRequested(executionState)
        }
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(
                    createCameraXConfig(
                        cameraFactory =
                            createFakeCameraFactory(frontCamera = true, backCamera = true)
                    )
                )
                .apply {
                    setCameraExecutor(handlerExecutor)
                    setSchedulerHandler(handler)
                    setCameraProviderInitRetryPolicy(policy)
                }

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        cameraX.initializeFuture.await()

        // Assert.
        assertThat(cameraX.isInitialized).isTrue()
        assertThat(executionStateMutableList).isEmpty()
    }

    @Test
    fun verifyInitSucceedsUsingDefaultRetryPolicy_OneCameraScenario() = runTest {
        // Arrange.
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(
                    createCameraXConfig(
                        cameraFactory =
                            createFakeCameraFactory(frontCamera = true, backCamera = false)
                    )
                )
                .setSchedulerHandler(handler)
                .setCameraExecutor(handlerExecutor)

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        cameraX.initializeFuture.await()

        // Assert.
        assertThat(cameraX.isInitialized).isTrue()
        cameraX.shutdown().get()
        assertThat(cameraX.isInitialized).isFalse()
    }

    @Test
    fun verifyInitFailureUsingDefaultRetryPolicy_NoCameraScenario() = runTest {
        // Arrange. Set up a simulated environment that no accessible cameras.
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(
                    createCameraXConfig(
                        cameraFactory =
                            createFakeCameraFactory(frontCamera = false, backCamera = false)
                    )
                )
                .setSchedulerHandler(handler)
                .setCameraExecutor(handlerExecutor)

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        val throwableSubject =
            assertThrows<InitializationException> { cameraX.initializeFuture.await() }

        // Assert.
        throwableSubject.hasCauseThat().isInstanceOf(CameraUnavailableException::class.java)
        assertThat(cameraX.isInitialized).isFalse()
        cameraX.shutdown().get()
    }

    @Test
    fun verifyImmediateFailureWithOptionNEVER() = runTest {
        // Arrange. Set up a simulated environment that no accessible cameras.
        var callCount = 0
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(
                    createCameraXConfig(
                        cameraFactory =
                            createFakeCameraFactory(frontCamera = false, backCamera = false)
                    )
                )
                .apply {
                    setCameraExecutor(handlerExecutor)
                    setSchedulerHandler(handler)
                    setCameraProviderInitRetryPolicy { executionState ->
                        callCount++
                        NEVER.onRetryDecisionRequested(executionState)
                    }
                }

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        assertThrows<InitializationException> { cameraX.initializeFuture.await() }

        // Assert.
        assertThat(cameraX.isInitialized).isFalse()
        assertThat(callCount).isAtMost(1)
    }

    @Test
    fun verifyImmediateFailureWithOptionRetryConfigNotRetry() = runTest {
        // Arrange. Set up a simulated environment that no accessible cameras.
        var callCount = 0
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(
                    createCameraXConfig(
                        cameraFactory =
                            createFakeCameraFactory(frontCamera = false, backCamera = false)
                    )
                )
                .apply {
                    setCameraExecutor(handlerExecutor)
                    setSchedulerHandler(handler)
                    setCameraProviderInitRetryPolicy {
                        callCount++
                        RetryConfig.Builder().setShouldRetry(false).build()
                    }
                }

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        assertThrows<InitializationException> { cameraX.initializeFuture.await() }

        // Assert.
        assertThat(cameraX.isInitialized).isFalse()
        assertThat(callCount).isAtMost(1)
    }

    @Test
    fun verifyInitFails_RetryCameraUnavailableMode_NoCameraScenario() = runTest {
        // Arrange. Set up a simulated environment that no accessible cameras.
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(createCameraXConfig()).apply {
                setCameraExecutor(handlerExecutor)
                setSchedulerHandler(handler)
                setCameraProviderInitRetryPolicy(RETRY_UNAVAILABLE_CAMERA)
            }

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        val throwableSubject =
            assertThrows<InitializationException> { cameraX.initializeFuture.await() }

        // Assert.
        throwableSubject.hasCauseThat().isInstanceOf(CameraUnavailableException::class.java)
        assertThat(cameraX.isInitialized).isFalse()
    }

    @Test
    fun verifyExecTimeNotExceedTimeout_RetryCamUnavailable_NoCameraScenario() = runTest {
        // Arrange. Set up a simulated environment that no accessible cameras.
        var executedTime = 0L
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(createCameraXConfig()).apply {
                setCameraExecutor(handlerExecutor)
                setSchedulerHandler(handler)
                setCameraProviderInitRetryPolicy { executionState: ExecutionState ->
                    RETRY_UNAVAILABLE_CAMERA.onRetryDecisionRequested(executionState).also {
                        executedTime = executionState.executedTimeInMillis
                    }
                }
            }

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        val throwableSubject =
            assertThrows<InitializationException> { cameraX.initializeFuture.await() }

        // Assert.
        throwableSubject.hasCauseThat().isInstanceOf(CameraUnavailableException::class.java)
        assertThat(cameraX.isInitialized).isFalse()
        assertThat(abs(DEFAULT_RETRY_TIMEOUT_IN_MILLIS - executedTime))
            .isLessThan(
                RetryConfig.DEFAULT_DELAY_RETRY.retryDelayInMillis + 100
                // Allow the tolerance for retry delay + 100ms potential processing time variations.
            )
    }

    @Test
    fun testTimeoutAdjustment_RetryCameraUnavailableMode() = runTest {
        testTimeoutAdjustment(RETRY_UNAVAILABLE_CAMERA)
    }

    @Test fun testTimeoutAdjustment_DefaultMode() = runTest { testTimeoutAdjustment(DEFAULT) }

    @Test
    fun testTimeoutAdjustment_CustomRetryPolicyMode() = runTest {
        // Arrange. Set up a RetryPolicy that persistently retries initialization attempts.
        val customAlwaysRetryPolicy = RetryPolicy { RetryConfig.MINI_DELAY_RETRY }

        // Act. & Assert. Confirm that retries cease if the total execution time surpasses the
        // defined timeout, preventing indefinite loops.
        testTimeoutAdjustment(customAlwaysRetryPolicy)
    }

    private suspend fun TestScope.testTimeoutAdjustment(policy: RetryPolicy) = coroutineScope {
        // Arrange. Set up a simulated environment that no accessible cameras.
        var executedTime = 0L
        val testCustomTimeout = 10000L
        val customTimeoutPolicy =
            RetryPolicy.Builder(policy).setTimeoutInMillis(testCustomTimeout).build()
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(createCameraXConfig()).apply {
                setCameraExecutor(handlerExecutor)
                setSchedulerHandler(handler)
                setCameraProviderInitRetryPolicy { executionState ->
                    customTimeoutPolicy.onRetryDecisionRequested(executionState).also {
                        executedTime = executionState.executedTimeInMillis
                    }
                }
            }

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        assertThrows<InitializationException> { cameraX.initializeFuture.await() }

        // Assert. Verify that initialization persists with retries until the total execution
        // time exhausts the allotted timeout.
        assertThat(abs(testCustomTimeout - executedTime))
            .isLessThan(
                RetryConfig.DEFAULT_DELAY_RETRY.retryDelayInMillis + 100
                // Allow the tolerance for retry delay + 100ms potential processing time variations.
            )
    }

    @Test
    fun verifyRetryDelayCustomization() = runTest {
        val desiredDelayTime = 900L

        assertThat(
                RetryConfig.Builder()
                    .setRetryDelayInMillis(desiredDelayTime)
                    .build()
                    .retryDelayInMillis
            )
            .isEqualTo(desiredDelayTime)
    }

    @Test
    fun verifyExecTimeNotExceedTimeout_CustomizedRetryPolicyOverrideGetTimeout_NoCameraScenario() =
        runTest {
            // Arrange. Set up a simulated environment that no accessible cameras.
            val timeoutInMs = 10000L
            val executionStateMutableList = mutableListOf<ExecutionState>()
            val policy =
                object : RetryPolicy {
                    override fun onRetryDecisionRequested(
                        executionState: ExecutionState
                    ): RetryConfig {
                        if (executionState.getExecutedTimeInMillis() < timeoutInMillis) {
                            executionStateMutableList.add(executionState)
                        }

                        return RetryConfig.DEFAULT_DELAY_RETRY
                    }

                    override fun getTimeoutInMillis(): Long {
                        return timeoutInMs
                    }
                }
            val configBuilder: CameraXConfig.Builder =
                CameraXConfig.Builder.fromConfig(createCameraXConfig()).apply {
                    setCameraExecutor(handlerExecutor)
                    setSchedulerHandler(handler)
                    setCameraProviderInitRetryPolicy(policy)
                }

            // Simulate the system time increases.
            repeatingJob = simulateSystemTimeIncrease()

            // Act.
            val cameraX = CameraX(context) { configBuilder.build() }
            assertThrows<InitializationException> { cameraX.initializeFuture.await() }

            // Assert. Confirm that initialization did not succeed.
            assertThat(cameraX.isInitialized).isFalse()

            // Assert. Verify that retry attempts occurred in sequential order.
            val numAttemptList =
                executionStateMutableList.map { executionState -> executionState.numOfAttempts }
            assertThat(numAttemptList).isInOrder()

            // Assert. Ensure all errors encountered were specifically due to camera unavailability.
            val statusList =
                executionStateMutableList.map { executionState -> executionState.status }.toSet()
            assertThat(statusList)
                .containsExactlyElementsIn(listOf(ExecutionState.STATUS_CAMERA_UNAVAILABLE))

            // Assert. Verify that the total execution time did not surpass the timeout limit.
            assertThat(executionStateMutableList.last().executedTimeInMillis)
                .isLessThan(timeoutInMs)
        }

    @Test
    fun verifyExecTimeNotExceedTimeout_CustomizedRetryPolicy_NoCameraScenario() = runTest {
        // Arrange. Set up a simulated environment that no accessible cameras.
        val timeoutInMs = 10000L
        val executionStateMutableList = mutableListOf<ExecutionState>()
        val policy = RetryPolicy { executionState ->
            if (executionState.getExecutedTimeInMillis() < timeoutInMs) {
                executionStateMutableList.add(executionState)
                return@RetryPolicy RetryConfig.DEFAULT_DELAY_RETRY
            }

            return@RetryPolicy RetryConfig.NOT_RETRY
        }
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(createCameraXConfig()).apply {
                setCameraExecutor(handlerExecutor)
                setSchedulerHandler(handler)
                setCameraProviderInitRetryPolicy(policy)
            }

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        assertThrows<InitializationException> { cameraX.initializeFuture.await() }

        // Assert. Confirm that initialization did not succeed.
        assertThat(cameraX.isInitialized).isFalse()

        // Assert. Verify that retry attempts occurred in sequential order.
        val numAttemptList =
            executionStateMutableList.map { executionState -> executionState.numOfAttempts }
        assertThat(numAttemptList).isInOrder()

        // Assert. Ensure all errors encountered were specifically due to camera unavailability.
        val statusList =
            executionStateMutableList.map { executionState -> executionState.status }.toSet()
        assertThat(statusList)
            .containsExactlyElementsIn(listOf(ExecutionState.STATUS_CAMERA_UNAVAILABLE))

        // Assert. Verify that the total execution time did not surpass the timeout limit.
        assertThat(executionStateMutableList.last().executedTimeInMillis).isLessThan(timeoutInMs)
    }

    @Test
    fun verifyMaxAttemptsAdhereToStopCriteria_CustomRetryPolicy() = runTest {
        // Arrange. Set up a simulated environment that no accessible cameras.
        val maxAttempts = 5
        val executionStateMutableList = mutableListOf<ExecutionState>()
        val policy = RetryPolicy { executionState: ExecutionState ->
            executionStateMutableList.add(executionState)
            if (executionState.numOfAttempts < maxAttempts) {
                return@RetryPolicy RetryConfig.DEFAULT_DELAY_RETRY
            }

            return@RetryPolicy RetryConfig.NOT_RETRY
        }
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(createCameraXConfig()).apply {
                setCameraExecutor(handlerExecutor)
                setSchedulerHandler(handler)
                setCameraProviderInitRetryPolicy(policy)
            }

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        assertThrows<InitializationException> { cameraX.initializeFuture.await() }

        // Assert. Confirm that initialization did not succeed.
        assertThat(cameraX.isInitialized).isFalse()
        // Assert. Confirm the maximum number of retry attempts was reached.
        assertThat(executionStateMutableList.last().numOfAttempts).isEqualTo(maxAttempts)
    }

    @Test
    fun testInitializationFailsPromptlyWithConfigurationError() = runTest {
        // Arrange.
        val resultList = mutableListOf<ExecutionState>()
        val policy = RetryPolicy { executionState: ExecutionState ->
            resultList.add(executionState)
            RETRY_UNAVAILABLE_CAMERA.onRetryDecisionRequested(executionState)
        }
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(
                    createCameraXConfig(surfaceManager = null, useCaseConfigFactory = null)
                )
                .apply {
                    setCameraExecutor(handlerExecutor)
                    setSchedulerHandler(handler)
                    setCameraProviderInitRetryPolicy(policy)
                }

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        assertThrows<InitializationException> { cameraX.initializeFuture.await() }

        // Assert.
        assertThat(resultList.size).isEqualTo(1)
        assertThat(resultList.last().status).isEqualTo(ExecutionState.STATUS_CONFIGURATION_FAIL)
    }

    @Test
    fun testInitializationFailsWithUnknownErrorDueToRuntimeException() = runTest {
        // Arrange. Create a CameraFactory simulation that throws RuntimeException on all API usage.
        val testException = RuntimeException("test")
        val executionStateMutableList = mutableListOf<ExecutionState>()
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(
                    createCameraXConfig(
                        cameraFactory =
                            object : CameraFactory {
                                override fun getCamera(cameraId: String): CameraInternal {
                                    throw testException
                                }

                                override fun getAvailableCameraIds(): MutableSet<String> {
                                    throw testException
                                }

                                override fun getCameraCoordinator(): CameraCoordinator {
                                    throw testException
                                }

                                override fun getCameraManager(): Any? {
                                    throw testException
                                }
                            }
                    )
                )
                .apply {
                    setCameraExecutor(handlerExecutor)
                    setSchedulerHandler(handler)
                    setCameraProviderInitRetryPolicy { executionState: ExecutionState ->
                        executionStateMutableList.add(executionState)
                        RetryConfig.NOT_RETRY
                    }
                }

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        assertThrows<InitializationException> { cameraX.initializeFuture.await() }

        // Assert.
        assertThat(executionStateMutableList.size).isEqualTo(1)
        assertThat(executionStateMutableList.last().status)
            .isEqualTo(ExecutionState.STATUS_UNKNOWN_ERROR)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, shadows = [TestShadowVDM::class])
    fun testIgnoreVirtualCameraValidation_WithAvailableDevices() = runTest {
        // Arrange.
        var callCount = 0
        val context =
            ContextUtilTest.FakeContext(
                "non-application",
                baseContext = context,
                deviceId = TEST_VDM_DEVICE_ID,
            )

        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(
                    createCameraXConfig(
                        cameraFactory =
                            createFakeCameraFactory(frontCamera = true, backCamera = false)
                    )
                )
                .setSchedulerHandler(handler)
                .setCameraExecutor(handlerExecutor)
                .setCameraProviderInitRetryPolicy {
                    callCount++
                    RetryConfig.Builder().setShouldRetry(false).build()
                }

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        cameraX.initializeFuture.await()

        // Assert.
        assertThat(cameraX.isInitialized).isTrue()
        assertThat(callCount).isEqualTo(0)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, shadows = [TestShadowVDM::class])
    fun testInitFailVirtualCameraValidation_NoAvailableDevices() = runTest {
        // Arrange. Set up a simulated environment that no accessible cameras.
        var callCount = 0
        val configBuilder: CameraXConfig.Builder =
            CameraXConfig.Builder.fromConfig(
                    createCameraXConfig(
                        cameraFactory =
                            createFakeCameraFactory(frontCamera = false, backCamera = false)
                    )
                )
                .setSchedulerHandler(handler)
                .setCameraExecutor(handlerExecutor)
                .setCameraProviderInitRetryPolicy {
                    callCount++
                    RetryConfig.Builder().setShouldRetry(false).build()
                }

        val context =
            ContextUtilTest.FakeContext(
                "non-application",
                baseContext = context,
                deviceId = TEST_VDM_DEVICE_ID,
            )

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        // Act.
        val cameraX = CameraX(context) { configBuilder.build() }
        val throwableSubject =
            assertThrows<InitializationException> { cameraX.initializeFuture.await() }

        // Assert.
        throwableSubject.hasCauseThat().isInstanceOf(CameraUnavailableException::class.java)
        assertThat(cameraX.isInitialized).isFalse()
        assertThat(callCount).isGreaterThan(0)
        cameraX.shutdown().get()
    }

    private fun createCameraXConfig(
        cameraFactory: CameraFactory = createFakeCameraFactory(),
        surfaceManager: CameraDeviceSurfaceManager? = FakeCameraDeviceSurfaceManager(),
        useCaseConfigFactory: UseCaseConfigFactory? = FakeUseCaseConfigFactory()
    ): CameraXConfig {
        val cameraFactoryProvider =
            Provider { _: Context?, _: CameraThreadConfig?, _: CameraSelector?, _: Long ->
                cameraFactory
            }
        return CameraXConfig.Builder()
            .setCameraFactoryProvider(cameraFactoryProvider)
            .apply {
                surfaceManager?.let {
                    setDeviceSurfaceManagerProvider { _: Context?, _: Any?, _: Set<String?>? -> it }
                }
                useCaseConfigFactory?.let { setUseCaseConfigFactoryProvider { _: Context? -> it } }
            }
            .build()
    }

    private fun createFakeCameraFactory(
        frontCamera: Boolean = false,
        backCamera: Boolean = false,
    ): CameraFactory =
        FakeCameraFactory(null).also { cameraFactory ->
            if (backCamera) {
                cameraFactory.insertCamera(CameraSelector.LENS_FACING_BACK, CAMERA_ID_0) {
                    FakeCamera(
                        CAMERA_ID_0,
                        null,
                        FakeCameraInfoInternal(CAMERA_ID_0, 0, CameraSelector.LENS_FACING_BACK)
                    )
                }
            }
            if (frontCamera) {
                cameraFactory.insertCamera(CameraSelector.LENS_FACING_FRONT, CAMERA_ID_1) {
                    FakeCamera(
                        CAMERA_ID_1,
                        null,
                        FakeCameraInfoInternal(CAMERA_ID_1, 0, CameraSelector.LENS_FACING_FRONT)
                    )
                }
            }
            cameraFactory.cameraCoordinator = FakeCameraCoordinator()
        }

    private fun TestScope.simulateSystemTimeIncrease() = async {
        val startTimeMs = SystemClock.elapsedRealtime()
        while (SystemClock.elapsedRealtime() - startTimeMs < 20000L) {
            shadowOf(handler.looper).idle()
            if (SystemClock.elapsedRealtime() < currentTime) {
                ShadowSystemClock.advanceBy(
                    currentTime - SystemClock.elapsedRealtime(),
                    TimeUnit.MILLISECONDS
                )
            }
            delay(FAKE_INIT_PROCESS_TIME_MS)
        }
    }

    @Implements(
        value = VirtualDeviceManager::class,
        minSdk = AndroidVersions.U.SDK_INT,
        isInAndroidSdk = false
    )
    class TestShadowVDM : ShadowVirtualDeviceManager() {
        @Implementation
        override fun isValidVirtualDeviceId(deviceId: Int): Boolean {
            if (deviceId == TEST_VDM_DEVICE_ID) {
                return true
            }
            return super.isValidVirtualDeviceId(deviceId)
        }
    }

    companion object {
        private const val CAMERA_ID_0 = "0"
        private const val CAMERA_ID_1 = "1"
        private const val FAKE_INIT_PROCESS_TIME_MS = 33L
        private const val TEST_VDM_DEVICE_ID = 2
    }
}
