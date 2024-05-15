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

import android.annotation.SuppressLint;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * A class to hold static methods used by code generation in Room's Guava compatibility library.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@SuppressWarnings("unused") // Used in GuavaListenableFutureQueryResultBinder code generation.
public class GuavaRoom {

    private GuavaRoom() {}

    /**
     * Returns a {@link ListenableFuture<T>} created by submitting the input {@code callable} to
     * {@link ArchTaskExecutor}'s background-threaded Executor.
     *
     * @deprecated Use {@link #createListenableFuture(RoomDatabase, boolean, Callable,
     *             RoomSQLiteQuery, boolean, CancellationSignal)}
     */
    @Deprecated
    @SuppressLint("LambdaLast")
    public static <T> ListenableFuture<T> createListenableFuture(
            final Callable<T> callable,
            final RoomSQLiteQuery query,
            final boolean releaseQuery) {
        return createListenableFuture(
                ArchTaskExecutor.getIOThreadExecutor(), callable, query, releaseQuery, null);
    }

    /**
     * Returns a {@link ListenableFuture<T>} created by submitting the input {@code callable} to
     * {@link RoomDatabase}'s {@link java.util.concurrent.Executor}.
     *
     * @deprecated Use {@link #createListenableFuture(RoomDatabase, boolean, Callable,
     *             RoomSQLiteQuery, boolean, CancellationSignal)}
     */
    @Deprecated
    @SuppressLint("LambdaLast")
    public static <T> ListenableFuture<T> createListenableFuture(
            final RoomDatabase roomDatabase,
            final Callable<T> callable,
            final RoomSQLiteQuery query,
            final boolean releaseQuery) {
        return createListenableFuture(
                roomDatabase.getQueryExecutor(), callable, query, releaseQuery, null);
    }

    /**
     * Returns a {@link ListenableFuture<T>} created by submitting the input {@code callable} to
     * {@link RoomDatabase}'s {@link java.util.concurrent.Executor}.
     */
    @SuppressLint("LambdaLast")
    public static <T> ListenableFuture<T> createListenableFuture(
            final RoomDatabase roomDatabase,
            final boolean inTransaction,
            final Callable<T> callable,
            final RoomSQLiteQuery query,
            final boolean releaseQuery) {
        return createListenableFuture(
                getExecutor(roomDatabase, inTransaction), callable, query, releaseQuery, null);
    }

    /**
     * Returns a {@link ListenableFuture<T>} created by submitting the input {@code callable} to
     * {@link RoomDatabase}'s {@link java.util.concurrent.Executor}.
     */
    @NonNull
    @SuppressLint("LambdaLast")
    public static <T> ListenableFuture<T> createListenableFuture(
            final @NonNull RoomDatabase roomDatabase,
            final boolean inTransaction,
            final @NonNull Callable<T> callable,
            final @NonNull RoomSQLiteQuery query,
            final boolean releaseQuery,
            final @Nullable CancellationSignal cancellationSignal) {
        return createListenableFuture(
                getExecutor(roomDatabase, inTransaction), callable, query, releaseQuery,
                cancellationSignal);
    }

    /**
     * Returns a {@link ListenableFuture<T>} created by submitting the input {@code callable} to
     * {@link RoomDatabase}'s {@link java.util.concurrent.Executor}.
     */
    @NonNull
    @SuppressLint("LambdaLast")
    public static <T> ListenableFuture<T> createListenableFuture(
            final @NonNull RoomDatabase roomDatabase,
            final boolean inTransaction,
            final @NonNull Callable<T> callable,
            final @NonNull SupportSQLiteQuery query,
            final boolean releaseQuery,
            final @Nullable CancellationSignal cancellationSignal) {
        return createListenableFuture(
                getExecutor(roomDatabase, inTransaction), callable, query, releaseQuery,
                cancellationSignal);
    }

    private static <T> ListenableFuture<T> createListenableFuture(
            final Executor executor,
            final Callable<T> callable,
            final SupportSQLiteQuery query,
            final boolean releaseQuery,
            final @Nullable CancellationSignal cancellationSignal) {

        final ListenableFuture<T> future = createListenableFuture(executor, callable);
        if (cancellationSignal != null) {
            future.addListener(new Runnable() {
                @Override
                public void run() {
                    if (future.isCancelled()) {
                        cancellationSignal.cancel();
                    }
                }
            }, sDirectExecutor);
        }

        if (releaseQuery) {
            future.addListener(new Runnable() {
                @Override
                public void run() {
                    if (query instanceof RoomSQLiteQuery) {
                        ((RoomSQLiteQuery) query).release();
                    }
                }
            }, sDirectExecutor);
        }

        return future;
    }

    /**
     * Returns a {@link ListenableFuture<T>} created by submitting the input {@code callable} to
     * {@link RoomDatabase}'s {@link java.util.concurrent.Executor}.
     *
     * @deprecated Use {@link #createListenableFuture(RoomDatabase, boolean, Callable)}
     */
    @Deprecated
    @NonNull
    public static <T> ListenableFuture<T> createListenableFuture(
            final @NonNull RoomDatabase roomDatabase,
            final @NonNull Callable<T> callable) {
        return createListenableFuture(roomDatabase, false, callable);
    }

    /**
     * Returns a {@link ListenableFuture<T>} created by submitting the input {@code callable} to
     * {@link RoomDatabase}'s {@link java.util.concurrent.Executor}.
     */
    @NonNull
    public static <T> ListenableFuture<T> createListenableFuture(
            final @NonNull RoomDatabase roomDatabase,
            final boolean inTransaction,
            final @NonNull Callable<T> callable) {
        return createListenableFuture(getExecutor(roomDatabase, inTransaction), callable);
    }

    /**
     * Returns a {@link ListenableFuture<T>} created by submitting the input {@code callable} to
     * an {@link java.util.concurrent.Executor}.
     */
    @NonNull
    private static <T> ListenableFuture<T> createListenableFuture(
            final @NonNull Executor executor,
            final @NonNull Callable<T> callable) {

        final ResolvableFuture<T> future = ResolvableFuture.create();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    T result = callable.call();
                    future.set(result);
                } catch (Throwable throwable) {
                    future.setException(throwable);
                }
            }
        });

        return future;
    }

    /**
     * A Direct Executor.
     */
    private static Executor sDirectExecutor = new Executor() {
        @Override
        public void execute(@NonNull Runnable runnable) {
            runnable.run();
        }
    };

    private static Executor getExecutor(RoomDatabase database, boolean inTransaction) {
        if (inTransaction) {
            return database.getTransactionExecutor();
        } else {
            return database.getQueryExecutor();
        }
    }
}
