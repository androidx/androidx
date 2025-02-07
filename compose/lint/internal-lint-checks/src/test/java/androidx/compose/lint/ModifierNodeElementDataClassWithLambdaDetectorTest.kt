/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.lint

import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ModifierNodeElementDataClassWithLambdaDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ModifierNodeElementDataClassWithLambdaDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ModifierNodeElementDataClassWithLambdaDetector.ISSUE)

    private val modifierNodeElementStub =
        bytecodeStub(
            filename = "ModifierNodeElement.kt",
            filepath = "androidx/compose/ui/node",
            checksum = 0xdc37d94,
            source =
                """
        package androidx.compose.ui.node

        abstract class ModifierNodeElement<N> {
            abstract override fun equals(other: Any?): Boolean
            abstract override fun hashCode(): Int
        }
        """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2NgYGBmYGBgAmJGBihQYtBiAAAcqiW0GAAAAA==
        """,
            """
        androidx/compose/ui/node/ModifierNodeElement.class:
        H4sIAAAAAAAA/5VSz0/UQBT+pu22paxSQHBZfyCKKxBDV+JJCYkaDSXLasSQ
        KKfZ3XF3oDvVdko47t/iwbsnEw9mw9E/yvimkGjEiz28770v87335pv++Pnt
        O4CHuMdwn6telsreSdRNhx/SXESFjFTaE9Fu2pPvpcjaVDxPxFAo7YExrGy2
        H7UO+TGPEq760cvOoejqx1sXKYbwb86Dw+BuSiX1FoO9srpfhQsvQAU+g6MH
        MmdYb/3PTjTGFR8LnpBybuXiFqvvGJZbadaPDoXuZFyqPOJKpZprmVLeLpKE
        dxJBffwBzwfPqHW5Wsww3TpKdSJVtCs073HN6ZA1PLbJPMsEZgIY2BHxJ9JU
        Tcp6Dxjejkf1wKpZwXgUWKEJvk3JJKFF6PuN2ni05vjjUcg2rKb11Dv95Dq+
        HTo7s2GlbjXdDT/06k6NNa3t08/Wjh/6xE5sN8yADZrZZmZ0JdUDkTHM/8Oa
        9SNNpp5daKollWgXw47I3pjrMsy00i5P9nkmTX1OLr8ulJZDEatjmUuiXvGM
        D4UW2ZPfnjFM7Mm+4rrISBLspUXWFS+k0S+c6/fP1H+InCVY9Mzms2lvenWK
        t6iKjIGElbWvmPhirMUSRbckHdymWD07gACTMH9QFZeIuUOcZ7SXS3IKYdl7
        uWxh426Ji2gQxnRqmhrMHMCOMRvjSow5zBPiaowaFg7ActRx7QBBjskc13Pc
        yOHm8HJUy/xmjilKfgGsFXXlPAMAAA==
        """
        )

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.node.ModifierNodeElement

                data class TestElement(val value: Boolean) : ModifierNodeElement<Boolean>()

                data class NotAnElement(val lambda: () -> Unit)

                class NotADataClass(val lambda: () -> Unit) : ModifierNodeElement<Boolean>() {
                    override fun equals(other: Any?): Boolean {
                        TODO("Not yet implemented")
                    }

                    override fun hashCode(): Int {
                        TODO("Not yet implemented")
                    }
                }
            """
                ),
                modifierNodeElementStub
            )
            .run()
            .expectClean()
    }

    @Test
    fun errors() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.node.ModifierNodeElement

                data class TestElement(val value: Boolean, val lambda: () -> Unit) : ModifierNodeElement<Boolean>()
            """
                ),
                modifierNodeElementStub
            )
            .run()
            .expect(
                """
src/test/TestElement.kt:6: Error: ModifierNodeElement implementations using a data class with lambda properties will result in incorrect equals implementations [ModifierNodeElementDataClassWithLambda]
                data class TestElement(val value: Boolean, val lambda: () -> Unit) : ModifierNodeElement<Boolean>()
                                                           ~~~~~~~~~~~~~~~~~~~~~~
1 errors, 0 warnings
            """
            )
    }
}
