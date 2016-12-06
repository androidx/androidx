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

package com.android.support.room.writer

import com.android.support.room.processor.DaoProcessor
import com.android.support.room.testing.TestProcessor
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaSourcesSubjectFactory
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

    fun singleDao(vararg jfo : JavaFileObject): CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(jfo.toList() + COMMON.USER)
                .processedWith(TestProcessor.builder()
                        .forAnnotations(com.android.support.room.Dao::class)
                        .nextRunHandler { invocation ->
                            val dao = invocation.roundEnv
                                    .getElementsAnnotatedWith(
                                            com.android.support.room.Dao::class.java)
                                    .first()
                            val parser = DaoProcessor(invocation.context)
                            val parsedDao = parser.parse(MoreElements.asType(dao))
                            DaoWriter(parsedDao).write(invocation.processingEnv)
                            true
                        }
                        .build())
    }
}
