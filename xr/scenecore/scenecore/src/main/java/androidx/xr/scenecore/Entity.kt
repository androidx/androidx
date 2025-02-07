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

@file:Suppress("BanConcurrentHashMap", "Deprecation")

package androidx.xr.scenecore

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import androidx.annotation.IntDef
import androidx.annotation.MainThread
import androidx.xr.arcore.Anchor
import androidx.xr.runtime.Session as PerceptionSession
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.JxrPlatformAdapter.Entity as RtEntity
import androidx.xr.scenecore.JxrPlatformAdapter.PanelEntity as RtPanelEntity
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

/**
 * Interface for a spatial Entity. An Entity's [Pose]s are represented as being relative to their
 * parent. Applications create and manage Entity instances to construct spatial scenes.
 */
public interface Entity : ActivityPose {

    /**
     * Sets this Entity to be represented in the parent's coordinate space. From a User's
     * perspective, as the parent moves, this Entity will move with it. Setting the parent to null
     * will cause the Entity to not be rendered.
     *
     * @param parent The [Entity] to attach to.
     */
    public fun setParent(parent: Entity?)

    /**
     * Returns the parent of this Entity.
     *
     * @return The [Entity] that this Entity is attached to. Returns null if this Entity has no
     *   parent and is the root of its hierarchy.
     */
    public fun getParent(): Entity?

    /**
     * Sets an Entity to be represented in this coordinate space. From a User's perspective, as this
     * Entity moves, the child Entity will move with it.
     *
     * @param child The [Entity] to be attached.
     */
    public fun addChild(child: Entity)

    /* TODO b/362296608: Add a getChildren() method. */

    /**
     * Sets the pose for this Entity, relative to its parent.
     *
     * @param pose The [Pose] offset from the parent.
     */
    public fun setPose(pose: Pose)

    /**
     * Returns the pose for this entity, relative to its parent.
     *
     * @return Current [Pose] offset from the parent.
     */
    public fun getPose(): Pose

    /**
     * Sets the scale of this entity relative to its parent. This value will affect the rendering of
     * this Entity's children. As the scale increases, this will uniformly stretch the content of
     * the Entity.
     *
     * @param scale The uniform scale factor from the parent.
     */
    public fun setScale(scale: Float)

    /**
     * Returns the local scale of this entity, not inclusive of the parent's scale.
     *
     * @return Current uniform scale applied to self and children.
     */
    public fun getScale(): Float

    /**
     * Returns the accumulated scale of this Entity. This value includes the parent's world space
     * scale.
     *
     * @return Total uniform scale applied to self and children.
     */
    public fun getWorldSpaceScale(): Float

    /**
     * Sets the dimensions in pixels for the Entity.
     *
     * @param dimensions Dimensions in pixels. (Z will be ignored)
     * @deprecated ("This method is deprecated. Use BasePanelEntity<*>.setPixelDimensions()
     *   instead.")
     */
    public fun setSize(dimensions: Dimensions)

    /** Returns the dimensions (in meters) for this Entity. */
    public fun getSize(): Dimensions

    /**
     * Sets the alpha transparency of the Entity and its children. Values are in the range [0, 1]
     * with 0 being fully transparent and 1 being fully opaque.
     *
     * This value will affect the rendering of this Entity's children. Children of this node will
     * have their alpha levels multiplied by this value and any alpha of this entity's ancestors.
     */
    public fun setAlpha(alpha: Float)

    /**
     * Returns the alpha transparency set for this Entity.
     *
     * This does not necessarily equal the perceived alpha of the entity as the entity may have some
     * alpha difference applied from its parent or the system.
     */
    public fun getAlpha(): Float

    /**
     * Returns the global alpha of this entity computed by multiplying the parent's global alpha to
     * this entity's local alpha.
     *
     * This does not necessarily equal the perceived alpha of the entity as the entity may have some
     * alpha difference applied from the system.
     *
     * @return Total [Float] alpha applied to this entity.
     */
    public fun getActivitySpaceAlpha(): Float

    /**
     * Sets the local hidden state of this Entity. When true, this Entity and all descendants will
     * not be rendered in the scene. When the hidden state is false, an entity will be rendered if
     * its ancestors are not hidden.
     *
     * @param hidden The new local hidden state of this Entity.
     */
    public fun setHidden(hidden: Boolean)

    /**
     * Returns the hidden status of this Entity.
     *
     * @param includeParents Whether to include the hidden status of parents in the returned value.
     * @return If includeParents is true, the returned value will be true if this Entity or any of
     *   its ancestors is hidden. If includeParents is false, the local hidden state is returned.
     *   Regardless of the local hidden state, an entity will not be rendered if any of its
     *   ancestors are hidden.
     */
    public fun isHidden(includeParents: Boolean = true): Boolean

    /**
     * Disposes of any system resources held by this Entity, and transitively calls dispose() on all
     * its children. Once disposed, this Entity is invalid and cannot be used again.
     */
    public fun dispose()

    /**
     * Sets alternate text for this entity to be consumed by Accessibility systems.
     *
     * @param text A11y content.
     */
    public fun setContentDescription(text: String)

    /**
     * Adds a Component to this Entity.
     *
     * @param component the Component to be added to the Entity.
     * @return True if given Component is added to the Entity.
     */
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

/**
 * ActivitySpace is an Entity used to track the system-managed pose and boundary of the volume
 * associated with this Spatialized Activity. The Application cannot directly control this volume,
 * but the system might update it in response to the User moving it or entering or exiting FullSpace
 * mode.
 */
public class ActivitySpace
private constructor(
    rtActivitySpace: JxrPlatformAdapter.ActivitySpace,
    entityManager: EntityManager,
) : BaseEntity<JxrPlatformAdapter.ActivitySpace>(rtActivitySpace, entityManager) {

    internal companion object {
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager
        ): ActivitySpace = ActivitySpace(adapter.activitySpace, entityManager)
    }

    private val boundsListeners:
        ConcurrentMap<
            Consumer<Dimensions>,
            JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener
        > =
        ConcurrentHashMap()

