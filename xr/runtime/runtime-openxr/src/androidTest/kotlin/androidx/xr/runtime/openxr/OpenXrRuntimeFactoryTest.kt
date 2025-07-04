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

package androidx.xr.runtime.openxr

// import android.content.Context
// import androidx.test.core.app.ApplicationProvider
// import androidx.xr.runtime.FEATURE_XR_API_OPENXR
// import org.robolectric.Shadows.shadowOf
// import org.robolectric.shadows.ShadowBuild

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO - b/382119583: Remove the @SdkSuppress annotation once "androidx.xr.runtime.openxr.test"
// supports a
// lower SDK version.
@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenXrRuntimeFactoryTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    @Ignore("Source utilizes robolectric which does not work with androidTests.")
    @Test
    fun createRuntime_createsOpenXrRuntime() {
        // ShadowBuild.setFingerprint("a_fake_openxr_device")
        // val context: Context = ApplicationProvider.getApplicationContext()
        // shadowOf(context.packageManager).setSystemFeature(FEATURE_XR_API_OPENXR, /* supported= */
        // true)

        activityRule.scenario.onActivity {
            assertThat((Session.create(it) as SessionCreateSuccess).session.runtime)
                .isInstanceOf(OpenXrRuntime::class.java)
        }
    }
}
