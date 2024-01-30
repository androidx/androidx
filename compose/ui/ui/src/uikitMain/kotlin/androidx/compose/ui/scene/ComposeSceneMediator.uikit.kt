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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.interop.LocalInteropContainer
import androidx.compose.ui.interop.LocalUIKitInteropContext
import androidx.compose.ui.interop.UIKitInteropContext
import androidx.compose.ui.interop.UIKitInteropTransaction
import androidx.compose.ui.platform.AccessibilityMediator
import androidx.compose.ui.platform.IOSPlatformContextImpl
import androidx.compose.ui.platform.LocalLayoutMargins
import androidx.compose.ui.platform.LocalSafeArea
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.UIKitTextInputService
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.toDpOffset
import androidx.compose.ui.toDpRect
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.LocalKeyboardOverlapHeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.FocusStack
import androidx.compose.ui.window.InteractionUIView
import androidx.compose.ui.window.KeyboardEventHandler
import androidx.compose.ui.window.KeyboardVisibilityListenerImpl
import androidx.compose.ui.window.RenderingUIView
import androidx.compose.ui.window.UITouchesEventPhase
import androidx.compose.ui.window.uiContentSizeCategoryToFontScaleMap
import kotlin.coroutines.CoroutineContext
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkikoKeyboardEvent
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformInvert
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSTimeInterval
import platform.QuartzCore.CATransaction
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIContentSizeCategoryUnspecified
import platform.UIKit.UIEvent
import platform.UIKit.UIKeyboardWillHideNotification
import platform.UIKit.UIKeyboardWillShowNotification
import platform.UIKit.UIScreen
import platform.UIKit.UITouch
import platform.UIKit.UITouchPhase
import platform.UIKit.UIView
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol
import platform.darwin.NSObject

/**
 * Layout of sceneView on the screen
 */
internal sealed interface SceneLayout {
    object Undefined : SceneLayout
    object UseConstraintsToFillContainer : SceneLayout
    class UseConstraintsToCenter(val size: CValue<CGSize>) : SceneLayout
    class Bounds(val rect: IntRect) : SceneLayout
}

private const val FEATURE_FLAG_ACCESSIBILITY_ENABLED = false

