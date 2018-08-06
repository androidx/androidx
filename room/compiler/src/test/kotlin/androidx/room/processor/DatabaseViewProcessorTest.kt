/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.annotation.NonNull
import androidx.room.parser.ParserErrors
import androidx.room.parser.SQLTypeAffinity
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.verifier.ColumnInfo
import androidx.room.vo.DatabaseView
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import createVerifierFromEntitiesAndViews
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.tools.JavaFileObject

@RunWith(JUnit4::class)
class DatabaseViewProcessorTest {

    companion object {
        const val DATABASE_PREFIX = """
            package foo.bar;
            import androidx.room.*;
            import androidx.annotation.NonNull;
            import java.util.*;
        """

        val ENTITIES = listOf(
                JavaFileObjects.forSourceString("foo.bar.Team", DATABASE_PREFIX + """
                    @Entity
                    public class Team {
                        @PrimaryKey
                        public int id;
                        public String name;
                    }
                """),
                JavaFileObjects.forSourceString("foo.bar.Employee", DATABASE_PREFIX + """
                    @Entity
                    public class Employee {
                        @PrimaryKey
                        public int id;
                        public String name;
                        public String fullName;
                        public int teamId;
                        public Integer managerId;
                    }
                """)
        )
    }

    @Test
    fun basic() {
        singleView("foo.bar.MyView", """
            @DatabaseView("SELECT * FROM Team")
            public class MyView {
            }
        """) { view, _ ->
            assertThat(view.query.original).isEqualTo("SELECT * FROM Team")
            assertThat(view.query.errors).isEmpty()
            assertThat(view.query.tables).hasSize(1)
            assertThat(view.query.tables.first().name).isEqualTo("Team")
            assertThat(view.constructor).isNotNull()
            assertThat(view.constructor?.params).isEmpty()
            assertThat(view.query.resultInfo).isNotNull()
            val resultInfo = view.query.resultInfo!!
            assertThat(resultInfo.columns).hasSize(2)
            assertThat(resultInfo.columns).containsAllOf(
                    ColumnInfo("id", SQLTypeAffinity.INTEGER),
                    ColumnInfo("name", SQLTypeAffinity.TEXT))
            assertThat(view.viewName).isEqualTo("MyView")
        }.compilesWithoutError()
    }

    @Test
    fun viewName() {
        singleView("foo.bar.MyView", """
            @DatabaseView(value = "SELECT * FROM Team", viewName = "abc")
            public class MyView {
            }
        """) { view, _ ->
            assertThat(view.viewName).isEqualTo("abc")
        }.compilesWithoutError()
    }

    @Test
    fun viewName_sqlite() {
        singleView("foo.bar.MyView", """
            @DatabaseView(value = "SELECT * FROM Team", viewName = "sqlite_bad_name")
            public class MyView {
            }
        """, verify = false) { _, _ ->
        }.failsToCompile().withErrorContaining(ProcessorErrors.VIEW_NAME_CANNOT_START_WITH_SQLITE)
    }

    @Test
    fun nonSelect() {
        singleView("foo.bar.MyView", """
            @DatabaseView("DELETE FROM Team")
            public class MyView {
            }
        """, verify = false) { _, _ ->
        }.failsToCompile().withErrorContaining(ProcessorErrors.VIEW_QUERY_MUST_BE_SELECT)
    }

    @Test
    fun arguments() {
        singleView("foo.bar.MyView", """
            @DatabaseView("SELECT * FROM Team WHERE id = :id")
            public class MyView {
            }
        """, verify = false) { _, _ ->
        }.failsToCompile().withErrorContaining(ProcessorErrors.VIEW_QUERY_CANNOT_TAKE_ARGUMENTS)
    }

    @Test
    fun emptyQuery() {
        singleView("foo.bar.MyView", """
            @DatabaseView("")
            public class MyView {
            }
        """, verify = false) { _, _ ->
        }.failsToCompile().withErrorContaining(ParserErrors.NOT_ONE_QUERY)
    }

    @Test
    fun missingAnnotation() {
        singleView("foo.bar.MyView", """
            public class MyView {
            }
        """, verify = false) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.VIEW_MUST_BE_ANNOTATED_WITH_DATABASE_VIEW)
    }

    @Test
    fun referenceOtherView() {
        val summary = JavaFileObjects.forSourceString("foo.bar.EmployeeSummary",
                DATABASE_PREFIX + """
                    @DatabaseView("SELECT id, name, managerId FROM Employee")
                    public class EmployeeSummary {
                        public int id;
                        public String name;
                        public Integer managerId;
                    }
                """)
        singleView("foo.bar.EmployeeName", """
            @DatabaseView("SELECT id, name FROM EmployeeSummary")
            public class EmployeeName {
                public int id;
                public String name;
            }
        """, ENTITIES + summary) { view, _ ->
            assertThat(view.viewName).isEqualTo("EmployeeName")
        }.compilesWithoutError()
    }

    private fun singleView(
        name: String,
        input: String,
        jfos: List<JavaFileObject> = ENTITIES,
        verify: Boolean = true,
        classLoader: ClassLoader = javaClass.classLoader,
        handler: (view: DatabaseView, invocation: TestInvocation) -> Unit
    ): CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(jfos + JavaFileObjects.forSourceString(name, DATABASE_PREFIX + input))
                .withClasspathFrom(classLoader)
                .processedWith(TestProcessor.builder()
                        .forAnnotations(
                                androidx.room.DatabaseView::class,
                                androidx.room.Entity::class,
                                androidx.room.PrimaryKey::class,
                                androidx.room.Embedded::class,
                                androidx.room.ColumnInfo::class,
                                NonNull::class)
                        .nextRunHandler { invocation ->
                            val view = invocation.roundEnv
                                    .rootElements
                                    .first { it.toString() == name }
                            val verifier = if (verify) {
                                createVerifierFromEntitiesAndViews(invocation)
                            } else null
                            val processor = DatabaseViewProcessor(invocation.context,
                                    MoreElements.asType(view))
                            val processedView = processor.process()
                            processedView.query.resultInfo =
                                    verifier?.analyze(processedView.query.original)
                            handler(processedView, invocation)
                            true
                        }
                        .build())
    }
}
