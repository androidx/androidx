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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.backhandler.LocalBackGestureDispatcher
import androidx.compose.ui.backhandler.UIKitBackGestureDispatcher
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.hapticfeedback.CupertinoHapticFeedback
import androidx.compose.ui.platform.IOSLifecycleOwner
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInternalViewModelStoreOwner
import androidx.compose.ui.platform.MotionDurationScaleImpl
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.LocalInterfaceOrientation
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.uikit.PlistSanityCheck
import androidx.compose.ui.uikit.density
import androidx.compose.ui.uikit.embedSubview
import androidx.compose.ui.uikit.utils.CMPViewController
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.asDpRect
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.viewinterop.UIKitInteropAction
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import androidx.compose.ui.window.ApplicationActiveStateListener
import androidx.compose.ui.window.ComposeView
import androidx.compose.ui.window.DisplayLinkListener
import androidx.compose.ui.window.FocusedViewsList
import androidx.compose.ui.window.MetalRedrawer
import androidx.compose.ui.window.MetalView
import androidx.compose.ui.window.ViewControllerLifecycleDelegate
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.coroutines.CoroutineContext
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.test.assertNotNull
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGSize
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol
import platform.UIKit.UIWindow
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(BetaInteropApi::class)
@ExportObjCClass
internal class ComposeHostingViewController(
    private val configuration: ComposeUIViewControllerConfiguration,
    private val content: @Composable () -> Unit,
    private val lifecycleOwner: IOSLifecycleOwner = IOSLifecycleOwner(),
    coroutineContext: CoroutineContext = Dispatchers.Main
) : CMPViewController(lifecycleDelegate = ViewControllerLifecycleDelegate(lifecycleOwner)) {
    private val hapticFeedback = CupertinoHapticFeedback()

    private val rootView = ComposeView(
        transparentForTouches = false,
        useOpaqueConfiguration = configuration.opaque,
    )
    // Used for testing
    val rootViewRedrawer: MetalRedrawer? get() = rootView.redrawer
    private var mediator: ComposeSceneMediator? = null
    private val windowContext = PlatformWindowContext()
    private var layers: UIKitComposeSceneLayersHolder? = null
    private val layoutDirection get() = getLayoutDirection()
    private var hasViewAppeared: Boolean = false
    private val motionDurationScale = MotionDurationScaleImpl()
    private var applicationActiveStateListener: ApplicationActiveStateListener? = null
    private val composeCoroutineContext: CoroutineContext = coroutineContext + motionDurationScale
    private var savableStateRegistry = SaveableStateRegistry(
        restoredValues = null, canBeSaved = { true }
    )

    private val backGestureDispatcher = UIKitBackGestureDispatcher(
        enableBackGesture = configuration.enableBackGesture,
        density = rootView.density,
        getTopLeftOffsetInWindow = { IntOffset.Zero } //full screen
    )

    fun hasInvalidations(): Boolean {
        return mediator?.hasInvalidations == true || layers?.hasInvalidations == true
    }

    /*
     * Initial value is arbitrarily chosen to avoid propagating invalid value logic
     * It's never the case in real usage scenario to reflect that in type system
     */
    private val interfaceOrientationState: MutableState<InterfaceOrientation> = mutableStateOf(
        InterfaceOrientation.Portrait
    )
    private val systemThemeState: MutableState<SystemTheme> = mutableStateOf(SystemTheme.Unknown)

    var focusedViewsList: FocusedViewsList? = FocusedViewsList()

    /*
     * On iOS >= 13.0 interfaceOrientation will be deduced from [UIWindowScene] of [UIWindow]
     * to which our [ComposeViewController] is attached.
     * It's never UIInterfaceOrientationUnknown, if accessed after owning [UIWindow] was made key and visible:
     * https://developer.apple.com/documentation/uikit/uiwindow/1621601-makekeyandvisible?language=objc
     */
    private val currentInterfaceOrientation: InterfaceOrientation?
        get() {
            // Modern: https://developer.apple.com/documentation/uikit/uiwindowscene/3198088-interfaceorientation?language=objc
            // Deprecated: https://developer.apple.com/documentation/uikit/uiapplication/1623026-statusbarorientation?language=objc
            return InterfaceOrientation.getByRawValue(
                if (available(OS.Ios to OSVersion(13))) {
                    view.window?.windowScene?.interfaceOrientation
                        ?: UIApplication.sharedApplication.statusBarOrientation
                } else {
                    UIApplication.sharedApplication.statusBarOrientation
                }
            )
        }

    @Suppress("DEPRECATION")
    override fun preferredStatusBarStyle(): UIStatusBarStyle =
        configuration.delegate.preferredStatusBarStyle
            ?: super.preferredStatusBarStyle()

    @Suppress("DEPRECATION")
    override fun preferredStatusBarUpdateAnimation(): UIStatusBarAnimation =
        configuration.delegate.preferredStatysBarAnimation
            ?: super.preferredStatusBarUpdateAnimation()

    @Suppress("DEPRECATION")
    override fun prefersStatusBarHidden(): Boolean =
        configuration.delegate.prefersStatusBarHidden
            ?: super.prefersStatusBarHidden()

    override fun loadView() {
        view = rootView
    }

    @Suppress("DEPRECATION")
    override fun viewDidLoad() {
        super.viewDidLoad()

        if (configuration.enforceStrictPlistSanityCheck) {
            PlistSanityCheck.performIfNeeded()
        }

        configuration.delegate.viewDidLoad()
        systemThemeState.value = traitCollection.userInterfaceStyle.asComposeSystemTheme()
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        mediator?.updateInteractionRect()

        windowContext.updateWindowContainerSize()
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)

        systemThemeState.value = traitCollection.userInterfaceStyle.asComposeSystemTheme()
    }

    private fun onDidMoveToWindow(window: UIWindow?) {
        backGestureDispatcher.onDidMoveToWindow(window, rootView)
        val windowContainer = window ?: return

        updateInterfaceOrientationState()

        layers?.containerView = layersContainerView()
        windowContext.setWindowContainer(windowContainer)
        updateMotionSpeed()
    }

    private fun updateInterfaceOrientationState() {
        currentInterfaceOrientation?.let {
            updateInterfaceOrientation(it)
        }
    }

    fun updateInterfaceOrientation(orientation: InterfaceOrientation) {
        interfaceOrientationState.value = orientation
    }


    override fun viewWillTransitionToSize(
        size: CValue<CGSize>,
        withTransitionCoordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        super.viewWillTransitionToSize(size, withTransitionCoordinator)

        updateInterfaceOrientationState()

        if (mediator?.hasInteropViews == true || layers?.hasInteropViews == true) {
            // Heavy interop views may slow down the animation of per-frame screen rotation.
            // For these cases, a cross-fade transition will be used instead.
            crossFadeSizeTransition(withTransitionCoordinator)
        } else {
            animateSizeTransition(withTransitionCoordinator)
        }
    }

    @Suppress("DEPRECATION")
    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)

        configuration.delegate.viewWillAppear(animated)
    }

    @Suppress("DEPRECATION")
    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        hasViewAppeared = true
        mediator?.sceneDidAppear()
        layers?.viewDidAppear()
        configuration.delegate.viewDidAppear(animated)

        // Because the container view can change during the modal transition animation,
        // the gesture handlers and layers view are added back when the animation ends.
        backGestureDispatcher.onDidMoveToWindow(view.window, rootView)
        layers?.containerView = layersContainerView()
    }

    @Suppress("DEPRECATION")
    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        hasViewAppeared = false
        mediator?.sceneWillDisappear()
        layers?.viewWillDisappear()
        configuration.delegate.viewWillDisappear(animated)

        backGestureDispatcher.onDidMoveToWindow(null, rootView)
    }

    @Suppress("DEPRECATION")
    @OptIn(NativeRuntimeApi::class)
    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)

        dispatch_async(dispatch_get_main_queue()) {
            GC.collect()
        }

        configuration.delegate.viewDidDisappear(animated)
    }

    override fun viewControllerDidEnterWindowHierarchy() {
        super.viewControllerDidEnterWindowHierarchy()

        val metalView = MetalView(
            retrieveInteropTransaction = {
                mediator?.retrieveInteropTransaction() ?: object : UIKitInteropTransaction {
                    override val actions = emptyList<UIKitInteropAction>()
                    override val isInteropActive = false
                }
            },
            useSeparateRenderThreadWhenPossible = configuration.parallelRendering,
            render = { canvas, nanoTime ->
                mediator?.render(canvas.asComposeCanvas(), nanoTime)
            }
        )
        metalView.canBeOpaque = configuration.opaque

        val layers = UIKitComposeSceneLayersHolder(windowContext, configuration.parallelRendering)
        layers.containerView = layersContainerView()
        this.layers = layers

        mediator = ComposeSceneMediator(
            onFocusBehavior = configuration.onFocusBehavior,
            focusedViewsList = focusedViewsList,
            windowContext = windowContext,
            coroutineContext = composeCoroutineContext,
            redrawer = metalView.redrawer,
            composeSceneFactory = { invalidate, context ->
                createComposeScene(invalidate, context, layers.metalView)
            },
            backGestureDispatcher = backGestureDispatcher,
            interfaceOrientationState = interfaceOrientationState,
        ).also { mediator ->
            rootView.embedSubview(mediator.inputView)
            rootView.updateMetalView(metalView, ::onDidMoveToWindow)
            rootView.embedSubview(mediator.overlayView)

            mediator.updateInteractionRect()
            mediator.setContent {
                ProvideContainerCompositionLocals(content)
            }
        }

        applicationActiveStateListener = ApplicationActiveStateListener { isApplicationActive ->
            if (isApplicationActive) {
                updateMotionSpeed()
            }
        }

        backGestureDispatcher.onDidMoveToWindow(view.window, rootView)
        onAccessibilityChanged()
    }

    override fun viewControllerDidLeaveWindowHierarchy() {
        super.viewControllerDidLeaveWindowHierarchy()

        // Store the current state in the next SaveableStateRegistry instance. It is used to
        // provide the saved state to the next compose scene when the view controller re-enters
        // the window hierarchy.
        savableStateRegistry = SaveableStateRegistry(
            restoredValues = savableStateRegistry.performSave(),
            canBeSaved = { true }
        )

        rootView.updateMetalView(metalView = null)
        backGestureDispatcher.onDidMoveToWindow(null, rootView)

        mediator?.dispose()
        mediator = null

        applicationActiveStateListener?.dispose()
        applicationActiveStateListener = null

        layers?.dispose(hasViewAppeared)
        layers = null
    }

    @OptIn(NativeRuntimeApi::class)
    override fun didReceiveMemoryWarning() {
        GC.collect()
        super.didReceiveMemoryWarning()
    }

    // The Layers view is a full screen overlay of the current content. To do this, the layers
    // container view should be placed as close as possible to the current UIWindow.
    // If another view controller is presented modaly, UIWindow changes accessibility elements,
    // so the layers view can't be accessed by the iOS accessibility engine.
    // Find the next view in the hierarchy. This view is unique to the root and each modal
    // view controller.
    private fun layersContainerView(): UIView? {
        val window = view.window ?: return null
        var iteratingView = view.superview
        while (iteratingView != null && iteratingView.superview != window) {
            iteratingView = iteratingView.superview
        }
        return iteratingView
    }

    /**
     * Animates the layout transition of root view as well as all layers.
     * The animation consists of the following steps
     * - Before the actual animation starts, all initial parameters should be stored in the
     * corresponding lambdas. See [ComposeSceneMediator.prepareAndGetSizeTransitionAnimation].
     * - At the time of the animation phase, the drawing canvas expands to fit the animated scene
     * throughout the animation cycle. See [ComposeView.animateSizeTransition].
     * - The animation phase consists of changing scene and window sizes frame by frame.
     * See [ComposeSceneMediator.prepareAndGetSizeTransitionAnimation] and
     * [PlatformWindowContext.prepareAndGetSizeTransitionAnimation].
     *
     * Known issue: Because per-frame updates between UIKit and Compose are not synchronised,
     * native views can be misaligned with Compose content during animation.
     *
     * @param transitionCoordinator The coordinator that mediates the transition animations.
     */
    private fun animateSizeTransition(
        transitionCoordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        val duration = transitionCoordinator.transitionDuration.toDuration(DurationUnit.SECONDS)
        if (duration == Duration.ZERO) return

        val displayLinkListener = DisplayLinkListener()
        val sizeTransitionScope = CoroutineScope(
            composeCoroutineContext + displayLinkListener.frameClock
        )
        displayLinkListener.start()

        val animations = mediator?.prepareAndGetSizeTransitionAnimation()
        layers?.animateSizeTransition(sizeTransitionScope, duration)
        rootView.animateSizeTransition(sizeTransitionScope) {
            animations?.invoke(duration)
        }

        transitionCoordinator.animateAlongsideTransition(
            animation = {},
            completion = {
                sizeTransitionScope.cancel()
                displayLinkListener.invalidate()
            }
        )
    }

    private fun crossFadeSizeTransition(
        transitionCoordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        val duration = transitionCoordinator.transitionDuration.toDuration(DurationUnit.SECONDS)
        if (duration == Duration.ZERO) return

        val transitionScope = CoroutineScope(composeCoroutineContext)
        val viewAnimationClosure = rootView.animateCrossFadeTransition(transitionScope)
        val layersAnimationClosure = layers?.animateCrossFadeTransition(transitionScope)

        transitionCoordinator.animateAlongsideTransition(
            animation = {
                viewAnimationClosure()
                layersAnimationClosure?.invoke()
            },
            completion = {
                transitionScope.cancel()
            }
        )
    }

    private fun createComposeSceneContext(
        platformContext: PlatformContext,
        metalView: MetalView
    ): ComposeSceneContext {
        return object : ComposeSceneContext {
            override val platformContext: PlatformContext = platformContext

            override fun createLayer(
                density: Density,
                layoutDirection: LayoutDirection,
                focusable: Boolean,
                compositionContext: CompositionContext
            ): ComposeSceneLayer {
                val layer = UIKitComposeSceneLayer(
                    onClosed = ::detachLayer,
                    createComposeSceneContext = { createComposeSceneContext(it, metalView) },
                    hostCompositionLocals = { ProvideContainerCompositionLocals(it) },
                    metalView = metalView,
                    initDensity = density,
                    initLayoutDirection = layoutDirection,
                    onFocusBehavior = configuration.onFocusBehavior,
                    onAccessibilityChanged = ::onAccessibilityChanged,
                    focusedViewsList = if (focusable) focusedViewsList else null,
                    windowContext = windowContext,
                    compositionContext = compositionContext,
                    coroutineContext = composeCoroutineContext,
                    enableBackGesture = configuration.enableBackGesture,
                    interfaceOrientationState = interfaceOrientationState
                )

                attachLayer(layer)

                return layer
            }
        }
    }

    private fun createComposeScene(
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        metalView: MetalView
    ): ComposeScene = PlatformLayersComposeScene(
        density = view.density,
        layoutDirection = layoutDirection,
        coroutineContext = composeCoroutineContext,
        composeSceneContext = createComposeSceneContext(
            platformContext = platformContext,
            metalView = metalView
        ),
        invalidate = invalidate,
    )

    /**
     * Enables or disables accessibility for each layer, as well as the root mediator, taking into
     * account layer order and ability to overlay underlying content.
     */
    private fun onAccessibilityChanged() {
        var isAccessibilityEnabled = true
        layers?.withLayers {
            it.fastForEachReversed { layer ->
                layer.isAccessibilityEnabled = isAccessibilityEnabled
                isAccessibilityEnabled = isAccessibilityEnabled && !layer.focusable
            }
        }
        mediator?.isAccessibilityEnabled = isAccessibilityEnabled
    }

    private fun attachLayer(layer: UIKitComposeSceneLayer) {
        assertNotNull(layers) { "Attempt to attach layers for disposed scene" }
        layers?.attach(layer, hasViewAppeared)
        onAccessibilityChanged()
    }

    private fun detachLayer(layer: UIKitComposeSceneLayer) {
        assertNotNull(layers) { "Attempt to detach layers for disposed scene" }
        layers?.detach(layer, hasViewAppeared)
        onAccessibilityChanged()
    }

    @Composable
    private fun ProvideContainerCompositionLocals(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalHapticFeedback provides hapticFeedback,
            LocalUIViewController provides this,
            LocalInterfaceOrientation provides interfaceOrientationState.value,
            LocalSystemTheme provides systemThemeState.value,
            LocalLifecycleOwner provides lifecycleOwner,
            LocalInternalViewModelStoreOwner provides lifecycleOwner,
            LocalBackGestureDispatcher provides backGestureDispatcher,
            LocalSaveableStateRegistry provides savableStateRegistry,
            content = content
        )

    private fun ComposeSceneMediator.updateInteractionRect() {
        interactionBounds = with(density) {
            view.bounds.asDpRect().toRect().roundToIntRect()
        }
    }

    private fun updateMotionSpeed() {
        motionDurationScale.scaleFactor = if (UIAccessibilityIsReduceMotionEnabled()) {
            // 0f would cause motion to finish in the next frame callback.
            // See [MotionDurationScale.scaleFactor] for more details.
            0f
        } else {
            1f / (view.window?.layer?.speed?.takeIf { it > 0 } ?: 1f)
        }
    }
}

private fun UIUserInterfaceStyle.asComposeSystemTheme(): SystemTheme {
    return when (this) {
        UIUserInterfaceStyle.UIUserInterfaceStyleLight -> SystemTheme.Light
        UIUserInterfaceStyle.UIUserInterfaceStyleDark -> SystemTheme.Dark
        else -> SystemTheme.Unknown
    }
}

private fun getLayoutDirection() =
    when (UIApplication.sharedApplication().userInterfaceLayoutDirection) {
        UIUserInterfaceLayoutDirection.UIUserInterfaceLayoutDirectionRightToLeft -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }
