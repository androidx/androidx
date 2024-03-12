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

import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.scene.ComposeSceneMediator
import java.awt.Dimension
import java.awt.Graphics
import javax.accessibility.Accessible
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.SkikoView

/**
 * Provides a heavyweight AWT [contentComponent] used to render content (provided by client.skikoView) on-screen with Skia.
 *
 * If smooth interop with Swing is needed, consider using [SwingSkiaLayerComponent]
 */
internal class WindowSkiaLayerComponent(
    private val mediator: ComposeSceneMediator,
    private val windowContext: PlatformWindowContext,
    skikoView: SkikoView,
    skiaLayerAnalytics: SkiaLayerAnalytics
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
        analytics = skiaLayerAnalytics
    ) {
        override fun paint(g: Graphics) {
            mediator.onChangeDensity()
            super.paint(g)
        }

        override fun getInputMethodRequests() = mediator.currentInputMethodRequests

        override fun doLayout() {
            super.doLayout()
            mediator.onChangeComponentSize()
        }

        override fun getPreferredSize(): Dimension = if (isPreferredSizeSet) {
            super.getPreferredSize()
        } else {
            mediator.preferredSize
        }
    }

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
        contentComponent.skikoView = skikoView
    }

    override fun dispose() {
        contentComponent.dispose()
    }

    override fun requestNativeFocusOnAccessible(accessible: Accessible) =
        contentComponent.requestNativeFocusOnAccessible(accessible)

    override fun onComposeInvalidation() {
        contentComponent.needRedraw()
    }

    override fun onRenderApiChanged(action: () -> Unit) {
        contentComponent.onStateChanged(SkiaLayer.PropertyKind.Renderer) { action() }
    }
}
