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

import android.app.Activity
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.camera.integration.extensions.R
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2

/**
 * Fragment used for each individual page showing a photo inside [ImageValidationActivity].
 *
 * @param imageUri The image uri to be displayed in this photo fragment
 * @param rotationDegrees The rotation degrees to rotate the image to the upright direction
 * @param scaleGestureListener The scale gesture listener which allow the caller activity to
 * receive the scale events to switch to another photo view which supports the translation
 * function in the X direction. It is because this fragment will be put inside a [ViewPager2]
 * and it will eat the X direction movement events for the [ViewPager2]'s page switch function. But
 * we'll need the translation function in X direction after the photo is zoomed in.
 */
class PhotoFragment constructor(
    private val imageUri: Uri,
    private val rotationDegrees: Int,
    private val scaleGestureListener: ScaleGestureDetector.SimpleOnScaleGestureListener?
) :
    Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.single_photo_viewer, container, false)

    private lateinit var photoViewer: ImageView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoViewer = view.findViewById(R.id.imageView)
        photoViewer.setImageBitmap(
            decodeImageToBitmap(
                (requireActivity() as Activity).contentResolver,
                imageUri,
                rotationDegrees
            )
        )

        setPhotoViewerScaleGestureListener()
    }

    private fun setPhotoViewerScaleGestureListener() {
        scaleGestureListener?.let {
            val scaleDetector = ScaleGestureDetector(requireContext(), scaleGestureListener)
            photoViewer.setOnTouchListener { _, e: MotionEvent ->
                scaleDetector.onTouchEvent(e)
            }
        }
    }

    companion object {
        fun decodeImageToBitmap(
            contentResolver: ContentResolver,
            imageUri: Uri,
            rotationDegrees: Int
        ): Bitmap {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(imageUri, "r")
            val bitmap = BitmapFactory.decodeFileDescriptor(parcelFileDescriptor?.fileDescriptor)
            parcelFileDescriptor?.close()

            // Rotates the bitmap to the correct orientation
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    }
}