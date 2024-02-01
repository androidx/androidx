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
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.ComposeContainer
import androidx.compose.ui.window.FocusStack
import androidx.compose.ui.window.ProvideContainerCompositionLocals
import androidx.compose.ui.window.RenderingUIView
import kotlin.coroutines.CoroutineContext
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIColor
import platform.UIKit.UIEvent
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
    compositionLocalContext: CompositionLocalContext?,
) : ComposeSceneLayer {

    override var focusable: Boolean = focusStack != null
    private var onOutsidePointerEvent: ((dismissRequest: Boolean) -> Unit)? = null
    private val rootView = composeContainer.view.window ?: composeContainer.view
    private val backgroundView: UIView = object : UIView(
        frame = CGRectZero.readValue()
    ) {
        init {
            translatesAutoresizingMaskIntoConstraints = false
            rootView.addSubview(this)
            NSLayoutConstraint.activateConstraints(
                getConstraintsToFillParent(this, rootView)
            )
        }

        override fun pointInside(point: CValue<CGPoint>, withEvent: UIEvent?): Boolean {
            //TODO pass invoke(true) on touch up event only when this touch event begins outside of layer bounds.
            // Also it should be only one touch event (not multitouch with 2 and more touches).
            // In other cases pass invoke(false)
            onOutsidePointerEvent?.invoke(true)
            return focusable
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
            composeSceneFactory = ::createComposeScene,
        ).also {
            it.compositionLocalContext = compositionLocalContext
        }
    }

    init {
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
        //todo It needs to handle dismiss key, like Esc. But on iOS it is very rare case.
        // But also it is exposed to public in Popup.skiko.kt
    }

    override fun setOutsidePointerEventListener(
        onOutsidePointerEvent: ((dismissRequest: Boolean) -> Unit)?
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
