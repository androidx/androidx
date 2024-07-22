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

package androidx.lifecycle.viewmodel.testing

import android.os.Bundle
import androidx.kruth.assertThat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
internal class ViewModelScenarioInstrumentedTest {

    @Test
    fun createSavedStateHandle_withDefaultExtras() {
        val scenario = viewModelScenario { TestViewModel(createSavedStateHandle()) }

        // assert `.viewModel` does not throw.
        assertThat(scenario.viewModel.handle).isNotNull()
    }

    @Test
    fun createSavedStateHandle_withInitialExtras() {
        val defaultArgs = Bundle().apply { putString("key", "value") }
        val creationExtras = DefaultCreationExtras(defaultArgs)
        val scenario = viewModelScenario(creationExtras) { TestViewModel(createSavedStateHandle()) }

        assertThat(scenario.viewModel.handle.get<String>("key")).isEqualTo("value")
    }

    private class TestViewModel(val handle: SavedStateHandle) : ViewModel()
}
