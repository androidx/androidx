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

import androidx.contentaccess.compiler.processor.badlyFormulatedOrderBy
import androidx.contentaccess.compiler.processor.columnInProjectionNotIncludedInReturnPojo
import androidx.contentaccess.compiler.processor.columnOnlyAsUri
import androidx.contentaccess.compiler.processor.constructorFieldNotIncludedInProjectionNotNullable
import androidx.contentaccess.compiler.processor.missingEntityOnMethod
import androidx.contentaccess.compiler.processor.missingUriOnMethod
import androidx.contentaccess.compiler.processor.missingUriParameter
import androidx.contentaccess.compiler.processor.nullableEntityColumnNotNullableInPojo
import androidx.contentaccess.compiler.processor.pojoFieldNotInEntity
import androidx.contentaccess.compiler.processor.pojoHasMoreThanOneQualifyingConstructor
import androidx.contentaccess.compiler.processor.pojoIsNotInstantiable
import androidx.contentaccess.compiler.processor.pojoWithNullablePrimitive
import androidx.contentaccess.compiler.processor.queriedColumnInProjectionNotInEntity
import androidx.contentaccess.compiler.processor.queriedColumnInProjectionTypeDoesntMatchReturnType
import androidx.contentaccess.compiler.processor.uriParameterIsNotString
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ContentQueryProcessorTest {

    val entityName = "androidx.contentaccess.compiler.processor.test.EntityWithUri"
    val pojoName = "androidx.contentaccess.compiler.processor.test.Pojo"

    fun generateMainSourceFile(
        accessorBody: String,
        pojos: String = "",
        withEntity: Boolean = true
    ): SourceFile {
        return SourceFile.kotlin("MyClass.kt", """
        package androidx.contentaccess.compiler.processor.test

        import androidx.contentaccess.ContentAccessObject
        import androidx.contentaccess.ContentPrimaryKey
        import androidx.contentaccess.ContentColumn
        import androidx.contentaccess.ContentQuery
        import androidx.contentaccess.ContentEntity
        import java.util.Optional

        @ContentEntity("uri")
        data class EntityWithUri(
            @ContentPrimaryKey("_id")
            val id: Long,
            @ContentColumn("dtstart")
            val startTime: Long?,
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

        @ContentAccessObject(${if (withEntity) "EntityWithUri::class" else ""})
        interface ContentAccessor {
            $accessorBody
        }

        $pojos
        """
        )
    }

    fun generateJavaPojo(pojoBody: String): SourceFile {
        return SourceFile.java("Pojo.java", """
        package androidx.contentaccess.compiler.processor.test;

        import org.jetbrains.annotations.Nullable;
        import androidx.contentaccess.ContentColumn;
        import androidx.contentaccess.ContentPrimaryKey;
        import androidx.contentaccess.IgnoreConstructor;

        public class Pojo {
            $pojoBody
        }
        """.trimIndent())
    }

    @Test
    fun validQueries() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery
        fun getAllStartTimeDescriptionAnnotatedPojo(): List<StartTimeDescription2>

        @ContentQuery(projection = arrayOf("dtstart", "description"))
        fun getAllStartTimeDescriptionNonAnnotatedPojoWithProjection(): List<StartTimeDescription1>

        @ContentQuery(projection = arrayOf("description"))
        fun getSingleColumnWithoutUsingPojo(): String?

        @ContentQuery(projection = arrayOf("description"))
        fun getSetOfSingleColumnResultsWithoutUsingPojo(): Set<String>

        @ContentQuery(projection = arrayOf("description"))
        fun getOptionalSingleColumn(): Optional<String?>

        @ContentQuery(projection = arrayOf("description"))
        fun getOptionalSingleColumnNonNullable(): Optional<String>

        @ContentQuery(projection = arrayOf("dtstart"),
            selection = "description = \"whatever\" and dtstart > 1010101010")
        fun getBasedOnSelectionWithoutParameter(): List<Long?>

        @ContentQuery(projection = arrayOf("dtstart"),
            selection = "description = :descr and dtstart > :startTime")
        fun getBasedOnSelectionWithParameter(descr: String, startTime: Long): List<Long?>

        @ContentQuery(projection = arrayOf("_id"), orderBy = arrayOf("dtstart", "description"))
        fun getAllOrderBy(descr: String): List<Long?>

        @ContentQuery(projection = arrayOf("_id"),
            orderBy = arrayOf("dtstart asc", "description desc"))
        fun getAllOrderByAscDesc(): List<Long?>

        @ContentQuery(projection = arrayOf("description"), uri = ":uriParam")
        fun getAllWithSpecifiedUriThroughParameter(uriParam: String): String?

        @ContentQuery(projection = arrayOf("description"), uri = "uri://inAnnotation:D")
        fun getAllWithSpecifiedUriInAnnotation(): String?

        @ContentQuery
        suspend fun getAllEntitiesSuspend(): List<EntityWithUri>
        """.trimIndent(), """
        data class StartTimeDescription1(
            @ContentColumn("dtstart") val startingTime: Long?,
            @ContentColumn("description") val theDescription: String?
        )
        // Field name should be considered as the column name since no @ContentColumn annotation
        data class StartTimeDescription2(
            val dtstart: Long?,
            val description: String?
        )
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    fun checkExistingEntity() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery
        fun getAll(): List<String>
        """.trimIndent(), withEntity = false)

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(missingEntityOnMethod("getAll"))
    }

    @Test
    fun checkingExistingUri() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(contentEntity = EntityWithoutUri::class)
        fun getAll(): List<String>
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(missingUriOnMethod())
    }

    @Test
    fun missingOrderByColumn() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(orderBy = arrayOf("nonExistingColumn"))
        fun getAll(): List<String>
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(badlyFormulatedOrderBy("nonExistingColumn"))
    }

    @Test
    fun badlyFormulatedOrderBy() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(orderBy = arrayOf("dtstart desc thisshouldntbehere"))
        fun getAll(): List<String>
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(badlyFormulatedOrderBy("dtstart desc " +
                "thisshouldntbehere"))
    }

    @Test
    fun ensureExistingColumn() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(projection = arrayOf("nonExisting"))
        fun getAll(): List<String>
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(
            queriedColumnInProjectionNotInEntity("nonExisting",
                "androidx.contentaccess.compiler.processor.test.EntityWithUri")
        )
    }

    @Test
    fun ensureMatchingReturnTypeNonPojo() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(projection = arrayOf("description"))
        fun getAll(): Long
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(
            queriedColumnInProjectionTypeDoesntMatchReturnType("long",
                "java.lang.String",
                "description")
        )
    }

    @Test
    // A "qualifying" constructor is a non private and non ignored constructor
    fun ensurePojoHasNoMoreThanOneQualifyingConstructor() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery
        fun getAll(): Pojo
        """.trimIndent())
        val javaPojoFile = generateJavaPojo("""
            // Two public constructors
            public Pojo() {}
            public Pojo(int unused) {}
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile, javaPojoFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(pojoHasMoreThanOneQualifyingConstructor(pojoName))
    }

    @Test
    fun ensurePojoIsInstantiable() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery
        fun getAll(): Pojo
        """.trimIndent())
        val javaPojoFile = generateJavaPojo("""
            // Two public constructors
            private Pojo() {}
            @IgnoreConstructor
            public Pojo(int unused) {}
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile, javaPojoFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(pojoIsNotInstantiable(pojoName))
    }

    @Test
    fun ensurePojoDoesntHaveANullablePrimitive() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery
        fun getAll(): Pojo
        """.trimIndent())
        val javaPojoFile = generateJavaPojo("""
            @Nullable
            public long dtstart;
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile, javaPojoFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(pojoWithNullablePrimitive("dtstart", pojoName))
    }

    @Test
    fun ensurePojoFieldsMatchOnesInEntity() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery
        fun getAll(): IncorrectPojo
        """.trimIndent(), """
            data class IncorrectPojo(@ContentColumn("irrelevant_column")val irrelevantField: String)
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(pojoFieldNotInEntity("irrelevantField",
            "java.lang.String", "irrelevant_column",
            "androidx.contentaccess.compiler.processor.test.IncorrectPojo",
            entityName))
    }

    @Test
    fun ensureFieldsNotInProjectionButInConstructorAreNullable() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(projection = arrayOf("dtstart"))
        fun getAll(): CustomPojo
        """.trimIndent(), """
            data class CustomPojo(val dtstart: Long?, val nonNullableField: Long)
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(
            constructorFieldNotIncludedInProjectionNotNullable("nonNullableField",
                "androidx.contentaccess.compiler.processor.test.CustomPojo")
        )
    }

    @Test
    fun ensurePojoContainsAllProjectionFields() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(projection = arrayOf("dtstart", "description"))
        fun getAll(): PojoMissingField
        """.trimIndent(), """
            data class PojoMissingField(val dtstart: Long?)
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(
            columnInProjectionNotIncludedInReturnPojo("description",
                "androidx.contentaccess.compiler.processor.test.PojoMissingField")
        )
    }

    @Test
    fun ensureNullableEntityFieldsAreAlsoNullableInPojo() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery
        fun getAll(): PojoWithNonNullableField
        """.trimIndent(), """
            data class PojoWithNonNullableField(val dtstart: Long)
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(
            nullableEntityColumnNotNullableInPojo(
                "dtstart",
                "long",
                "dtstart",
                entityName
            )
        )
    }

    @Test
    fun ensureUriParameterProperlySpecified() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(uri = ":")
        fun getAll(): List<EntityWithUri>
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(columnOnlyAsUri())
    }

    @Test
    fun ensureUriParameterExists() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(uri = ":param")
        fun getAll(): List<EntityWithUri>
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(missingUriParameter("param"))
    }

    @Test
    fun ensureUriParameterIsString() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(uri = ":param")
        fun getAll(param: Long): List<EntityWithUri>
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(uriParameterIsNotString("param"))
    }
}