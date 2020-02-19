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

import android.util.SparseArray;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Used by {@link MergeAdapter} to isolate view types between nested adapters, if necessary.
 */
interface ViewTypeStorage {
    @NonNull
    NestedAdapterWrapper getWrapperForGlobalType(int globalViewType);

    @NonNull
    ViewTypeLookup createViewTypeWrapper(
            @NonNull NestedAdapterWrapper wrapper
    );

    /**
     * Api given to {@link NestedAdapterWrapper}s.
     */
    interface ViewTypeLookup {
        int localToGlobal(int localType);

        int globalToLocal(int globalType);

        void dispose();
    }

    class SharedIdRangeViewTypeStorage implements ViewTypeStorage {
        // we keep a list of nested wrappers here even though we only need 1 to create because
        // they might be removed.
        SparseArray<List<NestedAdapterWrapper>> mGlobalTypeToWrapper = new SparseArray<>();

        @NonNull
        @Override
        public NestedAdapterWrapper getWrapperForGlobalType(int globalViewType) {
            List<NestedAdapterWrapper> nestedAdapterWrappers = mGlobalTypeToWrapper.get(
                    globalViewType);
            if (nestedAdapterWrappers == null || nestedAdapterWrappers.isEmpty()) {
                throw new IllegalArgumentException("Cannot find the wrapper for global view"
                        + " type " + globalViewType);
            }
            // just return the first one since they are shared
            return nestedAdapterWrappers.get(0);
        }

        @NonNull
        @Override
        public ViewTypeLookup createViewTypeWrapper(
                @NonNull NestedAdapterWrapper wrapper) {
            return new WrapperViewTypeLookup(wrapper);
        }

        void removeWrapper(@NonNull NestedAdapterWrapper wrapper) {
            for (int i = mGlobalTypeToWrapper.size() - 1; i >= 0; i--) {
                List<NestedAdapterWrapper> wrappers = mGlobalTypeToWrapper.valueAt(i);
                if (wrappers.remove(wrapper)) {
                    if (wrappers.isEmpty()) {
                        mGlobalTypeToWrapper.removeAt(i);
                    }
                }
            }
        }

        class WrapperViewTypeLookup implements ViewTypeLookup {
            final NestedAdapterWrapper mWrapper;

            WrapperViewTypeLookup(NestedAdapterWrapper wrapper) {
                mWrapper = wrapper;
            }

            @Override
            public int localToGlobal(int localType) {
                // register it first
                List<NestedAdapterWrapper> wrappers = mGlobalTypeToWrapper.get(
                        localType);
                if (wrappers == null) {
                    wrappers = new ArrayList<>();
                    mGlobalTypeToWrapper.put(localType, wrappers);
                }
                if (!wrappers.contains(mWrapper)) {
                    wrappers.add(mWrapper);
                }
                return localType;
            }

            @Override
            public int globalToLocal(int globalType) {
                return globalType;
            }

            @Override
            public void dispose() {
                removeWrapper(mWrapper);
            }
        }
    }

    class IsolatedViewTypeStorage implements ViewTypeStorage {
        SparseArray<NestedAdapterWrapper> mGlobalTypeToWrapper = new SparseArray<>();

        int mNextViewType = 0;

        int obtainViewType(NestedAdapterWrapper wrapper) {
            int nextId = mNextViewType++;
            mGlobalTypeToWrapper.put(nextId, wrapper);
            return nextId;
        }

        @NonNull
        @Override
        public NestedAdapterWrapper getWrapperForGlobalType(int globalViewType) {
            NestedAdapterWrapper wrapper = mGlobalTypeToWrapper.get(
                    globalViewType);
            if (wrapper == null) {
                throw new IllegalArgumentException("Cannot find the wrapper for global"
                        + " view type " + globalViewType);
            }
            return wrapper;
        }

        @Override
        @NonNull
        public ViewTypeLookup createViewTypeWrapper(
                @NonNull NestedAdapterWrapper wrapper) {
            return new WrapperViewTypeLookup(wrapper);
        }

        void removeWrapper(@NonNull NestedAdapterWrapper wrapper) {
            for (int i = mGlobalTypeToWrapper.size() - 1; i >= 0; i--) {
                NestedAdapterWrapper existingWrapper = mGlobalTypeToWrapper.valueAt(i);
                if (existingWrapper == wrapper) {
                    mGlobalTypeToWrapper.removeAt(i);
                }
            }
        }

        class WrapperViewTypeLookup implements ViewTypeLookup {
            private SparseIntArray mLocalToGlobalMapping = new SparseIntArray(1);
            private SparseIntArray mGlobalToLocalMapping = new SparseIntArray(1);
            final NestedAdapterWrapper mWrapper;

            WrapperViewTypeLookup(NestedAdapterWrapper wrapper) {
                mWrapper = wrapper;
            }

            @Override
            public int localToGlobal(int localType) {
                int index = mLocalToGlobalMapping.indexOfKey(localType);
                if (index > -1) {
                    return mLocalToGlobalMapping.valueAt(index);
                }
                // get a new key.
                int globalType = obtainViewType(mWrapper);
                mLocalToGlobalMapping.put(localType, globalType);
                mGlobalToLocalMapping.put(globalType, localType);
                return globalType;
            }

            @Override
            public int globalToLocal(int globalType) {
                int index = mGlobalToLocalMapping.indexOfKey(globalType);
                if (index < 0) {
                    throw new IllegalStateException("requested global type " + globalType + " does"
                            + " not belong to the adapter:" + mWrapper.adapter);
                }
                return mGlobalToLocalMapping.valueAt(index);
            }

            @Override
            public void dispose() {
                removeWrapper(mWrapper);
            }
        }
    }
}
