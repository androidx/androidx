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

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.ProjectDescription
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import java.io.FileNotFoundException

private class TestUtils

fun project(): ProjectDescription = ProjectDescription()

/**
 * Loads a [TestFile] from Java source code included in the JAR resources.
 */
fun javaSample(className: String): TestFile = TestFiles.java(
    TestUtils::class.java.getResource(
        "/java/${className.replace('.', '/')}.java"
    )?.readText() ?: throw FileNotFoundException(
        "Could not find Java sources for $className in the integration test project"
    )
)

/**
 * Loads a [TestFile] from Kotlin source code included in the JAR resources.
 */
fun ktSample(className: String): TestFile = TestFiles.kotlin(
    TestUtils::class.java.getResource(
        "/java/${className.replace('.', '/')}.kt"
    )?.readText() ?: throw FileNotFoundException(
        "Could not find Kotlin sources for $className in the integration test project"
    )
)