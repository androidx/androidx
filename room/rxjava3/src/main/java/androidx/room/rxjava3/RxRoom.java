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

package androidx.room.rxjava3;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeSource;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Helper class to add RxJava3 support to Room.
 */
public final class RxRoom {
    /**
     * Data dispatched by the publisher created by {@link #createFlowable(RoomDatabase, String...)}.
     */
    @NonNull
    public static final Object NOTHING = new Object();

    /**
     * Creates a {@link Flowable} that emits at least once and also re-emits whenever one of the
     * observed tables is updated.
     * <p>
     * You can easily chain a database operation to downstream of this {@link Flowable} to ensure
     * that it re-runs when database is modified.
     * <p>
     * Since database invalidation is batched, multiple changes in the database may results in just
     * 1 emission.
     *
     * @param database   The database instance
     * @param tableNames The list of table names that should be observed
     * @return A {@link Flowable} which emits {@link #NOTHING} when one of the observed tables
     * is modified (also once when the invalidation tracker connection is established).
     */
    @NonNull
    public static Flowable<Object> createFlowable(@NonNull final RoomDatabase database,
            @NonNull final String... tableNames) {
        return Flowable.create(emitter -> {
            final InvalidationTracker.Observer observer = new InvalidationTracker.Observer(
                    tableNames) {
                @Override
                public void onInvalidated(@androidx.annotation.NonNull Set<String> tables) {
                    if (!emitter.isCancelled()) {
                        emitter.onNext(NOTHING);
                    }
                }
            };
            if (!emitter.isCancelled()) {
                database.getInvalidationTracker().addObserver(observer);
                emitter.setDisposable(Disposable.fromAction(
                        () -> database.getInvalidationTracker().removeObserver(observer)));
            }

            // emit once to avoid missing any data and also easy chaining
            if (!emitter.isCancelled()) {
                emitter.onNext(NOTHING);
            }
        }, BackpressureStrategy.LATEST);
    }

    /**
     * Helper method used by generated code to bind a Callable such that it will be run in
     * our disk io thread and will automatically block null values since RxJava3 does not like null.
     *
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static <T> Flowable<T> createFlowable(@NonNull final RoomDatabase database,
            final boolean inTransaction, @NonNull final String[] tableNames,
            @NonNull final Callable<T> callable) {
        Scheduler scheduler = Schedulers.from(getExecutor(database, inTransaction));
        final Maybe<T> maybe = Maybe.fromCallable(callable);
        return createFlowable(database, tableNames)
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .observeOn(scheduler)
                .flatMapMaybe((Function<Object, MaybeSource<T>>) o -> maybe);
    }

    /**
     * Creates a {@link Observable} that emits at least once and also re-emits whenever one of the
     * observed tables is updated.
     * <p>
     * You can easily chain a database operation to downstream of this {@link Observable} to ensure
     * that it re-runs when database is modified.
     * <p>
     * Since database invalidation is batched, multiple changes in the database may results in just
     * 1 emission.
     *
     * @param database   The database instance
     * @param tableNames The list of table names that should be observed
     * @return A {@link Observable} which emits {@link #NOTHING} when one of the observed tables
     * is modified (also once when the invalidation tracker connection is established).
     */
    @NonNull
    public static Observable<Object> createObservable(@NonNull final RoomDatabase database,
            @NonNull final String... tableNames) {
        return Observable.create(emitter -> {
            final InvalidationTracker.Observer observer = new InvalidationTracker.Observer(
                    tableNames) {
                @Override
                public void onInvalidated(@androidx.annotation.NonNull Set<String> tables) {
                    emitter.onNext(NOTHING);
                }
            };
            database.getInvalidationTracker().addObserver(observer);
            emitter.setDisposable(Disposable.fromAction(
                    () -> database.getInvalidationTracker().removeObserver(observer)));

            // emit once to avoid missing any data and also easy chaining
            emitter.onNext(NOTHING);
        });
    }

    /**
     * Helper method used by generated code to bind a Callable such that it will be run in
     * our disk io thread and will automatically block null values since RxJava3 does not like null.
     *
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static <T> Observable<T> createObservable(@NonNull final RoomDatabase database,
            final boolean inTransaction, @NonNull final String[] tableNames,
            @NonNull final Callable<T> callable) {
        Scheduler scheduler = Schedulers.from(getExecutor(database, inTransaction));
        final Maybe<T> maybe = Maybe.fromCallable(callable);
        return createObservable(database, tableNames)
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .observeOn(scheduler)
                .flatMapMaybe(o -> maybe);
    }

    /**
     * Helper method used by generated code to create a Single from a Callable that will ignore
     * the EmptyResultSetException if the stream is already disposed.
     *
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static <T> Single<T> createSingle(@NonNull final Callable<T> callable) {
        return Single.create(emitter -> {
            try {
                emitter.onSuccess(callable.call());
            } catch (EmptyResultSetException e) {
                emitter.tryOnError(e);
            }
        });
    }

    private static Executor getExecutor(@NonNull RoomDatabase database, boolean inTransaction) {
        if (inTransaction) {
            return database.getTransactionExecutor();
        } else {
            return database.getQueryExecutor();
        }
    }

    private RxRoom() {
    }
}
