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

package androidx.wear.watchface

import android.util.Printer
import java.io.PrintWriter
import java.io.Writer

/**
 * Lightweight wrapper around [java.io.PrintWriter] that automatically indents newlines based
 * on internal state.
 *
 * Delays writing indent until first actual write on a newline, enabling indent modification after
 * newline.
 */
internal class IndentingPrintWriter(
    writer: Writer,
    private val singleIndent: String = "\t"
) : Printer {
    internal val writer: PrintWriter = PrintWriter(writer)

    /** Mutable version of current indent  */
    private val indentBuilder = StringBuilder()

    /** Cache of current [indentBuilder] value  */
    private var currentIndent: CharArray? = null

    /**
     * Flag indicating if we're currently sitting on an empty line, and that next write should be
     * prefixed with the current indent.
     */
    private var emptyLine = true

    /** Increases the indentation level for future lines.  */
    fun increaseIndent() {
        indentBuilder.append(singleIndent)
        currentIndent = null
    }

    /** Decreases the indentation level for future lines.  */
    fun decreaseIndent() {
        indentBuilder.delete(0, singleIndent.length)
        currentIndent = null
    }

    /** Prints `string`, followed by a newline.  */
    // Printer
    override fun println(string: String) {
        print(string)
        print("\n")
    }

    /** Prints `string`, or `"null"`  */
    fun print(string: String?) {
        val str = string ?: "null"
        write(str, 0, str.length)
    }

    /** Ensures that all pending data is sent out to the target  */
    fun flush() {
        writer.flush()
    }

    private fun write(string: String, offset: Int, count: Int) {
        val bufferEnd = offset + count
        var lineStart = offset
        var lineEnd = offset

        // March through incoming buffer looking for newlines
        while (lineEnd < bufferEnd) {
            val ch = string[lineEnd++]
            if (ch == '\n') {
                maybeWriteIndent()
                writer.write(string, lineStart, lineEnd - lineStart)
                lineStart = lineEnd
                emptyLine = true
            }
        }
        if (lineStart != lineEnd) {
            maybeWriteIndent()
            writer.write(string, lineStart, lineEnd - lineStart)
        }
    }

    private fun maybeWriteIndent() {
        if (emptyLine) {
            emptyLine = false
            if (indentBuilder.isNotEmpty()) {
                if (currentIndent == null) {
                    currentIndent = indentBuilder.toString().toCharArray()
                }
                writer.write(currentIndent, 0, currentIndent!!.size)
            }
        }
    }
}
