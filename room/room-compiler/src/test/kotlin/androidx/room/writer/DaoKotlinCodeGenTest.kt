/*
 * Copyright 2022 The Android Open Source Project
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

import COMMON
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compileFiles
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class DaoKotlinCodeGenTest : BaseDaoKotlinCodeGenTest() {

    @get:Rule val testName = TestName()

    val databaseSrc =
        Source.kotlin(
            "MyDatabase.kt",
            """
        import androidx.room.*

        @Database(entities = [MyEntity::class], version = 1, exportSchema = false)
        abstract class MyDatabase : RoomDatabase() {
          abstract fun getDao(): MyDao
        }
        """
                .trimIndent()
        )

    @Test
    fun pojoRowAdapter_variableProperty() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
              
              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            class MyEntity(
                @PrimaryKey
                var pk: Int
            ) {
                var variablePrimitive: Long = 0
                var variableString: String = ""
                var variableNullableString: String? = null
            }
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_variableProperty_java() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity

              @Insert
              fun addEntity(item: MyEntity)
            }
            """
                    .trimIndent()
            )
        val javaEntity =
            Source.java(
                "MyEntity",
                """
            import androidx.annotation.Nullable;
            import androidx.room.*;
            
            @Entity
            public class MyEntity {
              @PrimaryKey
              private long mValue;

              @Nullable
              private String mNullableValue;

              public long getValue() { return mValue; }
              public void setValue(long value) { mValue = value; }

              @Nullable
              public String getNullableValue() { return mNullableValue; }
              public void setNullableValue(@Nullable String nullableValue) {
                mNullableValue = nullableValue;
              }
            }
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, javaEntity, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    // b/274760383
    @Test
    fun pojoRowAdapter_otherModule() {
        val lib =
            compileFiles(
                sources =
                    listOf(
                        Source.kotlin(
                            "MyEntity.kt",
                            """
                import androidx.room.*

                @Entity
                class MyEntity(
                    @PrimaryKey
                    val pk: Int,
                    val primitive: Long = 0,
                    val string: String = "",
                    val nullableString: String? = null,
                    @JvmField val fieldString: String = "",
                    @JvmField val nullableFieldString: String? = null
                ) {
                    var variablePrimitive: Long = 0
                    var variableString: String = ""
                    var variableNullableString: String? = null
                    @JvmField var variableFieldString: String = ""
                    @JvmField var variableNullableFieldString: String? = null
                }
                """
                                .trimIndent()
                        )
                    )
            )
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity

              @Insert
              fun addEntity(item: MyEntity)
            }
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName),
            compiledFiles = lib
        )
    }

    @Test
    fun pojoRowAdapter_internalVisibility() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
              
              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            class MyEntity(
                @PrimaryKey
                val pk: Int,
                internal val internalVal: Long
            ) {
                internal var internalVar: Long = 0
                var internalSetterVar: Long = 0
                    internal set
            }
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_primitives() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
              
              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val int: Int,
                val short: Short,
                val byte: Byte,
                val long: Long,
                val char: Char,
                val float: Float,
                val double: Double,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_primitives_nullable() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
              
              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val int: Int?,
                val short: Short?,
                val byte: Byte?,
                val long: Long?,
                val char: Char?,
                val float: Float?,
                val double: Double?,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_boolean() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
              
              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val boolean: Boolean,
                val nullableBoolean: Boolean?,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_string() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
              
              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val string: String,
                val nullableString: String?,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_byteArray() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
              
              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val byteArray: ByteArray,
                val nullableByteArray: ByteArray?,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_enum() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
              
              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val enum: Fruit,
                val nullableEnum: Fruit?,
            )

            enum class Fruit {
                APPLE,
                BANANA
            }
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_uuid() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import java.util.UUID

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
              
              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val uuid: UUID,
                val nullableUuid: UUID?,
            )

            enum class Fruit {
                APPLE,
                BANANA
            }
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_embedded() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
              
              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                @Embedded
                val foo: Foo,
                @Embedded(prefix = "nullable")
                val nullableFoo: Foo?,
            )

            data class Foo(
                val numberData: Long,
                val stringData: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_customTypeConverter() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
              
              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            @TypeConverters(FooConverter::class)
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val foo: Foo,
            )

            data class Foo(val data: String)

            class FooConverter {
                @TypeConverter
                fun fromString(data: String): Foo = Foo(data)
                @TypeConverter
                fun toString(foo: Foo): String = foo.data
            }
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_customTypeConverter_provided() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity

              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            @TypeConverters(FooConverter::class)
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val foo: Foo,
            )

            data class Foo(val data: String)

            @ProvidedTypeConverter
            class FooConverter(val default: String) {
                @TypeConverter
                fun fromString(data: String?): Foo = Foo(data ?: default)
                @TypeConverter
                fun toString(foo: Foo): String = foo.data
            }
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_customTypeConverter_composite() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity

              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            @TypeConverters(FooBarConverter::class)
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val bar: Bar,
            )

            data class Foo(val data: String)
            data class Bar(val data: String)

            object FooBarConverter {
                @TypeConverter
                fun fromString(data: String): Foo = Foo(data)
                @TypeConverter
                fun toString(foo: Foo): String = foo.data

                @TypeConverter
                fun fromFoo(foo: Foo): Bar = Bar(foo.data)
                @TypeConverter
                fun toFoo(bar: Bar): Foo = Foo(bar.data)
            }
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_customTypeConverter_nullAware() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity

              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            @TypeConverters(FooBarConverter::class)
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val foo: Foo,
                val bar: Bar
            )

            data class Foo(val data: String)
            data class Bar(val data: String)

            object FooBarConverter {
                @TypeConverter
                fun fromString(data: String?): Foo? = data?.let { Foo(it) }
                @TypeConverter
                fun toString(foo: Foo?): String? = foo?.data

                @TypeConverter
                fun fromFoo(foo: Foo): Bar = Bar(foo.data)
                @TypeConverter
                fun toFoo(bar: Bar): Foo = Foo(bar.data)
            }
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_customTypeConverter_internalVisibility() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            abstract class MyDao {
              @Query("SELECT * FROM MyEntity")
              internal abstract fun getEntity(): MyEntity
              
              @Insert
              internal abstract fun addEntity(item: MyEntity)
            }

            @Entity
            @TypeConverters(FooConverter::class)
            internal data class MyEntity internal constructor(
                @PrimaryKey
                internal val pk: Int,
                internal val foo: Foo,
            )

            internal data class Foo(internal val data: String)

            internal class FooConverter internal constructor() {
                @TypeConverter
                internal fun fromString(data: String): Foo = Foo(data)
                @TypeConverter
                internal fun toString(foo: Foo): String = foo.data
            }
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun coroutineResultBinder() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import java.util.UUID

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              suspend fun getEntity(): MyEntity
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc, COMMON.COROUTINES_ROOM),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun multiTypedPagingSourceResultBinder() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import androidx.paging.*
            import androidx.paging.rxjava2.*
            import androidx.paging.rxjava3.*

            @Dao
            abstract class MyDao {
              @Query("SELECT pk FROM MyEntity")
              abstract fun getAllIds(): androidx.paging.PagingSource<Int, MyEntity>

              @Query("SELECT * FROM MyEntity WHERE pk > :gt ORDER BY pk ASC")
              abstract fun getAllIdsWithArgs(gt: Long): androidx.paging.PagingSource<Int, MyEntity>

              @Query("SELECT pk FROM MyEntity")
              abstract fun getAllIdsRx2(): androidx.paging.rxjava2.RxPagingSource<Int, MyEntity>

              @Query("SELECT pk FROM MyEntity")
              abstract fun getAllIdsRx3(): androidx.paging.rxjava3.RxPagingSource<Int, MyEntity>

              @Query("SELECT pk FROM MyEntity")
              abstract fun getAllIdsGuava(): androidx.paging.ListenableFuturePagingSource<Int, MyEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources =
                listOf(
                    src,
                    databaseSrc,
                    COMMON.LIMIT_OFFSET_PAGING_SOURCE,
                    COMMON.LIMIT_OFFSET_RX2_PAGING_SOURCE,
                    COMMON.LIMIT_OFFSET_RX3_PAGING_SOURCE,
                    COMMON.RX2_PAGING_SOURCE,
                    COMMON.RX3_PAGING_SOURCE,
                    COMMON.LIMIT_OFFSET_LISTENABLE_FUTURE_PAGING_SOURCE,
                    COMMON.LISTENABLE_FUTURE_PAGING_SOURCE
                ),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun basicParameterAdapter_string() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity WHERE string = :arg")
              fun stringParam(arg: String): MyEntity

              @Query("SELECT * FROM MyEntity WHERE string = :arg")
              fun nullableStringParam(arg: String?): MyEntity
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val string: String,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun collectionParameterAdapter_string() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity WHERE string IN (:arg)")
              fun listOfString(arg: List<String>): MyEntity

              @Query("SELECT * FROM MyEntity WHERE string IN (:arg)")
              fun nullableListOfString(arg: List<String>?): MyEntity

              @Query("SELECT * FROM MyEntity WHERE string IN (:arg)")
              fun listOfNullableString(arg: List<String?>): MyEntity

              @Query("SELECT * FROM MyEntity WHERE string IN (:arg)")
              fun setOfString(arg: Set<String>): MyEntity
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val string: String,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun arrayParameterAdapter() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity WHERE id IN (:arg)")
              fun arrayOfString(arg: Array<String>): MyEntity

              @Query("SELECT * FROM MyEntity WHERE id IN (:arg)")
              fun nullableArrayOfString(arg: Array<String>?): MyEntity

              @Query("SELECT * FROM MyEntity WHERE id IN (:arg)")
              fun arrayOfNullableString(arg: Array<String?>): MyEntity

              @Query("SELECT * FROM MyEntity WHERE id IN (:arg)")
              fun varargOfString(vararg arg: String): MyEntity

              @Query("SELECT * FROM MyEntity WHERE id IN (:arg)")
              fun varargOfNullableString(vararg arg: String?): MyEntity

              @Query("SELECT * FROM MyEntity WHERE id IN (:arg)")
              fun primitiveIntArray(arg: IntArray): MyEntity

              @Query("SELECT * FROM MyEntity WHERE id IN (:arg)")
              fun nullablePrimitiveIntArray(arg: IntArray?): MyEntity
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val id: String,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun preparedQueryAdapter() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("INSERT INTO MyEntity (id) VALUES (:id)")
              fun insertEntity(id: Long)

              @Query("INSERT INTO MyEntity (id) VALUES (:id)")
              fun insertEntityReturnLong(id: Long): Long

              @Query("INSERT INTO MyEntity (id) VALUES (:id)")
              fun insertEntityReturnVoid(id: Long): Void?

              @Query("UPDATE MyEntity SET text = :text")
              fun updateEntity(text: String)

              @Query("UPDATE MyEntity SET text = :text WHERE id = :id")
              fun updateEntityReturnInt(id: Long, text: String): Int

              @Query("DELETE FROM MyEntity")
              fun deleteEntity()

              @Query("DELETE FROM MyEntity")
              fun deleteEntityReturnInt(): Int

              @Query("DELETE FROM MyEntity WHERE id IN (:ids)")
              fun deleteEntitiesIn(ids: List<Long>)
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val id: Long,
                val text: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun rawQuery() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import androidx.sqlite.db.SupportSQLiteQuery
            import kotlinx.coroutines.flow.Flow

            @Dao
            interface MyDao {
                @RawQuery
                fun getEntitySupport(sql: SupportSQLiteQuery): MyEntity

                @RawQuery
                fun getNullableEntitySupport(sql: SupportSQLiteQuery): MyEntity?

                @RawQuery(observedEntities = [MyEntity::class])
                fun getEntitySupportFlow(sql: SupportSQLiteQuery): Flow<MyEntity>

                @RawQuery
                fun getEntity(query: RoomRawQuery): MyEntity

                @RawQuery
                fun getNullableEntity(query: RoomRawQuery): MyEntity?

                @RawQuery(observedEntities = [MyEntity::class])
                fun getEntityFlow(query: RoomRawQuery): Flow<MyEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Long,
                val doubleColumn: Double,
                val floatColumn: Float,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun delegatingFunctions_defaultImplBridge(
        @TestParameter("disable", "all-compatibility", "all") jvmDefaultMode: String
    ) {
        // For parametrized tests, use method name from reflection
        val testName = object {}.javaClass.enclosingMethod!!.name
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import androidx.sqlite.db.SupportSQLiteQuery

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
  
              fun implemented() {
                TODO("")
              }
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Long,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName),
            jvmDefaultMode = jvmDefaultMode
        )
    }

    @Test
    fun delegatingFunctions_boxedPrimitiveBridge() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import androidx.sqlite.db.SupportSQLiteQuery

            interface BaseDao<T> {
                fun getEntity(id: T): MyEntity

                fun insertEntity(id: T): T
            }

            @Dao
            interface MyDao : BaseDao<Long> {
              @Query("SELECT * FROM MyEntity WHERE pk = :id")
              override fun getEntity(id: Long): MyEntity

              @Query("INSERT INTO MyEntity (pk) VALUES (:id)")
              override fun insertEntity(id: Long): Long
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Long,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName),
        )
    }

    @Test
    fun transactionMethodAdapter_interface(
        @TestParameter("disable", "all-compatibility", "all") jvmDefaultMode: String
    ) {
        // For parametrized tests, use method name from reflection
        val testName = object {}.javaClass.enclosingMethod!!.name
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import androidx.sqlite.db.SupportSQLiteQuery

            interface BaseDao {
                @Transaction
                open fun baseConcrete() {
                }

                @Transaction
                open suspend fun baseSuspendConcrete() {
                }
            }

            @Dao
            interface MyDao : BaseDao {
              @Transaction
              fun concrete() {
              }

              @Transaction
              fun concreteWithReturn(): String {
                TODO("")
              }

              @Transaction
              fun concreteWithParamsAndReturn(text: String, num: Long): String {
                TODO("")
              }

              @Transaction
              fun concreteWithFunctionalParam(block: () -> Unit) {
              }

              @Transaction
              suspend fun suspendConcrete() {

              }

              @Transaction
              suspend fun suspendConcreteWithReturn(): String {
                TODO("")
              }

              @Transaction
              suspend fun suspendConcreteWithSuspendFunctionalParam(block: suspend () -> Unit) {
              }
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Long,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc, COMMON.COROUTINES_ROOM, COMMON.ROOM_DATABASE_KTX),
            expectedFilePath = getTestGoldenPath(testName),
            jvmDefaultMode = jvmDefaultMode
        )
    }

    @Test
    fun transactionMethodAdapter_abstractClass() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import androidx.sqlite.db.SupportSQLiteQuery

            interface BaseDao {
                @Transaction
                open fun baseConcrete() {
                }

                @Transaction
                open suspend fun baseSuspendConcrete() {
                }
            }

            @Dao
            abstract class MyDao : BaseDao {
              @Transaction
              open fun concrete() {
              }

              @Transaction
              internal open fun concreteInternal() {
              }

              @Transaction
              open suspend fun suspendConcrete() {
              }

              @Transaction
              open fun concreteWithVararg(vararg arr: Long) {
              }

              @Transaction
              open suspend fun suspendConcreteWithVararg(vararg arr: Long) {
              }
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Long,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc, COMMON.COROUTINES_ROOM, COMMON.ROOM_DATABASE_KTX),
            expectedFilePath = getTestGoldenPath(testName.methodName),
        )
    }

    @Test
    fun deleteOrUpdateMethodAdapter() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Update
              fun updateEntity(item: MyEntity)

              @Delete
              fun deleteEntity(item: MyEntity)

              @Update
              fun updateEntityAndReturnCount(item: MyEntity): Int

              @Delete
              fun deleteEntityAndReturnCount(item: MyEntity): Int
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Long,
                val data: String,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun insertOrUpsertMethodAdapter() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Insert
              fun insertEntity(item: MyEntity)

              @Upsert
              fun upsertEntity(item: MyEntity)

              @Insert
              fun insertEntityAndReturnRowId(item: MyEntity): Long

              @Upsert
              fun upsertEntityAndReturnRowId(item: MyEntity): Long

              @Insert
              fun insertEntityListAndReturnRowIds(items: List<MyEntity>): List<Long>

              @Upsert
              fun upsertEntityListAndReturnRowIds(items: List<MyEntity>): List<Long>

              @Upsert
              fun upsertEntityListAndReturnRowIdsArray(items: List<MyEntity>): Array<Long>

              @Upsert
              fun upsertEntityListAndReturnRowIdsOutArray(items: List<MyEntity>): Array<out Long>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Long,
                val data: String,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun queryResultAdapter_singleColumn() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT count(*) FROM MyEntity")
              fun count(): Int

              @Query("SELECT 'Tom' FROM MyEntity LIMIT 1")
              fun text(): String

              @Query("SELECT 'Tom' FROM MyEntity LIMIT 1")
              fun nullableText(): String?
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun queryResultAdapter_list() {
        val dbSource =
            Source.kotlin(
                "MyDatabase.kt",
                """
            import androidx.room.*

            @Database(entities = [MyEntity::class, MyNullableEntity::class], version = 1, exportSchema = false)
            abstract class MyDatabase : RoomDatabase() {
                abstract fun getDao(): MyDao
            }
            """
                    .trimIndent()
            )

        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun queryOfList(): List<MyEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            @Entity
            data class MyNullableEntity(
                @PrimaryKey
                val pk: Int?,
                val other: String?
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, dbSource),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun queryResultAdapter_array() {
        val dbSource =
            Source.kotlin(
                "MyDatabase.kt",
                """
            import androidx.room.*

            @Database(entities = [MyEntity::class], version = 1, exportSchema = false)
            abstract class MyDatabase : RoomDatabase() {
                abstract fun getDao(): MyDao
            }
            """
                    .trimIndent()
            )
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun queryOfArray(): Array<MyEntity>

              @Suppress(RoomWarnings.UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE)
              @Query("SELECT * FROM MyEntity")
              fun queryOfNullableArray(): Array<MyEntity?>

              @Query("SELECT pk FROM MyEntity")
              fun queryOfArrayWithLong(): Array<Long>

              @Suppress(RoomWarnings.UNNECESSARY_NULLABILITY_IN_DAO_RETURN_TYPE)
              @Query("SELECT pk FROM MyEntity")
              fun queryOfArrayWithNullableLong(): Array<Long?>

              @Query("SELECT pk FROM MyEntity")
              fun queryOfLongArray(): LongArray

              @Query("SELECT pk FROM MyEntity")
              fun queryOfShortArray(): ShortArray
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String,
                val other2: Long
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, dbSource),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun abstractClassWithParam() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            abstract class MyDao(val db: RoomDatabase) {
              @Query("SELECT * FROM MyEntity")
              abstract fun getEntity(): MyEntity
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun queryResultAdapter_optional() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun queryOfOptional(): java.util.Optional<MyEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun queryResultAdapter_guavaOptional() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun queryOfOptional(): com.google.common.base.Optional<MyEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun queryResultAdapter_immutable_list() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import com.google.common.collect.ImmutableList

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun queryOfList(): ImmutableList<MyEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun queryResultAdapter_map() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Database(entities = [Artist::class, Song::class], version = 1, exportSchema = false)
            abstract class MyDatabase : RoomDatabase() {
              abstract fun getDao(): MyDao
            }

            @Dao
            interface MyDao {
                @Query("SELECT * FROM Song JOIN Artist ON Song.artistKey = Artist.artistId")
                fun getSongsWithArtist(): Map<Song, Artist>

                @Query("SELECT * FROM Artist JOIN Song ON Artist.artistId = Song.artistKey")
                fun getArtistWithSongs(): Map<Artist, List<Song>>

                @Suppress("DEPRECATION") // For @MapInfo
                @MapInfo(valueColumn = "songCount")
                @Query(
                    "SELECT Artist.*, COUNT(songId) as songCount " +
                    "FROM Artist JOIN Song ON Artist.artistId = Song.artistKey " +
                    "GROUP BY artistId"
                )
                fun getArtistSongCount(): Map<Artist, Int>

                @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
                @Suppress("DEPRECATION") // For @MapInfo
                @MapInfo(valueColumn = "songId")
                @Query("SELECT * FROM Artist JOIN Song ON Artist.artistId = Song.artistKey")
                fun getArtistWithSongIds(): Map<Artist, List<String>>
            }

            @Entity
            data class Artist(
                @PrimaryKey
                val artistId: String
            )

            @Entity
            data class Song(
                @PrimaryKey
                val songId: String,
                val artistKey: String
            )
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src), expectedFilePath = getTestGoldenPath(testName.methodName))
    }

    @Test
    fun queryResultAdapter_nestedMap() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Database(
                entities = [Artist::class, Song::class, Album::class, Playlist::class],
                version = 1,
                exportSchema = false
            )
            abstract class MyDatabase : RoomDatabase() {
              abstract fun getDao(): MyDao
            }

            @Dao
            interface MyDao {
                @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
                @Query(
                    "SELECT * FROM Artist JOIN (Album JOIN Song ON Album.albumName = Song.album) " +
                    "ON Artist.artistName = Album.albumArtist"
                )
                fun singleNested(): Map<Artist, Map<Album, List<Song>>>

                @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
                @Query(
                    "SELECT * FROM Playlist JOIN (Artist JOIN (Album JOIN Song " +
                    "ON Album.albumName = Song.album) " +
                    "ON Artist.artistName = Album.albumArtist)" +
                    "ON Playlist.playlistArtist = Artist.artistName"
                )
                fun doubleNested(): Map<Playlist, Map<Artist, Map<Album, List<Song>>>>
            }

            @Entity
            data class Artist(
                @PrimaryKey
                val artistId: String,
                val artistName: String,
            )

            @Entity
            data class Album(
                @PrimaryKey
                val albumId: String,
                val albumName: String,
                val albumArtist: String
            )

            @Entity
            data class Playlist(
                @PrimaryKey
                val playlistId: String,
                val playlistArtist: String,
            )

            @Entity
            data class Song(
                @PrimaryKey
                val songId: String,
                val album: String,
                val songArtist: String
            )
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src), expectedFilePath = getTestGoldenPath(testName.methodName))
    }

    @Test
    fun queryResultAdapter_guavaImmutableMultimap() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Database(entities = [Artist::class, Song::class], version = 1, exportSchema = false)
            abstract class MyDatabase : RoomDatabase() {
              abstract fun getDao(): MyDao
            }

            @Dao
            interface MyDao {
                @Query("SELECT * FROM Artist JOIN Song ON Artist.artistId = Song.artistKey")
                fun getArtistWithSongs(): com.google.common.collect.ImmutableSetMultimap<Artist, Song>

                @Query("SELECT * FROM Artist JOIN Song ON Artist.artistId = Song.artistKey")
                fun getArtistWithSongIds(): com.google.common.collect.ImmutableListMultimap<Artist, Song>
            }

            @Entity
            data class Artist(
                @PrimaryKey
                val artistId: String
            )

            @Entity
            data class Song(
                @PrimaryKey
                val songId: String,
                val artistKey: String
            )
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src), expectedFilePath = getTestGoldenPath(testName.methodName))
    }

    @Test
    fun queryResultAdapter_guavaImmutableMap() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Database(entities = [Artist::class, Song::class], version = 1, exportSchema = false)
            abstract class MyDatabase : RoomDatabase() {
              abstract fun getDao(): MyDao
            }

            @Dao
            interface MyDao {
                @Query("SELECT * FROM Song JOIN Artist ON Song.artistKey = Artist.artistId")
                fun getSongsWithArtist(): com.google.common.collect.ImmutableMap<Song, Artist>
            }

            @Entity
            data class Artist(
                @PrimaryKey
                val artistId: String
            )

            @Entity
            data class Song(
                @PrimaryKey
                val songId: String,
                val artistKey: String
            )
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src), expectedFilePath = getTestGoldenPath(testName.methodName))
    }

    @Test
    fun queryResultAdapter_map_ambiguousIndexAdapter() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Database(entities = [User::class, Comment::class], version = 1, exportSchema = false)
            abstract class MyDatabase : RoomDatabase() {
              abstract fun getDao(): MyDao
            }

            @Dao
            interface MyDao {
                @Query("SELECT * FROM User JOIN Comment ON User.id = Comment.userId")
                fun getUserCommentMap(): Map<User, List<Comment>>

                @Query(
                    "SELECT User.id, name, Comment.id, userId, text " +
                    "FROM User JOIN Comment ON User.id = Comment.userId"
                )
                fun getUserCommentMapWithoutStarProjection(): Map<User, List<Comment>>

                @SkipQueryVerification
                @Query("SELECT * FROM User JOIN Comment ON User.id = Comment.userId")
                fun getUserCommentMapWithoutQueryVerification(): Map<User, List<Comment>>
            }

            @Entity
            data class User(
                @PrimaryKey val id: Int,
                val name: String,
            )

            @Entity
            data class Comment(
                @PrimaryKey val id: Int,
                val userId: Int,
                val text: String,
            )
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src), expectedFilePath = getTestGoldenPath(testName.methodName))
    }

    @Test
    fun queryResultAdapter_nestedMap_ambiguousIndexAdapter() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import java.nio.ByteBuffer

            @Database(
                entities = [User::class, Comment::class, Avatar::class],
                version = 1,
                exportSchema = false
            )
            abstract class MyDatabase : RoomDatabase() {
              abstract fun getDao(): MyDao
            }

            @Dao
            interface MyDao {
                @Query(
                    "SELECT * FROM User JOIN Avatar ON User.id = Avatar.userId JOIN " +
                    "Comment ON Avatar.userId = Comment.userId"
                )
                fun getLeftJoinUserNestedMap(): Map<User, Map<Avatar, List<Comment>>>
            }

            @Entity
            data class User(
                @PrimaryKey val id: Int,
                val name: String,
            )

            @Entity
            data class Comment(
                @PrimaryKey val id: Int,
                val userId: Int,
                val text: String,
            )

            @Entity
            data class Avatar(
                @PrimaryKey val userId: Int,
                val url: String,
                val data: ByteBuffer,
            )
            """
                    .trimIndent()
            )
        runTest(sources = listOf(src), expectedFilePath = getTestGoldenPath(testName.methodName))
    }

    @Test
    fun entityRowAdapter() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {

              @SkipQueryVerification // To make Room use EntityRowAdapter
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity

              @SkipQueryVerification // To make Room use EntityRowAdapter
              @Insert
              fun addEntity(item: MyEntity)
            }

            @Entity
            class MyEntity(
                @PrimaryKey
                val valuePrimitive: Long,
                val valueBoolean: Boolean,
                val valueString: String,
                val valueNullableString: String?
            ) {
                var variablePrimitive: Long = 0
                var variableNullableBoolean: Boolean? = null
                var variableString: String = ""
                var variableNullableString: String? = null
            }
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun paging_dataSource() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import androidx.paging.DataSource

            @Dao
            abstract class MyDao {
                @Query("SELECT * from MyEntity")
                abstract fun getDataSourceFactory(): DataSource.Factory<Int, MyEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources =
                listOf(src, databaseSrc, COMMON.DATA_SOURCE_FACTORY, COMMON.POSITIONAL_DATA_SOURCE),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun callableQuery_rx2() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import io.reactivex.*

            @Dao
            interface MyDao {
                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getFlowable(vararg arg: String?): Flowable<MyEntity>

                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getObservable(vararg arg: String?): Observable<MyEntity>

                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getSingle(vararg arg: String?): Single<MyEntity>

                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getMaybe(vararg arg: String?): Maybe<MyEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources =
                listOf(
                    src,
                    databaseSrc,
                ),
            compiledFiles =
                compileFiles(
                    listOf(
                        COMMON.RX2_ROOM,
                        COMMON.RX2_FLOWABLE,
                        COMMON.RX2_OBSERVABLE,
                        COMMON.RX2_SINGLE,
                        COMMON.RX2_MAYBE,
                        COMMON.RX2_COMPLETABLE,
                        COMMON.PUBLISHER,
                        COMMON.RX2_EMPTY_RESULT_SET_EXCEPTION
                    )
                ),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun callableQuery_rx3() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import io.reactivex.rxjava3.core.*

            @Dao
            interface MyDao {
                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getFlowable(vararg arg: String?): Flowable<MyEntity>

                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getObservable(vararg arg: String?): Observable<MyEntity>

                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getSingle(vararg arg: String?): Single<MyEntity>

                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getMaybe(vararg arg: String?): Maybe<MyEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources =
                listOf(
                    src,
                    databaseSrc,
                ),
            compiledFiles =
                compileFiles(
                    listOf(
                        COMMON.RX3_ROOM,
                        COMMON.RX3_FLOWABLE,
                        COMMON.RX3_OBSERVABLE,
                        COMMON.RX3_SINGLE,
                        COMMON.RX3_MAYBE,
                        COMMON.RX3_COMPLETABLE,
                        COMMON.PUBLISHER,
                        COMMON.RX3_EMPTY_RESULT_SET_EXCEPTION
                    )
                ),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun preparedCallableQuery_rx2() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import io.reactivex.*

            @Dao
            interface MyDao {
                @Query("INSERT INTO MyEntity (pk, other) VALUES (:id, :name)")
                fun insertPublisherSingle(id: String, name: String): Single<Long>

                @Query("INSERT INTO MyEntity (pk, other) VALUES (:id, :name)")
                fun insertPublisherMaybe(id: String, name: String): Maybe<Long>

                @Query("INSERT INTO MyEntity (pk, other) VALUES (:id, :name)")
                fun insertPublisherCompletable(id: String, name: String): Completable
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources =
                listOf(
                    src,
                    databaseSrc,
                ),
            compiledFiles =
                compileFiles(
                    listOf(
                        COMMON.RX2_ROOM,
                        COMMON.RX2_FLOWABLE,
                        COMMON.RX2_OBSERVABLE,
                        COMMON.RX2_SINGLE,
                        COMMON.RX2_MAYBE,
                        COMMON.RX2_COMPLETABLE,
                        COMMON.PUBLISHER,
                        COMMON.RX2_EMPTY_RESULT_SET_EXCEPTION
                    )
                ),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun preparedCallableQuery_rx3() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import io.reactivex.rxjava3.core.*

            @Dao
            interface MyDao {
                @Query("INSERT INTO MyEntity (pk, other) VALUES (:id, :name)")
                fun insertPublisherSingle(id: String, name: String): Single<Long>

                @Query("INSERT INTO MyEntity (pk, other) VALUES (:id, :name)")
                fun insertPublisherMaybe(id: String, name: String): Maybe<Long>

                @Query("INSERT INTO MyEntity (pk, other) VALUES (:id, :name)")
                fun insertPublisherCompletable(id: String, name: String): Completable
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources =
                listOf(
                    src,
                    databaseSrc,
                ),
            compiledFiles =
                compileFiles(
                    listOf(
                        COMMON.RX3_ROOM,
                        COMMON.RX3_FLOWABLE,
                        COMMON.RX3_OBSERVABLE,
                        COMMON.RX3_SINGLE,
                        COMMON.RX3_MAYBE,
                        COMMON.RX3_COMPLETABLE,
                        COMMON.PUBLISHER,
                        COMMON.RX3_EMPTY_RESULT_SET_EXCEPTION
                    )
                ),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun coroutines() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import kotlinx.coroutines.flow.Flow

            @Dao
            interface MyDao {
                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getFlow(vararg arg: String?): Flow<MyEntity>

                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getFlowNullable(vararg arg: String?): Flow<MyEntity?>

                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                suspend fun getSuspendList(vararg arg: String?): List<MyEntity>

                @Query("SELECT count(*) FROM MyEntity")
                suspend fun getCount(): Int

                @Query("INSERT INTO MyEntity (pk) VALUES (:pk)")
                suspend fun insertEntity(pk: Long)

                @Query("INSERT INTO MyEntity (pk) VALUES (:pk)")
                suspend fun insertEntityReturnLong(pk: Long): Long

                @Query("UPDATE MyEntity SET other = :text")
                suspend fun updateEntity(text: String)

                @Query("UPDATE MyEntity SET other = :text WHERE pk = :pk")
                suspend fun updateEntityReturnInt(pk: Long, text: String): Int

                @Query("DELETE FROM MyEntity")
                suspend fun deleteEntity()

                @Query("DELETE FROM MyEntity")
                suspend fun deleteEntityReturnInt(): Int

                @Query("DELETE FROM MyEntity WHERE pk IN (:pks)")
                suspend fun deleteEntitiesIn(pks: List<Long>)
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc, COMMON.FLOW, COMMON.COROUTINES_ROOM),
            expectedFilePath = getTestGoldenPath(testName.methodName),
        )
    }

    @Test
    fun shortcutMethods_rx2() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import io.reactivex.*

            @Dao
            interface MyDao {
                @Insert
                fun insertSingle(vararg entities: MyEntity): Single<List<Long>>

                @Upsert
                fun upsertSingle(vararg entities: MyEntity): Single<List<Long>>

                @Delete
                fun deleteSingle(entity: MyEntity): Single<Int>

                @Update
                fun updateSingle(entity: MyEntity): Single<Int>
                
                @Insert
                fun insertCompletable(vararg entities: MyEntity): Completable

                @Upsert
                fun upsertCompletable(vararg entities: MyEntity): Completable

                @Delete
                fun deleteCompletable(entity: MyEntity): Completable

                @Update
                fun updateCompletable(entity: MyEntity): Completable
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            compiledFiles =
                compileFiles(
                    listOf(
                        COMMON.RX2_ROOM,
                        COMMON.RX2_SINGLE,
                        COMMON.RX2_MAYBE,
                        COMMON.RX2_COMPLETABLE,
                        COMMON.RX2_FLOWABLE,
                        COMMON.RX2_OBSERVABLE,
                        COMMON.RX2_EMPTY_RESULT_SET_EXCEPTION,
                        COMMON.PUBLISHER,
                    )
                ),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun shortcutMethods_rx3() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import io.reactivex.rxjava3.core.*

            @Dao
            interface MyDao {
                @Insert
                fun insertSingle(vararg entities: MyEntity): Single<List<Long>>

                @Upsert
                fun upsertSingle(vararg entities: MyEntity): Single<List<Long>>

                @Delete
                fun deleteSingle(entity: MyEntity): Single<Int>

                @Update
                fun updateSingle(entity: MyEntity): Single<Int>
                
                @Insert
                fun insertCompletable(vararg entities: MyEntity): Completable

                @Upsert
                fun upsertCompletable(vararg entities: MyEntity): Completable

                @Delete
                fun deleteCompletable(entity: MyEntity): Completable

                @Update
                fun updateCompletable(entity: MyEntity): Completable
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            compiledFiles =
                compileFiles(
                    listOf(
                        COMMON.RX3_ROOM,
                        COMMON.RX3_SINGLE,
                        COMMON.RX3_MAYBE,
                        COMMON.RX3_COMPLETABLE,
                        COMMON.RX3_FLOWABLE,
                        COMMON.RX3_OBSERVABLE,
                        COMMON.RX3_EMPTY_RESULT_SET_EXCEPTION,
                        COMMON.PUBLISHER,
                    )
                ),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun guavaCallable() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import com.google.common.util.concurrent.ListenableFuture
            import androidx.room.*

            @Dao
            interface MyDao {
                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getListenableFuture(vararg arg: String?): ListenableFuture<MyEntity>

                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getListenableFutureNullable(vararg arg: String?): ListenableFuture<MyEntity?>

                @Query("INSERT INTO MyEntity (pk, other) VALUES (:id, :name)")
                fun insertListenableFuture(id: String, name: String): ListenableFuture<Long>

                @Query("UPDATE MyEntity SET other = :name WHERE pk = :id")
                fun updateListenableFuture(id: String, name: String): ListenableFuture<Void?>

                @Insert
                fun insertListenableFuture(vararg entities: MyEntity): ListenableFuture<List<Long>>

                @Upsert
                fun upsertListenableFuture(vararg entities: MyEntity): ListenableFuture<List<Long>>

                @Delete
                fun deleteListenableFuture(entity: MyEntity): ListenableFuture<Int>

                @Update
                fun updateListenableFuture(entity: MyEntity): ListenableFuture<Int>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources =
                listOf(
                    src,
                    databaseSrc,
                    COMMON.LISTENABLE_FUTURE,
                    COMMON.GUAVA_ROOM,
                ),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun guavaCallable_java() {
        val daoSrc =
            Source.java(
                "MyDao",
                """
            import com.google.common.util.concurrent.ListenableFuture;
            import androidx.room.*;

            @Dao
            public interface MyDao {
                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                ListenableFuture<MyEntity> getListenableFuture(String... arg);
            }
            """
                    .trimIndent()
            )
        val entitySrc =
            Source.kotlin(
                "MyEntity.kt",
                """
            import androidx.room.*

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources =
                listOf(
                    daoSrc,
                    entitySrc,
                    databaseSrc,
                ),
            compiledFiles = compileFiles(listOf(COMMON.LISTENABLE_FUTURE, COMMON.GUAVA_ROOM)),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun liveDataCallable() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import androidx.lifecycle.*

            @Dao
            interface MyDao {
                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getLiveData(vararg arg: String?): LiveData<MyEntity>

                @Query("SELECT * FROM MyEntity WHERE pk IN (:arg)")
                fun getLiveDataNullable(vararg arg: String?): LiveData<MyEntity?>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc, COMMON.COROUTINES_ROOM),
            expectedFilePath = getTestGoldenPath(testName.methodName),
            compiledFiles = compileFiles(listOf(COMMON.LIVE_DATA))
        )
    }

    @Test
    fun shortcutMethods_suspend() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
                @Insert
                suspend fun insert(vararg entities: MyEntity): List<Long>

                @Upsert
                suspend fun upsert(vararg entities: MyEntity): List<Long>

                @Delete
                suspend fun delete(entity: MyEntity): Int

                @Update
                suspend fun update(entity: MyEntity): Int
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc, COMMON.COROUTINES_ROOM),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    @Ignore("b/339809512")
    fun shortcutMethods_java() {
        val daoSrc =
            Source.java(
                "MyDao",
                """
            import androidx.room.*;
            import java.util.List;

            @Dao
            public interface MyDao {
                @Insert
                List<Long> insert(MyEntity... entities);

                @Insert
                long[] insertList(List<MyEntity> entities);

                @Upsert
                List<Long> upsert(MyEntity... entities);

                @Upsert
                long[] upsertList(List<MyEntity> entities);

                @Delete
                void delete(MyEntity entity);

                @Delete
                int deleteList(List<MyEntity> entity);

                @Update
                void update(MyEntity entity);
                
                @Update
                int updateList(List<MyEntity> entity);
            }
            """
                    .trimIndent()
            )
        val entitySrc =
            Source.kotlin(
                "MyEntity.kt",
                """
            import androidx.room.*

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val other: String
            )
            """
                    .trimIndent()
            )
        runTest(
            sources =
                listOf(
                    daoSrc,
                    entitySrc,
                    databaseSrc,
                ),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun pojoRowAdapter_valueClassConverter() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*
            import java.util.UUID

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity

              @Insert
              fun addEntity(item: MyEntity)
            }

            @JvmInline
            value class LongValueClass(val data: Long)

            @JvmInline
            value class NullableLongValueClass(val data: Long?)

            @JvmInline
            value class UUIDValueClass(val data: UUID)

            @JvmInline
            value class GenericValueClass<T>(val password: T)

            @Entity
            data class MyEntity (
                @PrimaryKey
                val pk: LongValueClass,
                val uuidData: UUIDValueClass,
                val nullableUuidData: UUIDValueClass?,
                val nullableLongData: NullableLongValueClass,
                val doubleNullableLongData: NullableLongValueClass?,
                val genericData: GenericValueClass<String>
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun overridePropertyQuery() {
        val src =
            Source.kotlin(
                "MyDao.kt",
                """
            import androidx.room.*

            @Dao
            interface MyDao {
              @get:Query("SELECT * FROM MyEntity")
              val entities: List<MyEntity>
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int
            )
            """
                    .trimIndent()
            )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }
}
