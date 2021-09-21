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

package androidx.room.solver

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.processor.CustomConverterProcessor
import androidx.room.solver.types.CompositeTypeConverter
import androidx.room.solver.types.CustomTypeConverterWrapper
import androidx.room.solver.types.TypeConverter
import androidx.room.testing.context
import androidx.room.vo.BuiltInConverterFlags
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TypeConverterStoreTest {
    @Test
    fun multiStepTypeConverters() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.*
            interface Type1_Super
            interface Type1 : Type1_Super
            interface Type1_Sub : Type1
            interface Type2_Super
            interface Type2 : Type2_Super
            interface Type2_Sub : Type2
            interface JumpType_1
            interface JumpType_2
            interface JumpType_3
            class MyConverters {
                @TypeConverter
                fun t1_jump1(inp : Type1): JumpType_1 = TODO()
                @TypeConverter
                fun jump1_t2_Sub(inp : JumpType_1): Type2_Sub = TODO()
                @TypeConverter
                fun jump1_t2(inp : JumpType_1): Type2 = TODO()
                @TypeConverter
                fun t1_super_jump2(inp : Type1_Super): JumpType_2 = TODO()
                @TypeConverter
                fun jump2_jump3(inp : JumpType_2): JumpType_3 = TODO()
                @TypeConverter
                fun jump2_Type2_Sub(inp : JumpType_3): Type2_Sub = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(source)) { invocation ->
            val convertersElm = invocation.processingEnv.requireTypeElement("MyConverters")
            val converters = CustomConverterProcessor(invocation.context, convertersElm)
                .process()
            val store = TypeAdapterStore.create(
                invocation.context,
                BuiltInConverterFlags.DEFAULT,
                converters.map(::CustomTypeConverterWrapper)
            )

            fun findConverter(from: String, to: String): String? {
                val input = invocation.processingEnv.requireType(from)
                val output = invocation.processingEnv.requireType(to)
                return store.findTypeConverter(
                    input = input,
                    output = output
                )?.also {
                    // validate that it makes sense to ensure test is correct
                    assertThat(output.isAssignableFrom(it.to)).isTrue()
                    assertThat(it.from.isAssignableFrom(input)).isTrue()
                }?.toSignature()
            }
            assertThat(
                findConverter("Type1", "Type2")
            ).isEqualTo(
                "Type1 -> JumpType_1 : JumpType_1 -> Type2"
            )
            assertThat(
                findConverter("Type1", "Type2_Sub")
            ).isEqualTo(
                "Type1 -> JumpType_1 : JumpType_1 -> Type2_Sub"
            )
            assertThat(
                findConverter("Type1_Super", "Type2_Super")
            ).isEqualTo(
                "Type1_Super -> JumpType_2 : JumpType_2 -> JumpType_3 : JumpType_3 -> Type2_Sub"
            )
            assertThat(
                findConverter("Type1", "Type2_Sub")
            ).isEqualTo(
                "Type1 -> JumpType_1 : JumpType_1 -> Type2_Sub"
            )
            assertThat(
                findConverter("Type1_Sub", "Type2_Sub")
            ).isEqualTo(
                "Type1 -> JumpType_1 : JumpType_1 -> Type2_Sub"
            )
            assertThat(
                findConverter("Type2", "Type2_Sub")
            ).isNull()
            assertThat(
                findConverter("Type2", "Type1")
            ).isNull()
        }
    }

    private fun TypeConverter.toSignature(): String {
        return when (this) {
            is CompositeTypeConverter -> "${conv1.toSignature()} : ${conv2.toSignature()}"
            else -> "${from.typeName} -> ${to.typeName}"
        }
    }
}