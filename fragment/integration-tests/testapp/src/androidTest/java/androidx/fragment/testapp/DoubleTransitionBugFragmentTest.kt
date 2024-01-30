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

package androidx.fragment.testapp

import androidx.fragment.app.testing.launchFragment
import androidx.fragment.testapp.doubleTransitionBug.DoubleTransitionBugFragment
import androidx.lifecycle.Lifecycle.State
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for {@link DoubleTransitionBugFragment}.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DoubleTransitionBugFragmentTest {
    @Test
    fun launchFragment() {
        with(launchFragment<DoubleTransitionBugFragment>()) {
            onFragment { fragment ->
                assertThat(fragment.lifecycle.currentState).isEqualTo(State.RESUMED)
            }
        }
    }
}
