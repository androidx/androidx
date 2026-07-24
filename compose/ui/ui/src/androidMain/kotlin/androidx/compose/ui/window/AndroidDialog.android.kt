/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.window

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline as ComposeOutline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.layout.Layout
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
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.util.equalsIncludingNaN
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Properties used to customize the behavior of a [Dialog].
 *
 * @property dismissOnBackPress whether the dialog can be dismissed by pressing the back or escape
 *   buttons. If true, pressing the back button will call onDismissRequest.
 * @property dismissOnClickOutside whether the dialog can be dismissed by clicking outside the
 *   dialog's bounds. If true, clicking outside the dialog will call onDismissRequest.
 * @property securePolicy Policy for setting [WindowManager.LayoutParams.FLAG_SECURE] on the
 *   dialog's window.
 * @property usePlatformDefaultWidth Whether the width of the dialog's content should be limited to
 *   the platform default, which is smaller than the screen width. It is recommended to use
 *   [decorFitsSystemWindows] set to `false` when [usePlatformDefaultWidth] is false to support
 *   using the entire screen and avoiding UI glitches on some devices when the IME animates in.
 * @property decorFitsSystemWindows Sets [WindowCompat.setDecorFitsSystemWindows] value. Set to
 *   `false` to use WindowInsets. If `false`, the
 *   [soft input mode][WindowManager.LayoutParams.softInputMode] will be changed to
 *   [WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE] on [Build.VERSION_CODES.R] and below and
 *   [WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING] on [Build.VERSION_CODES.S] and above.
 *   [Window.isFloating] will be `false` when `decorFitsSystemWindows` is `false`.
 * @property windowTitle Title to be set on the dialog's window.
 * @property windowType An optional [android.view.WindowManager.LayoutParams.type] to apply to the
 *   dialog's underlying [android.view.Window]. The default value is
 *   [android.view.WindowManager.LayoutParams.TYPE_APPLICATION], which is the platform's standard
 *   dialog window type. Overriding this allows you to change the layer or behavior of the dialog.
 *   For example, setting it to [android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY]
 *   allows the dialog to draw on top of other applications (requires the
 *   [android.Manifest.permission.SYSTEM_ALERT_WINDOW] permission). Note: If you are displaying a
 *   dialog from a non-Activity context (such as an [android.app.Service]) but still want it to be
 *   anchored to an existing application window, you should leave this as the default type and
 *   instead provide the [windowToken] of the target application window.
 * @property windowToken An optional [android.os.IBinder] to be used as the window token for the
 *   dialog window. If null, the dialog will typically derive the token from the context. This
 *   parameter is crucial for scenarios where the dialog is shown from a context without a suitable
 *   default token, such as a Service running in a separate process from the main application. In
 *   such cross-process cases, the token from the main application's window should be provided. The
 *   provided token must be a valid [android.os.IBinder] from an existing window and must have the
 *   necessary permissions to add windows of the specified [windowType]. Providing an invalid,
 *   stale, or permission-denied token will typically result in a
 *   [android.view.WindowManager.BadTokenException] when the dialog attempts to show.
 * @property blurBehindRadius Blurs the screen behind the window. The effect is similar to that of
 *   [scrimAlpha], but instead of having a scrim applied, the content behind the window will be
 *   blurred (or combined with the scrim opacity, if such is specified). The density of the blur is
 *   set by the blur radius. The radius defines the size of the neighboring area, from which pixels
 *   will be averaged to form the final color for each pixel. The operation approximates a Gaussian
 *   blur. A radius of `0.dp` means no blur. The higher the radius, the denser the blur. Note the
 *   difference with [backgroundBlurRadius], which blurs only within the bounds of the window. Blur
 *   behind blurs the whole screen behind the window. For blur behind, a radius of `10.dp` (~20 px)
 *   creates a good depth-of-field effect. Avoid blur radii higher than `50.dp` (~150 px), as this
 *   will significantly impact performance. Some devices might not support cross-window blur due to
 *   GPU limitations. It can also be disabled by the system at runtime (e.g. during battery saving
 *   mode). In such situations, no blur will be computed or drawn. Supported on Android 12
 *   ([Build.VERSION_CODES.S]) and above.
 * @property backgroundBlurRadius Blurs the screen behind the window within the bounds of the
 *   window. The density of the blur is set by the blur radius. The radius defines the size of the
 *   neighboring area, from which pixels will be averaged to form the final color for each pixel.
 *   The operation approximates a Gaussian blur. A radius of `0.dp` means no blur. The higher the
 *   radius, the denser the blur. The window background drawable is drawn on top of the blurred
 *   region. The blur region bounds and rounded corners will mimic those of the background drawable.
 *   Note the difference with [blurBehindRadius], which blurs the whole screen behind the window.
 *   Background blur blurs the screen behind only within the bounds of the window. For background
 *   blur, a radius of `30.dp` (~80 px) creates a good frosted-glass effect. Avoid blur radii higher
 *   than `50.dp` (~150 px), as this will significantly impact performance. Some devices might not
 *   support cross-window blur due to GPU limitations. It can also be disabled by the system at
 *   runtime (e.g. during battery saving mode). In such situations, no blur will be computed or
 *   drawn. Supported on Android 12 ([Build.VERSION_CODES.S]) and above. If the dialog content uses
 *   rounded corners, set [windowShape] to match it so the background blur clips to the rounded
 *   corners of the dialog card instead of the default rectangular window bounds.
 * @property scrimAlpha The opacity of the scrim (also known as dimming) applied behind the dialog
 *   window. Ranging from 0.0f (no scrim) to 1.0f (completely opaque). By default, this value is
 *   [Float.NaN], which means the dialog retains the standard system dialog behavior with the
 *   default scrim opacity defined by the window theme.
 * @property windowShape The [Shape] applied to the underlying native dialog window background. This
 *   defines the geometric outline of the window frame. When set (e.g., `RoundedCornerShape` or
 *   `CircleShape`), window-level hardware effects such as [backgroundBlurRadius] will clip to this
 *   shape instead of standard 90-degree rectangular bounds. Use this to align the background blur
 *   outline with the rounded shape of your dialog content card. If `null` (default), the window
 *   background uses standard rectangular bounds.
 *
 *   Example usage:
 *
 * @sample androidx.compose.ui.samples.DialogFromServiceSample
 * @sample androidx.compose.ui.samples.DialogWithBlurSample
 */
