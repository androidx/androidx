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

import android.content.Context
import android.os.Looper.getMainLooper
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.CameraDeviceSurfaceManager
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.QuirkSettings
import androidx.camera.core.impl.QuirkSettingsHolder
import androidx.camera.core.impl.UseCaseConfigFactory
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraCoordinator
import androidx.camera.testing.impl.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.impl.fakes.FakeCameraFactory
import androidx.camera.testing.impl.fakes.FakeUseCaseConfigFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 21)
class CameraXTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val quirkSettingsHolder = QuirkSettingsHolder.instance()

    @After
    fun tearDown() {
        quirkSettingsHolder.reset() // Reset after each test
        shadowOf(getMainLooper()).idle() // Ensure any pending updates are processed after reset.
    }

    @Test
    fun defaultQuirksSettings() {
        val configProvider = createConfigProvider()

        CameraX(context, configProvider)

        assertThat(quirkSettingsHolder.get()).isSameInstanceAs(QuirkSettingsHolder.DEFAULT)
    }

    @Test
    fun updateQuirkSettings_byCameraXConfig() {
        // Arrange: disable all quirks by CameraXConfig
        val quirkSettings = QuirkSettings.withAllQuirksDisabled()
        val configProvider = createConfigProvider(quirkSettings = quirkSettings)

        // Act.
        CameraX(context, configProvider)

        // Assert.
        assertThat(quirkSettingsHolder.get()).isSameInstanceAs(quirkSettings)
    }

    @Test
    fun updateQuirkSettings_multipleTimes_byCameraXConfig() {
        // Arrange: disable all quirks by CameraXConfig
        val quirkSettings = QuirkSettings.withAllQuirksDisabled()
        val configProvider = createConfigProvider(quirkSettings = quirkSettings)

        // Act.
        CameraX(context, configProvider)

        // Assert.
        assertThat(quirkSettingsHolder.get()).isSameInstanceAs(quirkSettings)

        // Arrange: enable default quirks by CameraXConfig
        val quirkSettings2 = QuirkSettings.withDefaultBehavior()
        val configProvider2 = createConfigProvider(quirkSettings = quirkSettings2)

        // Act.
        CameraX(context, configProvider2)

        // Assert.
        assertThat(quirkSettingsHolder.get()).isSameInstanceAs(quirkSettings2)
    }

    @Test
    fun updateQuirkSettings_byQuirkSettingsLoader() {
        val configProvider = CameraXConfig.Provider { CameraXConfig.Builder().build() }
        // Arrange: disable all quirks by quirkSettingsLoader
        val quirkSettingsLoader = { _: Context -> QuirkSettings.withAllQuirksDisabled() }

        // Act.
        CameraX(context, configProvider, quirkSettingsLoader)

        // Assert.
        val settings = quirkSettingsHolder.get()
        assertThat(settings.isEnabledWhenDeviceHasQuirk).isEqualTo(false)
        assertThat(settings.forceEnabledQuirks).isEmpty()
        assertThat(settings.forceDisabledQuirks).isEmpty()
    }

    @Test
    fun disableQuirks_cameraXConfigHasHigherPriorityThanQuirkSettingsLoader() {
        // Arrange: enable default quirks by CameraXConfig
        val quirkSettings = QuirkSettings.withDefaultBehavior()
        val configProvider = createConfigProvider(quirkSettings = quirkSettings)
        // Arrange: disable quirks by quirkSettingsLoader
        val quirkSettingsLoader = { _: Context -> QuirkSettings.withAllQuirksDisabled() }

        // Act.
        CameraX(context, configProvider, quirkSettingsLoader)

        // Assert: CameraXConfig has higher priority.
        val settings = quirkSettingsHolder.get()
        assertThat(settings).isSameInstanceAs(quirkSettings)
    }

    @Test
    fun init_createAndSetCameraUseCaseAdapterProviderToCameraInfo() {
        // Arrange.
        val cameraInfo1 = FakeCameraInfoInternal("0", 0, LENS_FACING_BACK)
        val cameraInfo2 = FakeCameraInfoInternal("1", 0, LENS_FACING_FRONT)
        val cameras = listOf(FakeCamera(cameraInfo1), FakeCamera(cameraInfo2))
        val cameraFactoryProvider = createCameraFactoryProvider(cameras)
        val configProvider = createConfigProvider(cameraFactoryProvider = cameraFactoryProvider)

        // Act.
        val cameraX = CameraX(context, configProvider)
        cameraX.initializeFuture.get(300L, TimeUnit.MILLISECONDS)

        // Assert.
        assertThat(cameraX.cameraUseCaseAdapterProvider).isNotNull()
        assertThat(cameraInfo1.cameraUseCaseAdapterProvider).isNotNull()
        assertThat(cameraInfo2.cameraUseCaseAdapterProvider).isNotNull()
    }

    private fun createCameraFactoryProvider(
        cameras: List<FakeCamera>,
        cameraCoordinator: CameraCoordinator = FakeCameraCoordinator(),
    ) =
        CameraFactory.Provider { _, _, _, _, _ ->
            FakeCameraFactory().apply {
                for (camera in cameras) {
                    val cameraInfo = camera.cameraInfoInternal
                    insertCamera(cameraInfo.lensFacing, cameraInfo.cameraId) { camera }
                }
                this.cameraCoordinator = cameraCoordinator
            }
        }

    private fun createConfigProvider(
        cameraFactoryProvider: CameraFactory.Provider =
            CameraFactory.Provider { _, _, _, _, _ -> FakeCameraFactory() },
        cameraDeviceSurfaceManager: CameraDeviceSurfaceManager.Provider =
            CameraDeviceSurfaceManager.Provider { _, _, _ -> FakeCameraDeviceSurfaceManager() },
        useCaseConfigFactoryProvider: UseCaseConfigFactory.Provider =
            UseCaseConfigFactory.Provider { FakeUseCaseConfigFactory() },
        cameraExecutor: Executor = CameraXExecutors.directExecutor(),
        quirkSettings: QuirkSettings? = null,
    ) =
        CameraXConfig.Provider {
            CameraXConfig.Builder()
                .apply {
                    setCameraFactoryProvider(cameraFactoryProvider)
                    setDeviceSurfaceManagerProvider(cameraDeviceSurfaceManager)
                    setUseCaseConfigFactoryProvider(useCaseConfigFactoryProvider)
                    setCameraExecutor(cameraExecutor)
                    quirkSettings?.let { setQuirkSettings(it) }
                }
                .build()
        }
}
