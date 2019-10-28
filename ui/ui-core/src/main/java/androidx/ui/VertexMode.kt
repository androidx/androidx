/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui

/**
 * Defines how a list of points is interpreted when drawing a set of triangles.
 *
 * Used by [Canvas.drawVertices].
 *These enum values must be kept in sync with SkVertices::VertexMode.
 */
enum class VertexMode(private val frameworkVertex: android.graphics.Canvas.VertexMode) {
    /** Draw each sequence of three points as the vertices of a triangle. */
    triangles(android.graphics.Canvas.VertexMode.TRIANGLES),

    /** Draw each sliding window of three points as the vertices of a triangle. */
    triangleStrip(android.graphics.Canvas.VertexMode.TRIANGLE_STRIP),

    /** Draw the first point and each sliding window of two points as the vertices of a triangle. */
    triangleFan(android.graphics.Canvas.VertexMode.TRIANGLE_FAN);

    fun toFrameworkVertexMode() = frameworkVertex
}