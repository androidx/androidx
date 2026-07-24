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

package androidx.compose.testutils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ParameterizedComposeTestRuleTest {
    @get:Rule val composeTestRule = createParameterizedComposeTestRule<Param>()

    @Test
    fun assertionErrorInParameterIsPropagated() {
        val paramList = listOf(Param("first"), Param("second"))
        composeTestRule.setContent { Box(modifier = Modifier.size(10.dp)) }

        val error =
            kotlin.runCatching {
                composeTestRule.forEachParameter(paramList) {
                    if (it.singleParam == "first") {
                        throw AssertionError()
                    }
                }
            }

        assertTrue(error.exceptionOrNull()?.localizedMessage?.contains("Error on Config") == true)
    }

    @Test(expected = IllegalStateException::class)
    fun setContentCannotBeCalledTwice() {
        composeTestRule.setContent { Box(modifier = Modifier.size(10.dp)) }
        composeTestRule.setContent { Box(modifier = Modifier.size(10.dp)) }
    }

    @Test(expected = IllegalStateException::class)
    fun forEachParameterNeedsAtLeastOneParameter() {
        composeTestRule.forEachParameter(emptyList()) {}
    }

    @Test
    fun forEachParameterRunsCompositionForEachParameter() {
        val paramList = listOf(Param("first"), Param("second"))
        var recompositionCount = 0
        composeTestRule.setContent { recompositionCount++ }
        composeTestRule.forEachParameter(paramList) {}
        assertTrue(recompositionCount == paramList.size)
    }

    @Test
    fun forEachParameterPropagatesParameterToContentAndRunBlock() {
        val paramList = listOf(Param("first"), Param("second"))
        var index = 0
        composeTestRule.setContent { assertTrue(it.singleParam == paramList[index].singleParam) }

        composeTestRule.forEachParameter(paramList) {
            assertTrue(it.singleParam == paramList[index].singleParam)
            index++
        }
    }

    data class Param(var singleParam: String)
}
