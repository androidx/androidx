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
package androidx.navigation.fragment

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.core.content.res.use
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorProvider
import androidx.navigation.fragment.FragmentNavigator.Destination

/**
 * Navigator that navigates through [fragment transactions][FragmentTransaction]. Every
 * destination using this Navigator must set a valid Fragment class name with
 * `android:name` or [Destination.setClassName].
 *
 * The current Fragment from FragmentNavigator's perspective can be retrieved by calling
 * [FragmentManager.getPrimaryNavigationFragment] with the FragmentManager
 * passed to this FragmentNavigator.
 *
 * Note that the default implementation does Fragment transactions
 * asynchronously, so the current Fragment will not be available immediately
 * (i.e., in callbacks to [NavController.OnDestinationChangedListener]).
 */
@Navigator.Name("fragment")
public open class FragmentNavigator(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val containerId: Int
) : Navigator<Destination>() {
    private val savedIds = mutableSetOf<String>()

    /**
     * {@inheritDoc}
     *
     * This method must call
     * [FragmentTransaction.setPrimaryNavigationFragment]
     * if the pop succeeded so that the newly visible Fragment can be retrieved with
     * [FragmentManager.getPrimaryNavigationFragment].
     *
     * Note that the default implementation pops the Fragment
     * asynchronously, so the newly visible Fragment from the back stack
     * is not instantly available after this call completes.
     */
    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        if (fragmentManager.isStateSaved) {
            Log.i(
                TAG, "Ignoring popBackStack() call: FragmentManager has already saved its state"
            )
            return
        }
        if (savedState) {
            val beforePopList = state.backStack.value
            val initialEntry = beforePopList.first()
            // Get the set of entries that are going to be popped
            val poppedList = beforePopList.subList(
                beforePopList.indexOf(popUpTo),
                beforePopList.size
            )
            // Now go through the list in reversed order (i.e., started from the most added)
            // and save the back stack state of each.
            for (entry in poppedList.reversed()) {
                if (entry == initialEntry) {
                    Log.i(
                        TAG,
                        "FragmentManager cannot save the state of the initial destination $entry"
                    )
                } else {
                    fragmentManager.saveBackStack(entry.id)
                    savedIds += entry.id
                }
            }
        } else {
            fragmentManager.popBackStack(
                popUpTo.id,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
        state.pop(popUpTo, savedState)
    }

    public override fun createDestination(): Destination {
        return Destination(this)
    }

    /**
     * Instantiates the Fragment via the FragmentManager's
     * [androidx.fragment.app.FragmentFactory].
     *
     * Note that this method is **not** responsible for calling
     * [Fragment.setArguments] on the returned Fragment instance.
     *
     * @param context Context providing the correct [ClassLoader]
     * @param fragmentManager FragmentManager the Fragment will be added to
     * @param className The Fragment to instantiate
     * @param args The Fragment's arguments, if any
     * @return A new fragment instance.
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated(
        """Set a custom {@link androidx.fragment.app.FragmentFactory} via
      {@link FragmentManager#setFragmentFactory(FragmentFactory)} to control
      instantiation of Fragments."""
    )
    public open fun instantiateFragment(
        context: Context,
        fragmentManager: FragmentManager,
        className: String,
        args: Bundle?
    ): Fragment {
        return fragmentManager.fragmentFactory.instantiate(context.classLoader, className)
    }

    /**
     * {@inheritDoc}
     *
     * This method should always call
     * [FragmentTransaction.setPrimaryNavigationFragment]
     * so that the Fragment associated with the new destination can be retrieved with
     * [FragmentManager.getPrimaryNavigationFragment].
     *
     * Note that the default implementation commits the new Fragment
     * asynchronously, so the new Fragment is not instantly available
     * after this call completes.
     */
    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        if (fragmentManager.isStateSaved) {
            Log.i(
                TAG, "Ignoring navigate() call: FragmentManager has already saved its state"
            )
            return
        }
        for (entry in entries) {
            navigate(entry, navOptions, navigatorExtras)
        }
    }

    private fun navigate(
        entry: NavBackStackEntry,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        val backStack = state.backStack.value
        val initialNavigation = backStack.isEmpty()
        val restoreState = (
            navOptions != null && !initialNavigation &&
                navOptions.shouldRestoreState() &&
                savedIds.remove(entry.id)
            )
        if (restoreState) {
            // Restore back stack does all the work to restore the entry
            fragmentManager.restoreBackStack(entry.id)
            state.push(entry)
            return
        }
        val destination = entry.destination as Destination
        val args = entry.arguments
        var className = destination.className
        if (className[0] == '.') {
            className = context.packageName + className
        }
        val frag = fragmentManager.fragmentFactory.instantiate(context.classLoader, className)
        frag.arguments = args
        val ft = fragmentManager.beginTransaction()
        var enterAnim = navOptions?.enterAnim ?: -1
        var exitAnim = navOptions?.exitAnim ?: -1
        var popEnterAnim = navOptions?.popEnterAnim ?: -1
        var popExitAnim = navOptions?.popExitAnim ?: -1
        if (enterAnim != -1 || exitAnim != -1 || popEnterAnim != -1 || popExitAnim != -1) {
            enterAnim = if (enterAnim != -1) enterAnim else 0
            exitAnim = if (exitAnim != -1) exitAnim else 0
            popEnterAnim = if (popEnterAnim != -1) popEnterAnim else 0
            popExitAnim = if (popExitAnim != -1) popExitAnim else 0
            ft.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
        }
        ft.replace(containerId, frag)
        ft.setPrimaryNavigationFragment(frag)
        @IdRes val destId = destination.id
        // TODO Build first class singleTop behavior for fragments
        val isSingleTopReplacement = (
            navOptions != null && !initialNavigation &&
                navOptions.shouldLaunchSingleTop() &&
                backStack.last().destination.id == destId
            )
        val isAdded = when {
            initialNavigation -> {
                true
            }
            isSingleTopReplacement -> {
                // Single Top means we only want one instance on the back stack
                if (backStack.size > 1) {
                    // If the Fragment to be replaced is on the FragmentManager's
                    // back stack, a simple replace() isn't enough so we
                    // remove it from the back stack and put our replacement
                    // on the back stack in its place
                    fragmentManager.popBackStack(
                        entry.id,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )
                    ft.addToBackStack(entry.id)
                }
                false
            }
            else -> {
                ft.addToBackStack(entry.id)
                true
            }
        }
        if (navigatorExtras is Extras) {
            for ((key, value) in navigatorExtras.sharedElements) {
                ft.addSharedElement(key, value)
            }
        }
        ft.setReorderingAllowed(true)
        ft.commit()
        // The commit succeeded, update our view of the world
        if (isAdded) {
            state.push(entry)
        }
    }

    public override fun onSaveState(): Bundle? {
        if (savedIds.isEmpty()) {
            return null
        }
        return bundleOf(KEY_SAVED_IDS to ArrayList(savedIds))
    }

    public override fun onRestoreState(savedState: Bundle) {
        val savedIds = savedState.getStringArrayList(KEY_SAVED_IDS)
        if (savedIds != null) {
            this.savedIds.clear()
            this.savedIds += savedIds
        }
    }

    /**
     * NavDestination specific to [FragmentNavigator]
     *
     * Construct a new fragment destination. This destination is not valid until you set the
     * Fragment via [setClassName].
     *
     * @param fragmentNavigator The [FragmentNavigator] which this destination will be associated
     * with. Generally retrieved via a [NavController]'s [NavigatorProvider.getNavigator] method.
     */
    @NavDestination.ClassType(Fragment::class)
    public open class Destination
    public constructor(fragmentNavigator: Navigator<out Destination>) :
        NavDestination(fragmentNavigator) {

        /**
         * Construct a new fragment destination. This destination is not valid until you set the
         * Fragment via [setClassName].
         *
         * @param navigatorProvider The [NavController] which this destination
         * will be associated with.
         */
        public constructor(navigatorProvider: NavigatorProvider) :
            this(navigatorProvider.getNavigator(FragmentNavigator::class.java))

        @CallSuper
        public override fun onInflate(context: Context, attrs: AttributeSet) {
            super.onInflate(context, attrs)
            context.resources.obtainAttributes(attrs, R.styleable.FragmentNavigator).use { array ->
                val className = array.getString(R.styleable.FragmentNavigator_android_name)
                if (className != null) setClassName(className)
            }
        }

        /**
         * Set the Fragment class name associated with this destination
         * @param className The class name of the Fragment to show when you navigate to this
         * destination
         * @return this [Destination]
         */
        public fun setClassName(className: String): Destination {
            _className = className
            return this
        }

        private var _className: String? = null
        /**
         * The Fragment's class name associated with this destination
         *
         * @throws IllegalStateException when no Fragment class was set.
         */
        public val className: String
            get() {
                checkNotNull(_className) { "Fragment class was not set" }
                return _className as String
            }

        public override fun toString(): String {
            val sb = StringBuilder()
            sb.append(super.toString())
            sb.append(" class=")
            if (_className == null) {
                sb.append("null")
            } else {
                sb.append(_className)
            }
            return sb.toString()
        }

        override fun equals(other: Any?): Boolean {
            if (other == null || other !is Destination) return false
            return super.equals(other) && _className == other._className
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + _className.hashCode()
            return result
        }
    }

    /**
     * Extras that can be passed to FragmentNavigator to enable Fragment specific behavior
     */
    public class Extras internal constructor(sharedElements: Map<View, String>) :
        Navigator.Extras {
        private val _sharedElements = LinkedHashMap<View, String>()

        /**
         * The map of shared elements associated with these Extras. The returned map
         * is an [unmodifiable][Map] copy of the underlying map and should be treated as immutable.
         */
        public val sharedElements: Map<View, String>
            get() = _sharedElements.toMap()

        /**
         * Builder for constructing new [Extras] instances. The resulting instances are
         * immutable.
         */
        public class Builder {
            private val _sharedElements = LinkedHashMap<View, String>()

            /**
             * Adds multiple shared elements for mapping Views in the current Fragment to
             * transitionNames in the Fragment being navigated to.
             *
             * @param sharedElements Shared element pairs to add
             * @return this [Builder]
             */
            public fun addSharedElements(sharedElements: Map<View, String>): Builder {
                for ((view, name) in sharedElements) {
                    addSharedElement(view, name)
                }
                return this
            }

            /**
             * Maps the given View in the current Fragment to the given transition name in the
             * Fragment being navigated to.
             *
             * @param sharedElement A View in the current Fragment to match with a View in the
             * Fragment being navigated to.
             * @param name The transitionName of the View in the Fragment being navigated to that
             * should be matched to the shared element.
             * @return this [Builder]
             * @see FragmentTransaction.addSharedElement
             */
            public fun addSharedElement(sharedElement: View, name: String): Builder {
                _sharedElements[sharedElement] = name
                return this
            }

            /**
             * Constructs the final [Extras] instance.
             *
             * @return An immutable [Extras] instance.
             */
            public fun build(): Extras {
                return Extras(_sharedElements)
            }
        }

        init {
            _sharedElements.putAll(sharedElements)
        }
    }

    private companion object {
        private const val TAG = "FragmentNavigator"
        private const val KEY_SAVED_IDS = "androidx-nav-fragment:navigator:savedIds"
    }
}
