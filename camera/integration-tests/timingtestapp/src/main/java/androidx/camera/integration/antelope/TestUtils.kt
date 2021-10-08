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

package androidx.camera.integration.antelope

import androidx.camera.integration.antelope.MainActivity.Companion.antelopeIdlingResource
import com.google.common.math.Stats
import androidx.camera.integration.antelope.MainActivity.Companion.cameraParams
import androidx.camera.integration.antelope.MainActivity.Companion.isSingleTestRunning
import androidx.camera.integration.antelope.MainActivity.Companion.logd
import androidx.camera.integration.antelope.MainActivity.Companion.testsRemaining
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

/**
 * During a multiple-test run, this should be called after each test is completed. Record the result
 * and call the automatic test runner to start the next test.
 *
 * If all test are completed, post result to screen and save the log.
 */
fun postTestResults(activity: MainActivity, testConfig: TestConfig) {
    MainActivity.testRun.add(testConfig.testResults)
    testsRemaining--

    var log = ""
    var csv = ""

    if (0 >= testsRemaining) {
        if (!isSingleTestRunning) {
            log += testSummaryString(activity, MainActivity.testRun)
            csv += testSummaryCSV(activity, MainActivity.testRun)
        }

        for ((index, result) in MainActivity.testRun.withIndex()) {
            // TODO with test summary we can combine these two cases
            if (0 == index) {
                log += result.toString(activity, false)
                csv += result.toCSV(activity, false)
            } else {
                log += result.toString(activity, false)
                csv += result.toCSV(activity, false)
            }
        }

        activity.resetUIAfterTest()
        activity.updateLog(log)
        writeCSV(activity, DeviceInfo().deviceShort, csv)

        // Indicate to Espresso that a test run has ended
        try {
            logd("Decrementing AntelopeIdlingResource")
            antelopeIdlingResource.decrement()
        } catch (ex: IllegalStateException) {
            logd("Antelope idling resource decremented below 0. This should never happen.")
        }
    } else {
        autoTestRunner(activity)
    }
}

/**
 * Set up the TestConfig object for a single test
 */
fun createSingleTestConfig(activity: MainActivity): TestConfig {
    val config = TestConfig()

    config.apply {
        when (PrefHelper.getSingleTestType(activity)) {
            "INIT" -> {
                testName = "Camera Open/Close"
                currentRunningTest = TestType.INIT
            }
            "PREVIEW" -> {
                testName = "Preview Start"
                currentRunningTest = TestType.PREVIEW
            }
            "SWITCH_CAMERA" -> {
                testName = "Switch Cameras"
                currentRunningTest = TestType.SWITCH_CAMERA
            }
            "MULTI_SWITCH" -> {
                testName = "Switch Cameras (Multiple)"
                currentRunningTest = TestType.MULTI_SWITCH
            }
            "MULTI_PHOTO" -> {
                testName = "Multiple Captures"
                currentRunningTest = TestType.MULTI_PHOTO
            }
            "MULTI_PHOTO_CHAIN" -> {
                testName = "Multiple Captures (Chained)"
                currentRunningTest = TestType.MULTI_PHOTO_CHAIN
            }
            else -> {
                testName = "Single Capture"
                currentRunningTest = TestType.PHOTO
            }
        }

        api = CameraAPI.valueOf(PrefHelper.getSingleTestApi(activity).uppercase())
        focusMode = FocusMode.valueOf(PrefHelper.getSingleTestFocus(activity).uppercase())
        imageCaptureSize =
            ImageCaptureSize.valueOf(PrefHelper.getSingleTestImageSize(activity).uppercase())
        camera = PrefHelper.getSingleTestCamera(activity)
        config.setupTestResults()
    }

    return config
}

/**
 * Create a test configuration given the test's name and the currently selected test values
 */
fun createTestConfig(testName: String): TestConfig {
    val config = TestConfig(testName)
    config.camera = MainActivity.camViewModel.getCurrentCamera().value.toString()
    config.api = MainActivity.camViewModel.getCurrentAPI().value ?: CameraAPI.CAMERA2
    config.imageCaptureSize =
        MainActivity.camViewModel.getCurrentImageCaptureSize().value ?: ImageCaptureSize.MAX

    // If we don't have auto-focus, we set the focus mode to FIXED
    if (MainActivity.cameraParams.get(config.camera)?.hasAF ?: true)
        config.focusMode = MainActivity.camViewModel.getCurrentFocusMode().value ?: FocusMode.AUTO
    else
        config.focusMode = FocusMode.FIXED

    config.setupTestResults()

    return config
}

