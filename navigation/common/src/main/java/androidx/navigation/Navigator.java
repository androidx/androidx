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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

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
    @SuppressWarnings("UnknownNullness") // TODO https://issuetracker.google.com/issues/112185120
    public @interface Name {
        String value();
    }

    private final CopyOnWriteArrayList<OnNavigatorBackPressListener> mOnBackPressListeners =
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
     * @param destination destination node to navigate to
     * @param args arguments to use for navigation
     * @param navOptions additional options for navigation
     * @param navigatorExtras extras unique to your Navigator.
     * @return The NavDestination that should be added to the back stack or null if
     * no change was made to the back stack (i.e., in cases of single top operations
     * where the destination is already on top of the back stack).
     */
    @Nullable
    public abstract NavDestination navigate(@NonNull D destination, @Nullable Bundle args,
            @Nullable NavOptions navOptions, @Nullable Extras navigatorExtras);

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
     * any calls to {@link #navigate(NavDestination, Bundle, NavOptions, Navigator.Extras)} or
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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected void onBackPressAdded() {
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected void onBackPressRemoved() {
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final void addOnNavigatorBackPressListener(
            @NonNull OnNavigatorBackPressListener listener) {
        boolean added = mOnBackPressListeners.add(listener);
        if (added && mOnBackPressListeners.size() == 1) {
            onBackPressAdded();
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final void removeOnNavigatorBackPressListener(
            @NonNull OnNavigatorBackPressListener listener) {
        boolean removed = mOnBackPressListeners.remove(listener);
        if (removed && mOnBackPressListeners.isEmpty()) {
            onBackPressRemoved();
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final void dispatchOnNavigatorBackPress() {
        for (OnNavigatorBackPressListener listener : mOnBackPressListeners) {
            listener.onPopBackStack(this);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface OnNavigatorBackPressListener {
        /**
         * This method is called after the Navigator navigates to a new destination.
         *
         * @param navigator
         */
        void onPopBackStack(@NonNull Navigator navigator);
    }

    /**
     * Interface indicating that this class should be passed to its respective
     * {@link Navigator} to enable Navigator specific behavior.
     */
    public interface Extras {
    }
}
