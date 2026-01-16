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

package androidx.xr.compose.spatial

import android.content.Context
import android.view.View
import android.view.ViewParent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.viewtree.setViewTreeDisjointParent
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.compose.R
import androidx.xr.compose.platform.LocalDialogManager
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.findNearestParentEntity
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.subspace.layout.CorePanelEntity
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.compose.unit.toMeter
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.PanelEntity

/**
 * Properties for configuring a [SpatialDialog].
 *
 * @property dismissOnBackPress whether the dialog should be dismissed when the device's back button
 *   is pressed. Defaults to `true`.
 * @property dismissOnClickOutside whether the dialog should be dismissed when the user touches
 *   outside of it. Defaults to `true`.
 * @property usePlatformDefaultWidth whether the dialog should use the platform's default width.
 *   Defaults to `true`. This is only used in non-spatial environments.
 * @property backgroundContentAnimationSpec the animation specification for the depth offset of the
 *   app content as it animates away from the user towards its recessed resting level when a spatial
 *   dialog is shown. The same specification is used when the app content animates back towards the
 *   user to its original resting level when the dialog is dismissed. This is only used in spatial
 *   environments.
 * @property elevation the elevation level of the dialog. Defaults to
 *   [SpatialElevationLevel.DialogDefault].
 * @see [SpatialDialog]
 */
public class SpatialDialogProperties(
    @get:Suppress("GetterSetterNames") public val dismissOnBackPress: Boolean = true,
    @get:Suppress("GetterSetterNames") public val dismissOnClickOutside: Boolean = true,
    @get:Suppress("GetterSetterNames") public val usePlatformDefaultWidth: Boolean = true,
    public val backgroundContentAnimationSpec: FiniteAnimationSpec<Float> = spring(),
    public val elevation: Dp = SpatialElevationLevel.DialogDefault,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialDialogProperties) return false

        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false
        if (usePlatformDefaultWidth != other.usePlatformDefaultWidth) return false
        if (backgroundContentAnimationSpec != other.backgroundContentAnimationSpec) return false
        if (elevation != other.elevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        result = 31 * result + usePlatformDefaultWidth.hashCode()
        result = 31 * result + backgroundContentAnimationSpec.hashCode()
        result = 31 * result + elevation.hashCode()
        return result
    }

    override fun toString(): String {
        return "SpatialDialogProperties(dismissOnBackPress=$dismissOnBackPress, dismissOnClickOutside=$dismissOnClickOutside, usePlatformDefaultWidth=$usePlatformDefaultWidth, restingLevelAnimationSpec=$backgroundContentAnimationSpec, spatialElevationLevel=$elevation)"
    }

    public fun copy(
        dismissOnBackPress: Boolean = this.dismissOnBackPress,
        dismissOnClickOutside: Boolean = this.dismissOnClickOutside,
        usePlatformDefaultWidth: Boolean = this.usePlatformDefaultWidth,
        restingLevelAnimationSpec: FiniteAnimationSpec<Float> = this.backgroundContentAnimationSpec,
        elevation: Dp = this.elevation,
    ): SpatialDialogProperties =
        SpatialDialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = usePlatformDefaultWidth,
            backgroundContentAnimationSpec = restingLevelAnimationSpec,
            elevation = elevation,
        )
}

private fun SpatialDialogProperties.toBaseDialogProperties() =
    DialogProperties(
        dismissOnClickOutside = dismissOnClickOutside,
        dismissOnBackPress = dismissOnBackPress,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
    )

private val EmptyContent: @Composable () -> Unit = {}

/**
 * [SpatialDialog] is a dialog that is elevated above the activity.
 *
 * When spatial dialogs are displayed the dialog appears on top of the content at the base elevation
 * level.
 *
 * In non-spatialized environments, a standard Compose Dialog is utilized to display the content.
 *
 * @param onDismissRequest a callback to be invoked when the dialog should be dismissed.
 * @param properties the dialog properties.
 * @param content the content of the dialog.
 */
@Composable
public fun SpatialDialog(
    onDismissRequest: () -> Unit,
    properties: SpatialDialogProperties = SpatialDialogProperties(),
    content: @Composable () -> Unit,
) {
    val movableContent = remember { movableContentOf(content) }
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        LayoutSpatialDialog(onDismissRequest, properties, movableContent)
    } else {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties.toBaseDialogProperties(),
            content = movableContent,
        )
    }
}

