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
import android.graphics.Rect
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.xr.compose.R
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.findNearestParentEntity
import androidx.xr.compose.platform.getActivity
import androidx.xr.compose.platform.isEmbedded
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.subspace.layout.CorePanelEntity
import androidx.xr.compose.subspace.spatialComposeView
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.PanelEntity

/**
 * A composable that creates a panel in 3D space to hoist Popup based composables.
 *
 * In non-spatialized environments or embedded activities, a standard Compose [Popup] is utilized to
 * display the content.
 *
 * @param alignment the alignment of the popup relative to its parent.
 * @param offset An offset from the original aligned position of the popup. Offset respects the
 *   Ltr/Rtl context, thus in Ltr it will be added to the original aligned position and in Rtl it
 *   will be subtracted from it.
 * @param onDismissRequest callback invoked when the user requests to dismiss the popup (e.g., by
 *   clicking outside).
 * @param elevation the elevation value of the SpatialPopUp.
 * @param properties [PopupProperties] configuration properties for further customization of this
 *   popup's behavior.
 * @param content the composable content to be displayed within the popup.
 */
@Composable
public fun SpatialPopup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    onDismissRequest: (() -> Unit)? = null,
    elevation: Dp = SpatialElevationLevel.Level3,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit,
) {
    val activity = LocalContext.current.getActivity()
    val movableContent = remember { movableContentOf(content) }
    val isActivityEmbedded = activity?.isEmbedded() ?: false

    if (!isActivityEmbedded && LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        LayoutSpatialPopup(
            alignment = alignment,
            offset = offset,
            onDismissRequest = onDismissRequest,
            properties = properties,
            elevation = elevation,
            content = movableContent,
        )
    } else {
        Popup(
            alignment = alignment,
            offset = offset,
            onDismissRequest = onDismissRequest,
            properties = properties,
            content = movableContent,
        )
    }
}

/**
 * Composable that creates a panel in 3d space to hoist Popup based composables.
 *
 * @param alignment The alignment relative to the parent.
 * @param offset An offset from the original aligned position of the popup. Offset respects the
 *   Ltr/Rtl context, thus in Ltr it will be added to the original aligned position and in Rtl it
 *   will be subtracted from it.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param elevation the elevation value of the SpatialPopUp.`
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param content The content to be displayed inside the popup.
 */
@Composable
private fun LayoutSpatialPopup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset(0, 0),
    onDismissRequest: (() -> Unit)? = null,
    elevation: Dp = SpatialElevationLevel.Level3,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit,
) {
    val popupPositioner =
        remember(alignment, offset) { AlignmentOffsetPositionProvider(alignment, offset) }
    LayoutSpatialPopup(
        popupPositionProvider = popupPositioner,
        onDismissRequest = onDismissRequest,
        properties = properties,
        elevation = elevation,
        content = content,
    )
}

/**
 * Opens a popup with the given content.
 *
 * The popup is positioned using a custom [popupPositionProvider].
 *
 * @param popupPositionProvider Provides the screen position of the popup.
 * @param onDismissRequest Executes when the user clicks outside of the popup.
 * @param elevation the elevation value of the SpatialPopUp.
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param content The content to be displayed inside the popup.
 */
