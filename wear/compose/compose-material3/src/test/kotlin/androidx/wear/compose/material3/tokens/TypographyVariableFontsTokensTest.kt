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

package androidx.wear.compose.material3.tokens

import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TypographyVariableFontsTokensTest {

    @Test
    fun everyToken_listsAxesInCanonicalOrder() {
        for ((name, settings) in allTokens) {
            val axes = settings.settings.map { it.axisName }
            assertEquals(
                "$name must list its axes as \"wght\", \"wdth\", then any other axes",
                axes.sortedBy { axisRank(it) },
                axes,
            )
        }
    }

    @Test
    fun tokensWithSameAxisValues_areEqual() {
        // FontVariation.Settings equality is order-sensitive and is used as a key in Compose's
        // typeface caches, so tokens with the same variations must compare equal to share a
        // typeface.
        allTokens
            .groupBy { (_, settings) -> settings.axisValues() }
            .values
            .filter { it.size > 1 }
            .forEach { tokens ->
                val (firstTokenName, firstTokenSettings) = tokens.first()
                for ((tokenName, tokenSettings) in tokens.drop(1)) {
                    assertEquals(
                        "$tokenName should equal $firstTokenName",
                        firstTokenSettings,
                        tokenSettings,
                    )
                    assertEquals(
                        "$tokenName and $firstTokenName should have the same hashCode",
                        firstTokenSettings.hashCode(),
                        tokenSettings.hashCode(),
                    )
                }
            }
    }

    private fun axisRank(axisName: String): Int =
        when (axisName) {
            "wght" -> 0
            "wdth" -> 1
            else -> 2
        }

    private fun FontVariation.Settings.axisValues(): Set<Pair<String, Float>> =
        settings.map { it.axisName to it.toVariationValue(Density(1f)) }.toSet()

    /** All [FontVariation.Settings] tokens, found via reflection so new tokens are covered too. */
    private val allTokens: List<Pair<String, FontVariation.Settings>> by lazy {
        val tokens =
            TypographyVariableFontsTokens::class
                .java
                .methods
                .filter {
                    it.parameterCount == 0 && it.returnType == FontVariation.Settings::class.java
                }
                .map { it.name.removePrefix("get") to it.invoke(TypographyVariableFontsTokens) }
                .map { (name, value) -> name to value as FontVariation.Settings }
                .sortedBy { it.first }
        assertTrue("No FontVariation.Settings tokens found", tokens.isNotEmpty())
        tokens
    }
}
