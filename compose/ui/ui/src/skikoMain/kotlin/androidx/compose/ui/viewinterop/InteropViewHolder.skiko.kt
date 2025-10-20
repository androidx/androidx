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

import androidx.compose.runtime.CompositeKeyHashCode
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.unit.Density
import kotlin.jvm.JvmName

/**
 * A holder that keeps references to user interop view and its group (container).
 * It's the actual implementation of `expect class [InteropViewFactoryHolder]`
 *
 * @see InteropViewFactoryHolder
 */
internal abstract class InteropViewHolder(
    val container: InteropContainer,
    open val group: InteropViewGroup,
    private val compositeKeyHashCode: CompositeKeyHashCode,
) : InteropViewFactoryHolder() {
    private var onModifierChanged: (() -> Unit)? = null

    /**
     * User-provided modifier that will be reapplied if changed.
     */
    var modifier: Modifier = Modifier
        set(value) {
            if (value !== field) {
                field = value
                onModifierChanged?.invoke()
            }
        }

    /**
     * Modifier provided by the platform-specific holder.
     */
    protected var platformModifier: Modifier = Modifier
        set(value) {
            if (value !== field) {
                field = value
                onModifierChanged?.invoke()
            }
        }

    protected abstract val measurePolicy: MeasurePolicy

    private var hasUpdateBlock = false

    var update: () -> Unit = {}
        protected set(value) {
            field = value
            hasUpdateBlock = true
            runUpdate()
        }

    protected var reset: () -> Unit = {}

    protected var release: () -> Unit = {}

    private var onDensityChanged: ((Density) -> Unit)? = null
    var density: Density = Density(1f)
        set(value) {
            if (value !== field) {
                field = value
                onDensityChanged?.invoke(value)
            }
        }

    /**
     * If the view is not attached, update on closure change (or on setting initial one) will
     * be postponed until it's attached and triggered when this flag is set to `true`.
     *
     * If the view is detached, the observer is stopped to avoid redundant callbacks.
     */
    private var isAttachedToWindow: Boolean = false
        set(value) {
            if (value != field) {
                field = value

                if (value) {
                    runUpdate()
                } else {
                    snapshotObserver.clear(this)
                }
            }
        }

    private val snapshotObserver: SnapshotStateObserver
        get() {
            return container.snapshotObserver
        }

    /**
     * If we're not attached, the observer won't be started in scope of this object. It will be run
     * after [insertInteropView] is called.
     *
     * Dispatch scheduling strategy is defined by platform implementation of
     * [InteropContainer.scheduleUpdate].
     */
    private val runUpdate: () -> Unit = {
        if (hasUpdateBlock && isAttachedToWindow) {
            snapshotObserver.observeReads(this, DispatchUpdateUsingContainerStrategy, update)
        }
    }

    /**
     * Construct a [LayoutNode] that is linked to this [InteropViewHolder].
     */
    val layoutNode: LayoutNode by lazy {
        val layoutNode = LayoutNode()

        layoutNode.interopViewFactoryHolder = this

        val coreModifier = Modifier
            .trackInteropPlacement(this)
            .onGloballyPositioned { layoutCoordinates ->
                layoutAccordingTo(layoutCoordinates)
                // TODO: Should be the same as [Owner.onInteropViewLayoutChange]?
                // container.onInteropViewLayoutChange(this)
            }

        layoutNode.compositeKeyHash = compositeKeyHashCode.hashCode()

        layoutNode.modifier = modifier then platformModifier then coreModifier

        onModifierChanged = {
            layoutNode.modifier = modifier then platformModifier then coreModifier
        }

        layoutNode.density = density
        onDensityChanged = { layoutNode.density = it }

        layoutNode.measurePolicy = measurePolicy

        layoutNode
    }

    fun place() {
        container.place(this)
    }

    fun unplace() {
        if (!container.contains(this)) {
            // TODO: remove when unplace is called only once
            return
        }

        container.unplace(this)
    }

    /**
     * Must be called by implementations when the interop view is attached to the window.
     */
    open fun insertInteropView(root: InteropViewGroup, index: Int) {
        isAttachedToWindow = true
    }

    /**
     * Must be called by implementations when the interop view is detached from the window.
     */
    open fun removeInteropView(root: InteropViewGroup) {
        isAttachedToWindow = false
    }

    // ===== Abstract methods to be implemented by platform-specific subclasses =====

    abstract fun changeInteropViewIndex(root: InteropViewGroup, index: Int)

    /**
     * Dispatches the pointer event to the interop view.
     */
    abstract fun dispatchToView(pointerEvent: PointerEvent)

    /**
     * Layout the interop view according to the given layout coordinates.
     */
    abstract fun layoutAccordingTo(layoutCoordinates: LayoutCoordinates)

    /**
     * Returns the actual interop view instance.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("interopView")
    abstract val interopView: InteropView

    // ===== InteropViewFactoryHolder implementation =====

    override fun onReuse() = container.scheduleUpdate {
        reset()
    }

    override fun onDeactivate() {
        // TODO: Android calls [reset] here, but it's not clear why it's needed, because
        //  [onReuse] will be called after [onDeactivate] if the holder is indeed reused.
        //  discuss it with Google when this code is commonized
    }

    override fun onRelease() = container.scheduleUpdate {
        release()
    }

    // Keep nullable to match the `expect` declaration of InteropViewFactoryHolder
    override fun getInteropView(): InteropView? = interopView

    companion object {
        private val DispatchUpdateUsingContainerStrategy: (InteropViewHolder) -> Unit = {
            it.container.scheduleUpdate { it.update() }
        }
    }
}