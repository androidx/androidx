package androidx.car.widget;

import android.support.v7.widget.RecyclerView;

import java.util.function.Function;

/**
 * Definition of items that can be inserted into {@link ListItemAdapter}.
 *
 * @param <VH> ViewHolder.
 */
public abstract class ListItem<VH extends RecyclerView.ViewHolder> {

    // Whether the item should calculate view layout params. This usually happens when the item is
    // updated after bind() is called. Calling bind() resets to false.
    private boolean mDirty;

    // Tag for indicating whether to hide the divider.
    private boolean mHideDivider;

    /**
     * Classes that extends {@code ListItem} should register its view type in
     * {@link ListItemAdapter#registerListItemViewType(int, int, Function)}.
     *
     * @return type of this ListItem.
     */
    abstract int getViewType();

    /**
     * Called when ListItem is bound to its ViewHolder.
     */
    public abstract void bind(VH viewHolder);

    /**
     * Marks this item so that sub-views in ViewHolder will need layout params re-calculated
     * in next bind().
     *
     * This method should be called in each setter.
     */
    protected void markDirty() {
        mDirty = true;
    }

    /**
     * Marks this item as not dirty - no need to calculate sub-view layout params in bind().
     */
    protected void markClean() {
        mDirty = false;
    }

    /**
     * @return {@code true} if this item needs to calculate sub-view layout params.
     */
    protected boolean isDirty() {
        return mDirty;
    }

    /**
     * Whether hide the item divider coming after this {@code ListItem}.
     *
     * <p>Note: For this to work, one must invoke
     * {@code PagedListView.setDividerVisibilityManager(adapter} for {@link ListItemAdapter} and
     * have dividers enabled on {@link PagedListView}.
     */
    public void setHideDivider(boolean hideDivider) {
        mHideDivider = hideDivider;
        markDirty();
    }

    /**
     * @return {@code true} if the divider that comes after this ListItem should be hidden.
     * Defaults to false.
     */
    public boolean shouldHideDivider() {
        return mHideDivider;
    };

    /**
     * Functional interface to provide a way to interact with views in {@code ViewHolder}.
     * {@code ListItem} calls all added ViewBinders when it {@code bind}s to {@code ViewHolder}.
     *
     * @param <VH> extends {@link RecyclerView.ViewHolder}.
     */
    public interface ViewBinder<VH extends RecyclerView.ViewHolder> {
        /**
         * Provides a way to interact with views in view holder.
         */
        void bind(VH viewHolder);
    }
}
