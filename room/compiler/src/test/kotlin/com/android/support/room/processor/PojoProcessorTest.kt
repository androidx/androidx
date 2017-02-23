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

import com.android.support.room.processor.ProcessorErrors.POJO_FIELD_HAS_DUPLICATE_COLUMN_NAME
import com.android.support.room.testing.TestInvocation
import com.android.support.room.vo.Pojo
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

    fun singleRun(code: String, handler: (Pojo) -> Unit): CompileTester {
        return singleRun(code) { pojo, invocation ->
            handler(pojo)
        }
    }

    fun singleRun(code: String, handler: (Pojo, TestInvocation) -> Unit): CompileTester {
        return simpleRun(
                """
                $HEADER
                $code
                $FOOTER
                """.toJFO(MY_POJO.toString())) { invocation ->
            handler.invoke(
                    PojoProcessor(baseContext = invocation.context,
                            element = invocation.typeElement(MY_POJO.toString()),
                            bindingScope = FieldProcessor.BindingScope.BIND_TO_STMT,
                            parent = null).process(),
                    invocation
            )
        }
    }
}