@Immutable
public actual class DialogProperties(
    public actual val dismissOnBackPress: Boolean = true,
    public actual val dismissOnClickOutside: Boolean = true,
    public val securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
    public actual val usePlatformDefaultWidth: Boolean = true,
    public val decorFitsSystemWindows: Boolean = true,
    public val windowTitle: String = "",
    public val windowType: Int = WindowManager.LayoutParams.TYPE_APPLICATION,
    public val windowToken: IBinder? = null,
    public val blurBehindRadius: Dp = Dp.Unspecified,
    public val backgroundBlurRadius: Dp = Dp.Unspecified,
    public val scrimAlpha: Float = Float.NaN,
    public val windowShape: Shape? = null,
) {
    public constructor() :
        this(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            securePolicy = SecureFlagPolicy.Inherit,
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = true,
            windowTitle = "",
            windowType = WindowManager.LayoutParams.TYPE_APPLICATION,
            windowToken = null,
            blurBehindRadius = Dp.Unspecified,
            backgroundBlurRadius = Dp.Unspecified,
            scrimAlpha = Float.NaN,
            windowShape = null,
        )

    public actual constructor(
        dismissOnBackPress: Boolean,
        dismissOnClickOutside: Boolean,
        usePlatformDefaultWidth: Boolean,
    ) : this(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        securePolicy = SecureFlagPolicy.Inherit,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
        decorFitsSystemWindows = true,
    )

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        dismissOnBackPress: Boolean = true,
        dismissOnClickOutside: Boolean = true,
        securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
        usePlatformDefaultWidth: Boolean = true,
        decorFitsSystemWindows: Boolean = true,
        windowTitle: String = "",
    ) : this(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        securePolicy = securePolicy,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
        decorFitsSystemWindows = decorFitsSystemWindows,
        windowTitle = windowTitle,
        windowType = WindowManager.LayoutParams.TYPE_APPLICATION,
        windowToken = null,
    )

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        dismissOnBackPress: Boolean = true,
        dismissOnClickOutside: Boolean = true,
        securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
        usePlatformDefaultWidth: Boolean = true,
        decorFitsSystemWindows: Boolean = true,
        windowTitle: String = "",
        windowType: Int = WindowManager.LayoutParams.TYPE_APPLICATION,
        windowToken: IBinder? = null,
    ) : this(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        securePolicy = securePolicy,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
        decorFitsSystemWindows = decorFitsSystemWindows,
        windowTitle = windowTitle,
        windowType = windowType,
        windowToken = windowToken,
        blurBehindRadius = Dp.Unspecified,
        backgroundBlurRadius = Dp.Unspecified,
        scrimAlpha = Float.NaN,
        windowShape = null,
    )

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        dismissOnBackPress: Boolean = true,
        dismissOnClickOutside: Boolean = true,
        securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
        usePlatformDefaultWidth: Boolean = true,
        decorFitsSystemWindows: Boolean = true,
    ) : this(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        securePolicy = SecureFlagPolicy.Inherit,
        usePlatformDefaultWidth = usePlatformDefaultWidth,
        decorFitsSystemWindows = true,
        windowTitle = "",
    )

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        dismissOnBackPress: Boolean = true,
        dismissOnClickOutside: Boolean = true,
        securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
    ) : this(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        securePolicy = securePolicy,
        usePlatformDefaultWidth = true,
        decorFitsSystemWindows = true,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DialogProperties) return false

        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false
        if (securePolicy != other.securePolicy) return false
        if (usePlatformDefaultWidth != other.usePlatformDefaultWidth) return false
        if (decorFitsSystemWindows != other.decorFitsSystemWindows) return false
        if (windowType != other.windowType) return false
        if (windowToken != other.windowToken) return false
        if (blurBehindRadius != other.blurBehindRadius) return false
        if (backgroundBlurRadius != other.backgroundBlurRadius) return false
        if (!scrimAlpha.equalsIncludingNaN(other.scrimAlpha)) return false
        if (windowShape != other.windowShape) return false
        return true
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        result = 31 * result + securePolicy.hashCode()
        result = 31 * result + usePlatformDefaultWidth.hashCode()
        result = 31 * result + decorFitsSystemWindows.hashCode()
        result = 31 * result + windowType
        result = 31 * result + (windowToken?.hashCode() ?: 0)
        result = 31 * result + blurBehindRadius.hashCode()
        result = 31 * result + backgroundBlurRadius.hashCode()
        result = 31 * result + scrimAlpha.hashCode()
        result = 31 * result + (windowShape?.hashCode() ?: 0)
        return result
    }
}

