/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static androidx.camera.core.ImageCapture.FLASH_MODE_AUTO;
import static androidx.camera.core.ImageCapture.FLASH_MODE_OFF;
import static androidx.camera.core.ImageCapture.FLASH_MODE_ON;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Range;
import android.util.Rational;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.ActiveRecording;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Recorder;
import androidx.camera.video.RecordingStats;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.content.ContextCompat;
import androidx.core.math.MathUtils;
import androidx.core.util.Consumer;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An activity with four use cases: (1) view finder, (2) image capture, (3) image analysis, (4)
 * video capture.
 *
 * <p>All four use cases are created with CameraX and tied to the activity's lifecycle. CameraX
 * automatically connects and disconnects the use cases from the camera in response to changes in
 * the activity's lifecycle. Therefore, the use cases function properly when the app is paused and
 * resumed and when the device is rotated. The complex interactions between the camera and these
 * lifecycle events are handled internally by CameraX.
 */
public class CameraXActivity extends AppCompatActivity {
    private static final String TAG = "CameraXActivity";
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
    // Possible values for this intent key: "backward" or "forward".
    private static final String INTENT_EXTRA_CAMERA_DIRECTION = "camera_direction";
    public static final String INTENT_EXTRA_CAMERA_IMPLEMENTATION = "camera_implementation";
    static final CameraSelector BACK_SELECTOR =
            new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
    static final CameraSelector FRONT_SELECTOR =
            new CameraSelector.Builder().requireLensFacing(
                    CameraSelector.LENS_FACING_FRONT).build();

    private final AtomicLong mImageAnalysisFrameCount = new AtomicLong(0);
    private final AtomicLong mPreviewFrameCount = new AtomicLong(0);
    private final MutableLiveData<String> mImageAnalysisResult = new MutableLiveData<>();
    private static final String BACKWARD = "BACKWARD";
    private ActiveRecording mActiveRecording;
    /** The camera to use */
    CameraSelector mCurrentCameraSelector = BACK_SELECTOR;
    ProcessCameraProvider mCameraProvider;
    private CameraXViewModel.CameraProviderResult mCameraProviderResult;

    // TODO: Move the analysis processing, capture processing to separate threads, so
    // there is smaller impact on the preview.
    View mViewFinder;
    private List<UseCase> mUseCases;
    private ExecutorService mImageCaptureExecutorService;
    Camera mCamera;

    private ToggleButton mVideoToggle;
    private ToggleButton mPhotoToggle;
    private ToggleButton mAnalysisToggle;
    private ToggleButton mPreviewToggle;

    private Button mTakePicture;
    private ImageButton mCameraDirectionButton;
    private ImageButton mFlashButton;
    private TextView mTextView;
    private ImageButton mTorchButton;
    private ToggleButton mCaptureQualityToggle;
    private Button mPlusEV;
    private Button mDecEV;
    private TextView mZoomRatioLabel;
    private SeekBar mZoomSeekBar;

    private OpenGLRenderer mPreviewRenderer;
    private DisplayManager.DisplayListener mDisplayListener;
    private RecordUi mRecordUi;

    SessionImagesUriSet mSessionImagesUriSet = new SessionImagesUriSet();

