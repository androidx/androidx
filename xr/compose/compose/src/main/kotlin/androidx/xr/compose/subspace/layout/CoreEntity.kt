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

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Density
import androidx.xr.compose.subspace.SceneCoreEntitySizeAdapter
import androidx.xr.compose.subspace.SpatialPanelDefaults
import androidx.xr.compose.subspace.node.SubspaceLayoutNode
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.Meter
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.Component
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GroupEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import kotlin.math.PI
import kotlin.math.max

/**
 * Wrapper class for Entities from SceneCore to provide convenience methods for working with
 * Entities from SceneCore.
 */
@PublishedApi
internal sealed class CoreEntity(public val entity: Entity) : OpaqueEntity {

    internal var layout: SubspaceLayoutNode? = null
        set(value) {
            field = value
            updateEntityPose()
        }

    protected val density: Density?
        get() = layout?.density

    internal open fun updateEntityPose() {
        val density = density ?: return

        // Compose XR uses pixels, SceneCore uses meters.
        val corePose =
            layout?.measurableLayout?.poseInParentEntity?.convertPixelsToMeters(density)
                ?: Pose.Identity
        if (entity.getPose() != corePose) {
            entity.setPose(corePose)
        }
    }

    public open fun dispose() {
        entity.dispose()
    }

    /**
     * The backing value for the size of the [CoreEntity] in pixels. It uses a MutableState object
     * so that recompositions can be triggered on size changes.
     */
    protected val mutableSize = mutableStateOf(IntVolumeSize.Zero)

    /** The volume size of the [CoreEntity] in pixels. */
    public open var size: IntVolumeSize
        get() = mutableSize.value
        set(value) {
            if (mutableSize.value == value) {
                return
            }
            mutableSize.value = value
        }

    /**
     * The scale of this entity relative to its parent. This value will affect the rendering of this
     * Entity's children. As the scale increases, this will uniformly stretch the content of the
     * Entity. This does not affect layout and other content will be laid out according to the
     * original scale of the entity.
     */
    internal var scale = 1f
        set(value) {
            if (field != value) {
                entity.setScale(value)
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
                entity.setAlpha(value)
            }
            field = value
        }

    public open var parent: CoreEntity? = null
        set(value) {
            field = value

            // Leave SceneCore's parent as-is if we're trying to clear it out. SceneCore
            // parents all
            // newly-created non-Anchor entities under a world space point of reference for the
            // activity
            // space, but we don't have access to it. To maintain this parent-is-not-null property,
            // we use
            // this hack to keep the original parent, even if it's not technically correct when
            // we're
            // trying to reparent a node. The correct parent will be set on the "set" part of the
            // reparent.
            //
            // TODO(b/356952297): Remove this hack once we can save and restore the original parent.
            if (value == null) return

            entity.parent = value.entity
        }

    /**
     * Add a SceneCore [Component] to this entity.
     *
     * @param component The [Component] to add.
     * @return true if the component was added successfully, false otherwise.
     */
    public fun addComponent(component: Component): Boolean {
        return entity.addComponent(component)
    }

    /**
     * Remove a SceneCore [Component] from this entity.
     *
     * @param component The [Component] to remove.
     */
    public fun removeComponent(component: Component) {
        entity.removeComponent(component)
    }
}

