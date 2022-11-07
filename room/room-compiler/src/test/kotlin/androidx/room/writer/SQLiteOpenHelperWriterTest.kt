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

import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.processor.DatabaseProcessor
import androidx.room.testing.context
import androidx.room.vo.Database
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SQLiteOpenHelperWriterTest {
    companion object {
        private const val DATABASE_PREFIX = """
            package foo.bar;
            import androidx.annotation.NonNull;
            import androidx.room.*;
        """
        private const val ENTITY_PREFIX = DATABASE_PREFIX + """
            @Entity%s
            public class MyEntity {
        """
        private const val ENTITY_SUFFIX = "}"
    }

    @Test
    fun createSimpleEntity() {
        singleEntity(
            """
                @PrimaryKey
                @NonNull
                String uuid;
                String name;
                int age;
            """.trimIndent()
        ) { database, _ ->
            val query = SQLiteOpenHelperWriter(database)
                .createTableQuery(database.entities.first())
            assertThat(
                query,
                `is`(
                    "CREATE TABLE IF NOT EXISTS" +
                        " `MyEntity` (`uuid` TEXT NOT NULL, `name` TEXT, `age` INTEGER NOT NULL," +
                        " PRIMARY KEY(`uuid`))"
                )
            )
        }
    }

    @Test
    fun multiplePrimaryKeys() {
        singleEntity(
            """
                @NonNull
                String uuid;
                @NonNull
                String name;
                int age;
            """.trimIndent(),
            attributes = mapOf("primaryKeys" to "{\"uuid\", \"name\"}")
        ) { database, _ ->
            val query = SQLiteOpenHelperWriter(database)
                .createTableQuery(database.entities.first())
            assertThat(
                query,
                `is`(
                    "CREATE TABLE IF NOT EXISTS" +
                        " `MyEntity` (`uuid` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                        "`age` INTEGER NOT NULL, PRIMARY KEY(`uuid`, `name`))"
                )
            )
        }
    }

    @Test
    fun autoIncrementObject() {
        listOf("Long", "Integer").forEach { type ->
            singleEntity(
                """
                @PrimaryKey(autoGenerate = true)
                $type uuid;
                String name;
                int age;
                """.trimIndent()
            ) { database, _ ->
                val query = SQLiteOpenHelperWriter(database)
                    .createTableQuery(database.entities.first())
                assertThat(
                    query,
                    `is`(
                        "CREATE TABLE IF NOT EXISTS" +
                            " `MyEntity` (`uuid` INTEGER PRIMARY KEY AUTOINCREMENT," +
                            " `name` TEXT, `age` INTEGER NOT NULL)"
                    )
                )
            }
        }
    }

    @Test
    fun autoIncrementPrimitives() {
        listOf("long", "int").forEach { type ->
            singleEntity(
                """
                @PrimaryKey(autoGenerate = true)
                $type uuid;
                String name;
                int age;
                """.trimIndent()
            ) { database, _ ->
                val query = SQLiteOpenHelperWriter(database)
                    .createTableQuery(database.entities.first())
                assertThat(
                    query,
                    `is`(
                        "CREATE TABLE IF NOT EXISTS" +
                            " `MyEntity` (`uuid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                            " `name` TEXT, `age` INTEGER NOT NULL)"
                    )
                )
            }
        }
    }

    @Test
    fun createSimpleView() {
        singleView("SELECT uuid, name FROM MyEntity") { database, _ ->
            val query = SQLiteOpenHelperWriter(database).createViewQuery(database.views.first())
            assertThat(query, `is`("CREATE VIEW `MyView` AS SELECT uuid, name FROM MyEntity"))
        }
    }

    private fun singleEntity(
        input: String,
        attributes: Map<String, String> = mapOf(),
        handler: (Database, XTestInvocation) -> Unit
    ) {
        val attributesReplacement = if (attributes.isEmpty()) {
            ""
        } else {
            "(" + attributes.entries.joinToString(",") { "${it.key} = ${it.value}" } + ")"
        }
        val entity = Source.java(
            "foo.bar.MyEntity",
            ENTITY_PREFIX.format(attributesReplacement) + input + ENTITY_SUFFIX
        )
        verify(listOf(entity), "", handler)
    }

    private fun singleView(
        query: String,
        handler: (Database, XTestInvocation) -> Unit
    ) {
        val entity = Source.java(
            "foo.bar.MyEntity",
            ENTITY_PREFIX.format("") + """
                    @PrimaryKey
                    @NonNull
                    String uuid;
                    @NonNull
                    String name;
                    int age;
            """ + ENTITY_SUFFIX
        )
        val view = Source.java(
            "foo.bar.MyView",
            DATABASE_PREFIX + """
                    @DatabaseView("$query")
                    public class MyView {
                        public String uuid;
                        public String name;
                    }
            """
        )
        return verify(listOf(entity, view), "views = {MyView.class},", handler)
    }

    private fun verify(
        sources: List<Source> = emptyList(),
        databaseAttribute: String,
        handler: (Database, XTestInvocation) -> Unit
    ) {
        val databaseCode = Source.java(
            "foo.bar.MyDatabase",
            """
            package foo.bar;
            import androidx.room.*;
            @Database(entities = {MyEntity.class}, $databaseAttribute version = 3)
            abstract public class MyDatabase extends RoomDatabase {
            }
            """
        )
        runProcessorTest(
            sources = sources + databaseCode
        ) { invocation ->
            val db = invocation.roundEnv
                .getElementsAnnotatedWith(androidx.room.Database::class.qualifiedName!!)
                .filterIsInstance<XTypeElement>()
                .first()
            handler(DatabaseProcessor(invocation.context, db).process(), invocation)
        }
    }
}
