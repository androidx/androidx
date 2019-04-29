/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core.impl.utils.futures;

import androidx.annotation.Nullable;
import androidx.camera.core.impl.utils.Optional;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Aggregate future that collects (stores) results of each future.
 *
 * <p>Copied and adapted from Guava.
 */
abstract class CollectionFuture<V, C> extends AggregateFuture<V, C> {

    abstract class CollectionFutureRunningState extends RunningState {
        private List<Optional<V>> mValues;

        CollectionFutureRunningState(
                Collection<? extends ListenableFuture<? extends V>> futures,
                boolean allMustSucceed) {
            super(futures, allMustSucceed, true);

            this.mValues =
                    futures.isEmpty()
                            ? new ArrayList<Optional<V>>()
                            : new ArrayList<Optional<V>>(futures.size());

            // Populate the results list with null initially.
            for (int i = 0; i < futures.size(); ++i) {
                mValues.add(null);
            }
        }

        @Override
        final void collectOneValue(boolean allMustSucceed, int index, @Nullable V returnValue) {
            List<Optional<V>> localValues = mValues;

            if (localValues != null) {
                localValues.set(index, Optional.fromNullable(returnValue));
            } else {
                // Some other future failed or has been cancelled, causing this one to also be
                // cancelled or have an exception set. This should only happen if allMustSucceed
                // is true or if the output itself has been cancelled.
                Preconditions.checkState(
                        allMustSucceed || isCancelled(),
                        "Future was done before all dependencies completed");
            }
        }

        @Override
        final void handleAllCompleted() {
            List<Optional<V>> localValues = mValues;
            if (localValues != null) {
                set(combine(localValues));
            } else {
                Preconditions.checkState(isDone());
            }
        }

        @Override
        void releaseResourcesAfterFailure() {
            super.releaseResourcesAfterFailure();
            this.mValues = null;
        }

        abstract C combine(List<Optional<V>> values);
    }

    /** Used for {@link Futures#successfulAsList}. */
    static final class ListFuture<V> extends CollectionFuture<V, List<V>> {
        ListFuture(
                Collection<? extends ListenableFuture<? extends V>> futures,
                boolean allMustSucceed) {
            init(new ListFutureRunningState(futures, allMustSucceed));
        }

        private final class ListFutureRunningState extends CollectionFutureRunningState {
            ListFutureRunningState(
                    Collection<? extends ListenableFuture<? extends V>> futures,
                    boolean allMustSucceed) {
                super(futures, allMustSucceed);
            }

            @Override
            public List<V> combine(List<Optional<V>> values) {
                List<V> result = new ArrayList<>(values.size());
                for (Optional<V> element : values) {
                    result.add(element != null ? element.orNull() : null);
                }
                return result;
            }
        }
    }
}
