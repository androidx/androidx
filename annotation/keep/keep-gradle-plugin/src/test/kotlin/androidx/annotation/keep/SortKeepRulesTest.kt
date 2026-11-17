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

package androidx.annotation.keep

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SortKeepRulesTest {

    @Test
    fun emptyInputReturnsEmptyString() {
        assertThat(sortKeepRules("")).isEmpty()
        assertThat(sortKeepRules("   \n\n  ")).isEmpty()
    }

    @Test
    fun sortsRulesDeterministicallyKeepingCommentsAttached() {
        val unsorted =
            """
            # context: Lpkg/Zeta;foo()V
            -keepclasseswithmembers class pkg.Zeta { void foo(); }
            # context: Lpkg/Alpha;bar()V
            -if class pkg.Alpha { void bar(); }
            -keepclasseswithmembers class pkg.Target { void m(); }
            # context: Lpkg/Alpha;bar()V
            -if class pkg.Alpha { void bar(); }
            -keepclasseswithmembers class ** extends pkg.Target { void m(); }
            """
                .trimIndent()

        val expected =
            """
            # context: Lpkg/Alpha;bar()V
            -if class pkg.Alpha { void bar(); }
            -keepclasseswithmembers class ** extends pkg.Target { void m(); }
            # context: Lpkg/Alpha;bar()V
            -if class pkg.Alpha { void bar(); }
            -keepclasseswithmembers class pkg.Target { void m(); }
            # context: Lpkg/Zeta;foo()V
            -keepclasseswithmembers class pkg.Zeta { void foo(); }
            """
                .trimIndent() + "\n"

        assertThat(sortKeepRules(unsorted)).isEqualTo(expected)
    }

    @Test
    fun preservesMultiLineCommentsWithFollowingRule() {
        val unsorted =
            """
            # context: Lpkg/B;m()V
            # description: second rule
            -keep class pkg.B
            # context: Lpkg/A;m()V
            # description: first rule
            -keep class pkg.A
            """
                .trimIndent()

        val expected =
            """
            # context: Lpkg/A;m()V
            # description: first rule
            -keep class pkg.A
            # context: Lpkg/B;m()V
            # description: second rule
            -keep class pkg.B
            """
                .trimIndent() + "\n"

        assertThat(sortKeepRules(unsorted)).isEqualTo(expected)
    }

    @Test
    fun ignoresNewlinesBetweenCommentsAndRules() {
        val unsorted =
            """
            # context: Lpkg/B;m()V

            # description: second rule

            -keep class pkg.B


            # context: Lpkg/A;m()V
            # description: first rule

            -keep class pkg.A
            """
                .trimIndent()

        val expected =
            """
            # context: Lpkg/A;m()V
            # description: first rule
            -keep class pkg.A
            # context: Lpkg/B;m()V
            # description: second rule
            -keep class pkg.B
            """
                .trimIndent() + "\n"

        assertThat(sortKeepRules(unsorted)).isEqualTo(expected)
    }
}
