/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.integration.extensions.validation

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.os.Environment.DIRECTORY_DOCUMENTS
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.ExtensionTestType.TEST_TYPE_CAMERA2_EXTENSION
import androidx.camera.integration.extensions.ExtensionTestType.TEST_TYPE_CAMERAX_EXTENSION
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_FAILED
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_NOT_SUPPORTED
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_NOT_TESTED
import androidx.camera.integration.extensions.TestResultType.TEST_RESULT_PASSED
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.AVAILABLE_CAMERA2_EXTENSION_MODES
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.getCamera2ExtensionModeIdFromString
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.getCamera2ExtensionModeStringFromId
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.isCamera2ExtensionModeSupported
import androidx.camera.integration.extensions.utils.CameraSelectorUtil.createCameraSelectorById
import androidx.camera.integration.extensions.utils.ExtensionModeUtil.AVAILABLE_EXTENSION_MODES
import androidx.camera.integration.extensions.utils.ExtensionModeUtil.getExtensionModeIdFromString
import androidx.camera.integration.extensions.utils.ExtensionModeUtil.getExtensionModeStringFromId
import androidx.camera.integration.extensions.utils.FileUtil.copyTempFileToOutputLocation
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.net.toUri
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.Format
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val TAG = "TestResults"

private const val TEST_RESULTS_FILE_NAME = "TestResult.csv"
private const val TEST_RESULT_INDEX_TEST_TYPE = 0
private const val TEST_RESULT_INDEX_CAMERA_ID = 1
private const val TEST_RESULT_INDEX_EXTENSION_MODE = 2
private const val TEST_RESULT_INDEX_TEST_RESULT = 3

private const val TEST_RESULT_STRING_NOT_SUPPORTED = "NOT_SUPPORTED"
private const val TEST_RESULT_STRING_NOT_TESTED = "NOT_TESTED"
private const val TEST_RESULT_STRING_PASSED = "PASSED"
private const val TEST_RESULT_STRING_FAILED = "FAILED"

/**
 * A class to load, save and export the test results.
 */
class TestResults constructor(private val context: Context) {
    /**
     * Camera id to lens facing map.
     */
    private val cameraLensFacingMap = linkedMapOf<String, Int>()

    /**
     * Pair of <test type, camera id> to list of <extension mode, test result> map.
     */
    private val cameraExtensionResultMap =
        linkedMapOf<Pair<String, String>, LinkedHashMap<Int, Int>>()

    fun loadTestResults(
        cameraProvider: ProcessCameraProvider,
        extensionsManager: ExtensionsManager
    ) {
        initTestResult(cameraProvider, extensionsManager)
        refreshTestResultsFromFile()
    }

    fun getCameraLensFacingMap() = cameraLensFacingMap

    fun getCameraExtensionResultMap() = cameraExtensionResultMap

    /**
     * Saves the test results.
     *
     * The input parameter is pair of <test type, camera id> to list of
     * <extension mode, test result> map.
     */
    fun saveTestResults(
        cameraExtensionResultMap: LinkedHashMap<Pair<String, String>, LinkedHashMap<Int, Int>>
    ) {
        val testResultsFile = File(context.getExternalFilesDir(null), TEST_RESULTS_FILE_NAME)
        val outputStream = FileOutputStream(testResultsFile)

        val headerString = "Camera Id,Extension Mode,Test Result\n"
        outputStream.write(headerString.toByteArray())

        cameraExtensionResultMap.forEach { entry ->
            val (testType, cameraId) = entry.key
            entry.value.forEach {
                val (extensionMode, testResult) = it
                val extensionModeString = getExtensionModeStringFromId(testType, extensionMode)
                val testResultString = getTestResultStringFromId(testResult)
                val resultString = "$testType,$cameraId,$extensionModeString,$testResultString\n"
                outputStream.write(resultString.toByteArray())
            }
        }

        outputStream.close()
    }

