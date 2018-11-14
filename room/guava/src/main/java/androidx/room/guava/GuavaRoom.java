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
package androidx.room.guava;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import androidx.annotation.RestrictTo;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * A class to hold static methods used by code generation in Room's Guava compatibility library.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressWarnings("unused") // Used in GuavaListenableFutureQueryResultBinder code generation.
public class GuavaRoom {

    private GuavaRoom() {}

    /**
     * Returns a {@link ListenableFuture<T>} created by submitting the input {@code callable} to
     * {@link ArchTaskExecutor}'s background-threaded Executor.
     *
     * @deprecated
     *      Use {@link #createListenableFuture(RoomDatabase, Callable, RoomSQLiteQuery, boolean)}
     */
    @Deprecated
    public static <T> ListenableFuture<T> createListenableFuture(
            final Callable<T> callable,
            final RoomSQLiteQuery query,
            final boolean releaseQuery) {
        return createListenableFuture(
                ArchTaskExecutor.getIOThreadExecutor(), callable, query, releaseQuery);
    }

    /**
     * Returns a {@link ListenableFuture<T>} created by submitting the input {@code callable} to
     * {@link RoomDatabase}'s {@link java.util.concurrent.Executor}.
     */
    public static <T> ListenableFuture<T> createListenableFuture(
            final RoomDatabase roomDatabase,
            final Callable<T> callable,
            final RoomSQLiteQuery query,
            final boolean releaseQuery) {
        return createListenableFuture(
                roomDatabase.getQueryExecutor(), callable, query, releaseQuery);
    }

    private static <T> ListenableFuture<T> createListenableFuture(
            final Executor executor,
            final Callable<T> callable,
            final RoomSQLiteQuery query,
            final boolean releaseQuery) {
        ListenableFutureTask<T> listenableFutureTask = ListenableFutureTask.create(callable);
        executor.execute(listenableFutureTask);

        if (releaseQuery) {
            Futures.addCallback(
                    listenableFutureTask,
                    new FutureCallback<T>() {
                        @Override
                        public void onSuccess(T t) {
                            query.release();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            query.release();
                        }
                    },
                    directExecutor());
        }

        return listenableFutureTask;
    }

    /**
     * Returns a {@link ListenableFuture<T>} created by submitting the input {@code callable} to
     * {@link RoomDatabase}'s {@link java.util.concurrent.Executor}.
     */
    public static <T> ListenableFuture<T> createListenableFuture(
            final RoomDatabase roomDatabase,
            final Callable<T> callable) {
        ListenableFutureTask<T> listenableFutureTask = ListenableFutureTask.create(callable);
        roomDatabase.getQueryExecutor().execute(listenableFutureTask);
        return listenableFutureTask;
    }
}
