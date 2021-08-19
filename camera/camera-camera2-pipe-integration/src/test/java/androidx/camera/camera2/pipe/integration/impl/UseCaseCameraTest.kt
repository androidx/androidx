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
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import android.view.Surface
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.TorchState
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.fakes.FakeUseCase
import androidx.camera.testing.fakes.FakeUseCaseConfig
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
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
    private val fakeCameraGraph = FakeCameraGraph()
    private val requestControl = UseCaseCameraRequestControlImpl(
        fakeCameraGraph, surfaceToStreamMap, useCaseThreads
    )

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
        val useCaseCamera = UseCaseCameraImpl(
            fakeCameraGraph, listOf(fakeUseCase),
            useCaseThreads, requestControl
        ).also {
            it.activeUseCases = setOf(fakeUseCase)
        }
        assumeTrue(
            fakeCameraGraph.fakeCameraGraphSession.repeatingRequests.tryAcquire(
                1, 3, TimeUnit.SECONDS
            )
        )

        // Act. Set an invalid SessionConfig which doesn't have the template.
        fakeUseCase.setupSessionConfig(
            SessionConfig.Builder().apply {
                addSurface(surface)
            }
        )
        useCaseCamera.activeUseCases = setOf(fakeUseCase)

        // Assert. The stopRepeating() should be called.
        assertThat(
            fakeCameraGraph.fakeCameraGraphSession.stopRepeating.tryAcquire(
                1, 3, TimeUnit.SECONDS
            )
        ).isTrue()
    }
}

private class FakeTestUseCase() : FakeUseCase(
    FakeUseCaseConfig.Builder().setTargetName("UseCase").useCaseConfig
) {

    fun setupSessionConfig(sessionConfigBuilder: SessionConfig.Builder) {
        updateSessionConfig(sessionConfigBuilder.build())
        notifyActive()
    }
}

private class FakeSurface : DeferrableSurface() {
    override fun provideSurface(): ListenableFuture<Surface> {
        return Futures.immediateFuture(null)
    }
}

private class FakeCameraGraph : CameraGraph {
    val fakeCameraGraphSession = FakeCameraGraphSession()

    override val streams: StreamGraph
        get() = throw NotImplementedError("Not used in testing")

    override suspend fun acquireSession(): CameraGraph.Session {
        return fakeCameraGraphSession
    }

    override fun acquireSessionOrNull(): CameraGraph.Session {
        return fakeCameraGraphSession
    }

    override fun close() {
        throw NotImplementedError("Not used in testing")
    }

    override fun setSurface(stream: StreamId, surface: Surface?) {
        // No-op
    }

    override fun start() {
        throw NotImplementedError("Not used in testing")
    }

    override fun stop() {
        throw NotImplementedError("Not used in testing")
    }
}

private class FakeCameraGraphSession : CameraGraph.Session {
    val repeatingRequests = Semaphore(0)
    val stopRepeating = Semaphore(0)

    override fun abort() {
        // No-op
    }

    override fun close() {
        // No-op
    }

    override suspend fun lock3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
        aeLockBehavior: Lock3ABehavior?,
        afLockBehavior: Lock3ABehavior?,
        awbLockBehavior: Lock3ABehavior?,
        frameLimit: Int,
        timeLimitNs: Long
    ): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override suspend fun lock3AForCapture(frameLimit: Int, timeLimitNs: Long): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override fun setTorch(torchState: TorchState): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override fun startRepeating(request: Request) {
        repeatingRequests.release()
    }

    override fun stopRepeating() {
        stopRepeating.release()
    }

    override fun submit(request: Request) {
        throw NotImplementedError("Not used in testing")
    }

    override fun submit(requests: List<Request>) {
        // No-op
    }

    override suspend fun submit3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?
    ): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override suspend fun unlock3A(ae: Boolean?, af: Boolean?, awb: Boolean?): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override suspend fun unlock3APostCapture(): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }

    override fun update3A(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?
    ): Deferred<Result3A> {
        throw NotImplementedError("Not used in testing")
    }
}