internal class ComposeSceneMediator(
    private val container: UIView,
    configuration: ComposeUIViewControllerConfiguration,
    private val focusStack: FocusStack<UIView>?,
    private val windowInfo: WindowInfo,
    val coroutineContext: CoroutineContext,
    private val renderingUIViewFactory: (RenderingUIView.Delegate) -> RenderingUIView,
    composeSceneFactory: (
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext
    ) -> ComposeScene,
) {
    private val focusable: Boolean get() = focusStack != null
    private val keyboardOverlapHeightState: MutableState<Float> = mutableStateOf(0f)
    private var _layout: SceneLayout = SceneLayout.Undefined
    private var constraints: List<NSLayoutConstraint> = emptyList()
        set(value) {
            if (field.isNotEmpty()) {
                NSLayoutConstraint.deactivateConstraints(field)
            }
            field = value
            NSLayoutConstraint.activateConstraints(value)
        }

    private val scene: ComposeScene by lazy {
        composeSceneFactory(
            ::onComposeSceneInvalidate,
            platformContext,
            coroutineContext,
        )
    }
    var compositionLocalContext
        get() = scene.compositionLocalContext
        set(value) {
            scene.compositionLocalContext = value
        }
    private val focusManager get() = scene.focusManager

    private val renderingView by lazy {
        renderingUIViewFactory(renderDelegate)
    }

    /**
     * Container for UIKitView and UIKitViewController
     */
    private val interopViewContainer = UIView()

    private val interactionView by lazy {
        InteractionUIView(
            keyboardEventHandler = keyboardEventHandler,
            touchesDelegate = touchesDelegate,
            updateTouchesCount = { count ->
                val needHighFrequencyPolling = count > 0
                renderingView.redrawer.needsProactiveDisplayLink = needHighFrequencyPolling
            },
            checkBounds = { dpPoint: DpOffset ->
                val point = dpPoint.toOffset(getSystemDensity())
                getBoundsInPx().contains(point.round())
            }
        )
    }

    private fun getSystemDensity(): Density {
        val contentSizeCategory = container.traitCollection.preferredContentSizeCategory
            ?: UIContentSizeCategoryUnspecified
        return Density(
            density = UIScreen.mainScreen.scale.toFloat(),
            fontScale = uiContentSizeCategoryToFontScaleMap[contentSizeCategory] ?: 1.0f
        )
    }

    private val interopContext: UIKitInteropContext by lazy {
        UIKitInteropContext(
            requestRedraw = { onComposeSceneInvalidate() }
        )
    }

    private val semanticsOwnerListener: PlatformContext.SemanticsOwnerListener by lazy {
        object : PlatformContext.SemanticsOwnerListener {
            var current: Pair<SemanticsOwner, AccessibilityMediator>? = null

            override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
                if (current == null) {
                    current = semanticsOwner to AccessibilityMediator(
                        container,
                        semanticsOwner,
                        coroutineContext
                    )
                } else {
                    // Multiple SemanticsOwner`s per ComposeSceneMediator is a legacy behavior and will not be supported
                }
            }

            override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
                val current = checkNotNull(current)

                if (current.first == semanticsOwner) {
                    current.second.dispose()
                    this.current = null
                }
            }

            override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
                val current = current ?: return

                if (current.first == semanticsOwner) {
                    current.second.onSemanticsChange()
                }
            }
        }
    }

    val platformContext: PlatformContext by lazy {
        val semanticsOwnerListener = if (FEATURE_FLAG_ACCESSIBILITY_ENABLED) {
            semanticsOwnerListener
        } else {
            null
        }

        IOSPlatformContextImpl(
            inputServices = uiKitTextInputService,
            textToolbar = uiKitTextInputService,
            windowInfo = windowInfo,
            density = getSystemDensity(),
            semanticsOwnerListener = semanticsOwnerListener
        )
    }

    private val keyboardVisibilityListener by lazy {
        KeyboardVisibilityListenerImpl(
            configuration = configuration,
            keyboardOverlapHeightState = keyboardOverlapHeightState,
            viewProvider = { container },
            densityProvider = ::getSystemDensity,
            composeSceneMediatorProvider = { this },
            focusManager = focusManager,
        )
    }

    private val keyboardEventHandler: KeyboardEventHandler by lazy {
        object : KeyboardEventHandler {
            override fun onKeyboardEvent(event: SkikoKeyboardEvent) {
                val composeEvent = KeyEvent(event)
                if (!uiKitTextInputService.onPreviewKeyEvent(composeEvent)) {
                    scene.sendKeyEvent(composeEvent)
                }
            }
        }
    }

    private val uiKitTextInputService: UIKitTextInputService by lazy {
        UIKitTextInputService(
            updateView = {
                renderingView.setNeedsDisplay() // redraw on next frame
                CATransaction.flush() // clear all animations
            },
            rootViewProvider = { container },
            densityProvider = ::getSystemDensity,
            focusStack = focusStack,
            keyboardEventHandler = keyboardEventHandler
        )
    }

    private val touchesDelegate: InteractionUIView.Delegate by lazy {
        object : InteractionUIView.Delegate {
            override fun pointInside(point: CValue<CGPoint>, event: UIEvent?): Boolean =
                point.useContents {
                    val position = this.toDpOffset().toOffset(density)
                    !scene.hitTestInteropView(position)
                }

            override fun onTouchesEvent(view: UIView, event: UIEvent, phase: UITouchesEventPhase) {
                scene.sendPointerEvent(
                    eventType = phase.toPointerEventType(),
                    pointers = event.touchesForView(view)?.map {
                        val touch = it as UITouch
                        val id = touch.hashCode().toLong()
                        val position = touch.offsetInView(view, density.density)
                        ComposeScenePointer(
                            id = PointerId(id),
                            position = position,
                            pressed = touch.isPressed,
                            type = PointerType.Touch,
                            pressure = touch.force.toFloat(),
                            historical = event.historicalChangesForTouch(
                                touch,
                                view,
                                density.density
                            )
                        )
                    } ?: emptyList(),
                    timeMillis = (event.timestamp * 1e3).toLong(),
                    nativeEvent = event
                )
            }
        }
    }

    private val renderDelegate by lazy {
        object : RenderingUIView.Delegate {
            override fun retrieveInteropTransaction(): UIKitInteropTransaction =
                interopContext.retrieve()

            override fun render(canvas: Canvas, targetTimestamp: NSTimeInterval) {
                val composeCanvas = canvas.asComposeCanvas()
                val topLeft = getBoundsInPx().topLeft.toOffset()
                composeCanvas.translate(-topLeft.x, -topLeft.y)
                scene.render(composeCanvas, targetTimestamp.toNanoSeconds())
                composeCanvas.translate(topLeft.x, topLeft.y)
            }
        }
    }

    private var onAttachedToWindow: (() -> Unit)? = null
    private fun runOnceViewAttached(block: () -> Unit) {
        if (renderingView.window == null) {
            onAttachedToWindow = {
                onAttachedToWindow = null
                block()
            }
        } else {
            block()
        }
    }

    init {
        renderingView.onAttachedToWindow = {
            renderingView.onAttachedToWindow = null
            viewWillLayoutSubviews()
            this.onAttachedToWindow?.invoke()
            focusStack?.pushAndFocus(interactionView)
        }
        container.addSubview(interopViewContainer)
        container.addSubview(interactionView)
        interactionView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activateConstraints(
            getConstraintsToFillParent(interactionView, container)
        )
        interactionView.addSubview(renderingView)
    }

    fun setContent(content: @Composable () -> Unit) {
        runOnceViewAttached {
            scene.setContent {
                /**
                 * TODO isReadyToShowContent it is workaround we need to fix.
                 *  https://github.com/JetBrains/compose-multiplatform-core/pull/861
                 *  Density problem already was fixed.
                 *  But there are still problem with safeArea.
                 *  Elijah founded possible solution:
                 *   https://developer.apple.com/documentation/uikit/uiviewcontroller/4195485-viewisappearing
                 *   It is public for iOS 17 and hope back ported for iOS 13 as well (but we need to check)
                 */
                if (renderingView.isReadyToShowContent.value) {
                    ProvideComposeSceneMediatorCompositionLocals {
                        content()
                    }
                }
            }
        }
    }

    private val safeAreaState: MutableState<PlatformInsets> by lazy {
        //TODO It calc 0,0,0,0 on initialization
        mutableStateOf(calcSafeArea())
    }
    private val layoutMarginsState: MutableState<PlatformInsets> by lazy {
        //TODO It calc 0,0,0,0 on initialization
        mutableStateOf(calcLayoutMargin())
    }

    fun viewSafeAreaInsetsDidChange() {
        safeAreaState.value = calcSafeArea()
        layoutMarginsState.value = calcLayoutMargin()
    }

    @OptIn(InternalComposeApi::class)
    @Composable
    private fun ProvideComposeSceneMediatorCompositionLocals(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalUIKitInteropContext provides interopContext,
            LocalKeyboardOverlapHeight provides keyboardOverlapHeightState.value,
            LocalSafeArea provides safeAreaState.value,
            LocalLayoutMargins provides layoutMarginsState.value,
            LocalInteropContainer provides interopViewContainer,
            content = content
        )

    fun dispose() {
        focusStack?.popUntilNext(renderingView)
        renderingView.dispose()
        renderingView.removeFromSuperview()
        interactionView.dispose()
        interactionView.removeFromSuperview()
        scene.close()
        // After scene is disposed all UIKit interop actions can't be deferred to be synchronized with rendering
        // Thus they need to be executed now.
        interopContext.retrieve().actions.forEach { it.invoke() }
    }

    fun onComposeSceneInvalidate() = renderingView.needRedraw()

    fun setLayout(value: SceneLayout) {
        _layout = value
        when (value) {
            SceneLayout.UseConstraintsToFillContainer -> {
                renderingView.setFrame(CGRectZero.readValue())
                renderingView.translatesAutoresizingMaskIntoConstraints = false
                constraints = getConstraintsToFillParent(renderingView, interactionView)
            }

            is SceneLayout.UseConstraintsToCenter -> {
                renderingView.setFrame(CGRectZero.readValue())
                renderingView.translatesAutoresizingMaskIntoConstraints = false
                constraints =
                    getConstraintsToCenterInParent(renderingView, interactionView, value.size)
            }

            is SceneLayout.Bounds -> {
                val density = getSystemDensity().density
                renderingView.translatesAutoresizingMaskIntoConstraints = true
                renderingView.setFrame(
                    with(value.rect) {
                        CGRectMake(
                            x = left.toDouble() / density,
                            y = top.toDouble() / density,
                            width = width.toDouble() / density,
                            height = height.toDouble() / density
                        )
                    }
                )
                constraints = emptyList()
            }

            is SceneLayout.Undefined -> error("setLayout, SceneLayout.Undefined")
        }
    }

    fun viewWillLayoutSubviews() {
        val density = getSystemDensity()
        //TODO: Current code updates layout based on rootViewController size.
        // Maybe we need to rewrite it for SingleLayerComposeScene.

        val boundsInWindow = container.convertRect(
            rect = container.bounds,
            toView = null
        ).useContents {
            with(density) {
                toDpRect().toRect().roundToIntRect()
            }
        }
        scene.density = density // TODO: Maybe it is wrong to set density to scene here?
        scene.boundsInWindow = boundsInWindow
        onComposeSceneInvalidate()
    }

    private fun calcSafeArea(): PlatformInsets =
        container.safeAreaInsets.useContents {
            PlatformInsets(
                left = left.dp,
                top = top.dp,
                right = right.dp,
                bottom = bottom.dp,
            )
        }

    private fun calcLayoutMargin(): PlatformInsets =
        container.directionalLayoutMargins.useContents {
            PlatformInsets(
                left = leading.dp, // TODO: Check RTL support
                top = top.dp,
                right = trailing.dp, // TODO: Check RTL support
                bottom = bottom.dp,
            )
        }

    fun getBoundsInDp(): DpRect = renderingView.frame.useContents { this.toDpRect() }

    fun getBoundsInPx(): IntRect = with(getSystemDensity()) {
        getBoundsInDp().toRect().roundToIntRect()
    }

    fun viewWillTransitionToSize(
        targetSize: CValue<CGSize>,
        coordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        if (_layout is SceneLayout.Bounds) {
            //TODO Add logic to SceneLayout.Bounds too
            return
        }

        val startSnapshotView = renderingView.snapshotViewAfterScreenUpdates(false) ?: return
        startSnapshotView.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(startSnapshotView)
        targetSize.useContents {
            NSLayoutConstraint.activateConstraints(
                listOf(
                    startSnapshotView.widthAnchor.constraintEqualToConstant(height),
                    startSnapshotView.heightAnchor.constraintEqualToConstant(width),
                    startSnapshotView.centerXAnchor.constraintEqualToAnchor(container.centerXAnchor),
                    startSnapshotView.centerYAnchor.constraintEqualToAnchor(container.centerYAnchor)
                )
            )
        }

        renderingView.isForcedToPresentWithTransactionEveryFrame = true

        setLayout(SceneLayout.UseConstraintsToCenter(size = targetSize))
        renderingView.transform = coordinator.targetTransform

        coordinator.animateAlongsideTransition(
            animation = {
                startSnapshotView.alpha = 0.0
                startSnapshotView.transform = CGAffineTransformInvert(coordinator.targetTransform)
                renderingView.transform = CGAffineTransformIdentity.readValue()
            },
            completion = {
                startSnapshotView.removeFromSuperview()
                setLayout(SceneLayout.UseConstraintsToFillContainer)
                renderingView.isForcedToPresentWithTransactionEveryFrame = false
            }
        )
    }

    private val nativeKeyboardVisibilityListener = object : NSObject() {
        @Suppress("unused")
        @ObjCAction
        fun keyboardWillShow(arg: NSNotification) {
            keyboardVisibilityListener.keyboardWillShow(arg)
        }

        @Suppress("unused")
        @ObjCAction
        fun keyboardWillHide(arg: NSNotification) {
            keyboardVisibilityListener.keyboardWillHide(arg)
        }
    }

    fun viewDidAppear(animated: Boolean) {
        NSNotificationCenter.defaultCenter.addObserver(
            observer = nativeKeyboardVisibilityListener,
            selector = NSSelectorFromString(nativeKeyboardVisibilityListener::keyboardWillShow.name + ":"),
            name = UIKeyboardWillShowNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.addObserver(
            observer = nativeKeyboardVisibilityListener,
            selector = NSSelectorFromString(nativeKeyboardVisibilityListener::keyboardWillHide.name + ":"),
            name = UIKeyboardWillHideNotification,
            `object` = null
        )
    }

    // viewDidUnload() is deprecated and not called.
    fun viewWillDisappear(animated: Boolean) {
        NSNotificationCenter.defaultCenter.removeObserver(
            observer = nativeKeyboardVisibilityListener,
            name = UIKeyboardWillShowNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.removeObserver(
            observer = nativeKeyboardVisibilityListener,
            name = UIKeyboardWillHideNotification,
            `object` = null
        )
    }

    fun getViewHeight(): Double = renderingView.frame.useContents {
        size.height
    }

    var density by scene::density
    var layoutDirection by scene::layoutDirection

}

internal fun getConstraintsToFillParent(view: UIView, parent: UIView) =
    listOf(
        view.leftAnchor.constraintEqualToAnchor(parent.leftAnchor),
        view.rightAnchor.constraintEqualToAnchor(parent.rightAnchor),
        view.topAnchor.constraintEqualToAnchor(parent.topAnchor),
        view.bottomAnchor.constraintEqualToAnchor(parent.bottomAnchor)
    )

private fun getConstraintsToCenterInParent(
    view: UIView,
    parentView: UIView,
    size: CValue<CGSize>,
) = size.useContents {
    listOf(
        view.centerXAnchor.constraintEqualToAnchor(parentView.centerXAnchor),
        view.centerYAnchor.constraintEqualToAnchor(parentView.centerYAnchor),
        view.widthAnchor.constraintEqualToConstant(width),
        view.heightAnchor.constraintEqualToConstant(height)
    )
}

private fun UITouchesEventPhase.toPointerEventType(): PointerEventType =
    when (this) {
        UITouchesEventPhase.BEGAN -> PointerEventType.Press
        UITouchesEventPhase.MOVED -> PointerEventType.Move
        UITouchesEventPhase.ENDED -> PointerEventType.Release
        UITouchesEventPhase.CANCELLED -> PointerEventType.Release
    }

private fun UIEvent.historicalChangesForTouch(
    touch: UITouch,
    view: UIView,
    density: Float
): List<HistoricalChange> {
    val touches = coalescedTouchesForTouch(touch) ?: return emptyList()

    return if (touches.size > 1) {
        // subList last index is exclusive, so the last touch in the list is not included
        // because it's the actual touch for which coalesced touches were requested
        touches.dropLast(1).map {
            val historicalTouch = it as UITouch
            HistoricalChange(
                uptimeMillis = (historicalTouch.timestamp * 1e3).toLong(),
                position = historicalTouch.offsetInView(view, density)
            )
        }
    } else {
        emptyList()
    }
}

private val UITouch.isPressed
    get() = when (phase) {
        UITouchPhase.UITouchPhaseEnded, UITouchPhase.UITouchPhaseCancelled -> false
        else -> true
    }

private fun UITouch.offsetInView(view: UIView, density: Float): Offset =
    locationInView(view).useContents {
        Offset(x.toFloat() * density, y.toFloat() * density)
    }

private fun NSTimeInterval.toNanoSeconds(): Long {
    // The calculation is split in two instead of
    // `(targetTimestamp * 1e9).toLong()`
    // to avoid losing precision for fractional part
    val integral = floor(this)
    val fractional = this - integral
    val secondsToNanos = 1_000_000_000L
    val nanos = integral.roundToLong() * secondsToNanos + (fractional * 1e9).roundToLong()
    return nanos
}
