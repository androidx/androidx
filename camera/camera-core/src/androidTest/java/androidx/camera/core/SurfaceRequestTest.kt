/*
 * Copyright 2020 The Android Open Source Project
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

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.impl.GarbageCollectionUtil
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

@SmallTest
@RunWith(AndroidJUnit4::class)
class SurfaceRequestTest {
    private val surfaceRequests: MutableList<SurfaceRequest> = ArrayList()

    @After
    fun tearDown() {
        // Ensure all requests complete
        for (request in surfaceRequests) {
            // Closing the deferrable surface should cause the request to be cancelled if it has
            // not yet been completed.
            request.deferrableSurface.close()
        }
    }

    @Test
    fun canRetrieveResolution() {
        val resolution = Size(640, 480)
        val request = createNewRequest(resolution)
        Truth.assertThat(request.resolution).isEqualTo(resolution)
    }

    @Test
    fun canRetrieveExpectedFrameRate() {
        val resolution = Size(640, 480)
        val expectedFrameRate = Range<Int>(12, 30)
        val request = createNewRequest(resolution, expectedFrameRate = expectedFrameRate)
        Truth.assertThat(request.expectedFrameRate).isEqualTo(expectedFrameRate)
    }

    @Test
    fun expectedFrameRateIsUnspecified_whenNotSet() {
        val resolution = Size(640, 480)
        val request = createNewRequest(resolution)
        Truth.assertThat(request.expectedFrameRate)
            .isEqualTo(SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED)
    }

    @Test
    fun canRetrieveDynamicRange() {
        val dynamicRange = DynamicRange.HLG_10_BIT
        val request = createNewRequest(FAKE_SIZE, dynamicRange)
        Truth.assertThat(request.dynamicRange).isEqualTo(dynamicRange)
    }

    @Test
    fun dynamicRangeIsSdr_whenNotSet() {
        val request = createNewRequest(FAKE_SIZE)
        Truth.assertThat(request.dynamicRange).isEqualTo(DynamicRange.SDR)
    }

    @Test
    fun surfaceRequestConstructor_withUnspecifiedDynamicRange_throwsException() {
        assertThrows<IllegalArgumentException> {
            createNewRequest(size = FAKE_SIZE, dynamicRange = DynamicRange.UNSPECIFIED)
        }
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun setWillNotProvideSurface_resultsInWILL_NOT_PROVIDE_SURFACE() {
        val request = createNewRequest(FAKE_SIZE)
        val listener: Consumer<SurfaceRequest.Result> =
            Mockito.mock(Consumer::class.java) as Consumer<SurfaceRequest.Result>
        request.willNotProvideSurface()
        request.provideSurface(SURFACE, CameraXExecutors.directExecutor(), listener)
        Mockito.verify(listener)
            .accept(
                ArgumentMatchers.eq(
                    SurfaceRequest.Result.of(
                        SurfaceRequest.Result.RESULT_WILL_NOT_PROVIDE_SURFACE,
                        SURFACE,
                    )
                )
            )
    }

    @Test
    fun willNotProvideSurface_returnsFalse_whenAlreadyCompleted() {
        val request = createNewRequest(FAKE_SIZE)

        // Complete the request
        request.provideSurface(SURFACE, CameraXExecutors.directExecutor(), NO_OP_RESULT_LISTENER)
        Truth.assertThat(request.willNotProvideSurface()).isFalse()
    }

    @Test
    fun willNotProvideSurface_returnsFalse_whenRequestIsCancelled() {
        val request = createNewRequest(FAKE_SIZE)

        // Cause request to be cancelled from producer side
        request.deferrableSurface.close()
        Truth.assertThat(request.willNotProvideSurface()).isFalse()
    }

    @Test
    fun willNotProvideSurface_returnsTrue_whenNotYetCompleted() {
        val request = createNewRequest(FAKE_SIZE)
        Truth.assertThat(request.willNotProvideSurface()).isTrue()
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun surfaceRequestResult_completesSuccessfully_afterProducerIsDone() {
        val request = createNewRequest(FAKE_SIZE)
        val listener: Consumer<SurfaceRequest.Result> =
            Mockito.mock(Consumer::class.java) as Consumer<SurfaceRequest.Result>
        request.provideSurface(
            SURFACE,
            ContextCompat.getMainExecutor(ApplicationProvider.getApplicationContext()),
            listener,
        )

        // Cause request to be completed from producer side
        request.deferrableSurface.close()
        Mockito.verify(listener, Mockito.timeout(500))
            .accept(
                ArgumentMatchers.eq(
                    SurfaceRequest.Result.of(
                        SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY,
                        SURFACE,
                    )
                )
            )
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun provideSurface_resultsInSURFACE_ALREADY_PROVIDED_onSecondInvocation() {
        val request = createNewRequest(FAKE_SIZE)
        val listener: Consumer<SurfaceRequest.Result> =
            Mockito.mock(Consumer::class.java) as Consumer<SurfaceRequest.Result>
        request.provideSurface(SURFACE, CameraXExecutors.directExecutor(), NO_OP_RESULT_LISTENER)
        request.provideSurface(SURFACE, CameraXExecutors.directExecutor(), listener)
        Mockito.verify(listener)
            .accept(
                ArgumentMatchers.eq(
                    SurfaceRequest.Result.of(
                        SurfaceRequest.Result.RESULT_SURFACE_ALREADY_PROVIDED,
                        SURFACE,
                    )
                )
            )
    }

    @Test
    fun handleInvalidate_runWhenInvalidateCalled() {
        // Arrange.
        var isCalled = false
        val request = createNewRequest(FAKE_SIZE) { isCalled = true }
        Truth.assertThat(isCalled).isFalse()

        // Act.
        request.willNotProvideSurface()
        request.invalidate()

        // Assert.
        Truth.assertThat(isCalled).isTrue()
    }

    @Test
    fun isServiced_trueAfterProvideSurface() {
        val request = createNewRequest(FAKE_SIZE)
        request.provideSurface(SURFACE, CameraXExecutors.directExecutor(), NO_OP_RESULT_LISTENER)
        Truth.assertThat(request.isServiced).isTrue()
    }

    @Test
    fun isServiced_trueAfterWillNotProvideSurface() {
        val request = createNewRequest(FAKE_SIZE)
        request.willNotProvideSurface()
        Truth.assertThat(request.isServiced).isTrue()
    }

    @Test
    fun isServiced_falseInitially() {
        val request = createNewRequest(FAKE_SIZE)
        Truth.assertThat(request.isServiced).isFalse()
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun cancelledRequest_resultsInREQUEST_CANCELLED() {
        val request = createNewRequest(FAKE_SIZE)

        // Cause request to be cancelled from producer side
        request.deferrableSurface.close()
        val listener: Consumer<SurfaceRequest.Result> =
            Mockito.mock(Consumer::class.java) as Consumer<SurfaceRequest.Result>
        request.provideSurface(SURFACE, CameraXExecutors.directExecutor(), listener)
        Mockito.verify(listener)
            .accept(
                ArgumentMatchers.eq(
                    SurfaceRequest.Result.of(
                        SurfaceRequest.Result.RESULT_REQUEST_CANCELLED,
                        SURFACE,
                    )
                )
            )
    }

    @Test
    fun cancelledRequest_callsCancellationListener_whenCancelledAfterAddingListener() {
        val request = createNewRequest(FAKE_SIZE)
        val listener = Mockito.mock(Runnable::class.java)
        request.addRequestCancellationListener(
            ContextCompat.getMainExecutor(ApplicationProvider.getApplicationContext()),
            listener,
        )

        // Cause request to be cancelled from producer side
        request.deferrableSurface.close()
        Mockito.verify(listener, Mockito.timeout(500)).run()
    }

    @Test
    fun cancelledRequest_callsCancellationListener_whenCancelledBeforeAddingListener() {
        val request = createNewRequest(FAKE_SIZE)

        // Cause request to be cancelled from producer side
        request.deferrableSurface.close()
        val listener = Mockito.mock(Runnable::class.java)
        request.addRequestCancellationListener(
            ContextCompat.getMainExecutor(ApplicationProvider.getApplicationContext()),
            listener,
        )
        Mockito.verify(listener, Mockito.timeout(500)).run()
    }

    @Test
    fun setListenerWhenTransformationAvailable_receivesImmediately() {
        // Arrange.
        val request = createNewRequest(FAKE_SIZE)
        request.updateTransformationInfo(FAKE_INFO)
        val infoReference = AtomicReference<SurfaceRequest.TransformationInfo>()

        // Act.
        request.setTransformationInfoListener(CameraXExecutors.directExecutor()) {
            newValue: SurfaceRequest.TransformationInfo ->
            infoReference.set(newValue)
        }

        // Assert.
        Truth.assertThat(infoReference.get()).isEqualTo(FAKE_INFO)
    }

    @Test
    fun setListener_receivesCallbackWhenAvailable() {
        // Arrange.
        val request = createNewRequest(FAKE_SIZE)
        val infoReference = AtomicReference<SurfaceRequest.TransformationInfo>()
        request.setTransformationInfoListener(CameraXExecutors.directExecutor()) {
            newValue: SurfaceRequest.TransformationInfo ->
            infoReference.set(newValue)
        }
        Truth.assertThat(infoReference.get()).isNull()

        // Act.
        request.updateTransformationInfo(FAKE_INFO)

        // Assert.
        Truth.assertThat(infoReference.get()).isEqualTo(FAKE_INFO)
    }

    // request assigned to null to make GC eligible
    @LargeTest
    @Test
    @Throws(TimeoutException::class, InterruptedException::class)
    fun deferrableSurface_stronglyReferencesSurfaceRequest() {
        // Arrange.
        var request: SurfaceRequest? = createNewRequestWithoutAutoCleanup(FAKE_SIZE)
        // Retrieve the DeferrableSurface which should maintain the strong reference to the
        // SurfaceRequest
        val deferrableSurface = request!!.deferrableSurface
        val referenceQueue = ReferenceQueue<SurfaceRequest?>()
        // Ensure surface request garbage collection is tracked
        val phantomReference = PhantomReference(request, referenceQueue)
        try {
            // Act.
            // Null out the original reference to the SurfaceRequest. DeferrableSurface should be
            // the only reference remaining.
            null.also { request = it }
            GarbageCollectionUtil.runFinalization()
            val requestFinalized = referenceQueue.poll() != null

            // Assert.
            Truth.assertThat(requestFinalized).isFalse()
        } finally {
            // Clean up
            phantomReference.clear()
            deferrableSurface.close()
        }
    }

    // deferrableSurface assigned to null to make GC eligible
    @FlakyTest(bugId = 228838770)
    @LargeTest
    @Test
    @Throws(TimeoutException::class, InterruptedException::class)
    fun surfaceRequest_stronglyReferencesDeferrableSurface() {
        // Arrange.
        val request = createNewRequestWithoutAutoCleanup(FAKE_SIZE)
        // Retrieve the DeferrableSurface which should maintain the strong reference to the
        // SurfaceRequest
        var deferrableSurface: DeferrableSurface? = request.deferrableSurface
        val referenceQueue = ReferenceQueue<DeferrableSurface?>()
        // Ensure surface request garbage collection is tracked
        val phantomReference = PhantomReference(deferrableSurface, referenceQueue)
        try {
            // Act.
            // Null out the original reference to the DeferrableSurface. SurfaceRequest should be
            // the only reference remaining.
            null.also { deferrableSurface = it }
            GarbageCollectionUtil.runFinalization()
            val deferrableSurfaceFinalized = referenceQueue.poll() != null

            // Assert.
            Truth.assertThat(deferrableSurfaceFinalized).isFalse()
        } finally {
            // Clean up
            phantomReference.clear()
            request.deferrableSurface.close()
        }
    }

    // The test method is responsible for ensuring that the SurfaceRequest is finished.
    private fun createNewRequestWithoutAutoCleanup(size: Size): SurfaceRequest {
        return createNewRequest(size, autoCleanup = false)
    }

    private fun createNewRequest(
        size: Size,
        dynamicRange: DynamicRange = DynamicRange.SDR,
        expectedFrameRate: Range<Int> = SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED,
        autoCleanup: Boolean = true,
        onInvalidated: () -> Unit = {},
    ): SurfaceRequest {
        val request =
            SurfaceRequest(size, FakeCamera(), dynamicRange, expectedFrameRate, onInvalidated)
        if (autoCleanup) {
            surfaceRequests.add(request)
        }
        return request
    }

    companion object {
        private val FAKE_SIZE: Size by lazy { Size(0, 0) }
        private val FAKE_INFO: SurfaceRequest.TransformationInfo by lazy {
            SurfaceRequest.TransformationInfo.of(
                Rect(),
                0,
                Surface.ROTATION_0,
                /*hasCameraTransform=*/ true,
                /*sensorToBufferTransform=*/ Matrix(),
                /*mirroring=*/ false,
            )
        }
        private val NO_OP_RESULT_LISTENER = Consumer { _: SurfaceRequest.Result? -> }
        private val SURFACE = Surface(SurfaceTexture(0))
    }
}
