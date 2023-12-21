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

import android.content.Context
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalMap
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.Updater
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.materialize
import androidx.compose.ui.node.ComposeUiNode
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.UiApplier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Composes an Android [View] obtained from [factory]. The [factory] block will be called exactly
 * once to obtain the [View] being composed, and it is also guaranteed to be invoked on the UI
 * thread. Therefore, in addition to creating the [View], the [factory] block can also be used to
 * perform one-off initializations and [View] constant properties' setting. The [update] block can
 * run multiple times (on the UI thread as well) due to recomposition, and it is the right place to
 * set the new properties. Note that the block will also run once right after the [factory] block
 * completes.
 *
 * [AndroidView] is commonly needed for using Views that are infeasible to be reimplemented in
 * Compose and there is no corresponding Compose API. Common examples for the moment are
 * WebView, SurfaceView, AdView, etc.
 *
 * This overload of [AndroidView] does not automatically pool or reuse Views. If placed inside of a
 * reusable container (including inside a [LazyRow][androidx.compose.foundation.lazy.LazyRow] or
 * [LazyColumn][androidx.compose.foundation.lazy.LazyColumn]), the View instances will always be
 * discarded and recreated if the composition hierarchy containing the AndroidView changes, even
 * if its group structure did not change and the View could have conceivably been reused.
 *
 * To opt-in for View reuse, call the overload of [AndroidView] that accepts an `onReset` callback,
 * and provide a non-null implementation for this callback. Since it is expensive to discard and
 * recreate View instances, reusing Views can lead to noticeable performance improvements —
 * especially when building a scrolling list of [AndroidViews][AndroidView]. It is highly
 * recommended to opt-in to View reuse when possible.
 *
 * [AndroidView] will not clip its content to the layout bounds. Use [View.setClipToOutline] on
 * the child View to clip the contents, if desired. Developers will likely want to do this with
 * all subclasses of SurfaceView to keep its contents contained.
 *
 * [AndroidView] has nested scroll interop capabilities if the containing view has nested scroll
 * enabled. This means this Composable can dispatch scroll deltas if it is placed inside a
 * container that participates in nested scroll. For more information on how to enable
 * nested scroll interop:
 * @sample androidx.compose.ui.samples.ViewInComposeNestedScrollInteropSample
 *
 * @sample androidx.compose.ui.samples.AndroidViewSample
 *
 * @param factory The block creating the [View] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update A callback to be invoked after the layout is inflated and upon recomposition to
 * update the information and state of the view.
 */
@Composable
@UiComposable
fun <T : View> AndroidView(
    factory: (Context) -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate
) {
    AndroidView(
        factory = factory,
        modifier = modifier,
        update = update,
        onRelease = NoOpUpdate
    )
}

