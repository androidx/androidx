/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.inspection.inspector

import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.unit.IntRect

internal const val UNDEFINED_ID = 0L

internal val emptyBox = IntRect(0, 0, 0, 0)

// Flags see accessors for description
private const val FLAGS_NONE = 0b0000
private const val FLAGS_INLINED = 0b0001
private const val FLAGS_DRAW_MODIFIER = 0b0010
private const val FLAGS_CHILD_DRAW_MODIFIER = 0b0100
private const val FLAGS_UNKNOWN_LOCATION = 0b1000
private const val FLAGS_OVERRIDDEN_BOX_SIZE = 0b10000

/** Node representing a Composable for the Layout Inspector. */
class InspectorNode
internal constructor(
    /** The associated render node id or 0. */
    val id: Long,

    /** The associated key for tracking recomposition counts. */
    val key: Int,

    /**
     * The id of the associated anchor for tracking recomposition counts.
     *
     * An Anchor is a mechanism in the compose runtime that can identify a Group in the slot storage
     * that is invariant to slot storage updates. See [androidx.compose.runtime.Anchor] for more
     * information.
     */
    val anchorId: Int,

    /** The name of the Composable. */
    val name: String,

    /** The fileName where the Composable was called. */
    val fileName: String,

    /**
     * A hash of the package name to help disambiguate duplicate [fileName] values.
     *
     * This hash is calculated by,
     *
     * `packageName.fold(0) { hash, current -> hash * 31 + current.toInt() }?.absoluteValue`
     *
     * where the package name is the dotted name of the package. This can be used to disambiguate
     * which file is referenced by [fileName]. This number is -1 if there was no package hash
     * information generated such as when the file does not contain a package declaration.
     */
    val packageHash: Int,

    /** The line number where the Composable was called. */
    val lineNumber: Int,

    /** The UTF-16 offset in the file where the Composable was called */
    val offset: Int,

    /** The bounding box of the Composable. */
    internal val box: IntRect,

    /** The 4 corners of the polygon after transformations of the original rectangle. */
    val bounds: QuadBounds? = null,

    /** Flags for: inlined, hasDrawModifier,, hasChildDrawModifier */
    val flags: Int = FLAGS_NONE,

    /** The parameters of this Composable. */
    val parameters: List<RawParameter>,

    /** The id of a android View embedded under this node. */
    val viewId: Long,

    /** The merged semantics information of this Composable. */
    val mergedSemantics: List<RawParameter>,

    /** The un-merged semantics information of this Composable. */
    val unmergedSemantics: List<RawParameter>,

    /** The children nodes of this Composable. */
    val children: List<InspectorNode>,
) {
    /** Left side of the Composable in pixels. */
    val left: Int
        get() = box.left

    /** Top of the Composable in pixels. */
    val top: Int
        get() = box.top

    /** Width of the Composable in pixels. */
    val width: Int
        get() = box.width

    /** Width of the Composable in pixels. */
    val height: Int
        get() = box.height

    /** This node (or a non-reported child) has a LayoutInfo.modifier with a DrawModifierNode */
    val hasDrawModifier: Boolean
        get() = (flags and FLAGS_DRAW_MODIFIER) != 0

    /** This node is the first non system parent of a child that has draw modifier */
    val hasChildDrawModifier: Boolean
        get() = (flags and FLAGS_CHILD_DRAW_MODIFIER) != 0

    /** True if the code for the Composable was inlined */
    val inlined: Boolean
        get() = (flags and FLAGS_INLINED) != 0

    fun parametersByKind(kind: ParameterKind): List<RawParameter> =
        when (kind) {
            ParameterKind.Normal -> parameters
            ParameterKind.MergedSemantics -> mergedSemantics
            ParameterKind.UnmergedSemantics -> unmergedSemantics
        }
}

data class QuadBounds(
    val x0: Int,
    val y0: Int,
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    val x3: Int,
    val y3: Int,
)

/** Parameter definition with a raw value reference. */
class RawParameter(val name: String, val value: Any?)