/** Wrapper class for group entities from SceneCore. */
@PublishedApi
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
    CoreEntity(panelEntity), MovableCoreEntity, ResizableCoreEntity {
    override var overrideSize: IntVolumeSize? = null

    /**
     * The size of the [CoreBasePanelEntity] in pixels.
     *
     * This value is used to set the size of the CoreBasePanelEntity.
     *
     * If the width or height is zero or negative, the panel will be hidden. And the panel size will
     * be adjusted to 1 because the underlying implementation of the main panel entity does not
     * allow for zero or negative sizes.
     */
    override var size: IntVolumeSize
        get() = super.size
        set(value) {
            var nextSize = overrideSize ?: value

            val shouldHide = nextSize.width <= 0 || nextSize.height <= 0

            if (shouldHide) {
                Log.w(
                    "CoreBasePanelEntity",
                    "Setting the panel size to 0 or less. The panel will be hidden.",
                )
            }
            hidden = shouldHide

            nextSize =
                IntVolumeSize(max(nextSize.width, 1), max(nextSize.height, 1), nextSize.depth)

            if (super.size != nextSize) {
                super.size = nextSize
                panelEntity.sizeInPixels = IntSize2d(size.width, size.height)

                if (density != null) {
                    updateShape(density!!)
                }
            }
        }

    /**
     * Whether this entity or any of its ancestors is marked as hidden.
     *
     * Note that a non-hidden entity may still not be visible if its alpha is 0.
     */
    var hidden: Boolean
        // TODO - b/421386891: Consider renaming this field to align with Entity.is/setEnabled
        get() = !entity.isEnabled(includeParents = true)
        set(value) {
            entity.setEnabled(!value)
        }

    /** The [SpatialShape] of this [CoreBasePanelEntity]. */
    private var shape: SpatialShape = SpatialPanelDefaults.shape

    /* Sets the [SpatialShape] of this [CoreBasePanelEntity] and updates the shape */
    public fun setShape(shape: SpatialShape, density: Density) {
        this.shape = shape
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

/**
 * Wrapper class for SceneCore's PanelEntity associated with the "main window" for the Activity.
 * This wrapper provides convenience methods for working with the main panel from SceneCore.
 */
internal class CoreMainPanelEntity(session: Session) :
    CoreBasePanelEntity(session.scene.mainPanelEntity) {

    override fun dispose() {
        // Do not call super.dispose() because we don't want to dispose the main panel entity.
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (entity != (other as CoreMainPanelEntity).entity) return false
        return true
    }

    override fun hashCode(): Int {
        return entity.hashCode()
    }
}

/** Wrapper class for surface entities from SceneCore. */
internal class CoreSurfaceEntity(
    internal val surfaceEntity: SurfaceEntity,
    private val localDensity: Density,
) : CoreEntity(surfaceEntity), ResizableCoreEntity, MovableCoreEntity {
    internal var stereoMode: Int
        get() = surfaceEntity.stereoMode
        set(value) {
            if (value != surfaceEntity.stereoMode) {
                surfaceEntity.stereoMode = value
            }
        }

    private var currentFeatheringEffect: SpatialFeatheringEffect = ZeroFeatheringEffect

    override var size: IntVolumeSize
        get() = super.size
        set(value) {
            val nextSize = overrideSize ?: value
            if (super.size != nextSize) {
                super.size = nextSize
                surfaceEntity.canvasShape =
                    SurfaceEntity.CanvasShape.Quad(
                        Meter.fromPixel(size.width.toFloat(), localDensity).value,
                        Meter.fromPixel(size.height.toFloat(), localDensity).value,
                    )
                updateFeathering()
            }
        }

    override var overrideSize: IntVolumeSize? = null

    internal fun setFeatheringEffect(featheringEffect: SpatialFeatheringEffect) {
        currentFeatheringEffect = featheringEffect
        updateFeathering()
    }

    private fun updateFeathering() {
        (currentFeatheringEffect as? SpatialSmoothFeatheringEffect)?.let {
            surfaceEntity.edgeFeather =
                SurfaceEntity.EdgeFeatheringParams.SmoothFeather(
                    it.size.toWidthPercent(size.width.toFloat(), localDensity),
                    it.size.toHeightPercent(size.height.toFloat(), localDensity),
                )
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
    private val headPose: Pose?,
    val initialDensity: Density,
) : CoreEntity(surfaceEntity) {

    internal var stereoMode: Int
        get() = surfaceEntity.stereoMode
        set(value) {
            if (value != surfaceEntity.stereoMode) {
                surfaceEntity.stereoMode = value
            }
        }

    private var currentFeatheringEffect: SpatialFeatheringEffect = ZeroFeatheringEffect

    // Layout's density is automatically updated during a configuration change, and may differ from
    // initialDensity.
    private val localDensity: Density
        get() = layout?.density ?: initialDensity

    override fun updateEntityPose() {
        if (headPose == null) {
            Log.w("CoreSphereSurfaceEntity", "Positioning Sphere without head Pose.")
            super.updateEntityPose()
        } else {
            // Center the sphere around the user and apply any corePose adjustment
            val corePose =
                layout?.measurableLayout?.poseInParentEntity?.convertPixelsToMeters(localDensity)
                    ?: Pose.Identity
            val poseFromHead = corePose.copy(corePose.translation.plus(headPose.translation))
            if (entity.getPose() != poseFromHead) {
                entity.setPose(poseFromHead)
            }
        }
    }

    /** The parent of spheres is always scene.activitySpaceRoot. Setting this has no affect. */
    override var parent: CoreEntity? = null

    /** Radius in meters. */
    internal var radius: Float
        get() = radiusFromShape(surfaceEntity.canvasShape)
        set(value) {
            val shape = surfaceEntity.canvasShape
            if (value != radiusFromShape(shape)) {
                if (shape is SurfaceEntity.CanvasShape.Vr180Hemisphere) {
                    surfaceEntity.canvasShape = SurfaceEntity.CanvasShape.Vr180Hemisphere(value)
                } else {
                    surfaceEntity.canvasShape = SurfaceEntity.CanvasShape.Vr360Sphere(value)
                }
                updateFeathering()
            }
        }

    private fun radiusFromShape(shape: SurfaceEntity.CanvasShape): Float {
        if (shape is SurfaceEntity.CanvasShape.Vr180Hemisphere) {
            return shape.radius
        } else if (shape is SurfaceEntity.CanvasShape.Vr360Sphere) {
            return shape.radius
        }
        throw IllegalStateException("Shape must be spherical")
    }

    internal fun setFeatheringEffect(featheringEffect: SpatialFeatheringEffect) {
        currentFeatheringEffect = featheringEffect
        updateFeathering()
    }

    private fun updateFeathering() {
        val semicircleArcLength = Meter((radius * PI).toFloat()).toPx(localDensity)
        (currentFeatheringEffect as? SpatialSmoothFeatheringEffect)?.let {
            val radiusX =
                it.size.toWidthPercent(
                    if (surfaceEntity.canvasShape is SurfaceEntity.CanvasShape.Vr180Hemisphere)
                        semicircleArcLength / 2
                    else semicircleArcLength,
                    localDensity,
                )
            val radiusY = it.size.toHeightPercent(semicircleArcLength, localDensity)
            surfaceEntity.edgeFeather =
                SurfaceEntity.EdgeFeatheringParams.SmoothFeather(radiusX, radiusY)
        }
    }
}

/** [CoreEntity] types that implement this interface may have the ResizableComponent attached. */
internal interface ResizableCoreEntity {
    /**
     * The size of the [CoreEntity] in pixels.
     *
     * This value is used to override the layout size of the [CoreEntity] when it is resizable. When
     * this value is null, the layout size of the [CoreEntity] is used.
     */
    public var overrideSize: IntVolumeSize?
}

/** [CoreEntity] types that implement this interface may have the MovableComponent attached. */
internal interface MovableCoreEntity
