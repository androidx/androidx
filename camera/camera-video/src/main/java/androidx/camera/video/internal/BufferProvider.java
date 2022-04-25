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

package androidx.camera.video.internal;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.Observable;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * BufferProvider provides buffers for writing data.
 *
 * <p>BufferProvider has {@link State}, it could be either {@link State#ACTIVE} or
 * {@link State#INACTIVE}. The state can be fetched directly through {@link #fetchData()} or use
 * {@link #addObserver} to receive state changes.
 *
 * <p>A buffer for writing data can be acquired with {@link #acquireBuffer()}". The buffer can
 * only be obtained when the state is {@link State#ACTIVE}. If the state is
 * {@link State#INACTIVE}, the {@link #acquireBuffer()} will return a failed
 * {@link ListenableFuture} with {@link IllegalStateException}. If the state is transitioned from
 * {@link State#ACTIVE} to {@link State#INACTIVE}, the incomplete {@link ListenableFuture} will
 * get {@link java.util.concurrent.CancellationException}. Buffer acquisition can be cancelled
 * with {@link ListenableFuture#cancel} if acquisition is not yet complete.
 *
 * @param <T> the buffer data type
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface BufferProvider<T> extends Observable<BufferProvider.State> {

    /**
     * Acquires a buffer.
     *
     * <p>A buffer can only be obtained when the state is {@link State#ACTIVE}. If the state is
     * {@link State#INACTIVE}, the {@link #acquireBuffer()} will return an failed
     * {@link ListenableFuture} with {@link IllegalStateException}. If the state is transitioned
     * from {@link State#ACTIVE} to {@link State#INACTIVE}, the incomplete
     * {@link ListenableFuture} will get {@link java.util.concurrent.CancellationException}.
     * Buffer acquisition can be cancelled with {@link ListenableFuture#cancel} if acquisition
     * is not yet complete.
     *
     * @return a {@link ListenableFuture} to represent the acquisition.
     */
    @NonNull
    ListenableFuture<T> acquireBuffer();

    /** The state of the BufferProvider. */
    enum State {

        /** The state means it is able to acquire a buffer. */
        ACTIVE,

        /**
         * The state means it is not able to acquire buffer.
         *
         * <p>The acquisition via {@link #acquireBuffer()} will get a result with
         * {@link IllegalStateException}.
         */
        INACTIVE,
    }
}

