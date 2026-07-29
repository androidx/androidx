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

package androidx.a2ui.model.catalog

import androidx.a2ui.model.schema.A2uiConstSchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.A2uiStringSchema
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class A2uiFunctionDefinitionTest(private val returnType: A2uiFunctionReturnType) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "returnType={0}")
        fun data(): Array<A2uiFunctionReturnType> = A2uiFunctionReturnType.entries.toTypedArray()
    }

    @Test
    fun toSchema_returnsExpectedA2uiObjectSchema() {
        val testFunctionDefinition =
            object : A2uiFunctionDefinition {
                override val name: String = "email"
                override val description: String = "Validates an email address"
                override val argumentSchema: A2uiSchema = A2uiStringSchema()
                override val returnType: A2uiFunctionReturnType =
                    this@A2uiFunctionDefinitionTest.returnType
            }

        val expectedSchema =
            A2uiObjectSchema(
                description = "Validates an email address",
                properties =
                    mapOf(
                        "call" to A2uiConstSchema("email"),
                        "args" to A2uiStringSchema(),
                        "returnType" to A2uiConstSchema(returnType.value),
                    ),
                required = setOf("call", "args"),
                isAdditionalPropertiesAllowed = false,
            )

        assertThat(testFunctionDefinition.toSchema()).isEqualTo(expectedSchema)
    }
}
