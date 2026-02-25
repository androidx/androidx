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

package androidx.xr.compose.subspace.layout

import android.annotation.SuppressLint
import android.content.Intent
import android.view.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.xr.compose.subspace.ActionQueue
import androidx.xr.compose.subspace.SceneCoreEntitySizeAdapter
import androidx.xr.compose.subspace.SpatialPanelDefaults
import androidx.xr.compose.subspace.draw.SpatialFeatheringEffect
import androidx.xr.compose.subspace.draw.SpatialSmoothFeatheringEffect
import androidx.xr.compose.subspace.node.SubspaceLayoutNode
import androidx.xr.compose.subspace.node.SubspaceOwner
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.toIntVolumeSize
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.Component
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.GltfModelNode
import androidx.xr.scenecore.GroupEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import kotlin.math.PI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import org.jetbrains.annotations.TestOnly

/**
 * Wrapper class for Entities from SceneCore to provide convenience methods for working with
 * Entities from SceneCore.
 */
internal sealed class CoreEntity(initialEntity: Entity? = null) : OpaqueEntity {
    /**
     * If [entity] is null, this will contain the set of mutations that are queued to be applied to
     * the entity once it is attached.
     */
    private val entityActionQueue =
        ActionQueue(initialValue = initialEntity, isValid = { !it.isDisposed })

    protected val entity: Entity?
        get() = entityActionQueue.value

    /**
     * This parameter is null for Composables without a layout, such as Orbiters and Spatial
     * Dialogs.
     */
    internal var layout: SubspaceLayoutNode? = null
        set(value) {
            field = value
            updatePoseFromLayout()
        }

    protected val density: Density?
        get() = layout?.density

    open val layoutPoseInPixels: Pose
        get() = layout?.measurableLayout?.poseInParent ?: Pose.Identity

    internal open var poseInMeters: Pose
        get() = entity?.getPose() ?: Pose.Identity
        set(value) {
            entityActionQueue.executeWhenAvailable { entity ->
                if (entity.getPose() != value) {
                    entity.setPose(value)
                }
            }
        }

    /** Get the [Entity] associated with this [CoreEntity] for testing purposes. */
    internal val semanticsEntity: Entity?
        @TestOnly get() = entity

    /**
     * The volume size of the [CoreEntity] in pixels. Reading this value may trigger recomposition.
     */
    var mutableSize by mutableStateOf(IntVolumeSize.Zero)
        private set

    /**
     * The volume size of the [CoreEntity] in pixels. Reading this value will not trigger
     * recomposition.
     */
    open var size: IntVolumeSize = IntVolumeSize.Zero
        set(value) {
            if (field == value) {
                return
            }
            field = value
            mutableSize = value
        }

    /**
     * Whether this entity and all of its ancestors are enabled. An entity will not render if it is
     * not enabled.
     *
     * Note that an enabled entity may still be invisible if its alpha value is 0.
     */
    open var enabled: Boolean
        get() = entity?.isEnabled(includeParents = true) ?: false
        set(value) {
            entityActionQueue.executeWhenAvailable { entity ->
                if (entity.isEnabled(includeParents = false) != value) {
                    entity.setEnabled(value)
                }
            }
        }

    /**
     * The scale of this entity relative to its parent. This value will affect the rendering of this
     * Entity's children. As the scale increases, this will uniformly stretch the content of the
     * Entity. This does not affect layout and other content will be laid out according to the
     * original scale of the entity.
     */
    internal open var scale = 1f
        set(value) {
            if (field != value) {
                entityActionQueue.executeWhenAvailable { it.setScale(value) }
            }
            field = value
        }

    /**
     * The opacity of this entity (and its children) as a value between [0..1]. An alpha value of
     * 0.0f means fully transparent while the value of 1.0f means fully opaque.
     */
    internal var alpha = 1f
        set(value) {
            if (field != value) {
                entityActionQueue.executeWhenAvailable { it.setAlpha(value) }
            }
            field = value
        }

    /**
     * SceneCore parents all newly-created non-Anchor entities under a world space point of
     * reference for the activity space, which we save for future use.
     */
    private val originalParent: Entity? = entity?.parent

