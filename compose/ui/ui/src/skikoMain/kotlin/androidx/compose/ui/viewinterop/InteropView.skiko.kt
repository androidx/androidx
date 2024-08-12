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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionLocalMap
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.Updater
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.materialize
import androidx.compose.ui.node.ComposeUiNode.Companion.SetCompositeKeyHash
import androidx.compose.ui.node.ComposeUiNode.Companion.SetResolvedCompositionLocals
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.DefaultUiApplier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val NoOp: Any.() -> Unit = {}

/**
 * Base class for any concrete implementation of [InteropViewHolder] that holds a specific type
 * of InteropView to be implemented by the platform-specific [TypedInteropViewHolder] subclass
 */
internal abstract class TypedInteropViewHolder<T : InteropView>(
    factory: () -> T,
    interopContainer: InteropContainer,
    group: InteropViewGroup,
    compositeKeyHash: Int,
    measurePolicy: MeasurePolicy,
    isInteractive: Boolean,
    platformModifier: Modifier
) : InteropViewHolder(
    interopContainer,
    group,
    compositeKeyHash,
    measurePolicy,
    isInteractive,
    platformModifier
) {
    protected val typedInteropView = factory()

    override fun getInteropView(): InteropView? {
        return typedInteropView
    }

    /**
     * A block containing the update logic for [T], to be forwarded to user.
     * Setting it will schedule an update immediately.
     * See [InteropViewHolder.update]
     */
    var updateBlock: (T) -> Unit = NoOp
        set(value) {
            field = value
            update = { typedInteropView.apply(updateBlock) }
        }

    /**
     * A block containing the reset logic for [T], to be forwarded to user.
     * It will be called if [LayoutNode] associated with this [InteropViewHolder] is reused to
     * avoid interop view reallocation.
     */
    var resetBlock: (T) -> Unit = NoOp
        set(value) {
            field = value
            reset = { typedInteropView.apply(resetBlock) }
        }

    /**
     * A block containing the release logic for [T], to be forwarded to user.
     * It will be called if [LayoutNode] associated with this [InteropViewHolder] is released.
     */
    var releaseBlock: (T) -> Unit = NoOp
        set(value) {
            field = value
            release = {
                typedInteropView.apply(releaseBlock)
            }
        }
}

/**
 * Create a [LayoutNode] factory that can be constructed from [TypedInteropViewHolder] built with
 * the [currentCompositeKeyHash]
 *
 * @see [AndroidView.android.kt:createAndroidViewNodeFactory]
 */
@Composable
private fun <T : InteropView> createInteropViewLayoutNodeFactory(
    factory: (compositeKeyHash: Int) -> TypedInteropViewHolder<T>
): () -> LayoutNode {
    val compositeKeyHash = currentCompositeKeyHash

    return {
        factory(compositeKeyHash).layoutNode
    }
}

/**
 * Entry point for creating a composable that wraps a platform specific interop view.
 * Platform implementations should call it and provide the appropriate factory, returning
 * a subclass of [TypedInteropViewHolder].
 *
 * @see [AndroidView.android.kt:AndroidView]
 */
@Composable
@UiComposable
internal fun <T : InteropView> InteropView(
    factory: (compositeKeyHash: Int) -> TypedInteropViewHolder<T>,
    modifier: Modifier,
    onReset: ((T) -> Unit)? = null,
    onRelease: (T) -> Unit = NoOp,
    update: (T) -> Unit = NoOp
) {
    val compositeKeyHash = currentCompositeKeyHash
    val materializedModifier = currentComposer.materialize(modifier)
    val density = LocalDensity.current
    val compositionLocalMap = currentComposer.currentCompositionLocalMap

    // TODO: there are other parameters on Android that we don't yet use:
    //  lifecycleOwner, savedStateRegistryOwner, layoutDirection
    if (onReset == null) {
        ComposeNode<LayoutNode, DefaultUiApplier>(
            factory = createInteropViewLayoutNodeFactory(factory),
            update = {
                updateParameters<T>(
                    compositionLocalMap,
                    materializedModifier,
                    density,
                    compositeKeyHash
                )
                set(update) { requireViewFactoryHolder<T>().updateBlock = it }
                set(onRelease) { requireViewFactoryHolder<T>().releaseBlock = it }
            }
        )
    } else {
        ReusableComposeNode<LayoutNode, DefaultUiApplier>(
            factory = createInteropViewLayoutNodeFactory(factory),
            update = {
                updateParameters<T>(
                    compositionLocalMap,
                    materializedModifier,
                    density,
                    compositeKeyHash
                )
                set(onReset) { requireViewFactoryHolder<T>().resetBlock = it }
                set(update) { requireViewFactoryHolder<T>().updateBlock = it }
                set(onRelease) { requireViewFactoryHolder<T>().releaseBlock = it }
            }
        )
    }
}

/**
 * Updates the parameters of the [LayoutNode] in the current [Updater] with the given values.
 * @see [AndroidView.android.kt:updateViewHolderParams]
 */
private fun <T : InteropView> Updater<LayoutNode>.updateParameters(
    compositionLocalMap: CompositionLocalMap,
    modifier: Modifier,
    density: Density,
    compositeKeyHash: Int
) {
    set(compositionLocalMap, SetResolvedCompositionLocals)
    set(modifier) { requireViewFactoryHolder<T>().modifier = it }
    set(density) { requireViewFactoryHolder<T>().density = it }
    set(compositeKeyHash, SetCompositeKeyHash)
}

/**
 * Returns the [TypedInteropViewHolder] associated with the current [LayoutNode].
 * Since the [TypedInteropViewHolder] is responsible for constructing the [LayoutNode], it
 * associates itself with the [LayoutNode] by setting the [LayoutNode.interopViewFactoryHolder]
 * property and it's safe to cast from [InteropViewHolder]
 */
@Suppress("UNCHECKED_CAST")
private fun <T : InteropView> LayoutNode.requireViewFactoryHolder(): TypedInteropViewHolder<T> {
    // This LayoutNode is created and managed internally here, so it's safe to cast
    return checkNotNull(interopViewFactoryHolder) as TypedInteropViewHolder<T>
}

