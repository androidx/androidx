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

package androidx.compose.remote.generator.codegen

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Structured, indentation-aware source writer for non-JVM target languages (C++, Rust, TypeScript,
 * Swift, and Python), replacing manual indentation prefixes and raw `StringBuilder` concatenation.
 */
internal class CodeWriter(private val indentUnit: String = "    ") {
    private val sb = StringBuilder()
    private var indentLevel: Int = 0
    private var indentPrefix: String = ""

    private fun updatePrefix() {
        indentPrefix = indentUnit.repeat(indentLevel)
    }

    /** Appends an empty newline without trailing whitespace. */
    fun line() {
        sb.append('\n')
    }

    /**
     * Appends [text] (splitting multi-line templates on `'\n'`), prefixing each non-empty line with
     * the current indentation level.
     */
    fun line(text: String) {
        if ('\n' !in text) {
            if (text.isNotEmpty()) {
                sb.append(indentPrefix).append(text)
            }
            sb.append('\n')
            return
        }
        for (part in text.split('\n')) {
            if (part.isNotEmpty()) {
                sb.append(indentPrefix).append(part)
            }
            sb.append('\n')
        }
    }

    /** Appends [text] verbatim at column 0 (for example C++ `#if` / `#endif` guards). */
    fun rawLine(text: String) {
        sb.append(text).append('\n')
    }

    /** Increases indentation for the duration of [body]. */
    inline fun indent(body: CodeWriter.() -> Unit) {
        pushIndent()
        try {
            body()
        } finally {
            popIndent()
        }
    }

    /**
     * Emits `[header][openSuffix]`, indents [body], and emits [close] when non-null.
     *
     * Pass `openSuffix = ""`, `close = null` (or use [pyBlock]) for colon-indented blocks.
     */
    inline fun block(
        header: String,
        openSuffix: String = " {",
        close: String? = "}",
        body: CodeWriter.() -> Unit,
    ) {
        line(if (header.isEmpty()) openSuffix.trimStart() else "$header$openSuffix")
        indent(body)
        if (close != null) {
            line(close)
        }
    }

    /** Emits a Python colon-delimited indented block (`header` with trailing `:`). */
    inline fun pyBlock(header: String, body: CodeWriter.() -> Unit) {
        line(if (header.endsWith(":")) header else "$header:")
        indent(body)
    }

    @PublishedApi
    internal fun pushIndent() {
        indentLevel++
        updatePrefix()
    }

    @PublishedApi
    internal fun popIndent() {
        check(indentLevel > 0) { "CodeWriter indent underflow" }
        indentLevel--
        updatePrefix()
    }

    override fun toString(): String = sb.toString()
}

internal inline fun buildCode(
    indentUnit: String = "    ",
    block: CodeWriter.() -> Unit,
): String = CodeWriter(indentUnit).apply(block).toString()

/**
 * Shared Apache 2.0 header and optional `ktfmt` post-formatting for KotlinPoet / JavaPoet outputs.
 */
internal object GeneratedSourceFormat {
    const val APACHE_2_HEADER: String =
        """/*
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
"""

    private val ktfmtMethodAndOptions: Pair<Method, Any>? by lazy {
        try {
            val formatterClass = Class.forName("com.facebook.ktfmt.format.Formatter")
            val optionsClass = Class.forName("com.facebook.ktfmt.format.FormattingOptions")
            val kotlinlangOptions = formatterClass.getField("KOTLINLANG_FORMAT").get(null)
            val formatMethod = formatterClass.getMethod("format", optionsClass, String::class.java)
            formatMethod to kotlinlangOptions
        } catch (_: ClassNotFoundException) {
            null
        }
    }

    /**
     * Formats [kotlinSource] using `ktfmt --kotlinlang-style` when `com.facebook:ktfmt` is present
     * on the runtime classpath (e.g. during `updateGeneratedCode` and unit tests).
     */
    @Suppress("BanUncheckedReflection") // Invokes optional non-SDK JVM ktfmt formatter
    fun formatKotlin(kotlinSource: String): String {
        val pair = ktfmtMethodAndOptions ?: return kotlinSource
        return try {
            pair.first.invoke(null, pair.second, kotlinSource) as String
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e
        }
    }
}
