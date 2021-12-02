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

package androidx.room.writer

import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runKaptTest
import androidx.room.ext.RoomTypeNames
import androidx.room.processor.DaoProcessor
import androidx.room.testing.context
import com.google.common.truth.StringSubject
import createVerifierFromEntitiesAndViews
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Tests that we generate the right calls for default method implementations.
 *
 * The assertions in these tests are more for visual inspection. The real test is that this code
 * compiles properly because when we generate the wrong code, it won't compile :).
 *
 * For Java default method tests, we have DefaultDaoMethodsTest in TestApp.
 */
@RunWith(JUnit4::class)
class DefaultsInDaoTest {
    @Test
    fun abstractDao() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.*
            class User
            interface BaseDao<T> {
                @Transaction
                fun upsert(obj: T) {
                    TODO("")
                }
            }

            @Dao
            abstract class SubjectDao : BaseDao<User>
            """.trimIndent()
        )
        compileInEachDefaultsMode(source) { _, generated ->
            generated.contains("public void upsert(final User obj)")
            generated.contains("SubjectDao_Impl.super.upsert(")
            generated.doesNotContain("SubjectDao.super.upsert")
            generated.doesNotContain("this.upsert")
        }
    }

    @Test
    fun interfaceDao() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.*
            class User
            interface BaseDao<T> {
                @Transaction
                fun upsert(obj: T) {
                    TODO("")
                }
            }

            @Dao
            interface SubjectDao : BaseDao<User>
            """.trimIndent()
        )
        compileInEachDefaultsMode(source) { mode, generated ->
            generated.contains("public void upsert(final User obj)")
            if (mode == JvmDefaultMode.ALL_INCOMPATIBLE) {
                generated.contains("SubjectDao.super.upsert(")
            } else {
                generated.contains("SubjectDao.DefaultImpls.upsert(SubjectDao_Impl.this")
            }

            generated.doesNotContain("SubjectDao_Impl.super.upsert")
            generated.doesNotContain("this.upsert")
        }
    }

    private fun compileInEachDefaultsMode(
        source: Source,
        handler: (JvmDefaultMode, StringSubject) -> Unit
    ) {
        listOf(
            JvmDefaultMode.ENABLE,
            JvmDefaultMode.ENABLE_WITH_DEFAULT_IMPLS,
            JvmDefaultMode.ALL_INCOMPATIBLE
        ).forEach { jvmDefaultMode ->
            // TODO should run these with KSP as well. https://github.com/google/ksp/issues/627
            runKaptTest(
                sources = listOf(source),
                kotlincArguments = listOf("-Xjvm-default=${jvmDefaultMode.description}")
            ) { invocation ->
                invocation.roundEnv
                    .getElementsAnnotatedWith(
                        androidx.room.Dao::class.qualifiedName!!
                    ).filterIsInstance<XTypeElement>()
                    .forEach { dao ->
                        val db = invocation.context.processingEnv
                            .requireTypeElement(RoomTypeNames.ROOM_DB)
                        val dbType = db.type
                        val parser = DaoProcessor(
                            baseContext = invocation.context,
                            element = dao,
                            dbType = dbType,
                            dbVerifier = createVerifierFromEntitiesAndViews(invocation)
                        )
                        val parsedDao = parser.process()
                        DaoWriter(parsedDao, db, invocation.processingEnv)
                            .write(invocation.processingEnv)
                        invocation.assertCompilationResult {
                            val relativePath = parsedDao.implTypeName.simpleName() + ".java"
                            handler(jvmDefaultMode, generatedSourceFileWithPath(relativePath))
                        }
                    }
            }
        }
    }
}