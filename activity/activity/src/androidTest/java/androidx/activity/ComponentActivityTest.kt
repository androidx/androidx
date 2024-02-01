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

package androidx.activity

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityTest {

    @Test
    fun accessOnBackPressedDispatcher() {
        withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            val dispatcher = withActivity {
                // Access the OnBackPressedDispatcher directly
                onBackPressedDispatcher
            }
            assertThat(dispatcher).isNotNull()
        }
    }

    @Test
    fun accessOnBackPressedDispatcherBackgroundThread() {
        withUse(ActivityScenario.launch(ComponentActivity::class.java)) {
            val activity = withActivity { this }
            // Access the OnBackPressedDispatcher on the test thread
            val dispatcher = activity.onBackPressedDispatcher
            assertThat(dispatcher).isNotNull()
        }
    }

    @Test
    fun earlyAccessOnBackPressedDispatcher() {
        withUse(ActivityScenario.launch(EarlyDispatcherAccessComponentActivity::class.java)) {
            val dispatcher = withActivity {
                // Access the OnBackPressedDispatcher directly
                onBackPressedDispatcher
            }
            assertThat(dispatcher).isNotNull()
        }
    }
}

class EarlyDispatcherAccessComponentActivity : ComponentActivity() {

    init {
        // Access the OnBackPressedDispatcher before the activity
        // has reached the CREATED state
        onBackPressedDispatcher
    }
}
