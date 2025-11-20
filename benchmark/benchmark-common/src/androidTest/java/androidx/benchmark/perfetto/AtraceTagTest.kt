/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.perfetto

import androidx.benchmark.Shell
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AtraceTagTest {
    private val shellSessionRooted = Shell.isSessionRooted()

    @Test
    fun atraceListCategories_readable() {
        val results = Shell.executeScriptCaptureStdout("atrace --list_categories")
        assertNotEquals("", results)
    }

    private fun getActualSupportedTags(): Set<String> {
        val results = Shell.executeScriptCaptureStdout("atrace --list_categories")

        assertNotEquals("", results)
        val actualSupportedTags =
            results
                .split("\n")
                .map {
                    println("captured $it")
                    it.trim().split(" ").first()
                }
                .filter { it.isNotBlank() }
                .toSet()

        // verify able to read stdout with guaranteed tag
        assertContains(actualSupportedTags, "view")

        return actualSupportedTags
    }

    @Test
    fun atraceListCategories_unsupported() {
        val actualSupportedTags = getActualSupportedTags()

        val expectedUnsupportedTags = AtraceTag.unsupported(rooted = shellSessionRooted)
        val unexpectedTags = expectedUnsupportedTags.intersect(actualSupportedTags)
        assertEquals(setOf(), unexpectedTags, "Tags expected to be unsupported weren't")
    }

    @Test
    fun atraceListCategories_supported() {
        val actualSupportedTags = getActualSupportedTags()
        val expectedSupportedTags =
            AtraceTag.supported(rooted = shellSessionRooted).map { it.tag }.toSet()

        val missingTags = expectedSupportedTags - actualSupportedTags
        assertEquals(setOf(), missingTags, "Tags expected to be supported weren't")
    }

    @Test
    fun shellSession_root() {
        Assume.assumeTrue(shellSessionRooted)
    }

    @Test
    fun shellSession_unroot() {
        Assume.assumeFalse(shellSessionRooted)
    }
}