    /**
     * The listener registered when using the deprecated registerOnBoundsChangedListener method. We
     * keep this reference so it can be removed using the corresponding unregister method.
     */
    // TODO: b/370538244 - remove with deprecated spatial state callbacks
    private var registeredBoundsListener: Consumer<Dimensions>? = null

    /**
     * Retrieves a copy of the current bounds of this ActivitySpace.
     *
     * @return [Dimensions] representing the current bounds of this ActivitySpace.
     */
    // TODO b/370618648: remove suppression after API review is complete.
    public fun getBounds(): Dimensions = rtEntity.bounds.toDimensions()

    /**
     * Adds the given [Consumer] as a listener to be invoked when this ActivitySpace's current
     * boundary changes. [Consumer#accept(Dimensions)] will be invoked on the main thread.
     *
     * @param listener The Consumer to be invoked when this ActivitySpace's current boundary
     *   changes.
     */
    // TODO b/370618648: remove suppression after API review is complete.
    public fun addBoundsChangedListener(listener: Consumer<Dimensions>): Unit =
        addBoundsChangedListener(HandlerExecutor.mainThreadExecutor, listener)

    /**
     * Adds the given [Consumer] as a listener to be invoked when this ActivitySpace's current
     * boundary changes. [Consumer#accept(Dimensions)] will be invoked on the given executor.
     *
     * @param callbackExecutor The executor on which to invoke the listener on.
     * @param listener The Consumer to be invoked when this ActivitySpace's current boundary
     *   changes.
     */
    // TODO b/370618648: remove suppression after API review is complete.
    public fun addBoundsChangedListener(
        callbackExecutor: Executor,
        listener: Consumer<Dimensions>
    ) {
        val rtListener: JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener =
            JxrPlatformAdapter.ActivitySpace.OnBoundsChangedListener { rtDimensions ->
                callbackExecutor.execute { listener.accept(rtDimensions.toDimensions()) }
            }
        boundsListeners.compute(
            listener,
            { _, _ ->
                rtEntity.addOnBoundsChangedListener(rtListener)
                rtListener
            },
        )
    }

    /**
     * Releases the given [Consumer] from receiving updates when the ActivitySpace's boundary
     * changes.
     *
     * @param listener The Consumer to be removed from receiving updates.
     */
    // TODO b/370618648: remove suppression after API review is complete.
    public fun removeBoundsChangedListener(listener: Consumer<Dimensions>): Unit {
        boundsListeners.computeIfPresent(
            listener,
            { _, rtListener ->
                rtEntity.removeOnBoundsChangedListener(rtListener)
                null // returning null from computeIfPresent removes this entry from the Map
            },
        )
    }

    /**
     * Sets a callback to be invoked when the bounds of the ActivitySpace change. The callback will
     * be dispatched on the UI thread.
     *
     * @param listener A ((Dimensions) -> Unit) callback, where Dimensions are in meters.
     */
    // TODO: b/370538244 - remove with deprecated spatial state callbacks
    @Deprecated(message = "use addBoundsChangedListener(Consumer<Dimensions>)")
    public fun registerOnBoundsChangedListener(listener: OnBoundsChangeListener) {
        if (registeredBoundsListener != null) unregisterOnBoundsChangedListener()
        registeredBoundsListener =
            Consumer<Dimensions> { bounds -> listener.onBoundsChanged(bounds) }
        addBoundsChangedListener(registeredBoundsListener!!)
    }

    /** Clears the listener set by [registerOnBoundsChangedListener]. */
    // TODO: b/370538244 - remove with deprecated spatial state callbacks
    @Deprecated(message = "use removeBoundsChangedListener(Consumer<Dimensions>)")
    public fun unregisterOnBoundsChangedListener() {
        if (registeredBoundsListener != null) {
            removeBoundsChangedListener(registeredBoundsListener!!)
            registeredBoundsListener = null
        }
    }

    /**
     * Registers a listener to be called when the underlying space has moved or changed.
     *
     * @param listener The listener to register if non-null, else stops listening if null.
     * @param executor The executor to run the listener on. Defaults to SceneCore executor if null.
     */
    @JvmOverloads
    @Suppress("ExecutorRegistration")
    public fun setOnSpaceUpdatedListener(
        listener: OnSpaceUpdatedListener?,
        executor: Executor? = null,
    ) {
        rtEntity.setOnSpaceUpdatedListener(listener?.let { { it.onSpaceUpdated() } }, executor)
    }
}

