/*
 * Copyright (C) 2016 The Android Open Source Project
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

import COMMON
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.ext.RoomTypeNames.ROOM_DB
import androidx.room.processor.DaoProcessor
import androidx.room.runProcessorTestWithK1
import androidx.room.testing.context
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import createVerifierFromEntitiesAndViews
import java.util.Locale
import loadTestSource
import org.junit.Test
import org.junit.runner.RunWith
import writeTestSource

@RunWith(TestParameterInjector::class)
class DaoWriterTest {
    @Test
    fun complexDao(@TestParameter javaLambdaSyntaxAvailable: Boolean) {
        singleDao(
            loadTestSource(
                fileName = "databasewriter/input/ComplexDatabase.java",
                qName = "foo.bar.ComplexDatabase"
            ),
            loadTestSource(
                fileName = "daoWriter/input/ComplexDao.java",
                qName = "foo.bar.ComplexDao"
            ),
            javaLambdaSyntaxAvailable = javaLambdaSyntaxAvailable,
            outputFileName = "ComplexDao.java"
        )
    }

    @Test
    fun complexDao_turkishLocale() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr")) // Turkish has special upper/lowercase i chars
            complexDao(false)
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun writerDao(@TestParameter javaLambdaSyntaxAvailable: Boolean) {
        singleDao(
            loadTestSource(
                fileName = "daoWriter/input/WriterDao.java",
                qName = "foo.bar.WriterDao"
            ),
            javaLambdaSyntaxAvailable = javaLambdaSyntaxAvailable,
            outputFileName = "WriterDao.java"
        )
    }

    @Test
    fun deletionDao(@TestParameter javaLambdaSyntaxAvailable: Boolean) {
        singleDao(
            loadTestSource(
                fileName = "daoWriter/input/DeletionDao.java",
                qName = "foo.bar.DeletionDao"
            ),
            javaLambdaSyntaxAvailable = javaLambdaSyntaxAvailable,
            outputFileName = "DeletionDao.java"
        )
    }

    @Test
    fun updateDao(@TestParameter javaLambdaSyntaxAvailable: Boolean) {
        singleDao(
            loadTestSource(
                fileName = "daoWriter/input/UpdateDao.java",
                qName = "foo.bar.UpdateDao"
            ),
            javaLambdaSyntaxAvailable = javaLambdaSyntaxAvailable,
            outputFileName = "UpdateDao.java"
        )
    }

    @Test
    fun upsertDao(@TestParameter javaLambdaSyntaxAvailable: Boolean) {
        singleDao(
            loadTestSource(
                fileName = "daoWriter/input/UpsertDao.java",
                qName = "foo.bar.UpsertDao"
            ),
            javaLambdaSyntaxAvailable = javaLambdaSyntaxAvailable,
            outputFileName = "UpsertDao.java"
        )
    }

    private fun singleDao(
        vararg inputs: Source,
        javaLambdaSyntaxAvailable: Boolean = false,
        outputFileName: String,
        handler: (XTestInvocation) -> Unit = {}
    ) {
        val sources =
            listOf(
                COMMON.USER,
                COMMON.MULTI_PKEY_ENTITY,
                COMMON.BOOK,
                COMMON.USER_SUMMARY,
                COMMON.PARENT,
                COMMON.CHILD1,
                COMMON.CHILD2,
                COMMON.INFO,
            ) + inputs
        val libs =
            compileFiles(
                listOf(
                    COMMON.GUAVA_ROOM,
                    COMMON.LIVE_DATA,
                    COMMON.COMPUTABLE_LIVE_DATA,
                    COMMON.RX2_SINGLE,
                    COMMON.RX2_MAYBE,
                    COMMON.RX2_COMPLETABLE,
                    COMMON.LISTENABLE_FUTURE,
                    COMMON.RX2_ROOM,
                    COMMON.RX2_FLOWABLE,
                    COMMON.RX3_FLOWABLE,
                    COMMON.RX2_OBSERVABLE,
                    COMMON.RX3_OBSERVABLE,
                    COMMON.PUBLISHER,
                    COMMON.PAGING_SOURCE,
                    COMMON.LIMIT_OFFSET_PAGING_SOURCE
                )
            )
        runProcessorTestWithK1(sources = sources, classpath = libs) { invocation ->
            if (invocation.isKsp && !javaLambdaSyntaxAvailable) {
                // Skip KSP backend without lambda syntax, it is a nonsensical combination.
                return@runProcessorTestWithK1
            }
            val dao =
                invocation.roundEnv
                    .getElementsAnnotatedWith(androidx.room.Dao::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .firstOrNull()
            if (dao != null) {
                val db =
                    invocation.roundEnv
                        .getElementsAnnotatedWith(androidx.room.Database::class.qualifiedName!!)
                        .filterIsInstance<XTypeElement>()
                        .firstOrNull()
                        ?: invocation.context.processingEnv.requireTypeElement(ROOM_DB)
                val dbType = db.type
                val dbVerifier = createVerifierFromEntitiesAndViews(invocation)
                invocation.context.attachDatabaseVerifier(dbVerifier)
                val parser =
                    DaoProcessor(
                        baseContext = invocation.context,
                        element = dao,
                        dbType = dbType,
                        dbVerifier = dbVerifier
                    )
                val parsedDao = parser.process()
                DaoWriter(
                        dao = parsedDao,
                        dbElement = db,
                        writerContext =
                            TypeWriter.WriterContext(
                                codeLanguage = CodeLanguage.JAVA,
                                javaLambdaSyntaxAvailable = javaLambdaSyntaxAvailable,
                                targetPlatforms = setOf(XProcessingEnv.Platform.JVM)
                            )
                    )
                    .write(invocation.processingEnv)
                val outputSubFolder = outputFolder(invocation, javaLambdaSyntaxAvailable)
                invocation.assertCompilationResult {
                    val expectedFilePath = "daoWriter/output/$outputSubFolder/$outputFileName"
                    val expectedSrc =
                        loadTestSource(
                            fileName = expectedFilePath,
                            qName = parsedDao.implTypeName.canonicalName
                        )
                    // Set ROOM_TEST_WRITE_SRCS env variable to make tests write expected sources,
                    // handy for big sweeping code gen changes. ;)
                    if (System.getenv("ROOM_TEST_WRITE_SRCS") != null) {
                        writeTestSource(
                            checkNotNull(this.findGeneratedSource(expectedSrc.relativePath)) {
                                "Couldn't find gen src: $expectedSrc"
                            },
                            expectedFilePath
                        )
                    }
                    generatedSource(expectedSrc)
                }
            }
            handler(invocation)
        }
    }

    private fun outputFolder(
        invocation: XTestInvocation,
        javaLambdaSyntaxAvailable: Boolean
    ): String {
        val backendFolder = invocation.processingEnv.backend.name.lowercase()
        val lambdaFolder = if (javaLambdaSyntaxAvailable) "withLambda" else "withoutLambda"
        if (invocation.isKsp) {
            return backendFolder
        } else {
            return "$backendFolder/$lambdaFolder"
        }
    }
}
