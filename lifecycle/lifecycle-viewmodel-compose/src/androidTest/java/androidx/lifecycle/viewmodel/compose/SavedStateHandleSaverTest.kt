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

package androidx.lifecycle.viewmodel.compose

import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SavedStateHandleSaverTest {

    @get:Rule
    public val activityTestRuleScenario: ActivityScenarioRule<AppCompatActivity> =
        ActivityScenarioRule(AppCompatActivity::class.java)

    @OptIn(SavedStateHandleSaveableApi::class)
    @Test
    fun simpleRestore() {
        var array: IntArray? = null
        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                array = viewModel.savedStateHandle.saveable<IntArray>("key") { intArrayOf(0) }
            }
        }

        assertThat(array).isEqualTo(intArrayOf(0))

        activityTestRuleScenario.scenario.onActivity {
            array!![0] = 1
            // we null it to ensure recomposition happened
            array = null
        }

        activityTestRuleScenario.scenario.recreate()

        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                array = viewModel.savedStateHandle.saveable<IntArray>("key") { intArrayOf(0) }
            }
        }

        assertThat(array).isEqualTo(intArrayOf(1))
    }
}

class SavingTestViewModel(val savedStateHandle: SavedStateHandle) : ViewModel()
