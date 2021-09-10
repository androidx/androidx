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

package com.android.tools.build.jetifier.processor.transform

import com.android.tools.build.jetifier.processor.FileMapping
import com.android.tools.build.jetifier.processor.Processor
import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Test that verifies that *.kotlin_module file is correctly rewritten.
 */
class KotlinModuleRewriteTest {
    @Test
    fun topLevel() {
        val jars = originalJar(
            kotlin(
                "toplevel.kt",
                """
                    package androidx.fake.lib;
                    
                    fun topLevel(): Int = 10
                """
            )
        )
        testKotlinCompilation(
            jars,
            """
                import android.old.fake.topLevel

                fun hello() {
                    println(topLevel())
                }
            """
        )
    }

    @Test
    fun multifile() {
        val originalJar = originalJar(
            kotlin(
                "mutlifile1.kt",
                """
                @file:JvmName("Single")
                @file:JvmMultifileClass
                
                package androidx.fake.lib
                
                fun stringMethod(): String = "one"
            """
            ),
            kotlin(
                "mutlifile2.kt",
                """
                @file:JvmName("Single")
                @file:JvmMultifileClass
                
                package androidx.fake.lib
                
                fun intMethod(): Int = 5
            """
            )
        )
        testKotlinCompilation(
            originalJar,
            """
                import android.old.fake.stringMethod
                import android.old.fake.intMethod

                fun hello() {
                    println(stringMethod())
                    println(intMethod())
                }
            """
        )
    }
}

private fun testKotlinCompilation(originalJar: File, @Language("kotlin") content: String) {
    val processor = Processor.createProcessor4(KotlinTestConfig)
    val output = Files.createTempFile("out", ".jar").toFile()
    processor.transform2(setOf(FileMapping(originalJar, output)))

    val kotlinCompilation = KotlinCompilation()
    kotlinCompilation.sources = listOf(kotlin("test.kt", content))
    kotlinCompilation.classpaths = listOf(output)
    assertThat(kotlinCompilation.compile().exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
}

fun originalJar(vararg sources: SourceFile): File {
    val originalJar = Files.createTempFile("original", ".jar").toFile()
    val kotlinCompilation = KotlinCompilation()
    kotlinCompilation.kotlincArguments = listOf("-d", originalJar.absolutePath)
    kotlinCompilation.sources = sources.toList()
    kotlinCompilation.compile()
    assertThat(kotlinCompilation.compile().exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    return originalJar
}