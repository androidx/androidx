/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core.impl.utils.futures;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.os.Build;

import androidx.annotation.Nullable;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.test.filters.SmallTest;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ImmediateFutureTest {

    @Test
    public void immediateFutureIsDone() {
        Future<Object> future = Futures.immediateFuture(mock(Object.class));

        assertThat(future.isDone()).isTrue();
    }

    @Test
    public void immediateFutureIsNotCancelled() {
        Future<Object> future = Futures.immediateFuture(mock(Object.class));

        assertThat(future.isCancelled()).isFalse();
    }

    @Test
    public void immediateFutureCannotBeCancelled() {
        Future<Object> future = Futures.immediateFuture(mock(Object.class));

        assertThat(future.cancel(false)).isFalse();
    }

    @Test
    public void canGetImmediateResult() throws ExecutionException, InterruptedException {
        Object result = mock(Object.class);
        Future<Object> future = Futures.immediateFuture(result);

        assertThat(future.get()).isSameInstanceAs(result);
    }

    @Test
    public void canListenForImmediateResult() {
        Object result = mock(Object.class);
        final ListenableFuture<Object> future = Futures.immediateFuture(result);

        final AtomicReference<Object> resultRef = new AtomicReference<>(null);
        Futures.addCallback(future, new FutureCallback<Object>() {
            @Override
            public void onSuccess(@Nullable Object result) {
                resultRef.set(result);
            }

            @Override
            public void onFailure(Throwable t) {

            }
        }, CameraXExecutors.directExecutor());

        assertThat(resultRef.get()).isSameInstanceAs(result);
    }

    @Test
    public void immediateFailedFutureIsDone() {
        Future<Object> future = Futures.immediateFailedFuture(mock(Throwable.class));

        assertThat(future.isDone()).isTrue();
    }

    @Test
    public void immediateFailedFutureIsNotCancelled() {
        Future<Object> future = Futures.immediateFailedFuture(mock(Throwable.class));

        assertThat(future.isCancelled()).isFalse();
    }

    @Test
    public void immediateFailedFutureCannotBeCancelled() {
        Future<Object> future = Futures.immediateFailedFuture(mock(Throwable.class));

        assertThat(future.cancel(false)).isFalse();
    }

    @Test
    public void canGetImmediateFailedResult() throws InterruptedException {
        Throwable cause = mock(Throwable.class);
        Future<Object> future = Futures.immediateFailedFuture(cause);

        Throwable retrievedCause = null;
        try {
            future.get();
        } catch (ExecutionException e) {
            retrievedCause = e.getCause();
        }

        assertThat(retrievedCause).isSameInstanceAs(cause);
    }

    @Test
    public void canListenForImmediateFailedResult() {
        Throwable cause = mock(Throwable.class);
        final ListenableFuture<Object> future = Futures.immediateFailedFuture(cause);

        final AtomicReference<Throwable> causeRef = new AtomicReference<>(null);
        Futures.addCallback(future, new FutureCallback<Object>() {
            @Override
            public void onSuccess(@Nullable Object result) {
            }

            @Override
            public void onFailure(Throwable t) {
                causeRef.set(t);
            }
        }, CameraXExecutors.directExecutor());

        assertThat(causeRef.get()).isSameInstanceAs(cause);
    }
}
