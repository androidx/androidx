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
import java.util.Objects

/**
 * Root marker for all AST nodes in a RemoteCompose document. Non-exhaustive to allow
 * backward-compatible future extensions as per AndroidX guidelines.
 */
sealed interface DocumentNode {
    fun write(buffer: WireBuffer)

    // Hidden private implementation preventing exhaustive 'when' in consumer code
    private class Hidden : DocumentNode {
        override fun write(buffer: WireBuffer) {}
    }
}

sealed interface OperationNode : DocumentNode {
    val opcode: Int
    val operationName: String
}

sealed interface ContainerNode : OperationNode {
    val children: List<DocumentNode>
}

sealed interface ComponentNode : ContainerNode {
    val componentId: Int
    val modifiers: List<ModifierNode>
}

sealed interface ModifierNode : OperationNode

sealed interface CanvasCommandNode : OperationNode

sealed interface ResourceNode : OperationNode

// -----------------------------------------------------------------------------
// Component Layout Nodes
// -----------------------------------------------------------------------------

class BoxLayoutNode(
    override val componentId: Int,
    val animationId: Int,
    val horizontalPositioning: Int,
    val verticalPositioning: Int,
    override val modifiers: List<ModifierNode>,
    override val children: List<DocumentNode>,
) : ComponentNode {
    override val opcode: Int
        get() = Operations.LAYOUT_BOX

    override val operationName: String
        get() = "BoxLayout"

    override fun write(buffer: WireBuffer) {
        writeContainerIterative(this, buffer)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BoxLayoutNode) return false
        return componentId == other.componentId &&
            animationId == other.animationId &&
            horizontalPositioning == other.horizontalPositioning &&
            verticalPositioning == other.verticalPositioning &&
            modifiers == other.modifiers &&
            areChildrenEqual(children, other.children)
    }

    override fun hashCode(): Int = computeContainerHashCode(this)

    override fun toString(): String = containerToStringIterative(this)
}

class RowLayoutNode(
    override val componentId: Int,
    val animationId: Int,
    val horizontalPositioning: Int,
    val verticalPositioning: Int,
    val spacedBy: Float,
    override val modifiers: List<ModifierNode>,
    override val children: List<DocumentNode>,
) : ComponentNode {
    override val opcode: Int
        get() = Operations.LAYOUT_ROW

    override val operationName: String
        get() = "RowLayout"

    override fun write(buffer: WireBuffer) {
        writeContainerIterative(this, buffer)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RowLayoutNode) return false
        return componentId == other.componentId &&
            animationId == other.animationId &&
            horizontalPositioning == other.horizontalPositioning &&
            verticalPositioning == other.verticalPositioning &&
            spacedBy.toRawBits() == other.spacedBy.toRawBits() &&
            modifiers == other.modifiers &&
            areChildrenEqual(children, other.children)
    }

    override fun hashCode(): Int = computeContainerHashCode(this)

    override fun toString(): String = containerToStringIterative(this)
}

class ColumnLayoutNode(
    override val componentId: Int,
    val animationId: Int,
    val horizontalPositioning: Int,
    val verticalPositioning: Int,
    val spacedBy: Float,
    override val modifiers: List<ModifierNode>,
    override val children: List<DocumentNode>,
) : ComponentNode {
    override val opcode: Int
        get() = Operations.LAYOUT_COLUMN

    override val operationName: String
        get() = "ColumnLayout"

    override fun write(buffer: WireBuffer) {
        writeContainerIterative(this, buffer)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColumnLayoutNode) return false
        return componentId == other.componentId &&
            animationId == other.animationId &&
            horizontalPositioning == other.horizontalPositioning &&
            verticalPositioning == other.verticalPositioning &&
            spacedBy.toRawBits() == other.spacedBy.toRawBits() &&
            modifiers == other.modifiers &&
            areChildrenEqual(children, other.children)
    }

    override fun hashCode(): Int = computeContainerHashCode(this)

    override fun toString(): String = containerToStringIterative(this)
}

