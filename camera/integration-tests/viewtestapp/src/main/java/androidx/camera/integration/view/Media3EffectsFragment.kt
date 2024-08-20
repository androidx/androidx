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

package androidx.camera.integration.view

import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraEffect.IMAGE_CAPTURE
import androidx.camera.core.CameraEffect.PREVIEW
import androidx.camera.core.CameraEffect.VIDEO_CAPTURE
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.media3.effect.Media3Effect
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Brightness

/** Fragment for testing effects integration. */
@OptIn(UnstableApi::class)
@Suppress("RestrictedApiAndroidX")
class Media3EffectsFragment : Fragment() {

    private lateinit var media3Effect: Media3Effect
    lateinit var cameraController: LifecycleCameraController
    lateinit var previewView: PreviewView
    lateinit var slider: SeekBar
    private lateinit var hdrToggle: ToggleButton
    private lateinit var cameraToggle: ToggleButton
    private lateinit var recordButton: Button
    private var recording: Recording? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment.
        val view = inflater.inflate(R.layout.media3_effect_view, container, false)
        previewView = view.findViewById(R.id.preview_view)
        slider = view.findViewById(R.id.slider)
        hdrToggle = view.findViewById(R.id.hdr)
        cameraToggle = view.findViewById(R.id.flip)
        recordButton = view.findViewById(R.id.record)

        cameraController = LifecycleCameraController(requireContext())
        cameraController.bindToLifecycle(viewLifecycleOwner)
        cameraController.setEnabledUseCases(CameraController.VIDEO_CAPTURE)
        previewView.controller = cameraController

        slider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    media3Effect.setEffects(listOf(Brightness(progress / 100f)))
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            }
        )
        cameraToggle.setOnClickListener { updateCameraOrientation() }
        hdrToggle.setOnClickListener {
            cameraController.videoCaptureDynamicRange =
                if (hdrToggle.isChecked) {
                    DynamicRange.HDR_UNSPECIFIED_10_BIT
                } else {
                    DynamicRange.SDR
                }
        }
        recordButton.setOnClickListener {
            if (recording == null) {
                startRecording()
            } else {
                stopRecording()
            }
        }
        media3Effect =
            Media3Effect(
                requireContext(),
                PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE,
                mainThreadExecutor(),
            ) {
                toast("Error in CameraFiltersAdapterProcessor")
            }
        cameraController.setEffects(setOf(media3Effect))
        // Set up  UI events.
        return view
    }

    private fun updateCameraOrientation() {
        cameraController.cameraSelector =
            if (cameraToggle.isChecked) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        media3Effect.close()
    }

    private fun toast(message: String?) {
        requireActivity().runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        recordButton.text = "Stop recording"
        val outputOptions: MediaStoreOutputOptions = getNewVideoOutputMediaStoreOptions()
        val audioConfig = AudioConfig.create(true)
        recording =
            cameraController.startRecording(outputOptions, audioConfig, directExecutor()) {
                if (it is VideoRecordEvent.Finalize) {
                    val uri = it.outputResults.outputUri
                    when (it.error) {
                        VideoRecordEvent.Finalize.ERROR_NONE,
                        ERROR_FILE_SIZE_LIMIT_REACHED,
                        ERROR_DURATION_LIMIT_REACHED,
                        ERROR_INSUFFICIENT_STORAGE,
                        ERROR_SOURCE_INACTIVE -> toast("Video saved to: $uri")
                        else -> toast("Failed to save video: uri $uri with code (${it.error})")
                    }
                }
            }
    }

    private fun stopRecording() {
        recordButton.text = "Record"
        recording?.stop()
        recording = null
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
            )
            .setContentValues(contentValues)
            .build()
    }
}
