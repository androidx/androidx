/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.awt

import androidx.compose.ui.ExperimentalComposeUiApi
import org.jetbrains.skiko.SkikoProperties

/**
 * Configuration class for rendering settings in Compose for Desktop.
 * 
 * This class provides options to control how Compose content is rendered in desktop applications.
 * There are two main rendering approaches available:
 * - [SwingGraphics]: Renders content to a Swing provided offscreen buffer (better for Swing integration)
 * - [SkiaSurface]: Renders content directly to a GPU surface (better performance)
 */
@ExperimentalComposeUiApi
sealed class RenderSettings {

    /**
     * Renders Compose content to a Swing provided offscreen buffer, with presentation controlled
     * by Swing.
     *
     * This approach provides better integration with Swing components and prevents transitional
     * rendering issues when panels are being shown, hidden or resized. It also enables proper
     * layering when combining Swing components and Compose panels.
     *
     * Note: This approach requires an additional copy from offscreen texture to Swing graphics
     * on each re-draw, which may result in some performance penalty (proportional to the size)
     * compared to [SkiaSurface].
     */
    @Suppress("CanSealedSubClassBeObject")
    @ExperimentalComposeUiApi
    class SwingGraphics : RenderSettings()

    /**
     * Renders Compose content directly to a GPU surface for better performance.
     *
     * This approach bypasses Swing's rendering pipeline and draws directly to the screen
     * using hardware acceleration, which can provide better performance than [SwingGraphics].
     *
     * @property isVsyncEnabled Controls vertical synchronization (vsync) for rendering:
     * - When `true`: Synchronizes frame presentation with the display's refresh rate, 
     *   eliminating visual artifacts like screen tearing at the cost of slightly increased latency.
     * - When `false`: Presents frames as soon as they're ready without waiting for the display's 
     *   refresh, reducing latency but potentially causing visual artifacts like screen tearing.
     * - When `null`: Uses the global configuration from [SkikoProperties.vsyncEnabled].
     */
    @ExperimentalComposeUiApi
    class SkiaSurface(val isVsyncEnabled: Boolean? = null): RenderSettings()
}
