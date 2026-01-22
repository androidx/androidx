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
import androidx.compose.ui.platform.a11y.AccessibleFocusHelper
import androidx.compose.ui.scene.ComposeSceneMediator
import java.awt.Component
import javax.accessibility.Accessible
import javax.accessibility.AccessibleContext
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

    fun requestFocusOnAccessible(accessible: Accessible)
    fun onComposeInvalidation()
    fun renderImmediately()
    fun onRenderApiChanged(action: () -> Unit)
}

/**
 * A base implementation of [SkiaLayerComponent]
 */
internal abstract class BaseSkiaLayerComponent(
    protected val mediator: ComposeSceneMediator,
): SkiaLayerComponent {
    private var accessibleFocusHelper: AccessibleFocusHelper? = null

    /**
     * Returns the [AccessibleContext] for the component.
     *
     * This is called every time (but with the same [component]) the a11y system calls
     * [Component.getAccessibleContext] on the underlying component where the scene is actually
     * rendered.
     */
    protected fun provideAccessibleContext(component: Component): AccessibleContext {
        val helper = accessibleFocusHelper ?:
        AccessibleFocusHelper(component, mediator.accessible).also {
            accessibleFocusHelper = it
        }
        return helper.accessibleContext
    }

    override fun requestFocusOnAccessible(accessible: Accessible) {
        accessibleFocusHelper?.requestFocusOnAccessible(accessible)
    }
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
