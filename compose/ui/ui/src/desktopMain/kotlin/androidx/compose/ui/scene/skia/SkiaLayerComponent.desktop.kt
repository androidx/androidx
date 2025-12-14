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
import javax.accessibility.Accessible
import javax.swing.JComponent
import org.jetbrains.skiko.ClipRectangle
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaLayerAnalytics
import org.jetbrains.skiko.SkikoRenderDelegate
import org.jetbrains.skiko.swing.SkiaSwingLayer

/**
 * Represents a component that is capable of rendering graphics using Skia library.
 *
 * It's implemented as adapter to [SkiaLayer] or [SkiaSwingLayer].
 */
internal interface SkiaLayerComponent {
    val contentComponent: JComponent
    // The Accessible that will be reported as the accessible parent of
    // ComposeSceneMediator.accessible (ComposeSceneAccessible)
    val sceneAccessibleParent: Accessible?
    val interopBlendingSupported: Boolean
    val renderApi: GraphicsApi
    val clipComponents: MutableList<ClipRectangle>

    var transparency: Boolean
    var fullscreen: Boolean
    val windowHandle: Long

    fun dispose()
    fun requestNativeFocusOnAccessible(accessible: Accessible)

    fun onComposeInvalidation()
    fun renderImmediately()
    fun onRenderApiChanged(action: () -> Unit)
}

/**
 * Factory method to create an instance of [SkiaLayerComponent] based on the render settings.
 */
internal fun SkiaLayerComponent(
    mediator: ComposeSceneMediator,
    windowContext: PlatformWindowContext,
    renderDelegate: SkikoRenderDelegate,
    skiaLayerAnalytics: SkiaLayerAnalytics,
    renderSettings: RenderSettings,
): SkiaLayerComponent = when (renderSettings) {
    is RenderSettings.SwingGraphics -> SwingSkiaLayerComponent(
        mediator = mediator,
        renderDelegate = renderDelegate,
        skiaLayerAnalytics = skiaLayerAnalytics
    )
    is RenderSettings.SkiaSurface -> WindowSkiaLayerComponent(
        mediator = mediator,
        windowContext = windowContext,
        renderDelegate = renderDelegate,
        skiaLayerAnalytics = skiaLayerAnalytics,
        renderSettings = renderSettings
    )
}
