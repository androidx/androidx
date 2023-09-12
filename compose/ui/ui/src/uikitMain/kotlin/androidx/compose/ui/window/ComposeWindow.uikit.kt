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

package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.toCompose
import androidx.compose.ui.interop.LocalLayerContainer
import androidx.compose.ui.interop.LocalUIKitInteropContext
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.interop.UIKitInteropContext
import androidx.compose.ui.native.getMainDispatcher
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.uikit.*
import androidx.compose.ui.unit.*
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.SkikoKeyboardEvent
import org.jetbrains.skiko.SkikoPointerEvent
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformInvert
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeEqualToSize
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private val uiContentSizeCategoryToFontScaleMap = mapOf(
    UIContentSizeCategoryExtraSmall to 0.8f,
    UIContentSizeCategorySmall to 0.85f,
    UIContentSizeCategoryMedium to 0.9f,
    UIContentSizeCategoryLarge to 1f, // default preference
    UIContentSizeCategoryExtraLarge to 1.1f,
    UIContentSizeCategoryExtraExtraLarge to 1.2f,
    UIContentSizeCategoryExtraExtraExtraLarge to 1.3f,

    // These values don't work well if they match scale shown by
    // Text Size control hint, because iOS uses non-linear scaling
    // calculated by UIFontMetrics, while Compose uses linear.
    UIContentSizeCategoryAccessibilityMedium to 1.4f, // 160% native
    UIContentSizeCategoryAccessibilityLarge to 1.5f, // 190% native
    UIContentSizeCategoryAccessibilityExtraLarge to 1.6f, // 235% native
    UIContentSizeCategoryAccessibilityExtraExtraLarge to 1.7f, // 275% native
    UIContentSizeCategoryAccessibilityExtraExtraExtraLarge to 1.8f, // 310% native

    // UIContentSizeCategoryUnspecified
)

fun ComposeUIViewController(content: @Composable () -> Unit): UIViewController =
    ComposeUIViewController(configure = {}, content = content)

fun ComposeUIViewController(
    configure: ComposeUIViewControllerConfiguration.() -> Unit = {},
    content: @Composable () -> Unit
): UIViewController =
    ComposeWindow().apply {
        configuration = ComposeUIViewControllerConfiguration()
            .apply(configure)
        setContent(content)
    }

private class AttachedComposeContext(
    val scene: ComposeScene,
    val view: SkikoUIView,
) {
    private var constraints: List<NSLayoutConstraint> = emptyList()
        set(value) {
            if (field.isNotEmpty()) {
                NSLayoutConstraint.deactivateConstraints(field)
            }
            field = value
            NSLayoutConstraint.activateConstraints(value)
        }

    fun setConstraintsToCenterInView(parentView: UIView, size: CValue<CGSize>) {
        size.useContents {
            constraints = listOf(
                view.centerXAnchor.constraintEqualToAnchor(parentView.centerXAnchor),
                view.centerYAnchor.constraintEqualToAnchor(parentView.centerYAnchor),
                view.widthAnchor.constraintEqualToConstant(width),
                view.heightAnchor.constraintEqualToConstant(height)
            )
        }
    }

    fun setConstraintsToFillView(parentView: UIView) {
        constraints = listOf(
            view.leftAnchor.constraintEqualToAnchor(parentView.leftAnchor),
            view.rightAnchor.constraintEqualToAnchor(parentView.rightAnchor),
            view.topAnchor.constraintEqualToAnchor(parentView.topAnchor),
            view.bottomAnchor.constraintEqualToAnchor(parentView.bottomAnchor)
        )
    }
    fun dispose() {
        scene.close()
        view.dispose()
    }
}

