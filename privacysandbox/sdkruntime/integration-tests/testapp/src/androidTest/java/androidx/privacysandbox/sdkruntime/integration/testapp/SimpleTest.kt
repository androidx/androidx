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

package androidx.privacysandbox.sdkruntime.integration.testapp

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SimpleTest {

    @Before
    fun setUp() {
        // TODO (b/305232796): Replace with tradefed preparer in config or rule
        val uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation()
        uiAutomation.executeShellCommand(
            "cmd sdk_sandbox set-state --enabled"
        )
        uiAutomation.executeShellCommand(
            "device_config set_sync_disabled_for_tests persistent"
        )
    }

    @After
    fun tearDown() {
        // TODO (b/305232796): Replace with tradefed preparer in config or rule
        val uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation()
        uiAutomation.executeShellCommand(
            "device_config set_sync_disabled_for_tests none"
        )
        uiAutomation.executeShellCommand(
            "cmd sdk_sandbox set-state --reset"
        )
    }

    @Test
    fun simpleTest() {
        with(ActivityScenario.launch(TestMainActivity::class.java)) {
            withActivity {
                val api = waitForSdkApiLoaded()
                val apiResult = api.invert(false)
                assertThat(apiResult).isTrue()
            }
        }
    }

    @Ignore("Testing CI integration, will be deleted later")
    @Test
    fun failTest() {
        // Testing CI integration, will be deleted later
        assertThat(true).isFalse()
    }
}
