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

import com.android.support.room.Entity
import com.android.support.room.ext.L
import com.android.support.room.ext.T
import com.android.support.room.processor.Context
import com.android.support.room.solver.types.CompositeAdapter
import com.android.support.room.solver.types.TypeConverter
import com.android.support.room.testing.TestInvocation
import com.android.support.room.testing.TestProcessor
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.TypeKind

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class TypeAdapterStoreTest {
    companion object {
        fun tmp(index: Int) = CodeGenScope._tmpVar(index)
    }

    @Test
    fun testDirect() {
        singleRun { invocation ->
            val store = TypeAdapterStore(Context(invocation.processingEnv))
            val primitiveType = invocation.processingEnv.typeUtils.getPrimitiveType(TypeKind.INT)
            val adapter = store.findColumnTypeAdapter(primitiveType)
            assertThat(adapter, notNullValue())
        }.compilesWithoutError()
    }

    @Test
    fun testVia1TypeAdapter() {
        singleRun { invocation ->
            val store = TypeAdapterStore(Context(invocation.processingEnv))
            val booleanType = invocation.processingEnv.typeUtils
                    .getPrimitiveType(TypeKind.BOOLEAN)
            val adapter = store.findColumnTypeAdapter(booleanType)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(CompositeAdapter::class.java))
            val bindScope = CodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            assertThat(bindScope.generate().trim(), `is`("""
                    final int ${tmp(0)};
                    ${tmp(0)} = fooVar ? 1 : 0;
                    stmt.bindLong(41, ${tmp(0)});
                    """.trimIndent()))

            val cursorScope = CodeGenScope()
            adapter.readFromCursor("res", "curs", "7", cursorScope)
            assertThat(cursorScope.generate().trim(), `is`("""
                    final int ${tmp(0)};
                    ${tmp(0)} = curs.getInt(7);
                    res = ${tmp(0)} != 0;
                    """.trimIndent()))
        }.compilesWithoutError()
    }

    @Test
    fun testVia2TypeAdapters() {
        singleRun { invocation ->
            val store = TypeAdapterStore(Context(invocation.processingEnv),
                    PointTypeConverter(invocation.processingEnv))
            val pointType = invocation.processingEnv.elementUtils
                    .getTypeElement("foo.bar.Point").asType()
            val adapter = store.findColumnTypeAdapter(pointType)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(CompositeAdapter::class.java))

            val bindScope = CodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            assertThat(bindScope.generate().trim(), `is`("""
                    final int ${tmp(0)};
                    final boolean ${tmp(1)};
                    ${tmp(1)} = foo.bar.Point.toBoolean(fooVar);
                    ${tmp(0)} = ${tmp(1)} ? 1 : 0;
                    stmt.bindLong(41, ${tmp(0)});
                    """.trimIndent()))

            val cursorScope = CodeGenScope()
            adapter.readFromCursor("res", "curs", "11", cursorScope).toString()
            assertThat(cursorScope.generate().trim(), `is`("""
                    final int ${tmp(0)};
                    ${tmp(0)} = curs.getInt(11);
                    final boolean ${tmp(1)};
                    ${tmp(1)} = ${tmp(0)} != 0;
                    res = foo.bar.Point.fromBoolean(${tmp(1)});
                    """.trimIndent()))
        }.compilesWithoutError()
    }

    @Test
    fun testIntList() {
        singleRun { invocation ->
            val store = TypeAdapterStore(Context(invocation.processingEnv))
            val intType = invocation.processingEnv.elementUtils
                    .getTypeElement(Integer::class.java.canonicalName)
                    .asType()
            val listType = invocation.processingEnv.elementUtils
                    .getTypeElement(java.util.List::class.java.canonicalName)
            val listOfInts = invocation.processingEnv.typeUtils.getDeclaredType(listType, intType)
            val adapter = store.findColumnTypeAdapter(listOfInts)
            assertThat(adapter, notNullValue())

            val bindScope = CodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            assertThat(bindScope.generate().trim(), `is`("""
                    final java.lang.String ${tmp(0)};
                    ${tmp(0)} = com.android.support.room.util.StringUtil.joinIntoString(fooVar);
                    if (${tmp(0)} == null) {
                      stmt.bindNull(41);
                    } else {
                      stmt.bindString(41, ${tmp(0)});
                    }
                    """.trimIndent()))
        }.compilesWithoutError()
    }

    fun singleRun(handler: (TestInvocation) -> Unit): CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.DummyClass",
                        """
                        package foo.bar;
                        import com.android.support.room.*;
                        @Entity
                        public class DummyClass {}
                        """
                ), JavaFileObjects.forSourceString("foo.bar.Point",
                        """
                        package foo.bar;
                        import com.android.support.room.*;
                        @Entity
                        public class Point {
                            public int x, y;
                            public Point(int x, int y) {
                                this.x = x;
                                this.y = y;
                            }
                            public static Point fromBoolean(boolean val) {
                                return val ? new Point(1, 1) : new Point(0, 0);
                            }
                            public static boolean toBoolean(Point point) {
                                return point.x > 0;
                            }
                        }
                        """
                )))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(Entity::class)
                        .nextRunHandler { invocation ->
                            handler(invocation)
                            true
                        }
                        .build())
    }

    class PointTypeConverter(processingEnv: ProcessingEnvironment) : TypeConverter(
            from = processingEnv.elementUtils.getTypeElement("foo.bar.Point").asType(),
            to = processingEnv.typeUtils.getPrimitiveType(TypeKind.BOOLEAN)) {
        override fun convertForward(inputVarName: String, outputVarName: String,
                                    scope: CodeGenScope) {
            scope.builder().apply {
                addStatement("$L = $T.toBoolean($L)", outputVarName, from, inputVarName)
            }
        }

        override fun convertBackward(inputVarName: String, outputVarName: String,
                                     scope: CodeGenScope) {
            scope.builder().apply {
                addStatement("$L = $T.fromBoolean($L)", outputVarName, from, inputVarName)
            }
        }
    }
}