/**
 * Opens a dialog with the given content.
 *
 * A dialog is a small window that prompts the user to make a decision or enter additional
 * information. A dialog does not fill the screen and is normally used for modal events that require
 * users to take an action before they can proceed.
 *
 * The dialog is visible as long as it is part of the composition hierarchy. In order to let the
 * user dismiss the Dialog, the implementation of [onDismissRequest] should contain a way to remove
 * the dialog from the composition hierarchy.
 *
 * Example usage:
 *
 * @sample androidx.compose.ui.samples.DialogSample
 * @param onDismissRequest Executes when the user tries to dismiss the dialog.
 * @param properties [DialogProperties] for further customization of this dialog's behavior.
 * @param content The content to be displayed inside the dialog.
 */
@Composable
public actual fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val composition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val dialogId = rememberSaveable { UUID.randomUUID() }
    // The window type cannot be changed dynamically after the window is added to the
    // WindowManager (it throws an exception on older APIs like API 30 and below). Therefore, we
    // add properties.windowType as a remember key to force the DialogWrapper to be
    // completely recreated from scratch when the type changes.
    val dialog =
        remember(view, density, properties.windowType, properties.windowToken) {
            DialogWrapper(onDismissRequest, properties, view, layoutDirection, density, dialogId)
                .apply {
                    setContent(composition) {
                        DialogLayout(Modifier.semantics { dialog() }, currentContent)
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
            density = density,
        )
    }
}

/**
 * Provides the underlying window of a dialog.
 *
 * Implemented by dialog's root layout.
 */
public interface DialogWindowProvider {
    public val window: Window
}

