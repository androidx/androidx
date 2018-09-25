/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.impl.utils.futures;

import android.arch.core.util.Function;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * A collection of utilities which make using
 * {@link com.google.common.util.concurrent.ListenableFuture}s pleasant.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FutureExtras {

    private FutureExtras() {
        // Don't instantiate.
    }

    /**
     * Helps transform {@link ListenableFuture} of an {@code Input} type into
     * {@link ListenableFuture} of an output type.
     */
    public static <Input, Output> ListenableFuture<Output> map(
            final @NonNull ListenableFuture<Input> inputFuture,
            final @NonNull Executor executor,
            final @NonNull Function<Input, Output> transformation) {

        final SettableFuture<Output> outputFuture = SettableFuture.create();
        inputFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Input input = inputFuture.get();
                    Output output = transformation.apply(input);
                    outputFuture.set(output);
                } catch (Throwable throwable) {
                    outputFuture.setException(throwable);
                }
            }
        }, executor);

        return outputFuture;
    }

    /**
     * Helps transform {@link ListenableFuture} of an {@code Input} type into
     * {@link ListenableFuture} of an output type.
     */
    public static <Input, Output> ListenableFuture<Output> flatMap(
            final @NonNull ListenableFuture<Input> inputFuture,
            final @NonNull Executor executor,
            final @NonNull Function<Input, ListenableFuture<Output>> transformation) {

        final SettableFuture<Output> outputFuture = SettableFuture.create();
        inputFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Input input = inputFuture.get();
                    ListenableFuture<Output> output = transformation.apply(input);
                    outputFuture.setFuture(output);
                } catch (Throwable throwable) {
                    outputFuture.setException(throwable);
                }
            }
        }, executor);
        return outputFuture;
    }
}
