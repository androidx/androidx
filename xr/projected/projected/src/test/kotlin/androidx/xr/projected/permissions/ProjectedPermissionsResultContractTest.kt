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

package androidx.xr.projected.permissions

import android.Manifest
import android.app.Activity
import android.app.ActivityOptions
import android.app.Application
import android.companion.virtual.VirtualDevice
import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle.State
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.projected.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplayManager
import org.robolectric.util.ReflectionHelpers

/**
 * This test class contains tests that span across [ProjectedPermissionsResultContract],
 * [GoToHostProjectedActivity], and [RequestPermissionsOnHostActivity].
 */
@RunWith(AndroidJUnit4::class)
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class ProjectedPermissionsResultContractTest {

    private val appContext: Application = getApplicationContext()
    private val virtualDeviceManager = appContext.getSystemService(VirtualDeviceManager::class.java)
    private lateinit var virtualDevice: VirtualDevice

    @get:Rule val composeTestRule = createEmptyComposeRule()

    private val deviceScopedContext: Context by lazy {
        appContext.createDeviceContext(virtualDevice.deviceId)
    }
    private val projectedDisplayId = ShadowDisplayManager.addDisplay("")
    private val projectedActivityOptions =
        ActivityOptions.makeBasic().setLaunchDisplayId(projectedDisplayId)

    @Before
    fun setUp() {
        createVirtualDevice()
        virtualDevice = virtualDeviceManager.virtualDevices.first()
    }

    @Test
    fun createIntent_isForProjectedActivity() {
        val deviceScopedContext = appContext.createDeviceContext(virtualDevice.deviceId)

        val intent =
            ProjectedPermissionsResultContract()
                .createIntent(deviceScopedContext, listOf(REQUEST_DATA_1, REQUEST_DATA_2))
        assertThat(intent.component!!.packageName).isEqualTo(appContext.packageName)
        assertThat(intent.component!!.className)
            .isEqualTo(GoToHostProjectedActivity::class.java.name)
    }

    @Test
    fun launchProjectedActivity_startsHostActivity() {
        val projectedActivityIntent =
            ProjectedPermissionsResultContract()
                .createIntent(deviceScopedContext, listOf(REQUEST_DATA_1, REQUEST_DATA_2))

        ActivityScenario.launchActivityForResult<GoToHostProjectedActivity>(projectedActivityIntent)
            .use { _ ->
                val startedActivityIntent = shadowOf(appContext).nextStartedActivity
                assertThat(startedActivityIntent.component!!.packageName)
                    .isEqualTo(appContext.packageName)
                assertThat(startedActivityIntent.component!!.className)
                    .isEqualTo(RequestPermissionsOnHostActivity::class.java.name)
            }
    }

    @Test
    fun launchHostActivity_noRationaleProvided_requestsPermissionsImmediately() {
        launchHostActivity(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = null,
                )
            )
        ) { activity, _ ->
            val request = getLastRequestedPermission(activity)!!
            assertThat(request.requestedPermissions.toList())
                .isEqualTo(NOT_DEVICE_SCOPED_PERMISSIONS)
            assertThat(request.deviceId).isEqualTo(Context.DEVICE_ID_DEFAULT)
        }
    }

    @Test
    fun launchHostActivity_rationaleProvided_requestsPermissionsOnlyAfterUserAcceptsRationale() {
        launchHostActivity(
            listOf(ProjectedPermissionsRequestParams(NOT_DEVICE_SCOPED_PERMISSIONS, "My rationale"))
        ) { activity, _ ->
            var request = getLastRequestedPermission(activity)
            // verify that no request is made
            assertThat(request).isNull()

            val continueButtonText = appContext.getString(R.string.continue_button)
            val cancelButtonText = appContext.getString(R.string.cancel_button)

            // verify that the rationale text and buttons are visible
            composeTestRule.onNodeWithText("My rationale").assertIsDisplayed()
            composeTestRule.onNodeWithText(continueButtonText).assertIsDisplayed()
            composeTestRule.onNodeWithText(cancelButtonText).assertIsDisplayed()

            // user taps on continue button
            composeTestRule.onNodeWithText(continueButtonText).performClick()

            // verify that the correct request is made
            request = getLastRequestedPermission(activity)!!
            assertThat(request.requestedPermissions.toList())
                .isEqualTo(NOT_DEVICE_SCOPED_PERMISSIONS)
            assertThat(request.deviceId).isEqualTo(Context.DEVICE_ID_DEFAULT)
        }
    }

    @Test
    fun userAcceptsPermissionDialog_sendsPermissionResultToAppActivity() {
        launchHostActivity(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = null,
                )
            )
        ) { activity, projectedActivityScenario ->
            val permissionRequest = getLastRequestedPermission(activity)!!

            // Simulates the user accepting the requests
            acceptPermissionsRequestFor(permissionRequest, activity)

            val resultReceivedByAppActivity = projectedActivityScenario.result
            assertThat(
                    ProjectedPermissionsResultContract()
                        .parseResult(
                            resultReceivedByAppActivity.resultCode,
                            resultReceivedByAppActivity.resultData,
                        )
                )
                .isEqualTo(
                    mapOf(
                        NOT_DEVICE_SCOPED_PERMISSIONS[0] to true,
                        NOT_DEVICE_SCOPED_PERMISSIONS[1] to true,
                    )
                )
        }
    }

    @Test
    fun userDeclinesPermissionDialog_sendsPermissionResultToAppActivity() {
        launchHostActivity(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = null,
                )
            )
        ) { activity, projectedActivityScenario ->
            val permissionRequest = getLastRequestedPermission(activity)!!

            // Simulates the user declining the requests
            declinePermissionsRequestFor(permissionRequest, activity)

            val resultReceivedByAppActivity = projectedActivityScenario.result
            assertThat(
                    ProjectedPermissionsResultContract()
                        .parseResult(
                            resultReceivedByAppActivity.resultCode,
                            resultReceivedByAppActivity.resultData,
                        )
                )
                .isEqualTo(
                    mapOf(
                        NOT_DEVICE_SCOPED_PERMISSIONS[0] to false,
                        NOT_DEVICE_SCOPED_PERMISSIONS[1] to false,
                    )
                )
        }
    }

    @Test
    fun userRejectsRationale_sendsPermissionResultToAppActivity() {
        launchHostActivity(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = "My rationale",
                )
            )
        ) { activity, projectedActivityScenario ->
            // user rejects rationale
            val cancelButtonText = appContext.getString(R.string.cancel_button)
            composeTestRule.onNodeWithText(cancelButtonText).performClick()

            val request = getLastRequestedPermission(activity)
            assertThat(request).isNull()
            val resultReceivedByAppActivity = projectedActivityScenario.result
            assertThat(
                    ProjectedPermissionsResultContract()
                        .parseResult(
                            resultReceivedByAppActivity.resultCode,
                            resultReceivedByAppActivity.resultData,
                        )
                )
                .isEqualTo(
                    mapOf(
                        NOT_DEVICE_SCOPED_PERMISSIONS[0] to false,
                        NOT_DEVICE_SCOPED_PERMISSIONS[1] to false,
                    )
                )
        }
    }

    @Test
    fun userRejectsRationale_rejectsAllPermissionsWhenNoOtherRequestHasRationale() {
        launchHostActivity(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.CAMERA),
                    rationale = "My rationale",
                ),
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = null,
                ),
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.RECORD_AUDIO),
                    rationale = null,
                ),
            )
        ) { activity, projectedActivityScenario ->
            // user rejects rationale
            val cancelButtonText = appContext.getString(R.string.cancel_button)
            composeTestRule.onNodeWithText(cancelButtonText).performClick()

            val request = getLastRequestedPermission(activity)
            assertThat(request).isNull()
            val resultReceivedByAppActivity = projectedActivityScenario.result
            assertThat(
                    ProjectedPermissionsResultContract()
                        .parseResult(
                            resultReceivedByAppActivity.resultCode,
                            resultReceivedByAppActivity.resultData,
                        )
                )
                .isEqualTo(
                    mapOf(
                        NOT_DEVICE_SCOPED_PERMISSIONS[0] to false,
                        NOT_DEVICE_SCOPED_PERMISSIONS[1] to false,
                        Manifest.permission.CAMERA to false,
                        Manifest.permission.RECORD_AUDIO to false,
                    )
                )
        }
    }

    @Test
    fun userRejectsRationale_rejectsAllPermissionsUntilNextRequestHasRationale() {
        launchHostActivity(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.CAMERA),
                    rationale = "My rationale 1",
                ),
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    rationale = null,
                ),
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.RECORD_AUDIO),
                    rationale = null,
                ),
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.READ_CALENDAR),
                    rationale = "My rationale 2",
                ),
            )
        ) { activity, projectedActivityScenario ->
            val continueButtonText = appContext.getString(R.string.continue_button)
            val cancelButtonText = appContext.getString(R.string.cancel_button)

            // user taps on cancel button for the first rationale
            composeTestRule.onNodeWithText(cancelButtonText).performClick()

            // verify that the user is presented with the next rationale screen
            composeTestRule.onNodeWithText("My rationale 2").assertIsDisplayed()
            composeTestRule.onNodeWithText(continueButtonText).assertIsDisplayed()
            composeTestRule.onNodeWithText(cancelButtonText).assertIsDisplayed()

            // user taps on continue for the second rationale
            composeTestRule.onNodeWithText(continueButtonText).performClick()

            val permissionRequest = getLastRequestedPermission(activity)!!
            // Simulates the user accepting the request
            acceptPermissionsRequestFor(permissionRequest, activity)

            val resultReceivedByAppActivity = projectedActivityScenario.result
            assertThat(
                    ProjectedPermissionsResultContract()
                        .parseResult(
                            resultReceivedByAppActivity.resultCode,
                            resultReceivedByAppActivity.resultData,
                        )
                )
                .isEqualTo(
                    mapOf(
                        Manifest.permission.CAMERA to false,
                        Manifest.permission.ACCESS_COARSE_LOCATION to false,
                        Manifest.permission.RECORD_AUDIO to false,
                        Manifest.permission.READ_CALENDAR to true,
                    )
                )
        }
    }

    @Test
    fun afterUserActsOnPermissionDialog_finishesBothHostAndProjectedActivity() {
        launchHostActivity(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = null,
                )
            )
        ) { activity, projectedActivityScenario ->
            val permissionRequest = getLastRequestedPermission(activity)!!

            // Simulates the user accepting the requests
            acceptPermissionsRequestFor(permissionRequest, activity)

            assertThat(activity.isFinishing).isTrue()
            projectedActivityScenario.onActivity { assertThat(it.isFinishing).isTrue() }
        }
    }

    @Test
    fun requestDeviceScopedPermissions_requestsPermissionsForBothHostAndProjectedDevice() {
        launchHostActivity(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions =
                        listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                    rationale = null,
                )
            )
        ) { activity, projectedActivityScenario ->
            // first request
            var request = getLastRequestedPermission(activity)!!
            assertThat(request.requestedPermissions.toList())
                .containsExactly(Manifest.permission.CAMERA)
            assertThat(request.deviceId).isEqualTo(Context.DEVICE_ID_DEFAULT)
            acceptPermissionsRequestFor(request, activity)
            // second request
            request = getLastRequestedPermission(activity)!!
            assertThat(request.requestedPermissions.toList())
                .containsExactly(Manifest.permission.CAMERA)
            assertThat(request.deviceId).isEqualTo(virtualDevice.deviceId)
            acceptPermissionsRequestFor(request, activity)
            // third request
            request = getLastRequestedPermission(activity)!!
            assertThat(request.requestedPermissions.toList())
                .containsExactly(Manifest.permission.RECORD_AUDIO)
            assertThat(request.deviceId).isEqualTo(Context.DEVICE_ID_DEFAULT)
            acceptPermissionsRequestFor(request, activity)
            // fourth request
            request = getLastRequestedPermission(activity)!!
            assertThat(request.requestedPermissions.toList())
                .containsExactly(Manifest.permission.RECORD_AUDIO)
            assertThat(request.deviceId).isEqualTo(virtualDevice.deviceId)
            acceptPermissionsRequestFor(request, activity)
            // verify results sent to the app
            val resultReceivedByAppActivity = projectedActivityScenario.result
            assertThat(
                    ProjectedPermissionsResultContract()
                        .parseResult(
                            resultReceivedByAppActivity.resultCode,
                            resultReceivedByAppActivity.resultData,
                        )
                )
                .isEqualTo(
                    mapOf(
                        Manifest.permission.CAMERA to true,
                        Manifest.permission.RECORD_AUDIO to true,
                    )
                )
        }
    }

    @Test
    fun requestDeviceScopedPermissions_userAcceptsHostPermissionButDeclinesProjected_sendsDeclineToApp() {
        launchHostActivity(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.CAMERA),
                    rationale = null,
                )
            )
        ) { activity, projectedActivityScenario ->
            var request = getLastRequestedPermission(activity)!!

            acceptPermissionsRequestFor(request, activity)
            request = getLastRequestedPermission(activity)!!
            declinePermissionsRequestFor(request, activity)

            val resultReceivedByAppActivity = projectedActivityScenario.result
            assertThat(
                    ProjectedPermissionsResultContract()
                        .parseResult(
                            resultReceivedByAppActivity.resultCode,
                            resultReceivedByAppActivity.resultData,
                        )
                )
                .isEqualTo(mapOf(Manifest.permission.CAMERA to false))
        }
    }

    @Test
    fun requestDeviceScopedPermissions_userDeclinesHostPermissionAndAcceptsProjected_sendsDeclineToApp() {
        launchHostActivity(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.CAMERA),
                    rationale = null,
                )
            )
        ) { activity, projectedActivityScenario ->
            var request = getLastRequestedPermission(activity)!!

            declinePermissionsRequestFor(request, activity)
            request = getLastRequestedPermission(activity)!!
            acceptPermissionsRequestFor(request, activity)

            val resultReceivedByAppActivity = projectedActivityScenario.result
            assertThat(
                    ProjectedPermissionsResultContract()
                        .parseResult(
                            resultReceivedByAppActivity.resultCode,
                            resultReceivedByAppActivity.resultData,
                        )
                )
                .isEqualTo(mapOf(Manifest.permission.CAMERA to false))
        }
    }

    @Test
    fun multipleRequests_sendsCorrectResult() {
        // This test verifies a user journey with multiple requests, which some accepted, some
        // declined,
        // and some rejected at the rationale screen.
        val continueButtonText = appContext.getString(R.string.continue_button)
        val cancelButtonText = appContext.getString(R.string.cancel_button)

        launchHostActivity(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.CAMERA),
                    rationale = "My rationale 1",
                ),
                ProjectedPermissionsRequestParams(
                    permissions =
                        listOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.BLUETOOTH_CONNECT,
                        ),
                    rationale = "My rationale 2",
                ),
                ProjectedPermissionsRequestParams(
                    permissions =
                        listOf(
                            Manifest.permission.READ_CALENDAR,
                            Manifest.permission.READ_CONTACTS,
                        ),
                    rationale = "My rationale 3",
                ),
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    rationale = null,
                ),
            )
        ) { activity, projectedActivityScenario ->
            composeTestRule.onNodeWithText("My rationale 1").assertIsDisplayed()
            // user rejects camera permission at rationale screen
            composeTestRule.onNodeWithText(cancelButtonText).performClick()
            composeTestRule.onNodeWithText("My rationale 2").assertIsDisplayed()
            // user continues
            composeTestRule.onNodeWithText(continueButtonText).performClick()
            var request = getLastRequestedPermission(activity)!!
            // RECORD_AUDIO is device-scoped, so separate requests for host and projected device are
            // made
            assertThat(request.requestedPermissions.toList())
                .containsExactly(Manifest.permission.RECORD_AUDIO)
            assertThat(request.deviceId).isEqualTo(Context.DEVICE_ID_DEFAULT)
            // user declines host's audio permission
            declinePermissionsRequestFor(request, activity)
            request = getLastRequestedPermission(activity)!!
            assertThat(request.requestedPermissions.toList())
                .containsExactly(Manifest.permission.RECORD_AUDIO)
            assertThat(request.deviceId).isEqualTo(virtualDevice.deviceId)
            // user accepts projected's device audio permission
            acceptPermissionsRequestFor(request, activity)
            request = getLastRequestedPermission(activity)!!
            assertThat(request.requestedPermissions.toList())
                .containsExactly(Manifest.permission.BLUETOOTH_CONNECT)
            // user accepts bluetooth permission
            acceptPermissionsRequestFor(request, activity)
            composeTestRule.onNodeWithText("My rationale 3").assertIsDisplayed()
            // user continues, declines the calendar permission, and grants the contacts permission
            composeTestRule.onNodeWithText(continueButtonText).performClick()
            request = getLastRequestedPermission(activity)!!
            assertThat(request.requestedPermissions.toList())
                .containsExactly(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.READ_CONTACTS,
                )
            respondToPermissionsRequest(
                request,
                activity,
                intArrayOf(PackageManager.PERMISSION_DENIED, PackageManager.PERMISSION_GRANTED),
            )
            // the fourth request is immediately made because it does not have a rationale
            request = getLastRequestedPermission(activity)!!
            assertThat(request.requestedPermissions.toList())
                .containsExactly(Manifest.permission.ACCESS_COARSE_LOCATION)
            // user accepts the location permission
            acceptPermissionsRequestFor(request, activity)

            val resultReceivedByAppActivity = projectedActivityScenario.result
            assertThat(
                    ProjectedPermissionsResultContract()
                        .parseResult(
                            resultReceivedByAppActivity.resultCode,
                            resultReceivedByAppActivity.resultData,
                        )
                )
                .isEqualTo(
                    mapOf(
                        Manifest.permission.CAMERA to false,
                        Manifest.permission.RECORD_AUDIO to false,
                        Manifest.permission.BLUETOOTH_CONNECT to true,
                        Manifest.permission.READ_CALENDAR to false,
                        Manifest.permission.READ_CONTACTS to true,
                        Manifest.permission.ACCESS_COARSE_LOCATION to true,
                    )
                )
        }
    }

    @Test
    fun projectedActivityRecreated_canStillReceiveResultsFromHostActivity() {
        launchHostActivity(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = null,
                )
            )
        ) { activity, projectedActivityScenario ->
            val permissionRequest = getLastRequestedPermission(activity)!!

            projectedActivityScenario.recreate()
            // Simulates the user accepting the requests
            acceptPermissionsRequestFor(permissionRequest, activity)

            val resultReceivedByAppActivity = projectedActivityScenario.result
            assertThat(
                    ProjectedPermissionsResultContract()
                        .parseResult(
                            resultReceivedByAppActivity.resultCode,
                            resultReceivedByAppActivity.resultData,
                        )
                )
                .isEqualTo(
                    mapOf(
                        NOT_DEVICE_SCOPED_PERMISSIONS[0] to true,
                        NOT_DEVICE_SCOPED_PERMISSIONS[1] to true,
                    )
                )
        }
    }

    @Test
    fun hostActivityRecreated_canStillSendResultsToProjectedActivity() {
        launchHostActivityWithActivityScenarioCallback(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = null,
                )
            )
        ) { hostActivityScenario, projectedActivityScenario ->
            // The host activity has been launched and the user is presented with the system
            // permission dialog
            hostActivityScenario.recreate()

            hostActivityScenario.onActivity { activity ->
                val permissionRequest = getLastRequestedPermission(activity)!!
                // Simulates the user accepting the requests
                acceptPermissionsRequestFor(permissionRequest, activity)

                val resultReceivedByAppActivity = projectedActivityScenario.result
                assertThat(
                        ProjectedPermissionsResultContract()
                            .parseResult(
                                resultReceivedByAppActivity.resultCode,
                                resultReceivedByAppActivity.resultData,
                            )
                    )
                    .isEqualTo(
                        mapOf(
                            NOT_DEVICE_SCOPED_PERMISSIONS[0] to true,
                            NOT_DEVICE_SCOPED_PERMISSIONS[1] to true,
                        )
                    )
            }
        }
    }

    @Test
    fun hostActivityRecreatedAtFirstRationaleScreen_canStillSendResultsToProjectedActivity() {
        launchHostActivityWithActivityScenarioCallback(
            listOf(ProjectedPermissionsRequestParams(NOT_DEVICE_SCOPED_PERMISSIONS, "My rationale"))
        ) { hostActivityScenario, projectedActivityScenario ->
            // The host activity has been launched and the user is presented with the rationale
            hostActivityScenario.recreate()

            hostActivityScenario.onActivity { activity ->
                val request = getLastRequestedPermission(activity)
                // verify that no request is made
                assertThat(request).isNull()
            }
            val continueButtonText = appContext.getString(R.string.continue_button)
            val cancelButtonText = appContext.getString(R.string.cancel_button)

            // verify that the rationale text and buttons are visible
            composeTestRule.onNodeWithText("My rationale").assertIsDisplayed()
            composeTestRule.onNodeWithText(continueButtonText).assertIsDisplayed()
            composeTestRule.onNodeWithText(cancelButtonText).assertIsDisplayed()

            // user taps on continue button
            composeTestRule.onNodeWithText(continueButtonText).performClick()
            hostActivityScenario.onActivity { activity ->
                // verify that the correct request is made
                val request = getLastRequestedPermission(activity)!!
                assertThat(request.requestedPermissions.toList())
                    .isEqualTo(NOT_DEVICE_SCOPED_PERMISSIONS)
                assertThat(request.deviceId).isEqualTo(Context.DEVICE_ID_DEFAULT)
                // simulate the user accepting the request
                acceptPermissionsRequestFor(request, activity)

                val resultReceivedByAppActivity = projectedActivityScenario.result
                assertThat(
                        ProjectedPermissionsResultContract()
                            .parseResult(
                                resultReceivedByAppActivity.resultCode,
                                resultReceivedByAppActivity.resultData,
                            )
                    )
                    .isEqualTo(
                        mapOf(
                            NOT_DEVICE_SCOPED_PERMISSIONS[0] to true,
                            NOT_DEVICE_SCOPED_PERMISSIONS[1] to true,
                        )
                    )
            }
        }
    }

    @Test
    fun hostActivityRecreatedAtSecondRationaleScreen_madeCorrectRequestsAndSendCorrectResults() {
        launchHostActivityWithActivityScenarioCallback(
            listOf(
                ProjectedPermissionsRequestParams(NOT_DEVICE_SCOPED_PERMISSIONS, "My rationale 1"),
                ProjectedPermissionsRequestParams(
                    listOf(Manifest.permission.BLUETOOTH_CONNECT),
                    "My rationale 2",
                ),
            )
        ) { hostActivityScenario, projectedActivityScenario ->
            val continueButtonText = appContext.getString(R.string.continue_button)
            val cancelButtonText = appContext.getString(R.string.cancel_button)
            // user taps on continue button on the first rationale screen
            composeTestRule.onNodeWithText(continueButtonText).performClick()
            hostActivityScenario.onActivity { activity ->
                val request = getLastRequestedPermission(activity)!!
                // simulate the user accepting the request, the second rationale will then be
                // visible
                acceptPermissionsRequestFor(request, activity)
            }

            hostActivityScenario.recreate()

            // verify that the second rationale text and buttons are visible
            composeTestRule.onNodeWithText("My rationale 2").assertIsDisplayed()
            composeTestRule.onNodeWithText(continueButtonText).assertIsDisplayed()
            composeTestRule.onNodeWithText(cancelButtonText).assertIsDisplayed()
            // user taps on continue button on the second rationale screen
            composeTestRule.onNodeWithText(continueButtonText).performClick()
            hostActivityScenario.onActivity { activity ->
                val request = getLastRequestedPermission(activity)!!
                // verify the correct permission is requested
                assertThat(request.requestedPermissions)
                    .isEqualTo(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                assertThat(request.deviceId).isEqualTo(Context.DEVICE_ID_DEFAULT)
                // simulate the user accepting the request
                acceptPermissionsRequestFor(request, activity)
                // verify results sent to the app activity
                val resultReceivedByAppActivity = projectedActivityScenario.result
                assertThat(
                        ProjectedPermissionsResultContract()
                            .parseResult(
                                resultReceivedByAppActivity.resultCode,
                                resultReceivedByAppActivity.resultData,
                            )
                    )
                    .isEqualTo(
                        mapOf(
                            NOT_DEVICE_SCOPED_PERMISSIONS[0] to true,
                            NOT_DEVICE_SCOPED_PERMISSIONS[1] to true,
                            Manifest.permission.BLUETOOTH_CONNECT to true,
                        )
                    )
            }
        }
    }

    @Test
    fun hostActivityRecreatedAfterUserAcceptsSecondRationaleScreen_madeCorrectRequestsAndSendCorrectResults() {
        launchHostActivityWithActivityScenarioCallback(
            listOf(
                ProjectedPermissionsRequestParams(NOT_DEVICE_SCOPED_PERMISSIONS, "My rationale 1"),
                ProjectedPermissionsRequestParams(
                    listOf(Manifest.permission.CAMERA),
                    "My rationale 2",
                ),
            )
        ) { hostActivityScenario, projectedActivityScenario ->
            val continueButtonText = appContext.getString(R.string.continue_button)
            // user taps on continue button on the first rationale screen
            composeTestRule.onNodeWithText(continueButtonText).performClick()
            hostActivityScenario.onActivity { activity ->
                val request = getLastRequestedPermission(activity)!!
                // simulate the user accepting the request, the second rationale will then be
                // visible
                acceptPermissionsRequestFor(request, activity)
            }
            // user taps on continue button on the second rationale screen, they will be presented
            // with the system permission prompt
            composeTestRule.onNodeWithText(continueButtonText).performClick()

            hostActivityScenario.recreate()

            // after the host activity is recreated, the system permission prompt is still visible
            // and on top of the host activity
            hostActivityScenario.onActivity { activity ->
                var request = getLastRequestedPermission(activity)!!
                // verify the correct permission is requested
                assertThat(request.requestedPermissions)
                    .isEqualTo(arrayOf(Manifest.permission.CAMERA))
                assertThat(request.deviceId).isEqualTo(Context.DEVICE_ID_DEFAULT)
                // simulate the user accepting the request
                acceptPermissionsRequestFor(request, activity)
                // Since CAMERA is device-scoped, we should request the Projected device-scoped
                // permission
                request = getLastRequestedPermission(activity)!!
                assertThat(request.requestedPermissions)
                    .isEqualTo(arrayOf(Manifest.permission.CAMERA))
                assertThat(request.deviceId).isEqualTo(virtualDevice.deviceId)
                declinePermissionsRequestFor(request, activity)
                // verify results sent to the app activity
                val resultReceivedByAppActivity = projectedActivityScenario.result
                assertThat(
                        ProjectedPermissionsResultContract()
                            .parseResult(
                                resultReceivedByAppActivity.resultCode,
                                resultReceivedByAppActivity.resultData,
                            )
                    )
                    .isEqualTo(
                        mapOf(
                            NOT_DEVICE_SCOPED_PERMISSIONS[0] to true,
                            NOT_DEVICE_SCOPED_PERMISSIONS[1] to true,
                            Manifest.permission.CAMERA to false,
                        )
                    )
            }
        }
    }

    @Test
    fun userQuitsProjectedActivity_finishesHostActivity() {
        launchHostActivity(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = null,
                )
            )
        ) { _, projectedActivityScenario ->

            // ActivityScenario finishes the activity when moving it to the DESTROYED state. We use
            // this to simulate a user quitting the projected activity.
            projectedActivityScenario.moveToState(State.DESTROYED)

            // Robolectric does not respect FLAG_ACTIVITY_SINGLE_TOP. When we launch the
            // newHostActivityIntent below, onNewIntent is not called on the existing host activity.
            // Therefore, we can only verify that the intent is for the correct class and contains
            // the correct extras.
            val newHostActivityIntent = shadowOf(appContext).nextStartedActivity
            assertThat(newHostActivityIntent.component!!.className)
                .isEqualTo(RequestPermissionsOnHostActivity::class.java.name)
            assertThat(
                    newHostActivityIntent.getBooleanExtra(
                        GoToHostProjectedActivity.EXTRA_SHOULD_FINISH,
                        false,
                    )
                )
                .isTrue()
        }
    }

    @Test
    fun userQuitsHostActivity_finishesProjectedActivity() {
        launchHostActivityWithActivityScenarioCallback(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = null,
                )
            )
        ) { hostActivityScenario, projectedActivityScenario ->

            // ActivityScenario finishes the activity when moving it to the DESTROYED state. We use
            // this to simulate a user quitting the host activity.
            hostActivityScenario.moveToState(State.DESTROYED)

            projectedActivityScenario.onActivity { projectedActivity ->
                assertThat(projectedActivity.isFinishing).isTrue()
            }
        }
    }

    @Test
    fun userQuitsHostActivity_noPermissionResultYet_sendsAllPermissionsDeclinedResultToApp() {
        launchHostActivityWithActivityScenarioCallback(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = null,
                )
            )
        ) { hostActivityScenario, projectedActivityScenario ->

            // ActivityScenario finishes the activity when moving it to the DESTROYED state. We use
            // this to simulate a user quitting the host activity.
            hostActivityScenario.moveToState(State.DESTROYED)

            val resultReceivedByAppActivity = projectedActivityScenario.result
            assertThat(
                    ProjectedPermissionsResultContract()
                        .parseResult(
                            resultReceivedByAppActivity.resultCode,
                            resultReceivedByAppActivity.resultData,
                        )
                )
                .isEqualTo(
                    mapOf(
                        NOT_DEVICE_SCOPED_PERMISSIONS[0] to false,
                        NOT_DEVICE_SCOPED_PERMISSIONS[1] to false,
                    )
                )
        }
    }

    @Test
    fun userQuitsHostActivityAtRationaleScreen_noPermissionResultYet_sendsAllPermissionsDeclinedResultToApp() {
        launchHostActivityWithActivityScenarioCallback(
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = "my rationale",
                )
            )
        ) { hostActivityScenario, projectedActivityScenario ->

            // ActivityScenario finishes the activity when moving it to the DESTROYED state. We use
            // this to simulate a user quitting the host activity.
            hostActivityScenario.moveToState(State.DESTROYED)

            val resultReceivedByAppActivity = projectedActivityScenario.result
            assertThat(
                    ProjectedPermissionsResultContract()
                        .parseResult(
                            resultReceivedByAppActivity.resultCode,
                            resultReceivedByAppActivity.resultData,
                        )
                )
                .isEqualTo(
                    mapOf(
                        NOT_DEVICE_SCOPED_PERMISSIONS[0] to false,
                        NOT_DEVICE_SCOPED_PERMISSIONS[1] to false,
                    )
                )
        }
    }

    @Test
    fun userQuitsHostActivityAfterGrantingFirstPermission_sendsCorrectPermissionsResultToApp() {
        launchHostActivityWithActivityScenarioCallback(
            listOf(
                ProjectedPermissionsRequestParams(NOT_DEVICE_SCOPED_PERMISSIONS, null),
                ProjectedPermissionsRequestParams(listOf(Manifest.permission.CAMERA), null),
            )
        ) { hostActivityScenario, projectedActivityScenario ->
            hostActivityScenario.onActivity { activity ->
                val request = getLastRequestedPermission(activity)!!
                // simulate the user accepting the request
                acceptPermissionsRequestFor(request, activity)
            }

            // ActivityScenario finishes the activity when moving it to the DESTROYED state. We use
            // this to simulate a user quitting the host activity.
            hostActivityScenario.moveToState(State.DESTROYED)

            val resultReceivedByAppActivity = projectedActivityScenario.result
            // Accepted permissions are returned as granted, skipped ones are declined
            assertThat(
                    ProjectedPermissionsResultContract()
                        .parseResult(
                            resultReceivedByAppActivity.resultCode,
                            resultReceivedByAppActivity.resultData,
                        )
                )
                .isEqualTo(
                    mapOf(
                        NOT_DEVICE_SCOPED_PERMISSIONS[0] to true,
                        NOT_DEVICE_SCOPED_PERMISSIONS[1] to true,
                        Manifest.permission.CAMERA to false,
                    )
                )
        }
    }

    @Test
    fun newHostActivityIntent_sendsResultsToPreviousAppActivity() {
        // When a projected activity requests permissions, we should cancel any outstanding requests
        // from a previous projected activity of the same app
        val permissionResultList1 =
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = null,
                ),
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.RECORD_AUDIO),
                    rationale = "my rationale",
                ),
            )
        val permissionResultList2 =
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.CAMERA),
                    rationale = null,
                )
            )
        // Launch the first projected activity and host activity
        launchHostActivity(permissionResultList1) { hostActivity, firstProjectedActivityScenario ->
            val permissionRequest = getLastRequestedPermission(hostActivity)!!
            // Simulates the user accepting the requests
            acceptPermissionsRequestFor(permissionRequest, hostActivity)
            // user launches another projected activity from the same app, and that activity
            // launches a new GoToHostProjectedActivity
            ActivityScenario.launchActivityForResult<GoToHostProjectedActivity>(
                    ProjectedPermissionsResultContract()
                        .createIntent(deviceScopedContext, permissionResultList2)
                )
                .use { _ ->
                    // The projected activity launches a new host activity intent, which triggers
                    // the host activity's onNewIntent method.
                    val newHostActivityIntent = shadowOf(appContext).nextStartedActivity
                    hostActivity.onNewIntent(newHostActivityIntent)
                    // Verify that the first app projected activity receives the results for the
                    // permissions acted on by the user
                    val resultReceivedByFirstAppActivity = firstProjectedActivityScenario.result
                    assertThat(
                            ProjectedPermissionsResultContract()
                                .parseResult(
                                    resultReceivedByFirstAppActivity.resultCode,
                                    resultReceivedByFirstAppActivity.resultData,
                                )
                        )
                        .isEqualTo(
                            mapOf(
                                NOT_DEVICE_SCOPED_PERMISSIONS[0] to true,
                                NOT_DEVICE_SCOPED_PERMISSIONS[1] to true,
                            )
                        )
                }
        }
    }

    @Test
    fun newHostActivityIntent_requestsNewPermissionsAndSendNewResultsToNewAppActivity() {
        // When a projected activity requests permissions, even if there is an outstanding request
        // from a previous projected activity of the same app, the new request should work
        val permissionResultList1 =
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = null,
                ),
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.RECORD_AUDIO),
                    rationale = "my rationale",
                ),
            )
        val permissionResultList2 =
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.BLUETOOTH_CONNECT),
                    rationale = null,
                )
            )
        // Launch the first projected activity and host activity
        launchHostActivity(permissionResultList1) { hostActivity, _ ->
            val permissionRequest = getLastRequestedPermission(hostActivity)!!
            // Simulates the user accepting the requests
            acceptPermissionsRequestFor(permissionRequest, hostActivity)
            // user launches another projected activity from the same app, and that activity
            // launches a new GoToHostProjectedActivity
            ActivityScenario.launchActivityForResult<GoToHostProjectedActivity>(
                    ProjectedPermissionsResultContract()
                        .createIntent(deviceScopedContext, permissionResultList2)
                )
                .use { secondProjectedActivityScenario ->
                    // The projected activity launches a new host activity intent, which triggers
                    // the host activity's onNewIntent method.
                    val newHostActivityIntent = shadowOf(appContext).nextStartedActivity
                    hostActivity.onNewIntent(newHostActivityIntent)
                    val request = getLastRequestedPermission(hostActivity)!!
                    // verify the correct permission from permissionResultList2 is requested
                    assertThat(request.requestedPermissions)
                        .isEqualTo(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                    assertThat(request.deviceId).isEqualTo(Context.DEVICE_ID_DEFAULT)
                    // simulate the user accepting the request
                    acceptPermissionsRequestFor(request, hostActivity)
                    // verify results sent to the new app activity
                    val resultReceivedByAppActivity = secondProjectedActivityScenario.result
                    assertThat(
                            ProjectedPermissionsResultContract()
                                .parseResult(
                                    resultReceivedByAppActivity.resultCode,
                                    resultReceivedByAppActivity.resultData,
                                )
                        )
                        .isEqualTo(mapOf(Manifest.permission.BLUETOOTH_CONNECT to true))
                }
        }
    }

    @Test
    fun newHostActivityIntentWithRationale_requestsNewPermissionsAndSendNewResultsToNewAppActivity() {
        // This test involves:
        // 1. User launching a projected activity that launches the GoToHostProjectedActivity
        // 2. User launching a second projected activity that launches a second
        // GoToHostProjectedActivity
        // 3. The user interacting with the permission dialogs on the host
        // The actual launch order should be GoToHostProjectedActivity1 ->
        // RequestPermissionsOnHostActivity -> GoToHostProjectedActivity2 ->
        // RequestPermissionsOnHostActivity newIntent
        // To make sure Compose checks the UI elements on RequestPermissionsOnHostActivity rather
        // than the projected activity, we launch the two projected activities first, then launch
        // the host activity.
        val permissionResultList1 =
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = NOT_DEVICE_SCOPED_PERMISSIONS,
                    rationale = "rationale 1",
                ),
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.RECORD_AUDIO),
                    rationale = "rationale 2",
                ),
            )
        val permissionResultList2 =
            listOf(
                ProjectedPermissionsRequestParams(
                    permissions = listOf(Manifest.permission.BLUETOOTH_CONNECT),
                    rationale = "rationale 3",
                )
            )
        val projectedActivityIntent1 =
            ProjectedPermissionsResultContract()
                .createIntent(deviceScopedContext, permissionResultList1)
        // launch projected activity 1
        ActivityScenario.launchActivityForResult<GoToHostProjectedActivity>(
                projectedActivityIntent1,
                projectedActivityOptions.toBundle(),
            )
            .use { _ ->
                val hostActivityIntent1 = shadowOf(appContext).nextStartedActivity
                val projectedActivityIntent2 =
                    ProjectedPermissionsResultContract()
                        .createIntent(deviceScopedContext, permissionResultList2)
                // launch projected activity 2
                ActivityScenario.launchActivityForResult<GoToHostProjectedActivity>(
                        projectedActivityIntent2,
                        projectedActivityOptions.toBundle(),
                    )
                    .use { projectedActivityScenario2 ->
                        val hostActivityIntent2 = shadowOf(appContext).nextStartedActivity
                        // launch host activity using the Intent from projected activity 1
                        ActivityScenario.launch<RequestPermissionsOnHostActivity>(
                                hostActivityIntent1
                            )
                            .use { hostActivityScenario ->
                                hostActivityScenario.onActivity { hostActivity ->
                                    // Projected activity 2 launches a new host activity intent,
                                    // which triggers the host activity's onNewIntent method.
                                    hostActivity.onNewIntent(hostActivityIntent2)
                                }

                                // Get button texts for Compose finders
                                val continueButtonText =
                                    appContext.getString(R.string.continue_button)
                                val cancelButtonText = appContext.getString(R.string.cancel_button)

                                // Verify that the new rationale is displayed
                                composeTestRule.onNodeWithText("rationale 3").assertIsDisplayed()
                                composeTestRule
                                    .onNodeWithText(continueButtonText)
                                    .assertIsDisplayed()
                                composeTestRule.onNodeWithText(cancelButtonText).assertIsDisplayed()

                                // user taps on continue button
                                composeTestRule.onNodeWithText(continueButtonText).performClick()

                                hostActivityScenario.onActivity { hostActivity ->
                                    // verify that the correct request is made
                                    val request = getLastRequestedPermission(hostActivity)!!
                                    assertThat(request.requestedPermissions)
                                        .isEqualTo(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                                    assertThat(request.deviceId)
                                        .isEqualTo(Context.DEVICE_ID_DEFAULT)
                                    // simulate the user accepting the request
                                    acceptPermissionsRequestFor(request, hostActivity)
                                    // verify results sent to the new app activity
                                    val resultReceivedByAppActivity =
                                        projectedActivityScenario2.result
                                    assertThat(
                                            ProjectedPermissionsResultContract()
                                                .parseResult(
                                                    resultReceivedByAppActivity.resultCode,
                                                    resultReceivedByAppActivity.resultData,
                                                )
                                        )
                                        .isEqualTo(
                                            mapOf(Manifest.permission.BLUETOOTH_CONNECT to true)
                                        )
                                }
                            }
                    }
            }
    }

    private fun acceptPermissionsRequestFor(request: PermissionsRequest, activity: Activity) {
        respondToPermissionsRequest(
            request,
            activity,
            IntArray(request.requestedPermissions.size) { PackageManager.PERMISSION_GRANTED },
        )
    }

    private fun declinePermissionsRequestFor(request: PermissionsRequest, activity: Activity) {
        respondToPermissionsRequest(
            request,
            activity,
            IntArray(request.requestedPermissions.size) { PackageManager.PERMISSION_DENIED },
        )
    }

    private fun respondToPermissionsRequest(
        request: PermissionsRequest,
        activity: Activity,
        results: IntArray,
    ) {
        val response = Intent()
        response.putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, request.requestedPermissions)
        response.putExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS, results)
        response.putExtra(EXTRA_REQUEST_PERMISSIONS_DEVICE_ID, request.deviceId)
        shadowOf(activity).receiveResult(request.requestIntent, Activity.RESULT_OK, response)
    }

    private fun getLastRequestedPermission(activity: Activity): PermissionsRequest? {
        val intentForResult = shadowOf(activity).nextStartedActivityForResult
        if (intentForResult == null) {
            return null
        }
        val permissions =
            intentForResult.intent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES)
        if (permissions == null) {
            // the captured intentForResult is not for a permission request
            return null
        }
        return PermissionsRequest(
            intentForResult.intent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES)!!,
            intentForResult.requestCode,
            intentForResult.intent.getIntExtra(
                EXTRA_REQUEST_PERMISSIONS_DEVICE_ID,
                Context.DEVICE_ID_DEFAULT,
            ),
            intentForResult.intent,
        )
    }

    private fun launchHostActivity(
        requestDataList: List<ProjectedPermissionsRequestParams>,
        block:
            (RequestPermissionsOnHostActivity, ActivityScenario<GoToHostProjectedActivity>) -> Unit,
    ) {
        launchHostActivityWithActivityScenarioCallback(requestDataList) {
            hostActivityScenario,
            projectedActivityScenario ->
            hostActivityScenario.onActivity { hostActivity ->
                block(hostActivity, projectedActivityScenario)
            }
        }
    }

    private fun launchHostActivityWithActivityScenarioCallback(
        requestDataList: List<ProjectedPermissionsRequestParams>,
        block:
            (
                ActivityScenario<RequestPermissionsOnHostActivity>,
                ActivityScenario<GoToHostProjectedActivity>,
            ) -> Unit,
    ) {
        val projectedActivityIntent =
            ProjectedPermissionsResultContract().createIntent(deviceScopedContext, requestDataList)
        ActivityScenario.launchActivityForResult<GoToHostProjectedActivity>(projectedActivityIntent)
            .use { projectedActivityScenario ->
                val hostActivityIntent = shadowOf(appContext).nextStartedActivity
                ActivityScenario.launch<RequestPermissionsOnHostActivity>(hostActivityIntent).use {
                    hostActivityScenario ->
                    block(hostActivityScenario, projectedActivityScenario)
                }
            }
    }

    private data class PermissionsRequest(
        val requestedPermissions: Array<String>,
        val requestCode: Int,
        val deviceId: Int,
        val requestIntent: Intent, // we need the request intent to use ShadowActivity#receiveResult
    )

    private fun createVirtualDevice() {
        val virtualDeviceParamsBuilderClass =
            Class.forName("android.companion.virtual.VirtualDeviceParams\$Builder")
        val virtualDeviceParamsClass =
            Class.forName("android.companion.virtual.VirtualDeviceParams")
        var virtualDeviceParamsBuilder =
            ReflectionHelpers.callConstructor(virtualDeviceParamsBuilderClass)
        virtualDeviceParamsBuilder =
            ReflectionHelpers.callInstanceMethod(
                virtualDeviceParamsBuilder,
                "setName",
                ReflectionHelpers.ClassParameter(String::class.java, "ProjectionDevice"),
            )
        virtualDeviceParamsBuilder =
            ReflectionHelpers.callInstanceMethod(virtualDeviceParamsBuilder, "build")
        ReflectionHelpers.callInstanceMethod<Any?>(
            virtualDeviceManager,
            "createVirtualDevice",
            ReflectionHelpers.ClassParameter(Int::class.javaPrimitiveType, 1),
            ReflectionHelpers.ClassParameter(virtualDeviceParamsClass, virtualDeviceParamsBuilder),
        )
    }

    private companion object {
        const val EXTRA_REQUEST_PERMISSIONS_NAMES =
            "android.content.pm.extra.REQUEST_PERMISSIONS_NAMES"
        const val EXTRA_REQUEST_PERMISSIONS_RESULTS =
            "android.content.pm.extra.REQUEST_PERMISSIONS_RESULTS"
        const val EXTRA_REQUEST_PERMISSIONS_DEVICE_ID =
            "android.content.pm.extra.REQUEST_PERMISSIONS_DEVICE_ID"
        val NOT_DEVICE_SCOPED_PERMISSIONS =
            listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_CALENDAR)
        val REQUEST_DATA_1 =
            ProjectedPermissionsRequestParams(
                permissions =
                    listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA),
                rationale = "My rationale 1",
            )
        val REQUEST_DATA_2 =
            ProjectedPermissionsRequestParams(
                listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                rationale = "My rationale 2",
            )
    }
}
