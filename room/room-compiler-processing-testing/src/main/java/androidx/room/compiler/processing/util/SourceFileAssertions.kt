/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.util

/**
 * Compares two source files and returns the line that does not match.
 *
 * It ignores all indentation and empty lines but is incapable of omitting comments etc (like
 * java compile testing does).
 */
internal fun Source.findMismatch(other: Source): SourceFileMismatch? {
    val myLines = contents.lineSequence().nonEmptySourceLines()
    val otherLines = other.contents.lineSequence().nonEmptySourceLines()
    do {
        val myLine = myLines.nextOrNull()
        val otherLine = otherLines.nextOrNull()
        if (myLine?.content != otherLine?.content) {
            return SourceFileMismatch(
                myLine,
                otherLine
            )
        }
    } while (myLine != null || otherLine != null)
    return null
}

/**
 * Associate each line with an index ([Line]) while also dropping empty lines and trimming each
 * line.
 */
private fun Sequence<String>.nonEmptySourceLines() =
    map {
        it.trim()
    }.mapIndexed { index, content ->
        Line(index + 1, content)
    }.filterNot {
        it.content.isNullOrBlank()
    }.iterator()

private fun <T> Iterator<T>.nextOrNull(): T? = if (this.hasNext()) {
    next()
} else {
    null
}

internal data class SourceFileMismatch(
    val expected: Line?,
    val actual: Line?
)

internal data class Line(
    val pos: Int,
    val content: String?
)
