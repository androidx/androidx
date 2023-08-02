/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraDevice
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.CaptureConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpInactiveSurfaceCloser
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraph
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeCapturePipeline
import androidx.camera.camera2.pipe.integration.testing.FakeSurface
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class UseCaseCameraTest {
    private val surface = FakeSurface()
    private val surfaceToStreamMap: Map<DeferrableSurface, StreamId> = mapOf(surface to StreamId(0))
    private val useCaseThreads by lazy {
        val dispatcher = Dispatchers.Default
        val cameraScope = CoroutineScope(
            Job() +
                dispatcher
        )

        UseCaseThreads(
            cameraScope,
            dispatcher.asExecutor(),
            dispatcher
        )
    }
    private val fakeCameraProperties = FakeCameraProperties()
    private val fakeCameraGraph = FakeCameraGraph()
    private val fakeUseCaseGraphConfig = UseCaseGraphConfig(
        graph = fakeCameraGraph,
        surfaceToStreamMap = surfaceToStreamMap,
        cameraStateAdapter = CameraStateAdapter(),
    )
    private val fakeConfigAdapter = CaptureConfigAdapter(
        useCaseGraphConfig = fakeUseCaseGraphConfig,
        cameraProperties = fakeCameraProperties,
        threads = useCaseThreads,
    )
    private val fakeUseCaseCameraState = UseCaseCameraState(
        useCaseGraphConfig = fakeUseCaseGraphConfig,
        threads = useCaseThreads,
    )
    private val requestControl = UseCaseCameraRequestControlImpl(
        capturePipeline = FakeCapturePipeline(),
        configAdapter = fakeConfigAdapter,
        state = fakeUseCaseCameraState,
        useCaseGraphConfig = fakeUseCaseGraphConfig,
    )

    @After
    fun tearDown() {
        surface.close()
    }

    @Test
    fun setInvalidSessionConfig_repeatingShouldStop() {
        // Arrange
        val fakeUseCase = FakeTestUseCase().apply {
            // Set a valid SessionConfig with Surface and template.
            setupSessionConfig(
                SessionConfig.Builder().apply {
                    setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                    addSurface(surface)
                }
            )
        }
        @Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        val useCaseCamera = UseCaseCameraImpl(
            controls = emptySet<UseCaseCameraControl>() as java.util.Set<UseCaseCameraControl>,
            useCaseGraphConfig = fakeUseCaseGraphConfig,
            useCases = arrayListOf(fakeUseCase),
            useCaseSurfaceManager = UseCaseSurfaceManager(
                useCaseThreads,
                CameraPipe(CameraPipe.Config(ApplicationProvider.getApplicationContext())),
                NoOpInactiveSurfaceCloser,
            ),
            threads = useCaseThreads,
            requestControl = requestControl
        ).also {
            it.runningUseCases = setOf(fakeUseCase)
        }
        assumeTrue(
            fakeCameraGraph.fakeCameraGraphSession.repeatingRequestSemaphore.tryAcquire(
                1, 3, TimeUnit.SECONDS
            )
        )

        // Act. Set an invalid SessionConfig which doesn't have the template.
        fakeUseCase.setupSessionConfig(
            SessionConfig.Builder().apply {
                addSurface(surface)
            }
        )

        useCaseCamera.runningUseCases = setOf(fakeUseCase)

        // Assert. The stopRepeating() should be called.
        assertThat(
            fakeCameraGraph.fakeCameraGraphSession.stopRepeatingSemaphore.tryAcquire(
                1, 3, TimeUnit.SECONDS
            )
        ).isTrue()
    }
}

@RequiresApi(21)
private class FakeTestUseCase() : FakeUseCase(
    FakeUseCaseConfig.Builder().setTargetName("UseCase").useCaseConfig
) {

    fun setupSessionConfig(sessionConfigBuilder: SessionConfig.Builder) {
        updateSessionConfig(sessionConfigBuilder.build())
        notifyActive()
    }
}
