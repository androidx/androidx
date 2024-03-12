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

package androidx.compose.ui.scene

import org.jetbrains.skia.Canvas

/**
 * Represents an abstract class for a desktop Compose scene layer.
 *
 * @see SwingComposeSceneLayer
 * @see WindowComposeSceneLayer
 */
internal abstract class DesktopComposeSceneLayer : ComposeSceneLayer {

    /**
     * Called when the focus of the window containing main Compose view has changed.
     */
    open fun onChangeWindowFocus() {
    }

    /**
     * Called when position of the window containing main Compose view has changed.
     */
    open fun onChangeWindowPosition() {
    }

    /**
     * Called when size of the window containing main Compose view has changed.
     */
    open fun onChangeWindowSize() {
    }

    /**
     * Renders the overlay on the main Compose view canvas.
     *
     * @param canvas the canvas of the main Compose view
     * @param width the width of the canvas
     * @param height the height of the canvas
     */
    open fun onRenderOverlay(canvas: Canvas, width: Int, height: Int) {
    }
}