/**
 * For multiple tests, configure the list of TestConfigs to run
 */
fun setupAutoTestRunner(activity: MainActivity) {
    MainActivity.autoTestConfigs.clear()
    val cameras: ArrayList<String> = PrefHelper.getCameraIds(activity, MainActivity.cameraParams)
    val logicalCameras: ArrayList<String> =
        PrefHelper.getLogicalCameraIds(MainActivity.cameraParams)
    val apis: ArrayList<CameraAPI> = PrefHelper.getAPIs(activity)
    val imageSizes: ArrayList<ImageCaptureSize> = PrefHelper.getImageSizes(activity)
    val focusModes: ArrayList<FocusMode> = PrefHelper.getFocusModes(activity)
    val testTypes: ArrayList<TestType> = ArrayList()
    val doSwitchTest: Boolean = PrefHelper.getSwitchTest(activity)

    testTypes.add(TestType.MULTI_PHOTO)
    testTypes.add(TestType.MULTI_PHOTO_CHAIN)

    if (doSwitchTest)
        testTypes.add(TestType.MULTI_SWITCH)

    MainActivity.testRun.clear()

    for (camera in cameras) {
        for (api in apis) {
            // Camera1 does not have access to physical cameras, only logical 0 and 1
            // Some devices have no camera or only 1 front-facing camera (like Chromebooks)
            // so we need to make sure they exist
            if ((CameraAPI.CAMERA1 == api) && !logicalCameras.contains(camera))
                continue

            // Currently CameraX only supports FRONT and BACK
            if ((CameraAPI.CAMERAX == api) && !(camera.equals("0") || camera.equals("1")))
                continue

            for (imageSize in imageSizes) {
                for (focusMode in focusModes) {
                    if (FocusMode.CONTINUOUS == focusMode) {
                        // If camera is fixed-focus, only run the AUTO test
                        if (!(MainActivity.cameraParams.get(camera)?.hasAF ?: true))
                            continue
                    }

                    for (testType in testTypes) {
                        // Camera1 does not have chaining capabilities
                        if ((CameraAPI.CAMERA1 == api) && (TestType.MULTI_PHOTO_CHAIN == testType))
                            continue

                        // For now we only test 0->1->0, just add this test for the first "camera"
                        // TODO: figure out a way to test different permutations
                        if ((TestType.MULTI_SWITCH == testType) && !camera.equals(cameras.first()))
                            continue

                        // Switch test doesn't do a capture don't repeat for all capture sizes
                        if (imageSize == ImageCaptureSize.MIN && testType == TestType.MULTI_SWITCH)
                            continue

                        // Switch test doesn't do a capture so don't repeat for all focus modes
                        if (focusMode != FocusMode.AUTO && testType == TestType.MULTI_SWITCH)
                            continue

                        // If this is a fixed focus lens, focusMode here has been set to auto,
                        // set it to fixed in the TestConfig
                        var realFocusMode: FocusMode = focusMode
                        if (!(MainActivity.cameraParams.get(camera)?.hasAF ?: true))
                            realFocusMode = FocusMode.FIXED

                        var testName = when (api) {
                            CameraAPI.CAMERA1 -> "Camera1"
                            CameraAPI.CAMERA2 -> "Camera2"
                            CameraAPI.CAMERAX -> "CameraX"
                        }

                        testName += " - "
                        testName += when (imageSize) {
                            ImageCaptureSize.MIN -> "Min"
                            ImageCaptureSize.MAX -> "Max"
                        }
                        testName += " image size - Camera device "

                        if (testType != TestType.MULTI_SWITCH) {
                            testName += camera
                            testName += " "
                        }

                        testName += when (realFocusMode) {
                            FocusMode.AUTO -> "(auto-focus)"
                            FocusMode.CONTINUOUS -> "(continuous focus)"
                            else -> "(fixed-focus)"
                        }

                        testName += " - "
                        testName += when (testType) {
                            TestType.MULTI_PHOTO -> "Multiple Captures"
                            TestType.MULTI_PHOTO_CHAIN -> "Multiple Captures (chained)"
                            TestType.MULTI_SWITCH -> "Switch Camera"
                            else -> "unknown test"
                        }

                        val testConfig =
                            TestConfig(testName, testType, api, imageSize, realFocusMode, camera)
                        testConfig.setupTestResults()
                        MainActivity.autoTestConfigs.add(testConfig)
                    }
                }
            }
        }
    }

    // Add Test X of Y string to test names
    for ((index, testConfig) in MainActivity.autoTestConfigs.withIndex()) {
        testConfig.testName = "" + (index + 1) + " of " +
            MainActivity.autoTestConfigs.size + ": " + testConfig.testName
    }

    testsRemaining = MainActivity.autoTestConfigs.size
}

