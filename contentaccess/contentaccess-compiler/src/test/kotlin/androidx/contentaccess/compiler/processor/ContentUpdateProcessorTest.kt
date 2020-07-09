/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.contentaccess.compiler

import androidx.contentaccess.compiler.processor.columnInContentUpdateParametersNotInEntity
import androidx.contentaccess.compiler.processor.contentUpdateAnnotatedMethodNotReturningAnInteger
import androidx.contentaccess.compiler.processor.methodSpecifiesWhereClauseWhenUpdatingUsingEntity
import androidx.contentaccess.compiler.processor.mismatchedColumnTypeForColumnToBeUpdated
import androidx.contentaccess.compiler.processor.nullableUpdateParamForNonNullableEntityColumn
import androidx.contentaccess.compiler.processor.unsureWhatToUpdate
import androidx.contentaccess.compiler.processor.updatingMultipleEntitiesAtTheSameType
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ContentUpdateProcessorTest {

    val entityName = "androidx.contentaccess.compiler.processor.test.Entity"

    fun generateMainSourceFile(accessorBody: String, entityWithoutUri: Boolean = false):
            SourceFile {
        return SourceFile.kotlin("MyClass.kt", """
        package androidx.contentaccess.compiler.processor.test

        import androidx.contentaccess.ContentAccessObject
        import androidx.contentaccess.ContentPrimaryKey
        import androidx.contentaccess.ContentColumn
        import androidx.contentaccess.ContentUpdate
        import androidx.contentaccess.ContentEntity

        @ContentEntity("uri")
        data class Entity(
            @ContentPrimaryKey("_id")
            val id: Long,
            @ContentColumn("dtstart")
            val startTime: Long?,
            @ContentColumn("dtend")
            val endTime: Long,
            @ContentColumn("description")
            val description: String?
        )

        @ContentEntity("uri")
        data class EntityWithoutUri(
            @ContentPrimaryKey("_id")
            val id: Long,
            @ContentColumn("dtstart")
            val startTime: Long?,
            @ContentColumn("description")
            val description: String?
        )

        @ContentAccessObject(${if (entityWithoutUri) "EntityWithoutUri::class" else
            "Entity::class"})
        interface ContentAccessor {
            $accessorBody
        }
        """
        )
    }

    @Test
    fun validUpdates() {
        val sourceFile = generateMainSourceFile("""
        @ContentUpdate
        fun updateDescription(@ContentColumn("description") desc: String): Int

        @ContentUpdate(where = "_id = :id", uri = ":uri")
        fun updateDescriptionAndStartTime(
            @ContentColumn("description") desc: String,
            @ContentColumn("dtstart") startTime: Long?,
            id: Long,
            uri: String
        ): Int
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    fun validUpdatesWithUriLessEntity() {
        val sourceFile = generateMainSourceFile("""
        @ContentUpdate(where = "_id = 123", uri = ":uri")
        fun updateDescriptionWithUri(
            @ContentColumn("description") desc: String,
            uri: String
        ): Int

        @ContentUpdate(where = "_id = 123", contentEntity = Entity::class)
        fun updateDescription(
            @ContentColumn("description") desc: String
        ): Int
        """.trimIndent(), entityWithoutUri = true)

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    fun checkColumnsExist() {
        val sourceFile = generateMainSourceFile("""
        @ContentUpdate(where = "_id = :id")
        fun updateDescription(@ContentColumn("nonexistent") desc: String, id: Long): Int
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(columnInContentUpdateParametersNotInEntity("desc",
            "nonexistent", entityName))
    }

    @Test
    fun ensureTypesMatch() {
        val sourceFile = generateMainSourceFile("""
        @ContentUpdate
        fun updateDescription(@ContentColumn("description") desc: Long): Int
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(
            mismatchedColumnTypeForColumnToBeUpdated("desc",
            "description", "long", entityName, "java.lang.String")
        )
    }

    @Test
    fun ensureNoWhereClauseWhenUpdatingEntity() {
        val sourceFile = generateMainSourceFile("""
        @ContentUpdate(where = "_id = 123")
        fun updateDescription(entityParam: Entity): Int
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(
            methodSpecifiesWhereClauseWhenUpdatingUsingEntity("entityParam")
        )
    }

    @Test
    fun ensureOnlyOneEntityIsUpdatedAtTheSameTime() {
        val sourceFile = generateMainSourceFile("""
        @ContentUpdate
        fun updateDescription(entityParam1: Entity, entityParam2: Entity): Int
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(
            updatingMultipleEntitiesAtTheSameType(entityName, "updateDescription")
        )
    }

    @Test
    fun ensureContentUpdateMethodReturnsAnInteger() {
        val sourceFile = generateMainSourceFile("""
        @ContentUpdate
        fun updateDescription(entityParam1: Entity): Long
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(contentUpdateAnnotatedMethodNotReturningAnInteger())
    }

    @Test
    fun ensureSomethingIsBeingUpdated() {
        val sourceFile = generateMainSourceFile("""
        @ContentUpdate
        fun updateDescription(entities: List<Entity>): Int
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(unsureWhatToUpdate())
    }

    @Test
    fun ensureNonNullableUpdateParametersForNonNullablEntityColumn() {
        val sourceFile = generateMainSourceFile("""
        @ContentUpdate
        fun updateDescription(@ContentColumn("dtend") newEndTime: Long?): Int
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(nullableUpdateParamForNonNullableEntityColumn
            ("newEndTime", "dtend", entityName))
    }
}