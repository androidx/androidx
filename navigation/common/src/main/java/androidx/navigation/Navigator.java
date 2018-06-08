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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Navigator defines a mechanism for navigating within an app.
 *
 * <p>Each Navigator sets the policy for a specific type of navigation, e.g.
 * {@link ActivityNavigator} knows how to launch into {@link NavDestination destinations}
 * backed by activities using {@link Context#startActivity(Intent) startActivity}.</p>
 *
 * <p>Navigators should be able to manage their own back stack when navigating between two
 * destinations that belong to that navigator. The {@link NavController} manages a back stack of
 * navigators representing the current navigation stack across all navigators.</p>
 *
 * <p>Each Navigator should add the {@link Name Navigator.Name annotation} to their class. Any
 * custom attributes used by the associated {@link NavDestination destination} subclass should
 * have a name corresponding with the name of the Navigator, e.g., {@link ActivityNavigator} uses
 * <code>&lt;declare-styleable name="ActivityNavigator"&gt;</code></p>
 *
 * @param <D> the subclass of {@link NavDestination} used with this Navigator which can be used
 *           to hold any special data that will be needed to navigate to that destination.
 *           Examples include information about an intent to navigate to other activities,
 *           or a fragment class name to instantiate and swap to a new fragment.
 */
public abstract class Navigator<D extends NavDestination> {
    /**
     * This annotation should be added to each Navigator subclass to denote the default name used
     * to register the Navigator with a {@link NavigatorProvider}.
     *
     * @see NavigatorProvider#addNavigator(Navigator)
     * @see NavigatorProvider#getNavigator(Class)
     */
    @Retention(RUNTIME)
    @Target({TYPE})
    public @interface Name {
        String value();
    }

    @Retention(SOURCE)
    @IntDef({BACK_STACK_UNCHANGED, BACK_STACK_DESTINATION_ADDED, BACK_STACK_DESTINATION_POPPED})
    @interface BackStackEffect {}

    /**
     * Indicator that the navigation event should not change the {@link NavController}'s back stack.
     *
     * <p>For example, a {@link NavOptions#shouldLaunchSingleTop() single top} navigation event may
     * not result in a back stack change if the existing destination is on the top of the stack.</p>
     *
     * @see #dispatchOnNavigatorNavigated
     */
    public static final int BACK_STACK_UNCHANGED = 0;

    /**
     * Indicator that the navigation event has added a new entry to the back stack. Only
     * destinations added with this flag will be handled by {@link NavController#navigateUp()}.
     *
     * @see #dispatchOnNavigatorNavigated
     */
    public static final int BACK_STACK_DESTINATION_ADDED = 1;

    /**
     * Indicator that the navigation event has popped an entry off the back stack.
     *
     * @see #dispatchOnNavigatorNavigated
     */
    public static final int BACK_STACK_DESTINATION_POPPED = 2;

    private final CopyOnWriteArrayList<OnNavigatorNavigatedListener> mOnNavigatedListeners =
            new CopyOnWriteArrayList<>();

    /**
     * Construct a new NavDestination associated with this Navigator.
     *
     * <p>Any initialization of the destination should be done in the destination's constructor as
     * it is not guaranteed that every destination will be created through this method.</p>
     * @return a new NavDestination
     */
    @NonNull
    public abstract D createDestination();

    /**
     * Navigate to a destination.
     *
     * <p>Requests navigation to a given destination associated with this navigator in
     * the navigation graph. This method generally should not be called directly;
     * {@link NavController} will delegate to it when appropriate.</p>
     *
     * <p>Implementations should {@link #dispatchOnNavigatorNavigated} to notify
     * listeners of the resulting navigation destination.</p>
     *
     * @param destination destination node to navigate to
     * @param args arguments to use for navigation
     * @param navOptions additional options for navigation
     */
    public abstract void navigate(@NonNull D destination, @Nullable Bundle args,
                                     @Nullable NavOptions navOptions);

    /**
     * Attempt to pop this navigator's back stack, performing the appropriate navigation.
     *
     * <p>Implementations should {@link #dispatchOnNavigatorNavigated} to notify
     * listeners of the resulting navigation destination and return {@code true} if navigation
     * was successful. Implementations should return {@code false} if navigation could not
     * be performed, for example if the navigator's back stack was empty.</p>
     *
     * @return {@code true} if pop was successful
     */
    public abstract boolean popBackStack();

    /**
     * Called to ask for a {@link Bundle} representing the Navigator's state. This will be
     * restored in {@link #onRestoreState(Bundle)}.
     */
    @Nullable
    public Bundle onSaveState() {
        return null;
    }

    /**
     * Restore any state previously saved in {@link #onSaveState()}. This will be called before
     * any calls to {@link #navigate(NavDestination, Bundle, NavOptions)} or
     * {@link #popBackStack()}.
     * <p>
     * Calls to {@link #createDestination()} should not be dependent on any state restored here as
     * {@link #createDestination()} can be called before the state is restored.
     *
     * @param savedState The state previously saved
     */
    public void onRestoreState(@NonNull Bundle savedState) {
    }

    /**
     * Add a listener to be notified when this navigator changes navigation destinations.
     *
     * <p>Most application code should use
     * {@link NavController#addOnNavigatedListener(NavController.OnNavigatedListener)} instead.
     * </p>
     *
     * @param listener listener to add
     */
    public final void addOnNavigatorNavigatedListener(
            @NonNull OnNavigatorNavigatedListener listener) {
        mOnNavigatedListeners.add(listener);
    }

    /**
     * Remove a listener so that it will no longer be notified when this navigator changes
     * navigation destinations.
     *
     * @param listener listener to remove
     */
    public final void removeOnNavigatorNavigatedListener(
            @NonNull OnNavigatorNavigatedListener listener) {
        mOnNavigatedListeners.remove(listener);
    }

    /**
     * Dispatch a navigated event to all registered {@link OnNavigatorNavigatedListener listeners}.
     * Utility for navigator implementations.
     *
     * @param destId id of the new destination
     * @param backStackEffect how the navigation event affects the back stack
     */
    public final void dispatchOnNavigatorNavigated(@IdRes int destId,
            @BackStackEffect int backStackEffect) {
        for (OnNavigatorNavigatedListener listener : mOnNavigatedListeners) {
            listener.onNavigatorNavigated(this, destId, backStackEffect);
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
         * @param backStackEffect
         */
        void onNavigatorNavigated(@NonNull Navigator navigator, @IdRes int destId,
                @BackStackEffect int backStackEffect);
    }
}