    // Analyzer to be used with ImageAnalysis.
    private ImageAnalysis.Analyzer mAnalyzer = new ImageAnalysis.Analyzer() {
        @Override
        public void analyze(@NonNull ImageProxy image) {
            // Since we set the callback handler to a main thread handler, we can call
            // setValue() here. If we weren't on the main thread, we would have to call
            // postValue() instead.
            mImageAnalysisResult.setValue(
                    Long.toString(image.getImageInfo().getTimestamp()));
            try {
                if (!mAnalysisIdlingResource.isIdleNow()) {
                    mAnalysisIdlingResource.decrement();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Unexpected counter decrement");
            }
            image.close();
        }
    };

    private FutureCallback<Integer> mEVFutureCallback = new FutureCallback<Integer>() {

        @Override
        public void onSuccess(@Nullable Integer result) {
            CameraInfo cameraInfo = getCameraInfo();
            if (cameraInfo != null) {
                ExposureState exposureState = cameraInfo.getExposureState();
                float ev = result * exposureState.getExposureCompensationStep().floatValue();
                Log.d(TAG, "success new EV: " + ev);
                Toast.makeText(getApplicationContext(), String.format("EV: %.2f", ev),
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailure(@NonNull Throwable t) {
            Log.d(TAG, "failed " + t);
            Toast.makeText(getApplicationContext(), "Fail to set EV", Toast.LENGTH_SHORT).show();
        }
    };

    // Listener that handles all ToggleButton events.
    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener =
            (compoundButton, isChecked) -> tryBindUseCases();

    private Consumer<Long> mFrameUpdateListener = timestamp -> {
        if (mPreviewFrameCount.getAndIncrement() >= FRAMES_UNTIL_VIEW_IS_READY) {
            try {
                if (!this.mViewIdlingResource.isIdleNow()) {
                    Log.d(TAG, FRAMES_UNTIL_VIEW_IS_READY + " or more counted on preview."
                            + " Make IdlingResource idle.");
                    this.mViewIdlingResource.decrement();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Unexpected decrement. Continuing");
            }
        }
    };

    // Espresso testing variables
    private final CountingIdlingResource mViewIdlingResource = new CountingIdlingResource("view");
    private static final int FRAMES_UNTIL_VIEW_IS_READY = 5;
    private final CountingIdlingResource mAnalysisIdlingResource =
            new CountingIdlingResource("analysis");
    private final CountingIdlingResource mImageSavedIdlingResource =
            new CountingIdlingResource("imagesaved");
    private final CountingIdlingResource mInitializationIdlingResource =
            new CountingIdlingResource("initialization");

    /**
     * Retrieve idling resource that waits for image received by analyzer).
     */
    @VisibleForTesting
    @NonNull
    public IdlingResource getAnalysisIdlingResource() {
        return mAnalysisIdlingResource;
    }

    /**
     * Retrieve idling resource that waits view to get texture update.
     */
    @VisibleForTesting
    @NonNull
    public IdlingResource getViewIdlingResource() {
        return mViewIdlingResource;
    }

    /**
     * Retrieve idling resource that waits for capture to complete (save or error).
     */
    @VisibleForTesting
    @NonNull
    public IdlingResource getImageSavedIdlingResource() {
        return mImageSavedIdlingResource;
    }

    /**
     * Retrieve idling resource that waits for initialization to finish.
     */
    @VisibleForTesting
    @NonNull
    public IdlingResource getInitializationIdlingResource() {
        return mInitializationIdlingResource;
    }

    /**
     * Returns the result of CameraX initialization.
     *
     * <p>This will only be set after initialization has finished, which will occur once
     * {@link #getInitializationIdlingResource()} is idle.
     *
     * <p>Should only be called on the main thread.
     */
    @VisibleForTesting
    @MainThread
    @Nullable
    public CameraXViewModel.CameraProviderResult getCameraProviderResult() {
        return mCameraProviderResult;
    }

    /**
     * Retrieve idling resource that waits for view to display frames before proceeding.
     */
    @VisibleForTesting
    public void resetViewIdlingResource() {
        mPreviewFrameCount.set(0);
        // Make the view idling resource non-idle, until required framecount achieved.
        mViewIdlingResource.increment();
    }

    /**
     * Delete images that were taking during this session so far.
     * May leak images if pending captures not completed.
     */
    @VisibleForTesting
    public void deleteSessionImages() {
        mSessionImagesUriSet.deleteAllUris();
    }


    @ImageCapture.CaptureMode
    int getCaptureMode() {
        return mCaptureQualityToggle.isChecked() ? ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY :
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY;
    }

    private boolean isFlashAvailable() {
        CameraInfo cameraInfo = getCameraInfo();
        return mPhotoToggle.isChecked() && cameraInfo != null && cameraInfo.hasFlashUnit();
    }

    private boolean isExposureCompensationSupported() {
        CameraInfo cameraInfo = getCameraInfo();
        return cameraInfo != null
                && cameraInfo.getExposureState().isExposureCompensationSupported();
    }

    private void setUpFlashButton() {
        mFlashButton.setOnClickListener(v -> {
            @ImageCapture.FlashMode int flashMode = getImageCapture().getFlashMode();
            if (flashMode == FLASH_MODE_ON) {
                getImageCapture().setFlashMode(FLASH_MODE_OFF);
            } else if (flashMode == FLASH_MODE_OFF) {
                getImageCapture().setFlashMode(FLASH_MODE_AUTO);
            } else if (flashMode == FLASH_MODE_AUTO) {
                getImageCapture().setFlashMode(FLASH_MODE_ON);
            }
            updateButtonsUi();
        });
    }

    private void setUpRecordButton() {
        mRecordUi.getButtonRecord().setOnClickListener((view) -> {
            RecordUi.State state = mRecordUi.getState();
            switch (state) {
                case IDLE:
                    createDefaultVideoFolderIfNotExist();
                    mActiveRecording = getVideoCapture().getOutput()
                            .prepareRecording(getNewVideoOutputFileOptions())
                            .withEventListener(ContextCompat.getMainExecutor(CameraXActivity.this),
                                    mVideoRecordEventListener)
                            .start();
                    mRecordUi.setState(RecordUi.State.RECORDING);
                    break;
                case RECORDING:
                case PAUSED:
                    mActiveRecording.stop();
                    mActiveRecording = null;
                    mRecordUi.setState(RecordUi.State.STOPPING);
                    break;
                case STOPPING:
                    // Record button should be disabled.
                default:
                    throw new IllegalStateException(
                            "Unexpected state when click record button: " + state);
            }
        });

        mRecordUi.getButtonPause().setOnClickListener(view -> {
            RecordUi.State state = mRecordUi.getState();
            switch (state) {
                case RECORDING:
                    mActiveRecording.pause();
                    mRecordUi.setState(RecordUi.State.PAUSED);
                    break;
                case PAUSED:
                    mActiveRecording.resume();
                    mRecordUi.setState(RecordUi.State.RECORDING);
                    break;
                case IDLE:
                case STOPPING:
                    // Pause button should be invisible.
                default:
                    throw new IllegalStateException(
                            "Unexpected state when click pause button: " + state);
            }
        });
    }

    private final Consumer<VideoRecordEvent> mVideoRecordEventListener = event -> {
        updateRecordingStats(event.getRecordingStats());

        switch (event.getEventType()) {
            case FINALIZE:
                VideoRecordEvent.Finalize finalize = (VideoRecordEvent.Finalize) event;

                switch (finalize.getError()) {
                    case VideoRecordEvent.ERROR_NONE:
                    case VideoRecordEvent.ERROR_FILE_SIZE_LIMIT_REACHED:
                    case VideoRecordEvent.ERROR_INSUFFICIENT_DISK:
                        Uri uri = finalize.getOutputResults().getOutputUri();
                        String msg = "Saved uri " + uri;
                        if (finalize.getError() != VideoRecordEvent.ERROR_NONE) {
                            msg += " with error (" + finalize.getError() + ")";
                        }
                        // The video file path is used in tracing e2e test log. Don't remove it.
                        String videoFile = getAbsolutePathFromUri(
                                getApplicationContext().getContentResolver(),
                                uri
                        );
                        Log.d(TAG, "Saved video file: " + videoFile);

                        Log.d(TAG, msg, finalize.getCause());
                        Toast.makeText(CameraXActivity.this, msg, Toast.LENGTH_LONG).show();
                        break;
                    default:
                        String errMsg = "Video capture failed by (" + finalize.getError() + "): "
                                + finalize.getCause();
                        Log.e(TAG, errMsg, finalize.getCause());
                        Toast.makeText(CameraXActivity.this, errMsg, Toast.LENGTH_LONG).show();
                }
                mRecordUi.setState(RecordUi.State.IDLE);
                break;

            default:
                // No-op
                break;
        }
    };

    @NonNull
    private MediaStoreOutputOptions getNewVideoOutputFileOptions() {
        String videoFileName = "video_" + System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.TITLE, videoFileName);
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName);
        contentValues.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        contentValues.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
        return MediaStoreOutputOptions.builder()
                .setContentResolver(getContentResolver())
                .setCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build();
    }

    private void updateRecordingStats(@NonNull RecordingStats stats) {
        double durationSec = TimeUnit.NANOSECONDS.toMillis(stats.getRecordedDurationNs()) / 1000d;
        // Show megabytes in International System of Units (SI)
        double sizeMb = stats.getNumBytesRecorded() / (1000d * 1000d);
        String msg = String.format("%.2f sec\n%.2f MB", durationSec, sizeMb);
        mRecordUi.getTextStats().setText(msg);
    }

    private void setUpTakePictureButton() {
        mTakePicture.setOnClickListener(
                new View.OnClickListener() {
                    long mStartCaptureTime = 0;

                    @Override
                    public void onClick(View view) {
                        mImageSavedIdlingResource.increment();

                        mStartCaptureTime = SystemClock.elapsedRealtime();
                        createDefaultPictureFolderIfNotExist();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                        ImageCapture.OutputFileOptions outputFileOptions =
                                new ImageCapture.OutputFileOptions.Builder(
                                        getContentResolver(),
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        contentValues).build();
                        getImageCapture().takePicture(outputFileOptions,
                                mImageCaptureExecutorService,
                                new ImageCapture.OnImageSavedCallback() {
                                    @Override
                                    public void onImageSaved(
                                            @NonNull ImageCapture.OutputFileResults
                                                    outputFileResults) {
                                        Log.d(TAG, "Saved image to "
                                                + outputFileResults.getSavedUri());
                                        try {
                                            mImageSavedIdlingResource.decrement();
                                        } catch (IllegalStateException e) {
                                            Log.e(TAG, "Error: unexpected onImageSaved "
                                                    + "callback received. Continuing.");
                                        }

                                        long duration =
                                                SystemClock.elapsedRealtime() - mStartCaptureTime;
                                        runOnUiThread(() -> Toast.makeText(CameraXActivity.this,
                                                "Image captured in " + duration + " ms",
                                                Toast.LENGTH_SHORT).show());
                                        if (mSessionImagesUriSet != null) {
                                            mSessionImagesUriSet.add(
                                                    Objects.requireNonNull(
                                                            outputFileResults.getSavedUri()));
                                        }
                                    }

                                    @Override
                                    public void onError(@NonNull ImageCaptureException exception) {
                                        Log.e(TAG, "Failed to save image.", exception.getCause());
                                        try {
                                            mImageSavedIdlingResource.decrement();
                                        } catch (IllegalStateException e) {
                                            Log.e(TAG, "Error: unexpected onImageSaved "
                                                    + "callback received. Continuing.");
                                        }
                                    }
                                });
                    }
                });
    }

    @SuppressWarnings("ObjectToString")
    private void setUpCameraDirectionButton() {
        mCameraDirectionButton.setOnClickListener(v -> {
            if (mCurrentCameraSelector == BACK_SELECTOR) {
                mCurrentCameraSelector = FRONT_SELECTOR;
            } else if (mCurrentCameraSelector == FRONT_SELECTOR) {
                mCurrentCameraSelector = BACK_SELECTOR;
            }
            Log.d(TAG, "Change camera direction: " + mCurrentCameraSelector);
            tryBindUseCases();
        });
    }

    private void setUpTorchButton() {
        mTorchButton.setOnClickListener(v -> {
            Objects.requireNonNull(getCameraInfo());
            Objects.requireNonNull(getCameraControl());
            Integer torchState = getCameraInfo().getTorchState().getValue();
            boolean toggledState = !Objects.equals(torchState, TorchState.ON);
            Log.d(TAG, "Set camera torch: " + toggledState);
            ListenableFuture<Void> future = getCameraControl().enableTorch(toggledState);
            Futures.addCallback(future, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void result) {
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    throw new RuntimeException(t);
                }
            }, CameraXExecutors.directExecutor());
        });
    }

    private void setUpEVButton() {
        mPlusEV.setOnClickListener(v -> {
            Objects.requireNonNull(getCameraInfo());
            Objects.requireNonNull(getCameraControl());

            ExposureState exposureState = getCameraInfo().getExposureState();
            Range<Integer> range = exposureState.getExposureCompensationRange();
            int ec = exposureState.getExposureCompensationIndex();

            if (range.contains(ec + 1)) {
                ListenableFuture<Integer> future =
                        getCameraControl().setExposureCompensationIndex(ec + 1);
                Futures.addCallback(future, mEVFutureCallback,
                        CameraXExecutors.mainThreadExecutor());
            } else {
                Toast.makeText(getApplicationContext(), String.format("EV: %.2f",
                        range.getUpper()
                                * exposureState.getExposureCompensationStep().floatValue()),
                        Toast.LENGTH_LONG).show();
            }
        });

        mDecEV.setOnClickListener(v -> {
            Objects.requireNonNull(getCameraInfo());
            Objects.requireNonNull(getCameraControl());

            ExposureState exposureState = getCameraInfo().getExposureState();
            Range<Integer> range = exposureState.getExposureCompensationRange();
            int ec = exposureState.getExposureCompensationIndex();

            if (range.contains(ec - 1)) {
                ListenableFuture<Integer> future =
                        getCameraControl().setExposureCompensationIndex(ec - 1);
                Futures.addCallback(future, mEVFutureCallback,
                        CameraXExecutors.mainThreadExecutor());
            } else {
                Toast.makeText(getApplicationContext(), String.format("EV: %.2f",
                        range.getLower()
                                * exposureState.getExposureCompensationStep().floatValue()),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateButtonsUi() {
        mRecordUi.setEnabled(mVideoToggle.isChecked());
        mTakePicture.setEnabled(mPhotoToggle.isChecked());
        mCaptureQualityToggle.setEnabled(mPhotoToggle.isChecked());
        mCameraDirectionButton.setEnabled(getCameraInfo() != null);
        mTorchButton.setEnabled(isFlashAvailable());
        // Flash button
        mFlashButton.setEnabled(mPhotoToggle.isChecked() && isFlashAvailable());
        if (mPhotoToggle.isChecked()) {
            switch (getImageCapture().getFlashMode()) {
                case FLASH_MODE_ON:
                    mFlashButton.setImageResource(R.drawable.ic_flash_on);
                    break;
                case FLASH_MODE_OFF:
                    mFlashButton.setImageResource(R.drawable.ic_flash_off);
                    break;
                case FLASH_MODE_AUTO:
                    mFlashButton.setImageResource(R.drawable.ic_flash_auto);
                    break;
            }
        }
        mPlusEV.setEnabled(isExposureCompensationSupported());
        mDecEV.setEnabled(isExposureCompensationSupported());
    }

    private void setUpButtonEvents() {
        mVideoToggle.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mPhotoToggle.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mAnalysisToggle.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mPreviewToggle.setOnCheckedChangeListener(mOnCheckedChangeListener);

        setUpRecordButton();
        setUpFlashButton();
        setUpTakePictureButton();
        setUpCameraDirectionButton();
        setUpTorchButton();
        setUpEVButton();
        mCaptureQualityToggle.setOnCheckedChangeListener(mOnCheckedChangeListener);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_xmain);
        mImageCaptureExecutorService = Executors.newSingleThreadExecutor();
        OpenGLRenderer previewRenderer = mPreviewRenderer = new OpenGLRenderer();
        ViewStub viewFinderStub = findViewById(R.id.viewFinderStub);
        mViewFinder = OpenGLActivity.chooseViewFinder(getIntent().getExtras(), viewFinderStub,
                previewRenderer);
        mViewFinder.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)
                        -> tryBindUseCases());

        mVideoToggle = findViewById(R.id.VideoToggle);
        mPhotoToggle = findViewById(R.id.PhotoToggle);
        mAnalysisToggle = findViewById(R.id.AnalysisToggle);
        mPreviewToggle = findViewById(R.id.PreviewToggle);

        mTakePicture = findViewById(R.id.Picture);
        mFlashButton = findViewById(R.id.flash_toggle);
        mCameraDirectionButton = findViewById(R.id.direction_toggle);
        mTorchButton = findViewById(R.id.torch_toggle);
        mCaptureQualityToggle = findViewById(R.id.capture_quality);
        mPlusEV = findViewById(R.id.plus_ev_toggle);
        mDecEV = findViewById(R.id.dec_ev_toggle);
        mZoomSeekBar = findViewById(R.id.seekBar);
        mZoomRatioLabel = findViewById(R.id.zoomRatio);

        mTextView = findViewById(R.id.textView);
        mRecordUi = new RecordUi(
                findViewById(R.id.Video),
                findViewById(R.id.video_pause),
                findViewById(R.id.video_stats)
        );

        setUpButtonEvents();
        setupViewFinderGestureControls();

        mImageAnalysisResult.observe(
                this,
                text -> {
                    if (mImageAnalysisFrameCount.getAndIncrement() % 30 == 0) {
                        mTextView.setText(
                                "ImgCount: " + mImageAnalysisFrameCount.get() + " @ts: "
                                        + text);
                    }
                });

        mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {

            }

            @Override
            public void onDisplayRemoved(int displayId) {

            }

            @Override
            public void onDisplayChanged(int displayId) {
                Display viewFinderDisplay = mViewFinder.getDisplay();
                if (viewFinderDisplay != null && viewFinderDisplay.getDisplayId() == displayId) {
                    previewRenderer.invalidateSurface(
                            Surfaces.toSurfaceRotationDegrees(viewFinderDisplay.getRotation()));
                }
            }
        };

        DisplayManager dpyMgr =
                Objects.requireNonNull((DisplayManager) getSystemService(Context.DISPLAY_SERVICE));
        dpyMgr.registerDisplayListener(mDisplayListener, new Handler(Looper.getMainLooper()));

        StrictMode.VmPolicy vmPolicy =
                new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setVmPolicy(vmPolicy);
        StrictMode.ThreadPolicy threadPolicy =
                new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setThreadPolicy(threadPolicy);

        // Get params from adb extra string
        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            String newCameraDirection = bundle.getString(INTENT_EXTRA_CAMERA_DIRECTION);
            if (newCameraDirection != null) {
                if (newCameraDirection.equals(BACKWARD)) {
                    mCurrentCameraSelector = BACK_SELECTOR;
                } else {
                    mCurrentCameraSelector = FRONT_SELECTOR;
                }
            }

            String cameraImplementation = bundle.getString(INTENT_EXTRA_CAMERA_IMPLEMENTATION);
            if (cameraImplementation != null) {
                CameraXViewModel.configureCameraProvider(cameraImplementation);
            }
        }

        mInitializationIdlingResource.increment();
        CameraXViewModel viewModel = new ViewModelProvider(this).get(CameraXViewModel.class);
        viewModel.getCameraProvider().observe(this, cameraProviderResult -> {
            mCameraProviderResult = cameraProviderResult;
            mInitializationIdlingResource.decrement();
            if (cameraProviderResult.hasProvider()) {
                mCameraProvider = cameraProviderResult.getProvider();
                tryBindUseCases();
            } else {
                Log.e(TAG, "Failed to retrieve ProcessCameraProvider",
                        cameraProviderResult.getError());
                Toast.makeText(getApplicationContext(), "Unable to initialize CameraX. See logs "
                        + "for details.", Toast.LENGTH_LONG).show();
            }
        });

        setupPermissions();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DisplayManager dpyMgr =
                Objects.requireNonNull((DisplayManager) getSystemService(Context.DISPLAY_SERVICE));
        dpyMgr.unregisterDisplayListener(mDisplayListener);
        mPreviewRenderer.shutdown();
        mImageCaptureExecutorService.shutdown();
    }

    void tryBindUseCases() {
        tryBindUseCases(false);
    }

    /**
     * Try building and binding current use cases.
     *
     * @param calledBySelf flag indicates if this is a recursive call.
     */
    void tryBindUseCases(boolean calledBySelf) {
        boolean isViewFinderReady = mViewFinder.getWidth() != 0 && mViewFinder.getHeight() != 0;
        boolean isCameraReady = mCameraProvider != null;
        if (isPermissionMissing() || !isCameraReady || !isViewFinderReady) {
            // No-op if permission if something is not ready. It will try again upon the
            // next thing being ready.
            return;
        }
        // Clear listening frame update before unbind all.
        mPreviewRenderer.clearFrameUpdateListener();

        // Stop video recording if exists.
        if (mRecordUi.getState() == RecordUi.State.RECORDING
                || mRecordUi.getState() == RecordUi.State.PAUSED) {
            mActiveRecording.stop();
            mActiveRecording = null;
            mRecordUi.setState(RecordUi.State.STOPPING);
        }

        mCameraProvider.unbindAll();
        try {
            List<UseCase> useCases = buildUseCases();
            mCamera = bindToLifecycleSafely(useCases);
            // Set the use cases after a successful binding.
            mUseCases = useCases;
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "bindToLifecycle() failed. Usually caused by binding too many use cases.");
            Toast.makeText(this, "Bind too many use cases.", Toast.LENGTH_SHORT).show();

            // Restore toggle buttons to the previous state if the bind failed.
            mPreviewToggle.setChecked(getPreview() != null);
            mPhotoToggle.setChecked(getImageCapture() != null);
            mAnalysisToggle.setChecked(getImageAnalysis() != null);
            mVideoToggle.setChecked(getVideoCapture() != null);

            if (!calledBySelf) {
                // Only call self if not already calling self to avoid an infinite loop.
                tryBindUseCases(true);
            }
        }
        updateButtonsUi();
    }

