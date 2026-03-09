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

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.uikit.addLayoutConstraintsToMatch
import androidx.compose.ui.uikit.embedSubview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dpSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import androidx.compose.ui.window.ComposeContainerView
import androidx.compose.ui.window.DisplayLinkListener
import androidx.compose.ui.window.MetalView
import androidx.compose.ui.window.MetalViewHolder
import kotlin.coroutines.CoroutineContext
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.jetbrains.skia.Canvas
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIEvent
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.beginAppearanceTransition
import platform.UIKit.endAppearanceTransition

/**
 * A class responsible for managing and rendering [UIKitComposeSceneLayer]s.
 */
internal class ComposeLayersViewController(
    useSeparateRenderThreadWhenPossible: Boolean,
    private val coroutineContext: CoroutineContext,
    private val hostingComposeView: ComposeContainerView
): UIViewController(nibName = null, bundle = null) {
    val windowContext = PlatformWindowContext()

    private var hasViewAppeared = false

    val metalView: MetalViewHolder = MetalView(
        retrieveInteropTransaction = ::retrieveAndMergeInteropTransactions,
        useSeparateRenderThreadWhenPossible = useSeparateRenderThreadWhenPossible,
        render = ::render
    ).apply {
        canBeOpaque = false
    }

    enum class AppearanceTransitionState {
        Appearing,
        Appeared,
        Disappearing,
        Disappeared
    }

    private var transitionState: AppearanceTransitionState = AppearanceTransitionState.Disappeared

    private val composeContainerView = ComposeContainerView(
        useOpaqueConfiguration = false,
        transparentForTouches = true
    ).apply {
        updateMetalView(
            metalView = metalView,
            onWillMoveToWindow = { beginAppearanceTransition(it != null, animated = false) },
            onDidMoveToWindow = { endAppearanceTransition() }
        )
    }

    init {
        coroutineContext.job.invokeOnCompletion {
            dispose()
        }
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        val initialSize = view.layer.presentationLayer()?.bounds?.dpSize()
        if (initialSize != null &&
            initialSize != DpSize.Zero &&
            initialSize != view.bounds.dpSize() &&
            !hasInteropViews
        ) {
            animateSizeTransition(initialSize = initialSize)
        }
        composeContainerView.setFrame(view.bounds)
        windowContext.updateWindowContainerSize()
    }

    private var isAnimating = false
    private fun animateSizeTransition(initialSize: DpSize) {
        if (isAnimating || layers.isEmpty()) {
            return
        }
        isAnimating = true

        val displayLinkListener = DisplayLinkListener()
        val job = Job()
        val sizeTransitionScope = CoroutineScope(coroutineContext + displayLinkListener.frameClock + job)
        displayLinkListener.start()

        val animationProgress: suspend ((Float) -> Unit) -> Unit = { onFrame ->
            onFrame(0f)
            while (isAnimating) {
                withFrameNanos {
                    val progress = view.transitionProgress(initialSize)
                    onFrame(progress)
                    if (progress >= 1.0f && isAnimating) {
                        job.cancel()
                        displayLinkListener.invalidate()
                        isAnimating = false
                    }
                }
            }
        }

        val animations = listOf(
            windowContext.prepareAndGetSizeTransitionAnimation(
                initialDpSize = initialSize,
                withProgress = animationProgress
            )
        ) + layers.map {
            it.prepareAndGetSizeTransitionAnimation(animationProgress)
        }

        composeContainerView.animateSizeTransition(sizeTransitionScope) {
            animations.map { animation ->
                sizeTransitionScope.launch { animation.invoke() }
            }.joinAll()
        }
    }

    fun withLayers(block: (List<UIKitComposeSceneLayer>) -> Unit) = layersCache.withCopy(block)

    override fun loadView() {
        this.view = ComposeLayersView()
        this.view.addSubview(composeContainerView)
    }

    val hasInvalidations: Boolean get() = this.layers.any { it.hasInvalidations }

    private val layers = mutableListOf<UIKitComposeSceneLayer>()

    private val layersCache = CopiedList {
        it.addAll(this.layers)
    }

    /**
     * Transactions of the layers that were imperatively removed before their changes were applied.
     */
    private var removedLayersTransactions = mutableListOf<UIKitInteropTransaction>()

    var containerWindow: UIWindow? by windowContext::window

    // The Layers view is a full screen overlay of the current content. To do this, the layers
    // container view should be placed as close as possible to the current UIWindow.
    // If another view controller is presented modally, UIWindow changes accessibility elements,
    // so the layers view can't be accessed by the iOS accessibility engine.
    // Find the next view in the hierarchy. This view is unique to the root and each modal
    // view controller.
    private fun layersContainerView(): UIView? {
        val window = containerWindow ?: return null
        var iteratingView = hostingComposeView.superview
        while (iteratingView != null && iteratingView.superview != window) {
            iteratingView = iteratingView.superview
        }
        return iteratingView
    }

    private fun show() {
        hide()

        val transitionView = layersContainerView() ?: return
        transitionView.embedSubview(view)
        transitionView.layoutIfNeeded()
    }

    private fun hide() {
        view.removeFromSuperview()
    }

    private fun dispose() {
        // `dispose` is called instead of `close`, because `close` is also used imperatively
        // to remove the layer from the array based on user interaction.
        while (this.layers.isNotEmpty()) {
            val layer = this.layers.removeLast()

            if (hasViewAppeared) {
                layer.sceneWillDisappear()
            }

            layer.dispose()
        }

        hide()
        composeContainerView.updateMetalView(metalView = null)
        composeContainerView.removeFromSuperview()
        containerWindow = null
        windowContext.dispose()
    }

    fun attach(layer: UIKitComposeSceneLayer) {
        val isFirstLayer = layers.isEmpty()
        layers.add(layer)
        composeContainerView.insertSubview(layer.interactionView, belowSubview = metalView.view)
        layer.interactionView.addLayoutConstraintsToMatch(composeContainerView)
        composeContainerView.embedSubview(layer.overlayView)

        if (isFirstLayer) {
            show()
        }
        if (hasViewAppeared) {
            layer.sceneDidAppear()
        }
    }

    fun detach(layer: UIKitComposeSceneLayer) {
        if (hasViewAppeared) {
            layer.sceneWillDisappear()
        }

        this.layers.remove(layer)

        // Intercept the actions UIKitInteropTransaction from the layer
        val transaction = layer.retrieveInteropTransaction()

        if (this.layers.isEmpty()) {
            // It was the last layer, remove the view and execute the actions immediately
            hide()

            transaction.actions.fastForEach { it.invoke() }
        } else {
            // It wasn't the last layer, pending transactions should be added to the list
            removedLayersTransactions.add(transaction)

            // Redraw content with layer removed
            metalView.redrawer.setNeedsRedraw()
        }
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)

        hasViewAppeared = true
        this.layers.fastForEach {
            it.sceneDidAppear()
        }
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)

        hasViewAppeared = false
        this.layers.fastForEach {
            it.sceneWillDisappear()
        }
    }

    private val hasInteropViews: Boolean
        get() {
            layersCache.withCopy { layers ->
                layers.fastForEach {
                    if (it.hasInteropViews) {
                        return true
                    }
                }
            }
            return false
        }

    /**
     * Iterate through existing layers and merge their interop transactions to be consumed by the
     * [MetalViewHolder], also including transactions of the layers that were removed and are not
     * present in [layers] anymore.
     */
    private fun retrieveAndMergeInteropTransactions(): UIKitInteropTransaction {
        val removedLayersTransactionsCopy = removedLayersTransactions.toList()
        removedLayersTransactions.clear()

        val transactions = this.layers.map {
            it.retrieveInteropTransaction()
        } + removedLayersTransactionsCopy
        return UIKitInteropTransaction.merge(
            transactions = transactions
        )
    }

    private fun render(canvas: Canvas, nanoTime: Long) {
        val composeCanvas = canvas.asComposeCanvas()

        // Some layers may be removed during rendering, because recomposition will happen in the
        // process, so we need to make a temporary copy of the list
        layersCache.withCopy { layers ->
            layers.fastForEach {
                it.render(composeCanvas, nanoTime)
            }
        }
    }
}

private class ComposeLayersView: UIView(frame = CGRectZero.readValue()) {
    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return super.hitTest(point, withEvent).takeUnless { it == this }
    }
}
