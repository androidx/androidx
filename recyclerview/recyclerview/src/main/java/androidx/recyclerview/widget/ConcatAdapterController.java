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

import static androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS;
import static androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.NO_STABLE_IDS;
import static androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.SHARED_STABLE_IDS;
import static androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.ALLOW;
import static androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT;
import static androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY;
import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.util.Log;
import android.util.Pair;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * All logic for the {@link ConcatAdapter} is here so that we can clearly see a separation
 * between an adapter implementation and merging logic.
 */
class ConcatAdapterController implements NestedAdapterWrapper.Callback {
    private final ConcatAdapter mConcatAdapter;

    /**
     * Holds the mapping from the view type to the adapter which reported that type.
     */
    private final ViewTypeStorage mViewTypeStorage;

    /**
     * We hold onto the list of attached recyclerviews so that we can dispatch attach/detach to
     * any adapter that was added later on.
     * Probably does not need to be a weak reference but playing safe here.
     */
    private List<WeakReference<RecyclerView>> mAttachedRecyclerViews = new ArrayList<>();

    /**
     * Keeps the information about which ViewHolder is bound by which adapter.
     * It is set in onBind, reset at onRecycle.
     */
    private final IdentityHashMap<ViewHolder, NestedAdapterWrapper>
            mBinderLookup = new IdentityHashMap<>();

    private List<NestedAdapterWrapper> mWrappers = new ArrayList<>();

    // keep one of these around so that we can return wrapper & position w/o allocation ¯\_(ツ)_/¯
    private WrapperAndLocalPosition mReusableHolder = new WrapperAndLocalPosition();

    @NonNull
    private final ConcatAdapter.Config.StableIdMode mStableIdMode;

    /**
     * This is where we keep stable ids, if supported
     */
    private final StableIdStorage mStableIdStorage;

    ConcatAdapterController(
            ConcatAdapter concatAdapter,
            ConcatAdapter.Config config) {
        mConcatAdapter = concatAdapter;

        // setup view type handling
        if (config.isolateViewTypes) {
            mViewTypeStorage = new ViewTypeStorage.IsolatedViewTypeStorage();
        } else {
            mViewTypeStorage = new ViewTypeStorage.SharedIdRangeViewTypeStorage();
        }

        // setup stable id handling
        mStableIdMode = config.stableIdMode;
        if (config.stableIdMode == NO_STABLE_IDS) {
            mStableIdStorage = new StableIdStorage.NoStableIdStorage();
        } else if (config.stableIdMode == ISOLATED_STABLE_IDS) {
            mStableIdStorage = new StableIdStorage.IsolatedStableIdStorage();
        } else if (config.stableIdMode == SHARED_STABLE_IDS) {
            mStableIdStorage = new StableIdStorage.SharedPoolStableIdStorage();
        } else {
            throw new IllegalArgumentException("unknown stable id mode");
        }
    }

    @Nullable
    private NestedAdapterWrapper findWrapperFor(Adapter<ViewHolder> adapter) {
        final int index = indexOfWrapper(adapter);
        if (index == -1) {
            return null;
        }
        return mWrappers.get(index);
    }

    private int indexOfWrapper(Adapter<ViewHolder> adapter) {
        final int limit = mWrappers.size();
        for (int i = 0; i < limit; i++) {
            if (mWrappers.get(i).adapter == adapter) {
                return i;
            }
        }
        return -1;
    }

    /**
     * return true if added, false otherwise.
     *
     * @see ConcatAdapter#addAdapter(Adapter)
     */
    boolean addAdapter(Adapter<ViewHolder> adapter) {
        return addAdapter(mWrappers.size(), adapter);
    }

