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

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.runtime.XrLog
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.CleanupAction
import androidx.xr.scenecore.runtime.Entity as RtEntity
import androidx.xr.scenecore.runtime.HandlerExecutor
import androidx.xr.scenecore.runtime.ReferenceCleaner
import androidx.xr.scenecore.runtime.ScenePose as RtScenePose
import androidx.xr.scenecore.runtime.SceneRuntime
import java.util.concurrent.atomic.AtomicReference

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
    public var contentDescription: CharSequence

    /**
     * Sets an Entity to be a child of this Entity in the scene graph. The child Entity will inherit
     * properties from the parent, and will be represented in the parent's coordinate space. From a
     * User's perspective, as this Entity moves, the child Entity will move with it.
     *
     * @param child The [Entity] to be attached.
     */
    public fun addChild(child: Entity)

    /** Provides the list of all children of this entity. */
    public val children: List<Entity>

    /**
     * Sets the [Pose] for this Entity. The Pose given is set relative to the [Space] provided.
     *
     * @param pose The [Pose] offset from the parent.
     * @param relativeTo Set the pose relative to given Space. Default value is the parent space.
     */
    public fun setPose(pose: Pose, relativeTo: Space = Space.PARENT)

    /** Sets the [Pose] for this Entity, relative to its parent. */
    public fun setPose(pose: Pose) {
        setPose(pose, Space.PARENT)
    }

    /**
     * Returns the [Pose] for this Entity, relative to the provided [Space].
     *
     * @param relativeTo Get the Pose relative to given Space. Default value is the parent space.
     * @return Current [Pose] of the Entity relative to the given space.
     */
    public fun getPose(relativeTo: Space = Space.PARENT): Pose

    /**
     * Returns the [Pose] for this Entity, relative to its parent.
     *
     * @return Current [Pose] offset from the parent.
     */
    public fun getPose(): Pose = getPose(Space.PARENT)

    /**
     * Sets the scale of this Entity relative to the given Space. This value will affect the
     * rendering of this Entity's children. As the scale increases, this will uniformly stretch the
     * content of the Entity.
     *
     * @param scale The uniform scale factor.
     * @param relativeTo Set the scale relative to given Space. Default value is the parent Space.
     */
    public fun setScale(@FloatRange(from = 0.0) scale: Float, relativeTo: Space = Space.PARENT)

    /**
     * Sets the scale of this Entity relative to the given Space. This value will affect the
     * rendering of this Entity's children. As the scale increases, this will stretch the content of
     * the Entity as specified along each axis.
     *
     * @param scale The scale factor for each axis.
     * @param relativeTo Set the scale relative to given Space. Default value is the parent Space.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun setScale(scale: Vector3, relativeTo: Space = Space.PARENT)

    /**
     * Sets the scale of this Entity relative to its parent. This value will affect the rendering of
     * this Entity's children. As the scale increases, this will uniformly stretch the content of
     * the Entity.
     *
     * @param scale The uniform scale factor from the parent.
     */
    public fun setScale(@FloatRange(from = 0.0) scale: Float) {
        setScale(scale, Space.PARENT)
    }

    /**
     * Returns the scale of this entity along each axis, relative to given space.
     *
     * @param relativeTo Get the scale relative to given Space. Default value is the parent space.
     * @return Current non-uniform scale applied to self and children.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun getNonUniformScale(relativeTo: Space = Space.PARENT): Vector3

    /**
     * Returns the scale of this entity, relative to given space.
     *
     * @param relativeTo Get the scale relative to given Space. Default value is the parent space.
     * @return Current uniform scale applied to self and children.
     */
    @FloatRange(from = 0.0) public fun getScale(relativeTo: Space = Space.PARENT): Float

    /**
     * Returns the local scale of this Entity, not inclusive of the parent's scale.
     *
     * @return Current uniform scale applied to self and children.
     */
    @FloatRange(from = 0.0) public fun getScale(): Float = getScale(Space.PARENT)

    /**
     * Sets the alpha transparency of the Entity relative to the parent Space. Values are in the
     * range [0, 1] with 0 being fully transparent and 1 being fully opaque.
     *
     * This value will affect the rendering of this Entity's children. Children of this node will
     * have their alpha levels multiplied by this value and any alpha of this Entity's ancestors. As
     * a result, the effective alpha of a child cannot exceed the effective alpha of its parent.
     *
     * Usage restrictions:
     * - If the provided `alpha` is outside the [0, 1] range, it will be clamped automatically to
     *   [0, 1].
     *
     * @param alpha Alpha transparency level for the Entity.
     */
    public fun setAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float)

    /**
     * Returns the alpha transparency set for this Entity, relative to given Space.
     *
     * @param relativeTo Gets alpha relative to given Space. Default value is the parent space.
     */
    @FloatRange(from = 0.0, to = 1.0) public fun getAlpha(relativeTo: Space = Space.PARENT): Float

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
    public fun setEnabled(enabled: Boolean)

    /**
     * Returns the enabled status of this Entity.
     *
     * @param includeParents Whether to include the enabled status of parents in the returned value.
     * @return If includeParents is `true`, the returned value will be `true` if this Entity or any
     *   of its ancestors is enabled. If includeParents is `false`, the local enabled state is
     *   returned. Regardless of the local enabled state, an Entity will be considered disabled if
     *   any of its ancestors are disabled.
     */
    public fun isEnabled(includeParents: Boolean = true): Boolean

    /** True if this entity is disposed. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public val isDisposed: Boolean

    /**
     * Exception type that is thrown if client is invoking any of the APIs after the entity instance
     * is already disposed.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class DisposedException(message: String) : IllegalStateException(message)

    /**
     * Disposes of any system resources held by this Entity, and transitively calls dispose() on all
     * its children. Once disposed, this Entity is invalid and cannot be used again.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Deprecated(
        "Entity instances are reclaimed automatically. Instead of `dispose()`, use `parent = null` to detach entities from the scene graph."
    )
    public fun dispose() {}

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
     * Retrieves all Components of the given type [T] and its subtypes attached to this Entity.
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

    public companion object {
        /**
         * Public factory method for creating a [Entity].
         *
         * @param session Session to create the Entity in.
         * @param name Name of the entity. This is unset by default.
         * @param pose Initial pose of the entity. The default value is [Pose.Identity].
         * @param parent Parent entity. If `null`, the entity is created but not attached to the
         *   scene graph and will not be visible until a parent is set. The default value is
         *   [Scene]'s [ActivitySpace].
         */
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            name: String? = null,
            pose: Pose = Pose.Identity,
            parent: Entity? = session.scene.activitySpace,
        ): Entity =
            EntityImpl.create(
                session.sceneRuntime,
                session.scene.entityRegistry,
                name,
                pose,
                parent,
            )
    }
}