@Composable
private fun LayoutSpatialPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)? = null,
    elevation: Dp = SpatialElevationLevel.Level3,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit,
) {
    val fullScreenRect = getWindowVisibleDisplayFrame()
    val windowSize = IntSize(fullScreenRect.width(), fullScreenRect.height())
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val parentView = LocalView.current
    val parentLayoutDirection = LocalLayoutDirection.current
    val parentEntity = findNearestParentEntity()
    val transition = updateTransition(targetState = elevation, label = "restingLevelTransition")
    val context = LocalContext.current
    val compositionContext = rememberCompositionContext()
    // TODO(b/474652577): Update from deprecated currentCompositeKey to currentCompositeKeyCode
    //  once we update JXR Compose to Compile SDK 35
    @Suppress("DEPRECATION") val localId = currentCompositeKeyHash

    BackHandler(enabled = properties.dismissOnBackPress) { onDismissRequest?.invoke() }

    val holder =
        remember(parentView) {
            SpatialPopupRenderer(
                localId = localId,
                context = context,
                parentView = parentView,
                compositionContext = compositionContext,
                session = session,
                transition = transition,
                initialPopupProperties = properties,
                initialParentLayoutDirection = parentLayoutDirection,
                initialPopupPositionProvider = popupPositionProvider,
                initialOnDismissRequest = onDismissRequest,
            )
        }

    DisposableEffect(parentView) {
        val listener =
            View.OnLayoutChangeListener { _, _, _, right, bottom, _, _, _, _ ->
                holder.parentViewSize = IntSize(right, bottom)
            }
        parentView.addOnLayoutChangeListener(listener)
        onDispose { parentView.removeOnLayoutChangeListener(listener) }
    }

    Layout(modifier = Modifier) { _, _ ->
        holder.parentLayoutDirection = layoutDirection
        layout(0, 0) {
            // Access coordinates immediately during placement phase
            val coordinatePair = coordinates
            if (coordinatePair != null) {
                val parentCoordinates = coordinatePair.parentLayoutCoordinates
                if (parentCoordinates != null) {
                    val layoutSize = parentCoordinates.size
                    val position = parentCoordinates.positionInWindow()
                    val layoutPosition =
                        IntOffset(position.x.fastRoundToInt(), position.y.fastRoundToInt())

                    holder.anchorBounds = IntRect(layoutPosition, layoutSize)
                }
            }
        }
    }

    SideEffect {
        holder.parentEntity = parentEntity
        holder.popupProperties = properties
        holder.content = content
        holder.positionProvider = popupPositionProvider
        holder.windowSize = windowSize
        holder.onDismissRequest = onDismissRequest
    }
}

private val EmptyContent: @Composable () -> Unit = {}

/**
 * A helper class that manages the lifecycle, rendering, and 3D positioning of a spatial popup.
 *
 * This renderer implements [RememberObserver] to bridge the Jetpack Compose lifecycle with the
 * underlying XR resources. It is responsible for:
 * - Creating and disposing the [CorePanelEntity] and off-screen [ComposeView] when the popup enters
 *   or leaves the composition.
 * - Hoisting the user's content into the [ComposeView] and measuring it.
 * - continuously calculating and updating the 3D pose of the panel based on the provided
 *   [PopupPositionProvider], anchor bounds, and Z-depth transition.
 */