/** The BaseEntity is an implementation of Entity interface that wraps a platform entity. */
public sealed class BaseEntity<out RtEntityType : RtEntity>(
    internal val rtEntity: RtEntityType,
    internal val entityManager: EntityManager,
) : Entity, BaseActivityPose<JxrPlatformAdapter.ActivityPose>(rtEntity) {

    init {
        entityManager.setEntityForRtEntity(rtEntity, this)
    }

    private companion object {
        private const val TAG = "BaseEntity"
    }

    private var dimensions: Dimensions = Dimensions()
    private val componentList = mutableListOf<Component>()

    override fun setParent(parent: Entity?) {
        if (parent == null) {
            rtEntity.parent = null
            return
        }

        if (parent !is BaseEntity<RtEntity>) {
            Log.e(TAG, "Parent must be a subclass of BaseEntity")
            return
        }
        rtEntity.parent = parent.rtEntity
    }

    override fun getParent(): Entity? {
        return rtEntity.parent?.let { entityManager.getEntityForRtEntity(it) }
    }

    override fun addChild(child: Entity) {
        if (child !is BaseEntity<RtEntity>) {
            Log.e(TAG, "Child must be a subclass of BaseEntity!")
            return
        }
        rtEntity.addChild(child.rtEntity)
    }

    override fun setPose(pose: Pose) {
        rtEntity.setPose(pose)
    }

    override fun getPose(): Pose {
        return rtEntity.pose
    }

    override fun setScale(scale: Float) {
        rtEntity.setScale(Vector3(scale, scale, scale))
    }

    override fun getScale(): Float {
        return rtEntity.scale.x
    }

    override fun getWorldSpaceScale(): Float {
        return rtEntity.worldSpaceScale.x
    }

    // TODO: b/356874139 - remove this method
    override fun setSize(dimensions: Dimensions) {
        rtEntity.setSize(dimensions.toRtDimensions())
        this.dimensions = dimensions
    }

    // TODO: b/328620113 - Get the dimensions from EntityImpl.
    override fun getSize(): Dimensions = dimensions

    override fun setAlpha(alpha: Float) {
        rtEntity.alpha = alpha
    }

    override fun getAlpha(): Float = rtEntity.alpha

    override fun getActivitySpaceAlpha(): Float = rtEntity.activitySpaceAlpha

    override fun setHidden(hidden: Boolean): Unit = rtEntity.setHidden(hidden)

    override fun isHidden(includeParents: Boolean): Boolean = rtEntity.isHidden(includeParents)

    override fun dispose() {
        removeAllComponents()
        entityManager.removeEntity(this)
        rtEntity.dispose()
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

    override fun setContentDescription(text: String) {}
}

/**
 * An Entity that itself has no content. ContentlessEntity is useful for organizing the placement,
 * movement of a group of SceneCore Entities.
 */
public class ContentlessEntity
private constructor(rtEntity: RtEntity, entityManager: EntityManager) :
    BaseEntity<RtEntity>(rtEntity, entityManager) {
    public companion object {
        /** Factory method to create ContentlessEntity entities. */
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            name: String,
            pose: Pose = Pose.Identity,
        ): Entity =
            ContentlessEntity(
                adapter.createEntity(pose, name, adapter.activitySpaceRootImpl),
                entityManager,
            )

        /**
         * Public factory function for creating a content-less entity. This entity is used as a
         * connection point for attaching children entities and managing them (i.e. setPose()) as a
         * group.
         *
         * @param session Session to create the ContentlessEntity in.
         * @param name Name of the entity.
         * @param pose Initial pose of the entity.
         */
        @JvmOverloads
        @JvmStatic
        public fun create(session: Session, name: String, pose: Pose = Pose.Identity): Entity =
            ContentlessEntity.create(session.platformAdapter, session.entityManager, name, pose)
    }
}

/** Provides implementations for common Panel functionality. */
public sealed class BasePanelEntity<out RtPanelEntityType : RtPanelEntity>(
    private val rtPanelEntity: RtPanelEntityType,
    entityManager: EntityManager,
) : BaseEntity<RtPanelEntity>(rtPanelEntity, entityManager) {

    /**
     * Sets the corner radius of the PanelEntity.
     *
     * @param radius The radius of the corners, in meters.
     * @throws IllegalArgumentException if radius is <= 0.0f.
     */
    public fun setCornerRadius(radius: Float) {
        rtPanelEntity.setCornerRadius(radius)
    }

    /** Gets the corner radius of this PanelEntity in meters. Has a default value of 0. */
    public fun getCornerRadius(): Float {
        return rtPanelEntity.cornerRadius
    }

    /**
     * Returns the dimensions of the view underlying this PanelEntity.
     *
     * @return The current (width, height) of the underlying surface in pixels.
     */
    public fun getPixelDimensions(): PixelDimensions {
        return rtPanelEntity.getPixelDimensions().toPixelDimensions()
    }

    /**
     * Sets the pixel (not Dp) dimensions of the view underlying this PanelEntity. Calling this
     * might cause the layout of the Panel contents to change. Updating this will not cause the
     * scale or pixel density to change.
     *
     * @param pxDimensions The [PixelDimensions] of the underlying surface to set.
     */
    public fun setPixelDimensions(pxDimensions: PixelDimensions) {
        rtPanelEntity.setPixelDimensions(pxDimensions.toRtPixelDimensions())
    }

    /**
     * Gets the number of pixels per meter for this panel. This value reflects changes to scale,
     * including parent scale.
     *
     * @return Vector3 scale applied to pixels within the Panel. (Z will be 0)
     */
    public fun getPixelDensity(): Vector3 {
        return rtPanelEntity.pixelDensity
    }

    /**
     * Returns the spatial size of this Panel in meters. This includes any scaling applied to this
     * panel by itself or its parents, which might be set via changes to setScale.
     *
     * @return [Dimensions] size of this panel in meters. (Z will be 0)
     */
    override fun getSize(): Dimensions {
        return rtPanelEntity.getSize().toDimensions()
    }
}

/**
 * GltfModelEntity is a concrete implementation of Entity that hosts a glTF model.
 *
 * Note: The size property of this Entity is always reported as {0, 0, 0}, regardless of the actual
 * size of the model.
 */
