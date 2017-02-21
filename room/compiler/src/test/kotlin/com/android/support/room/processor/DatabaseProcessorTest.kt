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

import com.android.support.room.RoomProcessor
import com.android.support.room.testing.TestInvocation
import com.android.support.room.testing.TestProcessor
import com.android.support.room.vo.Database
import com.android.support.room.vo.Warning
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.ClassName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

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
                    @Query("SELECT * FROM book")
                    public java.util.List<Book> loadAllBooks();
                }
                """)
    }

    @Test
    fun simple() {
        singleDb("""
            @Database(entities = {User.class})
            public abstract class MyDb extends RoomDatabase {
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

    @Test
    fun detectMissingTable() {
        singleDb(
                """
                @Database(entities = {Book.class})
                public abstract class MyDb extends RoomDatabase {
                    abstract BookDao bookDao();
                }
                """, BOOK, JavaFileObjects.forSourceString("foo.bar.BookDao",
                """
                package foo.bar;
                import com.android.support.room.*;
                @Dao
                public interface BookDao {
                    @Query("SELECT * FROM nonExistentTable")
                    public java.util.List<Book> loadAllBooks();
                }
                """)){ db, invocation ->

        }.failsToCompile().withErrorContaining("no such table: nonExistentTable")
    }

    @Test
    fun detectDuplicateTableNames() {
        singleDb("""
                @Database(entities = {User.class, AnotherClass.class})
                public abstract class MyDb extends RoomDatabase {
                    abstract UserDao userDao();
                }
                """, USER, USER_DAO, JavaFileObjects.forSourceString("foo.bar.AnotherClass",
                """
                package foo.bar;
                import com.android.support.room.*;
                @Entity(tableName="user")
                public class AnotherClass {
                    @PrimaryKey
                    int uid;
                }
                """)) { db, invocation ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.duplicateTableNames("user",
                        listOf("foo.bar.User", "foo.bar.AnotherClass"))
        )
    }

    @Test
    fun skipBadQueryVerification() {
        singleDb(
                """
                @SkipQueryVerification
                @Database(entities = {Book.class})
                public abstract class MyDb extends RoomDatabase {
                    abstract BookDao bookDao();
                }
                """, BOOK, JavaFileObjects.forSourceString("foo.bar.BookDao",
                """
                package foo.bar;
                import com.android.support.room.*;
                @Dao
                public interface BookDao {
                    @Query("SELECT nonExistingField FROM Book")
                    public java.util.List<Book> loadAllBooks();
                }
                """)){ db, invocation ->

        }.compilesWithoutError()
    }

    @Test
    fun multipleDatabases() {
        val db1 = JavaFileObjects.forSourceString("foo.bar.Db1",
                """
                $DATABASE_PREFIX
                @Database(entities = {Book.class})
                public abstract class Db1 extends RoomDatabase {
                    abstract BookDao bookDao();
                }
                """)
        val db2 = JavaFileObjects.forSourceString("foo.bar.Db2",
                """
                $DATABASE_PREFIX
                @Database(entities = {Book.class})
                public abstract class Db2 extends RoomDatabase {
                    abstract BookDao bookDao();
                }
                """)
        val db1_2 = JavaFileObjects.forSourceString("foo.barx.Db1",
                """
                package foo.barx;
                import com.android.support.room.*;
                import foo.bar.*;
                @Database(entities = {Book.class})
                public abstract class Db1 extends RoomDatabase {
                    abstract BookDao bookDao();
                }
                """)
        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(BOOK, BOOK_DAO, db1, db2, db1_2))
                .processedWith(RoomProcessor())
                .compilesWithoutError()
                .and()
                .generatesFileNamed(StandardLocation.CLASS_OUTPUT, "foo.bar", "Db1_Impl.class")
                .and()
                .generatesFileNamed(StandardLocation.CLASS_OUTPUT, "foo.bar", "Db2_Impl.class")
                .and()
                .generatesFileNamed(StandardLocation.CLASS_OUTPUT, "foo.barx", "Db1_Impl.class")
                .and()
                .generatesFileNamed(StandardLocation.CLASS_OUTPUT, "foo.bar",
                        "BookDao_Db1_0_Impl.class")
                .and()
                .generatesFileNamed(StandardLocation.CLASS_OUTPUT, "foo.bar",
                        "BookDao_Db1_1_Impl.class")
                .and()
                .generatesFileNamed(StandardLocation.CLASS_OUTPUT, "foo.bar",
                        "BookDao_Db2_Impl.class")
    }

    @Test
    fun twoDaoMethodsForTheSameDao() {
        singleDb(
                """
                @Database(entities = {User.class})
                public abstract class MyDb extends RoomDatabase {
                    abstract UserDao userDao();
                    abstract UserDao userDao2();
                }
                """, USER, USER_DAO){db, invocation -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.DAO_METHOD_CONFLICTS_WITH_OTHERS)
                .and()
                .withErrorContaining(ProcessorErrors.duplicateDao(
                        ClassName.get("foo.bar", "UserDao"), listOf("userDao", "userDao2")
                ))
    }

    @Test
    fun suppressedWarnings() {
        singleDb(
                """
                @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
                @Database(entities = {User.class})
                public abstract class MyDb extends RoomDatabase {
                    abstract UserDao userDao();
                }
                """, USER, USER_DAO) {db, invocation ->
            assertThat(DatabaseProcessor(invocation.context, db.element)
                    .context.logger.suppressedWarnings, `is`(setOf(Warning.CURSOR_MISMATCH)))
        }.compilesWithoutError()
    }

    @Test
    fun duplicateIndexNames() {
        val entity1 = JavaFileObjects.forSourceString("foo.bar.Entity1",
                """
                package foo.bar;
                import com.android.support.room.*;
                @Entity(indices = {@Index(name ="index_name", value = {"name"})})
                public class Entity1 {
                    @PrimaryKey
                    int uid;
                    String name;
                }
                """)

        val entity2 = JavaFileObjects.forSourceString("foo.bar.Entity2",
                """
                package foo.bar;
                import com.android.support.room.*;
                @Entity(indices = {@Index(name ="index_name", value = {"anotherName"})})
                public class Entity2 {
                    @PrimaryKey
                    int uid;
                    String anotherName;
                }
                """)
        singleDb("""
                @Database(entities = {Entity1.class, Entity2.class})
                public abstract class MyDb extends RoomDatabase {
                }
                """, entity1, entity2){ db, invocation ->

        }.failsToCompile().withErrorContaining(
                ProcessorErrors.duplicateIndexInDatabase("index_name",
                        listOf("foo.bar.Entity1 > index_name", "foo.bar.Entity2 > index_name"))
        )
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
                            val parser = DatabaseProcessor(invocation.context,
                                    MoreElements.asType(entity))
                            val parsedDb = parser.process()
                            handler(parsedDb, invocation)
                            true
                        }
                        .build())
    }
}
