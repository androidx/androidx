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
package androidx.ui.core

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.compose.Composable
import androidx.compose.Composition
import androidx.compose.CompositionReference
import androidx.compose.FrameManager
import androidx.compose.Recomposer
import androidx.compose.compositionFor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.ui.core.selection.SelectionContainer
import androidx.ui.node.UiComposer

/**
 * Composes the children of the view with the passed in [composable].
 *
 * @see setViewContent
 * @see Composition.dispose
 */
// TODO: Remove this API when View/LayoutNode mixed trees work
fun ViewGroup.setViewContent(
    parent: CompositionReference? = null,
    composable: @Composable () -> Unit
): Composition = compositionFor(
    context = context,
    container = this,
    recomposer = Recomposer.current(),
    parent = parent,
    onBeforeFirstComposition = {
        removeAllViews()
    }
).apply {
    setContent(composable)
}

/**
 * Sets the contentView of an activity to a FrameLayout, and composes the contents of the layout
 * with the passed in [composable].
 *
 * @see setContent
 * @see Activity.setContentView
 */
// TODO: Remove this API when View/LayoutNode mixed trees work
fun Activity.setViewContent(composable: @Composable () -> Unit): Composition {
    // TODO(lmr): add ambients here, or remove API entirely if we can
    // If there is already a FrameLayout in the root, we assume we want to compose
    // into it instead of create a new one. This allows for `setContent` to be
    // called multiple times.
    val root = window
        .decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? ViewGroup
        ?: FrameLayout(this).also { setContentView(it) }
    return root.setViewContent(null, composable)
}

// TODO(chuckj): This is a temporary work-around until subframes exist so that
// nextFrame() inside recompose() doesn't really start a new frame, but a new subframe
// instead.
@MainThread
fun subcomposeInto(
    context: Context,
    container: LayoutNode,
    recomposer: Recomposer,
    parent: CompositionReference? = null,
    composable: @Composable () -> Unit
): Composition = compositionFor(context, container, recomposer, parent).apply {
    setContent(composable)
}

@Deprecated(
    "Specify Recomposer explicitly",
    ReplaceWith(
        "subcomposeInto(context, container, Recomposer.current(), parent, composable)",
        "androidx.compose.Recomposer"
    )
)
@MainThread
fun subcomposeInto(
    container: LayoutNode,
    context: Context,
    parent: CompositionReference? = null,
    composable: @Composable () -> Unit
): Composition = subcomposeInto(context, container, Recomposer.current(), parent, composable)

/**
 * Composes the given composable into the given activity. The [content] will become the root view
 * of the given activity.
 *
 * [Composition.dispose] is called automatically when the Activity is destroyed.
 *
 * @param recomposer The [Recomposer] to coordinate scheduling of composition updates
 * @param content A `@Composable` function declaring the UI contents
 */
fun ComponentActivity.setContent(
    // Note: Recomposer.current() is the default here since all Activity view trees are hosted
    // on the main thread.
    recomposer: Recomposer = Recomposer.current(),
    content: @Composable () -> Unit
): Composition {
    FrameManager.ensureStarted()
    val composeView: AndroidOwner = window.decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? AndroidOwner
        ?: AndroidOwner(this, this).also {
            setContentView(it.view, DefaultLayoutParams)
        }
    return doSetContent(this, composeView, recomposer, content)
}

/**
 * We want text/image selection to be enabled by default and disabled per widget. Therefore a root
 * level [SelectionContainer] is installed at the root.
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun WrapWithSelectionContainer(noinline content: @Composable () -> Unit) {
    SelectionContainer(children = content)
}

/**
 * Composes the given composable into the given view.
 *
 * Note that this [ViewGroup] should have an unique id for the saved instance state mechanism to
 * be able to save and restore the values used within the composition. See [View.setId].
 *
 * @param recomposer The [Recomposer] to coordinate scheduling of composition updates
 * @param content Composable that will be the content of the view.
 */
fun ViewGroup.setContent(
    recomposer: Recomposer,
    content: @Composable () -> Unit
): Composition {
    FrameManager.ensureStarted()
    val composeView =
        if (childCount > 0) {
            getChildAt(0) as? AndroidOwner
        } else {
            removeAllViews(); null
        }
            ?: AndroidOwner(context).also { addView(it.view, DefaultLayoutParams) }
    return doSetContent(context, composeView, recomposer, content)
}

/**
 * Composes the given composable into the given view.
 *
 * Note that this [ViewGroup] should have an unique id for the saved instance state mechanism to
 * be able to save and restore the values used within the composition. See [View.setId].
 *
 * @param content Composable that will be the content of the view.
 */
@Deprecated(
    "Specify Recomposer explicitly",
    ReplaceWith(
        "setContent(Recomposer.current(), content)",
        "androidx.compose.Recomposer"
    )
)
fun ViewGroup.setContent(
    content: @Composable () -> Unit
): Composition = setContent(Recomposer.current(), content)

private fun doSetContent(
    context: Context,
    owner: AndroidOwner,
    recomposer: Recomposer,
    content: @Composable () -> Unit
): Composition {
    val original = compositionFor(context, owner.root, recomposer)
    val wrapped = owner.view.getTag(R.id.wrapped_composition_tag)
            as? WrappedComposition
        ?: WrappedComposition(owner, original).also {
            owner.view.setTag(R.id.wrapped_composition_tag, it)
        }
    wrapped.setContent(content)
    return wrapped
}

private class WrappedComposition(
    val owner: AndroidOwner,
    val original: Composition
) : Composition, LifecycleEventObserver {

    private var disposed = false
    private var addedToLifecycle: Lifecycle? = null

    override fun setContent(content: @Composable () -> Unit) {
        val lifecycle = owner.lifecycleOwner?.lifecycle
        if (lifecycle != null) {
            if (addedToLifecycle == null) {
                addedToLifecycle = lifecycle
                lifecycle.addObserver(this)
            }
            if (owner.savedStateRegistry != null) {
                original.setContent {
                    ProvideAndroidAmbients(owner) {
                        WrapWithSelectionContainer(content)
                    }
                }
            } else {
                // TODO(Andrey) unify setOnSavedStateRegistryAvailable and
                //  setOnLifecycleOwnerAvailable so we will postpone until we have everything.
                //  we should migrate to androidx SavedStateRegistry first
                // we will postpone the composition until composeView restores the state.
                owner.setOnSavedStateRegistryAvailable {
                    if (!disposed) setContent(content)
                }
            }
        } else {
            // we will postpone the composition until we got the lifecycle owner
            owner.setOnLifecycleOwnerAvailable { if (!disposed) setContent(content) }
        }
    }

    override fun dispose() {
        if (!disposed) {
            disposed = true
            owner.view.setTag(R.id.wrapped_composition_tag, null)
            addedToLifecycle?.removeObserver(this)
        }
        original.dispose()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            dispose()
        }
    }
}

@Suppress("NAME_SHADOWING")
private fun compositionFor(
    context: Context,
    container: Any,
    recomposer: Recomposer,
    parent: CompositionReference? = null,
    onBeforeFirstComposition: (() -> Unit)? = null
) = compositionFor(
    container = container,
    recomposer = recomposer,
    parent = parent,
    composerFactory = { slotTable, recomposer ->
        onBeforeFirstComposition?.invoke()
        UiComposer(context, container, slotTable, recomposer)
    }
)

private val DefaultLayoutParams = ViewGroup.LayoutParams(
    ViewGroup.LayoutParams.WRAP_CONTENT,
    ViewGroup.LayoutParams.WRAP_CONTENT
)
