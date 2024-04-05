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
import java.awt.Dimension
import java.awt.Graphics
import javax.accessibility.Accessible
import javax.accessibility.AccessibleContext
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.SkikoView
import org.jetbrains.skiko.swing.SkiaSwingLayer

/**
 * Provides a lightweight Swing [contentComponent] used to render content (provided by client.skikoView) on-screen with Skia.
 *
 * [SwingSkiaLayerComponent] provides smooth integration with Swing, so z-ordering, double-buffering etc. from Swing will be taken into account.
 *
 * However, if smooth interop with Swing is not needed, consider using [WindowSkiaLayerComponent]
 */
@OptIn(ExperimentalSkikoApi::class)
internal class SwingSkiaLayerComponent(
    private val mediator: ComposeSceneMediator,
    skikoView: SkikoView,
    skiaLayerAnalytics: SkiaLayerAnalytics,
) : SkiaLayerComponent {
    /**
     * See also backendLayer for standalone Compose in [WindowSkiaLayerComponent]
     */
    override val contentComponent: SkiaSwingLayer =
        object : SkiaSwingLayer(
            skikoView = skikoView,
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

            override fun getAccessibleContext(): AccessibleContext? {
                return mediator.accessible.accessibleContext
            }
        }

    override val renderApi by contentComponent::renderApi

    override val interopBlendingSupported: Boolean
        get() = true

    override val clipComponents by contentComponent::clipComponents

    override var transparency
        get() = true
        set(_) {}

    override var fullscreen
        get() = false
        set(_) {}

    override val windowHandle get() = 0L

    override fun dispose() {
        contentComponent.dispose()
    }

    override fun requestNativeFocusOnAccessible(accessible: Accessible) {
        contentComponent.requestNativeFocusOnAccessible(accessible)
    }

    override fun onComposeInvalidation() {
        contentComponent.repaint()
    }

    override fun onRenderApiChanged(action: () -> Unit) = Unit
}
