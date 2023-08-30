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
package androidx.navigation.fragment

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.CallSuper
import androidx.core.content.res.use
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorProvider
import androidx.navigation.NavigatorState
import androidx.navigation.fragment.DialogFragmentNavigator.Destination
import kotlinx.coroutines.flow.StateFlow

/**
 * Navigator that uses [DialogFragment.show]. Every
 * destination using this Navigator must set a valid DialogFragment class name with
 * `android:name` or [Destination.setClassName].
 */
@Navigator.Name("dialog")
public class DialogFragmentNavigator(
    private val context: Context,
    private val fragmentManager: FragmentManager
) : Navigator<Destination>() {
    private val restoredTagsAwaitingAttach = mutableSetOf<String>()
    private val observer = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    val dialogFragment = source as DialogFragment
                    val dialogOnBackStack = state.backStack.value.any {
                        it.id == dialogFragment.tag
                    }
                    if (!dialogOnBackStack) {
                        // If the Fragment is no longer on the back stack, it must have been
                        // been popped before it was actually attached to the FragmentManager
                        // (i.e., popped in the same frame as the navigate() call that added it).
                        // For that case, we need to dismiss the dialog to ensure the states stay
                        // in sync
                        dialogFragment.dismiss()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    val dialogFragment = source as DialogFragment
                    val entry = state.transitionsInProgress.value.lastOrNull { entry ->
                        entry.id == dialogFragment.tag
                    }
                    entry?.let { state.markTransitionComplete(it) }
                }
                Lifecycle.Event.ON_STOP -> {
                    val dialogFragment = source as DialogFragment
                    if (!dialogFragment.requireDialog().isShowing) {
                        val beforePopList = state.backStack.value
                        val poppedEntry = beforePopList.lastOrNull {
                            it.id == dialogFragment.tag
                        }
                        if (beforePopList.lastOrNull() != poppedEntry) {
                            Log.i(
                                TAG,
                                "Dialog $dialogFragment was dismissed while it was not the " +
                                    "top of the back stack, popping all dialogs above this " +
                                    "dismissed dialog"
                            )
                        }
                        poppedEntry?.let { state.popWithTransition(it, false) }
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    val dialogFragment = source as DialogFragment
                    val entry = state.transitionsInProgress.value.lastOrNull { entry ->
                        entry.id == dialogFragment.tag
                    }
                    entry?.let { state.markTransitionComplete(it) }
                    dialogFragment.lifecycle.removeObserver(this)
                }
                else -> { /* added to exhaust when */ }
            }
        }
    }

    /**
     * Gets the backstack of [NavBackStackEntry] associated with this Navigator
     */
    internal val backStack: StateFlow<List<NavBackStackEntry>>
        get() = state.backStack

    /**
     * Stores DialogFragments that have been created but pendingTransaction.
     */
    private val transitioningFragments: MutableMap<String, DialogFragment> = mutableMapOf()

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        if (fragmentManager.isStateSaved) {
            Log.i(
                TAG, "Ignoring popBackStack() call: FragmentManager has already saved its state"
            )
            return
        }
        val beforePopList = state.backStack.value
        // Get the set of entries that are going to be popped
        val popUpToIndex = beforePopList.indexOf(popUpTo)
        val poppedList = beforePopList.subList(
            popUpToIndex,
            beforePopList.size
        )
        // track transitioning state of incoming entry
        val incomingEntry = beforePopList.elementAtOrNull(popUpToIndex - 1)
        val incomingEntryTransitioning = state.transitionsInProgress.value.contains(incomingEntry)

        // Now go through the list in reversed order (i.e., starting from the most recently added)
        // and dismiss each dialog
        for (entry in poppedList.reversed()) {
            val existingFragment = fragmentManager.findFragmentByTag(entry.id)
            if (existingFragment != null) {
                (existingFragment as DialogFragment).dismiss()
            }
        }
        state.popWithTransition(popUpTo, savedState)
        // if incoming entry was marked as transitioning by popWithTransition, mark it as complete
        if (incomingEntry != null && !incomingEntryTransitioning) {
            state.markTransitionComplete(incomingEntry)
        }
    }

    public override fun createDestination(): Destination {
        return Destination(this)
    }

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        if (fragmentManager.isStateSaved) {
            Log.i(TAG, "Ignoring navigate() call: FragmentManager has already saved its state")
            return
        }
        for (entry in entries) {
            navigate(entry)
        }
    }

    private fun navigate(
        entry: NavBackStackEntry
    ) {
        val dialogFragment = createDialogFragment(entry)
        dialogFragment.show(fragmentManager, entry.id)
        val outGoingEntry = state.backStack.value.lastOrNull()
        val outGoingEntryTransitioning = state.transitionsInProgress.value.contains(outGoingEntry)
        state.pushWithTransition(entry)
        // if outgoing entry was put in Transition by push, mark complete here
        if (outGoingEntry != null && !outGoingEntryTransitioning) {
            state.markTransitionComplete(outGoingEntry)
        }
    }

    override fun onLaunchSingleTop(backStackEntry: NavBackStackEntry) {
        if (fragmentManager.isStateSaved) {
            Log.i(
                TAG,
                "Ignoring onLaunchSingleTop() call: FragmentManager has already saved its state"
            )
            return
        }

        // Ensure previous fragment is dismissed. If it is in transition, we have to dismiss it
        // here before its value with same key (entry.id) gets replaced by new fragment.
        val oldFragment = transitioningFragments[backStackEntry.id]
            ?: fragmentManager.findFragmentByTag(backStackEntry.id) as? DialogFragment
        if (oldFragment != null) {
            oldFragment.lifecycle.removeObserver(observer)
            oldFragment.dismiss()
        }

        val newFragment = createDialogFragment(backStackEntry)
        newFragment.show(fragmentManager, backStackEntry.id)
        state.onLaunchSingleTopWithTransition(backStackEntry)
    }

    private fun createDialogFragment(entry: NavBackStackEntry): DialogFragment {
        val destination = entry.destination as Destination
        var className = destination.className
        if (className[0] == '.') {
            className = context.packageName + className
        }
        val frag = fragmentManager.fragmentFactory.instantiate(
            context.classLoader, className
        )
        require(DialogFragment::class.java.isAssignableFrom(frag.javaClass)) {
            "Dialog destination ${destination.className} is not an instance of DialogFragment"
        }
        val dialogFragment = frag as DialogFragment
        dialogFragment.arguments = entry.arguments
        dialogFragment.lifecycle.addObserver(observer)
        // For singleTop navigations, this will overwrite existing transitioning fragments with
        // the same `entry.id`. This is fine because before the singleTop DialogFragment
        // is recreated and replaces the old record inside transitioningFragments, we would
        // have already dismissed the existing (old) fragment.
        transitioningFragments[entry.id] = dialogFragment
        return dialogFragment
    }

    override fun onAttach(state: NavigatorState) {
        super.onAttach(state)
        for (entry in state.backStack.value) {
            val fragment = fragmentManager
                .findFragmentByTag(entry.id) as DialogFragment?
            fragment?.lifecycle?.addObserver(observer)
                ?: restoredTagsAwaitingAttach.add(entry.id)
        }
        fragmentManager.addFragmentOnAttachListener { _, childFragment ->
            val needToAddObserver = restoredTagsAwaitingAttach.remove(childFragment.tag)
            if (needToAddObserver) {
                childFragment.lifecycle.addObserver(observer)
            }
            transitioningFragments.remove(childFragment.tag)
        }
    }

    /**
     * NavDestination specific to [DialogFragmentNavigator].
     *
     * Construct a new fragment destination. This destination is not valid until you set the
     * Fragment via [setClassName].
     *
     * @param fragmentNavigator The [DialogFragmentNavigator] which this destination will be
     *                          associated with. Generally retrieved via a [NavController]'s
     *                          [NavigatorProvider.getNavigator] method.
     */
    @NavDestination.ClassType(DialogFragment::class)
    public open class Destination
    public constructor(fragmentNavigator: Navigator<out Destination>) :
        NavDestination(fragmentNavigator), FloatingWindow {
        private var _className: String? = null
        /**
         * The DialogFragment's class name associated with this destination
         *
         * @throws IllegalStateException when no DialogFragment class was set.
         */
        public val className: String
            get() {
                checkNotNull(_className) { "DialogFragment class was not set" }
                return _className as String
            }

        /**
         * Construct a new fragment destination. This destination is not valid until you set the
         * Fragment via [setClassName].
         *
         * @param navigatorProvider The [NavController] which this destination
         * will be associated with.
         */
        public constructor(navigatorProvider: NavigatorProvider) : this(
            navigatorProvider.getNavigator(DialogFragmentNavigator::class.java)
        )

        @CallSuper
        public override fun onInflate(context: Context, attrs: AttributeSet) {
            super.onInflate(context, attrs)
            context.resources.obtainAttributes(
                attrs,
                R.styleable.DialogFragmentNavigator
            ).use { array ->
                val className = array.getString(R.styleable.DialogFragmentNavigator_android_name)
                className?.let { setClassName(it) }
            }
        }

        /**
         * Set the DialogFragment class name associated with this destination
         * @param className The class name of the DialogFragment to show when you navigate to this
         *                  destination
         * @return this [Destination]
         */
        public fun setClassName(className: String): Destination {
            _className = className
            return this
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

    private companion object {
        private const val TAG = "DialogFragmentNavigator"
    }
}
