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

package androidx.room3.solver

import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.VisibilityModifier
import androidx.room3.compiler.codegen.XClassName
import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XFunSpec
import androidx.room3.compiler.codegen.XPropertySpec
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.codegen.XTypeSpec
import androidx.room3.compiler.codegen.compat.XConverters.toString
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.util.XTestInvocation
import androidx.room3.compiler.processing.util.runKspTest
import androidx.room3.compiler.processing.writeTo
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.SQLiteDriverTypeNames
import androidx.room3.processor.Context
import androidx.room3.vo.BuiltInConverterFlags
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
                    "st.bindLong(6, inp.toLong())",
                    "out = readSt.getLong(9).toInt()",
                ),
                arrayOf(
                    XTypeName.PRIMITIVE_BYTE,
                    "st.bindLong(6, inp.toLong())",
                    "out = readSt.getLong(9).toByte()",
                ),
                arrayOf(
                    XTypeName.PRIMITIVE_SHORT,
                    "st.bindLong(6, inp.toLong())",
                    "out = readSt.getLong(9).toShort()",
                ),
                arrayOf(XTypeName.PRIMITIVE_LONG, "st.bindLong(6, inp)", "out = readSt.getLong(9)"),
                arrayOf(
                    XTypeName.PRIMITIVE_CHAR,
                    "st.bindLong(6, inp.toLong())",
                    "out = readSt.getLong(9).toInt().toChar()",
                ),
                arrayOf(
                    XTypeName.PRIMITIVE_FLOAT,
                    "st.bindDouble(6, inp.toDouble())",
                    "out = readSt.getDouble(9).toFloat()",
                ),
                arrayOf(
                    XTypeName.PRIMITIVE_DOUBLE,
                    "st.bindDouble(6, inp)",
                    "out = readSt.getDouble(9)",
                ),
                arrayOf(CommonTypeNames.STRING, "st.bindText(6, inp)", "out = readSt.getText(9)"),
                arrayOf(
                    XTypeName.getArrayName(XTypeName.PRIMITIVE_BYTE),
                    "st.bindBlob(6, inp)",
                    "out = readSt.getBlob(9)",
                ),
            )
        }
    }

    @Test
    fun bind() {
        runKspTest(sources = emptyList()) { invocation ->
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
            adapter.bindToStmt("st", "6", "inp", scope)
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim(), `is`(bindCode))
            generateCode(invocation, scope, type)
        }
    }

    @Test
    fun boxedBind() {
        runKspTest(sources = emptyList()) { invocation ->
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
            val expected = bindCode
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim(), `is`(expected))
            generateCode(invocation, scope, boxedType)
        }
    }

    @Test
    fun nullableBind() {
        runKspTest(sources = emptyList()) { invocation ->
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
            val nullableTypeName = nullableType.asTypeName()
            val tmpProperty = scope.getTmpVar("_tmp")
            scope.builder.addLocalVal(tmpProperty, nullableTypeName, "inp")
            adapter.bindToStmt("st", "6", tmpProperty, scope)
            assertThat(
                scope.generate().toString(CodeLanguage.KOTLIN).trim(),
                `is`(
                    """
                    val _tmp: ${nullableTypeName.toString(CodeLanguage.KOTLIN)} = inp
                    if (_tmp == null) {
                      st.bindNull(6)
                    } else {
                      ${bindCode.replace("inp", tmpProperty)}
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
                val properties =
                    listOf(
                        XPropertySpec.builder(
                                name = "st",
                                typeName = SQLiteDriverTypeNames.STATEMENT,
                                visibility = VisibilityModifier.PUBLIC,
                                isMutable = false,
                            )
                            .initializer(XCodeBlock.of("st"))
                            .build(),
                        XPropertySpec.builder(
                                name = "readSt",
                                typeName = SQLiteDriverTypeNames.STATEMENT,
                                visibility = VisibilityModifier.PUBLIC,
                                isMutable = false,
                            )
                            .initializer(XCodeBlock.of("readSt"))
                            .build(),
                        XPropertySpec.builder(
                                name = "out",
                                typeName = type.asTypeName(),
                                visibility = VisibilityModifier.PUBLIC,
                                isMutable = true,
                            )
                            .initializer(XCodeBlock.of("out"))
                            .build(),
                        XPropertySpec.builder(
                                name = "inp",
                                typeName = type.asTypeName(),
                                visibility = VisibilityModifier.PUBLIC,
                                isMutable = true,
                            )
                            .initializer(XCodeBlock.of("inp"))
                            .build(),
                    )
                properties.forEach { addProperty(it) }
                setPrimaryConstructor(
                    XFunSpec.constructorBuilder(VisibilityModifier.PUBLIC)
                        .apply { properties.forEach { addParameter(it.name, it.type) } }
                        .build()
                )
                addFunction(
                    XFunSpec.builder("foo", VisibilityModifier.PUBLIC)
                        .addCode(scope.generate())
                        .build()
                )
            }
            .build()
            .writeTo(CodeLanguage.KOTLIN, className.packageName, invocation.processingEnv.filer)
    }

    @Test
    fun read() {
        runKspTest(sources = emptyList()) { invocation ->
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
            adapter.readFromStatement("out", "readSt", "9", scope)
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim(), `is`(readCode))
            generateCode(invocation, scope, type)
        }
    }

    @Test
    fun readBoxed() {
        runKspTest(sources = emptyList()) { invocation ->
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
            val expected = readCode
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim(), `is`(expected))
            generateCode(invocation, scope, boxedType)
        }
    }

    @Test
    fun readNullable() {
        runKspTest(sources = emptyList()) { invocation ->
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
                scope.generate().toString(CodeLanguage.KOTLIN).trim(),
                `is`(
                    """
                    if (readSt.isNull(9)) {
                      out = null
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
