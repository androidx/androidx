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

package androidx.compose.ui.skiko

import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkikoView

/**
 * Decorator for [SkikoView] which adds overlay rendering functionality.
 *
 * @param decorated The decorated [SkikoView] instance.
 * @param onRenderOverlay Function to be called for rendering the overlay.
 */
internal class OverlaySkikoViewDecorator(
    private val decorated: SkikoView,
    private val onRenderOverlay: (canvas: Canvas, width: Int, height: Int) -> Unit
) : SkikoView by decorated {
    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        decorated.onRender(canvas, width, height, nanoTime)
        onRenderOverlay(canvas, width, height)
    }
}
