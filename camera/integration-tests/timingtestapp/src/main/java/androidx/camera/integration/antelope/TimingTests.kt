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

import androidx.camera.integration.antelope.MainActivity.Companion.cameraParams
import androidx.camera.integration.antelope.MainActivity.Companion.logd
import androidx.camera.integration.antelope.cameracontrollers.camera1OpenCamera
import androidx.camera.integration.antelope.cameracontrollers.camera2OpenCamera
import androidx.camera.integration.antelope.cameracontrollers.cameraXOpenCamera
import androidx.camera.integration.antelope.cameracontrollers.cameraXTakePicture
import androidx.camera.integration.antelope.cameracontrollers.closeAllCameras
import androidx.camera.integration.antelope.cameracontrollers.closePreviewAndCamera
import androidx.camera.integration.antelope.cameracontrollers.initializeStillCapture

// Keeps track of what iteration of a repeated test is occurring
internal var multiCounter: Int = 0

internal fun initializeTest(
    activity: MainActivity,
    params: CameraParams?,
    config: TestConfig
) {

    if (null == params)
        return

    // Camera1 cannot directly access physical cameras. If we try, abort.
    if ((CameraAPI.CAMERA1 == config.api) &&
        !(PrefHelper.getLogicalCameraIds(activity, cameraParams).contains(config.camera))) {
        activity.resetUIAfterTest()
        activity.updateLog("ABORTED: Camera1 API cannot access camera with id:" + config.camera,
            false, false)
        return
    }

    when (config.currentRunningTest) {
        TestType.INIT -> runInitTest(activity, params, config)
        TestType.PREVIEW ->
            runPreviewTest(activity, params, config)
        TestType.SWITCH_CAMERA ->
            runSwitchTest(activity, params, config)
        TestType.MULTI_SWITCH ->
            runMultiSwitchTest(activity, params, config)
        TestType.PHOTO ->
            runPhotoTest(activity, params, config)
        TestType.MULTI_PHOTO ->
            runMultiPhotoTest(activity, params, config)
        TestType.MULTI_PHOTO_CHAIN ->
            runMultiPhotoChainTest(activity, params, config)
        TestType.NONE -> Unit
    }
}

/**
 * Run the INIT test
 */
internal fun runInitTest(
    activity: MainActivity,
    params: CameraParams,
    config: TestConfig
) {

    logd("Running init test")
    activity.startBackgroundThread(params)
    activity.showProgressBar(true)

    closeAllCameras(activity, config)

    setupImageReader(activity, params, config)
    params.timer = CameraTimer()
    config.currentRunningTest = TestType.INIT
    params.timer.testStart = System.currentTimeMillis()
    beginTest(activity, params, config)
}

/**
 * Run the SWITCH test
 */
internal fun runSwitchTest(activity: MainActivity, params: CameraParams, config: TestConfig) {
    // For switch test, always go from default back camera to default front camera and back 0->1->0
    // TODO: Can we handle different permutations of physical cameras?
    if (!PrefHelper.getLogicalCameraIds(activity, cameraParams).contains("0") ||
        !PrefHelper.getLogicalCameraIds(activity, cameraParams).contains("1")) {
        activity.resetUIAfterTest()
        activity.updateLog("ABORTED: Camera 0 and 1 needed for Switch test.",
            false, false)
        return
    }

    config.switchTestCameras = arrayOf("0", "1")
    config.switchTestCurrentCamera = "0"

    logd("Running switch test")
    logd("Starting with camera: " + config.switchTestCurrentCamera)
    activity.startBackgroundThread(params)
    activity.showProgressBar(true)

    closeAllCameras(activity, config)

    setupImageReader(activity, params, config)
    params.timer = CameraTimer()
    config.currentRunningTest = TestType.SWITCH_CAMERA
    params.timer.testStart = System.currentTimeMillis()
    beginTest(activity, params, config)
}

/**
 * Run the MULTI_SWITCH test
 */
