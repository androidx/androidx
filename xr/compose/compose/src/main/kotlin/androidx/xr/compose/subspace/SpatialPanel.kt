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
import androidx.annotation.RestrictTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.core.graphics.drawable.toDrawable
import androidx.xr.compose.platform.LocalCoreMainPanelEntity
import androidx.xr.compose.platform.LocalDialogManager
import androidx.xr.compose.platform.LocalOpaqueEntity
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.disposableValueOf
import androidx.xr.compose.platform.getActivity
import androidx.xr.compose.platform.getValue
import androidx.xr.compose.subspace.layout.CorePanelEntity
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SpatialShape
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceMeasurePolicy
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.node.ComposeSubspaceNode
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetCompositionLocalMap
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetCoreEntity
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetMeasurePolicy
import androidx.xr.compose.subspace.node.ComposeSubspaceNode.Companion.SetModifier
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.compose.unit.Meter
import androidx.xr.compose.unit.Meter.Companion.millimeters
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.PanelEntity
import kotlin.math.roundToInt

private const val DEFAULT_SIZE_PX = 400

// Max allowed size for makeMeasureSpec is (1 << MeasureSpec.MODE_SHIFT) - 1.
private const val MAX_MEASURE_SPEC_SIZE = (1 shl 30) - 1

/** Set the scrim alpha to 32% opacity across all spatial panels. */
private const val DEFAULT_SCRIM_ALPHA = 0x52000000

private object SpatialPanelDimensions {
    /** Default minimum dimensions for a Spatial Panel in Meters. */
    public val minimumPanelDimension: FloatSize2d = FloatSize2d(0.1f, 0.1f)
}

/** Contains default values used by spatial panels. */
public object SpatialPanelDefaults {