private fun writeContainerIterative(container: ContainerNode, buffer: WireBuffer) {
    val stack = ArrayDeque<Any>()
    stack.addLast(container)

    while (stack.isNotEmpty()) {
        when (val item = stack.removeLast()) {
            is ContainerEndNode -> ContainerEndNode.write(buffer)
            is BoxLayoutNode -> {
                buffer.start(item.opcode)
                buffer.writeInt(item.componentId)
                buffer.writeInt(item.animationId)
                buffer.writeInt(item.horizontalPositioning)
                buffer.writeInt(item.verticalPositioning)
                for (m in item.modifiers) m.write(buffer)
                stack.addLast(ContainerEndNode)
                for (i in item.children.indices.reversed()) {
                    stack.addLast(item.children[i])
                }
            }
            is RowLayoutNode -> {
                buffer.start(item.opcode)
                buffer.writeInt(item.componentId)
                buffer.writeInt(item.animationId)
                buffer.writeInt(item.horizontalPositioning)
                buffer.writeInt(item.verticalPositioning)
                buffer.writeFloat(item.spacedBy)
                for (m in item.modifiers) m.write(buffer)
                stack.addLast(ContainerEndNode)
                for (i in item.children.indices.reversed()) {
                    stack.addLast(item.children[i])
                }
            }
            is ColumnLayoutNode -> {
                buffer.start(item.opcode)
                buffer.writeInt(item.componentId)
                buffer.writeInt(item.animationId)
                buffer.writeInt(item.horizontalPositioning)
                buffer.writeInt(item.verticalPositioning)
                buffer.writeFloat(item.spacedBy)
                for (m in item.modifiers) m.write(buffer)
                stack.addLast(ContainerEndNode)
                for (i in item.children.indices.reversed()) {
                    stack.addLast(item.children[i])
                }
            }
            is DocumentNode -> item.write(buffer)
        }
    }
}

private fun areChildrenEqual(listA: List<DocumentNode>, listB: List<DocumentNode>): Boolean {
    if (listA === listB) return true
    if (listA.size != listB.size) return false
    val deque = ArrayDeque<Pair<DocumentNode, DocumentNode>>()
    for (i in listA.indices) {
        deque.addLast(Pair(listA[i], listB[i]))
    }
    while (deque.isNotEmpty()) {
        val (a, b) = deque.removeFirst()
        if (a === b) continue
        if (a.javaClass != b.javaClass) return false
        when (a) {
            is BoxLayoutNode -> {
                val bBox = b as BoxLayoutNode
                if (
                    a.componentId != bBox.componentId ||
                        a.animationId != bBox.animationId ||
                        a.horizontalPositioning != bBox.horizontalPositioning ||
                        a.verticalPositioning != bBox.verticalPositioning ||
                        a.modifiers != bBox.modifiers ||
                        a.children.size != bBox.children.size
                ) {
                    return false
                }
                for (i in a.children.indices) {
                    deque.addLast(Pair(a.children[i], bBox.children[i]))
                }
            }
            is RowLayoutNode -> {
                val bRow = b as RowLayoutNode
                if (
                    a.componentId != bRow.componentId ||
                        a.animationId != bRow.animationId ||
                        a.horizontalPositioning != bRow.horizontalPositioning ||
                        a.verticalPositioning != bRow.verticalPositioning ||
                        a.spacedBy.toRawBits() != bRow.spacedBy.toRawBits() ||
                        a.modifiers != bRow.modifiers ||
                        a.children.size != bRow.children.size
                ) {
                    return false
                }
                for (i in a.children.indices) {
                    deque.addLast(Pair(a.children[i], bRow.children[i]))
                }
            }
            is ColumnLayoutNode -> {
                val bCol = b as ColumnLayoutNode
                if (
                    a.componentId != bCol.componentId ||
                        a.animationId != bCol.animationId ||
                        a.horizontalPositioning != bCol.horizontalPositioning ||
                        a.verticalPositioning != bCol.verticalPositioning ||
                        a.spacedBy.toRawBits() != bCol.spacedBy.toRawBits() ||
                        a.modifiers != bCol.modifiers ||
                        a.children.size != bCol.children.size
                ) {
                    return false
                }
                for (i in a.children.indices) {
                    deque.addLast(Pair(a.children[i], bCol.children[i]))
                }
            }
            else -> {
                if (a != b) return false
            }
        }
    }
    return true
}

