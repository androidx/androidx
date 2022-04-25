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

import static androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.NO_STABLE_IDS;

import android.util.Pair;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An {@link Adapter} implementation that presents the contents of multiple adapters in sequence.
 *
 * <pre>
 * MyAdapter adapter1 = ...;
 * AnotherAdapter adapter2 = ...;
 * ConcatAdapter concatenated = new ConcatAdapter(adapter1, adapter2);
 * recyclerView.setAdapter(concatenated);
 * </pre>
 * <p>
 * By default, {@link ConcatAdapter} isolates view types of nested adapters from each other such
 * that
 * it will change the view type before reporting it back to the {@link RecyclerView} to avoid any
 * conflicts between the view types of added adapters. This also means each added adapter will have
 * its own isolated pool of {@link ViewHolder}s, with no re-use in between added adapters.
 * <p>
 * If your {@link Adapter}s share the same view types, and can support sharing {@link ViewHolder}
 * s between added adapters, provide an instance of {@link Config} where you set
 * {@link Config#isolateViewTypes} to {@code false}. A common usage pattern for this is to return
 * the {@code R.layout.<layout_name>} from the {@link Adapter#getItemViewType(int)} method.
 * <p>
 * When an added adapter calls one of the {@code notify} methods, {@link ConcatAdapter} properly
 * offsets values before reporting it back to the {@link RecyclerView}.
 * If an adapter calls {@link Adapter#notifyDataSetChanged()}, {@link ConcatAdapter} also calls
 * {@link Adapter#notifyDataSetChanged()} as calling
 * {@link Adapter#notifyItemRangeChanged(int, int)} will confuse the {@link RecyclerView}.
 * You are highly encouraged to to use {@link SortedList} or {@link ListAdapter} to avoid
 * calling {@link Adapter#notifyDataSetChanged()}.
 * <p>
 * Whether {@link ConcatAdapter} should support stable ids is defined in the {@link Config}
 * object. Calling {@link Adapter#setHasStableIds(boolean)} has no effect. See documentation
 * for {@link Config.StableIdMode} for details on how to configure {@link ConcatAdapter} to use
 * stable ids. By default, it will not use stable ids and sub adapter stable ids will be ignored.
 * Similar to the case above, you are highly encouraged to use {@link ListAdapter}, which will
 * automatically calculate the changes in the data set for you so you won't need stable ids.
 * <p>
 * It is common to find the adapter position of a {@link ViewHolder} to handle user action on the
 * {@link ViewHolder}. For those cases, instead of calling {@link ViewHolder#getAdapterPosition()},
 * use {@link ViewHolder#getBindingAdapterPosition()}. If your adapters share {@link ViewHolder}s,
 * you can use the {@link ViewHolder#getBindingAdapter()} method to find the adapter which last
 * bound that {@link ViewHolder}.
 */
@SuppressWarnings("unchecked")
public final class ConcatAdapter extends Adapter<ViewHolder> {
    static final String TAG = "ConcatAdapter";
    /**
     * Bulk of the logic is in the controller to keep this class isolated to the public API.
     */
    private final ConcatAdapterController mController;

    /**
     * Creates a ConcatAdapter with {@link Config#DEFAULT} and the given adapters in the given
     * order.
     *
     * @param adapters The list of adapters to add
     */
    @SafeVarargs
    public ConcatAdapter(@NonNull Adapter<? extends ViewHolder>... adapters) {
        this(Config.DEFAULT, adapters);
    }

    /**
     * Creates a ConcatAdapter with the given config and the given adapters in the given order.
     *
     * @param config   The configuration for this ConcatAdapter
     * @param adapters The list of adapters to add
     * @see Config.Builder
     */
    @SafeVarargs
    public ConcatAdapter(
            @NonNull Config config,
            @NonNull Adapter<? extends ViewHolder>... adapters) {
        this(config, Arrays.asList(adapters));
    }

    /**
     * Creates a ConcatAdapter with {@link Config#DEFAULT} and the given adapters in the given
     * order.
     *
     * @param adapters The list of adapters to add
     */
    public ConcatAdapter(@NonNull List<? extends Adapter<? extends ViewHolder>> adapters) {
        this(Config.DEFAULT, adapters);
    }

    /**
     * Creates a ConcatAdapter with the given config and the given adapters in the given order.
     *
     * @param config   The configuration for this ConcatAdapter
     * @param adapters The list of adapters to add
     * @see Config.Builder
     */
    public ConcatAdapter(
            @NonNull Config config,
            @NonNull List<? extends Adapter<? extends ViewHolder>> adapters) {
        mController = new ConcatAdapterController(this, config);
        for (Adapter<? extends ViewHolder> adapter : adapters) {
            addAdapter(adapter);
        }
        // go through super as we override it to be no-op
        super.setHasStableIds(mController.hasStableIds());
    }

    /**
     * Appends the given adapter to the existing list of adapters and notifies the observers of
     * this {@link ConcatAdapter}.
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
     * @param index   The index into which to insert the adapter. ConcatAdapter will throw an
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
     * @return {@code true} if the adapter was previously added to this {@code ConcatAdapter} and
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
     * Calling this method is an error and will result in an {@link UnsupportedOperationException}.
     * You should use the {@link Config} object passed into the ConcatAdapter to configure this
     * behavior.
     *
     * @param hasStableIds Whether items in data set have unique identifiers or not.
     */
    @Override
    public void setHasStableIds(boolean hasStableIds) {
        throw new UnsupportedOperationException(
                "Calling setHasStableIds is not allowed on the ConcatAdapter. "
                        + "Use the Config object passed in the constructor to control this "
                        + "behavior");
    }

    /**
     * Calling this method is an error and will result in an {@link UnsupportedOperationException}.
     *
     * ConcatAdapter infers this value from added {@link Adapter}s.
     *
     * @param strategy The saved state restoration strategy for this Adapter such that
     *                 {@link ConcatAdapter} will allow state restoration only if all added
     *                 adapters allow it or
     *                 there are no adapters.
     */
    @Override
    public void setStateRestorationPolicy(@NonNull StateRestorationPolicy strategy) {
        // do nothing
        throw new UnsupportedOperationException(
                "Calling setStateRestorationPolicy is not allowed on the ConcatAdapter."
                        + " This value is inferred from added adapters");
    }

    @Override
    public long getItemId(int position) {
        return mController.getItemId(position);
    }

    /**
     * Internal method called by the ConcatAdapterController.
     */
    void internalSetStateRestorationPolicy(@NonNull StateRestorationPolicy strategy) {
        super.setStateRestorationPolicy(strategy);
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
     * Returns an unmodifiable copy of the list of adapters in this {@link ConcatAdapter}.
     * Note that this is a copy hence future changes in the ConcatAdapter are not reflected in
     * this list.
     *
     * @return A copy of the list of adapters in this ConcatAdapter.
     */
    @NonNull
    public List<? extends Adapter<? extends ViewHolder>> getAdapters() {
        return Collections.unmodifiableList(mController.getCopyOfAdapters());
    }

    /**
     * Returns the position of the given {@link ViewHolder} in the given {@link Adapter}.
     *
     * If the given {@link Adapter} is not part of this {@link ConcatAdapter},
     * {@link RecyclerView#NO_POSITION} is returned.
     *
     * @param adapter       The adapter which is a sub adapter of this ConcatAdapter or itself.
     * @param viewHolder    The view holder whose local position in the given adapter will be
     *                      returned.
     * @param localPosition The position of the given {@link ViewHolder} in this {@link Adapter}.
     * @return The local position of the given {@link ViewHolder} in the given {@link Adapter} or
     * {@link RecyclerView#NO_POSITION} if the {@link ViewHolder} is not bound to an item or the
     * given {@link Adapter} is not part of this ConcatAdapter.
     */
    @Override
    public int findRelativeAdapterPositionIn(
            @NonNull Adapter<? extends ViewHolder> adapter,
            @NonNull ViewHolder viewHolder,
            int localPosition) {
        return mController.getLocalAdapterPosition(adapter, viewHolder, localPosition);
    }


    /**
     * Retrieve the adapter and local position for a given position in this {@code ConcatAdapter}.
     *
     * This allows for retrieving wrapped adapter information in situations where you don't have a
     * {@link ViewHolder}, such as within a
     * {@link androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup} in which you want to
     * look up information from the source adapter.
     *
     * @param globalPosition The position in this {@code ConcatAdapter}.
     * @return a Pair with the first element set to the wrapped {@code Adapter} containing that
     * position and the second element set to the local position in the wrapped adapter
     * @throws IllegalArgumentException if the specified {@code globalPosition} does not
     * correspond to a valid element of this adapter.  That is, if {@code globalPosition} is less
     * than 0 or greater than the total number of items in the {@code ConcatAdapter}
     */
    @NonNull
    public Pair<Adapter<? extends ViewHolder>, Integer> getWrappedAdapterAndPosition(int
            globalPosition) {
        return mController.getWrappedAdapterAndPosition(globalPosition);
    }

    /**
     * The configuration object for a {@link ConcatAdapter}.
     */
    public static final class Config {
        /**
         * If {@code false}, {@link ConcatAdapter} assumes all assigned adapters share a global
         * view type pool such that they use the same view types to refer to the same
         * {@link ViewHolder}s.
         * <p>
         * Setting this to {@code false} will allow nested adapters to share {@link ViewHolder}s but
         * it also means these adapters should not have conflicting view types
         * ({@link Adapter#getItemViewType(int)}) such that two different adapters return the same
         * view type for different {@link ViewHolder}s.
         *
         * By default, it is set to {@code true} which means {@link ConcatAdapter} will isolate
         * view types across adapters, preventing them from using the same {@link ViewHolder}s.
         */
        public final boolean isolateViewTypes;

        /**
         * Defines whether the {@link ConcatAdapter} should support stable ids or not
         * ({@link Adapter#hasStableIds()}.
         * <p>
         * There are 3 possible options:
         *
         * {@link StableIdMode#NO_STABLE_IDS}: In this mode, {@link ConcatAdapter} ignores the
         * stable
         * ids reported by sub adapters. This is the default mode.
         *
         * {@link StableIdMode#ISOLATED_STABLE_IDS}: In this mode, {@link ConcatAdapter} will return
         * {@code true} from {@link ConcatAdapter#hasStableIds()} and will <b>require</b> all added
         * {@link Adapter}s to have stable ids. As two different adapters may return same stable ids
         * because they are unaware of each-other, {@link ConcatAdapter} will isolate each
         * {@link Adapter}'s id pool from each other such that it will overwrite the reported stable
         * id before reporting back to the {@link RecyclerView}. In this mode, the value returned
         * from {@link ViewHolder#getItemId()} might differ from the value returned from
         * {@link Adapter#getItemId(int)}.
         *
         * {@link StableIdMode#SHARED_STABLE_IDS}: In this mode, {@link ConcatAdapter} will return
         * {@code true} from {@link ConcatAdapter#hasStableIds()} and will <b>require</b> all added
         * {@link Adapter}s to have stable ids. Unlike {@link StableIdMode#ISOLATED_STABLE_IDS},
         * {@link ConcatAdapter} will not override the returned item ids. In this mode,
         * child {@link Adapter}s must be aware of each-other and never return the same id unless
         * an item is moved between {@link Adapter}s.
         *
         * Default value is {@link StableIdMode#NO_STABLE_IDS}.
         */
        @NonNull
        public final StableIdMode stableIdMode;


        /**
         * Default configuration for {@link ConcatAdapter} where {@link Config#isolateViewTypes}
         * is set to {@code true} and {@link Config#stableIdMode} is set to
         * {@link StableIdMode#NO_STABLE_IDS}.
         */
        @NonNull
        public static final Config DEFAULT = new Config(true, NO_STABLE_IDS);

        Config(boolean isolateViewTypes, @NonNull StableIdMode stableIdMode) {
            this.isolateViewTypes = isolateViewTypes;
            this.stableIdMode = stableIdMode;
        }

        /**
         * Defines how {@link ConcatAdapter} handle stable ids ({@link Adapter#hasStableIds()}).
         */
        public enum StableIdMode {
            /**
             * In this mode, {@link ConcatAdapter} ignores the stable
             * ids reported by sub adapters. This is the default mode.
             * Adding an {@link Adapter} with stable ids will result in a warning as it will be
             * ignored.
             */
            NO_STABLE_IDS,
            /**
             * In this mode, {@link ConcatAdapter} will return {@code true} from
             * {@link ConcatAdapter#hasStableIds()} and will <b>require</b> all added
             * {@link Adapter}s to have stable ids. As two different adapters may return
             * same stable ids because they are unaware of each-other, {@link ConcatAdapter} will
             * isolate each {@link Adapter}'s id pool from each other such that it will overwrite
             * the reported stable id before reporting back to the {@link RecyclerView}. In this
             * mode, the value returned from {@link ViewHolder#getItemId()} might differ from the
             * value returned from {@link Adapter#getItemId(int)}.
             *
             * Adding an adapter without stable ids will result in an
             * {@link IllegalArgumentException}.
             */
            ISOLATED_STABLE_IDS,
            /**
             * In this mode, {@link ConcatAdapter} will return {@code true} from
             * {@link ConcatAdapter#hasStableIds()} and will <b>require</b> all added
             * {@link Adapter}s to have stable ids. Unlike {@link StableIdMode#ISOLATED_STABLE_IDS},
             * {@link ConcatAdapter} will not override the returned item ids. In this mode,
             * child {@link Adapter}s must be aware of each-other and never return the same id
             * unless and item is moved between {@link Adapter}s.
             * Adding an adapter without stable ids will result in an
             * {@link IllegalArgumentException}.
             */
            SHARED_STABLE_IDS
        }

        /**
         * The builder for {@link Config} class.
         */
        public static final class Builder {
            private boolean mIsolateViewTypes = DEFAULT.isolateViewTypes;
            private StableIdMode mStableIdMode = DEFAULT.stableIdMode;

            /**
             * Sets whether {@link ConcatAdapter} should isolate view types of nested adapters from
             * each other.
             *
             * @param isolateViewTypes {@code true} if {@link ConcatAdapter} should override view
             *                         types of nested adapters to avoid view type
             *                         conflicts, {@code false} otherwise.
             *                         Defaults to {@link Config#DEFAULT}'s
             *                         {@link Config#isolateViewTypes} value ({@code true}).
             * @return this
             * @see Config#isolateViewTypes
             */
            @NonNull
            public Builder setIsolateViewTypes(boolean isolateViewTypes) {
                mIsolateViewTypes = isolateViewTypes;
                return this;
            }

            /**
             * Sets how the {@link ConcatAdapter} should handle stable ids
             * ({@link Adapter#hasStableIds()}). See documentation in {@link Config#stableIdMode}
             * for details.
             *
             * @param stableIdMode The stable id mode for the {@link ConcatAdapter}. Defaults to
             *                     {@link Config#DEFAULT}'s {@link Config#stableIdMode} value
             *                     ({@link StableIdMode#NO_STABLE_IDS}).
             * @return this
             * @see Config#stableIdMode
             */
            @NonNull
            public Builder setStableIdMode(@NonNull StableIdMode stableIdMode) {
                mStableIdMode = stableIdMode;
                return this;
            }

            /**
             * @return A new instance of {@link Config} with the given parameters.
             */
            @NonNull
            public Config build() {
                return new Config(mIsolateViewTypes, mStableIdMode);
            }
        }
    }
}
