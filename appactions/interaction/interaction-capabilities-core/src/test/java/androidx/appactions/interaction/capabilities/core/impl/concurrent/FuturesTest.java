/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.impl.concurrent;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.testing.spec.SettableFutureWrapper;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.ExecutionException;
import java.util.function.Function;

@RunWith(JUnit4.class)
public final class FuturesTest {

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void addCallback_onSuccess() throws Exception {
        SettableFutureWrapper<Integer> integerFutureWrapper = new SettableFutureWrapper<>();
        SettableFutureWrapper<Boolean> testFutureWrapper = new SettableFutureWrapper<>();
        Futures.addCallback(
                integerFutureWrapper.getFuture(),
                new FutureCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer value) {
                        assertThat(value).isEqualTo(25);
                        testFutureWrapper.set(true);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        testFutureWrapper.set(false);
                    }
                },
                Runnable::run);

        assertThat(testFutureWrapper.getFuture().isDone()).isFalse();
        integerFutureWrapper.set(25);
        assertThat(testFutureWrapper.getFuture().get()).isTrue();
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void addCallback_onFailure() throws Exception {
        SettableFutureWrapper<Object> objectFutureWrapper = new SettableFutureWrapper<>();
        SettableFutureWrapper<Boolean> testFutureWrapper = new SettableFutureWrapper<>();
        Futures.addCallback(
                objectFutureWrapper.getFuture(),
                new FutureCallback<Object>() {
                    @Override
                    public void onSuccess(Object value) {
                        testFutureWrapper.set(false);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        assertThat(t).isInstanceOf(IllegalStateException.class);
                        testFutureWrapper.set(true);
                    }
                },
                Runnable::run);

        assertThat(testFutureWrapper.getFuture().isDone()).isFalse();
        objectFutureWrapper.setException(new IllegalStateException());
        assertThat(testFutureWrapper.getFuture().get()).isTrue();
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void transform_success() throws Exception {
        SettableFutureWrapper<Integer> integerFutureWrapper = new SettableFutureWrapper<>();
        ListenableFuture<Integer> transformedFuture =
                Futures.transform(integerFutureWrapper.getFuture(), (x) -> x + 10, Runnable::run,
                        "add 10");

        assertThat(transformedFuture.isDone()).isFalse();
        integerFutureWrapper.set(25);
        assertThat(transformedFuture.get()).isEqualTo(35);
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void transformAsync_success() throws Exception {
        SettableFutureWrapper<Integer> integerFutureWrapper = new SettableFutureWrapper<>();
        ListenableFuture<Integer> transformedFuture =
                Futures.transformAsync(
                        integerFutureWrapper.getFuture(),
                        (x) -> {
                            SettableFutureWrapper<Integer> transformFuture =
                                    new SettableFutureWrapper<>();
                            transformFuture.set(x + 10);
                            return transformFuture.getFuture();
                        },
                        Runnable::run,
                        "add 10 async");

        assertThat(transformedFuture.isDone()).isFalse();
        integerFutureWrapper.set(25);
        assertThat(transformedFuture.get()).isEqualTo(35);
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void immediateFuture_success() throws Exception {
        ListenableFuture<Integer> immediateFuture = Futures.immediateFuture(25);
        immediateFuture.cancel(true);
        assertThat(immediateFuture.isCancelled()).isFalse();
        assertThat(immediateFuture.isDone()).isTrue();
        assertThat(immediateFuture.get()).isEqualTo(25);
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void immediateVoidFuture_success() throws Exception {
        ListenableFuture<Void> immediateVoidFuture = Futures.immediateVoidFuture();
        immediateVoidFuture.cancel(true);
        assertThat(immediateVoidFuture.isCancelled()).isFalse();
        assertThat(immediateVoidFuture.isDone()).isTrue();
        assertThat(immediateVoidFuture.get()).isEqualTo(null);
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void immediateFailedFuture_failure() throws Exception {
        ListenableFuture<Object> immediateFailedFuture =
                Futures.immediateFailedFuture(new CustomException());
        ListenableFuture<Object> transformedFuture =
                Futures.transform(immediateFailedFuture, Function.identity(), Runnable::run,
                        "test");

        assertThat(transformedFuture.isDone()).isTrue();
        ExecutionException e = assertThrows(ExecutionException.class, transformedFuture::get);
        assertThat(e).hasCauseThat().isInstanceOf(CustomException.class);
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void transform_synchronousExceptionPropagated() throws Exception {
        Function<Integer, Integer> badTransform =
                (unused) -> {
                    throw new IllegalStateException();
                };
        ListenableFuture<Integer> transformedFuture =
                Futures.transform(Futures.immediateFuture(25), badTransform, Runnable::run,
                        "badTransform");
        assertThat(transformedFuture.isDone()).isTrue();

        SettableFutureWrapper<Throwable> errorContainer = new SettableFutureWrapper<>();
        Futures.addCallback(
                transformedFuture,
                new FutureCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer value) {
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        errorContainer.set(t);
                    }
                },
                Runnable::run);
        assertThat(errorContainer.getFuture().get()).isInstanceOf(IllegalStateException.class);
        ExecutionException e = assertThrows(ExecutionException.class, transformedFuture::get);
        assertThat(e).hasCauseThat().isInstanceOf(IllegalStateException.class);
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    public void transformAsync_synchronousExceptionPropagated() throws Exception {
        Function<Integer, ListenableFuture<Integer>> badAsyncTransform =
                (unused) -> {
                    throw new IllegalStateException();
                };
        ListenableFuture<Integer> transformedFuture =
                Futures.transformAsync(
                        Futures.immediateFuture(25), badAsyncTransform, Runnable::run,
                        "badAsyncTransform");
        assertThat(transformedFuture.isDone()).isTrue();

        SettableFutureWrapper<Throwable> errorContainer = new SettableFutureWrapper<>();
        Futures.addCallback(
                transformedFuture,
                new FutureCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer value) {
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        errorContainer.set(t);
                    }
                },
                Runnable::run);
        assertThat(errorContainer.getFuture().get()).isInstanceOf(IllegalStateException.class);
        ExecutionException e = assertThrows(ExecutionException.class, transformedFuture::get);
        assertThat(e).hasCauseThat().isInstanceOf(IllegalStateException.class);
    }

    private static class CustomException extends Exception {
    }
}
