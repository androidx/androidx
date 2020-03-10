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

package androidx.compose

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.ui.core.ComponentNode
import androidx.ui.core.UiComposition
import androidx.ui.core.findComposition
import androidx.ui.core.setViewContent

object Compose {
    /**
     * This method is the way to initiate a composition. The [composable] passed in will be executed
     * to compose the children of the passed in [container].  Optionally, a [parent]
     * [CompositionReference] can be provided to make the composition behave as a sub-composition of
     * the parent.  The children of [container] will be updated and maintained by the time this
     * method returns.
     *
     * It is important to call [Composition.dispose] whenever this view is no longer needed in order
     * to release resources.
     *
     * @param container The view whose children is being composed.
     * @param parent The parent composition reference, if applicable. Default is null.
     * @param composable The composable function intended to compose the children of [container].
     *
     * @see Composition.dispose
     * @see Composable
     */
    @MainThread
    @Deprecated(
        "use setContent",
        replaceWith = ReplaceWith("ViewGroup#setContent")
    )
    fun composeInto(
        container: ViewGroup,
        parent: CompositionReference? = null,
        composable: @Composable() () -> Unit
    ): Composition = container.setViewContent(parent, composable)

    /**
     * Disposes any composition previously run with [container] as the root. This will
     * release any resources that have been built around the composition, including all [onDispose]
     * callbacks that have been registered with [CompositionLifecycleObserver] objects.
     *
     * It is important to call this for any [composeInto] call that is made, or else you may have
     * memory leaks in your application.
     *
     * @param container The view that was passed into [composeInto] as the root container of the composition
     * @param parent The parent composition reference, if applicable.
     *
     * @see composeInto
     * @see CompositionLifecycleObserver
     */
    @Suppress("UNUSED_PARAMETER")
    @MainThread
    @Deprecated(
        "disposing should be done with the Composition object returned by setContent",
        replaceWith = ReplaceWith("Composition#dispose()")
    )
    fun disposeComposition(container: ViewGroup, parent: CompositionReference? = null) {
        findComposition(container)?.dispose()
    }

    /**
     * This method is the way to initiate a composition. The [composable] passed in will be executed
     * to compose the children of the passed in [container].  Optionally, a [parent]
     * [CompositionReference] can be provided to make the composition behave as a sub-composition of
     * the parent.  The children of [container] will be updated and maintained by the time this
     * method returns.
     *
     * It is important to call [Composition.dispose] whenever this view is no longer needed in order
     * to release resources.
     *
     * @param container The emittable whose children is being composed.
     * @param context The android [Context] to associate with this composition.
     * @param parent The parent composition reference, if applicable. Default is null.
     * @param composable The composable function intended to compose the children of [container].
     *
     * @see Composition.dispose
     * @see Composable
     */
    @MainThread
    @Deprecated(
        "use setContent",
        replaceWith = ReplaceWith("ViewGroup#setContent")
    )
    fun composeInto(
        container: ComponentNode,
        context: Context,
        parent: CompositionReference? = null,
        composable: @Composable() () -> Unit
    ): Composition {
        val composition = findComposition(container)
            ?: UiComposition(container, context, parent)
        composition.compose(composable)
        return composition
    }

    /**
     * Disposes any composition previously run with [container] as the root. This will
     * release any resources that have been built around the composition, including all [onDispose]
     * callbacks that have been registered with [CompositionLifecycleObserver] objects.
     *
     * It is important to call this for any [composeInto] call that is made, or else you may have
     * memory leaks in your application.
     *
     * @param container The view that was passed into [composeInto] as the root container of the composition
     * @param context The android [Context] associated with the composition
     * @param parent The parent composition reference, if applicable.
     *
     * @see composeInto
     * @see CompositionLifecycleObserver
     */
    @MainThread
    @Suppress("UNUSED_PARAMETER")
    @Deprecated(
        "disposing should be done with the Composition object returned by setContent",
        replaceWith = ReplaceWith("Composition#dispose()")
    )
    fun disposeComposition(
        container: ComponentNode,
        context: Context,
        parent: CompositionReference? = null
    ) {
        // temporary easy way to call correct lifecycles on everything
        findComposition(container)?.dispose()
    }
}