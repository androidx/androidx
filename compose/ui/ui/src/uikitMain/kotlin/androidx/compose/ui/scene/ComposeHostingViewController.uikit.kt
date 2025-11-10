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
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.hapticfeedback.CupertinoHapticFeedback
import androidx.compose.ui.navigationevent.UIKitNavigationEventInput
import androidx.compose.ui.platform.DefaultArchitectureComponentsOwner
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.MotionDurationScaleImpl
import androidx.compose.ui.platform.PlatformArchitectureComponentsOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.uikit.PlistSanityCheck
import androidx.compose.ui.uikit.density
import androidx.compose.ui.uikit.embedSubview
import androidx.compose.ui.uikit.utils.CMPKeyValueObserver
import androidx.compose.ui.uikit.utils.CMPViewController
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.asDpRect
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.viewinterop.UIKitInteropAction
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import androidx.compose.ui.window.ComposeView
import androidx.compose.ui.window.DisplayLinkListener
import androidx.compose.ui.window.FocusedViewsList
import androidx.compose.ui.window.MetalRedrawer
import androidx.compose.ui.window.MetalView
import androidx.compose.ui.window.SceneActiveStateListener
import androidx.compose.ui.window.ViewControllerLifecycleDelegate
import androidx.lifecycle.enableSavedStateHandles
import androidx.savedstate.SavedState
import kotlin.coroutines.CoroutineContext
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGSize
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(BetaInteropApi::class)
@ExportObjCClass
internal class ComposeHostingViewController(
    private val configuration: ComposeUIViewControllerConfiguration,
    private val content: @Composable () -> Unit,
    coroutineContext: CoroutineContext = Dispatchers.Main,
    private val lifecycleDelegate: ViewControllerLifecycleDelegate = ViewControllerLifecycleDelegate()
) : CMPViewController(lifecycleDelegate = lifecycleDelegate) {
    private val hapticFeedback = CupertinoHapticFeedback()

    private val rootView = ComposeView(
        transparentForTouches = false,
        useOpaqueConfiguration = configuration.opaque,
    )

    // Used for testing
    val rootViewRedrawer: MetalRedrawer? get() = rootView.redrawer
    private var mediator: ComposeSceneMediator? = null
    private val windowContext = PlatformWindowContext()
    private var layersHolder: ComposeLayersHolder? = null
    private val layoutDirection get() = getLayoutDirection()
    private val motionDurationScale = MotionDurationScaleImpl()
    private var activeStateListener: SceneActiveStateListener? = null
    private val composeCoroutineContext: CoroutineContext = coroutineContext + motionDurationScale

    private var savedState: SavedState? = null
    private var mediatorComponentsOwner: DefaultArchitectureComponentsOwner? = null
    private val architectureComponentsOwner: DefaultArchitectureComponentsOwner
        get() = mediatorComponentsOwner
            ?: error("ArchitectureComponentsOwner is not initialized yet.")

    private val interfaceOrientationObserver = SceneGeometryObserver {
        updateInterfaceOrientationState()
    }
    private val navigationEventInput = UIKitNavigationEventInput(
        density = rootView.density,
        getTopLeftOffsetInWindow = { IntOffset.Zero }, //full screen
        endEdgePanGestureBehavior = configuration.endEdgePanGestureBehavior
    )

    fun hasInvalidations(): Boolean {
        return mediator?.hasInvalidations == true || layersHolder?.layersViewController?.hasInvalidations == true
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
            return InterfaceOrientation.getByRawValue(
                if (available(OS.Ios to OSVersion(16))) {
                    windowScene?.effectiveGeometry?.interfaceOrientation
                } else {
                    windowScene?.interfaceOrientation
                } ?: UIApplication.sharedApplication.statusBarOrientation
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
        navigationEventInput.onDidMoveToWindow(window, rootView)
        interfaceOrientationObserver.windowScene = window?.windowScene

        window ?: return

        updateInterfaceOrientationState()

        layersHolder?.layersViewController?.referenceWindow = view.window
        windowContext.window = window
        updateMotionSpeed()
        lifecycleDelegate.windowScene = window.windowScene
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

        val duration = withTransitionCoordinator.transitionDuration.toDuration(DurationUnit.SECONDS)
        if (duration == Duration.ZERO) return

        if (mediator?.hasInteropViews == true) {
            // Heavy interop views may slow down the animation of per-frame screen rotation.
            // For these cases, a cross-fade transition will be used instead.
            crossFadeSizeTransition(withTransitionCoordinator)
        } else {
            animateSizeTransition(withTransitionCoordinator, duration)
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

        mediator?.sceneDidAppear()
        configuration.delegate.viewDidAppear(animated)

        // Because the container view can change during the modal transition animation,
        // the gesture handlers and layers view are added back when the animation ends.
        navigationEventInput.onDidMoveToWindow(view.window, rootView)
    }

    @Suppress("DEPRECATION")
    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        mediator?.sceneWillDisappear()
        configuration.delegate.viewWillDisappear(animated)

        navigationEventInput.onDidMoveToWindow(null, rootView)
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
        val holder = ComposeLayersHolder(
            useSeparateRenderThreadWhenPossible = configuration.parallelRendering,
            context = composeCoroutineContext,
            getWindow = { view.window }
        ).also {
            layersHolder = it
        }

        mediatorComponentsOwner = DefaultArchitectureComponentsOwner(savedState)
        architectureComponentsOwner.enableSavedStateHandles()
        lifecycleDelegate.onLifecycleStateUpdated = architectureComponentsOwner::setLifecycleState

        mediator = ComposeSceneMediator(
            onFocusBehavior = configuration.onFocusBehavior,
            focusedViewsList = focusedViewsList,
            windowContext = windowContext,
            architectureComponentsOwner = architectureComponentsOwner,
            coroutineContext = composeCoroutineContext,
            redrawer = metalView.redrawer,
            composeSceneFactory = { invalidate, context ->
                createComposeScene(invalidate, context, holder)
            },
            navigationEventInput = navigationEventInput,
            interfaceOrientationState = interfaceOrientationState,
        ).also { mediator ->
            rootView.embedSubview(mediator.backgroundView)
            rootView.updateMetalView(metalView, ::onDidMoveToWindow)
            rootView.embedSubview(mediator.overlayView)

            mediator.updateInteractionRect()
            mediator.setContent {
                ProvideContainerCompositionLocals(content)
            }
        }

        activeStateListener = SceneActiveStateListener(
            getScene = ::windowScene
        ) { isSceneActive ->
            if (isSceneActive) {
                updateMotionSpeed()
            }
        }

        interfaceOrientationObserver.isObservingEnabled = true

        architectureComponentsOwner.navigationEventDispatcher.addInput(navigationEventInput)
        lifecycleDelegate.windowScene = windowScene
        navigationEventInput.onDidMoveToWindow(view.window, rootView)
        onAccessibilityChanged()
    }

    override fun viewControllerDidLeaveWindowHierarchy() {
        super.viewControllerDidLeaveWindowHierarchy()

        // Store the current state in the local savedState property. It is used to
        // provide the saved state to the next compose scene when the view controller re-enters
        // the window hierarchy.
        savedState = architectureComponentsOwner.saveState()
        lifecycleDelegate.onLifecycleStateUpdated = null

        rootView.updateMetalView(metalView = null)
        navigationEventInput.onDidMoveToWindow(null, rootView)
        architectureComponentsOwner.navigationEventDispatcher.removeInput(navigationEventInput)

        mediator?.dispose()
        mediator = null

        activeStateListener?.dispose()
        activeStateListener = null

        layersHolder?.disposeIfNeeded()
        layersHolder = null

        interfaceOrientationObserver.isObservingEnabled = false

        windowContext.dispose()
    }

    @OptIn(NativeRuntimeApi::class)
    override fun didReceiveMemoryWarning() {
        GC.collect()
        super.didReceiveMemoryWarning()
    }

    /**
     * Animates the layout transition of root view.
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
        transitionCoordinator: UIViewControllerTransitionCoordinatorProtocol,
        duration: Duration
    ) {
        val displayLinkListener = DisplayLinkListener()
        val sizeTransitionScope = CoroutineScope(
            composeCoroutineContext + displayLinkListener.frameClock
        )
        displayLinkListener.start()

        val animations = mediator?.prepareAndGetSizeTransitionAnimation()
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
        val transitionScope = CoroutineScope(composeCoroutineContext)
        val viewAnimationClosure = rootView.animateCrossFadeTransition(transitionScope)

        transitionCoordinator.animateAlongsideTransition(
            animation = {
                viewAnimationClosure()
            },
            completion = {
                transitionScope.cancel()
            }
        )
    }

    private fun createComposeSceneContext(
        platformContext: PlatformContext,
        layersHolder: ComposeLayersHolder
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
                    onClosed = {
                        layersHolder.getLayersViewController().detach(it)
                        onAccessibilityChanged()
                    },
                    createComposeSceneContext = { createComposeSceneContext(it, layersHolder) },
                    hostCompositionLocals = { ProvideContainerCompositionLocals(it) },
                    layersViewController = layersHolder.getLayersViewController(),
                    initialLayoutDirection = layoutDirection,
                    onFocusBehavior = configuration.onFocusBehavior,
                    endEdgeGestureBehavior = configuration.endEdgePanGestureBehavior,
                    onAccessibilityChanged = ::onAccessibilityChanged,
                    focusedViewsList = if (focusable) focusedViewsList?.childFocusedViewsList() else null,
                    compositionContext = compositionContext,
                    ownerProvider = architectureComponentsOwner,
                    coroutineContext = composeCoroutineContext,
                    interfaceOrientationState = interfaceOrientationState,
                )

                layersHolder.getLayersViewController().attach(layer)
                onAccessibilityChanged()

                return layer
            }
        }
    }

    private fun createComposeScene(
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        layersHolder: ComposeLayersHolder
    ): ComposeScene = PlatformLayersComposeScene(
        density = view.density,
        layoutDirection = layoutDirection,
        coroutineContext = composeCoroutineContext,
        composeSceneContext = createComposeSceneContext(
            platformContext = platformContext,
            layersHolder = layersHolder
        ),
        invalidate = invalidate,
    )

    /**
     * Enables or disables accessibility for each layer, as well as the root mediator, taking into
     * account layer order and ability to overlay underlying content.
     */
    private fun onAccessibilityChanged() {
        var isAccessibilityEnabled = true
        layersHolder?.layersViewController?.withLayers {
            it.fastForEachReversed { layer ->
                layer.isAccessibilityEnabled = isAccessibilityEnabled
                isAccessibilityEnabled = isAccessibilityEnabled && !layer.focusable
            }
        }
        mediator?.isAccessibilityEnabled = isAccessibilityEnabled
    }

    @Composable
    private fun ProvideContainerCompositionLocals(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalHapticFeedback provides hapticFeedback,
            LocalUIViewController provides this,
            LocalSystemTheme provides systemThemeState.value,
            content = content
        )

    private fun ComposeSceneMediator.updateInteractionRect() {
        interactionBounds = with(view.density) {
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

    private val windowScene: UIWindowScene?
        get() = view.window?.windowScene
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

private class ComposeLayersHolder(
    private val useSeparateRenderThreadWhenPossible: Boolean,
    private val context: CoroutineContext,
    private val getWindow: () -> UIWindow?
) {
    var layersViewController: ComposeLayersViewController? = null
        private set

    fun getLayersViewController(): ComposeLayersViewController {
        return layersViewController ?: run {
            val layers = ComposeLayersViewController(
                useSeparateRenderThreadWhenPossible = useSeparateRenderThreadWhenPossible,
                context = context
            )
            layers.referenceWindow = getWindow()
            layersViewController = layers
            layers
        }
    }

    fun disposeIfNeeded() {
        layersViewController?.dispose()
        layersViewController = null
    }
}

private class SceneGeometryObserver(
    val onGeometryChanged: () -> Unit
) : CMPKeyValueObserver() {
    private val observingKey = "effectiveGeometry"

    var windowScene: UIWindowScene? = null
        set(value) {
            if (field == value) return
            removeObserverIfNeeded()
            field = value
            addObserverIfNeeded()
        }

    var isObservingEnabled = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                addObserverIfNeeded()
            } else {
                removeObserverIfNeeded()
            }
        }

    private var isObservingAdded = false

    private fun addObserverIfNeeded() {
        if (isObservingEnabled && !isObservingAdded) {
            isObservingAdded = true
            windowScene?.addObserver(this, observingKey, NSKeyValueObservingOptionNew, null)
        }
    }

    private fun removeObserverIfNeeded() {
        windowScene?.removeObserver(this, observingKey)
        isObservingAdded = false
    }

    override fun observeValueForKeyPath(
        keyPath: String?,
        ofObject: Any?,
        change: Map<Any?, *>?,
        context: CPointer<out CPointed>?
    ) {
        onGeometryChanged()
    }
}