/**
 * Run the list of tests in autoTestConfigs
 */
fun autoTestRunner(activity: MainActivity) {
    if (MainActivity.cameras.isEmpty()) {
        testsRemaining = 0
        return
    }

    // If something goes wrong or we are aborted, stop testing
    if (0 == testsRemaining)
        return

    val currentTest: Int = MainActivity.autoTestConfigs.size - testsRemaining + 1
    val currentConfig: TestConfig = MainActivity.autoTestConfigs.get(currentTest - 1)
    MainActivity.logd("autoTestRun about to run: " + currentConfig.testName)

    activity.runOnUiThread {
        if (testsRemaining == MainActivity.autoTestConfigs.size)
            activity.setupUIForTest(currentConfig, false)
        else
            activity.setupUIForTest(currentConfig, true)
    }

    multiCounter = 0
    initializeTest(activity, cameraParams.get(currentConfig.camera), currentConfig)
}

/**
 * For an array of TestResults, generate a high-level summary of the most important values.
 *
 * This mostly consists of values for default rear camera.
 */
fun testSummaryString(activity: MainActivity, allTestResults: ArrayList<TestResults>): String {
    var output = ""

    if (allTestResults.isEmpty())
        return output

    val mainCamera = getMainCamera(activity, allTestResults)

    val c2Auto = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA2,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_PHOTO
    )
    val c2AutoChain = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA2,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_PHOTO_CHAIN
    )
    val c2Caf = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA2,
        ImageCaptureSize.MAX, FocusMode.CONTINUOUS, TestType.MULTI_PHOTO
    )
    val c2CafChain = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA2,
        ImageCaptureSize.MAX, FocusMode.CONTINUOUS, TestType.MULTI_PHOTO_CHAIN
    )
    val c2AutoMin = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA2,
        ImageCaptureSize.MIN, FocusMode.AUTO, TestType.MULTI_PHOTO
    )
    val c2Switch = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA2,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_SWITCH
    )
    val c1Auto = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA1,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_PHOTO
    )
    val c1Caf = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA1,
        ImageCaptureSize.MAX, FocusMode.CONTINUOUS, TestType.MULTI_PHOTO
    )
    val c1Switch = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA1,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_SWITCH
    )
    val cXAuto = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERAX,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_PHOTO
    )
    val cXCaf = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERAX,
        ImageCaptureSize.MAX, FocusMode.CONTINUOUS, TestType.MULTI_PHOTO
    )
    val cXSwitch = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERAX,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_SWITCH
    )

    // Header
    val dateFormatter = SimpleDateFormat("d MMM yyyy - kk'h'mm")
    val cal: Calendar = Calendar.getInstance()
    output += "DATE: " + dateFormatter.format(cal.time) + " (Antelope " +
        getVersionName(activity) + ")\n"

    output += "DEVICE: " + MainActivity.deviceInfo.device + "\n\n"
    output += "CAMERAS:\n"
    for (camera in MainActivity.cameras)
        output += camera + "\n"
    output += "\n"

    // Test summary
    output += "HIGH-LEVEL OVERVIEW:\n"

    output += "Capture (Cam2): " + meanOfSumOfTwoArrays(c2Auto.capture, c2Auto.imageready) +
        ", Cap chained (Cam2): " +
        meanOfSumOfTwoArrays(c2AutoChain.capture, c2AutoChain.imageready) +
        "\nCapture CAF (Cam2): " + meanOfSumOfTwoArrays(c2Caf.capture, c2Caf.imageready) +
        ", Chained CAF (Cam2): " +
        meanOfSumOfTwoArrays(c2CafChain.capture, c2CafChain.imageready) +
        "\nCapture (Cam1): " + meanOfSumOfTwoArrays(c1Auto.capture, c1Auto.imageready) +
        ", Cap CAF (Cam1): " + meanOfSumOfTwoArrays(c1Caf.capture, c1Caf.imageready) +
        "\nCapture (CamX): " + meanOfSumOfTwoArrays(cXAuto.capture, cXAuto.imageready) +
        ", Cap CAF (CamX): " + meanOfSumOfTwoArrays(cXCaf.capture, cXCaf.imageready) +
        "\nSwitch 1->2 (Cam2): " +
        mean(c2Switch.switchToSecond) + ", Switch 1->2 (Cam1): " + mean(c1Switch.switchToSecond) +
        ", Switch 1->2 (CamX): " + mean(cXSwitch.switchToSecond) +
        ", Switch 2->1 (Cam2): " +
        mean(c2Switch.switchToFirst) + ", Switch 2->1 (Cam1): " + mean(c1Switch.switchToFirst) +
        ", Switch 2->1 (CamX): " + mean(cXSwitch.switchToFirst) +
        "\nCam2 Open: " + meanOfSumOfTwoArrays(c2Auto.initialization, c2Auto.previewStart) +
        ", Cam1 Open: " + meanOfSumOfTwoArrays(c1Auto.initialization, c1Auto.previewStart) +
        "\nCam2 Close: " + meanOfSumOfTwoArrays(c2Auto.previewClose, c2Auto.cameraClose) +
        ", Cam1 Close: " + meanOfSumOfTwoArrays(c1Auto.previewClose, c1Auto.cameraClose) +
        "\n∆ Min to Max Size: " + (
        numericalMean(c2Auto.capture) +
            numericalMean(c2Auto.imageready) - numericalMean(c2AutoMin.capture) -
            numericalMean(c2AutoMin.imageready)
        ) +
        ", Init->Image saved (Cam2): " + mean(c2Auto.totalNoPreview) +
        "\n"

    output += "\n"

    return output
}

