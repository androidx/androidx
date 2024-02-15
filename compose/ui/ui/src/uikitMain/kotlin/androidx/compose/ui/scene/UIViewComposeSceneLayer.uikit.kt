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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.toDpOffset
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.ComposeContainer
import androidx.compose.ui.window.FocusStack
import androidx.compose.ui.window.ProvideContainerCompositionLocals
import androidx.compose.ui.window.RenderingUIView
import kotlin.coroutines.CoroutineContext
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIColor
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
    private var onOutsidePointerEvent: ((eventType: PointerEventType) -> Unit)? = null
    private val rootView = composeContainer.view.window ?: composeContainer.view
    private val backgroundView: UIView = object : UIView(
        frame = CGRectZero.readValue()
    ) {

        private var previousSuccessHitTestTimestamp: Double? = null

        private fun touchStartedOutside(withEvent: UIEvent?) {
            if (previousSuccessHitTestTimestamp != withEvent?.timestamp) {
                // This workaround needs to send PointerEventType.Press just once
                previousSuccessHitTestTimestamp = withEvent?.timestamp
                onOutsidePointerEvent?.invoke(PointerEventType.Press)
            }
        }

        /**
         * touchesEnded calls only when focused == true
         */
        override fun touchesEnded(touches: Set<*>, withEvent: UIEvent?) {
            val touch = touches.firstOrNull() as? UITouch
            val locationInView = touch?.locationInView(this)
            if (locationInView != null) {
                val offset = locationInView.useContents { toDpOffset() }
                val contains = boundsInWindow.contains(offset.toOffset(density).round())
                if (!contains) {
                    onOutsidePointerEvent?.invoke(PointerEventType.Release)
                }
            }
            super.touchesEnded(touches, withEvent)
        }

        override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
            if (
                mediator.hitTestInteractionView(point, withEvent) == null &&
                super.hitTest(point, withEvent) == this
            ) {
                touchStartedOutside(withEvent)
                if (focusable) {
                    return this // block touches
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
            coroutineContext = compositionContext.effectCoroutineContext,
            renderingUIViewFactory = ::createSkikoUIView,
            composeSceneFactory = ::createComposeScene
        )
    }

    init {
        backgroundView.translatesAutoresizingMaskIntoConstraints = false
        rootView.addSubview(backgroundView)
        NSLayoutConstraint.activateConstraints(
            getConstraintsToFillParent(backgroundView, rootView)
        )
        composeContainer.attachLayer(this)
    }

    private fun createSkikoUIView(renderDelegate: RenderingUIView.Delegate): RenderingUIView =
        RenderingUIView(renderDelegate = renderDelegate).apply {
            opaque = false
        }

    private fun createComposeScene(
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext,
    ): ComposeScene =
        SingleLayerComposeScene(
            coroutineContext = coroutineContext,
            composeSceneContext = composeContainer.createComposeSceneContext(platformContext),
            density = initDensity, // We should use the local density already set for the current layer.
            invalidate = invalidate,
            layoutDirection = initLayoutDirection,
        )

    override var density by mediator::density
    override var layoutDirection by mediator::layoutDirection

    override var boundsInWindow: IntRect
        get() = mediator.getBoundsInPx()
        set(value) {
            mediator.setLayout(
                SceneLayout.Bounds(rect = value)
            )
        }

    override var compositionLocalContext: CompositionLocalContext? by mediator::compositionLocalContext

    override var scrimColor: Color? = null
        get() = field
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
        onOutsidePointerEvent: ((eventType: PointerEventType) -> Unit)?
    ) {
        this.onOutsidePointerEvent = onOutsidePointerEvent
    }

    override fun calculateLocalPosition(positionInWindow: IntOffset): IntOffset {
        return positionInWindow
    }

    fun viewDidAppear(animated: Boolean) {
        mediator.viewDidAppear(animated)
    }

    fun viewWillDisappear(animated: Boolean) {
        mediator.viewWillDisappear(animated)
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

private fun Color.toUIColor() = UIColor(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble(),
)
