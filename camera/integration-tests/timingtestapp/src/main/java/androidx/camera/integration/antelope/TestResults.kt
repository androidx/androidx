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
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.google.common.math.Quantiles
import com.google.common.math.Stats
import androidx.camera.integration.antelope.MainActivity.Companion.logd
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Contains the results for a specific test. Most of the variables are arrays to accommodate tests
 * with multiple repetitions (MULTI_PHOTO, MULTI_PHOTO_CHAINED, MULTI_SWITCH, etc.)
 */
class TestResults {
    /** Name of test */
    var testName: String = ""
    /** Human readable camera name */
    var camera: String = ""
    /** Camera ID */
    var cameraId: String = ""
    /** Which API was used (1, 2, or X) */
    var cameraAPI: CameraAPI = CameraAPI.CAMERA2
    /** Image size that was requested */
    var imageCaptureSize: ImageCaptureSize = ImageCaptureSize.MAX
    /** Auto-focus, continuous focus, or fixed-foxus */
    var focusMode: FocusMode = FocusMode.AUTO
    /** Enum type of the test requested */
    var testType: TestType = TestType.NONE
    /** Time take to open camera */
    var initialization: ArrayList<Long> = ArrayList<Long>()
    /** Time taken for the preview to start */
    var previewStart: ArrayList<Long> = ArrayList<Long>()
    /** Time taken for the preview to run before next step in the test */
    var previewFill: ArrayList<Long> = ArrayList<Long>()
    /** Time taken to switch from first to second cameras */
    var switchToSecond: ArrayList<Long> = ArrayList<Long>()
    /** Time taken to switch from second to first cameras */
    var switchToFirst: ArrayList<Long> = ArrayList<Long>()
    /** Time taken for the auto-focus routine to complete */
    var autofocus: ArrayList<Long> = ArrayList<Long>()
    /** Time taken for image capture, not including auto-focus delay */
    var captureNoAF: ArrayList<Long> = ArrayList<Long>()
    /** Time taken for image capture, including auto-focus delay (if applicable) */
    var capture: ArrayList<Long> = ArrayList<Long>()
    /** Time taken after image is captured for it to be ready in the ImageReader */
    var imageready: ArrayList<Long> = ArrayList<Long>()
    /** Time taken for capture and the image to appear in the ImageReader */
    var capturePlusImageReady: ArrayList<Long> = ArrayList<Long>()
    /** Time taken to save image to disk */
    var imagesave: ArrayList<Long> = ArrayList<Long>()
    /** Time taken to close the preview stream */
    var previewClose: ArrayList<Long> = ArrayList<Long>()
    /** Time taken to close the camera */
    var cameraClose: ArrayList<Long> = ArrayList<Long>()
    /** Time taken for the entire test */
    var total: ArrayList<Long> = ArrayList<Long>()
    /** Time taken for the entire test not including filling the preview stream */
    var totalNoPreview: ArrayList<Long> = ArrayList<Long>()
    /** Was the image captured an HDR+ image? */
    var isHDRPlus: ArrayList<Boolean> = ArrayList<Boolean>()

    /**
     * Format results into a human readable string
     */
    fun toString(activity: MainActivity, header: Boolean): String {
        var output = ""

        if (header) {
            val dateFormatter = SimpleDateFormat("d MMM yyyy - kk'h'mm")
            val cal: Calendar = Calendar.getInstance()
            output += "DATE: " + dateFormatter.format(cal.time) +
                " (Antelope " + getVersionName(activity) + ")\n"

            output += "DEVICE: " + MainActivity.deviceInfo.device + "\n\n"
            output += "CAMERAS:\n"
            for (camera in MainActivity.cameras)
                output += camera + "\n"
            output += "\n"
        }

        output += testName + "\n"
        output += "Camera: " + camera + "\n"
        output += "API: " + cameraAPI + "\n"
        output += "Focus Mode: " + focusMode + "\n"
        output += "Image Capture Size: " + imageCaptureSize + "\n\n"

        output += outputResultLine("Camera open", initialization)
        output += outputResultLine("Preview start", previewStart)
        output += outputResultLine("Preview buffer", previewFill)

        when (focusMode) {
            FocusMode.CONTINUOUS -> {
                output += outputResultLine("Capture (continuous focus)", capture)
            }
            FocusMode.FIXED -> {
                output += outputResultLine("Capture (fixed-focus)", capture)
            }
            else -> {
                // CameraX doesn't allow us insight into autofocus
                if (CameraAPI.CAMERAX == cameraAPI) {
                    output += outputResultLine("Capture incl. autofocus", capture)
                } else {
                    output += outputResultLine("Autofocus", autofocus)
                    output += outputResultLine("Capture", captureNoAF)
                    output += outputResultLine("Capture incl. autofocus", capture)
                }
            }
        }

        output += outputResultLine("Image ready", imageready)
        output += outputResultLine("Cap + img ready", capturePlusImageReady)
        output += outputResultLine("Image save", imagesave)
        output += outputResultLine("Switch to 2nd", switchToSecond)
        output += outputResultLine("Switch to 1st", switchToFirst)
        output += outputResultLine("Preview close", previewClose)
        output += outputResultLine("Camera close", cameraClose)
        output += outputBooleanResultLine("HDR+", isHDRPlus)
        output += outputResultLine("Total", total)
        output += outputResultLine("Total w/o preview buffer", totalNoPreview)

        if (1 < capturePlusImageReady.size) {
            val captureStats = Stats.of(capturePlusImageReady)
            output += "Capture range: " + captureStats.min() + " - " + captureStats.max() + "\n"
            output += "Capture mean: " + captureStats.mean() +
                " (" + captureStats.count() + " captures)\n"
            output += "Capture median: " + Quantiles.median().compute(capturePlusImageReady) + "\n"
            output += "Capture standard deviation: " + captureStats.sampleStandardDeviation() + "\n"
        }
        output += "Total batch time: " + Stats.of(total).sum() + "\n\n\n"
        return output
    }

