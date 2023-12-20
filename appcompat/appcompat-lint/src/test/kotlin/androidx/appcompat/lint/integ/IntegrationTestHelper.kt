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

package androidx.appcompat.lint.integ

import androidx.appcompat.AppCompatIssueRegistry
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestMode

/**
 * Runs AppCompat lint checks against the specified [testFiles] in PARTIAL mode.
 */
fun check(vararg testFiles: TestFile): TestLintResult {
    return TestLintTask.lint()
        .files(
            *testFiles
        )
        .issues(*AppCompatIssueRegistry().issues.toTypedArray())
        .testModes(TestMode.PARTIAL)
        .allowCompilationErrors()
        .run()
}

/**
 * Loads a [TestFile] from Java source code included in the JAR resources.
 */
fun javaSample(className: String): TestFile {
    val url = TestFiles::class.java.getResource(
        "/java/${className.replace('.', '/')}.java"
    ) ?: throw IllegalArgumentException("Failed to load source for $className")
    return TestFiles.java(url.readText())
}

/**
 * Loads a [TestFile] from Kotlin source code included in the JAR resources.
 */
fun ktSample(className: String): TestFile {
    val url = TestFiles::class.java.getResource(
        "/java/${className.replace('.', '/')}.kt"
    ) ?: throw IllegalArgumentException("Failed to load source for $className")
    return TestFiles.kotlin(url.readText())
}

/**
 * Loads a [TestFile] from Kotlin source code included in the JAR resources.
 */
fun xmlSample(resName: String): TestFile {
    val path = "/res/${resName.replace('.', '/')}.xml"
    val url = TestFiles::class.java.getResource(path)
        ?: throw IllegalArgumentException("Failed to load source for $resName")
    return TestFiles.xml(path, url.readText())
}