/**
 * For an array of TestResults, generate a high-level summary of the most important values in a
 * comma-separated .csv string.
 *
 * This mostly consists of values for default rear camera.
 */
fun testSummaryCSV(activity: MainActivity, allTestResults: ArrayList<TestResults>): String {
    var output = ""

    if (allTestResults.isEmpty())
        return output

    val mainCamera = getMainCamera(activity, allTestResults)

    val c2Auto = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA2,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_PHOTO
    )
    val c2AutoChain = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA2,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_PHOTO_CHAIN
    )
    val c2Caf = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA2,
        ImageCaptureSize.MAX, FocusMode.CONTINUOUS, TestType.MULTI_PHOTO
    )
    val c2CafChain = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA2,
        ImageCaptureSize.MAX, FocusMode.CONTINUOUS, TestType.MULTI_PHOTO_CHAIN
    )
    val c2AutoMin = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA2,
        ImageCaptureSize.MIN, FocusMode.AUTO, TestType.MULTI_PHOTO
    )
    val c2Switch = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA2,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_SWITCH
    )
    val c1Auto = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA1,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_PHOTO
    )
    val c1Caf = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA1,
        ImageCaptureSize.MAX, FocusMode.CONTINUOUS, TestType.MULTI_PHOTO
    )
    val c1Switch = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERA1,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_SWITCH
    )
    val cXAuto = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERAX,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_PHOTO
    )
    val cXCaf = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERAX,
        ImageCaptureSize.MAX, FocusMode.CONTINUOUS, TestType.MULTI_PHOTO
    )
    val cXSwitch = findTest(
        allTestResults, mainCamera, CameraAPI.CAMERAX,
        ImageCaptureSize.MAX, FocusMode.AUTO, TestType.MULTI_SWITCH
    )

    // Header
    val dateFormatter = SimpleDateFormat("d MMM yyyy - kk'h'mm")
    val cal: Calendar = Calendar.getInstance()
    output += "DATE: " + dateFormatter.format(cal.time) + " (Antelope " +
        getVersionName(activity) + ")" + "\n"

    output += "DEVICE: " + MainActivity.deviceInfo.device + "\n" + "\n"
    output += "CAMERAS: " + "\n"
    for (camera in MainActivity.cameras)
        output += camera + "\n"
    output += "\n"

    // Test summary
    output += "HIGH-LEVEL OVERVIEW:\n"

    output += ","
    output += "Capture (Cam2)" + "," + "Cap chained (Cam2)" + "," +
        "Capture CAF (Cam2)" + "," + "Chained CAF (Cam2)" + "," +
        "Capture (Cam1)" + "," + "Cap CAF (Cam1)" + "," +
        "Capture (CamX)" + "," + "Cap CAF (CamX)" + "," +
        "Switch 1->2 (Cam2)" + "," + "Switch 1->2 (Cam1)" + "," + "Switch 1->2 (CamX)" + "," +
        "Switch 2->1 (Cam2)" + "," + "Switch 2->1 (Cam1)" + "," + "Switch 2->1 (CamX)" + "," +
        "Cam2 Open" + "," + "Cam1 Open" + "," +
        "Cam2 Close" + "," + "Cam1 Close" + "," +
        "∆ Min to Max Size" + "," +
        "Init->Image saved (Cam2)" +
        "\n"

    output += ","
    output += "" + meanOfSumOfTwoArrays(c2Auto.capture, c2Auto.imageready) + "," +
        meanOfSumOfTwoArrays(c2AutoChain.capture, c2AutoChain.imageready)
    output += "," + meanOfSumOfTwoArrays(c2Caf.capture, c2Caf.imageready) + "," +
        meanOfSumOfTwoArrays(c2CafChain.capture, c2CafChain.imageready)
    output += "," + meanOfSumOfTwoArrays(c1Auto.capture, c1Auto.imageready) + "," +
        meanOfSumOfTwoArrays(c1Caf.capture, c1Caf.imageready)
    output += "," + meanOfSumOfTwoArrays(cXAuto.capture, cXAuto.imageready) + "," +
        meanOfSumOfTwoArrays(cXCaf.capture, cXCaf.imageready)
    output += "," + mean(c2Switch.switchToSecond) + "," + mean(c1Switch.switchToSecond)
    output += "," + mean(cXSwitch.switchToSecond)
    output += "," + mean(c2Switch.switchToFirst) + "," + mean(c1Switch.switchToFirst)
    output += "," + mean(cXSwitch.switchToFirst)
    output += "," + meanOfSumOfTwoArrays(c2Auto.initialization, c2Auto.previewStart) + "," +
        meanOfSumOfTwoArrays(c1Auto.initialization, c1Auto.previewStart)
    output += "," + meanOfSumOfTwoArrays(c2Auto.previewClose, c2Auto.cameraClose) + "," +
        meanOfSumOfTwoArrays(c1Auto.previewClose, c1Auto.cameraClose)
    output += "," + (
        numericalMean(c2Auto.capture) + numericalMean(c2Auto.imageready) -
            numericalMean(c2AutoMin.capture) - numericalMean(c2AutoMin.imageready)
        )
    output += "," + mean(c2Auto.totalNoPreview) + "\n"

    output += "\n"
    return output
}

