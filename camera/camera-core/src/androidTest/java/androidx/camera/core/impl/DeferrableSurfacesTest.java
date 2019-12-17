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

import android.os.AsyncTask;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class DeferrableSurfacesTest {

    private ScheduledExecutorService mScheduledExecutorService;

    @Before
    public void setup() {
        mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @After
    public void tearDown() {
        mScheduledExecutorService.shutdown();
    }

    @Test
    @MediumTest
    public void getSurfaceTimeoutTest() {
        DeferrableSurface fakeDeferrableSurface = new DeferrableSurface() {
            @Override
            @NonNull
            public ListenableFuture<Surface> provideSurface() {
                return getFakeProcessingListenableFuture();
            }
        };

        List<DeferrableSurface> surfaces = Arrays.asList(fakeDeferrableSurface);
        ListenableFuture<List<Surface>> listenableFuture =
                DeferrableSurfaces.surfaceListWithTimeout(surfaces, false, 50,
                        AsyncTask.THREAD_POOL_EXECUTOR, mScheduledExecutorService);

        FutureCallback<List<Surface>> mockFutureCallback = mock(FutureCallback.class);
        Futures.addCallback(listenableFuture, mockFutureCallback, AsyncTask.THREAD_POOL_EXECUTOR);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(mockFutureCallback, timeout(3000).times(1)).onFailure(throwableCaptor.capture());
        // Expect get an TimeoutException since the fake getSurface task might take over 50
        // milliseconds.
        assertThat(throwableCaptor.getValue()).isInstanceOf(TimeoutException.class);
    }

    /**
     * Return a {@link ListenableFuture} which will never complete.
     */
    @NonNull
    private static <V> ListenableFuture<V> getFakeProcessingListenableFuture() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            // Not set the completer to keep the ListenableFuture unfinished.
            return "FakeProcessingListenableFuture";
        });
    }
}
