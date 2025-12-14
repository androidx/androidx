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
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.accessibility.Accessible
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.SkiaLayerProperties
import org.jetbrains.skiko.SkikoRenderDelegate

/**
 * Provides a heavyweight AWT [contentComponent] used to render content
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
    private val mediator: ComposeSceneMediator,
    private val windowContext: PlatformWindowContext,
    renderDelegate: SkikoRenderDelegate,
    skiaLayerAnalytics: SkiaLayerAnalytics,
    private val renderSettings: RenderSettings.SkiaSurface,
) : SkiaLayerComponent {
    /**
     * See also backend layer for swing interop in [SwingSkiaLayerComponent]
     */
    override val contentComponent: SkiaLayer = object : SkiaLayer(
        externalAccessibleFactory = {
            // It depends on initialization order, so explicitly
            // apply `checkNotNull` for "non-null" field.
            checkNotNull(mediator.accessible)
        },
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
    }

    override val sceneAccessibleParent: Accessible
        // SkiaLayer passes externalAccessibleFactory to a child component of itself.
        get() = contentComponent

    override val renderApi by contentComponent::renderApi

    override val interopBlendingSupported
        get() = when(renderApi) {
            GraphicsApi.DIRECT3D, GraphicsApi.METAL -> true
            else -> false
        }

    override val clipComponents by contentComponent::clipComponents

    override var transparency
        get() = contentComponent.transparency
        set(value) {
            contentComponent.transparency = value
            if (value && !windowContext.isWindowTransparent && renderApi == GraphicsApi.METAL) {
                /*
                 * SkiaLayer sets background inside transparency setter, that is required for
                 * cases like software rendering.
                 * In case of transparent Metal canvas on opaque window, background values with
                 * alpha == 0 will make the result color black after clearing the canvas.
                 *
                 * Reset it to null to keep the color default.
                 */
                contentComponent.background = null
            }
        }
    override var fullscreen by contentComponent::fullscreen

    override val windowHandle by contentComponent::windowHandle

    init {
        contentComponent.renderDelegate = renderDelegate
    }

    override fun dispose() {
        contentComponent.dispose()
    }

    override fun requestNativeFocusOnAccessible(accessible: Accessible) =
        contentComponent.requestNativeFocusOnAccessible(accessible)

    override fun onComposeInvalidation() {
        contentComponent.needRender()
    }

    override fun renderImmediately() {
        contentComponent.renderImmediately()
    }

    override fun onRenderApiChanged(action: () -> Unit) {
        contentComponent.onStateChanged(SkiaLayer.PropertyKind.Renderer) { action() }
    }
}
