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
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomProcessor
import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.tools.JavaFileObject
import javax.tools.StandardLocation

@RunWith(JUnit4::class)
class RemoveUnusedColumnsTest {

    @Test
    fun noAnnotationGivesWarning() {
        compile()
            .withWarningCount(1)
            .withWarningContaining("The query returns some columns [uid, ageColumn]")
    }

    @Test
    fun annotateMethod() {
        compile(
            annotateMethod = true
        ).withWarningCount(0)
    }

    @Test
    fun annotateDao() {
        compile(
            annotateDao = true
        ).withWarningCount(0)
    }

    @Test
    fun annotateDb() {
        compile(
            annotateDb = true
        ).withWarningCount(0)
    }

    @Test
    fun expandProjection_annotateDb() {
        compile(
            annotateDb = true,
            enableExpandProjection = true
        ).withWarningCount(1)
            .withWarningContaining(ProcessorErrors.EXPAND_PROJECTION_ALONG_WITH_REMOVE_UNUSED)
    }

    @Test
    fun expandProjection_annotateMethod() {
        compile(
            annotateMethod = true,
            enableExpandProjection = true
        ).withWarningCount(1)
            .withWarningContaining(ProcessorErrors.EXPAND_PROJECTION_ALONG_WITH_REMOVE_UNUSED)
    }

    @Test
    fun expandProjection_annotateDao() {
        compile(
            annotateDao = true,
            enableExpandProjection = true
        ).withWarningCount(1)
            .withWarningContaining(ProcessorErrors.EXPAND_PROJECTION_ALONG_WITH_REMOVE_UNUSED)
    }

    private fun compile(
        annotateDb: Boolean = false,
        annotateDao: Boolean = false,
        annotateMethod: Boolean = false,
        enableExpandProjection: Boolean = false
    ): CompileTester.SuccessfulCompilationClause {
        val jfos = dao(
            annotateDao = annotateDao,
            annotateDb = annotateDb,
            annotateMethod = annotateMethod
        ) + COMMON.USER
        return assertAbout(JavaSourcesSubjectFactory.javaSources())
            .that(jfos)
            .withCompilerOptions("-Xlint:-processing") // remove unclaimed annotation warnings
            .also {
                if (enableExpandProjection) {
                    it.withCompilerOptions("-Aroom.expandProjection=true")
                }
            }
            .processedWith(RoomProcessor())
            .compilesWithoutError()
            .also {
                it.and()
                    .generatesFileNamed(
                        StandardLocation.CLASS_OUTPUT, "foo.bar", "MyDao_Impl.class"
                    )
                    .and()
                    .generatesFileNamed(
                        StandardLocation.CLASS_OUTPUT, "foo.bar", "MyDb_Impl.class"
                    )
            }
    }

    companion object {
        private fun dao(
            annotateDb: Boolean,
            annotateDao: Boolean,
            annotateMethod: Boolean
        ): List<JavaFileObject> {
            fun annotationText(enabled: Boolean) = if (enabled) {
                "@${RewriteQueriesToDropUnusedColumns::class.java.canonicalName}"
            } else {
                ""
            }

            val pojo = JavaFileObjects.forSourceString(
                "foo.bar.Pojo",
                """
                    package foo.bar;
                    public class Pojo {
                        public String name;
                        public String lastName;
                    }
                """.trimIndent()
            )
            val dao = JavaFileObjects.forSourceString(
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
            val db = JavaFileObjects.forSourceString(
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