@Suppress("ViewConstructor")
private class DialogLayout(context: Context, override val window: Window) :
    AbstractComposeView(context), DialogWindowProvider, OnApplyWindowInsetsListener {

    private var content: @Composable () -> Unit by mutableStateOf({})

    private var usePlatformDefaultWidth = false
    private var decorFitsSystemWindows = false
    private var hasCalledSetLayout = false

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    init {
        ViewCompat.setOnApplyWindowInsetsListener(this, this)
        ViewCompat.setWindowInsetsAnimationCallback(
            this,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
                override fun onStart(
                    animation: WindowInsetsAnimationCompat,
                    bounds: WindowInsetsAnimationCompat.BoundsCompat,
                ): WindowInsetsAnimationCompat.BoundsCompat =
                    insetValue(bounds) { l, t, r, b -> bounds.inset(Insets.of(l, t, r, b)) }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>,
                ): WindowInsetsCompat =
                    insetValue(insets) { l, t, r, b -> insets.inset(l, t, r, b) }
            },
        )
    }

    fun updateProperties(usePlatformDefaultWidth: Boolean, decorFitsSystemWindows: Boolean) {
        val callSetLayout =
            !hasCalledSetLayout ||
                usePlatformDefaultWidth != this.usePlatformDefaultWidth ||
                decorFitsSystemWindows != this.decorFitsSystemWindows
        this.usePlatformDefaultWidth = usePlatformDefaultWidth
        this.decorFitsSystemWindows = decorFitsSystemWindows

        if (callSetLayout) {
            val attrs = window.attributes
            val measurementWidth = if (usePlatformDefaultWidth) WRAP_CONTENT else MATCH_PARENT
            if (measurementWidth != attrs.width || !hasCalledSetLayout) {
                // Always use WRAP_CONTENT for height. internalOnMeasure() will change
                // it to MATCH_PARENT if it needs more height. If we use MATCH_PARENT here,
                // and change to WRAP_CONTENT in internalOnMeasure(), the window size will
                // be wrong on the first frame.
                window.setLayout(measurementWidth, WRAP_CONTENT)
                hasCalledSetLayout = true
            }
        }
    }

    override fun internalOnMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val child = getChildAt(0)
        if (child == null) {
            super.internalOnMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val targetHeight =
            if (
                heightMode == MeasureSpec.AT_MOST &&
                    !usePlatformDefaultWidth &&
                    window.attributes.height == WRAP_CONTENT
            ) {
                if (decorFitsSystemWindows) {
                    // On API 31 and below, there is a bug in view framework (b/193978485) where
                    // system bar insets were incorrectly considered to calculate the max height
                    // a view can occupy resulting in view to be 1px or 2px smaller than its parent.
                    // To fix this issue we try to calculate the max height a dialog can occupy
                    // after excluding system bar insets and set that as target height.
                    getMaxDialogHeightExcludingInsets(window, height)
                } else {
                    // Any size larger than the WRAP_CONTENT to test to see if this is full-screen
                    // content.
                    height + 1
                }
            } else {
                height
            }

        val horizontalPadding = paddingLeft + paddingRight
        val verticalPadding = paddingTop + paddingBottom
        val remainingWidth = (width - horizontalPadding).fastCoerceAtLeast(0)
        val remainingHeight = (targetHeight - verticalPadding).fastCoerceAtLeast(0)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val childWidthSpec =
            if (widthMode == MeasureSpec.UNSPECIFIED) {
                widthMeasureSpec
            } else {
                MeasureSpec.makeMeasureSpec(remainingWidth, MeasureSpec.AT_MOST)
            }
        val childHeightSpec =
            if (heightMode == MeasureSpec.UNSPECIFIED) {
                heightMeasureSpec
            } else {
                MeasureSpec.makeMeasureSpec(remainingHeight, MeasureSpec.AT_MOST)
            }
        child.measure(childWidthSpec, childHeightSpec)

        // respect passed dimensions
        val measuredWidth =
            when (widthMode) {
                MeasureSpec.EXACTLY -> width
                MeasureSpec.AT_MOST -> minOf(width, child.measuredWidth + horizontalPadding)
                else -> child.measuredWidth + horizontalPadding
            }
        val measuredHeight =
            when (heightMode) {
                MeasureSpec.EXACTLY -> height
                MeasureSpec.AT_MOST -> minOf(height, child.measuredHeight + verticalPadding)
                else -> child.measuredHeight + verticalPadding
            }
        setMeasuredDimension(measuredWidth, measuredHeight)

        if (
            !decorFitsSystemWindows &&
                child.measuredHeight + verticalPadding > height &&
                window.attributes.height == WRAP_CONTENT
        ) {
            // We're going to use the full screen, so don't put a background behind the system bars
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            if (!usePlatformDefaultWidth) {
                // The size of the window is too small with WRAP_CONTENT for height. Change it
                // to use MATCH_PARENT to give as much room as possible
                window.setLayout(MATCH_PARENT, MATCH_PARENT)
            }
        }
    }

    private fun getMaxDialogHeightExcludingInsets(window: Window, height: Int): Int {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Api21Impl.getMaxDialogHeightExcludingSystemBarInsets(window)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2) {
            Api30Impl.getMaxDialogHeightExcludingSystemBarInsets(window)
        } else {
            // On API 32 and above we don't have to exclude insets height,
            // return the original height
            height
        }
    }

    override fun internalOnLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val child = getChildAt(0) ?: return

        // center content
        val hPadding = paddingLeft + paddingRight
        val vPadding = paddingTop + paddingBottom
        val width = right - left
        val height = bottom - top
        val childWidth = child.measuredWidth
        val childHeight = child.measuredHeight

        val extraWidth = width - childWidth - hPadding
        val extraHeight = height - childHeight - vPadding

        val l = paddingLeft + (extraWidth / 2)
        val t = paddingTop + (extraHeight / 2)
        val r = l + childWidth
        val b = t + childHeight
        child.layout(l, t, r, b)
    }

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
        createComposition()
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat =
        insetValue(insets) { l, t, r, b -> insets.inset(l, t, r, b) }

    private inline fun <T> insetValue(
        unchangedValue: T,
        block: (left: Int, top: Int, right: Int, bottom: Int) -> T,
    ): T {
        if (decorFitsSystemWindows) {
            return unchangedValue
        }
        val child = getChildAt(0)
        val left = maxOf(0, child.left)
        val top = maxOf(0, child.top)
        val right = maxOf(0, width - child.right)
        val bottom = maxOf(0, height - child.bottom)
        return if (left == 0 && top == 0 && right == 0 && bottom == 0) {
            unchangedValue
        } else {
            block(left, top, right, bottom)
        }
    }

    fun isInsideContent(event: MotionEvent): Boolean {
        if (!event.x.isFinite() || !event.y.isFinite()) return false
        val child = getChildAt(0) ?: return false
        val left = left + child.left
        val right = left + child.width
        val top = top + child.top
        val bottom = top + child.height
        return event.x.roundToInt() in left..right && event.y.roundToInt() in top..bottom
    }

    @Composable
    override fun Content() {
        content()
    }
}