    /**
     * Format results to a comma-based .csv string
     */
    fun toCSV(activity: MainActivity, header: Boolean = true): String {
        val numCommas = PrefHelper.getNumTests(activity)

        var output = ""

        if (header) {
            val dateFormatter = SimpleDateFormat("d MMM yyyy - kk'h'mm")
            val cal: Calendar = Calendar.getInstance()
            output += "DATE: " + dateFormatter.format(cal.time) + " (Antelope " +
                getVersionName(activity) + ")" + outputCommas(numCommas) + "\n"

            output += "DEVICE: " + MainActivity.deviceInfo.device + outputCommas(numCommas) +
                "\n" + outputCommas(numCommas) + "\n"
            output += "CAMERAS: " + outputCommas(numCommas) + "\n"
            for (camera in MainActivity.cameras)
                output += camera + outputCommas(numCommas) + "\n"
            output += outputCommas(numCommas) + "\n"
        }

        output += testName + outputCommas(numCommas) + outputCommas(numCommas) + "\n"
        output += "Camera: " + camera + outputCommas(numCommas) + "\n"
        output += "API: " + cameraAPI + outputCommas(numCommas) + "\n"
        output += "Focus Mode: " + focusMode + outputCommas(numCommas) + "\n"
        output += "Image Capture Size: " + imageCaptureSize + outputCommas(numCommas) + "\n" +
            outputCommas(numCommas) + "\n"

        output += outputResultLine("Camera open", initialization, numCommas, true)
        output += outputResultLine("Preview start", previewStart, numCommas, true)
        output += outputResultLine("Preview buffer", previewFill, numCommas, true)

        when (focusMode) {
            FocusMode.CONTINUOUS -> {
                output += outputResultLine("Capture (continuous focus)", capture,
                    numCommas, true)
            }
            FocusMode.FIXED -> {
                output += outputResultLine("Capture (fixed-focus)", capture,
                    numCommas, true)
            }
            else -> {
                // CameraX doesn't allow us insight into autofocus
                if (CameraAPI.CAMERAX == cameraAPI) {
                    output += outputResultLine("Capture incl. autofocus", capture,
                        numCommas, true)
                } else {
                    output += outputResultLine("Autofocus", autofocus,
                        numCommas, true)
                    output += outputResultLine("Capture", captureNoAF,
                        numCommas, true)
                    output += outputResultLine("Capture incl. autofocus", capture,
                        numCommas, true)
                }
            }
        }

        output += outputResultLine("Image ready", imageready, numCommas, true)
        output += outputResultLine("Cap + img ready", capturePlusImageReady,
            numCommas, true)
        output += outputResultLine("Image save", imagesave, numCommas, true)
        output += outputResultLine("Switch to 2nd", switchToSecond, numCommas, true)
        output += outputResultLine("Switch to 1st", switchToFirst, numCommas, true)
        output += outputResultLine("Preview close", previewClose, numCommas, true)
        output += outputResultLine("Camera close", cameraClose, numCommas, true)
        output += outputBooleanResultLine("HDR+", isHDRPlus, numCommas, true)
        output += outputResultLine("Total", total, numCommas, true)
        output += outputResultLine("Total w/o preview buffer", totalNoPreview,
            numCommas, true)

        if (1 < capturePlusImageReady.size) {
            val captureStats = Stats.of(capturePlusImageReady)
            output += "Capture range:," + captureStats.min() + " - " + captureStats.max() +
                outputCommas(numCommas) + "\n"
            output += "Capture mean " + " (" + captureStats.count() + " captures):," +
                Stats.of(capturePlusImageReady).mean() + outputCommas(numCommas) + "\n"
            output += "Capture median:," + Quantiles.median().compute(capturePlusImageReady) +
                outputCommas(numCommas) + "\n"
            output += "Capture standard deviation:," +
                captureStats.sampleStandardDeviation() + outputCommas(numCommas) + "\n"
        }

        output += "Total batch time:," + Stats.of(total).sum() + outputCommas(numCommas) + "\n"

        output += outputCommas(numCommas) + "\n"
        output += outputCommas(numCommas) + "\n"
        output += outputCommas(numCommas) + "\n"

        return output
    }
}

