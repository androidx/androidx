package androidx.ui.vectorgraphics

import kotlin.text.StringBuilder

fun addPathNode(node: PathNode, target: StringBuilder): StringBuilder {
    with(target) {
        append(node.command).append(" ")
        var index = 0
        for (value in node.args) {
            append(value)
            if (index < node.args.size - 1) {
                append(", ")
            }
            index++
        }
        return this
    }
}

data class PathNode(val command: PathCommand, val args: FloatArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PathNode) return false

        if (command != other.command) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = command.hashCode()
        result = 31 * result + args.contentHashCode()
        return result
    }

    override fun toString(): String = addPathNode(this, StringBuilder()).toString()
}