private class DialogWrapper(
    private var onDismissRequest: () -> Unit,
    private var properties: DialogProperties,
    private val composeView: View,
    layoutDirection: LayoutDirection,
    density: Density,
    dialogId: UUID,
) :
    ComponentDialog(
        /**
         * [Window.setClipToOutline] is only available from 22+, but the style attribute exists
         * on 21. So use a wrapped context that sets this attribute for compatibility back to 21.
         */
        ContextThemeWrapper(
            composeView.context,
            if (properties.decorFitsSystemWindows) {
                R.style.DialogWindowTheme
            } else {
                R.style.FloatingDialogWindowTheme
            },
        )
    ),
    ViewRootForInspector {

    private val dialogLayout: DialogLayout

    // On systems older than Android S, there is a bug in the surface insets matrix math used by
    // elevation, so high values of maxSupportedElevation break accessibility services: b/232788477.
    private val maxSupportedElevation = 8.dp

    private var isPressOutside = false

    override val subCompositionView: AbstractComposeView
        get() = dialogLayout

    init {
        val window = window ?: error("Dialog has no window")

        applyWindowTypeAndToken(properties)

        window.requestFeature(Window.FEATURE_NO_TITLE)
        setWindowBackgroundShape(window, properties.windowShape, density, layoutDirection)

        WindowCompat.setDecorFitsSystemWindows(window, properties.decorFitsSystemWindows)
        window.setGravity(Gravity.CENTER)
        if (!properties.decorFitsSystemWindows) {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )
            val attrs = window.attributes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Api28Impl.setLayoutInDisplayCutout(attrs)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Api30Impl.setFitInsetsSides(attrs, 0)
                Api30Impl.setFitInsetsTypes(attrs, 0)
            }
            window.attributes = attrs
        }

        dialogLayout =
            DialogLayout(context, window).apply {
                // Set window title.
                setTitle(properties.windowTitle)
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
                            // needed in the surface.
                            result.alpha = 0f
                        }
                    }
            }

        /**
         * Disables clipping for [this] and all its descendant [ViewGroup]s until we reach a
         * [DialogLayout] (the [ViewGroup] containing the Compose hierarchy).
         */
        fun ViewGroup.disableClipping() {
            clipChildren = false
            if (this is DialogLayout) return
            for (i in 0 until childCount) {
                (getChildAt(i) as? ViewGroup)?.disableClipping()
            }
        }

        // Turn of all clipping so shadows can be drawn outside the window
        (window.decorView as? ViewGroup)?.disableClipping()

        setContentView(dialogLayout)
        dialogLayout.setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        dialogLayout.setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        dialogLayout.setViewTreeSavedStateRegistryOwner(
            composeView.findViewTreeSavedStateRegistryOwner()
        )

        // Initial setup
        updateParameters(onDismissRequest, properties, layoutDirection, density)

        // Due to how the onDismissRequest callback works
        // (it enforces a just-in-time decision on whether to update the state to hide the dialog)
        // we need to unconditionally add a callback here that is always enabled,
        // meaning we'll never get a system UI controlled predictive back animation
        // for these dialogs
        onBackPressedDispatcher.addCallback(this) {
            if (properties.dismissOnBackPress) {
                onDismissRequest()
            }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (
            properties.dismissOnBackPress &&
                event.isTracking &&
                !event.isCanceled &&
                keyCode == KeyEvent.KEYCODE_ESCAPE
        ) {
            onDismissRequest()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun applyWindowTypeAndToken(properties: DialogProperties) {
        window?.let { window ->
            val attrs = window.attributes
            attrs.type = properties.windowType
            // Use windowToken if provided else let the framework handle it.
            properties.windowToken?.let { token -> attrs.token = token }
            window.attributes = attrs
        }
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

    private fun setWindowBackgroundShape(
        window: Window,
        windowShape: Shape?,
        density: Density,
        layoutDirection: LayoutDirection,
    ) {
        if (windowShape != null) {
            window.setBackgroundDrawable(
                WindowBackgroundShapeDrawable(windowShape, density, layoutDirection)
            )
        } else {
            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    fun updateParameters(
        onDismissRequest: () -> Unit,
        properties: DialogProperties,
        layoutDirection: LayoutDirection,
        density: Density,
    ) {
        this.onDismissRequest = onDismissRequest
        this.properties = properties
        setSecurePolicy(properties.securePolicy)
        setLayoutDirection(layoutDirection)
        val decorFitsSystemWindows = properties.decorFitsSystemWindows
        dialogLayout.updateProperties(
            usePlatformDefaultWidth = properties.usePlatformDefaultWidth,
            decorFitsSystemWindows = decorFitsSystemWindows,
        )
        setCanceledOnTouchOutside(properties.dismissOnClickOutside)
        val window = window
        if (window != null) {
            setWindowBackgroundShape(window, properties.windowShape, density, layoutDirection)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (properties.blurBehindRadius.isSpecified) {
                    val blurBehindRadiusPx =
                        with(density) { properties.blurBehindRadius.roundToPx() }
                    DialogApi31Impl.setBlurBehindRadius(window, blurBehindRadiusPx)
                }
                if (properties.backgroundBlurRadius.isSpecified) {
                    val backgroundBlurRadiusPx =
                        with(density) { properties.backgroundBlurRadius.roundToPx() }
                    DialogApi31Impl.setBackgroundBlurRadius(window, backgroundBlurRadiusPx)
                }
            }

            if (!properties.scrimAlpha.isNaN()) {
                window.setDimAmount(properties.scrimAlpha)
            }

            val softInput =
                when {
                    decorFitsSystemWindows ->
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ->
                        @Suppress("DEPRECATION") WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    else -> WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                }
            window.setSoftInputMode(softInput)
        }
    }

    fun disposeComposition() {
        dialogLayout.disposeComposition()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var result = super.onTouchEvent(event)
        if (properties.dismissOnClickOutside && !dialogLayout.isInsideContent(event)) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    isPressOutside = true
                    result = true
                }
                MotionEvent.ACTION_UP ->
                    if (isPressOutside) {
                        onDismissRequest()
                        result = true
                        isPressOutside = false
                    }
                MotionEvent.ACTION_CANCEL -> isPressOutside = false
            }
        } else {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> isPressOutside = false
            }
        }

        return result
    }

    override fun cancel() {
        // Prevents the dialog from dismissing itself
        return
    }
}

