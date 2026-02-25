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

package androidx.xr.compose.subspace

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewParent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.core.viewtree.getParentOrViewTreeDisjointParent
import androidx.core.viewtree.setViewTreeDisjointParent
import androidx.xr.compose.R
import androidx.xr.compose.platform.LocalComposeXrOwners
import androidx.xr.compose.platform.LocalDialogManager
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.disposableValueOf
import androidx.xr.compose.platform.getActivity
import androidx.xr.compose.platform.getValue
import androidx.xr.compose.subspace.layout.CoreActivityPanelEntity
import androidx.xr.compose.subspace.layout.CoreMainPanelEntity
import androidx.xr.compose.subspace.layout.CorePanelEntity
import androidx.xr.compose.subspace.layout.InteractionPolicy
import androidx.xr.compose.subspace.layout.PlaneOrientation
import androidx.xr.compose.subspace.layout.PlaneSemantic
import androidx.xr.compose.subspace.layout.SpatialMoveEndEvent
import androidx.xr.compose.subspace.layout.SpatialMoveEvent
import androidx.xr.compose.subspace.layout.SpatialMoveStartEvent
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SpatialShape
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceMeasurable
import androidx.xr.compose.subspace.layout.SubspaceMeasurePolicy
import androidx.xr.compose.subspace.layout.SubspaceMeasureResult
import androidx.xr.compose.subspace.layout.SubspaceMeasureScope
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.anchorable
import androidx.xr.compose.subspace.layout.interactable
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.node.ComposeSubspaceNode
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetCompositionLocalMap
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetCoreEntity
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetMeasurePolicy
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetModifier
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.Meter.Companion.millimeters
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.PanelEntity

private const val DEFAULT_SIZE_PX = 400

// Max allowed size for makeMeasureSpec is (1 << MeasureSpec.MODE_SHIFT) - 1.
private const val MAX_MEASURE_SPEC_SIZE = (1 shl 30) - 1

/** Set the scrim alpha to 32% opacity across all spatial panels. */
private const val DEFAULT_SCRIM_ALPHA = 0x52000000

private object SpatialPanelDimensions {
    /** Default minimum dimensions for a Spatial Panel in Meters. */
    val minimumPanelDimension: FloatSize2d = FloatSize2d(0.1f, 0.1f)
}

/** Contains default values used by spatial panels. */
public object SpatialPanelDefaults {

    /** Default shape for a Spatial Panel. */
    public val shape: SpatialShape = SpatialRoundedCornerShape(CornerSize(32.dp))
}

/**
 * Base Policy for motion behavior of spatial objects.
 *
 * This class serves as the foundation for defining how a spatial object can be moved or anchored in
 * the environment. Implementations of this class, such as [MovePolicy] and [AnchorPolicy], are
 * mutually exclusive.
 */
public abstract class DragPolicy internal constructor()

/**
 * Represents the anchoring behavior of a spatial object.
 *
 * An AnchorPolicy object can be placed and re-anchored on detected surfaces in the environment.
 * This class defines properties that control how anchoring behaves, such as whether it's enabled
 * and what types of planes it can anchor to.
 *
 * This functionality requires the
 * [android.permission.SCENE_UNDERSTANDING_COARSE][androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE]
 * permission. If this permission is not granted, anchoring will be disabled and the element will
 * behave as if this policy was not applied.
 *
 * @property isEnabled Whether anchoring is enabled for this object. If `false`, the object will not
 *   be able to anchor to surfaces. Defaults to `true`.
 * @property anchorPlaneOrientations A set of [PlaneOrientation] values that define the orientations
 *   of planes this object can anchor to. An empty set means anchoring is not restricted by
 *   orientation. For example, [PlaneOrientation.Horizontal] for floors/ceilings or
 *   [PlaneOrientation.Vertical] for walls. Defaults to an empty set.
 * @property anchorPlaneSemantics A set of [PlaneSemantic] values that define the semantic types of
 *   planes this object can anchor to. An empty set means anchoring is not restricted by semantic
 *   type. For example, [PlaneSemantic.Floor] or [PlaneSemantic.Wall]. Defaults to an empty set.
 */
