/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.uikit.ComposeUIViewConfiguration
import androidx.compose.ui.uikit.utils.CMPView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dpSize
import androidx.compose.ui.window.ComposeContainerLifecycleDelegate
import androidx.compose.ui.window.DisplayLinkListener
import androidx.compose.ui.window.MetalRedrawer
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGSize
import platform.UIKit.UIView

@OptIn(BetaInteropApi::class)
@ExportObjCClass
internal class ComposeHostingView(
    private val configuration: ComposeUIViewConfiguration,
    private val content: @Composable () -> Unit,
    coroutineContext: CoroutineContext = Dispatchers.Main,
    private val lifecycleDelegate: ComposeContainerLifecycleDelegate = ComposeContainerLifecycleDelegate()
) : CMPView(lifecycleDelegate = lifecycleDelegate) {
    private val container = ComposeContainer(
        configuration = configuration,
        content = content,
        coroutineContext = coroutineContext,
        lifecycleDelegate = lifecycleDelegate
    )

    // Used for testing
    val rootRedrawer: MetalRedrawer? get() = container.view.redrawer
    fun hasInvalidations(): Boolean = container.hasInvalidations()

    init {
        addSubview(container.view)
        clipsToBounds = true
        opaque = configuration.opaque
    }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        return container.view.sizeThatFits(size)
    }

    override fun intrinsicContentSize(): CValue<CGSize> {
        return container.view.intrinsicContentSize
    }

    override fun layoutSubviews() {
        super.layoutSubviews()

        container.updateInterfaceOrientationState()

        val initialSize = layer.presentationLayer()?.bounds?.dpSize()
        if (initialSize == null ||
            initialSize == bounds.dpSize() ||
            container.hasInteropViews) {
            container.view.setFrame(bounds)
            return
        }

        val scope = container.nestedCoroutineScope()
        if (!scope.isActive) {
            container.view.setFrame(bounds)
            return
        }

        scope.launch {
            // The only way to determine whether the animation is running is to check the bounds of
            // the presentation layer and compare them with the current bounds.
            // The presence of interop views can significantly slow down the process of transition
            // animations.
            // Unlike the transition in UIViewController, the exact duration of the animation is
            // unknown in this case here.
            val actualSize = layer.presentationLayer()?.bounds?.dpSize()
            if (actualSize != null && actualSize != bounds.dpSize() && !container.hasInteropViews) {
                animateSizeTransition(initialSize = initialSize)
            } else {
                container.view.setFrame(bounds)
            }
        }
    }

    override fun viewDidAppear() {
        container.sceneDidAppear()
    }

    override fun viewDidDisappear() {
        container.sceneWillDisappear()
    }

    override fun userInterfaceStyleDidChange() {
        container.updateUserInterfaceStyle(traitCollection.userInterfaceStyle)
    }

    override fun viewDidEnterWindowHierarchy() {
        container.updateUserInterfaceStyle(traitCollection.userInterfaceStyle)

        container.initializeComposeScene()
    }

    override fun viewDidLeaveWindowHierarchy() {
        container.disposeComposeScene()
    }

    private var isAnimating = false
    private fun animateSizeTransition(initialSize: DpSize) {
        if (isAnimating) {
            container.view.setFrame(bounds)
            return
        }
        isAnimating = true

        val displayLinkListener = DisplayLinkListener()
        val sizeTransitionScope = container.nestedCoroutineScope(displayLinkListener.frameClock)
        displayLinkListener.start()

        val animations = container.prepareAndGetSizeTransitionAnimation { onFrame ->
            onFrame(0.0f)
            while (isAnimating) {
                withFrameNanos {
                    val progress = transitionProgress(initialSize)
                    onFrame(progress)
                    if (progress >= 1.0f) {
                        container.view.clipsToBounds = true
                        isAnimating = false
                        sizeTransitionScope.cancel()
                    }
                }
            }
        }
        container.view.animateSizeTransition(sizeTransitionScope) {
            animations()
        }
        container.view.clipsToBounds = false
        container.view.setFrame(bounds)
    }
}

internal fun UIView.transitionProgress(initialSize: DpSize): Float {
    var progress: Float
    val targetSize = bounds.dpSize()
    val currentSize = layer.presentationLayer()?.bounds?.dpSize() ?: return 1.0f
    progress = when {
        targetSize.width != initialSize.width ->
            abs(initialSize.width.value - currentSize.width.value) /
                abs(targetSize.width.value - initialSize.width.value)

        targetSize.height != initialSize.height ->
            abs(initialSize.height.value - currentSize.height.value) /
                abs(targetSize.height.value - initialSize.height.value)

        else -> 1f
    }
    return progress.coerceIn(0.0f, 1.0f)
}