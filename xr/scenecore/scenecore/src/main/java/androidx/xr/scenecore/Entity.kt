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
 * An Entity is the fundamental building block of a SceneCore [Scene]. Entities have a parent-child
 * relationship with each other and are used to build a spatial scene graph.
 *
 * There are several different Entity types with Entity being the base class. The position of most
 * Entity types can be updated within the scene graph by updating their [Pose]. Additionally, the
 * Entity's scale, and alpha can be updated. Components can be attached to Entities to enable
 * additional behaviors.
 *
 * There are several subtypes of Entities. These include Entities with special positions, (e.g.
 * [ActivitySpace] and [AnchorEntity]) as well as those which are used to render different types of
 * content (e.g. [PanelEntity] and [GltfModelEntity]).
 */
public open class Entity
internal constructor(rtEntity: RtEntity, private val entityRegistry: EntityRegistry) :
    BaseScenePose<RtScenePose>(rtEntity) {

    private var _rtEntity: RtEntity? = null

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val rtEntity: RtEntity
        get() {
            checkNotDisposed()
            return _rtEntity!!
        }

    private var _parent: AtomicReference<Entity?> = AtomicReference(null)
    private val _children = mutableListOf<Entity>()
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
    @Throws(DisposedException::class)
    internal fun checkNotDisposed() {
        if (isDisposed) {
            // TODO: b/434266829 - Use name or content description for better error message.
            throw DisposedException("Entity $this is already disposed.")
        }
    }

    /** Alternate text for this Entity to be consumed by Accessibility systems. */
    public var contentDescription: CharSequence
        get() {
            return rtEntity.contentDescription
        }
        set(value) {
            rtEntity.contentDescription = value
        }

    /**
     * The parent of this Entity, from which this Entity will inherit most of its properties. For
     * example, this Entity's [Pose] is defined in relation to the parent Entity's coordinate space,
     * so as the parent moves, this Entity will move with it. Setting the parent to `null` will
     * remove the Entity from the scene graph.
     */
    public var parent: Entity?
        get() {
            checkNotDisposed()
            return _parent.get()
        }
        set(value) {
            synchronized(_parent) {
                checkNotDisposed()
                val oldParent = _parent.getAndSet(value)
                if (oldParent == value) return

                oldParent?.removeChildInternal(this)
                value?.addChildInternal(this)
                rtEntity.parent = value?.rtEntity
            }
        }

    private fun addChildInternal(child: Entity) {
        synchronized(_children) {
            if (!_children.contains(child)) {
                _children.add(child)
            }
        }
    }

    private fun removeChildInternal(child: Entity) {
        synchronized(_children) { _children.remove(child) }
    }

    /**
     * Sets an Entity to be a child of this Entity in the scene graph. The child Entity will inherit
     * properties from the parent, and will be represented in the parent's coordinate space. From a
     * user's perspective, as this Entity moves, the child Entity will move with it.
     *
     * @param child The [Entity] to be attached.
     */
    public fun addChild(child: Entity) {
        checkNotDisposed()
        child.parent = this
    }

    private fun getChildrenInternal(): List<Entity> {
        synchronized(_children) {
            return _children.toList()
        }
    }

    /**
     * Provides the list of all children of this entity. Adding children can be done using
     * [addChild] or by setting the [parent] property of the child Entity. To remove a child, set
     * the child's [parent] property to another Entity or `null`.
     *
     * @return List of all children of this entity.
     */
    public val children: List<Entity>
        get() {
            checkNotDisposed()
            return getChildrenInternal()
        }

    /**
     * Sets the [Pose] for this Entity. The Pose given is set relative to the [Space] provided.
     *
     * @param pose The [Pose] offset from the parent.
     * @param relativeTo Set the pose relative to given Space. Default value is the parent space.
     */
    @JvmOverloads
    public open fun setPose(pose: Pose, relativeTo: Space = Space.PARENT) {
        rtEntity.setPose(pose, relativeTo.toRtSpace())
    }

    /**
     * Returns the [Pose] for this Entity, relative to the provided [Space].
     *
     * @param relativeTo Get the Pose relative to given Space. Default value is the parent space.
     * @return Current [Pose] of the Entity relative to the given space.
     */
    @JvmOverloads
    public open fun getPose(relativeTo: Space = Space.PARENT): Pose {
        return rtEntity.getPose(relativeTo.toRtSpace())
    }

    /**
     * Sets the scale of this Entity relative to the given Space. This value will affect the
     * rendering of this Entity's children. As the scale increases, this will uniformly stretch the
     * content of the Entity.
     *
     * @param scale The uniform scale factor.
     * @param relativeTo Set the scale relative to given Space. Default value is the parent Space.
     */
    @JvmOverloads
    public open fun setScale(
        @FloatRange(from = 0.0) scale: Float,
        relativeTo: Space = Space.PARENT,
    ) {
        setScale(Vector3(scale, scale, scale), relativeTo)
    }

    /**
     * Sets the scale of this Entity relative to the given Space. This value will affect the
     * rendering of this Entity's children. As the scale increases, this will stretch the content of
     * the Entity as specified along each axis.
     *
     * @param scale The scale factor for each axis.
     * @param relativeTo Set the scale relative to given Space. Default value is the parent Space.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmOverloads
    public open fun setScale(scale: Vector3, relativeTo: Space = Space.PARENT) {
        rtEntity.setScale(scale, relativeTo.toRtSpace())
    }

    /**
     * Returns the scale of this entity along each axis, relative to given space.
     *
     * @param relativeTo Get the scale relative to given Space. Default value is the parent space.
     * @return Current non-uniform scale applied to self and children.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open fun getNonUniformScale(relativeTo: Space = Space.PARENT): Vector3 {
        return rtEntity.getScale(relativeTo.toRtSpace())
    }

    /**
     * Returns the scale of this entity, relative to given space.
     *
     * @param relativeTo Get the scale relative to given Space. Default value is the parent space.
     * @return Current uniform scale applied to self and children.
     */
    @JvmOverloads
    @FloatRange(from = 0.0)
    public open fun getScale(relativeTo: Space = Space.PARENT): Float {
        return rtEntity.getScale(relativeTo.toRtSpace()).x
    }

    /**
     * Sets the alpha transparency of the Entity relative to the parent Space. Values are in the
     * range [0, 1] with 0 being fully transparent and 1 being fully opaque.
     *
     * This value will affect the rendering of this Entity's children. Children of this node will
     * have their alpha levels multiplied by this value and any alpha of this Entity's ancestors. As
     * a result, the effective alpha of a child cannot exceed the effective alpha of its parent.
     *
     * @param alpha Alpha transparency level for the Entity.
     */
    public fun setAlpha(@FloatRange(from = 0.0, to = 1.0) alpha: Float) {
        rtEntity.setAlpha(alpha)
    }

    /**
     * Returns the alpha transparency set for this Entity, relative to given Space.
     *
     * @param relativeTo Gets alpha relative to given Space. Default value is the parent space.
     */
    @JvmOverloads
    @FloatRange(from = 0.0, to = 1.0)
    public fun getAlpha(relativeTo: Space = Space.PARENT): Float {
        return rtEntity.getAlpha(relativeTo.toRtSpace())
    }

    /**
     * Sets the local enabled state of this Entity.
     *
     * When `false`, this Entity and all descendants will not be rendered in the scene, and the
     * Entity will not respond to input events. If an Entity's local enabled state is `true`, the
     * Entity will still be considered not enabled if at least one of its ancestors is not enabled.
     *
     * @param enabled The new local enabled state of this Entity.
     */
    public fun setEnabled(enabled: Boolean) {
        rtEntity.setHidden(!enabled)
    }

    /**
     * Returns the enabled status of this Entity.
     *
     * If `includeParents` is `true`, the returned value will be `false` if this Entity or any of
     * its ancestors is disabled. If `includeParents` is `false`, the local enabled state is
     * returned. Regardless of the local enabled state, an Entity will be considered disabled if any
     * of its ancestors are disabled.
     *
     * @param includeParents Whether to include the enabled status of parents in the returned value.
     * @return True if this Entity is enabled, possibly including the enabled status of its parents.
     */
    @JvmOverloads
    public fun isEnabled(includeParents: Boolean = true): Boolean {
        return !(rtEntity.isHidden(includeParents))
    }

    /** True if this entity is disposed. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public val isDisposed: Boolean
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

    /**
     * Adds a Component to this Entity.
     *
     * @param component the Component to be added to the Entity.
     * @return True if given Component was successfully added to the Entity. This can be false for
     *   certain components if an Entity already has a component of that type or does not support
     *   the component type.
     */
    public fun addComponent(component: Component): Boolean {
        checkNotDisposed()
        if (component.handleAttachInternal(this)) {
            synchronized(componentList) { componentList.add(component) }
            return true
        }
        return false
    }

    /** Removes the given Component from this Entity. */
    public fun removeComponent(component: Component) {
        checkNotDisposed()
        synchronized(componentList) {
            if (componentList.contains(component)) {
                component.handleDetachInternal(this)
                componentList.remove(component)
            }
        }
    }

    /**
     * Retrieves all Components of the given type [T] and its subtypes attached to this Entity.
     *
     * @param type The type of Component to retrieve.
     * @return List<Component> of the given type attached to this Entity. This list will be empty if
     *   no components of the given type are attached to this Entity.
     */
    public fun <T : Component> getComponentsOfType(type: Class<out T>): List<T> {
        checkNotDisposed()
        synchronized(componentList) {
            return componentList.filterIsInstance(type)
        }
    }

    /**
     * Retrieves all components attached to this Entity.
     *
     * @return List<Component> attached to this Entity.
     */
    public fun getComponents(): List<Component> {
        checkNotDisposed()
        synchronized(componentList) {
            return componentList.toList()
        }
    }

    /** Remove all components from this Entity. */
    public fun removeAllComponents() {
        checkNotDisposed()
        synchronized(componentList) {
            componentList.forEach { it.handleDetachInternal(this) }
            componentList.clear()
        }
    }

    /**
     * Disposes of any system resources held by this Entity, and transitively calls dispose() on all
     * its children. Once disposed, this Entity is invalid and cannot be used again.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Deprecated(
        "Entity instances are reclaimed automatically. Instead of `dispose()`, use `parent = null` to detach entities from the scene graph."
    )
    public fun dispose() {
        if (!isDisposed) {
            parent = null
        }
    }

    public companion object {
        internal fun create(
            sceneRuntime: SceneRuntime,
            entityRegistry: EntityRegistry,
            name: String? = null,
            pose: Pose = Pose.Identity,
            parent: Entity? = entityRegistry.getEntityForRtEntity(sceneRuntime.activitySpace),
        ): Entity =
            Entity(sceneRuntime.createEntity(pose, name, parent?.rtEntity), entityRegistry).also {
                it.parent = parent
            }

        /**
         * Public factory method for creating an [Entity].
         *
         * @param session Session to create the Entity in.
         * @param name Name of the entity. This is unset by default.
         * @param pose Initial pose of the entity. The default value is [Pose.Identity].
         * @param parent Parent entity. Defaults to `null`. If `null`, the entity is created but not
         *   attached to the scene graph, meaning it will be invisible. If a parent entity (e.g.,
         *   [ActivitySpace] or any other [Entity] already present in the scene) is assigned later,
         *   the entity will become visible (provided it is enabled). This allows for [Entity]
         *   pre-configuration before making it visible.
         */
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            name: String? = null,
            pose: Pose = Pose.Identity,
            parent: Entity? = null,
        ): Entity = create(session.sceneRuntime, session.scene.entityRegistry, name, pose, parent)
    }

    /**
     * Exception type that is thrown if client is invoking any of the APIs after the entity instance
     * is already disposed.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public class DisposedException(message: String) : IllegalStateException(message)
}
