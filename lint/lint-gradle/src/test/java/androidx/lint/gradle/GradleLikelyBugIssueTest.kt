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

package androidx.lint.gradle

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GradleLikelyBugIssueTest :
    GradleLintDetectorTest(
        detector = DiscouragedGradleMethodDetector(),
        issues = listOf(DiscouragedGradleMethodDetector.TO_STRING_ON_PROVIDER_ISSUE),
    ) {
    @Test
    fun `Test usage of Provider#toString`() {
        val input =
            kotlin(
                """
                import org.gradle.api.provider.Property
                import org.gradle.api.provider.Provider

                fun configureProvider(provider: Provider<Any>) {
                    provider.toString()
                }

                fun configureProperty(property: Property<Any>) {
                    property.toString()
                }
            """
                    .trimIndent()
            )

        val expected =
            """
                src/test.kt:5: Error: Use get instead of toString [GradleLikelyBug]
                    provider.toString()
                             ~~~~~~~~
                src/test.kt:9: Error: Use get instead of toString [GradleLikelyBug]
                    property.toString()
                             ~~~~~~~~
                2 errors, 0 warnings
        """
                .trimIndent()
        val expectedFixDiffs =
            """
                Fix for src/test.kt line 5: Replace with get:
                @@ -5 +5
                -     provider.toString()
                +     provider.get()
                Fix for src/test.kt line 9: Replace with get:
                @@ -9 +9
                -     property.toString()
                +     property.get()
        """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test implicit usage of Provider#toString`() {
        val input =
            kotlin(
                """
                import org.gradle.api.provider.Property
                import org.gradle.api.provider.Provider

                fun configureProvider(provider: Provider<Any>) {
                    "this is a provider: ${"$"}provider"
                    "this is another provider ${"$"}{Foo.getProvider()}"
                }

                fun configureProperty(property: Property<Any>) {
                    "this is a property: ${"$"}{property}"
                    "this is another property ${"$"}{Foo.getProperty()}"
                }

                object Foo {
                    fun getProvider(): Provider<Any>? = null
                    fun getProperty(): Property<Any>? = null
                }
                """
            )

        val expected =
            """
                src/Foo.kt:6: Error: Implicit usage of toString on a Provider [GradleLikelyBug]
                                    "this is a provider: ＄provider"
                                                          ~~~~~~~~
                src/Foo.kt:7: Error: Implicit usage of toString on a Provider [GradleLikelyBug]
                                    "this is another provider ＄{Foo.getProvider()}"
                                                                ~~~~~~~~~~~~~~~~~
                src/Foo.kt:11: Error: Implicit usage of toString on a Provider [GradleLikelyBug]
                                    "this is a property: ＄{property}"
                                                           ~~~~~~~~
                src/Foo.kt:12: Error: Implicit usage of toString on a Provider [GradleLikelyBug]
                                    "this is another property ＄{Foo.getProperty()}"
                                                                ~~~~~~~~~~~~~~~~~
                4 errors, 0 warnings
            """
                .trimIndent()
        val expectedFixDiffs =
            """
                Fix for src/Foo.kt line 6: Replace with {provider.get()}:
                @@ -6 +6
                -                     "this is a provider: ＄provider"
                +                     "this is a provider: ＄{provider.get()}"
                Fix for src/Foo.kt line 7: Replace with Foo.getProvider().get():
                @@ -7 +7
                -                     "this is another provider ＄{Foo.getProvider()}"
                +                     "this is another provider ＄{Foo.getProvider().get()}"
                Fix for src/Foo.kt line 11: Replace with property.get():
                @@ -11 +11
                -                     "this is a property: ＄{property}"
                +                     "this is a property: ＄{property.get()}"
                Fix for src/Foo.kt line 12: Replace with Foo.getProperty().get():
                @@ -12 +12
                -                     "this is another property ＄{Foo.getProperty()}"
                +                     "this is another property ＄{Foo.getProperty().get()}"
            """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }
}