public class GltfModelEntity
private constructor(rtEntity: JxrPlatformAdapter.GltfEntity, entityManager: EntityManager) :
    BaseEntity<JxrPlatformAdapter.GltfEntity>(rtEntity, entityManager) {
    // TODO: b/362368652 - Add an OnAnimationEvent() Listener interface

    /** Specifies the current animation state of the GltfModelEntity. */
    @IntDef(AnimationState.PLAYING, AnimationState.STOPPED)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class AnimationStateValue

    public object AnimationState {
        public const val PLAYING: Int = 0
        public const val STOPPED: Int = 1
    }

    public companion object {
        /**
         * Factory method for GltfModelEntity.
         *
         * @param adapter Jetpack XR platform adapter.
         * @param model [GltfModel] which this entity will display.
         * @param pose Pose for this [GltfModelEntity], relative to its parent.
         */
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            model: GltfModel,
            pose: Pose = Pose.Identity,
        ): GltfModelEntity =
            GltfModelEntity(
                adapter.createGltfEntity(pose, model.model, adapter.activitySpaceRootImpl),
                entityManager,
            )

        /**
         * Public factory function for a [GltfModelEntity].
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * @param session Session to create the [GltfModel] in.
         * @param model The [GltfModel] this Entity is referencing.
         * @param pose The initial pose of the entity.
         * @return a GltfModelEntity instance
         */
        @MainThread
        @JvmStatic
        @JvmOverloads
        public fun create(
            session: Session,
            model: GltfModel,
            pose: Pose = Pose.Identity,
        ): GltfModelEntity =
            GltfModelEntity.create(session.platformAdapter, session.entityManager, model, pose)
    }

    /** Returns the current animation state of this glTF entity. */
    @AnimationStateValue
    public fun getAnimationState(): Int {
        return when (rtEntity.animationState) {
            JxrPlatformAdapter.GltfEntity.AnimationState.PLAYING -> return AnimationState.PLAYING
            JxrPlatformAdapter.GltfEntity.AnimationState.STOPPED -> return AnimationState.STOPPED
            else -> AnimationState.STOPPED
        }
    }

    /**
     * Starts the animation with the given name.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     *
     * @param animationName The name of the animation to start. If null, the first animation found
     *   in the glTF will be played.
     * @param loop Whether the animation should loop.
     */
    @MainThread
    @JvmOverloads
    public fun startAnimation(loop: Boolean, animationName: String? = null) {
        rtEntity.startAnimation(loop, animationName)
    }

    /**
     * Stops the animation of the glTF entity.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     */
    @MainThread
    public fun stopAnimation() {
        rtEntity.stopAnimation()
    }
}

/**
 * StereoSurfaceEntity is a concrete implementation of Entity that hosts a StereoSurface Canvas. The
 * entity creates and owns an Android Surface into which the application can render stereo image
 * content. This Surface is then texture mapped to the canvas, and if a stereoscopic StereoMode is
 * specified, then the User will see left and right eye content mapped to the appropriate display.
 *
 * Note that it is not currently possible to synchronize CanvasShape and StereoMode changes with
 * application rendering or video decoding. Applications are advised to carefully hide this entity
 * around transitions to manage glitchiness.
 *
 * @property canvasShape The [CanvasShape] which describes the mesh to which the Surface is mapped.
 * @property stereoMode The [StereoMode] which describes how parts of the surface are displayed to
 *   the user's eyes.
 * @property dimensions The dimensions of the canvas in the local spatial coordinate system of the
 *   entity.
 */
