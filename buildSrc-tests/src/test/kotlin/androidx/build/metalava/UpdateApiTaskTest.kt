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

package androidx.build.metalava

import org.gradle.api.GradleException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateApiTaskTest {
    @Rule
    @JvmField
    val tmpFolder = TemporaryFolder()

    @Test
    fun testCompareLineCount() {
        val testCases = mapOf(
            "No lines" to 0,
            "Has one\nline" to 1,
            "Has\ntwo\nlines" to 2,
            "" to 0,
            "\n" to 1,
            "\n\n" to 2
        )

        for ((text, count) in testCases) {
            assertEquals(compareLineCount(text, count), 0)
        }
    }

    @Test
    fun testCopyThrowsExceptionOnDiff() {
        val source = tmpFolder.newFile().apply {
            writeText("""
// Signature format: 4.0
            """.trimIndent())
        }
        val dest = tmpFolder.newFile().apply {
            writeText("""
// Signature format: 4.0
package androidx.core.accessibilityservice {
  public final class AccessibilityServiceInfoCompat {
    method public static String capabilityToString(int);
  }
}
            """.trimIndent())
        }

        assertThrows(GradleException::class.java) {
            copy(source, dest, false)
        }
    }
}
