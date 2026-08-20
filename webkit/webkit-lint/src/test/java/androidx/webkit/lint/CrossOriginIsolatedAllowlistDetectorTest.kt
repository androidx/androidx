/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.webkit.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

class CrossOriginIsolatedAllowlistDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = CrossOriginIsolatedAllowlistDetector()

    override fun getIssues(): List<Issue> = listOf(CrossOriginIsolatedAllowlistDetector.ISSUE)

    @Test
    fun testWarningWhenSetCrossOriginIsolatedAllowlistCalled() {
        lint()
            .files(
                java(
                        """
                package com.example;
                import androidx.webkit.Profile;
                import java.util.HashSet;

                public class MyClass {
                    public void configureProfile(Profile profile) {
                        profile.setCrossOriginIsolatedAllowlist(new HashSet<>());
                    }
                }
                """
                    )
                    .indented(),
                *stubs,
            )
            .run()
            .expect(
                """
                src/com/example/MyClass.java:7: Warning: Calling setCrossOriginIsolatedAllowlist enables potentially dangerous cross-origin isolated APIs (such as SharedArrayBuffer). Ensure only trusted origins are added to the allowlist. [CrossOriginIsolatedAllowlist]
                        profile.setCrossOriginIsolatedAllowlist(new HashSet<>());
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testWarningWhenSetCrossOriginIsolatedAllowlistCalledKotlin() {
        lint()
            .files(
                kotlin(
                        """
                package com.example
                import androidx.webkit.Profile

                class MyClass {
                    fun configureProfile(profile: Profile) {
                        profile.setCrossOriginIsolatedAllowlist(setOf("https://example.com"))
                    }
                }
                """
                    )
                    .indented(),
                *stubs,
            )
            .run()
            .expect(
                """
                src/com/example/MyClass.kt:6: Warning: Calling setCrossOriginIsolatedAllowlist enables potentially dangerous cross-origin isolated APIs (such as SharedArrayBuffer). Ensure only trusted origins are added to the allowlist. [CrossOriginIsolatedAllowlist]
                        profile.setCrossOriginIsolatedAllowlist(setOf("https://example.com"))
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testNoWarningWhenCalledOnUnrelatedClass() {
        lint()
            .files(
                java(
                        """
                package com.example;
                import java.util.Set;

                public class OtherClass {
                    public void setCrossOriginIsolatedAllowlist(Set<String> rules) {
                    }
                }

                public class MyClass {
                    public void configureOther(OtherClass other) {
                        other.setCrossOriginIsolatedAllowlist(null);
                    }
                }
                """
                    )
                    .indented(),
                *stubs,
            )
            .run()
            .expectClean()
    }

    private val stubs =
        arrayOf(
            java(
                    """
            package androidx.webkit;
            import java.util.Set;
            public interface Profile {
                default void setCrossOriginIsolatedAllowlist(Set<String> allowedOriginRules) {}
            }
            """
                )
                .indented()
        )
}