public class AnchorPolicy(
    public val isEnabled: Boolean = true,
    @Suppress("PrimitiveInCollection")
    public val anchorPlaneOrientations: Set<PlaneOrientation> = emptySet(),
    @Suppress("PrimitiveInCollection")
    public val anchorPlaneSemantics: Set<PlaneSemantic> = emptySet(),
) : DragPolicy() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnchorPolicy) return false
        if (isEnabled != other.isEnabled) return false
        if (anchorPlaneOrientations != other.anchorPlaneOrientations) return false
        if (anchorPlaneSemantics != other.anchorPlaneSemantics) return false
        return true
    }

    override fun hashCode(): Int {
        var result = anchorPlaneOrientations.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + anchorPlaneSemantics.hashCode()
        return result
    }

    override fun toString(): String {
        return "AnchorPolicy(enabled=$isEnabled, anchorPlaneOrientations=$anchorPlaneOrientations, " +
            "anchorPlaneSemantics=$anchorPlaneSemantics)"
    }
}

/**
 * Defines the movement policy for a spatial object.
 *
 * This class configures how a spatial object can be moved by user interaction or programmatic
 * changes. It provides options for enabling/disabling movement, controlling "stickiness" to its
 * current pose, and defining callbacks for various stages of the move operation.
 *
 * @property isEnabled Whether movement is enabled for this object. If `false`, the object cannot be
 *   moved. Defaults to `true`.
 * @property isStickyPose If `true`, the object will attempt to maintain its relative position and
 *   orientation to the user's view or the environment when moved, making it feel "sticky." If
 *   `false`, movement will be more direct. Defaults to `false`.
 * @property shouldScaleWithDistance If `true`, the object's perceived size will scale with its
 *   distance from the user during movement, giving an illusion of constant visual size. If `false`,
 *   its physical size remains constant. Defaults to `true`.
 * @property onMoveStart A callback function invoked when a move operation begins. It receives a
 *   [SpatialMoveStartEvent] providing initial move details. Defaults to `null`.
 * @property onMoveEnd A callback function invoked when a move operation ends. It receives a
 *   [SpatialMoveEndEvent] providing final move details. Defaults to `null`.
 * @property onMove A callback function invoked repeatedly during a move operation. It receives a
 *   [SpatialMoveEvent] with current move details and should return `true` to indicate the move
 *   should continue, or `false` to cancel it. Defaults to `null`.
 */
public class MovePolicy(
    public val isEnabled: Boolean = true,
    public val isStickyPose: Boolean = false,
    @get:JvmName("shouldScaleWithDistance") public val shouldScaleWithDistance: Boolean = true,
    public val onMoveStart: ((SpatialMoveStartEvent) -> Unit)? = null,
    public val onMoveEnd: ((SpatialMoveEndEvent) -> Unit)? = null,
    public val onMove: ((SpatialMoveEvent) -> Boolean)? = null,
) : DragPolicy() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MovePolicy) return false
        if (isEnabled != other.isEnabled) return false
        if (isStickyPose != other.isStickyPose) return false
        if (shouldScaleWithDistance != other.shouldScaleWithDistance) return false
        if (onMoveStart !== other.onMoveStart) return false
        if (onMoveEnd !== other.onMoveEnd) return false
        if (onMove !== other.onMove) return false
        return true
    }

    override fun hashCode(): Int {
        var result = isStickyPose.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + shouldScaleWithDistance.hashCode()
        result = 31 * result + onMoveStart.hashCode()
        result = 31 * result + onMoveEnd.hashCode()
        result = 31 * result + onMove.hashCode()
        return result
    }

    override fun toString(): String {
        return "MovePolicy(enabled=$isEnabled, stickyPose=$isStickyPose, " +
            "scaleWithDistance=$shouldScaleWithDistance, onMoveStart=$onMoveStart, onMoveEnd=$onMoveEnd, " +
            "onMove=$onMove)"
    }
}

/**
 * Defines the resizing policy for a spatial object.
 *
 * This class specifies how a spatial object can be resized, including enabling/disabling resizing,
 * setting minimum and maximum size constraints, and controlling aspect ratio maintenance.
 *
 * @property isEnabled Whether resizing is enabled for this object. If `false`, the object cannot be
 *   resized. Defaults to `true`.
 * @property minimumSize The minimum allowable size for the object, represented by a [DpVolumeSize].
 *   The object cannot be scaled down beyond these dimensions. Defaults to [DpVolumeSize.Zero].
 * @property maximumSize The maximum allowable size for the object, represented by a [DpVolumeSize].
 *   The object cannot be scaled up beyond these dimensions. Defaults to a [DpVolumeSize] with all
 *   dimensions set to [Dp.Infinity], meaning no upper limit by default.
 * @property shouldMaintainAspectRatio If `true`, the object's aspect ratio (proportions) will be
 *   preserved during resizing. If `false`, individual dimensions can be changed independently.
 *   Defaults to `false`.
 * @property onResizeStart A callback to be called when the resize event starts.
 * @property onResizeUpdate A callback to be called when the size changes during a resize event.
 * @property onResizeEnd A callback to be called with the new size when the resize event ends.
 * @property onSizeChange A callback to be called when the object's size changes, after a resize
 *   event has ended. It receives an [IntVolumeSize] representing the new size. Returning `true`
 *   from this callback indicates that the developer intends to handle the size change, and the API
 *   should not resize the object. Returning `false` indicates that the developer will not handle
 *   the size change, and the API should proceed with changing the size of the object itself. If the
 *   callback is `null` (the default), the API will change the size of the object.
 */
