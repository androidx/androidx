/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.arch.persistence.room.solver

import android.arch.persistence.room.processor.Context
import android.arch.persistence.room.testing.TestInvocation
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import simpleRun
import testCodeGenScope
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeKind.DECLARED
import javax.lang.model.type.TypeMirror

@RunWith(Parameterized::class)
class BasicTypeConvertersTest(val input: Input, val forwardCode: String,
                                  val backwardCode: String) {
    val scope = testCodeGenScope()

    companion object {
        @Parameterized.Parameters(name = "kind:{0},bind:_{1},cursor:_{2}")
        @JvmStatic
        fun params(): List<Array<Any>> {
            return listOf(
                    arrayOf(Input(TypeKind.BOOLEAN),
                            """
                            final int _tmp;
                            _tmp = inp ? 1 : 0;
                            out = java.lang.Integer.toString(_tmp);
                            """.trimIndent(),
                            """
                            final int _tmp;
                            _tmp = java.lang.Integer.parseInt(inp);
                            out = _tmp != 0;
                            """.trimIndent()),
                    arrayOf(Input(TypeKind.INT),
                            "out = java.lang.Integer.toString(inp);",
                            "out = java.lang.Integer.parseInt(inp);"),
                    arrayOf(Input(TypeKind.BYTE),
                            "out = java.lang.Byte.toString(inp);",
                            "out = java.lang.Byte.parseByte(inp);"),
                    arrayOf(Input(TypeKind.SHORT),
                            "out = java.lang.Short.toString(inp);",
                            "out = java.lang.Short.parseShort(inp);"),
                    arrayOf(Input(TypeKind.LONG),
                            "out = java.lang.Long.toString(inp);",
                            "out = java.lang.Long.parseLong(inp);"),
                    arrayOf(Input(TypeKind.CHAR),
                            "out = java.lang.Character.toString(inp);",
                            "out = inp.charAt(0);"),
                    arrayOf(Input(TypeKind.FLOAT),
                            "out = java.lang.Float.toString(inp);",
                            "out = java.lang.Float.parseFloat(inp);"),
                    arrayOf(Input(TypeKind.DOUBLE),
                            "out = java.lang.Double.toString(inp);",
                            "out = java.lang.Double.parseDouble(inp);"),
                    arrayOf(Input(DECLARED, "java.lang.Integer"),
                            "out = inp == null ? null : java.lang.Integer.toString(inp);",
                            "out = inp == null ? null : java.lang.Integer.parseInt(inp);"),
                    arrayOf(Input(DECLARED, "java.lang.Byte"),
                            "out = inp == null ? null : java.lang.Byte.toString(inp);",
                            "out = inp == null ? null : java.lang.Byte.parseByte(inp);"),
                    arrayOf(Input(DECLARED, "java.lang.Short"),
                            "out = inp == null ? null : java.lang.Short.toString(inp);",
                            "out = inp == null ? null : java.lang.Short.parseShort(inp);"),
                    arrayOf(Input(DECLARED, "java.lang.Long"),
                            "out = inp == null ? null : java.lang.Long.toString(inp);",
                            "out = inp == null ? null : java.lang.Long.parseLong(inp);"),
                    arrayOf(Input(DECLARED, "java.lang.Float"),
                            "out = inp == null ? null : java.lang.Float.toString(inp);",
                            "out = inp == null ? null : java.lang.Float.parseFloat(inp);"),
                    arrayOf(Input(DECLARED, "java.lang.Double"),
                            "out = inp == null ? null : java.lang.Double.toString(inp);",
                            "out = inp == null ? null : java.lang.Double.parseDouble(inp);"),
                    arrayOf(Input(DECLARED, "java.lang.Character"),
                            "out = inp == null ? null : java.lang.Character.toString(inp);",
                            "out = inp == null ? null : inp.charAt(0);"),
                    arrayOf(Input(DECLARED, "java.lang.Boolean"),
                            """
                            final java.lang.Integer _tmp;
                            _tmp = inp == null ? null : (inp ? 1 : 0);
                            out = _tmp == null ? null : java.lang.Integer.toString(_tmp);
                            """.trimIndent(),
                            """
                            final java.lang.Integer _tmp;
                            _tmp = inp == null ? null : java.lang.Integer.parseInt(inp);
                            out = _tmp == null ? null : _tmp != 0;
                            """.trimIndent()))
        }
    }

    @Test
    fun forward() {
        simpleRun { invocation ->
            val stringTypeMirror = invocation.context.COMMON_TYPES.STRING
            val converter = TypeAdapterStore.create(Context(invocation.processingEnv))
                    .findTypeConverter(input.getTypeMirror(invocation.processingEnv),
                            stringTypeMirror)!!
            converter.convert("inp", "out", scope)
            assertThat(scope.generate().trim(), `is`(forwardCode))
            generateCode(invocation, input.getTypeMirror(invocation.processingEnv),
                    stringTypeMirror)
        }.compilesWithoutError()
    }

    @Test
    fun backward() {
        simpleRun { invocation ->
            val stringTypeMirror = invocation.context.COMMON_TYPES.STRING
            val converter = TypeAdapterStore.create(Context(invocation.processingEnv))
                    .findTypeConverter(stringTypeMirror,
                            input.getTypeMirror(invocation.processingEnv))!!
            converter.convert("inp", "out", scope)
            assertThat(scope.generate().trim(), `is`(backwardCode))
            generateCode(invocation, stringTypeMirror,
                    input.getTypeMirror(invocation.processingEnv))
        }.compilesWithoutError()
    }

    private fun generateCode(invocation: TestInvocation, inpType : TypeMirror,
                             outType : TypeMirror) {
        input.getTypeMirror(invocation.processingEnv)
        val spec = TypeSpec.classBuilder("OutClass")
                .addField(FieldSpec.builder(TypeName.get(outType), "out").build())
                .addField(FieldSpec.builder(TypeName.get(inpType), "inp").build())
                .addMethod(
                        MethodSpec.methodBuilder("foo")
                                .addCode(scope.builder().build())
                                .build()
                )
                .build()
        JavaFile.builder("foo.bar", spec).build().writeTo(invocation.processingEnv.filer)
    }

    data class Input(val typeKind: TypeKind, val qName : String? = null) {
        fun getTypeMirror(processingEnv: ProcessingEnvironment) : TypeMirror {
            return if (typeKind.isPrimitive) {
                processingEnv.typeUtils.getPrimitiveType(typeKind)
            } else {
                processingEnv.elementUtils.getTypeElement(qName).asType()
            }
        }
    }
}