    open var parent: CoreEntity? = null
        set(value) {
            field = value
            // When the Compose-level parent is set to null, restore the original parent
            // (saved during the initial creation)
            entityActionQueue.executeWhenAvailable { it.parent = value?.entity ?: originalParent }
        }

    fun updatePoseFromLayout() {
        // Compose XR uses pixels, SceneCore uses meters.
        poseInMeters = layoutPoseInPixels.convertPixelsToMeters(density ?: return)
    }

    open fun dispose() {
        entityActionQueue.clear()
        entityActionQueue.value?.dispose()
    }

    /**
     * Add a SceneCore [Component] to this entity.
     *
     * @param component The [Component] to add.
     * @return true if the component was added successfully, false otherwise.
     */
    fun addComponent(component: Component): Boolean? {
        return entityActionQueue.executeWhenAvailable { it.addComponent(component) }
    }

    /**
     * Remove a SceneCore [Component] from this entity.
     *
     * @param component The [Component] to remove.
     */
    fun removeComponent(component: Component) {
        entityActionQueue.executeWhenAvailable { it.removeComponent(component) }
    }

    fun onEntityAttached(onEntityAttached: (Entity) -> Unit) {
        entityActionQueue.executeWhenAvailable(onEntityAttached)
    }

    fun attachEntity(entity: Entity) {
        entityActionQueue.value?.dispose()
        entityActionQueue.value = entity
    }

    override fun toString(): String = "CoreEntity(entity=$entity)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CoreEntity

        if (entity != other.entity) return false
        if (entity == null) return false

        return true
    }

    override fun hashCode(): Int {
        return entity?.hashCode() ?: 0
    }
}

/** Wrapper class for group entities from SceneCore. */
internal class CoreGroupEntity(entity: Entity) : CoreEntity(entity) {
    init {
        require(entity is GroupEntity) {
            "Entity passed to CoreGroupEntity should be a GroupEntity."
        }
    }
}

/**
 * Wrapper class for [PanelEntity] to provide convenience methods for working with panel entities
 * from SceneCore.
 */
internal sealed class CoreBasePanelEntity(private val panelEntity: PanelEntity) :
    CoreEntity(panelEntity), MovableCoreEntity, ResizableCoreEntity, InteractableCoreEntity {
    // Density set from setShape.
    private var shapeDensity: Density? = null

    /**
     * The size of the [CoreBasePanelEntity] in pixels.
     *
     * This value is used to set the size of the CoreBasePanelEntity.
     *
     * If the width or height is zero or negative, the panel will be hidden. And the panel size will
     * be adjusted to 1 because the underlying implementation of the main panel entity does not
     * allow for zero or negative sizes.
     *
     * For the MainPanel, the [CoreBasePanelEntity] size and the [PanelEntity] size (SceneCore) may
     * diverge. SceneCore uses the bounds from the WindowManager. Therefore unfortunately, it is
     * impossible to unit test the case for this diverging behavior.
     */
    override var size: IntVolumeSize
        get() = super.size
        set(value) {
            if (super.size != value) {
                super.size = value
                panelEntity.sizeInPixels =
                    IntSize2d(size.width.coerceAtLeast(1), size.height.coerceAtLeast(1))
                shapeDensity?.let { updateShape(it) }
            }

            updateEntityEnabledState()
        }

    /**
     * Update the entity enabled state based on the panel size.
     *
     * In Compose for XR, layout measurement dictates entity size and visibility. The visibility
     * state is dictated by the Compose layout size measurement. This means visibility will be set
     * after layout completes. See [SubspaceLayoutNode.SubspaceMeasurableLayout.placeAt] and
     * [CoreBasePanelEntity.size].
     */
    private fun updateEntityEnabledState() {
        super.enabled = size.width > 0 && size.height > 0
    }

    /** The [SpatialShape] of this [CoreBasePanelEntity]. */
    private var shape: SpatialShape = SpatialPanelDefaults.shape

    /* Sets the [SpatialShape] of this [CoreBasePanelEntity] and updates the shape */
    fun setShape(shape: SpatialShape, density: Density) {
        this.shape = shape
        this.shapeDensity = density
        updateShape(density)
    }

    /** Apply shape changes to the SceneCore [Entity]. */
    private fun updateShape(density: Density) {
        val shape = shape
        if (shape is SpatialRoundedCornerShape) {
            val radius =
                shape.computeCornerRadius(size.width.toFloat(), size.height.toFloat(), density)
            panelEntity.cornerRadius = Meter.fromPixel(radius, density).toM()
        }
    }
}

