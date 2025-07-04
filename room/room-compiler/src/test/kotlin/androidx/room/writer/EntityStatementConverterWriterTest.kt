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

package androidx.room.writer

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.compat.XConverters.applyToJavaPoet
import androidx.room.compiler.processing.XProcessingEnv.Platform
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.processor.BaseEntityParserTest
import javax.lang.model.element.Modifier
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EntityStatementConverterWriterTest : BaseEntityParserTest() {
    companion object {
        val OUT_PREFIX =
            """
            package foo.bar;
            import androidx.annotation.NonNull;
            import androidx.room.util.SQLiteStatementUtil;
            import androidx.sqlite.SQLiteStatement;
            import java.lang.SuppressWarnings;
            import javax.annotation.processing.Generated;
            @Generated("androidx.room.RoomProcessor")
            @SuppressWarnings({"unchecked", "deprecation", "removal"})
            public final class MyContainerClass {
        """
                .trimIndent()
        const val OUT_SUFFIX = "}"
    }

    @Test
    fun generateSimple() {
        generateAndMatch(
            input =
                """
                @PrimaryKey
                private int id;
                String name;
                String lastName;
                int age;
                public int getId() { return id; }
                public void setId(int id) { this.id = id; }
                """
                    .trimIndent(),
            output = {
                fun stringAdapterCode(out: String, indexVar: String) =
                    """
                    if (statement.isNull($indexVar)) {
                      $out = null;
                    } else {
                      $out = statement.getText($indexVar);
                    }
                    """
                        .trimIndent()
                """
                |private MyEntity __entityStatementConverter_fooBarMyEntity(
                |@NonNull final SQLiteStatement statement) {
                |  final MyEntity _entity;
                |  final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndex(statement, "id");
                |  final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndex(statement, "name");
                |  final int _columnIndexOfLastName = SQLiteStatementUtil.getColumnIndex(statement, "lastName");
                |  final int _columnIndexOfAge = SQLiteStatementUtil.getColumnIndex(statement, "age");
                |  _entity = new MyEntity();
                |  if (_columnIndexOfId != -1) {
                |    final int _tmpId;
                |    _tmpId = (int) (statement.getLong(_columnIndexOfId));
                |    _entity.setId(_tmpId);
                |  }
                |  if (_columnIndexOfName != -1) {
                |    ${stringAdapterCode("_entity.name", "_columnIndexOfName")}
                |  }
                |  if (_columnIndexOfLastName != -1) {
                |    ${stringAdapterCode("_entity.lastName", "_columnIndexOfLastName")}
                |  }
                |  if (_columnIndexOfAge != -1) {
                |    _entity.age = (int) (statement.getLong(_columnIndexOfAge));
                |  }
                |  return _entity;
                |}
                """
                    .trimMargin()
            },
        )
    }

    private fun generateAndMatch(input: String, output: (Boolean) -> String) {
        generate(input) {
            it.assertCompilationResult {
                generatedSource(
                    Source.java(
                        qName = "foo.bar.MyContainerClass",
                        code = listOf(OUT_PREFIX, output(it.isKsp), OUT_SUFFIX).joinToString("\n"),
                    )
                )
            }
        }
    }

    private fun generate(input: String, handler: (XTestInvocation) -> Unit) {
        singleEntity(input) { entity, invocation ->
            val className = XClassName.get("foo.bar", "MyContainerClass")
            val writer =
                object : TypeWriter(WriterContext(CodeLanguage.JAVA, setOf(Platform.JVM), true)) {
                    override val packageName = className.packageName

                    override fun createTypeSpecBuilder(): XTypeSpec.Builder {
                        getOrCreateFunction(EntityStatementConverterWriter(entity))
                        return XTypeSpec.classBuilder(className).applyToJavaPoet {
                            addModifiers(Modifier.PUBLIC)
                        }
                    }
                }
            writer.write(invocation.processingEnv)
            handler(invocation)
        }
    }
}
