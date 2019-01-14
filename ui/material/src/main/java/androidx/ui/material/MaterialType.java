/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material;

// TODO("Migration|Andrey: Convert to Kotlin once enums will be supported in IR modules")

/**
 * The various kinds of material in material design. Used to
 * configure the default behavior of [Material] widgets.
 *
 * See also:
 *
 * * [Material], in particular [Material.type]
 * * [MaterialEdges]
 */
public enum MaterialType {
    /** Rectangle using default theme canvas color. */
    CANVAS,

    /** Rounded edges, card theme color. */
    CARD,

    /** A circle, no color by default (used for floating action buttons). */
    CIRCLE,

    /** Rounded edges, no color by default (used for [MaterialButton] buttons). */
    BUTTON,

    /**
     * A transparent piece of material that draws ink splashes and highlights.
     *
     * While the material metaphor describes child widgets as printed on the
     * material itself and do not hide ink effects, in practice the [Material]
     * widget draws child widgets on top of the ink effects.
     * A [Material] with type transparency can be placed on top of opaque widgets
     * to show ink effects on top of them.
     *
     * Prefer using the [Ink] widget for showing ink effects on top of opaque
     * widgets.
     */
    TRANSPARENCY
}
