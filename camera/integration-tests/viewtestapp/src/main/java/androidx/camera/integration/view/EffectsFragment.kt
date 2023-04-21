/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.integration.view

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.camera.view.video.ExperimentalVideo
import androidx.fragment.app.Fragment

/**
 * Fragment for testing effects integration.
 */
@OptIn(markerClass = [ExperimentalVideo::class])
class EffectsFragment : Fragment() {

    private lateinit var cameraController: LifecycleCameraController
    lateinit var previewView: PreviewView
    private lateinit var surfaceEffectForPreviewVideo: RadioButton
    lateinit var surfaceEffectForImageCapture: RadioButton
    private lateinit var imageEffectForImageCapture: RadioButton
    private lateinit var previewVideoGroup: RadioGroup
    private lateinit var imageGroup: RadioGroup
    private lateinit var capture: Button
    private lateinit var record: Button
    private var recording: Recording? = null

    private lateinit var surfaceProcessor: ToneMappingSurfaceProcessor
    private var imageEffect: ToneMappingImageEffect? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment.
        val view = inflater.inflate(R.layout.effects_view, container, false)
        previewView = view.findViewById(R.id.preview_view)
        surfaceEffectForPreviewVideo = view.findViewById(R.id.surface_effect_for_preview_video)
        surfaceEffectForImageCapture = view.findViewById(R.id.surface_effect_for_image_capture)
        imageEffectForImageCapture = view.findViewById(R.id.image_effect_for_image_capture)
        previewVideoGroup = view.findViewById(R.id.preview_and_video_effect_group)
        imageGroup = view.findViewById(R.id.image_effect_group)
        capture = view.findViewById(R.id.capture)
        record = view.findViewById(R.id.record)

        // Set up  UI events.
        // previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        previewVideoGroup.setOnCheckedChangeListener { _, _ -> updateEffects() }
        imageGroup.setOnCheckedChangeListener { _, _ -> updateEffects() }
        capture.setOnClickListener { takePicture() }
        record.setOnClickListener {
            if (recording == null) {
                startRecording()
            } else {
                stopRecording()
            }
        }

        // Set up the surface processor.
        surfaceProcessor = ToneMappingSurfaceProcessor()

        // Set up the camera controller.
        cameraController = LifecycleCameraController(requireContext())
        cameraController.setEnabledUseCases(
            CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE
        )
        previewView.controller = cameraController
        updateEffects()
        cameraController.bindToLifecycle(viewLifecycleOwner)

        return view
    }

    private fun updateEffects() {
        val effects = mutableSetOf<CameraEffect>()
        if (surfaceEffectForPreviewVideo.isChecked && surfaceEffectForImageCapture.isChecked) {
            // Sharing surface effect to all 3 use cases
            effects.add(
                ToneMappingSurfaceEffect(
                    PREVIEW or IMAGE_CAPTURE or VIDEO_CAPTURE,
                    surfaceProcessor
                )
            )
        } else if (surfaceEffectForPreviewVideo.isChecked) {
            // Sharing surface effect to preview and video
            effects.add(
                ToneMappingSurfaceEffect(
                    PREVIEW or VIDEO_CAPTURE,
                    surfaceProcessor
                )
            )
        } else if (
            !surfaceEffectForPreviewVideo.isChecked && surfaceEffectForImageCapture.isChecked) {
            toast(
                "Cannot apply SurfaceProcessor to ImageCapture " +
                    "without applying it to Preview and VideoCapture."
            )
        }

        if (imageEffectForImageCapture.isChecked) {
            // Use ImageEffect for image capture
            imageEffect = ToneMappingImageEffect()
            effects.add(imageEffect!!)
        } else {
            imageEffect = null
        }
        cameraController.setEffects(effects)
    }

    override fun onDestroy() {
        super.onDestroy()
        surfaceProcessor.release()
    }

    private fun toast(message: String?) {
        requireActivity().runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun takePicture() {
        takePicture(
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    toast("Image saved successfully.")
                }

                override fun onError(exception: ImageCaptureException) {
                    toast("Image capture failed. $exception")
                }
            }
        )
    }

    fun takePicture(onImageSavedCallback: ImageCapture.OnImageSavedCallback) {
        createDefaultPictureFolderIfNotExist()
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        val outputFileOptions = OutputFileOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
        cameraController.takePicture(
            outputFileOptions,
            directExecutor(),
            onImageSavedCallback
        )
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        record.text = "Stop recording"
        val outputOptions: MediaStoreOutputOptions = getNewVideoOutputMediaStoreOptions()
        val audioConfig = AudioConfig.create(true)
        recording = cameraController.startRecording(
            outputOptions, audioConfig,
            directExecutor()
        ) {
            if (it is VideoRecordEvent.Finalize) {
                val uri = it.outputResults.outputUri
                if (it.error == VideoRecordEvent.Finalize.ERROR_NONE) {
                    toast("Video saved to: $uri")
                } else {
                    toast("Failed to save video: uri $uri with code (${it.error})")
                }
            }
        }
    }

    private fun stopRecording() {
        record.text = "Record"
        recording?.stop()
    }

    private fun getNewVideoOutputMediaStoreOptions(): MediaStoreOutputOptions {
        val videoFileName = "video_" + System.currentTimeMillis()
        val resolver = requireContext().contentResolver
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        contentValues.put(MediaStore.Video.Media.TITLE, videoFileName)
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
        return MediaStoreOutputOptions.Builder(
            resolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues)
            .build()
    }

    private fun createDefaultPictureFolderIfNotExist() {
        val pictureFolder = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        if (!pictureFolder.exists()) {
            if (!pictureFolder.mkdir()) {
                toast("Failed to create directory: $pictureFolder")
            }
        }
    }

    @VisibleForTesting
    fun getImageEffect(): ToneMappingImageEffect? {
        return imageEffect
    }

    @VisibleForTesting
    fun getSurfaceProcessor(): ToneMappingSurfaceProcessor {
        return surfaceProcessor
    }
}