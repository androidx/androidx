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

package androidx.room.writer

import androidx.room.DatabaseProcessingStep
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.processor.Context
import java.io.File
import loadTestSource
import org.jetbrains.kotlin.config.JvmDefaultMode

abstract class BaseDaoKotlinCodeGenTest {
    protected fun getTestGoldenPath(testName: String): String {
        return "kotlinCodeGen/$testName.kt"
    }

    protected fun runTest(
        sources: List<Source>,
        expectedFilePath: String,
        compiledFiles: List<File> = emptyList(),
        jvmDefaultMode: JvmDefaultMode = JvmDefaultMode.DEFAULT,
        handler: (XTestInvocation) -> Unit = { }
    ) {
        runKspTest(
            sources = sources,
            classpath = compiledFiles,
            options = mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "true"),
            kotlincArguments = listOf("-Xjvm-default=${jvmDefaultMode.description}")
        ) {
            val databaseFqn = "androidx.room.Database"
            DatabaseProcessingStep().process(
                it.processingEnv,
                mapOf(databaseFqn to it.roundEnv.getElementsAnnotatedWith(databaseFqn)),
                it.roundEnv.isProcessingOver
            )
            it.assertCompilationResult {
                this.generatedSource(
                    loadTestSource(
                        expectedFilePath,
                        "MyDao_Impl"
                    )
                )
                this.hasNoWarnings()
            }
            handler.invoke(it)
        }
    }
}