@OptIn(InternalComposeApi::class)
@ExportObjCClass
internal actual class ComposeWindow : UIViewController {

    internal lateinit var configuration: ComposeUIViewControllerConfiguration
    private val keyboardOverlapHeightState = mutableStateOf(0f)
    private var isInsideSwiftUI = false
    private val safeAreaState = mutableStateOf(IOSInsets())
    private val layoutMarginsState = mutableStateOf(IOSInsets())
    private val interopContext = UIKitInteropContext(requestRedraw = {
        attachedComposeContext?.view?.needRedraw()
    })

    /*
     * Initial value is arbitarily chosen to avoid propagating invalid value logic
     * It's never the case in real usage scenario to reflect that in type system
     */
    private val interfaceOrientationState = mutableStateOf(
        InterfaceOrientation.Portrait
    )

    private val systemTheme = mutableStateOf(
        traitCollection.userInterfaceStyle.asComposeSystemTheme()
    )

    /*
     * On iOS >= 13.0 interfaceOrientation will be deduced from [UIWindowScene] of [UIWindow]
     * to which our [ComposeWindow] is attached.
     * It's never UIInterfaceOrientationUnknown, if accessed after owning [UIWindow] was made key and visible:
     * https://developer.apple.com/documentation/uikit/uiwindow/1621601-makekeyandvisible?language=objc
     */
    private val currentInterfaceOrientation: InterfaceOrientation?
        get() {
            // Modern: https://developer.apple.com/documentation/uikit/uiwindowscene/3198088-interfaceorientation?language=objc
            // Deprecated: https://developer.apple.com/documentation/uikit/uiapplication/1623026-statusbarorientation?language=objc
            return if (available(OS.Ios to OSVersion(13))) {
                view.window?.windowScene?.interfaceOrientation?.let {
                    InterfaceOrientation.getByRawValue(it)
                }
            } else {
                InterfaceOrientation.getByRawValue(UIApplication.sharedApplication.statusBarOrientation)
            }
        }

    private val _windowInfo = WindowInfoImpl().apply {
        isWindowFocused = true
    }

    @OverrideInit
    actual constructor() : super(nibName = null, bundle = null)

    @OverrideInit
    constructor(coder: NSCoder) : super(coder)

    private val fontScale: Float
        get() {
            val contentSizeCategory =
                traitCollection.preferredContentSizeCategory ?: UIContentSizeCategoryUnspecified

            return uiContentSizeCategoryToFontScaleMap[contentSizeCategory] ?: 1.0f
        }

    private val density: Density
        get() = Density(
            attachedComposeContext?.view?.contentScaleFactor?.toFloat() ?: 1f,
            fontScale
        )

    private lateinit var content: @Composable () -> Unit

    private var attachedComposeContext: AttachedComposeContext? = null

    private val keyboardVisibilityListener = object : NSObject() {
        @Suppress("unused")
        @ObjCAction
        fun keyboardWillShow(arg: NSNotification) {
            val keyboardInfo = arg.userInfo!!["UIKeyboardFrameEndUserInfoKey"] as NSValue
            val keyboardHeight = keyboardInfo.CGRectValue().useContents { size.height }
            val screenHeight = UIScreen.mainScreen.bounds.useContents { size.height }

            val composeViewBottomY = UIScreen.mainScreen.coordinateSpace.convertPoint(
                point = CGPointMake(0.0, view.frame.useContents { size.height }),
                fromCoordinateSpace = view.coordinateSpace
            ).useContents { y }
            val bottomIndent = screenHeight - composeViewBottomY

            if (bottomIndent < keyboardHeight) {
                keyboardOverlapHeightState.value = (keyboardHeight - bottomIndent).toFloat()
            }

            val scene = attachedComposeContext?.scene ?: return

            if (configuration.onFocusBehavior == OnFocusBehavior.FocusableAboveKeyboard) {
                val focusedRect = scene.mainOwner?.focusOwner?.getFocusRect()?.toDpRect(density)

                if (focusedRect != null) {
                    updateViewBounds(
                        offsetY = calcFocusedLiftingY(focusedRect, keyboardHeight)
                    )
                }
            }
        }

        @Suppress("unused")
        @ObjCAction
        fun keyboardWillHide(arg: NSNotification) {
            keyboardOverlapHeightState.value = 0f
            if (configuration.onFocusBehavior == OnFocusBehavior.FocusableAboveKeyboard) {
                updateViewBounds(offsetY = 0.0)
            }
        }

        private fun calcFocusedLiftingY(focusedRect: DpRect, keyboardHeight: Double): Double {
            val viewHeight = attachedComposeContext?.view?.frame?.useContents {
                size.height
            } ?: 0.0

            val hiddenPartOfFocusedElement: Double =
                keyboardHeight - viewHeight + focusedRect.bottom.value
            return if (hiddenPartOfFocusedElement > 0) {
                // If focused element is partially hidden by the keyboard, we need to lift it upper
                val focusedTopY = focusedRect.top.value
                val isFocusedElementRemainsVisible = hiddenPartOfFocusedElement < focusedTopY
                if (isFocusedElementRemainsVisible) {
                    // We need to lift focused element to be fully visible
                    hiddenPartOfFocusedElement
                } else {
                    // In this case focused element height is bigger than remain part of the screen after showing the keyboard.
                    // Top edge of focused element should be visible. Same logic on Android.
                    maxOf(focusedTopY, 0f).toDouble()
                }
            } else {
                // Focused element is not hidden by the keyboard.
                0.0
            }
        }

        private fun updateViewBounds(offsetX: Double = 0.0, offsetY: Double = 0.0) {
            view.layer.setBounds(
                view.frame.useContents {
                    CGRectMake(
                        x = offsetX,
                        y = offsetY,
                        width = size.width,
                        height = size.height
                    )
                }
            )
        }
    }

    @Suppress("unused")
    @ObjCAction
    fun viewSafeAreaInsetsDidChange() {
        // super.viewSafeAreaInsetsDidChange() // TODO: call super after Kotlin 1.8.20
        view.safeAreaInsets.useContents {
            safeAreaState.value = IOSInsets(
                top = top.dp,
                bottom = bottom.dp,
                left = left.dp,
                right = right.dp,
            )
        }
        view.directionalLayoutMargins.useContents {
            layoutMarginsState.value = IOSInsets(
                top = top.dp,
                bottom = bottom.dp,
                left = leading.dp,
                right = trailing.dp,
            )
        }
    }

    override fun loadView() {
        view = UIView().apply {
            backgroundColor = UIColor.whiteColor
            setClipsToBounds(true)
        } // rootView needs to interop with UIKit
    }

    override fun viewDidLoad() {
        super.viewDidLoad()

        PlistSanityCheck.performIfNeeded()

        configuration.delegate.viewDidLoad()
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)

        systemTheme.value = traitCollection.userInterfaceStyle.asComposeSystemTheme()
    }

    override fun viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()

        // UIKit possesses all required info for layout at this point
        currentInterfaceOrientation?.let {
            interfaceOrientationState.value = it
        }

        attachedComposeContext?.let {
            updateLayout(it)
        }
    }

    private fun updateLayout(context: AttachedComposeContext) {
        val scale = density.density
        val size = view.frame.useContents {
            IntSize(
                width = (size.width * scale).roundToInt(),
                height = (size.height * scale).roundToInt()
            )
        }
        _windowInfo.containerSize = size
        context.scene.density = density
        context.scene.constraints = Constraints(
            maxWidth = size.width,
            maxHeight = size.height
        )

        context.view.needRedraw()
    }

    override fun viewWillTransitionToSize(
        size: CValue<CGSize>,
        withTransitionCoordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        super.viewWillTransitionToSize(size, withTransitionCoordinator)

        if (isInsideSwiftUI || presentingViewController != null) {
            // SwiftUI will do full layout and scene constraints update on each frame of orientation change animation
            // This logic is not needed

            // When presented modally, UIKit performs non-trivial hierarchy update durting orientation change,
            // its logic is not feasible to integrate into
            return
        }

        val attachedComposeContext = attachedComposeContext ?: return

        // Happens during orientation change from LandscapeLeft to LandscapeRight, for example
        val isSameSizeTransition = view.frame.useContents {
            CGSizeEqualToSize(size, this.size.readValue())
        }
        if (isSameSizeTransition) {
            return
        }

        val startSnapshotView =
            attachedComposeContext.view.snapshotViewAfterScreenUpdates(false) ?: return

        startSnapshotView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(startSnapshotView)
        size.useContents {
            NSLayoutConstraint.activateConstraints(
                listOf(
                    startSnapshotView.widthAnchor.constraintEqualToConstant(height),
                    startSnapshotView.heightAnchor.constraintEqualToConstant(width),
                    startSnapshotView.centerXAnchor.constraintEqualToAnchor(view.centerXAnchor),
                    startSnapshotView.centerYAnchor.constraintEqualToAnchor(view.centerYAnchor)
                )
            )
        }

        attachedComposeContext.view.isForcedToPresentWithTransactionEveryFrame = true

        attachedComposeContext.setConstraintsToCenterInView(view, size)
        attachedComposeContext.view.transform = withTransitionCoordinator.targetTransform

        view.layoutIfNeeded()

        withTransitionCoordinator.animateAlongsideTransition(
            animation = {
                startSnapshotView.alpha = 0.0
                startSnapshotView.transform =
                    CGAffineTransformInvert(withTransitionCoordinator.targetTransform)
                attachedComposeContext.view.transform = CGAffineTransformIdentity.readValue()
            },
            completion = {
                startSnapshotView.removeFromSuperview()
                attachedComposeContext.setConstraintsToFillView(view)
                attachedComposeContext.view.isForcedToPresentWithTransactionEveryFrame = false
            }
        )
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)

        isInsideSwiftUI = checkIfInsideSwiftUI()
        attachComposeIfNeeded()
        configuration.delegate.viewWillAppear(animated)
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)

        NSNotificationCenter.defaultCenter.addObserver(
            observer = keyboardVisibilityListener,
            selector = NSSelectorFromString(keyboardVisibilityListener::keyboardWillShow.name + ":"),
            name = UIKeyboardWillShowNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.addObserver(
            observer = keyboardVisibilityListener,
            selector = NSSelectorFromString(keyboardVisibilityListener::keyboardWillHide.name + ":"),
            name = UIKeyboardWillHideNotification,
            `object` = null
        )

        configuration.delegate.viewDidAppear(animated)

    }

    // viewDidUnload() is deprecated and not called.
    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)

        NSNotificationCenter.defaultCenter.removeObserver(
            observer = keyboardVisibilityListener,
            name = UIKeyboardWillShowNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.removeObserver(
            observer = keyboardVisibilityListener,
            name = UIKeyboardWillHideNotification,
            `object` = null
        )

        configuration.delegate.viewWillDisappear(animated)
    }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)

        dispose()

        dispatch_async(dispatch_get_main_queue()) {
            kotlin.native.internal.GC.collect()
        }

        configuration.delegate.viewDidDisappear(animated)
    }

    override fun didReceiveMemoryWarning() {
        println("didReceiveMemoryWarning")
        kotlin.native.internal.GC.collect()
        super.didReceiveMemoryWarning()
    }

    actual fun setContent(
        content: @Composable () -> Unit
    ) {
        this.content = content
    }

    actual fun dispose() {
        attachedComposeContext?.dispose()
        attachedComposeContext = null
    }

    private fun attachComposeIfNeeded() {
        if (attachedComposeContext != null) {
            return // already attached
        }

        val skikoUIView = SkikoUIView()

        skikoUIView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(skikoUIView)

        val inputServices = UIKitTextInputService(
            showSoftwareKeyboard = {
                skikoUIView.showScreenKeyboard()
            },
            hideSoftwareKeyboard = {
                skikoUIView.hideScreenKeyboard()
            },
            updateView = {
                skikoUIView.setNeedsDisplay() // redraw on next frame
                platform.QuartzCore.CATransaction.flush() // clear all animations
                skikoUIView.reloadInputViews() // update input (like screen keyboard)
            },
            textWillChange = { skikoUIView.textWillChange() },
            textDidChange = { skikoUIView.textDidChange() },
            selectionWillChange = { skikoUIView.selectionWillChange() },
            selectionDidChange = { skikoUIView.selectionDidChange() },
        )

        val inputTraits = inputServices.skikoUITextInputTraits

        val platform = object : Platform by Platform.Empty {
            override val windowInfo: WindowInfo
                get() = _windowInfo
            override val textInputService: PlatformTextInputService = inputServices
            override val viewConfiguration =
                object : ViewConfiguration {
                    override val longPressTimeoutMillis: Long get() = 500
                    override val doubleTapTimeoutMillis: Long get() = 300
                    override val doubleTapMinTimeMillis: Long get() = 40

                    // this value is originating from iOS 16 drag behavior reverse engineering
                    override val touchSlop: Float get() = with(density) { 10.dp.toPx() }
                }
            override val textToolbar = object : TextToolbar {
                override fun showMenu(
                    rect: Rect,
                    onCopyRequested: (() -> Unit)?,
                    onPasteRequested: (() -> Unit)?,
                    onCutRequested: (() -> Unit)?,
                    onSelectAllRequested: (() -> Unit)?
                ) {
                    val skiaRect = with(density) {
                        org.jetbrains.skia.Rect.makeLTRB(
                            l = rect.left / density,
                            t = rect.top / density,
                            r = rect.right / density,
                            b = rect.bottom / density,
                        )
                    }
                    skikoUIView.showTextMenu(
                        targetRect = skiaRect,
                        textActions = object : TextActions {
                            override val copy: (() -> Unit)? = onCopyRequested
                            override val cut: (() -> Unit)? = onCutRequested
                            override val paste: (() -> Unit)? = onPasteRequested
                            override val selectAll: (() -> Unit)? = onSelectAllRequested
                        }
                    )
                }

                /**
                 * TODO on UIKit native behaviour is hide text menu, when touch outside
                 */
                override fun hide() = skikoUIView.hideTextMenu()

                override val status: TextToolbarStatus
                    get() = if (skikoUIView.isTextMenuShown())
                        TextToolbarStatus.Shown
                    else
                        TextToolbarStatus.Hidden
            }

            override val inputModeManager = DefaultInputModeManager(InputMode.Touch)
        }

        val scene = ComposeScene(
            coroutineContext = getMainDispatcher(),
            platform = platform,
            density = density,
            invalidate = skikoUIView::needRedraw,
        )

        skikoUIView.input = inputServices.skikoInput
        skikoUIView.inputTraits = inputTraits
        skikoUIView.delegate = object : SkikoUIViewDelegate {
            override fun onKeyboardEvent(event: SkikoKeyboardEvent) {
                scene.sendKeyEvent(KeyEvent(event))
            }

            override fun pointInside(point: CValue<CGPoint>, event: UIEvent?): Boolean =
                point.useContents {
                    val hitsInteropView = attachedComposeContext?.scene?.mainOwner?.hitInteropView(
                        pointerPosition = Offset(
                            (x * density.density).toFloat(),
                            (y * density.density).toFloat()
                        ),
                        isTouchEvent = true,
                    ) ?: false

                    !hitsInteropView
                }

            override fun onPointerEvent(event: SkikoPointerEvent) {
                val scale = density.density

                scene.sendPointerEvent(
                    eventType = event.kind.toCompose(),
                    pointers = event.pointers.map {
                        ComposeScene.Pointer(
                            id = PointerId(it.id),
                            position = Offset(
                                x = it.x.toFloat() * scale,
                                y = it.y.toFloat() * scale
                            ),
                            pressed = it.pressed,
                            type = it.device.toCompose(),
                            pressure = it.pressure.toFloat(),
                        )
                    },
                    timeMillis = event.timestamp,
                    nativeEvent = event
                )
            }

            override fun retrieveCATransactionCommands(): List<() -> Unit> =
                interopContext.getActionsAndClear()

            override fun draw(surface: Surface, targetTimestamp: NSTimeInterval) {
                // The calculation is split in two instead of
                // `(targetTimestamp * 1e9).toLong()`
                // to avoid losing precision for fractional part
                val integral = floor(targetTimestamp)
                val fractional = targetTimestamp - integral
                val secondsToNanos = 1_000_000_000L
                val nanos = integral.roundToLong() * secondsToNanos + (fractional * 1e9).roundToLong()

                scene.render(surface.canvas, nanos)
            }
        }

        scene.setContent(
            onPreviewKeyEvent = inputServices::onPreviewKeyEvent,
            onKeyEvent = { false },
            content = {
                CompositionLocalProvider(
                    LocalLayerContainer provides view,
                    LocalUIViewController provides this,
                    LocalKeyboardOverlapHeightState provides keyboardOverlapHeightState,
                    LocalSafeAreaState provides safeAreaState,
                    LocalLayoutMarginsState provides layoutMarginsState,
                    LocalInterfaceOrientationState provides interfaceOrientationState,
                    LocalSystemTheme provides systemTheme.value,
                    LocalUIKitInteropContext provides interopContext,
                ) {
                    content()
                }
            },
        )


        attachedComposeContext =
            AttachedComposeContext(scene, skikoUIView).also {
                it.setConstraintsToFillView(view)
                updateLayout(it)
            }
    }
}

private fun UIViewController.checkIfInsideSwiftUI(): Boolean {
        var parent = parentViewController

        while (parent != null) {
            val isUIHostingController = parent.`class`()?.let {
                val className = NSStringFromClass(it)
                // SwiftUI UIHostingController has mangled name depending on generic instantiation type,
                // It always contains UIHostingController substring though
                return className.contains("UIHostingController")
            } ?: false

            if (isUIHostingController) {
                return true
            }

            parent = parent.parentViewController
        }

        return false
    }

private fun UIUserInterfaceStyle.asComposeSystemTheme(): SystemTheme {
    return when (this) {
        UIUserInterfaceStyle.UIUserInterfaceStyleLight -> SystemTheme.Light
        UIUserInterfaceStyle.UIUserInterfaceStyleDark -> SystemTheme.Dark
        else -> SystemTheme.Unknown
    }
}