internal fun runMultiSwitchTest(activity: MainActivity, params: CameraParams, config: TestConfig) {
    // For switch test, always go from default back camera to default front camera and back 0->1->0
    // TODO: Can we handle different permutations of physical cameras?
    if (!PrefHelper.getLogicalCameraIds(activity, cameraParams).contains("0") ||
        !PrefHelper.getLogicalCameraIds(activity, cameraParams).contains("1")) {
        activity.resetUIAfterTest()
        activity.updateLog("ABORTED: Camera 0 and 1 needed for Switch test.",
            false, false)
        return
    }

    config.switchTestCameras = arrayOf("0", "1")

    if (0 == multiCounter) {
        // New test
        logd("Running multi switch test")
        config.switchTestCurrentCamera = "0"
        activity.startBackgroundThread(params)
        activity.showProgressBar(true, 0)
        multiCounter = PrefHelper.getNumTests(activity)
        config.currentRunningTest = TestType.MULTI_SWITCH
        config.testFinished = false
    } else {
        // Add previous result
        params.timer.testEnd = System.currentTimeMillis()
        activity.showProgressBar(true, precentageCompleted(activity, multiCounter))
        logd("In Multi Switch Test. Counter: " + multiCounter)

        config.testResults.initialization.add(params.timer.openEnd - params.timer.openStart)
        config.testResults.previewStart.add(params.timer.previewEnd - params.timer.previewStart)
        config.testResults.switchToSecond
            .add(params.timer.switchToSecondEnd - params.timer.switchToSecondStart)
        config.testResults.switchToFirst
            .add(params.timer.switchToFirstEnd - params.timer.switchToFirstStart)
        config.testResults.previewClose
            .add(params.timer.previewCloseEnd - params.timer.previewCloseStart)
        config.testResults.cameraClose
            .add(params.timer.cameraCloseEnd - params.timer.cameraCloseStart)
        config.testResults.total.add(params.timer.testEnd - params.timer.testStart)

        config.testFinished = false
        config.isFirstOnActive = true
    }

    setupImageReader(activity, params, config)
    params.timer = CameraTimer()
    params.timer.testStart = System.currentTimeMillis()
    beginTest(activity, params, config)
}

/**
 * Run the PREVIEW test
 */
internal fun runPreviewTest(activity: MainActivity, params: CameraParams, config: TestConfig) {
    logd("Running preview test")
    activity.startBackgroundThread(params)
    activity.showProgressBar(true)

    closeAllCameras(activity, config)

    setupImageReader(activity, params, config)
    params.timer = CameraTimer()
    config.currentRunningTest = TestType.PREVIEW
    params.timer.testStart = System.currentTimeMillis()
    beginTest(activity, params, config)
}

/**
 * Run the PHOTO (single capture) test
 */
internal fun runPhotoTest(activity: MainActivity, params: CameraParams, config: TestConfig) {
    logd("Running photo test")
    activity.startBackgroundThread(params)
    activity.showProgressBar(true)

    closeAllCameras(activity, config)

    setupImageReader(activity, params, config)
    params.timer = CameraTimer()
    config.currentRunningTest = TestType.PHOTO
    params.timer.testStart = System.currentTimeMillis()

    logd("About to start photo test. " + config.currentRunningTest.toString())
    beginTest(activity, params, config)
}

/**
 * Run the MULTI_PHOTO (repeated capture) test
 */