    /** Default shape for a Spatial Panel. */
    public val shape: SpatialShape = SpatialRoundedCornerShape(CornerSize(32.dp))
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
 */
@Composable
@SubspaceComposable
public fun <T : View> SpatialAndroidViewPanel(
    factory: (Context) -> T,
    modifier: SubspaceModifier = SubspaceModifier,
    update: (T) -> Unit = {},
    shape: SpatialShape = SpatialPanelDefaults.shape,
) {
    val dialogManager = LocalDialogManager.current
    val context = LocalContext.current

    @Suppress("UnnecessaryLambdaCreation")
    AndroidViewPanel(
        factory = { factory(context) },
        modifier = modifier,
        update = { view ->
            if (dialogManager.isSpatialDialogActive.value) {
                view.foreground = DEFAULT_SCRIM_ALPHA.toDrawable()
                view.setOnClickListener { dialogManager.isSpatialDialogActive.value = false }
            } else {
                view.foreground = Color.TRANSPARENT.toDrawable()
                view.setOnClickListener(null)
            }
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

    val corePanelEntity =
        rememberCorePanelEntity(shape = shape) {
            PanelEntity.create(
                session = this,
                view = view,
                dimensions = SpatialPanelDimensions.minimumPanelDimension,
                name = "ViewPanel",
                pose = Pose.Identity,
            )
        }

    val measurePolicy = SubspaceMeasurePolicy { _, constraints ->
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
        val width = view.measuredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = view.measuredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val depth = constraints.minDepth.coerceAtLeast(0)
        layout(width, height, depth) {}
    }

    val compositionLocalMap = currentComposer.currentCompositionLocalMap
    ComposeNode<ComposeSubspaceNode, Applier<Any>>(
        factory = ComposeSubspaceNode.Constructor,
        update = {
            set(compositionLocalMap, SetCompositionLocalMap)
            set(measurePolicy, SetMeasurePolicy)
            set(corePanelEntity, SetCoreEntity)
            // TODO(b/390674036) Remove call-order dependency between SetCoreEntity and SetModifier
            // Execute SetModifier after SetCoreEntity, it depends on CoreEntity.
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
 * @param content The composable content to render within the SpatialPanel.
 */
@Composable
@SubspaceComposable
public fun SpatialPanel(
    modifier: SubspaceModifier = SubspaceModifier,
    shape: SpatialShape = SpatialPanelDefaults.shape,
    content: @Composable @UiComposable () -> Unit,
) {
    val view = rememberComposeView()
    val dialogManager = LocalDialogManager.current
    val corePanelEntity =
        rememberCorePanelEntity(shape = shape) {
            PanelEntity.create(
                session = this,
                view = view,
                dimensions = SpatialPanelDimensions.minimumPanelDimension,
                name = entityName("SpatialPanel"),
                pose = Pose.Identity,
            )
        }
    var intrinsicWidth by remember { mutableIntStateOf(DEFAULT_SIZE_PX) }
    var intrinsicHeight by remember { mutableIntStateOf(DEFAULT_SIZE_PX) }
    val scrimDrawable = remember { DEFAULT_SCRIM_ALPHA.toDrawable() }
    val transparentDrawable = remember { Color.TRANSPARENT.toDrawable() }

    SubspaceLayout(modifier = modifier, coreEntity = corePanelEntity) { _, volumeConstraints ->
        view.setContent {
            CompositionLocalProvider(LocalOpaqueEntity provides corePanelEntity) {
                Layout(content = content, modifier = Modifier) { measurables, constraints ->
                    intrinsicWidth =
                        measurables.fastMaxOfOrNull {
                            try {
                                it.maxIntrinsicWidth(volumeConstraints.maxHeight)
                            } catch (_: IllegalStateException) {
                                0
                            }
                        } ?: DEFAULT_SIZE_PX
                    intrinsicHeight =
                        measurables.fastMaxOfOrNull {
                            try {
                                it.maxIntrinsicHeight(volumeConstraints.maxWidth)
                            } catch (_: IllegalStateException) {
                                0
                            }
                        } ?: DEFAULT_SIZE_PX
                    val placeables = measurables.map { it.measure(constraints) }
                    layout(
                        placeables.fastMaxOfOrNull { it.measuredWidth } ?: DEFAULT_SIZE_PX,
                        placeables.fastMaxOfOrNull { it.measuredHeight } ?: DEFAULT_SIZE_PX,
                    ) {
                        placeables.fastForEach { placeable -> placeable.place(0, 0) }
                    }
                }
            }
            SideEffect {
                view.foreground =
                    if (dialogManager.isSpatialDialogActive.value) scrimDrawable
                    else transparentDrawable
            }
            if (dialogManager.isSpatialDialogActive.value) {
                Box(
                    modifier =
                        Modifier.fillMaxSize().pointerInput(Unit) {
                            detectTapGestures { dialogManager.isSpatialDialogActive.value = false }
                        }
                ) {}
            }
        }
        val width = intrinsicWidth.coerceIn(volumeConstraints.minWidth, volumeConstraints.maxWidth)
        val height =
            intrinsicHeight.coerceIn(volumeConstraints.minHeight, volumeConstraints.maxHeight)
        val depth = volumeConstraints.minDepth.coerceAtLeast(0)
        layout(width, height, depth) {}
    }
}

/**
 * Creates a [SpatialPanel] backed by the main Window content.
 *
 * This panel requires the following specific configuration in the Android Manifest for proper
 * sizing/resizing behavior:
 * ```
 * <activity
 * android:configChanges="orientation|screenSize|screenLayout|smallestScreenSize>
 * <!--suppress AndroidElementNotAllowed -->
 * <layout android:defaultWidth="50dp" android:defaultHeight="50dp" android:minHeight="50dp"
 * android:minWidth="50dp"/>
 * </activity>
 * ```
 *
 * @param modifier SubspaceModifier to apply to the MainPanel.
 * @param shape The shape of this Spatial Panel.
 */
@Composable
@SubspaceComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SpatialMainPanel(
    modifier: SubspaceModifier = SubspaceModifier,
    shape: SpatialShape = SpatialPanelDefaults.shape,
) {
    val mainPanel = LocalCoreMainPanelEntity.current ?: return
    val density = LocalDensity.current
    LaunchedEffect(shape, density) { mainPanel.setShape(shape, density) }

    val view = LocalContext.current.getActivity().window?.decorView ?: LocalView.current

    // When the mainPanel enters the compose hierarchy, we can't directly set the mainPanel.hidden
    // to false here because the hidden state is a subcomponent of the size calculation, see
    // [SubspaceLayoutNode.MeasureLayout.placeAt] and [CoreEntity.size].
    // This means hidden will be set after layout completes, on the first frame when the mainPanel
    // enters the Compose hierarchy.
    DisposableEffect(mainPanel) { onDispose { mainPanel.hidden = true } }

    SubspaceLayout(modifier = modifier, coreEntity = mainPanel) { _, constraints ->
        val width = view.measuredWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = view.measuredHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val depth = constraints.minDepth.coerceAtLeast(0)
        layout(width, height, depth) {}
    }
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
 */
@Composable
@SubspaceComposable
public fun SpatialActivityPanel(
    intent: Intent,
    modifier: SubspaceModifier = SubspaceModifier,
    shape: SpatialShape = SpatialPanelDefaults.shape,
) {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val dialogManager = LocalDialogManager.current
    val density = LocalDensity.current

    val pixelDimensions = IntSize2d(DEFAULT_SIZE_PX, DEFAULT_SIZE_PX)

    val activityPanelEntity: ActivityPanelEntity by
        remember(session, pixelDimensions) {
            disposableValueOf(
                ActivityPanelEntity.create(
                    session,
                    pixelDimensions,
                    entityName("ActivityPanel-${intent.action}"),
                )
            ) {
                it.dispose()
            }
        }

    val corePanelEntity: CorePanelEntity by
        remember(activityPanelEntity, density) {
            disposableValueOf(CorePanelEntity(activityPanelEntity)) { it.dispose() }
        }

    SideEffect { corePanelEntity.setShape(shape, density) }

    LaunchedEffect(intent) {
        (corePanelEntity.entity as ActivityPanelEntity).launchActivity(intent)
    }

    SpatialBox {
        SubspaceLayout(modifier = modifier, coreEntity = corePanelEntity) { _, constraints ->
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

            val scrimWidth = Meter.fromPixel(activityPanelEntity.size.width, density)
            val scrimHeight = Meter.fromPixel(activityPanelEntity.size.height, density)
            val scrimPanelEntity =
                rememberCorePanelEntity(shape = shape) {
                    PanelEntity.create(
                            session = session,
                            view = scrimView,
                            dimensions = FloatSize2d(scrimWidth.toM(), scrimHeight.toM()),
                            name = entityName("ScrimPanel"),
                            pose = Pose.Identity,
                        )
                        .apply {
                            parent = corePanelEntity.entity
                            setPose(Pose(translation = Vector3(0f, 0f, 3.millimeters.toM())))
                        }
                }
            SideEffect {
                scrimPanelEntity.size =
                    IntVolumeSize(
                        activityPanelEntity.size.width.roundToInt(),
                        activityPanelEntity.size.height.roundToInt(),
                        0,
                    )
            }
        }
    }
}
