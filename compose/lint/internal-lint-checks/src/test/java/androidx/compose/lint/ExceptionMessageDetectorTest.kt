/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExceptionMessageDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ExceptionMessageDetector()

    override fun getIssues(): MutableList<Issue> {
        return mutableListOf(ExceptionMessageDetector.ISSUE)
    }

    @Test
    fun checkWithMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        check(true) {"Message"}
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun checkWithNamedParameterValue() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        check(value = true) {"Message"}
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun checkWithNamedParameterMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        check(true, lazyMessage = {"Message"})
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun checkWithNamedParameterValueAndMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        check(value = true, lazyMessage = {"Message"})
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun checkWithNamedParameterValueAndMessageReversed() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        check(lazyMessage = {"Message"}, value = true)
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun checkWithoutMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        check(true)
                    }
                    """
                ),
            )
            .run()
            .expect(
                """
                    src/test/test.kt:5: Error: Please specify a lazyMessage param for check [ExceptionMessage]
                                            check(true)
                                            ~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun checkFromDifferentPackageWithoutMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        check(true)
                    }

                    fun check(boolean: Boolean) {}
                    """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun checkNotNullWithMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        checkNotNull(null) {"Message"}
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun checkNotNullWithNamedParameterValue() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        checkNotNull(value = null) {"Message"}
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun checkNotNullWithNamedParameterMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        checkNotNull(null, lazyMessage = {"Message"})
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun checkNotNullWithNamedParameterValueAndMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        checkNotNull(value = null, lazyMessage = {"Message"})
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun checkNotNullWithNamedParameterValueAndMessageReversed() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        checkNotNull(lazyMessage = {"Message"}, value = null)
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun checkNotNullWithoutMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        checkNotNull(null)
                    }
                    """
                ),
            )
            .run()
            .expect(
                """
                    src/test/test.kt:5: Error: Please specify a lazyMessage param for checkNotNull [ExceptionMessage]
                                            checkNotNull(null)
                                            ~~~~~~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun checkNotNullFromDifferentPackageWithoutMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        checkNotNull(null)
                    }

                    fun checkNotNull(value: Any?) {}
                    """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun requireWithMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        require(true) {"Message"}
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun requireWithNamedParameterValue() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        require(value = true) {"Message"}
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun requireWithNamedParameterMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        require(true, lazyMessage = {"Message"})
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun requireWithNamedParameterValueAndMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        require(value = true, lazyMessage = {"Message"})
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun requireWithNamedParameterValueAndMessageReversed() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        require(lazyMessage = {"Message"}, value = true)
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun requireWithoutMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        require(true)
                    }
                    """
                ),
            )
            .run()
            .expect(
                """
                    src/test/test.kt:5: Error: Please specify a lazyMessage param for require [ExceptionMessage]
                                            require(true)
                                            ~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun requireFromDifferentPackageWithoutMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        require(true)
                    }

                    fun require(boolean: Boolean) {}
                    """
                ),
            )
            .run()
            .expectClean()
    }

    @Test
    fun requireNotNullWithMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        requireNotNull(null) {"Message"}
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun requireNotNullWithNamedParameterValue() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        requireNotNull(value = null) {"Message"}
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun requireNotNullWithNamedParameterMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        requireNotNull(null, lazyMessage = {"Message"})
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun requireNotNullWithNamedParameterValueAndMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        requireNotNull(value = null, lazyMessage = {"Message"})
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun requireNotNullWithNamedParameterValueAndMessageReversed() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        requireNotNull(lazyMessage = {"Message"}, value = null)
                    }
                    """
                )
            )
            .run()
            .expectClean()
    }

    @Test
    fun requireNotNullWithoutMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        requireNotNull(null)
                    }
                    """
                ),
            )
            .run()
            .expect(
                """
                    src/test/test.kt:5: Error: Please specify a lazyMessage param for requireNotNull [ExceptionMessage]
                                            requireNotNull(null)
                                            ~~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """
            )
    }

    @Test
    fun requireNotNullFromDifferentPackageWithoutMessage() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    fun content() {
                        requireNotNull(null)
                    }

                    fun requireNotNull(value: Any?) {}
                    """
                ),
            )
            .run()
            .expectClean()
    }
}