/**
 * Composes an Android [View] obtained from [factory]. The [factory] block will be called exactly
 * once to obtain the [View] being composed, and it is also guaranteed to be invoked on the UI
 * thread. Therefore, in addition to creating the [View], the [factory] block can also be used to
 * perform one-off initializations and [View] constant properties' setting. The [update] block can
 * run multiple times (on the UI thread as well) due to recomposition, and it is the right place to
 * set the new properties. Note that the block will also run once right after the [factory] block
 * completes.
 *
 * [AndroidView] is commonly needed for using Views that are infeasible to be reimplemented in
 * Compose and there is no corresponding Compose API. Common examples for the moment are
 * WebView, SurfaceView, AdView, etc.
 *
 * By default, [AndroidView] does not automatically pool or reuse Views. If placed inside of a
 * reusable container (including inside a [LazyRow][androidx.compose.foundation.lazy.LazyRow] or
 * [LazyColumn][androidx.compose.foundation.lazy.LazyColumn]), the View instances will always be
 * discarded and recreated if the composition hierarchy containing the AndroidView changes, even
 * if its group structure did not change and the View could have conceivably been reused.
 *
 * Views are eligible for reuse if [AndroidView] is given a non-null [onReset] callback. Since
 * it is expensive to discard and recreate View instances, reusing Views can lead to noticeable
 * performance improvements — especially when building a scrolling list of
 * [AndroidViews][AndroidView]. It is highly recommended to specify an [onReset] implementation and
 * opt-in to View reuse when possible.
 *
 * When [onReset] is specified, [View] instances may be reused when hosted inside of a container
 * that supports reusable elements. Reuse occurs when compatible instances of [AndroidView] are
 * inserted and removed during recomposition. Two instances of `AndroidView` are considered
 * compatible if they are invoked with the same composable group structure. The most common
 * scenario where this happens is in lazy layout APIs like `LazyRow` and `LazyColumn`, which
 * can reuse layout nodes (and Views, in this case) between items when scrolling.
 *
 * [onReset] is invoked on the UI thread when the View will be reused, signaling that the View
 * should be prepared to appear in a new context in the composition hierarchy. This callback
 * is invoked before [update] and may be used to reset any transient View state like animations or
 * user input.
 *
 * Note that [onReset] may not be immediately followed by a call to [update]. Compose may
 * temporarily detach the View from the composition hierarchy if it is deactivated but not released
 * from composition. This can happen if the View appears in a [ReusableContentHost] that is not
 * currently active or inside of a [movable content][androidx.compose.runtime.movableContentOf]
 * block that is being moved. If this happens, the View will be removed from its parent, but
 * retained by Compose so that it may be reused if its content host becomes active again. If the
 * View never becomes active again and is instead discarded entirely, the [onReset] callback will
 * be invoked directly from this deactivated state when Compose releases the View.
 *
 * If you need to observe whether the View is currently used in the composition hierarchy, you may
 * observe whether it is attached via [View.addOnAttachStateChangeListener]. The View may also
 * observe the lifecycle of its host via [findViewTreeLifecycleOwner]. The lifecycle returned by
 * this function will match the [LocalLifecycleOwner]. Note that the lifecycle is not set and cannot
 * be used until the View is attached.
 *
 * When the View is removed from the composition permanently, [onRelease] will be invoked (also on
 * the UI thread). Once this callback returns, Compose will never attempt to reuse the previous
 * View instance regardless of whether an [onReset] implementation was provided. If the View is
 * needed again in the future, a new instance will be created, with a fresh lifecycle that begins
 * by calling the [factory].
 *
 * [AndroidView] will not clip its content to the layout bounds. Use [View.setClipToOutline] on
 * the child View to clip the contents, if desired. Developers will likely want to do this with
 * all subclasses of SurfaceView to keep its contents contained.
 *
 * [AndroidView] has nested scroll interop capabilities if the containing view has nested scroll
 * enabled. This means this Composable can dispatch scroll deltas if it is placed inside a
 * container that participates in nested scroll. For more information on how to enable
 * nested scroll interop:
 * @sample androidx.compose.ui.samples.ViewInComposeNestedScrollInteropSample
 *
 * @sample androidx.compose.ui.samples.AndroidViewSample
 *
 * @sample androidx.compose.ui.samples.ReusableAndroidViewInLazyColumnSample
 *
 * @sample androidx.compose.ui.samples.AndroidViewWithReleaseSample
 *
 * @param factory The block creating the [View] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param onReset A callback invoked as a signal that the view is about to be attached to the
 * composition hierarchy in a different context than its original creation. This callback is invoked
 * before [update] and should prepare the view for general reuse. If `null` or not specified, the
 * `AndroidView` instance will not support reuse, and the View instance will always be discarded
 * whenever the AndroidView is moved or removed from the composition hierarchy.
 * @param onRelease A callback invoked as a signal that this view instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * View should be freed at this time.
 * @param update A callback to be invoked after the layout is inflated and upon recomposition to
 * update the information and state of the view.
 */
@Composable
@UiComposable
fun <T : View> AndroidView(
    factory: (Context) -> T,
    modifier: Modifier = Modifier,
    onReset: ((T) -> Unit)? = null,
    onRelease: (T) -> Unit = NoOpUpdate,
    update: (T) -> Unit = NoOpUpdate
) {
    val materializedModifier = currentComposer.materialize(modifier)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val compositionLocalMap = currentComposer.currentCompositionLocalMap

    // These locals are initialized from the view tree at the AndroidComposeView hosting this
    // composition, but they need to be passed to this Android View so that the ViewTree*Owner
    // functions return the correct owners if different local values were provided by the
    // composition, e.g. by a navigation library.
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current

    if (onReset != null) {
        ReusableComposeNode<LayoutNode, UiApplier>(
            factory = createAndroidViewNodeFactory(factory),
            update = {
                updateViewHolderParams<T>(
                    modifier = materializedModifier,
                    density = density,
                    lifecycleOwner = lifecycleOwner,
                    savedStateRegistryOwner = savedStateRegistryOwner,
                    layoutDirection = layoutDirection,
                    compositionLocalMap = compositionLocalMap
                )
                set(onReset) { requireViewFactoryHolder<T>().resetBlock = it }
                set(update) { requireViewFactoryHolder<T>().updateBlock = it }
                set(onRelease) { requireViewFactoryHolder<T>().releaseBlock = it }
            }
        )
    } else {
        ComposeNode<LayoutNode, UiApplier>(
            factory = createAndroidViewNodeFactory(factory),
            update = {
                updateViewHolderParams<T>(
                    modifier = materializedModifier,
                    density = density,
                    lifecycleOwner = lifecycleOwner,
                    savedStateRegistryOwner = savedStateRegistryOwner,
                    layoutDirection = layoutDirection,
                    compositionLocalMap = compositionLocalMap
                )
                set(update) { requireViewFactoryHolder<T>().updateBlock = it }
                set(onRelease) { requireViewFactoryHolder<T>().releaseBlock = it }
            }
        )
    }
}

