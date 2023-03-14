/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room;

import android.annotation.SuppressLint;

import androidx.annotation.RestrictTo;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Helper class to add RxJava2 support to Room.
 */
@SuppressLint("PrivateConstructorForUtilityClass")
@SuppressWarnings("WeakerAccess")
public class RxRoom {
    /**
     * Data dispatched by the publisher created by {@link #createFlowable(RoomDatabase, String...)}.
     */
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
    public static Flowable<Object> createFlowable(final RoomDatabase database,
            final String... tableNames) {
        return Flowable.create(new FlowableOnSubscribe<Object>() {
            @Override
            public void subscribe(final FlowableEmitter<Object> emitter) throws Exception {
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
                    emitter.setDisposable(Disposables.fromAction(new Action() {
                        @Override
                        public void run() throws Exception {
                            database.getInvalidationTracker().removeObserver(observer);
                        }
                    }));
                }

                // emit once to avoid missing any data and also easy chaining
                if (!emitter.isCancelled()) {
                    emitter.onNext(NOTHING);
                }
            }
        }, BackpressureStrategy.LATEST);
    }

    /**
     * Helper method used by generated code to bind a Callable such that it will be run in
     * our disk io thread and will automatically block null values since RxJava2 does not like null.
     *
     * @deprecated Use {@link #createFlowable(RoomDatabase, boolean, String[], Callable)}
     *
     */
    @Deprecated
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static <T> Flowable<T> createFlowable(final RoomDatabase database,
            final String[] tableNames, final Callable<T> callable) {
        return createFlowable(database, false, tableNames, callable);
    }

    /**
     * Helper method used by generated code to bind a Callable such that it will be run in
     * our disk io thread and will automatically block null values since RxJava2 does not like null.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static <T> Flowable<T> createFlowable(final RoomDatabase database,
            final boolean inTransaction, final String[] tableNames, final Callable<T> callable) {
        Scheduler scheduler = Schedulers.from(getExecutor(database, inTransaction));
        final Maybe<T> maybe = Maybe.fromCallable(callable);
        return createFlowable(database, tableNames)
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .observeOn(scheduler)
                .flatMapMaybe(new Function<Object, MaybeSource<T>>() {
                    @Override
                    public MaybeSource<T> apply(Object o) throws Exception {
                        return maybe;
                    }
                });
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
    public static Observable<Object> createObservable(final RoomDatabase database,
            final String... tableNames) {
        return Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(final ObservableEmitter<Object> emitter) throws Exception {
                final InvalidationTracker.Observer observer = new InvalidationTracker.Observer(
                        tableNames) {
                    @Override
                    public void onInvalidated(@androidx.annotation.NonNull Set<String> tables) {
                        emitter.onNext(NOTHING);
                    }
                };
                database.getInvalidationTracker().addObserver(observer);
                emitter.setDisposable(Disposables.fromAction(new Action() {
                    @Override
                    public void run() throws Exception {
                        database.getInvalidationTracker().removeObserver(observer);
                    }
                }));

                // emit once to avoid missing any data and also easy chaining
                emitter.onNext(NOTHING);
            }
        });
    }

    /**
     * Helper method used by generated code to bind a Callable such that it will be run in
     * our disk io thread and will automatically block null values since RxJava2 does not like null.
     *
     * @deprecated Use {@link #createObservable(RoomDatabase, boolean, String[], Callable)}
     *
     */
    @Deprecated
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static <T> Observable<T> createObservable(final RoomDatabase database,
            final String[] tableNames, final Callable<T> callable) {
        return createObservable(database, false, tableNames, callable);
    }

    /**
     * Helper method used by generated code to bind a Callable such that it will be run in
     * our disk io thread and will automatically block null values since RxJava2 does not like null.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static <T> Observable<T> createObservable(final RoomDatabase database,
            final boolean inTransaction, final String[] tableNames, final Callable<T> callable) {
        Scheduler scheduler = Schedulers.from(getExecutor(database, inTransaction));
        final Maybe<T> maybe = Maybe.fromCallable(callable);
        return createObservable(database, tableNames)
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .observeOn(scheduler)
                .flatMapMaybe(new Function<Object, MaybeSource<T>>() {
                    @Override
                    public MaybeSource<T> apply(Object o) throws Exception {
                        return maybe;
                    }
                });
    }

    /**
     * Helper method used by generated code to create a Single from a Callable that will ignore
     * the EmptyResultSetException if the stream is already disposed.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static <T> Single<T> createSingle(final Callable<? extends T> callable) {
        return Single.create(new SingleOnSubscribe<T>() {
            @Override
            public void subscribe(SingleEmitter<T> emitter) throws Exception {
                try {
                    emitter.onSuccess(callable.call());
                } catch (EmptyResultSetException e) {
                    emitter.tryOnError(e);
                }
            }
        });
    }

    private static Executor getExecutor(RoomDatabase database, boolean inTransaction) {
        if (inTransaction) {
            return database.getTransactionExecutor();
        } else {
            return database.getQueryExecutor();
        }
    }

    /** @deprecated This type should not be instantiated as it contains only static methods. */
    @Deprecated
    @SuppressWarnings("PrivateConstructorForUtilityClass")
    public RxRoom() {
    }
}
