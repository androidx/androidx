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
import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.VirtualDisplay
import android.hardware.display.VirtualDisplayConfig
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.xr.projected.TrampolineRequestPermissionsOnHostActivity.Companion.EXTRA_DISMISS_ON_DISCONNECT
import androidx.xr.projected.TrampolineRequestPermissionsOnHostActivity.Companion.PERMISSION_REQUEST_CODE
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.testing.ProjectedTestRule
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDisplayManager
import org.robolectric.shadows.ShadowVirtualDeviceManager
import org.robolectric.shadows.VirtualDeviceParamsBuilder

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalProjectedApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM, Build.VERSION_CODES.BAKLAVA])
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class TrampolineRequestPermissionsOnHostActivityTest {

    @get:Rule val projectedTestRule = ProjectedTestRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        TrampolineRequestPermissionsOnHostActivity.coroutineContext = UnconfinedTestDispatcher()
    }

    @After
    fun tearDown() {
        TrampolineRequestPermissionsOnHostActivity.coroutineContext = EmptyCoroutineContext
    }

    @Test
    fun onCreate_withMissingExtras_finishesActivity() {
        val intent = Intent(context, TrampolineRequestPermissionsOnHostActivity::class.java)
        ActivityScenario.launch<TrampolineRequestPermissionsOnHostActivity>(intent).use { scenario
            ->
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
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
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_CANCELED)
            assertThat(resultReceiver.receivedCount).isEqualTo(1)
        }
    }

    @Test
    fun onCreate_withDismissOnDisconnectExtra_sendsCanceledResultAndFinishes() {
        val permissions = arrayOf("android.permission.CAMERA")
        val resultReceiver = TestResultReceiver()
        val intent =
            Intent(context, TrampolineRequestPermissionsOnHostActivity::class.java).apply {
                putExtra(ProjectedPermissionsConstants.EXTRA_PERMISSIONS, permissions)
                putExtra(ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER, resultReceiver)
                putExtra(EXTRA_DISMISS_ON_DISCONNECT, true)
            }

        val activity =
            Robolectric.buildActivity(
                    TrampolineRequestPermissionsOnHostActivity::class.java,
                    intent,
                )
                .create()
                .get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(shadowOf(activity).lastRequestedPermission).isNull()
        assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(resultReceiver.receivedCount).isEqualTo(1)
    }

    @Test
    fun onCreate_whenProjectedDeviceDisconnected_sendsCanceledResultAndFinishes() {
        projectedTestRule.isDeviceConnected = false
        val permissions = arrayOf("android.permission.CAMERA")
        val resultReceiver = TestResultReceiver()
        val intent =
            Intent(context, TrampolineRequestPermissionsOnHostActivity::class.java).apply {
                putExtra(ProjectedPermissionsConstants.EXTRA_PERMISSIONS, permissions)
                putExtra(ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER, resultReceiver)
            }

        ActivityScenario.launch<TrampolineRequestPermissionsOnHostActivity>(intent).use { scenario
            ->
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_CANCELED)
            assertThat(resultReceiver.receivedCount).isEqualTo(1)
        }
    }

    @Test
    fun onCreate_afterConfigurationChange_doesNotRequestPermissionsAgain() {
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
                assertThat(shadowOf(activity).lastRequestedPermission).isNotNull()
            }

            scenario.recreate()

            scenario.onActivity { activity ->
                assertThat(activity.isFinishing).isFalse()
                assertThat(shadowOf(activity).lastRequestedPermission).isNull()
                activity.onRequestPermissionsResult(
                    PERMISSION_REQUEST_CODE,
                    permissions,
                    grantResults,
                )
                assertThat(activity.isFinishing).isTrue()
            }

            assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_OK)
            assertThat(resultReceiver.receivedCount).isEqualTo(1)
        }
    }

    @Test
    fun onCreate_afterConfigurationChangeWhenDeviceDisconnected_dismissesAndSendsCanceledResult() {
        TrampolineRequestPermissionsOnHostActivity.coroutineContext = StandardTestDispatcher()
        val permissions = arrayOf("android.permission.CAMERA")
        val resultReceiver = TestResultReceiver()
        val intent =
            Intent(context, TrampolineRequestPermissionsOnHostActivity::class.java).apply {
                putExtra(ProjectedPermissionsConstants.EXTRA_PERMISSIONS, permissions)
                putExtra(ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER, resultReceiver)
            }

        ActivityScenario.launch<TrampolineRequestPermissionsOnHostActivity>(intent).use { scenario
            ->
            projectedTestRule.isDeviceConnected = false
            assertThat(resultReceiver.receivedCount).isEqualTo(0)

            scenario.recreate()
            shadowOf(Looper.getMainLooper()).idle()

            assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_CANCELED)
            assertThat(resultReceiver.receivedCount).isEqualTo(1)
            scenario.onActivity { activity ->
                val clearTopIntent = shadowOf(activity).nextStartedActivity
                assertThat(clearTopIntent).isNotNull()
                assertThat(
                        clearTopIntent.flags and
                            (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                    .isEqualTo(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                assertThat(activity.isFinishing).isFalse()
                activity.onNewIntent(clearTopIntent)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun onCreate_afterConfigurationChangeWhenDeviceIdChanged_dismissesAndSendsCanceledResult() {
        TrampolineRequestPermissionsOnHostActivity.coroutineContext = StandardTestDispatcher()
        val permissions = arrayOf("android.permission.CAMERA")
        val resultReceiver = TestResultReceiver()
        val intent =
            Intent(context, TrampolineRequestPermissionsOnHostActivity::class.java).apply {
                putExtra(ProjectedPermissionsConstants.EXTRA_PERMISSIONS, permissions)
                putExtra(ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER, resultReceiver)
            }

        ActivityScenario.launch<TrampolineRequestPermissionsOnHostActivity>(intent).use { scenario
            ->
            // Simulate a new projected virtual device connecting with a different deviceId
            // before the activity is recreated.
            val newerDevice = createVirtualDevice()
            createVirtualDisplayForDevice(newerDevice)
            assertThat(resultReceiver.receivedCount).isEqualTo(0)

            scenario.recreate()
            shadowOf(Looper.getMainLooper()).idle()

            assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_CANCELED)
            assertThat(resultReceiver.receivedCount).isEqualTo(1)
            scenario.onActivity { activity ->
                val clearTopIntent = shadowOf(activity).nextStartedActivity
                assertThat(clearTopIntent).isNotNull()
                assertThat(
                        clearTopIntent.flags and
                            (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                    .isEqualTo(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                assertThat(activity.isFinishing).isFalse()
                activity.onNewIntent(clearTopIntent)
                assertThat(activity.isFinishing).isTrue()
            }
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.BAKLAVA])
    fun onCreate_afterConfigurationChangeWhenResultAlreadySent_finishesImmediately() {
        val permissions = arrayOf("android.permission.CAMERA")
        val resultReceiver = TestResultReceiver()
        val intent =
            Intent(context, TrampolineRequestPermissionsOnHostActivity::class.java).apply {
                putExtra(ProjectedPermissionsConstants.EXTRA_PERMISSIONS, permissions)
                putExtra(ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER, resultReceiver)
            }
        val virtualDeviceManager = context.getSystemService(VirtualDeviceManager::class.java)
        val displayId = virtualDeviceManager.virtualDevices.first().displayIds[0]

        val controller =
            Robolectric.buildActivity(
                    TrampolineRequestPermissionsOnHostActivity::class.java,
                    intent,
                )
                .setup()
        ShadowDisplayManager.removeDisplay(displayId)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(resultReceiver.receivedCount).isEqualTo(1)
        assertThat(controller.get().isFinishing).isFalse()

        val savedState = Bundle()
        controller.saveInstanceState(savedState).pause().stop().destroy()

        val recreatedActivity =
            Robolectric.buildActivity(
                    TrampolineRequestPermissionsOnHostActivity::class.java,
                    intent,
                )
                .create(savedState)
                .get()

        assertThat(recreatedActivity.isFinishing).isTrue()
        assertThat(resultReceiver.receivedCount).isEqualTo(1)
    }

    @Test
    fun onCreate_afterConfigurationChangeWithMissingDeviceIdAndDeviceDisconnected_dismissesAndSendsCanceledResult() {
        projectedTestRule.isDeviceConnected = false
        val permissions = arrayOf("android.permission.CAMERA")
        val resultReceiver = TestResultReceiver()
        val intent =
            Intent(context, TrampolineRequestPermissionsOnHostActivity::class.java).apply {
                putExtra(ProjectedPermissionsConstants.EXTRA_PERMISSIONS, permissions)
                putExtra(ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER, resultReceiver)
            }

        val recreatedActivity =
            Robolectric.buildActivity(
                    TrampolineRequestPermissionsOnHostActivity::class.java,
                    intent,
                )
                .create(Bundle())
                .get()

        assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        assertThat(resultReceiver.receivedCount).isEqualTo(1)
        val clearTopIntent = shadowOf(recreatedActivity).nextStartedActivity
        assertThat(clearTopIntent).isNotNull()
        recreatedActivity.onNewIntent(clearTopIntent)
        assertThat(recreatedActivity.isFinishing).isTrue()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.BAKLAVA])
    fun onProjectedDeviceDisconnected_whilePermissionRequestInFlight_dismissesAndSendsCanceledResult() {
        val permissions = arrayOf("android.permission.CAMERA")
        val resultReceiver = TestResultReceiver()
        val intent =
            Intent(context, TrampolineRequestPermissionsOnHostActivity::class.java).apply {
                putExtra(ProjectedPermissionsConstants.EXTRA_PERMISSIONS, permissions)
                putExtra(ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER, resultReceiver)
            }
        val virtualDeviceManager = context.getSystemService(VirtualDeviceManager::class.java)
        val displayId = virtualDeviceManager.virtualDevices.first().displayIds[0]

        ActivityScenario.launch<TrampolineRequestPermissionsOnHostActivity>(intent).use { scenario
            ->
            ShadowDisplayManager.removeDisplay(displayId)
            shadowOf(Looper.getMainLooper()).idle()

            assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_CANCELED)
            assertThat(resultReceiver.receivedCount).isEqualTo(1)

            scenario.onActivity { activity ->
                val clearTopIntent = shadowOf(activity).nextStartedActivity
                assertThat(clearTopIntent).isNotNull()
                assertThat(clearTopIntent.component?.className)
                    .isEqualTo(TrampolineRequestPermissionsOnHostActivity::class.java.name)
                assertThat(
                        clearTopIntent.flags and
                            (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                    .isEqualTo(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

                assertThat(activity.isFinishing).isFalse()
                activity.onNewIntent(clearTopIntent)
                assertThat(activity.intent).isEqualTo(clearTopIntent)
                assertThat(activity.isFinishing).isTrue()

                // Verify a subsequent canceled permission result from GrantPermissionsActivity is
                // ignored without sending a duplicate result.
                activity.onRequestPermissionsResult(
                    PERMISSION_REQUEST_CODE,
                    emptyArray(),
                    intArrayOf(),
                )
            }
            assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_CANCELED)
            assertThat(resultReceiver.receivedCount).isEqualTo(1)
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
    fun onRequestPermissionsResult_withEmptyPermissions_sendsCanceledResultAndFinishes() {
        val permissions = arrayOf("android.permission.CAMERA")
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
                    emptyArray(),
                    intArrayOf(),
                )
                assertThat(activity.isFinishing).isTrue()
            }

            assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_CANCELED)
            assertThat(resultReceiver.receivedCount).isEqualTo(1)
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

    @Test
    fun onDestroy_withoutSendingResult_sendsCanceledResult() {
        val permissions = arrayOf("android.permission.CAMERA")
        val resultReceiver = TestResultReceiver()
        val intent =
            Intent(context, TrampolineRequestPermissionsOnHostActivity::class.java).apply {
                putExtra(ProjectedPermissionsConstants.EXTRA_PERMISSIONS, permissions)
                putExtra(ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER, resultReceiver)
            }

        ActivityScenario.launch<TrampolineRequestPermissionsOnHostActivity>(intent).use { scenario
            ->
            scenario.moveToState(Lifecycle.State.DESTROYED)
            assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_CANCELED)
            assertThat(resultReceiver.receivedCount).isEqualTo(1)
        }
    }

    @Test
    fun onDestroy_afterSendingResult_doesNotSendCanceledResult() {
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
            }
            scenario.moveToState(Lifecycle.State.DESTROYED)
            assertThat(resultReceiver.resultCode).isEqualTo(Activity.RESULT_OK)
            assertThat(resultReceiver.receivedCount).isEqualTo(1)
        }
    }

    private fun createVirtualDevice(): Any {
        val virtualDeviceManager = context.getSystemService(VirtualDeviceManager::class.java)
        return Shadow.extract<ShadowVirtualDeviceManager>(virtualDeviceManager)
            .createVirtualDevice(
                /* associationId= */ 1,
                VirtualDeviceParamsBuilder.newBuilder()
                    .setName(ProjectedContext.PROJECTED_DEVICE_NAME)
                    .build(),
            )
    }

    private fun createVirtualDisplayForDevice(virtualDevice: Any): VirtualDisplay =
        Shadow.extract<ShadowVirtualDeviceManager.ShadowVirtualDevice>(virtualDevice)
            .createVirtualDisplay(
                VirtualDisplayConfig.Builder(
                        ProjectedContext.PROJECTED_DISPLAY_NAME,
                        /* width= */ 10,
                        /* height= */ 10,
                        /* densityDpi= */ 10,
                    )
                    .build(),
                /* executor= */ null,
                /* callback= */ null,
            )

    private class TestResultReceiver : ResultReceiver(Handler(Looper.getMainLooper())) {
        var resultCode: Int = DEFAULT_RESULT_CODE
        var resultData: Bundle? = null
        var receivedCount: Int = 0

        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            this.resultCode = resultCode
            this.resultData = resultData
            this.receivedCount++
        }
    }

    private companion object {
        const val DEFAULT_RESULT_CODE = Int.MIN_VALUE
        const val WRONG_REQUEST_CODE = 100
    }
}
