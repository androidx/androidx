/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3

import android.content.Context
import android.graphics.Outline
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.WindowManager
import androidx.activity.BackEventCompat
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.internal.PredictiveBack
import androidx.compose.material3.internal.shouldApplySecureFlag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.view.WindowCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Logic forked from androidx.compose.ui.window.DialogProperties. Removed dismissOnClickOutside
// and usePlatformDefaultWidth as they are not relevant for fullscreen experience.
/**
 * Properties used to customize the behavior of a [ModalBottomSheet].
 *
 * @param securePolicy Policy for setting [WindowManager.LayoutParams.FLAG_SECURE] on the bottom
 *   sheet's window.
 * @param shouldDismissOnBackPress Whether the modal bottom sheet can be dismissed by pressing the
 *   back button. If true, pressing the back button will call onDismissRequest.
 */
@Immutable
@ExperimentalMaterial3Api
actual class ModalBottomSheetProperties {
    val securePolicy: SecureFlagPolicy
    actual val shouldDismissOnBackPress: Boolean
    @get:JvmName("shouldDismissOnClickOutside") actual val shouldDismissOnClickOutside: Boolean
    internal val isAppearanceLightStatusBars: Boolean?
    internal val isAppearanceLightNavigationBars: Boolean?

    /**
     * Properties used to customize the behavior of a [ModalBottomSheet].
     *
     * This constructor provides default behavior for [ModalBottomSheet]. See other constructors for
     * customization options.
     */
    constructor() {
        this.securePolicy = SecureFlagPolicy.Inherit
        this.shouldDismissOnBackPress = true
        this.shouldDismissOnClickOutside = true
        this.isAppearanceLightStatusBars = null
        this.isAppearanceLightNavigationBars = null
    }

    actual constructor(shouldDismissOnBackPress: Boolean, shouldDismissOnClickOutside: Boolean) {
        this.securePolicy = SecureFlagPolicy.Inherit
        this.shouldDismissOnBackPress = shouldDismissOnBackPress
        this.shouldDismissOnClickOutside = shouldDismissOnClickOutside
        this.isAppearanceLightNavigationBars = null
        this.isAppearanceLightStatusBars = null
    }

    /**
     * Properties used to customize the behavior of a [ModalBottomSheet].
     *
     * @param securePolicy Policy for setting [WindowManager.LayoutParams.FLAG_SECURE] on the bottom
     *   sheet's window.
     * @param shouldDismissOnBackPress Whether the modal bottom sheet can be dismissed by pressing
     *   the back button. If true, pressing the back button will call onDismissRequest.
     * @param shouldDismissOnClickOutside Whether the modal bottom sheet can be dismissed by
     *   clicking on the scrim.
     */
    constructor(
        securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
        shouldDismissOnBackPress: Boolean = true,
        shouldDismissOnClickOutside: Boolean = true,
    ) {
        this.securePolicy = securePolicy
        this.shouldDismissOnBackPress = shouldDismissOnBackPress
        this.shouldDismissOnClickOutside = shouldDismissOnClickOutside
        this.isAppearanceLightNavigationBars = null
        this.isAppearanceLightStatusBars = null
    }

    /**
     * Properties used to customize the behavior of a [ModalBottomSheet].
     *
     * Use this constructor to customize the behavior of status and navigation bars on the
     * [ModalBottomSheet] window.
     *
     * @param isAppearanceLightStatusBars If true, changes the foreground color of the status bars
     *   to light so that the items on the bar can be read clearly. If false, reverts to the default
     *   appearance.
     * @param isAppearanceLightNavigationBars If true, changes the foreground color of the
     *   navigation bars to light so that the items on the bar can be read clearly. If false,
     *   reverts to the default appearance.
     * @param securePolicy Policy for setting [WindowManager.LayoutParams.FLAG_SECURE] on the bottom
     *   sheet's window.
     * @param shouldDismissOnBackPress Whether the modal bottom sheet can be dismissed by pressing
     *   the back button. If true, pressing the back button will call onDismissRequest.
     * @param shouldDismissOnClickOutside Whether the modal bottom sheet can be dismissed by
     *   clicking on the scrim.
     */
    constructor(
        isAppearanceLightStatusBars: Boolean,
        isAppearanceLightNavigationBars: Boolean,
        securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
        shouldDismissOnBackPress: Boolean = true,
        shouldDismissOnClickOutside: Boolean = true,
    ) {
        this.shouldDismissOnBackPress = shouldDismissOnBackPress
        this.shouldDismissOnClickOutside = shouldDismissOnClickOutside
        this.securePolicy = securePolicy
        this.isAppearanceLightStatusBars = isAppearanceLightStatusBars
        this.isAppearanceLightNavigationBars = isAppearanceLightNavigationBars
    }

    @Deprecated(
        level = DeprecationLevel.HIDDEN,
        message = "Replaced with additional shouldDismissOnScrimClick param constructor.",
    )
    actual constructor(shouldDismissOnBackPress: Boolean) : this(shouldDismissOnBackPress, true)

    @Deprecated(
        message = "Use empty constructor or constructor including shouldDismissOnScrimClick param.",
        level = DeprecationLevel.HIDDEN,
    )
    constructor(
        securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
        shouldDismissOnBackPress: Boolean = true,
    ) : this(securePolicy, shouldDismissOnBackPress, true)

    @Deprecated(
        message = "Use empty constructor or constructor including shouldDismissOnScrimClick param.",
        level = DeprecationLevel.HIDDEN,
    )
    constructor(
        isAppearanceLightStatusBars: Boolean,
        isAppearanceLightNavigationBars: Boolean,
        securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
        shouldDismissOnBackPress: Boolean = true,
    ) {
        this.shouldDismissOnBackPress = shouldDismissOnBackPress
        this.shouldDismissOnClickOutside = true
        this.securePolicy = securePolicy
        this.isAppearanceLightStatusBars = isAppearanceLightStatusBars
        this.isAppearanceLightNavigationBars = isAppearanceLightNavigationBars
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModalBottomSheetProperties) return false
        if (securePolicy != other.securePolicy) return false
        if (isAppearanceLightStatusBars != other.isAppearanceLightStatusBars) return false
        if (isAppearanceLightNavigationBars != other.isAppearanceLightNavigationBars) return false
        if (shouldDismissOnClickOutside != other.shouldDismissOnClickOutside) return false
        if (shouldDismissOnBackPress != other.shouldDismissOnBackPress) return false
        return true
    }

    override fun hashCode(): Int {
        var result = securePolicy.hashCode()
        result = 31 * result + shouldDismissOnBackPress.hashCode()
        result = 31 * result + (isAppearanceLightStatusBars?.hashCode() ?: 0)
        result = 31 * result + (isAppearanceLightNavigationBars?.hashCode() ?: 0)
        result = 31 * result + shouldDismissOnClickOutside.hashCode()
        return result
    }
}

