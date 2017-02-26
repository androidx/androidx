/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.support.room.processor

import COMMON
import com.android.support.room.parser.SQLTypeAffinity
import com.android.support.room.processor.ProcessorErrors.CANNOT_FIND_TYPE
import com.android.support.room.processor.ProcessorErrors.ENTITY_MUST_BE_ANNOTATED_WITH_ENTITY
import com.android.support.room.processor.ProcessorErrors.POJO_FIELD_HAS_DUPLICATE_COLUMN_NAME
import com.android.support.room.processor.ProcessorErrors.RELATION_NOT_COLLECTION
import com.android.support.room.processor.ProcessorErrors.relationCannotFindEntityField
import com.android.support.room.processor.ProcessorErrors.relationCannotFindParentEntityField
import com.android.support.room.testing.TestInvocation
import com.android.support.room.vo.Pojo
import com.android.support.room.vo.RelationCollector
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import simpleRun
import javax.tools.JavaFileObject

/**
 * Some of the functionality is tested via EntityProcessor.
 */
@RunWith(JUnit4::class)
class PojoProcessorTest {

    companion object {
        val MY_POJO = ClassName.get("foo.bar", "MyPojo")
        val HEADER = """
            package foo.bar;
            import com.android.support.room.*;
            import java.util.*;
            public class MyPojo {
            """
        val FOOTER = "\n}"
    }

    private fun String.toJFO(qName: String) = JavaFileObjects.forSourceLines(qName, this)

    @Test
    fun inheritedPrivate() {
        val parent = """
            package foo.bar.x;
            import com.android.support.room.*;
            public class BaseClass {
                private String baseField;
                public String getBaseField(){ return baseField; }
                public void setBaseField(String baseField){ }
            }
        """
        simpleRun(
                """
                package foo.bar;
                import com.android.support.room.*;
                public class ${MY_POJO.simpleName()} extends foo.bar.x.BaseClass {
                    public String myField;
                }
                """.toJFO(MY_POJO.toString()),
                parent.toJFO("foo.bar.x.BaseClass")) { invocation ->
            val pojo = PojoProcessor(baseContext = invocation.context,
                    element = invocation.typeElement(MY_POJO.toString()),
                    bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                    parent = null).process()
            assertThat(pojo.fields.find { it.name == "myField" }, notNullValue())
            assertThat(pojo.fields.find { it.name == "baseField" }, notNullValue())
        }.compilesWithoutError()
    }

