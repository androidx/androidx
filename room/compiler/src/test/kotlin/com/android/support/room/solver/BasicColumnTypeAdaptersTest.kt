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

package com.android.support.room.solver

import com.android.support.room.testing.TestInvocation
import com.squareup.javapoet.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import simpleRun
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

@RunWith(Parameterized::class)
class BasicColumnTypeAdaptersTest(val input: Input, val bindCode: String,
                                  val cursorCode: String) {
    val scope = CodeGenScope()

    companion object {
        val SQLITE_STMT : TypeName = ClassName.get("android.database.sqlite", "SQLiteStatement")
        val CURSOR: TypeName = ClassName.get("android.database", "Cursor")

        @Parameterized.Parameters(name = "kind:{0},bind:_{1},cursor:_{2}")
        @JvmStatic
        fun params(): List<Array<Any>> {
            return listOf(
                    arrayOf(Input(TypeKind.INT),
                            "st.bindLong(6, inp);",
                            "out = crs.getInt(9);"),
                    arrayOf(Input(TypeKind.BYTE),
                            "st.bindLong(6, inp);",
                            "out = (byte) crs.getShort(9);"),
                    arrayOf(Input(TypeKind.SHORT),
                            "st.bindLong(6, inp);",
                            "out = crs.getShort(9);"),
                    arrayOf(Input(TypeKind.LONG),
                            "st.bindLong(6, inp);",
                            "out = crs.getLong(9);"),
                    arrayOf(Input(TypeKind.CHAR),
                            "st.bindLong(6, inp);",
                            "out = (char) crs.getInt(9);"),
                    arrayOf(Input(TypeKind.FLOAT),
                            "st.bindDouble(6, inp);",
                            "out = crs.getFloat(9);"),
                    arrayOf(Input(TypeKind.DOUBLE),
                            "st.bindDouble(6, inp);",
                            "out = crs.getDouble(9);"),
                    arrayOf(Input(TypeKind.DECLARED, "java.lang.String"),
                            """
                            if (inp == null) {
                              st.bindNull(6);
                            } else {
                              st.bindString(6, inp);
                            }
                            """.trimIndent(),
                            "out = crs.isNull(9) ? null : crs.getString(9);")
            )
        }
    }

    @Test
    fun bind() {
        simpleRun { invocation ->
            val adapter = TypeAdapterStore(invocation.roundEnv, invocation.processingEnv)
                    .getAdapterFor(input.getTypeMirror(invocation.processingEnv))!!
            adapter.bindToStmt("st", 6, "inp", scope)
            assertThat(scope.generate().trim(), `is`(bindCode))
            generateCode(invocation)
        }.compilesWithoutError()
    }

    private fun generateCode(invocation: TestInvocation) {
        val typeMirror = input.getTypeMirror(invocation.processingEnv)
        val spec = TypeSpec.classBuilder("OutClass")
                .addField(FieldSpec.builder(SQLITE_STMT, "st").build())
                .addField(FieldSpec.builder(CURSOR, "crs").build())
                .addField(FieldSpec.builder(TypeName.get(typeMirror), "out").build())
                .addField(FieldSpec.builder(TypeName.get(typeMirror), "inp").build())
                .addMethod(
                        MethodSpec.methodBuilder("foo")
                                .addCode(scope.builder().build())
                                .build()
                )
                .build()
        JavaFile.builder("foo.bar", spec).build().writeTo(invocation.processingEnv.filer)
    }

    @Test
    fun read() {
        simpleRun { invocation ->
            val adapter = TypeAdapterStore(invocation.roundEnv, invocation.processingEnv)
                    .getAdapterFor(input.getTypeMirror(invocation.processingEnv))!!
            adapter.readFromCursor("out", "crs", 9, scope)
            assertThat(scope.generate().trim(), `is`(cursorCode))
            generateCode(invocation)
        }.compilesWithoutError()
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
