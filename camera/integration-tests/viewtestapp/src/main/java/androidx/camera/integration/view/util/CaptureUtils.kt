/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.integration.view.util

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import android.view.View
import android.widget.ImageView
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCapture.OnImageSavedCallback
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.TransformUtils
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.integration.view.R
import androidx.camera.view.CameraController
import java.util.concurrent.Executor

fun interface ToastMessenger {
    fun show(message: String)
}

/** Take a picture based on the current configuration. */
fun CameraController.takePicture(
    context: Context,
    executor: Executor,
    toastMessenger: ToastMessenger,
    onDisk: () -> Boolean,
) {
    try {
        if (onDisk()) {
            takePictureOnDisk(
                context,
                executor,
                toastMessenger,
                onImageSaved = { results ->
                    toastMessenger.show("Image saved to: " + results.savedUri)
                },
                onError = { e -> toastMessenger.show("Failed to save picture: " + e.message) }
            )
        } else {
            takePicture(
                executor,
                object : OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        displayImage(context, image)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        toastMessenger.show(
                            "Failed to capture in-memory picture: " + exception.message
                        )
                    }
                }
            )
        }
    } catch (exception: RuntimeException) {
        toastMessenger.show("Failed to take picture: " + exception.message)
    }
}

/** Displays a [ImageProxy] in a pop-up dialog. */
private fun displayImage(context: Context, image: ImageProxy) {
    val rotationDegrees = image.imageInfo.rotationDegrees
    val cropped: Bitmap = getCroppedBitmap(image)
    image.close()

    CameraXExecutors.mainThreadExecutor().execute {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.image_dialog)
        val imageView = dialog.findViewById<View>(R.id.dialog_image) as ImageView
        imageView.setImageBitmap(cropped)
        imageView.rotation = rotationDegrees.toFloat()
        dialog.findViewById<View>(R.id.dialog_button).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}

@VisibleForTesting
fun CameraController.takePictureOnDisk(
    context: Context,
    executor: Executor,
    toastMessenger: ToastMessenger,
    onImageSaved: (OutputFileResults) -> Unit = {},
    onError: (ImageCaptureException) -> Unit = {},
) {
    createDefaultPictureFolderIfNotExist(toastMessenger)
    val contentValues = ContentValues()
    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    val outputFileOptions =
        OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()
    takePicture(
        outputFileOptions,
        executor,
        object : OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: OutputFileResults) {
                onImageSaved(outputFileResults)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

/** Converts the [ImageProxy] to [Bitmap] with crop rect applied. */
private fun getCroppedBitmap(image: ImageProxy): Bitmap {
    val byteBuffer = image.planes[0].buffer
    val bytes = ByteArray(byteBuffer.remaining())
    byteBuffer[bytes]
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val cropRect = image.cropRect
    val newSize = Size(cropRect.width(), cropRect.height())
    val cropped = Bitmap.createBitmap(newSize.width, newSize.height, Bitmap.Config.ARGB_8888)

    val croppingTransform =
        TransformUtils.getRectToRect(
            RectF(cropRect),
            RectF(0f, 0f, cropRect.width().toFloat(), cropRect.height().toFloat()),
            0
        )

    val canvas = Canvas(cropped)
    canvas.drawBitmap(bitmap, croppingTransform, Paint())
    canvas.save()

    bitmap.recycle()
    return cropped
}

private fun createDefaultPictureFolderIfNotExist(toastMessenger: ToastMessenger) {
    val pictureFolder =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    if (!pictureFolder.exists()) {
        if (!pictureFolder.mkdir()) {
            toastMessenger.show("Failed to create directory: $pictureFolder")
        }
    }
}