public class StereoSurfaceEntity
private constructor(
    rtEntity: JxrPlatformAdapter.StereoSurfaceEntity,
    entityManager: EntityManager,
    canvasShape: CanvasShape,
) : BaseEntity<JxrPlatformAdapter.StereoSurfaceEntity>(rtEntity, entityManager) {

    /** Represents the shape of the StereoSurface Canvas that backs a StereoSurfaceEntity. */
    public abstract class CanvasShape private constructor() {
        public open val dimensions: Dimensions = Dimensions(0.0f, 0.0f, 0.0f)

        // A Quad-shaped canvas. Width and height are represented in the local spatial coordinate
        // system of the entity. (0,0,0) is the center of the canvas.
        public class Quad(public val width: Float, public val height: Float) : CanvasShape() {
            override val dimensions: Dimensions
                get() = Dimensions(width, height, 0.0f)
        }

        // An inwards-facing sphere-shaped canvas, centered at (0,0,0) in the local coordinate
        // space.
        // This is intended to be used by setting the entity's pose to the user's head pose.
        // Radius is represented in the local spatial coordinate system of the entity.
        // The center of the Surface will be mapped to (0, 0, -radius) in the local coordinate
        // space,
        // and UV's are applied from positive X to negative X in an equirectangular projection.
        public class Vr360Sphere(public val radius: Float) : CanvasShape() {
            override val dimensions: Dimensions
                get() = Dimensions(radius * 2, radius * 2, radius * 2)
        }

        // An inwards-facing hemisphere-shaped canvas, where (0,0,0) is the center of the base of
        // the
        // hemisphere. Radius is represented in the local spatial coordinate system of the entity.
        // This is intended to be used by setting the entity's pose to the user's head pose.
        // The center of the Surface will be mapped to (0, 0, -radius) in the local coordinate
        // space,
        // and UV's are applied from positive X to negative X in an equirectangular projection.
        public class Vr180Hemisphere(public val radius: Float) : CanvasShape() {
            override val dimensions: Dimensions
                get() = Dimensions(radius * 2, radius * 2, radius)
        }
    }

    /**
     * Specifies how the surface content will be routed for stereo viewing. Applications must render
     * into the surface in accordance with what is specified here in order for the compositor to
     * correctly produce a stereoscopic view to the user.
     *
     * Values here match values from androidx.media3.common.C.StereoMode in
     * //third_party/java/android_libs/media:common
     */
    @IntDef(StereoMode.MONO, StereoMode.TOP_BOTTOM, StereoMode.SIDE_BY_SIDE)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class StereoModeValue

    public object StereoMode {
        // Each eye will see the entire surface (no separation)
        public const val MONO: Int = 0
        // The [bottom, top] halves of the surface will map to [left, right] eyes
        public const val TOP_BOTTOM: Int = 1
        // The [left, right] halves of the surface will map to [left, right] eyes
        public const val SIDE_BY_SIDE: Int = 2
    }

    public companion object {
        private fun getRtStereoMode(stereoMode: Int): Int {
            return when (stereoMode) {
                StereoMode.MONO -> JxrPlatformAdapter.StereoSurfaceEntity.StereoMode.MONO
                StereoMode.TOP_BOTTOM ->
                    JxrPlatformAdapter.StereoSurfaceEntity.StereoMode.TOP_BOTTOM
                else -> JxrPlatformAdapter.StereoSurfaceEntity.StereoMode.SIDE_BY_SIDE
            }
        }

        /**
         * Factory method for StereoSurfaceEntity.
         *
         * @param adapter JxrPlatformAdapter to use.
         * @param entityManager A SceneCore EntityManager
         * @param stereoMode An [Int] which defines how surface subregions map to eyes
         * @param pose Pose for this StereoSurface entity, relative to its parent.
         * @param canvasShape The [CanvasShape] which describes the spatialized shape of the canvas.
         */
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            stereoMode: Int = StereoMode.SIDE_BY_SIDE,
            pose: Pose = Pose.Identity,
            canvasShape: CanvasShape = CanvasShape.Quad(1.0f, 1.0f),
        ): StereoSurfaceEntity {
            val rtCanvasShape =
                when (canvasShape) {
                    is CanvasShape.Quad ->
                        JxrPlatformAdapter.StereoSurfaceEntity.CanvasShape.Quad(
                            canvasShape.width,
                            canvasShape.height,
                        )
                    is CanvasShape.Vr360Sphere ->
                        JxrPlatformAdapter.StereoSurfaceEntity.CanvasShape.Vr360Sphere(
                            canvasShape.radius
                        )
                    is CanvasShape.Vr180Hemisphere ->
                        JxrPlatformAdapter.StereoSurfaceEntity.CanvasShape.Vr180Hemisphere(
                            canvasShape.radius
                        )
                    else -> throw IllegalArgumentException("Unsupported canvas shape: $canvasShape")
                }
            return StereoSurfaceEntity(
                adapter.createStereoSurfaceEntity(
                    getRtStereoMode(stereoMode),
                    rtCanvasShape,
                    pose,
                    adapter.activitySpaceRootImpl,
                ),
                entityManager,
                canvasShape,
            )
        }

        /**
         * Public factory function for a StereoSurfaceEntity.
         *
         * This method must be called from the main thread.
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * @param session Session to create the StereoSurfaceEntity in.
         * @param stereoMode Stereo mode for the surface.
         * @param pose Pose of this entity relative to its parent, default value is Identity.
         * @param canvasShape The [CanvasShape] which describes the spatialized shape of the canvas.
         * @return a StereoSurfaceEntity instance
         */
        @MainThread
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            stereoMode: Int = StereoSurfaceEntity.StereoMode.SIDE_BY_SIDE,
            pose: Pose = Pose.Identity,
            canvasShape: CanvasShape = CanvasShape.Quad(1.0f, 1.0f),
        ): StereoSurfaceEntity =
            StereoSurfaceEntity.create(
                session.platformAdapter,
                session.entityManager,
                stereoMode,
                pose,
                canvasShape,
            )
    }

    /**
     * Controls how the surface content will be routed for stereo viewing. Applications must render
     * into the surface in accordance with what is specified here in order for the compositor to
     * correctly produce a stereoscopic view to the user.
     *
     * Values must be one of the values from [StereoMode].
     */
    public var stereoMode: Int
        get() = rtEntity.stereoMode
        @MainThread
        set(value) {
            rtEntity.stereoMode = getRtStereoMode(value)
        }

    /**
     * Returns the dimensions of the Entity.
     *
     * This is the size of the canvas in the local spatial coordinate system of the entity. This
     * field cannot be directly set - to update the dimensions of the canvas, update the value of
     * [canvasShape].
     */
    public val dimensions: Dimensions
        get() = rtEntity.dimensions.toDimensions()

    /**
     * The shape of the canvas that backs the Entity.
     *
     * Updating this value will alter the dimensions of the Entity.
     */
    public var canvasShape: CanvasShape = canvasShape
        @MainThread
        set(value) {
            val rtCanvasShape =
                when (value) {
                    is CanvasShape.Quad ->
                        JxrPlatformAdapter.StereoSurfaceEntity.CanvasShape.Quad(
                            value.width,
                            value.height
                        )
                    is CanvasShape.Vr360Sphere ->
                        JxrPlatformAdapter.StereoSurfaceEntity.CanvasShape.Vr360Sphere(value.radius)
                    is CanvasShape.Vr180Hemisphere ->
                        JxrPlatformAdapter.StereoSurfaceEntity.CanvasShape.Vr180Hemisphere(
                            value.radius
                        )
                    else -> throw IllegalArgumentException("Unsupported canvas shape: $value")
                }
            rtEntity.setCanvasShape(rtCanvasShape)
            field = value
        }

    /**
     * Returns a surface into which the application can render stereo image content.
     *
     * This method must be called from the main thread.
     * https://developer.android.com/guide/components/processes-and-threads
     */
    @MainThread
    public fun getSurface(): Surface {
        return rtEntity.surface
    }
}

/** PanelEntity creates a spatial panel in Android XR. */
public open class PanelEntity
internal constructor(
    rtEntity: JxrPlatformAdapter.PanelEntity,
    entityManager: EntityManager,
    // TODO(ricknels): move isMainPanelEntity check to JxrPlatformAdapter.
    public val isMainPanelEntity: Boolean = false,
) : BasePanelEntity<JxrPlatformAdapter.PanelEntity>(rtEntity, entityManager) {

    public companion object {
        /**
         * Factory method for PanelEntity.
         *
         * @param adapter JxrPlatformAdapter to use.
         * @param view View to insert in this panel.
         * @param surfaceDimensionsPx Dimensions for the underlying surface for the given view.
         * @param dimensions The size of this spatial Panel Entity in Meters.
         * @param name Name of this panel.
         * @param context Activity which created this panel.
         * @param pose Pose for this panel, relative to its parent.
         */
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            view: View,
            surfaceDimensionsPx: Dimensions,
            dimensions: Dimensions,
            name: String,
            context: Context,
            pose: Pose = Pose.Identity,
        ): PanelEntity =
            PanelEntity(
                adapter.createPanelEntity(
                    pose,
                    view,
                    PixelDimensions(
                            surfaceDimensionsPx.width.toInt(),
                            surfaceDimensionsPx.height.toInt()
                        )
                        .toRtPixelDimensions(),
                    dimensions.toRtDimensions(),
                    name,
                    context,
                    adapter.activitySpaceRootImpl,
                ),
                entityManager,
            )

        // TODO(b/352629832): Update surfaceDimensionsPx to be a PixelDimensions
        /**
         * Public factory function for a spatialized PanelEntity.
         *
         * @param session Session to create the PanelEntity in.
         * @param view View to embed in this panel entity.
         * @param surfaceDimensionsPx Dimensions for the underlying surface for the given view.
         * @param dimensions Dimensions for the panel in meters.
         * @param name Name of the panel.
         * @param pose Pose of this entity relative to its parent, default value is Identity.
         * @return a PanelEntity instance.
         */
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            view: View,
            surfaceDimensionsPx: Dimensions,
            dimensions: Dimensions,
            name: String,
            pose: Pose = Pose.Identity,
        ): PanelEntity =
            PanelEntity.create(
                session.platformAdapter,
                session.entityManager,
                view,
                surfaceDimensionsPx,
                dimensions,
                name,
                session.activity,
                pose,
            )

        /** Returns the PanelEntity backed by the main window for the Activity. */
        internal fun createMainPanelEntity(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
        ): PanelEntity =
            PanelEntity(adapter.mainPanelEntity, entityManager, isMainPanelEntity = true)
    }
}

