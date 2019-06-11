/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION")

package androidx.camera.integration.antelope

import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.preference.PreferenceManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.IdlingRegistry
import org.junit.Rule
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.Before
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.assertion.ViewAssertions.matches
import java.io.File
import androidx.test.espresso.matcher.ViewMatchers.withSubstring
import androidx.test.filters.MediumTest
import androidx.test.filters.LargeTest
import org.junit.Assume.assumeTrue
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import java.util.concurrent.TimeUnit
import androidx.camera.integration.antelope.MainActivity.Companion.antelopeIdlingResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import org.junit.After
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat

const val PREVIEW_BUFFER = "1500"

/**
 * Suite of tests that cover the major use cases for Antelope.
 *
 * Assumes device/emulator has a front and a back camera.
 *
 * Note: tests are suppressed for pre/post-submit testing as these tests exercise the camera
 * device thoroughly - failures that leave the device in a bad state can cause future tests to fail.
 */
@androidx.test.filters.Suppress
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AntelopeInstrumentedTests {
    @get: Rule
    var activityRule: ActivityTestRule<MainActivity> =
        ActivityTestRule(MainActivity::class.java)
    @get: Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)
    @get: Rule
    val writeStoragePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    @get: Rule
    val readStoragePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.READ_EXTERNAL_STORAGE)

    @Rule @JvmField
    var repeatRule: RepeatRule = RepeatRule()

    /**
     * On some API levels, permissions Rules do not always work but explicitly
     * using a shell command does.
     */
    @Before
    fun grantPermissions() {
        getInstrumentation().getUiAutomation().executeShellCommand(
            "pm grant " + activityRule.activity.applicationContext +
                " android.permission.CAMERA")
        getInstrumentation().getUiAutomation().executeShellCommand(
            "pm grant " + activityRule.activity.applicationContext +
                " android.permission.READ_EXTERNAL_STORAGE")
        getInstrumentation().getUiAutomation().executeShellCommand(
            "pm grant " + activityRule.activity.applicationContext +
                " android.permission.WRITE_EXTERNAL_STORAGE")
    }

    /**
     * Delete any pre-existing logs
     */
    @Before
    fun deleteLogs() {
        val activity = activityRule.activity as MainActivity
        deleteCSVFiles(activity)
    }

    /**
     * Make sure all system dialogs are closed from any previous tests
     */
    @Before
    fun closeSystemDialogs() {
        val activity = activityRule.activity as MainActivity
        activity.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    }

    /**
     * Setup idling resource timeouts and registry
     */
    @Before
    fun setupIdlingResources() {
        // Slow devices may take up to 2 minutes
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(2, TimeUnit.MINUTES)

        IdlingRegistry.getInstance().register(antelopeIdlingResource)
    }

    /**
     * Remove idling resource from the registry after a test
     */
    @After
    fun removeIdlingResources() {
        IdlingRegistry.getInstance().unregister(antelopeIdlingResource)
    }

    /**
     * Basic context sanity test
     */
    @Test
    @MediumTest
    fun test01ContextSanity() {
        val context = activityRule.activity.applicationContext
        Assert.assertEquals("androidx.camera.integration.antelope", context.packageName)
    }

    /**
     * Test log file deletion
     */
    @Test
    @MediumTest
    fun test02WriteandDeleteLogFiles() {
        val activity = activityRule.activity as MainActivity

        // Write a fake log file
        writeCSV(activity, "fakelogfile", "This is a fake log file")
        assertThat(isLogDirEmpty()).isFalse()

        // Delete all logs from the device
        deleteCSVFiles(activity)
        assertThat(isLogDirEmpty()).isTrue()
    }

    /**
     * Performs a single capture with the camera device 0 using the Camera 2 API
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test03SingleCaptureTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), PREVIEW_BUFFER)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        activity.runOnUiThread {
            activity.startSingleTest()
        }

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("Single Capture\nCamera")))
        assertThat(isLogDirEmpty()).isFalse()
    }

    /**
     * Performs a multi capture with the camera device 1 using the Camera 2 API
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test04MultiCaptureTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "MULTI_PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "3")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), PREVIEW_BUFFER)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)

        if (hasCamera("1"))
            prefEditor.putString(res.getString(R.string.settings_single_test_camera_key), "1")
        else
            prefEditor.putString(res.getString(R.string.settings_single_test_camera_key), "0")

        prefEditor.commit()

        activity.runOnUiThread {
            activity.startSingleTest()
        }

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("Multiple Captures\nCamera")))
        assertThat(isLogDirEmpty()).isFalse()
    }

    /**
     * Performs a multi capture "chained" test with camera device 0 using the Camera 2 API
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test05MultiCaptureChainedTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key),
            "MULTI_PHOTO_CHAIN")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Min")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "3")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), PREVIEW_BUFFER)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        activity.runOnUiThread {
            activity.startSingleTest()
        }

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(
            withSubstring("Multiple Captures (Chained)\nCamera")))
        assertThat(isLogDirEmpty()).isFalse()
    }

    /**
     * Starts a multi-capture test with camera device 0 using Camera 2 and aborts it after 5s
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test06AbortTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "MULTI_PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "30")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), PREVIEW_BUFFER)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        activity.runOnUiThread {
            activity.startSingleTest()

            // Abort the capture right after it started
            activity.abortTests()
        }

        // Check if test was aborted successfully and log directory is still empty
        onView(withId(R.id.text_log)).check(matches(withSubstring("ABORTED")))
        assertThat(isLogDirEmpty()).isTrue()
    }

    /**
     * Performs a single camera switch back->front->back
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test07SwitchCameraTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device doesn't have both camera 0 and 1, skip this test
        assumeTrue(hasCameraZeroAndOne())

        // Set up switch test
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "SWITCH_CAMERA")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), PREVIEW_BUFFER)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        activity.runOnUiThread {
            activity.startSingleTest()
        }

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("Switch Cameras\nCamera")))
        assertThat(isLogDirEmpty()).isFalse()
    }

    /**
     * Performs a single capture and saves the image to disk. Tests:
     *  - image was saved to disk
     *  - deleting images from settings menu works
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test08ImageSaveAndDeleteTest() {
        val activity = activityRule.activity as MainActivity
        val context = activityRule.activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Set up single a capture and save the photo
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera2")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), PREVIEW_BUFFER)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), false)
        prefEditor.commit()

        activity.runOnUiThread {
            activity.startSingleTest()
        }

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("Single Capture\nCamera")))

        // Check photo is on disk
        assertThat(isPhotoDirEmpty()).isFalse()

        // Delete all photos on the device
        deleteTestPhotos(activity)
        assertThat(isPhotoDirEmpty()).isTrue()
    }

    /**
     * Performs a multi capture with the camera device 0 using the Camera 1 API
     */
    @FlakyTest
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test09MultiCaptureCamera1Test() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "MULTI_PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "Camera1")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "3")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), PREVIEW_BUFFER)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        activity.runOnUiThread {
            MainActivity.camViewModel.getShouldOutputLog().value = true
            activity.startSingleTest()
        }

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("Multiple Captures\nCamera")))
        assertThat(isLogDirEmpty()).isFalse()
    }

    /**
     * Performs a multi capture with the camera device 0 using the Camera X API
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test10MultiCaptureCameraXTest() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Set up capture
        prefEditor.putString(res.getString(R.string.settings_single_test_type_key), "MULTI_PHOTO")
        prefEditor.putString(res.getString(R.string.settings_single_test_api_key), "CameraX")
        prefEditor.putString(res.getString(R.string.settings_single_test_imagesize_key), "Max")
        prefEditor.putString(res.getString(R.string.settings_single_test_focus_key), "Auto")
        prefEditor.putString(res.getString(R.string.settings_single_test_camera_key),
            getFirstCamera())
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "3")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), PREVIEW_BUFFER)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.commit()

        activity.runOnUiThread {
            MainActivity.camViewModel.getShouldOutputLog().value = true
            activity.startSingleTest()
        }

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("Multiple Captures\nCamera")))
        assertThat(isLogDirEmpty()).isFalse()
    }

    /**
     * Performs a full set of captures for all possible cameras/image sizes/tests for Camera2
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test11FullCaptureTestCamera2() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Set up maximum test coverage for Camera2
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_api_key),
            hashSetOf("Camera2"))
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_imagesize_key),
            res.getStringArray(R.array.array_settings_imagesize).toHashSet())
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_focus_key),
            res.getStringArray(R.array.array_settings_focus).toHashSet())

        if (hasCameraZeroAndOne())
            prefEditor.putBoolean(res.getString(R.string.settings_autotest_switchtest_key), true)
        else
            prefEditor.putBoolean(res.getString(R.string.settings_autotest_switchtest_key), false)

        prefEditor.putBoolean(res.getString(R.string.settings_autotest_cameras_key), true)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "1")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), PREVIEW_BUFFER)
        prefEditor.commit()

        activity.runOnUiThread {
            MainActivity.camViewModel.getShouldOutputLog().value = true
            activity.startMultiTest()
        }

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("DATE:")))
        assertThat(isLogDirEmpty()).isFalse()
    }

    /**
     * Performs a full set of captures for all possible cameras/image sizes/tests for CameraX
     */
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test12FullCaptureTestCameraX() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Set up maximum test coverage for CameraX
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_api_key),
            hashSetOf("CameraX"))
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_imagesize_key),
            res.getStringArray(R.array.array_settings_imagesize).toHashSet())
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_focus_key),
            res.getStringArray(R.array.array_settings_focus).toHashSet())

        if (hasFrontAndBackCamera())
            prefEditor.putBoolean(res.getString(R.string.settings_autotest_switchtest_key), true)
        else
            prefEditor.putBoolean(res.getString(R.string.settings_autotest_switchtest_key), false)

        prefEditor.putBoolean(res.getString(R.string.settings_autotest_cameras_key), true)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "1")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), PREVIEW_BUFFER)
        prefEditor.commit()

        activity.runOnUiThread {
            MainActivity.camViewModel.getShouldOutputLog().value = true
            activity.startMultiTest()
        }

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("DATE:")))
        assertThat(isLogDirEmpty()).isFalse()
    }

    /**
     * Performs a full set of captures for all possible cameras/image sizes/tests for Camera1
     */
    @FlakyTest
    @Test
    @LargeTest
    @RepeatRule.Repeat(times = 1)
    fun test13FullCaptureTestCamera1() {
        val activity = activityRule.activity as MainActivity
        val context = activity.applicationContext
        val res = context.resources
        val prefEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        // If device has no camera, skip this test
        assumeTrue(hasAnyCamera())

        // Set up maximum test coverage for Camera1
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_api_key),
            hashSetOf("Camera1"))
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_imagesize_key),
            res.getStringArray(R.array.array_settings_imagesize).toHashSet())
        prefEditor.putStringSet(res.getString(R.string.settings_autotest_focus_key),
            res.getStringArray(R.array.array_settings_focus).toHashSet())

        if (hasCameraZeroAndOne())
            prefEditor.putBoolean(res.getString(R.string.settings_autotest_switchtest_key), true)
        else
            prefEditor.putBoolean(res.getString(R.string.settings_autotest_switchtest_key), false)

        prefEditor.putBoolean(res.getString(R.string.settings_autotest_cameras_key), true)
        prefEditor.putBoolean(res.getString(R.string.settings_autodelete_key), true)
        prefEditor.putString(res.getString(R.string.settings_numtests_key), "1")
        prefEditor.putString(res.getString(R.string.settings_previewbuffer_key), PREVIEW_BUFFER)
        prefEditor.commit()

        activity.runOnUiThread {
            MainActivity.camViewModel.getShouldOutputLog().value = true
            activity.startMultiTest()
        }

        // Check if test was successful
        onView(withId(R.id.text_log)).check(matches(withSubstring("DATE:")))
        assertThat(isLogDirEmpty()).isFalse()
    }

    /**
     * Checks whether the default .csv log directory is empty
     */
    private fun isLogDirEmpty(): Boolean {
        val csvDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOCUMENTS), MainActivity.LOG_DIR)

        return if (csvDir.exists()) {
            val children = csvDir.listFiles()
            (children!!.isEmpty())
        } else {
            true
        }
    }

    /**
     * Checks whether the default image directory is empty
     */
    private fun isPhotoDirEmpty(): Boolean {
        val photoDir = File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DCIM), MainActivity.PHOTOS_DIR)

        return if (photoDir.exists()) {
            val children = photoDir.listFiles()
            (children!!.isEmpty())
        } else {
            true
        }
    }

    /**
     * Checks whether the test device has the given camera
     */
    private fun hasCamera(cameraId: String): Boolean {
        val activity = activityRule.activity as MainActivity
        val manager = activity.getSystemService(AppCompatActivity.CAMERA_SERVICE) as CameraManager
        return manager.cameraIdList.contains(cameraId)
    }

    /**
     * Checks whether the test device has a camera of the given type. Usually
     * CameraMetadata.LENS_FACING_BACK or CameraMetadata.LENS_FACING_FRONT
     */
    private fun hasCameraType(cameraType: Int): Boolean {
        val activity = activityRule.activity as MainActivity
        val manager = activity.getSystemService(AppCompatActivity.CAMERA_SERVICE) as CameraManager

        for (cameraId in manager.cameraIdList) {
            try {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (cameraFacing == cameraType) {
                    return true
                }
            } catch (e: CameraAccessException) {
                return false
            }
        }
        return false
    }

    /**
     * Checks if this devices has either a camera with id 0 or id 1
     */
    private fun hasAnyCamera(): Boolean = hasCamera("0") || hasCamera("1")

    /**
     * Checks if this devices has both a camera with id 0 and id 1
     */
    private fun hasCameraZeroAndOne(): Boolean = hasCamera("0") && hasCamera("1")

    /**
     * Checks if this devices has both a front and back camera
     */
    private fun hasFrontAndBackCamera(): Boolean = hasCameraType(CameraMetadata.LENS_FACING_BACK) &&
            hasCameraType(CameraMetadata.LENS_FACING_FRONT)

    /**
     * Determine what the first camera in the system is.
     *
     * Return 0 if camera 0 exists, 1 if camera 1 exists, otherwise empty string
     */
    private fun getFirstCamera(): String {
        if (hasCamera("0"))
            return "0"
        if (hasCamera("1"))
            return "1"

        return ""
    }
}