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
import androidx.room.ext.RoomTypeNames
import androidx.room.processor.DaoProcessor
import androidx.room.testing.TestProcessor
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaSourcesSubjectFactory
import createVerifierFromEntities
import loadJavaCode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.tools.JavaFileObject

@RunWith(JUnit4::class)
class DaoWriterTest {
    @Test
    fun complexDao() {
        singleDao(
                loadJavaCode("databasewriter/input/ComplexDatabase.java",
                        "foo.bar.ComplexDatabase"),
                loadJavaCode("daoWriter/input/ComplexDao.java", "foo.bar.ComplexDao")
        ).compilesWithoutError().and().generatesSources(
                loadJavaCode("daoWriter/output/ComplexDao.java", "foo.bar.ComplexDao_Impl")
        )
    }

    @Test
    fun writerDao() {
        singleDao(
                loadJavaCode("daoWriter/input/WriterDao.java", "foo.bar.WriterDao")
        ).compilesWithoutError().and().generatesSources(
                loadJavaCode("daoWriter/output/WriterDao.java", "foo.bar.WriterDao_Impl")
        )
    }

    @Test
    fun deletionDao() {
        singleDao(
                loadJavaCode("daoWriter/input/DeletionDao.java", "foo.bar.DeletionDao")
        ).compilesWithoutError().and().generatesSources(
                loadJavaCode("daoWriter/output/DeletionDao.java", "foo.bar.DeletionDao_Impl")
        )
    }

    @Test
    fun updateDao() {
        singleDao(
                loadJavaCode("daoWriter/input/UpdateDao.java", "foo.bar.UpdateDao")
        ).compilesWithoutError().and().generatesSources(
                loadJavaCode("daoWriter/output/UpdateDao.java", "foo.bar.UpdateDao_Impl")
        )
    }

    fun singleDao(vararg jfo: JavaFileObject): CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(jfo.toList() + COMMON.USER + COMMON.MULTI_PKEY_ENTITY + COMMON.BOOK +
                        COMMON.LIVE_DATA + COMMON.COMPUTABLE_LIVE_DATA)
                .processedWith(TestProcessor.builder()
                        .forAnnotations(androidx.room.Dao::class)
                        .nextRunHandler { invocation ->
                            val dao = invocation.roundEnv
                                    .getElementsAnnotatedWith(
                                            androidx.room.Dao::class.java)
                                    .first()
                            val db = invocation.roundEnv
                                    .getElementsAnnotatedWith(
                                            androidx.room.Database::class.java)
                                    .firstOrNull()
                            val dbType = MoreTypes.asDeclared(if (db != null) {
                                db.asType()
                            } else {
                                invocation.context.processingEnv.elementUtils
                                        .getTypeElement(RoomTypeNames.ROOM_DB.toString()).asType()
                            })
                            val parser = DaoProcessor(
                                    baseContext = invocation.context,
                                    element = MoreElements.asType(dao),
                                    dbType = dbType,
                                    dbVerifier = createVerifierFromEntities(invocation))
                            val parsedDao = parser.process()
                            DaoWriter(parsedDao, invocation.processingEnv)
                                    .write(invocation.processingEnv)
                            true
                        }
                        .build())
    }
}
