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
import androidx.compose.ui.animation.withAnimationProgress
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.utils.CMPViewController
import androidx.compose.ui.window.ComposeContainerLifecycleDelegate
import androidx.compose.ui.window.ComposeContainerView
import androidx.compose.ui.window.DisplayLinkListener
import androidx.compose.ui.window.MetalRedrawer
import kotlin.coroutines.CoroutineContext
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import platform.CoreGraphics.CGSize
import platform.UIKit.UIStatusBarAnimation
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol

@OptIn(BetaInteropApi::class)
@ExportObjCClass
internal class ComposeHostingViewController(
    private val configuration: ComposeUIViewControllerConfiguration,
    private val content: @Composable () -> Unit,
    coroutineContext: CoroutineContext = Dispatchers.Main,
    private val lifecycleDelegate: ComposeContainerLifecycleDelegate = ComposeContainerLifecycleDelegate()
) : CMPViewController(lifecycleDelegate = lifecycleDelegate) {
    private val container = ComposeContainer(
        configuration = configuration,
        content = content,
        coroutineContext = coroutineContext,
        lifecycleDelegate = lifecycleDelegate
    )

    // Used for testing
    val rootRedrawer: MetalRedrawer? get() = container.view.redrawer
    fun hasInvalidations(): Boolean = container.hasInvalidations()

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
        view = container.view
    }

    @Suppress("DEPRECATION")
    override fun viewDidLoad() {
        super.viewDidLoad()

        configuration.delegate.viewDidLoad()
        container.userInterfaceStyleDidChange()
    }

    override fun userInterfaceStyleDidChange() {
        container.userInterfaceStyleDidChange()
    }

    override fun viewWillTransitionToSize(
        size: CValue<CGSize>,
        withTransitionCoordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        super.viewWillTransitionToSize(size, withTransitionCoordinator)

        container.updateInterfaceOrientationState()

        val duration = withTransitionCoordinator.transitionDuration.toDuration(DurationUnit.SECONDS)
        if (duration == Duration.ZERO) return

        if (container.hasInteropViews) {
            // Heavy interop views may slow down the animation of per-frame screen rotation.
            // For these cases, a cross-fade transition will be used instead.
            animateCrossFadeSizeTransition(withTransitionCoordinator)
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

        container.sceneDidAppear()

        configuration.delegate.viewDidAppear(animated)
    }

    @Suppress("DEPRECATION")
    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)

        container.sceneWillDisappear()

        configuration.delegate.viewWillDisappear(animated)
    }

    @Suppress("DEPRECATION")
    @OptIn(NativeRuntimeApi::class)
    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)

        configuration.delegate.viewDidDisappear(animated)
    }

    override fun viewControllerDidEnterWindowHierarchy() {
        container.initializeComposeScene()
    }

    override fun viewControllerDidLeaveWindowHierarchy() {
        container.disposeComposeScene()
    }

    /**
     * Animates the layout transition of root view. The animation consists of the following steps:
     * - Before the actual animation starts, all initial parameters should be stored in the
     * corresponding lambdas. See [ComposeSceneMediator.prepareAndGetSizeTransitionAnimation].
     * - At the time of the animation phase, the drawing canvas expands to fit the animated scene
     * throughout the animation cycle. See [ComposeContainerView.animateSizeTransition].
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
            container.composeCoroutineContext + displayLinkListener.frameClock
        )
        displayLinkListener.start()

        val animations = container.prepareAndGetSizeTransitionAnimation { onFrame ->
            withAnimationProgress(duration, update = onFrame)
        }
        container.view.animateSizeTransition(sizeTransitionScope) {
            animations()
        }

        transitionCoordinator.animateAlongsideTransition(
            animation = {},
            completion = {
                sizeTransitionScope.cancel()
                displayLinkListener.invalidate()
            }
        )
    }

    private fun animateCrossFadeSizeTransition(
        transitionCoordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        val transitionScope = CoroutineScope(container.composeCoroutineContext)
        val viewAnimationClosure = container.view.animateCrossFadeTransition(transitionScope)

        transitionCoordinator.animateAlongsideTransition(
            animation = {
                viewAnimationClosure()
            },
            completion = {
                transitionScope.cancel()
            }
        )
    }
}
