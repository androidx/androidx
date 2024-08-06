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

package androidx.compose.material3.internal

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
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.isFlagSecureEnabled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
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
import kotlin.coroutines.cancellation.CancellationException

/**
 * A dialog that is _always_ full screen and edge-to-edge. This is intended to be the underlying
 * backbone for more complicated and opinionated dialogs.
 *
 * The [content] will fill the entire window, going entirely edge-to-edge.
 *
 * This [BasicEdgeToEdgeDialog] provides no scrim or dismissing when the scrim is pressed. If this
 * is desired, it must be implemented by the [content] or supplied by enabling background dim on the
 * dialog's window.
 *
 * [DialogProperties] will be respected, but [DialogProperties.decorFitsSystemWindows] and
 * [DialogProperties.usePlatformDefaultWidth] are ignored.
 *
 * The [content] will be passed a [PredictiveBackState] that encapsulates the predictive back state
 * if [DialogProperties.dismissOnBackPress] is true.
 */
@Composable
internal fun BasicEdgeToEdgeDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    lightStatusBars: Boolean = LocalContentColor.current.luminance() < 0.5f,
    lightNavigationBars: Boolean = LocalContentColor.current.luminance() < 0.5f,
    content: @Composable (PredictiveBackState) -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val composition = rememberCompositionContext()
    val dialogId = rememberSaveable { UUID.randomUUID() }

    val currentContent by rememberUpdatedState(content)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val currentDismissOnBackPress by rememberUpdatedState(properties.dismissOnBackPress)

    val dialog =
        remember(view, density) {
            DialogWrapper(
                    onDismissRequest = onDismissRequest,
                    properties = properties,
                    composeView = view,
                    layoutDirection = layoutDirection,
                    density = density,
                    dialogId = dialogId,
                    lightStatusBars = lightStatusBars,
                    lightNavigationBars = lightNavigationBars,
                )
                .apply {
                    setContent(composition) {
                        val predictiveBackState = rememberPredictiveBackState()

                        PredictiveBackStateHandler(
                            state = predictiveBackState,
                            enabled = currentDismissOnBackPress,
                            onBack = currentOnDismissRequest,
                        )

                        Box(modifier.semantics { dialog() }) { currentContent(predictiveBackState) }
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
            layoutDirection = layoutDirection,
            lightStatusBars = lightStatusBars,
            lightNavigationBars = lightNavigationBars,
        )
    }
}

private class DialogWrapper(
    private var onDismissRequest: () -> Unit,
    private var properties: DialogProperties,
    private val composeView: View,
    layoutDirection: LayoutDirection,
    density: Density,
    dialogId: UUID,
    lightStatusBars: Boolean,
    lightNavigationBars: Boolean,
) :
    ComponentDialog(
        ContextThemeWrapper(
            composeView.context,
            androidx.compose.material3.R.style.EdgeToEdgeFloatingDialogWindowTheme
        )
    ),
    ViewRootForInspector {
    private val dialogLayout: DialogLayout

    // On systems older than Android S, there is a bug in the surface insets matrix math used by
    // elevation, so high values of maxSupportedElevation break accessibility services: b/232788477.
    private val maxSupportedElevation = 8.dp

    override val subCompositionView: AbstractComposeView
        get() = dialogLayout

    init {
        val window = window ?: error("Dialog has no window")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        dialogLayout =
            DialogLayout(context, window).apply {
                // Set unique id for AbstractComposeView. This allows state restoration for the
                // state
                // defined inside the Dialog via rememberSaveable()
                setTag(R.id.compose_view_saveable_id_tag, "Dialog:$dialogId")

                // Enable children to draw their shadow by not clipping them
                clipChildren = false

                // Allocate space for elevation
                with(density) { elevation = maxSupportedElevation.toPx() }

                // Simple outline to force window manager to allocate space for shadow.
                // Note that the outline affects clickable area for the dismiss listener. In case of
                // shapes like circle the area for dismiss might be to small (rectangular outline
                // consuming clicks outside of the circle).
                outlineProvider =
                    object : ViewOutlineProvider() {
                        override fun getOutline(view: View, result: Outline) {
                            result.setRect(0, 0, view.width, view.height)
                            // We set alpha to 0 to hide the view's shadow and let the composable to
                            // draw its own shadow. This still enables us to get the extra space
                            // needed
                            // in the surface.
                            result.alpha = 0f
                        }
                    }
            }

        setContentView(dialogLayout)
        dialogLayout.setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        dialogLayout.setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        dialogLayout.setViewTreeSavedStateRegistryOwner(
            composeView.findViewTreeSavedStateRegistryOwner()
        )

        // Initial setup
        updateParameters(
            onDismissRequest = onDismissRequest,
            properties = properties,
            layoutDirection = layoutDirection,
            lightStatusBars = lightStatusBars,
            lightNavigationBars = lightNavigationBars,
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
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    fun updateParameters(
        onDismissRequest: () -> Unit,
        properties: DialogProperties,
        layoutDirection: LayoutDirection,
        lightStatusBars: Boolean,
        lightNavigationBars: Boolean,
    ) {
        this.onDismissRequest = onDismissRequest
        this.properties = properties
        setSecurePolicy(properties.securePolicy)
        setLayoutDirection(layoutDirection)
        window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = lightStatusBars
                isAppearanceLightNavigationBars = lightNavigationBars
            }
            // Window flags to span parent window.
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
            window.setSoftInputMode(
                if (Build.VERSION.SDK_INT >= 30) {
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                } else {
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                },
            )
        }
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
}

@Suppress("ViewConstructor")
private class DialogLayout(
    context: Context,
    override val window: Window,
) : AbstractComposeView(context), DialogWindowProvider {

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
        createComposition()
    }

    @Composable
    override fun Content() {
        content()
    }
}

