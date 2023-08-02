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
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Logger
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.E2ETestUtil.canDeviceWriteToMediaStore
import androidx.camera.testing.impl.E2ETestUtil.generateVideoFileOutputOptions
import androidx.camera.testing.impl.E2ETestUtil.generateVideoMediaStoreOptions
import androidx.camera.testing.impl.E2ETestUtil.writeTextToExternalFile
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.fragment.app.Fragment

private const val TAG = "StreamSharingFragment"
private const val PREFIX_INFORMATION = "test_information"
private const val PREFIX_VIDEO = "video"
private const val KEY_ORIENTATION = "device_orientation"

class StreamSharingFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var exportButton: Button
    private lateinit var recordButton: Button
    private lateinit var streamSharingStateText: TextView
    private lateinit var useCases: Array<UseCase>
    private var camera: Camera? = null
    private var activeRecording: Recording? = null
    private var isUseCasesBound: Boolean = false
    private var deviceOrientation: Int = -1
    private val orientationEventListener by lazy {
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                deviceOrientation = orientation
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stream_sharing, container, false)
        previewView = view.findViewById(R.id.preview_view)
        streamSharingStateText = view.findViewById(R.id.stream_sharing_state)
        exportButton = view.findViewById(R.id.export_button)
        exportButton.setOnClickListener {
            exportTestInformation()
        }
        recordButton = view.findViewById(R.id.record_button)
        recordButton.setOnClickListener {
            if (activeRecording == null) startRecording() else stopRecording()
        }

        startCamera()
        return view
    }

    override fun onResume() {
        super.onResume()
        orientationEventListener.enable()
    }

    override fun onPause() {
        super.onPause()
        orientationEventListener.disable()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            bindUseCases(cameraProviderFuture.get())
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindUseCases(cameraProvider: ProcessCameraProvider) {
        displayStreamSharingState(GONE)
        enableRecording(false)
        cameraProvider.unbindAll()
        useCases = arrayOf(
            createPreview(),
            createImageCapture(),
            createImageAnalysis(),
            createVideoCapture()
        )
        isUseCasesBound = try {
            camera = cameraProvider.bindToLifecycle(this, getCameraSelector(), *useCases)
            enableRecording(true)
            true
        } catch (exception: Exception) {
            Logger.e(TAG, "Failed to bind use cases.", exception)
            false
        }
        if (isStreamSharingEnabled()) {
            displayStreamSharingState(VISIBLE)
        }
    }

    private fun createPreview(): Preview {
        return Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
    }

    private fun createImageCapture(): ImageCapture {
        return ImageCapture.Builder().build()
    }

    private fun createImageAnalysis(): ImageAnalysis {
        return ImageAnalysis.Builder().build()
    }

    private fun createVideoCapture(): VideoCapture<Recorder> {
        val recorder = Recorder.Builder().build()
        return VideoCapture.withOutput(recorder)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getVideoCapture(): VideoCapture<Recorder>? {
        return findUseCase(VideoCapture::class.java) as VideoCapture<Recorder>?
    }

    private fun <T : UseCase?> findUseCase(useCaseSubclass: Class<T>): T? {
        for (useCase in useCases) {
            if (useCaseSubclass.isInstance(useCase)) {
                return useCaseSubclass.cast(useCase)
            }
        }
        return null
    }

    private fun getCameraSelector(): CameraSelector {
        val cameraDirection: String? = requireActivity().intent.extras?.getString(
            MainActivity.INTENT_EXTRA_CAMERA_DIRECTION,
            MainActivity.CAMERA_DIRECTION_BACK
        )

        return when (cameraDirection) {
            MainActivity.CAMERA_DIRECTION_BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            MainActivity.CAMERA_DIRECTION_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    private fun isStreamSharingEnabled(): Boolean {
        val isCombinationSupported =
            camera != null && camera!!.isUseCasesCombinationSupported(*useCases)
        return !isCombinationSupported && isUseCasesBound
    }

    private fun displayStreamSharingState(visibility: Int) {
        streamSharingStateText.visibility = visibility
    }

    private fun enableRecording(enabled: Boolean) {
        recordButton.isEnabled = enabled
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        recordButton.text = getString(R.string.btn_video_stop_recording)
        activeRecording = getVideoCapture()!!.let {
            prepareRecording(requireContext(), it.output).withAudioEnabled().start(
                CameraXExecutors.directExecutor(),
                generateVideoRecordEventListener()
            )
        }
    }

    private fun stopRecording() {
        recordButton.text = getString(R.string.btn_video_record)
        activeRecording!!.stop()
        activeRecording = null
    }

    private fun prepareRecording(context: Context, recorder: Recorder): PendingRecording {
        val fileName = generateFileName(PREFIX_VIDEO)

        return if (canDeviceWriteToMediaStore()) {
            recorder.prepareRecording(
                context,
                generateVideoMediaStoreOptions(context.contentResolver, fileName)
            )
        } else {
            recorder.prepareRecording(context, generateVideoFileOutputOptions(fileName))
        }
    }

    private fun exportTestInformation() {
        val fileName = generateFileName(PREFIX_INFORMATION)
        val information = "$KEY_ORIENTATION: $deviceOrientation"

        writeTextToExternalFile(information, fileName)
    }

    private fun generateFileName(prefix: String? = null): String {
        val timeMillis = System.currentTimeMillis()
        return if (prefix != null) "${prefix}_$timeMillis" else "$timeMillis"
    }

    private fun generateVideoRecordEventListener(): Consumer<VideoRecordEvent> {
        return Consumer<VideoRecordEvent> { event ->
            if (event is VideoRecordEvent.Finalize) {
                val uri = event.outputResults.outputUri
                when (event.error) {
                    VideoRecordEvent.Finalize.ERROR_NONE,
                    VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED,
                    VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED,
                    VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE,
                    VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE -> Logger.d(
                        TAG,
                        "Video saved to: $uri"
                    )

                    else -> Logger.e(
                        TAG,
                        "Failed to save video: uri $uri with code (${event.error})"
                    )
                }
            }
        }
    }
}