    /**
     * Builds all use cases based on current settings and return as an array.
     */
    private List<UseCase> buildUseCases() {
        List<UseCase> useCases = new ArrayList<>();
        if (mPreviewToggle.isChecked()) {
            Preview preview = new Preview.Builder()
                    .setTargetName("Preview")
                    .build();
            resetViewIdlingResource();
            // Use the listener of the future to make sure the Preview setup the new surface.
            mPreviewRenderer.attachInputPreview(preview).addListener(() -> {
                Log.d(TAG, "OpenGLRenderer get the new surface for the Preview");
                mPreviewRenderer.setFrameUpdateListener(
                        ContextCompat.getMainExecutor(this), mFrameUpdateListener
                );
            }, ContextCompat.getMainExecutor(this));

            useCases.add(preview);
        }

        if (mPhotoToggle.isChecked()) {
            ImageCapture imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(getCaptureMode())
                    .setTargetName("ImageCapture")
                    .build();
            useCases.add(imageCapture);
        }

        if (mAnalysisToggle.isChecked()) {
            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetName("ImageAnalysis")
                    .build();
            useCases.add(imageAnalysis);
            // Make the analysis idling resource non-idle, until a frame received.
            mAnalysisIdlingResource.increment();
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), mAnalyzer);
        }

        if (mVideoToggle.isChecked()) {
            VideoCapture<Recorder> videoCapture =
                    VideoCapture.withOutput(new Recorder.Builder().build());
            useCases.add(videoCapture);
        }
        return useCases;
    }

    /**
     * Request permission if missing.
     */
    private void setupPermissions() {
        if (isPermissionMissing()) {
            ActivityResultLauncher<String[]> permissionLauncher =
                    registerForActivityResult(
                            new ActivityResultContracts.RequestMultiplePermissions(),
                            result -> {
                                for (String permission : REQUIRED_PERMISSIONS) {
                                    if (!Objects.requireNonNull(result.get(permission))) {
                                        Toast.makeText(getApplicationContext(),
                                                "Camera permission denied.",
                                                Toast.LENGTH_SHORT)
                                                .show();
                                        finish();
                                        return;
                                    }
                                }
                                tryBindUseCases();
                            });

            permissionLauncher.launch(REQUIRED_PERMISSIONS);
        } else {
            // Permissions already granted. Start camera.
            tryBindUseCases();
        }
    }

    /** Returns true if any of the required permissions is missing. */
    private boolean isPermissionMissing() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    private void createDefaultPictureFolderIfNotExist() {
        File pictureFolder = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        if (!pictureFolder.exists()) {
            if (!pictureFolder.mkdir()) {
                Log.e(TAG, "Failed to create directory: " + pictureFolder);
            }
        }
    }

    /** Checks the folder existence by how the video file be created. */
    private void createDefaultVideoFolderIfNotExist() {
        String videoFilePath =
                getAbsolutePathFromUri(getApplicationContext().getContentResolver(),
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

        // If cannot get the video path, just skip checking and create folder.
        if (videoFilePath == null) {
            return;
        }
        File videoFile = new File(videoFilePath);

        if (videoFile.getParentFile() != null && !videoFile.getParentFile().exists()) {
            if (!videoFile.getParentFile().mkdir()) {
                Log.e(TAG, "Failed to create directory: " + videoFile);
            }
        }
    }

    /**
     * Binds use cases to the current lifecycle.
     */
    private Camera bindToLifecycleSafely(List<UseCase> useCases) {
        ViewPort viewPort = new ViewPort.Builder(new Rational(mViewFinder.getWidth(),
                mViewFinder.getHeight()),
                mViewFinder.getDisplay().getRotation())
                .setScaleType(ViewPort.FILL_CENTER).build();
        UseCaseGroup.Builder useCaseGroupBuilder = new UseCaseGroup.Builder().setViewPort(
                viewPort);
        for (UseCase useCase : useCases) {
            useCaseGroupBuilder.addUseCase(useCase);
        }
        mCamera = mCameraProvider.bindToLifecycle(this, mCurrentCameraSelector,
                useCaseGroupBuilder.build());
        setupZoomSeeker();
        return mCamera;
    }

    private static final int MAX_SEEKBAR_VALUE = 100000;

    void showZoomRatioIsAlive() {
        mZoomRatioLabel.setTextColor(getResources().getColor(R.color.zoom_ratio_activated));
    }

    void showNormalZoomRatio() {
        mZoomRatioLabel.setTextColor(getResources().getColor(R.color.zoom_ratio_set));
    }

    ScaleGestureDetector.SimpleOnScaleGestureListener mScaleGestureListener =
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    if (mCamera == null) {
                        return true;
                    }

                    CameraInfo cameraInfo = mCamera.getCameraInfo();
                    CameraControl cameraControl = mCamera.getCameraControl();
                    float newZoom =
                            cameraInfo.getZoomState().getValue().getZoomRatio()
                                    * detector.getScaleFactor();
                    float clampedNewZoom = MathUtils.clamp(newZoom,
                            cameraInfo.getZoomState().getValue().getMinZoomRatio(),
                            cameraInfo.getZoomState().getValue().getMaxZoomRatio());

                    Log.d(TAG, "setZoomRatio ratio: " + clampedNewZoom);
                    showNormalZoomRatio();
                    ListenableFuture<Void> listenableFuture = cameraControl.setZoomRatio(
                            clampedNewZoom);
                    Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            Log.d(TAG, "setZoomRatio onSuccess: " + clampedNewZoom);
                            showZoomRatioIsAlive();
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Log.d(TAG, "setZoomRatio failed, " + t);
                        }
                    }, ContextCompat.getMainExecutor(CameraXActivity.this));
                    return true;
                }
            };

    GestureDetector.OnGestureListener onTapGestureListener =
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    // Since we are showing full camera preview we will be using
                    // DisplayOrientedMeteringPointFactory to map the view's (x, y) to a
                    // metering point.
                    MeteringPointFactory factory =
                            new DisplayOrientedMeteringPointFactory(
                                    mViewFinder.getDisplay(),
                                    mCamera.getCameraInfo(),
                                    mViewFinder.getWidth(),
                                    mViewFinder.getHeight());
                    FocusMeteringAction action = new FocusMeteringAction.Builder(
                            factory.createPoint(e.getX(), e.getY())
                    ).build();
                    Futures.addCallback(
                            mCamera.getCameraControl().startFocusAndMetering(action),
                            new FutureCallback<FocusMeteringResult>() {
                                @Override
                                public void onSuccess(FocusMeteringResult result) {
                                    Log.d(TAG, "Focus and metering succeeded.");
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    Log.e(TAG, "Focus and metering failed.", t);
                                }
                            },
                            CameraXExecutors.mainThreadExecutor());
                    return true;
                }
            };

    private void setupZoomSeeker() {
        CameraControl cameraControl = mCamera.getCameraControl();
        CameraInfo cameraInfo = mCamera.getCameraInfo();

        mZoomSeekBar.setMax(MAX_SEEKBAR_VALUE);
        mZoomSeekBar.setProgress(
                (int) (cameraInfo.getZoomState().getValue().getLinearZoom() * MAX_SEEKBAR_VALUE));
        mZoomSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }

                float percentage = (float) progress / MAX_SEEKBAR_VALUE;
                showNormalZoomRatio();
                ListenableFuture<Void> listenableFuture =
                        cameraControl.setLinearZoom(percentage);

                Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void result) {
                        Log.d(TAG, "setZoomPercentage " + percentage + " onSuccess");
                        showZoomRatioIsAlive();
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.d(TAG, "setZoomPercentage " + percentage + " failed, " + t);
                    }
                }, ContextCompat.getMainExecutor(CameraXActivity.this));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        cameraInfo.getZoomState().removeObservers(this);
        cameraInfo.getZoomState().observe(this,
                state -> {
                String str = String.format("%.2fx", state.getZoomRatio());
                mZoomRatioLabel.setText(str);
                mZoomSeekBar.setProgress((int) (MAX_SEEKBAR_VALUE * state.getLinearZoom()));
            });
    }

    private void setupViewFinderGestureControls() {
        GestureDetector tapGestureDetector = new GestureDetector(this, onTapGestureListener);
        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this, mScaleGestureListener);
        mViewFinder.setOnTouchListener((view, e) -> {
            boolean tapEventProcessed = tapGestureDetector.onTouchEvent(e);
            boolean scaleEventProcessed = scaleDetector.onTouchEvent(e);
            return tapEventProcessed || scaleEventProcessed;
        });
    }

    /** Gets the absolute path from a Uri. */
    @Nullable
    @SuppressWarnings("deprecation")
    public String getAbsolutePathFromUri(@NonNull ContentResolver resolver,
            @NonNull Uri contentUri) {
        Cursor cursor = null;
        try {
            // We should include in any Media collections.
            String[] proj;
            int columnIndex;
            // MediaStore.Video.Media.DATA was deprecated in API level 29.
            proj = new String[]{MediaStore.Video.Media.DATA};
            cursor = resolver.query(contentUri, proj, null, null, null);
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);

            cursor.moveToFirst();
            return cursor.getString(columnIndex);
        } catch (RuntimeException e) {
            Log.e(TAG, String.format(
                    "Failed in getting absolute path for Uri %s with Exception %s",
                    contentUri.toString(), e.toString()));
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private class SessionImagesUriSet {
        private final Set<Uri> mSessionImages;

        SessionImagesUriSet() {
            mSessionImages = Collections.synchronizedSet(new HashSet<>());
        }

        public void add(@NonNull Uri uri) {
            mSessionImages.add(uri);
        }

        public void deleteAllUris() {
            synchronized (mSessionImages) {
                Iterator<Uri> it = mSessionImages.iterator();
                while (it.hasNext()) {
                    getContentResolver().delete(it.next(), null, null);
                    it.remove();
                }
            }
        }
    }

    @UiThread
    private static class RecordUi {

        enum State {
            IDLE, RECORDING, PAUSED, STOPPING
        }

        private final Button mButtonRecord;
        private final Button mButtonPause;
        private final TextView mTextStats;
        private boolean mEnabled = false;
        private State mState = State.IDLE;

        RecordUi(@NonNull Button buttonRecord, @NonNull Button buttonPause,
                @NonNull TextView textStats) {
            mButtonRecord = buttonRecord;
            mButtonPause = buttonPause;
            mTextStats = textStats;
        }

        void setEnabled(boolean enabled) {
            mEnabled = enabled;
            if (enabled) {
                mTextStats.setText("");
                mTextStats.setVisibility(View.VISIBLE);
                updateUi();
            } else {
                mButtonRecord.setText("Record");
                mButtonRecord.setEnabled(false);
                mButtonPause.setVisibility(View.INVISIBLE);
                mTextStats.setVisibility(View.GONE);
            }
        }

        void setState(@NonNull State state) {
            mState = state;
            updateUi();
        }

        @NonNull
        State getState() {
            return mState;
        }

        private void updateUi() {
            if (!mEnabled) {
                return;
            }
            switch (mState) {
                case IDLE:
                    mButtonRecord.setText("Record");
                    mButtonRecord.setEnabled(true);
                    mButtonPause.setText("Pause");
                    mButtonPause.setVisibility(View.INVISIBLE);
                    break;
                case RECORDING:
                    mButtonRecord.setText("Stop");
                    mButtonRecord.setEnabled(true);
                    mButtonPause.setText("Pause");
                    mButtonPause.setVisibility(View.VISIBLE);
                    break;
                case STOPPING:
                    mButtonRecord.setText("Saving");
                    mButtonRecord.setEnabled(false);
                    mButtonPause.setText("Pause");
                    mButtonPause.setVisibility(View.INVISIBLE);
                    break;
                case PAUSED:
                    mButtonRecord.setText("Stop");
                    mButtonRecord.setEnabled(true);
                    mButtonPause.setText("Resume");
                    mButtonPause.setVisibility(View.VISIBLE);
                    break;
            }
        }

        Button getButtonRecord() {
            return mButtonRecord;
        }

        Button getButtonPause() {
            return mButtonPause;
        }

        TextView getTextStats() {
            return mTextStats;
        }
    }

    Preview getPreview() {
        return findUseCase(Preview.class);
    }

    ImageAnalysis getImageAnalysis() {
        return findUseCase(ImageAnalysis.class);
    }

    ImageCapture getImageCapture() {
        return findUseCase(ImageCapture.class);
    }

    @SuppressWarnings("unchecked")
    VideoCapture<Recorder> getVideoCapture() {
        return findUseCase(VideoCapture.class);
    }

    /**
     * Finds the use case by the given class.
     */
    @Nullable
    private <T extends UseCase> T findUseCase(Class<T> useCaseSubclass) {
        if (mUseCases != null) {
            for (UseCase useCase : mUseCases) {
                if (useCaseSubclass.isInstance(useCase)) {
                    return useCaseSubclass.cast(useCase);
                }
            }
        }
        return null;
    }

    @VisibleForTesting
    @Nullable
    CameraInfo getCameraInfo() {
        return mCamera != null ? mCamera.getCameraInfo() : null;
    }

    @VisibleForTesting
    @Nullable
    CameraControl getCameraControl() {
        return mCamera != null ? mCamera.getCameraControl() : null;
    }
}
