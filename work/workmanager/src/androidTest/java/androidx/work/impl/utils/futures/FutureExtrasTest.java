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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.arch.core.util.Function;
import android.support.annotation.NonNull;

import androidx.concurrent.futures.SettableFuture;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FutureExtrasTest {
    private Executor mExecutor = new Executor() {
        @Override
        public void execute(@NonNull Runnable runnable) {
            runnable.run();
        }
    };

    @Test
    public void testResolveWithValue() {
        final int value = 10;
        final ListenableFuture<Integer> future = resolve(value);
        assertValue(future, value);
    }

    @Test
    public void testResolveWithException() {
        final Throwable throwable = new RuntimeException("An exception occurred.");
        final ListenableFuture<Integer> future =
                resolveWithException(throwable);
        assertExceptionWithMessage(future, throwable.getMessage());
    }

    @Test
    public void testMap() {
        int value = 10;
        ListenableFuture<Integer> input = resolve(value);
        Function<Integer, String> mapper = new Function<Integer, String>() {
            @Override
            public String apply(Integer input) {
                return String.valueOf(input);
            }
        };

        ListenableFuture<String> result = FutureExtras.map(input, mExecutor, mapper);
        assertValue(result, String.valueOf(value));
    }

    @Test
    public void testMapWithException() {
        final String message = "Something bad happened";
        ListenableFuture<Integer> input = resolve(10);
        Function<Integer, String> mapper = new Function<Integer, String>() {
            @Override
            public String apply(Integer input) {
                throw new RuntimeException(message);
            }
        };

        ListenableFuture<String> result = FutureExtras.map(input, mExecutor, mapper);
        assertExceptionWithMessage(result, message);
    }

    @Test
    public void testFlatMap() {
        int value = 10;
        ListenableFuture<Integer> input = resolve(value);
        Function<Integer, ListenableFuture<String>> flatMapper =
                new Function<Integer, ListenableFuture<String>>() {
                    @Override
                    public ListenableFuture<String> apply(Integer integer) {
                        return resolve(String.valueOf(integer));
                    }
                };

        ListenableFuture<String> result = FutureExtras.flatMap(input, mExecutor, flatMapper);
        assertValue(result, String.valueOf(value));
    }

    @Test
    public void testFlatMapWithException() {
        final String message = "Something bad happened";
        ListenableFuture<Integer> input = resolve(10);
        Function<Integer, ListenableFuture<String>> flatMapper =
                new Function<Integer, ListenableFuture<String>>() {
                    @Override
                    public ListenableFuture<String> apply(Integer integer) {
                        throw new RuntimeException(message);
                    }
                };

        ListenableFuture<String> result = FutureExtras.flatMap(input, mExecutor, flatMapper);
        assertExceptionWithMessage(result, message);
    }

    private <T> void assertValue(
            final ListenableFuture<T> future,
            final T valueToAssert) {

        future.addListener(new Runnable() {
            @Override
            public void run() {
                assertThat(future.isDone(), is(true));
                try {
                    assertThat(future.get(), is(valueToAssert));
                } catch (Throwable exception) {
                    assertThat("Should never happen", false);
                }
            }
        }, mExecutor);
    }

    private <T> void assertExceptionWithMessage(
            final ListenableFuture<T> future,
            final String messageToAssert) {

        future.addListener(new Runnable() {
            @Override
            public void run() {
                assertThat(future.isDone(), is(true));
                try {
                    future.get();
                    assertThat("Should never happen", false);
                } catch (Throwable exception) {
                    assertThat(exception.getCause().getMessage(), is(messageToAssert));
                }
            }
        }, mExecutor);
    }

    private <T> ListenableFuture<T> resolve(@NonNull T value) {
        final SettableFuture<T> future = SettableFuture.create();
        future.set(value);
        return future;
    }

    private <T> ListenableFuture<T> resolveWithException(@NonNull Throwable exception) {
        final SettableFuture<T> future = SettableFuture.create();
        future.setException(exception);
        return future;
    }
}
