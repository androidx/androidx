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

package androidx.compose.ui.graphics.layer

import androidx.compose.runtime.Immutable

/**
 * Determines when to render the contents of a layer into an offscreen buffer before being drawn to
 * the destination.
 */
@Immutable
@kotlin.jvm.JvmInline
value class CompositingStrategy internal constructor(@Suppress("unused") private val value: Int) {

    companion object {

        /**
         * Rendering to an offscreen buffer will be determined automatically by the rest of the
         * graphicsLayer parameters. This is the default behavior. For example, whenever an alpha
         * value less than 1.0f is provided on [Modifier.graphicsLayer], a compositing layer is
         * created automatically to first render the contents fully opaque, then draw this offscreen
         * buffer to the destination with the corresponding alpha. This is necessary for correctness
         * otherwise alpha applied to individual drawing instructions that overlap will have a
         * different result than expected. Additionally usage of [RenderEffect] on the graphicsLayer
         * will also render into an intermediate offscreen buffer before being drawn into the
         * destination.
         */
        val Auto = CompositingStrategy(0)

        /**
         * Rendering of content will always be rendered into an offscreen buffer first then drawn to
         * the destination regardless of the other parameters configured on the graphics layer. This
         * is useful for leveraging different blending algorithms for masking content. For example,
         * the contents can be drawn into this graphics layer and masked out by drawing additional
         * shapes with [BlendMode.Clear]
         */
        val Offscreen = CompositingStrategy(1)

        /**
         * Modulates alpha for each of the drawing instructions recorded within the graphicsLayer.
         * This avoids usage of an offscreen buffer for purposes of alpha rendering. [ModulateAlpha]
         * is more efficient than [Auto] in performance in scenarios where an alpha value less than
         * 1.0f is provided. Otherwise the performance is similar to that of [Auto]. However, this
         * can provide different results than [Auto] if there is overlapping content within the
         * layer and alpha is applied. This should only be used if the contents of the layer are
         * known well in advance and are expected to not be overlapping.
         */
        val ModulateAlpha = CompositingStrategy(2)
    }
}
