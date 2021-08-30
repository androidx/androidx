/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.activity.lint

import androidx.activity.lint.ActivityResultFragmentVersionDetector.Companion.FRAGMENT_VERSION
import androidx.activity.lint.stubs.STUBS
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ActivityResultFragmentVersionDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ActivityResultFragmentVersionDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ActivityResultFragmentVersionDetector.ISSUE)

    @Test
    fun expectPassRegisterForActivityResult() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.activity.result.ActivityResultCaller
                import androidx.activity.result.contract.ActivityResultContract

                val launcher = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
            """
            ),
            *STUBS,
            gradle(
                "build.gradle",
                """
                dependencies {
                    api("androidx.fragment:fragment:$FRAGMENT_VERSION")
                }
            """
            ).indented()
        )
            .run().expectClean()
    }

    @Test
    fun expectPassRegisterForActivityResultStableVersions() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.activity.result.ActivityResultCaller
                import androidx.activity.result.contract.ActivityResultContract

                val launcher = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
            """
            ),
            *STUBS,
            gradle(
                "build.gradle",
                """
                dependencies {
                    api("androidx.fragment:fragment:1.3.1")
                }
            """
            ).indented()
        )
            .run().expectClean()
    }

    @Test
    fun expectPassNewerStableVersion() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.activity.result.ActivityResultCaller
                import androidx.activity.result.contract.ActivityResultContract

                val launcher = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
            """
            ),
            *STUBS,
            gradle(
                "build.gradle",
                """
                dependencies {
                    api("androidx.fragment:fragment:1.4.0")
                }
            """
            ).indented()
        ).run().expectClean()
    }

    @Test
    fun expectPassNewerSnapshotVersions() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.activity.result.ActivityResultCaller
                import androidx.activity.result.contract.ActivityResultContract

                val launcher = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
            """
            ),
            *STUBS,
            gradle(
                "build.gradle",
                """
                dependencies {
                    api("androidx.fragment:fragment:1.4.0-SNAPSHOT")
                }
            """
            ).indented()
        ).run().expectClean()
    }

    @Test
    fun expectPassNewerAlphaVersions() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.activity.result.ActivityResultCaller
                import androidx.activity.result.contract.ActivityResultContract

                val launcher = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
            """
            ),
            *STUBS,
            gradle(
                "build.gradle",
                """
                dependencies {
                    api("androidx.fragment:fragment:1.4.0-alpha01")
                }
            """
            ).indented()
        ).run().expectClean()
    }

    @Test
    fun expectPassRegisterForActivityResultProject() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.activity.result.ActivityResultCaller
                import androidx.activity.result.contract.ActivityResultContract

                val launcher = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
            """
            ),
            *STUBS,
            gradle(
                "build.gradle",
                """
                dependencies {
                    implementation(project(":fragment:fragment-ktx"))
                }
            """
            ).indented()
        )
            .run().expectClean()
    }

    @Test
    fun expectFailRegisterForActivityResult() {
        lint().files(
            gradle(
                """
                dependencies {
                    api("androidx.fragment:fragment:1.2.4")
                }
            """
            ),
            *STUBS,
            kotlin(
                """
                package com.example

                import androidx.activity.result.ActivityResultCaller
                import androidx.activity.result.contract.ActivityResultContract

                val launcher = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
            """
            ).indented()
        )
            .run()
            .expect(
                """
                src/main/kotlin/com/example/test.kt:6: Error: Upgrade Fragment version to at least $FRAGMENT_VERSION. [InvalidFragmentVersionForActivityResult]
                val launcher = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
    }

    @Test
    fun expectFailRegisterForActivityResultInMethod() {
        lint().files(
            gradle(
                """
                dependencies {
                    api("androidx.fragment:fragment:1.2.4")
                }
            """
            ),
            *STUBS,
            kotlin(
                """
                package com.example

                import androidx.activity.result.ActivityResultCaller
                import androidx.activity.result.contract.ActivityResultContract

                lateinit var launcher: ActivityResultLauncher

                fun foo() {
                    launcher = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
                }
            """
            ).indented()
        )
            .run()
            .expect(
                """
                src/main/kotlin/com/example/test.kt:9: Error: Upgrade Fragment version to at least $FRAGMENT_VERSION. [InvalidFragmentVersionForActivityResult]
                    launcher = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
    }

    @Test
    fun expectFailRegisterForActivityResultMultipleCalls() {
        lint().files(
            gradle(
                """
                dependencies {
                    api("androidx.fragment:fragment:1.2.4")
                }
            """
            ),
            *STUBS,
            kotlin(
                """
                package com.example

                import androidx.activity.result.ActivityResultCaller
                import androidx.activity.result.contract.ActivityResultContract

                val launcher1 = ActivityResultCaller().registerForActivityResult(ActivityResultContract())

                lateinit var launcher2: ActivityResultLauncher

                fun foo() {
                    launcher2 = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
                }
            """
            ).indented()
        )
            .run()
            .expect(
                """
                src/main/kotlin/com/example/test.kt:6: Error: Upgrade Fragment version to at least $FRAGMENT_VERSION. [InvalidFragmentVersionForActivityResult]
                val launcher1 = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                src/main/kotlin/com/example/test.kt:11: Error: Upgrade Fragment version to at least $FRAGMENT_VERSION. [InvalidFragmentVersionForActivityResult]
                    launcher2 = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                2 errors, 0 warnings
                """.trimIndent()
            )
    }

    @Ignore("b/187524979")
    @Test
    fun expectFailTransitiveDependency() {
        val projectFragment = project(
            kotlin(
                """
                package com.example

                import androidx.activity.result.ActivityResultCaller
                import androidx.activity.result.contract.ActivityResultContract

                val launcher = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
            """
            ),
            gradle(
                "build.gradle",
                """
                dependencies {
                    implementation("androidx.fragment:fragment-ktx:1.2.4")
                }
            """
            ).indented()
        ).withDependencyGraph(
            """
                +--- androidx.fragment:fragment-ktx:1.2.4
                     \--- androidx.fragment:fragment:1.2.4
            """.trimIndent()
        )

        lint().projects(projectFragment).run().expect(
            """
                src/main/kotlin/com/example/test.kt:7: Error: Upgrade Fragment version to at least $FRAGMENT_VERSION. [InvalidFragmentVersionForActivityResult]
                                val launcher = ActivityResultCaller().registerForActivityResult(ActivityResultContract())
                                               ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
            """.trimIndent()
        )
    }
}
