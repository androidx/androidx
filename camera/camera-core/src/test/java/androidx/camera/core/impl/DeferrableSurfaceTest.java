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

import static androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class DeferrableSurfaceTest {
    private DeferrableSurface mDeferrableSurface;

    @Before
    public void setup() {
        mDeferrableSurface = new DeferrableSurface() {
            @Override
            @NonNull
            public ListenableFuture<Surface> provideSurface() {
                return Futures.immediateFuture(null);
            }
        };
    }

    @After
    public void tearDown() {
        mDeferrableSurface.close();
    }

    @Test
    public void usageCountIsCorrect() throws DeferrableSurface.SurfaceClosedException {

        mDeferrableSurface.incrementUseCount();
        mDeferrableSurface.decrementUseCount();
        mDeferrableSurface.incrementUseCount();
        mDeferrableSurface.decrementUseCount();
        mDeferrableSurface.incrementUseCount();
        mDeferrableSurface.decrementUseCount();
        mDeferrableSurface.incrementUseCount();
        mDeferrableSurface.decrementUseCount();

        int firstCount = mDeferrableSurface.getUseCount();

        mDeferrableSurface.incrementUseCount();
        mDeferrableSurface.incrementUseCount();
        mDeferrableSurface.incrementUseCount();
        mDeferrableSurface.decrementUseCount();

        assertThat(firstCount).isEqualTo(0);
        assertThat(mDeferrableSurface.getUseCount()).isEqualTo(2);
    }

    @Test
    public void close_closeFutureCompletes() {
        // Arrange.
        ListenableFuture<Void> closeFuture = mDeferrableSurface.getCloseFuture();
        assertThat(closeFuture.isDone()).isFalse();
        // Act.
        mDeferrableSurface.close();
        // Assert.
        assertThat(closeFuture.isDone()).isTrue();
    }

    @Test
    public void terminationFutureFinishesWhenCompletelyDecremented()
            throws DeferrableSurface.SurfaceClosedException {
        Runnable listener = Mockito.mock(Runnable.class);

        mDeferrableSurface.incrementUseCount();
        mDeferrableSurface.close();
        mDeferrableSurface.getTerminationFuture().addListener(listener,
                directExecutor());
        mDeferrableSurface.incrementUseCount();
        mDeferrableSurface.decrementUseCount();
        mDeferrableSurface.decrementUseCount();

        Mockito.verify(listener, times(1)).run();
    }

    @Test
    public void terminationFutureFinishesAfterClose() {
        Runnable listener = Mockito.mock(Runnable.class);

        mDeferrableSurface.close();
        mDeferrableSurface.getTerminationFuture().addListener(listener,
                directExecutor());

        Mockito.verify(listener, times(1)).run();
    }

    @Test(expected = DeferrableSurface.SurfaceClosedException.class)
    public void incrementAfterCloseOnly_throwsException()
            throws DeferrableSurface.SurfaceClosedException {
        mDeferrableSurface.close();
        mDeferrableSurface.incrementUseCount();
    }

    @Test
    public void incrementAfterIncrementThenClose_increments()
            throws DeferrableSurface.SurfaceClosedException {
        mDeferrableSurface.incrementUseCount();
        mDeferrableSurface.close();
        mDeferrableSurface.incrementUseCount();

        assertThat(mDeferrableSurface.getUseCount()).isEqualTo(2);
    }

    @Test(expected = IllegalStateException.class)
    public void detachInWrongState_throwException()
            throws DeferrableSurface.SurfaceClosedException {
        mDeferrableSurface.incrementUseCount();
        mDeferrableSurface.decrementUseCount();

        mDeferrableSurface.decrementUseCount();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void closedSurfaceContainsSurfaceClosedException() {
        mDeferrableSurface.close();

        ListenableFuture<Surface> surfaceListenableFuture = mDeferrableSurface.getSurface();

        FutureCallback<Surface> futureCallback = Mockito.mock(FutureCallback.class);
        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

        Futures.addCallback(surfaceListenableFuture, futureCallback,
                directExecutor());
        verify(futureCallback, times(1)).onFailure(throwableCaptor.capture());

        assertThat(throwableCaptor.getValue()).isInstanceOf(
                DeferrableSurface.SurfaceClosedException.class);
    }
}
