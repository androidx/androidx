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

package androidx.build.lint.replacewith

import androidx.build.lint.ReplaceWithDetector
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask

fun check(vararg testFiles: TestFile): TestLintResult {
    return TestLintTask.lint()
        .files(
            ANDROIDX_REPLACE_WITH_KT,
            ANDROIDX_ANY_THREAD_KT,
            *testFiles
        )
        .issues(ReplaceWithDetector.ISSUE)
        .run()
}

/**
 * Loads a [TestFile] from Java source code included in the JAR resources.
 */
fun javaSample(className: String): TestFile {
    return TestFiles.java(
        ReplaceWithDetectorMethodTest::class.java.getResource(
            "/java/${className.replace('.', '/')}.java"
        )!!.readText()
    )
}

/**
 * Loads a [TestFile] from Kotlin source code included in the JAR resources.
 */
fun ktSample(className: String): TestFile {
    return TestFiles.kotlin(
        ReplaceWithDetectorMethodTest::class.java.getResource(
            "/java/${className.replace('.', '/')}.kt"
        )!!.readText()
    )
}

/**
 * [TestFile] containing ReplaceWith.kt from the Annotation library.
 *
 * This is a workaround for IntelliJ failing to recognize source files if they are also
 * included as resources.
 */
val ANDROIDX_REPLACE_WITH_KT: TestFile = TestFiles.kotlin(
    """
            package androidx.annotation

            @Retention(AnnotationRetention.BINARY)
            @Target(
                AnnotationTarget.CLASS,
                AnnotationTarget.FUNCTION,
                AnnotationTarget.PROPERTY,
                AnnotationTarget.ANNOTATION_CLASS,
                AnnotationTarget.CONSTRUCTOR,
                AnnotationTarget.PROPERTY_SETTER,
                AnnotationTarget.PROPERTY_GETTER,
                AnnotationTarget.TYPEALIAS
            )
            @java.lang.annotation.Target(
                ElementType.CONSTRUCTOR,
                ElementType.FIELD,
                ElementType.METHOD,
                ElementType.TYPE,
            )
            annotation class ReplaceWith(
                val expression: String,
                vararg val imports: String
            )
            """.trimIndent()
)

/**
 * [TestFile] containing AnyThread.kt from the Annotation library.
 */
val ANDROIDX_ANY_THREAD_KT: TestFile = TestFiles.kotlin(
    """
            package androidx.annotation
            
            @MustBeDocumented
            @Retention(AnnotationRetention.BINARY)
            @Target(
                AnnotationTarget.FUNCTION,
                AnnotationTarget.PROPERTY_GETTER,
                AnnotationTarget.PROPERTY_SETTER,
                AnnotationTarget.CONSTRUCTOR,
                AnnotationTarget.ANNOTATION_CLASS,
                AnnotationTarget.CLASS,
                AnnotationTarget.VALUE_PARAMETER
            )
            annotation class AnyThread
            """.trimIndent()
)
