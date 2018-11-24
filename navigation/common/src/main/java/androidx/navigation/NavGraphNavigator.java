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
    private static final String KEY_BACK_STACK_IDS = "androidx-nav-graph:navigator:backStackIds";

    private final NavigatorProvider mNavigatorProvider;
    private ArrayDeque<Integer> mBackStack = new ArrayDeque<>();

    /**
     * Construct a Navigator capable of routing incoming navigation requests to the proper
     * destination within a {@link NavGraph}.
     *
     * @param navigatorProvider NavigatorProvider used to retrieve the correct
     *                          {@link Navigator} to navigate to the start destination
     */
    public NavGraphNavigator(@NonNull NavigatorProvider navigatorProvider) {
        mNavigatorProvider = navigatorProvider;
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

    @Nullable
    @Override
    public NavDestination navigate(@NonNull NavGraph destination, @Nullable Bundle args,
            @Nullable NavOptions navOptions, @Nullable Extras navigatorExtras) {
        int startId = destination.getStartDestination();
        if (startId == 0) {
            throw new IllegalStateException("no start destination defined via"
                    + " app:startDestination for "
                    + destination.getDisplayName());
        }
        NavDestination startDestination = destination.findNode(startId, false);
        if (startDestination == null) {
            final String dest = destination.getStartDestDisplayName();
            throw new IllegalArgumentException("navigation destination " + dest
                    + " is not a direct child of this NavGraph");
        }
        if (navOptions == null || !(navOptions.shouldLaunchSingleTop()
                && isAlreadyTop(destination))) {
            mBackStack.add(destination.getId());
        }
        Navigator<NavDestination> navigator = mNavigatorProvider.getNavigator(
                startDestination.getNavigatorName());
        return navigator.navigate(startDestination, startDestination.addInDefaultArgs(args),
                navOptions, navigatorExtras);
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
    private boolean isAlreadyTop(NavGraph destination) {
        if (mBackStack.isEmpty()) {
            return false;
        }
        int topDestId = mBackStack.peekLast();
        NavGraph current = destination;
        while (current.getId() != topDestId) {
            NavDestination startDestination = current.findNode(current.getStartDestination());
            if (startDestination instanceof NavGraph) {
                current = (NavGraph) startDestination;
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean popBackStack() {
        return mBackStack.pollLast() != null;
    }

    @Override
    @Nullable
    public Bundle onSaveState() {
        Bundle b = new Bundle();
        int[] backStack = new int[mBackStack.size()];
        int index = 0;
        for (Integer id : mBackStack) {
            backStack[index++] = id;
        }
        b.putIntArray(KEY_BACK_STACK_IDS, backStack);
        return b;
    }

    @Override
    public void onRestoreState(@Nullable Bundle savedState) {
        if (savedState != null) {
            int[] backStack = savedState.getIntArray(KEY_BACK_STACK_IDS);
            if (backStack != null) {
                mBackStack.clear();
                for (int destId : backStack) {
                    mBackStack.add(destId);
                }
            }
        }
    }
}
