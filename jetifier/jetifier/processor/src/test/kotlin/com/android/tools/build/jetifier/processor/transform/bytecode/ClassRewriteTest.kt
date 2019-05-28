/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.bytecode

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.config.ConfigParser
import com.android.tools.build.jetifier.core.rule.RewriteRule
import com.android.tools.build.jetifier.core.rule.RewriteRulesMap
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.core.type.TypesMap
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.google.common.truth.Truth
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceClassVisitor
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.Paths

/**
 * Tests that individual references in the class file were correctly rewritten.
 */
class ClassRewriteTest {

    @Test
    fun testClassRewrite_replacesAllReferences() {
        val config = Config.fromOptional(
            restrictToPackagePrefixes = setOf("android/support"),
            reversedRestrictToPackagesPrefixes = setOf("androidx"),
            typesMap = TypesMap(mapOf(
                "android/support/v4/app/Fragment"
                    to "androidx/fragment/app/Fragment"
            ).map { JavaType(it.key) to JavaType(it.value) }.toMap())
        )

        val inputClassPath = "/classRewriteTest/FragmentKt.class"
        val inputFile = File(javaClass.getResource(inputClassPath).file)
        val archiveFile = ArchiveFile(Paths.get("/", "FragmentKt.class"), inputFile.readBytes())

        val context = TransformationContext(config = config)
        val transformer = ByteCodeTransformer(context)

        transformer.runTransform(archiveFile)

        val decompiledResult = decompileClassFileToString(archiveFile.data)

        Truth.assertThat(decompiledResult).doesNotContain("androidx.fragment.app.Fragment")
        Truth.assertThat(decompiledResult).contains("androidx/fragment/app/Fragment")
        Truth.assertThat(decompiledResult).contains("Landroidx/fragment/app/Fragment;")
        Truth.assertThat(decompiledResult).doesNotContain("LL")
        Truth.assertThat(decompiledResult).doesNotContain(";;")
    }

    @Test
    fun testClassRewrite_replacesAllReferences2() {
        val config = Config.fromOptional(
            restrictToPackagePrefixes = setOf("android/support"),
            reversedRestrictToPackagesPrefixes = setOf("androidx"),
            typesMap = TypesMap(mapOf(
                "android/support/design/widget/Snackbar"
                    to "com/google/android/material/snackbar/Snackbar"
            ).map { JavaType(it.key) to JavaType(it.value) }.toMap()),
            rulesMap = RewriteRulesMap(RewriteRule(
                from = "android/support/annotation/(.*)",
                to = "androidx/annotation/()"))
        )

        val inputClassPath = "/classRewriteTest/RxSnackbarKt.class"
        val inputFile = File(javaClass.getResource(inputClassPath).file)
        val archiveFile = ArchiveFile(Paths.get("/", "RxSnackbarKt.class"), inputFile.readBytes())

        val context = TransformationContext(config = config)
        val transformer = ByteCodeTransformer(context)

        transformer.runTransform(archiveFile)

        val decompiledResult = decompileClassFileToString(archiveFile.data)

        Truth.assertThat(decompiledResult).contains("com/google/android/material/snackbar/Snackbar")
        Truth.assertThat(decompiledResult)
                .doesNotContain("Lcom.google.android.material.snackbar.Snackbar")
        Truth.assertThat(decompiledResult).doesNotContain("android/support/design/widget/Snackbar")
        Truth.assertThat(decompiledResult).doesNotContain("android.support.design.widget.Snackbar")

        val expectedFileContent = File(
            javaClass.getResource("/classRewriteTest/RxSnackbarKt-expected-decompiled.txt").file)
                .readBytes()
                .toString(Charset.defaultCharset())
        Truth.assertThat(decompiledResult).isEqualTo(expectedFileContent)
    }

    @Test
    fun testClassRewrite_bundleStringsPreserved() {
        val config = ConfigParser.loadDefaultConfig()!!

        val inputClassPath = "/classRewriteTest/ShareCompat.class"
        val inputFile = File(javaClass.getResource(inputClassPath).file)
        val archiveFile = ArchiveFile(Paths.get("/", "ShareCompat.class"), inputFile.readBytes())

        val context = TransformationContext(config = config)
        val transformer = ByteCodeTransformer(context)

        transformer.runTransform(archiveFile)

        val decompiledResult = decompileClassFileToString(archiveFile.data)

        // Both constants need to be present
        Truth.assertThat(decompiledResult).contains(
            "EXTRA_CALLING_PACKAGE = \"androidx.core.app.EXTRA_CALLING_PACKAGE\"")
        Truth.assertThat(decompiledResult).contains(
            "EXTRA_CALLING_PACKAGE_INTEROP = \"android.support.v4.app.EXTRA_CALLING_PACKAGE\"")
    }

    private fun decompileClassFileToString(data: ByteArray): String {
        val reader = ClassReader(data)

        val textifier = Textifier()
        val stream = ByteArrayOutputStream()
        val traceVisitor = TraceClassVisitor(null, textifier, PrintWriter(stream))

        reader.accept(traceVisitor, 0)

        return String(stream.toByteArray())
    }
}