/** Default values for [ModalBottomSheet] */
@Immutable
@ExperimentalMaterial3Api
actual object ModalBottomSheetDefaults {

    /** Properties used to customize the behavior of a [ModalBottomSheet]. */
    actual val properties = ModalBottomSheetProperties()

    /**
     * Properties used to customize the behavior of a [ModalBottomSheet].
     *
     * @param securePolicy Policy for setting [WindowManager.LayoutParams.FLAG_SECURE] on the bottom
     *   sheet's window.
     * @param isFocusable Whether the modal bottom sheet is focusable. When true, the modal bottom
     *   sheet will receive IME events and key presses, such as when the back button is pressed.
     * @param shouldDismissOnBackPress Whether the modal bottom sheet can be dismissed by pressing
     *   the back button. If true, pressing the back button will call onDismissRequest. Note that
     *   [isFocusable] must be set to true in order to receive key events such as the back button -
     *   if the modal bottom sheet is not focusable then this property does nothing.
     */
    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "'isFocusable' param is no longer used. Use value without this parameter.",
        replaceWith = ReplaceWith("properties"),
    )
    @Suppress("UNUSED_PARAMETER")
    fun properties(
        securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
        isFocusable: Boolean = true,
        shouldDismissOnBackPress: Boolean = true,
    ) =
        ModalBottomSheetProperties(
            securePolicy = securePolicy,
            shouldDismissOnBackPress = shouldDismissOnBackPress,
        )
}

