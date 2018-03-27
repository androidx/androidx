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

package androidx.car.drawer;

import android.app.Activity;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.car.R;
import androidx.car.utils.CarUxRestrictionsHelper;
import androidx.car.widget.PagedListView;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Base adapter for displaying items in the car navigation drawer, which uses a
 * {@link PagedListView}.
 *
 * <p>Subclasses can optionally set the title that will be displayed when displaying the contents
 * of the drawer via {@link #setTitle(CharSequence)}. The title can be updated at any point later
 * on. The title of the root adapter will also be the main title showed in the toolbar when the
 * drawer is closed. See {@link CarDrawerController#setRootAdapter(CarDrawerAdapter)} for more
 * information.
 *
 * <p>This class also takes care of implementing the PageListView.ItemCamp contract and subclasses
 * should implement {@link #getActualItemCount()}.
 *
 * <p>To enable support for {@link CarUxRestrictions}, call {@link #start()} in your
 * {@code Activity}'s {@link android.app.Activity#onCreate(Bundle)}, and {@link #stop()} in
 * {@link Activity#onStop()}.
 */
public abstract class CarDrawerAdapter extends RecyclerView.Adapter<DrawerItemViewHolder>
        implements PagedListView.ItemCap, DrawerItemClickListener {
    private final boolean mShowDisabledListOnEmpty;
    private final Drawable mEmptyListDrawable;
    private int mMaxItems = PagedListView.ItemCap.UNLIMITED;
    private CharSequence mTitle;
    private TitleChangeListener mTitleChangeListener;

    private final CarUxRestrictionsHelper mUxRestrictionsHelper;
    private CarUxRestrictions mCurrentUxRestrictions;

    /**
     * Enables support for {@link CarUxRestrictions}.
     *
     * <p>This method can be called from {@code Activity}'s {@link Activity#onStart()}, or at the
     * time of construction.
     *
     * <p>This method must be accompanied with a matching {@link #stop()} to avoid leak.
     */
    public void start() {
        mUxRestrictionsHelper.start();
    }

    /**
     * Disables support for {@link CarUxRestrictions}, and frees up resources.
     *
     * <p>This method should be called from {@code Activity}'s {@link Activity#onStop()}, or at the
     * time of this adapter being discarded.
     */
    public void stop() {
        mUxRestrictionsHelper.stop();
    }

    /**
     * Interface for a class that will be notified a new title has been set on this adapter.
     */
    interface TitleChangeListener {
        /**
         * Called when {@link #setTitle(CharSequence)} has been called and the title has been
         * changed.
         */
        void onTitleChanged(@Nullable CharSequence newTitle);
    }

    protected CarDrawerAdapter(Context context, boolean showDisabledListOnEmpty) {
        mShowDisabledListOnEmpty = showDisabledListOnEmpty;

        mEmptyListDrawable = context.getDrawable(R.drawable.ic_list_view_disable);
        mEmptyListDrawable.setColorFilter(context.getColor(R.color.car_tint),
                PorterDuff.Mode.SRC_IN);

        mUxRestrictionsHelper =
                new CarUxRestrictionsHelper(context, carUxRestrictions -> {
                    mCurrentUxRestrictions = carUxRestrictions;
                    notifyDataSetChanged();
                });
    }

    /** Returns the title set via {@link #setTitle(CharSequence)}. */
    CharSequence getTitle() {
        return mTitle;
    }

    /** Updates the title to display in the toolbar for this Adapter. */
    public final void setTitle(@Nullable CharSequence title) {
        mTitle = title;

        if (mTitleChangeListener != null) {
            mTitleChangeListener.onTitleChanged(mTitle);
        }
    }

    /** Sets a listener to be notified whenever the title of this adapter has been changed. */
    void setTitleChangeListener(@Nullable TitleChangeListener listener) {
        mTitleChangeListener = listener;
    }

    @Override
    public final void setMaxItems(int maxItems) {
        mMaxItems = maxItems;
    }

    @Override
    public final int getItemCount() {
        if (shouldShowDisabledListItem()) {
            return 1;
        }
        return mMaxItems >= 0 ? Math.min(mMaxItems, getActualItemCount()) : getActualItemCount();
    }

    /**
     * Returns the absolute number of items that can be displayed in the list.
     *
     * <p>A class should implement this method to supply the number of items to be displayed.
     * Returning 0 from this method will cause an empty list icon to be displayed in the drawer.
     *
     * <p>A class should override this method rather than {@link #getItemCount()} because that
     * method is handling the logic of when to display the empty list icon. It will return 1 when
     * {@link #getActualItemCount()} returns 0.
     *
     * @return The number of items to be displayed in the list.
     */
    protected abstract int getActualItemCount();

    @Override
    public final int getItemViewType(int position) {
        if (shouldShowDisabledListItem()) {
            return R.layout.car_drawer_list_item_empty;
        }

        return usesSmallLayout(position)
                ? R.layout.car_drawer_list_item_small
                : R.layout.car_drawer_list_item_normal;
    }

    /**
     * Used to indicate the layout used for the Drawer item at given position. Subclasses can
     * override this to use normal layout which includes text element below title.
     *
     * <p>A small layout is presented by the layout {@code R.layout.car_drawer_list_item_small}.
     * Otherwise, the layout {@code R.layout.car_drawer_list_item_normal} will be used.
     *
     * @param position Adapter position of item.
     * @return Whether the item at this position will use a small layout (default) or normal layout.
     */
    protected boolean usesSmallLayout(int position) {
        return true;
    }

    @Override
    public final DrawerItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new DrawerItemViewHolder(view);
    }

    @Override
    public final void onBindViewHolder(DrawerItemViewHolder holder, int position) {
        // Car may not be initialized thus current UXR will not be available.
        if (mCurrentUxRestrictions != null) {
            holder.complyWithUxRestrictions(mCurrentUxRestrictions);
        }

        if (shouldShowDisabledListItem()) {
            holder.getTitle().setText(null);
            holder.getIcon().setImageDrawable(mEmptyListDrawable);
            holder.setItemClickListener(null);
        } else {
            holder.setItemClickListener(this);
            populateViewHolder(holder, position);
        }
    }

    /**
     * Whether or not this adapter should be displaying an empty list icon. The icon is shown if it
     * has been configured to show and there are no items to be displayed.
     */
    private boolean shouldShowDisabledListItem() {
        return mShowDisabledListOnEmpty && getActualItemCount() == 0;
    }

    /**
     * Subclasses should set all elements in {@code holder} to populate the drawer-item. If some
     * element is not used, it should be nulled out since these ViewHolder/View's are recycled.
     */
    protected abstract void populateViewHolder(DrawerItemViewHolder holder, int position);

    /**
     * Called when this adapter has been popped off the stack and is no longer needed. Subclasses
     * can override to do any necessary cleanup.
     */
    public void cleanup() {}
}
