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

import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.testing.R
import androidx.lifecycle.Lifecycle

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
): FragmentScenario<F> = FragmentScenario.launch(
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
): FragmentScenario<F> = FragmentScenario.launch(
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
