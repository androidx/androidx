/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.viewinterop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewbinding.R
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.fragment.app.findFragment
import androidx.viewbinding.ViewBinding

/**
 * Composes an Android layout resource in the presence of [ViewBinding]. The binding is obtained
 * from the [factory] block, which will be called exactly once to obtain the [ViewBinding]
 * to be composed, and it is also guaranteed to be invoked on the UI thread.
 * Therefore, in addition to creating the [ViewBinding], the block can also be used
 * to perform one-off initializations and [View] constant properties' setting.
 * The [update] block can be run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set [View] properties depending on state. When state changes,
 * the block will be reexecuted to set the new properties. Note the block will also be ran once
 * right after the [factory] block completes.
 *
 * This overload of [AndroidViewBinding] does not automatically pool or reuse Views and their
 * bindings. If placed inside of a reusable container (including inside a
 * [LazyRow][androidx.compose.foundation.lazy.LazyRow] or
 * [LazyColumn][androidx.compose.foundation.lazy.LazyColumn]), the View instances and their
 * bindings will always be discarded and recreated if the composition hierarchy containing the
 * `AndroidViewBinding` changes, even if its group structure did not change and the `View` and its
 * binding could have conceivably been reused.
 *
 * To opt-in for View reuse, call the overload of [AndroidViewBinding] that accepts an `onReset`
 * callback, and provide a non-null implementation for this callback. Since it is expensive to
 * discard and recreate View instances, reusing Views can lead to noticeable performance
 * improvements — especially when building a scrolling list of [AndroidViews][AndroidView]. It is
 * highly recommended to opt-in to View reuse when possible.
 *
 * There is generally no need to opt-in for reuse when using an `AndroidViewBinding` to host a
 * Fragment, since Fragments have their own view lifecycles and do not usually appear in contexts
 * where the reuse offered by `AndroidViewBinding` would lead to measurable performance
 * improvements.
 *
 * @sample androidx.compose.ui.samples.AndroidViewBindingSample
 *
 * @param factory The block creating the [ViewBinding] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update The callback to be invoked after the layout is inflated and upon recomposition to
 * update the information and state of the binding
 */
@Composable
fun <T : ViewBinding> AndroidViewBinding(
    factory: (inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean) -> T,
    modifier: Modifier = Modifier,
    update: T.() -> Unit = { }
) {
    AndroidViewBinding(
        factory = factory,
        modifier = modifier,
        onReset = null,
        update = update
    )
}

/**
 * Composes an Android layout resource in the presence of [ViewBinding]. The binding is obtained
 * from the [factory] block, which will be called exactly once to obtain the [ViewBinding]
 * to be composed, and it is also guaranteed to be invoked on the UI thread.
 * Therefore, in addition to creating the [ViewBinding], the block can also be used
 * to perform one-off initializations and [View] constant properties' setting.
 * The [update] block can be run multiple times (on the UI thread as well) due to recomposition,
 * and it is the right place to set [View] properties depending on state. When state changes,
 * the block will be reexecuted to set the new properties. Note the block will also be ran once
 * right after the [factory] block completes.
 *
 * This overload includes support for View reuse, which behaves in the same way as it does for
 * [AndroidView]. Namely, Views and their binding instances are only eligible for reuse if a
 * non-null [onReset] callback is provided. It is expensive to discard and recreate View instances,
 * so opting-in to View reuse can lead to noticeable performance improvements — especially when
 * [AndroidViewBinding] is used in a scrolling list. It is highly recommended to specify an
 * [onReset] implementation and opt-in to View reuse when possible.
 *
 * When [onReset] is specified, [View] instances and their bindings may be reused when hosted
 * inside of a container that supports reusable elements. Reuse occurs when compatible instances
 * of [AndroidViewBinding] are inserted and removed during recomposition. Two instances of
 * `AndroidViewBinding` are considered compatible if they are invoked with the same composable
 * group structure. The most common scenario where this happens is in lazy layout APIs like
 * `LazyRow` and `LazyColumn`, which can reuse layout nodes (and, in this case, Views and their
 * bindings as well) between items when scrolling.
 *
 * [onReset] is invoked on the UI thread when the View and its binding will be reused, signaling
 * that the View and binding should be prepared to appear in a new context in the composition
 * hierarchy. This callback is invoked before [update] and may be used to reset any transient View
 * state like animations or user input.
 *
 * Note that [onReset] may not be immediately followed by a call to [update]. Compose may
 * temporarily detach the View from the composition hierarchy if it is deactivated but not released
 * from composition. This can happen if the View appears in a [ReusableContentHost] that is not
 * currently active or inside of a [movable content][androidx.compose.runtime.movableContentOf]
 * block that is being moved. If this happens, the View will be removed from its parent, but
 * retained by Compose so that it may be reused if its content host becomes active again. If the
 * View never becomes active again and is instead discarded entirely, the [onReset] callback will
 * be invoked directly from this deactivated state when Compose releases the View and its binding.
 *
 * When the View is removed from the composition permanently, [onRelease] will be invoked (also on
 * the UI thread). Once this callback returns, Compose will never attempt to reuse the previous
 * View or binding instance regardless of whether an [onReset] implementation was provided. If the
 * View is needed again in the future, a new instance will be created, with a fresh lifecycle that
 * begins by calling the [factory].
 *
 * @sample androidx.compose.ui.samples.AndroidViewBindingReusableSample
 *
 * @param factory The block creating the [ViewBinding] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param onReset A callback invoked as a signal that the view is about to be attached to the
 * composition hierarchy in a different context than its original creation. This callback is invoked
 * before [update] and should prepare the view for general reuse. If `null` or not specified, the
 * `AndroidViewBinding` instance will not support reuse, and the View and its binding will always be
 * discarded whenever the AndroidViewBinding is moved or removed from the composition hierarchy.
 * @param onRelease A callback invoked as a signal that this view and its binding have exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * binding should be freed at this time.
 * @param update The callback to be invoked after the layout is inflated and upon recomposition to
 * update the information and state of the binding.
 */
