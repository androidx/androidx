/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation.runtime.lint

import androidx.navigation.lint.common.KEEP_ANNOTATION
import androidx.navigation.lint.common.NAVIGATION_STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class MissingKeepAnnotationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = TypeSafeDestinationMissingAnnotationDetector()

    override fun getIssues(): List<Issue> =
        listOf(TypeSafeDestinationMissingAnnotationDetector.MissingKeepAnnotationIssue)

    @Test
    fun testActivityNavigatorDestinationBuilderConstructor_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.annotation.Keep

                @Keep enum class TestEnum { ONE, TWO }
                class TestClass(val arg: TestEnum)

                fun navigation() {
                    ActivityNavigatorDestinationBuilder(route = TestClass::class)
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testActivityNavigatorDestinationBuilderConstructor_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*

                enum class TestEnum { ONE, TWO }
                class TestClass(val arg: TestEnum)

                fun navigation() {
                    ActivityNavigatorDestinationBuilder(route = TestClass::class)
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expect(
                """
src/com/example/TestEnum.kt:5: Warning: To prevent this Enum's serializer from being obfuscated in minified builds, annotate it with @androidx.annotation.Keep [MissingKeepAnnotation]
enum class TestEnum { ONE, TWO }
           ~~~~~~~~
0 errors, 1 warnings
            """
                    .trimIndent()
            )
    }

    @Test
    fun testActivity_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.annotation.Keep

                class RouteClass
                @Keep enum class TestEnum { ONE, TWO }
                class TestClass(val arg: TestEnum)

                fun navigation() {
                    val builder = NavGraphBuilder(RouteClass::class)
                    builder.activity<TestClass>()
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testActivity_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*

                class RouteClass
                enum class TestEnum { ONE, TWO }
                class TestClass(val arg: TestEnum)

                fun navigation() {
                    val builder = NavGraphBuilder(RouteClass::class)
                    builder.activity<TestClass>()
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expect(
                """
src/com/example/RouteClass.kt:6: Warning: To prevent this Enum's serializer from being obfuscated in minified builds, annotate it with @androidx.annotation.Keep [MissingKeepAnnotation]
enum class TestEnum { ONE, TWO }
           ~~~~~~~~
0 errors, 1 warnings
            """
                    .trimIndent()
            )
    }

    val STUBS = arrayOf(*NAVIGATION_STUBS, ACTIVITY_NAVIGATION_DESTINATION_BUILDER, KEEP_ANNOTATION)
}
