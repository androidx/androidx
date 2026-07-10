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

package androidx.xr.compose.platform

import androidx.annotation.CallSuper

/**
 * Represents the basic building block for a [SpatialElement] that can contain other
 * [SpatialElement] instances.
 *
 * It provides functionality for managing child SpatialElements and for attaching and detaching from
 * a [SpatialComposeScene].
 */
internal open class SpatialElement {

    /**
     * Interface definition for callbacks that are invoked when this element's attachment state
     * changes.
     */
    public interface OnAttachStateChangeListener {
        /**
         * Called when the [SpatialElement] is attached to a [SpatialComposeScene].
         *
         * @param spatialComposeScene The [SpatialComposeScene] that the element was attached to.
         */
        public fun onElementAttachedToSubspace(spatialComposeScene: SpatialComposeScene) {}

        /**
         * Called when the [SpatialElement] is detached from a [SpatialComposeScene].
         *
         * @param spatialComposeScene The [SpatialComposeScene] that the element was detached from.
         */
        public fun onElementDetachedFromSubspace(spatialComposeScene: SpatialComposeScene) {}
    }

    /**
     * The parent of this [SpatialElement].
     *
     * This property is `null` if the element is not attached to a parent [SpatialElement].
     */
    public var parent: SpatialElement? = null
        internal set(parentEl) {
            field = parentEl
            spatialComposeScene = parentEl?.spatialComposeScene
        }

    /**
     * The [SpatialComposeScene] that this element is attached to, or `null` if it is not attached
     * to any subspace.
     */
    public var spatialComposeScene: SpatialComposeScene? = null
        set(value) {
            if (field == value) {
                return
            }

            if (value == null) {
                val oldScene = checkNotNull(field) { "Scene must be non-null before clearing." }
                onDetachedFromSubspace(oldScene)
                onAttachStateChangeListeners?.forEach { it.onElementDetachedFromSubspace(oldScene) }
            } else {
                onAttachedToSubspace(value)
                onAttachStateChangeListeners?.forEach { it.onElementAttachedToSubspace(value) }
            }
            field = value
        }

    /** Whether the element is attached to a [SpatialComposeScene]. */
    protected val isAttachedToSpatialComposeScene: Boolean
        get() = spatialComposeScene != null

    private var onAttachStateChangeListeners: MutableList<OnAttachStateChangeListener>? = null

    /**
     * Detaches this element from its parent [SpatialElement], setting the [parent] to `null` and
     * removing from parent's [SpatialElement.children] list.
     */
    private fun detachFromParent() {
        @Suppress("UNUSED_VARIABLE") val unused = parent?.removeChild(this)
        parent = null
    }

    /**
     * Registers the [listener] whose callbacks are invoked when this [SpatialElement]'s attachment
     * state to [SpatialComposeScene] changes.
     *
     * Use [removeOnAttachStateChangeListener] to unregister the [listener].
     *
     * @param listener The listener to be registered.
     */
    @Suppress("ExecutorRegistration")
    public fun addOnAttachStateChangeListener(listener: OnAttachStateChangeListener) {
        val listeners = onAttachStateChangeListeners

        if (listeners == null) {
            onAttachStateChangeListeners = mutableListOf(listener)
        } else {
            listeners.add(listener)
        }
    }

    /**
     * Registers a one-time callback to be invoked when this [SpatialElement] is detached from the
     * [SpatialComposeScene].
     *
     * @param listener the callback to be invoked when the element is detached.
     */
    public fun onDetachedFromSubspaceOnce(listener: (SpatialComposeScene) -> Unit) {
        addOnAttachStateChangeListener(
            object : OnAttachStateChangeListener {
                override fun onElementDetachedFromSubspace(
                    spatialComposeScene: SpatialComposeScene
                ) {
                    removeOnAttachStateChangeListener(this)
                    listener(spatialComposeScene)
                }
            }
        )
    }

    /**
     * Removes and unregisters the previously registered [listener].
     *
     * The [listener] will no longer receive any further notification whenever [spatialComposeScene]
     * attachment changes.
     *
     * @param listener The listener to be removed.
     */
    public fun removeOnAttachStateChangeListener(listener: OnAttachStateChangeListener) {
        onAttachStateChangeListeners?.remove(listener)
    }

    private val _children = mutableListOf<SpatialElement>()

    /** Immediate children of this [SpatialElement]. */
    public val children: List<SpatialElement>
        get() = _children

    /**
     * Appends the given [element] to the [children] of this element.
     *
     * @param element the element to add as a child.
     */
    @CallSuper
    public open fun addChild(element: SpatialElement) {
        element.parent = this
        _children.add(element)
    }

    /**
     * Removes the given [element] from the [children] of this element.
     *
     * @param element the element to remove as a child.
     * @return `true` if element was successfully removed from the list, `false` otherwise.
     */
    @CallSuper
    public open fun removeChild(element: SpatialElement): Boolean {
        if (_children.remove(element)) {
            element.parent = null
            return true
        }

        return false
    }

    /** Detaches all of its children and clears the [children] list. */
    @CallSuper
    public open fun removeChildren() {
        _children.forEach { it.parent = null }
        _children.clear()
    }

    /**
     * Update children's spatialComposeScene to [SpatialComposeScene] when this element is attached
     * to it.
     *
     * @param spatialComposeScene the [SpatialComposeScene] this element is attached to.
     */
    @CallSuper
    public open fun onAttachedToSubspace(spatialComposeScene: SpatialComposeScene) {
        // Make sure all children have the same `spatialComposeScene` reference too.
        _children.forEach { it.spatialComposeScene = spatialComposeScene }
    }

    /**
     * Called when this element is detached from a [SpatialComposeScene].
     *
     * @param spatialComposeScene the [SpatialComposeScene] this element is detached from.
     */
    @CallSuper
    public open fun onDetachedFromSubspace(spatialComposeScene: SpatialComposeScene) {
        // make sure `spatialComposeScene` references of all children are cleaned up too.
        _children.forEach { it.spatialComposeScene = null }
    }
}
