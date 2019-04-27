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

/** The different types of tests Antelop can perform */
enum class TestType {
    /** No test  */
    NONE,
    /** Open and close camera only*/
    INIT,
    /** Start preview stream */
    PREVIEW,
    /** Capture a single image */
    PHOTO,
    /** Capture multiple photos, closing/opening camera between each capture */
    MULTI_PHOTO,
    /** Capture multiple photos, leave camera open between captures */
    MULTI_PHOTO_CHAIN,
    /** Switch between two cameras one time. Preview stream only, no capture */
    SWITCH_CAMERA,
    /** Switch between two cameras multiple times, Preview only, no capture */
    MULTI_SWITCH
}

/**
 * Configuration and state variables for the current running test.
 *
 * Also contains the TestResults object to record the results.
 */
class TestConfig(
    /** Name of test */
    var testName: String = "",
    /** Enum type of test */
    var currentRunningTest: TestType = TestType.NONE,
    /** API to use for test (Camera 1, 2, or X) */
    var api: CameraAPI = CameraAPI.CAMERA2,
    /** Size of capture to request */
    var imageCaptureSize: ImageCaptureSize = ImageCaptureSize.MAX,
    /** Auto-focus, Continuous focus, or Fixe-focus */
    var focusMode: FocusMode = FocusMode.AUTO,
    /** Camera ID */
    var camera: String = "0",
    /** Camera array to use for the switch camera test */
    var switchTestCameras: Array<String> = arrayOf("0", "1"),
    /** Convenience variable, currently active camera during switch test */
    var switchTestCurrentCamera: String = "0",
    /** Save the original camera for a switch test */
    var switchTestRealCameraId: String = "0",
    /** Semaphor for first onActive preview state */
    var isFirstOnActive: Boolean = true,
    /** Semaphor for first completed capture */
    var isFirstOnCaptureComplete: Boolean = true,
    /** Semaphor for when the test is completed */
    var testFinished: Boolean = false,
    /** Accumulate test results in this object */
    var testResults: TestResults = TestResults()
) {

    /**
     * Set up the TestResults object to reflect the test configuration
     */
    fun setupTestResults() {
        testResults.testName = testName
        testResults.testType = currentRunningTest
        testResults.camera = camera
        testResults.cameraId = camera
        testResults.cameraAPI = api
        testResults.imageCaptureSize = imageCaptureSize
        testResults.focusMode = focusMode
    }
}