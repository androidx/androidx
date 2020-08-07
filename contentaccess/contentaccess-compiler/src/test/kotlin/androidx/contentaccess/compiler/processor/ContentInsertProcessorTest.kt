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

import androidx.contentaccess.compiler.processor.contentInsertAnnotatedMethodNotReturningAUri
import androidx.contentaccess.compiler.processor.insertMethodHasMoreThanOneEntity
import androidx.contentaccess.compiler.processor.insertMethodHasNoEntityInParameters
import androidx.contentaccess.compiler.processor.missingUriOnMethod
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ContentInsertProcessorTest {

    fun generateMainSourceFile(accessorBody: String):
            SourceFile {
        return SourceFile.kotlin("MyClass.kt", """
        package androidx.contentaccess.compiler.processor.test

        import androidx.contentaccess.ContentAccessObject
        import androidx.contentaccess.ContentPrimaryKey
        import androidx.contentaccess.ContentColumn
        import androidx.contentaccess.ContentInsert
        import androidx.contentaccess.ContentEntity
        import android.net.Uri

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

        @ContentAccessObject
        interface ContentAccessor {
            $accessorBody
        }
        """
        )
    }

    @Test
    fun validInsert() {
        val sourceFile = generateMainSourceFile("""
        @ContentInsert
        fun insertEntity(entity: Entity): Uri?
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    fun validInsertWithUriLessEntity() {
        val sourceFile = generateMainSourceFile("""
        @ContentInsert(uri = ":uri")
        fun insertUriLessEntity(
            entity: EntityWithoutUri,
            uri: String
        ): Uri?
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    fun checkThereIsAnEntityInParameters() {
        val sourceFile = generateMainSourceFile("""
        @ContentInsert
        fun insertWithNoEntity(randomNonEntityParam: String): Uri?
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(insertMethodHasNoEntityInParameters())
    }

    @Test
    fun checkNoMoreThanOneEntityInParameters() {
        val sourceFile = generateMainSourceFile("""
        @ContentInsert
        fun insertMultipleEntities(entity1: Entity, entity2: Entity): Uri?
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(insertMethodHasMoreThanOneEntity())
    }

    @Test
    fun ensureReturnTypeIsUri() {
        val sourceFile = generateMainSourceFile("""
        @ContentInsert
        fun insertEntity(entity: Entity): Int?
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(contentInsertAnnotatedMethodNotReturningAUri()
        )
    }

    @Test
    fun ensureUriExists() {
        val sourceFile = generateMainSourceFile("""
        @ContentInsert
        fun insertEntity(entity: EntityWithoutUri): Int?
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(missingUriOnMethod()
        )
    }
}