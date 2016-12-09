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

package com.android.support.room.processor

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
import javax.tools.JavaFileObject

@RunWith(JUnit4::class)
class DatabaseProcessorTest {
    companion object {
        const val DATABASE_PREFIX = """
            package foo.bar;
            import com.android.support.room.*;
            """
        val USER: JavaFileObject = JavaFileObjects.forSourceString("foo.bar.User",
                """
                package foo.bar;
                import com.android.support.room.*;
                @Entity
                public class User {
                    @PrimaryKey
                    int uid;
                }
                """)
        val USER_DAO: JavaFileObject = JavaFileObjects.forSourceString("foo.bar.UserDao",
                """
                package foo.bar;
                import com.android.support.room.*;
                @Dao
                public interface UserDao {
                    @Query("SELECT * FROM user")
                    public java.util.List<User> loadAll();
                }
                """)
        val BOOK: JavaFileObject = JavaFileObjects.forSourceString("foo.bar.Book",
                """
                package foo.bar;
                import com.android.support.room.*;
                @Entity
                public class Book {
                    @PrimaryKey
                    int bookId;
                }
                """)
        val BOOK_DAO: JavaFileObject = JavaFileObjects.forSourceString("foo.bar.BookDao",
                """
                package foo.bar;
                import com.android.support.room.*;
                @Dao
                public interface BookDao {
                    @Query("SELECT * FROM books")
                    public java.util.List<Book> loadAllBooks();
                }
                """)
    }

    @Test
    fun simple() {
        singleDb("""
            @Database(entities = {User.class})
            public abstract class MyDb extends RoomDatabase {
                public MyDb(DatabaseConfiguration config) {
                    super(config);
                }
                abstract UserDao userDao();
            }
            """, USER, USER_DAO) { db, invocation ->
            assertThat(db.daoMethods.size, `is`(1))
            assertThat(db.entities.size, `is`(1))
        }.compilesWithoutError()
    }

    @Test
    fun multiple() {
        singleDb("""
            @Database(entities = {User.class, Book.class})
            public abstract class MyDb extends RoomDatabase {
                public MyDb(DatabaseConfiguration config) {
                    super(config);
                }
                abstract UserDao userDao();
                abstract BookDao bookDao();
            }
            """, USER, USER_DAO, BOOK, BOOK_DAO) { db, invocation ->
            assertThat(db.daoMethods.size, `is`(2))
            assertThat(db.entities.size, `is`(2))
            assertThat(db.daoMethods.map { it.name }, `is`(listOf("userDao", "bookDao")))
            assertThat(db.entities.map { it.type.toString() },
                    `is`(listOf("foo.bar.User", "foo.bar.Book")))
        }.compilesWithoutError()
    }

    @Test
    fun detectMissingBaseClass() {
        singleDb("""
            @Database(entities = {User.class, Book.class})
            public abstract class MyDb {
            }
            """, USER, BOOK) { db, invocation ->
        }.failsToCompile().withErrorContaining(ProcessorErrors.DB_MUST_EXTEND_ROOM_DB)
    }

    fun singleDb(input: String, vararg otherFiles: JavaFileObject,
                 handler: (Database, TestInvocation) -> Unit): CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(otherFiles.toMutableList()
                        + JavaFileObjects.forSourceString("foo.bar.MyDb",
                        DATABASE_PREFIX + input
                ))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(com.android.support.room.Database::class)
                        .nextRunHandler { invocation ->
                            val entity = invocation.roundEnv
                                    .getElementsAnnotatedWith(
                                            com.android.support.room.Database::class.java)
                                    .first()
                            val parser = DatabaseProcessor(invocation.context)
                            val parsedDb = parser.parse(MoreElements.asType(entity))
                            handler(parsedDb, invocation)
                            true
                        }
                        .build())
    }
}
