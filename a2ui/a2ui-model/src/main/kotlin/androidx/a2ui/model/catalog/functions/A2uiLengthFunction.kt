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

package androidx.a2ui.model.catalog.functions

import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.A2uiFunctionReturnType
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.a2ui.model.schema.A2uiNumberSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.commontypes.A2uiDynamicStringSchema
import androidx.annotation.RestrictTo

/**
 * Validates string length limits.
 *
 * Use this [A2uiFunction] to check minimum and maximum length bounds.
 */
public class A2uiLengthFunction private constructor() : A2uiFunction {

    override val definition: A2uiFunctionDefinition
        get() = DEFINITION

    /**
     * Validates that the string length in [args] meets the required bounds.
     *
     * @param args arguments containing "value" string and optional "min" or "max" limits
     * @param executionContext context allowing to execute other functions, evaluate dynamic
     *   payloads and resolving data bindings
     * @return true if the string length is within bounds, false otherwise
     */
    override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? =
        typedExecute(
            value = A2uiFunctionArgParser.getStringArg(args, ARG_VALUE_KEY),
            min =
                if (args.containsKey(ARG_MIN_KEY)) {
                    A2uiFunctionArgParser.getIntArg(args, ARG_MIN_KEY)
                } else {
                    0
                },
            max =
                if (args.containsKey(ARG_MAX_KEY)) {
                    A2uiFunctionArgParser.getIntArg(args, ARG_MAX_KEY)
                } else {
                    Int.MAX_VALUE
                },
        )

    /**
     * Checks that the length of [value] is within [min] and [max], inclusive.
     *
     * @param value the string to check
     * @param min the minimum length, or 0 for no minimum
     * @param max the maximum length, or [Int.MAX_VALUE] for no maximum
     * @return true if the length of [value] is within the bounds, false otherwise
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public fun typedExecute(value: String, min: Int, max: Int): Boolean = value.length in min..max

    public companion object {
        /** The definition of the `length` function in the A2UI basic catalog. */
        @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
        public val DEFINITION: A2uiFunctionDefinition =
            object : A2uiFunctionDefinition {
                override val name: String = "length"

                override val description: String = """Checks string length constraints."""

                override val returnType: A2uiFunctionReturnType = A2uiFunctionReturnType.BOOLEAN

                override val argumentSchema: A2uiSchema =
                    A2uiObjectSchema(
                        properties =
                            mapOf(
                                ARG_VALUE_KEY to A2uiDynamicStringSchema.DEFAULT_INSTANCE,
                                ARG_MIN_KEY to
                                    A2uiNumberSchema(
                                        description = """The minimum allowed length."""
                                    ),
                                ARG_MAX_KEY to
                                    A2uiNumberSchema(
                                        description = """The maximum allowed length."""
                                    ),
                            ),
                        required = setOf(ARG_VALUE_KEY),
                        isAdditionalPropertiesAllowed = false,
                    )
            }

        @JvmField public val INSTANCE: A2uiLengthFunction = A2uiLengthFunction()

        private const val ARG_VALUE_KEY: String = "value"
        private const val ARG_MIN_KEY: String = "min"
        private const val ARG_MAX_KEY: String = "max"
    }
}
