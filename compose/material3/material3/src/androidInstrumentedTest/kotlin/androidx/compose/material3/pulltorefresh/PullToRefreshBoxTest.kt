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

package androidx.compose.material3.pulltorefresh

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class PullToRefreshBoxTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun box_startRefreshing_updatesFraction() {
        val state = PullToRefreshState()
        rule.setContent { PullToRefreshBox(isRefreshing = true, state = state, onRefresh = {}) {} }

        assertThat(state.distanceFraction).isEqualTo(1f)
    }

    @Test
    fun box_startNotRefreshing_updatesFraction() {
        val state = PullToRefreshState()
        rule.setContent { PullToRefreshBox(isRefreshing = false, state = state, onRefresh = {}) {} }

        assertThat(state.distanceFraction).isEqualTo(0f)
    }

    @Test
    fun startRefreshing_pull_isNoop() {
        val state = PullToRefreshState()
        rule.setContent {
            PullToRefreshBox(isRefreshing = true, state = state, onRefresh = {}) {
                LazyColumn(Modifier.testTag("lazy")) { items(50) { Text("Item") } }
            }
        }

        rule.onNodeWithTag("lazy").performTouchInput { swipeDown(startY = 0f, endY = 600f, 700L) }
        rule.runOnIdle { assertThat(state.distanceFraction).isEqualTo(1f) }
    }

    @Test
    fun startIdle_pull_triggersRefresh() {
        val state = PullToRefreshState()
        val isRefreshing = mutableStateOf(false)

        rule.setContent {
            PullToRefreshBox(
                isRefreshing = isRefreshing.value,
                state = state,
                onRefresh = { isRefreshing.value = true }
            ) {
                LazyColumn(Modifier.testTag("lazy")) { items(50) { Text("Item") } }
            }
        }

        rule.runOnIdle { assertThat(state.distanceFraction).isEqualTo(0f) }

        rule.onNodeWithTag("lazy").performTouchInput { swipeDown(startY = 0f, endY = 600f, 700L) }

        rule.runOnIdle { assertThat(state.distanceFraction).isEqualTo(1f) }
    }
}
