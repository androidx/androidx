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
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.ext.RoomTypeNames
import androidx.room.processor.DaoProcessor
import androidx.room.testing.context
import createVerifierFromEntitiesAndViews
import loadTestSource
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Locale

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
            it.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        fileName = "daoWriter/output/ComplexDao.java",
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
            it.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        fileName = "daoWriter/output/WriterDao.java",
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
            it.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        fileName = "daoWriter/output/DeletionDao.java",
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
            it.assertCompilationResult {
                generatedSource(
                    loadTestSource(
                        fileName = "daoWriter/output/UpdateDao.java",
                        qName = "foo.bar.UpdateDao_Impl"
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
            COMMON.INFO, COMMON.LISTENABLE_FUTURE, COMMON.GUAVA_ROOM
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
            }
            // we could call handler inside the if block but if something happens and we cannot
            // find the dao, test will never assert on generated code.
            handler(invocation)
        }
    }
}
