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

import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.writeTo
import androidx.room.processor.Context
import androidx.room.vo.BuiltInConverterFlags
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
import testCodeGenScope

@RunWith(Parameterized::class)
class BasicColumnTypeAdaptersTest(
    val input: TypeName,
    val bindCode: String,
    val cursorCode: String
) {
    companion object {
        val SQLITE_STMT: TypeName = ClassName.get("android.database.sqlite", "SQLiteStatement")
        val CURSOR: TypeName = ClassName.get("android.database", "Cursor")

        @Parameterized.Parameters(name = "kind:{0},bind:_{1},cursor:_{2}")
        @JvmStatic
        fun params(): List<Array<Any>> {
            return listOf(
                arrayOf(
                    TypeName.INT,
                    "st.bindLong(6, inp);",
                    "out = crs.getInt(9);"
                ),
                arrayOf(
                    TypeName.BYTE,
                    "st.bindLong(6, inp);",
                    "out = (byte) crs.getShort(9);"
                ),
                arrayOf(
                    TypeName.SHORT,
                    "st.bindLong(6, inp);",
                    "out = crs.getShort(9);"
                ),
                arrayOf(
                    TypeName.LONG,
                    "st.bindLong(6, inp);",
                    "out = crs.getLong(9);"
                ),
                arrayOf(
                    TypeName.CHAR,
                    "st.bindLong(6, inp);",
                    "out = (char) crs.getInt(9);"
                ),
                arrayOf(
                    TypeName.FLOAT,
                    "st.bindDouble(6, inp);",
                    "out = crs.getFloat(9);"
                ),
                arrayOf(
                    TypeName.DOUBLE,
                    "st.bindDouble(6, inp);",
                    "out = crs.getDouble(9);"
                ),
                arrayOf(
                    TypeName.get(String::class.java),
                    "st.bindString(6, inp);",
                    "out = crs.getString(9);"
                ),
                arrayOf(
                    TypeName.get(ByteArray::class.java),
                    "st.bindBlob(6, inp);",
                    "out = crs.getBlob(9);"
                )
            )
        }
    }

    @Test
    fun bind() {
        runProcessorTest { invocation ->
            val scope = testCodeGenScope()
            val type = invocation.processingEnv.requireType(input)
            val adapter = TypeAdapterStore.create(
                Context(invocation.processingEnv),
                BuiltInConverterFlags.DEFAULT
            )
                .findColumnTypeAdapter(
                    out = type,
                    affinity = null,
                    skipDefaultConverter = false
                )!!
            val expected = if (input.isAlwaysCheckedForNull()) {
                """
                if (inp == null) {
                  st.bindNull(6);
                } else {
                  $bindCode
                }
                """.trimIndent()
            } else {
                bindCode
            }
            adapter.bindToStmt("st", "6", "inp", scope)
            assertThat(scope.generate().toString().trim(), `is`(expected))
            generateCode(invocation, scope, type)
        }
    }

    @Test
    fun boxedBind() {
        runProcessorTest { invocation ->
            val scope = testCodeGenScope()
            val boxedType = invocation.processingEnv.requireType(input).boxed()
            val adapter = TypeAdapterStore.create(
                Context(invocation.processingEnv),
                BuiltInConverterFlags.DEFAULT
            ).findColumnTypeAdapter(
                out = boxedType,
                affinity = null,
                skipDefaultConverter = false
            )!!
            adapter.bindToStmt("st", "6", "inp", scope)
            val expected = if (invocation.isKsp && !input.isAlwaysCheckedForNull()) {
                bindCode
            } else {
                """
                if (inp == null) {
                  st.bindNull(6);
                } else {
                  $bindCode
                }
                """.trimIndent()
            }
            assertThat(
                scope.generate().toString().trim(),
                `is`(
                    expected
                )
            )
            generateCode(invocation, scope, boxedType)
        }
    }

    @Test
    fun nullableBind() {
        runProcessorTest { invocation ->
            val scope = testCodeGenScope()
            val nullableType = invocation.processingEnv.requireType(input).makeNullable()
            val adapter = TypeAdapterStore.create(
                Context(invocation.processingEnv),
                BuiltInConverterFlags.DEFAULT
            )
                .findColumnTypeAdapter(
                    out = nullableType,
                    affinity = null,
                    skipDefaultConverter = false
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
            generateCode(invocation, scope, nullableType)
        }
    }

    private fun generateCode(invocation: XTestInvocation, scope: CodeGenScope, type: XType) {
        if (invocation.processingEnv.findTypeElement("foo.bar.OutClass") != null) {
            // guard against multi round
            return
        }
        val spec = TypeSpec.classBuilder("OutClass")
            .addField(FieldSpec.builder(SQLITE_STMT, "st").build())
            .addField(FieldSpec.builder(CURSOR, "crs").build())
            .addField(FieldSpec.builder(type.typeName, "out").build())
            .addField(FieldSpec.builder(type.typeName, "inp").build())
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
        runProcessorTest { invocation ->
            val scope = testCodeGenScope()
            val type = invocation.processingEnv.requireType(input)
            val adapter = TypeAdapterStore.create(
                Context(invocation.processingEnv),
                BuiltInConverterFlags.DEFAULT
            ).findColumnTypeAdapter(
                out = type,
                affinity = null,
                skipDefaultConverter = false
            )!!
            val expected = if (input.isAlwaysCheckedForNull()) {
                """
                if (crs.isNull(9)) {
                  out = null;
                } else {
                  $cursorCode
                }
                """.trimIndent()
            } else {
                cursorCode
            }
            adapter.readFromCursor("out", "crs", "9", scope)
            assertThat(scope.generate().toString().trim(), `is`(expected))
            generateCode(invocation, scope, type)
        }
    }

    @Test
    fun readBoxed() {
        runProcessorTest { invocation ->
            val scope = testCodeGenScope()
            val boxedType = invocation.processingEnv.requireType(input).boxed()
            val adapter = TypeAdapterStore.create(
                Context(invocation.processingEnv),
                BuiltInConverterFlags.DEFAULT
            ).findColumnTypeAdapter(
                out = boxedType,
                affinity = null,
                skipDefaultConverter = false
            )!!
            adapter.readFromCursor("out", "crs", "9", scope)
            val expected = if (invocation.isKsp && !input.isAlwaysCheckedForNull()) {
                cursorCode
            } else {
                """
                if (crs.isNull(9)) {
                  out = null;
                } else {
                  $cursorCode
                }
                """.trimIndent()
            }
            assertThat(
                scope.generate().toString().trim(),
                `is`(
                    expected
                )
            )
            generateCode(invocation, scope, boxedType)
        }
    }

    @Test
    fun readNullable() {
        runProcessorTest { invocation ->
            val scope = testCodeGenScope()
            val nullableType = invocation.processingEnv.requireType(input).makeNullable()
            val adapter = TypeAdapterStore.create(
                Context(invocation.processingEnv),
                BuiltInConverterFlags.DEFAULT
            ).findColumnTypeAdapter(nullableType, null, false)!!
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
            generateCode(invocation, scope, nullableType)
        }
    }

    /*
     * KSP knows when a boxed primitive type is non-null but for declared types (e.g. String) we
     * still generate code that checks for null. If we start accounting for the nullability in
     * the generated code for declared types, this function should be removed from this test.
     */
    private fun TypeName.isAlwaysCheckedForNull() = !this.isPrimitive
}