    /**
     * return true if added, false otherwise.
     * throws exception if index is out of bounds
     *
     * @see ConcatAdapter#addAdapter(int, Adapter)
     */
    boolean addAdapter(int index, Adapter<ViewHolder> adapter) {
        if (index < 0 || index > mWrappers.size()) {
            throw new IndexOutOfBoundsException("Index must be between 0 and "
                    + mWrappers.size() + ". Given:" + index);
        }
        if (hasStableIds()) {
            Preconditions.checkArgument(adapter.hasStableIds(),
                    "All sub adapters must have stable ids when stable id mode "
                    + "is ISOLATED_STABLE_IDS or SHARED_STABLE_IDS");
        } else {
            if (adapter.hasStableIds()) {
                Log.w(ConcatAdapter.TAG, "Stable ids in the adapter will be ignored as the"
                        + " ConcatAdapter is configured not to have stable ids");
            }
        }
        NestedAdapterWrapper existing = findWrapperFor(adapter);
        if (existing != null) {
            return false;
        }
        NestedAdapterWrapper wrapper = new NestedAdapterWrapper(adapter, this,
                mViewTypeStorage, mStableIdStorage.createStableIdLookup());
        mWrappers.add(index, wrapper);
        // notify attach for all recyclerview
        for (WeakReference<RecyclerView> reference : mAttachedRecyclerViews) {
            RecyclerView recyclerView = reference.get();
            if (recyclerView != null) {
                adapter.onAttachedToRecyclerView(recyclerView);
            }
        }
        // new items, notify add for them
        if (wrapper.getCachedItemCount() > 0) {
            mConcatAdapter.notifyItemRangeInserted(
                    countItemsBefore(wrapper),
                    wrapper.getCachedItemCount()
            );
        }
        // reset state restoration strategy
        calculateAndUpdateStateRestorationPolicy();
        return true;
    }

    boolean removeAdapter(Adapter<ViewHolder> adapter) {
        final int index = indexOfWrapper(adapter);
        if (index == -1) {
            return false;
        }
        NestedAdapterWrapper wrapper = mWrappers.get(index);
        int offset = countItemsBefore(wrapper);
        mWrappers.remove(index);
        mConcatAdapter.notifyItemRangeRemoved(offset, wrapper.getCachedItemCount());
        // notify detach for all recyclerviews
        for (WeakReference<RecyclerView> reference : mAttachedRecyclerViews) {
            RecyclerView recyclerView = reference.get();
            if (recyclerView != null) {
                adapter.onDetachedFromRecyclerView(recyclerView);
            }
        }
        wrapper.dispose();
        calculateAndUpdateStateRestorationPolicy();
        return true;
    }

    private int countItemsBefore(NestedAdapterWrapper wrapper) {
        int count = 0;
        for (NestedAdapterWrapper item : mWrappers) {
            if (item != wrapper) {
                count += item.getCachedItemCount();
            } else {
                break;
            }
        }
        return count;
    }

    public long getItemId(int globalPosition) {
        WrapperAndLocalPosition wrapperAndPos = findWrapperAndLocalPosition(globalPosition);
        long globalItemId = wrapperAndPos.mWrapper.getItemId(wrapperAndPos.mLocalPosition);
        releaseWrapperAndLocalPosition(wrapperAndPos);
        return globalItemId;
    }

    @Override
    public void onChanged(@NonNull NestedAdapterWrapper wrapper) {
        // TODO should we notify more cleverly, maybe in v2
        mConcatAdapter.notifyDataSetChanged();
        calculateAndUpdateStateRestorationPolicy();
    }

    @Override
    public void onItemRangeChanged(@NonNull NestedAdapterWrapper nestedAdapterWrapper,
            int positionStart, int itemCount) {
        final int offset = countItemsBefore(nestedAdapterWrapper);
        mConcatAdapter.notifyItemRangeChanged(
                positionStart + offset,
                itemCount
        );
    }

    @Override
    public void onItemRangeChanged(@NonNull NestedAdapterWrapper nestedAdapterWrapper,
            int positionStart, int itemCount, @Nullable Object payload) {
        final int offset = countItemsBefore(nestedAdapterWrapper);
        mConcatAdapter.notifyItemRangeChanged(
                positionStart + offset,
                itemCount,
                payload
        );
    }

    @Override
    public void onItemRangeInserted(@NonNull NestedAdapterWrapper nestedAdapterWrapper,
            int positionStart, int itemCount) {
        final int offset = countItemsBefore(nestedAdapterWrapper);
        mConcatAdapter.notifyItemRangeInserted(
                positionStart + offset,
                itemCount
        );
    }

    @Override
    public void onItemRangeRemoved(@NonNull NestedAdapterWrapper nestedAdapterWrapper,
            int positionStart, int itemCount) {
        int offset = countItemsBefore(nestedAdapterWrapper);
        mConcatAdapter.notifyItemRangeRemoved(
                positionStart + offset,
                itemCount
        );
    }

    @Override
    public void onItemRangeMoved(@NonNull NestedAdapterWrapper nestedAdapterWrapper,
            int fromPosition, int toPosition) {
        int offset = countItemsBefore(nestedAdapterWrapper);
        mConcatAdapter.notifyItemMoved(
                fromPosition + offset,
                toPosition + offset
        );
    }

    @Override
    public void onStateRestorationPolicyChanged(NestedAdapterWrapper nestedAdapterWrapper) {
        calculateAndUpdateStateRestorationPolicy();
    }