private class HashTask(val node: DocumentNode, var visited: Boolean = false)

private fun computeContainerHashCode(root: ContainerNode): Int {
    val stack = ArrayDeque<HashTask>()
    val resultStack = ArrayDeque<Int>()
    stack.addLast(HashTask(root, false))

    while (stack.isNotEmpty()) {
        val task = stack.last()
        if (task.visited) {
            stack.removeLast()
            when (val node = task.node) {
                is BoxLayoutNode -> {
                    var childrenHash = 1
                    val count = node.children.size
                    val childHashes = IntArray(count)
                    for (i in (count - 1) downTo 0) {
                        childHashes[i] = resultStack.removeLast()
                    }
                    for (h in childHashes) {
                        childrenHash = 31 * childrenHash + h
                    }
                    resultStack.addLast(
                        Objects.hash(
                            node.componentId,
                            node.animationId,
                            node.horizontalPositioning,
                            node.verticalPositioning,
                            node.modifiers,
                            childrenHash,
                        )
                    )
                }
                is RowLayoutNode -> {
                    var childrenHash = 1
                    val count = node.children.size
                    val childHashes = IntArray(count)
                    for (i in (count - 1) downTo 0) {
                        childHashes[i] = resultStack.removeLast()
                    }
                    for (h in childHashes) {
                        childrenHash = 31 * childrenHash + h
                    }
                    resultStack.addLast(
                        Objects.hash(
                            node.componentId,
                            node.animationId,
                            node.horizontalPositioning,
                            node.verticalPositioning,
                            node.spacedBy,
                            node.modifiers,
                            childrenHash,
                        )
                    )
                }
                is ColumnLayoutNode -> {
                    var childrenHash = 1
                    val count = node.children.size
                    val childHashes = IntArray(count)
                    for (i in (count - 1) downTo 0) {
                        childHashes[i] = resultStack.removeLast()
                    }
                    for (h in childHashes) {
                        childrenHash = 31 * childrenHash + h
                    }
                    resultStack.addLast(
                        Objects.hash(
                            node.componentId,
                            node.animationId,
                            node.horizontalPositioning,
                            node.verticalPositioning,
                            node.spacedBy,
                            node.modifiers,
                            childrenHash,
                        )
                    )
                }
                else -> {}
            }
        } else {
            task.visited = true
            when (val node = task.node) {
                is BoxLayoutNode -> {
                    for (i in node.children.indices.reversed()) {
                        stack.addLast(HashTask(node.children[i], false))
                    }
                }
                is RowLayoutNode -> {
                    for (i in node.children.indices.reversed()) {
                        stack.addLast(HashTask(node.children[i], false))
                    }
                }
                is ColumnLayoutNode -> {
                    for (i in node.children.indices.reversed()) {
                        stack.addLast(HashTask(node.children[i], false))
                    }
                }
                else -> {
                    stack.removeLast()
                    resultStack.addLast(node.hashCode())
                }
            }
        }
    }
    return resultStack.removeLast()
}

