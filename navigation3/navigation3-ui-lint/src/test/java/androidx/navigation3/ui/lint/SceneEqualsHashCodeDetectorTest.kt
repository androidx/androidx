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

@file:Suppress("UnstableApiUsage")

package androidx.navigation3.ui.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SceneEqualsHashCodeDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = SceneEqualsHashCodeDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(SceneEqualsHashCodeDetector.ISSUE)

    private val sceneStub: TestFile =
        kotlin(
                """
        package androidx.navigation3.scene

        interface Scene<T : Any> {
            val key: Any
        }
        """
            )
            .indented()

    @Test
    fun testRegularClassImplementingScene_reportsError() {
        lint()
            .files(
                sceneStub,
                kotlin(
                        """
                    package com.example

                    import androidx.navigation3.scene.Scene

                    class CustomScene<T : Any>(
                        override val key: T
                    ) : Scene<T>
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/CustomScene.kt:5: Warning: Classes implementing Scene must either be a data class or explicitly override both equals() and hashCode(). [SceneEqualsHashCode]
                class CustomScene<T : Any>(
                      ~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testDataClassImplementingScene_noError() {
        lint()
            .files(
                sceneStub,
                kotlin(
                        """
                    package com.example

                    import androidx.navigation3.scene.Scene

                    data class CustomScene<T : Any>(
                        override val key: T
                    ) : Scene<T>
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testClassOverridingEqualsAndHashCode_noError() {
        lint()
            .files(
                sceneStub,
                kotlin(
                        """
                    package com.example

                    import androidx.navigation3.scene.Scene

                    class CustomScene<T : Any>(
                        override val key: T
                    ) : Scene<T> {
                        override fun equals(other: Any?): Boolean {
                            if (this === other) return true
                            if (other !is CustomScene<*>) return false
                            return key == other.key
                        }

                        override fun hashCode(): Int {
                            return key.hashCode()
                        }
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }
}
