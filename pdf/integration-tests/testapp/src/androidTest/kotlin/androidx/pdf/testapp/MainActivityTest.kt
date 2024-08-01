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

package androidx.pdf.testapp

import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.android.material.button.MaterialButton
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*

@Suppress("UNCHECKED_CAST")
@SmallTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testActivityInitialization() {
        launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)

            Assert.assertEquals(Lifecycle.State.CREATED, scenario.state)
        }
    }

    @Test
    fun testGetContentButtonClickLaunchesFilePicker() {
        val mockFilePicker: ActivityResultLauncher<String> =
            mock(ActivityResultLauncher::class.java) as ActivityResultLauncher<String>

        launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity { activity ->
                activity.filePicker = mockFilePicker
                val getContentButton: MaterialButton = activity.findViewById(R.id.launch_button)

                Assert.assertNotNull(getContentButton)
                getContentButton.performClick()
                verify(mockFilePicker).launch("application/pdf")
            }
        }
    }
}
