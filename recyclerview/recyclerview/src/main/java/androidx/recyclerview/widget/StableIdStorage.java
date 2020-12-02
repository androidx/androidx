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

package androidx.recyclerview.widget;

import androidx.annotation.NonNull;
import androidx.collection.LongSparseArray;

/**
 * Used by {@link ConcatAdapter} to isolate item ids between nested adapters, if necessary.
 */
interface StableIdStorage {
    @NonNull
    StableIdLookup createStableIdLookup();

    /**
     * Interface that provides {@link NestedAdapterWrapper}s a way to map their local stable ids
     * into global stable ids, based on the configuration of the {@link ConcatAdapter}.
     */
    interface StableIdLookup {
        long localToGlobal(long localId);
    }

    /**
     * Returns {@link RecyclerView#NO_ID} for all positions. In other words, stable ids are not
     * supported.
     */
    class NoStableIdStorage implements StableIdStorage {
        private final StableIdLookup mNoIdLookup = new StableIdLookup() {
            @Override
            public long localToGlobal(long localId) {
                return RecyclerView.NO_ID;
            }
        };

        @NonNull
        @Override
        public StableIdLookup createStableIdLookup() {
            return mNoIdLookup;
        }
    }

    /**
     * A pass-through implementation that reports the stable id in sub adapters as is.
     */
    class SharedPoolStableIdStorage implements StableIdStorage {
        private final StableIdLookup mSameIdLookup = new StableIdLookup() {
            @Override
            public long localToGlobal(long localId) {
                return localId;
            }
        };

        @NonNull
        @Override
        public StableIdLookup createStableIdLookup() {
            return mSameIdLookup;
        }
    }

    /**
     * An isolating implementation that ensures the stable ids among adapters do not conflict with
     * each-other. It keeps a mapping for each adapter from its local stable ids to a global domain
     * and always replaces the local id w/ a globally available ID to be consistent.
     */
    class IsolatedStableIdStorage implements StableIdStorage {
        long mNextStableId = 0;

        long obtainId() {
            return mNextStableId++;
        }

        @NonNull
        @Override
        public StableIdLookup createStableIdLookup() {
            return new WrapperStableIdLookup();
        }

        class WrapperStableIdLookup implements StableIdLookup {
            private final LongSparseArray<Long> mLocalToGlobalLookup = new LongSparseArray<>();

            @Override
            public long localToGlobal(long localId) {
                Long globalId = mLocalToGlobalLookup.get(localId);
                if (globalId == null) {
                    globalId = obtainId();
                    mLocalToGlobalLookup.put(localId, globalId);
                }
                return globalId;
            }
        }
    }
}
