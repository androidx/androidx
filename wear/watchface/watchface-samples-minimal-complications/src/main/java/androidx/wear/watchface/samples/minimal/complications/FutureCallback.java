/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.samples.minimal.complications;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

/** A callback for a future that explicitly handles different types of outcomes. */
interface FutureCallback<T> {

    static <T> void addCallback(
            ListenableFuture<T> future, FutureCallback<T> callback, Executor executor) {
        if (!future.isDone()) {
            callback.onPending();
        }
        future.addListener(
                () -> {
                    if (future.isCancelled()) {
                        callback.onCancelled();
                    } else {
                        try {
                            callback.onSuccess(future.get());
                        } catch (InterruptedException e) {
                            callback.onInterrupted();
                        } catch (ExecutionException e) {
                            callback.onFailure(e.getCause());
                        }
                    }
                },
                executor);
    }

    /** Called immediately if a callback is added to a future that is not yet done. */
    void onPending();

    /** Called if the future returns a value. */
    void onSuccess(T value);

    /** Called if the future throws an exception. */
    void onFailure(Throwable throwable);

    /** Called if the future is interrupted. */
    void onInterrupted();

    /** Called if the future is cancelled. */
    void onCancelled();
}