@Composable
fun <T : ViewBinding> AndroidViewBinding(
    factory: (inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean) -> T,
    modifier: Modifier = Modifier,
    onReset: (T.() -> Unit)? = null,
    onRelease: T.() -> Unit = { },
    update: T.() -> Unit = { }
) {
    val localView = LocalView.current
    // Find the parent fragment, if one exists. This will let us ensure that
    // fragments inflated via a FragmentContainerView are properly nested
    // (which, in turn, allows the fragments to properly save/restore their state)
    val parentFragment = remember(localView) {
        try {
            localView.findFragment<Fragment>()
        } catch (e: IllegalStateException) {
            // findFragment throws if no parent fragment is found
            null
        }
    }

    val localContext = LocalContext.current
    AndroidView(
        factory = { context ->
            // Inflated fragments are automatically nested properly when
            // using the parent fragment's LayoutInflater
            val inflater = parentFragment?.layoutInflater ?: LayoutInflater.from(context)
            val viewBinding = factory(inflater, FrameLayout(context), false)
            viewBinding.root.apply {
                setBinding(viewBinding)
            }
        },
        modifier = modifier,
        onReset = onReset?.let { reset -> { view -> view.getBinding<T>().reset() } },
        onRelease = { view ->
            view.getBinding<T>().onRelease()

            (view as? ViewGroup)?.let { rootGroup ->
                // clean up inflated fragments when the AndroidViewBinding is disposed
                // Find the right FragmentManager
                val fragmentManager = parentFragment?.childFragmentManager
                    ?: (localContext as? FragmentActivity)?.supportFragmentManager
                forEachFragmentContainerView(rootGroup) { container ->
                    // Now find the fragment inflated via the FragmentContainerView
                    val existingFragment = fragmentManager?.findFragmentById(container.id)
                    if (existingFragment != null && !fragmentManager.isStateSaved) {
                        // If the state isn't saved, that means that some state change
                        // has removed this Composable from the hierarchy
                        fragmentManager.commit {
                            remove(existingFragment)
                        }
                    }
                }
            }
        },
        update = { view -> view.getBinding<T>().update() }
    )
}

private fun <T : ViewBinding> View.setBinding(binding: T) =
    setTag(R.id.binding_reference, binding)

@Suppress("UNCHECKED_CAST")
private fun <T : ViewBinding> View.getBinding(): T =
    getTag(R.id.binding_reference) as T

private fun forEachFragmentContainerView(
    viewGroup: ViewGroup,
    action: (FragmentContainerView) -> Unit
) {
    if (viewGroup is FragmentContainerView) {
        action(viewGroup)
    } else {
        viewGroup.forEach {
            if (it is ViewGroup) {
                forEachFragmentContainerView(it, action)
            }
        }
    }
}
