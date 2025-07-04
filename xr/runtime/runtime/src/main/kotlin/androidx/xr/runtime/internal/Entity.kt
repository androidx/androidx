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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import java.util.concurrent.Executor

/** Interface for an XR Runtime Entity. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface Entity : ActivityPose {

    /** Sets the provided Entities to be children of the Entity. */
    public val children: List<Entity>

    /**
     * Sets the parent Entity for this Entity. The child Entity's pose will be relative to the pose
     * of its parent.
     *
     * @param parent The parent entity.
     */
    public var parent: Entity?

    /** Sets context-text for this entity to be consumed by Accessibility systems. */
    public var contentDescription: String

    /**
     * Sets the local hidden state of this Entity. When true, this Entity and all descendants will
     * not be rendered in the scene. When the hidden state is false, an entity will be rendered if
     * its ancestors are not hidden.
     *
     * @param hidden The new local hidden state of this Entity.
     */
    public fun setHidden(hidden: Boolean): Unit

    /**
     * Add given Entity as child. The child Entity's pose will be relative to the pose of its parent
     *
     * @param child The child entity.
     */
    public fun addChild(child: Entity)

    /** Returns the pose for this entity, relative to the given space. */
    public fun getPose(@SpaceValue relativeTo: Int): Pose

    /** Returns the pose for this entity, relative to its parent. */
    public fun getPose(): Pose = getPose(Space.PARENT)

    /** Updates the pose (position and rotation) of the Entity relative to the given space. */
    public fun setPose(pose: Pose, @SpaceValue relativeTo: Int)

    /** Updates the pose (position and rotation) of the Entity relative to its parent. */
    public fun setPose(pose: Pose): Unit = setPose(pose, Space.PARENT)

    /**
     * Returns the scale of this entity, relative to the given space.
     *
     * @return Current [Vector3] scale relative to the given space.
     */
    public fun getScale(@SpaceValue relativeTo: Int): Vector3

    /**
     * Returns the scale of this entity, relative to its parent.
     *
     * @return Current [Vector3] scale relative to the parent.
     */
    public fun getScale(): Vector3 = getScale(Space.PARENT)

    /**
     * Sets the scale of this entity relative to the given space. This value will affect the
     * rendering of this Entity's children. As the scale increases, this will stretch the content of
     * the Entity.
     *
     * @param scale The [Vector3] scale factor relative to the given space.
     * @param relativeTo The space in which to set the scale.
     */
    public fun setScale(scale: Vector3, @SpaceValue relativeTo: Int)

    /**
     * Sets the scale of this entity relative to its parent. This value will affect the rendering of
     * this Entity's children. As the scale increases, this will stretch the content of the Entity.
     *
     * @param scale The [Vector3] scale factor from the parent.
     */
    public fun setScale(scale: Vector3): Unit = setScale(scale, Space.PARENT)

    /**
     * Returns the effective alpha transparency level of the entity, relative to the given space.
     *
     * @param relativeTo The space in which to evaluate the alpha.
     */
    public fun getAlpha(@SpaceValue relativeTo: Int): Float

    /** Returns the set alpha transparency level for this Entity. */
    public fun getAlpha(): Float = getAlpha(Space.PARENT)

    /**
     * Sets the alpha transparency for the given Entity, relative to the given space.
     *
     * @param alpha Alpha transparency level for the Entity.
     * @param relativeTo The space in which to set the alpha.
     */
    public fun setAlpha(alpha: Float, @SpaceValue relativeTo: Int)

    /**
     * Sets the alpha transparency for the given Entity.
     *
     * @param alpha Alpha transparency level for the Entity.
     */
    public fun setAlpha(alpha: Float): Unit = setAlpha(alpha, Space.PARENT)

    /** Sets the provided Entities to be children of the Entity. */
    public fun addChildren(children: List<Entity>): Unit

    /**
     * Returns the hidden status of this Entity.
     *
     * @param includeParents Whether to include the hidden status of parents in the returned value.
     * @return If includeParents is true, the returned value will be true if this Entity or any of
     *   its ancestors is hidden. If includeParents is false, the local hidden state is returned.
     *   Regardless of the local hidden state, an entity will not be rendered if any of its
     *   ancestors are hidden.
     */
    public fun isHidden(includeParents: Boolean): Boolean

    /**
     * Adds the listener to the set of active input listeners, for input events targeted to this
     * entity or its child entities.
     *
     * @param executor The executor to run the listener on.
     * @param listener The input event listener to add.
     */
    @Suppress("ExecutorRegistration")
    public fun addInputEventListener(executor: Executor, listener: InputEventListener)

    /** Removes the given listener from the set of active input listeners. */
    public fun removeInputEventListener(listener: InputEventListener)

    /**
     * Dispose any system resources held by this entity, and transitively calls dispose() on all the
     * children. Once disposed, Entity shouldn't be used again.
     */
    public fun dispose()

    /**
     * Add given component to entity.
     *
     * @param component Component to add to the Entity.
     * @return True if the given component is added to the Entity.
     */
    public fun addComponent(component: Component): Boolean

    /**
     * Retrieves all Components of the given type [T] and its sub-types attached to this Entity.
     *
     * @param type The type of Component to retrieve.
     * @return List<Component> of the given type attached to this Entity.
     */
    public fun <T : Component> getComponentsOfType(type: Class<out T>): List<T>

    /**
     * Retrieves all components attached to this Entity.
     *
     * @return List<Component> attached to this Entity.
     */
    public fun getComponents(): List<Component>

    /**
     * Remove the given component from the entity.
     *
     * @param component Component to remove from the entity.
     */
    public fun removeComponent(component: Component)

    /** Remove all components from this entity. */
    public fun removeAllComponents()
}
