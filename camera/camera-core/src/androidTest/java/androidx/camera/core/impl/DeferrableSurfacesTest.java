/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.view.Surface;

import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class DeferrableSurfacesTest {

    private ScheduledExecutorService mScheduledExecutorService;
    private List<CallbackToFutureAdapter.Completer<Surface>> mCompleterList = new ArrayList<>();
    private List<DeferrableSurface> mFakeDeferrableSurfaces = new ArrayList<>();

    @Before
    public void setup() {
        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @After
    public void tearDown() {
        for (CallbackToFutureAdapter.Completer<Surface> completer : mCompleterList) {
            completer.setCancelled();
        }

        for (DeferrableSurface surface : mFakeDeferrableSurfaces) {
            surface.close();
        }

        mCompleterList.clear();
        mScheduledExecutorService.shutdown();
    }

    @Test
    @MediumTest
    @SuppressWarnings({"deprecation", "unchecked"}) /* AsyncTask */
    public void getSurfaceTimeoutTest() {
        DeferrableSurface fakeDeferrableSurface = getFakeDeferrableSurface();

        List<DeferrableSurface> surfaces = Arrays.asList(fakeDeferrableSurface);
        ListenableFuture<List<Surface>> listenableFuture =
                DeferrableSurfaces.surfaceListWithTimeout(surfaces, false, 50,
                        android.os.AsyncTask.THREAD_POOL_EXECUTOR, mScheduledExecutorService);

        FutureCallback<List<Surface>> mockFutureCallback = mock(FutureCallback.class);
        Futures.addCallback(listenableFuture, mockFutureCallback,
                android.os.AsyncTask.THREAD_POOL_EXECUTOR);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(mockFutureCallback, timeout(3000).times(1)).onFailure(throwableCaptor.capture());
        // Expect get an TimeoutException since the fake getSurface task might take over 50
        // milliseconds.
        assertThat(throwableCaptor.getValue()).isInstanceOf(TimeoutException.class);
    }

    @Test
    public void surfaceListWithTimeout_cancelReturnedFutureWontCancelDeferrableSurfaces() {
        DeferrableSurface deferrableSurface = new DeferrableSurface() {
            private final ListenableFuture<Surface> mSurfaceFuture = ResolvableFuture.create();
            @Override
            protected @NonNull ListenableFuture<Surface> provideSurface() {
                // Return a never complete future.
                return mSurfaceFuture;
            }
        };
        List<DeferrableSurface> surfaces = Arrays.asList(deferrableSurface);
        ListenableFuture<List<Surface>> listenableFuture =
                DeferrableSurfaces.surfaceListWithTimeout(surfaces, false,
                        /*timeout=*/Long.MAX_VALUE, CameraXExecutors.directExecutor(),
                        mScheduledExecutorService);

        listenableFuture.cancel(true);

        assertThat(deferrableSurface.getSurface().isCancelled()).isFalse();
    }

    @Test
    public void tryIncrementAll_canIncrementAllCounts() {
        DeferrableSurface fakeSurface0 = getFakeDeferrableSurface();
        DeferrableSurface fakeSurface1 = getFakeDeferrableSurface();
        DeferrableSurface fakeSurface2 = getFakeDeferrableSurface();

        int initialCount0 = fakeSurface0.getUseCount();
        int initialCount1 = fakeSurface1.getUseCount();
        int initialCount2 = fakeSurface2.getUseCount();

        boolean success = DeferrableSurfaces.tryIncrementAll(Arrays.asList(fakeSurface0,
                fakeSurface1,
                fakeSurface2));

        assertThat(success).isTrue();
        assertThat(fakeSurface0.getUseCount()).isEqualTo(initialCount0 + 1);
        assertThat(fakeSurface1.getUseCount()).isEqualTo(initialCount1 + 1);
        assertThat(fakeSurface2.getUseCount()).isEqualTo(initialCount2 + 1);

        DeferrableSurfaces.decrementAll(Arrays.asList(fakeSurface0,
                fakeSurface1,
                fakeSurface2));
    }

    @Test
    public void incrementAll_onClosedSurface_willThrowWithClosedSurface() {
        DeferrableSurface fakeSurface0 = getFakeDeferrableSurface();
        DeferrableSurface fakeSurface1 = getFakeDeferrableSurface();
        DeferrableSurface fakeSurface2 = getFakeDeferrableSurface();

        fakeSurface1.close();

        DeferrableSurface.SurfaceClosedException ex = null;
        try {
            DeferrableSurfaces.incrementAll(Arrays.asList(fakeSurface0, fakeSurface1,
                    fakeSurface2));
        } catch (DeferrableSurface.SurfaceClosedException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getDeferrableSurface()).isSameInstanceAs(fakeSurface1);
    }

    @Test
    public void tryIncrementAll_onClosedSurface_causesUseCountToStayConstant() {
        DeferrableSurface fakeSurface0 = getFakeDeferrableSurface();
        DeferrableSurface fakeSurface1 = getFakeDeferrableSurface();
        DeferrableSurface fakeSurface2 = getFakeDeferrableSurface();

        int initialCount0 = fakeSurface0.getUseCount();
        int initialCount1 = fakeSurface1.getUseCount();
        int initialCount2 = fakeSurface2.getUseCount();

        fakeSurface2.close();

        boolean success = DeferrableSurfaces.tryIncrementAll(Arrays.asList(fakeSurface0,
                fakeSurface1,
                fakeSurface2));

        assertThat(success).isFalse();
        assertThat(fakeSurface0.getUseCount()).isEqualTo(initialCount0);
        assertThat(fakeSurface1.getUseCount()).isEqualTo(initialCount1);
        assertThat(fakeSurface2.getUseCount()).isEqualTo(initialCount2);
    }

    /**
     * Return a {@link ListenableFuture} which will never complete.
     */
    private @NonNull ListenableFuture<Surface> getFakeProcessingListenableFuture() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            // Only keep the completer instance to avoid the garbage collection and not set the
            // completer to keep the ListenableFuture unfinished.
            mCompleterList.add(completer);
            return "FakeProcessingListenableFuture";
        });
    }

    private @NonNull DeferrableSurface getFakeDeferrableSurface() {
        DeferrableSurface surface = new DeferrableSurface() {
            @Override
            public @NonNull ListenableFuture<Surface> provideSurface() {
                return getFakeProcessingListenableFuture();
            }
        };

        mFakeDeferrableSurfaces.add(surface);
        return surface;
    }
}
