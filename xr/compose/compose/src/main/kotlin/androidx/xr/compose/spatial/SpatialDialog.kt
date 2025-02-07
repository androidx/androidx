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

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.xr.compose.platform.LocalDialogManager
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Session
import kotlinx.coroutines.launch

/**
 * Properties for configuring a [SpatialDialog].
 *
 * @property dismissOnBackPress whether the dialog should be dismissed when the device's back button
 *   is pressed. Defaults to `true`.
 * @property dismissOnClickOutside whether the dialog should be dismissed when the user touches
 *   outside of it. Defaults to `true`.
 * @property usePlatformDefaultWidth whether the dialog should use the platform's default width.
 *   Defaults to `true`.
 * @property restingLevelAnimationSpec the animation specification for the resting level.
 * @property spatialElevationLevel the elevation level of the dialog. Defaults to
 *   [SpatialElevationLevel.DialogDefault].
 */
public class SpatialDialogProperties(
    @get:Suppress("GetterSetterNames") public val dismissOnBackPress: Boolean = true,
    @get:Suppress("GetterSetterNames") public val dismissOnClickOutside: Boolean = true,
    @get:Suppress("GetterSetterNames") public val usePlatformDefaultWidth: Boolean = true,
    public val restingLevelAnimationSpec: FiniteAnimationSpec<Float> = spring(),
    public val spatialElevationLevel: SpatialElevationLevel = SpatialElevationLevel.DialogDefault,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialDialogProperties) return false

        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false
        if (usePlatformDefaultWidth != other.usePlatformDefaultWidth) return false
        if (restingLevelAnimationSpec != other.restingLevelAnimationSpec) return false
        if (spatialElevationLevel != other.spatialElevationLevel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        result = 31 * result + usePlatformDefaultWidth.hashCode()
        result = 31 * result + restingLevelAnimationSpec.hashCode()
        result = 31 * result + spatialElevationLevel.hashCode()
        return result
    }

    override fun toString(): String {
        return "SpatialDialogProperties(dismissOnBackPress=$dismissOnBackPress, dismissOnClickOutside=$dismissOnClickOutside, usePlatformDefaultWidth=$usePlatformDefaultWidth, restingLevelAnimationSpec=$restingLevelAnimationSpec, spatialElevationLevel=$spatialElevationLevel)"
    }

    public fun copy(
        dismissOnBackPress: Boolean = this.dismissOnBackPress,
        dismissOnClickOutside: Boolean = this.dismissOnClickOutside,
        usePlatformDefaultWidth: Boolean = this.usePlatformDefaultWidth,
        restingLevelAnimationSpec: FiniteAnimationSpec<Float> = this.restingLevelAnimationSpec,
        spatialElevationLevel: SpatialElevationLevel = this.spatialElevationLevel,
    ): SpatialDialogProperties =
        SpatialDialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = usePlatformDefaultWidth,
            restingLevelAnimationSpec = restingLevelAnimationSpec,
            spatialElevationLevel = spatialElevationLevel,
        )
}

private fun SpatialDialogProperties.toBaseDialogProperties() =
    DialogProperties(
        dismissOnClickOutside = dismissOnClickOutside,
        dismissOnBackPress = dismissOnBackPress,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
    )

/**
 * [SpatialDialog] is a dialog that is elevated above the activity.
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
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        LayoutSpatialDialog(onDismissRequest, properties, content)
    } else {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties.toBaseDialogProperties(),
            content = content,
        )
    }
}

@Composable
private fun LayoutSpatialDialog(
    onDismissRequest: () -> Unit,
    properties: SpatialDialogProperties = SpatialDialogProperties(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    // Start elevation at Level0 to prevent effects where the dialog flashes behind its parent.
    var spatialElevationLevel by remember { mutableStateOf(SpatialElevationLevel.Level0) }
    val dialogManager = LocalDialogManager.current

    DisposableEffect(Unit) {
        scope.launch {
            animate(
                initialValue = SpatialElevationLevel.ActivityDefault.level,
                targetValue = -properties.spatialElevationLevel.level,
                animationSpec = properties.restingLevelAnimationSpec,
            ) { value, _ ->
                session.setActivitySpaceZDepth(value.meters)
            }
        }
        dialogManager.isSpatialDialogActive.value = true
        onDispose {
            session.resetActivitySpaceZDepth()
            dialogManager.isSpatialDialogActive.value = false
        }
    }

    LaunchedEffect(Unit) { spatialElevationLevel = properties.spatialElevationLevel }

    LaunchedEffect(dialogManager.isSpatialDialogActive.value) {
        if (!dialogManager.isSpatialDialogActive.value) {
            onDismissRequest()
        }
    }

    // Paint the scrim on the parent panel and capture dismiss events.
    Dialog(
        onDismissRequest = {
            scope.launch {
                animate(
                    initialValue = -properties.spatialElevationLevel.level,
                    targetValue = SpatialElevationLevel.ActivityDefault.level,
                    animationSpec = properties.restingLevelAnimationSpec,
                ) { value, _ ->
                    session.setActivitySpaceZDepth(value.meters)
                }
            }
            dialogManager.isSpatialDialogActive.value = false
        },
        properties = properties.toBaseDialogProperties(),
    ) {
        // We need a very small (non-zero) content to fill the remaining space with the scrim.
        Spacer(Modifier.size(1.dp))
    }

    var contentSize by remember { mutableStateOf(view.size) }

    val zDepth by
        updateTransition(targetState = spatialElevationLevel, label = "restingLevelTransition")
            .animateFloat(
                transitionSpec = { properties.restingLevelAnimationSpec },
                label = "zDepth"
            ) { state ->
                state.level
            }

    ElevatedPanel(
        contentSize = contentSize,
        pose = Pose(translation = MeterPosition(z = zDepth.meters).toVector3()),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.onSizeChanged { contentSize = it }) { content() }
        }
    }
}

private fun Session.setActivitySpaceZDepth(value: Meter) {
    activitySpace.setPose(Pose(translation = Vector3(0f, 0f, value.toM())))
}

private fun Session.resetActivitySpaceZDepth() {
    setActivitySpaceZDepth(SpatialElevationLevel.ActivityDefault.level.meters)
}
