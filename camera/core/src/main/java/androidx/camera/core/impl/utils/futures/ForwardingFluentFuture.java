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

package androidx.camera.core.impl.utils.futures;

import static androidx.core.util.Preconditions.checkNotNull;

import androidx.annotation.RestrictTo;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *  Primary logic is copy from concurrent-futures package in Guava to AndroidX namespace since we
 *  would need ListenableFuture related implementation but not want to include whole Guava library.
 *
 * {@link FluentFuture} that forwards all calls to a delegate.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
final class ForwardingFluentFuture<V> extends FluentFuture<V> {
    private final ListenableFuture<V> mDelegate;

    ForwardingFluentFuture(ListenableFuture<V> delegate) {
        this.mDelegate = checkNotNull(delegate);
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        mDelegate.addListener(listener, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return mDelegate.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return mDelegate.isCancelled();
    }

    @Override
    public boolean isDone() {
        return mDelegate.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return mDelegate.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return mDelegate.get(timeout, unit);
    }
}