/**
 * Wrapper class for [PanelEntity] to provide convenience methods for working with panel entities
 * from SceneCore.
 */
internal class CorePanelEntity(entity: PanelEntity) : CoreBasePanelEntity(entity)

internal class CoreActivityPanelEntity(private val activityPanelEntity: ActivityPanelEntity) :
    CoreBasePanelEntity(activityPanelEntity) {
    fun startActivity(intent: Intent) {
        activityPanelEntity.startActivity(intent)
    }
}

/**
 * Wrapper class for SceneCore's PanelEntity associated with the "main window" for the Activity.
 * This wrapper provides convenience methods for working with the main panel from SceneCore.
 */
internal class CoreMainPanelEntity(session: Session) :
    CoreBasePanelEntity(session.scene.mainPanelEntity) {

    override fun dispose() {
        // [CoreMainPanelEntity] is backed by SceneCore “Main Panel Entity” which is never deleted
        // even if [androidx.xr.compose.subspace.SpatialMainPanel] is disposed. Therefore we just
        // disable the main panel entity to turn off it's visibility.
        enabled = false

        // Set the parent to null so the main panel is not disposed when its parent is disposed.
        parent = null
    }
}

/** Wrapper class for surface entities from SceneCore. */
internal class CoreSurfaceEntity(
    internal val surfaceEntity: SurfaceEntity,
    private val localDensity: Density,
) : CoreEntity(surfaceEntity), ResizableCoreEntity, MovableCoreEntity, InteractableCoreEntity {
    private var pendingOnSurfaceDestroyed: ((Surface) -> Unit)? = null

    internal var stereoMode: SurfaceEntity.StereoMode
        get() = surfaceEntity.stereoMode
        set(value) {
            if (value != surfaceEntity.stereoMode) {
                surfaceEntity.stereoMode = value
            }
        }

    private var currentFeatheringEffect: SpatialFeatheringEffect? = null

    override var size: IntVolumeSize
        get() = super.size
        set(value) {
            if (super.size != value) {
                super.size = value
                surfaceEntity.shape =
                    SurfaceEntity.Shape.Quad(
                        FloatSize2d(
                            Meter.fromPixel(size.width.toFloat(), localDensity).value,
                            Meter.fromPixel(size.height.toFloat(), localDensity).value,
                        )
                    )
                updateFeathering()
            }
        }

    override fun dispose() {
        pendingOnSurfaceDestroyed?.let { it(surfaceEntity.getSurface()) }
        pendingOnSurfaceDestroyed = null
        super.dispose()
    }

    internal fun setOnSurfaceDestroyed(onSurfaceDestroyed: (Surface) -> Unit) {
        pendingOnSurfaceDestroyed = onSurfaceDestroyed
    }

    internal fun setFeatheringEffect(featheringEffect: SpatialFeatheringEffect?) {
        currentFeatheringEffect = featheringEffect
        updateFeathering()
    }

    private fun updateFeathering() {
        val featheringEffect = currentFeatheringEffect as? SpatialSmoothFeatheringEffect
        if (featheringEffect != null) {
            surfaceEntity.edgeFeatheringParams =
                SurfaceEntity.EdgeFeatheringParams.RectangleFeather(
                    featheringEffect.size.toWidthPercent(size.width.toFloat(), localDensity),
                    featheringEffect.size.toHeightPercent(size.height.toFloat(), localDensity),
                )
        } else {
            surfaceEntity.edgeFeatheringParams = SurfaceEntity.EdgeFeatheringParams.NoFeathering()
        }
    }
}

