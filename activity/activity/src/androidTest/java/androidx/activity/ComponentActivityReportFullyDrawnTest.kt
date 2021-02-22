/*
 * Copyright 2020 The Android Open Source Project
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
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComponentActivityReportFullyDrawnTest {

    @Test
    fun testReportFullyDrawn() {
        with(ActivityScenario.launch(ReportFullyDrawnActivity::class.java)) {
            withActivity {
                // This test makes sure that this method does not throw an exception on devices
                // running API 19 (without UPDATE_DEVICE_STATS permission) and earlier
                // (regardless or permissions).
                reportFullyDrawn()
            }
        }
    }
}

class ReportFullyDrawnActivity : ComponentActivity()
