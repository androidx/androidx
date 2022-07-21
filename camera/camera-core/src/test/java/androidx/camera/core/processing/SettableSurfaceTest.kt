/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.processing

import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceEffect
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.SurfaceRequest.Result.RESULT_REQUEST_CANCELLED
import androidx.camera.core.impl.DeferrableSurface.SurfaceClosedException
import androidx.camera.core.impl.DeferrableSurface.SurfaceUnavailableException
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.fakes.FakeCamera
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [SettableSurface].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SettableSurfaceTest {

    companion object {
        private val IDENTITY_MATRIX = FloatArray(16).apply {
            android.opengl.Matrix.setIdentityM(this, 0)
        }
    }

    private lateinit var settableSurface: SettableSurface
    private lateinit var fakeSurface: Surface
    private lateinit var fakeSurfaceTexture: SurfaceTexture

    @Before
    fun setUp() {
        settableSurface = SettableSurface(
            SurfaceEffect.PREVIEW, Size(640, 480), ImageFormat.PRIVATE,
            Matrix(), true, Rect(), 0, false
        )
        fakeSurfaceTexture = SurfaceTexture(0)
        fakeSurface = Surface(fakeSurfaceTexture)
    }

    @After
    fun tearDown() {
        settableSurface.close()
        fakeSurfaceTexture.release()
        fakeSurface.release()
    }

    @Test
    fun closeProviderAfterConnected_surfaceNotReleased() {
        // Arrange.
        val surfaceRequest = settableSurface.createSurfaceRequest(FakeCamera())
        var result: SurfaceRequest.Result? = null
        surfaceRequest.provideSurface(fakeSurface, mainThreadExecutor()) {
            result = it
        }
        // Act: close the provider
        surfaceRequest.deferrableSurface.close()
        shadowOf(getMainLooper()).idle()
        // Assert: the surface is not released because the parent is not closed.
        assertThat(result).isNull()
    }

    @Test(expected = SurfaceClosedException::class)
    fun connectToClosedProvider_getsException() {
        val closedDeferrableSurface = ImmediateSurface(fakeSurface).apply {
            this.close()
        }
        settableSurface.setProvider(closedDeferrableSurface)
    }

    @Test
    fun createSurfaceRequestAndCancel_cancellationIsPropagated() {
        // Arrange: create a SurfaceRequest.
        val surfaceRequest = settableSurface.createSurfaceRequest(FakeCamera())
        var throwable: Throwable? = null
        Futures.addCallback(settableSurface.surface, object : FutureCallback<Surface> {
            override fun onFailure(t: Throwable) {
                throwable = t
            }

            override fun onSuccess(result: Surface?) {
                throw IllegalStateException("Should not succeed.")
            }
        }, mainThreadExecutor())

        // Act: set it as "will not provide".
        surfaceRequest.willNotProvideSurface()
        shadowOf(getMainLooper()).idle()

        // Assert: the DeferrableSurface returns an error.
        assertThat(throwable).isInstanceOf(SurfaceUnavailableException::class.java)
    }

    @Test
    fun createSurfaceRequestWithClosedInstance_surfaceRequestCancelled() {
        // Arrange: create a SurfaceRequest from a closed LinkableSurface
        settableSurface.close()
        val surfaceRequest = settableSurface.createSurfaceRequest(FakeCamera())

        // Act: provide a Surface and get the result.
        var result: SurfaceRequest.Result? = null
        surfaceRequest.provideSurface(fakeSurface, mainThreadExecutor()) {
            result = it
        }
        shadowOf(getMainLooper()).idle()

        // Assert: the Surface is never used.
        assertThat(result!!.resultCode).isEqualTo(RESULT_REQUEST_CANCELLED)
    }

    @Test
    fun createSurfaceOutputWithClosedInstance_surfaceOutputNotCreated() {
        // Arrange: create a SurfaceOutput future from a closed LinkableSurface
        settableSurface.close()
        val surfaceOutput = settableSurface.createSurfaceOutputFuture(IDENTITY_MATRIX)

        // Act: wait for the SurfaceOutput to return.
        var successful: Boolean? = null
        Futures.addCallback(surfaceOutput, object : FutureCallback<SurfaceOutput> {
            override fun onSuccess(result: SurfaceOutput?) {
                successful = true
            }

            override fun onFailure(t: Throwable) {
                successful = false
            }
        }, mainThreadExecutor())
        shadowOf(getMainLooper()).idle()

        // Assert: the SurfaceOutput is not created.
        assertThat(successful!!).isEqualTo(false)
    }

    @Test
    fun createSurfaceRequestAndProvide_surfaceIsPropagated() {
        // Arrange: create a SurfaceRequest.
        val surfaceRequest = settableSurface.createSurfaceRequest(FakeCamera())
        // Act: provide Surface.
        surfaceRequest.provideSurface(fakeSurface, mainThreadExecutor()) {}
        shadowOf(getMainLooper()).idle()
        // Assert: the surface is received.
        assertThat(settableSurface.surface.isDone).isTrue()
        assertThat(settableSurface.surface.get()).isEqualTo(fakeSurface)
    }

    @Test
    fun setSourceSurfaceFutureAndProvide_surfaceIsPropagated() {
        // Arrange: set a ListenableFuture<Surface> as the source.
        var completer: CallbackToFutureAdapter.Completer<Surface>? = null
        val surfaceFuture = CallbackToFutureAdapter.getFuture {
            completer = it
            return@getFuture null
        }
        settableSurface.setProvider(surfaceFuture)
        // Act: provide Surface.
        completer!!.set(fakeSurface)
        shadowOf(getMainLooper()).idle()
        // Assert: the surface is received.
        assertThat(settableSurface.surface.isDone).isTrue()
        assertThat(settableSurface.surface.get()).isEqualTo(fakeSurface)
    }

    @Test
    fun linkBothProviderAndConsumer_surfaceAndResultsArePropagatedE2E() {
        // Arrange: link a LinkableSurface with a SurfaceRequest and a SurfaceOutput.
        val surfaceRequest = settableSurface.createSurfaceRequest(FakeCamera())
        val surfaceOutputFuture = settableSurface.createSurfaceOutputFuture(IDENTITY_MATRIX)
        var surfaceOutput: SurfaceOutput? = null
        Futures.transform(surfaceOutputFuture, {
            surfaceOutput = it
        }, mainThreadExecutor())

        // Act: provide a Surface via the SurfaceRequest.
        var isSurfaceReleased = false
        surfaceRequest.provideSurface(fakeSurface, mainThreadExecutor()) {
            isSurfaceReleased = true
        }
        shadowOf(getMainLooper()).idle()

        // Assert: SurfaceOutput is received and it contains the right Surface
        assertThat(surfaceOutput).isNotNull()
        var surfaceOutputCloseRequested = false
        val surface = surfaceOutput!!.getSurface(mainThreadExecutor()) {
            surfaceOutputCloseRequested = true
        }
        shadowOf(getMainLooper()).idle()
        assertThat(surface).isEqualTo(fakeSurface)
        assertThat(isSurfaceReleased).isEqualTo(false)

        // Act: close the LinkableSurface, signaling the intention to close the Surface.
        settableSurface.close()
        shadowOf(getMainLooper()).idle()

        // Assert: The close is propagated to the SurfaceRequest.
        assertThat(surfaceOutputCloseRequested).isEqualTo(true)
        assertThat(isSurfaceReleased).isEqualTo(false)

        // Act: close the LinkableSurface, signaling it's safe to release the Surface.
        surfaceOutput!!.close()
        shadowOf(getMainLooper()).idle()

        // Assert: The close is propagated to the SurfaceRequest.
        assertThat(isSurfaceReleased).isEqualTo(true)
    }

    @Test(expected = IllegalStateException::class)
    fun createSurfaceRequestTwice_throwsException() {
        settableSurface.createSurfaceRequest(FakeCamera())
        settableSurface.createSurfaceRequest(FakeCamera())
        shadowOf(getMainLooper()).idle()
    }

    @Test(expected = IllegalStateException::class)
    fun createSurfaceOutputTwice_throwsException() {
        settableSurface.createSurfaceOutputFuture(IDENTITY_MATRIX)
        settableSurface.createSurfaceOutputFuture(IDENTITY_MATRIX)
        shadowOf(getMainLooper()).idle()
    }
}