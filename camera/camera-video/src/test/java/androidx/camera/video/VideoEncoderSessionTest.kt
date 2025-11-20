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

package androidx.camera.video

import android.os.Looper
import android.util.Size
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.fakes.FakeEncoder
import androidx.camera.testing.impl.fakes.FakeEncoderSurfaceInput
import androidx.camera.testing.impl.fakes.createFakeVideoEncoderConfig
import androidx.camera.video.internal.encoder.Encoder
import androidx.camera.video.internal.encoder.EncoderFactory
import androidx.camera.video.internal.encoder.InvalidConfigException
import androidx.camera.video.internal.encoder.VideoEncoderConfig
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
@DoNotInstrument
class VideoEncoderSessionTest {

    private lateinit var sequentialExecutor: Executor
    private lateinit var executor: ExecutorService
    private lateinit var encoderFactory: EncoderFactory
    private lateinit var encoder: FakeEncoder
    private lateinit var encoderInput: Encoder.SurfaceInput
    private lateinit var surfaceRequest: SurfaceRequest
    private lateinit var videoEncoderConfig: VideoEncoderConfig
    private lateinit var fakeCamera: FakeCamera

    private val surfaceRequest2 = lazy { SurfaceRequest(Size(640, 480), fakeCamera) {} }

    @Before
    fun setUp() {
        sequentialExecutor = CameraXExecutors.newSequentialExecutor(mainThreadExecutor())
        executor = mainThreadExecutor()
        encoderInput = FakeEncoderSurfaceInput()
        encoder = FakeEncoder(encoderInput = encoderInput)
        encoderFactory = EncoderFactory { _, _, _ -> encoder }
        videoEncoderConfig = createFakeVideoEncoderConfig()
        fakeCamera = FakeCamera()
        surfaceRequest = SurfaceRequest(Size(640, 480), fakeCamera) {}
    }

    @After
    fun tearDown() {
        if (surfaceRequest2.isInitialized()) {
            surfaceRequest2.value.deferrableSurface.close()
        }
        surfaceRequest.deferrableSurface.close()
        encoderInput.surface.release()
    }

    @Test
    fun configure_succeeds_andProvidesSurface() {
        // Arrange
        val session = VideoEncoderSession(encoderFactory, sequentialExecutor, executor)

        // Act
        val configFuture = session.configure(surfaceRequest, videoEncoderConfig)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(configFuture.isDone).isTrue()
        assertThat(configFuture.get()).isEqualTo(encoder)
        assertThat(session.activeSurface).isEqualTo(encoderInput.surface)
        assertThat(session.videoEncoder).isEqualTo(encoder)
        assertThat(surfaceRequest.isServiced).isTrue()
    }

    @Test
    fun configure_throwsException_whenEncoderFactoryFails() {
        // Arrange
        val encoderFactory = EncoderFactory { _, _, _ ->
            throw InvalidConfigException("Failed in purpose")
        }
        val session = VideoEncoderSession(encoderFactory, sequentialExecutor, executor)

        // Act
        val configFuture = session.configure(surfaceRequest, videoEncoderConfig)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(configFuture.isDone).isTrue()
        assertThat(configFuture.isCancelled).isFalse()
        var exception: InvalidConfigException? = null
        try {
            configFuture.get()
        } catch (e: Exception) {
            exception = e.cause as? InvalidConfigException
        }
        assertThat(exception).isNotNull()
        assertThat(session.activeSurface).isNull()
    }

    @Test
    fun configure_throwsException_whenCalledMoreThanOnce() {
        // Arrange
        val session = VideoEncoderSession(encoderFactory, sequentialExecutor, executor)
        session.configure(surfaceRequest, videoEncoderConfig)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Act
        val future = session.configure(surfaceRequest2.value, videoEncoderConfig)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(future.isDone).isTrue()
        assertThat(future.isCancelled).isFalse()
        var exception: IllegalStateException? = null
        try {
            future.get()
        } catch (e: Exception) {
            exception = e.cause as? IllegalStateException
        }
        assertThat(exception).isNotNull()
    }

    @Test
    fun signalTermination_beforeConfigure_terminatesImmediately() {
        // Arrange
        val session = VideoEncoderSession(encoderFactory, sequentialExecutor, executor)

        // Act
        val releasedFuture = session.signalTermination()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(releasedFuture.isDone).isTrue()
        assertThat(session.activeSurface).isNull()
        assertThat(session.videoEncoder).isNull()
    }

    @Test
    fun signalTermination_afterConfigure_setsPendingRelease() {
        // Arrange
        val session = VideoEncoderSession(encoderFactory, sequentialExecutor, executor)
        session.configure(surfaceRequest, videoEncoderConfig)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Act
        val releasedFuture = session.signalTermination()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(releasedFuture.isDone).isFalse()
        assertThat(encoder.isReleaseCalled).isFalse()
        assertThat(session.activeSurface).isNull()
        assertThat(session.videoEncoder).isNotNull()
    }

    @Test
    fun terminateNow_releasesEncoder() {
        // Arrange
        val session = VideoEncoderSession(encoderFactory, sequentialExecutor, executor)
        session.configure(surfaceRequest, videoEncoderConfig)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Act
        session.terminateNow()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(encoder.isReleaseCalled).isTrue()
        assertThat(session.activeSurface).isNull()
        assertThat(session.videoEncoder).isNull()
    }

    @Test
    fun isConfiguredSurfaceRequest_returnsCorrectState() {
        // Arrange
        val session = VideoEncoderSession(encoderFactory, sequentialExecutor, executor)

        // Act & Assert
        assertThat(session.isConfiguredSurfaceRequest(surfaceRequest)).isFalse()

        session.configure(surfaceRequest, videoEncoderConfig)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(session.isConfiguredSurfaceRequest(surfaceRequest)).isTrue()
        assertThat(session.isConfiguredSurfaceRequest(surfaceRequest2.value)).isFalse()

        session.signalTermination()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(session.isConfiguredSurfaceRequest(surfaceRequest)).isFalse()
    }

    @Test
    fun onSurfaceRequestComplete_releasesSurface() {
        // Arrange
        val session = VideoEncoderSession(encoderFactory, sequentialExecutor, executor)
        session.configure(surfaceRequest, videoEncoderConfig)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Act
        surfaceRequest.deferrableSurface.close()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Assert
        assertThat(session.activeSurface).isNull()
    }
}
