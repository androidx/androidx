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

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.util.SparseArray;

/**
 * NavGraph is a collection of {@link NavDestination} nodes fetchable by ID.
 */
public class NavGraph extends NavDestination {
    private final SparseArray<NavDestination> mNodes = new SparseArray<>();

    /**
     * NavGraphs should be created via {@link NavController#createGraph}
     */
    public NavGraph(@NonNull Navigator navigator) {
        super(navigator);
    }

    /**
     * Adds a destination to the collection.
     *
     * @param node destination to add
     */
    public void addDestination(NavDestination node) {
        if (node.getId() == 0) {
            throw new IllegalArgumentException("Destinations must have an id."
                    + " Call setId() or include an android:id in your navigation XML.");
        }
        mNodes.put(node.getId(), node);
    }

    /**
     * Finds a destination in the collection by ID.
     *
     * @param resid ID to locate
     * @return the node with ID resid
     */
    public NavDestination findNode(@IdRes int resid) {
        return mNodes.get(resid);
    }

    /**
     * Add all destinations from another collection to this one.
     *
     * @param other collection of destinations to add
     */
    public void addAll(NavGraph other) {
        for (int i = 0, size = other.mNodes.size(); i < size; i++) {
            mNodes.put(other.mNodes.keyAt(i), other.mNodes.valueAt(i));
        }
    }
}