@Composable
private fun <T : View> createAndroidViewNodeFactory(
    factory: (Context) -> T
): () -> LayoutNode {
    val context = LocalContext.current
    val parentReference = rememberCompositionContext()
    val stateRegistry = LocalSaveableStateRegistry.current
    val stateKey = currentCompositeKeyHash.toString()

    return {
        ViewFactoryHolder<T>(
            context = context,
            factory = factory,
            parentContext = parentReference,
            saveStateRegistry = stateRegistry,
            saveStateKey = stateKey
        ).layoutNode
    }
}

private fun <T : View> Updater<LayoutNode>.updateViewHolderParams(
    modifier: Modifier,
    density: Density,
    lifecycleOwner: LifecycleOwner,
    savedStateRegistryOwner: SavedStateRegistryOwner,
    layoutDirection: LayoutDirection,
    compositionLocalMap: CompositionLocalMap
) {
    set(compositionLocalMap, ComposeUiNode.SetResolvedCompositionLocals)
    set(modifier) { requireViewFactoryHolder<T>().modifier = it }
    set(density) { requireViewFactoryHolder<T>().density = it }
    set(lifecycleOwner) { requireViewFactoryHolder<T>().lifecycleOwner = it }
    set(savedStateRegistryOwner) {
        requireViewFactoryHolder<T>().savedStateRegistryOwner = it
    }
    set(layoutDirection) {
        requireViewFactoryHolder<T>().layoutDirection = when (it) {
            LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
            LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : View> LayoutNode.requireViewFactoryHolder(): ViewFactoryHolder<T> {
    return checkNotNull(interopViewFactoryHolder) as ViewFactoryHolder<T>
}

/**
 * An empty update block used by [AndroidView].
 */
val NoOpUpdate: View.() -> Unit = {}

internal class ViewFactoryHolder<T : View> private constructor(
    context: Context,
    parentContext: CompositionContext? = null,
    val typedView: T,
    // NestedScrollDispatcher that will be passed/used for nested scroll interop
    val dispatcher: NestedScrollDispatcher = NestedScrollDispatcher(),
    private val saveStateRegistry: SaveableStateRegistry?,
    private val saveStateKey: String
) : AndroidViewHolder(context, parentContext, dispatcher, typedView), ViewRootForInspector {

    constructor(
        context: Context,
        factory: (Context) -> T,
        parentContext: CompositionContext? = null,
        saveStateRegistry: SaveableStateRegistry?,
        saveStateKey: String
    ) : this(
        context = context,
        typedView = factory(context),
        parentContext = parentContext,
        saveStateRegistry = saveStateRegistry,
        saveStateKey = saveStateKey,
    )

    override val viewRoot: View get() = this

    private var saveableRegistryEntry: SaveableStateRegistry.Entry? = null
        set(value) {
            field?.unregister()
            field = value
        }

    init {
        clipChildren = false

        @Suppress("UNCHECKED_CAST")
        val savedState = saveStateRegistry
            ?.consumeRestored(saveStateKey) as? SparseArray<Parcelable>
        savedState?.let { typedView.restoreHierarchyState(it) }
        registerSaveStateProvider()
    }

    var updateBlock: (T) -> Unit = NoOpUpdate
        set(value) {
            field = value
            update = { typedView.apply(updateBlock) }
        }

    var resetBlock: (T) -> Unit = NoOpUpdate
        set(value) {
            field = value
            reset = { typedView.apply(resetBlock) }
        }

    var releaseBlock: (T) -> Unit = NoOpUpdate
        set(value) {
            field = value
            release = {
                typedView.apply(releaseBlock)
                unregisterSaveStateProvider()
            }
        }

    private fun registerSaveStateProvider() {
        if (saveStateRegistry != null) {
            saveableRegistryEntry = saveStateRegistry.registerProvider(saveStateKey) {
                SparseArray<Parcelable>().apply {
                    typedView.saveHierarchyState(this)
                }
            }
        }
    }

    private fun unregisterSaveStateProvider() {
        saveableRegistryEntry = null
    }
}