private class SpatialPopupRenderer(
    private val localId: Int,
    private val context: Context,
    private val parentView: View,
    private val compositionContext: CompositionContext,
    private val session: Session,
    private val transition: Transition<Dp>,
    initialPopupProperties: PopupProperties,
    initialParentLayoutDirection: LayoutDirection,
    initialPopupPositionProvider: PopupPositionProvider,
    initialOnDismissRequest: (() -> Unit)?,
) : RememberObserver {

    var popupProperties by mutableStateOf(initialPopupProperties)
    var positionProvider by mutableStateOf(initialPopupPositionProvider)
    var onDismissRequest by mutableStateOf(initialOnDismissRequest)

    var parentLayoutDirection by mutableStateOf(initialParentLayoutDirection)
    var anchorBounds by mutableStateOf(IntRect.Zero)
    var parentViewSize by mutableStateOf(parentView.size)
    var windowSize by mutableStateOf(IntSize.Zero)

    var content: @Composable () -> Unit by mutableStateOf(EmptyContent)
    var parentEntity: CoreEntity? by mutableStateOf(null)

    private var panelEntity: CorePanelEntity? = null
    private var view: ComposeView? = null

    override fun onRemembered() {
        val view = spatialComposeView(parentView, context, compositionContext, localId)
        this.view = view
        panelEntity =
            CorePanelEntity(
                    PanelEntity.create(
                        session = session,
                        view = view,
                        pixelDimensions = IntSize2d(IntSize.Zero.width, IntSize.Zero.height),
                        name = "ElevatedPanel:${view.id}",
                    )
                )
                .apply { view.setTag(R.id.compose_xr_local_view_entity, this) }

        view.setContent {
            val density = LocalDensity.current
            val zDepth by
                transition.animateDp(transitionSpec = { spring() }, label = "zDepth") { it }

            Layout(
                content = content,
                modifier =
                    Modifier.onClickOutside(
                            enabled = popupProperties.dismissOnClickOutside,
                            onClickOutside = { onDismissRequest?.invoke() },
                        )
                        .constrainTo(Constraints()),
            ) { measurables, constraints ->
                val placeables = measurables.fastMap { it.measure(constraints) }
                val contentSize =
                    placeables.fastFold(IntSize.Zero) { acc, placeable ->
                        IntSize(
                            acc.width.coerceAtLeast(placeable.width),
                            acc.height.coerceAtLeast(placeable.height),
                        )
                    }

                val currentParent = parentEntity
                val provider = positionProvider

                if (!anchorBounds.isEmpty) {
                    val popupOffset =
                        provider.calculatePosition(
                            anchorBounds,
                            windowSize,
                            parentLayoutDirection,
                            contentSize,
                        )

                    panelEntity?.apply {
                        size = IntVolumeSize(contentSize.width, contentSize.height, 0)
                        poseInMeters =
                            calculatePose(
                                contentOffset =
                                    Offset(popupOffset.x.toFloat(), popupOffset.y.toFloat()),
                                parentViewSize = parentViewSize,
                                contentSize = contentSize,
                                zDepth = zDepth,
                                density = density,
                            )
                        parent = currentParent
                    }
                }

                layout(contentSize.width, contentSize.height) {
                    placeables.fastForEach { it.place(0, 0) }
                }
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

// Get the visible display Rect for the current window of the device
@Composable
private fun getWindowVisibleDisplayFrame(): Rect {
    return Rect().apply { LocalView.current.getWindowVisibleDisplayFrame(this) }
}

/** Calculates the position of a [Popup] on a screen. */
@Immutable
private interface PopupPositionProvider {
    /**
     * Calculates the position of a [Popup] on screen.
     *
     * The window size is useful in cases where the popup is meant to be positioned next to its
     * anchor instead of inside of it. The size can be used to calculate available space around the
     * parent to find a spot with enough clearance (e.g. when implementing a dropdown). Note that
     * positioning the popup outside of the window bounds might prevent it from being visible.
     *
     * @param anchorBounds the bounds of the anchor layout relative to the window.
     * @param windowSize The size of the window containing the anchor layout.
     * @param layoutDirection The layout direction of the anchor layout.
     * @param popupContentSize The size of the popup's content.
     * @return The window relative position where the popup should be positioned.
     */
    fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset
}

/** Calculates the position of a [Popup] on screen. */
private class AlignmentOffsetPositionProvider(val alignment: Alignment, val offset: IntOffset) :
    PopupPositionProvider {

    /**
     * Calculates the position of a [Popup] on screen.
     *
     * The window size is useful in cases where the popup is meant to be positioned next to its
     * anchor instead of inside of it. The size can be used to calculate available space around the
     * parent to find a spot with enough clearance (e.g. when implementing a dropdown). Note that
     * positioning the popup outside of the window bounds might prevent it from being visible.
     *
     * @param anchorBounds The window relative bounds of the layout which this popup is anchored to.
     * @param windowSize The size of the window containing the anchor layout.
     * @param layoutDirection The layout direction of the anchor layout.
     * @param popupContentSize The size of the popup's content.
     * @return The window relative position where the popup should be positioned.
     */
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchorAlignmentPoint = alignment.align(IntSize.Zero, anchorBounds.size, layoutDirection)
        // Note the negative sign. Popup alignment point contributes negative offset.
        val popupAlignmentPoint = -alignment.align(IntSize.Zero, popupContentSize, layoutDirection)
        val resolvedUserOffset =
            IntOffset(offset.x * (if (layoutDirection == LayoutDirection.Ltr) 1 else -1), offset.y)

        return anchorBounds.topLeft +
            anchorAlignmentPoint +
            popupAlignmentPoint +
            resolvedUserOffset
    }
}
