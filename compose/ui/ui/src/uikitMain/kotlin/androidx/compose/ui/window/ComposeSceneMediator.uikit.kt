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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.interop.LocalUIKitInteropContext
import androidx.compose.ui.interop.UIKitInteropContext
import androidx.compose.ui.platform.AccessibilityMediator
import androidx.compose.ui.platform.IOSPlatformContextImpl
import androidx.compose.ui.platform.LocalLayoutMargins
import androidx.compose.ui.platform.LocalSafeArea
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.UIKitTextInputService
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.LocalKeyboardOverlapHeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import org.jetbrains.skiko.SkikoKeyboardEvent
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformInvert
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CATransaction
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIKeyboardWillHideNotification
import platform.UIKit.UIKeyboardWillShowNotification
import platform.UIKit.UIView
import platform.UIKit.UIViewController
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
    private val viewController: UIViewController,
    configuration: ComposeUIViewControllerConfiguration,
    private val focusStack: FocusStack<UIView>?,
    private val windowInfo: WindowInfo,
    transparency: Boolean,
    private val coroutineContext: CoroutineContext,
    buildScene: (ComposeSceneMediator) -> ComposeScene,
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

    private val scene: ComposeScene by lazy { buildScene(this) }
    var compositionLocalContext
        get() = scene.compositionLocalContext
        set(value) {
            scene.compositionLocalContext = value
        }
    private val focusManager get() = scene.focusManager

    val view: SkikoUIView by lazy {
        SkikoUIView(keyboardEventHandler, delegate, transparency)
    }

    val densityProvider by lazy {
        DensityProviderImpl(
            uiViewControllerProvider = { viewController },
            viewProvider = { view },
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
                    current = semanticsOwner to AccessibilityMediator(viewController.view, semanticsOwner, coroutineContext)
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
            densityProvider = densityProvider,
            semanticsOwnerListener = semanticsOwnerListener
        )
    }

    private val keyboardVisibilityListener by lazy {
        KeyboardVisibilityListenerImpl(
            configuration = configuration,
            keyboardOverlapHeightState = keyboardOverlapHeightState,
            viewProvider = { viewController.view },
            densityProvider = densityProvider,
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
                view.setNeedsDisplay() // redraw on next frame
                CATransaction.flush() // clear all animations
            },
            rootViewProvider = { viewController.view },
            densityProvider = densityProvider,
            focusStack = focusStack,
            keyboardEventHandler = keyboardEventHandler
        )
    }

    private val delegate: SkikoUIViewDelegate by lazy {
        SkikoUIViewDelegateImpl(
            { scene },
            interopContext,
            densityProvider,
        )
    }

    private var onAttachedToWindow: (() -> Unit)? = null
    private fun runOnceViewAttached(block: () -> Unit) {
        if (view.window == null) {
            onAttachedToWindow = {
                onAttachedToWindow = null
                block()
            }
        } else {
            block()
        }
    }

    init {
        view.onAttachedToWindow = {
            view.onAttachedToWindow = null
            viewWillLayoutSubviews()
            this.onAttachedToWindow?.invoke()
            focusStack?.pushAndFocus(view)
        }
        viewController.view.addSubview(view)
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
                if (view.isReadyToShowContent.value) {
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
            content = content
        )

    fun dispose() {
        focusStack?.popUntilNext(view)
        view.dispose()
        view.removeFromSuperview()
        scene.close()
        // After scene is disposed all UIKit interop actions can't be deferred to be synchronized with rendering
        // Thus they need to be executed now.
        interopContext.retrieve().actions.forEach { it.invoke() }
    }

    fun onComposeSceneInvalidate() = view.needRedraw()

    fun setLayout(value: SceneLayout) {
        _layout = value
        when (value) {
            SceneLayout.UseConstraintsToFillContainer -> {
                delegate.metalOffset = Offset.Zero
                view.setFrame(CGRectZero.readValue())
                view.translatesAutoresizingMaskIntoConstraints = false
                constraints = listOf(
                    view.leftAnchor.constraintEqualToAnchor(viewController.view.leftAnchor),
                    view.rightAnchor.constraintEqualToAnchor(viewController.view.rightAnchor),
                    view.topAnchor.constraintEqualToAnchor(viewController.view.topAnchor),
                    view.bottomAnchor.constraintEqualToAnchor(viewController.view.bottomAnchor)
                )
            }

            is SceneLayout.UseConstraintsToCenter -> {
                delegate.metalOffset = Offset.Zero
                view.setFrame(CGRectZero.readValue())
                view.translatesAutoresizingMaskIntoConstraints = false
                constraints = value.size.useContents {
                    listOf(
                        view.centerXAnchor.constraintEqualToAnchor(viewController.view.centerXAnchor),
                        view.centerYAnchor.constraintEqualToAnchor(viewController.view.centerYAnchor),
                        view.widthAnchor.constraintEqualToConstant(width),
                        view.heightAnchor.constraintEqualToConstant(height)
                    )
                }
            }

            is SceneLayout.Bounds -> {
                delegate.metalOffset = -value.rect.topLeft.toOffset()
                val density = densityProvider().density
                view.translatesAutoresizingMaskIntoConstraints = true
                view.setFrame(
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
        view.updateMetalLayerSize()
    }

    fun viewWillLayoutSubviews() {
        val density = densityProvider()
        val scale = density.density
        //TODO: Current code updates layout based on rootViewController size.
        // Maybe we need to rewrite it for SingleLayerComposeScene.
        val size = viewController.view.frame.useContents {
            IntSize(
                width = (size.width * scale).roundToInt(),
                height = (size.height * scale).roundToInt()
            )
        }
        scene.density = density
        scene.size = size
        onComposeSceneInvalidate()
    }

    private fun calcSafeArea(): PlatformInsets =
        viewController.view.safeAreaInsets.useContents {
            PlatformInsets(
                left = left.dp,
                top = top.dp,
                right = right.dp,
                bottom = bottom.dp,
            )
        }

    private fun calcLayoutMargin(): PlatformInsets =
        viewController.view.directionalLayoutMargins.useContents {
            PlatformInsets(
                left = leading.dp, // TODO: Check RTL support
                top = top.dp,
                right = trailing.dp, // TODO: Check RTL support
                bottom = bottom.dp,
            )
        }

    fun getViewBounds(): IntRect = view.frame.useContents {
        val density = densityProvider().density
        IntRect(
            offset = IntOffset(
                x = (origin.x * density).roundToInt(),
                y = (origin.y * density).roundToInt(),
            ),
            size = IntSize(
                width = (size.width * density).roundToInt(),
                height = (size.height * density).roundToInt(),
            )
        )
    }

    fun viewWillTransitionToSize(
        targetSize: CValue<CGSize>,
        coordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        if (_layout is SceneLayout.Bounds) {
            //TODO Add logic to SceneLayout.Bounds too
            return
        }

        val startSnapshotView = view.snapshotViewAfterScreenUpdates(false) ?: return
        startSnapshotView.translatesAutoresizingMaskIntoConstraints = false
        viewController.view.addSubview(startSnapshotView)
        targetSize.useContents {
            NSLayoutConstraint.activateConstraints(
                listOf(
                    startSnapshotView.widthAnchor.constraintEqualToConstant(height),
                    startSnapshotView.heightAnchor.constraintEqualToConstant(width),
                    startSnapshotView.centerXAnchor.constraintEqualToAnchor(viewController.view.centerXAnchor),
                    startSnapshotView.centerYAnchor.constraintEqualToAnchor(viewController.view.centerYAnchor)
                )
            )
        }

        view.isForcedToPresentWithTransactionEveryFrame = true

        setLayout(SceneLayout.UseConstraintsToCenter(size = targetSize))
        view.transform = coordinator.targetTransform

        coordinator.animateAlongsideTransition(
            animation = {
                startSnapshotView.alpha = 0.0
                startSnapshotView.transform = CGAffineTransformInvert(coordinator.targetTransform)
                view.transform = CGAffineTransformIdentity.readValue()
            },
            completion = {
                startSnapshotView.removeFromSuperview()
                setLayout(SceneLayout.UseConstraintsToFillContainer)
                view.isForcedToPresentWithTransactionEveryFrame = false
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

}
