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

package androidx.wear.compose.foundation.lazy

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnStateTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun testInitialState() {
        lateinit var state: TransformingLazyColumnState
        rule.setContent { state = rememberTransformingLazyColumnState() }
        assertThat(state.anchorItemIndex).isEqualTo(0)
        assertThat(state.anchorItemScrollOffset).isEqualTo(0)
    }

    @Test
    fun testInitialScrollPosition() {
        lateinit var state: TransformingLazyColumnState
        rule.setContent {
            state =
                rememberTransformingLazyColumnState(
                    initialAnchorItemIndex = 10,
                    initialAnchorItemScrollOffset = 20
                )
        }
        assertThat(state.anchorItemIndex).isEqualTo(10)
        assertThat(state.anchorItemScrollOffset).isEqualTo(20)
    }
}