/** The BaseEntity is an implementation of Entity interface that wraps a platform entity. */
public abstract class BaseEntity<RtEntityType : RtEntity>
internal constructor(rtEntity: RtEntityType, private val entityRegistry: EntityRegistry) :
    Entity, BaseScenePose<RtScenePose>(rtEntity) {

    private var _rtEntity: RtEntityType? = null

    internal val rtEntity: RtEntityType
        get() {
            checkNotDisposed()
            return _rtEntity!!
        }

    private var _parent: AtomicReference<BaseEntity<*>?> = AtomicReference(null)
    private val _children = mutableListOf<BaseEntity<*>>()
    private val cleanupAction: EntityCleanupAction

    init {
        this._rtEntity = rtEntity
        entityRegistry.setEntityForRtEntity(rtEntity, this)
        cleanupAction = EntityCleanupAction(AtomicReference(rtEntity), entityRegistry)
        ReferenceCleaner.getInstance()
            .register(this, HandlerExecutor.mainThreadExecutor, cleanupAction)
    }

    private class EntityCleanupAction(
        @JvmField val rtEntityRef: AtomicReference<RtEntity?>,
        private val entityRegistry: EntityRegistry,
    ) :
        CleanupAction({
            val rtEntity = rtEntityRef.getAndSet(null)
            rtEntity?.let {
                entityRegistry.removeEntity(it)
                it.dispose()
            }
        })

    private val componentList = mutableListOf<Component>()

    /*
     * Throws an [IllegalStateException] if the entity is disposed.
     */
    @Throws(Entity.DisposedException::class)
    internal fun checkNotDisposed() {
        if (isDisposed) {
            // TODO: b/434266829 - Use name or content description for better error message.
            throw Entity.DisposedException("Entity $this is already disposed.")
        }
    }

    override var contentDescription: CharSequence
        get() {
            return rtEntity.contentDescription
        }
        set(value) {
            rtEntity.contentDescription = value
        }

    override var parent: Entity?
        get() {
            checkNotDisposed()
            return _parent.get()
        }
        set(value) {
            synchronized(_parent) {
                checkNotDisposed()
                require(value == null || value is BaseEntity<*>) {
                    "Parent must be a subtype of BaseEntity or null."
                }
                val oldParent = _parent.getAndSet(value)
                if (oldParent == value) return

                oldParent?.removeChildInternal(this)
                value?.addChildInternal(this)
                rtEntity.parent = value?.rtEntity
            }
        }

    private fun addChildInternal(child: BaseEntity<*>) {
        synchronized(_children) {
            if (!_children.contains(child)) {
                _children.add(child)
            }
        }
    }

    private fun removeChildInternal(child: BaseEntity<*>) {
        synchronized(_children) { _children.remove(child) }
    }

    override fun addChild(child: Entity) {
        checkNotDisposed()
        require(child is BaseEntity<*>) { "Child must be a subtype of BaseEntity." }
        child.parent = this
    }

    private fun getChildrenInternal(): List<BaseEntity<*>> {
        synchronized(_children) {
            return _children.toList()
        }
    }

    override val children: List<Entity>
        get() {
            checkNotDisposed()
            return getChildrenInternal()
        }

    override fun setPose(pose: Pose, relativeTo: Space) {
        rtEntity.setPose(pose, relativeTo.toRtSpace())
    }

    override fun getPose(relativeTo: Space): Pose {
        return rtEntity.getPose(relativeTo.toRtSpace())
    }

    override fun setScale(scale: Float, relativeTo: Space) {
        setScale(Vector3(scale, scale, scale), relativeTo)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun setScale(scale: Vector3, relativeTo: Space) {
        rtEntity.setScale(scale, relativeTo.toRtSpace())
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun getNonUniformScale(relativeTo: Space): Vector3 {
        return rtEntity.getScale(relativeTo.toRtSpace())
    }

    override fun getScale(relativeTo: Space): Float {
        return rtEntity.getScale(relativeTo.toRtSpace()).x
    }

    override fun setAlpha(alpha: Float) {
        rtEntity.setAlpha(alpha)
    }

    override fun getAlpha(relativeTo: Space): Float {
        return rtEntity.getAlpha(relativeTo.toRtSpace())
    }

    override fun setEnabled(enabled: Boolean) {
        rtEntity.setHidden(!enabled)
    }

    override fun isEnabled(includeParents: Boolean): Boolean {
        return !(rtEntity.isHidden(includeParents))
    }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    override val isDisposed: Boolean
        get() = _rtEntity == null

    internal open fun disposeInternal() {
        if (isDisposed) return
        // Clear the parent to remove this entity from the parent's children list.
        parent = null

        // Detach immediate children.
        val childrenToDetach =
            synchronized(_children) {
                val list = _children.toList()
                _children.clear()
                list
            }
        childrenToDetach.forEach { it.parent = null }

        _rtEntity?.let {
            cleanupAction.rtEntityRef.set(null)
            removeAllComponents()
            entityRegistry.removeEntity(it)
            it.dispose()
            _rtEntity = null
        }
    }

    override fun addComponent(component: Component): Boolean {
        checkNotDisposed()
        if (component.handleAttachInternal(this)) {
            synchronized(componentList) { componentList.add(component) }
            return true
        }
        return false
    }

    override fun removeComponent(component: Component) {
        checkNotDisposed()
        synchronized(componentList) {
            if (componentList.contains(component)) {
                component.handleDetachInternal(this)
                componentList.remove(component)
            }
        }
    }

    override fun <T : Component> getComponentsOfType(type: Class<out T>): List<T> {
        checkNotDisposed()
        synchronized(componentList) {
            return componentList.filterIsInstance(type)
        }
    }

    override fun getComponents(): List<Component> {
        checkNotDisposed()
        synchronized(componentList) {
            return componentList.toList()
        }
    }

    override fun removeAllComponents() {
        checkNotDisposed()
        synchronized(componentList) {
            componentList.forEach { it.handleDetachInternal(this) }
            componentList.clear()
        }
    }
}

internal class EntityImpl private constructor(rtEntity: RtEntity, entityRegistry: EntityRegistry) :
    BaseEntity<RtEntity>(rtEntity, entityRegistry) {
    companion object {
        /** Factory method to create EntityImpl entities. */
        @Suppress("RestrictedApiAndroidX")
        internal fun create(
            sceneRuntime: SceneRuntime,
            entityRegistry: EntityRegistry,
            name: String? = null,
            pose: Pose = Pose.Identity,
            parent: Entity? = entityRegistry.getEntityForRtEntity(sceneRuntime.activitySpace),
        ): EntityImpl =
            EntityImpl(
                    sceneRuntime.createEntity(
                        pose,
                        name,
                        if (parent != null && parent !is BaseEntity<*>) {
                            XrLog.warn(
                                "The provided parent is not a BaseEntity. The Entity will " +
                                    "be created without a parent."
                            )
                            null
                        } else {
                            parent?.rtEntity
                        },
                    ),
                    entityRegistry,
                ) // TODO: b/503217486 - Refactor BaseEntity to allow subtypes specify a parent at
                // instantiation.
                .also { it.parent = parent as? BaseEntity<*> }
    }
}
