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

import android.content.ContentValues
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.integration.extensions.R
import androidx.camera.integration.extensions.utils.ExtensionModeUtil.getExtensionModeStringFromId
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_CAMERA_ID
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_ERROR_CODE
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_EXTENSION_MODE
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_IMAGE_ROTATION_DEGREES
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_IMAGE_URI
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_LENS_FACING
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_REQUEST_CODE
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INTENT_EXTRA_KEY_TEST_RESULT
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.INVALID_LENS_FACING
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity.Companion.getLensFacingStringFromInt
import androidx.camera.integration.extensions.validation.ImageCaptureActivity.Companion.ERROR_CODE_BIND_FAIL
import androidx.camera.integration.extensions.validation.ImageCaptureActivity.Companion.ERROR_CODE_EXTENSION_MODE_NOT_SUPPORT
import androidx.camera.integration.extensions.validation.ImageCaptureActivity.Companion.ERROR_CODE_NONE
import androidx.camera.integration.extensions.validation.ImageCaptureActivity.Companion.ERROR_CODE_TAKE_PICTURE_FAILED
import androidx.camera.integration.extensions.validation.PhotoFragment.Companion.decodeImageToBitmap
import androidx.camera.integration.extensions.validation.TestResults.Companion.INVALID_EXTENSION_MODE
import androidx.camera.integration.extensions.validation.TestResults.Companion.TEST_RESULT_FAILED
import androidx.camera.integration.extensions.validation.TestResults.Companion.TEST_RESULT_NOT_TESTED
import androidx.camera.integration.extensions.validation.TestResults.Companion.TEST_RESULT_PASSED
import androidx.camera.integration.extensions.validation.TestResults.Companion.copyTempFileToOutputLocation
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import java.text.Format
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private const val TAG = "ImageValidationActivity"

class ImageValidationActivity : AppCompatActivity() {

