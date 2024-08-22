/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.fragment.compose

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Allows for adding a [Fragment] directly into Compose. It creates a fragment of the given class
 * and adds it to the fragment manager.
 *
 * Updating the [clazz] or [fragmentState] parameters will result in a new fragment instance being
 * added to the fragment manager and invoke the [onUpdate] callback with the new instance.
 *
 * @sample androidx.fragment.compose.samples.BasicAndroidFragment
 * @param modifier the modifier to be applied to the layout
 * @param fragmentState the savedState of the fragment
 * @param arguments args to be passed to the fragment
 * @param onUpdate callback that provides the created fragment
 */
@Composable
inline fun <reified T : Fragment> AndroidFragment(
    modifier: Modifier = Modifier,
    fragmentState: FragmentState = rememberFragmentState(),
    arguments: Bundle = Bundle.EMPTY,
    noinline onUpdate: (T) -> Unit = {}
) {
    AndroidFragment(clazz = T::class.java, modifier, fragmentState, arguments, onUpdate)
}

/**
 * Allows for adding a [Fragment] directly into Compose. It creates a fragment of the given class
 * and adds it to the fragment manager.
 *
 * Updating the [clazz] or [fragmentState] parameters will result in a new fragment instance being
 * added to the fragment manager and invoke the [onUpdate] callback with the new instance.
 *
 * @sample androidx.fragment.compose.samples.BasicAndroidFragment
 * @param clazz fragment class to be created
 * @param modifier the modifier to be applied to the layout
 * @param fragmentState the savedState of the fragment
 * @param arguments args to be passed to the fragment
 * @param onUpdate callback that provides the created fragment
 */
@Suppress("MissingJvmstatic")
@Composable
fun <T : Fragment> AndroidFragment(
    clazz: Class<T>,
    modifier: Modifier = Modifier,
    fragmentState: FragmentState = rememberFragmentState(),
    arguments: Bundle = Bundle.EMPTY,
    onUpdate: (T) -> Unit = {}
) {
    val updateCallback = rememberUpdatedState(onUpdate)
    val hashKey = currentCompositeKeyHash
    val view = LocalView.current
    val fragmentManager = remember(view) { FragmentManager.findFragmentManager(view) }
    val context = LocalContext.current
    lateinit var container: FragmentContainerView
    AndroidView(
        {
            container = FragmentContainerView(context)
            container.id = hashKey
            container
        },
        modifier
    )

    DisposableEffect(fragmentManager, clazz, fragmentState) {
        var removeEvenIfStateIsSaved = false
        val fragment =
            fragmentManager.findFragmentById(container.id)
                ?: fragmentManager.fragmentFactory
                    .instantiate(context.classLoader, clazz.name)
                    .apply {
                        setInitialSavedState(fragmentState.state.value)
                        setArguments(arguments)
                        val transaction =
                            fragmentManager
                                .beginTransaction()
                                .setReorderingAllowed(true)
                                .add(container, this, "$hashKey")
                        if (fragmentManager.isStateSaved) {
                            // If the state is saved when we add the fragment,
                            // we want to remove the Fragment in onDispose
                            // if isStateSaved never becomes true for the lifetime
                            // of this AndroidFragment - we use a LifecycleObserver
                            // on the Fragment as a proxy for that signal
                            removeEvenIfStateIsSaved = true
                            lifecycle.addObserver(
                                object : DefaultLifecycleObserver {
                                    override fun onStart(owner: LifecycleOwner) {
                                        removeEvenIfStateIsSaved = false
                                        lifecycle.removeObserver(this)
                                    }
                                }
                            )
                            transaction.commitNowAllowingStateLoss()
                        } else {
                            transaction.commitNow()
                        }
                    }
        fragmentManager.onContainerAvailable(container)
        @Suppress("UNCHECKED_CAST") updateCallback.value(fragment as T)
        onDispose {
            val state = fragmentManager.saveFragmentInstanceState(fragment)
            fragmentState.state.value = state
            if (removeEvenIfStateIsSaved) {
                // The Fragment was added when the state was saved and
                // isStateSaved never became true for the lifetime of this
                // AndroidFragment, so we unconditionally remove it here
                fragmentManager.commitNow(allowStateLoss = true) { remove(fragment) }
            } else if (!fragmentManager.isStateSaved) {
                // If the state isn't saved, that means that some state change
                // has removed this Composable from the hierarchy
                fragmentManager.commitNow { remove(fragment) }
            }
        }
    }
}
