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
package androidx.navigation

import android.os.Bundle
import androidx.navigation.Navigator.Name

/**
 * Navigator defines a mechanism for navigating within an app.
 *
 * Each Navigator sets the policy for a specific type of navigation, e.g.
 * [ActivityNavigator] knows how to launch into [destinations][NavDestination]
 * backed by activities using [startActivity][Context.startActivity].
 *
 * Navigators should be able to manage their own back stack when navigating between two
 * destinations that belong to that navigator. The [NavController] manages a back stack of
 * navigators representing the current navigation stack across all navigators.
 *
 * Each Navigator should add the [Navigator.Name annotation][Name] to their class. Any
 * custom attributes used by the associated [destination][NavDestination] subclass should
 * have a name corresponding with the name of the Navigator, e.g., [ActivityNavigator] uses
 * `<declare-styleable name="ActivityNavigator">`
 *
 * @param <D> the subclass of [NavDestination] used with this Navigator which can be used
 * to hold any special data that will be needed to navigate to that destination.
 * Examples include information about an intent to navigate to other activities,
 * or a fragment class name to instantiate and swap to a new fragment.
 */
public abstract class Navigator<D : NavDestination> {
    /**
     * This annotation should be added to each Navigator subclass to denote the default name used
     * to register the Navigator with a [NavigatorProvider].
     *
     * @see NavigatorProvider.addNavigator
     * @see NavigatorProvider.getNavigator
     */
    @kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
    public annotation class Name(val value: String)

    /**
     * Construct a new NavDestination associated with this Navigator.
     *
     * Any initialization of the destination should be done in the destination's constructor as
     * it is not guaranteed that every destination will be created through this method.
     * @return a new NavDestination
     */
    public abstract fun createDestination(): D

    /**
     * Navigate to a destination.
     *
     * Requests navigation to a given destination associated with this navigator in
     * the navigation graph. This method generally should not be called directly;
     * [NavController] will delegate to it when appropriate.
     *
     * @param destination destination node to navigate to
     * @param args arguments to use for navigation
     * @param navOptions additional options for navigation
     * @param navigatorExtras extras unique to your Navigator.
     * @return The NavDestination that should be added to the back stack or null if
     * no change was made to the back stack (i.e., in cases of single top operations
     * where the destination is already on top of the back stack).
     */
    public abstract fun navigate(
        destination: D,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ): NavDestination?

    /**
     * Attempt to pop this navigator's back stack, performing the appropriate navigation.
     *
     * Implementations should return `true` if navigation
     * was successful. Implementations should return `false` if navigation could not
     * be performed, for example if the navigator's back stack was empty.
     *
     * @return `true` if pop was successful
     */
    public abstract fun popBackStack(): Boolean

    /**
     * Called to ask for a [Bundle] representing the Navigator's state. This will be
     * restored in [.onRestoreState].
     */
    public open fun onSaveState(): Bundle? {
        return null
    }

    /**
     * Restore any state previously saved in [.onSaveState]. This will be called before
     * any calls to [.navigate] or
     * [.popBackStack].
     *
     * Calls to [.createDestination] should not be dependent on any state restored here as
     * [.createDestination] can be called before the state is restored.
     *
     * @param savedState The state previously saved
     */
    public open fun onRestoreState(savedState: Bundle) {}

    /**
     * Interface indicating that this class should be passed to its respective
     * [Navigator] to enable Navigator specific behavior.
     */
    public interface Extras
}