@Composable
private fun DialogLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        var maxWidth = 0
        var maxHeight = 0
        val placeables =
            measurables.fastMap {
                it.measure(constraints).apply {
                    maxWidth = max(maxWidth, width)
                    maxHeight = max(maxHeight, height)
                }
            }
        if (measurables.isEmpty()) {
            maxWidth = constraints.minWidth
            maxHeight = constraints.minHeight
        }
        layout(maxWidth, maxHeight) { placeables.fastForEach { it.placeRelative(0, 0) } }
    }
}

private object Api21Impl {

    @DoNotInline
    fun getMaxDialogHeightExcludingSystemBarInsets(window: Window): Int {
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION") /* defaultDisplay + getMetrics() */
        window.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels -
            getSystemBarsHeight(window, displayMetrics.heightPixels)
    }

    private fun getSystemBarsHeight(window: Window, displayHeight: Int): Int {
        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)

        // status bar height
        val topOffset = rect.top

        // displayHeight is the height of current app window.
        // rect is overall display size including decor view.
        // Navigation bar height is the difference between rect.bottom and displayHeight.
        val bottomOffset =
            if (rect.bottom > displayHeight) {
                rect.bottom - displayHeight
            } else 0

        return topOffset + bottomOffset
    }
}

@RequiresApi(28)
private object Api28Impl {
    @DoNotInline
    fun setLayoutInDisplayCutout(attrs: WindowManager.LayoutParams) {
        attrs.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }
}

