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

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Logger;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.ExperimentalPersistentRecording;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.video.internal.compat.quirk.DeviceQuirks;
import androidx.camera.video.internal.compat.quirk.MediaStoreVideoCannotWrite;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
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
    private static final long INVALID_TIME_VALUE = -1;
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            "android.permission.CAMERA"
    };
    private static final long DEFAULT_DURATION_MILLIS = 10000;
    private static final long DEFAULT_SWITCH_TIME_MILLIS = 5000;

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
                if (extraCameraDirection.equals("FORWARD")) {
                    mCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                } else {
                    mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                }
            }
            extraDurationMillis = bundle.getLong(INTENT_EXTRA_DURATION, INVALID_TIME_VALUE);
            extraSwitchTimeMillis = bundle.getLong(INTENT_EXTRA_SWITCH_TIME, INVALID_TIME_VALUE);
        }

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
        mVideoCapture = VideoCapture.withOutput(new Recorder.Builder().build());
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
        final long durationMillis = Long.parseLong(mDurationText.getText().toString());
        final long switchTimeMillis = Long.parseLong(mSwitchTimeText.getText().toString());
        if (switchTimeMillis >= durationMillis) {
            String msg = "The switch time should be less than the duration.";
            Logger.d(TAG, msg);
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            return;
        }
        mStartButton.setClickable(false);
        mStartButton.setText(R.string.record_button_recording);
        mDurationText.setClickable(false);
        mSwitchTimeText.setClickable(false);

        final PendingRecording pendingRecording;
        if (DeviceQuirks.get(MediaStoreVideoCannotWrite.class) != null) {
            // Use FileOutputOption for devices in MediaStoreVideoCannotWrite Quirk.
            pendingRecording = mVideoCapture.getOutput().prepareRecording(
                    this, generateFileOutputOptions(durationMillis));
        } else {
            // Use MediaStoreOutputOptions for public share media storage.
            pendingRecording = mVideoCapture.getOutput().prepareRecording(
                    this, generateMediaStoreOutputOptions(durationMillis));
        }
        mRecording = pendingRecording
                .asPersistentRecording() // Perform the recording as a persistent recording.
                .start(ContextCompat.getMainExecutor(this),
                        videoRecordEvent -> {
                            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                                // Switch camera after the specified time.
                                CameraXExecutors.mainThreadExecutor().schedule(this::switchCamera,
                                        switchTimeMillis, TimeUnit.MILLISECONDS);
                            }
                            if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                                mRecording = null;
                                mStartButton.setClickable(true);
                                mStartButton.setText(R.string.record_button_idling);
                                mDurationText.setClickable(true);
                                mSwitchTimeText.setClickable(true);
                            }
                        });
    }

    @NonNull
    private FileOutputOptions generateFileOutputOptions(@IntRange(from = 0) long durationMillis) {
        String videoFileName = "video_" + System.currentTimeMillis() + ".mp4";
        File videoFolder = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES);
        if (!videoFolder.exists() && !videoFolder.mkdirs()) {
            Logger.e(TAG, "Failed to create directory: " + videoFolder);
        }
        return new FileOutputOptions.Builder(
                new File(videoFolder, videoFileName)).setDurationLimitMillis(
                durationMillis).build();
    }

    @NonNull
    private MediaStoreOutputOptions generateMediaStoreOutputOptions(
            @IntRange(from = 0) long durationMillis) {
        String videoFileName = "video_" + System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.TITLE, videoFileName);
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName);
        contentValues.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        contentValues.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
        return new MediaStoreOutputOptions.Builder(getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .setDurationLimitMillis(durationMillis)
                .build();
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
