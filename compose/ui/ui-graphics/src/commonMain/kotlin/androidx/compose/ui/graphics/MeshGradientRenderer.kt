/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * A renderer responsible for tessellating and drawing a 2D mesh gradient.
 *
 * A mesh gradient is defined by a grid of vertices, where each vertex has a position, color, and
 * four optional Bezier control points (tangents) that define the curvature of the edges connecting
 * neighboring vertices. Colors can be interpolated using either bilinear or bicubic interpolation.
 *
 * The renderer can be stateful (e.g. on Android) and maintain internal buffers (such as index and
 * vertex buffers) to optimize rendering performance and avoid per-frame allocations.
 *
 * @see MeshGradientConfig
 */
internal interface MeshGradientRenderer {
    /**
     * Renders the mesh gradient defined by [config] onto the given [DrawScope].
     *
     * @param config The MeshGradientConfig to draw
     * @see MeshGradientConfig
     */
    fun DrawScope.draw(config: MeshGradientConfig)
}

/** Creates a [MeshGradientRenderer]. */
internal expect fun MeshGradientRenderer(): MeshGradientRenderer