/**
 * [Material Design modal bottom sheet](https://m3.material.io/components/bottom-sheets/overview)
 *
 * Modal bottom sheets are used as an alternative to inline menus or simple dialogs on mobile,
 * especially when offering a long list of action items, or when items require longer descriptions
 * and icons. Like dialogs, modal bottom sheets appear in front of app content, disabling all other
 * app functionality when they appear, and remaining on screen until confirmed, dismissed, or a
 * required action has been taken.
 *
 * ![Bottom sheet
 * image](https://developer.android.com/images/reference/androidx/compose/material3/bottom_sheet.png)
 *
 * A simple example of a modal bottom sheet looks like this:
 *
 * @sample androidx.compose.material3.samples.ModalBottomSheetSample
 * @param onDismissRequest Executes when the user clicks outside of the bottom sheet, after sheet
 *   animates to [Hidden].
 * @param modifier Optional [Modifier] for the bottom sheet.
 * @param sheetState The state of the bottom sheet.
 * @param sheetMaxWidth [Dp] that defines what the maximum width the sheet will take. Pass in
 *   [Dp.Unspecified] for a sheet that spans the entire screen width.
 * @param shape The shape of the bottom sheet.
 * @param containerColor The color used for the background of this bottom sheet
 * @param contentColor The preferred color for content inside this bottom sheet. Defaults to either
 *   the matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param scrimColor Color of the scrim that obscures content when the bottom sheet is open.
 * @param dragHandle Optional visual marker to swipe the bottom sheet.
 * @param windowInsets window insets to be passed to the bottom sheet content via [PaddingValues]
 *   params.
 * @param properties [ModalBottomSheetProperties] for further customization of this modal bottom
 *   sheet's window behavior.
 * @param content The content to be displayed inside the bottom sheet.
 */
@Composable
@ExperimentalMaterial3Api
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Use constructor with contentWindowInsets parameter.",
    replaceWith =
        ReplaceWith(
            "ModalBottomSheet(" +
                "onDismissRequest," +
                "modifier," +
                "sheetState," +
                "sheetMaxWidth," +
                "shape," +
                "containerColor," +
                "contentColor," +
                "tonalElevation," +
                "scrimColor," +
                "dragHandle," +
                "{ windowInsets }," +
                "properties," +
                "content," +
                ")"
        ),
)
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    windowInsets: WindowInsets = BottomSheetDefaults.windowInsets,
    properties: ModalBottomSheetProperties = ModalBottomSheetDefaults.properties,
    content: @Composable ColumnScope.() -> Unit,
) =
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetMaxWidth = sheetMaxWidth,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        contentWindowInsets = { windowInsets },
        properties = properties,
        content = content,
    )

// Fork of androidx.compose.ui.window.AndroidDialog_androidKt.Dialog
// Added predictiveBackProgress param to pass into BottomSheetDialogWrapper.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun ModalBottomSheetDialog(
    onDismissRequest: () -> Unit,
    contentColor: Color,
    properties: ModalBottomSheetProperties,
    predictiveBackProgress: Animatable<Float, AnimationVector1D>,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val composition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val dialogId = rememberSaveable { UUID.randomUUID() }
    val scope = rememberCoroutineScope()
    val dialog =
        remember(view, density) {
            ModalBottomSheetDialogWrapper(
                    onDismissRequest,
                    properties,
                    contentColor,
                    view,
                    layoutDirection,
                    density,
                    dialogId,
                    predictiveBackProgress,
                    scope,
                )
                .apply {
                    setContent(composition) {
                        Box(Modifier.semantics { dialog() }) { currentContent() }
                    }
                }
        }

    DisposableEffect(dialog) {
        dialog.show()

        onDispose {
            dialog.dismiss()
            dialog.disposeComposition()
        }
    }

    SideEffect {
        dialog.updateParameters(
            onDismissRequest = onDismissRequest,
            properties = properties,
            contentColor = contentColor,
            layoutDirection = layoutDirection,
        )
    }
}