/**
 * Write all results to disk in a .csv file
 *
 * @param activity The main activity
 * @param filePrefix The prefix for the .csv file
 * @param csv The comma-based csv string
 */
fun writeCSV(activity: MainActivity, filePrefix: String, csv: String) {

    val csvFile = File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOCUMENTS),
        File.separatorChar + MainActivity.LOG_DIR + File.separatorChar +
            filePrefix + "_" + generateCSVTimestamp() + ".csv")

    val csvDir = File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOCUMENTS), MainActivity.LOG_DIR)
    val docsDir = File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOCUMENTS), "")

    if (!docsDir.exists()) {
        val createSuccess = docsDir.mkdir()
        if (!createSuccess) {
            activity.runOnUiThread {
                Toast.makeText(activity, "Documents" + " creation failed.",
                    Toast.LENGTH_SHORT).show()
            }
            MainActivity.logd("Log storage directory Documents" + " creation failed!!")
        } else {
            MainActivity.logd("Log storage directory Documents" + " did not exist. Created.")
        }
    }

    if (!csvDir.exists()) {
        val createSuccess = csvDir.mkdir()
        if (!createSuccess) {
            activity.runOnUiThread {
                Toast.makeText(activity, "Documents/" + MainActivity.LOG_DIR +
                    " creation failed.", Toast.LENGTH_SHORT).show()
            }
            MainActivity.logd("Log storage directory Documents/" +
                MainActivity.LOG_DIR + " creation failed!!")
        } else {
            MainActivity.logd("Log storage directory Documents/" +
                MainActivity.LOG_DIR + " did not exist. Created.")
        }
    }

    val output = BufferedWriter(OutputStreamWriter(FileOutputStream(csvFile)))
    try {
        output.write(csv)
        logd("CSV write completed successfully.")

        // File is written, let media scanner know
        val scannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        scannerIntent.data = Uri.fromFile(csvFile)
        activity.sendBroadcast(scannerIntent)
    } catch (e: IOException) {
        logd("IOException vail on CSV write: " + e.printStackTrace())
    } finally {
        try {
            output.close()
        } catch (e: IOException) {
            logd("IOException vail on CSV close: " + e.printStackTrace())
            e.printStackTrace()
        }
    }
}

/**
 * Delete all Antelope .csv files in the documents directory
 */
fun deleteCSVFiles(activity: MainActivity) {
    val csvDir = File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DOCUMENTS), MainActivity.LOG_DIR)

    if (csvDir.exists()) {

        for (csv in csvDir.listFiles()!!)
            csv.delete()

        // Files are deleted, let media scanner know
        val scannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        scannerIntent.data = Uri.fromFile(csvDir)
        activity.sendBroadcast(scannerIntent)

        activity.runOnUiThread {
            Toast.makeText(activity, "CSV logs deleted", Toast.LENGTH_SHORT).show()
        }
            logd("All csv logs in directory DOCUMENTS/" + MainActivity.LOG_DIR + " deleted.")
    }
}

/**
 * Generate a timestamp for csv filenames
 */
fun generateCSVTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd-HH'h'mm", Locale.US)
    return sdf.format(Date())
}

/**
 * Create a string consisting solely of the number of commas indicated
 *
 * Handy for properly formatting comma-based .csv files as the number of columns will depend on
 * the user configurable number of test repetitions.
 */
fun outputCommas(numCommas: Int): String {
    var output = ""
    for (i in 1..numCommas)
        output += ","
    return output
}

/**
 * For a list of Longs, output a comma separated .csv line
 */
fun outputResultLine(
    name: String,
    results: ArrayList<Long>,
    numCommas: Int = 30,
    isCSV: Boolean = false
): String {
    var output = ""

    if (!results.isEmpty()) {
        output += name + ": "
        for ((index, result) in results.withIndex()) {
            if (isCSV || (0 != index))
                output += ","
            output += result
        }
        if (isCSV)
            output += outputCommas(numCommas - results.size)
        output += "\n"
    }

    return output
}

/**
 * For a list of Booleans, output a comma separated .csv line
 */
fun outputBooleanResultLine(
    name: String,
    results: ArrayList<Boolean>,
    numCommas: Int = 30,
    isCSV: Boolean = false
): String {
    var output = ""

    // If every result is false, don't output this line at all
    if (!results.isEmpty() && results.contains(true)) {
        output += name + ": "
        for ((index, result) in results.withIndex()) {
            if (isCSV || (0 != index))
                output += ","
            if (result)
                output += "HDR+"
            else
                output += " - "
        }
        if (isCSV)
            output += outputCommas(numCommas - results.size)
        output += "\n"
    }

    return output
}