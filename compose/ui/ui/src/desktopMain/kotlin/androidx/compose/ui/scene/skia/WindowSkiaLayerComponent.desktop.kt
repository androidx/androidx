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

package androidx.compose.ui.scene.skia

import androidx.compose.ui.awt.RenderSettings
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.scene.ComposeSceneMediator
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.SkiaLayerProperties
import org.jetbrains.skiko.SkikoRenderDelegate

/**
 * Provides a heavyweight AWT [hierarchyRoot] used to render content
 * (provided by [SkikoRenderDelegate]) on-screen with Skia.
 *
 * This component renders content directly to a Skia surface for better performance,
 * using the configuration specified in [renderSettings]. It configures the vsync behavior
 * based on the [RenderSettings.SkiaSurface.isVsyncEnabled] property.
 *
 * If smooth interop with Swing is needed, consider using [SwingSkiaLayerComponent] instead,
 * which is created when using [RenderSettings.SwingGraphics].
 */
internal class WindowSkiaLayerComponent(
    mediator: ComposeSceneMediator,
    private val windowContext: PlatformWindowContext,
    renderDelegate: SkikoRenderDelegate,
    skiaLayerAnalytics: SkiaLayerAnalytics,
    private val renderSettings: RenderSettings.SkiaSurface,
) : SkiaLayerComponent {
    /**
     * See also backend layer for swing interop in [SwingSkiaLayerComponent]
     */
    override val hierarchyRoot: SkiaLayer = object : SkiaLayer(
        accessibleContextProvider = mediator.accessibility.accessibleContextProvider,
        properties = run {
            val defaultProperties = SkiaLayerProperties()

            SkiaLayerProperties(
                isVsyncEnabled = renderSettings.isVsyncEnabled ?: defaultProperties.isVsyncEnabled,
            )
        },
        analytics = skiaLayerAnalytics
    ) {

        init {
            // SkiaLayer never receives focus, only its underlying canvas
            canvas.addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent?) = onFocusEvent()
                override fun focusLost(e: FocusEvent?) = onFocusEvent()
            })
        }

        private var endCompositionWorkaround: InputMethodEndCompositionWorkaround? = null

        override fun getInputContext() =
            endCompositionWorkaround?.inputContext ?: super.getInputContext()

        override fun addNotify() {
            super.addNotify()

            endCompositionWorkaround = InputMethodEndCompositionWorkaround.forCurrentEnvironment(
                componentInputContext = { super.getInputContext() }
            )
        }

        override fun paint(g: Graphics) {
            mediator.onChangeDensity()
            super.paint(g)
        }

        override fun getInputMethodRequests() = mediator.currentInputMethodRequests

        override fun doLayout() {
            super.doLayout()
            mediator.onContainerSizeChanged()
        }

        override fun getPreferredSize(): Dimension = if (isPreferredSizeSet) {
            super.getPreferredSize()
        } else {
            mediator.preferredSize
        }

        // Workaround for enableInputMethods being ignored until the component is actually focused.
        // This also controls the default state, without needing it to be set from the outside.
        private var inputMethodsEnabled = false

        private fun onFocusEvent() {
            // enableInputMethods is idempotent (and quick when applying the same value),
            // so it's ok to call it on every event
            super.enableInputMethods(inputMethodsEnabled)
        }

        override fun enableInputMethods(enable: Boolean) {
            inputMethodsEnabled = enable
            super.enableInputMethods(enable)
        }

        override fun dispose() {
            super.dispose()
            isDisposed = true
        }
    }

    override val contentRoot: Component
        get() = hierarchyRoot.canvas

    override val renderApi by hierarchyRoot::renderApi

    override val interopBlendingSupported
        get() = when(renderApi) {
            GraphicsApi.DIRECT3D, GraphicsApi.METAL -> true
            else -> false
        }

    override val clipComponents by hierarchyRoot::clipComponents

    override var transparency
        get() = hierarchyRoot.transparency
        set(value) {
            hierarchyRoot.transparency = value
            hierarchyRoot.background = when {
                !value -> null  // This will use the parent's background
                // In case of transparent Metal canvas on an opaque window, background values with
                // alpha == 0 will make the result color black after clearing the canvas.
                // The case of `transparency = true` together with `!windowContext.isWindowTransparent`
                // is needed for "Experimental interop on a regular (non-transparent) window"
                // (per @MatkovIvan)
                !windowContext.isWindowTransparent && (renderApi == GraphicsApi.METAL) -> null
                else -> java.awt.Color(0, 0, 0, 0)
            }
        }
    override var fullscreen by hierarchyRoot::fullscreen

    override val windowHandle by hierarchyRoot::windowHandle

    private var isDisposed = false

    init {
        hierarchyRoot.renderDelegate = renderDelegate
    }

    override fun dispose() {
        hierarchyRoot.dispose()
        isDisposed = true
    }

    override fun needRender() {
        if (!isDisposed) {
            hierarchyRoot.needRender()
        }
    }

    override fun renderImmediately() {
        hierarchyRoot.renderImmediately()
    }

    override fun onRenderApiChanged(action: () -> Unit) {
        hierarchyRoot.onStateChanged(SkiaLayer.PropertyKind.Renderer) { action() }
    }
}
