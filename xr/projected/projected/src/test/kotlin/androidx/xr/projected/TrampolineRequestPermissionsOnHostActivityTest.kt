/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.projected

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.testing.ProjectedTestRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalProjectedApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class TrampolineRequestPermissionsOnHostActivityTest {

    @get:Rule val projectedTestRule = ProjectedTestRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun onCreate_withMissingExtras_finishesActivity() {
        val intent = Intent(context, TrampolineRequestPermissionsOnHostActivity::class.java)
        ActivityScenario.launch<TrampolineRequestPermissionsOnHostActivity>(intent).use { scenario
            ->
            if (scenario.state != Lifecycle.State.DESTROYED) {
                scenario.onActivity { activity -> assertThat(activity.isFinishing).isTrue() }
            }
        }
    }

    @Test
    fun onCreate_withMissingPermissions_finishesActivity() {
        val resultReceiver = TestResultReceiver()
        val intent =
            Intent(context, TrampolineRequestPermissionsOnHostActivity::class.java).apply {
                putExtra(ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER, resultReceiver)
            }
        ActivityScenario.launch<TrampolineRequestPermissionsOnHostActivity>(intent).use { scenario
            ->
            if (scenario.state != Lifecycle.State.DESTROYED) {
                scenario.onActivity { activity -> assertThat(activity.isFinishing).isTrue() }
            }
            assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        }
    }

    @Test
    fun onRequestPermissionsResult_sendsResultAndFinishes() {
        val permissions = arrayOf("android.permission.CAMERA")
        val grantResults = intArrayOf(PackageManager.PERMISSION_GRANTED)
        val resultReceiver = TestResultReceiver()
        val intent =
            Intent(context, TrampolineRequestPermissionsOnHostActivity::class.java).apply {
                putExtra(ProjectedPermissionsConstants.EXTRA_PERMISSIONS, permissions)
                putExtra(ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER, resultReceiver)
            }

        ActivityScenario.launch<TrampolineRequestPermissionsOnHostActivity>(intent).use { scenario
            ->
            scenario.onActivity { activity ->
                activity.onRequestPermissionsResult(
                    PERMISSION_REQUEST_CODE,
                    permissions,
                    grantResults,
                )
                assertThat(activity.isFinishing).isTrue()
            }

            assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_OK)
            val resultData = resultReceiver.resultData!!
            assertThat(
                    resultData.getStringArray(ProjectedPermissionsConstants.RESULT_DATA_PERMISSIONS)
                )
                .isEqualTo(permissions)
            assertThat(
                    resultData.getIntArray(ProjectedPermissionsConstants.RESULT_DATA_GRANT_RESULTS)
                )
                .isEqualTo(grantResults)
        }
    }

    @Test
    fun onRequestPermissionsResult_withWrongRequestCode_doesNothing() {
        val permissions = arrayOf("android.permission.CAMERA")
        val grantResults = intArrayOf(PackageManager.PERMISSION_GRANTED)
        val resultReceiver = TestResultReceiver()
        val intent =
            Intent(context, TrampolineRequestPermissionsOnHostActivity::class.java).apply {
                putExtra(ProjectedPermissionsConstants.EXTRA_PERMISSIONS, permissions)
                putExtra(ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER, resultReceiver)
            }

        ActivityScenario.launch<TrampolineRequestPermissionsOnHostActivity>(intent).use { scenario
            ->
            scenario.onActivity { activity ->
                activity.onRequestPermissionsResult(WRONG_REQUEST_CODE, permissions, grantResults)
                assertThat(activity.isFinishing).isFalse()
            }
            shadowOf(Looper.getMainLooper()).idle()

            assertThat(resultReceiver.resultCode).isEqualTo(DEFAULT_RESULT_CODE)
            assertThat(scenario.state).isNotEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    private class TestResultReceiver : ResultReceiver(Handler(Looper.getMainLooper())) {
        var resultCode: Int = DEFAULT_RESULT_CODE
        var resultData: Bundle? = null

        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            this.resultCode = resultCode
            this.resultData = resultData
        }
    }

    private companion object {
        const val DEFAULT_RESULT_CODE = Int.MIN_VALUE
        const val PERMISSION_REQUEST_CODE = 105
        const val WRONG_REQUEST_CODE = 100
    }
}
