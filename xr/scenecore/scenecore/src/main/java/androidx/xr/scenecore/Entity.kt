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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore

import android.util.Log
import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.xr.runtime.internal.ActivityPose as RtActivityPose
import androidx.xr.runtime.internal.Entity as RtEntity
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

/**
 * Interface for a spatial Entity. An Entity's [Pose]s are represented as being relative to their
 * parent. Applications create and manage Entity instances to construct spatial scenes.
 */
public interface Entity : ScenePose {

    /**
     * The parent of this Entity, from which this Entity will inherit most of its properties. For
     * example, this Entity's [Pose] is defined in relation to the parent Entity's coordinate space,
     * so as the parent moves, this Entity will move with it. Setting the parent to null will remove
     * the Entity from the scene graph.
     */
    public var parent: Entity?

    /** Alternate text for this Entity to be consumed by Accessibility systems. */
    public var contentDescription: String

    /**
     * Sets an Entity to be a child of this Entity in the scene graph. The child Entity will inherit
     * properties from the parent, and will be represented in the parent's coordinate space. From a
     * User's perspective, as this Entity moves, the child Entity will move with it.
     *
     * @param child The [Entity] to be attached.
     */
    public fun addChild(child: Entity)

    /* TODO b/362296608: Add a getChildren() method. */

    /**
     * Sets the [Pose] for this Entity. The Pose given is set relative to the [Space] provided.
     *
     * @param pose The [Pose] offset from the parent.
     * @param relativeTo Set the pose relative to given Space. Default value is the parent space.
     */
    public fun setPose(pose: Pose, @SpaceValue relativeTo: Int = Space.PARENT)

    /** Sets the [Pose] for this Entity, relative to its parent. */
    public fun setPose(pose: Pose): Unit = setPose(pose, Space.PARENT)

    /**
     * Returns the [Pose] for this Entity, relative to the provided [Space].
     *
     * @param relativeTo Get the Pose relative to given Space. Default value is the parent space.
     * @return Current [Pose] of the Entity relative to the given space.
     */
    public fun getPose(@SpaceValue relativeTo: Int = Space.PARENT): Pose

    /**
     * Returns the [Pose] for this Entity, relative to its parent.
     *
     * @return Current [Pose] offset from the parent.
     */
    public fun getPose(): Pose = getPose(Space.PARENT)

    /**
     * Sets the scale of this Entity relative to given Space. This value will affect the rendering
     * of this Entity's children. As the scale increases, this will uniformly stretch the content of
     * the Entity.
     *
     * @param scale The uniform scale factor.
     * @param relativeTo Set the scale relative to given Space. Default value is the parent Space.
     */
    public fun setScale(
        @FloatRange(from = 0.0) scale: Float,
        @SpaceValue relativeTo: Int = Space.PARENT,
    )

    /**
     * Sets the scale of this Entity relative to its parent. This value will affect the rendering of
     * this Entity's children. As the scale increases, this will uniformly stretch the content of
     * the Entity.
     *
     * @param scale The uniform scale factor from the parent.
     */
    public fun setScale(@FloatRange(from = 0.0) scale: Float): Unit = setScale(scale, Space.PARENT)

    /**
     * Returns the scale of this entity, relative to given space.
     *
     * @param relativeTo Get the scale relative to given Space. Default value is the parent space.
     * @return Current uniform scale applied to self and children.
     */
    @FloatRange(from = 0.0) public fun getScale(@SpaceValue relativeTo: Int = Space.PARENT): Float

    /**
     * Returns the local scale of this Entity, not inclusive of the parent's scale.
     *
     * @return Current uniform scale applied to self and children.
     */
    @FloatRange(from = 0.0) public fun getScale(): Float = getScale(Space.PARENT)

