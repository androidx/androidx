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

package androidx.ui.rendering.custompaint

import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.foundation.Key
import androidx.ui.semantics.SemanticsProperties
import androidx.ui.semantics.SemanticsTag
import androidx.ui.vectormath64.Matrix4

/**
 * Signature of the function returned by [CustomPainter.semanticsBuilder].
 *
 * Builds semantics information describing the picture drawn by a
 * [CustomPainter]. Each [CustomPainterSemantics] in the returned list is
 * converted into a [SemanticsNode] by copying its properties.
 *
 * The returned list must not be mutated after this function completes. To
 * change the semantic information, the function must return a new list
 * instead.
 */
typealias SemanticsBuilderCallback = (size: Size) -> List<CustomPainterSemantics>

/**
 * Contains properties describing information drawn in a rectangle contained by
 * the [Canvas] used by a [CustomPaint].
 *
 * This information is used, for example, by assistive technologies to improve
 * the accessibility of applications.
 *
 * Implement [CustomPainter.semanticsBuilder] to build the semantic
 * description of the whole picture drawn by a [CustomPaint], rather that one
 * particular rectangle.
 *
 * See also:
 *
 * * [SemanticsNode], which is created using the properties of this class.
 * * [CustomPainter], which creates instances of this class.
 */
data class CustomPainterSemantics(
    /**
     * Identifies this object in a list of siblings.
     *
     * [SemanticsNode] inherits this key, so that when the list of nodes is
     * updated, its nodes are updated from [CustomPainterSemantics] with matching
     * keys.
     *
     * If this is null, the update algorithm does not guarantee which
     * [SemanticsNode] will be updated using this instance.
     *
     * This value is assigned to [SemanticsNode.key] during update.
     */
    val key: Key? = null,
    /**
     * The location and size of the box on the canvas where this piece of semantic
     * information applies.
     *
     * This value is assigned to [SemanticsNode.rect] during update.
     */
    val rect: Rect,
    /**
     * Contains properties that are assigned to the [SemanticsNode] created or
     * updated from this object.
     *
     * See also:
     *
     * * [Semantics], which is a widget that also uses [SemanticsProperties] to
     *   annotate.
     */
    val properties: SemanticsProperties,
    /**
     * The transform from the canvas' coordinate system to its parent's
     * coordinate system.
     *
     * This value is assigned to [SemanticsNode.transform] during update.
     */
    val transform: Matrix4? = null,
    /**
     * Tags used by the parent [SemanticsNode] to determine the layout of the
     * semantics tree.
     *
     * This value is assigned to [SemanticsNode.tags] during update.
     */
    val tags: Set<SemanticsTag>? = null
)