package androidx.room.processor

import COMMON
import androidx.room.ext.RoomTypeNames
import androidx.room.vo.Dao
import androidx.room.writer.DaoWriter
import com.google.auto.common.MoreTypes
import com.google.testing.compile.JavaFileObjects
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import simpleRun

/**
 * we don't assert much in these tests since if type resolution fails, compilation fails.
 */
@RunWith(JUnit4::class)
class BaseDaoTest {
    private fun String.toJFO(qName: String) = JavaFileObjects.forSourceLines(qName, this)

    @Test
    fun insert() {
        baseDao("""
            @Insert
            void insertMe(T t);
        """) { dao ->
            assertThat(dao.insertionMethods.size, `is`(1))
        }
    }

    @Test
    fun insertArray() {
        baseDao("""
            @Insert
            void insertMe(T[] t);
        """) { dao ->
            assertThat(dao.insertionMethods.size, `is`(1))
        }
    }

    @Test
    fun insertVarArg() {
        baseDao("""
            @Insert
            void insertMe(T... t);
        """) { dao ->
            assertThat(dao.insertionMethods.size, `is`(1))
        }
    }

    @Test
    fun insertList() {
        baseDao("""
            @Insert
            void insertMe(List<T> t);
        """) { dao ->
            assertThat(dao.insertionMethods.size, `is`(1))
        }
    }

    @Test
    fun delete() {
        baseDao("""
            @Delete
            void deleteMe(T t);
        """) { dao ->
            assertThat(dao.deletionMethods.size, `is`(1))
        }
    }

    @Test
    fun deleteArray() {
        baseDao("""
            @Delete
            void deleteMe(T[] t);
        """) { dao ->
            assertThat(dao.deletionMethods.size, `is`(1))
        }
    }

    @Test
    fun deleteVarArg() {
        baseDao("""
            @Delete
            void deleteMe(T... t);
        """) { dao ->
            assertThat(dao.deletionMethods.size, `is`(1))
        }
    }

    @Test
    fun deleteList() {
        baseDao("""
            @Delete
            void deleteMe(List<T> t);
        """) { dao ->
            assertThat(dao.deletionMethods.size, `is`(1))
        }
    }

    @Test
    fun update() {
        baseDao("""
            @Update
            void updateMe(T t);
        """) { dao ->
            assertThat(dao.updateMethods.size, `is`(1))
        }
    }

    @Test
    fun updateArray() {
        baseDao("""
            @Update
            void updateMe(T[] t);
        """) { dao ->
            assertThat(dao.updateMethods.size, `is`(1))
        }
    }

    @Test
    fun updateVarArg() {
        baseDao("""
            @Update
            void updateMe(T... t);
        """) { dao ->
            assertThat(dao.updateMethods.size, `is`(1))
        }
    }

    @Test
    fun updateList() {
        baseDao("""
            @Update
            void updateMe(List<T> t);
        """) { dao ->
            assertThat(dao.updateMethods.size, `is`(1))
        }
    }

    fun baseDao(code: String, handler: (Dao) -> Unit) {
        val baseClass = """
            package foo.bar;
            import androidx.room.*;
            import java.util.List;

            interface BaseDao<K, T> {
                $code
            }
        """.toJFO("foo.bar.BaseDao")
        val extension = """
            package foo.bar;
            import androidx.room.*;
            @Dao
            interface MyDao extends BaseDao<Integer, User> {
            }
        """.toJFO("foo.bar.MyDao")
        simpleRun(baseClass, extension, COMMON.USER) { invocation ->
            val daoElm = invocation.processingEnv.elementUtils.getTypeElement("foo.bar.MyDao")
            val dbType = MoreTypes.asDeclared(invocation.context.processingEnv.elementUtils
                    .getTypeElement(RoomTypeNames.ROOM_DB.toString()).asType())
            val processedDao = DaoProcessor(invocation.context, daoElm, dbType, null).process()
            handler(processedDao)
            DaoWriter(processedDao, invocation.processingEnv).write(invocation.processingEnv)
        }.compilesWithoutError()
    }
}