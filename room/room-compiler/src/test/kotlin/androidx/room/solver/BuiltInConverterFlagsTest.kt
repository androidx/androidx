/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.solver

import androidx.room.BuiltInTypeConverters
import androidx.room.BuiltInTypeConverters.State.DISABLED
import androidx.room.BuiltInTypeConverters.State.ENABLED
import androidx.room.Database
import androidx.room.DatabaseProcessingStep
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors.CANNOT_FIND_COLUMN_TYPE_ADAPTER
import androidx.room.processor.ProcessorErrors.CANNOT_FIND_STMT_READER
import androidx.room.runProcessorTestWithK1
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BuiltInConverterFlagsTest {

    @Test
    fun enums_disabledInDb() {
        compile(dbAnnotation = createTypeConvertersCode(enums = DISABLED)) {
            hasError(CANNOT_FIND_COLUMN_TYPE_ADAPTER, "val myEnum: MyEnum")
            hasError(CANNOT_FIND_STMT_READER, "val myEnum: MyEnum")
            hasErrorCount(2)
        }
    }

    @Test
    fun uuid_disabledInDb() {
        compile(dbAnnotation = createTypeConvertersCode(uuid = DISABLED)) {
            hasError(CANNOT_FIND_COLUMN_TYPE_ADAPTER, "val uuid: UUID")
            hasError(CANNOT_FIND_STMT_READER, "val uuid: UUID")
            hasErrorCount(2)
        }
    }

    @Test
    fun byteBuffer_disabledInDb() {
        compile(dbAnnotation = createTypeConvertersCode(byteBuffer = DISABLED)) {
            hasError(CANNOT_FIND_COLUMN_TYPE_ADAPTER, "val blob: ByteBuffer")
            hasError(CANNOT_FIND_STMT_READER, "val blob: ByteBuffer")
            hasErrorCount(2)
        }
    }

    @Test
    fun all_disabledInDb_enabledInDao_enabledInEntity() {
        compile(
            dbAnnotation = createTypeConvertersCode(enums = DISABLED, uuid = DISABLED),
            daoAnnotation = createTypeConvertersCode(enums = ENABLED, uuid = ENABLED),
            entityAnnotation = createTypeConvertersCode(enums = ENABLED, uuid = ENABLED)
        ) {
            // success
        }
    }

    @Test
    fun all_disabledInEntity() {
        compile(
            entityAnnotation =
                createTypeConvertersCode(enums = DISABLED, uuid = DISABLED, byteBuffer = DISABLED)
        ) {
            hasError(CANNOT_FIND_COLUMN_TYPE_ADAPTER, "val uuid: UUID")
            hasError(CANNOT_FIND_COLUMN_TYPE_ADAPTER, "val myEnum: MyEnum")
            hasError(CANNOT_FIND_COLUMN_TYPE_ADAPTER, "val blob: ByteBuffer")
            // even though it is enabled in dao or db, since pojo processing will visit the pojo,
            // we'll still get errors for these because entity disabled them
            hasError(CANNOT_FIND_STMT_READER, "val uuid: UUID")
            hasError(CANNOT_FIND_STMT_READER, "val myEnum: MyEnum")
            hasError(CANNOT_FIND_STMT_READER, "val blob: ByteBuffer")
            hasErrorCount(6)
        }
    }

    @Test
    fun all_disabledInDb_disabledInDao_enabledInEntity() {
        compile(
            dbAnnotation =
                createTypeConvertersCode(enums = DISABLED, uuid = DISABLED, byteBuffer = DISABLED),
            daoAnnotation =
                createTypeConvertersCode(enums = DISABLED, uuid = DISABLED, byteBuffer = DISABLED),
            entityAnnotation =
                createTypeConvertersCode(enums = ENABLED, uuid = ENABLED, byteBuffer = ENABLED)
        ) {
            // success since we only fetch full objects.
        }
    }

    @Test
    fun all_undefined() {
        compile {
            // success
        }
    }

    /** KAPT does not have proper error lines so we only test message contents there */
    private fun XTestInvocation.hasError(msg: String, lineContent: String) {
        assertCompilationResult {
            if (isKsp) {
                hasErrorContaining(msg).onLineContaining(lineContent)
            } else {
                hasErrorContaining(msg)
            }
        }
    }

    private fun XTestInvocation.hasErrorCount(expected: Int) = assertCompilationResult {
        hasErrorCount(expected)
    }

    fun compile(
        entityAnnotation: String = "",
        daoAnnotation: String = "",
        dbAnnotation: String = "",
        assertion: XTestInvocation.() -> Unit
    ) {
        val source =
            buildSource(
                entityAnnotation = entityAnnotation,
                daoAnnotation = daoAnnotation,
                dbAnnotation = dbAnnotation
            )
        runProcessorTestWithK1(
            sources = listOf(source),
            options = mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "false"),
        ) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("MyDatabase")
            DatabaseProcessingStep()
                .process(
                    env = invocation.processingEnv,
                    elementsByAnnotation = mapOf(Database::class.qualifiedName!! to setOf(subject)),
                    false
                )
            invocation.assertCompilationResult {
                generatedSourceFileWithPath("MyDatabase_Impl.java")
                generatedSourceFileWithPath("MyDao_Impl.java")
            }
            assertion.invoke(invocation)
        }
    }

    private fun buildSource(
        entityAnnotation: String = "",
        daoAnnotation: String = "",
        dbAnnotation: String = "",
    ): Source {
        return Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.*import java.nio.ByteBuffer
            import java.util.UUID
            enum class MyEnum {
                VAL_1,
                VAL_2
            }

            $entityAnnotation
            @Entity
            data class MyEntity(
                @PrimaryKey
                val id:Int,
                val uuid: UUID,
                val myEnum: MyEnum,
                val blob: ByteBuffer
            )

            $daoAnnotation
            @Dao
            interface MyDao {
                @Query("SELECT * FROM MyEntity")
                fun entities(): List<MyEntity>
            }

            $dbAnnotation
            @Database(version = 1, entities = [MyEntity::class], exportSchema = false)
            abstract class MyDatabase : RoomDatabase() {
                abstract val myDao: MyDao
            }
            """
                .trimIndent()
        )
    }

    private fun createTypeConvertersCode(
        enums: BuiltInTypeConverters.State? = null,
        uuid: BuiltInTypeConverters.State? = null,
        byteBuffer: BuiltInTypeConverters.State? = null
    ): String {
        val builtIns =
            listOfNotNull(
                    enums?.let { "enums = BuiltInTypeConverters.State.${enums.name}" },
                    uuid?.let { "uuid = BuiltInTypeConverters.State.${uuid.name}" },
                    byteBuffer?.let {
                        "byteBuffer = BuiltInTypeConverters.State.${byteBuffer.name}"
                    },
                )
                .joinToString(",")
        return if (builtIns.isBlank()) {
            ""
        } else {
            "@TypeConverters(" + "builtInTypeConverters = BuiltInTypeConverters($builtIns)" + ")"
        }
    }
}
