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

package androidx.room.solver

import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.writeTo
import androidx.room.processor.Context
import androidx.room.testing.TestInvocation
import com.squareup.javapoet.ClassName
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

@RunWith(Parameterized::class)
class BasicColumnTypeAdaptersTest(
    val input: Input,
    val bindCode: String,
    val cursorCode: String
) {
    val scope = testCodeGenScope()

    companion object {
        val SQLITE_STMT: TypeName = ClassName.get("android.database.sqlite", "SQLiteStatement")
        val CURSOR: TypeName = ClassName.get("android.database", "Cursor")

        @Parameterized.Parameters(name = "kind:{0},bind:_{1},cursor:_{2}")
        @JvmStatic
        fun params(): List<Array<Any>> {
            return listOf(
                arrayOf(
                    Input(TypeName.INT),
                    "st.bindLong(6, inp);",
                    "out = crs.getInt(9);"
                ),
                arrayOf(
                    Input(TypeName.BYTE),
                    "st.bindLong(6, inp);",
                    "out = (byte) crs.getShort(9);"
                ),
                arrayOf(
                    Input(TypeName.SHORT),
                    "st.bindLong(6, inp);",
                    "out = crs.getShort(9);"
                ),
                arrayOf(
                    Input(TypeName.LONG),
                    "st.bindLong(6, inp);",
                    "out = crs.getLong(9);"
                ),
                arrayOf(
                    Input(TypeName.CHAR),
                    "st.bindLong(6, inp);",
                    "out = (char) crs.getInt(9);"
                ),
                arrayOf(
                    Input(TypeName.FLOAT),
                    "st.bindDouble(6, inp);",
                    "out = crs.getFloat(9);"
                ),
                arrayOf(
                    Input(TypeName.DOUBLE),
                    "st.bindDouble(6, inp);",
                    "out = crs.getDouble(9);"
                ),
                arrayOf(
                    Input(TypeName.get(String::class.java)),
                    """
                            if (inp == null) {
                              st.bindNull(6);
                            } else {
                              st.bindString(6, inp);
                            }
                    """.trimIndent(),
                    "out = crs.getString(9);"
                )
            )
        }
    }

    @Test
    fun bind() {
        simpleRun { invocation ->
            val adapter = TypeAdapterStore.create(Context(invocation.processingEnv))
                .findColumnTypeAdapter(input.getTypeMirror(invocation.processingEnv), null)!!
            adapter.bindToStmt("st", "6", "inp", scope)
            assertThat(scope.generate().toString().trim(), `is`(bindCode))
            generateCode(invocation, false)
        }.compilesWithoutError()
    }

    @Test
    fun boxedBind() {
        if (!input.typeName.isPrimitive) {
            return // no-op for those
        }
        simpleRun { invocation ->
            val adapter = TypeAdapterStore.create(Context(invocation.processingEnv))
                .findColumnTypeAdapter(
                    input.getBoxedTypeMirror(invocation.processingEnv), null
                )!!
            adapter.bindToStmt("st", "6", "inp", scope)
            assertThat(
                scope.generate().toString().trim(),
                `is`(
                    """
                    if (inp == null) {
                      st.bindNull(6);
                    } else {
                      $bindCode
                    }
                    """.trimIndent()
                )
            )
            generateCode(invocation, true)
        }.compilesWithoutError()
    }

    private fun generateCode(invocation: TestInvocation, boxed: Boolean) {
        val typeMirror = if (boxed) input.getBoxedTypeMirror(invocation.processingEnv)
        else input.getTypeMirror(invocation.processingEnv)
        val spec = TypeSpec.classBuilder("OutClass")
            .addField(FieldSpec.builder(SQLITE_STMT, "st").build())
            .addField(FieldSpec.builder(CURSOR, "crs").build())
            .addField(FieldSpec.builder(typeMirror.typeName, "out").build())
            .addField(FieldSpec.builder(typeMirror.typeName, "inp").build())
            .addMethod(
                MethodSpec.methodBuilder("foo")
                    .addCode(scope.builder().build())
                    .build()
            )
            .build()
        JavaFile.builder("foo.bar", spec).build().writeTo(
            invocation.processingEnv.filer
        )
    }

    @Test
    fun read() {
        simpleRun { invocation ->
            val adapter = TypeAdapterStore.create(Context(invocation.processingEnv))
                .findColumnTypeAdapter(input.getTypeMirror(invocation.processingEnv), null)!!
            adapter.readFromCursor("out", "crs", "9", scope)
            assertThat(scope.generate().toString().trim(), `is`(cursorCode))
            generateCode(invocation, false)
        }.compilesWithoutError()
    }

    @Test
    fun readBoxed() {
        if (!input.typeName.isPrimitive) {
            return // no-op for those
        }
        simpleRun { invocation ->
            val adapter = TypeAdapterStore.create(Context(invocation.processingEnv))
                .findColumnTypeAdapter(
                    input.getBoxedTypeMirror(invocation.processingEnv), null
                )!!
            adapter.readFromCursor("out", "crs", "9", scope)
            assertThat(
                scope.generate().toString().trim(),
                `is`(
                    """
                    if (crs.isNull(9)) {
                      out = null;
                    } else {
                      $cursorCode
                    }
                    """.trimIndent()
                )
            )
            generateCode(invocation, true)
        }.compilesWithoutError()
    }

    data class Input(val typeName: TypeName) {
        fun getTypeMirror(processingEnv: XProcessingEnv): XType {
            return processingEnv.requireType(typeName)
        }

        fun getBoxedTypeMirror(processingEnv: XProcessingEnv): XType {
            return getTypeMirror(processingEnv).boxed()
        }
    }
}
