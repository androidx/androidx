/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.kotlintestapp.TestActivity
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@LargeTest
@RunWith(AndroidJUnit4::class)
class LifecycleScopeIntegrationTest {
    private val testScope = CoroutineScope(Job() + Dispatchers.Default)

    @JvmField
    @Rule
    val rule = activityScenarioRule<TestActivity>()

    @After
    fun cancelScope() {
        testScope.cancel()
    }

    @Test
    fun alreadyResumed() = runBlocking {
        rule.scenario.moveToState(RESUMED)
        assertThat(owner().lifecycleScope.async {
            true
        }.await()).isTrue()
    }

    @Test
    fun createdState() = runBlocking {
        rule.scenario.moveToState(CREATED)
        assertThat(owner().lifecycleScope.async {
            true
        }.await()).isTrue()
    }

    @Test
    fun startOnMainThread() = runBlocking {
        rule.scenario.moveToState(RESUMED)
        val owner = owner()
        assertThat(
            testScope.async(Dispatchers.Main) {
                withContext(owner.lifecycleScope.coroutineContext) {
                    true
                }
            }.await()
        ).isTrue()
    }

    @Test
    fun alreadyDestroyed() = runBlocking {
        val owner = owner() // grab it before destroying
        rule.scenario.moveToState(DESTROYED)
        val action = owner.lifecycleScope.async {
            true
        }
        action.join()
        assertThat(action.isCancelled).isTrue()
    }

    @Test
    fun destroyedWhileRunning() = runBlocking {
        val owner = owner() // grab it before destroying
        rule.scenario.moveToState(STARTED)
        val runningMutex = Mutex(true)
        val action = owner.lifecycleScope.async {
            runningMutex.unlock()
            delay(10_000)
        }
        runningMutex.lock()
        assertThat(action.isActive).isTrue()
        rule.scenario.moveToState(DESTROYED)
        action.join()
        assertThat(action.isCancelled).isTrue()
    }

    private fun owner(): LifecycleOwner {
        lateinit var owner: LifecycleOwner
        rule.scenario.onActivity {
            owner = it
        }
        return owner
    }
}