    @Test
    fun decomposed() {
        singleRun(
                """
                int id;
                @Decompose
                Point myPoint;
                static class Point {
                    int x;
                    int y;
                }
                """
        ) { pojo ->
            assertThat(pojo.fields.size, `is`(3))
            assertThat(pojo.fields[1].name, `is`("x"))
            assertThat(pojo.fields[2].name, `is`("y"))
            assertThat(pojo.fields[0].parent, nullValue())
            assertThat(pojo.fields[1].parent, notNullValue())
            assertThat(pojo.fields[2].parent, notNullValue())
            val parent = pojo.fields[2].parent!!
            assertThat(parent.prefix, `is`(""))
            assertThat(parent.field.name, `is`("myPoint"))
            assertThat(parent.pojo.typeName,
                    `is`(ClassName.get("foo.bar.MyPojo", "Point") as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun decomposedWithPrefix() {
        singleRun(
                """
                int id;
                @Decompose(prefix = "foo")
                Point myPoint;
                static class Point {
                    int x;
                    @ColumnInfo(name = "y2")
                    int y;
                }
                """
        ) { pojo ->
            assertThat(pojo.fields.size, `is`(3))
            assertThat(pojo.fields[1].name, `is`("x"))
            assertThat(pojo.fields[2].name, `is`("y"))
            assertThat(pojo.fields[1].columnName, `is`("foox"))
            assertThat(pojo.fields[2].columnName, `is`("fooy2"))
            val parent = pojo.fields[2].parent!!
            assertThat(parent.prefix, `is`("foo"))
        }.compilesWithoutError()
    }

    @Test
    fun nestedDecompose() {
        singleRun(
                """
                int id;
                @Decompose(prefix = "foo")
                Point myPoint;
                static class Point {
                    int x;
                    @ColumnInfo(name = "y2")
                    int y;
                    @Decompose(prefix = "bar")
                    Coordinate coordinate;
                }
                static class Coordinate {
                    double lat;
                    double lng;
                    @Ignore
                    String ignored;
                }
                """
        ) { pojo ->
            assertThat(pojo.fields.size, `is`(5))
            assertThat(pojo.fields.map { it.columnName }, `is`(
                    listOf("id", "foox", "fooy2", "foobarlat", "foobarlng")))
        }.compilesWithoutError()
    }

    @Test
    fun duplicateColumnNames() {
        singleRun(
                """
                int id;
                @ColumnInfo(name = "id")
                int another;
                """
        ) { pojo ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.pojoDuplicateFieldNames("id", listOf("id", "another"))
        ).and().withErrorContaining(
                POJO_FIELD_HAS_DUPLICATE_COLUMN_NAME
        ).and().withErrorCount(3)
    }

    @Test
    fun duplicateColumnNamesFromDecomposed() {
        singleRun(
                """
                int id;
                @Decompose
                Foo foo;
                static class Foo {
                    @ColumnInfo(name = "id")
                    int x;
                }
                """
        ) { pojo ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.pojoDuplicateFieldNames("id", listOf("id", "foo > x"))
        ).and().withErrorContaining(
                POJO_FIELD_HAS_DUPLICATE_COLUMN_NAME
        ).and().withErrorCount(3)
    }

    @Test
    fun dropSubPrimaryKeyNoWarningForPojo() {
        singleRun(
                """
                @PrimaryKey
                int id;
                @Decompose
                Point myPoint;
                static class Point {
                    @PrimaryKey
                    int x;
                    int y;
                }
                """
        ) { pojo ->
        }.compilesWithoutError().withWarningCount(0)
    }

    @Test
    fun relation_notCollection() {
        singleRun(
                """
                int id;
                @Relation(parentField = "id", entityField = "uid")
                public User user;
                """, COMMON.USER
        ) { pojo ->
        }.failsToCompile().withErrorContaining(RELATION_NOT_COLLECTION)
    }

    @Test
    fun relation_columnInfo() {
        singleRun(
                """
                int id;
                @ColumnInfo
                @Relation(parentField = "id", entityField = "uid")
                public List<User> user;
                """, COMMON.USER
        ) { pojo ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.CANNOT_USE_MORE_THAN_ONE_POJO_FIELD_ANNOTATION)
    }

    @Test
    fun relation_notEntity() {
        singleRun(
                """
                int id;
                @Relation(parentField = "id", entityField = "uid")
                public List<NotAnEntity> user;
                """, COMMON.NOT_AN_ENTITY
        ) { pojo ->
        }.failsToCompile().withErrorContaining(ENTITY_MUST_BE_ANNOTATED_WITH_ENTITY)
    }

    @Test
    fun relation_missingParent() {
        singleRun(
                """
                int id;
                @Relation(parentField = "idk", entityField = "uid")
                public List<User> user;
                """, COMMON.USER
        ) { pojo ->
        }.failsToCompile().withErrorContaining(
                relationCannotFindParentEntityField("foo.bar.MyPojo", "idk", listOf("id"))
        )
    }

    @Test
    fun relation_missingEntityField() {
        singleRun(
                """
                int id;
                @Relation(parentField = "id", entityField = "idk")
                public List<User> user;
                """, COMMON.USER
        ) { pojo ->
        }.failsToCompile().withErrorContaining(
                relationCannotFindEntityField("foo.bar.User", "idk",
                        listOf("uid", "name", "lastName", "age"))
        )
    }

    @Test
    fun relation_missingType() {
        singleRun(
                """
                int id;
                @Relation(parentField = "id", entityField = "uid")
                public List<User> user;
                """
        ) { pojo ->
        }.failsToCompile().withErrorContaining(CANNOT_FIND_TYPE)
    }

    @Test
    fun relation_nestedField() {
        singleRun(
                """
                static class Nested {
                    @ColumnInfo(name = "foo")
                    public int id;
                }
                @Decompose
                Nested nested;
                @Relation(parentField = "nested.id", entityField = "uid")
                public List<User> user;
                """, COMMON.USER
        ) { pojo ->
            assertThat(pojo.relations.first().parentField.columnName, `is`("foo"))
        }.compilesWithoutError()
    }

    @Test
    fun relation_nestedRelation() {
        singleRun(
                """
                static class UserWithNested {
                    @Decompose
                    public User user;
                    @Relation(parentField = "user.uid", entityField = "uid")
                    public List<User> selfs;
                }
                int id;
                @Relation(parentField = "id", entityField = "uid", entity = User.class)
                public List<UserWithNested> user;
                """, COMMON.USER
        ) { pojo, invocation ->
            assertThat(pojo.relations.first().parentField.name, `is`("id"))
        }.compilesWithoutError().withWarningCount(0)
    }

    @Test
    fun relation_affinityMismatch() {
        singleRun(
                """
                String id;
                @Relation(parentField = "id", entityField = "uid")
                public List<User> user;
                """, COMMON.USER
        ) { pojo, invocation ->
            // trigger assignment evaluation
            RelationCollector.createCollectors(invocation.context, pojo.relations)
            assertThat(pojo.relations.size, `is`(1))
            assertThat(pojo.relations.first().entityField.name, `is`("uid"))
            assertThat(pojo.relations.first().parentField.name, `is`("id"))
        }.compilesWithoutError().withWarningContaining(
                ProcessorErrors.relationAffinityMismatch(
                        parentAffinity = SQLTypeAffinity.TEXT,
                        childAffinity = SQLTypeAffinity.INTEGER,
                        parentField = "id",
                        childField = "uid")
        )
    }

    @Test
    fun relation_simple() {
        singleRun(
                """
                int id;
                @Relation(parentField = "id", entityField = "uid")
                public List<User> user;
                """, COMMON.USER
        ) { pojo ->
            assertThat(pojo.relations.size, `is`(1))
            assertThat(pojo.relations.first().entityField.name, `is`("uid"))
            assertThat(pojo.relations.first().parentField.name, `is`("id"))
        }.compilesWithoutError().withWarningCount(0)
    }

    @Test
    fun relation_badProjection() {
        singleRun(
                """
                int id;
                @Relation(parentField = "id", entityField = "uid", projection={"i_dont_exist"})
                public List<User> user;
                """, COMMON.USER
        ) { pojo ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.relationBadProject("foo.bar.User", listOf("i_dont_exist"),
                        listOf("uid", "name", "lastName", "ageColumn"))
        )
    }

    fun singleRun(code: String, vararg jfos:JavaFileObject, handler: (Pojo) -> Unit)
            : CompileTester {
        return singleRun(code, *jfos) { pojo, invocation ->
            handler(pojo)
        }
    }

    fun singleRun(code: String, vararg jfos:JavaFileObject,
                  handler: (Pojo, TestInvocation) -> Unit): CompileTester {
        val pojoJFO = """
                $HEADER
                $code
                $FOOTER
                """.toJFO(MY_POJO.toString())
        val all = (jfos.toList() + pojoJFO).toTypedArray()
        return simpleRun(*all) { invocation ->
            handler.invoke(
                    PojoProcessor(baseContext = invocation.context,
                            element = invocation.typeElement(MY_POJO.toString()),
                            bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                            parent = null).process(),
                    invocation
            )
        }
    }
}