    private void calculateAndUpdateStateRestorationPolicy() {
        StateRestorationPolicy newPolicy = computeStateRestorationPolicy();
        if (newPolicy != mConcatAdapter.getStateRestorationPolicy()) {
            mConcatAdapter.internalSetStateRestorationPolicy(newPolicy);
        }
    }

    private StateRestorationPolicy computeStateRestorationPolicy() {
        for (NestedAdapterWrapper wrapper : mWrappers) {
            StateRestorationPolicy strategy =
                    wrapper.adapter.getStateRestorationPolicy();
            if (strategy == PREVENT) {
                // one adapter can block all
                return PREVENT;
            } else if (strategy == PREVENT_WHEN_EMPTY && wrapper.getCachedItemCount() == 0) {
                // an adapter wants to allow w/ size but we need to make sure there is no prevent
                return PREVENT;
            }
        }
        return ALLOW;
    }

    public int getTotalCount() {
        // should we cache this as well ?
        int total = 0;
        for (NestedAdapterWrapper wrapper : mWrappers) {
            total += wrapper.getCachedItemCount();
        }
        return total;
    }

    public int getItemViewType(int globalPosition) {
        WrapperAndLocalPosition wrapperAndPos = findWrapperAndLocalPosition(globalPosition);
        int itemViewType = wrapperAndPos.mWrapper.getItemViewType(wrapperAndPos.mLocalPosition);
        releaseWrapperAndLocalPosition(wrapperAndPos);
        return itemViewType;
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int globalViewType) {
        NestedAdapterWrapper wrapper = mViewTypeStorage.getWrapperForGlobalType(globalViewType);
        return wrapper.onCreateViewHolder(parent, globalViewType);
    }

    public Pair<Adapter<? extends ViewHolder>, Integer> getWrappedAdapterAndPosition(
            int globalPosition) {
        WrapperAndLocalPosition wrapper = findWrapperAndLocalPosition(globalPosition);
        Pair<Adapter<? extends ViewHolder>, Integer> pair = new Pair<>(wrapper.mWrapper.adapter,
                wrapper.mLocalPosition);
        releaseWrapperAndLocalPosition(wrapper);
        return pair;
    }

    /**
     * Always call {@link #releaseWrapperAndLocalPosition(WrapperAndLocalPosition)} when you are
     * done with it
     */
    @NonNull
    private WrapperAndLocalPosition findWrapperAndLocalPosition(
            int globalPosition
    ) {
        WrapperAndLocalPosition result;
        if (mReusableHolder.mInUse) {
            result = new WrapperAndLocalPosition();
        } else {
            mReusableHolder.mInUse = true;
            result = mReusableHolder;
        }
        int localPosition = globalPosition;
        for (NestedAdapterWrapper wrapper : mWrappers) {
            if (wrapper.getCachedItemCount() > localPosition) {
                result.mWrapper = wrapper;
                result.mLocalPosition = localPosition;
                break;
            }
            localPosition -= wrapper.getCachedItemCount();
        }
        if (result.mWrapper == null) {
            throw new IllegalArgumentException("Cannot find wrapper for " + globalPosition);
        }
        return result;
    }

    private void releaseWrapperAndLocalPosition(WrapperAndLocalPosition wrapperAndLocalPosition) {
        wrapperAndLocalPosition.mInUse = false;
        wrapperAndLocalPosition.mWrapper = null;
        wrapperAndLocalPosition.mLocalPosition = -1;
        mReusableHolder = wrapperAndLocalPosition;
    }

    public void onBindViewHolder(ViewHolder holder, int globalPosition) {
        WrapperAndLocalPosition wrapperAndPos = findWrapperAndLocalPosition(globalPosition);
        mBinderLookup.put(holder, wrapperAndPos.mWrapper);
        wrapperAndPos.mWrapper.onBindViewHolder(holder, wrapperAndPos.mLocalPosition);
        releaseWrapperAndLocalPosition(wrapperAndPos);
    }

    public boolean canRestoreState() {
        for (NestedAdapterWrapper wrapper : mWrappers) {
            if (!wrapper.adapter.canRestoreState()) {
                return false;
            }
        }
        return true;
    }

    public void onViewAttachedToWindow(ViewHolder holder) {
        NestedAdapterWrapper wrapper = getWrapper(holder);
        wrapper.adapter.onViewAttachedToWindow(holder);
    }

    public void onViewDetachedFromWindow(ViewHolder holder) {
        NestedAdapterWrapper wrapper = getWrapper(holder);
        wrapper.adapter.onViewDetachedFromWindow(holder);
    }