    /**
     * Sets the alpha transparency of the Entity relative to given Space. Values are in the range
     * [0, 1] with 0 being fully transparent and 1 being fully opaque.
     *
     * This value will affect the rendering of this Entity's children. Children of this node will
     * have their alpha levels multiplied by this value and any alpha of this entity's ancestors.
     *
     * @param alpha Alpha transparency level for the Entity.
     * @param relativeTo Sets alpha relative to given Space. Default value is the parent Space.
     */
    // TODO - b/421456320: Can a child have an alpha greater than its parent?
    public fun setAlpha(
        @FloatRange(from = 0.0, to = 1.0) alpha: Float,
        @SpaceValue relativeTo: Int = Space.PARENT,
    )

    /**
     * Sets the alpha transparency of the Entity and its children. Values are in the range [0, 1]
     * with 0 being fully transparent and 1 being fully opaque.
     *
     * This value will affect the rendering of this Entity's children. Children of this node will
     * have their alpha levels multiplied by this value and any alpha of this Entity's ancestors.
     */
    public fun setAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float): Unit =
        setAlpha(alpha, Space.PARENT)

    /**
     * Returns the alpha transparency set for this Entity, relative to given Space.
     *
     * @param relativeTo Gets alpha relative to given Space. Default value is the parent space.
     */
    @FloatRange(from = 0.0, to = 1.0)
    public fun getAlpha(@SpaceValue relativeTo: Int = Space.PARENT): Float

    /**
     * Returns the alpha transparency set for this Entity.
     *
     * This does not necessarily equal the perceived alpha of the Entity, as the Entity may have
     * some alpha difference applied from its parent or the system.
     */
    @FloatRange(from = 0.0, to = 1.0) public fun getAlpha(): Float = getAlpha(Space.PARENT)

    /**
     * Sets the local enabled state of this Entity.
     *
     * When `false`, this Entity and all descendants will not be rendered in the scene, and the
     * Entity will not respond to input events. If an Entity's local enabled state is `true`, the
     * Entity will still be considered not enabled if at least one of its ancestors is not enabled.
     *
     * @param enabled The new local enabled state of this Entity.
     */
    @Suppress("Deprecation") public fun setEnabled(enabled: Boolean): Unit = setHidden(!enabled)

    /**
     * Returns the enabled status of this Entity.
     *
     * @param includeParents Whether to include the enabled status of parents in the returned value.
     * @return If includeParents is `true`, the returned value will be `true` if this Entity or any
     *   of its ancestors is enabled. If includeParents is `false`, the local enabled state is
     *   returned. Regardless of the local enabled state, an Entity will be considered disabled if
     *   any of its ancestors are disabled.
     */
    @Suppress("Deprecation")
    public fun isEnabled(includeParents: Boolean = true): Boolean = !isHidden(includeParents)

    /**
     * Sets the local hidden state of this Entity. When `true`, this Entity and all descendants will
     * not be rendered in the scene. When the hidden state is `false`, an entity will be rendered if
     * its ancestors are not hidden.
     *
     * @param hidden The new local hidden state of this Entity.
     */
    @Deprecated("Use setEnabled(Boolean)")
    // TODO - b/421386891: remove this deprecated method
    public fun setHidden(hidden: Boolean)

    /**
     * Returns the hidden status of this Entity.
     *
     * @param includeParents Whether to include the hidden status of parents in the returned value.
     * @return If includeParents is `true`, the returned value will be `true` if this Entity or any
     *   of its ancestors is hidden. If includeParents is `false`, the local hidden state is
     *   returned. Regardless of the local hidden state, an Entity will not be rendered if any of
     *   its ancestors are hidden.
     */
    @Deprecated("Use isEnabled(Boolean)")
    // TODO - b/421386891: remove this method
    public fun isHidden(includeParents: Boolean = true): Boolean

    /**
     * Disposes of any system resources held by this Entity, and transitively calls dispose() on all
     * its children. Once disposed, this Entity is invalid and cannot be used again.
     */
    public fun dispose()

    /**
     * Adds a Component to this Entity.
     *
     * @param component the Component to be added to the Entity.
     * @return True if given Component was successfully added to the Entity.
     */
    // TODO: b/428196727 - Consider a better indication of failures.
    public fun addComponent(component: Component): Boolean

    /**
     * Removes the given Component from this Entity.
     *
     * @param component Component to be removed from this entity.
     */
    public fun removeComponent(component: Component)

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

    /** Remove all components from this Entity. */
    public fun removeAllComponents()
}

