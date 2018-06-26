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

package androidx.navigation;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayDeque;

/**
 * A Navigator built specifically for {@link NavGraph} elements. Handles navigating to the
 * correct destination when the NavGraph is the target of navigation actions.
 */
@Navigator.Name("navigation")
public class NavGraphNavigator extends Navigator<NavGraph> {
    private Context mContext;
    private ArrayDeque<NavGraph> mBackStack = new ArrayDeque<>();

    /**
     * Construct a Navigator capable of routing incoming navigation requests to the proper
     * destination within a {@link NavGraph}.
     * @param context
     */
    public NavGraphNavigator(@NonNull Context context) {
        mContext = context;
    }

    /**
     * Creates a new {@link NavGraph} associated with this navigator.
     * @return
     */
    @NonNull
    @Override
    public NavGraph createDestination() {
        return new NavGraph(this);
    }

    @Override
    public void navigate(@NonNull NavGraph destination, @Nullable Bundle args,
            @Nullable NavOptions navOptions) {
        int startId = destination.getStartDestination();
        if (startId == 0) {
            throw new IllegalStateException("no start destination defined via"
                    + " app:startDestination for "
                    + (destination.getId() != 0
                            ? NavDestination.getDisplayName(mContext, destination.getId())
                            : "the root navigation"));
        }
        NavDestination startDestination = destination.findNode(startId, false);
        if (startDestination == null) {
            final String dest = NavDestination.getDisplayName(mContext, startId);
            throw new IllegalArgumentException("navigation destination " + dest
                    + " is not a direct child of this NavGraph");
        }
        if (navOptions != null && navOptions.shouldLaunchSingleTop()
                && isAlreadyTop(destination.getId())) {
            dispatchOnNavigatorNavigated(destination.getId(), BACK_STACK_UNCHANGED);
        } else {
            mBackStack.add(destination);
            dispatchOnNavigatorNavigated(destination.getId(), BACK_STACK_DESTINATION_ADDED);
        }
        startDestination.navigate(args, navOptions);
    }

    /**
     * This method to checks to see if navigating to the given destId would result in you
     * being right back where you started (we want to avoid creating a duplicate stack of the
     * same destinations).
     *
     * Because you can have a NavGraph as the start destination of another graph, we need to both
     * check the current NavGraph (i.e., no direct singleTop copies) and all of the parents that
     * start the current NavGraph via their start destinations.
     */
    private boolean isAlreadyTop(int destId) {
        if (mBackStack.isEmpty()) {
            return false;
        }
        NavGraph current = mBackStack.peekLast();
        while (current.getId() != destId) {
            NavGraph parent = current.getParent();
            if (parent == null || parent.getStartDestination() != current.getId()) {
                return false;
            }
            current = parent;
        }
        return true;
    }

    @Override
    public boolean popBackStack() {
        if (mBackStack.isEmpty()) {
            return false;
        }
        mBackStack.removeLast();
        int destId = mBackStack.isEmpty() ? 0 : mBackStack.peekLast().getId();
        dispatchOnNavigatorNavigated(destId, BACK_STACK_DESTINATION_POPPED);
        return true;
    }
}
