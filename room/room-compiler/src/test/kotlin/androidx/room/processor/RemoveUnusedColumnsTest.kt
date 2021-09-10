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

package androidx.room.processor

import COMMON
import androidx.room.DatabaseProcessingStep
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.compiler.processing.util.CompilationResultSubject
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runProcessorTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoveUnusedColumnsTest {

    @Test
    fun noAnnotationGivesWarning() {
        compile { result ->
            result.hasWarningContaining("The query returns some columns [uid, ageColumn]")
            result.hasWarningCount(1)
        }
    }

    @Test
    fun annotateMethod() {
        compile(
            annotateMethod = true
        ) { result ->
            result.hasNoWarnings()
        }
    }

    @Test
    fun annotateDao() {
        compile(
            annotateDao = true
        ) { result ->
            result.hasNoWarnings()
        }
    }

    @Test
    fun annotateDb() {
        compile(
            annotateDb = true
        ) { result ->
            result.hasNoWarnings()
        }
    }

    @Test
    fun expandProjection_annotateDb() {
        compile(
            annotateDb = true,
            enableExpandProjection = true
        ) { result ->
            result.hasWarningContaining(ProcessorErrors.EXPAND_PROJECTION_ALONG_WITH_REMOVE_UNUSED)
            result.hasWarningCount(1)
        }
    }

    @Test
    fun expandProjection_annotateMethod() {
        compile(
            annotateMethod = true,
            enableExpandProjection = true
        ) { result ->
            result.hasWarningContaining(ProcessorErrors.EXPAND_PROJECTION_ALONG_WITH_REMOVE_UNUSED)
            result.hasWarningCount(1)
        }
    }

    @Test
    fun expandProjection_annotateDao() {
        compile(
            annotateDao = true,
            enableExpandProjection = true
        ) { result ->
            result.hasWarningContaining(
                ProcessorErrors.EXPAND_PROJECTION_ALONG_WITH_REMOVE_UNUSED
            )
            result.hasWarningCount(1)
        }
    }

    private fun compile(
        annotateDb: Boolean = false,
        annotateDao: Boolean = false,
        annotateMethod: Boolean = false,
        enableExpandProjection: Boolean = false,
        validate: (CompilationResultSubject) -> Unit,
    ) {
        val sources = dao(
            annotateDao = annotateDao,
            annotateDb = annotateDb,
            annotateMethod = annotateMethod
        ) + COMMON.USER

        runProcessorTest(
            sources = sources,
            createProcessingStep = {
                DatabaseProcessingStep()
            },
            options = mapOf(
                "room.expandProjection" to enableExpandProjection.toString()
            )
        ) { result ->
            validate(result)
            result.generatedSourceFileWithPath(
                "foo/bar/MyDao_Impl.java"
            )
            result.generatedSourceFileWithPath(
                "foo/bar/MyDb_Impl.java"
            )
        }
    }

    companion object {
        private fun dao(
            annotateDb: Boolean,
            annotateDao: Boolean,
            annotateMethod: Boolean
        ): List<Source> {
            fun annotationText(enabled: Boolean) = if (enabled) {
                "@${RewriteQueriesToDropUnusedColumns::class.java.canonicalName}"
            } else {
                ""
            }

            val pojo = Source.java(
                "foo.bar.Pojo",
                """
                    package foo.bar;
                    public class Pojo {
                        public String name;
                        public String lastName;
                    }
                """.trimIndent()
            )
            val dao = Source.java(
                "foo.bar.MyDao",
                """
                    package foo.bar;
                    import androidx.room.*;
                    @Dao
                    ${annotationText(annotateDao)}
                    public interface MyDao {
                        ${annotationText(annotateMethod)}
                        @Query("SELECT * FROM User")
                        public java.util.List<Pojo> loadAll();
                    }
                """.trimIndent()
            )
            val db = Source.java(
                "foo.bar.MyDb",
                """
                    package foo.bar;
                    import androidx.room.*;
                    @Database(
                        entities = {User.class},
                        version = 1,
                        exportSchema = false
                    )
                    ${annotationText(annotateDb)}
                    abstract class MyDb extends RoomDatabase {
                        abstract public MyDao getDao();
                    }
                """.trimIndent()
            )
            return listOf(pojo, dao, db)
        }
    }
}