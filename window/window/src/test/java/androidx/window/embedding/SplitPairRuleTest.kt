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

package androidx.window.embedding

import androidx.window.core.ActivityComponentInfo
import org.junit.Assert.assertEquals
import org.junit.Test

internal class SplitPairRuleTest {

    @Test
    fun test_builderMatchesConstruction() {
        val splitAttributes = SplitAttributes()
        val filterSet = setOf(createSplitPairFilter())
        val expected = SplitPairRule(
            filterSet,
            splitAttributes
        )

        val actual = SplitPairRule.Builder(filterSet).build()

        assertEquals(expected, actual)
    }

    private fun createSplitPairFilter(): SplitPairFilter {
        return SplitPairFilter(
            ActivityComponentInfo("package", "class"),
            ActivityComponentInfo("otherPackage", "otherClass"),
            null
        )
    }
}