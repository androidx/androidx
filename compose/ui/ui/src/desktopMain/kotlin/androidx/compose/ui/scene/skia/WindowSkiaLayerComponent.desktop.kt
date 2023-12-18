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

import androidx.compose.ui.awt.ComposeBridge
import androidx.compose.ui.platform.PlatformWindowContext
import java.awt.Dimension
import java.awt.Graphics
import javax.accessibility.Accessible
import org.jetbrains.skiko.ClipRectangle
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerAnalytics

/**
 * Provides a heavyweight AWT [contentComponent] used to render content (provided by client.skikoView) on-screen with Skia.
 *
 * If smooth interop with Swing is needed, consider using [SwingSkiaLayerComponent]
 */
internal class WindowSkiaLayerComponent(
    skiaLayerAnalytics: SkiaLayerAnalytics,
    private val windowContext: PlatformWindowContext,
    private val bridge: ComposeBridge
) : SkiaLayerComponent {
    /**
     * See also backend layer for swing interop in [SwingSkiaLayerComponent]
     */
    override val contentComponent: SkiaLayer = object : SkiaLayer(
        externalAccessibleFactory = { bridge.accessible },
        analytics = skiaLayerAnalytics
    ), Accessible {
        override fun paint(g: Graphics) {
            bridge.resetSceneDensity()
            super.paint(g)
        }

        override fun getInputMethodRequests() = bridge.currentInputMethodRequests

        override fun doLayout() {
            super.doLayout()
            bridge.updateSceneSize()
        }

        override fun getPreferredSize(): Dimension {
            return if (isPreferredSizeSet) super.getPreferredSize() else bridge.preferredSize
        }
    }

    override val renderApi: GraphicsApi
        get() = contentComponent.renderApi

    override val interopBlendingSupported: Boolean
        get() = when(renderApi) {
            GraphicsApi.DIRECT3D, GraphicsApi.METAL -> true
            else -> false
        }

    override val clipComponents: MutableList<ClipRectangle>
        get() = contentComponent.clipComponents

    override var transparency: Boolean
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

    override var fullscreen: Boolean
        get() = contentComponent.fullscreen
        set(value) {
            contentComponent.fullscreen = value
        }

    override val windowHandle: Long get() = contentComponent.windowHandle

    init {
        contentComponent.skikoView = bridge.skikoView
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