private fun containerToStringIterative(root: ContainerNode): String {
    val sb = StringBuilder()
    val stack = ArrayDeque<Any>()
    stack.addLast(root)

    while (stack.isNotEmpty()) {
        when (val item = stack.removeLast()) {
            is String -> sb.append(item)
            is BoxLayoutNode -> {
                sb.append("BoxLayoutNode(id=").append(item.componentId)
                sb.append(", hPos=").append(item.horizontalPositioning)
                sb.append(", vPos=").append(item.verticalPositioning)
                sb.append(", modifiers=").append(item.modifiers)
                sb.append(", children=[")
                stack.addLast("])")
                for (i in item.children.indices.reversed()) {
                    if (i < item.children.size - 1) {
                        stack.addLast(", ")
                    }
                    stack.addLast(item.children[i])
                }
            }
            is RowLayoutNode -> {
                sb.append("RowLayoutNode(id=").append(item.componentId)
                sb.append(", spacedBy=").append(item.spacedBy)
                sb.append(", modifiers=").append(item.modifiers)
                sb.append(", children=[")
                stack.addLast("])")
                for (i in item.children.indices.reversed()) {
                    if (i < item.children.size - 1) {
                        stack.addLast(", ")
                    }
                    stack.addLast(item.children[i])
                }
            }
            is ColumnLayoutNode -> {
                sb.append("ColumnLayoutNode(id=").append(item.componentId)
                sb.append(", spacedBy=").append(item.spacedBy)
                sb.append(", modifiers=").append(item.modifiers)
                sb.append(", children=[")
                stack.addLast("])")
                for (i in item.children.indices.reversed()) {
                    if (i < item.children.size - 1) {
                        stack.addLast(", ")
                    }
                    stack.addLast(item.children[i])
                }
            }
            is DocumentNode -> sb.append(item.toString())
        }
    }
    return sb.toString()
}

// -----------------------------------------------------------------------------
// Modifier Nodes
// -----------------------------------------------------------------------------

class PaddingModifierNode(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) : ModifierNode {
    override val opcode: Int
        get() = Operations.MODIFIER_PADDING

    override val operationName: String
        get() = "PaddingModifier"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeFloat(left)
        buffer.writeFloat(top)
        buffer.writeFloat(right)
        buffer.writeFloat(bottom)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaddingModifierNode) return false
        return left.toRawBits() == other.left.toRawBits() &&
            top.toRawBits() == other.top.toRawBits() &&
            right.toRawBits() == other.right.toRawBits() &&
            bottom.toRawBits() == other.bottom.toRawBits()
    }

    override fun hashCode(): Int = Objects.hash(left, top, right, bottom)

    override fun toString(): String = "PaddingModifierNode(l=$left, t=$top, r=$right, b=$bottom)"
}

class WidthModifierNode(val width: Float) : ModifierNode {
    override val opcode: Int
        get() = Operations.MODIFIER_WIDTH

    override val operationName: String
        get() = "WidthModifier"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeFloat(width)
    }

    override fun equals(other: Any?): Boolean =
        other is WidthModifierNode && width.toRawBits() == other.width.toRawBits()

    override fun hashCode(): Int = width.hashCode()

    override fun toString(): String = "WidthModifierNode(w=$width)"
}

class HeightModifierNode(val height: Float) : ModifierNode {
    override val opcode: Int
        get() = Operations.MODIFIER_HEIGHT

    override val operationName: String
        get() = "HeightModifier"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeFloat(height)
    }

    override fun equals(other: Any?): Boolean =
        other is HeightModifierNode && height.toRawBits() == other.height.toRawBits()

    override fun hashCode(): Int = height.hashCode()

    override fun toString(): String = "HeightModifierNode(h=$height)"
}

class BackgroundModifierNode(val color: Int) : ModifierNode {
    override val opcode: Int
        get() = Operations.MODIFIER_BACKGROUND

    override val operationName: String
        get() = "BackgroundModifier"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeInt(color)
    }

    override fun equals(other: Any?): Boolean =
        other is BackgroundModifierNode && color == other.color

    override fun hashCode(): Int = color.hashCode()

    override fun toString(): String =
        "BackgroundModifierNode(color=0x${Integer.toHexString(color)})"
}

// -----------------------------------------------------------------------------
// Canvas Command Nodes
// -----------------------------------------------------------------------------

class DrawRectNode(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) : CanvasCommandNode {
    override val opcode: Int
        get() = Operations.DRAW_RECT