public class ResizePolicy(
    public val isEnabled: Boolean = true,
    public val minimumSize: DpVolumeSize = DpVolumeSize.Zero,
    public val maximumSize: DpVolumeSize = DpVolumeSize(Dp.Infinity, Dp.Infinity, Dp.Infinity),
    @get:JvmName("shouldMaintainAspectRatio") public val shouldMaintainAspectRatio: Boolean = false,
    public val onResizeStart: ((IntVolumeSize) -> Unit)? = null,
    public val onResizeUpdate: ((IntVolumeSize) -> Unit)? = null,
    public val onResizeEnd: ((IntVolumeSize) -> Unit)? = null,
    public val onSizeChange: ((IntVolumeSize) -> Boolean)? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResizePolicy

        if (isEnabled != other.isEnabled) return false
        if (shouldMaintainAspectRatio != other.shouldMaintainAspectRatio) return false
        if (minimumSize != other.minimumSize) return false
        if (maximumSize != other.maximumSize) return false
        if (onResizeStart !== other.onResizeStart) return false
        if (onResizeUpdate !== other.onResizeUpdate) return false
        if (onResizeEnd !== other.onResizeEnd) return false
        if (onSizeChange !== other.onSizeChange) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isEnabled.hashCode()
        result = 31 * result + shouldMaintainAspectRatio.hashCode()
        result = 31 * result + minimumSize.hashCode()
        result = 31 * result + maximumSize.hashCode()
        result = 31 * result + (onResizeStart?.hashCode() ?: 0)
        result = 31 * result + (onResizeUpdate?.hashCode() ?: 0)
        result = 31 * result + (onResizeEnd?.hashCode() ?: 0)
        result = 31 * result + (onSizeChange?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ResizePolicy(isEnabled=$isEnabled, minimumSize=$minimumSize, " +
            "maximumSize=$maximumSize, shouldMaintainAspectRatio=$shouldMaintainAspectRatio, " +
            "onResizeStart=$onResizeStart, onResizeUpdate=$onResizeUpdate, " +
            "onResizeEnd=$onResizeEnd, onSizeChange=$onSizeChange)"
    }
}

/**
 * Creates a [SpatialAndroidViewPanel] representing a 2D plane in 3D space where an Android View
 * will be hosted.
 *
 * The presented View is obtained from [factory]. The [factory] block will be called exactly once to
 * obtain the [View] being composed into this panel, and it is also guaranteed to be executed on the
 * main thread. Therefore, in addition to creating the [View], the [factory] block can also be used
 * to perform one-off initializations and [View] constant properties' setting. The factory inside of
 * the constructor is used to avoid the need to pass the context to the factory. There is one [View]
 * for every [SpatialAndroidViewPanel] instance and it is reused across recompositions. This [View]
 * is shown effectively in isolation and does not interact directly with the other composable's that
 * surround it. The [update] block can run multiple times (on the UI thread as well) due to
 * recomposition, and it is the right place to set the new properties. Note that the block will also
 * run once right after the [factory] block completes. [SpatialAndroidViewPanel] will clip the view
 * content to fit the panel.
 *
 * @param T The type of the Android View to be created.
 * @param factory A lambda that creates an instance of the Android View [T].
 * @param modifier SubspaceModifiers to apply to the SpatialPanel.
 * @param update A lambda that allows updating the created Android View [T].
 * @param shape The shape of this Spatial Panel.
 * @param dragPolicy An optional [DragPolicy] that defines the motion behavior of the
 *   [SpatialPanel]. This can be either a [MovePolicy] for free movement or an [AnchorPolicy] for
 *   anchoring to real-world surfaces. If a policy is provided, draggable UI controls will be shown,
 *   allowing the user to manipulate the panel in 3D space. If null, no motion behavior is applied.
 * @param resizePolicy An optional [ResizePolicy] configuration object that resizing behavior of
 *   this [SpatialPanel]. The draggable UI controls will be shown that allow the user to resize the
 *   element in 3D space. If null, there is no resize behavior applied to the element.
 * @param interactionPolicy An optional [InteractionPolicy] that can be set to detect 3D input
 *   events. Setting this will not intercept 2D input events and is intended to provide additional
 *   spatial input information.
 */
@Composable
@SubspaceComposable
public fun <T : View> SpatialAndroidViewPanel(
    factory: (Context) -> T,
    modifier: SubspaceModifier = SubspaceModifier,
    update: (T) -> Unit = {},
    shape: SpatialShape = SpatialPanelDefaults.shape,
    dragPolicy: DragPolicy? = null,
    resizePolicy: ResizePolicy? = null,
    interactionPolicy: InteractionPolicy? = null,
) {
    val finalModifier =
        buildSpatialPanelModifier(
            baseModifier = modifier,
            dragPolicy = dragPolicy,
            resizePolicy = resizePolicy,
            interactionPolicy = interactionPolicy,
        )
    val dialogManager = LocalDialogManager.current
    val context = LocalContext.current
    val parentView = LocalView.current

    @Suppress("UnnecessaryLambdaCreation")
    AndroidViewPanel(
        factory = { factory(context) },
        modifier = finalModifier,
        update = { view ->
            if (dialogManager.isSpatialDialogActive.value) {
                view.foreground = DEFAULT_SCRIM_ALPHA.toDrawable()
                view.setOnClickListener { dialogManager.isSpatialDialogActive.value = false }
            } else {
                view.foreground = Color.TRANSPARENT.toDrawable()
                view.setOnClickListener(null)
            }
            view.setViewTreeDisjointParent(parentView as? ViewParent ?: parentView.parent)
            update(view)
        },
        shape = shape,
    )
}

/**
 * Private [AndroidViewPanel] implementation that reports its created PanelEntity. ComposeNode is
 * used directly for better timing when it comes to Update invocations.
 *
 * @param factory A lambda that creates an instance of the Android View [T].
 * @param modifier SubspaceModifiers.
 * @param update A lambda that allows updating the created Android View [T].
 * @param shape The shape of this Spatial Panel.
 */
@Composable
@SubspaceComposable
private fun <T : View> AndroidViewPanel(
    factory: (Context) -> T,
    modifier: SubspaceModifier = SubspaceModifier,
    update: (T) -> Unit = {},
    shape: SpatialShape,
) {
    val context = LocalContext.current
    val view = remember { factory(context) }
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val density = LocalDensity.current
    val corePanelEntity: CorePanelEntity = remember {
        CorePanelEntity(
                PanelEntity.create(
                    session = session,
                    view = view,
                    dimensions = SpatialPanelDimensions.minimumPanelDimension,
                    name = "ViewPanel:${view.id}",
                    pose = Pose.Identity,
                    parent = null,
                )
            )
            .also {
                it.setShape(shape, density)
                it.enabled = false
                view.setTag(R.id.compose_xr_local_view_entity, it)
            }
    }

    LaunchedEffect(shape, density) { corePanelEntity.setShape(shape, density) }

    val measurePolicy = SpatialViewPanelMeasurePolicy(view)

    val compositionLocalMap = currentComposer.currentCompositionLocalMap
    ComposeNode<ComposeSubspaceNode, Applier<Any>>(
        factory = ComposeSubspaceNode.Constructor,
        update = {
            set(compositionLocalMap, SetCompositionLocalMap)
            set(measurePolicy, SetMeasurePolicy)
            set(corePanelEntity, SetCoreEntity)
            set(modifier, SetModifier)
            update(view)
        },
    )
}

/**
 * Creates a [SpatialPanel] representing a 2D plane in 3D space in which an application can fill
 * content.
 *
 * @param modifier SubspaceModifiers to apply to the SpatialPanel.
 * @param shape The shape of this Spatial Panel.
 * @param dragPolicy An optional [DragPolicy] that defines the motion behavior of the
 *   [SpatialPanel]. This can be either a [MovePolicy] for free movement or an [AnchorPolicy] for
 *   anchoring to real-world surfaces. If a policy is provided, draggable UI controls will be shown,
 *   allowing the user to manipulate the panel in 3D space. If null, no motion behavior is applied.
 * @param resizePolicy An optional [ResizePolicy] that defines the resizing behavior of this
 *   [SpatialPanel]. If a policy is provided, resize UI controls will be shown, allowing the user to
 *   resize the element in 3D space. If null, no resize behavior is applied to the element.
 * @param interactionPolicy An optional [InteractionPolicy] that can be set to detect 3D input
 *   events. Setting this will not intercept 2D input events and is intended to provide additional
 *   spatial input information.
 * @param content The composable content to render within the SpatialPanel.
 */
@Composable
@SubspaceComposable
public fun SpatialPanel(
    modifier: SubspaceModifier = SubspaceModifier,
    shape: SpatialShape = SpatialPanelDefaults.shape,
    dragPolicy: DragPolicy? = null,
    resizePolicy: ResizePolicy? = null,
    interactionPolicy: InteractionPolicy? = null,
    content: @Composable @UiComposable () -> Unit,
) {
    val finalModifier =
        buildSpatialPanelModifier(
            baseModifier = modifier,
            dragPolicy = dragPolicy,
            resizePolicy = resizePolicy,
            interactionPolicy = interactionPolicy,
        )
    // TODO(b/474652577): Update from deprecated currentCompositeKey to currentCompositeKeyCode
    //  once we update JXR Compose to Compile SDK 35
    @Suppress("DEPRECATION") val localId = currentCompositeKeyHash
    val context = LocalContext.current
    val parentView = LocalView.current
    val compositionContext = rememberCompositionContext()
    val dialogManager = LocalDialogManager.current
    val isDialogActive = dialogManager.isSpatialDialogActive.value
    AndroidViewPanel(
        factory = { spatialComposeView(parentView, context, compositionContext, localId) },
        modifier = finalModifier,
        update = { composeView ->
            composeView.setContent {
                // The root is a Box. Its size is determined by its content.
                @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
                Box {
                    content()
                    // The scrim for input handling. It uses matchParentSize to avoid affecting
                    // the measurement of the parent Box.
                    if (isDialogActive) {
                        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
                        Box(
                            modifier =
                                Modifier
                                    .matchParentSize() // This sizes the overlay without affecting
                                    // the parent's size.
                                    .pointerInput(Unit) {
                                        detectTapGestures {
                                            dialogManager.isSpatialDialogActive.value = false
                                        }
                                    }
                        )
                    }
                }
                SideEffect {
                    composeView.foreground =
                        if (isDialogActive) {
                            DEFAULT_SCRIM_ALPHA.toDrawable()
                        } else {
                            Color.TRANSPARENT.toDrawable()
                        }
                }
            }
        },
        shape = shape,
    )
}

/**
 * A composable that renders the Activity's main window's 2D UI content, defined in
 * [androidx.activity.compose.setContent], as a panel in a Subspace.
 *
 * This composable acts as the bridge between the traditional 2D Android UI hierarchy and the 3D
 * Subspace environment. Unlike [SpatialPanel], which renders its own specific composable content,
 * [SpatialMainPanel] takes the entire view hierarchy from the Activity's main window and presents
 * it on a movable, resizable panel in the Compose for XR's Spatial Scene Graph.
 *
 * For the main window to be visible when [androidx.xr.compose.spatial.Subspace] is present in the
 * UI hierarchy, a [SpatialMainPanel] *must* be included in the Subspace composition. If it is not
 * composed, the underlying main panel entity is disabled by default. When [SpatialMainPanel] is
 * removed from the composition, it will again be disable (hidden).
 *
 * ### How It Works
 * [SpatialMainPanel] is backed by a single shared instance that will move to the main content to
 * its active usage. When the main content panel moves inside the composition, its state moves with
 * it regardless of whether it is in a [androidx.compose.runtime.MovableContent] block or not.
 * Components that depend on the main panel's state (such as [androidx.xr.compose.spatial.Orbiter]),
 * will always access a single deterministic instance of the panel.
 *
 * Only the first `SpatialMainPanel` added to the composition will be granted ownership of the main
 * panel at any point in time. Subsequent instances of `SpatialMainPanel` will be queued to be
 * shown, but will not be granted ownership of the main panel until the first instance is removed
 * from composition. If the original owner is removed from and added back to composition, it will be
 * added to the back of the queue.
 *
 * The size of the panel in the Subspace is controlled by the standard Compose layout system, driven
 * by the SubspaceModifier applied to it. Modifiers like SubspaceModifier.width directly dictate the
 * panel's dimensions, following the same measurement and layout rules as other
 * [SubspaceComposable]. To ensure stability, if the panel's layout size results in a width or
 * height of zero, it will be automatically disabled to prevent crashes.
 *
 * ### Manifest Configuration
 * This panel requires the following specific configuration in the `AndroidManifest.xml` on the
 * *base* activity for proper sizing and resizing behavior. Without it, resizing the main panel will
 *
 * cause a crash.
 *
 * ```xml
 * <activity android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize">
 *   ...
 * </activity>
 * ```
 *
 * @param modifier The [SubspaceModifier] to be applied to this panel, controlling its layout, size,
 *   and position within the parent.
 * @param shape The shape of this Spatial Panel.
 * @param dragPolicy An optional [DragPolicy] that defines the motion behavior of the
 *   [SpatialPanel]. This can be either a [MovePolicy] for free movement or an [AnchorPolicy] for
 *   anchoring to real-world surfaces. If a policy is provided, draggable UI controls will be shown,
 *   allowing the user to manipulate the panel in 3D space. If null, no motion behavior is applied.
 * @param resizePolicy An optional [ResizePolicy] configuration object that resizing behavior of
 *   this [SpatialPanel]. The draggable UI controls will be shown that allow the user to resize the
 *   element in 3D space. If null, there is no resize behavior applied to the element.
 * @param interactionPolicy An optional [InteractionPolicy] that can be set to detect 3D input
 *   events. Setting this will not intercept 2D input events and is intended to provide additional
 *   spatial input information.
 * @sample androidx.xr.compose.samples.SpatialMainPanelSample
 */
@Composable
@SubspaceComposable
public fun SpatialMainPanel(
    modifier: SubspaceModifier = SubspaceModifier,
    shape: SpatialShape = SpatialPanelDefaults.shape,
    dragPolicy: DragPolicy? = null,
    resizePolicy: ResizePolicy? = null,
    interactionPolicy: InteractionPolicy? = null,
) {
    val finalModifier =
        buildSpatialPanelModifier(
            baseModifier = modifier,
            dragPolicy = dragPolicy,
            resizePolicy = resizePolicy,
            interactionPolicy = interactionPolicy,
        )
    val mainPanel = requestMainPanelOwnership().value ?: return
    val density = LocalDensity.current
    val view = LocalView.current

    LaunchedEffect(shape, density) { mainPanel.setShape(shape, density) }

    SubspaceLayout(modifier = finalModifier, coreEntity = mainPanel) { _, constraints ->
        val width = view.measuredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = view.measuredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val depth = constraints.minDepth.coerceAtLeast(0)
        layout(width, height, depth) {}
    }
}

/**
 * Allows the caller to request ownership of the main panel.
 *
 * @return A state object that will contain the [CoreMainPanelEntity] of the main panel if ownership
 *   is granted to the caller or null if ownership is not currently granted. The state will change
 *   once ownership is granted to the requestor.
 */
@Composable
private fun requestMainPanelOwnership(): State<CoreMainPanelEntity?> {
    val result = remember { mutableStateOf<CoreMainPanelEntity?>(null) }
    val mainPanel = LocalComposeXrOwners.current?.coreMainPanelEntity ?: return result
    // TODO(b/460459113) - For now we are using the decorView but we should be able to use LocalView
    //  once the view tree is properly connected via `setViewTreeDisjointParent`.
    val ownerQueue =
        LocalContext.current.getActivity()?.window?.decorView?.findViewTreeMainPanelOwnerQueue()
            ?: return result

    DisposableEffect(mainPanel) {
        val onFirstInQueue = { result.value = mainPanel }
        if (ownerQueue.isEmpty()) {
            onFirstInQueue()
        }
        ownerQueue.add(onFirstInQueue)
        onDispose {
            if (ownerQueue.firstOrNull() === onFirstInQueue) {
                ownerQueue.removeFirst()
                ownerQueue.firstOrNull()?.invoke()
            } else {
                ownerQueue.remove(onFirstInQueue)
            }
        }
    }

    return result
}

/**
 * Returns the parent [MainPanelOwnerQueue] for this point in the view hierarchy, or `null` if none
 * can be found.
 *
 * See [mainPanelOwnerQueue] to get or set the parent [MainPanelOwnerQueue] for a specific view.
 */
internal fun View.findViewTreeMainPanelOwnerQueue(): MainPanelOwnerQueue {
    val ancestors = generateSequence(this) { it.getParentOrViewTreeDisjointParent() as? View }
    var topParent: View = this

    for (view in ancestors) {
        val queue = view.mainPanelOwnerQueue
        if (queue != null) {
            return queue
        }
        topParent = view
    }

    return MainPanelOwnerQueue().also { topParent.mainPanelOwnerQueue = it }
}

/**
 * The [MainPanelOwnerQueue] that should be used for compositions at or below this view in the
 * hierarchy. Set to non-`null` to provide a [MainPanelOwnerQueue] for compositions created by child
 * views, or `null` to fall back to any [MainPanelOwnerQueue] provided by ancestor views.
 */
private var View.mainPanelOwnerQueue: MainPanelOwnerQueue?
    get() = getTag(R.id.compose_xr_main_panel_owner_queue) as? MainPanelOwnerQueue
    set(value) {
        setTag(R.id.compose_xr_main_panel_owner_queue, value)
    }

/**
 * A first-in-first-out queue for determining the next main panel owner when the current owner
 * leaves composition.
 *
 * We create this as a new type so we can safely cast to it from `View.getTag`.
 */
internal class MainPanelOwnerQueue(private val queue: ArrayDeque<() -> Unit> = ArrayDeque()) :
    MutableList<() -> Unit> by queue {
    // Use the more efficient ArrayDeque version of `firstOrNull`.
    fun firstOrNull() = queue.firstOrNull()

    // Use the more efficient ArrayDeque version of `removeFirst`.
    fun removeFirst() = queue.removeFirst()
}

/**
 * Creates a [SpatialActivityPanel] and launches an Activity within it.
 *
 * The only supported use case for this SpatialPanel is to launch activities that are a part of the
 * same application.
 *
 * @param intent The intent of an Activity to launch within this panel.
 * @param modifier SubspaceModifiers to apply to the SpatialPanel.
 * @param shape The shape of this Spatial Panel.
 * @param dragPolicy An optional [DragPolicy] that defines the motion behavior of the
 *   [SpatialPanel]. This can be either a [MovePolicy] for free movement or an [AnchorPolicy] for
 *   anchoring to real-world surfaces. If a policy is provided, draggable UI controls will be shown,
 *   allowing the user to manipulate the panel in 3D space. If null, no motion behavior is applied.
 * @param resizePolicy An optional [ResizePolicy] configuration object that resizing behavior of
 *   this [SpatialPanel]. The draggable UI controls will be shown that allow the user to resize the
 *   element in 3D space. If null, there is no resize behavior applied to the element.
 * @param interactionPolicy An optional [InteractionPolicy] that can be set to detect 3D input
 *   events. Setting this will not intercept 2D input events and is intended to provide additional
 *   spatial input information.
 */
@Composable
@SubspaceComposable
public fun SpatialActivityPanel(
    intent: Intent,
    modifier: SubspaceModifier = SubspaceModifier,
    shape: SpatialShape = SpatialPanelDefaults.shape,
    dragPolicy: DragPolicy? = null,
    resizePolicy: ResizePolicy? = null,
    interactionPolicy: InteractionPolicy? = null,
) {
    val finalModifier =
        buildSpatialPanelModifier(
            baseModifier = modifier,
            dragPolicy = dragPolicy,
            resizePolicy = resizePolicy,
            interactionPolicy = interactionPolicy,
        )
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val dialogManager = LocalDialogManager.current
    val density = LocalDensity.current

    val pixelDimensions = IntSize2d(0, 0)

    val corePanelEntity: CoreActivityPanelEntity = remember {
        CoreActivityPanelEntity(
                ActivityPanelEntity.create(
                    session,
                    pixelDimensions,
                    "ActivityPanel-${intent.action}",
                    parent = null,
                )
            )
            .apply { enabled = false }
    }

    SideEffect { corePanelEntity.setShape(shape, density) }

    LaunchedEffect(intent) { corePanelEntity.startActivity(intent) }

    SpatialBox {
        SubspaceLayout(modifier = finalModifier, coreEntity = corePanelEntity) { _, constraints ->
            val width = DEFAULT_SIZE_PX.coerceIn(constraints.minWidth, constraints.maxWidth)
            val height = DEFAULT_SIZE_PX.coerceIn(constraints.minHeight, constraints.maxHeight)
            val depth = constraints.minDepth.coerceAtLeast(0)
            layout(width, height, depth) {}
        }

        if (dialogManager.isSpatialDialogActive.value) {
            val localContext = LocalContext.current
            val scrimView =
                remember(localContext) {
                    View(localContext).apply {
                        foreground = DEFAULT_SCRIM_ALPHA.toDrawable()
                        setOnClickListener { dialogManager.isSpatialDialogActive.value = false }
                    }
                }

            val entityName = "ScrimPanel"
            val scrimPanelEntity by
                remember(session, scrimView) {
                    disposableValueOf(
                        CorePanelEntity(
                                PanelEntity.create(
                                    session = session,
                                    view = scrimView,
                                    pixelDimensions =
                                        corePanelEntity.size.run { IntSize2d(width, height) },
                                    name = entityName,
                                    pose = Pose.Identity,
                                )
                            )
                            .apply {
                                parent = corePanelEntity
                                poseInMeters =
                                    Pose(translation = Vector3(0f, 0f, 3.millimeters.toM()))
                            }
                    ) {
                        it.dispose()
                    }
                }

            SideEffect {
                scrimPanelEntity.size = corePanelEntity.mutableSize
                scrimPanelEntity.setShape(shape, density)
            }
        }
    }
}

private class SpatialViewPanelMeasurePolicy(private val view: View) : SubspaceMeasurePolicy {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialViewPanelMeasurePolicy) return false
        return view == other.view
    }

    override fun hashCode(): Int {
        return view.hashCode()
    }

    override fun SubspaceMeasureScope.measure(
        measurables: List<SubspaceMeasurable>,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        view.setTag(R.id.compose_xr_panel_volume_constraints, constraints)
        view.measure(
            MeasureSpec.makeMeasureSpec(
                constraints.maxWidth.coerceAtMost(MAX_MEASURE_SPEC_SIZE),
                MeasureSpec.AT_MOST,
            ),
            MeasureSpec.makeMeasureSpec(
                constraints.maxHeight.coerceAtMost(MAX_MEASURE_SPEC_SIZE),
                MeasureSpec.AT_MOST,
            ),
        )
        // The measured size of the view is used to lay out the SubspaceNode.
        val width = view.measuredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = view.measuredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val depth = constraints.minDepth.coerceAtLeast(0)
        return layout(width, height, depth) {}
    }
}

