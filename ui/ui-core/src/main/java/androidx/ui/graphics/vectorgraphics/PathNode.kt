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

package androidx.ui.graphics.vectorgraphics

import kotlin.text.StringBuilder

operator fun StringBuilder.plus(node: PathNode): StringBuilder {
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

/**
 * Represents a node within the path. A node is defined as a combination
 * of a PathCommand as well as a collection of floating point arguments
 */
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

    override fun toString(): String = StringBuilder().plus(
        this
    ).toString()
}