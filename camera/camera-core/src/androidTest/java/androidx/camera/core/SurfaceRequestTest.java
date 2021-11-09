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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.graphics.Rect;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.GarbageCollectionUtil;
import androidx.camera.testing.fakes.FakeCamera;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 21)
public final class SurfaceRequestTest {

    private static final Size FAKE_SIZE = new Size(0, 0);
    private static final SurfaceRequest.TransformationInfo FAKE_INFO =
            SurfaceRequest.TransformationInfo.of(new Rect(), 0, Surface.ROTATION_0);
    private static final Consumer<SurfaceRequest.Result> NO_OP_RESULT_LISTENER = ignored -> {
    };
    private static final Surface MOCK_SURFACE = mock(Surface.class);
    private final List<SurfaceRequest> mSurfaceRequests = new ArrayList<>();

    @After
    public void tearDown() {
        // Ensure all requests complete
        for (SurfaceRequest request : mSurfaceRequests) {
            // Closing the deferrable surface should cause the request to be cancelled if it has
            // not yet been completed.
            request.getDeferrableSurface().close();
        }
    }

    @Test
    public void canRetrieveResolution() {
        Size resolution = new Size(640, 480);
        SurfaceRequest request = createNewRequest(resolution);

        assertThat(request.getResolution()).isEqualTo(resolution);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setWillNotProvideSurface_resultsInWILL_NOT_PROVIDE_SURFACE() {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        Consumer<SurfaceRequest.Result> listener = mock(Consumer.class);
        request.willNotProvideSurface();
        request.provideSurface(MOCK_SURFACE, CameraXExecutors.directExecutor(), listener);

        verify(listener).accept(eq(SurfaceRequest.Result.of(
                SurfaceRequest.Result.RESULT_WILL_NOT_PROVIDE_SURFACE, MOCK_SURFACE)));
    }

    @Test
    public void willNotProvideSurface_returnsFalse_whenAlreadyCompleted() {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        // Complete the request
        request.provideSurface(MOCK_SURFACE, CameraXExecutors.directExecutor(),
                NO_OP_RESULT_LISTENER);

        assertThat(request.willNotProvideSurface()).isFalse();
    }

    @Test
    public void willNotProvideSurface_returnsFalse_whenRequestIsCancelled() {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        // Cause request to be cancelled from producer side
        request.getDeferrableSurface().close();

        assertThat(request.willNotProvideSurface()).isFalse();
    }

    @Test
    public void willNotProvideSurface_returnsTrue_whenNotYetCompleted() {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        assertThat(request.willNotProvideSurface()).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void surfaceRequestResult_completesSuccessfully_afterProducerIsDone() {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        Consumer<SurfaceRequest.Result> listener = mock(Consumer.class);
        request.provideSurface(MOCK_SURFACE,
                ContextCompat.getMainExecutor(ApplicationProvider.getApplicationContext()),
                listener);

        // Cause request to be completed from producer side
        request.getDeferrableSurface().close();

        verify(listener, timeout(500)).accept(eq(SurfaceRequest.Result.of(
                SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY, MOCK_SURFACE)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void provideSurface_resultsInSURFACE_ALREADY_PROVIDED_onSecondInvocation() {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        Consumer<SurfaceRequest.Result> listener = mock(Consumer.class);
        request.provideSurface(MOCK_SURFACE, CameraXExecutors.directExecutor(),
                NO_OP_RESULT_LISTENER);
        request.provideSurface(MOCK_SURFACE, CameraXExecutors.directExecutor(), listener);

        verify(listener).accept(eq(SurfaceRequest.Result.of(
                SurfaceRequest.Result.RESULT_SURFACE_ALREADY_PROVIDED, MOCK_SURFACE)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void cancelledRequest_resultsInREQUEST_CANCELLED() {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        // Cause request to be cancelled from producer side
        request.getDeferrableSurface().close();

        Consumer<SurfaceRequest.Result> listener = mock(Consumer.class);
        request.provideSurface(MOCK_SURFACE, CameraXExecutors.directExecutor(), listener);

        verify(listener).accept(eq(SurfaceRequest.Result.of(
                SurfaceRequest.Result.RESULT_REQUEST_CANCELLED, MOCK_SURFACE)));
    }

    @Test
    public void cancelledRequest_callsCancellationListener_whenCancelledAfterAddingListener() {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        Runnable listener = mock(Runnable.class);
        request.addRequestCancellationListener(
                ContextCompat.getMainExecutor(ApplicationProvider.getApplicationContext()),
                listener);

        // Cause request to be cancelled from producer side
        request.getDeferrableSurface().close();

        verify(listener, timeout(500)).run();
    }

    @Test
    public void cancelledRequest_callsCancellationListener_whenCancelledBeforeAddingListener() {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        // Cause request to be cancelled from producer side
        request.getDeferrableSurface().close();

        Runnable listener = mock(Runnable.class);
        request.addRequestCancellationListener(
                ContextCompat.getMainExecutor(ApplicationProvider.getApplicationContext()),
                listener);

        verify(listener, timeout(500)).run();
    }

    @Test
    public void setListenerWhenTransformationAvailable_receivesImmediately() {
        // Arrange.
        SurfaceRequest request = createNewRequest(FAKE_SIZE);
        request.updateTransformationInfo(FAKE_INFO);
        AtomicReference<SurfaceRequest.TransformationInfo> infoReference = new AtomicReference<>();

        // Act.
        request.setTransformationInfoListener(CameraXExecutors.directExecutor(),
                infoReference::set);

        // Assert.
        assertThat(infoReference.get()).isEqualTo(FAKE_INFO);
    }

    @Test
    public void setListener_receivesCallbackWhenAvailable() {
        // Arrange.
        SurfaceRequest request = createNewRequest(FAKE_SIZE);
        AtomicReference<SurfaceRequest.TransformationInfo> infoReference = new AtomicReference<>();
        request.setTransformationInfoListener(CameraXExecutors.directExecutor(),
                infoReference::set);
        assertThat(infoReference.get()).isNull();

        // Act.
        request.updateTransformationInfo(FAKE_INFO);

        // Assert.
        assertThat(infoReference.get()).isEqualTo(FAKE_INFO);
    }

    @SuppressWarnings("UnusedAssignment") // request assigned to null to make GC eligible
    @LargeTest
    @Test
    public void deferrableSurface_stronglyReferencesSurfaceRequest()
            throws TimeoutException, InterruptedException {
        // Arrange.
        SurfaceRequest request = createNewRequestWithoutAutoCleanup(FAKE_SIZE);
        // Retrieve the DeferrableSurface which should maintain the strong reference to the
        // SurfaceRequest
        DeferrableSurface deferrableSurface = request.getDeferrableSurface();
        ReferenceQueue<SurfaceRequest> referenceQueue = new ReferenceQueue<>();
        // Ensure surface request garbage collection is tracked
        PhantomReference<SurfaceRequest> phantomReference = new PhantomReference<>(request,
                referenceQueue);
        try {
            // Act.
            // Null out the original reference to the SurfaceRequest. DeferrableSurface should be
            // the only reference remaining.
            request = null;
            GarbageCollectionUtil.runFinalization();
            boolean requestFinalized = (referenceQueue.poll() != null);

            // Assert.
            assertThat(requestFinalized).isFalse();
        } finally {
            // Clean up
            phantomReference.clear();
            deferrableSurface.close();
        }
    }

    @SuppressWarnings("UnusedAssignment") // deferrableSurface assigned to null to make GC eligible
    @LargeTest
    @Test
    public void surfaceRequest_stronglyReferencesDeferrableSurface()
            throws TimeoutException, InterruptedException {
        // Arrange.
        SurfaceRequest request = createNewRequestWithoutAutoCleanup(FAKE_SIZE);
        // Retrieve the DeferrableSurface which should maintain the strong reference to the
        // SurfaceRequest
        DeferrableSurface deferrableSurface = request.getDeferrableSurface();
        ReferenceQueue<DeferrableSurface> referenceQueue = new ReferenceQueue<>();
        // Ensure surface request garbage collection is tracked
        PhantomReference<DeferrableSurface> phantomReference = new PhantomReference<>(
                deferrableSurface, referenceQueue);
        try {
            // Act.
            // Null out the original reference to the DeferrableSurface. SurfaceRequest should be
            // the only reference remaining.
            deferrableSurface = null;
            GarbageCollectionUtil.runFinalization();
            boolean deferrableSurfaceFinalized = (referenceQueue.poll() != null);

            // Assert.
            assertThat(deferrableSurfaceFinalized).isFalse();
        } finally {
            // Clean up
            phantomReference.clear();
            request.getDeferrableSurface().close();
        }
    }

    // The test method is responsible for ensuring that the SurfaceRequest is finished.
    private SurfaceRequest createNewRequestWithoutAutoCleanup(@NonNull Size size) {
        return createNewRequest(size, /*autoCleanUp=*/false);
    }

    private SurfaceRequest createNewRequest(@NonNull Size size) {
        return createNewRequest(size, /*autoCleanUp=*/true);
    }

    private SurfaceRequest createNewRequest(@NonNull Size size, boolean autoCleanup) {
        SurfaceRequest request = new SurfaceRequest(size, new FakeCamera(), false);
        if (autoCleanup) {
            mSurfaceRequests.add(request);
        }
        return request;
    }
}