/** Mutable version of [InspectorNode]. */
internal class MutableInspectorNode {
    var id = UNDEFINED_ID
    var key = 0
    var anchorId = 0
    val layoutNodes = mutableListOf<LayoutInfo>()
    val mergedSemantics = mutableListOf<RawParameter>()
    val unmergedSemantics = mutableListOf<RawParameter>()
    var name = ""
    var fileName = ""
    var packageHash = -1
    var lineNumber = 0
    var offset = 0
    var box: IntRect = emptyBox
    var bounds: QuadBounds? = null
    val parameters = mutableListOf<RawParameter>()
    var viewId = UNDEFINED_ID
    val children = mutableListOf<InspectorNode>()
    var flags = FLAGS_NONE
        private set

    var unknownLocation: Boolean
        get() = (flags and FLAGS_UNKNOWN_LOCATION) != 0
        set(value) {
            setFlag(FLAGS_UNKNOWN_LOCATION, value)
        }

    var inlined: Boolean
        get() = (flags and FLAGS_INLINED) != 0
        set(value) {
            setFlag(FLAGS_INLINED, value)
        }

    var hasDrawModifier: Boolean
        get() = (flags and FLAGS_DRAW_MODIFIER) != 0
        set(value) {
            setFlag(FLAGS_DRAW_MODIFIER, value)
        }

    var hasChildDrawModifier: Boolean
        get() = (flags and FLAGS_CHILD_DRAW_MODIFIER) != 0
        set(value) {
            setFlag(FLAGS_CHILD_DRAW_MODIFIER, value)
        }

    var boxSizeOverridden: Boolean
        get() = (flags and FLAGS_OVERRIDDEN_BOX_SIZE) != 0
        set(value) {
            setFlag(FLAGS_OVERRIDDEN_BOX_SIZE, value)
        }

    fun reset() {
        markUnwanted()
        id = UNDEFINED_ID
        key = 0
        anchorId = 0
        viewId = UNDEFINED_ID
        layoutNodes.clear()
        mergedSemantics.clear()
        unmergedSemantics.clear()
        box = emptyBox
        bounds = null
        flags = FLAGS_NONE
        children.clear()
    }

    fun markUnwanted(): MutableInspectorNode {
        name = ""
        fileName = ""
        packageHash = -1
        lineNumber = 0
        offset = 0
        parameters.clear()
        return this
    }

    fun shallowCopy(node: MutableInspectorNode): MutableInspectorNode = apply {
        id = node.id
        key = node.key
        anchorId = node.anchorId
        mergedSemantics.addAll(node.mergedSemantics)
        unmergedSemantics.addAll(node.unmergedSemantics)
        name = node.name
        fileName = node.fileName
        packageHash = node.packageHash
        lineNumber = node.lineNumber
        offset = node.offset
        box = node.box
        bounds = node.bounds
        inlined = node.inlined
        parameters.addAll(node.parameters)
        viewId = node.viewId
        flags = node.flags
    }

    fun shallowCopy(node: InspectorNode): MutableInspectorNode = apply {
        id = node.id
        key = node.key
        anchorId = node.anchorId
        mergedSemantics.addAll(node.mergedSemantics)
        unmergedSemantics.addAll(node.unmergedSemantics)
        name = node.name
        fileName = node.fileName
        packageHash = node.packageHash
        lineNumber = node.lineNumber
        offset = node.offset
        box = node.box
        bounds = node.bounds
        inlined = node.inlined
        parameters.addAll(node.parameters)
        viewId = node.viewId
        flags = node.flags
    }

    val isUnwanted: Boolean
        get() = name.isEmpty()

    val hostingAndroidView: Boolean
        get() = viewId != UNDEFINED_ID

    val hasLayerId: Boolean
        get() = id > UNDEFINED_ID

    val hasAssignedId: Boolean
        get() = id != UNDEFINED_ID

    private fun setFlag(flag: Int, value: Boolean) {
        flags =
            if (value) {
                flags or flag
            } else {
                flags and flag.inv()
            }
    }

    fun build(withSemantics: Boolean = true): InspectorNode =
        InspectorNode(
            id,
            key,
            anchorId,
            name,
            fileName,
            packageHash,
            lineNumber,
            offset,
            box,
            bounds,
            flags,
            parameters.toList(),
            viewId,
            if (withSemantics) mergedSemantics.toList() else emptyList(),
            if (withSemantics) unmergedSemantics.toList() else emptyList(),
            children.toList(),
        )
}