/**
 * A [CoreEntity] used in a [androidx.xr.compose.subspace.SceneCoreEntity]. The exact semantics of
 * this entity are unknown to compose; however, the developer may supply information that we may use
 * to set and derive the size of the entity.
 */
internal class AdaptableCoreEntity<T : Entity>(
    val coreEntity: T,
    var sceneCoreEntitySizeAdapter: SceneCoreEntitySizeAdapter<T>? = null,
) : CoreEntity(coreEntity) {
    override var size: IntVolumeSize
        get() = sceneCoreEntitySizeAdapter?.intrinsicSize?.invoke(coreEntity) ?: super.size
        set(value) {
            sceneCoreEntitySizeAdapter?.onLayoutSizeChanged?.let { coreEntity.it(value) }
            super.size = value
        }
}

/**
 * Wrapper class for sphere-based surface entities from SceneCore. Head pose is not a dynamic
 * property, and should just be calculated upon instantiation to avoid head locking the sphere.
 */
internal class CoreSphereSurfaceEntity(
    internal val surfaceEntity: SurfaceEntity,
    val initialDensity: Density,
) : CoreEntity(surfaceEntity), InteractableCoreEntity {
    private var pendingOnSurfaceDestroyed: ((Surface) -> Unit)? = null

    internal var stereoMode: SurfaceEntity.StereoMode
        get() = surfaceEntity.stereoMode
        set(value) {
            if (value != surfaceEntity.stereoMode) {
                surfaceEntity.stereoMode = value
            }
        }

    private var isDisposed = false

    override fun dispose() {
        isDisposed = true
        pendingOnSurfaceDestroyed?.let { it(surfaceEntity.getSurface()) }
        pendingOnSurfaceDestroyed = null
        super.dispose()
    }

    internal fun setOnSurfaceDestroyed(onSurfaceDestroyed: (Surface) -> Unit) {
        pendingOnSurfaceDestroyed = onSurfaceDestroyed
    }

    internal var isBoundaryAvailable = true
        set(value) {
            if (field != value) {
                field = value
                updateFeathering()
            }
        }

    private var currentFeatheringEffect: SpatialFeatheringEffect? = null

    // Layout's density is automatically updated during a configuration change, and may differ from
    // initialDensity.
    private val localDensity: Density
        get() = layout?.density ?: initialDensity

    /** Radius in meters. */
    internal var radius: Float
        get() = radiusFromShape(surfaceEntity.shape)
        set(value) {
            val shape = surfaceEntity.shape
            if (value != radiusFromShape(shape)) {
                if (shape is SurfaceEntity.Shape.Hemisphere) {
                    surfaceEntity.shape = SurfaceEntity.Shape.Hemisphere(value)
                } else {
                    surfaceEntity.shape = SurfaceEntity.Shape.Sphere(value)
                }
                updateFeathering()
            }
        }

    private fun radiusFromShape(shape: SurfaceEntity.Shape): Float {
        if (shape is SurfaceEntity.Shape.Hemisphere) {
            return shape.radius
        } else if (shape is SurfaceEntity.Shape.Sphere) {
            return shape.radius
        }
        throw IllegalStateException("Shape must be spherical")
    }

    internal fun setFeatheringEffect(featheringEffect: SpatialFeatheringEffect?) {
        currentFeatheringEffect = featheringEffect
        updateFeathering()
    }

    // When there is no boundary, we need a feathering value higher than 50 on 360 Surfaces to not
    // obstruct the user's vision, hence we need the lint suppression.
    @SuppressLint("Range")
    private fun updateFeathering() {
        if (isDisposed) {
            // At the Compose level, dispose calls happen before we clear passthrough listeners,
            // and since those passthrough listeners update feathering, this is at risk of being
            // executed after the Entity is already disposed.
            return
        }

        surfaceEntity.edgeFeatheringParams =
            if (!isBoundaryAvailable) {
                val radius = if (isHemisphere) 0.5f else 0.7f
                SurfaceEntity.EdgeFeatheringParams.RectangleFeather(radius, radius)
            } else {
                val semicircleArcLength = Meter((radius * PI).toFloat()).toPx(localDensity)
                val featheringEffect = currentFeatheringEffect as? SpatialSmoothFeatheringEffect
                if (featheringEffect != null) {
                    val radiusX =
                        featheringEffect.size.toWidthPercent(
                            if (surfaceEntity.shape is SurfaceEntity.Shape.Hemisphere)
                                semicircleArcLength / 2
                            else semicircleArcLength,
                            localDensity,
                        )
                    val radiusY =
                        featheringEffect.size.toHeightPercent(semicircleArcLength, localDensity)
                    SurfaceEntity.EdgeFeatheringParams.RectangleFeather(radiusX, radiusY)
                } else {
                    SurfaceEntity.EdgeFeatheringParams.NoFeathering()
                }
            }
    }

    private val isHemisphere
        get() = surfaceEntity.shape is SurfaceEntity.Shape.Hemisphere
}

