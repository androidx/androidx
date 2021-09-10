/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.fragment.app.testing

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.RestrictTo
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.commitNow
import androidx.fragment.app.testing.FragmentScenario.Companion.launch
import androidx.fragment.testing.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import java.io.Closeable

@Deprecated(
    "Superseded by launchFragment that takes an initialState",
    level = DeprecationLevel.HIDDEN
) // Binary API compatibility.
public inline fun <reified F : Fragment> launchFragment(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
    factory: FragmentFactory? = null
): FragmentScenario<F> = launchFragment(
    fragmentArgs, themeResId, Lifecycle.State.RESUMED,
    factory
)

@Deprecated(
    "Superseded by launchFragment that takes an initialState",
    level = DeprecationLevel.HIDDEN
) // Binary API compatibility.
public inline fun <reified F : Fragment> launchFragment(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
    crossinline instantiate: () -> F
): FragmentScenario<F> = launchFragment(fragmentArgs, themeResId) {
    instantiate()
}

@Deprecated(
    "Superseded by launchFragmentInContainer that takes an initialState",
    level = DeprecationLevel.HIDDEN
) // Binary API compatibility.
public inline fun <reified F : Fragment> launchFragmentInContainer(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
    factory: FragmentFactory? = null
): FragmentScenario<F> = launchFragmentInContainer(
    fragmentArgs, themeResId, Lifecycle.State.RESUMED,
    factory
)

@Deprecated(
    "Superseded by launchFragmentInContainer that takes an initialState",
    level = DeprecationLevel.HIDDEN
) // Binary API compatibility.
public inline fun <reified F : Fragment> launchFragmentInContainer(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
    crossinline instantiate: () -> F
): FragmentScenario<F> = launchFragmentInContainer(fragmentArgs, themeResId) {
    instantiate()
}

/**
 * Launches a Fragment with given arguments hosted by an empty [FragmentActivity] using
 * given [FragmentFactory] and waits for it to reach [initialState].
 *
 * This method cannot be called from the main thread.
 *
 * @param fragmentArgs a bundle to passed into fragment
 * @param themeResId a style resource id to be set to the host activity's theme
 * @param initialState the initial [Lifecycle.State]. This must be one of
 * [Lifecycle.State.CREATED], [Lifecycle.State.STARTED], or [Lifecycle.State.RESUMED].
 * @param factory a fragment factory to use or null to use default factory
 */
public inline fun <reified F : Fragment> launchFragment(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
    initialState: Lifecycle.State = Lifecycle.State.RESUMED,
    factory: FragmentFactory? = null
): FragmentScenario<F> = launch(
    F::class.java, fragmentArgs, themeResId, initialState,
    factory
)

/**
 * Launches a Fragment with given arguments hosted by an empty [FragmentActivity] using
 * [instantiate] to create the Fragment and waits for it to reach [initialState].
 *
 * This method cannot be called from the main thread.
 *
 * @param fragmentArgs a bundle to passed into fragment
 * @param themeResId a style resource id to be set to the host activity's theme
 * @param initialState the initial [Lifecycle.State]. This must be one of
 * [Lifecycle.State.CREATED], [Lifecycle.State.STARTED], or [Lifecycle.State.RESUMED].
 * @param instantiate method which will be used to instantiate the Fragment.
 */
public inline fun <reified F : Fragment> launchFragment(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
    initialState: Lifecycle.State = Lifecycle.State.RESUMED,
    crossinline instantiate: () -> F
): FragmentScenario<F> = launch(
    F::class.java, fragmentArgs, themeResId, initialState,
    object : FragmentFactory() {
        override fun instantiate(
            classLoader: ClassLoader,
            className: String
        ) = when (className) {
            F::class.java.name -> instantiate()
            else -> super.instantiate(classLoader, className)
        }
    }
)

