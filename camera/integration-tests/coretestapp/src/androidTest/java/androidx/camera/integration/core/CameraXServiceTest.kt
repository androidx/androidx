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

package androidx.camera.integration.core

import android.Manifest
import android.app.ActivityManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector.LENS_FACING_BACK
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.UseCase
import androidx.camera.integration.core.CameraXService.ACTION_BIND_USE_CASES
import androidx.camera.integration.core.CameraXService.ACTION_START_RECORDING
import androidx.camera.integration.core.CameraXService.ACTION_STOP_RECORDING
import androidx.camera.integration.core.CameraXService.ACTION_TAKE_PICTURE
import androidx.camera.integration.core.CameraXService.EXTRA_IMAGE_ANALYSIS_ENABLED
import androidx.camera.integration.core.CameraXService.EXTRA_IMAGE_CAPTURE_ENABLED
import androidx.camera.integration.core.CameraXService.EXTRA_VIDEO_CAPTURE_ENABLED
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.hasCameraWithLensFacing
import androidx.camera.testing.impl.mocks.MockConsumer
import androidx.camera.testing.impl.mocks.helpers.ArgumentCaptor
import androidx.camera.testing.impl.mocks.helpers.CallTimes
import androidx.camera.video.VideoCapture
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.LifecycleOwnerUtils
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class CameraXServiceTest(
    private val implName: String,
    private val cameraXConfig: CameraXConfig
) {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraXConfig)
    )

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private lateinit var serviceConnection: ServiceConnection
    private lateinit var service: CameraXService

    @Before
    fun setUp() = runBlocking {
        assumeTrue(hasCameraWithLensFacing(LENS_FACING_BACK))
        assumeFalse(isBackgroundRestricted())

        service = bindService()

        // Ensure service is started.
        LifecycleOwnerUtils.waitUntilState(service, Lifecycle.State.STARTED)
    }

    @After
    fun tearDown() {
        if (this::service.isInitialized) {
            service.deleteSavedMediaFiles()
            context.unbindService(serviceConnection)
            context.stopService(createServiceIntent())

            // Ensure service is destroyed
            LifecycleOwnerUtils.waitUntilState(service, Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun canStartServiceAsForeground() {
        assertThat(isForegroundService(service)).isTrue()
    }

    @Test
    fun canBindUseCases() {
        // Arrange: set up onUseCaseBound callback.
        val useCaseCallback = MockConsumer<Collection<UseCase>>()
        service.setOnUseCaseBoundCallback(useCaseCallback)

        // Act: bind VideoCapture and ImageCapture.
        context.startService(createServiceIntent(ACTION_BIND_USE_CASES).apply {
            putExtra(EXTRA_VIDEO_CAPTURE_ENABLED, true)
            putExtra(EXTRA_IMAGE_CAPTURE_ENABLED, true)
        })

        // Assert: verify bound UseCases.
        val captor = ArgumentCaptor<Collection<UseCase>>()
        useCaseCallback.verifyAcceptCall(Collection::class.java, false, 3000L, CallTimes(1), captor)
        assertThat(captor.value!!.map { it.javaClass }).containsExactly(
            VideoCapture::class.java,
            ImageCapture::class.java
        )

        // Act: rebind by ImageAnalysis.
        useCaseCallback.clearAcceptCalls()
        context.startService(createServiceIntent(ACTION_BIND_USE_CASES).apply {
            putExtra(EXTRA_IMAGE_ANALYSIS_ENABLED, true)
        })

        // Assert: verify bound UseCases.
        useCaseCallback.verifyAcceptCall(Collection::class.java, false, 3000L, CallTimes(1), captor)
        assertThat(captor.value!!.map { it.javaClass }).containsExactly(
            ImageAnalysis::class.java,
        )
    }

    @Test
    fun canReceiveAnalysisFrame() {
        // Arrange.
        context.startService(createServiceIntent(ACTION_BIND_USE_CASES).apply {
            putExtra(EXTRA_IMAGE_ANALYSIS_ENABLED, true)
        })

        // Act.
        val latch = service.acquireAnalysisFrameCountDownLatch()

        // Assert.
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun canTakePicture() {
        // Arrange.
        context.startService(createServiceIntent(ACTION_BIND_USE_CASES).apply {
            putExtra(EXTRA_IMAGE_CAPTURE_ENABLED, true)
        })

        // Act.
        val latch = service.acquireTakePictureCountDownLatch()
        context.startService(createServiceIntent(ACTION_TAKE_PICTURE))

        // Assert.
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun canRecordVideo() = runBlocking {
        // Arrange.
        context.startService(createServiceIntent(ACTION_BIND_USE_CASES).apply {
            putExtra(EXTRA_VIDEO_CAPTURE_ENABLED, true)
        })

        // Act.
        val latch = service.acquireRecordVideoCountDownLatch()
        context.startService(createServiceIntent(ACTION_START_RECORDING))

        delay(3000L)

        context.startService(createServiceIntent(ACTION_STOP_RECORDING))

        // Assert.
        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue()
    }

    private fun createServiceIntent(action: String? = null) =
        Intent(context, CameraXService::class.java).apply {
            action?.let { setAction(it) }
        }

    private fun isForegroundService(service: Service): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (serviceInfo in manager.getRunningServices(Int.MAX_VALUE)) {
            if (service.javaClass.name == serviceInfo.service.className) {
                return serviceInfo.foreground
            }
        }
        return false
    }

    private suspend fun bindService(): CameraXService {
        val serviceDeferred = CompletableDeferred<CameraXService>()
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as CameraXService.CameraXServiceBinder
                serviceDeferred.complete(binder.service)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
            }
        }
        context.bindService(createServiceIntent(), serviceConnection, Service.BIND_AUTO_CREATE)
        return withTimeout(3000L) {
            serviceDeferred.await()
        }
    }

    private fun isBackgroundRestricted(): Boolean =
        if (Build.VERSION.SDK_INT >= 28) activityManager.isBackgroundRestricted else false
}
