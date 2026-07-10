/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.inspection.validators

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Parameter
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Parameter.Type

/**
 * DSL validator of [Parameter] data received from the compose agent. This is typically used from a
 * different validator that contains [Parameter] data.
 */
class ParameterListValidator(
    private val strings: Map<Int, String>,
    private val parameters: List<Parameter>,
) {
    private var index = 0

    /**
     * The parameter is check to have the expected [name], [type], [value]. For floating point
     * values it is possible to specify a [tolerance] which defaults to 0.01.
     */
    fun parameter(
        name: String,
        type: Type,
        value: Any,
        tolerance: Any? = null,
        block: ParameterListValidator.() -> Unit = {},
    ) {
        if (index >= parameters.size) {
            error("Only ${parameters.size} parameters found at this level")
        }
        val parameter = parameters[index++]
        assertThat(strings[parameter.name]).isEqualTo(name)
        assertThat(parameter.type).isEqualTo(type)
        when (type) {
            Type.STRING,
            Type.ITERABLE -> assertThat(strings[parameter.int32Value]).isEqualTo(value)
            Type.BOOLEAN -> assertThat(parameter.int32Value).isEqualTo(if (value == true) 1 else 0)
            Type.DOUBLE ->
                assertThat(parameter.doubleValue)
                    .isWithin(tolerance as? Double ?: 0.01)
                    .of(value as Double)
            Type.DIMENSION_DP,
            Type.DIMENSION_SP,
            Type.DIMENSION_EM,
            Type.FLOAT ->
                assertThat(parameter.floatValue)
                    .isWithin(tolerance as? Float ?: 0.01f)
                    .of(value as Float)
            Type.INT32 -> assertThat(parameter.int32Value).isEqualTo(value)
            Type.INT64 -> assertThat(parameter.int64Value).isEqualTo(value)
            Type.COLOR ->
                assertWithMessage(
                        "Expected: ${(value as Int).toString(16)}, " +
                            "actual: ${parameter.int32Value.toString(16)}"
                    )
                    .that(parameter.int32Value)
                    .isEqualTo(value)
            Type.RESOURCE -> error("todo")
            Type.LAMBDA -> error("todo")
            Type.FUNCTION_REFERENCE -> error("todo")
            else -> error("unexpected type: $type")
        }
        val validator = ParameterListValidator(strings, parameter.elementsList)
        validator.block()
    }

    /**
     * This method can be used to construct a DSL from actual data. Typical used in error messages.
     */
    fun dump(
        output: StringBuilder,
        indent: Int,
        label: String = "parameter",
        showName: Boolean = true,
    ) {
        val spaces = "    ".repeat(indent)
        for (parameter in parameters) {
            output.append("$spaces$label(")
            if (showName) {
                output.append("\"${strings[parameter.name]}\", ")
            }
            output.append("Type.").append(parameter.type.name).append(", ")
            when (parameter.type) {
                Type.STRING,
                Type.ITERABLE -> output.append("\"${strings[parameter.int32Value]}\"")
                Type.BOOLEAN -> output.append(if (parameter.int32Value != 0) "true" else "false")
                Type.DOUBLE -> output.append(parameter.doubleValue)
                Type.DIMENSION_DP,
                Type.DIMENSION_SP,
                Type.DIMENSION_EM,
                Type.FLOAT -> output.append("${parameter.floatValue}f")
                Type.INT32 -> output.append(parameter.int32Value)
                Type.INT64 -> output.append(parameter.int64Value)
                Type.COLOR -> output.append(parameter.int32Value.toString(16))
                else -> output.append("unexpected type")
            }
            output.append(")")
            if (parameter.elementsList.isNotEmpty()) {
                output.appendLine(" {")
                val validator = ParameterListValidator(strings, parameter.elementsList)
                validator.dump(output, indent + 1)
                output.append("$spaces}")
            }
            output.appendLine()
        }
    }
}