/**
 * Launches a Fragment in the Activity's root view container `android.R.id.content`, with
 * given arguments hosted by an empty [FragmentActivity] and waits for it to reach [initialState].
 *
 * This method cannot be called from the main thread.
 *
 * @param fragmentArgs a bundle to passed into fragment
 * @param themeResId a style resource id to be set to the host activity's theme
 * @param initialState the initial [Lifecycle.State]. This must be one of
 * [Lifecycle.State.CREATED], [Lifecycle.State.STARTED], or [Lifecycle.State.RESUMED].
 * @param factory a fragment factory to use or null to use default factory
 */
public inline fun <reified F : Fragment> launchFragmentInContainer(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
    initialState: Lifecycle.State = Lifecycle.State.RESUMED,
    factory: FragmentFactory? = null
): FragmentScenario<F> = FragmentScenario.launchInContainer(
    F::class.java, fragmentArgs, themeResId, initialState,
    factory
)

/**
 * Launches a Fragment in the Activity's root view container `android.R.id.content`, with
 * given arguments hosted by an empty [FragmentActivity] using
 * [instantiate] to create the Fragment and waits for it to reach [initialState].
 *
 * This method cannot be called from the main thread.
 *
 * @param fragmentArgs a bundle to passed into fragment
 * @param themeResId a style resource id to be set to the host activity's theme
 * @param initialState the initial [Lifecycle.State]. This must be one of
 * [Lifecycle.State.CREATED], [Lifecycle.State.STARTED], or [Lifecycle.State.RESUMED].
 * @param instantiate method which will be used to instantiate the Fragment. This is a
 * simplification of the [FragmentFactory] interface for cases where only a single class
 * needs a custom constructor called.
 */
public inline fun <reified F : Fragment> launchFragmentInContainer(
    fragmentArgs: Bundle? = null,
    @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
    initialState: Lifecycle.State = Lifecycle.State.RESUMED,
    crossinline instantiate: () -> F
): FragmentScenario<F> = FragmentScenario.launchInContainer(
    F::class.java, fragmentArgs, themeResId, initialState,
    object : FragmentFactory() {
        override fun instantiate(
            classLoader: ClassLoader,
            className: String
        ) = when (className) {
            F::class.java.name -> instantiate()
            else -> super.instantiate(classLoader, className)
        }
    }
)

/**
 * Run [block] using [FragmentScenario.onFragment], returning the result of the [block].
 *
 * If any exceptions are raised while running [block], they are rethrown.
 */
@SuppressWarnings("DocumentExceptions")
public inline fun <reified F : Fragment, T : Any> FragmentScenario<F>.withFragment(
    crossinline block: F.() -> T
): T {
    lateinit var value: T
    var err: Throwable? = null
    onFragment { fragment ->
        try {
            value = block(fragment)
        } catch (t: Throwable) {
            err = t
        }
    }
    err?.let { throw it }
    return value
}

/**
 * FragmentScenario provides API to start and drive a Fragment's lifecycle state for testing. It
 * works with arbitrary fragments and works consistently across different versions of the Android
 * framework.
 *
 * FragmentScenario only supports [androidx.fragment.app.Fragment][Fragment]. If you are using
 * a deprecated fragment class such as `android.support.v4.app.Fragment` or
 * [android.app.Fragment], please update your code to
 * [androidx.fragment.app.Fragment][Fragment].
 *
 * If your testing Fragment has a dependency to specific theme such as `Theme.AppCompat`,
 * use the theme ID parameter in [launch] method.
 *
 * @param F The Fragment class being tested
 *
 * @see ActivityScenario a scenario API for Activity
 */
