/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.painting.matrixutils

import androidx.ui.foundation.diagnostics.DiagnosticLevel
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.foundation.diagnostics.TextTreeConfiguration
import androidx.ui.foundation.diagnostics.kNoDefaultValue
import androidx.ui.vectormath64.Matrix4

/**
 * Property which handles [Matrix4] that represent transforms.
 */
class TransformProperty(
    name: String,
    private val value: Matrix4,
    showName: Boolean = true,
    defaultValue: Any? = kNoDefaultValue,
    level: DiagnosticLevel = DiagnosticLevel.info
) : DiagnosticsProperty<Matrix4>(
        name = name,
        value = value,
        showName = showName,
        defaultValue = defaultValue,
        level = level
) {

    // The [showName] and [level] arguments must not be null.
    init {
        assert(showName != null)
        assert(level != null)
    }

    override fun valueToString(parentConfiguration: TextTreeConfiguration?): String {
        if (parentConfiguration != null && !parentConfiguration.lineBreakProperties) {
            // Format the value on a single line to be compatible with the parent's
            // style.
            val rows = listOf(
                    value.getRow(0),
                    value.getRow(1),
                    value.getRow(2),
                    value.getRow(3)
                    )
            return "[${rows.joinToString(separator = "; ")}]"
        }
        return debugDescribeTransform(value).joinToString(separator = "\n")
    }

    /**
     * Returns a list of strings representing the given transform in a format
     * useful for [TransformProperty].
     *
     * If the argument is null, returns a list with the single string "null".
     */
    private fun debugDescribeTransform(transform: Matrix4?): List<String> {
        if (transform == null)
            return listOf("null")
        val matrix = transform.toString().split('\n').toMutableList()
        matrix.removeAt(matrix.lastIndex)
        return matrix
    }
}