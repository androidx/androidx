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
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Navigator defines a mechanism for navigating within an app.
 *
 * <p>Each Navigator sets the policy for a specific type of navigation, e.g.
 * {@link ActivityNavigator} knows how to launch into {@link NavDestination destinations}
 * backed by activities using {@link Context#startActivity(Intent) startActivity} and
 * {@link FragmentNavigator} knows how to navigate by replacing fragments within a container.</p>
 *
 * <p>Navigators should be able to manage their own back stack when navigating between two
 * destinations that belong to that navigator. The {@link NavController} manages a back stack of
 * navigators representing the current navigation stack across all navigators.</p>
 *
 * @param <D> the subclass of {@link NavDestination} used with this Navigator which can be used
 *           to hold any special data that will be needed to navigate to that destination.
 *           Examples include information about an intent to navigate to other activities,
 *           or a fragment class name to instantiate and swap to a new fragment.
 */
public abstract class Navigator<D extends NavDestination> {
    private final CopyOnWriteArrayList<OnNavigatorNavigatedListener> mOnNavigatedListeners =
            new CopyOnWriteArrayList<>();

    /**
     * Construct a new NavDestination associated with this Navigator.
     *
     * <p>Any initialization of the destination should be done in the destination's constructor as
     * it is not guaranteed that every destination will be created through this method.</p>
     * @return a new NavDestination
     */
    public abstract D createDestination();

    /**
     * Navigate to a destination.
     *
     * <p>Requests navigation to a given destination associated with this navigator in
     * the navigation graph. This method generally should not be called directly;
     * {@link NavController} will delegate to it when appropriate.</p>
     *
     * @param destination destination node to navigate to
     * @param args arguments to use for navigation
     * @param navOptions additional options for navigation
     * @return true if navigation created a back stack entry that should be tracked
     */
    public abstract boolean navigate(D destination, Bundle args,
                                     NavOptions navOptions);

    /**
     * Attempt to pop this navigator's back stack, performing the appropriate navigation.
     *
     * <p>Implementations should {@link #dispatchOnNavigatorNavigated(int, boolean)} to notify
     * listeners of the resulting navigation destination and return {@link true} if navigation
     * was successful. Implementations should return {@code false} if navigation could not
     * be performed, for example if the navigator's back stack was empty.</p>
     *
     * @return {@code true} if pop was successful
     */
    public abstract boolean popBackStack();

    /**
     * Add a listener to be notified when this navigator changes navigation destinations.
     *
     * <p>Most application code should use
     * {@link NavController#addOnNavigatedListener(NavController.OnNavigatedListener)} instead.
     * </p>
     *
     * @param listener listener to add
     */
    public final void addOnNavigatorNavigatedListener(OnNavigatorNavigatedListener listener) {
        mOnNavigatedListeners.add(listener);
    }

    /**
     * Remove a listener so that it will no longer be notified when this navigator changes
     * navigation destinations.
     *
     * @param listener listener to remove
     */
    public final void removeOnNavigatorNavigatedListener(OnNavigatorNavigatedListener listener) {
        mOnNavigatedListeners.remove(listener);
    }

    /**
     * Dispatch a navigated event to all registered {@link OnNavigatorNavigatedListener listeners}.
     * Utility for navigator implementations.
     *
     * @param destId id of the new destination
     * @param isPopOperation true if this was the result of a pop operation
     */
    public final void dispatchOnNavigatorNavigated(@IdRes int destId, boolean isPopOperation) {
        for (OnNavigatorNavigatedListener listener : mOnNavigatedListeners) {
            listener.onNavigatorNavigated(this, destId, isPopOperation);
        }
    }

    /**
     * Listener for observing navigation events for this specific navigator. Most app code
     * should use {@link NavController.OnNavigatedListener} instead.
     */
    public interface OnNavigatorNavigatedListener {
        /**
         * This method is called after the Navigator navigates to a new destination.
         *
         * @param navigator
         * @param destId
         * @param isPopOperation
         */
        void onNavigatorNavigated(Navigator navigator, @IdRes int destId, boolean isPopOperation);
    }
}
