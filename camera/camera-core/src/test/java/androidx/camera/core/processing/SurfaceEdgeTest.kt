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
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.SurfaceRequest.Result.RESULT_REQUEST_CANCELLED
import androidx.camera.core.SurfaceRequest.TransformationInfo
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.DeferrableSurface.SurfaceClosedException
import androidx.camera.core.impl.DeferrableSurface.SurfaceUnavailableException
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.impl.ImageOutputConfig.ROTATION_NOT_SPECIFIED
import androidx.camera.core.impl.StreamSpec
import androidx.camera.core.impl.utils.TransformUtils.sizeToRect
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.fakes.FakeDeferrableSurface
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [SurfaceEdge].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SurfaceEdgeTest {

    companion object {
        private val INPUT_SIZE = Size(640, 480)
        private val FRAME_RATE = Range.create(30, 30)
        private val FRAME_SPEC =
            StreamSpec.builder(INPUT_SIZE).setExpectedFrameRateRange(FRAME_RATE).build()
        private val SENSOR_TO_BUFFER = Matrix().apply { setScale(-1f, 1f) }
    }

    private lateinit var surfaceEdge: SurfaceEdge
    private lateinit var fakeSurface: Surface
    private lateinit var fakeSurfaceTexture: SurfaceTexture
    private lateinit var provider: FakeDeferrableSurface

    @Before
    fun setUp() {
        surfaceEdge = SurfaceEdge(
            PREVIEW, INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            StreamSpec.builder(INPUT_SIZE).build(), SENSOR_TO_BUFFER, true, Rect(), 0,
            ROTATION_NOT_SPECIFIED, false
        )
        fakeSurfaceTexture = SurfaceTexture(0)
        fakeSurface = Surface(fakeSurfaceTexture)
        provider = FakeDeferrableSurface(INPUT_SIZE, ImageFormat.PRIVATE)
    }

    @After
    fun tearDown() {
        surfaceEdge.close()
        provider.close()
        fakeSurfaceTexture.release()
        fakeSurface.release()
    }

    @Test
    fun closeEdgeThenInvalidate_callbackNotInvoked() {
        // Arrange.
        var invalidated = false
        surfaceEdge.addOnInvalidatedListener {
            invalidated = true
        }
        val surfaceRequest = surfaceEdge.createSurfaceRequest(FakeCamera())
        // Act.
        surfaceEdge.close()
        surfaceRequest.invalidate()
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(invalidated).isFalse()
    }

    @Test
    fun invalidateWithoutProvider_settableSurfaceNotRecreated() {
        // Arrange.
        val deferrableSurface = surfaceEdge.deferrableSurfaceForTesting
        // Act.
        surfaceEdge.invalidate()
        // Assert.
        assertThat(surfaceEdge.deferrableSurfaceForTesting).isSameInstanceAs(deferrableSurface)
    }

    @Test
    fun invalidateWithProvider_settableSurfaceNotRecreated() {
        // Arrange.
        val deferrableSurface = surfaceEdge.deferrableSurfaceForTesting
        surfaceEdge.createSurfaceRequest(FakeCamera())
        // Act.
        surfaceEdge.invalidate()
        // Assert.
        assertThat(surfaceEdge.deferrableSurfaceForTesting).isNotSameInstanceAs(deferrableSurface)
    }

    @Test
    fun callCloseTwice_noException() {
        surfaceEdge.close()
        surfaceEdge.close()
    }

    @Test
    fun createWithStreamSpec_canGetStreamSpec() {
        val edge = SurfaceEdge(
            PREVIEW,
            INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            FRAME_SPEC,
            Matrix(),
            true,
            Rect(),
            0,
            ROTATION_NOT_SPECIFIED,
            false
        )
        assertThat(edge.streamSpec).isEqualTo(FRAME_SPEC)
    }

    @Test(expected = IllegalStateException::class)
    fun setProviderOnClosedEdge_throwsException() {
        surfaceEdge.close()
        surfaceEdge.setProvider(provider)
    }

    @Test(expected = IllegalStateException::class)
    fun getDeferrableSurfaceOnClosedEdge_throwsException() {
        surfaceEdge.close()
        surfaceEdge.deferrableSurface
    }

    @Test(expected = IllegalStateException::class)
    fun getSurfaceOutputOnClosedEdge_throwsException() {
        surfaceEdge.close()
        createSurfaceOutputFuture(surfaceEdge)
    }

    @Test(expected = IllegalStateException::class)
    fun createSurfaceRequest_throwsException() {
        surfaceEdge.close()
        surfaceEdge.createSurfaceRequest(FakeCamera())
    }

    @Test
    fun closeProviderOnNonUiThread_noCrash() {
        // Arrange.
        val providerDeferrableSurface = FakeDeferrableSurface(INPUT_SIZE, ImageFormat.PRIVATE)
        surfaceEdge.setProvider(providerDeferrableSurface)
        val nonUiExecutor = Executors.newSingleThreadExecutor()
        // Act.
        nonUiExecutor.execute {
            providerDeferrableSurface.close()
        }
        nonUiExecutor.shutdown()
        assertThat(nonUiExecutor.awaitTermination(1, TimeUnit.SECONDS)).isTrue()
        // Assert.
        assertThat(providerDeferrableSurface.isClosed).isTrue()
    }

    @Test
    fun closeProviderOnClosedEdge_noCrash() {
        // Arrange: create SurfaceRequest and close the edge.
        val providerDeferrableSurface = FakeDeferrableSurface(INPUT_SIZE, ImageFormat.PRIVATE)
        val edgeDeferrableSurface = surfaceEdge.deferrableSurface
        surfaceEdge.setProvider(providerDeferrableSurface)
        surfaceEdge.close()
        // Act: close the provider.
        providerDeferrableSurface.close()
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(edgeDeferrableSurface.isClosed).isTrue()
    }

    @Test
    fun closeSurfaceRequestProviderOnClosedEdge_noCrash() {
        // Arrange: create SurfaceRequest and close the edge.
        val surfaceRequest = surfaceEdge.createSurfaceRequest(FakeCamera())
        val edgeDeferrableSurface = surfaceEdge.deferrableSurface
        surfaceEdge.close()
        // Act: close the provider.
        surfaceRequest.deferrableSurface.close()
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(edgeDeferrableSurface.isClosed).isTrue()
    }

    @Test
    fun createSurfaceRequest_transformationInfoContainsSensorToBufferTransform() {
        // Act.
        val surfaceRequest = surfaceEdge.createSurfaceRequest(FakeCamera())
        var transformationInfo: TransformationInfo? = null
        surfaceRequest.setTransformationInfoListener(mainThreadExecutor()) {
            transformationInfo = it
        }
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(transformationInfo!!.sensorToBufferTransform).isEqualTo(SENSOR_TO_BUFFER)
    }

    @Test
    fun provideSurfaceThenImmediatelyInvalidate_surfaceOutputFails() {
        // Arrange: create SurfaceOutput and set provider.
        var succeeded = false
        var failed = false
        val surfaceOutput = createSurfaceOutputFuture(surfaceEdge)
        Futures.addCallback(surfaceOutput, object : FutureCallback<SurfaceOutput> {
            override fun onSuccess(result: SurfaceOutput?) {
                succeeded = true
            }

            override fun onFailure(t: Throwable) {
                failed = true
            }
        }, mainThreadExecutor())
        surfaceEdge.setProvider(provider)

        // Act: Provides Surface then immediately invalidate. The mSettableSurface is recreated
        // before the Surface Future callback is executed.
        provider.setSurface(fakeSurface)
        surfaceEdge.invalidate()
        shadowOf(getMainLooper()).idle()

        // Assert: surfaceOutput is not propagated.
        assertThat(failed).isTrue()
        assertThat(succeeded).isFalse()
    }

    @Test
    fun setSameProviderTwice_noException() {
        surfaceEdge.setProvider(provider)
        surfaceEdge.setProvider(provider)
    }

    @Test
    fun closeProvider_surfaceReleasedWhenRefCountingReaches0() {
        // Arrange: create edge with ref counting incremented.
        val surfaceRequest = surfaceEdge.createSurfaceRequest(FakeCamera())
        var result: SurfaceRequest.Result? = null
        surfaceRequest.provideSurface(fakeSurface, mainThreadExecutor()) {
            result = it
        }
        val parentDeferrableSurface = surfaceEdge.deferrableSurface
        parentDeferrableSurface.incrementUseCount()
        // Act: close the provider
        surfaceRequest.deferrableSurface.close()
        shadowOf(getMainLooper()).idle()
        // Assert: the surface is not released because the parent has ref counting.
        assertThat(result).isNull()
        // Act: decrease ref counting
        parentDeferrableSurface.decrementUseCount()
        shadowOf(getMainLooper()).idle()
        // Assert: the surface is released because the parent has not ref counting.
        assertThat(result!!.resultCode)
            .isEqualTo(SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY)
    }

    @Test
    fun closeChildProvider_parentEdgeClosed() {
        // Arrange.
        val parentEdge = SurfaceEdge(
            PREVIEW, INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            StreamSpec.builder(INPUT_SIZE).build(), SENSOR_TO_BUFFER, true, Rect(), 0,
            ROTATION_NOT_SPECIFIED, false
        )
        val childDeferrableSurface = surfaceEdge.deferrableSurface
        parentEdge.setProvider(childDeferrableSurface)
        // Act.
        childDeferrableSurface.close()
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(parentEdge.deferrableSurface.isClosed).isTrue()
        // Clean up.
        parentEdge.close()
    }

    @Test(expected = SurfaceClosedException::class)
    fun connectToClosedProvider_getsException() {
        provider.close()
        surfaceEdge.setProvider(provider)
    }

    @Test
    fun createSurfaceRequestAndCancel_cancellationIsPropagated() {
        // Arrange: create a SurfaceRequest.
        val surfaceRequest = surfaceEdge.createSurfaceRequest(FakeCamera())
        var throwable: Throwable? = null
        Futures.addCallback(
            surfaceEdge.deferrableSurface.surface,
            object : FutureCallback<Surface> {
                override fun onFailure(t: Throwable) {
                    throwable = t
                }

                override fun onSuccess(result: Surface?) {
                    throw IllegalStateException("Should not succeed.")
                }
            },
            mainThreadExecutor()
        )

        // Act: set it as "will not provide".
        surfaceRequest.willNotProvideSurface()
        shadowOf(getMainLooper()).idle()

        // Assert: the DeferrableSurface returns an error.
        assertThat(throwable).isInstanceOf(SurfaceUnavailableException::class.java)
    }

    @Test
    fun createSurfaceRequestThenDisconnect_surfaceRequestCancelled() {
        // Arrange: create a SurfaceRequest then close.
        val surfaceRequest = surfaceEdge.createSurfaceRequest(FakeCamera())
        surfaceEdge.disconnect()

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
    fun createSurfaceOutputWithDisconnectedEdge_surfaceOutputNotCreated() {
        // Arrange: create a SurfaceOutput future from a closed Edge
        surfaceEdge.setProvider(provider)
        provider.setSurface(fakeSurface)
        surfaceEdge.disconnect()
        // Act: wait for the SurfaceOutput to return.
        val surfaceOutput = getSurfaceOutputFromFuture(createSurfaceOutputFuture(surfaceEdge))

        // Assert: the SurfaceOutput is not created.
        assertThat(surfaceOutput).isNull()
    }

    @Test
    fun createSurfaceOutput_inheritSurfaceEdgeTransformation() {
        // Arrange: set the provider and create a SurfaceOutput future.
        surfaceEdge.setProvider(provider)
        provider.setSurface(fakeSurface)
        // Act: create a SurfaceOutput from the SurfaceEdge
        val surfaceOutput = getSurfaceOutputFromFuture(createSurfaceOutputFuture(surfaceEdge))
        // Assert: the SurfaceOutput inherits the transformation from the SurfaceEdge.
        assertThat(surfaceOutput!!.sensorToBufferTransform).isEqualTo(SENSOR_TO_BUFFER)
    }

    @Test
    fun createSurfaceRequestAndInvalidate_edgeResets() {
        // Arrange: listen for the reset.
        var isReset = false
        val surfaceRequest = surfaceEdge.createSurfaceRequest(FakeCamera())
        surfaceEdge.addOnInvalidatedListener { isReset = true }
        // Act: invalidate the SurfaceRequest.
        surfaceRequest.invalidate()
        shadowOf(getMainLooper()).idle()
        // Assert: edge is reset.
        assertThat(isReset).isTrue()
    }

    @Test
    fun createSurfaceRequestAndProvide_surfaceIsPropagated() {
        // Arrange: create a SurfaceRequest.
        val surfaceRequest = surfaceEdge.createSurfaceRequest(FakeCamera())
        // Act: provide Surface.
        surfaceRequest.provideSurface(fakeSurface, mainThreadExecutor()) {}
        shadowOf(getMainLooper()).idle()
        // Assert: the surface is received.
        val deferrableSurface = surfaceEdge.deferrableSurface
        assertThat(deferrableSurface.surface.isDone).isTrue()
        assertThat(deferrableSurface.surface.get()).isEqualTo(fakeSurface)
    }

    @Test
    fun createSurfaceRequest_hasCameraTransformSetCorrectly() {
        assertThat(getSurfaceRequestHasTransform(true)).isTrue()
        assertThat(getSurfaceRequestHasTransform(false)).isFalse()
    }

    /**
     * Creates a [SurfaceEdge] with the given hasCameraTransform value, and returns the
     * [TransformationInfo.hasCameraTransform] from the [SurfaceRequest].
     */
    private fun getSurfaceRequestHasTransform(hasCameraTransform: Boolean): Boolean {
        // Arrange.
        val surface = SurfaceEdge(
            PREVIEW,
            INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            StreamSpec.builder(Size(640, 480)).build(),
            Matrix(),
            hasCameraTransform,
            Rect(),
            0,
            ROTATION_NOT_SPECIFIED,
            false
        )
        var transformationInfo: TransformationInfo? = null

        // Act: get the hasCameraTransform bit from the SurfaceRequest.
        surface.createSurfaceRequest(FakeCamera()).setTransformationInfoListener(
            mainThreadExecutor()
        ) {
            transformationInfo = it
        }
        shadowOf(getMainLooper()).idle()
        surface.close()
        return transformationInfo!!.hasCameraTransform()
    }

    @Test
    fun setSourceSurfaceFutureAndProvide_surfaceIsPropagated() {
        // Arrange: set a ListenableFuture<Surface> as the source.
        var completer: CallbackToFutureAdapter.Completer<Surface>? = null
        val surfaceFuture = CallbackToFutureAdapter.getFuture {
            completer = it
            return@getFuture null
        }
        surfaceEdge.setProvider(object : DeferrableSurface(INPUT_SIZE, ImageFormat.PRIVATE) {
            override fun provideSurface(): ListenableFuture<Surface> {
                return surfaceFuture
            }
        })
        // Act: provide Surface.
        completer!!.set(fakeSurface)
        shadowOf(getMainLooper()).idle()
        // Assert: the surface is received.
        val deferrableSurface = surfaceEdge.deferrableSurface
        assertThat(deferrableSurface.surface.isDone).isTrue()
        assertThat(deferrableSurface.surface.get()).isEqualTo(fakeSurface)
    }

    @Test
    fun linkBothProviderAndConsumer_surfaceAndResultsArePropagatedE2E() {
        // Arrange: link a LinkableSurface with a SurfaceRequest and a SurfaceOutput.
        val surfaceRequest = surfaceEdge.createSurfaceRequest(FakeCamera())
        val surfaceOutputFuture = createSurfaceOutputFuture(surfaceEdge)
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
        surfaceEdge.disconnect()
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

    @Test
    fun createSurfaceRequestThenInvalidate_canCreateSurfaceRequestAgain() {
        // Arrange: set up the connection E2E
        linkBothProviderAndConsumer_surfaceAndResultsArePropagatedE2E()
        // Act: invalidate.
        surfaceEdge.invalidate()
        // Arrange: set up the connection E2E again
        linkBothProviderAndConsumer_surfaceAndResultsArePropagatedE2E()
    }

    @Test
    fun close_providerDeferrableSurfaceNotClosed() {
        // Arrange.
        surfaceEdge.setProvider(provider)
        // Act.
        surfaceEdge.close()
        shadowOf(getMainLooper()).idle()
        // Assert: provider is not closed. The creator is responsible for closing it.
        assertThat(provider.isClosed).isFalse()
    }

    @Test
    fun close_surfaceRequestClosed() {
        // Arrange.
        var surfaceRequestClosed = false
        val surfaceRequest = surfaceEdge.createSurfaceRequest(FakeCamera())
        surfaceRequest.provideSurface(fakeSurface, mainThreadExecutor()) {
            surfaceRequestClosed = true
        }
        // Act.
        surfaceEdge.close()
        shadowOf(getMainLooper()).idle()
        // Assert: SurfaceRequest is closed and the app should release the Surface.
        assertThat(surfaceRequestClosed).isTrue()
    }

    @Test
    fun setProviderThenInvalidate_canSetProviderAgain() {
        // Arrange: set the provider and then invalidate.
        val newProvider = FakeDeferrableSurface(INPUT_SIZE, ImageFormat.PRIVATE)
        surfaceEdge.setProvider(provider)
        surfaceEdge.invalidate()
        // Act: set the provider again.
        surfaceEdge.setProvider(newProvider)
        // Assert: drain the main thread and there is no crash.
        shadowOf(getMainLooper()).idle()
        newProvider.close()
    }

    @Test(expected = IllegalStateException::class)
    fun getDeferrableSurfaceThenSurfaceOutput_throwsException() {
        createSurfaceOutputFuture(surfaceEdge)
        surfaceEdge.deferrableSurface
    }

    @Test(expected = IllegalStateException::class)
    fun getSurfaceOutputThenDeferrableSurface_throwsException() {
        surfaceEdge.deferrableSurface
        createSurfaceOutputFuture(surfaceEdge)
    }

    @Test(expected = IllegalStateException::class)
    fun createSurfaceRequestThenSetProvider_throwsException() {
        surfaceEdge.createSurfaceRequest(FakeCamera())
        surfaceEdge.setProvider(provider)
    }

    @Test(expected = IllegalStateException::class)
    fun setProviderThenCreateSurfaceRequest_throwsException() {
        surfaceEdge.setProvider(provider)
        surfaceEdge.createSurfaceRequest(FakeCamera())
    }

    @Test(expected = IllegalArgumentException::class)
    fun setProviderWithDifferentSize_throwsException() {
        val providerWithWrongSize = FakeDeferrableSurface(Size(10, 20), ImageFormat.PRIVATE)
        try {
            surfaceEdge.setProvider(providerWithWrongSize)
        } finally {
            providerWithWrongSize.close()
        }
    }

    @Test
    fun setRotationDegrees_sendTransformationInfoUpdate() {
        // Arrange.
        var transformationInfo: TransformationInfo? = null
        val surfaceRequest = surfaceEdge.createSurfaceRequest(FakeCamera())
        surfaceRequest.setTransformationInfoListener(mainThreadExecutor()) {
            transformationInfo = it
        }

        // Act.
        surfaceEdge.updateTransformation(90)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(transformationInfo).isNotNull()
        assertThat(transformationInfo!!.rotationDegrees).isEqualTo(90)
    }

    private fun getSurfaceOutputFromFuture(
        future: ListenableFuture<SurfaceOutput>
    ): SurfaceOutput? {
        var surfaceOutput: SurfaceOutput? = null
        Futures.addCallback(future, object : FutureCallback<SurfaceOutput> {
            override fun onSuccess(result: SurfaceOutput?) {
                surfaceOutput = result
            }

            override fun onFailure(t: Throwable) {
            }
        }, mainThreadExecutor())
        shadowOf(getMainLooper()).idle()
        return surfaceOutput
    }

    private fun createSurfaceOutputFuture(surfaceEdge: SurfaceEdge) =
        surfaceEdge.createSurfaceOutputFuture(
            INPUT_SIZE,
            INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE,
            sizeToRect(INPUT_SIZE),
            /*rotationDegrees=*/0,
            /*mirroring=*/false,
            FakeCamera()
        )
}
