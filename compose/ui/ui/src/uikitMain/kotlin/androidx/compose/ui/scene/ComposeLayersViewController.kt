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

import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.uikit.addLayoutConstraintsToMatch
import androidx.compose.ui.uikit.embedSubview
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.UIKitInteropTransaction
import androidx.compose.ui.window.ComposeView
import androidx.compose.ui.window.DisplayLinkListener
import androidx.compose.ui.window.MetalView
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.cinterop.CValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.jetbrains.skia.Canvas
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGSize
import platform.UIKit.UIEvent
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowLevelAlert
import platform.UIKit.UIWindowLevelNormal

/**
 * A class responsible for managing and rendering [UIKitComposeSceneLayer]s.
 */
internal class ComposeLayersViewController(
    useSeparateRenderThreadWhenPossible: Boolean,
    private val context: CoroutineContext
): UIViewController(nibName = null, bundle = null) {
    val windowContext = PlatformWindowContext()

    private val window = LayersWindow()
    private var hasViewAppeared = false

    val metalView: MetalView = MetalView(
        retrieveInteropTransaction = ::retrieveAndMergeInteropTransactions,
        useSeparateRenderThreadWhenPossible = useSeparateRenderThreadWhenPossible,
        render = ::render
    ).apply {
        canBeOpaque = false
    }

    private val rootView = ComposeView(
        useOpaqueConfiguration = false,
        transparentForTouches = true
    ).also {
        it.updateMetalView(
            metalView = metalView,
            onLayoutSubviews = { windowContext.updateWindowContainerSize() }
        )
    }

    fun withLayers(block: (List<UIKitComposeSceneLayer>) -> Unit) = layersCache.withCopy(block)

    override fun loadView() {
        this.view = rootView
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

    var referenceWindow: UIWindow? = null
        set(value) {
            field = value
            window.windowScene = value?.windowScene
            window.windowLevel = value?.windowLevel ?: UIWindowLevelNormal
        }

    private fun show() {
        windowContext.window = window
        window.rootViewController = this
        window.windowLevel = UIWindowLevelAlert + 1
        window.setHidden(false)
    }

    private fun hide() {
        window.rootViewController = null
        window.setHidden(true)
    }

    fun dispose() {
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
        rootView.updateMetalView(metalView = null)
        referenceWindow = null
        windowContext.dispose()
    }

    fun attach(layer: UIKitComposeSceneLayer) {
        val isFirstLayer = layers.isEmpty()
        layers.add(layer)
        view.insertSubview(layer.interactionView, belowSubview = metalView)
        layer.interactionView.addLayoutConstraintsToMatch(view)
        view.embedSubview(layer.overlayView)

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
            // It was the last layer, remove the view and executed the actions immediately
            hide()

            transaction.actions.fastForEach { it.invoke() }

            // If the layers list is empty, the draw call will clear the canvas.
            metalView.redrawer.draw(waitUntilCompletion = false)
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
     * [MetalView], also include transactions of the layers that were removed and are not
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

    override fun viewWillTransitionToSize(
        size: CValue<CGSize>,
        withTransitionCoordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        super.viewWillTransitionToSize(size, withTransitionCoordinator)

        if (layers.isEmpty()) {
            return
        }

        val duration = withTransitionCoordinator.transitionDuration.toDuration(DurationUnit.SECONDS)
        if (duration == Duration.ZERO) return

        if (hasInteropViews) {
            // Heavy interop views may slow down the animation of per-frame screen rotation.
            // For these cases, a cross-fade transition will be used instead.
            crossFadeSizeTransition(withTransitionCoordinator)
        } else {
            animateSizeTransition(withTransitionCoordinator, duration)
        }
    }

    /**
     * Animates the layout transition of layers.
     * See [androidx.compose.ui.scene.ComposeHostingViewController.animateSizeTransition]
     */
    private fun animateSizeTransition(
        transitionCoordinator: UIViewControllerTransitionCoordinatorProtocol,
        duration: Duration,
    ) {
        val displayLinkListener = DisplayLinkListener()
        val sizeTransitionScope = CoroutineScope(context + displayLinkListener.frameClock)
        displayLinkListener.start()

        animateSizeTransition(sizeTransitionScope, duration)

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
        val transitionScope = CoroutineScope(context)
        val layersAnimationClosure = rootView.animateCrossFadeTransition(transitionScope)

        transitionCoordinator.animateAlongsideTransition(
            animation = {
                layersAnimationClosure.invoke()
            },
            completion = {
                transitionScope.cancel()
            }
        )
    }

    fun animateSizeTransition(scope: CoroutineScope, duration: Duration) {
        if (this.layers.isEmpty()) {
            return
        }
        val animations = listOf(
            windowContext.prepareAndGetSizeTransitionAnimation()
        ) + this.layers.map {
            it.prepareAndGetSizeTransitionAnimation()
        }

        rootView.animateSizeTransition(scope) {
            animations.map {
                scope.launch { it.invoke(duration) }
            }.joinAll()
        }
    }
}

private class LayersWindow: UIWindow(frame = UIScreen.mainScreen.bounds) {
    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        // Hit-testing only the Compose view or any view that located on top of it.
        for (subview in subviews.reversed()) {
            subview as UIView
            val isDescendantOfComposeView = rootViewController?.view?.isDescendantOfView(subview)
            if (isDescendantOfComposeView == true) {
                return rootViewController?.view?.hitTest(point, withEvent)
            } else {
                subview.hitTest(point, withEvent)?.let {
                    return it
                }
            }
        }
        return null
    }
}
