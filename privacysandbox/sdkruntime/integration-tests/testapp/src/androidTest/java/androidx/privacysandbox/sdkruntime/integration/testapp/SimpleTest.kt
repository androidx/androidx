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

import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkInfo
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SimpleTest {

    @get:Rule val activityScenarioRule = ActivityScenarioRule(TestMainActivity::class.java)

    @After
    fun tearDown() {
        activityScenarioRule.withActivity { api.unloadAllSdks() }
    }

    @Test
    fun simpleTest() {
        activityScenarioRule.withActivity {
            val testSdkApi = runBlocking { api.loadTestSdk() }
            val apiResult = testSdkApi.invert(false)
            assertThat(apiResult).isTrue()
        }
    }

    private fun TestAppApi.unloadAllSdks() {
        getSandboxedSdks()
            .mapNotNull(SandboxedSdkCompat::getSdkInfo)
            .map(SandboxedSdkInfo::name)
            .forEach(::unloadSdk)
    }
}