internal fun runMultiPhotoTest(activity: MainActivity, params: CameraParams, config: TestConfig) {
    if (0 == multiCounter) {
        // New test
        logd("Running multi photo test")
        activity.startBackgroundThread(params)
        activity.showProgressBar(true, 0)
        multiCounter = PrefHelper.getNumTests(activity)
        config.currentRunningTest = TestType.MULTI_PHOTO
        logd("About to start multi photo test. multi_counter: " + multiCounter + " and test: " +
            config.currentRunningTest.toString())
    } else {
        // Add previous result
        params.timer.testEnd = System.currentTimeMillis()
        activity.showProgressBar(true, precentageCompleted(activity, multiCounter))
        logd("In Multi Photo Test. Counter: " + multiCounter)

        config.testResults.initialization.add(params.timer.openEnd - params.timer.openStart)
        config.testResults.previewStart.add(params.timer.previewEnd - params.timer.previewStart)
        config.testResults.previewFill
            .add(params.timer.previewFillEnd - params.timer.previewFillStart)
        config.testResults.autofocus.add(params.timer.autofocusEnd - params.timer.autofocusStart)
        config.testResults.captureNoAF.add((params.timer.captureEnd - params.timer.captureStart) -
            (params.timer.autofocusEnd - params.timer.autofocusStart))
        config.testResults.capture.add(params.timer.captureEnd - params.timer.captureStart)
        config.testResults.imageready
            .add(params.timer.imageReaderEnd - params.timer.imageReaderStart)
        config.testResults.capturePlusImageReady
            .add((params.timer.captureEnd - params.timer.captureStart) +
            (params.timer.imageReaderEnd - params.timer.imageReaderStart))
        config.testResults.imagesave
            .add(params.timer.imageSaveEnd - params.timer.imageSaveStart)
        config.testResults.isHDRPlus.add(params.timer.isHDRPlus)
        config.testResults.previewClose
            .add(params.timer.previewCloseEnd - params.timer.previewCloseStart)
        config.testResults.cameraClose
            .add(params.timer.cameraCloseEnd - params.timer.cameraCloseStart)
        config.testResults.total.add(params.timer.testEnd - params.timer.testStart)
        config.testResults.totalNoPreview.add((params.timer.testEnd - params.timer.testStart) -
            (params.timer.previewFillEnd - params.timer.previewFillStart))
        config.testFinished = false
        config.isFirstOnActive = true
        config.isFirstOnCaptureComplete = true
    }

    closeAllCameras(activity, config)

    setupImageReader(activity, params, config)
    params.timer = CameraTimer()
    params.timer.testStart = System.currentTimeMillis()
    beginTest(activity, params, config)
}

/**
 * Run the MULTI_PHOTO_CHAIN (multiple captures, do not close camera) test
 */
internal fun runMultiPhotoChainTest(
    activity: MainActivity,
    params: CameraParams,
    config: TestConfig
) {

    // Cannot chain with Camera 1, run default test
    if (CameraAPI.CAMERA1 == config.api) {
        logd("Cannot run Chain test with Camera 1, running regular multi-test instead")
        config.currentRunningTest = TestType.MULTI_PHOTO
        runMultiPhotoTest(activity, params, config)
        return
    }

    if (0 == multiCounter) {
        // New test
        logd("Running multi photo (chain) test")
        activity.startBackgroundThread(params)
        activity.showProgressBar(true, 0)
        multiCounter = PrefHelper.getNumTests(activity)
        params.timer = CameraTimer()
        params.timer.testStart = System.currentTimeMillis()
        config.currentRunningTest = TestType.MULTI_PHOTO_CHAIN
        logd("About to start multi chain test. multi_counter: " + multiCounter + " and test: " +
            config.currentRunningTest.toString())

        closeAllCameras(activity, config)

        setupImageReader(activity, params, config)
        beginTest(activity, params, config)
    } else {
        // Add previous result
        activity.showProgressBar(true, precentageCompleted(activity, multiCounter))

        if (config.api == CameraAPI.CAMERA1) {
            beginTest(activity, params, config)
        } else {

            // Camera2 and CameraX
            config.testResults.autofocus
                .add(params.timer.autofocusEnd - params.timer.autofocusStart)
            config.testResults.captureNoAF
                .add((params.timer.captureEnd - params.timer.captureStart) -
                (params.timer.autofocusEnd - params.timer.autofocusStart))
            config.testResults.capture.add(params.timer.captureEnd - params.timer.captureStart)
            config.testResults.imageready
                .add(params.timer.imageReaderEnd - params.timer.imageReaderStart)
            config.testResults.capturePlusImageReady
                .add((params.timer.captureEnd - params.timer.captureStart) +
                (params.timer.imageReaderEnd - params.timer.imageReaderStart))
            config.testResults.imagesave
                .add(params.timer.imageSaveEnd - params.timer.imageSaveStart)
            config.testResults.isHDRPlus.add(params.timer.isHDRPlus)
            config.testFinished = false

            params.timer.clearImageTimers()
            config.isFirstOnCaptureComplete = true

            when (config.api) {
                CameraAPI.CAMERA2 -> initializeStillCapture(activity, params, config)
                CameraAPI.CAMERAX -> cameraXTakePicture(activity, params, config)
            }
        }
    }
}