    public void onViewRecycled(ViewHolder holder) {
        NestedAdapterWrapper wrapper = mBinderLookup.get(holder);
        if (wrapper == null) {
            throw new IllegalStateException("Cannot find wrapper for " + holder
                    + ", seems like it is not bound by this adapter: " + this);
        }
        wrapper.adapter.onViewRecycled(holder);
        mBinderLookup.remove(holder);
    }

    public boolean onFailedToRecycleView(ViewHolder holder) {
        NestedAdapterWrapper wrapper = mBinderLookup.get(holder);
        if (wrapper == null) {
            throw new IllegalStateException("Cannot find wrapper for " + holder
                    + ", seems like it is not bound by this adapter: " + this);
        }
        final boolean result = wrapper.adapter.onFailedToRecycleView(holder);
        mBinderLookup.remove(holder);
        return result;
    }

    @NonNull
    private NestedAdapterWrapper getWrapper(ViewHolder holder) {
        NestedAdapterWrapper wrapper = mBinderLookup.get(holder);
        if (wrapper == null) {
            throw new IllegalStateException("Cannot find wrapper for " + holder
                    + ", seems like it is not bound by this adapter: " + this);
        }
        return wrapper;
    }

    private boolean isAttachedTo(RecyclerView recyclerView) {
        for (WeakReference<RecyclerView> reference : mAttachedRecyclerViews) {
            if (reference.get() == recyclerView) {
                return true;
            }
        }
        return false;
    }

    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        if (isAttachedTo(recyclerView)) {
            return;
        }
        mAttachedRecyclerViews.add(new WeakReference<>(recyclerView));
        for (NestedAdapterWrapper wrapper : mWrappers) {
            wrapper.adapter.onAttachedToRecyclerView(recyclerView);
        }
    }

    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        for (int i = mAttachedRecyclerViews.size() - 1; i >= 0; i--) {
            WeakReference<RecyclerView> reference = mAttachedRecyclerViews.get(i);
            if (reference.get() == null) {
                mAttachedRecyclerViews.remove(i);
            } else if (reference.get() == recyclerView) {
                mAttachedRecyclerViews.remove(i);
                break; // here we can break as we don't keep duplicates
            }
        }
        for (NestedAdapterWrapper wrapper : mWrappers) {
            wrapper.adapter.onDetachedFromRecyclerView(recyclerView);
        }
    }

    public int getLocalAdapterPosition(
            Adapter<? extends ViewHolder> adapter,
            ViewHolder viewHolder,
            int globalPosition
    ) {
        NestedAdapterWrapper wrapper = mBinderLookup.get(viewHolder);
        if (wrapper == null) {
            return NO_POSITION;
        }
        int itemsBefore = countItemsBefore(wrapper);
        // local position is globalPosition - itemsBefore
        int localPosition = globalPosition - itemsBefore;
        // Early error detection:
        int itemCount = wrapper.adapter.getItemCount();
        if (localPosition < 0 || localPosition >= itemCount) {
            throw new IllegalStateException("Detected inconsistent adapter updates. The"
                    + " local position of the view holder maps to " + localPosition + " which"
                    + " is out of bounds for the adapter with size "
                    + itemCount + "."
                    + "Make sure to immediately call notify methods in your adapter when you "
                    + "change the backing data"
                    + "viewHolder:" + viewHolder
                    + "adapter:" + adapter);
        }
        return wrapper.adapter.findRelativeAdapterPositionIn(adapter, viewHolder, localPosition);
    }


    @Nullable
    public Adapter<? extends ViewHolder> getBoundAdapter(ViewHolder viewHolder) {
        NestedAdapterWrapper wrapper = mBinderLookup.get(viewHolder);
        if (wrapper == null) {
            return null;
        }
        return wrapper.adapter;
    }

    @SuppressWarnings("MixedMutabilityReturnType")
    public List<Adapter<? extends ViewHolder>> getCopyOfAdapters() {
        if (mWrappers.isEmpty()) {
            return Collections.emptyList();
        }
        List<Adapter<? extends ViewHolder>> adapters = new ArrayList<>(mWrappers.size());
        for (NestedAdapterWrapper wrapper : mWrappers) {
            adapters.add(wrapper.adapter);
        }
        return adapters;
    }

    public boolean hasStableIds() {
        return mStableIdMode != NO_STABLE_IDS;
    }

    /**
     * Helper class to hold onto wrapper and local position without allocating objects as this is
     * a very common call.
     */
    static class WrapperAndLocalPosition {
        NestedAdapterWrapper mWrapper;
        int mLocalPosition;
        boolean mInUse;
    }
}
