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
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraIdentifier
import androidx.camera.core.CameraPresenceListener
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Preview
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.Observable
import androidx.camera.core.internal.StreamSpecsCalculator
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.SurfaceTextureProvider
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.lifecycle.Observer
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Device tests for verifying behavior when cameras are dynamically added or removed.
 *
 * This test uses a custom [ControllableCameraFactory] to simulate camera hot-plugging events and
 * verify that CameraX reacts correctly.
 */
@LargeTest
@RunWith(Parameterized::class)
class CameraAvailabilityDeviceTest(
    private val implName: String,
    private val baseConfig: CameraXConfig,
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName.contains(CameraPipeConfig::class.simpleName!!))

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
                add(arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()))
            }
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var factoryWrapper: FakeCameraFactoryWrapper

    @Before
    fun setUp() {
        fakeLifecycleOwner = FakeLifecycleOwner()
        fakeLifecycleOwner.startAndResume()
    }

    @After
    fun tearDown() {
        ProcessCameraProvider.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun cameraAdded_canBeOpenedAndUsed() {
        // Arrange: Ensure device has both front and back cameras for this test.
        assumeTrue("Device must have front and back cameras", deviceHasFrontAndBackCameras())
        val backCameraId = CameraUtil.getCameraIdWithLensFacing(CameraSelector.LENS_FACING_BACK)!!
        val frontCameraId = CameraUtil.getCameraIdWithLensFacing(CameraSelector.LENS_FACING_FRONT)!!
        val frontCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        // Arrange: Setup the wrapper to start with only the back camera visible.
        factoryWrapper =
            FakeCameraFactoryWrapper(
                baseConfig.getCameraFactoryProvider(null)!!,
                initialVisibleIds = setOf(backCameraId),
            )
        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraFactoryProvider(factoryWrapper)
                .build()

        // Act & Assert (Phase 1: Initial state)
        initializeProviderWithConfig(customConfig)
        assertThat(cameraProvider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)).isTrue()
        assertThat(cameraProvider!!.hasCamera(frontCameraSelector)).isFalse()

        // Arrange (Phase 2: Prepare to detect the new camera)
        val latch = CountDownLatch(1)
        cameraProvider!!.addCameraPresenceListener(
            Executors.newSingleThreadExecutor(),
            object : CameraPresenceListener {
                override fun onCamerasAdded(cameraIdentifiers: Set<CameraIdentifier>) {
                    if (cameraProvider!!.hasCamera(frontCameraSelector)) {
                        latch.countDown()
                    }
                }

                override fun onCamerasRemoved(cameraIdentifiers: Set<CameraIdentifier>) {
                    // No-op
                }
            },
        )
        val controllableFactory = factoryWrapper.controllableFactory!!
        // Act (Phase 2: Make the front camera available and notify CameraX)
        controllableFactory.setVisibleCameraIds(setOf(backCameraId, frontCameraId))

        // Assert (Phase 2: The provider detects the change)
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(cameraProvider!!.availableCameraInfos.size).isEqualTo(2)
        assertThat(cameraProvider!!.hasCamera(frontCameraSelector)).isTrue()

        // Assert (Phase 3: The newly added camera can be opened and used)
        val preview = Preview.Builder().build()
        val previewMonitor = PreviewMonitor()
        instrumentation.runOnMainSync {
            preview.surfaceProvider = previewMonitor.getSurfaceProvider()
            cameraProvider!!.bindToLifecycle(fakeLifecycleOwner, frontCameraSelector, preview)
        }
        previewMonitor.waitForStream()
    }

    @Test
    fun cameraRemoved_isNoLongerAvailableAndUsable() {
        // Arrange: Ensure device has both front and back cameras for this test.
        assumeTrue("Device must have front and back cameras", deviceHasFrontAndBackCameras())
        val backCameraId = CameraUtil.getCameraIdWithLensFacing(CameraSelector.LENS_FACING_BACK)!!
        val frontCameraId = CameraUtil.getCameraIdWithLensFacing(CameraSelector.LENS_FACING_FRONT)!!
        val frontCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        // Arrange: Start with both cameras visible
        factoryWrapper =
            FakeCameraFactoryWrapper(
                baseConfig.getCameraFactoryProvider(null)!!,
                initialVisibleIds = setOf(backCameraId, frontCameraId),
            )
        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraFactoryProvider(factoryWrapper)
                .build()
        initializeProviderWithConfig(customConfig)

        // Arrange: Bind to the front camera to get its Camera object and identifier
        lateinit var camera: Camera
        instrumentation.runOnMainSync {
            camera = cameraProvider!!.bindToLifecycle(fakeLifecycleOwner, frontCameraSelector)
        }
        val cameraIdentifier =
            cameraProvider!!
                .availableCameraInfos
                .first { it.lensFacing == CameraSelector.LENS_FACING_FRONT }
                .cameraIdentifier
        val cameraStateLatch = CountDownLatch(1)

        // Arrange: Listen for the camera state to become CLOSED after removal.
        instrumentation.runOnMainSync {
            camera.cameraInfo.cameraState.observe(
                fakeLifecycleOwner,
                object : Observer<CameraState> {
                    override fun onChanged(value: CameraState) {
                        if (
                            value.type == CameraState.Type.CLOSED &&
                                value.error?.code == CameraState.ERROR_CAMERA_REMOVED
                        ) {
                            cameraStateLatch.countDown()
                            camera.cameraInfo.cameraState.removeObserver(this)
                        }
                    }
                },
            )
        }

        // Arrange: Listen for the removal callback
        val removalLatch = CountDownLatch(1)
        cameraProvider!!.addCameraPresenceListener(
            Executors.newSingleThreadExecutor(),
            object : CameraPresenceListener {
                override fun onCamerasAdded(cameraIdentifiers: Set<CameraIdentifier>) {}

                override fun onCamerasRemoved(cameraIdentifiers: Set<CameraIdentifier>) {
                    if (cameraIdentifiers.contains(cameraIdentifier)) {
                        removalLatch.countDown()
                    }
                }
            },
        )
        val controllableFactory = factoryWrapper.controllableFactory!!

        // Act: Remove the front camera from the visible set
        controllableFactory.setVisibleCameraIds(setOf(backCameraId))

        // Assert: The removal was detected and the camera state was updated
        assertThat(removalLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(cameraStateLatch.await(5, TimeUnit.SECONDS)).isTrue()

        // Assert: The provider no longer reports the camera as available
        assertThat(cameraProvider!!.availableCameraInfos.size).isEqualTo(1)
        assertThat(cameraProvider!!.availableCameraInfos.map { it.cameraIdentifier })
            .doesNotContain(cameraIdentifier)
        assertThat(cameraProvider!!.hasCamera(frontCameraSelector)).isFalse()

        // Assert: Attempting to bind to the removed camera now throws an exception
        assertThrows(IllegalArgumentException::class.java) {
            instrumentation.runOnMainSync {
                cameraProvider!!.bindToLifecycle(fakeLifecycleOwner, frontCameraSelector)
            }
        }
    }

    @Test
    fun cameraReconnected_providesNewValidInstance() {
        // Arrange: Setup the factory and initialize the provider.
        val allCameraIds = CameraUtil.getBackwardCompatibleCameraIdListOrThrow()
        assumeTrue("Device must have at least one camera", allCameraIds.isNotEmpty())
        val cameraIdToTest = allCameraIds.first() // Dynamically pick the first camera

        factoryWrapper =
            FakeCameraFactoryWrapper(
                baseConfig.getCameraFactoryProvider(null)!!,
                initialVisibleIds = setOf(cameraIdToTest),
            )
        val customConfig =
            CameraXConfig.Builder.fromConfig(baseConfig)
                .setCameraFactoryProvider(factoryWrapper)
                .build()
        initializeProviderWithConfig(customConfig)
        val controllableFactory = factoryWrapper.controllableFactory!!

        // Arrange: Create a selector that specifically targets our chosen camera.
        val cameraIdentifierToTest = cameraProvider!!.availableCameraInfos.first().cameraIdentifier
        val specificCameraSelector =
            CameraSelector.Builder()
                .addCameraFilter { cameraInfos ->
                    cameraInfos.filter { it.cameraIdentifier == cameraIdentifierToTest }
                }
                .build()

        // === Phase 1: Bind to the camera for the first time ===
        lateinit var camera1: Camera
        instrumentation.runOnMainSync {
            camera1 = cameraProvider!!.bindToLifecycle(fakeLifecycleOwner, specificCameraSelector)
        }
        assertThat(camera1).isNotNull()
        // The rest of the test logic remains the same...
        val camera1Identifier = camera1.cameraInfo.cameraIdentifier

        // === Phase 2: Simulate unplugging the camera ===
        val removalLatch = CountDownLatch(1)
        cameraProvider!!.addCameraPresenceListener(
            Executors.newSingleThreadExecutor(),
            object : CameraPresenceListener {
                override fun onCamerasAdded(cameraIdentifiers: Set<CameraIdentifier>) {}

                override fun onCamerasRemoved(cameraIdentifiers: Set<CameraIdentifier>) {
                    if (cameraIdentifiers.contains(camera1Identifier)) {
                        removalLatch.countDown()
                    }
                }
            },
        )

        val camera1StateLatch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            camera1.cameraInfo.cameraState.observe(
                fakeLifecycleOwner,
                Observer<CameraState> { state ->
                    if (
                        state.type == CameraState.Type.CLOSED &&
                            state.error?.code == CameraState.ERROR_CAMERA_REMOVED
                    ) {
                        camera1StateLatch.countDown()
                    }
                },
            )
        }

        controllableFactory.setVisibleCameraIds(emptySet())
        assertThat(removalLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(camera1StateLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(cameraProvider!!.hasCamera(specificCameraSelector)).isFalse()

        // === Phase 3: Simulate plugging the camera back in ===
        val additionLatch = CountDownLatch(1)
        cameraProvider!!.addCameraPresenceListener(
            Executors.newSingleThreadExecutor(),
            object : CameraPresenceListener {
                override fun onCamerasAdded(cameraIdentifiers: Set<CameraIdentifier>) {
                    if (cameraIdentifiers.any { it.cameraIds == camera1Identifier?.cameraIds }) {
                        additionLatch.countDown()
                    }
                }

                override fun onCamerasRemoved(cameraIdentifiers: Set<CameraIdentifier>) {}
            },
        )

        controllableFactory.setVisibleCameraIds(setOf(cameraIdToTest))
        assertThat(additionLatch.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(cameraProvider!!.hasCamera(specificCameraSelector)).isTrue()

        // === Phase 4: Re-bind and verify a new, valid camera instance is returned ===
        lateinit var camera2: Camera
        val preview = Preview.Builder().build()
        val previewMonitor = PreviewMonitor()
        val camera2StateLatch = CountDownLatch(1)

        instrumentation.runOnMainSync {
            preview.surfaceProvider = previewMonitor.getSurfaceProvider()
            camera2 =
                cameraProvider!!.bindToLifecycle(
                    fakeLifecycleOwner,
                    specificCameraSelector,
                    preview,
                )

            camera2.cameraInfo.cameraState.observe(
                fakeLifecycleOwner,
                Observer<CameraState> { state ->
                    if (state.type == CameraState.Type.OPEN) {
                        camera2StateLatch.countDown()
                    }
                },
            )
        }

        assertThat(camera2).isNotNull()
        assertThat(camera2).isNotSameInstanceAs(camera1)
        assertThat(camera2StateLatch.await(5, TimeUnit.SECONDS)).isTrue()
        previewMonitor.waitForStream()
    }

    private fun initializeProviderWithConfig(config: CameraXConfig) {
        ProcessCameraProvider.configureInstance(config)
        cameraProvider = ProcessCameraProvider.getInstance(context).get(10, TimeUnit.SECONDS)
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
}

/** A [CameraFactory.Provider] that creates a [ControllableCameraFactory]. */
class FakeCameraFactoryWrapper(
    private val delegate: CameraFactory.Provider,
    private val initialVisibleIds: Set<String>? = null,
) : CameraFactory.Provider {

    @Volatile private var cachedControllableFactory: ControllableCameraFactory? = null
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
            cachedControllableFactory?.let {
                return it
            }

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

            cachedControllableFactory = newFactory
            return newFactory
        }
    }
}

/** A CameraFactory wrapper that allows dynamically controlling which cameras are visible. */
class ControllableCameraFactory(
    private val delegate: CameraFactory,
    initialVisibleIds: Set<String>,
) : CameraFactory by delegate {
    private val cameraPresenceSource =
        FakeObservable(initialVisibleIds.map { CameraIdentifier.Factory.create(it) })

    @Volatile private var visibleCameraIds: Set<String> = initialVisibleIds

    fun setVisibleCameraIds(ids: Set<String>) {
        visibleCameraIds = ids
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

/** A Fake Observable to control camera presence updates. */
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
        return Futures.immediateFuture(value)
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
        assertThat(frameReceivedSemaphore.tryAcquire(5, timeoutSeconds, TimeUnit.SECONDS)).isTrue()
    }
}
