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

package androidx.navigation.common.lint

import androidx.navigation.lint.common.NAVIGATION_STUBS
import androidx.navigation.lint.common.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class MissingKeepAnnotationDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = TypeSafeDestinationMissingAnnotationDetector()

    override fun getIssues(): List<Issue> =
        listOf(TypeSafeDestinationMissingAnnotationDetector.MissingKeepAnnotationIssue)

    @Test
    fun testNavDestinationBuilderConstructor_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.annotation.Keep

                @Keep
                enum class TestEnum { ONE, TWO }
                class TestClass(val enum: TestEnum)

                fun navigation() {
                    NavDestinationBuilder<NavGraph>(route = TestClass::class)
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .allowDuplicates()
            .run()
            .expectClean()
    }

    @Test
    fun testNavDestinationBuilderConstructor_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*

                enum class TestEnum { ONE, TWO }
                class TestClass(val enum: TestEnum)

                fun navigation() {
                    NavDestinationBuilder<NavGraph>(route = TestClass::class)
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .allowDuplicates()
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
    fun testNavGraphBuilderConstructor_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.annotation.Keep

                @Keep
                enum class TestEnum { ONE, TWO }
                class TestClass(val enum: TestEnum)

                fun navigation() {
                    NavGraphBuilder(route = TestClass::class)
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
    fun testNavGraphBuilderConstructor_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*

                enum class TestEnum { ONE, TWO }
                class TestClass(val enum: TestEnum)

                fun navigation() {
                    NavGraphBuilder(route = TestClass::class)
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
0 errors, 1 warnings"""
            )
    }

    @Test
    fun testNavGraphBuilderNavigation_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.annotation.Keep

                @Keep
                enum class TestEnum { ONE, TWO }
                class TestClass(val enum: TestEnum)

                fun navigation() {
                    val builder = NavGraphBuilder(provider, TestGraph, null)
                    builder.navigation<TestClass>()
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
    fun testNavGraphBuilderNavigation_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*

                enum class TestEnum { ONE, TWO }
                class TestClass(val enum: TestEnum)

                fun navigation() {
                    val builder = NavGraphBuilder(provider, TestGraph, null)
                    builder.navigation<TestClass>()
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
    fun testNavProviderNavigation_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import androidx.annotation.Keep

                @Keep
                enum class TestEnum { ONE, TWO }
                class TestClass(val enum: TestEnum)

                fun navigation() {
                    val provider = NavigatorProvider()
                    provider.navigation(route = TestClass::class)
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
    fun testNavProviderNavigation_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*

                enum class TestEnum { ONE, TWO }
                class TestClass(val enum: TestEnum)

                fun navigation() {
                    val provider = NavigatorProvider()
                    provider.navigation(route = TestClass::class)
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
    fun testDeeplink_noError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import kotlinx.serialization.*
                import androidx.annotation.Keep

                @Serializable class TestClass
                @Keep enum class DeepLinkArg
                @Serializable class DeepLink(val arg: DeepLinkArg)

                fun navigation() {
                    val builder = NavDestinationBuilder<NavGraph>(route = TestClass::class)
                    builder.deepLink<DeepLink>()
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
    fun testDeeplink_hasError() {
        lint()
            .files(
                kotlin(
                        """
                package com.example

                import androidx.navigation.*
                import kotlinx.serialization.*
                import androidx.annotation.Keep

                @Serializable class TestClass
                enum class DeepLinkArg
                @Serializable class DeepLink(val arg: DeepLinkArg)

                fun navigation() {
                    val builder = NavDestinationBuilder<NavGraph>(route = TestClass::class)
                    builder.deepLink<DeepLink>()
                }
                """
                    )
                    .indented(),
                *STUBS,
            )
            .run()
            .expect(
                """
src/com/example/TestClass.kt:8: Warning: To prevent this Enum's serializer from being obfuscated in minified builds, annotate it with @androidx.annotation.Keep [MissingKeepAnnotation]
enum class DeepLinkArg
           ~~~~~~~~~~~
0 errors, 1 warnings
            """
                    .trimIndent()
            )
    }

    internal val KEEP_ANNOTATION =
        bytecodeStub(
            "Keep.kt",
            "androidx/annotation",
            0x2645a498,
            """
package androidx.annotation

@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.FIELD
)
public annotation class Keep
        """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijgsuUSTsxLKcrPTKnQy0ssy0xPLMnM
                zxPicsyrLMnIzEv3LhHi90ssc87PKynKz8lJLQIKcAIFPPKLS7xLuCS5uJPz
                c/VSKxJzC3JShbhCUotLXPNKc71LlBi0GACE4q01cgAAAA==
                """,
            """
                androidx/annotation/Keep.class:
                H4sIAAAAAAAA/4VSy3ISQRQ9PYRnNIAahcSYGCPxTUy5y4rgoFOSGWroWEWx
                SHWgKzVhmEkxAyY7dn6G/+HColz6UZZ9JcIsUDenT98+99n3x8+v3wC8wUuG
                gvC6A9/pXpaF5/mhCB3fK3+Q8iIJxpA7FyNRdoV3VrZOz2UnTCLGsDm3Rpwq
                M5pEnGGj3vND1/GiEluG0iN2wBAfCXcoGXYX6Oahoh6JQ8Os2C2GtQUuXAzO
                ZKhUK8J1/U+yOzUEDDv/TDDzW6oZdV11XDFNi1e4YZkn1Xql2VSVXp/LVcts
                cvu4yi2bIVU7NqskY8g2bKuh27x18k7nXLejlua1JV4z9Ppbhq36wuFF+yz9
                R9LwXadzdUAjXiictbS9+F13ZV9F4lcXkvrmrYbqO3Gk8/eWKjAbGcD0Kf9n
                gEcyFF0RCuWl9UcxtUKMIE0ABtZT9kuHbnuKdV8zFCfjVEYraBktt576/lkr
                TMb72h47nIxJsE9f+bf9U0lUzCTRV72QIdP0h4OOrDmuWpqiPVSj6MuPTuCc
                unL+nUFJBcaS8kxQUYo//43P8EKdXxCH2mukJNLIYFnRG22kJW5ihSBLkJux
                PMEtgtsEdwhWcXca4B5yKBBtIy5RxBrBKsE6QZ7gPjZUxgdtxAxsGtgy8BDb
                iuKRgR08boMFKGG3DS3AkwBPfwGJU24VmQMAAA==
                """
        )

    val STUBS = arrayOf(*NAVIGATION_STUBS, KEEP_ANNOTATION)
}
