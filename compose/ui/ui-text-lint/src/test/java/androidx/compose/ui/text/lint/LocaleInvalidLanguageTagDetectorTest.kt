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

package androidx.compose.ui.text.lint

import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LocaleInvalidLanguageTagDetectorTest : LintDetectorTest() {
    private val LocaleStub: TestFile =
        bytecodeStub(
            filename = "Locale.kt",
            filepath = "androidx/compose/ui/text/intl",
            checksum = 0xddb66f8c,
            """
            package androidx.compose.ui.text.intl
            
            class Locale internal constructor(val value: Int) {
                constructor(languageTag: String) : this(5)
            }
            
        """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg4uNiKUktLhFiCwGS3iVKDFoMALlw
                FOQoAAAA
                """,
            """
                androidx/compose/ui/text/intl/Locale.class:
                H4sIAAAAAAAA/41SS08TURT+7kwf02GgQwWEovgAoRRlCnGHIT4Sk0lqNUC6
                YeNte1NuO50xM7cNy/4W125MNBgXpnHpjzKe2zaAkYXJzLnnfPOdx3zn/vr9
                /QeAp9hn2OBhK45k69xrRr0PUSK8vvSUOFeeDFXgVaMmD0QWjMHt8AH3Ah62
                vbeNjmiqLEyGzDMZSnXIkCr523UGs7Rdd5BG1kYKFkN6wIO+YGC+AxszORhw
                iKzOZMKwWf2f7gcMVluo+qQQNfAZFkvVq3GOVSzD9oFuv16N4rbXEaoRcxkm
                Hg/DSHElI/Jrkar1g4DKzei0Pm+LE962sMCw1o1UIEOvM+jpziIOeeD5oa6b
                yGaSxRK1bJ6JZnda5B2PeU8QkWHr+igTZQ5uGs7BMlZs3EZR65OxSYk7DIV/
                qQzz1ek8b4TiLa44YUZvYNLSmDY5bUCidgk/lzqqkNfaY3g/GhZtY9mwDXc0
                tOkx3DnbsHRs0Wkuj4b7RoW9zFtLhYxrFI2K+fNjxnBThIyGBKUJyoyh7NH8
                ZWRRqWLKstyc7kP3hrrnJuvZ7Spa6KuoRcvJV2Uoav1eQ8QnvBEI/X+aVOex
                1PEU3Djqh0r2hB8OZCIJupTzxdW+GOzjqB83xWupc1amOfVJxjUi9kjK1FiQ
                gr5j5Jnkk8ZkH1N0SN8NOu3yBXLl1a+Y/UyRgSdkNQeUvUt2acLCHPJaX/J0
                NVoHXHontTwtO53p8hfMfrqxjDMhTMvoQQrE0snPp4M4O4XFb1gtWxe4+/co
                GWJfjeJcjuJgAWv03SM/qye4NU6rjO0OKQD4hN6j9PunMH088PHQxzo2yMUj
                H5vYOgVLUML2KewE+QTlBAtjW0g0Qk4mgZtg/g+5KP69IAQAAA==
                """
        )

    private val LocaleListStub: TestFile =
        bytecodeStub(
            filename = "LocaleList.kt",
            filepath = "androidx/compose/ui/text/intl",
            checksum = 0x843815c8,
            """
            package androidx.compose.ui.text.intl
            
            class LocaleList(val localeList: Int) {
                constructor(languageTags: String) : this(5)
            }
        """,
            """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgBGJOBijg4uNiKUktLhFiCwGS3iVKDFoMALlw
                FOQoAAAA
                """,
            """
                androidx/compose/ui/text/intl/LocaleList.class:
                H4sIAAAAAAAA/41STW/TQBB9a6eJYxzihra0KZSPFpqmUKcVt6IKioRkyRTU
                VrlUHDbJKt3EsZG9iXrsb+HMBQlUxAFVHPlRiNkkSoroAcmenRnPvHl+s79+
                f/8B4Bl2GCo8aiWxbJ15zbj3IU6F15eeEmfKk5EKvSBu8lAEMlU5MAa3wwfc
                C3nU9t42OqJJWZMh+1xGUu0xZCr+Rp3BrGzUHcwgZyMDi8EOJygMzHdg40Ye
                BhzqUKcyZagG/0tjl6HQFiq4gkjjfIb5SjAld6QSGbV3NZnVIE7aXkeoRsJl
                lHo8imLFlYzJP4jVQT8MCdPRbX3eFse8nVqYY1jpxiqUkdcZ9DQHkUQ89PxI
                A6eymeawQDObp6LZHaO84wnvCSpkWL/KZSTU7nXsHCxiycZtlLVcWZs0ucNQ
                +reUYTYY83kjFG9xxSln9AYm7ZFpk9cGJG+X8mdSRzXyWtsM7y/Py7axaNiG
                e3lu02O4N23D0rFFp7l4eb5j1Nh+0VooZV2jbNTMnx+zhpvZL1A8Q3F2GOcO
                ZyeRRTjljGW5eT2E7hGNLkyXstWlvWRexS3BUAxkJA76vYZIjnkjFPoHdWGd
                J1LH4+TaYT9Ssif8aCBTSamJni+nG6O7dBT3k6Z4LXXP0rinPuq4Uoht0jIz
                VKSkrxt5JvkkMtknFO3Rd4NOu3qBfHX5KwqfKTLwlKyuAXVvkV0YVeEmilpg
                8jQa7QMuvSMsT+tO50z1CwqfroVxRgVjGE2kRFW6+cWYiLNZmv+G5ap1gbt/
                U8lS9ZSKM6HiYA4r9N0jP6cZ3Bq21YZ2kxQAfMreo/b7JzB9PPDx0Mcq1sjF
                Ix+PsX4ClqKCjRPYKYopqinmhraU6gw52RRuitk/ysbdOTQEAAA=
                """
        )

    override fun getDetector() = LocaleInvalidLanguageTagDetector()

    override fun getIssues() =
        mutableListOf(LocaleInvalidLanguageTagDetector.InvalidLanguageTagDelimiter)

    @Test
    fun hyphensDelimiter_locale_shouldNotWarn() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    import androidx.compose.ui.text.intl.Locale

                    fun foo() {
                        val locale = Locale("en-UK")
                    }
                """
                ),
                LocaleStub
            )
            .run()
            .expectClean()
    }

    @Test
    fun hyphensDelimiter_localeList_single_shouldNotWarn() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    import androidx.compose.ui.text.intl.LocaleList

                    fun foo() {
                        val locale = LocaleList("en-UK")
                    }
                """
                ),
                LocaleStub,
                LocaleListStub
            )
            .run()
            .expectClean()
    }

    @Test
    fun hyphensDelimiter_localeList_multiple_shouldNotWarn() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    import androidx.compose.ui.text.intl.LocaleList

                    fun foo() {
                        val locale = LocaleList("en-UK,en-US")
                    }
                """
                ),
                LocaleStub,
                LocaleListStub
            )
            .run()
            .expectClean()
    }

    @Test
    fun underscoreDelimiter_locale_shouldWarn() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    import androidx.compose.ui.text.intl.Locale

                    fun bar(locale: Locale) {}
                    fun foo() {
                        bar(Locale("en_UK"))
                    }
                """
                ),
                LocaleStub
            )
            .run()
            .expect(
                """
                src/test/test.kt:8: Error: A hyphen (-), not an underscore (_) delimiter should be used in a language tag [InvalidLanguageTagDelimiter]
                                        bar(Locale("en_UK"))
                                                   ~~~~~~~
                1 errors, 0 warnings
            """
            )
    }

    @Test
    fun underscoreDelimiter_localeList_single_shouldWarn() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    import androidx.compose.ui.text.intl.LocaleList

                    fun foo() {
                        val locale = LocaleList("en_UK")
                    }
                """
                ),
                LocaleStub,
                LocaleListStub
            )
            .run()
            .expect(
                """
                src/test/test.kt:7: Error: A hyphen (-), not an underscore (_) delimiter should be used in a language tag [InvalidLanguageTagDelimiter]
                                        val locale = LocaleList("en_UK")
                                                                ~~~~~~~
                1 errors, 0 warnings
            """
            )
    }

    @Test
    fun underscoreDelimiter_localeList_multiple_shouldWarn() {
        lint()
            .files(
                kotlin(
                    """
                    package test

                    import androidx.compose.ui.text.intl.LocaleList

                    fun foo() {
                        val locale = LocaleList("en_UK,en-US")
                    }
                """
                ),
                LocaleStub,
                LocaleListStub
            )
            .run()
            .expect(
                """
                src/test/test.kt:7: Error: A hyphen (-), not an underscore (_) delimiter should be used in a language tag [InvalidLanguageTagDelimiter]
                                        val locale = LocaleList("en_UK,en-US")
                                                                ~~~~~~~~~~~~~
                1 errors, 0 warnings
            """
            )
    }
}
