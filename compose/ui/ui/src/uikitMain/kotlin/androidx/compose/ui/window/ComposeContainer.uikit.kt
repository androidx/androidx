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
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.hapticfeedback.CupertinoHapticFeedback
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInternalViewModelStoreOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.ComposeSceneMediator
import androidx.compose.ui.scene.PlatformLayersComposeScene
import androidx.compose.ui.scene.SceneLayout
import androidx.compose.ui.scene.UIViewComposeSceneLayer
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.LocalInterfaceOrientation
import androidx.compose.ui.uikit.PlistSanityCheck
import androidx.compose.ui.uikit.systemDensity
import androidx.compose.ui.uikit.utils.CMPViewController
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.UIKitInteropContainer
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.SkikoRenderDelegate
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeEqualToSize
import platform.Foundation.NSStringFromClass
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityMedium
import platform.UIKit.UIContentSizeCategoryExtraExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraSmall
import platform.UIKit.UIContentSizeCategoryLarge
import platform.UIKit.UIContentSizeCategoryMedium
import platform.UIKit.UIContentSizeCategorySmall
import platform.UIKit.UIDevice
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceIdiomPhone
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

// TODO: Move to androidx.compose.ui.scene
@OptIn(BetaInteropApi::class)
@ExportObjCClass
internal class ComposeContainer(
    private val configuration: ComposeUIViewControllerConfiguration,
    private val content: @Composable () -> Unit,
    private val coroutineContext: CoroutineContext = Dispatchers.Main
) : CMPViewController(nibName = null, bundle = null) {
    // TODO: Rename and make private
    val lifecycleOwner = ViewControllerBasedLifecycleOwner()
    val hapticFeedback = CupertinoHapticFeedback()

    private var isInsideSwiftUI = false
    private var mediator: ComposeSceneMediator? = null
    private val layers: MutableList<UIViewComposeSceneLayer> = mutableListOf()
    private val layoutDirection get() = getLayoutDirection()
    private var isViewAppeared: Boolean = false

    fun hasInvalidations(): Boolean {
        return mediator?.hasInvalidations() == true || layers.any { it.hasInvalidations() }
    }

    @OptIn(ExperimentalComposeApi::class)
    private val windowContainer: UIView
        get() = if (configuration.platformLayers) {
            view.window ?: view
        } else view

    /*
     * Initial value is arbitrarily chosen to avoid propagating invalid value logic
     * It's never the case in real usage scenario to reflect that in type system
     */
    val interfaceOrientationState: MutableState<InterfaceOrientation> = mutableStateOf(
        InterfaceOrientation.Portrait
    )
    val systemThemeState: MutableState<SystemTheme> = mutableStateOf(SystemTheme.Unknown)
    private val focusStack: FocusStack<UIView> = FocusStackImpl()
    private val windowContext = PlatformWindowContext().apply {
        setWindowFocused(true)
    }

    /*
     * On iOS >= 13.0 interfaceOrientation will be deduced from [UIWindowScene] of [UIWindow]
     * to which our [RootUIViewController] is attached.
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

    override fun preferredStatusBarStyle(): UIStatusBarStyle =
        configuration.delegate.preferredStatusBarStyle
            ?: super.preferredStatusBarStyle()

    override fun preferredStatusBarUpdateAnimation(): UIStatusBarAnimation =
        configuration.delegate.preferredStatysBarAnimation
            ?: super.preferredStatusBarUpdateAnimation()

    override fun prefersStatusBarHidden(): Boolean =
        configuration.delegate.prefersStatusBarHidden
            ?: super.prefersStatusBarHidden()

    override fun viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()

        mediator?.viewSafeAreaInsetsDidChange()
        layers.fastForEach {
            it.viewSafeAreaInsetsDidChange()
        }
    }

    @OptIn(ExperimentalComposeApi::class)
    override fun loadView() {
        view = UIView().apply {
            setClipsToBounds(true)
            opaque = configuration.opaque
            backgroundColor = if (configuration.opaque) UIColor.whiteColor else UIColor.clearColor
        } // rootView needs to interop with UIKit
    }

    override fun viewDidLoad() {
        super.viewDidLoad()

        if (configuration.enforceStrictPlistSanityCheck) {
            PlistSanityCheck.performIfNeeded()
        }

        configuration.delegate.viewDidLoad()
        systemThemeState.value = traitCollection.userInterfaceStyle.asComposeSystemTheme()
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        systemThemeState.value = traitCollection.userInterfaceStyle.asComposeSystemTheme()
    }

    override fun viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()

        // UIKit possesses all required info for layout at this point
        currentInterfaceOrientation?.let {
            interfaceOrientationState.value = it
        }

        updateWindowContainer()
        mediator?.viewWillLayoutSubviews()
        layers.fastForEach {
            it.viewWillLayoutSubviews()
        }
    }

    private fun updateWindowContainer() {
        val scale = windowContainer.systemDensity.density
        val size = windowContainer.frame.useContents<CGRect, IntSize> {
            IntSize(
                width = (size.width * scale).roundToInt(),
                height = (size.height * scale).roundToInt()
            )
        }
        windowContext.setContainerSize(size)
        windowContext.setWindowContainer(windowContainer)
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

        // Happens during orientation change from LandscapeLeft to LandscapeRight, for example
        val isSameSizeTransition = view.frame.useContents {
            CGSizeEqualToSize(size, this.size.readValue())
        }
        if (isSameSizeTransition) {
            return
        }

        mediator?.viewWillTransitionToSize(
            targetSize = size,
            coordinator = withTransitionCoordinator,
        )
        layers.fastForEach {
            it.viewWillTransitionToSize(
                targetSize = size,
                coordinator = withTransitionCoordinator,
            )
        }
        view.layoutIfNeeded()
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)

        isInsideSwiftUI = checkIfInsideSwiftUI()
        createMediatorIfNeeded()

        lifecycleOwner.handleViewWillAppear()
        configuration.delegate.viewWillAppear(animated)
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        isViewAppeared = true
        mediator?.sceneDidAppear()
        layers.fastForEach {
            it.sceneDidAppear()
        }
        updateWindowContainer()
        configuration.delegate.viewDidAppear(animated)
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        isViewAppeared = false
        mediator?.sceneWillDisappear()
        layers.fastForEach {
            it.sceneWillDisappear()
        }
        configuration.delegate.viewWillDisappear(animated)
    }

    @OptIn(NativeRuntimeApi::class)
    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)

        dispatch_async(dispatch_get_main_queue()) {
            GC.collect()
        }

        lifecycleOwner.handleViewDidDisappear()
        configuration.delegate.viewDidDisappear(animated)
    }

    override fun viewControllerDidLeaveWindowHierarchy() {
        super.viewControllerDidLeaveWindowHierarchy()
        dispose()
    }

    @OptIn(NativeRuntimeApi::class)
    override fun didReceiveMemoryWarning() {
        println("didReceiveMemoryWarning")
        GC.collect()
        super.didReceiveMemoryWarning()
    }

    fun createComposeSceneContext(platformContext: PlatformContext): ComposeSceneContext =
        ComposeSceneContextImpl(platformContext)

    @OptIn(ExperimentalComposeApi::class)
    private fun createSkikoUIView(
        interopContainer: UIKitInteropContainer,
        renderRelegate: SkikoRenderDelegate
    ): RenderingUIView =
        RenderingUIView(
            renderDelegate = renderRelegate,
            retrieveInteropTransaction = {
                interopContainer.retrieveTransaction()
            }
        ).apply {
            opaque = configuration.opaque
        }

    @OptIn(ExperimentalComposeApi::class)
    private fun createComposeScene(
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext,
    ): ComposeScene = if (configuration.platformLayers) {
        PlatformLayersComposeScene(
            density = systemDensity,
            layoutDirection = layoutDirection,
            coroutineContext = coroutineContext,
            composeSceneContext = ComposeSceneContextImpl(
                platformContext = platformContext
            ),
            invalidate = invalidate,
        )
    } else {
        CanvasLayersComposeScene(
            density = systemDensity,
            layoutDirection = layoutDirection,
            coroutineContext = coroutineContext,
            composeSceneContext = ComposeSceneContextImpl(
                platformContext = platformContext
            ),
            invalidate = invalidate,
        )
    }

    private fun createMediatorIfNeeded() {
        if (mediator == null) {
            mediator = createMediator()
        }
    }

    private fun createMediator(): ComposeSceneMediator {
        val mediator = ComposeSceneMediator(
            container = view,
            configuration = configuration,
            focusStack = focusStack,
            windowContext = windowContext,
            coroutineContext = coroutineContext,
            renderingUIViewFactory = ::createSkikoUIView,
            composeSceneFactory = ::createComposeScene,
        )
        mediator.setContent {
            ProvideContainerCompositionLocals(this, content)
        }
        mediator.setLayout(SceneLayout.UseConstraintsToFillContainer)
        return mediator
    }

    private fun dispose() {
        lifecycleOwner.dispose()
        mediator?.dispose()
        mediator = null
        layers.fastForEach {
            it.close()
        }
    }

    fun attachLayer(layer: UIViewComposeSceneLayer) {
        layers.add(layer)
        if (isViewAppeared) {
            layer.sceneDidAppear()
        }
    }

    fun detachLayer(layer: UIViewComposeSceneLayer) {
        if (isViewAppeared) {
            layer.sceneWillDisappear()
        }
        layers.remove(layer)
    }

    private inner class ComposeSceneContextImpl(
        override val platformContext: PlatformContext,
    ) : ComposeSceneContext {
        override fun createPlatformLayer(
            density: Density,
            layoutDirection: LayoutDirection,
            focusable: Boolean,
            compositionContext: CompositionContext
        ): ComposeSceneLayer =
            UIViewComposeSceneLayer(
                composeContainer = this@ComposeContainer,
                initDensity = density,
                initLayoutDirection = layoutDirection,
                configuration = configuration,
                focusStack = if (focusable) focusStack else null,
                windowContext = windowContext,
                compositionContext = compositionContext,
            )
    }
}

@OptIn(BetaInteropApi::class)
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

private fun getLayoutDirection() =
    when (UIApplication.sharedApplication().userInterfaceLayoutDirection) {
        UIUserInterfaceLayoutDirection.UIUserInterfaceLayoutDirectionRightToLeft -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }

@Composable
internal fun ProvideContainerCompositionLocals(
    composeContainer: ComposeContainer,
    content: @Composable () -> Unit,
) = with(composeContainer) {
    CompositionLocalProvider(
        LocalHapticFeedback provides hapticFeedback,
        LocalUIViewController provides this,
        LocalInterfaceOrientation provides interfaceOrientationState.value,
        LocalSystemTheme provides systemThemeState.value,
        LocalLifecycleOwner provides lifecycleOwner,
        LocalInternalViewModelStoreOwner provides lifecycleOwner,
        content = content
    )
}

internal val uiContentSizeCategoryToFontScaleMap = mapOf(
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
