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

package androidx.room.parser

import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SQLTypeAffinityTest {
    /**
     * This is more of a visual verification test to see what values are returned.
     *
     * In javac implementation, nullability does not matter and boxed types may be nullable or
     * platform type (depends if they are created by boxing a primitive)
     * In ksp implementation, nullability matters.
     */
    @Test
    fun affinityTypes() {
        runProcessorTest(sources = emptyList()) { invocation ->
            fun XNullability.toSignature() = if (invocation.isKsp) {
                when (this) {
                    XNullability.NONNULL -> "!!"
                    XNullability.NULLABLE -> "?"
                    XNullability.UNKNOWN -> ""
                }
            } else {
                // in javac, type's nullability does not mean anything for type equality.
                ""
            }

            fun XType.toSignature(): String {
                return "${typeName}${nullability.toSignature()}"
            }

            val result = SQLTypeAffinity.values().associate {
                it to it.getTypeMirrors(invocation.processingEnv).map(XType::toSignature)
            }
            assertThat(result).containsExactlyEntriesIn(
                if (invocation.isKsp) KSP_MAPPING
                else JAVAC_MAPPING
            )
        }
    }

    companion object {
        private val KSP_MAPPING = mapOf(
            SQLTypeAffinity.NULL to listOf(),
            SQLTypeAffinity.TEXT to listOf(
                "java.lang.String!!",
                "java.lang.String?"
            ),
            SQLTypeAffinity.BLOB to listOf(
                "byte[]!!",
                "byte[]?"
            ),
            SQLTypeAffinity.INTEGER to listOf(
                "int!!",
                "java.lang.Integer!!",
                "java.lang.Integer?",
                "byte!!",
                "java.lang.Byte!!",
                "java.lang.Byte?",
                "char!!",
                "java.lang.Character!!",
                "java.lang.Character?",
                "long!!",
                "java.lang.Long!!",
                "java.lang.Long?",
                "short!!",
                "java.lang.Short!!",
                "java.lang.Short?"
            ),
            SQLTypeAffinity.REAL to listOf(
                "double!!",
                "java.lang.Double!!",
                "java.lang.Double?",
                "float!!",
                "java.lang.Float!!",
                "java.lang.Float?",
            )
        )
        private val JAVAC_MAPPING = mapOf(
            SQLTypeAffinity.NULL to listOf(),
            SQLTypeAffinity.TEXT to listOf(
                "java.lang.String"
            ),
            SQLTypeAffinity.BLOB to listOf(
                "byte[]"
            ),
            SQLTypeAffinity.INTEGER to listOf(
                "int",
                "java.lang.Integer",
                "byte",
                "java.lang.Byte",
                "char",
                "java.lang.Character",
                "long",
                "java.lang.Long",
                "short",
                "java.lang.Short"
            ),
            SQLTypeAffinity.REAL to listOf(
                "double",
                "java.lang.Double",
                "float",
                "java.lang.Float",
            )
        )
    }
}