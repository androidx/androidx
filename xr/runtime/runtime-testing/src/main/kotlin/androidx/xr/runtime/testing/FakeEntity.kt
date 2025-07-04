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

package androidx.xr.runtime.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.Component
import androidx.xr.runtime.internal.Entity
import androidx.xr.runtime.internal.InputEventListener
import androidx.xr.runtime.internal.SpaceValue
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import java.util.Collections
import java.util.concurrent.Executor

/**
 * A test double for [Entity], designed for use in unit or integration tests.
 *
 * This test double offers greater control compared to the real [Entity] by allowing:
 * * Direct modification of most properties to simulate specific scenarios or states.
 * * Mocking of hit test results for predictable and verifiable interaction testing.
 *
 * @see Entity
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public open class FakeEntity() : FakeActivityPose(), Entity {

    private val _children = mutableSetOf<Entity>()

    /** Sets the provided Entities to be children of the Entity. */
    override val children: List<Entity>
        get() = _children.toList()

    private var _parent: Entity? = null

    /**
     * Sets the parent Entity for this Entity. The child Entity's pose will be relative to the pose
     * of its parent.
     *
     * @param parent The parent entity.
     */
    override var parent: Entity?
        get() = _parent
        set(value) {
            if (_parent == value) {
                return
            }

            (_parent as? FakeEntity)?.removeChildInternal(this)
            _parent = value
            (_parent as? FakeEntity)?.addChildInternal(this)
        }

    /** Sets context-text for this entity to be consumed by Accessibility systems. */
    override var contentDescription: String = ""

    private var hidden = false

    /**
     * Sets the local hidden state of this Entity. When true, this Entity and all descendants will
     * not be rendered in the scene. When the hidden state is false, an entity will be rendered if
     * its ancestors are not hidden.
     *
     * @param hidden The new local hidden state of this Entity.
     */
    override fun setHidden(hidden: Boolean) {
        this.hidden = hidden
    }

    /**
     * Add given Entity as child. The child Entity's pose will be relative to the pose of its parent
     *
     * @param child The child entity.
     */
    override fun addChild(child: Entity) {
        child.parent = this
    }

    private var pose: Pose = Pose.Identity

    /** Returns the pose for this entity, relative to the given space. */
    override fun getPose(@SpaceValue relativeTo: Int): Pose {
        return pose
    }

    /** Updates the pose (position and rotation) of the Entity relative to the given space. */
    override fun setPose(pose: Pose, @SpaceValue relativeTo: Int) {
        this.pose = pose
    }

    private var scale: Vector3 = Vector3.One

    /**
     * Returns the scale of this entity, relative to the given space.
     *
     * @return Current [Vector3] scale relative to the given space.
     */
    override fun getScale(@SpaceValue relativeTo: Int): Vector3 {
        return scale
    }

    /**
     * Sets the scale of this entity relative to the given space. This value will affect the
     * rendering of this Entity's children. As the scale increases, this will stretch the content of
     * the Entity.
     *
     * @param scale The [Vector3] scale factor relative to the given space.
     * @param relativeTo The space in which to set the scale.
     */
    override fun setScale(scale: Vector3, @SpaceValue relativeTo: Int) {
        if (scale.x > 0 && scale.y > 0 && scale.z > 0) {
            this.scale = scale
        }
    }

    private var alpha = 1.0f

    /**
     * Returns the effective alpha transparency level of the entity, relative to the given space.
     *
     * @param relativeTo The space in which to evaluate the alpha.
     */
    override fun getAlpha(@SpaceValue relativeTo: Int): Float {
        return alpha
    }

    /**
     * Sets the alpha transparency for the given Entity, relative to the given space.
     *
     * @param alpha Alpha transparency level for the Entity.
     * @param relativeTo The space in which to set the alpha.
     */
    override fun setAlpha(alpha: Float, @SpaceValue relativeTo: Int) {
        // make sure input alpha with in range [0, 1]
        val clampedAlpha = alpha.coerceIn(0f, 1f)
        this.alpha = clampedAlpha
    }

    /** Sets the provided Entities to be children of the Entity. */
    override fun addChildren(children: List<Entity>) {
        for (child in children) {
            addChild(child)
        }
    }

    /**
     * Returns the hidden status of this Entity.
     *
     * @param includeParents Whether to include the hidden status of parents in the returned value.
     * @return If includeParents is true, the returned value will be true if this Entity or any of
     *   its ancestors is hidden. If includeParents is false, the local hidden state is returned.
     *   Regardless of the local hidden state, an entity will not be rendered if any of its
     *   ancestors are hidden.
     */
    override fun isHidden(includeParents: Boolean): Boolean {
        if (!includeParents || _parent == null) {
            return hidden
        }
        return hidden || _parent!!.isHidden(true)
    }

    /**
     * For test purposes only.
     *
     * The map of input event listeners to their executors.
     */
    public val inputEventListenerMap: MutableMap<InputEventListener, Executor> =
        Collections.synchronizedMap(mutableMapOf())

    /**
     * Adds the listener to the set of active input listeners, for input events targeted to this
     * entity or its child entities.
     *
     * @param executor The executor to run the listener on.
     * @param listener The input event listener to add.
     */
    @Suppress("ExecutorRegistration")
    override fun addInputEventListener(executor: Executor, listener: InputEventListener) {
        inputEventListenerMap.put(listener, executor)
    }

    /** Removes the given listener from the set of active input listeners. */
    override fun removeInputEventListener(listener: InputEventListener) {
        inputEventListenerMap.remove(listener)
    }

    /**
     * Dispose any system resources held by this entity, and transitively calls dispose() on all the
     * children. Once disposed, Entity shouldn't be used again.
     */
    override fun dispose() {
        inputEventListenerMap.clear()
        parent = null
        val childrenToDispose = _children.toList()
        childrenToDispose.forEach { it.dispose() }
        removeAllComponents()
    }

    private val componentList: MutableList<Component> = mutableListOf()

    /**
     * Add given component to entity.
     *
     * @param component Component to add to the Entity.
     * @return True if the given component is added to the Entity.
     */
    override fun addComponent(component: Component): Boolean {
        if (component.onAttach(this)) {
            componentList.add(component)
            return true
        }
        return false
    }

    /**
     * Retrieves all Components of the given type [T] and its sub-types attached to this Entity.
     *
     * @param type The type of Component to retrieve.
     * @return List<Component> of the given type attached to this Entity.
     */
    public override fun <T : Component> getComponentsOfType(type: Class<out T>): List<T> {
        return componentList.filterIsInstance(type)
    }

    /**
     * Retrieves all components attached to this Entity.
     *
     * @return List<Component> attached to this Entity.
     */
    override fun getComponents(): List<Component> = componentList

    /**
     * Remove the given component from the entity.
     *
     * @param component Component to remove from the entity.
     */
    override fun removeComponent(component: Component) {
        if (componentList.contains(component)) {
            component.onDetach(this)
            componentList.remove(component)
        }
    }

    /** Remove all components from this entity. */
    override fun removeAllComponents() {
        for (component in componentList) {
            component.onDetach(this)
        }
        componentList.clear()
    }

    private fun addChildInternal(child: Entity) {
        _children.add(child)
    }

    private fun removeChildInternal(child: Entity) {
        _children.remove(child)
    }
}
