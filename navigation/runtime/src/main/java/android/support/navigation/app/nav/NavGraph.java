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

package android.support.navigation.app.nav;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.SparseArray;

import com.android.support.navigation.R;

import java.util.Collection;
import java.util.Iterator;

/**
 * NavGraph is a collection of {@link NavDestination} nodes fetchable by ID.
 *
 * <p>A NavGraph serves as a 'virtual' destination: while the NavGraph itself will not appear
 * on the back stack, navigating to the NavGraph will cause the
 * {@link #getStartDestination starting destination} to be added to the back stack.</p>
 */
public class NavGraph extends NavDestination implements Iterable<NavDestination> {
    private final SparseArray<NavDestination> mNodes = new SparseArray<>();
    private int mStartDestId;

    /**
     * Construct a new NavGraph.
     *
     * @param navigatorProvider The {@link NavController} which this NavGraph
     *                          will be associated with.
     */
    public NavGraph(@NonNull NavigatorProvider navigatorProvider) {
        super(navigatorProvider.getNavigator(NavGraphNavigator.NAME));
    }

    NavGraph(@NonNull Navigator navigator) {
        super(navigator);
    }

    @Override
    public void onInflate(Context context, AttributeSet attrs) {
        super.onInflate(context, attrs);
        TypedArray a = context.getResources().obtainAttributes(attrs,
                R.styleable.NavGraph);
        setStartDestination(
                a.getResourceId(R.styleable.NavGraph_startDestination, 0));
        a.recycle();
    }

    /**
     * Adds a destination to this NavGraph. The destination must have an
     * {@link NavDestination#getId()} id} set.
     *
     * <p>The destination must not have a {@link NavDestination#getParent() parent} set. If
     * the destination is already part of a {@link NavGraph navigation graph}, call
     * {@link #remove(NavDestination)} before calling this method.</p>
     *
     * @param node destination to add
     */
    public void addDestination(@NonNull NavDestination node) {
        if (node.getId() == 0) {
            throw new IllegalArgumentException("Destinations must have an id."
                    + " Call setId() or include an android:id in your navigation XML.");
        }
        NavDestination existingDestination = mNodes.get(node.getId());
        if (existingDestination == node) {
            return;
        }
        if (node.getParent() != null) {
            throw new IllegalStateException("Destination already has a parent set."
                    + " Call NavGraph.remove() to remove the previous parent.");
        }
        if (existingDestination != null) {
            existingDestination.setParent(null);
        }
        node.setParent(this);
        mNodes.put(node.getId(), node);
    }

    /**
     * Adds multiple destinations to this NavGraph. Each destination must have an
     * {@link NavDestination#getId()} id} set.
     *
     * <p> Each destination must not have a {@link NavDestination#getParent() parent} set. If
     * any destination is already part of a {@link NavGraph navigation graph}, call
     * {@link #remove(NavDestination)} before calling this method.</p>
     *
     * @param nodes destinations to add
     */
    public void addDestinations(@NonNull Collection<NavDestination> nodes) {
        for (NavDestination node : nodes) {
            if (node == null) {
                continue;
            }
            addDestination(node);
        }
    }

    /**
     * Adds multiple destinations to this NavGraph. Each destination must have an
     * {@link NavDestination#getId()} id} set.
     *
     * <p> Each destination must not have a {@link NavDestination#getParent() parent} set. If
     * any destination is already part of a {@link NavGraph navigation graph}, call
     * {@link #remove(NavDestination)} before calling this method.</p>
     *
     * @param nodes destinations to add
     */
    public void addDestinations(@NonNull NavDestination... nodes) {
        for (NavDestination node : nodes) {
            if (node == null) {
                continue;
            }
            addDestination(node);
        }
    }

    /**
     * Finds a destination in the collection by ID. This will recursively check the
     * {@link #getParent() parent} of this navigation graph if node is not found in
     * this navigation graph.
     *
     * @param resid ID to locate
     * @return the node with ID resid
     */
    public NavDestination findNode(@IdRes int resid) {
        return findNode(resid, true);
    }

    NavDestination findNode(@IdRes int resid, boolean searchParents) {
        NavDestination destination = mNodes.get(resid);
        // Search the parent for the NavDestination if it is not a child of this navigation graph
        // and searchParents is true
        return destination != null
                ? destination
                : searchParents && getParent() != null ? getParent().findNode(resid) : null;
    }

    @Override
    public Iterator<NavDestination> iterator() {
        return new Iterator<NavDestination>() {
            private int mIndex = 0;

            @Override
            public boolean hasNext() {
                return mIndex + 1 < mNodes.size();
            }

            @Override
            public NavDestination next() {
                return mNodes.valueAt(mIndex++);
            }

            @Override
            public void remove() {
                mNodes.valueAt(mIndex).setParent(null);
                mNodes.removeAt(mIndex);
            }
        };
    }

    /**
     * Add all destinations from another collection to this one. As each destination has at most
     * one parent, the destinations will be removed from the given NavGraph.
     *
     * @param other collection of destinations to add. All destinations will be removed from this
     * graph after being added to this graph.
     */
    public void addAll(NavGraph other) {
        Iterator<NavDestination> iterator = other.iterator();
        while (iterator.hasNext()) {
            NavDestination destination = iterator.next();
            iterator.remove();
            addDestination(destination);
        }
    }

    /**
     * Remove a given destination from this NavGraph
     *
     * @param node the destination to remove.
     */
    public void remove(NavDestination node) {
        int index = mNodes.indexOfKey(node.getId());
        if (index >= 0) {
            mNodes.valueAt(index).setParent(null);
            mNodes.removeAt(index);
        }
    }

    /**
     * Clear all destinations from this navigation graph.
     */
    public void clear() {
        Iterator<NavDestination> iterator = iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    /**
     * Returns the starting destination for this NavGraph. When navigating to the NavGraph, this
     * destination is the one the user will initially see.
     * @return
     */
    @IdRes
    public int getStartDestination() {
        return mStartDestId;
    }

    /**
     * Sets the starting destination for this NavGraph.
     *
     * @param startDestId The id of the destination to be shown when navigating to this NavGraph.
     */
    public void setStartDestination(@IdRes int startDestId) {
        mStartDestId = startDestId;
    }
}
