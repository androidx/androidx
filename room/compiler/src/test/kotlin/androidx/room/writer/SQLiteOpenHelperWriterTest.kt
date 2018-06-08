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

import androidx.annotation.NonNull
import androidx.room.processor.DatabaseProcessor
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.vo.Database
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SQLiteOpenHelperWriterTest {
    companion object {
        const val ENTITY_PREFIX = """
            package foo.bar;
            import androidx.annotation.NonNull;
            import androidx.room.*;
            @Entity%s
            public class MyEntity {
            """
        const val ENTITY_SUFFIX = "}"
        const val DATABASE_CODE = """
            package foo.bar;
            import androidx.room.*;
            @Database(entities = {MyEntity.class}, version = 3)
            abstract public class MyDatabase extends RoomDatabase {
            }
            """
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
                    .createQuery(database.entities.first())
            assertThat(query, `is`("CREATE TABLE IF NOT EXISTS" +
                    " `MyEntity` (`uuid` TEXT NOT NULL, `name` TEXT, `age` INTEGER NOT NULL," +
                    " PRIMARY KEY(`uuid`))"))
        }.compilesWithoutError()
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
                """.trimIndent(), attributes = mapOf("primaryKeys" to "{\"uuid\", \"name\"}")
        ) { database, _ ->
            val query = SQLiteOpenHelperWriter(database)
                    .createQuery(database.entities.first())
            assertThat(query, `is`("CREATE TABLE IF NOT EXISTS" +
                    " `MyEntity` (`uuid` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                    "`age` INTEGER NOT NULL, PRIMARY KEY(`uuid`, `name`))"))
        }.compilesWithoutError()
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
                        .createQuery(database.entities.first())
                assertThat(query, `is`("CREATE TABLE IF NOT EXISTS" +
                        " `MyEntity` (`uuid` INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " `name` TEXT, `age` INTEGER NOT NULL)"))
            }.compilesWithoutError()
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
                        .createQuery(database.entities.first())
                assertThat(query, `is`("CREATE TABLE IF NOT EXISTS" +
                        " `MyEntity` (`uuid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                        " `name` TEXT, `age` INTEGER NOT NULL)"))
            }.compilesWithoutError()
        }
    }

    fun singleEntity(input: String, attributes: Map<String, String> = mapOf(),
                     handler: (Database, TestInvocation) -> Unit): CompileTester {
        val attributesReplacement: String
        if (attributes.isEmpty()) {
            attributesReplacement = ""
        } else {
            attributesReplacement = "(" +
                    attributes.entries.joinToString(",") { "${it.key} = ${it.value}" } +
                    ")".trimIndent()
        }
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.MyEntity",
                        ENTITY_PREFIX.format(attributesReplacement) + input + ENTITY_SUFFIX
                ), JavaFileObjects.forSourceString("foo.bar.MyDatabase",
                        DATABASE_CODE)))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(androidx.room.Database::class,
                                NonNull::class)
                        .nextRunHandler { invocation ->
                            val db = MoreElements.asType(invocation.roundEnv
                                    .getElementsAnnotatedWith(
                                            androidx.room.Database::class.java)
                                    .first())
                            handler(DatabaseProcessor(invocation.context, db).process(), invocation)
                            true
                        }
                        .build())
    }
}
