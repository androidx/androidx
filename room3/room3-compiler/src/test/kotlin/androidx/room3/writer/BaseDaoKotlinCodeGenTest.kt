/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.room3.DatabaseProcessingStep
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.XTestInvocation
import androidx.room3.compiler.processing.util.runKspTest
import java.io.File
import loadTestSource
import writeTestSource

abstract class BaseDaoKotlinCodeGenTest {
    protected fun getTestGoldenPath(testName: String): String {
        return "kotlinCodeGen/$testName.kt"
    }

    protected fun runTest(
        sources: List<Source>,
        expectedFilePath: String,
        compiledFiles: List<File> = emptyList(),
        jvmDefaultMode: String = "disable",
        handler: (XTestInvocation) -> Unit = {},
    ) {
        val kotlincArguments = listOf("-jvm-target=11", "-jvm-default=${jvmDefaultMode}")
        val invocationHandler: (XTestInvocation) -> Unit = {
            val databaseFqn = "androidx.room3.Database"
            DatabaseProcessingStep()
                .process(
                    it.processingEnv,
                    mapOf(databaseFqn to it.roundEnv.getElementsAnnotatedWith(databaseFqn)),
                    it.roundEnv.isProcessingOver,
                )
            it.assertCompilationResult {
                val expectedSrc = loadTestSource(expectedFilePath, "MyDao_Impl")
                // Set ROOM_TEST_WRITE_SRCS env variable to make tests write expected sources,
                // handy for big sweeping code gen changes. ;)
                if (System.getenv("ROOM_TEST_WRITE_SRCS") != null) {
                    this.findGeneratedSource(expectedSrc.relativePath)?.let { expectedSrc ->
                        writeTestSource(source = expectedSrc, fileName = expectedFilePath)
                    }
                }
                this.generatedSource(expectedSrc)
                this.hasNoWarnings()
            }
            handler.invoke(it)
        }
        runKspTest(
            sources = sources,
            classpath = compiledFiles,
            kotlincArguments = kotlincArguments,
            handler = invocationHandler,
        )
    }
}
