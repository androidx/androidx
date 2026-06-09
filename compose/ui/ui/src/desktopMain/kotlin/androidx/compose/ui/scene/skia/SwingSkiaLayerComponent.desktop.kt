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

import androidx.compose.ui.scene.ComposeSceneMediator
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.FocusEvent
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.SkikoRenderDelegate
import org.jetbrains.skiko.swing.SkiaSwingLayer

/**
 * Provides a lightweight Swing [hierarchyRoot] used to render content
 * (provided by [SkikoRenderDelegate]) on-screen with Skia.
 *
 * [SwingSkiaLayerComponent] provides smooth integration with Swing, so z-ordering, double-buffering etc. from Swing will be taken into account.
 *
 * However, if smooth interop with Swing is not needed, consider using [SurfaceSkiaLayerComponent]
 */
@OptIn(ExperimentalSkikoApi::class)
internal class SwingSkiaLayerComponent(
    mediator: ComposeSceneMediator,
    renderDelegate: SkikoRenderDelegate,
    skiaLayerAnalytics: SkiaLayerAnalytics,
) : SkiaLayerComponent {
    /**
     * See also backendLayer for standalone Compose in [SurfaceSkiaLayerComponent]
     */
    override val hierarchyRoot: SkiaSwingLayer =
        object : SkiaSwingLayer(
            renderDelegate = renderDelegate,
            analytics = skiaLayerAnalytics,
            accessibleContextProvider = mediator.accessibility.accessibleContextProvider
        ) {
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

            override fun processFocusEvent(e: FocusEvent?) {
                super.processFocusEvent(e)

                // enableInputMethods is idempotent (and quick when applying the same value),
                // so it's ok to call it on every event
                super.enableInputMethods(inputMethodsEnabled)
            }

            override fun enableInputMethods(enable: Boolean) {
                inputMethodsEnabled = enable
                super.enableInputMethods(enable)
            }
        }

    override val contentRoot: Component
        get() = hierarchyRoot

    override val renderApi by hierarchyRoot::renderApi

    override val interopBlendingSupported: Boolean
        get() = true

    override val clipComponents by hierarchyRoot::clipComponents

    override var transparency
        get() = true
        set(_) {}

    override var fullscreen
        get() = false
        set(_) {}

    override val windowHandle get() = 0L

    override fun dispose() {
        hierarchyRoot.dispose()
    }

    override fun needRender() {
        hierarchyRoot.repaint()
    }

    override fun renderImmediately() {
        hierarchyRoot.paintImmediately(0, 0, hierarchyRoot.width, hierarchyRoot.height)
    }

    override fun onRenderApiChanged(action: () -> Unit) = Unit
}
