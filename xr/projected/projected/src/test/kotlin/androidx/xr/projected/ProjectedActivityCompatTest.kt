/*
 * Copyright 2025 The Android Open Source Project
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
import android.app.Application
import android.app.PendingIntent
import android.companion.virtual.VirtualDeviceManager
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.xr.projected.ProjectedInputEvent.ProjectedInputAction
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.platform.IProjectedPermissionRequestCallback
import androidx.xr.projected.platform.IProjectedService
import androidx.xr.projected.platform.ProjectedPermissionRequestState
import androidx.xr.projected.testing.ProjectedTestRule
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@Suppress("DEPRECATION")
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalProjectedApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class ProjectedActivityCompatTest {

    @get:Rule() val projectedTestRule = ProjectedTestRule()
    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun create_withContext_returnsProjectedActivityCompatInstance() = runBlocking {
        val projectedActivityCompat = ProjectedActivityCompat.create(context)

        assertThat(projectedActivityCompat).isNotNull()
    }

    @Test
    fun create_withContext_throwsIllegalStateException() {
        projectedTestRule.shouldThrowIllegalStateExceptionWhenCreatingControllers = true

        assertFailsWith<IllegalStateException> {
            runBlocking { ProjectedActivityCompat.create(context) }
        }
    }

    @Test
    fun create_withProjectedActivity_returnsProjectedActivityCompatInstance() =
        projectedTestRule.launchTestProjectedDeviceActivity { activity ->
            runBlocking {
                val projectedActivityCompat = ProjectedActivityCompat.create(activity)

                assertThat(projectedActivityCompat).isNotNull()
            }
        }

    @Test
    fun create_withNonProjectedActivity_throwsIllegalArgumentException() =
        launchTestActivity { activity ->
            assertFailsWith<IllegalArgumentException> {
                runBlocking { ProjectedActivityCompat.create(activity) }
            }
        }

    @Test
    fun create_withProjectedActivity_throwsIllegalStateException() =
        projectedTestRule.launchTestProjectedDeviceActivity { activity ->
            projectedTestRule.shouldThrowIllegalStateExceptionWhenCreatingControllers = true

            assertFailsWith<IllegalStateException> {
                runBlocking { ProjectedActivityCompat.create(activity) }
            }
        }

    @Test
    fun projectedInputEvents_emitsProjectedInputEvent() =
        runTest(UnconfinedTestDispatcher()) {
            val projectedActivityCompat = ProjectedActivityCompat.create(context)

            launch {
                val receivedInputEvent = projectedActivityCompat.projectedInputEvents.first()
                assertThat(receivedInputEvent.inputAction)
                    .isEqualTo(ProjectedInputAction.TOGGLE_APP_CAMERA)
            }

            projectedTestRule.sendProjectedInputEvent(ProjectedInputAction.TOGGLE_APP_CAMERA)
        }

    @Test
    fun projectedInputEvents_flowIsClosed_afterCloseCalled() =
        runTest(UnconfinedTestDispatcher()) {
            val projectedActivityCompat = ProjectedActivityCompat.create(context)
            var isFlowClosed = false
            val job =
                backgroundScope.launch {
                    try {
                        projectedActivityCompat.projectedInputEvents.collect { /* Do nothing */ }
                    } finally {
                        isFlowClosed = true
                    }
                }

            projectedActivityCompat.close()
            job.join()

            assertThat(isFlowClosed).isTrue()
        }

    @Test
    fun projectedInputAction_fromCode_returnsCorrectEnum() {
        val action = ProjectedInputAction.fromCode(ProjectedInputAction.TOGGLE_APP_CAMERA.code)

        assertThat(action).isEqualTo(ProjectedInputAction.TOGGLE_APP_CAMERA)
    }

    @Test
    fun projectedInputAction_fromCode_withInvalidCode_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            ProjectedInputAction.fromCode(INVALID_PROJECTED_ACTION_CODE)
        }
    }

    @Test
    fun requestPermissions_launchesProjectedPermissionRequest() =
        launchTestProjectedPermissionActivity { activity ->
            val permissions = arrayOf("android.permission.CAMERA")
            val requestCode = 123

            ProjectedActivityCompat.requestPermissions(activity, permissions, requestCode)

            val mockProjectedService =
                ReflectionHelpers.getField<IProjectedService>(
                    projectedTestRule,
                    "mockProjectedService",
                )
            verify(mockProjectedService).launchProjectedPermissionRequest(any(), any())
        }

    @Test
    fun requestPermissions_withNonProjectedActivity_throwsIllegalArgumentException() =
        launchTestActivity { activity ->
            assertFailsWith<IllegalArgumentException> {
                ProjectedActivityCompat.requestPermissions(activity, emptyArray(), 0)
            }
        }

    @Test
    fun requestPermissions_onDenied_callsOnRequestPermissionsResult() =
        launchTestProjectedPermissionActivity { activity ->
            val permissions = arrayOf("android.permission.CAMERA")
            val requestCode = 123

            val mockProjectedService =
                ReflectionHelpers.getField<IProjectedService>(
                    projectedTestRule,
                    "mockProjectedService",
                )
            val callbackCaptor = argumentCaptor<IProjectedPermissionRequestCallback>()

            ProjectedActivityCompat.requestPermissions(activity, permissions, requestCode)

            verify(mockProjectedService)
                .launchProjectedPermissionRequest(any(), callbackCaptor.capture())

            callbackCaptor.firstValue.onProjectedPermissionRequestStateChanged(
                ProjectedPermissionRequestState.DENIED,
                null,
            )

            assertThat(activity.onRequestPermissionsResultCalled).isTrue()
            assertThat(activity.lastRequestCode).isEqualTo(requestCode)
            assertThat(activity.lastPermissions).isEqualTo(permissions)
            assertThat(activity.lastGrantResults)
                .isEqualTo(intArrayOf(PackageManager.PERMISSION_DENIED))
            assertThat(activity.lastDeviceId).isEqualTo(activity.deviceId)
        }

    @Test
    fun requestPermissions_onAllowed_startsIntentSenderAndActivity() =
        launchTestProjectedPermissionActivity { activity ->
            val permissions = arrayOf("android.permission.CAMERA")
            val requestCode = 123
            val pendingIntent = mock<PendingIntent>()
            val intentSender = mock<IntentSender>()
            whenever(pendingIntent.intentSender).thenReturn(intentSender)

            val mockProjectedService =
                ReflectionHelpers.getField<IProjectedService>(
                    projectedTestRule,
                    "mockProjectedService",
                )
            val callbackCaptor = argumentCaptor<IProjectedPermissionRequestCallback>()

            ProjectedActivityCompat.requestPermissions(activity, permissions, requestCode)

            verify(mockProjectedService)
                .launchProjectedPermissionRequest(any(), callbackCaptor.capture())

            callbackCaptor.firstValue.onProjectedPermissionRequestStateChanged(
                ProjectedPermissionRequestState.ALLOWED,
                pendingIntent,
            )

            assertThat(activity.startIntentSenderForResultCalled).isTrue()
            assertThat(activity.lastIntentSender).isEqualTo(intentSender)

            val startedIntent = shadowOf(activity).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.hasExtra(ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER))
                .isTrue()
            assertThat(
                    startedIntent.getStringArrayExtra(
                        ProjectedPermissionsConstants.EXTRA_PERMISSIONS
                    )
                )
                .isEqualTo(permissions)
        }

    @Test
    fun requestPermissions_onReceiveResult_callsOnRequestPermissionsResult() =
        launchTestProjectedPermissionActivity { activity ->
            val permissions = arrayOf("android.permission.CAMERA")
            val grantResults = intArrayOf(PackageManager.PERMISSION_GRANTED)
            val requestCode = 123

            val mockProjectedService =
                ReflectionHelpers.getField<IProjectedService>(
                    projectedTestRule,
                    "mockProjectedService",
                )
            val callbackCaptor = argumentCaptor<IProjectedPermissionRequestCallback>()

            ProjectedActivityCompat.requestPermissions(activity, permissions, requestCode)

            verify(mockProjectedService)
                .launchProjectedPermissionRequest(any(), callbackCaptor.capture())

            val pendingIntent = mock<PendingIntent>()
            val intentSender = mock<IntentSender>()
            whenever(pendingIntent.intentSender).thenReturn(intentSender)

            callbackCaptor.firstValue.onProjectedPermissionRequestStateChanged(
                ProjectedPermissionRequestState.ALLOWED,
                pendingIntent,
            )

            val startedIntent = shadowOf(activity).nextStartedActivity
            val resultReceiver =
                startedIntent.getParcelableExtra<ResultReceiver>(
                    ProjectedPermissionsConstants.EXTRA_RESULT_RECEIVER
                )
            assertThat(resultReceiver).isNotNull()

            val resultData =
                Bundle().apply {
                    putStringArray(
                        ProjectedPermissionsConstants.RESULT_DATA_PERMISSIONS,
                        permissions,
                    )
                    putIntArray(
                        ProjectedPermissionsConstants.RESULT_DATA_GRANT_RESULTS,
                        grantResults,
                    )
                }
            resultReceiver!!.send(Activity.RESULT_OK, resultData)

            verify(mockProjectedService).finishProjectedPermissionRequest()

            assertThat(activity.onRequestPermissionsResultCalled).isTrue()
            assertThat(activity.lastRequestCode).isEqualTo(requestCode)
            assertThat(activity.lastPermissions).isEqualTo(permissions)
            assertThat(activity.lastGrantResults).isEqualTo(grantResults)
        }

    private fun launchTestActivity(block: (Activity) -> Unit) {
        shadowOf(context.packageManager)
            .addOrUpdateActivity(
                ActivityInfo().apply {
                    name = TestActivity::class.java.name
                    packageName = context.packageName
                }
            )
        val activityScenario: ActivityScenario<TestActivity> =
            ActivityScenario.launch(Intent(context, TestActivity::class.java))
        activityScenario.onActivity { activity -> block(activity) }
    }

    private fun launchTestProjectedPermissionActivity(block: (TestProjectedActivity) -> Unit) {
        shadowOf(context.packageManager)
            .addOrUpdateActivity(
                ActivityInfo().apply {
                    name = TestProjectedActivity::class.java.name
                    packageName = context.packageName
                }
            )
        val activityScenario: ActivityScenario<TestProjectedActivity> =
            ActivityScenario.launch(Intent(context, TestProjectedActivity::class.java))
        activityScenario.onActivity { activity -> block(activity) }
    }

    companion object {
        private const val INVALID_PROJECTED_ACTION_CODE = -50
    }

    private class TestActivity : Activity()

    private class TestProjectedActivity : Activity() {
        var onRequestPermissionsResultCalled = false
        var lastRequestCode = -1
        var lastPermissions: Array<String>? = null
        var lastGrantResults: IntArray? = null
        var lastDeviceId = -1
        var startIntentSenderForResultCalled = false
        var lastIntentSender: IntentSender? = null

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray,
            deviceId: Int,
        ) {
            onRequestPermissionsResultCalled = true
            lastRequestCode = requestCode
            lastPermissions = permissions
            lastGrantResults = grantResults
            lastDeviceId = deviceId
        }

        override fun startIntentSenderForResult(
            intent: IntentSender,
            requestCode: Int,
            fillInIntent: Intent?,
            flagsMask: Int,
            flagsValues: Int,
            extraFlags: Int,
            options: Bundle?,
        ) {
            startIntentSenderForResultCalled = true
            lastIntentSender = intent
        }

        private val virtualDeviceManager by lazy {
            getSystemService(VIRTUAL_DEVICE_SERVICE) as VirtualDeviceManager
        }

        override fun getDeviceId(): Int {
            return try {
                virtualDeviceManager.virtualDevices.firstOrNull()?.deviceId ?: super.getDeviceId()
            } catch (e: Exception) {
                super.getDeviceId()
            }
        }
    }
}
