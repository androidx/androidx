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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.view.Surface;

import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.concurrent.ExecutionException;

@SmallTest
@RunWith(AndroidJUnit4.class)
public final class ImmediateSurfaceTest {
    private Surface mMockSurface = Mockito.mock(Surface.class);
    private ImmediateSurface mImmediateSurface;

    @Before
    public void setup() {
        mImmediateSurface = new ImmediateSurface(mMockSurface);
    }

    @After
    public void tearDown() {
        mImmediateSurface.close();
    }

    @Test
    public void getSurface_returnsInstance() throws ExecutionException, InterruptedException {
        ListenableFuture<Surface> surfaceListenableFuture = mImmediateSurface.getSurface();

        assertThat(surfaceListenableFuture.get()).isSameInstanceAs(mMockSurface);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void surfaceClosedExceptionWhenClosed() {
        mImmediateSurface.close();

        ListenableFuture<Surface> surface = mImmediateSurface.getSurface();

        FutureCallback<Surface> futureCallback = Mockito.mock(FutureCallback.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

        Futures.addCallback(surface, futureCallback, CameraXExecutors.directExecutor());
        verify(futureCallback, times(1)).onFailure(throwableCaptor.capture());

        assertThat(throwableCaptor.getValue()).isInstanceOf(
                DeferrableSurface.SurfaceClosedException.class);
    }
}
