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

package androidx.room.processor

import COMMON
import androidx.kruth.assertThat
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeName.Companion.PRIMITIVE_LONG
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.parser.SQLTypeAffinity
import androidx.room.processor.ProcessorErrors.RELATION_IN_ENTITY
import androidx.room.runProcessorTestWithK1
import androidx.room.testing.context
import androidx.room.vo.CallType
import androidx.room.vo.Field
import androidx.room.vo.FieldGetter
import androidx.room.vo.FieldSetter
import androidx.room.vo.Fields
import androidx.room.vo.Index
import androidx.room.vo.Pojo
import androidx.room.vo.columnNames
import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private typealias IndexOrder = androidx.room.Index.Order

@RunWith(JUnit4::class)
class TableEntityProcessorTest : BaseEntityParserTest() {
    @Test
    fun simple() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public int getId() { return id; }
                public void setId(int id) { this.id = id; }
            """
        ) { entity, invocation ->
            assertThat(
                entity.type.asTypeName().toString(CodeLanguage.JAVA),
                `is`("foo.bar.MyEntity")
            )
            assertThat(entity.fields.size, `is`(1))
            val field = entity.fields.first()
            val intType = invocation.processingEnv.requireType(XTypeName.PRIMITIVE_INT)
            assertThat(
                field,
                `is`(
                    Field(
                        element = field.element,
                        name = "id",
                        type = intType,
                        columnName = "id",
                        affinity = SQLTypeAffinity.INTEGER
                    )
                )
            )
            assertThat(field.setter, `is`(FieldSetter("id", "setId", intType, CallType.METHOD)))
            assertThat(field.getter, `is`(FieldGetter("id", "getId", intType, CallType.METHOD)))
            assertThat(entity.primaryKey.fields, `is`(Fields(field)))
        }
    }

    @Test
    fun noGetter() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public void setId(int id) {this.id = id;}
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.CANNOT_FIND_GETTER_FOR_FIELD)
            }
        }
    }

    @Test
    fun noGetterInLibraryClass() {
        val libraryClasspath =
            compileFiles(
                sources =
                    listOf(
                        Source.java(
                            "test.library.MissingGetterEntity",
                            """
                    package test.library;
                    import androidx.room.*;
                    @Entity
                    public class MissingGetterEntity {
                        @PrimaryKey
                        private long id;
                        public void setId(int id) {this.id = id;}
                    }
                    """
                        )
                    )
            )
        singleEntity(
            "",
            baseClass = "test.library.MissingGetterEntity",
            classpathFiles = libraryClasspath
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.CANNOT_FIND_GETTER_FOR_FIELD)
            }
        }
    }

    @Test
    fun getterWithBadType() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public float getId() {return 0f;}
                public void setId(int id) {this.id = id;}
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.CANNOT_FIND_GETTER_FOR_FIELD)
                    .onLineContaining("int id")
            }
        }
    }

    @Test
    fun setterWithBadType() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public int getId() {return id;}
                public void setId(float id) {}
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.CANNOT_FIND_SETTER_FOR_FIELD)
            }
        }
    }

    @Test
    fun setterWithAssignableType() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public int getId() {return id;}
                public void setId(Integer id) {}
                """
        ) { entity, _ ->
            assertThat(entity.fields.columnNames).contains("id")
        }
    }

    @Test
    fun index_sort_desc() {
        val annotation =
            mapOf("indices" to """@Index(value = {"foo"}, orders = {Index.Order.DESC})""")
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public String foo;
                """,
            annotation
        ) { entity, _ ->
            assertThat(
                entity.indices,
                `is`(
                    listOf(
                        Index(
                            name = "index_MyEntity_foo",
                            unique = false,
                            fields = fieldsByName(entity, "foo"),
                            orders = listOf(IndexOrder.DESC)
                        )
                    )
                )
            )
        }
    }

    @Test
    fun index_sort_multiple() {
        val annotation =
            mapOf(
                "tableName" to "\"MyTable\"",
                "indices" to
                    """@Index(value = {"foo", "id"}, orders = {Index.Order.DESC, Index.Order.ASC})"""
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public String foo;
                """,
            annotation
        ) { entity, _ ->
            assertThat(
                entity.indices,
                `is`(
                    listOf(
                        Index(
                            name = "index_MyTable_foo_id",
                            unique = false,
                            fields = fieldsByName(entity, "foo", "id"),
                            orders = listOf(IndexOrder.DESC, IndexOrder.ASC)
                        )
                    )
                )
            )
        }
    }

    @Test
    fun index_invalidOrdersSize() {
        val annotation =
            mapOf("indices" to """@Index(value = {"foo", "id"}, orders = {Index.Order.DESC})""")
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public String foo;
                """,
            annotation
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.INVALID_INDEX_ORDERS_SIZE)
            }
        }
    }

    @Test
    fun getterWithAssignableType() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public Integer getId() {return id;}
                public void setId(int id) {}
                """
        ) { entity, _ ->
            assertThat(entity.fields.columnNames).contains("id")
        }
    }

    @Test
    fun setterWithAssignableType_2() {
        singleEntity(
            """
                @PrimaryKey
                private Integer id;
                public Integer getId() {return id;}
                public void setId(int id) {}
                """
        ) { entity, invocation ->
            val idField = entity.fields.first()
            val cursorValueReader =
                idField.cursorValueReader ?: throw AssertionError("must have a cursor value reader")
            assertThat(
                cursorValueReader.typeMirror().asTypeName(),
                `is`(invocation.processingEnv.requireType(XTypeName.PRIMITIVE_INT).asTypeName())
            )
            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.mismatchedSetter(
                        fieldName = "id",
                        ownerType = "foo.bar.MyEntity",
                        setterType = "int",
                        fieldType = XTypeName.BOXED_INT.canonicalName
                    )
                )
            }
        }
    }

    @Test
    fun getterWithAssignableType_2() {
        singleEntity(
            """
                @PrimaryKey
                private Integer id;
                public int getId() {return id == null ? 0 : id;}
                public void setId(Integer id) {}
                """
        ) { entity, invocation ->
            val idField = entity.fields.first()
            val statementBinder =
                idField.statementBinder ?: throw AssertionError("must have a statement binder")
            assertThat(
                statementBinder.typeMirror().asTypeName(),
                `is`(invocation.processingEnv.requireType(XTypeName.PRIMITIVE_INT).asTypeName())
            )
        }
    }

    @Test
    fun noSetter() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public int getId(){ return id; }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.CANNOT_FIND_SETTER_FOR_FIELD)
            }
        }
    }

    @Test
    fun tooManyGetters() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public void setId(int id) {}
                public int getId(){ return id; }
                public int id(){ return id; }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasErrorContaining("getId, id") }
        }
    }

    @Test
    fun tooManyGettersWithIgnore() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public void setId(int id) {}
                public int getId(){ return id; }
                @Ignore public int id(){ return id; }
                """
        ) { entity, _ ->
            assertThat(entity.fields.first().getter.jvmName, `is`("getId"))
        }
    }

    @Test
    fun tooManyGettersWithDifferentVisibility() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public void setId(int id) {}
                public int getId(){ return id; }
                protected int id(){ return id; }
                """
        ) { entity, _ ->
            assertThat(entity.fields.first().getter.jvmName, `is`("getId"))
        }
    }

    @Test
    fun tooManyGettersWithDifferentTypes() {
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public void setId(int id) {}
                public int getId(){ return id; }
                """
        ) { entity, _ ->
            assertThat(entity.fields.first().getter.jvmName, `is`("id"))
            assertThat(entity.fields.first().getter.callType, `is`(CallType.FIELD))
        }
    }

    @Test
    fun tooManySetters() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public void setId(int id) {}
                public void id(int id) {}
                public int getId(){ return id; }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasErrorContaining("setId, id") }
        }
    }

    @Test
    fun tooManySettersWithIgnore() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public void setId(int id) {}
                @Ignore public void id(int id) {}
                public int getId(){ return id; }
                """
        ) { entity, _ ->
            assertThat(entity.fields.first().setter.jvmName, `is`("setId"))
        }
    }

    @Test
    fun tooManySettersWithDifferentVisibility() {
        singleEntity(
            """
                @PrimaryKey
                private int id;
                public void setId(int id) {}
                protected void id(int id) {}
                public int getId(){ return id; }
                """
        ) { entity, _ ->
            assertThat(entity.fields.first().setter.jvmName, `is`("setId"))
        }
    }

    @Test
    fun tooManySettersWithDifferentTypes() {
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public void setId(int id) {}
                public int getId(){ return id; }
                """
        ) { entity, _ ->
            assertThat(entity.fields.first().setter.jvmName, `is`("id"))
            assertThat(entity.fields.first().setter.callType, `is`(CallType.FIELD))
        }
    }

    @Test
    fun preferPublicOverProtected() {
        singleEntity(
            """
                @PrimaryKey
                int id;
                public void setId(int id) {}
                public int getId(){ return id; }
                """
        ) { entity, _ ->
            assertThat(entity.fields.first().setter.jvmName, `is`("setId"))
            assertThat(entity.fields.first().getter.jvmName, `is`("getId"))
        }
    }

    @Test
    fun customName() {
        singleEntity(
            """
                @PrimaryKey
                int x;
                """,
            hashMapOf(Pair("tableName", "\"foo_table\""))
        ) { entity, _ ->
            assertThat(entity.tableName, `is`("foo_table"))
        }
    }

    @Test
    fun emptyCustomName() {
        singleEntity(
            """
                @PrimaryKey
                int x;
                """,
            hashMapOf(Pair("tableName", "\" \""))
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.ENTITY_TABLE_NAME_CANNOT_BE_EMPTY)
            }
        }
    }

    @Test
    fun missingPrimaryKey() {
        singleEntity(
            """
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.MISSING_PRIMARY_KEY)
            }
        }
    }

    @Test
    fun missingColumnAdapter() {
        singleEntity(
            """
                @PrimaryKey
                public java.util.Date myDate;
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.CANNOT_FIND_COLUMN_TYPE_ADAPTER)
                    .onLineContaining("myDate")
            }
        }
    }

    @Test
    fun dropSubPrimaryKey() {
        singleEntity(
            """
                @PrimaryKey
                int id;
                @Embedded
                Point myPoint;
                static class Point {
                    @PrimaryKey
                    int x;
                    int y;
                }
                """
        ) { entity, invocation ->
            assertThat(entity.primaryKey.fields.map { it.name }, `is`(listOf("id")))
            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.embeddedPrimaryKeyIsDropped("foo.bar.MyEntity", "x")
                )
            }
        }
    }

    @Test
    fun ignoreDropSubPrimaryKey() {
        singleEntity(
            """
                @PrimaryKey
                int id;
                @Embedded
                @SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
                Point myPoint;
                static class Point {
                    @PrimaryKey
                    int x;
                    int y;
                }
                """
        ) { entity, invocation ->
            assertThat(entity.primaryKey.fields.map { it.name }, `is`(listOf("id")))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun notNull() {
        singleEntity(
            """
                @PrimaryKey int id;
                @NonNull public String name;
                """
        ) { entity, _ ->
            val field = fieldsByName(entity, "name").first()
            assertThat(field.name, `is`("name"))
            assertThat(field.columnName, `is`("name"))
            assertThat(field.nonNull, `is`(true))
        }
    }

    private fun fieldsByName(entity: Pojo, vararg fieldNames: String): List<Field> {
        return fieldNames.mapNotNull { name -> entity.fields.find { it.name == name } }
    }

    @Test
    fun index_simple() {
        val annotation = mapOf("indices" to """@Index("foo")""")
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public String foo;
                """,
            annotation
        ) { entity, _ ->
            assertThat(
                entity.indices,
                `is`(
                    listOf(
                        Index(
                            name = "index_MyEntity_foo",
                            unique = false,
                            fields = fieldsByName(entity, "foo"),
                            emptyList()
                        )
                    )
                )
            )
        }
    }

    @Test
    fun index_fromField() {
        singleEntity(
            """
                @PrimaryKey
                public int id;
                @ColumnInfo(index = true)
                public String foo;
                """
        ) { entity, _ ->
            assertThat(
                entity.indices,
                `is`(
                    listOf(
                        Index(
                            name = "index_MyEntity_foo",
                            unique = false,
                            fields = fieldsByName(entity, "foo"),
                            orders = emptyList()
                        )
                    )
                )
            )
        }
    }

    @Test
    fun index_multiColumn() {
        val annotation = mapOf("indices" to """@Index({"foo", "id"})""")
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public String foo;
                """,
            annotation
        ) { entity, _ ->
            assertThat(
                entity.indices,
                `is`(
                    listOf(
                        Index(
                            name = "index_MyEntity_foo_id",
                            unique = false,
                            fields = fieldsByName(entity, "foo", "id"),
                            orders = emptyList()
                        )
                    )
                )
            )
        }
    }

    @Test
    fun index_multiple() {
        val annotation =
            mapOf("indices" to """{@Index({"foo", "id"}), @Index({"bar_column", "foo"})}""")
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public String foo;
                @ColumnInfo(name = "bar_column")
                public String bar;
                """,
            annotation
        ) { entity, _ ->
            assertThat(
                entity.indices,
                `is`(
                    listOf(
                        Index(
                            name = "index_MyEntity_foo_id",
                            unique = false,
                            fields = fieldsByName(entity, "foo", "id"),
                            orders = emptyList()
                        ),
                        Index(
                            name = "index_MyEntity_bar_column_foo",
                            unique = false,
                            fields = fieldsByName(entity, "bar", "foo"),
                            orders = emptyList()
                        )
                    )
                )
            )
        }
    }

    @Test
    fun index_unique() {
        val annotation = mapOf("indices" to """@Index(value = {"foo", "id"}, unique = true)""")
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public String foo;
                """,
            annotation
        ) { entity, _ ->
            assertThat(
                entity.indices,
                `is`(
                    listOf(
                        Index(
                            name = "index_MyEntity_foo_id",
                            unique = true,
                            fields = fieldsByName(entity, "foo", "id"),
                            orders = emptyList()
                        )
                    )
                )
            )
        }
    }

    @Test
    fun index_customName() {
        val annotation = mapOf("indices" to """@Index(value = {"foo"}, name = "myName")""")
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public String foo;
                """,
            annotation
        ) { entity, _ ->
            assertThat(
                entity.indices,
                `is`(
                    listOf(
                        Index(
                            name = "myName",
                            unique = false,
                            fields = fieldsByName(entity, "foo"),
                            orders = emptyList()
                        )
                    )
                )
            )
        }
    }

    @Test
    fun index_customTableName() {
        val annotation =
            mapOf("tableName" to "\"MyTable\"", "indices" to """@Index(value = {"foo"})""")
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public String foo;
                """,
            annotation
        ) { entity, _ ->
            assertThat(
                entity.indices,
                `is`(
                    listOf(
                        Index(
                            name = "index_MyTable_foo",
                            unique = false,
                            fields = fieldsByName(entity, "foo"),
                            orders = emptyList()
                        )
                    )
                )
            )
        }
    }

    @Test
    fun index_empty() {
        val annotation = mapOf("indices" to """@Index({})""")
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public String foo;
                """,
            annotation
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.INDEX_COLUMNS_CANNOT_BE_EMPTY)
            }
        }
    }

    @Test
    fun index_missingColumn() {
        val annotation = mapOf("indices" to """@Index({"foo", "bar"})""")
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public String foo;
                """,
            annotation
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.indexColumnDoesNotExist("bar", listOf("id, foo"))
                )
            }
        }
    }

    @Test
    fun index_nameConflict() {
        val annotation = mapOf("indices" to """@Index({"foo"})""")
        singleEntity(
            """
                @PrimaryKey
                public int id;
                @ColumnInfo(index = true)
                public String foo;
                """,
            annotation
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.duplicateIndexInEntity("index_MyEntity_foo"))
            }
        }
    }

    @Test
    fun index_droppedParentFieldIndex() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;
                public class Base {
                    @PrimaryKey
                    long baseId;
                    @ColumnInfo(index = true)
                    String name;
                    String lastName;
                }
                """
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { entity, invocation ->
            assertThat(entity.indices.isEmpty(), `is`(true))
            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.droppedSuperClassFieldIndex(
                        fieldName = "name",
                        childEntity = "foo.bar.MyEntity",
                        superEntity = "foo.bar.Base"
                    )
                )
            }
        }
    }

    @Test
    fun index_keptGrandParentEntityIndex() {
        val grandParent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;
                @Entity(indices = @Index({"name", "lastName"}))
                public class Base {
                    @PrimaryKey
                    long baseId;
                    String name, lastName;
                }
                """
            )
        val parent =
            Source.java(
                qName = "foo.bar.Parent",
                code =
                    """
                package foo.bar;
                import androidx.room.*;

                public class Parent extends Base {
                    String iHaveAField;
                }
                """
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                """,
            baseClass = "foo.bar.Parent",
            attributes = hashMapOf("inheritSuperIndices" to "true"),
            sources = listOf(parent, grandParent)
        ) { entity, invocation ->
            assertThat(entity.indices.size, `is`(1))
            assertThat(
                entity.indices.first(),
                `is`(
                    Index(
                        name = "index_MyEntity_name_lastName",
                        unique = false,
                        fields = fieldsByName(entity, "name", "lastName"),
                        orders = emptyList()
                    )
                )
            )
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun index_keptParentEntityIndex() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;
                @Entity(indices = @Index({"name", "lastName"}))
                public class Base {
                    @PrimaryKey
                    long baseId;
                    String name, lastName;
                }
                """
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                """,
            baseClass = "foo.bar.Base",
            attributes = hashMapOf("inheritSuperIndices" to "true"),
            sources = listOf(parent)
        ) { entity, invocation ->
            assertThat(entity.indices.size, `is`(1))
            assertThat(
                entity.indices.first(),
                `is`(
                    Index(
                        name = "index_MyEntity_name_lastName",
                        unique = false,
                        fields = fieldsByName(entity, "name", "lastName"),
                        orders = emptyList()
                    )
                )
            )
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun ignoredFields() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;
                public class Base {
                    String name;
                    String tmp1;
                }
                """
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                public String tmp2;
                """,
            baseClass = "foo.bar.Base",
            attributes = hashMapOf("ignoredColumns" to "{\"tmp1\", \"tmp2\"}"),
            sources = listOf(parent)
        ) { entity, invocation ->
            assertThat(entity.fields.size, `is`(2))
            assertThat(entity.fields.map(Field::name), hasItems("name", "id"))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun index_keptParentFieldIndex() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;
                public class Base {
                    @PrimaryKey
                    long baseId;
                    @ColumnInfo(index = true)
                    String name;
                    String lastName;
                }
                """
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                """,
            baseClass = "foo.bar.Base",
            attributes = hashMapOf("inheritSuperIndices" to "true"),
            sources = listOf(parent)
        ) { entity, invocation ->
            assertThat(entity.indices.size, `is`(1))
            assertThat(
                entity.indices.first(),
                `is`(
                    Index(
                        name = "index_MyEntity_name",
                        unique = false,
                        fields = fieldsByName(entity, "name"),
                        orders = emptyList()
                    )
                )
            )
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun index_droppedGrandParentEntityIndex() {
        val grandParent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;
                @Entity(indices = @Index({"name", "lastName"}))
                public class Base {
                    @PrimaryKey
                    long baseId;
                    String name, lastName;
                }
                """
            )
        val parent =
            Source.java(
                qName = "foo.bar.Parent",
                code =
                    """
                package foo.bar;
                import androidx.room.*;

                public class Parent extends Base {
                    String iHaveAField;
                }
                """
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                """,
            baseClass = "foo.bar.Parent",
            sources = listOf(parent, grandParent)
        ) { entity, invocation ->
            assertThat(entity.indices.isEmpty(), `is`(true))
            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.droppedSuperClassIndex(
                        childEntity = "foo.bar.MyEntity",
                        superEntity = "foo.bar.Base"
                    )
                )
            }
        }
    }

    @Test
    fun index_droppedParentEntityIndex() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;
                @Entity(indices = @Index({"name", "lastName"}))
                public class Base {
                    @PrimaryKey
                    long baseId;
                    String name, lastName;
                }
                """
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { entity, invocation ->
            assertThat(entity.indices.isEmpty(), `is`(true))
            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.droppedSuperClassIndex(
                        childEntity = "foo.bar.MyEntity",
                        superEntity = "foo.bar.Base"
                    )
                )
            }
        }
    }

    @Test
    fun index_droppedEmbeddedEntityIndex() {
        singleEntity(
            """
                @PrimaryKey
                public int id;
                @Embedded
                public Foo foo;
                @Entity(indices = {@Index("a")})
                static class Foo {
                    @PrimaryKey
                    @ColumnInfo(name = "foo_id")
                    int id;
                    @ColumnInfo(index = true)
                    public int a;
                }
                """
        ) { entity, invocation ->
            assertThat(entity.indices.isEmpty(), `is`(true))
            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.droppedEmbeddedIndex(
                        entityName = "foo.bar.MyEntity.Foo",
                        fieldPath = "foo",
                        grandParent = "foo.bar.MyEntity"
                    )
                )
            }
        }
    }

    @Test
    fun index_onEmbeddedField() {
        singleEntity(
            """
                @PrimaryKey
                public int id;
                @Embedded
                @ColumnInfo(index = true)
                public Foo foo;
                static class Foo {
                    @ColumnInfo(index = true)
                    public int a;
                }
                """
        ) { entity, invocation ->
            assertThat(entity.indices.isEmpty(), `is`(true))
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.CANNOT_USE_MORE_THAN_ONE_POJO_FIELD_ANNOTATION)
            }
        }
    }

    @Test
    fun index_droppedEmbeddedFieldIndex() {
        singleEntity(
            """
                @PrimaryKey
                public int id;
                @Embedded
                public Foo foo;
                static class Foo {
                    @ColumnInfo(index = true)
                    public int a;
                }
                """
        ) { entity, invocation ->
            assertThat(entity.indices.isEmpty(), `is`(true))
            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.droppedEmbeddedFieldIndex("foo > a", "foo.bar.MyEntity")
                )
            }
        }
    }

    @Test
    fun index_referenceEmbeddedField() {
        singleEntity(
            """
                @PrimaryKey
                public int id;
                @Embedded
                public Foo foo;
                static class Foo {
                    public int a;
                }
                """,
            attributes = mapOf("indices" to "@Index(\"a\")")
        ) { entity, _ ->
            assertThat(entity.indices.size, `is`(1))
            assertThat(
                entity.indices.first(),
                `is`(
                    Index(
                        name = "index_MyEntity_a",
                        unique = false,
                        fields = fieldsByName(entity, "a"),
                        orders = emptyList()
                    )
                )
            )
        }
    }

    @Test
    fun primaryKey_definedInBothWays() {
        singleEntity(
            """
                public int id;
                @PrimaryKey
                public String foo;
                """,
            attributes = mapOf("primaryKeys" to "\"id\"")
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.multiplePrimaryKeyAnnotations(
                        listOf("PrimaryKey[id]", "PrimaryKey[foo]")
                    )
                )
            }
        }
    }

    @Test
    fun primaryKey_badColumnName() {
        singleEntity(
            """
                public int id;
                """,
            attributes = mapOf("primaryKeys" to "\"foo\"")
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.primaryKeyColumnDoesNotExist("foo", listOf("id"))
                )
            }
        }
    }

    @Test
    fun primaryKey_multipleAnnotations() {
        singleEntity(
            """
                @PrimaryKey
                int x;
                @PrimaryKey
                int y;
                """
        ) { entity, invocation ->
            assertThat(entity.primaryKey.fields.isEmpty(), `is`(true))
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.multiplePrimaryKeyAnnotations(
                        listOf("PrimaryKey[x]", "PrimaryKey[y]")
                    )
                )
            }
        }
    }

    @Test
    fun primaryKey_fromParentField() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;
                public class Base {
                    @PrimaryKey
                    long baseId;
                    String name, lastName;
                }
                """
            )
        singleEntity(
            """
                public int id;
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { entity, invocation ->
            assertThat(entity.primaryKey.fields.firstOrNull()?.name, `is`("baseId"))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun primaryKey_fromParentEntity() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;
                @Entity(primaryKeys = "baseId")
                public class Base {
                    long baseId;
                    String name, lastName;
                }
                """
            )
        singleEntity(
            """
                public int id;
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { entity, invocation ->
            assertThat(entity.primaryKey.fields.firstOrNull()?.name, `is`("baseId"))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun primaryKey_overrideFromParentField() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;
                public class Base {
                    @PrimaryKey
                    long baseId;
                    String name, lastName;
                }
                """
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { entity, invocation ->
            assertThat(entity.primaryKey.fields.size, `is`(1))
            assertThat(entity.primaryKey.fields.firstOrNull()?.name, `is`("id"))
            assertThat(entity.primaryKey.autoGenerateId, `is`(false))
            invocation.assertCompilationResult {
                hasNoteContaining("PrimaryKey[baseId] is overridden by PrimaryKey[id]")
            }
        }
    }

    @Test
    fun primaryKey_overrideFromParentEntityViaField() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;
                @Entity(primaryKeys = "baseId")
                public class Base {
                    long baseId;
                    String name, lastName;
                }
                """
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { entity, invocation ->
            assertThat(entity.primaryKey.fields.size, `is`(1))
            assertThat(entity.primaryKey.fields.firstOrNull()?.name, `is`("id"))
            invocation.assertCompilationResult {
                hasNoteContaining("PrimaryKey[baseId] is overridden by PrimaryKey[id]")
            }
        }
    }

    @Test
    fun primaryKey_overrideFromParentEntityViaEntity() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;
                @Entity(primaryKeys = "baseId")
                public class Base {
                    long baseId;
                    String name, lastName;
                }
                """
            )
        singleEntity(
            """
                public int id;
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent),
            attributes = mapOf("primaryKeys" to "\"id\"")
        ) { entity, invocation ->
            assertThat(entity.primaryKey.fields.size, `is`(1))
            assertThat(entity.primaryKey.fields.firstOrNull()?.name, `is`("id"))
            assertThat(entity.primaryKey.autoGenerateId, `is`(false))
            invocation.assertCompilationResult {
                hasNoteContaining("PrimaryKey[baseId] is overridden by PrimaryKey[id]")
            }
        }
    }

    @Test
    fun primaryKey_autoGenerate() {
        listOf("long", "Long", "Integer", "int").forEach { type ->
            singleEntity(
                """
                @PrimaryKey(autoGenerate = true)
                public $type id;
                """
            ) { entity, _ ->
                assertThat(entity.primaryKey.fields.size, `is`(1))
                assertThat(entity.primaryKey.fields.firstOrNull()?.name, `is`("id"))
                assertThat(entity.primaryKey.autoGenerateId, `is`(true))
            }
        }
    }

    @Test
    fun primaryKey_nonNull_notNeeded() {
        listOf("long", "Long", "Integer", "int").forEach { type ->
            singleEntity(
                """
                @PrimaryKey
                public $type id;
                """
            ) { entity, _ ->
                assertThat(entity.primaryKey.fields.size, `is`(1))
                assertThat(entity.primaryKey.fields.firstOrNull()?.name, `is`("id"))
                assertThat(entity.primaryKey.autoGenerateId, `is`(false))
            }
        }
    }

    @Test
    fun primaryKey_autoGenerateBadType() {
        listOf("String", "float", "Float", "Double", "double").forEach { type ->
            singleEntity(
                """
                @PrimaryKey(autoGenerate = true)
                public $type id;
                """
            ) { entity, invocation ->
                assertThat(entity.primaryKey.fields.size, `is`(1))
                assertThat(entity.primaryKey.fields.firstOrNull()?.name, `is`("id"))
                assertThat(entity.primaryKey.autoGenerateId, `is`(true))
                invocation.assertCompilationResult {
                    hasErrorContaining(ProcessorErrors.AUTO_INCREMENTED_PRIMARY_KEY_IS_NOT_INT)
                }
            }
        }
    }

    @Test
    fun primaryKey_embedded() {
        singleEntity(
            """
                public int id;

                @Embedded(prefix = "bar_")
                @PrimaryKey
                @NonNull
                public Foo foo;

                static class Foo {
                    public int a;
                    public int b;
                }
                """
        ) { entity, invocation ->
            assertThat(entity.primaryKey.columnNames, `is`(listOf("bar_a", "bar_b")))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun primaryKey_embeddedInherited() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.annotation.NonNull;
                import androidx.room.*;

                public class Base {
                    long baseId;
                    String name, lastName;
                    @Embedded(prefix = "bar_")
                    @PrimaryKey
                    @NonNull
                    public Foo foo;

                    static class Foo {
                        public int a;
                        public int b;
                    }
                }
                """
            )
        singleEntity(
            """
                public int id;
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { entity, invocation ->
            assertThat(entity.primaryKey.columnNames, `is`(listOf("bar_a", "bar_b")))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun primaryKey_overrideViaEmbedded() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;

                @Entity(primaryKeys = "baseId")
                public class Base {
                    long baseId;
                    String name, lastName;
                }
                """
            )
        singleEntity(
            """
                public int id;
                @Embedded(prefix = "bar_")
                @PrimaryKey
                @NonNull
                public Foo foo;

                static class Foo {
                    public int a;
                    public int b;
                }
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { entity, invocation ->
            assertThat(entity.primaryKey.columnNames, `is`(listOf("bar_a", "bar_b")))
            invocation.assertCompilationResult {
                hasNoteContaining(
                    "PrimaryKey[baseId] is overridden by PrimaryKey[foo > a, foo > b]"
                )
            }
        }
    }

    @Test
    fun primaryKey_overrideEmbedded() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.annotation.NonNull;
                import androidx.room.*;

                public class Base {
                    long baseId;
                    String name, lastName;
                    @Embedded(prefix = "bar_")
                    @PrimaryKey
                    @NonNull
                    public Foo foo;

                    static class Foo {
                        public int a;
                        public int b;
                    }
                }
                """
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { entity, invocation ->
            assertThat(entity.primaryKey.columnNames, `is`(listOf("id")))
            invocation.assertCompilationResult {
                hasNoteContaining("PrimaryKey[foo > a, foo > b] is overridden by PrimaryKey[id]")
            }
        }
    }

    @Test
    fun primaryKey_NonNull() {
        singleEntity(
            """
            @PrimaryKey
            @NonNull
            public String id;
            """
        ) { entity, _ ->
            assertThat(entity.primaryKey.fields.size, `is`(1))
            assertThat(entity.primaryKey.fields.firstOrNull()?.name, `is`("id"))
        }
    }

    @Test
    fun primaryKey_Nullable() {
        singleEntity(
            """
            @PrimaryKey
            public String id;
            """
        ) { entity, invocation ->
            assertThat(entity.primaryKey.fields.size, `is`(1))
            assertThat(entity.primaryKey.fields.firstOrNull()?.name, `is`("id"))
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.primaryKeyNull("id"))
            }
        }
    }

    @Test
    fun primaryKey_MultipleNullable() {
        singleEntity(
            """
            @PrimaryKey
            public String id;
            @PrimaryKey
            public String anotherId;
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.primaryKeyNull("id"))
                hasErrorContaining(ProcessorErrors.primaryKeyNull("anotherId"))
            }
        }
    }

    @Test
    fun primaryKey_MultipleNullableAndNonNullable() {
        singleEntity(
            """
            @PrimaryKey
            @NonNull
            public String id;
            @PrimaryKey
            public String anotherId;
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.primaryKeyNull("anotherId"))
            }
        }
    }

    @Test
    fun primaryKey_definedAsAttributesNullable() {
        singleEntity(
            """
                public int id;
                public String foo;
                """,
            attributes = mapOf("primaryKeys" to "{\"id\", \"foo\"}")
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo"))
            }
        }
    }

    @Test
    fun primaryKey_definedAsAttributesNonNull() {
        singleEntity(
            """
                public int id;
                @NonNull
                public String foo;
                """,
            attributes = mapOf("primaryKeys" to "{\"id\", \"foo\"}")
        ) { entity, _ ->
            assertThat(entity.primaryKey.fields.map { it.name }, `is`(listOf("id", "foo")))
        }
    }

    @Test
    fun primaryKey_nullableEmbedded() {
        singleEntity(
            """
                public int id;

                @Embedded(prefix = "bar_")
                @PrimaryKey
                public Foo foo;

                static class Foo {
                    public int a;
                    public int b;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo"))
            }
        }
    }

    @Test
    fun primaryKey_nullableEmbeddedObject() {
        singleEntity(
            """
                public int id;

                @Embedded(prefix = "bar_")
                @PrimaryKey
                public Foo foo;

                static class Foo {
                    public String a;
                    public String b;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo > a"))
                    .onLineContaining("String a")
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo > b"))
                    .onLineContaining("String b")
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo"))
                    .onLineContaining("Foo foo")
                hasErrorCount(3)
            }
        }
    }

    @Test
    fun primaryKey_nullableEmbeddedObject_multipleParents() {
        singleEntity(
            """
                public int id;

                @Embedded(prefix = "bar_")
                @PrimaryKey
                public Foo foo;

                static class Foo {
                @Embedded(prefix = "baz_")
                public Baz a;
                public String b;

                static class Baz {
                    public Integer bb;
                }
            }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo > a"))
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo > b"))
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo"))
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo > a > bb"))
                hasErrorCount(4)
            }
        }
    }

    @Test
    fun primaryKey_nullableEmbeddedInherited() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.annotation.NonNull;
                import androidx.room.*;

                public class Base {
                    long baseId;
                    String name, lastName;
                    @Embedded(prefix = "bar_")
                    @PrimaryKey
                    public Foo foo;

                    static class Foo {
                        public int a;
                        public int b;
                    }
                }
                """
            )
        singleEntity(
            """
                public int id;
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo"))
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo > a"))
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo > b"))
                hasErrorCount(3)
            }
        }
    }

    @Test
    fun primaryKey_nullableOverrideViaEmbedded() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.room.*;

                @Entity(primaryKeys = "baseId")
                public class Base {
                    long baseId;
                    String name, lastName;
                }
                """
            )
        singleEntity(
            """
                public int id;
                @Embedded(prefix = "bar_")
                @PrimaryKey
                public Foo foo;

                static class Foo {
                    public int a;
                    public int b;
                }
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo"))
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo > a"))
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo > b"))
                hasNoteContaining(
                    "PrimaryKey[baseId] is overridden by PrimaryKey[foo > a, foo > b]"
                )
                hasErrorCount(3)
            }
        }
    }

    @Test
    fun primaryKey_nullableOverrideEmbedded() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.annotation.NonNull;
                import androidx.room.*;

                public class Base {
                    long baseId;
                    String name, lastName;
                    @Embedded(prefix = "bar_")
                    @PrimaryKey
                    public Foo foo;

                    static class Foo {
                        public int a;
                        public int b;
                    }
                }
                """
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo"))
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo > a"))
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo > b"))
                hasNoteContaining("PrimaryKey[foo > a, foo > b] is overridden by PrimaryKey[id]")
                hasErrorCount(3)
            }
        }
    }

    @Test
    fun primaryKey_integerOverrideEmbedded() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.annotation.NonNull;
                import androidx.room.*;

                public class Base {
                    long baseId;
                    String name, lastName;
                    @Embedded(prefix = "bar_")
                    @PrimaryKey
                    public Foo foo;

                    static class Foo {
                        public Integer a;
                    }
                }
                """
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasNoteContaining("PrimaryKey[foo > a] is overridden by PrimaryKey[id]")
            }
        }
    }

    @Test
    fun primaryKey_singleStringPrimaryKeyOverrideEmbedded() {
        val parent =
            Source.java(
                qName = "foo.bar.Base",
                code =
                    """
                package foo.bar;
                import androidx.annotation.NonNull;
                import androidx.room.*;

                public class Base {
                    long baseId;
                    String name, lastName;
                    @Embedded(prefix = "bar_")
                    @PrimaryKey
                    public Foo foo;

                    static class Foo {
                        public String a;
                    }
                }
                """
            )
        singleEntity(
            """
                @PrimaryKey
                public int id;
                """,
            baseClass = "foo.bar.Base",
            sources = listOf(parent)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo"))
                hasErrorContaining(ProcessorErrors.primaryKeyNull("foo > a"))
                hasNoteContaining("PrimaryKey[foo > a] is overridden by PrimaryKey[id]")
                hasErrorCount(2)
            }
        }
    }

    @Test
    fun relationInEntity() {
        singleEntity(
            """
                @PrimaryKey
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid")
                java.util.List<User> users;
                """,
            sources = listOf(COMMON.USER)
        ) { _, invocation ->
            invocation.assertCompilationResult { hasErrorContaining(RELATION_IN_ENTITY) }
        }
    }

    @Test
    fun foreignKey_invalidAction() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = ${COMMON.USER_TYPE_NAME.canonicalName}.class,
                    parentColumns = "lastName",
                    childColumns = "name",
                    onDelete = 101
                )}
            """
                        .trimIndent()
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.INVALID_FOREIGN_KEY_ACTION)
            }
        }
    }

    @Test
    fun foreignKey_badEntity() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = dsa.class,
                    parentColumns = "lastName",
                    childColumns = "name"
                )}
            """
                        .trimIndent()
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    "Element 'foo.bar.MyEntity' references a type that is not present"
                )
            }
        }
    }

    @Test
    fun foreignKey_notAnEntity() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = ${COMMON.NOT_AN_ENTITY_TYPE_NAME.canonicalName}.class,
                    parentColumns = "lastName",
                    childColumns = "name"
                )}
            """
                        .trimIndent()
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                """,
            attributes = annotation,
            sources = listOf(COMMON.NOT_AN_ENTITY)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.foreignKeyNotAnEntity(
                        COMMON.NOT_AN_ENTITY_TYPE_NAME.canonicalName
                    )
                )
            }
        }
    }

    @Test
    fun foreignKey_invalidChildColumn() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = ${COMMON.USER_TYPE_NAME.canonicalName}.class,
                    parentColumns = "lastName",
                    childColumns = "namex"
                )}
            """
                        .trimIndent()
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.foreignKeyChildColumnDoesNotExist("namex", listOf("id", "name"))
                )
            }
        }
    }

    @Test
    fun foreignKey_columnCountMismatch() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = ${COMMON.USER_TYPE_NAME.canonicalName}.class,
                    parentColumns = "lastName",
                    childColumns = {"name", "id"}
                )}
            """
                        .trimIndent()
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.foreignKeyColumnNumberMismatch(
                        listOf("name", "id"),
                        listOf("lastName")
                    )
                )
            }
        }
    }

    @Test
    fun foreignKey_emptyChildColumns() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = ${COMMON.USER_TYPE_NAME.canonicalName}.class,
                    parentColumns = "lastName",
                    childColumns = {}
                )}
            """
                        .trimIndent()
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.FOREIGN_KEY_EMPTY_CHILD_COLUMN_LIST)
            }
        }
    }

    @Test
    fun foreignKey_emptyParentColumns() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = ${COMMON.USER_TYPE_NAME.canonicalName}.class,
                    parentColumns = {},
                    childColumns = {"name"}
                )}
            """
                        .trimIndent()
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.FOREIGN_KEY_EMPTY_PARENT_COLUMN_LIST)
            }
        }
    }

    @Test
    fun foreignKey_simple() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = ${COMMON.USER_TYPE_NAME.canonicalName}.class,
                    parentColumns = "lastName",
                    childColumns = "name",
                    onDelete = ForeignKey.SET_NULL,
                    onUpdate = ForeignKey.CASCADE,
                    deferred = true
                )}
            """
                        .trimIndent()
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { entity, _ ->
            assertThat(entity.foreignKeys.size, `is`(1))
            val fKey = entity.foreignKeys.first()
            assertThat(fKey.parentTable, `is`("User"))
            assertThat(fKey.parentColumns, `is`(listOf("lastName")))
            assertThat(fKey.deferred, `is`(true))
            assertThat(fKey.childFields.size, `is`(1))
            val field = fKey.childFields.first()
            assertThat(field.name, `is`("name"))
        }
    }

    @Test
    fun foreignKey_dontDuplicationChildIndex_SingleColumn() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = ${COMMON.USER_TYPE_NAME.canonicalName}.class,
                    parentColumns = "lastName",
                    childColumns = "name",
                    onDelete = ForeignKey.SET_NULL,
                    onUpdate = ForeignKey.CASCADE,
                    deferred = true
                )}
            """
                        .trimIndent(),
                "indices" to """@Index("name")"""
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { _, invocation ->
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun foreignKey_dontDuplicationChildIndex_MultipleColumns() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = ${COMMON.USER_TYPE_NAME.canonicalName}.class,
                    parentColumns = {"lastName", "name"},
                    childColumns = {"lName", "name"},
                    onDelete = ForeignKey.SET_NULL,
                    onUpdate = ForeignKey.CASCADE,
                    deferred = true
                )}
            """
                        .trimIndent(),
                "indices" to """@Index({"lName", "name"})"""
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                String lName;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { entity, invocation ->
            assertThat(entity.indices.size, `is`(1))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun foreignKey_dontDuplicationChildIndex_WhenCovered() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = ${COMMON.USER_TYPE_NAME.canonicalName}.class,
                    parentColumns = {"lastName"},
                    childColumns = {"name"},
                    onDelete = ForeignKey.SET_NULL,
                    onUpdate = ForeignKey.CASCADE,
                    deferred = true
                )}
            """
                        .trimIndent(),
                "indices" to """@Index({"name", "lName"})"""
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                String lName;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { entity, invocation ->
            assertThat(entity.indices.size, `is`(1))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun foreignKey_warnMissingChildIndex() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = ${COMMON.USER_TYPE_NAME.canonicalName}.class,
                    parentColumns = "lastName",
                    childColumns = "name",
                    onDelete = ForeignKey.SET_NULL,
                    onUpdate = ForeignKey.CASCADE,
                    deferred = true
                )}
            """
                        .trimIndent()
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { entity, invocation ->
            assertThat(entity.indices, `is`(emptyList()))
            invocation.assertCompilationResult {
                hasWarningContaining(ProcessorErrors.foreignKeyMissingIndexInChildColumn("name"))
            }
        }
    }

    @Test
    fun foreignKey_warnMissingChildrenIndex() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = ${COMMON.USER_TYPE_NAME.canonicalName}.class,
                    parentColumns = {"lastName", "name"},
                    childColumns = {"lName", "name"}
                )}
            """
                        .trimIndent()
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                String lName;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { entity, invocation ->
            assertThat(entity.indices, `is`(emptyList()))
            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.foreignKeyMissingIndexInChildColumns(listOf("lName", "name"))
                )
            }
        }
    }

    @Test
    fun foreignKey_dontIndexIfAlreadyPrimaryKey() {
        val annotation =
            mapOf(
                "foreignKeys" to
                    """{@ForeignKey(
                    entity = ${COMMON.USER_TYPE_NAME.canonicalName}.class,
                    parentColumns = "lastName",
                    childColumns = "id",
                    onDelete = ForeignKey.SET_NULL,
                    onUpdate = ForeignKey.CASCADE,
                    deferred = true
                )}
            """
                        .trimIndent()
            )
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { entity, invocation ->
            assertThat(entity.indices, `is`(emptyList()))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun recursion_1Level() {
        singleEntity(
            """
                @Embedded
                MyEntity myEntity;
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyEntity -> foo.bar.MyEntity"
                    )
                )
            }
        }
    }

    @Test
    fun recursion_2Levels_embedToRelation() {
        singleEntity(
            """
                int pojoId;
                @Embedded
                A a;

                static class A {
                    int entityId;
                    @Relation(parentColumn = "entityId", entityColumn = "pojoId")
                    List<MyEntity> myEntity;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyEntity -> foo.bar.MyEntity.A -> foo.bar.MyEntity"
                    )
                )
            }
        }
    }

    @Test
    fun recursion_2Levels_onlyEmbeds_entityToPojo() {
        singleEntity(
            """
                @Embedded
                A a;

                static class A {
                    @Embedded
                    MyEntity myEntity;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyEntity -> foo.bar.MyEntity.A -> foo.bar.MyEntity"
                    )
                )
            }
        }
    }

    @Test
    fun recursion_2Levels_onlyEmbeds_onlyEntities() {
        singleEntity(
            """
                @Embedded
                A a;

                @Entity
                static class A {
                    @Embedded
                    MyEntity myEntity;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyEntity -> foo.bar.MyEntity.A -> foo.bar.MyEntity"
                    )
                )
            }
        }
    }

    @Test
    fun okTableName() {
        val annotation = mapOf("tableName" to "\"foo bar\"")
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { entity, _ ->
            assertThat(entity.tableName).isEqualTo("foo bar")
        }
    }

    @Test
    fun badTableName() {
        val annotation = mapOf("tableName" to """ "foo`bar" """)
        singleEntity(
            """
                @PrimaryKey
                int id;
                String name;
                """,
            attributes = annotation,
            sources = listOf(COMMON.USER)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.INVALID_TABLE_NAME)
            }
        }
    }

    @Test
    fun badColumnName() {
        singleEntity(
            """
                @PrimaryKey
                int id;
                @ColumnInfo(name = "\"foo bar\"")
                String name;
                """,
            sources = listOf(COMMON.USER)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.INVALID_COLUMN_NAME)
            }
        }
    }

    @Test
    fun typeAlias() {
        val src =
            Source.kotlin(
                "Entity.kt",
                """
            import androidx.room.*;

            typealias MyLong = Long
            @Entity(tableName = "par_table")
            data class Subject(@PrimaryKey @ColumnInfo(name = "my_long") val myLong: MyLong)
            """
                    .trimIndent()
            )
        runProcessorTestWithK1(sources = listOf(src)) { invocation ->
            val parser =
                TableEntityProcessor(
                    invocation.context,
                    invocation.processingEnv.requireTypeElement("Subject")
                )
            val parsed = parser.process()
            val field = parsed.primaryKey.fields.first()
            assertThat(field.typeName).isEqualTo(PRIMITIVE_LONG)
        }
    }
}
