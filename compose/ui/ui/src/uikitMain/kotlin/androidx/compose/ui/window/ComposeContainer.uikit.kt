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
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.interop.LocalLayerContainer
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.LocalInterfaceOrientation
import androidx.compose.ui.uikit.PlistSanityCheck
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import kotlin.math.roundToInt
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeEqualToSize
import platform.Foundation.NSStringFromClass
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private val coroutineDispatcher = Dispatchers.Main

@OptIn(InternalComposeApi::class)
@ExportObjCClass
internal class ComposeContainer(
    private val configuration: ComposeUIViewControllerConfiguration,
    private val content: @Composable () -> Unit,
) : UIViewController(nibName = null, bundle = null) {

    private var isInsideSwiftUI = false
    private var composeSceneMediator: ComposeSceneMediator? = null
    private val layers: MutableList<UIViewComposeSceneLayer> = mutableListOf()
    private val layoutDirection get() = getLayoutDirection()

    /*
     * Initial value is arbitrarily chosen to avoid propagating invalid value logic
     * It's never the case in real usage scenario to reflect that in type system
     */
    val interfaceOrientationState: MutableState<InterfaceOrientation> = mutableStateOf(
        InterfaceOrientation.Portrait
    )
    val systemThemeState: MutableState<SystemTheme> = mutableStateOf(SystemTheme.Unknown)
    private val focusStack: FocusStack<UIView> = FocusStackImpl()
    private val windowInfo = WindowInfoImpl().also {
        it.isWindowFocused = true
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

    @Suppress("unused")
    @ObjCAction
    fun viewSafeAreaInsetsDidChange() {
        // super.viewSafeAreaInsetsDidChange() // TODO: call super after Kotlin 1.8.20
        composeSceneMediator?.viewSafeAreaInsetsDidChange()
        layers.fastForEach {
            it.viewSafeAreaInsetsDidChange()
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

        val window = checkNotNull(view.window) {
            "ComposeUIViewController.view should be attached to window"
        }
        val scale = window.screen.scale
        val size = window.frame.useContents<CGRect, IntSize> {
            IntSize(
                width = (size.width * scale).roundToInt(),
                height = (size.height * scale).roundToInt()
            )
        }
        windowInfo.containerSize = size
        composeSceneMediator?.viewWillLayoutSubviews()
        layers.fastForEach {
            it.viewWillLayoutSubviews()
        }
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

        composeSceneMediator?.viewWillTransitionToSize(
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
        setContent(content)
        configuration.delegate.viewWillAppear(animated)
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        composeSceneMediator?.viewDidAppear(animated)
        layers.fastForEach {
            it.viewDidAppear(animated)
        }
        configuration.delegate.viewDidAppear(animated)
    }

    // viewDidUnload() is deprecated and not called.
    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        composeSceneMediator?.viewWillDisappear(animated)
        layers.fastForEach {
            it.viewWillDisappear(animated)
        }
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

    @OptIn(ExperimentalComposeApi::class)
    private fun setContent(content: @Composable () -> Unit) {
        if (composeSceneMediator != null) {
            return // already attached
        }

        val mediator = if (configuration.platformLayers) {
            createSingleLayerComposeSceneMediator()
        } else {
            createMultiLayerComposeSceneMediator()
        }
        composeSceneMediator = mediator
        mediator.setContent {
            ProvideContainerCompositionLocals(this) {
                content()
            }
        }
        mediator.setLayout(SceneLayout.UseConstraintsToFillContainer)
    }

    private fun dispose() {
        composeSceneMediator?.dispose()
        composeSceneMediator = null
        layers.fastForEach {
            it.close()
        }

    }

    private fun createSingleLayerComposeSceneMediator(): ComposeSceneMediator =
        ComposeSceneMediator(
            viewController = this,
            configuration = configuration,
            focusStack = focusStack,
            windowInfo = windowInfo,
            transparency = false,
            coroutineContext = coroutineDispatcher,
        ) { mediator: ComposeSceneMediator ->
            SingleLayerComposeScene(
                coroutineContext = coroutineDispatcher,
                density = mediator.densityProvider(),
                invalidate = mediator::onComposeSceneInvalidate,
                layoutDirection = layoutDirection,
                composeSceneContext = ComposeSceneContextImpl(
                    platformContext = mediator.platformContext
                ),
            )
        }

    private fun createMultiLayerComposeSceneMediator(): ComposeSceneMediator =
        ComposeSceneMediator(
            viewController = this,
            configuration = configuration,
            focusStack = focusStack,
            windowInfo = windowInfo,
            transparency = false,
            coroutineContext = coroutineDispatcher,
        ) { mediator ->
            MultiLayerComposeScene(
                coroutineContext = coroutineDispatcher,
                composeSceneContext = ComposeSceneContextImpl(
                    platformContext = mediator.platformContext
                ),
                density = mediator.densityProvider(),
                invalidate = mediator::onComposeSceneInvalidate,
                layoutDirection = layoutDirection,
            )
        }

    fun attachLayer(layer: UIViewComposeSceneLayer) {
        layers.add(layer)
    }

    fun detachLayer(layer: UIViewComposeSceneLayer) {
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
                density = density,
                layoutDirection = layoutDirection,
                configuration = configuration,
                focusStack = if (focusable) focusStack else null,
                windowInfo = windowInfo,
                compositionContext = compositionContext,
                compositionLocalContext = composeSceneMediator?.compositionLocalContext,
                composeSceneContext = this,
            )
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

private fun getLayoutDirection() =
    when (UIApplication.sharedApplication().userInterfaceLayoutDirection) {
        UIUserInterfaceLayoutDirection.UIUserInterfaceLayoutDirectionRightToLeft -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }

@OptIn(InternalComposeApi::class)
@Composable
internal fun ProvideContainerCompositionLocals(
    composeContainer: ComposeContainer,
    content: @Composable () -> Unit,
) = with(composeContainer) {
    CompositionLocalProvider(
        LocalUIViewController provides this,
        LocalLayerContainer provides view,
        LocalInterfaceOrientation provides interfaceOrientationState.value,
        LocalSystemTheme provides systemThemeState.value,
        content = content
    )
}
