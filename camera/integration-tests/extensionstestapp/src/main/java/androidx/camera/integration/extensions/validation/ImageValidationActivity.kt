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

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.integration.extensions.R
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_CAMERA_ID
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_ERROR_CODE
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_EXTENSION_MODE
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_IMAGE_ROTATION_DEGREES
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_IMAGE_URI
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_REQUEST_CODE
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_TEST_RESULT
import androidx.camera.integration.extensions.validation.ImageCaptureActivity.Companion.ERROR_CODE_BIND_FAIL
import androidx.camera.integration.extensions.validation.ImageCaptureActivity.Companion.ERROR_CODE_EXTENSION_MODE_NOT_SUPPORT
import androidx.camera.integration.extensions.validation.ImageCaptureActivity.Companion.ERROR_CODE_NONE
import androidx.camera.integration.extensions.validation.ImageCaptureActivity.Companion.ERROR_CODE_TAKE_PICTURE_FAILED
import androidx.camera.integration.extensions.validation.TestResults.Companion.INVALID_EXTENSION_MODE
import androidx.camera.integration.extensions.validation.TestResults.Companion.TEST_RESULT_FAILED
import androidx.camera.integration.extensions.validation.TestResults.Companion.TEST_RESULT_NOT_TESTED
import androidx.camera.integration.extensions.validation.TestResults.Companion.TEST_RESULT_PASSED
import androidx.camera.integration.extensions.validation.TestResults.Companion.getExtensionModeStringFromId
import androidx.core.app.ActivityCompat

private const val TAG = "ImageValidationActivity"

class ImageValidationActivity : AppCompatActivity() {

    private var extensionMode = INVALID_EXTENSION_MODE
    private val result = Intent()
    private lateinit var cameraId: String
    private lateinit var photoViewer: ImageView
    private lateinit var failButton: ImageButton
    private lateinit var passButton: ImageButton
    private lateinit var captureButton: ImageButton
    private val imageCaptureActivityRequestCode = ImageCaptureActivity::class.java.hashCode() % 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_validation_activity)

        cameraId = intent?.getStringExtra(INTENT_EXTRA_KEY_CAMERA_ID)!!
        extensionMode = intent.getIntExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, INVALID_EXTENSION_MODE)

        result.putExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, extensionMode)
        result.putExtra(INTENT_EXTRA_KEY_TEST_RESULT, TEST_RESULT_NOT_TESTED)
        val requestCode = intent.getIntExtra(INTENT_EXTRA_KEY_REQUEST_CODE, -1)
        setResult(requestCode, result)

        supportActionBar?.title = "${resources.getString(R.string.extensions_validator)}"
        supportActionBar!!.subtitle =
            "Camera $cameraId [${getExtensionModeStringFromId(extensionMode)}]"

        photoViewer = findViewById(R.id.photo_viewer)
        setupButtonControls()
        startCaptureImageActivity(cameraId, extensionMode)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != imageCaptureActivityRequestCode) {
            return
        }

        val errorCode = data?.getIntExtra(INTENT_EXTRA_KEY_ERROR_CODE, ERROR_CODE_NONE)

        // Returns with error
        if (errorCode == ERROR_CODE_BIND_FAIL ||
            errorCode == ERROR_CODE_EXTENSION_MODE_NOT_SUPPORT ||
            errorCode == ERROR_CODE_TAKE_PICTURE_FAILED
        ) {
            result.putExtra(INTENT_EXTRA_KEY_TEST_RESULT, TEST_RESULT_FAILED)
            Log.e(TAG, "Failed to take a picture with error code: $errorCode")
            finish()
            return
        }

        val uri = data?.getParcelableExtra(INTENT_EXTRA_KEY_IMAGE_URI) as Uri?

        // Returns without capturing a picture
        if (uri == null) {
            finish()
            return
        }

        val rotationDegrees = data?.getIntExtra(INTENT_EXTRA_KEY_IMAGE_ROTATION_DEGREES, 0)!!

        photoViewer.setImageBitmap(decodeImageFromUri(uri))
        photoViewer.rotation = rotationDegrees.toFloat()
    }

    private fun setupButtonControls() {
        failButton = findViewById(R.id.fail_button)
        failButton.setOnClickListener {
            result.putExtra(INTENT_EXTRA_KEY_TEST_RESULT, TEST_RESULT_FAILED)
            finish()
        }

        passButton = findViewById(R.id.pass_button)
        passButton.setOnClickListener {
            result.putExtra(INTENT_EXTRA_KEY_TEST_RESULT, TEST_RESULT_PASSED)
            finish()
        }

        captureButton = findViewById(R.id.capture_button)
        captureButton.setOnClickListener {
            startCaptureImageActivity(cameraId, extensionMode)
        }
    }

    private fun startCaptureImageActivity(cameraId: String, mode: Int) {
        val intent = Intent(this, ImageCaptureActivity::class.java)
        intent.putExtra(INTENT_EXTRA_KEY_CAMERA_ID, cameraId)
        intent.putExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, mode)
        intent.putExtra(INTENT_EXTRA_KEY_REQUEST_CODE, imageCaptureActivityRequestCode)

        ActivityCompat.startActivityForResult(
            this,
            intent,
            imageCaptureActivityRequestCode,
            null
        )
    }

    private fun decodeImageFromUri(uri: Uri): Bitmap {
        val parcelFileDescriptor = this.contentResolver.openFileDescriptor(uri, "r")
        val bitmap = BitmapFactory.decodeFileDescriptor(parcelFileDescriptor?.fileDescriptor)
        parcelFileDescriptor?.close()
        return bitmap
    }
}