    override val operationName: String
        get() = "DrawRect"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeFloat(left)
        buffer.writeFloat(top)
        buffer.writeFloat(right)
        buffer.writeFloat(bottom)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawRectNode) return false
        return left.toRawBits() == other.left.toRawBits() &&
            top.toRawBits() == other.top.toRawBits() &&
            right.toRawBits() == other.right.toRawBits() &&
            bottom.toRawBits() == other.bottom.toRawBits()
    }

    override fun hashCode(): Int = Objects.hash(left, top, right, bottom)

    override fun toString(): String = "DrawRectNode($left, $top, $right, $bottom)"
}

class DrawCircleNode(
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
) : CanvasCommandNode {
    override val opcode: Int
        get() = Operations.DRAW_CIRCLE

    override val operationName: String
        get() = "DrawCircle"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeFloat(centerX)
        buffer.writeFloat(centerY)
        buffer.writeFloat(radius)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawCircleNode) return false
        return centerX.toRawBits() == other.centerX.toRawBits() &&
            centerY.toRawBits() == other.centerY.toRawBits() &&
            radius.toRawBits() == other.radius.toRawBits()
    }

    override fun hashCode(): Int = Objects.hash(centerX, centerY, radius)

    override fun toString(): String = "DrawCircleNode(cx=$centerX, cy=$centerY, r=$radius)"
}

class DrawLineNode(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
) : CanvasCommandNode {
    override val opcode: Int
        get() = Operations.DRAW_LINE

    override val operationName: String
        get() = "DrawLine"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeFloat(x1)
        buffer.writeFloat(y1)
        buffer.writeFloat(x2)
        buffer.writeFloat(y2)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawLineNode) return false
        return x1.toRawBits() == other.x1.toRawBits() &&
            y1.toRawBits() == other.y1.toRawBits() &&
            x2.toRawBits() == other.x2.toRawBits() &&
            y2.toRawBits() == other.y2.toRawBits()
    }

    override fun hashCode(): Int = Objects.hash(x1, y1, x2, y2)

    override fun toString(): String = "DrawLineNode(($x1,$y1)->($x2,$y2))"
}

class DrawRoundRectNode(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val radiusX: Float,
    val radiusY: Float,
) : CanvasCommandNode {
    override val opcode: Int
        get() = Operations.DRAW_ROUND_RECT

    override val operationName: String
        get() = "DrawRoundRect"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeFloat(left)
        buffer.writeFloat(top)
        buffer.writeFloat(right)
        buffer.writeFloat(bottom)
        buffer.writeFloat(radiusX)
        buffer.writeFloat(radiusY)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawRoundRectNode) return false
        return left.toRawBits() == other.left.toRawBits() &&
            top.toRawBits() == other.top.toRawBits() &&
            right.toRawBits() == other.right.toRawBits() &&
            bottom.toRawBits() == other.bottom.toRawBits() &&
            radiusX.toRawBits() == other.radiusX.toRawBits() &&
            radiusY.toRawBits() == other.radiusY.toRawBits()
    }

    override fun hashCode(): Int = Objects.hash(left, top, right, bottom, radiusX, radiusY)

    override fun toString(): String =
        "DrawRoundRectNode($left,$top,$right,$bottom, rx=$radiusX, ry=$radiusY)"
}

// -----------------------------------------------------------------------------
// Resource & Data Nodes
// -----------------------------------------------------------------------------

class FloatConstantNode(val id: Int, val value: Float) : ResourceNode {
    override val opcode: Int
        get() = Operations.DATA_FLOAT

    override val operationName: String
        get() = "FloatConstant"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeInt(id)
        buffer.writeFloat(value)
    }

    override fun equals(other: Any?): Boolean =
        other is FloatConstantNode && id == other.id && value.toRawBits() == other.value.toRawBits()

    override fun hashCode(): Int = Objects.hash(id, value)

    override fun toString(): String = "FloatConstantNode(id=$id, value=$value)"
}

class IntegerConstantNode(val id: Int, val value: Int) : ResourceNode {
    override val opcode: Int
        get() = Operations.DATA_INT

