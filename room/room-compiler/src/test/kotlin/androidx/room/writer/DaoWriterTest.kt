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
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.ext.RoomTypeNames.ROOM_DB
import androidx.room.processor.DaoProcessor
import androidx.room.testing.context
import createVerifierFromEntitiesAndViews
import java.util.Locale
import loadTestSource
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DaoWriterTest {
    @Test
    fun complexDao() {
        singleDao(
            loadTestSource(
                fileName = "databasewriter/input/ComplexDatabase.java",
                qName = "foo.bar.ComplexDatabase"
            ),
            loadTestSource(
                fileName = "daoWriter/input/ComplexDao.java",
                qName = "foo.bar.ComplexDao"
            )
        ) {
            val backendFolder = backendFolder(it)
            it.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        fileName = "daoWriter/output/$backendFolder/ComplexDao.java",
                        qName = "foo.bar.ComplexDao_Impl"
                    )
                )
            }
        }
    }

    @Test
    fun complexDao_turkishLocale() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr")) // Turkish has special upper/lowercase i chars
            complexDao()
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun writerDao() {
        singleDao(
            loadTestSource(
                fileName = "daoWriter/input/WriterDao.java",
                qName = "foo.bar.WriterDao"
            )
        ) {
            val backendFolder = backendFolder(it)
            it.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        fileName = "daoWriter/output/$backendFolder/WriterDao.java",
                        qName = "foo.bar.WriterDao_Impl"
                    )
                )
            }
        }
    }

    @Test
    fun deletionDao() {
        singleDao(
            loadTestSource(
                fileName = "daoWriter/input/DeletionDao.java",
                qName = "foo.bar.DeletionDao"
            )
        ) {
            val backendFolder = backendFolder(it)
            it.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        fileName = "daoWriter/output/$backendFolder/DeletionDao.java",
                        qName = "foo.bar.DeletionDao_Impl"
                    )
                )
            }
        }
    }

    @Test
    fun updateDao() {
        singleDao(
            loadTestSource(
                fileName = "daoWriter/input/UpdateDao.java",
                qName = "foo.bar.UpdateDao"
            )
        ) {
            val backendFolder = backendFolder(it)
            it.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        fileName = "daoWriter/output/$backendFolder/UpdateDao.java",
                        qName = "foo.bar.UpdateDao_Impl"
                    )
                )
            }
        }
    }

    @Test
    fun upsertDao() {
        singleDao(
            loadTestSource(
                fileName = "daoWriter/input/UpsertDao.java",
                qName = "foo.bar.UpsertDao"
            )
        ) {
            val backendFolder = backendFolder(it)
            it.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        fileName = "daoWriter/output/$backendFolder/UpsertDao.java",
                        qName = "foo.bar.UpsertDao_Impl"
                    )
                )
            }
        }
    }

    private fun singleDao(
        vararg inputs: Source,
        handler: (XTestInvocation) -> Unit
    ) {
        val sources = listOf(
            COMMON.USER, COMMON.MULTI_PKEY_ENTITY, COMMON.BOOK,
            COMMON.LIVE_DATA, COMMON.COMPUTABLE_LIVE_DATA, COMMON.RX2_SINGLE,
            COMMON.RX2_MAYBE, COMMON.RX2_COMPLETABLE, COMMON.USER_SUMMARY,
            COMMON.RX2_ROOM, COMMON.PARENT, COMMON.CHILD1, COMMON.CHILD2,
            COMMON.INFO, COMMON.LISTENABLE_FUTURE, COMMON.GUAVA_ROOM,
            COMMON.RX2_FLOWABLE, COMMON.RX3_FLOWABLE, COMMON.RX2_OBSERVABLE,
            COMMON.RX3_OBSERVABLE, COMMON.PUBLISHER
        ) + inputs
        runProcessorTest(
            sources = sources
        ) { invocation ->
            val dao = invocation.roundEnv
                .getElementsAnnotatedWith(
                    androidx.room.Dao::class.qualifiedName!!
                ).filterIsInstance<XTypeElement>().firstOrNull()
            if (dao != null) {
                val db = invocation.roundEnv
                    .getElementsAnnotatedWith(
                        androidx.room.Database::class.qualifiedName!!
                    ).filterIsInstance<XTypeElement>().firstOrNull()
                    ?: invocation.context.processingEnv
                        .requireTypeElement(ROOM_DB)
                val dbType = db.type
                val dbVerifier = createVerifierFromEntitiesAndViews(invocation)
                invocation.context.attachDatabaseVerifier(dbVerifier)
                val parser = DaoProcessor(
                    baseContext = invocation.context,
                    element = dao,
                    dbType = dbType,
                    dbVerifier = dbVerifier
                )
                val parsedDao = parser.process()
                DaoWriter(parsedDao, db, CodeLanguage.JAVA)
                    .write(invocation.processingEnv)
            }
            // we could call handler inside the if block but if something happens and we cannot
            // find the dao, test will never assert on generated code.
            handler(invocation)
        }
    }

    private fun backendFolder(invocation: XTestInvocation) =
        invocation.processingEnv.backend.name.lowercase()
}