public class FragmentScenario<F : Fragment> private constructor(
    @Suppress("MemberVisibilityCanBePrivate") /* synthetic access */
    internal val fragmentClass: Class<F>,
    private val activityScenario: ActivityScenario<EmptyFragmentActivity>
) : Closeable {

    /**
     * An empty activity inheriting FragmentActivity. This Activity is used to host Fragment in
     * FragmentScenario.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal class EmptyFragmentActivity : FragmentActivity() {
        @SuppressLint("RestrictedApi")
        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(
                intent.getIntExtra(
                    THEME_EXTRAS_BUNDLE_KEY,
                    R.style.FragmentScenarioEmptyFragmentActivityTheme
                )
            )

            // Checks if we have a custom FragmentFactory and set it.
            val factory = FragmentFactoryHolderViewModel.getInstance(this).fragmentFactory
            if (factory != null) {
                supportFragmentManager.fragmentFactory = factory
            }

            // FragmentFactory needs to be set before calling the super.onCreate, otherwise the
            // Activity crashes when it is recreating and there is a fragment which has no
            // default constructor.
            super.onCreate(savedInstanceState)
        }

        companion object {
            const val THEME_EXTRAS_BUNDLE_KEY = "androidx.fragment.app.testing.FragmentScenario" +
                ".EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY"
        }
    }

    /**
     * A view-model to hold a fragment factory.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    internal class FragmentFactoryHolderViewModel : ViewModel() {
        var fragmentFactory: FragmentFactory? = null

        override fun onCleared() {
            super.onCleared()
            fragmentFactory = null
        }

        companion object {
            @Suppress("MemberVisibilityCanBePrivate")
            internal val FACTORY: ViewModelProvider.Factory =
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        val viewModel =
                            FragmentFactoryHolderViewModel()
                        return viewModel as T
                    }
                }

            fun getInstance(activity: FragmentActivity): FragmentFactoryHolderViewModel {
                val viewModel: FragmentFactoryHolderViewModel by activity.viewModels { FACTORY }
                return viewModel
            }
        }
    }

    /**
     * Moves Fragment state to a new state.
     *
     *  If a new state and current state are the same, this method does nothing. It accepts
     * [CREATED][Lifecycle.State.CREATED], [STARTED][Lifecycle.State.STARTED],
     * [RESUMED][Lifecycle.State.RESUMED], and [DESTROYED][Lifecycle.State.DESTROYED].
     * [DESTROYED][Lifecycle.State.DESTROYED] is a terminal state.
     * You cannot move to any other state after the Fragment reaches that state.
     *
     * This method cannot be called from the main thread.
     */
    public fun moveToState(newState: Lifecycle.State): FragmentScenario<F> {
        if (newState == Lifecycle.State.DESTROYED) {
            activityScenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager
                    .findFragmentByTag(FRAGMENT_TAG)
                // Null means the fragment has been destroyed already.
                if (fragment != null) {
                    activity.supportFragmentManager.commitNow {
                        remove(fragment)
                    }
                }
            }
        } else {
            activityScenario.onActivity { activity ->
                val fragment = requireNotNull(
                    activity.supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
                ) {
                    "The fragment has been removed from the FragmentManager already."
                }
                activity.supportFragmentManager.commitNow {
                    setMaxLifecycle(fragment, newState)
                }
            }
        }
        return this
    }

    /**
     * Recreates the host Activity.
     *
     * After this method call, it is ensured that the Fragment state goes back to the same state
     * as its previous state.
     *
     * This method cannot be called from the main thread.
     */
    public fun recreate(): FragmentScenario<F> {
        activityScenario.recreate()
        return this
    }

    /**
     * FragmentAction interface should be implemented by any class whose instances are intended to
     * be executed by the main thread. A Fragment that is instrumented by the FragmentScenario is
     * passed to [FragmentAction.perform] method.
     *
     * You should never keep the Fragment reference as it will lead to unpredictable behaviour.
     * It should only be accessed in [FragmentAction.perform] scope.
     */
    public fun interface FragmentAction<F : Fragment> {
        /**
         * This method is invoked on the main thread with the reference to the Fragment.
         *
         * @param fragment a Fragment instrumented by the FragmentScenario.
         */
        public fun perform(fragment: F)
    }

    /**
     * Runs a given [action] on the current Activity's main thread.
     *
     * Note that you should never keep Fragment reference passed into your [action]
     * because it can be recreated at anytime during state transitions.
     *
     * Throwing an exception from [action] makes the host Activity crash. You can
     * inspect the exception in logcat outputs.
     *
     * This method cannot be called from the main thread.
     */
    public fun onFragment(action: FragmentAction<F>): FragmentScenario<F> {
        activityScenario.onActivity { activity ->
            val fragment = requireNotNull(
                activity.supportFragmentManager.findFragmentByTag(FRAGMENT_TAG)
            ) {
                "The fragment has been removed from the FragmentManager already."
            }
            check(fragmentClass.isInstance(fragment))
            action.perform(requireNotNull(fragmentClass.cast(fragment)))
        }
        return this
    }

    /**
     * Finishes the managed fragments and cleans up device's state. This method blocks execution
     * until the host activity becomes [Lifecycle.State.DESTROYED].
     */
    public override fun close() {
        activityScenario.close()
    }

    public companion object {
        private const val FRAGMENT_TAG = "FragmentScenario_Fragment_Tag"

        /**
         * Launches a Fragment with given arguments hosted by an empty [FragmentActivity] using
         * the given [FragmentFactory] and waits for it to reach the resumed state.
         *
         *
         * This method cannot be called from the main thread.
         *
         * @param fragmentClass a fragment class to instantiate
         * @param fragmentArgs a bundle to passed into fragment
         * @param factory a fragment factory to use or null to use default factory
         */
        @JvmStatic
        public fun <F : Fragment> launch(
            fragmentClass: Class<F>,
            fragmentArgs: Bundle?,
            factory: FragmentFactory?
        ): FragmentScenario<F> = launch(
            fragmentClass,
            fragmentArgs,
            R.style.FragmentScenarioEmptyFragmentActivityTheme,
            Lifecycle.State.RESUMED,
            factory
        )

        /**
         * Launches a Fragment with given arguments hosted by an empty [FragmentActivity] themed
         * by [themeResId], using the given [FragmentFactory] and waits for it to reach the
         * resumed state.
         *
         * This method cannot be called from the main thread.
         *
         * @param fragmentClass a fragment class to instantiate
         * @param fragmentArgs a bundle to passed into fragment
         * @param themeResId a style resource id to be set to the host activity's theme
         * @param factory a fragment factory to use or null to use default factory
         */
        @JvmStatic
        public fun <F : Fragment> launch(
            fragmentClass: Class<F>,
            fragmentArgs: Bundle?,
            @StyleRes themeResId: Int,
            factory: FragmentFactory?
        ): FragmentScenario<F> = launch(
            fragmentClass,
            fragmentArgs,
            themeResId,
            Lifecycle.State.RESUMED,
            factory
        )

        /**
         * Launches a Fragment with given arguments hosted by an empty [FragmentActivity] themed
         * by [themeResId], using the given [FragmentFactory] and waits for it to reach
         * [initialState].
         *
         * This method cannot be called from the main thread.
         *
         * @param fragmentClass a fragment class to instantiate
         * @param fragmentArgs a bundle to passed into fragment
         * @param themeResId a style resource id to be set to the host activity's theme
         * @param initialState The initial [Lifecycle.State]. This must be one of
         * [CREATED][Lifecycle.State.CREATED], [STARTED][Lifecycle.State.STARTED], and
         * [RESUMED][Lifecycle.State.RESUMED].
         * @param factory a fragment factory to use or null to use default factory
         */
        @JvmOverloads
        @JvmStatic
        public fun <F : Fragment> launch(
            fragmentClass: Class<F>,
            fragmentArgs: Bundle? = null,
            @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
            initialState: Lifecycle.State = Lifecycle.State.RESUMED,
            factory: FragmentFactory? = null
        ): FragmentScenario<F> = internalLaunch(
            fragmentClass,
            fragmentArgs,
            themeResId,
            initialState,
            factory,
            0 /*containerViewId=*/
        )

        /**
         * Launches a Fragment in the Activity's root view container `android.R.id.content`, with
         * given arguments hosted by an empty [FragmentActivity] using the given
         * [FragmentFactory] and waits for it to reach the resumed state.
         *
         * This method cannot be called from the main thread.
         *
         * @param fragmentClass a fragment class to instantiate
         * @param fragmentArgs a bundle to passed into fragment
         * @param factory a fragment factory to use or null to use default factory
         */
        @JvmStatic
        public fun <F : Fragment> launchInContainer(
            fragmentClass: Class<F>,
            fragmentArgs: Bundle?,
            factory: FragmentFactory?
        ): FragmentScenario<F> = launchInContainer(
            fragmentClass,
            fragmentArgs,
            R.style.FragmentScenarioEmptyFragmentActivityTheme,
            Lifecycle.State.RESUMED,
            factory
        )

        /**
         * Launches a Fragment in the Activity's root view container `android.R.id.content`, with
         * given arguments hosted by an empty [FragmentActivity] themed by [themeResId],
         * using the given [FragmentFactory] and waits for it to reach the resumed state.
         *
         * This method cannot be called from the main thread.
         *
         * @param fragmentClass a fragment class to instantiate
         * @param fragmentArgs a bundle to passed into fragment
         * @param themeResId a style resource id to be set to the host activity's theme
         * @param factory a fragment factory to use or null to use default factory
         */
        @JvmStatic
        public fun <F : Fragment> launchInContainer(
            fragmentClass: Class<F>,
            fragmentArgs: Bundle?,
            @StyleRes themeResId: Int,
            factory: FragmentFactory?
        ): FragmentScenario<F> = launchInContainer(
            fragmentClass,
            fragmentArgs,
            themeResId,
            Lifecycle.State.RESUMED,
            factory
        )

        /**
         * Launches a Fragment in the Activity's root view container `android.R.id.content`, with
         * given arguments hosted by an empty [FragmentActivity] themed by [themeResId],
         * using the given [FragmentFactory] and waits for it to reach [initialState].
         *
         * This method cannot be called from the main thread.
         *
         * @param fragmentClass a fragment class to instantiate
         * @param fragmentArgs a bundle to passed into fragment
         * @param themeResId a style resource id to be set to the host activity's theme
         * @param initialState The initial [Lifecycle.State]. This must be one of
         * [CREATED][Lifecycle.State.CREATED], [STARTED][Lifecycle.State.STARTED], and
         * [RESUMED][Lifecycle.State.RESUMED].
         * @param factory a fragment factory to use or null to use default factory
         */
        @JvmOverloads
        @JvmStatic
        public fun <F : Fragment> launchInContainer(
            fragmentClass: Class<F>,
            fragmentArgs: Bundle? = null,
            @StyleRes themeResId: Int = R.style.FragmentScenarioEmptyFragmentActivityTheme,
            initialState: Lifecycle.State = Lifecycle.State.RESUMED,
            factory: FragmentFactory? = null
        ): FragmentScenario<F> = internalLaunch(
            fragmentClass,
            fragmentArgs,
            themeResId,
            initialState,
            factory,
            android.R.id.content
        )

        @SuppressLint("RestrictedApi")
        internal fun <F : Fragment> internalLaunch(
            fragmentClass: Class<F>,
            fragmentArgs: Bundle?,
            @StyleRes themeResId: Int,
            initialState: Lifecycle.State,
            factory: FragmentFactory?,
            @IdRes containerViewId: Int
        ): FragmentScenario<F> {
            require(initialState != Lifecycle.State.DESTROYED) {
                "Cannot set initial Lifecycle state to $initialState for FragmentScenario"
            }
            val componentName = ComponentName(
                ApplicationProvider.getApplicationContext(),
                EmptyFragmentActivity::class.java
            )
            val startActivityIntent = Intent.makeMainActivity(componentName)
                .putExtra(EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY, themeResId)
            val scenario = FragmentScenario(
                fragmentClass,
                ActivityScenario.launch(
                    startActivityIntent
                )
            )
            scenario.activityScenario.onActivity { activity ->
                if (factory != null) {
                    FragmentFactoryHolderViewModel.getInstance(activity).fragmentFactory = factory
                    activity.supportFragmentManager.fragmentFactory = factory
                }
                val fragment = activity.supportFragmentManager.fragmentFactory
                    .instantiate(requireNotNull(fragmentClass.classLoader), fragmentClass.name)
                fragment.arguments = fragmentArgs
                activity.supportFragmentManager.commitNow {
                    add(containerViewId, fragment, FRAGMENT_TAG)
                    setMaxLifecycle(fragment, initialState)
                }
            }
            return scenario
        }
    }
}
