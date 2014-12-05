/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.view.View;
import android.view.ViewGroup;

/**
 * A Presenter is used to generate {@link View}s and bind Objects to them on
 * demand. It is closely related to concept of an {@link
 * android.support.v7.widget.RecyclerView.Adapter RecyclerView.Adapter}, but is
 * not position-based.
 *
 * <p>
 * A trivial Presenter that takes a string and renders it into a {@link
 * android.widget.TextView TextView}:
 *
 * <pre class="prettyprint">
 * public class StringTextViewPresenter extends Presenter {
 *     // This class does not need a custom ViewHolder, since it does not use
 *     // a complex layout.
 *
 *     {@literal @}Override
 *     public ViewHolder onCreateViewHolder(ViewGroup parent) {
 *         return new ViewHolder(new TextView(parent.getContext()));
 *     }
 *
 *     {@literal @}Override
 *     public void onBindViewHolder(ViewHolder viewHolder, Object item) {
 *         String str = (String) item;
 *         TextView textView = (TextView) viewHolder.mView;
 *
 *         textView.setText(item);
 *     }
 *
 *     {@literal @}Override
 *     public void onUnbindViewHolder(ViewHolder viewHolder) {
 *         // Nothing to unbind for TextView, but if this viewHolder had
 *         // allocated bitmaps, they can be released here.
 *     }
 * }
 * </pre>
 */
public abstract class Presenter {
    /**
     * ViewHolder can be subclassed and used to cache any view accessors needed
     * to improve binding performance (for example, results of findViewById)
     * without needing to subclass a View.
     */
    public static class ViewHolder {
        public final View view;

        public ViewHolder(View view) {
            this.view = view;
        }
    }

    /**
     * Creates a new {@link View}.
     */
    public abstract ViewHolder onCreateViewHolder(ViewGroup parent);

    /**
     * Binds a {@link View} to an item.
     */
    public abstract void onBindViewHolder(ViewHolder viewHolder, Object item);

    /**
     * Unbinds a {@link View} from an item. Any expensive references may be
     * released here, and any fields that are not bound for every item should be
     * cleared here.
     */
    public abstract void onUnbindViewHolder(ViewHolder viewHolder);

    /**
     * Called when a view created by this presenter has been attached to a window.
     *
     * <p>This can be used as a reasonable signal that the view is about to be seen
     * by the user. If the adapter previously freed any resources in
     * {@link #onViewDetachedFromWindow(ViewHolder)}
     * those resources should be restored here.</p>
     *
     * @param holder Holder of the view being attached
     */
    public void onViewAttachedToWindow(ViewHolder holder) {
    }

    /**
     * Called when a view created by this presenter has been detached from its window.
     *
     * <p>Becoming detached from the window is not necessarily a permanent condition;
     * the consumer of an presenter's views may choose to cache views offscreen while they
     * are not visible, attaching and detaching them as appropriate.</p>
     *
     * Any view property animations should be cancelled here or the view may fail
     * to be recycled.
     *
     * @param holder Holder of the view being detached
     */
    public void onViewDetachedFromWindow(ViewHolder holder) {
        // If there are view property animations running then RecyclerView won't recycle.
        cancelAnimationsRecursive(holder.view);
    }

    /**
     * Utility method for removing all running animations on a view.
     */
    protected static void cancelAnimationsRecursive(View view) {
        if (view != null && view.hasTransientState()) {
            view.animate().cancel();
            if (view instanceof ViewGroup) {
                final int count = ((ViewGroup) view).getChildCount();
                for (int i = 0; view.hasTransientState() && i < count; i++) {
                    cancelAnimationsRecursive(((ViewGroup) view).getChildAt(i));
                }
            }
        }
    }

    /**
     * Called to set a click listener for the given view holder.
     *
     * The default implementation sets the click listener on the root view in the view holder.
     * If the root view isn't focusable this method should be overridden to set the listener
     * on the appropriate focusable child view(s).
     *
     * @param holder The view holder containing the view(s) on which the listener should be set.
     * @param listener The click listener to be set.
     */
    public void setOnClickListener(ViewHolder holder, View.OnClickListener listener) {
        holder.view.setOnClickListener(listener);
    }
}
