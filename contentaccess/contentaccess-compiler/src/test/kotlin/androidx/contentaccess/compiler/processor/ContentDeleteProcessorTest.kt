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

import androidx.contentaccess.compiler.processor.columnInSelectionMissingFromEntity
import androidx.contentaccess.compiler.processor.contentDeleteAnnotatedMethodNotReturningAnInteger
import androidx.contentaccess.compiler.processor.missingUriOnMethod
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ContentDeleteProcessorTest {

    val entityName = "androidx.contentaccess.compiler.processor.test.Entity"

    fun generateMainSourceFile(accessorBody: String, entityWithoutUri: Boolean = false):
            SourceFile {
        return SourceFile.kotlin("MyClass.kt", """
        package androidx.contentaccess.compiler.processor.test

        import androidx.contentaccess.ContentAccessObject
        import androidx.contentaccess.ContentPrimaryKey
        import androidx.contentaccess.ContentColumn
        import androidx.contentaccess.ContentDelete
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

        @ContentEntity
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
    fun validDelete() {
        val sourceFile = generateMainSourceFile("""
        @ContentDelete
        fun deleteAll(): Int
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    fun validDeleteWithUriLessEntity() {
        val sourceFile = generateMainSourceFile("""
        @ContentDelete(uri = ":uri")
        fun deleteUriLessEntity(uri: String): Int?
        """.trimIndent(), true)

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    fun checkThereIsAUriSomewhere() {
        val sourceFile = generateMainSourceFile("""
        @ContentDelete
        fun deleteWithNoUri(): Int?
        """.trimIndent(), true)

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(missingUriOnMethod())
    }

    @Test
    fun ensureContentEntityInAnnotationTakesPrecedence() {
        val sourceFile = generateMainSourceFile("""
        @ContentDelete(contentEntity = EntityWithoutUri::class)
        fun deleteAll(): Int?
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        // If this says no uri, that means it did indeed consider the EntityWithoutUri instead of
        // Entity.
        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(missingUriOnMethod())
    }

    @Test
    fun ensureReturnsAnInt() {
        val sourceFile = generateMainSourceFile("""
        @ContentDelete
        fun deleteAll(): String?
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(
            contentDeleteAnnotatedMethodNotReturningAnInteger()
        )
    }

    @Test
    fun ensureColumnsInWhereExist() {
        val sourceFile = generateMainSourceFile("""
        @ContentDelete(where = "unknown_column = :param")
        fun deleteAll(param: String): Int?
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(
            columnInSelectionMissingFromEntity("unknown_column", entityName)
        )
    }
}