@Composable
private fun LayoutSpatialDialog(
    onDismissRequest: () -> Unit,
    properties: SpatialDialogProperties = SpatialDialogProperties(),
    content: @Composable () -> Unit,
) {
    // Start elevation at Level0 to prevent effects where the dialog flashes behind its parent.
    var spatialElevationLevel by remember { mutableStateOf(SpatialElevationLevel.Level0) }
    val dialogManager = LocalDialogManager.current
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val parentView = LocalView.current
    val parentEntity = findNearestParentEntity()
    val context = LocalContext.current
    val compositionContext = rememberCompositionContext()
    @Suppress("DEPRECATION") val localId = currentCompositeKeyHash

    BackHandler {
        if (properties.dismissOnBackPress) {
            // TODO(b/401028662) Investigate if we need the animation inside of this scope.
            dialogManager.isSpatialDialogActive.value = false
        }
    }
    DisposableEffect(Unit) {
        dialogManager.isSpatialDialogActive.value = true
        onDispose { dialogManager.isSpatialDialogActive.value = false }
    }

    LaunchedEffect(Unit) { spatialElevationLevel = properties.elevation }

    LaunchedEffect(dialogManager.isSpatialDialogActive.value) {
        if (!dialogManager.isSpatialDialogActive.value) {
            onDismissRequest()
        }
    }

    // Paint the scrim on the parent panel and capture dismiss events.
    Dialog(
        onDismissRequest = { dialogManager.isSpatialDialogActive.value = false },
        properties = properties.toBaseDialogProperties(),
    ) {
        // We need a very small (non-zero) content to fill the remaining space with the scrim.
        Spacer(Modifier.size(1.dp))
    }

    val zDepth by
        updateTransition(targetState = spatialElevationLevel, label = "restingLevelTransition")
            .animateFloat(
                transitionSpec = { properties.backgroundContentAnimationSpec },
                label = "zDepth",
            ) { state ->
                state.toMeter().toM()
            }

    val holder =
        remember(parentView) {
            SpatialDialogRenderer(
                context = context,
                parentView = parentView,
                compositionContext = compositionContext,
                session = session,
                localId = localId,
            )
        }

    SideEffect {
        holder.parentEntity = parentEntity
        holder.poseInMeters = Pose(translation = MeterPosition(z = zDepth.meters).toVector3())
        holder.content = content
    }
}

/**
 * A holder class that manages the lifecycle of a spatial dialog's rendering elements: a
 * Compose-backed [View] and its corresponding [CorePanelEntity] in the XR scene.
 *
 * This class implements [RememberObserver] to manage resource allocation ([onRemembered]) and
 * cleanup ([onForgotten]) within the Compose lifecycle. It acts as the bridge between the ephemeral
 * Compose state and the persistent native scene entity.
 *
 * All external configuration changes (pose, parent, content) are synchronized to the internal state
 * and the [CorePanelEntity].
 *
 * @param context The [Context] used to create the internal [ComposeView].
 * @param parentView The parent Android [View] used to establish View Tree ownership (Lifecycle,
 *   ViewModel, etc.).
 * @param compositionContext The parent's [CompositionContext] to ensure proper disposal.
 * @param session The active XR [Session] required to create the [PanelEntity].
 * @param localId A unique ID for saving/restoring the Compose state.
 */
private class SpatialDialogRenderer(
    private var context: Context,
    private var parentView: View,
    private var compositionContext: CompositionContext,
    private var session: Session,
    private var localId: Int,
) : RememberObserver {

    private var view: ComposeView? = null
    private var panelEntity: CorePanelEntity? = null
    var parentEntity: CoreEntity? = null
        set(value) {
            if (field != value) {
                field = value
                panelEntity?.parent = value
            }
        }

    var poseInMeters: Pose = Pose.Identity
        set(value) {
            if (field != value) {
                field = value
                panelEntity?.poseInMeters = value
            }
        }

    var content: @Composable () -> Unit by mutableStateOf(EmptyContent)

    override fun onRemembered() {

        val view =
            ComposeView(context).apply {
                id = View.generateViewId()

                setViewTreeLifecycleOwner(parentView.findViewTreeLifecycleOwner())
                setViewTreeViewModelStoreOwner(parentView.findViewTreeViewModelStoreOwner())
                setViewTreeSavedStateRegistryOwner(parentView.findViewTreeSavedStateRegistryOwner())
                setViewTreeDisjointParent(parentView as? ViewParent ?: parentView.parent)

                // Set the strategy to automatically dispose the composition
                // when the ComposeView is detached from the window.
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                // Dispose of the Composition when the view's LifecycleOwner is destroyed
                setParentCompositionContext(compositionContext)

                // Set unique id for AbstractComposeView. This allows state restoration
                // for the state defined inside the SpatialElevation via rememberSaveable().
                setTag(
                    androidx.compose.ui.R.id.compose_view_saveable_id_tag,
                    "ComposeView:$localId",
                )

                // Enable children to draw their shadow by not clipping them
                clipChildren = false
            }
        this.view = view
        panelEntity =
            view
                .let {
                    CorePanelEntity(
                        PanelEntity.create(
                            session = session,
                            view = it,
                            pixelDimensions = IntSize2d(IntSize.Zero.width, IntSize.Zero.height),
                            name = "ElevatedPanel:${it.id}",
                        )
                    )
                }
                .apply {
                    setShape(SpatialRoundedCornerShape(ZeroCornerSize), Density(1.0f))
                    parent = parentEntity
                    view.setTag(R.id.compose_xr_local_view_entity, this)
                }

        view.setContent {
            Box(
                modifier =
                    Modifier.constrainTo(Constraints()).onSizeChanged {
                        panelEntity?.size =
                            IntVolumeSize(width = it.width, height = it.height, depth = 0)
                    }
            ) {
                content()
            }
        }
    }

    override fun onForgotten() {
        panelEntity?.dispose()
        view?.disposeComposition()
    }

    override fun onAbandoned() {
        // No-op. If resources were created during 'init' (constructor),
        // they should be released here since onRemembered() was never called.
    }
}