    override val operationName: String
        get() = "IntegerConstant"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeInt(id)
        buffer.writeInt(value)
    }

    override fun equals(other: Any?): Boolean =
        other is IntegerConstantNode && id == other.id && value == other.value

    override fun hashCode(): Int = Objects.hash(id, value)

    override fun toString(): String = "IntegerConstantNode(id=$id, value=$value)"
}

class BooleanConstantNode(val id: Int, val value: Boolean) : ResourceNode {
    override val opcode: Int
        get() = Operations.DATA_BOOLEAN

    override val operationName: String
        get() = "BooleanConstant"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeInt(id)
        buffer.writeByte(if (value) 1 else 0)
    }

    override fun equals(other: Any?): Boolean =
        other is BooleanConstantNode && id == other.id && value == other.value

    override fun hashCode(): Int = Objects.hash(id, value)

    override fun toString(): String = "BooleanConstantNode(id=$id, value=$value)"
}

class NamedVariableNode(val varId: Int, val varType: Int, val name: String) : ResourceNode {
    override val opcode: Int
        get() = Operations.NAMED_VARIABLE

    override val operationName: String
        get() = "NamedVariable"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeInt(varId)
        buffer.writeInt(varType)
        buffer.writeUTF8(name)
    }

    override fun equals(other: Any?): Boolean =
        other is NamedVariableNode &&
            varId == other.varId &&
            varType == other.varType &&
            name == other.name

    override fun hashCode(): Int = Objects.hash(varId, varType, name)

    override fun toString(): String = "NamedVariableNode(id=$varId, type=$varType, name='$name')"
}

class TextDataNode(
    val textId: Int,
    val text: String,
) : OperationNode {
    override val opcode: Int
        get() = Operations.DATA_TEXT

    override val operationName: String
        get() = "TextData"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeInt(textId)
        buffer.writeUTF8(text)
    }

    override fun equals(other: Any?): Boolean =
        other is TextDataNode && textId == other.textId && text == other.text

    override fun hashCode(): Int = Objects.hash(textId, text)

    override fun toString(): String = "TextDataNode(textId=$textId, text='$text')"
}

class BitmapDataNode(
    val imageId: Int,
    val widthAndType: Int,
    val heightAndEncoding: Int,
    val bitmap: ByteArray,
) : OperationNode {
    override val opcode: Int
        get() = Operations.DATA_BITMAP

    override val operationName: String
        get() = "BitmapData"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
        buffer.writeInt(imageId)
        buffer.writeInt(widthAndType)
        buffer.writeInt(heightAndEncoding)
        buffer.writeBuffer(bitmap)
    }

    override fun equals(other: Any?): Boolean =
        other is BitmapDataNode &&
            imageId == other.imageId &&
            widthAndType == other.widthAndType &&
            heightAndEncoding == other.heightAndEncoding &&
            bitmap.contentEquals(other.bitmap)

    override fun hashCode(): Int =
        Objects.hash(imageId, widthAndType, heightAndEncoding, bitmap.contentHashCode())

    override fun toString(): String =
        "BitmapDataNode(imageId=$imageId, widthAndType=$widthAndType, " +
            "heightAndEncoding=$heightAndEncoding, bitmap=${bitmap.size} bytes)"
}

object ContainerEndNode : OperationNode {
    override val opcode: Int
        get() = Operations.CONTAINER_END

    override val operationName: String
        get() = "ContainerEnd"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
    }

    override fun toString(): String = "ContainerEndNode"
}

class UnknownOperationNode(override val opcode: Int) : OperationNode {
    override val operationName: String
        get() = "UnknownOperation($opcode)"

    override fun write(buffer: WireBuffer) {
        buffer.start(opcode)
    }

    override fun equals(other: Any?): Boolean =
        other is UnknownOperationNode && opcode == other.opcode

    override fun hashCode(): Int = opcode.hashCode()

    override fun toString(): String = "UnknownOperationNode(opcode=$opcode)"
}
