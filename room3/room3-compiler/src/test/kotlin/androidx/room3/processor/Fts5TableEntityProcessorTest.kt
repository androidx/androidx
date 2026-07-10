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

package androidx.room3.processor

import androidx.room3.FtsOptions
import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.runKspTest
import androidx.room3.parser.FtsVersion
import androidx.room3.parser.SQLTypeAffinity
import androidx.room3.testing.context
import androidx.room3.vo.CallType
import androidx.room3.vo.Properties
import androidx.room3.vo.Property
import androidx.room3.vo.PropertyGetter
import androidx.room3.vo.PropertySetter
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class Fts5TableEntityProcessorTest : BaseFtsEntityParserTest() {

    override fun getFtsVersion() = 5

    @Test
    fun simple() {
        singleEntity(
            """
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                private int rowId;
                public int getRowId() { return rowId; }
                public void setRowId(int rowId) { this.rowId = rowId; }
            """
        ) { entity, invocation ->
            assertThat(
                entity.type.asTypeName().toString(CodeLanguage.JAVA),
                `is`("foo.bar.MyEntity"),
            )
            assertThat(entity.properties.size, `is`(1))
            val field = entity.properties.first()
            val intType = invocation.processingEnv.requireType(XTypeName.PRIMITIVE_INT)
            assertThat(
                field,
                `is`(
                    Property(
                        element = field.element,
                        name = "rowId",
                        type = intType,
                        columnName = "rowid",
                        affinity = SQLTypeAffinity.INTEGER,
                    )
                ),
            )
            assertThat(
                field.setter,
                `is`(PropertySetter("rowId", "setRowId", intType, CallType.FUNCTION)),
            )
            assertThat(
                field.getter,
                `is`(PropertyGetter("rowId", "getRowId", intType, CallType.FUNCTION)),
            )
            assertThat(entity.primaryKey.properties, `is`(Properties(field)))
            assertThat(entity.shadowTableName, `is`("MyEntity_content"))
            assertThat(entity.ftsVersion, `is`(FtsVersion.FTS5))
        }
    }

    @Test
    fun missingEntityAnnotation() {
        runKspTest(
            sources =
                listOf(
                    Source.java(
                        "foo.bar.MyEntity",
                        """
                        package foo.bar;
                        import androidx.room3.*;
                        @Fts5
                        public class MyEntity {
                            public String content;
                        }
                        """,
                    )
                )
        ) { invocation ->
            val entity = invocation.processingEnv.requireTypeElement("foo.bar.MyEntity")
            FtsTableEntityProcessor(invocation.context, entity).process()
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.ENTITY_MUST_BE_ANNOTATED_WITH_ENTITY)
            }
        }
    }

    @Test
    fun contentRowIdWithoutContentEntity() {
        singleEntity(
            """
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                public int rowId;
                public String body;
                """,
            ftsAttributes = hashMapOf("contentRowId" to "\"rowId\""),
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasError(
                    "Cannot declare a 'contentRowId' without also declaring an external content entity class."
                )
            }
        }
    }

    @Test
    fun columnSize() {
        singleEntity(
            """
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                public int rowId;
                public String body;
                """,
            ftsAttributes = hashMapOf("hasColumnSize" to "false"),
        ) { entity, _ ->
            assertThat(entity.ftsOptions.columnSize, `is`(false))
        }
    }

    @Test
    fun detail() {
        singleEntity(
            """
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                public int rowId;
                public String body;
                """,
            ftsAttributes = hashMapOf("detail" to "FtsOptions.Detail.COLUMN"),
        ) { entity, _ ->
            assertThat(entity.ftsOptions.detail, `is`(FtsOptions.Detail.COLUMN))
        }
    }

    @Test
    fun trigramTokenizer() {
        singleEntity(
            """
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                public int rowId;
                public String body;
                """,
            ftsAttributes = hashMapOf("tokenizer" to "FtsOptions.TOKENIZER_TRIGRAM"),
        ) { entity, _ ->
            assertThat(entity.ftsOptions.tokenizer, `is`(FtsOptions.TOKENIZER_TRIGRAM))
        }
    }

    @Test
    fun trigramTokenizerWithArgs() {
        singleEntity(
            """
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                public int rowId;
                public String body;
                """,
            ftsAttributes =
                hashMapOf(
                    "tokenizer" to "FtsOptions.TOKENIZER_TRIGRAM",
                    "tokenizerArgs" to "{\"case_sensitive\", \"1\"}",
                ),
        ) { entity, _ ->
            assertThat(entity.ftsOptions.tokenizer, `is`(FtsOptions.TOKENIZER_TRIGRAM))
            assertThat(entity.ftsOptions.tokenizerArgs, `is`(listOf("case_sensitive", "1")))
        }
    }
}