    private var extensionMode = INVALID_EXTENSION_MODE
    private val result = Intent()
    private var lensFacing = INVALID_LENS_FACING
    private lateinit var cameraId: String
    private lateinit var failButton: ImageButton
    private lateinit var passButton: ImageButton
    private lateinit var captureButton: ImageButton
    private lateinit var viewPager: ViewPager2
    private lateinit var photoImageView: ImageView
    private val imageCaptureActivityRequestCode = ImageCaptureActivity::class.java.hashCode() % 1000
    private val imageUris = arrayListOf<Pair<Uri, Int>>()
    private var scaledBitmapWidth = 0
    private var scaledBitmapHeight = 0
    private var currentScale = 1.0f
    private var translationX = 0.0f
    private var translationY = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_validation_activity)

        cameraId = intent?.getStringExtra(INTENT_EXTRA_KEY_CAMERA_ID)!!
        lensFacing = intent.getIntExtra(INTENT_EXTRA_KEY_LENS_FACING, INVALID_LENS_FACING)
        extensionMode = intent.getIntExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, INVALID_EXTENSION_MODE)

        result.putExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, extensionMode)
        result.putExtra(INTENT_EXTRA_KEY_TEST_RESULT, TEST_RESULT_NOT_TESTED)
        val requestCode = intent.getIntExtra(INTENT_EXTRA_KEY_REQUEST_CODE, -1)
        setResult(requestCode, result)

        supportActionBar?.title = "${resources.getString(R.string.extensions_validator)}"
        supportActionBar!!.subtitle =
            "Camera $cameraId [${getLensFacingStringFromInt(lensFacing)}]" +
                "[${getExtensionModeStringFromId(extensionMode)}]"

        viewPager = findViewById(R.id.photo_view_pager)
        photoImageView = findViewById(R.id.photo_image_view)

        setupButtonControls()
        setupGestureControls()
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
            // Closes the activity if there is no image captured.
            if (imageUris.isEmpty()) {
                finish()
            }
            return
        }

        val rotationDegrees = data?.getIntExtra(INTENT_EXTRA_KEY_IMAGE_ROTATION_DEGREES, 0)!!
        imageUris.add(Pair(uri, rotationDegrees))

        viewPager.adapter = PhotoPagerAdapter(this)
        viewPager.currentItem = imageUris.size - 1
        viewPager.visibility = View.VISIBLE
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePhotoImageView()
            }
        })

        updatePhotoImageView()
        resetAndHidePhotoImageView()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        resetAndHidePhotoImageView()
        updateScaledBitmapDims(scaledBitmapWidth, scaledBitmapHeight)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.image_validation_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_save_image -> {
                saveCurrentImage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveCurrentImage() {
        val formatter: Format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        val savedFileName =
            "${imageUris[viewPager.currentItem].first.lastPathSegment}" +
                "[${formatter.format(Calendar.getInstance().time)}].jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, savedFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ExtensionsValidation")
        }

        if (copyTempFileToOutputLocation(
                contentResolver,
                imageUris[viewPager.currentItem].first,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        ) {
            Toast.makeText(
                this,
                "Image is saved as Pictures/ExtensionsValidation/$savedFileName.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(this, "Failed to export the CSV file!", Toast.LENGTH_LONG).show()
        }
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
        intent.putExtra(INTENT_EXTRA_KEY_LENS_FACING, lensFacing)
        intent.putExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, mode)
        intent.putExtra(INTENT_EXTRA_KEY_REQUEST_CODE, imageCaptureActivityRequestCode)

        ActivityCompat.startActivityForResult(
            this,
            intent,
            imageCaptureActivityRequestCode,
            null
        )
    }

    /** Adapter class used to present a fragment containing one photo or video as a page */
    inner class PhotoPagerAdapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int {
            return imageUris.size
        }

        override fun createFragment(position: Int): Fragment {
            // Set scale gesture listener to the fragments inside the ViewPager2 so that we can
            // switch to another photo view which supports the translation function in the X
            // direction. Otherwise, the fragments inside the ViewPager2 will eat the X direction
            // movement events for the ViewPager2's page switch function. But we'll need the
            // translation function in X direction after the photo is zoomed in.
            val scaleGestureListener: ScaleGestureDetector.SimpleOnScaleGestureListener =
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        updatePhotoViewScale(detector.scaleFactor)
                        return true
                    }
                }

            return PhotoFragment(
                imageUris[position].first,
                imageUris[position].second,
                scaleGestureListener
            )
        }
    }

    private fun setupGestureControls() {
        // Registers the scale gesture event to allow the users to scale the photo image view
        // between 1.0 and 3.0 times.
        val scaleGestureListener: ScaleGestureDetector.SimpleOnScaleGestureListener =
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    updatePhotoViewScale(detector.scaleFactor)
                    return true
                }
            }

        // Registers double tap event to reset and hide the photo image view.
        val onDoubleTapGestureListener: GestureDetector.OnGestureListener =
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    resetAndHidePhotoImageView()
                    return true
                }
            }

        val scaleDetector = ScaleGestureDetector(this, scaleGestureListener)
        val doubleTapDetector = GestureDetector(this, onDoubleTapGestureListener)
        var previousX = 0.0f
        var previousY = 0.0f

        photoImageView.setOnTouchListener { _, e: MotionEvent ->
            if (photoImageView.visibility != View.VISIBLE) {
                return@setOnTouchListener false
            }

            val doubleTapProcessed = doubleTapDetector.onTouchEvent(e)
            val scaleGestureProcessed = scaleDetector.onTouchEvent(e)

            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    previousX = e.x
                    previousY = e.y
                }
                MotionEvent.ACTION_MOVE -> {
                    updatePhotoImageViewTranslation(e.x, e.y, previousX, previousY)
                }
            }

            doubleTapProcessed || scaleGestureProcessed
        }
    }

    internal fun updatePhotoViewScale(scaleFactor: Float) {
        currentScale *= scaleFactor

        // Don't let the object get too small or too large.
        currentScale = max(1.0f, min(currentScale, 3.0f))

        photoImageView.scaleX = currentScale
        photoImageView.scaleY = currentScale

        // Shows the photoImageView when the scale is larger than 1.0f. Hides the photoImageView
        // when the scale has been reduced as 1.0f.
        if (photoImageView.visibility != View.VISIBLE && currentScale > 1.0f) {
            photoImageView.visibility = View.VISIBLE
            viewPager.visibility = View.INVISIBLE
        } else if (photoImageView.visibility == View.VISIBLE && currentScale == 1.0f) {
            resetAndHidePhotoImageView()
        }
    }

    private fun updatePhotoImageViewTranslation(
        x: Float,
        y: Float,
        previousX: Float,
        previousY: Float
    ) {
        val newTranslationX = translationX + x - previousX

        if (scaledBitmapWidth * currentScale > photoImageView.width) {
            val maxTranslationX = (scaledBitmapWidth * currentScale - photoImageView.width) / 2

            translationX = if (newTranslationX >= 0) {
                if (maxTranslationX - newTranslationX >= 0) {
                    newTranslationX
                } else {
                    maxTranslationX
                }
            } else {
                if (maxTranslationX + newTranslationX >= 0) {
                    newTranslationX
                } else {
                    -maxTranslationX
                }
            }
            photoImageView.translationX = translationX
        }

        val newTranslationY = translationY + y - previousY

        if (scaledBitmapHeight * currentScale > photoImageView.height) {
            val maxTranslationY = (scaledBitmapHeight * currentScale - photoImageView.height) / 2

            translationY = if (newTranslationY >= 0) {
                if (maxTranslationY - newTranslationY >= 0) {
                    newTranslationY
                } else {
                    maxTranslationY
                }
            } else {
                if (maxTranslationY + newTranslationY >= 0) {
                    newTranslationY
                } else {
                    -maxTranslationY
                }
            }
            photoImageView.translationY = translationY
        }
    }

    internal fun updatePhotoImageView() {
        val bitmap = decodeImageToBitmap(
            this@ImageValidationActivity.contentResolver,
            imageUris[viewPager.currentItem].first,
            imageUris[viewPager.currentItem].second
        )

        photoImageView.setImageBitmap(bitmap)
        updateScaledBitmapDims(bitmap.width, bitmap.height)

        // Updates the index and file name to the subtitle
        supportActionBar!!.subtitle = "[${viewPager.currentItem + 1}/${imageUris.size}]" +
            "${imageUris[viewPager.currentItem].first.lastPathSegment}"
    }

    private fun updateScaledBitmapDims(width: Int, height: Int) {
        val scale: Float
        if (width * photoImageView.height / photoImageView.width > height) {
            scale = photoImageView.width.toFloat() / width
        } else {
            scale = photoImageView.height.toFloat() / height
        }

        scaledBitmapWidth = (width * scale).toInt()
        scaledBitmapHeight = (height * scale).toInt()
    }

    internal fun resetAndHidePhotoImageView() {
        viewPager.visibility = View.VISIBLE
        photoImageView.visibility = View.INVISIBLE
        photoImageView.scaleX = 1.0f
        photoImageView.scaleY = 1.0f
        photoImageView.translationX = 0.0f
        photoImageView.translationY = 0.0f
        currentScale = 1.0f
        translationX = 0.0f
        translationY = 0.0f
    }
}
