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

import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.Arrays;
import java.util.List;

/**
 * An {@link Adapter} implementation that presents the contents of multiple adapters in sequence.
 *
 * <pre>
 * MyAdapter adapter1 = ...;
 * AnotherAdapter adapter2 = ...;
 * MergeAdapter merged = new MergeAdapter(adapter1, adapter2);
 * recyclerView.setAdapter(mergedAdapter);
 * </pre>
 * <p>
 * By default, {@link MergeAdapter} isolates view types of nested adapters from each other such that
 * it will change the view type before reporting it back to the {@link RecyclerView} to avoid any
 * conflicts between the view types of added adapters. This also means each added adapter will have
 * its own isolated pool of {@link ViewHolder}s, with no re-use in between added adapters.
 * <p>
 * If your {@link Adapter}s share the same view types, and can support sharing {@link ViewHolder}
 * s between added adapters, provide an instance of {@link Config} where you set
 * {@link Config#isolateViewTypes} to {@code false}. A common usage pattern for this is to return
 * the {@code R.layout.<layout_name>} from the {@link Adapter#getItemViewType(int)} method.
 * <p>
 * When an added adapter calls one of the {@code notify} methods, {@link MergeAdapter} properly
 * offsets values before reporting it back to the {@link RecyclerView}.
 * If an adapter calls {@link Adapter#notifyDataSetChanged()}, {@link MergeAdapter} also calls
 * {@link Adapter#notifyDataSetChanged()} as calling
 * {@link Adapter#notifyItemRangeChanged(int, int)} will confuse the {@link RecyclerView}.
 * You are highly encouraged to to use {@link SortedList} or {@link ListAdapter} to avoid
 * calling {@link Adapter#notifyDataSetChanged()}.
 * <p>
 * {@link MergeAdapter} does not yet support stable ids. Even if one of the added adapters have
 * stable ids, {@link MergeAdapter} will not use it.
 * Calling {@link Adapter#setHasStableIds(boolean)} with {@code true} on a {@link MergeAdapter}
 * will result in an {@link IllegalArgumentException}.
 * Similar to the case above, you are highly encouraged to use {@link ListAdapter}, which will
 * automatically calculate the changes in the data set for you.
 * <p>
 * It is common to find the adapter position of a {@link ViewHolder} to handle user action on the
 * {@link ViewHolder}. For those cases, instead of calling {@link ViewHolder#getAdapterPosition()},
 * use {@link ViewHolder#getBindingAdapterPosition()}. If your adapters share {@link ViewHolder}s,
 * you can use the {@link ViewHolder#getBindingAdapter()} method to find the adapter which last
 * bound that {@link ViewHolder}.
 */
@SuppressWarnings("unchecked")
public final class MergeAdapter extends Adapter<ViewHolder> {
    private static final String TAG = "MergeAdapter";
    /**
     * Bulk of the logic is in the controller to keep this class isolated to the public API.
     */
    private final MergeAdapterController mController;

    /**
     * Creates a MergeAdapter with {@link Config#DEFAULT} and the given adapters in the given order.
     *
     * @param adapters The list of adapters to add
     */
    @SafeVarargs
    public MergeAdapter(@NonNull Adapter<? extends ViewHolder>... adapters) {
        this(Config.DEFAULT, adapters);
    }

    /**
     * Creates a MergeAdapter with the given config and the given adapters in the given order.
     *
     * @param config   The configuration for this MergeAdapter
     * @param adapters The list of adapters to add
     * @see Config.Builder
     */
    @SafeVarargs
    public MergeAdapter(
            @NonNull Config config,
            @NonNull Adapter<? extends ViewHolder>... adapters) {
        this(config, Arrays.asList(adapters));
    }

    /**
     * Creates a MergeAdapter with {@link Config#DEFAULT} and the given adapters in the given order.
     *
     * @param adapters The list of adapters to add
     */
    public MergeAdapter(@NonNull List<Adapter<? extends ViewHolder>> adapters) {
        this(Config.DEFAULT, adapters);
    }

    /**
     * Creates a MergeAdapter with the given config and the given adapters in the given order.
     *
     * @param config   The configuration for this MergeAdapter
     * @param adapters The list of adapters to add
     * @see Config.Builder
     */
    public MergeAdapter(
            @NonNull Config config,
            @NonNull List<Adapter<? extends ViewHolder>> adapters) {
        mController = new MergeAdapterController(this, config);
        for (Adapter<? extends ViewHolder> adapter : adapters) {
            addAdapter(adapter);
        }
    }

    /**
     * Appends the given adapter to the existing list of adapters and notifies the observers of
     * this {@link MergeAdapter}.
     *
     * @param adapter The new adapter to add
     * @return {@code true} if the adapter is successfully added because it did not already exist,
     * {@code false} otherwise.
     * @see #addAdapter(int, Adapter)
     * @see #removeAdapter(Adapter)
     */
    public boolean addAdapter(@NonNull Adapter<? extends ViewHolder> adapter) {
        return mController.addAdapter((Adapter<ViewHolder>) adapter);
    }

    /**
     * Adds the given adapter to the given index among other adapters that are already added.
     *
     * @param index   The index into which to insert the adapter. MergeAdapter will throw an
     *                {@link IndexOutOfBoundsException} if the index is not between 0 and current
     *                adapter count (inclusive).
     * @param adapter The new adapter to add to the adapters list.
     * @return {@code true} if the adapter is successfully added because it did not already exist,
     * {@code false} otherwise.
     * @see #addAdapter(Adapter)
     * @see #removeAdapter(Adapter)
     */
    public boolean addAdapter(int index, @NonNull Adapter<? extends ViewHolder> adapter) {
        return mController.addAdapter(index, (Adapter<ViewHolder>) adapter);
    }

