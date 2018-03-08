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

package androidx.room.processor

import COMMON
import androidx.room.RoomProcessor
import androidx.room.solver.query.result.EntityRowAdapter
import androidx.room.solver.query.result.PojoRowAdapter
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.vo.Database
import androidx.room.vo.Warning
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.ClassName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.sameInstance
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
            import androidx.room.*;
            """
        val DB1: JavaFileObject = JavaFileObjects.forSourceString("foo.bar.Db1",
                """
                $DATABASE_PREFIX
                @Database(entities = {Book.class}, version = 42)
                public abstract class Db1 extends RoomDatabase {
                    abstract BookDao bookDao();
                }
                """)
        val DB2: JavaFileObject = JavaFileObjects.forSourceString("foo.bar.Db2",
                """
                $DATABASE_PREFIX
                @Database(entities = {Book.class}, version = 42)
                public abstract class Db2 extends RoomDatabase {
                    abstract BookDao bookDao();
                }
                """)
        val DB3: JavaFileObject = JavaFileObjects.forSourceString("foo.bar.Db3",
                """
                $DATABASE_PREFIX
                @Database(entities = {Book.class}, version = 42)
                public abstract class Db3 extends RoomDatabase {
                }
                """)
        val USER: JavaFileObject = JavaFileObjects.forSourceString("foo.bar.User",
                """
                package foo.bar;
                import androidx.room.*;
                @Entity
                public class User {
                    @PrimaryKey
                    int uid;
                }
                """)
        val USER_DAO: JavaFileObject = JavaFileObjects.forSourceString("foo.bar.UserDao",
                """
                package foo.bar;
                import androidx.room.*;
                @Dao
                public interface UserDao {
                    @Query("SELECT * FROM user")
                    public java.util.List<User> loadAll();

                    @Insert
                    public void insert(User... users);

                    @Query("SELECT * FROM user where uid = :uid")
                    public User loadOne(int uid);

                    @TypeConverters(Converter.class)
                    @Query("SELECT * FROM user where uid = :uid")
                    public User loadWithConverter(int uid);

                    @Query("SELECT * FROM user where uid = :uid")
                    public Pojo loadOnePojo(int uid);

                    @Query("SELECT * FROM user")
                    public java.util.List<Pojo> loadAllPojos();

                    @TypeConverters(Converter.class)
                    @Query("SELECT * FROM user where uid = :uid")
                    public Pojo loadPojoWithConverter(int uid);

                    public static class Converter {
                        @TypeConverter
                        public static java.util.Date foo(Long input) {return null;}
                    }

                    public static class Pojo {
                        public int uid;
                    }
                }
                """)
        val BOOK: JavaFileObject = JavaFileObjects.forSourceString("foo.bar.Book",
                """
                package foo.bar;
                import androidx.room.*;
                @Entity
                public class Book {
                    @PrimaryKey
                    int bookId;
                }
                """)
        val BOOK_DAO: JavaFileObject = JavaFileObjects.forSourceString("foo.bar.BookDao",
                """
                package foo.bar;
                import androidx.room.*;
                @Dao
                public interface BookDao {
                    @Query("SELECT * FROM book")
                    public java.util.List<Book> loadAllBooks();
                    @Insert
                    public void insert(Book book);
                }
                """)
    }

    @Test
    fun simple() {
        singleDb("""
            @Database(entities = {User.class}, version = 42)
            public abstract class MyDb extends RoomDatabase {
                abstract UserDao userDao();
            }
            """, USER, USER_DAO) { db, _ ->
            assertThat(db.daoMethods.size, `is`(1))
            assertThat(db.entities.size, `is`(1))
        }.compilesWithoutError()
    }

    @Test
    fun multiple() {
        singleDb("""
            @Database(entities = {User.class, Book.class}, version = 42)
            public abstract class MyDb extends RoomDatabase {
                abstract UserDao userDao();
                abstract BookDao bookDao();
            }
            """, USER, USER_DAO, BOOK, BOOK_DAO) { db, _ ->
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
            @Database(entities = {User.class, Book.class}, version = 42)
            public abstract class MyDb {
            }
            """, USER, BOOK) { _, _ ->
        }.failsToCompile().withErrorContaining(ProcessorErrors.DB_MUST_EXTEND_ROOM_DB)
    }

    @Test
    fun detectMissingTable() {
        singleDb(
                """
                @Database(entities = {Book.class}, version = 42)
                public abstract class MyDb extends RoomDatabase {
                    abstract BookDao bookDao();
                }
                """, BOOK, JavaFileObjects.forSourceString("foo.bar.BookDao",
                """
                package foo.bar;
                import androidx.room.*;
                @Dao
                public interface BookDao {
                    @Query("SELECT * FROM nonExistentTable")
                    public java.util.List<Book> loadAllBooks();
                }
                """)) { _, _ ->
        }.failsToCompile().withErrorContaining("no such table: nonExistentTable")
    }

    @Test
    fun detectDuplicateTableNames() {
        singleDb("""
                @Database(entities = {User.class, AnotherClass.class}, version = 42)
                public abstract class MyDb extends RoomDatabase {
                    abstract UserDao userDao();
                }
                """, USER, USER_DAO, JavaFileObjects.forSourceString("foo.bar.AnotherClass",
                """
                package foo.bar;
                import androidx.room.*;
                @Entity(tableName="user")
                public class AnotherClass {
                    @PrimaryKey
                    int uid;
                }
                """)) { _, _ ->
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
                @Database(entities = {Book.class}, version = 42)
                public abstract class MyDb extends RoomDatabase {
                    abstract BookDao bookDao();
                }
                """, BOOK, JavaFileObjects.forSourceString("foo.bar.BookDao",
                """
                package foo.bar;
                import androidx.room.*;
                @Dao
                public interface BookDao {
                    @Query("SELECT nonExistingField FROM Book")
                    public java.util.List<Book> loadAllBooks();
                }
                """)) { _, _ ->
        }.compilesWithoutError()
    }

    @Test
    fun multipleDatabases() {
        val db1_2 = JavaFileObjects.forSourceString("foo.barx.Db1",
                """
                package foo.barx;
                import androidx.room.*;
                import foo.bar.*;
                @Database(entities = {Book.class}, version = 42)
                public abstract class Db1 extends RoomDatabase {
                    abstract BookDao bookDao();
                }
                """)
        Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(BOOK, BOOK_DAO, DB1, DB2, db1_2))
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
                @Database(entities = {User.class}, version = 42)
                public abstract class MyDb extends RoomDatabase {
                    abstract UserDao userDao();
                    abstract UserDao userDao2();
                }
                """, USER, USER_DAO) { _, _ -> }
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
                @Database(entities = {User.class}, version = 42)
                public abstract class MyDb extends RoomDatabase {
                    abstract UserDao userDao();
                }
                """, USER, USER_DAO) { db, invocation ->
            assertThat(DatabaseProcessor(invocation.context, db.element)
                    .context.logger.suppressedWarnings, `is`(setOf(Warning.CURSOR_MISMATCH)))
        }.compilesWithoutError()
    }

    @Test
    fun duplicateIndexNames() {
        val entity1 = JavaFileObjects.forSourceString("foo.bar.Entity1",
                """
                package foo.bar;
                import androidx.room.*;
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
                import androidx.room.*;
                @Entity(indices = {@Index(name ="index_name", value = {"anotherName"})})
                public class Entity2 {
                    @PrimaryKey
                    int uid;
                    String anotherName;
                }
                """)
        singleDb("""
                @Database(entities = {Entity1.class, Entity2.class}, version = 42)
                public abstract class MyDb extends RoomDatabase {
                }
                """, entity1, entity2) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.duplicateIndexInDatabase("index_name",
                        listOf("foo.bar.Entity1 > index_name", "foo.bar.Entity2 > index_name"))
        )
    }

    @Test
    fun foreignKey_missingParent() {
        val entity1 = JavaFileObjects.forSourceString("foo.bar.Entity1",
                """
                package foo.bar;
                import androidx.room.*;
                @Entity(foreignKeys = @ForeignKey(entity = ${COMMON.USER_TYPE_NAME}.class,
                        parentColumns = "lastName",
                        childColumns = "name"))
                public class Entity1 {
                    @PrimaryKey
                    int uid;
                    String name;
                }
                """)
        singleDb("""
                @Database(entities = {Entity1.class}, version = 42)
                public abstract class MyDb extends RoomDatabase {
                }
                """, entity1, COMMON.USER) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.foreignKeyMissingParentEntityInDatabase("User", "foo.bar.Entity1")
        )
    }

    @Test
    fun foreignKey_missingParentIndex() {
        val entity1 = JavaFileObjects.forSourceString("foo.bar.Entity1",
                """
                package foo.bar;
                import androidx.room.*;
                @Entity(foreignKeys = @ForeignKey(entity = ${COMMON.USER_TYPE_NAME}.class,
                        parentColumns = "lastName",
                        childColumns = "name"))
                public class Entity1 {
                    @PrimaryKey
                    int uid;
                    String name;
                }
                """)
        singleDb("""
                @Database(entities = {Entity1.class, User.class}, version = 42)
                public abstract class MyDb extends RoomDatabase {
                }
                """, entity1, COMMON.USER) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.foreignKeyMissingIndexInParent(
                        parentEntity = COMMON.USER_TYPE_NAME.toString(),
                        parentColumns = listOf("lastName"),
                        childEntity = "foo.bar.Entity1",
                        childColumns = listOf("name")
                )
        )
    }

    @Test
    fun foreignKey_goodWithPrimaryKey() {
        val entity1 = JavaFileObjects.forSourceString("foo.bar.Entity1",
                """
                package foo.bar;
                import androidx.room.*;
                @Entity(foreignKeys = @ForeignKey(entity = Entity2.class,
                    parentColumns = "uid",
                    childColumns = "parentId"))
                public class Entity1 {
                    @PrimaryKey
                    int uid;
                    int parentId;
                    String name;
                }
                """)

        val entity2 = JavaFileObjects.forSourceString("foo.bar.Entity2",
                """
                package foo.bar;
                import androidx.room.*;
                @Entity
                public class Entity2 {
                    @PrimaryKey
                    int uid;
                    String anotherName;
                }
                """)
        singleDb("""
                @Database(entities = {Entity1.class, Entity2.class}, version = 42)
                public abstract class MyDb extends RoomDatabase {
                }
                """, entity1, entity2) { _, _ ->
        }.compilesWithoutError()
    }

    @Test
    fun foreignKey_missingParentColumn() {
        val entity1 = JavaFileObjects.forSourceString("foo.bar.Entity1",
                """
                package foo.bar;
                import androidx.room.*;
                @Entity(foreignKeys = @ForeignKey(entity = Entity2.class,
                    parentColumns = {"anotherName", "anotherName2"},
                    childColumns = {"name", "name2"}))
                public class Entity1 {
                    @PrimaryKey
                    int uid;
                    String name;
                    String name2;
                }
                """)

        val entity2 = JavaFileObjects.forSourceString("foo.bar.Entity2",
                """
                package foo.bar;
                import androidx.room.*;
                @Entity
                public class Entity2 {
                    @PrimaryKey
                    int uid;
                    String anotherName;
                }
                """)
        singleDb("""
                @Database(entities = {Entity1.class, Entity2.class}, version = 42)
                public abstract class MyDb extends RoomDatabase {
                }
                """, entity1, entity2) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.foreignKeyParentColumnDoesNotExist("foo.bar.Entity2",
                        "anotherName2", listOf("uid", "anotherName"))
        )
    }

    @Test
    fun foreignKey_goodWithIndex() {
        val entity1 = JavaFileObjects.forSourceString("foo.bar.Entity1",
                """
                package foo.bar;
                import androidx.room.*;
                @Entity(foreignKeys = @ForeignKey(entity = Entity2.class,
                    parentColumns = {"anotherName", "anotherName2"},
                    childColumns = {"name", "name2"}))
                public class Entity1 {
                    @PrimaryKey
                    int uid;
                    String name;
                    String name2;
                }
                """)

        val entity2 = JavaFileObjects.forSourceString("foo.bar.Entity2",
                """
                package foo.bar;
                import androidx.room.*;
                @Entity(indices = @Index(value = {"anotherName2", "anotherName"}, unique = true))
                public class Entity2 {
                    @PrimaryKey
                    int uid;
                    String anotherName;
                    String anotherName2;
                }
                """)
        singleDb("""
                @Database(entities = {Entity1.class, Entity2.class}, version = 42)
                public abstract class MyDb extends RoomDatabase {
                }
                """, entity1, entity2) { _, _ ->
        }.compilesWithoutError()
    }

    @Test
    fun insertNotAReferencedEntity() {
        singleDb("""
                @Database(entities = {User.class}, version = 42)
                public abstract class MyDb extends RoomDatabase {
                    abstract BookDao bookDao();
                }
                """, USER, USER_DAO, BOOK, BOOK_DAO) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.shortcutEntityIsNotInDatabase(
                        database = "foo.bar.MyDb",
                        dao = "foo.bar.BookDao",
                        entity = "foo.bar.Book"
                )
        )
    }

    @Test
    fun cache_entity() {
        singleDb("""
                @Database(entities = {User.class}, version = 42)
                @SkipQueryVerification
                public abstract class MyDb extends RoomDatabase {
                    public abstract MyUserDao userDao();
                    @Dao
                    interface MyUserDao {
                        @Insert
                        public void insert(User... users);

                        @Query("SELECT * FROM user where uid = :uid")
                        public User loadOne(int uid);

                        @TypeConverters(Converter.class)
                        @Query("SELECT * FROM user where uid = :uid")
                        public User loadWithConverter(int uid);
                    }
                    public static class Converter {
                        @TypeConverter
                        public static java.util.Date foo(Long input) {return null;}
                    }
                }
                """, USER, USER_DAO) { db, _ ->
            val userDao = db.daoMethods.first().dao
            val insertionMethod = userDao.insertionMethods.find { it.name == "insert" }
            assertThat(insertionMethod, notNullValue())
            val loadOne = userDao.queryMethods.find { it.name == "loadOne" }
            assertThat(loadOne, notNullValue())
            val adapter = loadOne?.queryResultBinder?.adapter?.rowAdapter
            assertThat("test sanity", adapter, instanceOf(EntityRowAdapter::class.java))
            val adapterEntity = (adapter as EntityRowAdapter).entity
            assertThat(insertionMethod?.entities?.values?.first(), sameInstance(adapterEntity))

            val withConverter = userDao.queryMethods.find { it.name == "loadWithConverter" }
            assertThat(withConverter, notNullValue())
            val convAdapter = withConverter?.queryResultBinder?.adapter?.rowAdapter
            assertThat("test sanity", adapter, instanceOf(EntityRowAdapter::class.java))
            val convAdapterEntity = (convAdapter as EntityRowAdapter).entity
            assertThat(insertionMethod?.entities?.values?.first(),
                    not(sameInstance(convAdapterEntity)))

            assertThat(convAdapterEntity, notNullValue())
            assertThat(adapterEntity, notNullValue())
        }.compilesWithoutError()
    }

    @Test
    fun cache_pojo() {
        singleDb("""
                @Database(entities = {User.class}, version = 42)
                public abstract class MyDb extends RoomDatabase {
                    public abstract UserDao userDao();
                }
                """, USER, USER_DAO) { db, _ ->
            val userDao = db.daoMethods.first().dao
            val loadOne = userDao.queryMethods.find { it.name == "loadOnePojo" }
            assertThat(loadOne, notNullValue())
            val adapter = loadOne?.queryResultBinder?.adapter?.rowAdapter
            assertThat("test sanity", adapter, instanceOf(PojoRowAdapter::class.java))
            val adapterPojo = (adapter as PojoRowAdapter).pojo

            val loadAll = userDao.queryMethods.find { it.name == "loadAllPojos" }
            assertThat(loadAll, notNullValue())
            val loadAllAdapter = loadAll?.queryResultBinder?.adapter?.rowAdapter
            assertThat("test sanity", loadAllAdapter, instanceOf(PojoRowAdapter::class.java))
            val loadAllPojo = (loadAllAdapter as PojoRowAdapter).pojo
            assertThat(adapter, not(sameInstance(loadAllAdapter)))
            assertThat(adapterPojo, sameInstance(loadAllPojo))

            val withConverter = userDao.queryMethods.find { it.name == "loadPojoWithConverter" }
            assertThat(withConverter, notNullValue())
            val convAdapter = withConverter?.queryResultBinder?.adapter?.rowAdapter
            assertThat("test sanity", adapter, instanceOf(PojoRowAdapter::class.java))
            val convAdapterPojo = (convAdapter as PojoRowAdapter).pojo
            assertThat(convAdapterPojo, notNullValue())
            assertThat(convAdapterPojo, not(sameInstance(adapterPojo)))
        }.compilesWithoutError()
    }

    @Test
    fun daoConstructor_RoomDatabase() {
        assertConstructor(listOf(DB1), "BookDao(RoomDatabase db) {}")
                .compilesWithoutError()
    }

    @Test
    fun daoConstructor_specificDatabase() {
        assertConstructor(listOf(DB1), "BookDao(Db1 db) {}")
                .compilesWithoutError()
    }

    @Test
    fun daoConstructor_wrongDatabase() {
        assertConstructor(listOf(DB1, DB3), "BookDao(Db3 db) {}")
                .failsToCompile()
                .withErrorContaining(ProcessorErrors
                        .daoMustHaveMatchingConstructor("foo.bar.BookDao", "foo.bar.Db1"))
    }

    @Test
    fun daoConstructor_multipleDatabases_RoomDatabase() {
        assertConstructor(listOf(DB1, DB2), "BookDao(RoomDatabase db) {}")
                .compilesWithoutError()
    }

    @Test
    fun daoConstructor_multipleDatabases_specificDatabases() {
        assertConstructor(listOf(DB1, DB2), """
                    BookDao(Db1 db) {}
                    BookDao(Db2 db) {}
                """)
                .compilesWithoutError()
    }

    @Test
    fun daoConstructor_multipleDatabases_empty() {
        assertConstructor(listOf(DB1, DB2), """
                    BookDao(Db1 db) {}
                    BookDao() {} // Db2 uses this
                """)
                .compilesWithoutError()
    }

    @Test
    fun daoConstructor_multipleDatabases_noMatch() {
        assertConstructor(listOf(DB1, DB2), """
                    BookDao(Db1 db) {}
                """)
                .failsToCompile()
                .withErrorContaining(ProcessorErrors
                        .daoMustHaveMatchingConstructor("foo.bar.BookDao", "foo.bar.Db2"))
    }

    fun assertConstructor(dbs: List<JavaFileObject>, constructor: String): CompileTester {
        val bookDao = JavaFileObjects.forSourceString("foo.bar.BookDao",
                """
                package foo.bar;
                import androidx.room.*;
                @Dao
                public abstract class BookDao {
                    $constructor
                }
                """)
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(BOOK, bookDao) + dbs)
                .processedWith(RoomProcessor())
    }

    fun singleDb(input: String, vararg otherFiles: JavaFileObject,
                 handler: (Database, TestInvocation) -> Unit): CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(otherFiles.toMutableList()
                        + JavaFileObjects.forSourceString("foo.bar.MyDb",
                        DATABASE_PREFIX + input
                ))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(androidx.room.Database::class)
                        .nextRunHandler { invocation ->
                            val entity = invocation.roundEnv
                                    .getElementsAnnotatedWith(
                                            androidx.room.Database::class.java)
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
