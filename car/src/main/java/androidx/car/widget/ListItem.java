package androidx.car.widget;

import android.support.v7.widget.RecyclerView;

import java.util.function.Function;

/**
 * Definition of items that can be inserted into {@link ListItemAdapter}.
 *
 * @param <VH> ViewHolder.
 */
public abstract class ListItem<VH extends RecyclerView.ViewHolder> {

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
     * @return whether the divider that comes after this ListItem should be hidden. Defaults to
     *         false.
     */
    public boolean shouldHideDivider() {
        return false;
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
