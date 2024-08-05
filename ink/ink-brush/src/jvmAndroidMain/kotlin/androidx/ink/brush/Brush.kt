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

package androidx.ink.brush

import androidx.ink.brush.color.Color as ComposeColor

/**
 * Defines how stroke inputs are interpreted to create the visual representation of a stroke.
 *
 * A [Brush] completely describes how inputs are used to create stroke meshes, and how those meshes
 * should be drawn by stroke renderers.
 *
 * Note: This is a placeholder implementation/API until the real code is migrated here.
 *
 * @param color The color of the stroke.
 * @param size The overall thickness of strokes created with a given brush, in the same units as the
 *   stroke coordinate system.
 */
public class Brush(public val color: Long, public val size: Float) {
    private val colorObj = ComposeColor(color.toULong())
}
