/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.fragment.app.testing

import androidx.lifecycle.Lifecycle.State
import androidx.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * An example test with FragmentScenario in Kotlin.
 */
@RunWith(AndroidJUnit4::class)
class FragmentScenarioKotlinTest {
    @Test
    fun testFragmentLifecycle_withFragmentScenario() {
        val scenario = launchFragmentInContainer<StateRecordingFragment>()
        scenario.onFragment {
            assertThat(it.numberOfRecreations).isEqualTo(0)
            assertThat(it.state).isEqualTo(State.RESUMED)
            assertThat(it.isViewAttachedToWindow).isTrue()
        }

        scenario.recreate()
        scenario.onFragment {
            assertThat(it.numberOfRecreations).isEqualTo(1)
            assertThat(it.state).isEqualTo(State.RESUMED)
        }

        scenario.moveToState(State.STARTED)
        scenario.onFragment {
            assertThat(it.numberOfRecreations).isEqualTo(1)
            assertThat(it.state).isEqualTo(State.STARTED)
        }

        scenario.moveToState(State.CREATED)
        scenario.onFragment {
            assertThat(it.numberOfRecreations).isEqualTo(1)
            assertThat(it.state).isEqualTo(State.CREATED)
        }

        scenario.moveToState(State.DESTROYED)
    }

    @Test
    fun testlaunch_withFragmentScenario() {
        val scenario = launchFragment<StateRecordingFragment>()
        scenario.onFragment {
            assertThat(it.state).isEqualTo(State.RESUMED)
        }
    }

    @Test
    fun testlaunchInContainer_withInstantiate() {
        var numberOfInstantiations = 0
        val scenario = launchFragmentInContainer(null) {
            numberOfInstantiations++
            NoDefaultConstructorFragment("my constructor arg")
        }
        assertThat(numberOfInstantiations).isEqualTo(1)
        scenario.onFragment {
            assertThat(it.constructorArg).isEqualTo("my constructor arg")
            assertThat(it.numberOfRecreations).isEqualTo(0)
            assertThat(it.state).isEqualTo(State.RESUMED)
            assertThat(it.isViewAttachedToWindow).isTrue()
        }
    }

    @Test
    fun testlaunch_withInstantiate() {
        var numberOfInstantiations = 0
        val scenario = launchFragment(null) {
            numberOfInstantiations++
            NoDefaultConstructorFragment("my constructor arg")
        }
        assertThat(numberOfInstantiations).isEqualTo(1)
        scenario.onFragment {
            assertThat(it.constructorArg).isEqualTo("my constructor arg")
            assertThat(it.state).isEqualTo(State.RESUMED)
        }
    }
}
