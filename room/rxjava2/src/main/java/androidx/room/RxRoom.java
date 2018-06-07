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

import androidx.annotation.RestrictTo;

import java.util.Set;
import java.util.concurrent.Callable;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Helper class to add RxJava2 support to Room.
 */
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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static <T> Flowable<T> createFlowable(final RoomDatabase database,
            final String[] tableNames, final Callable<T> callable) {
        Scheduler scheduler = Schedulers.from(database.getQueryExecutor());
        final Maybe<T> maybe = Maybe.fromCallable(callable);
        return createFlowable(database, tableNames).observeOn(scheduler)
                .flatMapMaybe(new Function<Object, MaybeSource<T>>() {
                    @Override
                    public MaybeSource<T> apply(Object o) throws Exception {
                        return maybe;
                    }
                });
    }

    /** @deprecated This type should not be instantiated as it contains only static methods. */
    @Deprecated
    @SuppressWarnings("PrivateConstructorForUtilityClass")
    public RxRoom() {
    }
}