/** The BaseEntity is an implementation of Entity interface that wraps a platform entity. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Suppress("HiddenSuperClass")
public abstract class BaseEntity<out RtEntityType : RtEntity>
internal constructor(
    internal val rtEntity: RtEntityType,
    private val entityManager: EntityManager,
) : Entity, BaseScenePose<RtActivityPose>(rtEntity) {

    init {
        entityManager.setEntityForRtEntity(rtEntity, this)
    }

    private companion object {
        private const val TAG = "BaseEntity"
    }

    private val componentList = mutableListOf<Component>()

    override var contentDescription: String
        get() = rtEntity.contentDescription
        set(value) {
            rtEntity.contentDescription = value
        }

    override var parent: Entity?
        get() = rtEntity.parent?.let { entityManager.getEntityForRtEntity(it) }
        set(value) {
            if (value == null) {
                rtEntity.parent = null
                return
            }

            if (value !is BaseEntity<RtEntity>) {
                Log.e(TAG, "Parent must be a subclass of BaseEntity")
                return
            }
            rtEntity.parent = value.rtEntity
        }

    override fun addChild(child: Entity) {
        if (child !is BaseEntity<RtEntity>) {
            Log.e(TAG, "Child must be a subclass of BaseEntity!")
            return
        }
        rtEntity.addChild(child.rtEntity)
    }

    override fun setPose(pose: Pose, @SpaceValue relativeTo: Int) {
        rtEntity.setPose(pose, relativeTo.toRtSpace())
    }

    override fun getPose(@SpaceValue relativeTo: Int): Pose {
        return rtEntity.getPose(relativeTo.toRtSpace())
    }

    override fun setScale(scale: Float, relativeTo: Int) {
        rtEntity.setScale(Vector3(scale, scale, scale), relativeTo.toRtSpace())
    }

    override fun getScale(@SpaceValue relativeTo: Int): Float {
        return rtEntity.getScale(relativeTo.toRtSpace()).x
    }

    override fun setAlpha(alpha: Float, @SpaceValue relativeTo: Int) {
        rtEntity.setAlpha(alpha, relativeTo.toRtSpace())
    }

    override fun getAlpha(@SpaceValue relativeTo: Int): Float =
        rtEntity.getAlpha(relativeTo.toRtSpace())

    @Deprecated("Use setEnabled(Boolean)")
    override fun setHidden(hidden: Boolean): Unit = rtEntity.setHidden(hidden)

    @Deprecated("Use isEnabled(Boolean)")
    override fun isHidden(includeParents: Boolean): Boolean = rtEntity.isHidden(includeParents)

    override fun dispose() {
        removeAllComponents()
        entityManager.removeEntity(this)
        rtEntity.dispose()
        // TODO b/427314036: Set rtEntity to null here and add checkDisposed() to all public
        //                   methods.
    }

    override fun addComponent(component: Component): Boolean {
        if (component.onAttach(this)) {
            componentList.add(component)
            return true
        }
        return false
    }

    override fun removeComponent(component: Component) {
        if (componentList.contains(component)) {
            component.onDetach(this)
            componentList.remove(component)
        }
    }

    override fun <T : Component> getComponentsOfType(type: Class<out T>): List<T> {
        return componentList.filterIsInstance(type)
    }

    override fun getComponents(): List<Component> {
        return componentList
    }

    override fun removeAllComponents() {
        componentList.forEach { it.onDetach(this) }
        componentList.clear()
    }
}
