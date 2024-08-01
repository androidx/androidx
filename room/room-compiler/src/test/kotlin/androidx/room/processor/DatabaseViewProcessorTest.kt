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

import androidx.kruth.assertThat
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.parser.ParserErrors
import androidx.room.parser.SQLTypeAffinity
import androidx.room.runProcessorTestWithK1
import androidx.room.testing.context
import androidx.room.verifier.ColumnInfo
import androidx.room.vo.DatabaseView
import createVerifierFromEntitiesAndViews
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DatabaseViewProcessorTest {

    companion object {
        const val DATABASE_PREFIX =
            """
            package foo.bar;
            import androidx.room.*;
            import androidx.annotation.NonNull;
            import java.util.*;
        """

        val ENTITIES =
            listOf(
                Source.java(
                    "foo.bar.Team",
                    DATABASE_PREFIX +
                        """
                    @Entity
                    public class Team {
                        @PrimaryKey
                        public int id;
                        public String name;
                    }
                """
                ),
                Source.java(
                    "foo.bar.Employee",
                    DATABASE_PREFIX +
                        """
                    @Entity
                    public class Employee {
                        @PrimaryKey
                        public int id;
                        public String name;
                        public String fullName;
                        public int teamId;
                        public Integer managerId;
                    }
                """
                )
            )
    }

    @Test
    fun basic() {
        singleView(
            "foo.bar.MyView",
            """
            @DatabaseView("SELECT * FROM Team")
            public class MyView {
            }
        """
        ) { view, _ ->
            assertThat(view.query.original).isEqualTo("SELECT * FROM Team")
            assertThat(view.query.errors).isEmpty()
            assertThat(view.query.tables).hasSize(1)
            assertThat(view.query.tables.first().name).isEqualTo("Team")
            assertThat(view.constructor).isNotNull()
            assertThat(view.constructor?.params).isEmpty()
            assertThat(view.query.resultInfo).isNotNull()
            val resultInfo = view.query.resultInfo!!
            assertThat(resultInfo.columns).hasSize(2)
            assertThat(resultInfo.columns)
                .containsAtLeast(
                    ColumnInfo("id", SQLTypeAffinity.INTEGER, "Team"),
                    ColumnInfo("name", SQLTypeAffinity.TEXT, "Team")
                )
            assertThat(view.viewName).isEqualTo("MyView")
        }
    }

    @Test
    fun viewName() {
        singleView(
            "foo.bar.MyView",
            """
            @DatabaseView(value = "SELECT * FROM Team", viewName = "abc")
            public class MyView {
            }
        """
        ) { view, _ ->
            assertThat(view.viewName).isEqualTo("abc")
        }
    }

    @Test
    fun viewName_sqlite() {
        singleView(
            "foo.bar.MyView",
            """
            @DatabaseView(value = "SELECT * FROM Team", viewName = "sqlite_bad_name")
            public class MyView {
            }
        """,
            verify = false
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.VIEW_NAME_CANNOT_START_WITH_SQLITE)
            }
        }
    }

    @Test
    fun nonSelect() {
        singleView(
            "foo.bar.MyView",
            """
            @DatabaseView("DELETE FROM Team")
            public class MyView {
            }
        """,
            verify = false
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.VIEW_QUERY_MUST_BE_SELECT)
            }
        }
    }

    @Test
    fun arguments() {
        singleView(
            "foo.bar.MyView",
            """
            @DatabaseView("SELECT * FROM Team WHERE id = :id")
            public class MyView {
            }
        """,
            verify = false
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.VIEW_QUERY_CANNOT_TAKE_ARGUMENTS)
            }
        }
    }

    @Test
    fun emptyQuery() {
        singleView(
            "foo.bar.MyView",
            """
            @DatabaseView("")
            public class MyView {
            }
        """,
            verify = false
        ) { _, invocation ->
            invocation.assertCompilationResult { hasErrorContaining(ParserErrors.NOT_ONE_QUERY) }
        }
    }

    @Test
    fun missingAnnotation() {
        singleView(
            "foo.bar.MyView",
            """
            public class MyView {
            }
        """,
            verify = false
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.VIEW_MUST_BE_ANNOTATED_WITH_DATABASE_VIEW)
            }
        }
    }

    @Test
    fun referenceOtherView() {
        val summary =
            Source.java(
                "foo.bar.EmployeeSummary",
                DATABASE_PREFIX +
                    """
                    @DatabaseView("SELECT id, name, managerId FROM Employee")
                    public class EmployeeSummary {
                        public int id;
                        public String name;
                        public Integer managerId;
                    }
                """
            )
        singleView(
            "foo.bar.EmployeeName",
            """
            @DatabaseView("SELECT id, name FROM EmployeeSummary")
            public class EmployeeName {
                public int id;
                public String name;
            }
        """,
            ENTITIES + summary
        ) { view, _ ->
            assertThat(view.viewName).isEqualTo("EmployeeName")
        }
    }

    private fun singleView(
        name: String,
        input: String,
        sources: List<Source> = ENTITIES,
        verify: Boolean = true,
        handler: (view: DatabaseView, invocation: XTestInvocation) -> Unit
    ) {
        runProcessorTestWithK1(sources = sources + Source.java(name, DATABASE_PREFIX + input)) {
            invocation ->
            val view = invocation.processingEnv.requireTypeElement(name)
            val verifier =
                if (verify) {
                    createVerifierFromEntitiesAndViews(invocation)
                } else null
            val processor = DatabaseViewProcessor(invocation.context, view)
            val processedView = processor.process()
            processedView.query.resultInfo = verifier?.analyze(processedView.query.original)
            handler(processedView, invocation)
        }
    }
}
