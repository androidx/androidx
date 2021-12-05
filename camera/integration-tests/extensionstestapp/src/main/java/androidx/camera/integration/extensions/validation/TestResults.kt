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

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

private const val TEST_RESULTS_FILE_NAME = "TestResult.csv"
private const val TEST_RESULT_INDEX_CAMERA_ID = 0
private const val TEST_RESULT_INDEX_EXTENSION_MODE = 1
private const val TEST_RESULT_INDEX_TEST_RESULT = 2

private const val TEST_RESULT_STRING_NOT_SUPPORTED = "NOT_SUPPORTED"
private const val TEST_RESULT_STRING_NOT_TESTED = "NOT_TESTED"
private const val TEST_RESULT_STRING_PASSED = "PASSED"
private const val TEST_RESULT_STRING_FAILED = "FAILED"

class TestResults constructor(private val context: Context) {
    private val cameraLensFacingMap = linkedMapOf<String, Int>()
    private val cameraExtensionResultMap = linkedMapOf<String, LinkedHashMap<Int, Int>>()

    fun loadTestResults(
        cameraProvider: ProcessCameraProvider,
        extensionsManager: ExtensionsManager
    ) {
        initTestResult(cameraProvider, extensionsManager)
        refreshTestResultsFromFile()
    }

    fun getCameraLensFacingMap() = cameraLensFacingMap

    fun getCameraExtensionResultMap() = cameraExtensionResultMap

    fun saveTestResults(cameraExtensionResultMap: LinkedHashMap<String, LinkedHashMap<Int, Int>>) {
        val testResultsFile = File(context.getExternalFilesDir(null), TEST_RESULTS_FILE_NAME)
        val outputStream = FileOutputStream(testResultsFile)

        val headerString = "Camera Id,Extension Mode,Test Result\n"
        outputStream.write(headerString.toByteArray())

        cameraExtensionResultMap.forEach {
            val cameraId = it.key
            it.value.forEach {
                val resultString =
                    "$cameraId,${getExtensionModeStringFromId(it.key)},${
                        getTestResultStringFromId(
                            it.value
                        )
                    }\n"
                outputStream.write(resultString.toByteArray())
            }
        }

        outputStream.close()
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
        cameraProvider.availableCameraInfos.forEach {
            val cameraId = Camera2CameraInfo.from(it).cameraId
            val testResultMap = linkedMapOf<Int, Int>()

            EXTENSION_MODES.forEach { mode ->
                val isSupported = extensionsManager.isExtensionAvailable(
                    createCameraSelectorById(cameraId),
                    mode
                )

                testResultMap[mode] =
                    if (isSupported) TEST_RESULT_NOT_TESTED else TEST_RESULT_NOT_SUPPORTED
            }

            if (!testResultMap.isEmpty()) {
                cameraExtensionResultMap[cameraId] = testResultMap
                cameraLensFacingMap[cameraId] = cameraProvider.getLensFacingById(cameraId)
            }
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
            if (values.size != 3) {
                throw IllegalArgumentException("Extensions validation test results parsing error!")
            }

            val extensionResultMap = cameraExtensionResultMap[values[TEST_RESULT_INDEX_CAMERA_ID]]
            val mode = getExtensionModeIdFromString(values[TEST_RESULT_INDEX_EXTENSION_MODE])
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
            var camera2CameraInfo = Camera2CameraInfo.from(it)

            if (camera2CameraInfo.cameraId.equals(cameraId)) {
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

        @OptIn(ExperimentalCamera2Interop::class)
        fun createCameraSelectorById(cameraId: String) =
            CameraSelector.Builder().addCameraFilter(CameraFilter { cameraInfos ->
                cameraInfos.forEach {
                    if (Camera2CameraInfo.from(it).cameraId.equals(cameraId)) {
                        return@CameraFilter listOf<CameraInfo>(it)
                    }
                }

                throw IllegalArgumentException("No camera can be find for id: $cameraId")
            }).build()

        fun getExtensionModeStringFromId(mode: Int): String = when (mode) {
            ExtensionMode.BOKEH -> EXTENSION_MODE_STRING_BOKEH
            ExtensionMode.HDR -> EXTENSION_MODE_STRING_HDR
            ExtensionMode.NIGHT -> EXTENSION_MODE_STRING_NIGHT
            ExtensionMode.FACE_RETOUCH -> EXTENSION_MODE_STRING_FACE_RETOUCH
            ExtensionMode.AUTO -> EXTENSION_MODE_STRING_AUTO
            else -> throw IllegalArgumentException("Invalid extension mode!!")
        }

        fun getExtensionModeIdFromString(mode: String): Int = when (mode) {
            EXTENSION_MODE_STRING_BOKEH -> ExtensionMode.BOKEH
            EXTENSION_MODE_STRING_HDR -> ExtensionMode.HDR
            EXTENSION_MODE_STRING_NIGHT -> ExtensionMode.NIGHT
            EXTENSION_MODE_STRING_FACE_RETOUCH -> ExtensionMode.FACE_RETOUCH
            EXTENSION_MODE_STRING_AUTO -> ExtensionMode.AUTO
            else -> throw IllegalArgumentException("Invalid extension mode!!")
        }

        const val INVALID_EXTENSION_MODE = -1

        val EXTENSION_MODES = arrayOf(
            ExtensionMode.BOKEH,
            ExtensionMode.HDR,
            ExtensionMode.NIGHT,
            ExtensionMode.FACE_RETOUCH,
            ExtensionMode.AUTO
        )

        private const val EXTENSION_MODE_STRING_BOKEH = "BOKEH"
        private const val EXTENSION_MODE_STRING_HDR = "HDR"
        private const val EXTENSION_MODE_STRING_NIGHT = "NIGHT"
        private const val EXTENSION_MODE_STRING_FACE_RETOUCH = "FACE RETOUCH"
        private const val EXTENSION_MODE_STRING_AUTO = "AUTO"

        const val TEST_RESULT_NOT_SUPPORTED = -1
        const val TEST_RESULT_NOT_TESTED = 0
        const val TEST_RESULT_PARTIALLY_TESTED = 1
        const val TEST_RESULT_PASSED = 2
        const val TEST_RESULT_FAILED = 3
    }
}
