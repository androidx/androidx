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

package androidx.camera.camera2.adapter

import android.os.Handler
import android.os.HandlerThread
import androidx.camera.camera2.pipe.AudioRestrictionMode
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.ConfigQueryResult
import androidx.camera.camera2.pipe.FrameGraph
import androidx.camera.camera2.pipe.testing.FakeCameraBackend
import androidx.camera.camera2.pipe.testing.FakeCameraDevices
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.StreamSpecsCalculator.Companion.NO_OP_STREAM_SPECS_CALCULATOR
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class CameraFactoryProviderTest {
    private lateinit var schedulerThread: HandlerThread
    private lateinit var schedulerHandler: Handler
    private lateinit var fakeCameraPipe: FakeCameraPipe

    @Before
    fun setUp() {
        schedulerThread = HandlerThread("TestScheduler").apply { start() }
        schedulerHandler = Handler(schedulerThread.looper)

        val fakeCameraDevices =
            FakeCameraDevices(
                defaultCameraBackendId = FakeCameraBackend.FAKE_CAMERA_BACKEND_ID,
                concurrentCameraBackendIds = emptySet(),
                cameraMetadataMap = mapOf(FakeCameraBackend.FAKE_CAMERA_BACKEND_ID to emptyList()),
            )
        fakeCameraPipe = FakeCameraPipe(fakeCameraDevices)
    }

    @After
    fun tearDown() {
        schedulerThread.quitSafely()
        schedulerThread.join(1000)
    }

    @Test
    fun directExecutor_isKeptAsCameraExecutor() {
        val directExecutor = CameraXExecutors.directExecutor()

        val factory = createFactory(directExecutor)

        assertThat(factory.threadConfig.cameraExecutor).isSameInstanceAs(directExecutor)
        assertThat(factory.threadConfig.schedulerHandler).isSameInstanceAs(schedulerHandler)
        factory.shutdown()
        fakeCameraPipe.awaitShutdown()
    }

    @Test
    fun directExecutor_shutsDownCameraPipeOnDedicatedThread() {
        val factory = createFactory(CameraXExecutors.directExecutor())

        factory.shutdown()

        fakeCameraPipe.awaitShutdown()
        assertThat(fakeCameraPipe.shutdownThread).isNotSameInstanceAs(Thread.currentThread())
        assertThat(fakeCameraPipe.shutdownThread!!.name).isEqualTo("CXCP-Shutdown")
    }

    @Test
    fun customInlineExecutor_isKeptAndShutsDownCameraPipeOnDedicatedThread() {
        val customDirectExecutor = Executor { command -> command.run() }

        val factory = createFactory(customDirectExecutor)
        factory.shutdown()

        assertThat(factory.threadConfig.cameraExecutor).isSameInstanceAs(customDirectExecutor)
        fakeCameraPipe.awaitShutdown()
        assertThat(fakeCameraPipe.shutdownThread!!.name).isEqualTo("CXCP-Shutdown")
    }

    @Test
    fun backgroundExecutor_isKeptAndShutsDownCameraPipeSynchronously() {
        val backgroundExecutor = Executors.newSingleThreadExecutor()
        try {
            val factory = createFactory(backgroundExecutor)

            factory.shutdown()

            assertThat(factory.threadConfig.cameraExecutor).isSameInstanceAs(backgroundExecutor)
            // Non-direct executors keep the original synchronous shutdown behavior.
            assertThat(fakeCameraPipe.shutdownCount).isEqualTo(1)
            assertThat(fakeCameraPipe.shutdownThread).isSameInstanceAs(Thread.currentThread())
        } finally {
            backgroundExecutor.shutdownNow()
            backgroundExecutor.awaitTermination(1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun directExecutor_repeatedShutdown_shutsDownCameraPipeOnce() {
        val factory = createFactory(CameraXExecutors.directExecutor())

        factory.shutdown()
        factory.shutdown()

        fakeCameraPipe.awaitShutdown()
        assertThat(fakeCameraPipe.shutdownCount).isEqualTo(1)
    }

    private fun createFactory(cameraExecutor: Executor): CameraFactoryAdapter =
        CameraFactoryProvider(sharedCameraPipe = fakeCameraPipe)
            .newInstance(
                ApplicationProvider.getApplicationContext(),
                CameraThreadConfig.create(cameraExecutor, schedulerHandler),
                availableCamerasLimiter = null,
                cameraOpenRetryMaxTimeoutInMs = -1L,
                cameraXConfig = null,
                streamSpecsCalculator = NO_OP_STREAM_SPECS_CALCULATOR,
            ) as CameraFactoryAdapter

    private class FakeCameraPipe(private val cameraDevices: CameraDevices) : CameraPipe {
        private val shutdownLatch = CountDownLatch(1)

        @Volatile
        var shutdownThread: Thread? = null
            private set

        @Volatile
        var shutdownCount = 0
            private set

        fun awaitShutdown() {
            assertThat(shutdownLatch.await(5, TimeUnit.SECONDS)).isTrue()
        }

        override fun cameras(): CameraDevices = cameraDevices

        override fun shutdown() {
            try {
                shutdownThread = Thread.currentThread()
                shutdownCount++
            } finally {
                shutdownLatch.countDown()
            }
        }

        @Deprecated(
            "Use createCameraGraph instead.",
            replaceWith = ReplaceWith("createCameraGraph(config)"),
        )
        override fun create(config: CameraGraph.Config): CameraGraph =
            throw UnsupportedOperationException()

        override fun createCameraGraph(config: CameraGraph.Config): CameraGraph =
            throw UnsupportedOperationException()

        override fun createCameraGraphs(config: CameraGraph.ConcurrentConfig): List<CameraGraph> =
            throw UnsupportedOperationException()

        override fun createFrameGraph(frameGraphConfig: FrameGraph.Config): FrameGraph =
            throw UnsupportedOperationException()

        override fun createFrameGraphs(
            frameGraphConfigs: FrameGraph.ConcurrentConfig
        ): List<FrameGraph> = throw UnsupportedOperationException()

        override fun cameraSurfaceManager(): CameraSurfaceManager =
            throw UnsupportedOperationException()

        override suspend fun isConfigSupported(graphConfig: CameraGraph.Config): ConfigQueryResult =
            throw UnsupportedOperationException()

        override fun prewarmIsConfigSupported(graphConfig: CameraGraph.Config) =
            throw UnsupportedOperationException()

        override var globalAudioRestrictionMode: AudioRestrictionMode =
            AudioRestrictionMode.AUDIO_RESTRICTION_NONE
    }
}
