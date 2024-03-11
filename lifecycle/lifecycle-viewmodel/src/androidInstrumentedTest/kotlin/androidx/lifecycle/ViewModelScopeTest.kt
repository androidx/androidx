/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.lifecycle

import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ViewModelScopeTest {

    private val testScope = TestScope()

    //region viewModelScope with default scope
    @Test
    fun viewModelScope_withDefaultScope_whenLaunch_cancelsOnClear() {
        val viewModel = ViewModel1()

        val job1 = viewModel.viewModelScope.launch { delay(1.seconds) }
        val job2 = viewModel.viewModelScope.launch { delay(1.seconds) }
        viewModel.clear()

        assertThat(job1.isCancelled).isTrue()
        assertThat(job2.isCancelled).isTrue()
    }

    @Test
    fun viewModelScope_withDefaultScope_afterClear_launchesCancelledJob() {
        val viewModel = ViewModel1()

        viewModel.clear()
        val job1 = viewModel.viewModelScope.launch { delay(1.seconds) }

        assertThat(job1.isCancelled).isTrue()
    }

    @Test
    fun viewModelScope_withDefaultScope_afterClear_returnsSameScope() {
        val viewModel = ViewModel1()

        val scopeBeforeClear = viewModel.viewModelScope
        viewModel.clear()
        val scopeAfterClear = viewModel.viewModelScope

        assertThat(scopeBeforeClear).isSameInstanceAs(scopeAfterClear)
    }

    @Test
    fun viewModelScope_defaultScope_launchesSupervisedJobs() {
        testScope.runTest {
            val viewModel = ViewModel1()

            val delayingDeferred = viewModel.viewModelScope.async { delay(Long.MAX_VALUE) }
            val failingDeferred = viewModel.viewModelScope.async { throw Error() }
            runCatching { failingDeferred.await() }

            assertThat(delayingDeferred.isActive).isTrue()
            delayingDeferred.cancelAndJoin()
        }
    }
    //endregion

    //region viewModelScope with custom scope
    @Test
    fun viewModelScope_withCustomScope_whenLaunch_cancelsOnClear() {
        val viewModel = ViewModel2(viewModelScope = testScope.backgroundScope)

        val job1 = viewModel.viewModelScope.launch { delay(1.seconds) }
        val job2 = viewModel.viewModelScope.launch { delay(1.seconds) }
        viewModel.clear()

        assertThat(job1.isCancelled).isTrue()
        assertThat(job2.isCancelled).isTrue()
    }

    @Test
    fun viewModelScope_withCustomScope_afterClear_launchesCancelledJob() {
        val viewModel = ViewModel2(viewModelScope = testScope.backgroundScope)

        viewModel.clear()
        val job1 = viewModel.viewModelScope.launch { delay(1.seconds) }

        assertThat(job1.isCancelled).isTrue()
    }

    @Test
    fun viewModelScope_withCustomScope_afterClear_returnsSameScope() {
        val viewModel = ViewModel2(viewModelScope = testScope.backgroundScope)

        val scopeBeforeClear = viewModel.viewModelScope
        viewModel.clear()
        val scopeAfterClear = viewModel.viewModelScope

        assertThat(scopeBeforeClear).isSameInstanceAs(scopeAfterClear)
        assertThat(scopeAfterClear.coroutineContext)
            .isSameInstanceAs(testScope.backgroundScope.coroutineContext)
    }
    //endregion

    private class ViewModel1() : ViewModel()

    private class ViewModel2(viewModelScope: CoroutineScope) : ViewModel(viewModelScope)
}
