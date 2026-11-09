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

package androidx.compose.remote.generator.ast

import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.WireBuffer
import java.io.InputStream
import java.util.Objects

/**
 * Top-level representation of a RemoteCompose document, containing structured metadata and an
 * introspectable AST of components, modifiers, drawing commands, and resources.
 *
 * Designed to conform to AndroidX API guidelines (no data classes, non-exhaustive sealed
 * interfaces, full binary compatibility).
 */
class RemoteDocument(
    val header: DocumentHeader,
    val nodes: List<DocumentNode>,
) {
    /** Serializes this document back to its binary RemoteCompose wire format. */
    fun save(): ByteArray {
        val buffer = WireBuffer()
        save(buffer)
        return buffer.cloneBytes()
    }

    /** Serializes this document into an existing WireBuffer. */
    fun save(buffer: WireBuffer) {
        header.write(buffer)
        for (node in nodes) {
            node.write(buffer)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteDocument) return false
        return header == other.header && nodes == other.nodes
    }

    override fun hashCode(): Int = Objects.hash(header, nodes)

    override fun toString(): String = "RemoteDocument(header=$header, nodeCount=${nodes.size})"

    companion object {
        const val MAX_CONTAINER_DEPTH = 100

        /** Loads and parses a RemoteCompose document from a byte array. */
        fun load(bytes: ByteArray, maxDepth: Int = MAX_CONTAINER_DEPTH): RemoteDocument {
            val buffer = WireBuffer()
            buffer.write(bytes)
            buffer.setIndex(0)
            return load(buffer, maxDepth)
        }

        /** Loads and parses a RemoteCompose document from an InputStream. */
        fun load(inputStream: InputStream, maxDepth: Int = MAX_CONTAINER_DEPTH): RemoteDocument {
            return load(inputStream.readBytes(), maxDepth)
        }

        /** Loads and parses a RemoteCompose document from a WireBuffer. */
        fun load(buffer: WireBuffer, maxDepth: Int = MAX_CONTAINER_DEPTH): RemoteDocument {
            val header = DocumentHeader.read(buffer)
            val nodes = ArrayList<DocumentNode>()
            val containerStack = ArrayDeque<MutableContainerNode>()

            while (buffer.available()) {
                val opcode = buffer.readByte() and 0xFF
                val node = decodeNode(opcode, buffer, containerStack, maxDepth)
                if (node != null) {
                    if (containerStack.isNotEmpty()) {
                        containerStack.last().addChild(node)
                    } else {
                        nodes.add(node)
                    }
                }
            }

            return RemoteDocument(header, nodes)
        }

        private fun decodeNode(
            opcode: Int,
            buffer: WireBuffer,
            containerStack: ArrayDeque<MutableContainerNode>,
            maxDepth: Int,
        ): DocumentNode? {
            return when (opcode) {
                Operations.CONTAINER_END -> {
                    if (containerStack.isNotEmpty()) {
                        val closed = containerStack.removeLast()
                        closed.toImmutable()
                    } else {
                        ContainerEndNode
                    }
                }
                Operations.DATA_FLOAT -> {
                    val id = buffer.readInt()
                    val value = buffer.readFloat()
                    FloatConstantNode(id, value)
                }
                Operations.DATA_INT -> {
                    val id = buffer.readInt()
                    val value = buffer.readInt()
                    IntegerConstantNode(id, value)
                }
                Operations.DATA_BOOLEAN -> {
                    val id = buffer.readInt()
                    val value = buffer.readBoolean()
                    BooleanConstantNode(id, value)
                }
                Operations.NAMED_VARIABLE -> {
                    val varId = buffer.readInt()
                    val varType = buffer.readInt()
                    val name = buffer.readUTF8(1024)
                    NamedVariableNode(varId, varType, name)
                }
                Operations.DRAW_RECT -> {
                    val l = buffer.readNanId()
                    val t = buffer.readNanId()
                    val r = buffer.readNanId()
                    val b = buffer.readNanId()
                    DrawRectNode(l, t, r, b)
                }
                Operations.DRAW_CIRCLE -> {
                    val cx = buffer.readNanId()
                    val cy = buffer.readNanId()
                    val rad = buffer.readNanId()
                    DrawCircleNode(cx, cy, rad)
                }
                Operations.DRAW_LINE -> {
                    val x1 = buffer.readNanId()
                    val y1 = buffer.readNanId()
                    val x2 = buffer.readNanId()
                    val y2 = buffer.readNanId()
                    DrawLineNode(x1, y1, x2, y2)
                }
                Operations.DRAW_ROUND_RECT -> {
                    val l = buffer.readNanId()
                    val t = buffer.readNanId()
                    val r = buffer.readNanId()
                    val b = buffer.readNanId()
                    val rx = buffer.readNanId()
                    val ry = buffer.readNanId()
                    DrawRoundRectNode(l, t, r, b, rx, ry)
                }
                Operations.MODIFIER_PADDING -> {
                    val l = buffer.readNanId()
                    val t = buffer.readNanId()
                    val r = buffer.readNanId()
                    val b = buffer.readNanId()
                    PaddingModifierNode(l, t, r, b)
                }
                Operations.MODIFIER_WIDTH -> {
                    val w = buffer.readNanId()
                    WidthModifierNode(w)
                }
                Operations.MODIFIER_HEIGHT -> {
                    val h = buffer.readNanId()
                    HeightModifierNode(h)
                }
                Operations.MODIFIER_BACKGROUND -> {
                    val color = buffer.readInt()
                    BackgroundModifierNode(color)
                }
                Operations.LAYOUT_BOX -> {
                    if (containerStack.size >= maxDepth) {
                        throw IllegalArgumentException(
                            "Exceeded maximum container nesting depth: $maxDepth"
                        )
                    }
                    val compId = buffer.readInt()
                    val animId = buffer.readInt()
                    val hPos = buffer.readInt()
                    val vPos = buffer.readInt()
                    val mutableBox = MutableBoxNode(compId, animId, hPos, vPos)
                    containerStack.addLast(mutableBox)
                    null // Added to parent when CONTAINER_END closes it
                }
                Operations.LAYOUT_ROW -> {
                    if (containerStack.size >= maxDepth) {
                        throw IllegalArgumentException(
                            "Exceeded maximum container nesting depth: $maxDepth"
                        )
                    }
                    val compId = buffer.readInt()
                    val animId = buffer.readInt()
                    val hPos = buffer.readInt()
                    val vPos = buffer.readInt()
                    val spacedBy = buffer.readFloat()
                    val mutableRow = MutableRowNode(compId, animId, hPos, vPos, spacedBy)
                    containerStack.addLast(mutableRow)
                    null
                }
                Operations.LAYOUT_COLUMN -> {
                    if (containerStack.size >= maxDepth) {
                        throw IllegalArgumentException(
                            "Exceeded maximum container nesting depth: $maxDepth"
                        )
                    }
                    val compId = buffer.readInt()
                    val animId = buffer.readInt()
                    val hPos = buffer.readInt()
                    val vPos = buffer.readInt()
                    val spacedBy = buffer.readFloat()
                    val mutableCol = MutableColumnNode(compId, animId, hPos, vPos, spacedBy)
                    containerStack.addLast(mutableCol)
                    null
                }
                Operations.DATA_TEXT -> {
                    val textId = buffer.readInt()
                    val text = buffer.readUTF8(4096)
                    TextDataNode(textId, text)
                }
                Operations.DATA_BITMAP -> {
                    val imageId = buffer.readInt()
                    val widthAndType = buffer.readInt()
                    val heightAndEncoding = buffer.readInt()
                    val bitmap = buffer.readBuffer()
                    BitmapDataNode(imageId, widthAndType, heightAndEncoding, bitmap)
                }
                else -> {
                    // Unknown or unmodeled operation in prototype
                    UnknownOperationNode(opcode)
                }
            }
        }
    }
}

