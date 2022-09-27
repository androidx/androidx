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

import androidx.room.DatabaseProcessingStep
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.processor.Context
import loadTestSource
import org.junit.Test

// Dany's Kotlin codegen test playground (and tests too)
class KotlinCodeGenTest {

    val databaseSrc = Source.kotlin(
        "MyDatabase.kt",
        """
        import androidx.room.*

        @Database(entities = [MyEntity::class], version = 1, exportSchema = false)
        abstract class MyDatabase : RoomDatabase() {
          abstract fun getDao(): MyDao
        }
        """.trimIndent()
    )

    @Test
    fun pojoRowAdapter_primitives() {
        val testName = object {}.javaClass.enclosingMethod!!.name
        val src = Source.kotlin(
            "MyDao.kt",
            """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
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
            """.trimIndent()
        )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName)
        )
    }

    @Test
    fun pojoRowAdapter_primitives_nullable() {
        val testName = object {}.javaClass.enclosingMethod!!.name
        val src = Source.kotlin(
            "MyDao.kt",
            """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
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
            """.trimIndent()
        )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName)
        )
    }

    @Test
    fun pojoRowAdapter_boolean() {
        val testName = object {}.javaClass.enclosingMethod!!.name
        val src = Source.kotlin(
            "MyDao.kt",
            """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val boolean: Boolean,
                val nullableBoolean: Boolean?,
            )
            """.trimIndent()
        )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName)
        )
    }

    @Test
    fun pojoRowAdapter_string() {
        val testName = object {}.javaClass.enclosingMethod!!.name
        val src = Source.kotlin(
            "MyDao.kt",
            """
            import androidx.room.*

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val string: String,
                val nullableString: String?,
            )
            """.trimIndent()
        )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName)
        )
    }

    @Test
    fun pojoRowAdapter_byteArray() {
        val testName = object {}.javaClass.enclosingMethod!!.name
        val src = Source.kotlin(
            "MyDao.kt",
            """
            import androidx.room.*import java.nio.ByteBuffer

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val byteArray: ByteArray,
                val nullableByteArray: ByteArray?,
            )
            """.trimIndent()
        )
        runTest(
            sources = listOf(src, databaseSrc),
            expectedFilePath = getTestGoldenPath(testName)
        )
    }

    private fun getTestGoldenPath(testName: String): String {
        return "kotlinCodeGen/$testName.kt"
    }

    private fun runTest(
        sources: List<Source>,
        expectedFilePath: String,
        handler: (XTestInvocation) -> Unit = { }
    ) {
        runKspTest(
            sources = sources,
            options = mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "true"),
        ) {
            val databaseFqn = "androidx.room.Database"
            DatabaseProcessingStep().process(
                it.processingEnv,
                mapOf(databaseFqn to it.roundEnv.getElementsAnnotatedWith(databaseFqn)),
                it.roundEnv.isProcessingOver
            )
            it.assertCompilationResult {
                this.generatedSource(
                    loadTestSource(
                        expectedFilePath,
                        "androidx.room.temp.PojoRowAdapter_1427165205"
                    )
                )
                this.hasNoWarnings()
            }
            handler.invoke(it)
        }
    }
}