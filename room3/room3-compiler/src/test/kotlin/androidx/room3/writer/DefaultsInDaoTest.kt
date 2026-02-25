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

package androidx.room3.writer

import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.processing.XProcessingEnv
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.runKspTest
import androidx.room3.ext.RoomTypeNames.ROOM_DB
import androidx.room3.processor.DaoProcessor
import androidx.room3.testing.context
import com.google.common.truth.StringSubject
import createVerifierFromEntitiesAndViews
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Tests that we generate the right calls for default method implementations.
 *
 * The assertions in these tests are more for visual inspection. The real test is that this code
 * compiles properly because when we generate the wrong code, it won't compile :).
 *
 * For Java default method tests, we have DefaultDaoMethodsTest in TestApp.
 */
@RunWith(Parameterized::class)
class DefaultsInDaoTest(private val jvmDefaultMode: String) {
    @Test
    fun abstractDao() {
        val defaultWithCompatibilityAnnotation =
            if (jvmDefaultMode == "enable") {
                "@JvmDefaultWithoutCompatibility"
            } else {
                ""
            }

        val source =
            Source.kotlin(
                "Foo.kt",
                """
            import androidx.room3.*
            class User
            interface BaseDao<T> {
                @Transaction
                fun upsert(obj: T) {
                    TODO("")
                }
            }

            $defaultWithCompatibilityAnnotation
            @Dao
            abstract class SubjectDao : BaseDao<User>
            """
                    .trimIndent(),
            )
        compileInEachDefaultsMode(source) { generated ->
            generated.contains("public override fun upsert(obj: User): Unit")
            generated.contains("super@SubjectDao_Impl.upsert(obj)")
            generated.doesNotContain("SubjectDao_Impl.super.upsert(")
            generated.doesNotContain("SubjectDao.super.upsert")
            generated.doesNotContain("this.upsert")
        }
    }

    @Test
    fun interfaceDao() {
        val source =
            Source.kotlin(
                "Foo.kt",
                """
                import androidx.room3.*
                class User
                interface BaseDao<T> {
                    @Transaction
                    fun upsert(obj: T) {
                        TODO("")
                    }
                }

                @Dao
                interface SubjectDao : BaseDao<User>
                """
                    .trimIndent(),
            )
        compileInEachDefaultsMode(source) { generated ->
            generated.contains("public override fun upsert(obj: User): Unit")
            generated.contains("super@SubjectDao_Impl.upsert(obj)")
            generated.doesNotContain("SubjectDao.DefaultImpls.upsert(SubjectDao_Impl.this")
            generated.doesNotContain("SubjectDao.super.upsert(")
            generated.doesNotContain("SubjectDao_Impl.super.upsert")
            generated.doesNotContain("this.upsert")
        }
    }

    @Test
    fun interfaceDao_suspend() {
        val source =
            Source.kotlin(
                "Foo.kt",
                """
                import androidx.room3.*
                class User
                interface BaseDao<T> {
                    @Transaction
                    suspend fun upsert(obj: T) {
                        TODO("")
                    }
                }

                @Dao
                interface SubjectDao : BaseDao<User>
                """
                    .trimIndent(),
            )
        compileInEachDefaultsMode(source) { generated ->
            generated.contains("public override suspend fun upsert(obj: User): Unit")
            generated.contains("super@SubjectDao_Impl.upsert(obj)")
            generated.doesNotContain("SubjectDao.DefaultImpls.upsert(SubjectDao_Impl.this")
            generated.doesNotContain("SubjectDao.super.upsert(")
            generated.doesNotContain("SubjectDao_Impl.super.upsert")
            generated.doesNotContain("this.upsert")
        }
    }

    @Test
    fun interfaceDao_private() {
        val source =
            Source.kotlin(
                "Foo.kt",
                """
                import androidx.room3.*
                @Dao
                interface SubjectDao {
                    private fun upsert() {
                        TODO("")
                    }

                    private suspend fun suspendUpsert() {
                        TODO("")
                    }
                }
                """
                    .trimIndent(),
            )
        compileInEachDefaultsMode(
            source = source,
            jvmTarget = "11", // private functions in interface require target jvm 9+
        ) {}
    }

    private fun compileInEachDefaultsMode(
        source: Source,
        jvmTarget: String = "1.8",
        handler: (StringSubject) -> Unit,
    ) {
        runKspTest(
            sources = listOf(source),
            javacArguments = listOf("-source", jvmTarget),
            kotlincArguments = listOf("-jvm-target=$jvmTarget", "-jvm-default=${jvmDefaultMode}"),
        ) { invocation ->
            invocation.roundEnv
                .getElementsAnnotatedWith(androidx.room3.Dao::class.qualifiedName!!)
                .filterIsInstance<XTypeElement>()
                .forEach { dao ->
                    val db = invocation.context.processingEnv.requireTypeElement(ROOM_DB)
                    val dbType = db.type
                    val parser =
                        DaoProcessor(
                            baseContext = invocation.context,
                            element = dao,
                            dbType = dbType,
                            dbVerifier = createVerifierFromEntitiesAndViews(invocation),
                        )
                    val parsedDao = parser.process()
                    DaoWriter(
                            dao = parsedDao,
                            dbElement = db,
                            writerContext =
                                TypeWriter.WriterContext(
                                    codeLanguage = CodeLanguage.KOTLIN,
                                    javaLambdaSyntaxAvailable = true,
                                    targetPlatforms = setOf(XProcessingEnv.Platform.JVM),
                                ),
                        )
                        .write(invocation.processingEnv)
                    invocation.assertCompilationResult {
                        val relativePath = parsedDao.implTypeName.canonicalName + ".kt"
                        handler(generatedSourceFileWithPath(relativePath))
                    }
                }
        }
    }

    companion object {
        @JvmStatic
        @Parameters(name = "jvmDefaultMode={0}")
        fun modes() = listOf("enable", "no-compatibility", "disable")
    }
}
