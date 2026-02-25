/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.integration.core

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AE
import android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AF
import android.hardware.camera2.CameraCharacteristics.CONTROL_MAX_REGIONS_AWB
import android.os.Handler
import android.os.HandlerThread
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraIdentifier
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraUnavailableException
import androidx.camera.core.CameraXConfig
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.InitializationException
import androidx.camera.core.Preview
import androidx.camera.core.RetryPolicy
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.impl.CameraDeviceSurfaceManager
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.QuirkSettings
import androidx.camera.core.impl.QuirkSettingsHolder
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.StreamSpecsCalculator
import androidx.camera.core.internal.compat.quirk.ImageCaptureRotationOptionQuirk
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Device tests for verifying the options set in [CameraXConfig].
 *
 * This test uses a custom [CameraFactory.Provider] wrapper to intercept and verify the behavior of
 * different configurations.
 */
@LargeTest
@RunWith(Parameterized::class)
class CameraXConfigDeviceTest(private val implName: String, private val baseConfig: CameraXConfig) {
    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(baseConfig)
        )

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "implName={0}")
        fun data() =
            mutableListOf<Array<Any?>>().apply {
                add(arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()))
            }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    @Before
    fun setUp() {
        fakeLifecycleOwner = FakeLifecycleOwner()
        fakeLifecycleOwner.startAndResume()
    }

    @After
    fun tearDown() {
        ProcessCameraProvider.shutdown().get(10, TimeUnit.SECONDS)
        QuirkSettingsHolder.instance().reset()
    }

    @Test
    fun init_fails_withInvalidCameraFactoryProvider() {
        // Arrange: Create a config with a provider that will fail during initialization.
        val invalidConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraFactoryProvider(InvalidCameraFactoryProvider())
                .build()
        ProcessCameraProvider.configureInstance(invalidConfig)

        // Act & Assert
        val exception =
            assertThrows<ExecutionException> {
                ProcessCameraProvider.getInstance(context).get(10, TimeUnit.SECONDS)
            }
        exception.hasCauseThat().isInstanceOf(InitializationException::class.java)
    }

    @Test
    fun init_fails_whenCameraIsUnavailableDuringInit() {
        // Arrange: Get a real camera ID to make "unavailable".
        val allCameraIds = CameraUtil.getBackwardCompatibleCameraIdListOrThrow()
        assumeTrue("Device must have at least one camera", allCameraIds.isNotEmpty())
        val faultyCameraId = allCameraIds[0]

        // Arrange: Create a factory provider that will throw an exception for that ID.
        val faultyProvider =
            FaultyCameraFactoryProvider(baseConfig.getCameraFactoryProvider(null)!!, faultyCameraId)
        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraFactoryProvider(faultyProvider)
                .build()
        ProcessCameraProvider.configureInstance(customConfig)

        // Act & Assert: Initialization should fail with a wrapped CameraUnavailableException.
        val exception =
            assertThrows<ExecutionException> {
                ProcessCameraProvider.getInstance(context).get(10, TimeUnit.SECONDS)
            }
        exception.hasCauseThat().isInstanceOf(InitializationException::class.java)
    }

    @Test
    fun reconfigure_succeeds_afterInitWithInvalidConfigFails() {
        // --- Round 1: Configure with an invalid provider and fail ---
        val invalidConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraFactoryProvider(InvalidCameraFactoryProvider())
                .build()
        ProcessCameraProvider.configureInstance(invalidConfig)
        assertThrows<ExecutionException> {
            ProcessCameraProvider.getInstance(context).get(10, TimeUnit.SECONDS)
        }

        // --- Round 2: Reconfigure with a valid provider and succeed ---
        ProcessCameraProvider.shutdown().get(10, TimeUnit.SECONDS)
        ProcessCameraProvider.configureInstance(baseConfig)
        val provider = ProcessCameraProvider.getInstance(context).get(10, TimeUnit.SECONDS)

        // Assert: The second attempt succeeded.
        assertThat(provider).isNotNull()
        assertThat(provider.availableCameraInfos.isNotEmpty()).isTrue()
    }

    @Test
    fun quirkSettings_areAppliedToQuirkSettingsHolder() {
        // Arrange: Create a custom quirk setting.
        val customQuirk = ImageCaptureRotationOptionQuirk()
        val customQuirkSettings =
            QuirkSettings.Builder().forceEnableQuirks(setOf(customQuirk::class.java)).build()

        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setQuirkSettings(customQuirkSettings)
                .build()

        // Act: Initialize CameraX with the custom config.
        initializeProviderWithConfig(customConfig)

        // Assert: The global QuirkSettingsHolder now contains our custom quirk.
        val appliedSettings = QuirkSettingsHolder.instance().get()
        assertThat(appliedSettings.shouldEnableQuirk(customQuirk::class.java, true)).isTrue()
    }

    @Test
    fun repeatingStreamIsForcedByDefault_allowingFocusAndMetering() {
        val selector = CameraUtil.assumeFirstAvailableCameraSelector()
        assumeTrue(
            "No AF/AE/AWB region available on this device!",
            isFocusMeteringSupported(selector),
        )
        // Arrange: Use the default baseConfig where the repeating stream is forced.
        initializeProviderWithConfig(baseConfig)
        val imageCapture = ImageCapture.Builder().build()

        // Act: Bind a non-repeating use case and attempt to trigger focus/metering.
        lateinit var camera: Camera
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            camera = cameraProvider!!.bindToLifecycle(fakeLifecycleOwner, selector, imageCapture)
        }
        val factory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
        val point = factory.createPoint(0.5f, 0.5f)
        val action = FocusMeteringAction.Builder(point).build()
        assertThat(camera.cameraInfo.isFocusMeteringSupported(action)).isTrue()
        val future = camera.cameraControl.startFocusAndMetering(action)

        // Assert: The operation is not immediately canceled. We expect it to complete
        // (successfully or not, depending on the device) without an OperationCanceledException.
        try {
            future.get(10, TimeUnit.SECONDS)
        } catch (e: ExecutionException) {
            // Fails if the cause is an OperationCanceledException. Other causes are fine.
            assertThat(e.cause)
                .isNotInstanceOf(CameraControl.OperationCanceledException::class.java)
        }
    }

    @Test
    fun repeatingStreamIsNotForced_whenConfigIsSet() {
        // Arrange: Create a config that disables the forced repeating stream.
        val selector = CameraUtil.assumeFirstAvailableCameraSelector()
        assumeTrue(
            "No AF/AE/AWB region available on this device!",
            isFocusMeteringSupported(selector),
        )
        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig).setRepeatingStreamForced(false).build()
        initializeProviderWithConfig(customConfig)
        val imageCapture = ImageCapture.Builder().build()

        // Act: Bind a non-repeating use case and attempt to trigger focus/metering.
        lateinit var camera: Camera
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            camera = cameraProvider!!.bindToLifecycle(fakeLifecycleOwner, selector, imageCapture)
        }

        val factory = SurfaceOrientedMeteringPointFactory(1.0f, 1.0f)
        val point = factory.createPoint(0.5f, 0.5f)
        val action = FocusMeteringAction.Builder(point).build()
        val future = camera.cameraControl.startFocusAndMetering(action)

        // Assert: The future should fail with an OperationCanceledException because there is
        // no repeating stream available to handle the request.
        val exception = assertThrows<ExecutionException> { future.get(5, TimeUnit.SECONDS) }
        exception.hasCauseThat().isInstanceOf(CameraControl.OperationCanceledException::class.java)
    }

    @Test
    fun init_failsAndRetainsConfig_whenNoCamerasAvailable() = runTest {
        // Arrange: A factory that reports no cameras, which will cause validation to fail.
        val factoryWrapper =
            FakeCameraFactoryWrapper(
                baseConfig.getCameraFactoryProvider(null)!!,
                initialVisibleIds = emptySet(),
            )
        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraFactoryProvider(factoryWrapper)
                .build()
        ProcessCameraProvider.configureInstance(customConfig)

        // Act & Assert: The first getInstance call should fail.
        assertThrows<InitializationException> { ProcessCameraProvider.getInstance(context).await() }

        // Assert: The second getInstance call should also fail, proving the configuration
        // was retained without needing to call configureInstance() again.
        assertThrows<InitializationException> { ProcessCameraProvider.getInstance(context).await() }
    }

    @Test
    fun reinitialization_succeeds_withoutWaitingForShutdown() {
        // Arrange: Initialize a first instance of the provider.
        ProcessCameraProvider.configureInstance(baseConfig)
        val provider1 = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        assertThat(provider1).isNotNull()

        // Act: Call shutdown but DO NOT wait for it to complete.
        val shutdownFuture = provider1.shutdownAsync()

        // Act: Immediately reconfigure with a verifiable provider and re-initialize.
        val originalFactoryProvider = baseConfig.getCameraFactoryProvider(null)!!
        val verifiableProvider = VerifiableCameraFactoryProvider(originalFactoryProvider)
        val newConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraFactoryProvider(verifiableProvider)
                .build()

        ProcessCameraProvider.configureInstance(newConfig)
        val provider2 = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]

        // Assert: The new provider instance is successfully initialized and is a different object.
        assertThat(provider2).isNotNull()

        // Assert: The new configuration was used because our verifiable provider was called.
        assertThat(verifiableProvider.isNewInstanceCalled).isTrue()

        // Assert: The original shutdown future should also complete successfully without errors.
        shutdownFuture.get(10, TimeUnit.SECONDS)
    }

    @Test
    fun reconfigure_canBeDoneAfterShutdown() {
        // --- Round 1: Configure with a verifiable provider and get instance ---
        val originalProvider = baseConfig.getCameraFactoryProvider(null)!!
        val verifiableProvider1 = VerifiableCameraFactoryProvider(originalProvider)
        val config1 =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraFactoryProvider(verifiableProvider1)
                .build()

        ProcessCameraProvider.configureInstance(config1)
        val provider1 = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]

        // Assert that the first configuration was used.
        assertThat(verifiableProvider1.isNewInstanceCalled).isTrue()

        // --- Shutdown ---
        provider1.shutdownAsync()[10, TimeUnit.SECONDS]

        // --- Round 2: Reconfigure with a new verifiable provider and get instance ---
        val verifiableProvider2 = VerifiableCameraFactoryProvider(originalProvider)
        val config2 =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraFactoryProvider(verifiableProvider2)
                .build()

        ProcessCameraProvider.configureInstance(config2)
        ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]

        // Assert that the second configuration was used.
        assertThat(verifiableProvider2.isNewInstanceCalled).isTrue()
    }

    @Test
    fun availableCamerasLimiter_limitsAvailableCameras() {
        // Arrange
        assumeTrue(deviceHasFrontCamera())
        val limiter = CameraSelector.DEFAULT_FRONT_CAMERA
        val originalFactoryProvider = baseConfig.getCameraFactoryProvider(null)!!
        val factoryWrapper = FakeCameraFactoryWrapper(originalFactoryProvider)

        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setAvailableCamerasLimiter(limiter)
                .setCameraFactoryProvider(factoryWrapper)
                .build()

        // Act
        initializeProviderWithConfig(customConfig)

        // Assert
        val availableCameras = cameraProvider!!.availableCameraInfos
        assertThat(availableCameras.isNotEmpty()).isTrue()
        availableCameras.forEach {
            assertThat(it.lensFacing).isEqualTo(CameraSelector.LENS_FACING_FRONT)
        }
    }

    @Test
    fun customExecutorAndScheduler_canBindUseCase() {
        // Arrange
        val handlerThread = HandlerThread("CustomScheduler").apply { start() }
        val customSchedulerHandler = Handler(handlerThread.looper)
        val customCameraExecutor = Executors.newSingleThreadExecutor()
        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraExecutor(customCameraExecutor)
                .setSchedulerHandler(customSchedulerHandler)
                .build()

        // Act
        initializeProviderWithConfig(customConfig)

        // Assert
        bindPreviewAndVerify()

        // Cleanup
        cameraProvider?.shutdownAsync()?.get(10, TimeUnit.SECONDS)
        handlerThread.quitSafely()
    }

    @Test
    fun directExecutorAndCustomScheduler_canBindUseCase() {
        // TODO(b/439976984): Enable this test for CameraPipe when the issue is resolved.
        assumeFalse(
            "CameraPipe fails with directExecutor (b/439976984)",
            implName == Camera2Config::class.simpleName,
        )

        // Arrange
        val handlerThread = HandlerThread("CustomScheduler").apply { start() }
        val customSchedulerHandler = Handler(handlerThread.looper)
        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraExecutor(CameraXExecutors.directExecutor())
                .setSchedulerHandler(customSchedulerHandler)
                .build()

        // Act
        initializeProviderWithConfig(customConfig)

        // Assert
        bindPreviewAndVerify()

        // Cleanup
        cameraProvider?.shutdownAsync()?.get(10, TimeUnit.SECONDS)
        handlerThread.quitSafely()
    }

    @Test
    fun retryPolicy_succeedsWhenCamerasBecomeAvailable() {
        // Arrange: Check if device is expected to have front/back cameras.
        assumeTrue(
            "Device must report FEATURE_CAMERA and FEATURE_CAMERA_FRONT",
            deviceHasFrontAndBackCameras(),
        )

        val allCameraIds = CameraUtil.getBackwardCompatibleCameraIdListOrThrow().toSet()
        val retryAttemptToSucceed = 2
        val factoryWrapper =
            FakeCameraFactoryWrapper(
                baseConfig.getCameraFactoryProvider(null)!!,
                // Start with an initial state of NO cameras available.
                initialVisibleIds = emptySet(),
            )

        // Arrange: A policy that "fixes" the camera availability on the 2nd retry attempt.
        val customPolicy = RetryPolicy { executionState ->
            if (executionState.numOfAttempts >= retryAttemptToSucceed) {
                // On the 2nd attempt, restore all cameras to the controllable factory.
                factoryWrapper.controllableFactory?.setVisibleCameraIds(allCameraIds)
            }
            // Always ask to retry until it succeeds.
            RetryPolicy.RetryConfig.DEFAULT_DELAY_RETRY
        }

        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraProviderInitRetryPolicy(customPolicy)
                .setCameraFactoryProvider(factoryWrapper)
                .build()

        // Act: Initialize. This will hang and retry until our policy "fixes" the camera list.
        initializeProviderWithConfig(customConfig)

        // Assert: Initialization succeeded and all cameras are now available.
        assertThat(cameraProvider).isNotNull()
        assertThat(cameraProvider!!.availableCameraInfos.size).isEqualTo(allCameraIds.size)
        assertThat(cameraProvider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)).isTrue()
        assertThat(cameraProvider!!.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)).isTrue()
    }

    @Test
    fun customCameraFactoryProvider_isUsedByCameraX() {
        // Arrange: Create a verifiable provider that delegates to the real one.
        val originalFactoryProvider = baseConfig.getCameraFactoryProvider(null)!!
        val verifiableProvider = VerifiableCameraFactoryProvider(originalFactoryProvider)

        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraFactoryProvider(verifiableProvider)
                .build()

        // Act: Initialize CameraX with the custom provider.
        initializeProviderWithConfig(customConfig)

        // Assert: The custom provider's newInstance method was called.
        assertThat(verifiableProvider.isNewInstanceCalled).isTrue()

        // Assert: CameraX still initialized correctly.
        assertThat(cameraProvider!!.availableCameraInfos.size).isGreaterThan(0)
        if (deviceHasBackCamera()) {
            assertThat(cameraProvider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)).isTrue()
        }
        if (deviceHasFrontCamera()) {
            assertThat(cameraProvider!!.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)).isTrue()
        }
    }

    @Test
    fun customSurfaceManagerProvider_isUsedByCameraX() {
        // Arrange: Create a verifiable provider that delegates to the real one.
        val originalProvider = baseConfig.getDeviceSurfaceManagerProvider(null)!!
        val verifiableProvider = VerifiableSurfaceManagerProvider(originalProvider)

        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setDeviceSurfaceManagerProvider(verifiableProvider)
                .build()

        // Act: Initialize CameraX with the custom provider.
        initializeProviderWithConfig(customConfig)

        // Assert: The custom provider's newInstance method was called.
        assertThat(verifiableProvider.isNewInstanceCalled).isTrue()

        // Assert: CameraX still initialized correctly.
        assumeTrue(deviceHasBackCamera())
        assertThat(cameraProvider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)).isTrue()
    }

    @Test
    fun customUseCaseConfigFactoryProvider_isUsedByCameraX() {
        // Arrange: Create a verifiable provider that delegates to the real one.
        val originalProvider = baseConfig.getUseCaseConfigFactoryProvider(null)!!
        val verifiableProvider = VerifiableUseCaseConfigFactoryProvider(originalProvider)

        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setUseCaseConfigFactoryProvider(verifiableProvider)
                .build()

        // Act: Initialize CameraX with the custom provider.
        initializeProviderWithConfig(customConfig)

        // Assert: The custom provider's newInstance method was called.
        assertThat(verifiableProvider.isNewInstanceCalled).isTrue()

        // Assert: CameraX still initialized correctly.
        assumeTrue(deviceHasBackCamera())
        assertThat(cameraProvider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)).isTrue()
    }

    private fun initializeProviderWithConfig(config: CameraXConfig) {
        ProcessCameraProvider.configureInstance(config)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
    }

    private fun bindPreviewAndVerify() {
        val selector = CameraUtil.assumeFirstAvailableCameraSelector()
        val preview = Preview.Builder().build()
        val previewMonitor = PreviewMonitor()

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = previewMonitor.getSurfaceProvider()
            cameraProvider!!.bindToLifecycle(fakeLifecycleOwner, selector, preview)
        }
        previewMonitor.waitForStream()
    }

    private fun deviceHasFrontCamera(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
    }

    private fun deviceHasBackCamera(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    private fun deviceHasFrontAndBackCameras(): Boolean {
        return deviceHasFrontCamera() && deviceHasBackCamera()
    }

    private fun isFocusMeteringSupported(selector: CameraSelector): Boolean {
        return try {
            val cameraCharacteristics = CameraUtil.getCameraCharacteristics(selector.lensFacing!!)
            cameraCharacteristics?.run {
                // Return true if any of the metering regions are supported
                ((get(CONTROL_MAX_REGIONS_AF) ?: 0) > 0) ||
                    ((get(CONTROL_MAX_REGIONS_AE) ?: 0) > 0) ||
                    ((get(CONTROL_MAX_REGIONS_AWB) ?: 0) > 0)
            } ?: false // If characteristics are null, return false
        } catch (_: Exception) {
            false // If any exception occurs, assume not supported
        }
    }

    /**
     * A [CameraFactory.Provider] that creates a [ControllableCameraFactory].
     *
     * @param delegate The actual [CameraFactory.Provider] to be wrapped.
     * @param initialVisibleIds The set of camera IDs that the created factory should expose
     *   initially. If null, all cameras from the delegate are visible.
     */
    class FakeCameraFactoryWrapper(
        private val delegate: CameraFactory.Provider,
        private val initialVisibleIds: Set<String>? = null,
    ) : CameraFactory.Provider {

        private var cachedControllableFactory: ControllableCameraFactory? = null
        private val lock = Any()

        val controllableFactory: ControllableCameraFactory?
            get() = cachedControllableFactory

        override fun newInstance(
            context: Context,
            threadConfig: CameraThreadConfig,
            availableCamerasLimiter: CameraSelector?,
            cameraOpenRetryMaxTimeoutInMs: Long,
            cameraXConfig: CameraXConfig?,
            streamSpecsCalculator: StreamSpecsCalculator,
        ): CameraFactory {
            synchronized(lock) {
                // If we have already created and cached an instance, return it immediately.
                cachedControllableFactory?.let {
                    return it
                }

                // Otherwise, create it for the first time.
                val realFactory =
                    delegate.newInstance(
                        context,
                        threadConfig,
                        availableCamerasLimiter,
                        cameraOpenRetryMaxTimeoutInMs,
                        cameraXConfig,
                        streamSpecsCalculator,
                    )
                val initialIds = initialVisibleIds ?: realFactory.availableCameraIds
                val newFactory = ControllableCameraFactory(realFactory, initialIds)

                // Cache it before returning.
                cachedControllableFactory = newFactory
                return newFactory
            }
        }
    }

    /** A CameraFactory.Provider that always fails to create a factory. */
    private class InvalidCameraFactoryProvider : CameraFactory.Provider {
        override fun newInstance(
            context: Context,
            threadConfig: CameraThreadConfig,
            availableCamerasLimiter: CameraSelector?,
            cameraOpenRetryMaxTimeoutInMs: Long,
            cameraXConfig: CameraXConfig?,
            streamSpecsCalculator: StreamSpecsCalculator,
        ): CameraFactory {
            // Simulate a failure during factory creation.
            throw InitializationException(RuntimeException("Test: Invalid provider"))
        }
    }

    /** A wrapper to create a [FaultyCameraFactory] that throws on getCamera(). */
    private class FaultyCameraFactoryProvider(
        private val delegate: CameraFactory.Provider,
        private val faultyCameraId: String,
    ) : CameraFactory.Provider {
        override fun newInstance(
            context: Context,
            threadConfig: CameraThreadConfig,
            availableCamerasLimiter: CameraSelector?,
            cameraOpenRetryMaxTimeoutInMs: Long,
            cameraXConfig: CameraXConfig?,
            streamSpecsCalculator: StreamSpecsCalculator,
        ): CameraFactory {
            val realFactory =
                delegate.newInstance(
                    context,
                    threadConfig,
                    availableCamerasLimiter,
                    cameraOpenRetryMaxTimeoutInMs,
                    cameraXConfig,
                    streamSpecsCalculator,
                )
            // Return the wrapper that injects the failure.
            return FaultyCameraFactory(realFactory, faultyCameraId)
        }
    }

    /** A CameraFactory wrapper that throws an exception for a specific camera ID. */
    private class FaultyCameraFactory(
        private val delegate: CameraFactory,
        private val faultyCameraId: String,
    ) : CameraFactory by delegate {
        override fun getCamera(cameraId: String): CameraInternal {
            if (cameraId == faultyCameraId) {
                // Simulate a failure to access this specific camera's characteristics/info.
                throw CameraUnavailableException(
                    CameraUnavailableException.CAMERA_ERROR,
                    "Test Exception: Camera $cameraId is faulty.",
                )
            }
            return delegate.getCamera(cameraId)
        }

        override fun shutdown() {
            delegate.shutdown()
        }
    }

    /**
     * A CameraFactory wrapper that allows dynamically controlling which cameras are visible.
     *
     * @param delegate The real CameraFactory instance.
     * @param initialVisibleIds The initial set of camera IDs this factory will report.
     */
    class ControllableCameraFactory(
        private val delegate: CameraFactory,
        initialVisibleIds: Set<String>,
    ) : CameraFactory by delegate {
        private val cameraPresenceSource =
            FakeObservable(initialVisibleIds.map { CameraIdentifier.Factory.create(it) })

        @Volatile private var visibleCameraIds: Set<String> = initialVisibleIds

        fun setVisibleCameraIds(ids: Set<String>) {
            visibleCameraIds = ids
            // When the visible cameras change, push an update through our fake observable.
            cameraPresenceSource.setValue(ids.map { CameraIdentifier.Factory.create(it) })
        }

        override fun getAvailableCameraIds(): Set<String> = visibleCameraIds

        override fun getCamera(cameraId: String): CameraInternal {
            if (!visibleCameraIds.contains(cameraId)) {
                throw IllegalArgumentException("Camera $cameraId is not in the visible set.")
            }
            return delegate.getCamera(cameraId)
        }

        override fun getCameraPresenceSource(): Observable<List<CameraIdentifier>> {
            return cameraPresenceSource
        }

        override fun shutdown() {
            delegate.shutdown()
        }
    }

    /**
     * A wrapper for a CameraFactory.Provider that records whether its newInstance method has been
     * called.
     *
     * @param delegate The actual [CameraFactory.Provider] to delegate to after recording the call.
     */
    private class VerifiableCameraFactoryProvider(private val delegate: CameraFactory.Provider) :
        CameraFactory.Provider {

        @Volatile
        var isNewInstanceCalled = false
            private set

        override fun newInstance(
            context: Context,
            threadConfig: CameraThreadConfig,
            availableCamerasLimiter: CameraSelector?,
            cameraOpenRetryMaxTimeoutInMs: Long,
            cameraXConfig: CameraXConfig?,
            streamSpecsCalculator: StreamSpecsCalculator,
        ): CameraFactory {
            // Record that this custom provider was used.
            isNewInstanceCalled = true
            // Delegate to the original provider to ensure CameraX can initialize correctly.
            return delegate.newInstance(
                context,
                threadConfig,
                availableCamerasLimiter,
                cameraOpenRetryMaxTimeoutInMs,
                cameraXConfig,
                streamSpecsCalculator,
            )
        }
    }

    /**
     * A wrapper for a CameraDeviceSurfaceManager.Provider that records whether its newInstance
     * method has been called.
     */
    private class VerifiableSurfaceManagerProvider(
        private val delegate: CameraDeviceSurfaceManager.Provider
    ) : CameraDeviceSurfaceManager.Provider {

        @Volatile
        var isNewInstanceCalled = false
            private set

        override fun newInstance(
            context: Context,
            cameraManager: Any?,
            availableCameraIds: Set<String>,
        ): CameraDeviceSurfaceManager {
            isNewInstanceCalled = true
            return delegate.newInstance(context, cameraManager, availableCameraIds)
        }
    }

    /**
     * A wrapper for a UseCaseConfigFactory.Provider that records whether its newInstance method has
     * been called.
     */
    private class VerifiableUseCaseConfigFactoryProvider(
        private val delegate: UseCaseConfigFactory.Provider
    ) : UseCaseConfigFactory.Provider {

        @Volatile
        var isNewInstanceCalled = false
            private set

        override fun newInstance(context: Context): UseCaseConfigFactory {
            isNewInstanceCalled = true
            return delegate.newInstance(context)
        }
    }

    private class FakeObservable<T>(initialValue: T) : Observable<T> {
        private val observers = CopyOnWriteArrayList<Observable.Observer<T>>()
        private var value: T = initialValue

        fun setValue(newValue: T) {
            value = newValue
            for (observer in observers) {
                observer.onNewData(value)
            }
        }

        override fun fetchData(): ListenableFuture<T> {
            for (observer in observers) {
                observer.onNewData(value)
            }
            return Futures.immediateFuture<T>(value)
        }

        override fun addObserver(executor: Executor, observer: Observable.Observer<in T>) {
            @Suppress("UNCHECKED_CAST") observers.add(observer as Observable.Observer<T>)
            executor.execute { observer.onNewData(value) }
        }

        override fun removeObserver(observer: Observable.Observer<in T>) {
            @Suppress("UNCHECKED_CAST") observers.remove(observer as Observable.Observer<T>)
        }
    }

    /** A simple monitor to verify that preview frames are being produced. */
    class PreviewMonitor {
        private val frameReceivedSemaphore = Semaphore(0)
        private val surfaceProvider =
            SurfaceTextureProvider.createAutoDrainingSurfaceTextureProvider {
                frameReceivedSemaphore.release()
            }

        fun getSurfaceProvider(): Preview.SurfaceProvider = surfaceProvider

        fun waitForStream(timeoutSeconds: Long = 5) {
            assertThat(frameReceivedSemaphore.tryAcquire(5, timeoutSeconds, TimeUnit.SECONDS))
                .isTrue()
        }
    }
}