/**
 * ActivityPanelEntity creates a spatial panel for embedding an Activity in Android XR. Users can
 * either use an intent to launch an activity in the given panel or provide an instance of activity
 * to move into this panel. Calling dispose() on this entity will destroy the underlying activity.
 */
public class ActivityPanelEntity
private constructor(
    private val rtActivityPanelEntity: JxrPlatformAdapter.ActivityPanelEntity,
    entityManager: EntityManager,
) : PanelEntity(rtActivityPanelEntity, entityManager) {

    /**
     * Launches an activity in the given panel. Subsequent calls to this method will replace the
     * already existing activity in the panel with the new one. If the intent fails to launch the
     * activity, the panel will not be visible. Note this will not update the dimensions of the
     * surface underlying the panel. The Activity will be letterboxed as required to fit the size of
     * the panel. The underlying surface can be resized by calling setPixelDimensions().
     *
     * @param intent Intent to launch the activity.
     * @param bundle Bundle to pass to the activity, can be null.
     */
    @JvmOverloads
    public fun launchActivity(intent: Intent, bundle: Bundle? = null) {
        rtActivityPanelEntity.launchActivity(intent, bundle)
    }

    /**
     * Moves the given activity into this panel. Note this will not update the dimensions of the
     * surface underlying the panel. The Activity will be letterboxed as required to fit the size of
     * the panel. The underlying surface can be resized by calling setPixelDimensions().
     *
     * @param activity Activity to move into this panel.
     */
    public fun moveActivity(activity: Activity) {
        rtActivityPanelEntity.moveActivity(activity)
    }

    public companion object {
        /**
         * Factory method for ActivityPanelEntity.
         *
         * @param adapter JxrPlatformAdapter to use.
         * @param windowBoundsPx Bounds for the underlying surface for the given view.
         * @param name Name of this panel.
         * @param hostActivity Activity which created this panel.
         * @param pose Pose for this panel, relative to its parent.
         */
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            windowBoundsPx: PixelDimensions,
            name: String,
            hostActivity: Activity,
            pose: Pose = Pose.Identity,
        ): ActivityPanelEntity =
            ActivityPanelEntity(
                adapter.createActivityPanelEntity(
                    pose,
                    windowBoundsPx.toRtPixelDimensions(),
                    name,
                    hostActivity,
                    adapter.activitySpaceRootImpl,
                ),
                entityManager,
            )

        // TODO(b/352629832): Update windowBoundsPx to be a PixelDimensions
        /**
         * Public factory function for a spatial ActivityPanelEntity.
         *
         * @param session Session to create the ActivityPanelEntity in.
         * @param windowBoundsPx Bounds for the panel window in pixels.
         * @param name Name of the panel.
         * @param pose Pose of this entity relative to its parent, default value is Identity.
         * @return an ActivityPanelEntity instance.
         */
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            windowBoundsPx: Rect,
            name: String,
            pose: Pose = Pose.Identity,
        ): ActivityPanelEntity =
            ActivityPanelEntity.create(
                session.platformAdapter,
                session.entityManager,
                PixelDimensions(windowBoundsPx.width(), windowBoundsPx.height()),
                name,
                session.activity,
                pose,
            )
    }
}

/** TODO - Convert AnchorEntity into a Space (This will remove setParent) */

/**
 * An AnchorEntity is created to track a Pose relative to some position or surface in the "Real
 * World." Children of this Entity will remain positioned relative to that location in the real
 * world, for the purposes of creating Augmented Reality experiences.
 *
 * Note that Anchors are only relative to the "real world", and not virtual environments. Also,
 * calling setParent() on an AnchorEntity has no effect, as the parenting of an Anchor is controlled
 * by the system.
 */
