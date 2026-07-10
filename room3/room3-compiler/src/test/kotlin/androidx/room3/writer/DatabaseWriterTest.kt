/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.room3.writer

import androidx.room3.RoomProcessor
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.runKspProcessorTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Test to validate onValidateSchema is split in various functions to avoid code too big. See
 * b/111166670 and b/493708172
 */
@RunWith(Parameterized::class)
class ValidationWriterTest(val entitiesCount: Int, val columnsPerEntity: Int) {
    @Test
    fun bigDatabase() {
        check(entitiesCount > 0)
        check(columnsPerEntity > 0)
        val entitySources =
            List(entitiesCount) { entityId ->
                val entityProperties =
                    List(columnsPerEntity) { propertyId ->
                            if (propertyId == 0) {
                                "@PrimaryKey val pk: Long"
                            } else {
                                "val prop$propertyId: String"
                            }
                        }
                        .joinToString()
                Source.kotlin(
                    "Entity$entityId.kt",
                    """
                package foo.bar
                import androidx.room3.*
                @Entity
                data class Entity$entityId($entityProperties)
                """
                        .trimIndent(),
                )
            }
        val entityClasses = List(entitiesCount) { "Entity$it::class" }.joinToString()
        val dbSource =
            Source.kotlin(
                filePath = "TestDatabase.kt",
                code =
                    """
                package foo.bar
                import androidx.room3.*
                @Database(entities = [$entityClasses], version = 1, exportSchema = false)
                abstract class TestDatabase : RoomDatabase()
                """,
            )
        runKspProcessorTest(
            sources = entitySources + dbSource,
            symbolProcessorProviders = listOf(RoomProcessor.Provider()),
        ) {
            it.hasErrorCount(0)
            it.hasWarningCount(0)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "entitiesCount={0}, columnsPerEntity={1}")
        fun getParams(): Array<Array<Int>> =
            arrayOf(
                arrayOf(10, 10),
                arrayOf(10, 30),
                arrayOf(10, 50),
                arrayOf(30, 10),
                arrayOf(30, 30),
                arrayOf(30, 50),
                arrayOf(100, 10),
                arrayOf(100, 30),
                arrayOf(100, 50),
            )
    }
}
