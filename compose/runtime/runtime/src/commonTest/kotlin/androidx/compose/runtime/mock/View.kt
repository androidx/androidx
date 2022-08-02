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

package androidx.compose.runtime.mock

import androidx.compose.runtime.snapshots.fastForEach

fun indent(indent: Int, builder: StringBuilder) {
    repeat(indent) { builder.append(' ') }
}

open class View {
    var name: String = ""
    val children = mutableListOf<View>()
    val attributes = mutableMapOf<String, Any>()

    // Used to validated insert/remove constraints
    private var parent: View? = null

    private fun render(indent: Int = 0, builder: StringBuilder) {
        indent(indent, builder)
        builder.append("<$name$attributesAsString")
        if (children.size > 0) {
            builder.appendLine(">")
            children.forEach { it.render(indent + 2, builder) }
            indent(indent, builder)
            builder.appendLine("</$name>")
        } else {
            builder.appendLine(" />")
        }
    }

    fun addAt(index: Int, view: View) {
        if (view.parent != null) {
            error(
                "Inserting a view named ${view.name} already has a parent into a view named $name"
            )
        }
        view.parent = this
        children.add(index, view)
    }

    fun removeAt(index: Int, count: Int) {
        if (index < children.count()) {
            if (count == 1) {
                val removedChild = children.removeAt(index)
                removedChild.parent = null
            } else {
                val removedChildren = children.subList(index, index + count)
                removedChildren.fastForEach { child -> child.parent = null }
                removedChildren.clear()
            }
        }
    }

    fun moveAt(from: Int, to: Int, count: Int) {
        if (count == 1) {
            val insertLocation = if (from > to) to else (to - 1)
            children.add(insertLocation, children.removeAt(from))
        } else {
            val insertLocation = if (from > to) to else (to - count)
            val itemsToMove = children.subList(from, from + count)
            val copyOfItems = itemsToMove.map { it }
            itemsToMove.clear()
            children.addAll(insertLocation, copyOfItems)
        }
    }

    fun attribute(name: String, value: Any) { attributes[name] = value }

    var value: String?
        get() = attributes["value"] as? String
        set(value) {
            if (value != null) {
                attributes["value"] = value
            } else {
                attributes.remove("value")
            }
        }

    var text: String?
        get() = attributes["text"] as? String
        set(value) {
            if (value != null) {
                attributes["text"] = value
            } else {
                attributes.remove("text")
            }
        }

    private val attributesAsString get() =
        if (attributes.isEmpty()) ""
        else attributes.map { " ${it.key}='${it.value}'" }.joinToString()
    private val childrenAsString: String get() =
        children.map { it.toString() }.joinToString(" ")

    override fun toString() =
            if (children.isEmpty()) "<$name$attributesAsString>" else
            "<$name$attributesAsString>$childrenAsString</$name>"

    fun toFmtString() = StringBuilder().let {
        render(0, it)
        it.toString()
    }

    fun findFirstOrNull(predicate: (view: View) -> Boolean): View? {
        if (predicate(this)) return this
        for (child in children) {
            child.findFirstOrNull(predicate)?.let { return it }
        }
        return null
    }

    fun findFirst(predicate: (view: View) -> Boolean) =
        findFirstOrNull(predicate) ?: error("View not found")
}
fun View.flatten(): List<View> = listOf(this) + children.flatMap { it.flatten() }