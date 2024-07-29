/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.extensions

/** Invalid extension mode */
const val INVALID_EXTENSION_MODE = -1

/** Extension is not enabled. Uses a non camera extension session (eg. camera2 or cameraX) */
const val EXTENSION_MODE_NONE = -1

/** Invalid lens facing */
const val INVALID_LENS_FACING = -1

/** Permissions request code */
const val PERMISSIONS_REQUEST_CODE = 42

/** Intent extra keys to pass necessary information between the caller and callee activities. */
object IntentExtraKey {
    /** Launches the activity with the specified CameraX implementation. */
    const val INTENT_EXTRA_CAMERA_IMPLEMENTATION = "camera_implementation"

    /**
     * Launches the activity with the specified direction of camera.
     *
     * Possible values for this intent key are listed in [CameraDirection]
     */
    const val INTENT_EXTRA_KEY_CAMERA_DIRECTION = "camera_direction"

    /**
     * Launches the activity with the specified lens facing of camera.
     *
     * Possible values for this intent key: [CameraMetadata#LENS_FACING_BACK] or
     * [CameraMetadata#LENS_FACING_FRONT].
     */
    const val INTENT_EXTRA_KEY_LENS_FACING = "LensFacing"

    /** Launches the activity with the specified id of camera. */
    const val INTENT_EXTRA_KEY_CAMERA_ID = "camera_id"

    /** Launches the activity with the specified extension mode. */
    const val INTENT_EXTRA_KEY_EXTENSION_MODE = "extension_mode"

    /**
     * The captured image will be deleted automatically if the intent used to launch the activity
     * includes the setting as true.
     */
    const val INTENT_EXTRA_KEY_DELETE_CAPTURED_IMAGE = "delete_captured_image"

    /**
     * Launches the activity for the specified test type.
     *
     * Possible values for this intent key are listed in [ExtensionTestType]
     */
    const val INTENT_EXTRA_KEY_TEST_TYPE = "TestType"

    /** Used to pass the captured image Uri to the caller activity. */
    const val INTENT_EXTRA_KEY_IMAGE_URI = "ImageUri"

    /**
     * Used to pass the rotation degrees of the captured image to the caller activity to show the
     * image in correct orientation.
     */
    const val INTENT_EXTRA_KEY_IMAGE_ROTATION_DEGREES = "ImageRotationDegrees"

    /** Used to pass the request code to the callee activity. */
    const val INTENT_EXTRA_KEY_REQUEST_CODE = "RequestCode"

    /** Used to pass the error code to the caller activity. */
    const val INTENT_EXTRA_KEY_ERROR_CODE = "ErrorCode"

    /** Used to pass the running mode to check. The valid values are debug and release. */
    const val INTENT_EXTRA_RUNNING_MODE_CHECK = "running_mode_check"

    /** Used to pass the result error message. */
    const val INTENT_EXTRA_RESULT_ERROR_MESSAGE = "result_error_message"
}

/** Implementation options */
object ImplementationOption {
    /** CameraX implementation option */
    const val CAMERA2_IMPLEMENTATION_OPTION: String = "camera2"
    /** Camera-pipe implementation option */
    const val CAMERA_PIPE_IMPLEMENTATION_OPTION: String = "camera_pipe"
}

/** Camera directions */
object CameraDirection {
    /** Backward direction */
    const val BACKWARD = "BACKWARD"

    /** Forward direction */
    const val FORWARD = "FORWARD"
}

/** Error Codes for validation activity results */
object ValidationErrorCode {
    /** No error */
    const val ERROR_CODE_NONE = 0

    /** Failed to bind the use cases to the lifecycle owner */
    const val ERROR_CODE_BIND_TO_LIFECYCLE_FAILED = 1

    /** The specified extension mode is not supported */
    const val ERROR_CODE_EXTENSION_MODE_NOT_SUPPORT = 2

    /** Failed to take picture */
    const val ERROR_CODE_TAKE_PICTURE_FAILED = 3

    /** Failed to save the captured image */
    const val ERROR_CODE_SAVE_IMAGE_FAILED = 4
}

/** Extension test types. */
object ExtensionTestType {
    const val TEST_TYPE_CAMERAX_EXTENSION = "CameraX Extension"
    const val TEST_TYPE_CAMERA2_EXTENSION = "Camera2 Extension"
    const val TEST_TYPE_CAMERA2_EXTENSION_STREAM_CONFIG_LATENCY =
        "Camera2 Extension Stream Config Latency"
}

/** Test result types */
object TestResultType {
    /** Extension mode is not supported on the camera device */
    const val TEST_RESULT_NOT_SUPPORTED = -1

    /** Not tested yet */
    const val TEST_RESULT_NOT_TESTED = 0

    /** Only part of the tests are tested */
    const val TEST_RESULT_PARTIALLY_TESTED = 1

    /** All tests have been run and all passed */
    const val TEST_RESULT_PASSED = 2

    /** All tests have been run and some items are failed */
    const val TEST_RESULT_FAILED = 3
}

/** Request result error codes for release apk test. */
object RequestResultErrorCode {
    /** None error happens. */
    const val RESULT_SUCCESS = 0
    /** Running mode (debug or release) incorrect. */
    const val RESULT_ERROR_RUNNING_MODE_INCORRECT = 1
    /** Permission requirements are not satisfied. */
    const val RESULT_ERROR_PERMISSION_NOT_SATISFIED = 2
    /** Failed to retrieve ExtensionsManager. */
    const val RESULT_ERROR_FAILED_TO_RETRIEVE_EXTENSIONS_MANAGER = 3
    /** Target testing extension mode is not supported on the target camera device. */
    const val RESULT_ERROR_EXTENSION_MOD_NOT_SUPPORTED = 4
    /** Incorrect camera implementation. */
    const val RESULT_ERROR_INCORRECT_CAMERA_IMPLEMENTATION = 5
    /** Failed to take a picture. */
    const val RESULT_ERROR_TAKE_PICTURE_FAILED = 6
}
