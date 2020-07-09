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

import androidx.contentaccess.compiler.processor.entityFieldWithBothAnnotations
import androidx.contentaccess.compiler.processor.entityWithMultipleConstructors
import androidx.contentaccess.compiler.processor.entityWithNullablePrimitiveType
import androidx.contentaccess.compiler.processor.missingAnnotationOnEntityFieldErrorMessage
import androidx.contentaccess.compiler.processor.missingEntityPrimaryKeyErrorMessage
import androidx.contentaccess.compiler.processor.missingFieldsInContentEntityErrorMessage
import androidx.contentaccess.compiler.processor.nonInstantiableEntity
import androidx.contentaccess.compiler.processor.unsupportedColumnType
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ContentEntityProcessorTest {

    val entityName = "androidx.contentaccess.compiler.processor.test.Entity"

    fun generateMainSourceFile(entityCode: String = ""): SourceFile {
        return SourceFile.kotlin("MyClass.kt", """
        package androidx.contentaccess.compiler.processor.test

        import androidx.contentaccess.ContentAccessObject
        import androidx.contentaccess.ContentPrimaryKey
        import androidx.contentaccess.ContentColumn
        import androidx.contentaccess.ContentQuery
        import androidx.contentaccess.ContentEntity

        @ContentAccessObject(Entity::class)
        interface ContentAccessor {
            @ContentQuery
            fun getAll(): List<Entity>
        }

        $entityCode
        """)
    }

    fun generateJavaEntity(entityBody: String): SourceFile {
        return SourceFile.java("Entity.java", """
        package androidx.contentaccess.compiler.processor.test;

        import org.jetbrains.annotations.Nullable;
        import androidx.contentaccess.ContentColumn;
        import androidx.contentaccess.ContentPrimaryKey;
        import androidx.contentaccess.IgnoreConstructor;
        import androidx.contentaccess.ContentEntity;

        @ContentEntity(uri = "uri")
        public class Entity {
            $entityBody
        }
        """.trimIndent())
    }

    @Test
    fun validContentEntity() {
        val sourceFile = generateMainSourceFile("""
        @ContentEntity("example.uri")
        data class Entity(
            @ContentPrimaryKey("_id") val id: Int,
            @ContentColumn("a_random_long") val randomLong: Long
        )
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    fun validJavaContentEntity() {
        val mainSourceFile = generateMainSourceFile()
        val javaEntityFile = generateJavaEntity("""
            @ContentPrimaryKey(columnName = "_id")
            public long eventId;

            @ContentColumn(columnName = "dtstart")
            public long startTime;

            @ContentColumn(columnName = "dtend")
            @Nullable
            public Long endTime;
        """.trimIndent())

        val result = runCompilation(listOf(mainSourceFile, javaEntityFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    fun ensureExistingContentPrimaryKey() {
        val sourceFile = generateMainSourceFile("""
        @ContentEntity("example.uri")
        data class Entity(
            @ContentColumn("_id") val id: Int,
            @ContentColumn("a_random_long") val randomLong: Long
        )
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(missingEntityPrimaryKeyErrorMessage(entityName))
    }

    @Test
    fun ensureEntityContainsFields() {
        val sourceFile = generateMainSourceFile("""
        @ContentEntity("example.uri")
        data class Entity()
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(missingFieldsInContentEntityErrorMessage(entityName))
    }

    @Test
    fun ensureAllFieldsAreAnnotated() {
        val sourceFile = generateMainSourceFile("""
        @ContentEntity("example.uri")
        data class Entity(
            val id: Int
        )
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages)
            .contains(missingAnnotationOnEntityFieldErrorMessage("id", entityName))
    }

    @Test
    fun ensureOnlyOneAnnotation() {
        val sourceFile = generateMainSourceFile("""
        @ContentEntity("example.uri")
        data class Entity(
            @ContentPrimaryKey("_id")
            @ContentColumn("_id")
            val id: Int,
            @ContentColumn("a_random_long") val randomLong: Long
        )
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages)
            .contains(entityFieldWithBothAnnotations("id", entityName))
    }

    @Test
    fun ensureSupportedColumnType() {
        val sourceFile = generateMainSourceFile("""
        @ContentEntity("example.uri")
        data class Entity(
            @ContentPrimaryKey("_id") val id: List<Long>,
            @ContentColumn("a_random_long") val randomLong: Long
        )
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages)
            .contains(unsupportedColumnType("id", entityName, "java.util.List<java.lang.Long>"))
    }

    @Test
    fun ensureSingleNonPrivateNonIgnoredConstructor() {
        val sourceFile = generateMainSourceFile("""
        @ContentEntity("example.uri")
        data class Entity(
            @ContentPrimaryKey("_id") val id: Long,
            @ContentColumn("a_random_long") val randomLong: Long?
        ) {
            constructor(@ContentPrimaryKey("_id") id: Long) : this(id, null)
        }
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(entityWithMultipleConstructors(entityName))
    }

    @Test
    fun ensureInstantiableEntity() {
        val mainSourceFile = generateMainSourceFile()
        val javaEntitySourceFile = generateJavaEntity("""
            @ContentColumn(columnName = "_id")
            public long eventId;

            @ContentColumn(columnName = "dtstart")
            @Nullable
            public Long startTime;

            // This constructor should be ignored.
            @IgnoreConstructor
            public Entity(int randomParameter) {}

            private Entity() {}
        """.trimIndent())
        val result = runCompilation(listOf(mainSourceFile, javaEntitySourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(nonInstantiableEntity(entityName))
    }

    @Test
    fun ensureNonNullablePrimitives() {
        val mainSourceFile = generateMainSourceFile()
        val javaEntitySourceFile = generateJavaEntity("""
            @ContentPrimaryKey(columnName = "_id")
            public long eventId;

            @ContentColumn(columnName = "dtstart")
            @Nullable
            public long startTime;
        """.trimIndent())
        val result = runCompilation(listOf(mainSourceFile, javaEntitySourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(entityWithNullablePrimitiveType("startTime",
            entityName))
    }
}

fun runCompilation(sourceFiles: List<SourceFile>): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        sources = sourceFiles
        annotationProcessors = listOf(ContentAccessProcessor())
        inheritClassPath = true
        verbose = false
    }.compile()
}