/** Mutable builder used during streaming decode to collect children into containers. */
private sealed interface MutableContainerNode {
    fun addChild(child: DocumentNode)

    fun toImmutable(): DocumentNode
}

private class MutableBoxNode(
    val componentId: Int,
    val animationId: Int,
    val horizontalPositioning: Int,
    val verticalPositioning: Int,
) : MutableContainerNode {
    val modifiers = ArrayList<ModifierNode>()
    val children = ArrayList<DocumentNode>()

    override fun addChild(child: DocumentNode) {
        if (child is ModifierNode) {
            modifiers.add(child)
        } else {
            children.add(child)
        }
    }

    override fun toImmutable(): DocumentNode =
        BoxLayoutNode(
            componentId,
            animationId,
            horizontalPositioning,
            verticalPositioning,
            modifiers,
            children,
        )
}

private class MutableRowNode(
    val componentId: Int,
    val animationId: Int,
    val horizontalPositioning: Int,
    val verticalPositioning: Int,
    val spacedBy: Float,
) : MutableContainerNode {
    val modifiers = ArrayList<ModifierNode>()
    val children = ArrayList<DocumentNode>()

    override fun addChild(child: DocumentNode) {
        if (child is ModifierNode) {
            modifiers.add(child)
        } else {
            children.add(child)
        }
    }

    override fun toImmutable(): DocumentNode =
        RowLayoutNode(
            componentId,
            animationId,
            horizontalPositioning,
            verticalPositioning,
            spacedBy,
            modifiers,
            children,
        )
}

private class MutableColumnNode(
    val componentId: Int,
    val animationId: Int,
    val horizontalPositioning: Int,
    val verticalPositioning: Int,
    val spacedBy: Float,
) : MutableContainerNode {
    val modifiers = ArrayList<ModifierNode>()
    val children = ArrayList<DocumentNode>()

    override fun addChild(child: DocumentNode) {
        if (child is ModifierNode) {
            modifiers.add(child)
        } else {
            children.add(child)
        }
    }

    override fun toImmutable(): DocumentNode =
        ColumnLayoutNode(
            componentId,
            animationId,
            horizontalPositioning,
            verticalPositioning,
            spacedBy,
            modifiers,
            children,
        )
}
