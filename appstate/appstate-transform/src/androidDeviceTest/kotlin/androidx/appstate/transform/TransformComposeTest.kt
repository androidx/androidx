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

package androidx.appstate.transform

import androidx.compose.runtime.State
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransformComposeTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testComposableTransform() {
        var state: State<Int>? = null
        composeTestRule.setContent {
            state =
                transform(defaultValue = 0) {
                    val innerState = transform(defaultValue = 10) { 20 }
                    innerState.value
                }
        }
        composeTestRule.waitForIdle()
        assertThat(state!!.value).isEqualTo(20)
    }
}