internal enum class SwipeEdge {
    Left,
    Right
}

/**
 * The state describing a one-shot back state, with use in a [PredictiveBackStateHandler].
 *
 * Because the back handler can only be used once, there are three states that [BackEventProgress]
 * can be in:
 * - [NotRunning]
 * - [InProgress], which can happen on API 34 and above if a predictive back is in progress.
 * - [Completed]
 */
internal sealed interface BackEventProgress {
    /** There is no predictive back ongoing, and the back has not been completed. */
    object NotRunning : BackEventProgress

    /** There is an ongoing predictive back animation, with the given [progress]. */
    data class InProgress(
        val touchX: Float,
        val touchY: Float,
        val progress: Float,
        val swipeEdge: SwipeEdge,
    ) : BackEventProgress

    /** The back has completed. */
    object Completed : BackEventProgress
}

@Composable
internal fun rememberPredictiveBackState(): PredictiveBackState = remember {
    PredictiveBackStateImpl()
}

internal sealed interface PredictiveBackState {
    val value: BackEventProgress
}

private class PredictiveBackStateImpl : PredictiveBackState {
    override var value: BackEventProgress by mutableStateOf(BackEventProgress.NotRunning)
}

@Composable
internal fun PredictiveBackStateHandler(
    state: PredictiveBackState,
    enabled: Boolean = true,
    onBack: () -> Unit,
) {
    // Safely update the current `onBack` lambda when a new one is provided
    val currentOnBack by rememberUpdatedState(onBack)

    key(state) {
        state as PredictiveBackStateImpl
        PredictiveBackHandler(
            enabled = enabled && state.value !is BackEventProgress.Completed,
        ) { progress ->
            try {
                progress.collect { backEvent ->
                    state.value =
                        BackEventProgress.InProgress(
                            backEvent.touchX,
                            backEvent.touchY,
                            backEvent.progress,
                            when (backEvent.swipeEdge) {
                                BackEventCompat.EDGE_LEFT -> SwipeEdge.Left
                                BackEventCompat.EDGE_RIGHT -> SwipeEdge.Right
                                else -> error("Unknown swipe edge")
                            },
                        )
                }
                state.value = BackEventProgress.Completed
                currentOnBack()
            } catch (e: CancellationException) {
                state.value = BackEventProgress.NotRunning
                throw e
            }
        }
    }
}

// Taken from AndroidPopup.android.kt
internal fun SecureFlagPolicy.shouldApplySecureFlag(isSecureFlagSetOnParent: Boolean): Boolean {
    return when (this) {
        SecureFlagPolicy.SecureOff -> false
        SecureFlagPolicy.SecureOn -> true
        SecureFlagPolicy.Inherit -> isSecureFlagSetOnParent
    }
}
