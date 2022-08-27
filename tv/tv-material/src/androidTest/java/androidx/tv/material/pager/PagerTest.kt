/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.tv.material.pager

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.tv.material.ExperimentalTvMaterialApi
import org.junit.Rule
import org.junit.Test

class PagerTest {
    @get:Rule
    val rule = createComposeRule()

    @OptIn(ExperimentalTvMaterialApi::class)
    @Test
    fun pager_zeroSlideCount_drawsSomething() {
        val testTag = "pager"
        rule.setContent {
            Pager(slideCount = 0, modifier = Modifier.testTag(testTag)) {}
        }

        rule.onNodeWithTag(testTag).assertExists()
    }
}