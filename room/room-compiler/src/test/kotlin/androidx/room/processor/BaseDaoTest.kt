package androidx.room.processor

import COMMON
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.testing.context
import androidx.room.vo.Dao
import androidx.room.writer.DaoWriter
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * we don't assert much in these tests since if type resolution fails, compilation fails.
 */
@RunWith(JUnit4::class)
class BaseDaoTest {
    @Test
    fun insert() {
        baseDao(
            """
            @Insert
            void insertMe(T t);
        """
        ) { dao ->
            assertThat(dao.insertionMethods.size, `is`(1))
        }
    }

    @Test
    fun insertArray() {
        baseDao(
            """
            @Insert
            void insertMe(T[] t);
        """
        ) { dao ->
            assertThat(dao.insertionMethods.size, `is`(1))
        }
    }

    @Test
    fun insertVarArg() {
        baseDao(
            """
            @Insert
            void insertMe(T... t);
        """
        ) { dao ->
            assertThat(dao.insertionMethods.size, `is`(1))
        }
    }

    @Test
    fun insertList() {
        baseDao(
            """
            @Insert
            void insertMe(List<T> t);
        """
        ) { dao ->
            assertThat(dao.insertionMethods.size, `is`(1))
        }
    }

    @Test
    fun delete() {
        baseDao(
            """
            @Delete
            void deleteMe(T t);
        """
        ) { dao ->
            assertThat(dao.deletionMethods.size, `is`(1))
        }
    }

    @Test
    fun deleteArray() {
        baseDao(
            """
            @Delete
            void deleteMe(T[] t);
        """
        ) { dao ->
            assertThat(dao.deletionMethods.size, `is`(1))
        }
    }

    @Test
    fun deleteVarArg() {
        baseDao(
            """
            @Delete
            void deleteMe(T... t);
        """
        ) { dao ->
            assertThat(dao.deletionMethods.size, `is`(1))
        }
    }

    @Test
    fun deleteList() {
        baseDao(
            """
            @Delete
            void deleteMe(List<T> t);
        """
        ) { dao ->
            assertThat(dao.deletionMethods.size, `is`(1))
        }
    }

    @Test
    fun update() {
        baseDao(
            """
            @Update
            void updateMe(T t);
        """
        ) { dao ->
            assertThat(dao.updateMethods.size, `is`(1))
        }
    }

    @Test
    fun updateArray() {
        baseDao(
            """
            @Update
            void updateMe(T[] t);
        """
        ) { dao ->
            assertThat(dao.updateMethods.size, `is`(1))
        }
    }

    @Test
    fun updateVarArg() {
        baseDao(
            """
            @Update
            void updateMe(T... t);
        """
        ) { dao ->
            assertThat(dao.updateMethods.size, `is`(1))
        }
    }

    @Test
    fun updateList() {
        baseDao(
            """
            @Update
            void updateMe(List<T> t);
        """
        ) { dao ->
            assertThat(dao.updateMethods.size, `is`(1))
        }
    }

    @Test
    fun extendSameInterfaceTwice() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.*

            interface Parent<T> {
                @Delete
                fun delete(t: T)
            }

            interface Child1<T> : Parent<T> {
                @Insert
                fun insert(t: T)
            }

            interface Child2<T> : Parent<T> {
                @Update
                fun update(t: T)
            }

            @Entity
            data class Data(
                @PrimaryKey(autoGenerate = false)
                val id: Long,
                val data: String
            )

            @Dao
            abstract class Dao1 : Child1<Data>, Child2<Data>, Parent<Data>

            @Dao
            abstract class Dao2 : Child1<Data>, Parent<Data>

            @Dao
            abstract class Dao3 : Child1<Data>, Parent<Data> {
                @Delete
                abstract override fun delete(t: Data)
            }

            abstract class MyDb : RoomDatabase() {
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(source)
        ) { invocation ->
            val dbElm = invocation.context.processingEnv
                .requireTypeElement("MyDb")
            val dbType = dbElm.type
            // if we could create valid code, it is good, no need for assertions.
            listOf("Dao1", "Dao2", "Dao3").forEach { name ->
                val dao = invocation.processingEnv.requireTypeElement(name)
                val processed = DaoProcessor(
                    invocation.context, dao, dbType, null
                ).process()
                DaoWriter(processed, dbElm, invocation.processingEnv)
                    .write(invocation.processingEnv)
            }
        }
    }

    fun baseDao(code: String, handler: (Dao) -> Unit) {
        val baseClass = Source.java(
            "foo.bar.BaseDao",
            """
                package foo.bar;
                import androidx.room.*;
                import java.util.List;

                interface BaseDao<K, T> {
                    $code
                }
            """
        )
        val extension = Source.java(
            "foo.bar.MyDao",
            """
                package foo.bar;
                import androidx.room.*;
                @Dao
                interface MyDao extends BaseDao<Integer, User> {
                }
            """
        )
        val fakeDb = Source.java(
            "foo.bar.MyDb",
            """
                package foo.bar;
                import androidx.room.*;
                // we need a RoomDatabase subclass in sources to match incremental compilation
                // check requirements
                abstract class MyDb extends RoomDatabase {
                }
            """
        )
        runProcessorTest(
            sources = listOf(baseClass, extension, COMMON.USER, fakeDb)
        ) { invocation ->
            val daoElm = invocation.processingEnv.requireTypeElement("foo.bar.MyDao")
            val dbElm = invocation.context.processingEnv
                .requireTypeElement("foo.bar.MyDb")
            val dbType = dbElm.type
            val processedDao = DaoProcessor(
                invocation.context, daoElm, dbType, null
            ).process()
            handler(processedDao)
            DaoWriter(processedDao, dbElm, invocation.processingEnv).write(invocation.processingEnv)
        }
    }
}