    /**
     * Removes the given adapter from the adapters list if it exists
     *
     * @param adapter The adapter to remove
     * @return {@code true} if the adapter was previously added to this {@code MergeAdapter} and
     * now removed or {@code false} if it couldn't be found.
     */
    public boolean removeAdapter(@NonNull Adapter<? extends ViewHolder> adapter) {
        return mController.removeAdapter((Adapter<ViewHolder>) adapter);
    }

    @Override
    public int getItemViewType(int position) {
        return mController.getItemViewType(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return mController.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        mController.onBindViewHolder(holder, position);
    }

    /**
     * MergeAdapter does not support stable ids yet.
     * Calling this method with {@code true} will result in an {@link IllegalArgumentException}.
     *
     * @param hasStableIds Whether items in data set have unique identifiers or not.
     */
    @Override
    public void setHasStableIds(boolean hasStableIds) {
        Preconditions.checkArgument(!hasStableIds,
                "Stable ids are not supported for MergeAdapter yet");
        //noinspection ConstantConditions
        super.setHasStableIds(hasStableIds);
    }

    /**
     * Calling this method on the MergeAdapter has no impact as the MergeAdapter infers this
     * value from added sub adapters.
     *
     * {@link MergeAdapter} picks the least allowing strategy from given adapters. So if a nested
     * adapter sets this to
     * {@link androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationStrategy#PREVENT},
     * merge adapter will report that.
     *
     * @param strategy The saved state restoration strategy for this Adapter.
     */
    @Override
    public void setStateRestorationStrategy(@NonNull StateRestorationStrategy strategy) {
        // do nothing
        Log.w(TAG, "Calling setStateRestorationStrategy has no impact on the MergeAdapter as"
                + " it derives its state restoration strategy from nested adapters");
    }

    /**
     * Internal method to be called from the wrappers.
     */
    void internalSetStateRestorationStrategy(@NonNull StateRestorationStrategy strategy) {
        super.setStateRestorationStrategy(strategy);
    }

    @Override
    public int getItemCount() {
        return mController.getTotalCount();
    }

    @Override
    public boolean onFailedToRecycleView(@NonNull ViewHolder holder) {
        return mController.onFailedToRecycleView(holder);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        mController.onViewAttachedToWindow(holder);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        mController.onViewDetachedFromWindow(holder);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        mController.onViewRecycled(holder);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        mController.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        mController.onDetachedFromRecyclerView(recyclerView);
    }

    /**
     * Returns a copy of the list of adapters in this {@link MergeAdapter}.
     * Note that this is a copy hence future changes in the MergeAdapter are not reflected in
     * this list.
     *
     * @return A copy of the list of adapters in this MergeAdapter.
     */
    @NonNull
    public List<Adapter<? extends ViewHolder>> getCopyOfAdapters() {
        return mController.getCopyOfAdapters();
    }

    /**
     * Returns the position of the given {@link ViewHolder} in the given {@link Adapter}.
     *
     * If the given {@link Adapter} is not part of this {@link MergeAdapter},
     * {@link RecyclerView#NO_POSITION} is returned.
     *
     * @param adapter    The adapter which is a sub adapter of this MergeAdapter or itself.
     * @param viewHolder The view holder whose local position in the given adapter will be returned.
     * @return The local position of the given {@link ViewHolder} in the given {@link Adapter} or
     * {@link RecyclerView#NO_POSITION} if the {@link ViewHolder} is not bound to an item or the
     * given {@link Adapter} is not part of this MergeAdapter.
     */
    @Override
    public int findRelativeAdapterPositionIn(
            @NonNull Adapter<? extends ViewHolder> adapter,
            @NonNull ViewHolder viewHolder,
            int globalPosition) {
        return mController.getLocalAdapterPosition(adapter, viewHolder, globalPosition);
    }

    /**
     * The configuration object for a {@link MergeAdapter}.
     */
    public static class Config {
        /**
         * If {@code false}, {@link MergeAdapter} assumes all assigned adapters share a global
         * view type pool such that they use the same view types to refer to the same
         * {@link ViewHolder}s.
         * <p>
         * Setting this to {@code false} will allow nested adapters to share {@link ViewHolder}s but
         * it also means these adapters should not have conflicting view types
         * ({@link Adapter#getItemViewType(int)}) such that two different adapters return the same
         * view type for different {@link ViewHolder}s.
         *
         * By default, it is set to {@code true} which means {@link MergeAdapter} will isolate
         * view types across adapters, preventing them from using the same {@link ViewHolder}s.
         */
        public final boolean isolateViewTypes;
        @NonNull
        public static final Config DEFAULT = new Config(true);

        Config(boolean isolateViewTypes) {
            this.isolateViewTypes = isolateViewTypes;
        }

        /**
         * The builder for {@link Config} class.
         */
        public static class Builder {
            private boolean mIsolateViewTypes;

            /**
             * Sets whether {@link MergeAdapter} should isolate view types of nested adapters from
             * each other.
             *
             * @param isolateViewTypes {@code true} if {@link MergeAdapter} should override view
             *                         types of nested adapters to avoid view type
             *                         conflicts, {@code false} otherwise.
             *                         Defaults to true.
             * @return this
             * @see Config#isolateViewTypes
             */
            @NonNull
            public Builder setIsolateViewTypes(boolean isolateViewTypes) {
                mIsolateViewTypes = isolateViewTypes;
                return this;
            }

            /**
             * @return A new instance of {@link Config} with the given parameters.
             */
            @NonNull
            public Config build() {
                return new Config(mIsolateViewTypes);
            }
        }
    }
}
