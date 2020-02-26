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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class SurfaceRequestTest {

    private static final Size FAKE_SIZE = new Size(0, 0);
    private static final Surface MOCK_SURFACE = mock(Surface.class);
    private List<SurfaceRequest> mSurfaceRequests = new ArrayList<>();

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

    @Test
    public void willNotProvideSurface_setsIllegalStateException_onReturnedFuture()
            throws InterruptedException {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        request.willNotProvideSurface();
        ListenableFuture<Void> completion = request.provideSurface(MOCK_SURFACE);

        try {
            completion.get();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void willNotProvideSurface_returnsFalse_whenAlreadyCompleted() {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        // Complete the request
        request.provideSurface(MOCK_SURFACE);

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

    @Test
    public void returnedFuture_completesSuccessfully_afterProducerIsDone()
            throws InterruptedException, ExecutionException {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        ListenableFuture<Void> completion = request.provideSurface(MOCK_SURFACE);

        Runnable listener = mock(Runnable.class);
        completion.addListener(listener,
                ContextCompat.getMainExecutor(ApplicationProvider.getApplicationContext()));

        // Cause request to be completed from producer side
        request.getDeferrableSurface().close();

        verify(listener, timeout(500)).run();
        // Should not throw
        completion.get();
    }

    @Test
    public void provideSurface_setsIllegalStateException_onSecondInvocation()
            throws InterruptedException {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        ListenableFuture<Void> completion1 = request.provideSurface(MOCK_SURFACE);
        ListenableFuture<Void> completion2 = request.provideSurface(MOCK_SURFACE);

        try {
            completion2.get();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
        }
        assertThat(completion1).isNotSameInstanceAs(completion2);
    }

    @Test
    public void cancelledRequest_setsRequestCancelledException_onReturnedFuture()
            throws InterruptedException {
        SurfaceRequest request = createNewRequest(FAKE_SIZE);

        // Cause request to be cancelled from producer side
        request.getDeferrableSurface().close();

        ListenableFuture<Void> completion = request.provideSurface(MOCK_SURFACE);

        try {
            completion.get();
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(SurfaceRequest.RequestCancelledException.class);
        }
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

    private SurfaceRequest createNewRequest(@NonNull Size size) {
        SurfaceRequest request = new SurfaceRequest(size);
        mSurfaceRequests.add(request);
        return request;
    }
}
