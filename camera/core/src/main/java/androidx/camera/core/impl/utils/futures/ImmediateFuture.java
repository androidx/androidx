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

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.concurrent.ListenableFuture;
import androidx.core.util.Preconditions;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link ListenableFuture} which immediately contains a result.
 *
 * <p>This implementation is based off of the Guava ImmediateSuccessfulFuture class.
 * @param <V> The type of the value stored in the future.
 */
class ImmediateFuture<V> implements ListenableFuture<V> {

    private static final String TAG = "ImmediateFuture";

    private static final ImmediateFuture<Object> NULL_FUTURE = new ImmediateFuture<>(null);

    @Nullable
    private final V mValue;

    ImmediateFuture(@Nullable V value) {
        mValue = value;
    }

    /**
     * Returns a future that contains a null value.
     *
     * <p>This should be used any time a null value is needed as it uses a static ListenableFuture
     * that contains null, and thus will not allocate.
     */
    public static <V> ListenableFuture<V> nullFuture() {
        @SuppressWarnings({"unchecked", "rawtypes"}) // Safe since null can be cast to any type
        ListenableFuture<V> typedNull = (ListenableFuture) NULL_FUTURE;
        return typedNull;
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        Preconditions.checkNotNull(listener);
        Preconditions.checkNotNull(executor);

        try {
            executor.execute(listener);
        } catch (RuntimeException e) {
            // ListenableFuture does not throw runtime exceptions, so swallow the exception and
            // log it here.
            Log.e(TAG, "Experienced RuntimeException while attempting to notify " + listener
                    + " on Executor " + executor, e);
        }

    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    @Nullable
    public V get() {
        return mValue;
    }

    @Override
    @Nullable
    public V get(long timeout, TimeUnit unit) {
        return mValue;
    }

    @Override
    public String toString() {
        // Behaviour analogous to AbstractResolvableFuture#toString().
        return super.toString() + "[status=SUCCESS, result=[" + mValue + "]]";
    }
}
