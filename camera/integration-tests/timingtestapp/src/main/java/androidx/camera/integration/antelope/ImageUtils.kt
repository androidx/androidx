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
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import androidx.camera.integration.antelope.MainActivity.Companion.PHOTOS_DIR
import androidx.camera.integration.antelope.MainActivity.Companion.logd
import androidx.camera.integration.antelope.cameracontrollers.CameraState
import androidx.camera.integration.antelope.cameracontrollers.closeCameraX
import androidx.camera.integration.antelope.cameracontrollers.closePreviewAndCamera
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ImageReader listener for use with Camera 2 API.
 *
 * Extract image and write to disk
 */
class ImageAvailableListener(
    internal val activity: MainActivity,
    internal var params: CameraParams,
    internal val testConfig: TestConfig
) : ImageReader.OnImageAvailableListener {

    override fun onImageAvailable(reader: ImageReader) {
        logd("onImageAvailable enter. Current test: " + testConfig.currentRunningTest +
            " state: " + params.state)

        // Only save 1 photo each time
        if (CameraState.IMAGE_REQUESTED != params.state)
            return
        else
            params.state = CameraState.UNINITIALIZED

        val image = reader.acquireLatestImage()

        when (image.format) {
            ImageFormat.JPEG -> {
                // Orientation
                val rotation = activity.windowManager.defaultDisplay.rotation
                val capturedImageRotation = getOrientation(params, rotation)

                params.timer.imageReaderEnd = System.currentTimeMillis()
                params.timer.imageSaveStart = System.currentTimeMillis()

                val bytes = ByteArray(image.planes[0].buffer.remaining())
                image.planes[0].buffer.get(bytes)

                params.backgroundHandler?.post(ImageSaver(activity, bytes, capturedImageRotation,
                    params.isFront, params, testConfig))
            }

            // TODO: add RAW support
            ImageFormat.RAW_SENSOR -> {
            }

            else -> {
            }
        }

        image.close()
    }
}

/**
 * Asynchronously save ByteArray to disk
 */
class ImageSaver internal constructor(
    private val activity: MainActivity,
    private val bytes: ByteArray,
    private val rotation: Int,
    private val flip: Boolean,
    private val params: CameraParams,
    private val testConfig: TestConfig
) : Runnable {

    override fun run() {
        logd("ImageSaver. ImageSaver is running, saving image to disk.")

        // TODO: Once Android supports HDR+ detection add this in
//        if (isHDRPlus(bytes))
//            params.timer.isHDRPlus = true;

        writeFile(activity, bytes)

        params.timer.imageSaveEnd = System.currentTimeMillis()

        // The test is over only if the capture call back has already been hit
        // It is possible to be here before the callback is hit
        if (0L != params.timer.captureEnd) {
            if (TestType.MULTI_PHOTO_CHAIN == testConfig.currentRunningTest) {
                testEnded(activity, params, testConfig)
            } else {
                logd("ImageSaver: photo saved, test is finished, closing the camera.")
                testConfig.testFinished = true
                closePreviewAndCamera(activity, params, testConfig)
            }
        }
    }
}

/**
 * Rotate a given Bitmap by degrees
 */
fun rotateBitmap(original: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(original, 0, 0, original.width, original.height,
        matrix, true)
}

/**
 * Scale a given Bitmap by scaleFactor
 */
fun scaleBitmap(bitmap: Bitmap, scaleFactor: Float): Bitmap {
    val scaledWidth = Math.round(bitmap.width * scaleFactor)
    val scaledHeight = Math.round(bitmap.height * scaleFactor)

    return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
}

/**
 * Flip a Bitmap horizontal
 */
fun horizontalFlip(bitmap: Bitmap): Bitmap {
    val matrix = Matrix()
    matrix.preScale(-1.0f, 1.0f)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/**
 * Generate a timestamp to append to saved filenames.
 */
fun generateTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
    return sdf.format(Date())
}

/**
 * Actually write a byteArray file to disk. Assume the file is a jpg and use that extension
 */
