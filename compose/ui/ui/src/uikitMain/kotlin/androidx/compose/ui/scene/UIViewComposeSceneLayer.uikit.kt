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
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.skiko.RecordDrawRectRenderDecorator
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.toUIColor
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.viewinterop.UIKitInteropContainer
import androidx.compose.ui.window.ComposeContainer
import androidx.compose.ui.window.FocusStack
import androidx.compose.ui.window.ProvideContainerCompositionLocals
import androidx.compose.ui.window.RenderingUIView
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import org.jetbrains.skiko.SkikoRenderDelegate
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIEvent
import platform.UIKit.UITouch
import platform.UIKit.UIView
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol

internal class UIViewComposeSceneLayer(
    private val composeContainer: ComposeContainer,
    private val initDensity: Density,
    private val initLayoutDirection: LayoutDirection,
    configuration: ComposeUIViewControllerConfiguration,
    focusStack: FocusStack<UIView>?,
    windowContext: PlatformWindowContext,
    compositionContext: CompositionContext,
) : ComposeSceneLayer {

    override var focusable: Boolean = focusStack != null
    private var onOutsidePointerEvent: ((
        eventType: PointerEventType,
        button: PointerButton?
    ) -> Unit)? = null
    private val rootView = composeContainer.view.window ?: composeContainer.view
    private val backgroundView: UIView = object : UIView(
        frame = CGRectZero.readValue()
    ) {
        private var previousSuccessHitTestTimestamp: Double? = null

        private fun touchStartedOutside(withEvent: UIEvent?) {
            if (previousSuccessHitTestTimestamp != withEvent?.timestamp) {
                // This workaround needs to send PointerEventType.Press just once
                previousSuccessHitTestTimestamp = withEvent?.timestamp
                onOutsidePointerEvent?.invoke(PointerEventType.Press, null)
            }
        }

        /**
         * touchesEnded calls only when focused == true
         */
        override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
            val touch = touches.firstOrNull() as? UITouch
            val locationInView = touch?.locationInView(this)?.useContents { asDpOffset() }
            if (locationInView != null) {
                // This view's coordinate space is equal to [ComposeScene]'s
                val contains = boundsInWindow.contains(locationInView.toOffset(density).round())
                if (!contains) {
                    // TODO: Send only for last pointer in case of multi-touch
                    onOutsidePointerEvent?.invoke(PointerEventType.Release, null)
                }
            }
            super.touchesEnded(touches, withEvent)
        }

        override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
            val positionInWindow = point.useContents { asDpOffset().toOffset(density).round() }
            val inBounds = mediator.hitTestInteractionView(point, withEvent) != null &&
                boundsInWindow.contains(positionInWindow) // canvas might be bigger than logical bounds
            if (!inBounds && super.hitTest(point, withEvent) == this) {
                touchStartedOutside(withEvent)
                if (focusable) {
                    // Focusable layers don't pass touches through, even if it's out of bounds.
                    return this
                }
            }
            return null // transparent for touches
        }
    }

    private val mediator by lazy {
        ComposeSceneMediator(
            container = rootView,
            configuration = configuration,
            focusStack = focusStack,
            windowContext = windowContext,
            measureDrawLayerBounds = true,
            coroutineContext = compositionContext.effectCoroutineContext,
            renderingUIViewFactory = ::createSkikoUIView,
            composeSceneFactory = ::createComposeScene
        )
    }

    /**
     * Bounds of real drawings based on previous renders.
     */
    private var drawBounds = IntRect.Zero

    /**
     * The maximum amount to inflate the [drawBounds] comparing to [boundsInWindow].
     */
    private var maxDrawInflate = IntRect.Zero

    init {
        backgroundView.translatesAutoresizingMaskIntoConstraints = false
        rootView.addSubview(backgroundView)
        NSLayoutConstraint.activateConstraints(
            getConstraintsToFillParent(backgroundView, rootView)
        )
        composeContainer.attachLayer(this)
    }

    private fun createSkikoUIView(
        interopContainer: UIKitInteropContainer,
        renderDelegate: SkikoRenderDelegate
    ) = RenderingUIView(
            renderDelegate = recordDrawBounds(renderDelegate),
            retrieveInteropTransaction = {
                interopContainer.retrieveTransaction()
            }
        ).apply {
            opaque = false
        }

    private fun createComposeScene(
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext,
    ): ComposeScene =
        PlatformLayersComposeScene(
            density = initDensity, // We should use the local density already set for the current layer.
            layoutDirection = initLayoutDirection,
            coroutineContext = coroutineContext,
            composeSceneContext = composeContainer.createComposeSceneContext(platformContext),
            invalidate = invalidate,
        )

    fun hasInvalidations() = mediator.hasInvalidations()

    override var density by mediator::density
    override var layoutDirection by mediator::layoutDirection
    override var boundsInWindow: IntRect = IntRect.Zero
        set(value) {
            field = value
            updateBounds()
        }
    override var compositionLocalContext: CompositionLocalContext? by mediator::compositionLocalContext

    override var scrimColor: Color? = null
        set(value) {
            field = value
            backgroundView.setBackgroundColor(value?.toUIColor())
        }

    override fun close() {
        mediator.dispose()
        composeContainer.detachLayer(this)
        backgroundView.removeFromSuperview()
    }

    override fun setContent(content: @Composable () -> Unit) {
        mediator.setContent {
            ProvideContainerCompositionLocals(composeContainer) {
                content()
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
        this.onOutsidePointerEvent = onOutsidePointerEvent
    }

    override fun calculateLocalPosition(positionInWindow: IntOffset): IntOffset {
        return positionInWindow
    }

    private fun recordDrawBounds(renderDelegate: SkikoRenderDelegate) =
        RecordDrawRectRenderDecorator(renderDelegate) { canvasBoundsInPx ->
            val currentCanvasOffset = drawBounds.topLeft
            val drawBoundsInWindow = canvasBoundsInPx.roundToIntRect().translate(currentCanvasOffset)
            maxDrawInflate = maxInflate(boundsInWindow, drawBoundsInWindow, maxDrawInflate)
            drawBounds = IntRect(
                left = boundsInWindow.left - maxDrawInflate.left,
                top = boundsInWindow.top - maxDrawInflate.top,
                right = boundsInWindow.right + maxDrawInflate.right,
                bottom = boundsInWindow.bottom + maxDrawInflate.bottom
            )
            updateBounds()
        }

    private fun updateBounds() {
        mediator.setLayout(
            SceneLayout.Bounds(
                renderBounds = drawBounds,
                interactionBounds = boundsInWindow
            )
        )
    }

    fun sceneDidAppear() {
        mediator.sceneDidAppear()
    }

    fun sceneWillDisappear() {
        mediator.sceneWillDisappear()
    }

    fun viewSafeAreaInsetsDidChange() {
        mediator.viewSafeAreaInsetsDidChange()
    }

    fun viewWillLayoutSubviews() {
        mediator.viewWillLayoutSubviews()
    }

    fun viewWillTransitionToSize(
        targetSize: CValue<CGSize>,
        coordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        mediator.viewWillTransitionToSize(targetSize, coordinator)
    }
}

private fun maxInflate(baseBounds: IntRect, currentBounds: IntRect, maxInflate: IntRect) = IntRect(
    left = max(baseBounds.left - currentBounds.left, maxInflate.left),
    top = max(baseBounds.top - currentBounds.top, maxInflate.top),
    right = max(currentBounds.right - baseBounds.right, maxInflate.right),
    bottom = max(currentBounds.bottom - baseBounds.bottom, maxInflate.bottom)
)