    /**
     * Exports the test results to a CSV file under the Documents folder.
     *
     * @return the file path if it is successful to export the test results. Otherwise, null will
     * be returned.
     */
    fun exportTestResults(contentResolver: ContentResolver): String? {
        val testResultsFile = File(context.getExternalFilesDir(null), TEST_RESULTS_FILE_NAME)
        if (!testResultsFile.exists()) {
            Log.e(TAG, "Test result does not exist!")
            return null
        }

        val formatter: Format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        val savedFileName = "TestResult[${formatter.format(Calendar.getInstance().time)}].csv"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, savedFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/comma-separated-values")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "$DIRECTORY_DOCUMENTS/ExtensionsValidation"
            )
        }

        if (copyTempFileToOutputLocation(
                contentResolver,
                testResultsFile.toUri(),
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                contentValues
            ) != null
        ) {
            return "$DIRECTORY_DOCUMENTS/ExtensionsValidation/$savedFileName"
        }

        return null
    }

    fun resetTestResults(
        cameraProvider: ProcessCameraProvider,
        extensionsManager: ExtensionsManager
    ) {
        val testResultsFile = File(context.getExternalFilesDir(null), TEST_RESULTS_FILE_NAME)

        if (testResultsFile.exists()) {
            testResultsFile.delete()
        }

        cameraExtensionResultMap.clear()
        cameraLensFacingMap.clear()
        initTestResult(cameraProvider, extensionsManager)
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun initTestResult(
        cameraProvider: ProcessCameraProvider,
        extensionsManager: ExtensionsManager
    ) {
        val availableCameraIds = mutableListOf<String>()

        cameraProvider.availableCameraInfos.forEach {
            val cameraId = Camera2CameraInfo.from(it).cameraId
            availableCameraIds.add(cameraId)
            cameraLensFacingMap[cameraId] = cameraProvider.getLensFacingById(cameraId)
        }

        // Generates CameraX extension test items
        availableCameraIds.forEach { cameraId ->
            val testResultMap = linkedMapOf<Int, Int>()

            AVAILABLE_EXTENSION_MODES.forEach { mode ->
                val isSupported = extensionsManager.isExtensionAvailable(
                    createCameraSelectorById(cameraId),
                    mode
                )

                testResultMap[mode] =
                    if (isSupported) TEST_RESULT_NOT_TESTED else TEST_RESULT_NOT_SUPPORTED
            }

            cameraExtensionResultMap[Pair(TEST_TYPE_CAMERAX_EXTENSION, cameraId)] = testResultMap
        }

        if (Build.VERSION.SDK_INT < 31) {
            return
        }

        // Generates Camera2 extension test items
        availableCameraIds.forEach { cameraId ->
            val testResultMap = linkedMapOf<Int, Int>()

            AVAILABLE_CAMERA2_EXTENSION_MODES.forEach { mode ->
                val isSupported = isCamera2ExtensionModeSupported(context, cameraId, mode)

                testResultMap[mode] =
                    if (isSupported) TEST_RESULT_NOT_TESTED else TEST_RESULT_NOT_SUPPORTED
            }

            cameraExtensionResultMap[Pair(TEST_TYPE_CAMERA2_EXTENSION, cameraId)] = testResultMap
        }
    }

    private fun refreshTestResultsFromFile() {
        val testResultsFile = File(context.getExternalFilesDir(null), TEST_RESULTS_FILE_NAME)

        if (!testResultsFile.exists()) {
            return
        }

        val fileInputStream = FileInputStream(testResultsFile)
        val dataInputStream = DataInputStream(fileInputStream)
        val bufferedReader = BufferedReader(InputStreamReader(dataInputStream))

        var readHeader = false
        var lineContent = ""
        while ((bufferedReader.readLine()?.also { lineContent = it }) != null) {
            if (!readHeader) {
                readHeader = true
                continue
            }

            val values = lineContent.split(",")
            if (values.size != 4) {
                throw IllegalArgumentException("Extensions validation test results parsing error!")
            }

            val testType = values[TEST_RESULT_INDEX_TEST_TYPE]
            val cameraId = values[TEST_RESULT_INDEX_CAMERA_ID]
            val extensionResultMap = cameraExtensionResultMap[Pair(testType, cameraId)]
            val mode =
                getExtensionModeIdFromString(testType, values[TEST_RESULT_INDEX_EXTENSION_MODE])

            extensionResultMap?.set(
                mode,
                getTestResultIdFromString(values[TEST_RESULT_INDEX_TEST_RESULT])
            )
        }

        fileInputStream.close()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun ProcessCameraProvider.getLensFacingById(cameraId: String): Int {
        availableCameraInfos.forEach {
            val camera2CameraInfo = Camera2CameraInfo.from(it)

            if (camera2CameraInfo.cameraId == cameraId) {
                return camera2CameraInfo.getCameraCharacteristic(
                    CameraCharacteristics.LENS_FACING)!!
            }
        }

        throw IllegalArgumentException("Can't retrieve lens facing info for camera $cameraId")
    }

    private fun getTestResultStringFromId(result: Int): String = when (result) {
        TEST_RESULT_NOT_SUPPORTED -> TEST_RESULT_STRING_NOT_SUPPORTED
        TEST_RESULT_FAILED -> TEST_RESULT_STRING_FAILED
        TEST_RESULT_PASSED -> TEST_RESULT_STRING_PASSED
        else -> TEST_RESULT_STRING_NOT_TESTED
    }

    private fun getTestResultIdFromString(result: String): Int = when (result) {
        TEST_RESULT_STRING_NOT_SUPPORTED -> TEST_RESULT_NOT_SUPPORTED
        TEST_RESULT_STRING_FAILED -> TEST_RESULT_FAILED
        TEST_RESULT_STRING_PASSED -> TEST_RESULT_PASSED
        else -> TEST_RESULT_NOT_TESTED
    }

    companion object {
        fun getExtensionModeStringFromId(testType: String, extensionMode: Int) =
            if (testType == TEST_TYPE_CAMERAX_EXTENSION) {
                getExtensionModeStringFromId(extensionMode)
            } else if (testType == TEST_TYPE_CAMERA2_EXTENSION && Build.VERSION.SDK_INT >= 31) {
                getCamera2ExtensionModeStringFromId(extensionMode)
            } else {
                throw RuntimeException(
                    "Something went wrong about testType ($testType) and device API level" +
                        " (${Build.VERSION.SDK_INT})."
                )
            }

        fun getExtensionModeIdFromString(testType: String, extensionModeString: String) =
            if (testType == TEST_TYPE_CAMERAX_EXTENSION) {
                getExtensionModeIdFromString(extensionModeString)
            } else if (testType == TEST_TYPE_CAMERA2_EXTENSION && Build.VERSION.SDK_INT >= 31) {
                getCamera2ExtensionModeIdFromString(extensionModeString)
            } else {
                throw RuntimeException(
                    "Something went wrong about testType ($testType) and device API level" +
                        " (${Build.VERSION.SDK_INT})."
                )
            }
    }
}