fun writeFile(activity: MainActivity, bytes: ByteArray) {
    val jpgFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
        File.separatorChar + PHOTOS_DIR + File.separatorChar +
            "Antelope" + generateTimestamp() + ".jpg")

    val photosDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
        PHOTOS_DIR)

    if (!photosDir.exists()) {
        val createSuccess = photosDir.mkdir()
        if (!createSuccess) {
            activity.runOnUiThread {
                Toast.makeText(activity, "DCIM/" + PHOTOS_DIR + " creation failed.",
                    Toast.LENGTH_SHORT).show()
            }
            logd("Photo storage directory DCIM/" + PHOTOS_DIR + " creation failed!!")
        } else {
            logd("Photo storage directory DCIM/" + PHOTOS_DIR + " did not exist. Created.")
        }
    }

    var output: FileOutputStream? = null
    try {
        output = FileOutputStream(jpgFile)
        output.write(bytes)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        if (null != output) {
            try {
                output.close()

                if (!PrefHelper.getAutoDelete(activity)) {
                    // File is written, let media scanner know
                    val scannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    scannerIntent.data = Uri.fromFile(jpgFile)
                    activity.sendBroadcast(scannerIntent)

                    // File is written, now delete it.
                    // TODO: make sure this does not add extra latency
                } else {
                    jpgFile.delete()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    logd("writeFile: Completed.")
}

/**
 * Delete all the photos generated by testing from the default Antelope PHOTOS_DIR
 */
fun deleteTestPhotos(activity: MainActivity) {
    val photosDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
        PHOTOS_DIR)

    if (photosDir.exists()) {

        for (photo in photosDir.listFiles()!!)
            photo.delete()

        // Files are deleted, let media scanner know
        val scannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        scannerIntent.data = Uri.fromFile(photosDir)
        activity.sendBroadcast(scannerIntent)

        activity.runOnUiThread {
            Toast.makeText(activity, "All test photos deleted", Toast.LENGTH_SHORT).show()
        }
        logd("All photos in storage directory DCIM/" + PHOTOS_DIR + " deleted.")
    }
}

/**
 * Try to detect if a saved image file has had HDR effects applied to it by examining the EXIF tag.
 *
 * Note: this does not currently work.
 */
@RequiresApi(24)
fun isHDRPlus(bytes: ByteArray?): Boolean {
    if (24 <= Build.VERSION.SDK_INT) {
        val bytestream = ByteArrayInputStream(bytes)
        val exif = ExifInterface(bytestream)
        val software: String = exif.getAttribute(ExifInterface.TAG_SOFTWARE) ?: ""
        val makernote: String = exif.getAttribute(ExifInterface.TAG_MAKER_NOTE) ?: ""
        logd("In isHDRPlus, software: " + software + ", makernote: " + makernote)
        if (software.contains("HDR+") || makernote.contains("HDRP")) {
            logd("Photo is HDR+: " + software + ", " + makernote)
            return true
        }
    }
    return false
}

/**
 * ImageReader listener for use with Camera X API.
 *
 * Extract image and write to disk
 */
class CameraXImageAvailableListener(
    internal val activity: MainActivity,
    internal var params: CameraParams,
    internal val testConfig: TestConfig
) : ImageCapture.OnImageCapturedListener() {

    /** Image was captured successfully */
    override fun onCaptureSuccess(image: ImageProxy?, rotationDegrees: Int) {
        logd("CameraXImageAvailableListener onCaptureSuccess. Current test: " +
            testConfig.currentRunningTest)

        when (image?.format) {
            ImageFormat.JPEG -> {
                params.timer.imageReaderEnd = System.currentTimeMillis()

                // TODO As of CameraX 0.3.0 has a bug so capture session callbacks are never called.
                // As a workaround for now we use this onCaptureSuccess callback as the only measure
                // of capture timing (as opposed to capture callback + image ready for camera2
                // Remove these lines when bug is fixed
                // /////////////////////////////////////////////////////////////////////////////////
                params.timer.captureEnd = System.currentTimeMillis()

                params.timer.imageReaderStart = System.currentTimeMillis()
                params.timer.imageReaderEnd = System.currentTimeMillis()

                // End Remove lines ////////////////////////////////////////////////////////////////

                // Orientation
                val rotation = activity.windowManager.defaultDisplay.rotation
                val capturedImageRotation = getOrientation(params, rotation)

                params.timer.imageSaveStart = System.currentTimeMillis()

                val bytes = ByteArray(image.planes[0].buffer.remaining())
                image.planes[0].buffer.get(bytes)

                params.backgroundHandler?.post(ImageSaver(activity, bytes, capturedImageRotation,
                    params.isFront, params, testConfig))
            }

            ImageFormat.RAW_SENSOR -> {
            }

            else -> {
            }
        }

        image?.close()
    }

    /** Camera X was unable to capture a still image and threw an error */
    override fun onError(
        useCaseError: ImageCapture.UseCaseError?,
        message: String?,
        cause: Throwable?
    ) {
        logd("CameraX ImageCallback onError. Error: " + message)
        params.timer.imageReaderEnd = System.currentTimeMillis()
        params.timer.imageSaveStart = System.currentTimeMillis()
        params.timer.imageSaveEnd = System.currentTimeMillis()

        // The test is over only if the capture call back has already been hit
        // It is possible to be here before the callback is hit
        if (0L != params.timer.captureEnd) {
            if (TestType.MULTI_PHOTO_CHAIN == testConfig.currentRunningTest) {
                testEnded(activity, params, testConfig)
            } else {
                testConfig.testFinished = true
                closeCameraX(activity, params, testConfig)
            }
        }
    }
}