internal class CoreModelEntity() : CoreEntity() {
    private val scope: CoroutineScope by lazy {
        val context = requireOwner().coroutineContext
        CoroutineScope(context + Job(context[Job]))
    }

    val nodes: List<GltfModelNode>
        get() = (entity as? GltfModelEntity)?.nodes ?: emptyList()

    val animationStateFlow by lazy {
        callbackFlow {
                onEntity { @Suppress("DEPRECATION") addAnimationStateListener(::trySend) }
                awaitClose {
                    onEntity { @Suppress("DEPRECATION") removeAnimationStateListener(::trySend) }
                }
            }
            .shareIn(scope = scope, started = SharingStarted.WhileSubscribed(5000), replay = 1)
    }

    /**
     * The size of the glTF entity will be scaled uniformly such that it fits within the most
     * restrictive dimension according to the constraints.
     */
    override var size: IntVolumeSize
        get() = super.size
        set(value) {
            onEntity {
                if (super.size != value) {
                    val heightScale =
                        value.height / (intrinsicSize.height.toFloat().coerceAtLeast(1f))
                    val widthScale = value.width / (intrinsicSize.width.toFloat().coerceAtLeast(1f))
                    val depthScale = value.depth / (intrinsicSize.depth.toFloat().coerceAtLeast(1f))
                    scale = minOf(heightScale, widthScale, depthScale)
                }
                super.size = value
            }
        }

    val intrinsicSize: IntVolumeSize
        get() =
            density?.let { density ->
                (entity as? GltfModelEntity)
                    ?.gltfModelBoundingBox
                    ?.halfExtents
                    ?.times(2)
                    ?.toIntVolumeSize(density)
            } ?: IntVolumeSize.Zero

    val isAnimating: Boolean
        @Suppress("DEPRECATION")
        get() {
            return (entity as? GltfModelEntity)?.animationState ==
                GltfModelEntity.AnimationState.PLAYING
        }

    override fun dispose() {
        scope.cancel()
        super.dispose()
    }

    @Suppress("DEPRECATION")
    fun startAnimation(name: String? = null) {
        if (name == null) {
            onEntity { startAnimation(loop = false) }
        } else {
            onEntity { startAnimation(loop = false, animationName = name) }
        }
    }

    @Suppress("DEPRECATION")
    fun loopAnimation(name: String? = null) {
        if (name == null) {
            onEntity { startAnimation(loop = true) }
        } else {
            onEntity { startAnimation(loop = true, animationName = name) }
        }
    }

    @Suppress("DEPRECATION")
    fun stopAllAnimations() {
        onEntity { stopAnimation() }
    }

    private fun onEntity(action: GltfModelEntity.() -> Unit) {
        onEntityAttached { entity -> (entity as GltfModelEntity).action() }
    }
}

/** [CoreEntity] types that implement this interface may have the ResizableComponent attached. */
internal interface ResizableCoreEntity

/** [CoreEntity] types that implement this interface may have the MovableComponent attached. */
internal interface MovableCoreEntity

/** [CoreEntity] types that implement this interface may have the InteractableComponent attached. */
internal interface InteractableCoreEntity

private fun CoreEntity.requireOwner(): SubspaceOwner =
    checkNotNull(layout?.owner) { "Failed to get SubspaceOwner for CoreEntity." }