@RequiresApi(30)
private object Api30Impl {
    @DoNotInline
    fun setFitInsetsSides(attrs: WindowManager.LayoutParams, sides: Int) {
        attrs.setFitInsetsSides(sides)
    }

    @DoNotInline
    fun setFitInsetsTypes(attrs: WindowManager.LayoutParams, types: Int) {
        attrs.setFitInsetsTypes(types)
    }

    @DoNotInline
    fun getMaxDialogHeightExcludingSystemBarInsets(window: Window): Int {
        val currentWindowMetrics = window.windowManager.currentWindowMetrics
        val windowInsets = currentWindowMetrics.windowInsets
        val systemBarInsets = windowInsets.getInsets(WindowInsets.Type.systemBars())
        val systemBarInsetsHeight = systemBarInsets.top + systemBarInsets.bottom
        return currentWindowMetrics.bounds.height() - systemBarInsetsHeight
    }
}

@RequiresApi(31)
private object DialogApi31Impl {
    @DoNotInline
    fun setBlurBehindRadius(window: Window, blurBehindRadius: Int) {
        if (blurBehindRadius > 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val attributes = window.attributes
            attributes.blurBehindRadius = blurBehindRadius
            window.attributes = attributes
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        }
    }

    fun setBackgroundBlurRadius(window: Window, backgroundBlurRadius: Int) {
        window.setBackgroundBlurRadius(backgroundBlurRadius)
    }
}

private class WindowBackgroundShapeDrawable(
    private val shape: Shape,
    private val density: Density,
    private val layoutDirection: LayoutDirection,
) : Drawable() {
    override fun draw(canvas: Canvas) {}

    @Deprecated("Deprecated in Java") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun getOutline(outline: Outline) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        if (width <= 0f || height <= 0f) return

        outline.alpha = 0f

        when (
            val composeOutline = shape.createOutline(Size(width, height), layoutDirection, density)
        ) {
            is ComposeOutline.Rectangle -> {
                outline.setRect(bounds)
            }

            is ComposeOutline.Rounded -> {
                val radius = composeOutline.roundRect.topLeftCornerRadius.x
                outline.setRoundRect(bounds, radius)
            }

            is ComposeOutline.Generic -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    outline.setPath(composeOutline.path.asAndroidPath())
                } else {
                    outline.setRect(bounds)
                }
            }
        }
    }
}