/**
 * A test has ended. Depending on which test and if we are at the beginning, middle or end of a
 * repeated test, record the results and repeat/return,
 */
internal fun testEnded(activity: MainActivity, params: CameraParams?, config: TestConfig) {
    if (null == params)
        return
    logd("In testEnded. multi_counter: " + multiCounter + " and test: " +
        config.currentRunningTest.toString())

    when (config.currentRunningTest) {

        TestType.INIT -> {
            params.timer.testEnd = System.currentTimeMillis()
            logd("Test ended")
            config.testResults.initialization.add(params.timer.openEnd - params.timer.openStart)
            config.testResults.cameraClose
                .add(params.timer.cameraCloseEnd - params.timer.cameraCloseStart)
            config.testResults.total.add(params.timer.testEnd - params.timer.testStart)
        }

        TestType.PREVIEW -> {
            params.timer.testEnd = System.currentTimeMillis()
            logd("Test ended")
            config.testResults.initialization.add(params.timer.openEnd - params.timer.openStart)
            config.testResults.previewStart.add(params.timer.previewEnd - params.timer.previewStart)
            config.testResults.previewClose
                .add(params.timer.previewCloseEnd - params.timer.previewCloseStart)
            config.testResults.cameraClose
                .add(params.timer.cameraCloseEnd - params.timer.cameraCloseStart)
            config.testResults.total.add(params.timer.testEnd - params.timer.testStart)
        }

        TestType.SWITCH_CAMERA -> {
            params.timer.testEnd = System.currentTimeMillis()
            logd("Test ended")
            config.testResults.initialization.add(params.timer.openEnd - params.timer.openStart)
            config.testResults.previewStart.add(params.timer.previewEnd - params.timer.previewStart)
            config.testResults.switchToSecond
                .add(params.timer.switchToSecondEnd - params.timer.switchToSecondStart)
            config.testResults.switchToFirst
                .add(params.timer.switchToFirstEnd - params.timer.switchToFirstStart)
            config.testResults.previewClose
                .add(params.timer.previewCloseEnd - params.timer.previewCloseStart)
            config.testResults.cameraClose
                .add(params.timer.cameraCloseEnd - params.timer.cameraCloseStart)
            config.testResults.total.add(params.timer.testEnd - params.timer.testStart)
        }

        TestType.MULTI_SWITCH -> {
            val lastResult = params.timer.captureEnd - params.timer.captureStart

            if (1 == multiCounter) {
                params.timer.testEnd = System.currentTimeMillis()
                config.testFinished = false // Reset flag

                logd("Test ended")
                activity.showProgressBar(true, precentageCompleted(activity, multiCounter))

                config.testResults.initialization.add(params.timer.openEnd - params.timer.openStart)
                config.testResults.previewStart
                    .add(params.timer.previewEnd - params.timer.previewStart)
                config.testResults.switchToSecond
                    .add(params.timer.switchToSecondEnd - params.timer.switchToSecondStart)
                config.testResults.switchToFirst
                    .add(params.timer.switchToFirstEnd - params.timer.switchToFirstStart)
                config.testResults.previewClose
                    .add(params.timer.previewCloseEnd - params.timer.previewCloseStart)
                config.testResults.cameraClose
                    .add(params.timer.cameraCloseEnd - params.timer.cameraCloseStart)
                config.testResults.total.add(params.timer.testEnd - params.timer.testStart)

                multiCounter = 0
            } else {
                logd("Switch " + (Math.abs(multiCounter - PrefHelper.getNumTests(activity)) + 1) +
                    " completed.")
                multiCounter--
                runMultiSwitchTest(activity, params, config)
                return
            }
        }

        TestType.PHOTO -> {
            params.timer.testEnd = System.currentTimeMillis()
            logd("Test ended")
            config.testResults.initialization.add(params.timer.openEnd - params.timer.openStart)
            config.testResults.previewStart.add(params.timer.previewEnd - params.timer.previewStart)
            config.testResults.previewFill
                .add(params.timer.previewFillEnd - params.timer.previewFillStart)
            config.testResults.autofocus
                .add(params.timer.autofocusEnd - params.timer.autofocusStart)
            config.testResults.captureNoAF
                .add((params.timer.captureEnd - params.timer.captureStart) -
                (params.timer.autofocusEnd - params.timer.autofocusStart))
            config.testResults.capture.add(params.timer.captureEnd - params.timer.captureStart)
            config.testResults.imageready
                .add(params.timer.imageReaderEnd - params.timer.imageReaderStart)
            config.testResults.capturePlusImageReady
                .add((params.timer.captureEnd - params.timer.captureStart) +
                (params.timer.imageReaderEnd - params.timer.imageReaderStart))
            config.testResults.imagesave
                .add(params.timer.imageSaveEnd - params.timer.imageSaveStart)
            config.testResults.isHDRPlus
                .add(params.timer.isHDRPlus)
            config.testResults.previewClose
                .add(params.timer.previewCloseEnd - params.timer.previewCloseStart)
            config.testResults.cameraClose
                .add(params.timer.cameraCloseEnd - params.timer.cameraCloseStart)
            config.testResults.total.add(params.timer.testEnd - params.timer.testStart)
            config.testResults.totalNoPreview.add((params.timer.testEnd - params.timer.testStart) -
                (params.timer.previewFillEnd - params.timer.previewFillStart))
        }

        TestType.MULTI_PHOTO -> {
            val lastResult = params.timer.captureEnd - params.timer.captureStart

            if (1 == multiCounter) {
                params.timer.testEnd = System.currentTimeMillis()
                config.testFinished = false // Reset flag

                logd("Test ended")
                activity.showProgressBar(true, precentageCompleted(activity, multiCounter))

                config.testResults.initialization.add(params.timer.openEnd - params.timer.openStart)
                config.testResults.previewStart
                    .add(params.timer.previewEnd - params.timer.previewStart)
                config.testResults.previewFill
                    .add(params.timer.previewFillEnd - params.timer.previewFillStart)
                config.testResults.autofocus
                    .add(params.timer.autofocusEnd - params.timer.autofocusStart)
                config.testResults.captureNoAF
                    .add((params.timer.captureEnd - params.timer.captureStart) -
                    (params.timer.autofocusEnd - params.timer.autofocusStart))
                config.testResults.capture.add(params.timer.captureEnd - params.timer.captureStart)
                config.testResults.imageready
                    .add(params.timer.imageReaderEnd - params.timer.imageReaderStart)
                config.testResults.capturePlusImageReady
                    .add((params.timer.captureEnd - params.timer.captureStart) +
                    (params.timer.imageReaderEnd - params.timer.imageReaderStart))
                config.testResults.imagesave
                    .add(params.timer.imageSaveEnd - params.timer.imageSaveStart)
                config.testResults.isHDRPlus.add(params.timer.isHDRPlus)
                config.testResults.previewClose
                    .add(params.timer.previewCloseEnd - params.timer.previewCloseStart)
                config.testResults.cameraClose
                    .add(params.timer.cameraCloseEnd - params.timer.cameraCloseStart)
                config.testResults.total.add(params.timer.testEnd - params.timer.testStart)
                config.testResults.totalNoPreview
                    .add((params.timer.testEnd - params.timer.testStart) -
                    (params.timer.previewFillEnd - params.timer.previewFillStart))

                closeAllCameras(activity, config)

                multiCounter = 0
            } else {
                logd("Capture " + (Math.abs(multiCounter - PrefHelper.getNumTests(activity)) + 1) +
                    " completed: " + lastResult + "ms")
                multiCounter--
                runMultiPhotoTest(activity, params, config)
                return
            }
        }

        TestType.MULTI_PHOTO_CHAIN -> {
            val lastResult = params.timer.captureEnd - params.timer.captureStart

            if (1 == multiCounter) {

                // If this is a chain test, the camera may still be open
                if (params.isOpen) {
                    config.testFinished = true
                    closePreviewAndCamera(activity, params, config)
                    return
                }

                params.timer.testEnd = System.currentTimeMillis()
                logd("Test ended")
                config.testFinished = false // Reset flag

                activity.showProgressBar(true, precentageCompleted(activity, multiCounter))

                config.testResults.initialization.add(params.timer.openEnd -
                    params.timer.openStart)
                config.testResults.previewStart.add(params.timer.previewEnd -
                    params.timer.previewStart)
                config.testResults.previewFill.add(params.timer.previewFillEnd -
                    params.timer.previewFillStart)
                config.testResults.autofocus.add(params.timer.autofocusEnd -
                    params.timer.autofocusStart)
                config.testResults.captureNoAF
                    .add((params.timer.captureEnd - params.timer.captureStart) -
                    (params.timer.autofocusEnd - params.timer.autofocusStart))
                config.testResults.capture.add(params.timer.captureEnd - params.timer.captureStart)
                config.testResults.imageready.add(params.timer.imageReaderEnd -
                    params.timer.imageReaderStart)
                config.testResults.capturePlusImageReady.add((params.timer.captureEnd -
                    params.timer.captureStart) +
                    (params.timer.imageReaderEnd - params.timer.imageReaderStart))
                config.testResults.imagesave.add(params.timer.imageSaveEnd -
                    params.timer.imageSaveStart)
                config.testResults.isHDRPlus.add(params.timer.isHDRPlus)
                config.testResults.previewClose.add(params.timer.previewCloseEnd -
                    params.timer.previewCloseStart)
                config.testResults.cameraClose.add(params.timer.cameraCloseEnd -
                    params.timer.cameraCloseStart)
                config.testResults.total.add(params.timer.testEnd - params.timer.testStart)
                config.testResults.totalNoPreview
                    .add((params.timer.testEnd - params.timer.testStart) -
                    (params.timer.previewFillEnd - params.timer.previewFillStart))

                closeAllCameras(activity, config)

                multiCounter = 0
            } else {
                logd("Capture " + (Math.abs(multiCounter - PrefHelper.getNumTests(activity)) + 1) +
                    " completed: " + lastResult + "ms")
                multiCounter--
                runMultiPhotoChainTest(activity, params, config)
                return
            }
        }
    }

    multiCounter = 0
    postTestResults(activity, config)
}

/**
 * Calculate the percentage of repeated tests that are complete
 */
fun precentageCompleted(activity: MainActivity, testCounter: Int): Int {
    return (100 * (PrefHelper.getNumTests(activity) - testCounter)) /
        PrefHelper.getNumTests(activity)
}

/**
 * Test is configured, begin it based on the API in the test config
 */
internal fun beginTest(activity: MainActivity, params: CameraParams?, testConfig: TestConfig) {
    if (null == params)
        return

    when (testConfig.api) {
        CameraAPI.CAMERA1 -> {
            // Camera 1 doesn't have its own threading built-in
            val runnable = Runnable {
                camera1OpenCamera(activity, params, testConfig)
            }
            params.backgroundHandler?.post(runnable)
        }

        CameraAPI.CAMERA2 -> {
            camera2OpenCamera(activity, params, testConfig)
        }

        CameraAPI.CAMERAX -> {
            cameraXOpenCamera(activity, params, testConfig)
        }
    }
}
