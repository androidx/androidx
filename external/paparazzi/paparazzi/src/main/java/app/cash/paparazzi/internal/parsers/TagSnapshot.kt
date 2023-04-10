/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paparazzi.internal.parsers

/**
 * Derived from https://cs.android.com/android-studio/platform/tools/adt/idea/+/mirror-goog-studio-master-dev:android/src/com/android/tools/idea/rendering/parsers/TagSnapshot.java
 *
 * A snapshot of the state of an xml tag.
 *
 * Used by the rendering architecture to be able to hold a consistent view of
 * the layout files across a long rendering operation without holding read locks,
 * as well as to for example let the property sheet evaluate and paint the values
 * of properties as they were at the time of rendering, not as they are at the current
 * instant.
 */
data class TagSnapshot(
  val name: String,
  val namespace: String,
  val prefix: String?,
  val attributes: List<AttributeSnapshot>,
  val children: List<TagSnapshot>,
  val hasDeclaredAaptAttrs: Boolean = false
) {
  var next: TagSnapshot? = null

  @Suppress("unused") // Used for debugging
  fun printFormatted(): String {
    indent++
    val output = """
      |$name:
      |${pad(indent + 1)}attributes: ${print(attributes)}
      |${pad(indent + 1)}children: ${print(children)} 
    """.trimMargin()
    indent--
    return output
  }

  private fun print(children: List<*>): String {
    if (children.isEmpty()) return children.toString()

    indent++
    val output = children.joinToString(
      prefix = "[\n${pad(indent + 1)}",
      separator = "\n${pad(indent + 1)}",
      postfix = "\n${pad(indent)}]"
    )
    indent--
    return output
  }

  private fun pad(length: Int): String = "  ".repeat(length)

  companion object {
    var indent = -1
  }
}