/**
 * Applies move, anchor, and resize policies to a [SubspaceModifier], returning the combined final
 * modifier. This is a private helper function for [SpatialPanel] and [SpatialExternalSurface].
 *
 * @param baseModifier The initial [SubspaceModifier] to which policies will be applied.
 * @param dragPolicy An optional [AnchorPolicy] or [MovePolicy] to configure either anchoring or
 *   movement behavior.
 * @param resizePolicy An optional [ResizePolicy] to configure resizing behavior.
 * @param interactionPolicy An optional [InteractionPolicy] that can be set to detect 3D input
 *   events.
 * @return A [SubspaceModifier] with all applicable policies integrated.
 */
internal fun buildSpatialPanelModifier(
    baseModifier: SubspaceModifier,
    dragPolicy: DragPolicy?,
    resizePolicy: ResizePolicy?,
    interactionPolicy: InteractionPolicy? = null,
): SubspaceModifier {

    var finalModifier =
        when (dragPolicy) {
            is AnchorPolicy ->
                baseModifier.anchorable(
                    enabled = dragPolicy.isEnabled,
                    anchorPlaneOrientations = dragPolicy.anchorPlaneOrientations,
                    anchorPlaneSemantics = dragPolicy.anchorPlaneSemantics,
                )

            is MovePolicy ->
                SubspaceModifier.movable(
                        enabled = dragPolicy.isEnabled,
                        stickyPose = dragPolicy.isStickyPose,
                        scaleWithDistance = dragPolicy.shouldScaleWithDistance,
                        onMoveStart = dragPolicy.onMoveStart,
                        onMoveEnd = dragPolicy.onMoveEnd,
                        onMove = dragPolicy.onMove,
                    )
                    .then(baseModifier)

            else -> {
                baseModifier
            }
        }

    if (resizePolicy != null) {
        finalModifier =
            finalModifier.resizable(
                enabled = resizePolicy.isEnabled,
                minimumSize = resizePolicy.minimumSize,
                maximumSize = resizePolicy.maximumSize,
                maintainAspectRatio = resizePolicy.shouldMaintainAspectRatio,
                onResizeStart = resizePolicy.onResizeStart,
                onResizeUpdate = resizePolicy.onResizeUpdate,
                onResizeEnd = resizePolicy.onResizeEnd,
                onSizeChange = resizePolicy.onSizeChange,
            )
    }

    if (interactionPolicy != null) {
        finalModifier =
            finalModifier.interactable(
                enabled = interactionPolicy.isEnabled,
                onInputEvent = interactionPolicy.onInputEvent,
            )
    }

    return finalModifier
}
