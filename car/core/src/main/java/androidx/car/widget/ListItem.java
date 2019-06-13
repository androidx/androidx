/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.car.widget;

import android.view.View;

import androidx.annotation.Nullable;
import androidx.car.uxrestrictions.OnUxRestrictionsChangedListener;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Definition of items that can be inserted into {@link ListItemAdapter}.
 *
 * @param <VH> ViewHolder that extends {@link ListItem.ViewHolder}.
 */
public abstract class ListItem<VH extends ListItem.ViewHolder> {

    // Whether the item should calculate view layout params. This usually happens when the item is
    // updated after bind() is called. Calling bind() resets to false.
    private boolean mDirty;

    // Tag for indicating whether to show the divider.
    private boolean mShowDivider = true;

    private final List<ViewBinder<VH>> mCustomBinders = new ArrayList<>();
    // Stores ViewBinders to revert customization. Does not guarantee to 1:1 match ViewBinders
    // in mCustomerBinders.
    private final List<ViewBinder<VH>> mCustomBinderCleanUps = new ArrayList<>();

    /**
     * Classes that extends {@code ListItem} should register its view type in
     * {@link ListItemAdapter#registerListItemViewType(int, int, Function)}.
     *
     * @return type of this ListItem.
     */
    public abstract int getViewType();

    /**
     * Called when ListItem is bound to its ViewHolder.
     */
    @SuppressWarnings("unchecked")
    final void bind(VH viewHolder) {
        // Attempt to clean up custom view binder from previous item (if any).
        // Then save the clean up binders for next item.
        viewHolder.cleanUp();
        for (ViewBinder cleanUp : mCustomBinderCleanUps) {
            viewHolder.addCleanUp(cleanUp);
        }

        if (isDirty()) {
            resolveDirtyState();
            markClean();
        }
        onBind(viewHolder);

        // Custom view binders are applied after view layout.
        for (ViewBinder<VH> binder: mCustomBinders) {
            binder.bind(viewHolder);
        }
    }

    /**
     * Sets the enabled state of the bound {@link ViewHolder}.
     *
     * <p>All visible children views of {@code ViewHolder} should be set to {@code enabled}. Caller
     * is responsible for notifying {@link ListItemAdapter} about data change.
     *
     * <p>Disabled items are usually styled at 50% opacity. Consider similar styling for
     * consistency.
     */
    public abstract void setEnabled(boolean enabled);

    /**
     * Marks this item as dirty so {@link #resolveDirtyState()} is required in next bind() call.
     *
     * <p>This method should be called in each setter.
     */
    protected void markDirty() {
        mDirty = true;
    }

    /**
     * Marks this item as not dirty. No need to call {@link #resolveDirtyState()} in next bind().
     */
    protected void markClean() {
        mDirty = false;
    }

    /**
     * @return {@code true} if next bind() should call {@link #resolveDirtyState()}.
     */
    protected boolean isDirty() {
        return mDirty;
    }

    /**
     * Whether to show the item divider coming after this {@code ListItem}.
     *
     * <p>Note: For this to work, one must invoke
     * {@code PagedListView.setDividerVisibilityManager(adapter} for {@link ListItemAdapter} and
     * have dividers enabled on {@link PagedListView}.
     */
    public void setShowDivider(boolean showDivider) {
        mShowDivider = showDivider;
        markDirty();
    }

    /**
     * Returns whether or not the divider that comes after this ListItem should be shown.
     *
     * @return {@code true} if the divider should be shown. Defaults to {@code true}.
     */
    public boolean getShowDivider() {
        return mShowDivider;
    }

    /**
     * Does the work that moves the ListItem from dirty state to clean state, i.e. the work required
     * the first time this ListItem {@code bind}s to {@link ListItem.ViewHolder}.
     * This method will transition ListItem to clean state. ListItem in clean state should move to
     * dirty state when it is modified by calling {@link #markDirty()}.
     */
    protected abstract void resolveDirtyState();

    /**
     * Binds this ListItem to {@code viewHolder} by applying data in ListItem to sub-views.
     * Assume {@link ViewHolder#cleanUp()} has already been invoked.
     */
    protected abstract void onBind(VH viewHolder);

    /**
     * Same as {@link #addViewBinder(ViewBinder, ViewBinder)} when {@code cleanUp} ViewBinder
     * is null.
     *
     * @param binder to interact with subviews in {@code ViewHolder}.
     *
     * @see #addViewBinder(ViewBinder, ViewBinder)
     */
    public final void addViewBinder(ViewBinder<VH> binder) {
        addViewBinder(binder, null);
    }

    /**
     * Adds {@link ViewBinder} to interact with sub-views in {@link ViewHolder}. These ViewBinders
     * will always be applied after {@link #onBind(ViewHolder)}.
     *
     * <p>To interact with a foobar sub-view in {@code ViewHolder}, make sure to first set its
     * visibility, or call setFoobar() setter method.
     *
     * <p>Example:
     * <pre>
     * {@code
     * TextListItem item = new TextListItem(context);
     * item.setTitle("title");
     * item.addViewBinder((viewHolder) -> {
     *     viewHolder.getTitle().doFoobar();
     * }, (viewHolder) -> {
     *     viewHolder.getTitle().revertFoobar();
     * });
     * }
     * </pre>
     *
     * @param binder to interact with subviews in {@code ViewHolder}.
     * @param cleanUp view binder to revert the effect of {@code binder}. cleanUp binders will be
     *                 stored in {@link ListItem.ViewHolder} and should be invoked via
     *                 {@link ViewHolder#cleanUp()} before {@code ViewHolder} is recycled.
     *                 This is to avoid changed made to ViewHolder lingers around when ViewHolder is
     *                 recycled. Pass in null to skip.
     */
    public final void addViewBinder(ViewBinder<VH> binder, @Nullable ViewBinder<VH> cleanUp) {
        mCustomBinders.add(binder);
        if (cleanUp != null) {
            mCustomBinderCleanUps.add(cleanUp);
        }
        markDirty();
    }

    /**
     * Removes the first occurrence of the specified item.
     *
     * @param binder to be removed.
     * @return {@code true} if {@code binder} exists. {@code false} otherwise.
     */
    public boolean removeViewBinder(ViewBinder<VH> binder) {
        return mCustomBinders.remove(binder);
    }

    /**
     * Functional interface to provide a way to interact with views in {@code ViewHolder}.
     * {@code ListItem} calls all added ViewBinders when it {@code bind}s to {@code ViewHolder}.
     *
     * @param <VH> class that extends {@link RecyclerView.ViewHolder}.
     */
    public interface ViewBinder<VH> {
        /**
         * Provides a way to interact with views in view holder.
         */
        void bind(VH viewHolder);
    }

    /**
     * ViewHolder that supports {@link ViewBinder}.
     */
    public abstract static class ViewHolder extends RecyclerView.ViewHolder implements
            OnUxRestrictionsChangedListener {
        private final List<ViewBinder> mCleanUps = new ArrayList<>();

        public ViewHolder(View itemView) {
            super(itemView);
        }

        /**
         * Removes customization from previous ListItem. Intended to be used when this ViewHolder is
         * bound to a ListItem.
         */
        @SuppressWarnings("unchecked")
        public final void cleanUp() {
            for (ViewBinder binder : mCleanUps) {
                binder.bind(this);
            }
        }

        /**
         * Stores clean up ViewBinders that will be called in {@code cleanUp()}.
         */
        public final void addCleanUp(@Nullable ViewBinder<ViewHolder> cleanUp) {
            if (cleanUp != null) {
                mCleanUps.add(cleanUp);
            }
        }
    }
}
