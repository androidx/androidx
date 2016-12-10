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

package com.android.support.room.writer

import com.android.support.room.processor.DatabaseProcessor
import com.android.support.room.testing.TestInvocation
import com.android.support.room.testing.TestProcessor
import com.android.support.room.vo.Database
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
            import com.android.support.room.*;
            @Entity
            public class MyEntity {
            """
        const val ENTITY_SUFFIX = "}"
        const val DATABASE_CODE = """
            package foo.bar;
            import com.android.support.room.*;
            @Database(entities = {MyEntity.class})
            abstract public class MyDatabase extends RoomDatabase {
                public MyDatabase(DatabaseConfiguration configuration) {
                    super(configuration);
                }
            }
            """
    }

    @Test
    fun createSimpleEntity() {
        singleEntity(
                """
                @PrimaryKey
                String uuid;
                String name;
                int age;
                """.trimIndent()
        ) { database, invocation ->
            val query = SQLiteOpenHelperWriter(database)
                    .createQuery(database.entities.first())
            assertThat(query, `is`("CREATE TABLE IF NOT EXISTS" +
                    " `MyEntity` (`uuid` TEXT, `name` TEXT, `age` INTEGER, PRIMARY KEY(`uuid`))"))
        }.compilesWithoutError()
    }

    @Test
    fun multiplePrimaryKeys() {
        singleEntity(
                """
                @PrimaryKey
                String uuid;
                @PrimaryKey
                String name;
                int age;
                """.trimIndent()
        ) { database, invocation ->
            val query = SQLiteOpenHelperWriter(database)
                    .createQuery(database.entities.first())
            assertThat(query, `is`("CREATE TABLE IF NOT EXISTS" +
                    " `MyEntity` (`uuid` TEXT, `name` TEXT, `age` INTEGER," +
                    " PRIMARY KEY(`uuid`, `name`))"))
        }.compilesWithoutError()
    }

    fun singleEntity(input: String, attributes: Map<String, String> = mapOf(),
                     handler: (Database, TestInvocation) -> Unit): CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.MyEntity",
                        ENTITY_PREFIX + input + ENTITY_SUFFIX
                ), JavaFileObjects.forSourceString("foo.bar.MyDatabase",
                        DATABASE_CODE)))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(com.android.support.room.Database::class)
                        .nextRunHandler { invocation ->
                            val db = MoreElements.asType(invocation.roundEnv
                                    .getElementsAnnotatedWith(
                                            com.android.support.room.Database::class.java)
                                    .first())
                            handler(DatabaseProcessor(invocation.context).parse(db), invocation)
                            true
                        }
                        .build())
    }
}
