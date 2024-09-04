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

package androidx.camera.integration.core;

import static androidx.camera.core.MirrorMode.MIRROR_MODE_OFF;
import static androidx.camera.core.MirrorMode.MIRROR_MODE_ON;
import static androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.OrientationEventListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.pipe.integration.CameraPipeConfig;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.testing.impl.FileUtil;
import androidx.camera.video.ExperimentalPersistentRecording;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Activity for verifying behavior of switching camera during a recording.
 */
public class VideoCameraSwitchingActivity extends AppCompatActivity {

    private static final String TAG = "VideoCameraSwitchingActivity";
    // Possible values for this intent key: "BACKWARD" or "FORWARD".
    private static final String INTENT_EXTRA_CAMERA_DIRECTION = "camera_direction";
    // Launch the activity with the specified recording duration.
    private static final String INTENT_EXTRA_DURATION = "recording_duration";
    // Launch the activity with the specified camera switching time.
    private static final String INTENT_EXTRA_SWITCH_TIME = "recording_switch_time";
    private static final String INTENT_EXTRA_MIRROR_MODE = "mirror_mode";
    private static final long INVALID_TIME_VALUE = -1;
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            "android.permission.CAMERA"
    };
    private static final long DEFAULT_DURATION_MILLIS = 10000;
    private static final long DEFAULT_SWITCH_TIME_MILLIS = 5000;
    private static final String VIDEO_FILE_PREFIX = "video";
    private static final String INFO_FILE_PREFIX = "video_camera_switching_test_info";
    private static final String KEY_DEVICE_ORIENTATION = "device_orientation";
    private static final String INTENT_EXTRA_CAMERA_IMPLEMENTATION = "camera_implementation";
    // Camera2 implementation.
    private static final String CAMERA2_IMPLEMENTATION_OPTION = "camera2";
    // Camera-pipe implementation.
    private static final String CAMERA_PIPE_IMPLEMENTATION_OPTION = "camera_pipe";

    private static String sCameraImplementationType;

    @NonNull
    private CameraSelector mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    @Nullable
    private ProcessCameraProvider mCameraProvider;
    @Nullable
    private PreviewView mPreviewView;
    @Nullable
    private EditText mDurationText;
    @Nullable
    private EditText mSwitchTimeText;
    @Nullable
    private Button mStartButton;
    @Nullable
    private Preview mPreview;
    @Nullable
    private VideoCapture<Recorder> mVideoCapture;
    @Nullable
    private Camera mCamera;
    @Nullable
    private Recording mRecording;
    private boolean mNotYetSwitched = true;
    private Integer mDeviceOrientation = null;
    private OrientationEventListener mOrientationEventListener;
    private int mMirrorMode = MIRROR_MODE_OFF;

    @OptIn(markerClass = androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration.class)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_camera_switching);

        Bundle bundle = this.getIntent().getExtras();
        long extraDurationMillis = INVALID_TIME_VALUE;
        long extraSwitchTimeMillis = INVALID_TIME_VALUE;
        if (bundle != null) {
            String extraCameraDirection = bundle.getString(INTENT_EXTRA_CAMERA_DIRECTION);
            if (extraCameraDirection != null) {
                if (extraCameraDirection.equalsIgnoreCase("FORWARD")) {
                    mCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                } else {
                    mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                }
            }
            extraDurationMillis = bundle.getLong(INTENT_EXTRA_DURATION, INVALID_TIME_VALUE);
            extraSwitchTimeMillis = bundle.getLong(INTENT_EXTRA_SWITCH_TIME, INVALID_TIME_VALUE);
            String mirrorMode = bundle.getString(INTENT_EXTRA_MIRROR_MODE, "OFF");
            if (mirrorMode.equalsIgnoreCase("ON")) {
                mMirrorMode = MIRROR_MODE_ON;
            } else if (mirrorMode.equalsIgnoreCase("FRONT_ONLY")) {
                mMirrorMode = MIRROR_MODE_ON_FRONT_ONLY;
            } else {
                mMirrorMode = MIRROR_MODE_OFF;
            }

            String cameraImplementation = bundle.getString(INTENT_EXTRA_CAMERA_IMPLEMENTATION);
            if (cameraImplementation != null && sCameraImplementationType == null) {
                if (cameraImplementation.equals(CAMERA2_IMPLEMENTATION_OPTION)) {
                    ProcessCameraProvider.configureInstance(Camera2Config.defaultConfig());
                    sCameraImplementationType = cameraImplementation;
                } else if (cameraImplementation.equals(CAMERA_PIPE_IMPLEMENTATION_OPTION)) {
                    ProcessCameraProvider.configureInstance(
                            CameraPipeConfig.defaultConfig());
                    sCameraImplementationType = cameraImplementation;
                } else {
                    throw new IllegalArgumentException("Failed to configure the CameraProvider "
                            + "using unknown " + cameraImplementation + " implementation option.");
                }
            }
        }

        mOrientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int i) {
                mDeviceOrientation = i;
            }
        };

        mPreviewView = findViewById(R.id.camera_preview);
        mDurationText = findViewById(R.id.duration);
        mSwitchTimeText = findViewById(R.id.switch_time);
        mStartButton = findViewById(R.id.start_recording);

        Preconditions.checkNotNull(mPreviewView, "Cannot get the preview view.");
        Preconditions.checkNotNull(mDurationText, "Cannot get the duration text view.");
        Preconditions.checkNotNull(mSwitchTimeText, "Cannot get the switch time text view.");
        Preconditions.checkNotNull(mStartButton, "Cannot get the start button view.");

        mDurationText.setText(Long.toString(
                extraDurationMillis == INVALID_TIME_VALUE ? DEFAULT_DURATION_MILLIS
                        : extraDurationMillis));
        mSwitchTimeText.setText(Long.toString(
                extraSwitchTimeMillis == INVALID_TIME_VALUE ? DEFAULT_SWITCH_TIME_MILLIS
                        : extraSwitchTimeMillis));
        mStartButton.setOnClickListener(view -> startRecording());

        if (allPermissionsGranted()) {
            if (mCameraProvider != null) {
                mCameraProvider.unbindAll();
            }
            prepareCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOrientationEventListener.enable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientationEventListener.disable();
    }

    private void prepareCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                mCameraProvider = cameraProviderFuture.get();
                bindUseCases(mCameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                String msg = "Fail to bind the use cases with the camera.";
                Logger.d(TAG, msg);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        mPreviewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        mPreview = new Preview.Builder().build();
        mPreview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
        mVideoCapture = new VideoCapture.Builder<>(new Recorder.Builder().build()).setMirrorMode(
                mMirrorMode).build();
        mCamera = cameraProvider.bindToLifecycle(this, mCameraSelector, mPreview, mVideoCapture);
    }

    private void switchCamera() {
        Preconditions.checkNotNull(mCamera, "The camera instance should not be null.");
        Preconditions.checkNotNull(mCameraProvider, "The camera provider should not be null");
        Preconditions.checkNotNull(mPreview, "The preview use case should not be null.");
        Preconditions.checkNotNull(mVideoCapture, "The video capture use case should not be null.");
        mCameraProvider.unbindAll();
        final CameraSelector newLensFacing;
        switch (mCamera.getCameraInfo().getLensFacing()) {
            case CameraSelector.LENS_FACING_BACK:
                newLensFacing = CameraSelector.DEFAULT_FRONT_CAMERA;
                break;
            case CameraSelector.LENS_FACING_FRONT:
                newLensFacing = CameraSelector.DEFAULT_BACK_CAMERA;
                break;
            default:
                throw new IllegalStateException("Invalid camera lens facing.");
        }
        mCamera = mCameraProvider.bindToLifecycle(this, newLensFacing, mPreview, mVideoCapture);
    }

    @SuppressLint("NullAnnotationGroup")
    @SuppressWarnings("FutureReturnValueIgnored")
    @OptIn(markerClass = ExperimentalPersistentRecording.class)
    private void startRecording() {
        Preconditions.checkNotNull(mVideoCapture, "The video capture use case should not be null.");
        final long expectedDurationMillis = Long.parseLong(mDurationText.getText().toString());
        final long expectedSwitchTimeMillis = Long.parseLong(mSwitchTimeText.getText().toString());
        if (expectedSwitchTimeMillis >= expectedDurationMillis) {
            String msg = "The switch time should be less than the duration.";
            Logger.d(TAG, msg);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            return;
        }
        mStartButton.setClickable(false);
        mStartButton.setText(R.string.record_button_recording);
        mDurationText.setClickable(false);
        mSwitchTimeText.setClickable(false);

        // Export the test information whenever a new recording is started.
        exportTestInformation();

        final String videoFileName = generateFileName(VIDEO_FILE_PREFIX, true);
        final PendingRecording pendingRecording;
        if (FileUtil.canDeviceWriteToMediaStore()) {
            // Use MediaStoreOutputOptions for public share media storage.
            pendingRecording = mVideoCapture.getOutput().prepareRecording(this,
                    FileUtil.generateVideoMediaStoreOptions(this.getContentResolver(),
                            videoFileName));
        } else {
            pendingRecording = mVideoCapture.getOutput().prepareRecording(this,
                    FileUtil.generateVideoFileOutputOptions(videoFileName, "mp4"));
        }
        mRecording = pendingRecording
                .asPersistentRecording() // Perform the recording as a persistent recording.
                .start(ContextCompat.getMainExecutor(this),
                        videoRecordEvent -> {
                            if (videoRecordEvent instanceof VideoRecordEvent.Status) {
                                long currentDurationMillis = TimeUnit.NANOSECONDS.toMillis(
                                        videoRecordEvent.getRecordingStats()
                                                .getRecordedDurationNanos());
                                if (currentDurationMillis >= expectedSwitchTimeMillis) {
                                    if (mNotYetSwitched) {
                                        switchCamera();
                                        mNotYetSwitched = false;
                                    }
                                    if (currentDurationMillis >= expectedDurationMillis) {
                                        Preconditions.checkNotNull(mRecording, "The in-progress "
                                                + "recording should not be null.");
                                        mRecording.stop();
                                    }
                                }
                            }
                            if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                                mRecording = null;
                                mNotYetSwitched = true;
                                mStartButton.setClickable(true);
                                mStartButton.setText(R.string.record_button_idling);
                                mDurationText.setClickable(true);
                                mSwitchTimeText.setClickable(true);
                            }
                        });
    }

    private void exportTestInformation() {
        String information = KEY_DEVICE_ORIENTATION + ": " + mDeviceOrientation;
        FileUtil.writeTextToExternalFile(information,
                generateFileName(INFO_FILE_PREFIX, true), "txt");
    }

    @NonNull
    private String generateFileName(@Nullable String prefix, boolean isUnique) {
        if (!isUnique && !FileUtil.isFileNameValid(prefix)) {
            throw new IllegalArgumentException("Invalid arguments for generating file name.");
        }
        StringBuilder fileName = new StringBuilder();
        if (FileUtil.isFileNameValid(prefix)) {
            fileName.append(prefix);
            if (isUnique) {
                fileName.append("_");
            }
        }
        if (isUnique) {
            fileName.append(System.currentTimeMillis());
        }
        return fileName.toString();
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                prepareCamera();
            } else {
                Toast.makeText(this, getString(R.string.permission_warning),
                        Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }
}
