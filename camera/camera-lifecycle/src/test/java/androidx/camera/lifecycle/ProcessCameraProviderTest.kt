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

package androidx.camera.lifecycle

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.InitializationException
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.CameraDeviceSurfaceManager
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraFactory.Provider
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore
import androidx.camera.core.impl.ForwardingCameraInfo
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeCameraFactory
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.concurrent.futures.await
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.internal.os.HandlerExecutor
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.shadows.ShadowSystemClock

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
@OptIn(ExperimentalCoroutinesApi::class)
class ProcessCameraProviderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val handler = Handler(Looper.getMainLooper()) // Same to the looper of TestScope
    private val handlerExecutor = HandlerExecutor(handler)
    private lateinit var shadowPackageManager: ShadowPackageManager
    private var repeatingJob: Deferred<Unit>? = null
    private lateinit var provider: ProcessCameraProvider
    private val lifecycleOwner0 = FakeLifecycleOwner()

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
    fun tearDown() = runTest {
        repeatingJob?.cancel()
        try {
            ProcessCameraProvider.shutdown().await()
        } catch (_: IllegalStateException) {
            // ProcessCameraProvider may not be configured. Ignore.
        }
    }

    @Test
    fun processCameraProviderFail_retainCameraConfig() = runTest {
        // Arrange
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
                }

        // Simulate the system time increases.
        repeatingJob = simulateSystemTimeIncrease()

        ProcessCameraProvider.configureInstance(configBuilder.build())

        // Act
        assertThrows<InitializationException> { ProcessCameraProvider.getInstance(context).await() }

        // Assert
        // When retrying ProcessCameraProvider#getInstance, it should be able to try without calling
        // configureInstance again.
        assertThrows<InitializationException> { ProcessCameraProvider.getInstance(context).await() }
    }

    @Test
    fun bindUseCasesOrSessionConfig_withNotExistedLensFacingCamera() = runTest {
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA, true)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_CAMERA_FRONT, false)

        val cameraFactoryProvider = Provider { _, _, _, _, _, _ ->
            val cameraFactory = FakeCameraFactory()
            cameraFactory.insertCamera(LENS_FACING_BACK, "0") {
                FakeCamera("0", null, FakeCameraInfoInternal("0", 0, LENS_FACING_BACK))
            }
            cameraFactory.cameraCoordinator = FakeCameraCoordinator()
            cameraFactory
        }

        val appConfigBuilder =
            CameraXConfig.Builder()
                .setCameraFactoryProvider(cameraFactoryProvider)
                .setDeviceSurfaceManagerProvider { _, _, _, _ -> FakeCameraDeviceSurfaceManager() }
                .setUseCaseConfigFactoryProvider { _, _ -> FakeUseCaseConfigFactory() }

        ProcessCameraProvider.configureInstance(appConfigBuilder.build())

        provider = ProcessCameraProvider.getInstance(context).await()

        val useCase = Preview.Builder().build()

        // The front camera is not defined, we should get the IllegalArgumentException when it
        // tries to get the camera.
        assertThrows<IllegalArgumentException> {
            provider.bindToLifecycle(lifecycleOwner0, CameraSelector.DEFAULT_FRONT_CAMERA, useCase)
        }

        val sessionConfig = SessionConfig(useCases = listOf(useCase))
        assertThrows<IllegalArgumentException> {
            provider.bindToLifecycle(
                lifecycleOwner0,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                sessionConfig,
            )
        }

        assertThat(provider.isConcurrentCameraModeOn).isFalse()
    }

    @Test
    fun getAvailableCameraInfos_usesFilteredCameras() = runBlocking {
        ProcessCameraProvider.configureInstance(
            FakeAppConfig.create(CameraSelector.DEFAULT_BACK_CAMERA)
        )
        provider = ProcessCameraProvider.getInstance(context).await()

        val cameraInfos = provider.availableCameraInfos
        assertThat(cameraInfos.size).isEqualTo(1)

        val cameraInfo = cameraInfos.first() as FakeCameraInfoInternal
        assertThat(cameraInfo.lensFacing).isEqualTo(LENS_FACING_BACK)
    }

    @Test
    fun getCameraInfo_returnsCorrectCameraInfo() = runTest {
        // Arrange
        ProcessCameraProvider.configureInstance(
            createCameraXConfig(
                cameraFactory = createFakeCameraFactory(frontCamera = true, backCamera = true)
            )
        )
        provider = ProcessCameraProvider.getInstance(context).await()

        // Act & Assert
        val backCameraInfo = provider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)
        assertThat(backCameraInfo.lensFacing).isEqualTo(CameraSelector.LENS_FACING_BACK)

        val frontCameraInfo = provider.getCameraInfo(CameraSelector.DEFAULT_FRONT_CAMERA)
        assertThat(frontCameraInfo.lensFacing).isEqualTo(CameraSelector.LENS_FACING_FRONT)
    }

    @Test
    fun getCameraInfoWithSessionConfig_returnsCorrectCameraInfo() = runTest {
        // Arrange
        ProcessCameraProvider.configureInstance(
            createCameraXConfig(
                cameraFactory =
                    createFakeCameraFactory(
                        backCamera = true,
                        anotherBackCamera = true,
                        frontCamera = true,
                    )
            )
        )
        provider = ProcessCameraProvider.getInstance(context).await()
        val sessionConfig =
            CustomSessionConfig(
                CameraFilter { cameraInfos ->
                    cameraInfos.filter { (it as CameraInfoInternal).cameraId == CAMERA_ID_2 }
                }
            )

        // Act & Assert
        val backCameraInfo =
            provider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA, sessionConfig)
        assertThat((backCameraInfo as CameraInfoInternal).cameraId).isEqualTo(CAMERA_ID_2)
    }

    @Test
    fun getCameraInfoWithSessionConfig_throwsExceptionForUnavailableCamera() = runTest {
        // Arrange
        ProcessCameraProvider.configureInstance(
            createCameraXConfig(
                cameraFactory =
                    createFakeCameraFactory(
                        backCamera = true,
                        anotherBackCamera = true,
                        frontCamera = true,
                    )
            )
        )
        provider = ProcessCameraProvider.getInstance(context).await()
        val sessionConfig =
            CustomSessionConfig(
                CameraFilter { cameraInfos ->
                    // Filter for a camera that doesn't exist
                    cameraInfos.filter { (it as CameraInfoInternal).cameraId == "non-existent-id" }
                }
            )

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            provider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA, sessionConfig)
        }
    }

    @Test
    fun getSupportedLensCategories_returnsCorrectCategories() = runTest {
        provider = setupMultiCameraProvider()

        val backCategories = provider.getSupportedLensCategories(CameraSelector.LENS_FACING_BACK)
        assertThat(backCategories)
            .containsExactly(
                CameraSelector.LENS_CATEGORY_DEFAULT,
                CameraSelector.LENS_CATEGORY_ULTRA_WIDE,
                CameraSelector.LENS_CATEGORY_TELEPHOTO,
                CameraSelector.LENS_CATEGORY_WIDEST_FOV,
                CameraSelector.LENS_CATEGORY_NARROWEST_FOV,
            )

        val frontCategories = provider.getSupportedLensCategories(CameraSelector.LENS_FACING_FRONT)
        assertThat(frontCategories)
            .containsExactly(
                CameraSelector.LENS_CATEGORY_DEFAULT,
                CameraSelector.LENS_CATEGORY_WIDEST_FOV,
                CameraSelector.LENS_CATEGORY_NARROWEST_FOV,
            )

        // Verify the returned list is unmodifiable
        assertThrows<UnsupportedOperationException> { (backCategories as MutableList<Int>).add(99) }
    }

    @Test
    fun getCameraInfo_withLensCategories_resolvesCorrectPhysicalCamera() = runTest {
        provider = setupMultiCameraProvider()

        // Act & Assert - Ultra-Wide
        val ultraWideSelector = createSelector(category = CameraSelector.LENS_CATEGORY_ULTRA_WIDE)
        val ultraWideInfo = provider.getCameraInfo(ultraWideSelector)
        assertThat((ultraWideInfo as AdapterCameraInfo).physicalCameraId).isEqualTo("1")
        assertThat(ultraWideInfo.intrinsicZoomRatio).isEqualTo(0.5f)

        // Act & Assert - Telephoto
        val teleSelector = createSelector(category = CameraSelector.LENS_CATEGORY_TELEPHOTO)
        val teleInfo = provider.getCameraInfo(teleSelector)
        assertThat((teleInfo as AdapterCameraInfo).physicalCameraId).isEqualTo("2")
        assertThat(teleInfo.intrinsicZoomRatio).isEqualTo(3.0f)

        // Act & Assert - Widest
        val widestSelector = createSelector(category = CameraSelector.LENS_CATEGORY_WIDEST_FOV)
        val widestInfo = provider.getCameraInfo(widestSelector)
        assertThat((widestInfo as AdapterCameraInfo).physicalCameraId).isEqualTo("1")

        // Act & Assert - Narrowest
        val narrowestSelector =
            createSelector(category = CameraSelector.LENS_CATEGORY_NARROWEST_FOV)
        val narrowestInfo = provider.getCameraInfo(narrowestSelector)
        assertThat((narrowestInfo as AdapterCameraInfo).physicalCameraId).isEqualTo("2")

        // Act & Assert - Default
        val defaultSelector = createSelector(category = CameraSelector.LENS_CATEGORY_DEFAULT)
        val defaultInfo = provider.getCameraInfo(defaultSelector)
        assertThat((defaultInfo as AdapterCameraInfo).physicalCameraId).isNull()
    }

    @Test
    fun getCameraInfo_throwsExceptionForUnsupportedLensCategory() = runTest {
        provider = setupMultiCameraProvider()

        val ultraWideFrontSelector =
            createSelector(
                CameraSelector.LENS_FACING_FRONT,
                CameraSelector.LENS_CATEGORY_ULTRA_WIDE,
            )

        assertThrows<IllegalArgumentException> { provider.getCameraInfo(ultraWideFrontSelector) }
    }

    @Test
    fun hasCamera_withLensCategory_returnsCorrectValue() = runTest {
        val physicalWide = createFakePhysicalCamera(id = "1", intrinsicZoomRatio = 0.5f)
        val logicalBack = createFakeCamera(id = "0", physicalSubCameras = setOf(physicalWide))
        provider = initCameraProvider(logicalBack)

        val ultraWideBackSelector =
            createSelector(category = CameraSelector.LENS_CATEGORY_ULTRA_WIDE)
        assertThat(provider.hasCamera(ultraWideBackSelector)).isTrue()

        val teleBackSelector = createSelector(category = CameraSelector.LENS_CATEGORY_TELEPHOTO)
        assertThat(provider.hasCamera(teleBackSelector)).isFalse()

        val ultraWideFrontSelector =
            createSelector(
                CameraSelector.LENS_FACING_FRONT,
                CameraSelector.LENS_CATEGORY_ULTRA_WIDE,
            )
        assertThat(provider.hasCamera(ultraWideFrontSelector)).isFalse()
    }

    @Test
    fun bindToLifecycle_withLensCategory_setsPhysicalCameraIdOnUseCase() = runTest {
        provider = setupMultiCameraProvider()

        val preview = Preview.Builder().build()
        val ultraWideSelector = createSelector(category = CameraSelector.LENS_CATEGORY_ULTRA_WIDE)

        provider.bindToLifecycle(lifecycleOwner0, ultraWideSelector, preview)

        assertThat(preview.physicalCameraId).isEqualTo("1")
    }

    @Test
    fun standaloneCamera_priorityOverPhysicalCamera_whenSameIntrinsicZoomRatio() = runTest {
        // Arrange: Standalone camera "4" (0.5f) and logical camera "0" with sub-camera "1" (0.5f)
        val standaloneUltraWide = createFakeCamera(id = "4", intrinsicZoomRatio = 0.5f)
        provider = setupMultiCameraProvider(standaloneUltraWide)

        val ultraWideSelector = createSelector(category = CameraSelector.LENS_CATEGORY_ULTRA_WIDE)
        val cameraInfo = provider.getCameraInfo(ultraWideSelector)

        // Standalone camera "4" should be selected because standalone cameras take priority
        assertThat((cameraInfo as CameraInfoInternal).cameraId).isEqualTo("4")
        assertThat((cameraInfo as AdapterCameraInfo).physicalCameraId).isNull()
    }

    @Test
    fun bindToLifecycle_withLensCategoryAndCustomFilter_preservesCustomFilter() = runTest {
        // Arrange: Logical cameras "0" (sub-camera "1", 0.5f) and "2" (sub-camera "3", 0.5f)
        val physicalUltraWide0 = createFakePhysicalCamera(id = "1", intrinsicZoomRatio = 0.5f)
        val logicalBack0 =
            createFakeCamera(id = "0", physicalSubCameras = setOf(physicalUltraWide0))

        val physicalUltraWide2 = createFakePhysicalCamera(id = "3", intrinsicZoomRatio = 0.5f)
        val logicalBack2 =
            createFakeCamera(id = "2", physicalSubCameras = setOf(physicalUltraWide2))

        provider = initCameraProvider(logicalBack0, logicalBack2)

        // Custom filter with custom identifier, simulating extension or vendor filter
        val testIdentifier = Identifier.create("test-custom-filter-id")
        val fakeCameraConfig =
            object : CameraConfig {
                override fun getConfig(): androidx.camera.core.impl.Config =
                    MutableOptionsBundle.create()

                override fun getCompatibilityId(): Identifier = testIdentifier
            }
        ExtendedCameraConfigProviderStore.addConfig(testIdentifier) { _, _ -> fakeCameraConfig }

        var filterInvoked = false
        val customFilter =
            object : CameraFilter {
                override fun getIdentifier(): Identifier = testIdentifier

                override fun filter(cameraInfos: List<CameraInfo>): List<CameraInfo> {
                    filterInvoked = true
                    return cameraInfos.filter { (it as? CameraInfoInternal)?.cameraId == "2" }
                }
            }

        val selector =
            createSelector(
                category = CameraSelector.LENS_CATEGORY_ULTRA_WIDE,
                filter = customFilter,
            )
        val preview = Preview.Builder().build()
        val camera = provider.bindToLifecycle(lifecycleOwner0, selector, preview)

        assertThat(filterInvoked).isTrue()
        assertThat(preview.physicalCameraId).isEqualTo("3")
        val adapterCameraInfo = camera.cameraInfo as AdapterCameraInfo
        assertThat(adapterCameraInfo.cameraConfig.compatibilityId).isEqualTo(testIdentifier)
    }

    @Test
    fun bindToLifecycle_dualSelfieModeWithLensCategory_assignsDistinctPhysicalCameraIds() =
        runTest {
            // Arrange: Front camera "0" with sub-cameras "1" (0.5f) and "2" (2.0f)
            val physicalFrontUltraWide =
                createFakePhysicalCamera(
                    id = "1",
                    lensFacing = CameraSelector.LENS_FACING_FRONT,
                    intrinsicZoomRatio = 0.5f,
                )
            val physicalFrontTele =
                createFakePhysicalCamera(
                    id = "2",
                    lensFacing = CameraSelector.LENS_FACING_FRONT,
                    intrinsicZoomRatio = 2.0f,
                )
            val logicalFront =
                createFakeCamera(
                    id = "0",
                    lensFacing = CameraSelector.LENS_FACING_FRONT,
                    physicalSubCameras = setOf(physicalFrontUltraWide, physicalFrontTele),
                )

            provider = initCameraProvider(logicalFront)

            val ultraWideSelector =
                createSelector(
                    CameraSelector.LENS_FACING_FRONT,
                    CameraSelector.LENS_CATEGORY_ULTRA_WIDE,
                )
            val teleSelector =
                createSelector(
                    CameraSelector.LENS_FACING_FRONT,
                    CameraSelector.LENS_CATEGORY_TELEPHOTO,
                )

            val preview0 = Preview.Builder().build()
            val preview1 = Preview.Builder().build()

            val config0 =
                SingleCameraConfig(
                    ultraWideSelector,
                    UseCaseGroup.Builder().addUseCase(preview0).build(),
                    lifecycleOwner0,
                )
            val config1 =
                SingleCameraConfig(
                    teleSelector,
                    UseCaseGroup.Builder().addUseCase(preview1).build(),
                    lifecycleOwner0,
                )

            provider.bindToLifecycle(listOf(config0, config1))

            // Assert: preview0 gets physicalCameraId "1" and preview1 gets physicalCameraId "2"
            assertThat(preview0.physicalCameraId).isEqualTo("1")
            assertThat(preview1.physicalCameraId).isEqualTo("2")
        }

    @Test
    fun bindToLifecycle_withLensCategoryDefault_fallsBackToLogicalCamera_whenNoPhysicalSubCameraMatchesDefault() =
        runTest {
            // Arrange: Logical camera "0" (1.0f) with sub-cameras "1" (0.5f) and "3" (2.0f).
            // Note that no physical sub-camera has intrinsic zoom ratio 1.0f.
            val physicalWide = createFakePhysicalCamera(id = "1", intrinsicZoomRatio = 0.5f)
            val physicalTele = createFakePhysicalCamera(id = "3", intrinsicZoomRatio = 2.0f)
            val logicalBack =
                createFakeCamera(id = "0", physicalSubCameras = setOf(physicalWide, physicalTele))

            provider = initCameraProvider(logicalBack)

            val selector = createSelector(category = CameraSelector.LENS_CATEGORY_DEFAULT)
            val preview = Preview.Builder().build()
            provider.bindToLifecycle(lifecycleOwner0, selector, preview)

            // Assert: falls back to logical camera container "0", so physicalCameraId is null
            assertThat(preview.physicalCameraId).isNull()
        }

    @Test
    fun bindToLifecycle_withMultipleTelephotoLenses_selectsCorrectTelephotoAndNarrowest() =
        runTest {
            // Arrange: Logical camera with sub-cameras 0.5f, 1.0f, 3.0f, 5.0f, 5.0f
            val physicalWide = createFakePhysicalCamera(id = "1", intrinsicZoomRatio = 0.5f)
            val physicalMain = createFakePhysicalCamera(id = "2", intrinsicZoomRatio = 1.0f)
            val physicalTele1 = createFakePhysicalCamera(id = "3", intrinsicZoomRatio = 3.0f)
            val physicalTele2 = createFakePhysicalCamera(id = "4", intrinsicZoomRatio = 5.0f)
            val physicalTele3 = createFakePhysicalCamera(id = "5", intrinsicZoomRatio = 5.0f)
            val logicalBack =
                createFakeCamera(
                    id = "0",
                    physicalSubCameras =
                        setOf(
                            physicalWide,
                            physicalMain,
                            physicalTele1,
                            physicalTele2,
                            physicalTele3,
                        ),
                )

            provider = initCameraProvider(logicalBack)

            // TELEPHOTO selects the first lens with intrinsic zoom ratio > 1.0f -> "3" (3.0f)
            val previewTele = Preview.Builder().build()
            val teleSelector = createSelector(category = CameraSelector.LENS_CATEGORY_TELEPHOTO)
            provider.bindToLifecycle(lifecycleOwner0, teleSelector, previewTele)
            assertThat(previewTele.physicalCameraId).isEqualTo("3")

            provider.unbindAll()

            // NARROWEST_FOV selects the first lens with maximum intrinsic zoom ratio (5.0f) -> "4"
            val previewNarrowest = Preview.Builder().build()
            val narrowestSelector =
                createSelector(category = CameraSelector.LENS_CATEGORY_NARROWEST_FOV)
            provider.bindToLifecycle(lifecycleOwner0, narrowestSelector, previewNarrowest)
            assertThat(previewNarrowest.physicalCameraId).isEqualTo("4")

            provider.unbindAll()

            // WIDEST_FOV selects the lens with minimum intrinsic zoom ratio -> "1" (0.5f)
            val previewWidest = Preview.Builder().build()
            val widestSelector = createSelector(category = CameraSelector.LENS_CATEGORY_WIDEST_FOV)
            provider.bindToLifecycle(lifecycleOwner0, widestSelector, previewWidest)
            assertThat(previewWidest.physicalCameraId).isEqualTo("1")

            provider.unbindAll()

            // DEFAULT prioritizes top-level camera devices over physical sub-cameras
            val previewDefault = Preview.Builder().build()
            val defaultSelector = createSelector(category = CameraSelector.LENS_CATEGORY_DEFAULT)
            provider.bindToLifecycle(lifecycleOwner0, defaultSelector, previewDefault)
            assertThat(previewDefault.physicalCameraId).isNull()
        }

    @Test
    fun bindToLifecycle_withIdenticalIntrinsicZoomRatios_prioritizesTopLevelCameraOverPhysicalSubCamera() =
        runTest {
            // Arrange: Top-level camera "0" and physical sub-camera "1" both have
            // intrinsic zoom ratio 1.0f
            val physicalSingle = createFakePhysicalCamera(id = "1", intrinsicZoomRatio = 1.0f)
            val logicalBack = createFakeCamera(id = "0", physicalSubCameras = setOf(physicalSingle))

            provider = initCameraProvider(logicalBack)

            val narrowestSelector =
                createSelector(category = CameraSelector.LENS_CATEGORY_NARROWEST_FOV)
            val widestSelector = createSelector(category = CameraSelector.LENS_CATEGORY_WIDEST_FOV)
            val defaultSelector = createSelector(category = CameraSelector.LENS_CATEGORY_DEFAULT)

            // When intrinsic zoom ratios match, top-level cameras take precedence
            // over physical sub-cameras (physicalCameraId remains null)
            val previewNarrowest = Preview.Builder().build()
            provider.bindToLifecycle(lifecycleOwner0, narrowestSelector, previewNarrowest)
            assertThat(previewNarrowest.physicalCameraId).isNull()

            provider.unbindAll()

            val previewWidest = Preview.Builder().build()
            provider.bindToLifecycle(lifecycleOwner0, widestSelector, previewWidest)
            assertThat(previewWidest.physicalCameraId).isNull()

            provider.unbindAll()

            val previewDefault = Preview.Builder().build()
            provider.bindToLifecycle(lifecycleOwner0, defaultSelector, previewDefault)
            assertThat(previewDefault.physicalCameraId).isNull()
        }

    private fun createSelector(
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        category: Int? = null,
        filter: CameraFilter? = null,
    ): CameraSelector =
        CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .apply {
                category?.let { setLensCategory(it) }
                filter?.let { addCameraFilter(it) }
            }
            .build()

    private fun createFakeCamera(
        id: String,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        intrinsicZoomRatio: Float = 1.0f,
        physicalSubCameras: Set<CameraInfo> = emptySet(),
    ): FakeCamera {
        val info =
            FakeTestCameraInfoInternal(
                cameraId = id,
                lensFacing = lensFacing,
                physicalCameraInfos = physicalSubCameras,
                intrinsicZoomRatio = intrinsicZoomRatio,
            )
        return FakeCamera(id, null, info)
    }

    private fun createFakePhysicalCamera(
        id: String,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        intrinsicZoomRatio: Float = 1.0f,
    ): FakeTestCameraInfoInternal =
        FakeTestCameraInfoInternal(
            cameraId = id,
            lensFacing = lensFacing,
            physicalCameraId = id,
            intrinsicZoomRatio = intrinsicZoomRatio,
        )

    private suspend fun setupMultiCameraProvider(
        vararg additionalCameras: FakeCamera
    ): ProcessCameraProvider {
        val physicalWide = createFakePhysicalCamera(id = "1", intrinsicZoomRatio = 0.5f)
        val physicalTele = createFakePhysicalCamera(id = "2", intrinsicZoomRatio = 3.0f)
        val logicalBack =
            createFakeCamera(
                id = "0",
                physicalSubCameras = setOf(physicalWide, physicalTele),
                intrinsicZoomRatio = 1.0f,
            )
        val frontCamera =
            createFakeCamera(
                id = "3",
                lensFacing = CameraSelector.LENS_FACING_FRONT,
                intrinsicZoomRatio = 1.0f,
            )
        return initCameraProvider(logicalBack, frontCamera, *additionalCameras)
    }

    private suspend fun initCameraProvider(vararg cameras: FakeCamera): ProcessCameraProvider {
        val cameraFactory =
            FakeCameraFactory(null).apply {
                for (camera in cameras) {
                    val info = camera.cameraInfoInternal
                    insertCamera(info.lensFacing, info.cameraId) { camera }
                }
                val hasBack = cameras.any {
                    it.cameraInfoInternal.lensFacing == CameraSelector.LENS_FACING_BACK
                }
                val hasFront = cameras.any {
                    it.cameraInfoInternal.lensFacing == CameraSelector.LENS_FACING_FRONT
                }
                if (!hasBack) {
                    insertCamera(CameraSelector.LENS_FACING_BACK, "dummy_back") {
                        FakeCamera(
                            "dummy_back",
                            null,
                            FakeCameraInfoInternal(
                                "dummy_back",
                                0,
                                CameraSelector.LENS_FACING_BACK,
                            ),
                        )
                    }
                }
                if (!hasFront) {
                    insertCamera(CameraSelector.LENS_FACING_FRONT, "dummy_front") {
                        FakeCamera(
                            "dummy_front",
                            null,
                            FakeCameraInfoInternal(
                                "dummy_front",
                                0,
                                CameraSelector.LENS_FACING_FRONT,
                            ),
                        )
                    }
                }
                cameraCoordinator = FakeCameraCoordinator()
            }
        ProcessCameraProvider.configureInstance(createCameraXConfig(cameraFactory = cameraFactory))
        return ProcessCameraProvider.getInstance(context).await()
    }

    private fun createCameraXConfig(
        cameraFactory: CameraFactory = createFakeCameraFactory(),
        surfaceManager: CameraDeviceSurfaceManager? = FakeCameraDeviceSurfaceManager(),
        useCaseConfigFactory: UseCaseConfigFactory? = FakeUseCaseConfigFactory(),
    ): CameraXConfig {
        val cameraFactoryProvider = Provider { _, _, _, _, _, _ -> cameraFactory }
        return CameraXConfig.Builder()
            .setCameraFactoryProvider(cameraFactoryProvider)
            .apply {
                surfaceManager?.let {
                    setDeviceSurfaceManagerProvider {
                        _: Context?,
                        _: Any?,
                        _: Set<String?>?,
                        _: String? ->
                        it
                    }
                }
                useCaseConfigFactory?.let {
                    setUseCaseConfigFactoryProvider { _: Context?, _: Boolean -> it }
                }
            }
            .build()
    }

    private fun createFakeCameraFactory(
        frontCamera: Boolean = false,
        backCamera: Boolean = false,
        anotherBackCamera: Boolean = false,
    ): CameraFactory =
        FakeCameraFactory(null).also { cameraFactory ->
            if (backCamera) {
                cameraFactory.insertCamera(CameraSelector.LENS_FACING_BACK, CAMERA_ID_0) {
                    FakeCamera(
                        CAMERA_ID_0,
                        null,
                        FakeCameraInfoInternal(CAMERA_ID_0, 0, CameraSelector.LENS_FACING_BACK),
                    )
                }
            }
            if (anotherBackCamera) {
                cameraFactory.insertCamera(CameraSelector.LENS_FACING_BACK, CAMERA_ID_2) {
                    FakeCamera(
                        CAMERA_ID_2,
                        null,
                        FakeCameraInfoInternal(CAMERA_ID_2, 0, CameraSelector.LENS_FACING_BACK),
                    )
                }
            }
            if (frontCamera) {
                cameraFactory.insertCamera(CameraSelector.LENS_FACING_FRONT, CAMERA_ID_1) {
                    FakeCamera(
                        CAMERA_ID_1,
                        null,
                        FakeCameraInfoInternal(CAMERA_ID_1, 0, CameraSelector.LENS_FACING_FRONT),
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
                    TimeUnit.MILLISECONDS,
                )
            }
            delay(FAKE_INIT_PROCESS_TIME_MS)
        }
    }

    private class FakeTestCameraInfoInternal(
        cameraId: String = "0",
        sensorRotationDegrees: Int = 0,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        private val physicalCameraId: String? = null,
        private val physicalCameraInfos: Set<CameraInfo> = emptySet(),
        private val intrinsicZoomRatio: Float = 1.0f,
    ) : ForwardingCameraInfo(FakeCameraInfoInternal(cameraId, sensorRotationDegrees, lensFacing)) {

        override fun getCameraSelector(): CameraSelector {
            val base = super.getCameraSelector()
            return if (physicalCameraId != null) {
                CameraSelector.Builder.fromSelector(base)
                    .setPhysicalCameraId(physicalCameraId)
                    .build()
            } else {
                base
            }
        }

        override fun getPhysicalCameraInfos(): Set<CameraInfo> = physicalCameraInfos

        override fun getIntrinsicZoomRatio(): Float = intrinsicZoomRatio
    }

    private class CustomSessionConfig(
        cameraFilter: CameraFilter,
        requireNonEmptyUseCases: Boolean = false,
        useCases: List<UseCase> = emptyList(),
    ) :
        SessionConfig(
            useCases,
            cameraFilter = cameraFilter,
            requireNonEmptyUseCases = requireNonEmptyUseCases,
        ) {}

    companion object {
        private const val CAMERA_ID_0 = "0"
        private const val CAMERA_ID_1 = "1"
        private const val CAMERA_ID_2 = "2"
        private const val FAKE_INIT_PROCESS_TIME_MS = 33L
    }
}