// Fork of androidx.compose.ui.window.DialogLayout
// Additional parameters required for current predictive back implementation.
@Suppress("ViewConstructor")
private class ModalBottomSheetDialogLayout(context: Context, override val window: Window) :
    AbstractComposeView(context), DialogWindowProvider {

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
        createComposition()
    }

    // Display width and height logic removed, size will always span fillMaxSize().

    @Composable
    override fun Content() {
        content()
    }
}

// Fork of androidx.compose.ui.window.DialogWrapper.
// predictiveBackProgress and scope params added for predictive back implementation.
// EdgeToEdgeFloatingDialogWindowTheme provided to allow theme to extend into status bar.
@ExperimentalMaterial3Api
private class ModalBottomSheetDialogWrapper(
    private var onDismissRequest: () -> Unit,
    private var properties: ModalBottomSheetProperties,
    private var contentColor: Color,
    private val composeView: View,
    layoutDirection: LayoutDirection,
    density: Density,
    dialogId: UUID,
    predictiveBackProgress: Animatable<Float, AnimationVector1D>,
    scope: CoroutineScope,
) :
    ComponentDialog(
        ContextThemeWrapper(
            composeView.context,
            androidx.compose.material3.R.style.EdgeToEdgeFloatingDialogWindowTheme,
        )
    ),
    ViewRootForInspector {

    private val dialogLayout: ModalBottomSheetDialogLayout

    // On systems older than Android S, there is a bug in the surface insets matrix math used by
    // elevation, so high values of maxSupportedElevation break accessibility services: b/232788477.
    private val maxSupportedElevation = 8.dp

    override val subCompositionView: AbstractComposeView
        get() = dialogLayout

    init {
        val window = window ?: error("Dialog has no window")
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        dialogLayout =
            ModalBottomSheetDialogLayout(context, window).apply {
                // Set unique id for AbstractComposeView. This allows state restoration for the
                // state defined inside the Dialog via rememberSaveable()
                setTag(R.id.compose_view_saveable_id_tag, "Dialog:$dialogId")
                // Enable children to draw their shadow by not clipping them
                clipChildren = false
                // Allocate space for elevation
                with(density) { elevation = maxSupportedElevation.toPx() }
                // Simple outline to force window manager to allocate space for shadow.
                // Note that the outline affects clickable area for the dismiss listener. In
                // case of shapes like circle the area for dismiss might be to small
                // (rectangular outline consuming clicks outside of the circle).
                outlineProvider =
                    object : ViewOutlineProvider() {
                        override fun getOutline(view: View, result: Outline) {
                            result.setRect(0, 0, view.width, view.height)
                            // We set alpha to 0 to hide the view's shadow and let the
                            // composable to draw its own shadow. This still enables us to get
                            // the extra space needed in the surface.
                            result.alpha = 0f
                        }
                    }
            }
        // Clipping logic removed because we are spanning edge to edge.

        setContentView(dialogLayout)
        dialogLayout.setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        dialogLayout.setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        dialogLayout.setViewTreeSavedStateRegistryOwner(
            composeView.findViewTreeSavedStateRegistryOwner()
        )

        // Initial setup
        updateParameters(onDismissRequest, properties, contentColor, layoutDirection)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            // Theme system bars based on content color. Light system bars provide dark icons
            // and vice-versa. This maintains visible system bars for the bottom sheet window.
            isAppearanceLightStatusBars =
                properties.isAppearanceLightStatusBars ?: contentColor.isDark()
            isAppearanceLightNavigationBars =
                properties.isAppearanceLightNavigationBars ?: contentColor.isDark()
        }
        // Due to how the onDismissRequest callback works
        // (it enforces a just-in-time decision on whether to update the state to hide the dialog)
        // we need to provide a custom onBackPressedCallback to provide predictive back animations
        // for this component while handling onDismissRequest.
        onBackPressedDispatcher.addCallback(
            owner = this,
            onBackPressedCallback =
                PredictiveBackOnBackPressedCallback(
                    isEnabled = properties.shouldDismissOnBackPress,
                    scope = scope,
                    predictiveBackProgress = predictiveBackProgress,
                    onDismissRequest = {
                        this.onDismissRequest()
                    }, // Ensure lambda captures current onDismissRequest
                ),
        )
    }

    private fun setLayoutDirection(layoutDirection: LayoutDirection) {
        dialogLayout.layoutDirection =
            when (layoutDirection) {
                LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
                LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
            }
    }

    fun setContent(parentComposition: CompositionContext, children: @Composable () -> Unit) {
        dialogLayout.setContent(parentComposition, children)
    }

    private fun setSecurePolicy(securePolicy: SecureFlagPolicy) {
        val secureFlagEnabled =
            securePolicy.shouldApplySecureFlag(composeView.isFlagSecureEnabled())
        window!!.setFlags(
            if (secureFlagEnabled) {
                WindowManager.LayoutParams.FLAG_SECURE
            } else {
                WindowManager.LayoutParams.FLAG_SECURE.inv()
            },
            WindowManager.LayoutParams.FLAG_SECURE,
        )
    }

    fun updateParameters(
        onDismissRequest: () -> Unit,
        properties: ModalBottomSheetProperties,
        contentColor: Color,
        layoutDirection: LayoutDirection,
    ) {
        this.onDismissRequest = onDismissRequest
        this.properties = properties
        this.contentColor = contentColor
        setSecurePolicy(properties.securePolicy)
        setLayoutDirection(layoutDirection)

        // Window flags to span parent window.
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
        window?.setSoftInputMode(
            if (Build.VERSION.SDK_INT >= 30) {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                @Suppress("DEPRECATION") WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }
        )
    }

    fun disposeComposition() {
        dialogLayout.disposeComposition()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = super.onTouchEvent(event)
        if (result) {
            onDismissRequest()
        }

        return result
    }

    override fun cancel() {
        // Prevents the dialog from dismissing itself
        return
    }

    private class PredictiveBackOnBackPressedCallback(
        isEnabled: Boolean,
        val scope: CoroutineScope,
        val predictiveBackProgress: Animatable<Float, AnimationVector1D>,
        var onDismissRequest: () -> Unit,
    ) : OnBackPressedCallback(isEnabled) {

        override fun handleOnBackStarted(backEvent: BackEventCompat) {
            scope.launch {
                predictiveBackProgress.snapTo(PredictiveBack.transform(backEvent.progress))
            }
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            scope.launch {
                // Use snapTo for immediate feedback during the gesture
                predictiveBackProgress.snapTo(PredictiveBack.transform(backEvent.progress))
            }
        }

        override fun handleOnBackPressed() {
            // Back gesture completed successfully, invoke dismiss
            onDismissRequest()
        }

        override fun handleOnBackCancelled() {
            // Back gesture cancelled, animate back to 0
            scope.launch { predictiveBackProgress.animateTo(0f) }
        }
    }
}

internal fun View.isFlagSecureEnabled(): Boolean {
    val windowParams = rootView.layoutParams as? WindowManager.LayoutParams
    if (windowParams != null) {
        return (windowParams.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    }
    return false
}

/** Determines if a color should be considered light or dark. */
internal fun Color.isDark(): Boolean {
    return this != Color.Transparent && luminance() <= 0.5
}
