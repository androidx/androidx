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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@Suppress("deprecation")
class LaunchWhenTest {
    private val expectations = Expectations()

    @Test
    fun runSynchronously() {
        runBlocking(Dispatchers.Main.immediate) {
            val owner = FakeLifecycleOwner()
            owner.lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
                fun onCreate() {
                    expectations.expect(2)
                }

                @OnLifecycleEvent(Lifecycle.Event.ON_START)
                fun onStart() {
                    expectations.expect(5)
                }

                @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                fun onResume() {
                    expectations.expect(8)
                }
            })
            owner.lifecycleScope.launchWhenCreated {
                expectations.expect(3)
            }
            owner.lifecycleScope.launchWhenStarted {
                expectations.expect(6)
            }
            owner.lifecycleScope.launchWhenResumed {
                expectations.expect(9)
            }
            expectations.expect(1)
            owner.setState(Lifecycle.State.CREATED)
            expectations.expect(4)
            owner.setState(Lifecycle.State.STARTED)
            expectations.expect(7)
            owner.setState(Lifecycle.State.RESUMED)
            expectations.expect(10)
        }
    }
}