/**
 * Search an array of TestResults for the first test result that matches the given parameters
 */
fun findTest(
    allTestResults: ArrayList<TestResults>,
    camera: String,
    api: CameraAPI,
    imageCaptureSize: ImageCaptureSize,
    focusMode: FocusMode,
    testType: TestType
): TestResults {

    for (testResult in allTestResults) {
        // Look for the matching test result
        if (testResult.camera.equals(camera) &&
            testResult.cameraAPI == api &&
            testResult.imageCaptureSize == imageCaptureSize &&
            (testResult.focusMode == focusMode || testResult.focusMode == FocusMode.FIXED) &&
            testResult.testType == testType
        ) {
            return testResult
        }
    }

    // Return empty test result
    return TestResults()
}

/**
 * The mean of a given array of longs, as a string
 */
fun mean(array: ArrayList<Long>): String {
    if (array.isEmpty()) {
        return "n/a"
    } else
        return Stats.meanOf(array).roundToInt().toString()
}

/**
 * The mean of a given array of longs, as a double
 */
fun numericalMean(array: ArrayList<Long>): Double {
    if (array.isEmpty())
        return 0.0
    else
        return Stats.meanOf(array)
}

/**
 * The mean of two arrays of longs added together, as a string
 */
fun meanOfSumOfTwoArrays(array1: ArrayList<Long>, array2: ArrayList<Long>): String {
    if (array1.isEmpty() && array2.isEmpty()) {
        return "n/a"
    }
    if (array1.isEmpty())
        return mean(array2)
    if (array2.isEmpty())
        return mean(array1)
    else
        return (Stats.meanOf(array1) + Stats.meanOf(array2)).roundToInt().toString()
}

/**
 * Find the "main" camera id, priority is: first rear facing physical, first rear-facing logical,
 * first camera in the system.
 */
fun getMainCamera(activity: MainActivity, allTestResults: ArrayList<TestResults>): String {
    val mainCamera = allTestResults.first().cameraId

    // Return the first rear-facing camera
    for (param in MainActivity.cameraParams) {
        if (!param.value.isFront && !param.value.isExternal)

        // If only logical cameras, first rear-facing is fine
            if (PrefHelper.getOnlyLogical(activity)) {
                logd("The MAIN camera id is:" + param.value.id)
                return param.value.id

                // Otherwise, make sure this is a physical camera
            } else {
                if (param.value.physicalCameras.contains(param.value.id)) {
                    logd("The MAIN camera id is:" + param.value.id)
                    return param.value.id
                }
            }
    }

    return mainCamera
}

/**
 * Return the version name of the Activity
 */
fun getVersionName(activity: MainActivity): String {
    val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
    return packageInfo.versionName
}