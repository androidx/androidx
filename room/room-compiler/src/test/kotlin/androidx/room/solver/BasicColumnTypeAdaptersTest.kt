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

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.compat.XConverters.toString
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.writeTo
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.SQLiteDriverTypeNames
import androidx.room.processor.Context
import androidx.room.vo.BuiltInConverterFlags
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import testCodeGenScope

@RunWith(Parameterized::class)
class BasicColumnTypeAdaptersTest(
    val input: XTypeName,
    val bindCode: String,
    val readCode: String,
) {
    companion object {

        @Parameterized.Parameters(name = "kind:{0},bind:_{1},read:_{2}")
        @JvmStatic
        fun params(): List<Array<Any>> {
            return listOf(
                arrayOf(
                    XTypeName.PRIMITIVE_INT,
                    "st.bindLong(6, inp);",
                    "out = (int) (readSt.getLong(9));",
                ),
                arrayOf(
                    XTypeName.PRIMITIVE_BYTE,
                    "st.bindLong(6, inp);",
                    "out = (byte) (readSt.getLong(9));",
                ),
                arrayOf(
                    XTypeName.PRIMITIVE_SHORT,
                    "st.bindLong(6, inp);",
                    "out = (short) (readSt.getLong(9));",
                ),
                arrayOf(
                    XTypeName.PRIMITIVE_LONG,
                    "st.bindLong(6, inp);",
                    "out = readSt.getLong(9);",
                ),
                arrayOf(
                    XTypeName.PRIMITIVE_CHAR,
                    "st.bindLong(6, inp);",
                    "out = (char) (readSt.getLong(9));",
                ),
                arrayOf(
                    XTypeName.PRIMITIVE_FLOAT,
                    "st.bindDouble(6, inp);",
                    "out = (float) (readSt.getDouble(9));",
                ),
                arrayOf(
                    XTypeName.PRIMITIVE_DOUBLE,
                    "st.bindDouble(6, inp);",
                    "out = readSt.getDouble(9);",
                ),
                arrayOf(CommonTypeNames.STRING, "st.bindText(6, inp);", "out = readSt.getText(9);"),
                arrayOf(
                    XTypeName.getArrayName(XTypeName.PRIMITIVE_BYTE),
                    "st.bindBlob(6, inp);",
                    "out = readSt.getBlob(9);",
                ),
            )
        }
    }

    @Test
    fun bind() {
        runProcessorTest { invocation ->
            val scope = testCodeGenScope()
            val type = invocation.processingEnv.requireType(input)
            val adapter =
                TypeAdapterStore.create(
                        Context(invocation.processingEnv),
                        BuiltInConverterFlags.DEFAULT,
                    )
                    .findColumnTypeAdapter(
                        out = type,
                        affinity = null,
                        skipDefaultConverter = false,
                    )!!
            val expected =
                if (invocation.isKsp || input.isPrimitive) {
                    bindCode
                } else {
                    """
                if (inp == null) {
                  st.bindNull(6);
                } else {
                  $bindCode
                }
                """
                        .trimIndent()
                }
            adapter.bindToStmt("st", "6", "inp", scope)
            assertThat(scope.generate().toString(CodeLanguage.JAVA).trim(), `is`(expected))
            generateCode(invocation, scope, type)
        }
    }

    @Test
    fun boxedBind() {
        runProcessorTest { invocation ->
            val scope = testCodeGenScope()
            val boxedType = invocation.processingEnv.requireType(input).boxed()
            val adapter =
                TypeAdapterStore.create(
                        Context(invocation.processingEnv),
                        BuiltInConverterFlags.DEFAULT,
                    )
                    .findColumnTypeAdapter(
                        out = boxedType,
                        affinity = null,
                        skipDefaultConverter = false,
                    )!!
            adapter.bindToStmt("st", "6", "inp", scope)
            val expected =
                if (invocation.isKsp) {
                    bindCode
                } else {
                    """
                if (inp == null) {
                  st.bindNull(6);
                } else {
                  $bindCode
                }
                """
                        .trimIndent()
                }
            assertThat(scope.generate().toString(CodeLanguage.JAVA).trim(), `is`(expected))
            generateCode(invocation, scope, boxedType)
        }
    }

    @Test
    fun nullableBind() {
        runProcessorTest { invocation ->
            val scope = testCodeGenScope()
            val nullableType = invocation.processingEnv.requireType(input).makeNullable()
            val adapter =
                TypeAdapterStore.create(
                        Context(invocation.processingEnv),
                        BuiltInConverterFlags.DEFAULT,
                    )
                    .findColumnTypeAdapter(
                        out = nullableType,
                        affinity = null,
                        skipDefaultConverter = false,
                    )!!
            adapter.bindToStmt("st", "6", "inp", scope)
            assertThat(
                scope.generate().toString(CodeLanguage.JAVA).trim(),
                `is`(
                    """
                    if (inp == null) {
                      st.bindNull(6);
                    } else {
                      $bindCode
                    }
                    """
                        .trimIndent()
                ),
            )
            generateCode(invocation, scope, nullableType)
        }
    }

    private fun generateCode(invocation: XTestInvocation, scope: CodeGenScope, type: XType) {
        if (invocation.processingEnv.findTypeElement("foo.bar.OutClass") != null) {
            // guard against multi round
            return
        }
        val className = XClassName.get("foo.bar", "OuterClass")
        XTypeSpec.classBuilder(className)
            .apply {
                addProperty(
                    XPropertySpec.builder(
                            name = "st",
                            typeName = SQLiteDriverTypeNames.STATEMENT,
                            visibility = VisibilityModifier.PUBLIC,
                            isMutable = true,
                        )
                        .build()
                )
                addProperty(
                    XPropertySpec.builder(
                            name = "readSt",
                            typeName = SQLiteDriverTypeNames.STATEMENT,
                            visibility = VisibilityModifier.PUBLIC,
                            isMutable = true,
                        )
                        .build()
                )
                addProperty(
                    XPropertySpec.builder(
                            name = "out",
                            typeName = type.asTypeName(),
                            visibility = VisibilityModifier.PUBLIC,
                            isMutable = true,
                        )
                        .build()
                )
                addProperty(
                    XPropertySpec.builder(
                            name = "inp",
                            typeName = type.asTypeName(),
                            visibility = VisibilityModifier.PUBLIC,
                            isMutable = true,
                        )
                        .build()
                )
                addFunction(
                    XFunSpec.builder("foo", VisibilityModifier.PUBLIC)
                        .addCode(scope.generate())
                        .build()
                )
            }
            .build()
            .writeTo(CodeLanguage.JAVA, className.packageName, invocation.processingEnv.filer)
    }

    @Test
    fun read() {
        runProcessorTest { invocation ->
            val scope = testCodeGenScope()
            val type = invocation.processingEnv.requireType(input)
            val adapter =
                TypeAdapterStore.create(
                        Context(invocation.processingEnv),
                        BuiltInConverterFlags.DEFAULT,
                    )
                    .findColumnTypeAdapter(
                        out = type,
                        affinity = null,
                        skipDefaultConverter = false,
                    )!!
            val expected =
                if (invocation.isKsp || input.isPrimitive) {
                    readCode
                } else {
                    """
                if (readSt.isNull(9)) {
                  out = null;
                } else {
                  $readCode
                }
                """
                        .trimIndent()
                }
            adapter.readFromStatement("out", "readSt", "9", scope)
            assertThat(scope.generate().toString(CodeLanguage.JAVA).trim(), `is`(expected))
            generateCode(invocation, scope, type)
        }
    }

    @Test
    fun readBoxed() {
        runProcessorTest { invocation ->
            val scope = testCodeGenScope()
            val boxedType = invocation.processingEnv.requireType(input).boxed()
            val adapter =
                TypeAdapterStore.create(
                        Context(invocation.processingEnv),
                        BuiltInConverterFlags.DEFAULT,
                    )
                    .findColumnTypeAdapter(
                        out = boxedType,
                        affinity = null,
                        skipDefaultConverter = false,
                    )!!
            adapter.readFromStatement("out", "readSt", "9", scope)
            val expected =
                if (invocation.isKsp) {
                    readCode
                } else {
                    """
                if (readSt.isNull(9)) {
                  out = null;
                } else {
                  $readCode
                }
                """
                        .trimIndent()
                }
            assertThat(scope.generate().toString(CodeLanguage.JAVA).trim(), `is`(expected))
            generateCode(invocation, scope, boxedType)
        }
    }

    @Test
    fun readNullable() {
        runProcessorTest { invocation ->
            val scope = testCodeGenScope()
            val nullableType = invocation.processingEnv.requireType(input).makeNullable()
            val adapter =
                TypeAdapterStore.create(
                        Context(invocation.processingEnv),
                        BuiltInConverterFlags.DEFAULT,
                    )
                    .findColumnTypeAdapter(nullableType, null, false)!!
            adapter.readFromStatement("out", "readSt", "9", scope)
            assertThat(
                scope.generate().toString(CodeLanguage.JAVA).trim(),
                `is`(
                    """
                    if (readSt.isNull(9)) {
                      out = null;
                    } else {
                      $readCode
                    }
                    """
                        .trimIndent()
                ),
            )
            generateCode(invocation, scope, nullableType)
        }
    }
}
