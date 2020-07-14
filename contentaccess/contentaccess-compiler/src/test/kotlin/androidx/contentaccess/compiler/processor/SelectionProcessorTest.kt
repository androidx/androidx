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
import androidx.contentaccess.compiler.processor.findWordsStartingWithColumn
import androidx.contentaccess.compiler.processor.selectionParameterNotInMethodParameters
import androidx.contentaccess.compiler.processor.strayColumnInSelectionErrorMessage
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SelectionProcessorTest {

    val entityName = "androidx.contentaccess.compiler.processor.test.Entity"

    fun generateMainSourceFile(accessorBody: String): SourceFile {
        return SourceFile.kotlin("MyClass.kt", """
        package androidx.contentaccess.compiler.processor.test

        import androidx.contentaccess.ContentAccessObject
        import androidx.contentaccess.ContentPrimaryKey
        import androidx.contentaccess.ContentColumn
        import androidx.contentaccess.ContentQuery
        import androidx.contentaccess.ContentEntity

        @ContentEntity("uri")
        data class Entity(
            @ContentPrimaryKey("_id")
            val id: Long,
            @ContentColumn("dtstart")
            val startTime: Long?,
            @ContentColumn("description")
            val description: String?
        )

        @ContentAccessObject(Entity::class)
        interface ContentAccessor {
            $accessorBody
        }
        """
        )
    }

    @Test
    fun validSelection() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(selection = "dtstart > 1 OR (dtstart < 5 AND description =  \"abc\")" + 
            " OR ((description = :parameter AND (dtstart * 6 = 30)))")
        fun getAll(parameter: String): List<Entity>
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.OK)
    }

    @Test
    fun ensureNoStraySelectionColumns() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(selection = "dtstart = : AND description > :parameter")
        fun getAll(parameter: String): List<Entity>
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(strayColumnInSelectionErrorMessage())
    }

    @Test
    fun ensureSelectionArgumentsAreSpecifiedInParameter() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(selection = "description > :parameter")
        fun getAll(): List<Entity>
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(selectionParameterNotInMethodParameters("parameter"))
    }

    @Test
    fun ensureColumnsMentionedInSelectionExistInEntity() {
        val sourceFile = generateMainSourceFile("""
        @ContentQuery(selection = "unknown_column > 5")
        fun getAll(): List<Entity>
        """.trimIndent())

        val result = runCompilation(listOf(sourceFile))

        assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
        assertThat(result.messages).contains(
            columnInSelectionMissingFromEntity("unknown_column", entityName))
    }

    @Test
    fun findsWordsStartingWithColumnInSelectionProperly() {
        val selection1 = "a = :a and b = :b"
        val wordsStartingWithColumnFromSelection1 = listOf(":a", ":b")
        assertThat(findWordsStartingWithColumn(selection1))
            .containsExactlyElementsOf(wordsStartingWithColumnFromSelection1)

        val selection2 = "a=:a and b=:b"
        val wordsStartingWithColumnFromSelection2 = listOf(":a", ":b")
        assertThat(findWordsStartingWithColumn(selection2))
            .containsExactlyElementsOf(wordsStartingWithColumnFromSelection2)

        val selection3 = "uri!=:uriParam"
        assertThat(findWordsStartingWithColumn(selection3)).containsOnly(":uriParam")

        val selection4 = "col = 1 and col2 = 2"
        assertThat(findWordsStartingWithColumn(selection4)).isEmpty()

        val selection5 = "a = :"
        assertThat(findWordsStartingWithColumn(selection5)).containsOnly(":")
    }
}