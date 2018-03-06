/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v7.preference;

import android.support.annotation.IdRes;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

/**
 * A {@link android.support.v7.widget.RecyclerView.ViewHolder} class which caches views associated
 * with the default {@link Preference} layouts. Cached views can be retrieved by calling
 * {@link #findViewById(int)}.
 */
public class PreferenceViewHolder extends RecyclerView.ViewHolder {
    private final SparseArray<View> mCachedViews = new SparseArray<>(4);
    private boolean mDividerAllowedAbove;
    private boolean mDividerAllowedBelow;

    /* package */ PreferenceViewHolder(View itemView) {
        super(itemView);

        // Pre-cache the views that we know in advance we'll want to find
        mCachedViews.put(android.R.id.title, itemView.findViewById(android.R.id.title));
        mCachedViews.put(android.R.id.summary, itemView.findViewById(android.R.id.summary));
        mCachedViews.put(android.R.id.icon, itemView.findViewById(android.R.id.icon));
        mCachedViews.put(R.id.icon_frame, itemView.findViewById(R.id.icon_frame));
        mCachedViews.put(AndroidResources.ANDROID_R_ICON_FRAME,
                itemView.findViewById(AndroidResources.ANDROID_R_ICON_FRAME));
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.TESTS)
    public static PreferenceViewHolder createInstanceForTests(View itemView) {
        return new PreferenceViewHolder(itemView);
    }

    /**
     * Returns a cached reference to a subview managed by this object. If the view reference is not
     * yet cached, it falls back to calling {@link View#findViewById(int)} and caches the result.
     *
     * @param id Resource ID of the view to find
     * @return The view, or null if no view with the requested ID is found.
     */
    public View findViewById(@IdRes int id) {
        final View cachedView = mCachedViews.get(id);
        if (cachedView != null) {
            return cachedView;
        } else {
            final View v = itemView.findViewById(id);
            if (v != null) {
                mCachedViews.put(id, v);
            }
            return v;
        }
    }

    /**
     * Dividers are only drawn between items if both items allow it, or above the first and below
     * the last item if that item allows it.
     *
     * @return true if dividers are allowed above this item
     */
    public boolean isDividerAllowedAbove() {
        return mDividerAllowedAbove;
    }

    /**
     * Dividers are only drawn between items if both items allow it, or above the first and below
     * the last item if that item allows it.
     *
     * By default, {@link Preference#onBindViewHolder(PreferenceViewHolder)} will set this to the
     * same value as returned by {@link Preference#isSelectable()}, so that non-selectable items
     * do not have a divider drawn above them.
     *
     * @param allowed false to prevent dividers being drawn above this item
     */
    public void setDividerAllowedAbove(boolean allowed) {
        mDividerAllowedAbove = allowed;
    }

    /**
     * Dividers are only drawn between items if both items allow it, or above the first and below
     * the last item if that item allows it.
     *
     * @return true if dividers are allowed below this item
     */
    public boolean isDividerAllowedBelow() {
        return mDividerAllowedBelow;
    }

    /**
     * Dividers are only drawn between items if both items allow it, or above the first and below
     * the last item if that item allows it.
     *
     * By default, {@link Preference#onBindViewHolder(PreferenceViewHolder)} will set this to the
     * same value as returned by {@link Preference#isSelectable()}, so that non-selectable items
     * do not have a divider drawn below them.
     *
     * @param allowed false to prevent dividers being drawn below this item
     */
    public void setDividerAllowedBelow(boolean allowed) {
        mDividerAllowedBelow = allowed;
    }
}
