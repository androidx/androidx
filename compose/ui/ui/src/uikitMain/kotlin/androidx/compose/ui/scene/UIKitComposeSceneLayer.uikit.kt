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
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.navigationevent.UIKitNavigationEventInput
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformArchitectureComponentsOwner
import androidx.compose.ui.uikit.EndEdgePanGestureBehavior
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.uikit.density
import androidx.compose.ui.uikit.embedSubview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.asDpRect
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.window.FocusedViewsList
import androidx.navigationevent.NavigationEventDispatcher
import kotlin.coroutines.CoroutineContext
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGPoint
import platform.UIKit.UIView
import platform.UIKit.UIWindow

internal class UIKitComposeSceneLayer(
    private val onClosed: (UIKitComposeSceneLayer) -> Unit,
    private val createComposeSceneContext: (PlatformContext) -> ComposeSceneContext,

    // FIXME: Remove it. All locals should be available from the composition context
    private val hostCompositionLocals: @Composable (@Composable () -> Unit) -> Unit,

    private val layersViewController: ComposeLayersViewController,
    private val initialLayoutDirection: LayoutDirection,
    private val onAccessibilityChanged: () -> Unit,
    onFocusBehavior: OnFocusBehavior,
    endEdgeGestureBehavior: EndEdgePanGestureBehavior,
    private var focusedViewsList: FocusedViewsList?,
    compositionContext: CompositionContext,
    private val ownerProvider: PlatformArchitectureComponentsOwner,
    private val coroutineContext: CoroutineContext,
    private val interfaceOrientationState: State<InterfaceOrientation>,
) : ComposeSceneLayer {

    override var focusable: Boolean = focusedViewsList != null
        set(value) {
            if (field != value) {
                field = value
                onAccessibilityChanged()
            }
        }

    val interactionView = UIKitComposeSceneLayerView(
        ::onDidMoveToWindow,
        ::isInsideInteractionBounds,
        isInterceptingOutsideEvents = { focusable }
    )

    val overlayView: UIView get() = mediator.overlayView

    private val navigationEventDispatcher: NavigationEventDispatcher
        get() = ownerProvider.navigationEventDispatcherOwner.navigationEventDispatcher

    private val navigationEventInput = UIKitNavigationEventInput(
        density = interactionView.density,
        getTopLeftOffsetInWindow = { boundsInWindow.topLeft },
        endEdgePanGestureBehavior = endEdgeGestureBehavior
    ).also { navigationEventDispatcher.addInput(it) }

    private val mediator = ComposeSceneMediator(
        onFocusBehavior = onFocusBehavior,
        focusedViewsList = focusedViewsList,
        windowContext = layersViewController.windowContext,
        architectureComponentsOwner = ownerProvider,
        coroutineContext = compositionContext.effectCoroutineContext,
        redrawer = layersViewController.metalView.redrawer,
        composeSceneFactory = ::createComposeScene,
        navigationEventInput = navigationEventInput,
        interfaceOrientationState = interfaceOrientationState
    ).also {
        interactionView.embedSubview(it.backgroundView)
    }

    private fun isInsideInteractionBounds(point: CValue<CGPoint>): Boolean =
        boundsInWindow.contains(point.asDpOffset().toOffset(interactionView.density).round())

    private fun createComposeScene(
        invalidate: () -> Unit,
        platformContext: PlatformContext
    ): ComposeScene =
        PlatformLayersComposeScene(
            density = mediator.screenDensity,
            layoutDirection = initialLayoutDirection,
            coroutineContext = coroutineContext,
            composeSceneContext = createComposeSceneContext(platformContext),
            invalidate = invalidate,
        )

    val hasInvalidations by mediator::hasInvalidations

    var isAccessibilityEnabled by mediator::isAccessibilityEnabled

    override var density: Density
        get() = mediator.composeSceneDensity
        set(_) {
            // density of the layer cannot be customized
        }

    override var layoutDirection by mediator::layoutDirection

    override var boundsInWindow: IntRect by mediator::interactionBounds

    override var compositionLocalContext by mediator::compositionLocalContext

    override var scrimColor: Color? = null
        set(value) {
            if (field != value) {
                field = value
                value?.let {
                    scrimPaint.color = value
                }
            }
        }

    private val scrimPaint = Paint()

    private fun onDidMoveToWindow(window: UIWindow?) {
        if (window != null) {
            focusedViewsList?.addAndFocus(mediator.backgroundView)
        }
        navigationEventInput.onDidMoveToWindow(window, interactionView)
    }

    fun render(canvas: Canvas, nanoTime: Long) {
        if (scrimColor != null) {
            val density = layersViewController.metalView.density
            val rect = layersViewController.metalView.bounds.asDpRect().toRect(density)

            canvas.drawRect(rect, scrimPaint)
        }

        mediator.render(canvas, nanoTime)
    }

    fun retrieveInteropTransaction() = mediator.retrieveInteropTransaction()

    val hasInteropViews: Boolean get() = mediator.hasInteropViews

    fun prepareAndGetSizeTransitionAnimation() = mediator.prepareAndGetSizeTransitionAnimation()

    override fun close() {
        onClosed(this)

        dispose()
    }

    internal fun dispose() {
        navigationEventDispatcher.removeInput(navigationEventInput)
        focusedViewsList?.disposeChild()
        focusedViewsList = null
        mediator.dispose()
        interactionView.removeFromSuperview()
        interactionView.dispose()
    }

    @Composable
    private fun ProvideComposeSceneLayerCompositionLocals(
        content: @Composable () -> Unit
    ) = CompositionLocalProvider(
        LocalUIViewController provides layersViewController,
        content = content
    )

    override fun setContent(content: @Composable () -> Unit) {
        mediator.setContent {
            hostCompositionLocals {
                ProvideComposeSceneLayerCompositionLocals(content)
            }
        }
    }

    override fun setKeyEventListener(
        onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
        onKeyEvent: ((KeyEvent) -> Boolean)?
    ) {
        mediator.setKeyEventListener(onPreviewKeyEvent, onKeyEvent)
    }

    override fun setOutsidePointerEventListener(
        onOutsidePointerEvent: ((eventType: PointerEventType, button: PointerButton?) -> Unit)?
    ) {
        interactionView.onOutsidePointerEvent = {
            onOutsidePointerEvent?.invoke(it, null)
        }
    }

    /**
     * Since layer is assumed to be the same size as the window it is attached to, just return the same position.
     */
    override fun calculateLocalPosition(positionInWindow: IntOffset): IntOffset = positionInWindow

    fun sceneDidAppear() {
        mediator.sceneDidAppear()
    }

    fun sceneWillDisappear() {
        mediator.sceneWillDisappear()
    }
}