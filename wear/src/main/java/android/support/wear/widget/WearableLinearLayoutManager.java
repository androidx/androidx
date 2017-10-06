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
package android.support.wear.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * This wear-specific implementation of {@link LinearLayoutManager} provides basic
 * offsetting logic for updating child layout. For round devices it offsets the children
 * horizontally to make them appear to travel around a circle. For square devices it aligns them in
 * a straight list. This functionality is provided by the {@link CurvingLayoutCallback} which is
 * set when constructing the this class with its default constructor
 * {@link #WearableLinearLayoutManager(Context)}.
 */
public class WearableLinearLayoutManager extends LinearLayoutManager {

    @Nullable
    private LayoutCallback mLayoutCallback;

    /**
     * Callback for interacting with layout passes.
     */
    public abstract static class LayoutCallback {
        /**
         * Override this method to implement custom child layout behavior on scroll. It is called
         * at the end of each layout pass of the view (including scrolling) and enables you to
         * modify any property of the child view. Examples include scaling the children based on
         * their distance from the center of the parent, or changing the translation of the children
         * to create an illusion of the path they are moving along.
         *
         * @param child  the current child to be affected.
         * @param parent the {@link RecyclerView} parent that this class is attached to.
         */
        public abstract void onLayoutFinished(View child, RecyclerView parent);
    }

    /**
     * Creates a {@link WearableLinearLayoutManager} for a vertical list.
     *
     * @param context Current context, will be used to access resources.
     * @param layoutCallback Callback to be associated with this {@link WearableLinearLayoutManager}
     */
    public WearableLinearLayoutManager(Context context, LayoutCallback layoutCallback) {
        super(context, VERTICAL, false);
        mLayoutCallback = layoutCallback;
    }

    /**
     * Creates a {@link WearableLinearLayoutManager} for a vertical list.
     *
     * @param context Current context, will be used to access resources.
     */
    public WearableLinearLayoutManager(Context context) {
        this(context, new CurvingLayoutCallback(context));
    }

    /**
     * Set a particular instance of the layout callback for this
     * {@link WearableLinearLayoutManager}. The callback will be called on the Ui thread.
     *
     * @param layoutCallback
     */
    public void setLayoutCallback(@Nullable LayoutCallback layoutCallback) {
        mLayoutCallback = layoutCallback;
    }

    /**
     * @return the current {@link LayoutCallback} associated with this
     * {@link WearableLinearLayoutManager}.
     */
    @Nullable
    public LayoutCallback getLayoutCallback() {
        return mLayoutCallback;
    }

    @Override
    public int scrollVerticallyBy(
            int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int scrolled = super.scrollVerticallyBy(dy, recycler, state);

        updateLayout();
        return scrolled;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        if (getChildCount() == 0) {
            return;
        }

        updateLayout();
    }

    private void updateLayout() {
        if (mLayoutCallback == null) {
            return;
        }
        final int childCount = getChildCount();
        for (int count = 0; count < childCount; count++) {
            View child = getChildAt(count);
            mLayoutCallback.onLayoutFinished(child, (WearableRecyclerView) child.getParent());
        }
    }
}
