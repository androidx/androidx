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

package androidx.work.multiprocess;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.arch.core.util.Function;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * A collection of utilities which make using
 * {@link com.google.common.util.concurrent.ListenableFuture} easier.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RemoteClientUtils {
    private RemoteClientUtils() {
        // Utilities
    }

    /**
     * A mapper that essentially drops the byte[].
     */
    public static final Function<byte[], Void> sVoidMapper = new Function<byte[], Void>() {
        @Override
        public Void apply(byte[] input) {
            return null;
        }
    };

    /**
     * Defines a mapper for a {@link ListenableFuture}.
     */
    @NonNull
    public static <I, O> ListenableFuture<O> map(
            @NonNull final ListenableFuture<I> input,
            @NonNull final Function<I, O> transformation,
            @NonNull Executor executor) {

        final SettableFuture<O> output = SettableFuture.create();
        input.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    I in = input.get();
                    O out = transformation.apply(in);
                    output.set(out);
                } catch (Throwable throwable) {
                    Throwable cause = throwable.getCause();
                    cause = cause == null ? throwable : cause;
                    output.setException(cause);
                }
            }
        }, executor);
        return output;
    }
}