public class AnchorEntity
private constructor(rtEntity: JxrPlatformAdapter.AnchorEntity, entityManager: EntityManager) :
    BaseEntity<JxrPlatformAdapter.AnchorEntity>(rtEntity, entityManager) {
    private val state = AtomicReference(rtEntity.state.fromRtState())
    private val persistState = AtomicReference(rtEntity.persistState.fromRtPersistState())

    private var onStateChangedListener: OnStateChangedListener? = null

    /** Specifies the current tracking state of the Anchor. */
    @Target(AnnotationTarget.TYPE)
    @IntDef(State.ANCHORED, State.UNANCHORED, State.TIMEDOUT, State.ERROR)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class StateValue

    public object State {
        /**
         * The ANCHORED state means that this Anchor is being actively tracked and updated by the
         * perception stack. The application should expect children to maintain their relative
         * positioning to the system's best understanding of a pose in the real world.
         */
        public const val ANCHORED: Int = 0
        /**
         * An UNANCHORED state could mean that the perception stack hasn't found an anchor for this
         * Space, that it has lost tracking.
         */
        public const val UNANCHORED: Int = 1
        /**
         * The AnchorEntity timed out while searching for an underlying anchor. This it is not
         * possible to recover the AnchorEntity.
         */
        public const val TIMEDOUT: Int = 2
        /**
         * The ERROR state means that something has gone wrong and this AnchorEntity is invalid
         * without the possibility of recovery.
         */
        public const val ERROR: Int = 3
        /**
         * The PERMISSIONS_NOT_GRANTED state means that the user has not granted the necessary
         * permissions i.e. SCENE_UNDERSTANDING to create this AnchorEntity.
         */
        public const val PERMISSIONS_NOT_GRANTED: Int = 4
    }

    /** Specifies the current persist state of the Anchor. */
    public enum class PersistState {
        /** The anchor hasn't been requested to persist. */
        PERSIST_NOT_REQUESTED,
        /** The anchor is requested to persist but hasn't been persisted yet. */
        PERSIST_PENDING,
        /** The anchor is persisted successfully. */
        PERSISTED,
    }

    /** Returns the current tracking state for this AnchorEntity. */
    public fun getState(): @StateValue Int = state.get()

    /** Registers a listener callback to be issued when an anchor's state changes. */
    @Suppress("ExecutorRegistration")
    public fun setOnStateChangedListener(onStateChangedListener: OnStateChangedListener?) {
        this.onStateChangedListener = onStateChangedListener
        if (state.get() == State.PERMISSIONS_NOT_GRANTED) {
            onStateChangedListener?.onStateChanged(State.PERMISSIONS_NOT_GRANTED)
        }
    }

    /** Updates the current state. */
    private fun setState(newState: @StateValue Int) {
        state.set(newState)
        onStateChangedListener?.onStateChanged(newState)
    }

    /** Gets the current PersistState. */
    public fun getPersistState(): PersistState = persistState.get()

    /**
     * Requests to persist the anchor. If the anchor's State is not ANCHORED, no request will be
     * sent and null is returned. If the request is sent successfully, returns an UUID of the anchor
     * immediately; otherwise returns null. After this call, client should use getPersistState() to
     * check the PersistState of the anchor. If the anchor's PersistState becomes PERSISTED before
     * the app is closed the anchor can be recreated in a new session by calling
     * Session.createPersistedAnchorEntity(uuid). If the PersistState doesn't become PERSISTED
     * before the app is closed, the recreation will fail.
     */
    public fun persist(): UUID? {
        if (state.get() != State.ANCHORED) {
            Log.e(TAG, "Cannot persist an anchor that is not in the ANCHORED state.")
            return null
        }
        val uuid = rtEntity.persist()
        if (uuid == null) {
            Log.e(TAG, "Failed to get a UUID for the anchor.")
            return null
        }

        rtEntity.registerPersistStateChangeListener { newRtPersistState ->
            persistState.set(newRtPersistState.fromRtPersistState())
        }
        return uuid
    }

    /**
     * Loads the ARCore for XR Anchor using a Jetpack XR Runtime session.
     *
     * @param session the Jetpack XR Runtime session to load the Anchor from.
     * @return the ARCore for XR Anchor corresponding to the native pointer.
     */
    // TODO(b/373711152) : Remove this method once the ARCore for XR API migration is done.
    public fun getAnchor(session: PerceptionSession): Anchor {
        return Anchor.loadFromNativePointer(session, rtEntity.nativePointer())
    }

    public companion object {
        private const val TAG = "AnchorEntity"

        /**
         * Factory method for AnchorEntity.
         *
         * @param adapter JxrPlatformAdapter to use.
         * @param bounds Bounds for this Anchor Entity.
         * @param planeType Orientation for the plane to which this Anchor should attach.
         * @param planeSemantic Semantics for the plane to which this Anchor should attach.
         * @param timeout Maximum time to search for the anchor, if a suitable plane is not found
         *   within the timeout time the AnchorEntity state will be set to TIMED_OUT.
         */
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            bounds: Dimensions,
            planeType: @PlaneTypeValue Int,
            planeSemantic: @PlaneSemanticValue Int,
            timeout: Duration = Duration.ZERO,
        ): AnchorEntity {
            val rtAnchorEntity =
                adapter.createAnchorEntity(
                    bounds.toRtDimensions(),
                    planeType.toRtPlaneType(),
                    planeSemantic.toRtPlaneSemantic(),
                    timeout,
                )
            return create(rtAnchorEntity, entityManager)
        }

        /**
         * Factory method for AnchorEntity.
         *
         * @param anchor Anchor to create an AnchorEntity for.
         */
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            anchor: Anchor,
        ): AnchorEntity {
            val rtAnchorEntity = adapter.createAnchorEntity(anchor)
            return create(rtAnchorEntity, entityManager)
        }

        /**
         * Factory method for AnchorEntity.
         *
         * @param rtAnchorEntity Runtime AnchorEntity instance.
         */
        internal fun create(
            rtAnchorEntity: JxrPlatformAdapter.AnchorEntity,
            entityManager: EntityManager,
        ): AnchorEntity {
            val anchorEntity = AnchorEntity(rtAnchorEntity, entityManager)
            rtAnchorEntity.setOnStateChangedListener { newRtState ->
                when (newRtState) {
                    JxrPlatformAdapter.AnchorEntity.State.UNANCHORED ->
                        anchorEntity.setState(State.UNANCHORED)
                    JxrPlatformAdapter.AnchorEntity.State.ANCHORED ->
                        anchorEntity.setState(State.ANCHORED)
                    JxrPlatformAdapter.AnchorEntity.State.TIMED_OUT ->
                        anchorEntity.setState(State.TIMEDOUT)
                    JxrPlatformAdapter.AnchorEntity.State.ERROR ->
                        anchorEntity.setState(State.ERROR)
                    JxrPlatformAdapter.AnchorEntity.State.PERMISSIONS_NOT_GRANTED ->
                        anchorEntity.setState(State.PERMISSIONS_NOT_GRANTED)
                }
            }
            return anchorEntity
        }

        /**
         * Factory method for AnchorEntity.
         *
         * @param adapter JxrPlatformAdapter to use.
         * @param uuid UUID of the persisted Anchor Entity to create.
         * @param timeout Maximum time to search for the anchor, if a persisted anchor isn't located
         *   within the timeout time the AnchorEntity state will be set to TIMED_OUT.
         */
        internal fun create(
            adapter: JxrPlatformAdapter,
            entityManager: EntityManager,
            uuid: UUID,
            timeout: Duration = Duration.ZERO,
        ): AnchorEntity {
            val rtAnchorEntity = adapter.createPersistedAnchorEntity(uuid, timeout)
            val anchorEntity = AnchorEntity(rtAnchorEntity, entityManager)
            rtAnchorEntity.setOnStateChangedListener { newRtState ->
                when (newRtState) {
                    JxrPlatformAdapter.AnchorEntity.State.UNANCHORED ->
                        anchorEntity.setState(State.UNANCHORED)
                    JxrPlatformAdapter.AnchorEntity.State.ANCHORED ->
                        anchorEntity.setState(State.ANCHORED)
                    JxrPlatformAdapter.AnchorEntity.State.TIMED_OUT ->
                        anchorEntity.setState(State.TIMEDOUT)
                    JxrPlatformAdapter.AnchorEntity.State.ERROR ->
                        anchorEntity.setState(State.ERROR)
                    JxrPlatformAdapter.AnchorEntity.State.PERMISSIONS_NOT_GRANTED ->
                        anchorEntity.setState(State.PERMISSIONS_NOT_GRANTED)
                }
            }
            return anchorEntity
        }

        /**
         * Public factory function for an AnchorEntity which searches for a location to create an
         * Anchor among the tracked planes available to the perception system.
         *
         * Note that this function will fail if the application has not been granted the
         * "android.permission.SCENE_UNDERSTANDING" permission. Consider using PermissionHelper to
         * help request permission from the User.
         *
         * @param session Session to create the AnchorEntity in.
         * @param bounds Bounds for this AnchorEntity.
         * @param planeType Orientation of plane to which this Anchor should attach.
         * @param planeSemantic Semantics of the plane to which this Anchor should attach.
         * @param timeout The amount of time as a [Duration] to search for the a suitable plane to
         *   attach to. If a plane is not found within the timeout, the returned AnchorEntity state
         *   will be set to AnchorEntity.State.TIMEDOUT. It may take longer than the timeout period
         *   before the anchor state is updated. If the timeout duration is zero it will search for
         *   the anchor indefinitely.
         */
        @JvmStatic
        @JvmOverloads
        public fun create(
            session: Session,
            bounds: Dimensions,
            planeType: @PlaneTypeValue Int,
            planeSemantic: @PlaneSemanticValue Int,
            timeout: Duration = Duration.ZERO,
        ): AnchorEntity {
            return AnchorEntity.create(
                session.platformAdapter,
                session.entityManager,
                bounds,
                planeType,
                planeSemantic,
                timeout,
            )
        }

        /**
         * Public factory function for an AnchorEntity which uses an Anchor from ARCore for XR.
         *
         * @param session Session to create the AnchorEntity in.
         * @param anchor The PerceptionAnchor to use for this AnchorEntity.
         */
        @JvmStatic
        public fun create(session: Session, anchor: Anchor): AnchorEntity {
            return AnchorEntity.create(session.platformAdapter, session.entityManager, anchor)
        }
    }

    /**
     * Extension function that converts [JxrPlatformAdapter.AnchorEntity.State] to
     * [AnchorEntity.State].
     */
    private fun JxrPlatformAdapter.AnchorEntity.State.fromRtState() =
        when (this) {
            JxrPlatformAdapter.AnchorEntity.State.UNANCHORED -> State.UNANCHORED
            JxrPlatformAdapter.AnchorEntity.State.ANCHORED -> State.ANCHORED
            JxrPlatformAdapter.AnchorEntity.State.TIMED_OUT -> State.TIMEDOUT
            JxrPlatformAdapter.AnchorEntity.State.ERROR -> State.ERROR
            JxrPlatformAdapter.AnchorEntity.State.PERMISSIONS_NOT_GRANTED ->
                State.PERMISSIONS_NOT_GRANTED
        }

    /**
     * Extension function that converts [JxrPlatformAdapter.AnchorEntity.PersistState] to
     * [AnchorEntity.PersistState].
     */
    private fun JxrPlatformAdapter.AnchorEntity.PersistState.fromRtPersistState() =
        when (this) {
            JxrPlatformAdapter.AnchorEntity.PersistState.PERSIST_NOT_REQUESTED ->
                PersistState.PERSIST_NOT_REQUESTED
            JxrPlatformAdapter.AnchorEntity.PersistState.PERSIST_PENDING ->
                PersistState.PERSIST_PENDING
            JxrPlatformAdapter.AnchorEntity.PersistState.PERSISTED -> PersistState.PERSISTED
        }

    /**
     * Registers a listener to be called when the Anchor moves relative to its underlying space.
     *
     * <p> The callback is triggered by any anchor movements such as those made by the underlying
     * perception stack to maintain the anchor's position relative to the real world. Any cached
     * data relative to the activity space or any other "space" should be updated when this callback
     * is triggered.
     *
     * @param listener The listener to register if non-null, else stops listening if null.
     * @param executor The executor to run the listener on. Defaults to SceneCore executor if null.
     */
    @JvmOverloads
    @Suppress("ExecutorRegistration")
    public fun setOnSpaceUpdatedListener(
        listener: OnSpaceUpdatedListener?,
        executor: Executor? = null,
    ) {
        rtEntity.setOnSpaceUpdatedListener(listener?.let { { it.onSpaceUpdated() } }, executor)
    }
}

// TODO: b/370538244 - remove with deprecated spatial state callbacks
@Deprecated(message = "Use addBoundsChangedListener(Consumer<Dimensions>)")
public fun interface OnBoundsChangeListener {
    public fun onBoundsChanged(bounds: Dimensions) // Dimensions are in meters.
}

public fun interface OnStateChangedListener {
    public fun onStateChanged(newState: @AnchorEntity.StateValue Int)
}

public fun interface OnSpaceUpdatedListener {
    public fun onSpaceUpdated()
}
