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

package androidx.ui.tooling.inspector

/**
 * Node representing a Composable for the Layout Inspector.
 */
class InspectorNode(
    /**
     * The associated render node id or 0.
     */
    val id: Long,

    /**
     * The name of the Composable.
     */
    val name: String,

    /**
     * The fileName where the Composable was called.
     */
    val fileName: String,

    /**
     * The line number where the Composable was called.
     */
    val lineNumber: Int,

    /**
     * The name of the function that called the Composable.
     */
    val functionName: String,

    /**
     * Left side of the Composable in pixels.
     */
    val left: Int,

    /**
     * Top of the Composable in pixels.
     */
    val top: Int,

    /**
     * Width of the Composable in pixels.
     */
    val width: Int,

    /**
     * Width of the Composable in pixels.
     */
    val height: Int,

    /**
     * The children nodes of this Composable.
     */
    val children: List<InspectorNode>
)

/**
 * Mutable version of [InspectorNode].
 */
internal class MutableInspectorNode {
    var id = 0L
    var name = ""
    var fileName = ""
    var lineNumber = 0
    var functionName = ""
    var left = 0
    var top = 0
    var width = 0
    var height = 0
    val children = mutableListOf<InspectorNode>()

    fun reset() {
        resetExceptIdAndChildren()
        id = 0L
        children.clear()
    }

    fun resetExceptIdAndChildren() {
        name = ""
        fileName = ""
        lineNumber = 0
        functionName = ""
        left = 0
        top = 0
        width = 0
        height = 0
    }

    fun build(): InspectorNode =
        InspectorNode(
            id, name, fileName, lineNumber, functionName, left, top, width, height,
            children.toList()
        )
}
