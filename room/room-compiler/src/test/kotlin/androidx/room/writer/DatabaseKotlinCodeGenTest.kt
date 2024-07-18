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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

class DatabaseKotlinCodeGenTest {

    @get:Rule
    val testName = TestName()

    @Test
    fun database_simple() {
        val src = Source.kotlin(
            "MyDatabase.kt",
            """
            import androidx.room.*

            @Database(entities = [MyEntity::class], version = 1, exportSchema = false)
            abstract class MyDatabase : RoomDatabase() {
              abstract fun getDao(): MyDao
            }

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
            }

            @Entity
            data class MyEntity(
                @PrimaryKey
                var pk: Int
            )
            """.trimIndent()
        )
        runTest(
            sources = listOf(src),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun database_withFtsAndView() {
        val src = Source.kotlin(
            "MyDatabase.kt",
            """
            import androidx.room.*

            @Database(
                entities = [
                    MyParentEntity::class,
                    MyEntity::class,
                    MyFtsEntity::class,
                ],
                views = [ MyView::class ],
                version = 1,
                exportSchema = false
            )
            abstract class MyDatabase : RoomDatabase() {
              abstract fun getDao(): MyDao
            }

            @Dao
            interface MyDao {
              @Query("SELECT * FROM MyEntity")
              fun getEntity(): MyEntity
            }

            @Entity
            data class MyParentEntity(@PrimaryKey val parentKey: Long)

            @Entity(
                foreignKeys = [
                    ForeignKey(
                        entity = MyParentEntity::class,
                        parentColumns = ["parentKey"],
                        childColumns = ["indexedCol"],
                        onDelete = ForeignKey.CASCADE
                    )
                ],
                indices = [Index("indexedCol")]
            )
            data class MyEntity(
                @PrimaryKey
                val pk: Int,
                val indexedCol: String
            )

            @Fts4
            @Entity
            data class MyFtsEntity(
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                val pk: Int,
                val text: String
            )

            @DatabaseView("SELECT text FROM MyFtsEntity")
            data class MyView(val text: String)
            """.trimIndent()
        )
        runTest(
            sources = listOf(src),
            expectedFilePath = getTestGoldenPath(testName.methodName)
        )
    }

    @Test
    fun database_internalVisibility() {
        val src = Source.kotlin(
            "MyDatabase.kt",
            """
            import androidx.room.*

            @Database(entities = [MyEntity::class], version = 1, exportSchema = false)
            internal abstract class MyDatabase : RoomDatabase() {
              internal abstract fun getDao(): MyDao
            }

            @Dao
            internal abstract class MyDao {
              @Query("SELECT * FROM MyEntity")
              internal abstract fun getEntity(): MyEntity
            }

            @Entity
            internal data class MyEntity internal constructor(
                @PrimaryKey
                var pk: Int
            )
            """.trimIndent()
        )
        runTest(
            sources = listOf(src),
            expectedFilePath = getTestGoldenPath(testName.methodName)
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
                        "MyDatabase_Impl"
                    )
                )
                this.hasNoWarnings()
            }
            handler.invoke(it)
        }
    }
}
