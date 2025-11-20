/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.transition;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.core.view.ViewCompat;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
class GhostViewHolder extends FrameLayout {

    private @NonNull ViewGroup mParent;
    private boolean mAttached;

    GhostViewHolder(ViewGroup parent) {
        super(parent.getContext());
        setClipChildren(false);
        mParent = parent;
        mParent.setTag(R.id.ghost_view_holder, this);
        ViewCompat.addOverlayView(mParent, this);
        mAttached = true;
    }

    @Override
    public void onViewAdded(View child) {
        if (!mAttached) {
            throw new IllegalStateException("This GhostViewHolder is detached!");
        }
        super.onViewAdded(child);
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        // before 4.3 onViewRemoved called before removing the child from the children list
        // after 4.3 onViewRemoved called after removing the child from the children
        // let's be on a safe side and check smartly
        if ((getChildCount() == 1 && getChildAt(0) == child)
                || getChildCount() == 0) {
            mParent.setTag(R.id.ghost_view_holder, null);
            mParent.getOverlay().remove(this);
            mAttached = false;
        }
    }

    static GhostViewHolder getHolder(@NonNull ViewGroup parent) {
        return (GhostViewHolder) parent.getTag(R.id.ghost_view_holder);
    }

    void popToOverlayTop() {
        if (!mAttached) {
            throw new IllegalStateException("This GhostViewHolder is detached!");
        }
        // we can't reuse the overlay object as this method can return another object after
        // calling remove as it was cleaned up because of no overlay view (in backport impl.)
        mParent.getOverlay().remove(this);
        mParent.getOverlay().add(this);
    }


    /**
     * Inserts a GhostView into the overlay's ViewGroup in the order in which they
     * should be displayed by the UI.
     */
    void addGhostView(GhostViewPort ghostView) {
        ArrayList<View> viewParents = new ArrayList<>();
        getParents(ghostView.mView, viewParents);

        int index = getInsertIndex(viewParents);
        if (index < 0 || index >= getChildCount()) {
            addView(ghostView);
        } else {
            addView(ghostView, index);
        }
    }

    /**
     * Find the index into the overlay to insert the GhostView based on the order that the
     * views should be drawn. This keeps GhostViews layered in the same order
     * that they are ordered in the UI.
     */
    private int getInsertIndex(ArrayList<View> viewParents) {
        ArrayList<View> tempParents = new ArrayList<>();
        int low = 0;
        int high = getChildCount() - 1;

        while (low <= high) {
            int mid = (low + high) / 2;
            GhostViewPort midView = (GhostViewPort) getChildAt(mid);
            getParents(midView.mView, tempParents);
            if (isOnTop(viewParents, tempParents)) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
            tempParents.clear();
        }

        return low;
    }


    /**
     * Returns true if viewParents is from a View that is on top of the comparedWith's view.
     * The ArrayLists contain the ancestors of views in order from top most grandparent, to
     * the view itself, in order. The goal is to find the first matching parent and then
     * compare the draw order of the siblings.
     */
    private static boolean isOnTop(ArrayList<View> viewParents, ArrayList<View> comparedWith) {
        if (viewParents.isEmpty() || comparedWith.isEmpty()
                || viewParents.get(0) != comparedWith.get(0)) {
            // Not the same decorView -- arbitrary ordering
            return true;
        }
        int depth = Math.min(viewParents.size(), comparedWith.size());
        for (int i = 1; i < depth; i++) {
            View viewParent = viewParents.get(i);
            View comparedWithParent = comparedWith.get(i);

            if (viewParent != comparedWithParent) {
                // i - 1 is the same parent, but these are different children.
                return isOnTop(viewParent, comparedWithParent);
            }
        }

        // one of these is the parent of the other
        return (comparedWith.size() == depth);
    }

    /**
     * Adds all the parents, grandparents, etc. of view to parents.
     */
    private static void getParents(View view, ArrayList<View> parents) {
        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            getParents((View) parent, parents);
        }
        parents.add(view);
    }

    /**
     * Returns true if view would be drawn on top of comparedWith or false otherwise.
     * view and comparedWith are siblings with the same parent. This uses the logic
     * that dispatchDraw uses to determine which View should be drawn first.
     */
    private static boolean isOnTop(View view, View comparedWith) {
        ViewGroup parent = (ViewGroup) view.getParent();

        final int childrenCount = parent.getChildCount();

        // From the implementation of ViewGroup.buildOrderedChildList() used by dispatchDraw:
        // The drawing order list is sorted by Z first.
        if (view.getZ() != comparedWith.getZ()) {
            return view.getZ() > comparedWith.getZ();
        }

        // This default value shouldn't be used because both view and comparedWith
        // should be in the list. If there is an error, then just return an arbitrary
        // view is on top.
        boolean isOnTop = true;
        for (int i = 0; i < childrenCount; i++) {
            int childIndex = ViewGroupUtils.getChildDrawingOrder(parent, i);
            final View child = parent.getChildAt(childIndex);
            if (child == view) {
                isOnTop = false;
                break;
            } else if (child == comparedWith) {
                isOnTop = true;
                break;
            }
        }

        return isOnTop;
    }
}
