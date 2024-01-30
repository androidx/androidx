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

package androidx.annotation.experimental.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles

/**
 * Loads a [TestFile] from Java source code included in the JAR resources.
 */
fun javaSample(className: String): TestFile {
    return TestFiles.java(
        RequiresOptInDetectorTest::class.java.getResource(
            "/java/${className.replace('.', '/')}.java"
        )!!.readText()
    )
}

/**
 * Loads a [TestFile] from Kotlin source code included in the JAR resources.
 */
fun ktSample(className: String): TestFile {
    return TestFiles.kotlin(
        RequiresOptInDetectorTest::class.java.getResource(
            "/java/${className.replace('.', '/')}.kt"
        )!!.readText()
    )
}
