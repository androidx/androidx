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

import static androidx.camera.core.ImageCapture.ERROR_CAMERA_CLOSED;
import static androidx.camera.core.ImageCapture.ERROR_CAPTURE_FAILED;
import static androidx.camera.core.ImageCapture.ERROR_FILE_IO;
import static androidx.camera.core.ImageCapture.ERROR_INVALID_CAMERA;
import static androidx.camera.core.ImageCapture.ERROR_UNKNOWN;
import static androidx.camera.core.ImageCapture.FLASH_MODE_AUTO;
import static androidx.camera.core.ImageCapture.FLASH_MODE_OFF;
import static androidx.camera.core.ImageCapture.FLASH_MODE_ON;
import static androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.display.DisplayManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Range;
import android.util.Rational;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.internal.compat.quirk.CrashWhenTakingPhotoWithAutoFlashAEModeQuirk;
import androidx.camera.camera2.internal.compat.quirk.ImageCaptureFailWithAutoFlashQuirk;
import androidx.camera.camera2.internal.compat.quirk.ImageCaptureFlashNotFireQuirk;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.DisplayOrientedMeteringPointFactory;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ExperimentalLensFacing;
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
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.utils.AspectRatioUtil;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.OutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.RecordingStats;
import androidx.camera.video.VideoCapabilities;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.video.internal.compat.quirk.DeviceQuirks;
import androidx.camera.video.internal.compat.quirk.MediaStoreVideoCannotWrite;
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
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
    private static final String[] REQUIRED_PERMISSIONS;

    static {
        // From Android T, skips the permission check of WRITE_EXTERNAL_STORAGE since it won't be
        // granted any more.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }

    //Use this activity title when Camera Pipe configuration is used by core test app
    private static final String APP_TITLE_FOR_CAMERA_PIPE = "CameraPipe Core Test App";

    // Possible values for this intent key: "backward" or "forward".
    private static final String INTENT_EXTRA_CAMERA_DIRECTION = "camera_direction";
    // Possible values for this intent key: "switch_test_case", "preview_test_case" or
    // "default_test_case".
    private static final String INTENT_EXTRA_E2E_TEST_CASE = "e2e_test_case";
    // Launch the activity with the specified video quality.
    private static final String INTENT_EXTRA_VIDEO_QUALITY = "video_quality";
    public static final String INTENT_EXTRA_CAMERA_IMPLEMENTATION = "camera_implementation";
    public static final String INTENT_EXTRA_CAMERA_IMPLEMENTATION_NO_HISTORY =
            "camera_implementation_no_history";

    // Launch the activity with the specified target aspect ratio.
    public static final String INTENT_EXTRA_TARGET_ASPECT_RATIO = "target_aspect_ratio";

    // Launch the activity with the specified scale type. The default value is FILL_CENTER.
    public static final String INTENT_EXTRA_SCALE_TYPE = "scale_type";
    public static final int INTENT_EXTRA_FILL_CENTER = 1;
    public static final int INTENT_EXTRA_FIT_CENTER = 4;

    // Launch the activity with the specified camera id.
    @VisibleForTesting
    public static final String INTENT_EXTRA_CAMERA_ID = "camera_id";
    // Launch the activity with the specified use case combination.
    @VisibleForTesting
    public static final String INTENT_EXTRA_USE_CASE_COMBINATION = "use_case_combination";
    @VisibleForTesting
    // Sets this bit to bind Preview when using INTENT_EXTRA_USE_CASE_COMBINATION
    public static final int BIND_PREVIEW = 0x1;
    @VisibleForTesting
    // Sets this bit to bind ImageCapture when using INTENT_EXTRA_USE_CASE_COMBINATION
    public static final int BIND_IMAGE_CAPTURE = 0x2;
    @VisibleForTesting
    // Sets this bit to bind VideoCapture when using INTENT_EXTRA_USE_CASE_COMBINATION
    public static final int BIND_VIDEO_CAPTURE = 0x4;
    @VisibleForTesting
    // Sets this bit to bind ImageAnalysis when using INTENT_EXTRA_USE_CASE_COMBINATION
    public static final int BIND_IMAGE_ANALYSIS = 0x8;

    static final CameraSelector BACK_SELECTOR =
            new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
    static final CameraSelector FRONT_SELECTOR =
            new CameraSelector.Builder().requireLensFacing(
                    CameraSelector.LENS_FACING_FRONT).build();
    private CameraSelector mExternalCameraSelector = null;

    private final AtomicLong mImageAnalysisFrameCount = new AtomicLong(0);
    private final AtomicLong mPreviewFrameCount = new AtomicLong(0);
    // Automatically stops the video recording when this length value is set to be non-zero and
    // video length reaches the length in ms.
    private long mVideoCaptureAutoStopLength = 0;
    final MutableLiveData<String> mImageAnalysisResult = new MutableLiveData<>();
    private static final String BACKWARD = "BACKWARD";
    private static final String SWITCH_TEST_CASE = "switch_test_case";
    private static final String PREVIEW_TEST_CASE = "preview_test_case";
    private static final String DESCRIPTION_FLASH_MODE_NOT_SUPPORTED = "FLASH_MODE_NOT_SUPPORTED";
    private static final Quality QUALITY_AUTO = null;

    // The target aspect ratio of Preview and ImageCapture. It can be adjusted by setting
    // INTENT_EXTRA_TARGET_ASPECT_RATIO for the e2e testing.
    private int mTargetAspectRatio = AspectRatio.RATIO_DEFAULT;

    private Recording mActiveRecording;
    /** The camera to use */
    CameraSelector mCurrentCameraSelector = BACK_SELECTOR;
    ProcessCameraProvider mCameraProvider;
    private CameraXViewModel.CameraProviderResult mCameraProviderResult;

    // TODO: Move the analysis processing, capture processing to separate threads, so
    // there is smaller impact on the preview.
    View mViewFinder;
    private List<UseCase> mUseCases;
    ExecutorService mImageCaptureExecutorService;
    Camera mCamera;

    private CameraSelector mLaunchingCameraIdSelector = null;
    private int mLaunchingCameraLensFacing = CameraSelector.LENS_FACING_UNKNOWN;

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
    private ToggleButton mZslToggle;
    private TextView mZoomRatioLabel;
    private SeekBar mZoomSeekBar;
    private Button mZoomIn2XToggle;
    private Button mZoomResetToggle;
    private Toast mEvToast = null;

    private OpenGLRenderer mPreviewRenderer;
    private DisplayManager.DisplayListener mDisplayListener;
    private RecordUi mRecordUi;
    private Quality mVideoQuality;
    // TODO: Use SDR by now. A UI for selecting different dynamic ranges will be added when the
    //  related functionality is complete.
    private final DynamicRange mDynamicRange = DynamicRange.SDR;

    SessionMediaUriSet mSessionImagesUriSet = new SessionMediaUriSet();
    SessionMediaUriSet mSessionVideosUriSet = new SessionMediaUriSet();

    // Analyzer to be used with ImageAnalysis.
    private final ImageAnalysis.Analyzer mAnalyzer = new ImageAnalysis.Analyzer() {
        @Override
        public void analyze(@NonNull ImageProxy image) {
            // Since we set the callback handler to a main thread handler, we can call
            // setValue() here. If we weren't on the main thread, we would have to call
            // postValue() instead.
            mImageAnalysisResult.setValue(
                    Long.toString(image.getImageInfo().getTimestamp()));
            try {
                if (mImageAnalysisFrameCount.get() >= FRAMES_UNTIL_IMAGE_ANALYSIS_IS_READY
                        && !mAnalysisIdlingResource.isIdleNow()) {
                    mAnalysisIdlingResource.decrement();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Unexpected counter decrement");
            }
            image.close();
        }
    };

    private final FutureCallback<Integer> mEVFutureCallback = new FutureCallback<Integer>() {

        @Override
        public void onSuccess(@Nullable Integer result) {
            CameraInfo cameraInfo = getCameraInfo();
            if (cameraInfo != null) {
                ExposureState exposureState = cameraInfo.getExposureState();
                float ev = result * exposureState.getExposureCompensationStep().floatValue();
                Log.d(TAG, "success new EV: " + ev);
                showEVToast(String.format("EV: %.2f", ev));
            }
        }

        @Override
        public void onFailure(@NonNull Throwable t) {
            Log.d(TAG, "failed " + t);
            showEVToast("Fail to set EV");
        }
    };

    // Listener that handles all ToggleButton events.
    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener =
            (compoundButton, isChecked) -> tryBindUseCases();

    private final Consumer<Long> mFrameUpdateListener = timestamp -> {
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
    private static final int FRAMES_UNTIL_VIEW_IS_READY = 5;
    // Espresso testing variables
    private static final int FRAMES_UNTIL_IMAGE_ANALYSIS_IS_READY = 5;
    private final CountingIdlingResource mViewIdlingResource = new CountingIdlingResource("view");
    private final CountingIdlingResource mInitializationIdlingResource =
            new CountingIdlingResource("initialization");
    private final CountingIdlingResource mAnalysisIdlingResource =
            new CountingIdlingResource("analysis");
    private final CountingIdlingResource mImageSavedIdlingResource =
            new CountingIdlingResource("imagesaved");
    private final CountingIdlingResource mVideoSavedIdlingResource =
            new CountingIdlingResource("videosaved");

    /**
     * Saves the error message of the last take picture action if any error occurs. This will be
     * null which means no error occurs.
     */
    @Nullable
    private String mLastTakePictureErrorMessage = null;

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
     * Retrieve idling resource that waits for a video being recorded and saved.
     */
    @VisibleForTesting
    @NonNull
    public IdlingResource getVideoSavedIdlingResource() {
        return mVideoSavedIdlingResource;
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
        if (mViewIdlingResource.isIdleNow()) {
            mViewIdlingResource.increment();
        }
    }

    /**
     * Retrieve idling resource that waits for ImageAnalysis to receive images.
     */
    @VisibleForTesting
    public void resetAnalysisIdlingResource() {
        mImageAnalysisFrameCount.set(0);
        // Make the analysis idling resource non-idle, until required images achieved.
        if (mAnalysisIdlingResource.isIdleNow()) {
            mAnalysisIdlingResource.increment();
        }
    }

    /**
     * Retrieve idling resource that waits for VideoCapture to record a video.
     */
    @VisibleForTesting
    public void resetVideoSavedIdlingResource() {
        // Make the video saved idling resource non-idle, until required video length recorded.
        if (mVideoSavedIdlingResource.isIdleNow()) {
            mVideoSavedIdlingResource.increment();
        }
    }

    /**
     * Delete images that were taking during this session so far.
     * May leak images if pending captures not completed.
     */
    @VisibleForTesting
    public void deleteSessionImages() {
        mSessionImagesUriSet.deleteAllUris();
    }

    /**
     * Delete videos that were taking during this session so far.
     */
    @VisibleForTesting
    public void deleteSessionVideos() {
        mSessionVideosUriSet.deleteAllUris();
    }

    @SuppressLint("NullAnnotationGroup")
    @OptIn(markerClass = androidx.camera.core.ExperimentalZeroShutterLag.class)
    @ImageCapture.CaptureMode
    int getCaptureMode() {
        if (mZslToggle.isChecked()) {
            return ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG;
        } else {
            return mCaptureQualityToggle.isChecked() ? ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY :
                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY;
        }
    }

    private boolean isFlashAvailable() {
        CameraInfo cameraInfo = getCameraInfo();
        return mPhotoToggle.isChecked() && cameraInfo != null && cameraInfo.hasFlashUnit();
    }

    private boolean isFlashTestSupported(@ImageCapture.FlashMode int flashMode) {
        switch (flashMode) {
            case FLASH_MODE_OFF:
                return false;
            case FLASH_MODE_AUTO:
                CameraInfo cameraInfo = getCameraInfo();
                if (cameraInfo instanceof CameraInfoInternal) {
                    Quirks deviceQuirks = DeviceQuirks.getAll();
                    Quirks cameraQuirks = ((CameraInfoInternal) cameraInfo).getCameraQuirks();
                    if (deviceQuirks.contains(CrashWhenTakingPhotoWithAutoFlashAEModeQuirk.class)
                            || cameraQuirks.contains(ImageCaptureFailWithAutoFlashQuirk.class)
                            || cameraQuirks.contains(ImageCaptureFlashNotFireQuirk.class)) {

                        Toast.makeText(this, DESCRIPTION_FLASH_MODE_NOT_SUPPORTED,
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                break;
            default: // fall out
        }
        return true;
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

    @SuppressLint("MissingPermission")
    private void setUpRecordButton() {
        mRecordUi.getButtonRecord().setOnClickListener((view) -> {
            RecordUi.State state = mRecordUi.getState();
            switch (state) {
                case IDLE:
                    createDefaultVideoFolderIfNotExist();
                    final PendingRecording pendingRecording;
                    if (DeviceQuirks.get(MediaStoreVideoCannotWrite.class) != null) {
                        // Use FileOutputOption for devices in MediaStoreVideoCannotWrite Quirk.
                        pendingRecording = getVideoCapture().getOutput().prepareRecording(
                                this, getNewVideoFileOutputOptions());
                    } else {
                        // Use MediaStoreOutputOptions for public share media storage.
                        pendingRecording = getVideoCapture().getOutput().prepareRecording(
                                this, getNewVideoOutputMediaStoreOptions());
                    }

                    resetVideoSavedIdlingResource();

                    mActiveRecording = pendingRecording
                            .withAudioEnabled()
                            .start(ContextCompat.getMainExecutor(CameraXActivity.this),
                                    mVideoRecordEventListener);
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

        mRecordUi.getButtonQuality().setText(getQualityIconName(mVideoQuality));
        mRecordUi.getButtonQuality().setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(this, view);
            Menu menu = popup.getMenu();

            // Add Auto item
            final int groupId = Menu.NONE;
            final int autoOrder = 0;
            final int autoMenuId = qualityToItemId(QUALITY_AUTO);
            menu.add(groupId, autoMenuId, autoOrder, getQualityMenuItemName(QUALITY_AUTO));
            if (mVideoQuality == QUALITY_AUTO) {
                menu.findItem(autoMenuId).setChecked(true);
            }

            // Add device supported qualities
            VideoCapabilities videoCapabilities = Recorder.getVideoCapabilities(
                    mCamera.getCameraInfo());
            List<Quality> supportedQualities = videoCapabilities.getSupportedQualities(
                    mDynamicRange);
            // supportedQualities has been sorted by descending order.
            for (int i = 0; i < supportedQualities.size(); i++) {
                Quality quality = supportedQualities.get(i);
                int itemId = qualityToItemId(quality);
                menu.add(groupId, itemId, autoOrder + 1 + i, getQualityMenuItemName(quality));
                if (mVideoQuality == quality) {
                    menu.findItem(itemId).setChecked(true);
                }

            }
            // Make menu single checkable
            menu.setGroupCheckable(groupId, true, true);

            popup.setOnMenuItemClickListener(item -> {
                Quality quality = itemIdToQuality(item.getItemId());
                if (quality != mVideoQuality) {
                    mVideoQuality = quality;
                    mRecordUi.getButtonQuality().setText(getQualityIconName(mVideoQuality));
                    // Quality changed, rebind UseCases
                    tryBindUseCases();
                }
                return true;
            });

            popup.show();
        });
    }

    private final Consumer<VideoRecordEvent> mVideoRecordEventListener = event -> {
        updateRecordingStats(event.getRecordingStats());

        if (event instanceof VideoRecordEvent.Finalize) {
            VideoRecordEvent.Finalize finalize = (VideoRecordEvent.Finalize) event;

            switch (finalize.getError()) {
                case ERROR_NONE:
                case ERROR_FILE_SIZE_LIMIT_REACHED:
                case ERROR_DURATION_LIMIT_REACHED:
                case ERROR_INSUFFICIENT_STORAGE:
                case ERROR_SOURCE_INACTIVE:
                    Uri uri = finalize.getOutputResults().getOutputUri();
                    OutputOptions outputOptions = finalize.getOutputOptions();
                    String msg;
                    String videoFilePath;
                    if (outputOptions instanceof MediaStoreOutputOptions) {
                        msg = "Saved uri " + uri;
                        videoFilePath = getAbsolutePathFromUri(
                                getApplicationContext().getContentResolver(),
                                uri
                        );
                        updateVideoSavedSessionData(uri);
                    } else if (outputOptions instanceof FileOutputOptions) {
                        videoFilePath = ((FileOutputOptions) outputOptions).getFile().getPath();
                        MediaScannerConnection.scanFile(this,
                                new String[]{videoFilePath}, null,
                                (path, uri1) -> {
                                    Log.i(TAG, "Scanned " + path + " -> uri= " + uri1);
                                    updateVideoSavedSessionData(uri1);
                                });
                        msg = "Saved file " + videoFilePath;
                    } else {
                        throw new AssertionError("Unknown or unsupported OutputOptions type: "
                                + outputOptions.getClass().getSimpleName());
                    }
                    // The video file path is used in tracing e2e test log. Don't remove it.
                    Log.d(TAG, "Saved video file: " + videoFilePath);

                    if (finalize.getError() != ERROR_NONE) {
                        msg += " with code (" + finalize.getError() + ")";
                    }
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
        }
    };

    private void updateVideoSavedSessionData(@NonNull Uri uri) {
        if (mSessionVideosUriSet != null) {
            mSessionVideosUriSet.add(uri);
        }

        if (!mVideoSavedIdlingResource.isIdleNow()) {
            mVideoSavedIdlingResource.decrement();
        }
    }

    @NonNull
    private MediaStoreOutputOptions getNewVideoOutputMediaStoreOptions() {
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
                .build();
    }

    @NonNull
    private FileOutputOptions getNewVideoFileOutputOptions() {
        String videoFileName = "video_" + System.currentTimeMillis() + ".mp4";
        File videoFolder = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES);
        if (!videoFolder.exists() && !videoFolder.mkdirs()) {
            Log.e(TAG, "Failed to create directory: " + videoFolder);
        }
        return new FileOutputOptions.Builder(new File(videoFolder, videoFileName)).build();
    }

    private void updateRecordingStats(@NonNull RecordingStats stats) {
        double durationMs = TimeUnit.NANOSECONDS.toMillis(stats.getRecordedDurationNanos());
        // Show megabytes in International System of Units (SI)
        double sizeMb = stats.getNumBytesRecorded() / (1000d * 1000d);
        String msg = String.format("%.2f sec\n%.2f MB", durationMs / 1000d, sizeMb);
        mRecordUi.getTextStats().setText(msg);

        if (mVideoCaptureAutoStopLength > 0 && durationMs >= mVideoCaptureAutoStopLength
                && mRecordUi.getState() == RecordUi.State.RECORDING) {
            mRecordUi.getButtonRecord().callOnClick();
        }
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
                        Format formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS",
                                Locale.US);
                        String fileName = "CoreTestApp-" + formatter.format(
                                Calendar.getInstance().getTime()) + ".jpg";
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
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
                                        Log.e(TAG, "Failed to save image.", exception);

                                        mLastTakePictureErrorMessage =
                                                getImageCaptureErrorMessage(exception);
                                        if (!mImageSavedIdlingResource.isIdleNow()) {
                                            mImageSavedIdlingResource.decrement();
                                        }
                                    }
                                });
                    }
                });
    }

    private String getImageCaptureErrorMessage(@NonNull ImageCaptureException exception) {
        String errorCodeString;
        int errorCode = exception.getImageCaptureError();

        switch (errorCode) {
            case ERROR_UNKNOWN:
                errorCodeString = "ImageCaptureErrorCode: ERROR_UNKNOWN";
                break;
            case ERROR_FILE_IO:
                errorCodeString = "ImageCaptureErrorCode: ERROR_FILE_IO";
                break;
            case ERROR_CAPTURE_FAILED:
                errorCodeString = "ImageCaptureErrorCode: ERROR_CAPTURE_FAILED";
                break;
            case ERROR_CAMERA_CLOSED:
                errorCodeString = "ImageCaptureErrorCode: ERROR_CAMERA_CLOSED";
                break;
            case ERROR_INVALID_CAMERA:
                errorCodeString = "ImageCaptureErrorCode: ERROR_INVALID_CAMERA";
                break;
            default:
                errorCodeString = "ImageCaptureErrorCode: " + errorCode;
                break;
        }

        return errorCodeString + ", Message: " + exception.getMessage() + ", Cause: "
                + exception.getCause();
    }

    @SuppressWarnings("ObjectToString")
    private void setUpCameraDirectionButton() {
        mCameraDirectionButton.setOnClickListener(v -> {
            Log.d(TAG, "Change camera direction: " + mCurrentCameraSelector);
            CameraSelector switchedCameraSelector =
                    getSwitchedCameraSelector(mCurrentCameraSelector);
            try {
                if (isUseCasesCombinationSupported(switchedCameraSelector, mUseCases)) {
                    mCurrentCameraSelector = switchedCameraSelector;
                    tryBindUseCases();
                } else {
                    String msg = "Camera of the other lens facing can't support current use case "
                            + "combination.";
                    Log.d(TAG, msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, "Failed to swich Camera. Error:" + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @NonNull
    private CameraSelector getSwitchedCameraSelector(
            @NonNull CameraSelector currentCameraSelector) {
        CameraSelector switchedCameraSelector;
        // When the activity is launched with a specific camera id, camera switch function
        // will switch the cameras between the camera of the specified camera id and the
        // default camera of the opposite lens facing.
        if (mLaunchingCameraIdSelector != null) {
            if (currentCameraSelector != mLaunchingCameraIdSelector) {
                switchedCameraSelector = mLaunchingCameraIdSelector;
            } else {
                if (mLaunchingCameraLensFacing == CameraSelector.LENS_FACING_BACK) {
                    switchedCameraSelector = FRONT_SELECTOR;
                } else {
                    switchedCameraSelector = BACK_SELECTOR;
                }
            }
        } else {
            if (currentCameraSelector == BACK_SELECTOR) {
                switchedCameraSelector = FRONT_SELECTOR;
            } else if (currentCameraSelector == FRONT_SELECTOR) {
                if (mExternalCameraSelector != null) {
                    switchedCameraSelector = mExternalCameraSelector;
                } else {
                    switchedCameraSelector = BACK_SELECTOR;
                }
            } else {
                switchedCameraSelector = BACK_SELECTOR;
            }
        }

        return switchedCameraSelector;
    }

    private boolean isUseCasesCombinationSupported(@NonNull CameraSelector cameraSelector,
            @NonNull List<UseCase> useCases) {
        if (mCameraProvider == null) {
            throw new IllegalStateException("Need to obtain mCameraProvider first!");
        }

        Camera targetCamera = mCameraProvider.bindToLifecycle(this, cameraSelector);
        return targetCamera.isUseCasesCombinationSupported(useCases.toArray(new UseCase[0]));
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
                showEVToast(String.format("EV: %.2f", range.getUpper()
                        * exposureState.getExposureCompensationStep().floatValue()));
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
                showEVToast(String.format("EV: %.2f", range.getLower()
                        * exposureState.getExposureCompensationStep().floatValue()));
            }
        });
    }

    void showEVToast(String message) {
        if (mEvToast != null) {
            mEvToast.cancel();
        }
        mEvToast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        mEvToast.show();
    }

    private void updateAppUIForE2ETest(@NonNull String testCase) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        mCaptureQualityToggle.setVisibility(View.GONE);
        mZslToggle.setVisibility(View.GONE);
        mPlusEV.setVisibility(View.GONE);
        mDecEV.setVisibility(View.GONE);
        mZoomSeekBar.setVisibility(View.GONE);
        mZoomRatioLabel.setVisibility(View.GONE);
        mTextView.setVisibility(View.GONE);

        if (testCase.equals(PREVIEW_TEST_CASE) || testCase.equals(SWITCH_TEST_CASE)) {
            mTorchButton.setVisibility(View.GONE);
            mFlashButton.setVisibility(View.GONE);
            mTakePicture.setVisibility(View.GONE);
            mZoomIn2XToggle.setVisibility(View.GONE);
            mZoomResetToggle.setVisibility(View.GONE);
            mVideoToggle.setVisibility(View.GONE);
            mPhotoToggle.setVisibility(View.GONE);
            mPreviewToggle.setVisibility(View.GONE);
            mAnalysisToggle.setVisibility(View.GONE);
            mRecordUi.hideUi();
            if (!testCase.equals(SWITCH_TEST_CASE)) {
                mCameraDirectionButton.setVisibility(View.GONE);
            }
        }
    }

    private void updatePreviewRatioAndScaleTypeByIntent(ViewStub viewFinderStub) {
        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            mTargetAspectRatio = bundle.getInt(INTENT_EXTRA_TARGET_ASPECT_RATIO,
                    AspectRatio.RATIO_4_3);
            int scaleType = bundle.getInt(INTENT_EXTRA_SCALE_TYPE, INTENT_EXTRA_FILL_CENTER);
            if (scaleType == INTENT_EXTRA_FIT_CENTER) {
                // Scale the view according to the target aspect ratio, display size and device
                // orientation, so preview can be entirely contained within the view.
                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                Rational ratio = (mTargetAspectRatio == AspectRatio.RATIO_16_9)
                        ? AspectRatioUtil.ASPECT_RATIO_16_9 : AspectRatioUtil.ASPECT_RATIO_4_3;
                int orientation = getResources().getConfiguration().orientation;
                ViewGroup.LayoutParams lp = viewFinderStub.getLayoutParams();
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    lp.width = displayMetrics.widthPixels;
                    lp.height = (int) (displayMetrics.widthPixels / ratio.getDenominator()
                            * ratio.getNumerator());
                } else {
                    lp.height = displayMetrics.heightPixels;
                    lp.width = (int) (displayMetrics.heightPixels / ratio.getDenominator()
                            * ratio.getNumerator());
                }
                viewFinderStub.setLayoutParams(lp);
            }
        }
    }

    @SuppressLint("NullAnnotationGroup")
    @OptIn(markerClass = androidx.camera.core.ExperimentalZeroShutterLag.class)
    private void updateButtonsUi() {
        mRecordUi.setEnabled(mVideoToggle.isChecked());
        mTakePicture.setEnabled(mPhotoToggle.isChecked());
        mCaptureQualityToggle.setEnabled(mPhotoToggle.isChecked());
        mZslToggle.setVisibility(getCameraInfo() != null
                && getCameraInfo().isZslSupported() ? View.VISIBLE : View.GONE);
        mZslToggle.setEnabled(mPhotoToggle.isChecked());
        mCameraDirectionButton.setEnabled(getCameraInfo() != null);
        mTorchButton.setEnabled(isFlashAvailable());
        // Flash button
        mFlashButton.setEnabled(mPhotoToggle.isChecked() && isFlashAvailable());
        if (mPhotoToggle.isChecked()) {
            int flashMode = getImageCapture().getFlashMode();
            if (isFlashTestSupported(flashMode)) {
                // Reset content description if flash is ready for test.
                mFlashButton.setContentDescription("");
            } else {
                // Set content description for e2e testing.
                mFlashButton.setContentDescription(DESCRIPTION_FLASH_MODE_NOT_SUPPORTED);
            }
            switch (flashMode) {
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
        mZoomIn2XToggle.setEnabled(is2XZoomSupported());
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
        setUpZoomButton();
        mCaptureQualityToggle.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mZslToggle.setOnCheckedChangeListener(mOnCheckedChangeListener);
    }

    private void updateUseCaseCombinationByIntent(@NonNull Intent intent) {
        Bundle bundle = intent.getExtras();

        if (bundle == null) {
            return;
        }

        int useCaseCombination = bundle.getInt(INTENT_EXTRA_USE_CASE_COMBINATION, 0);

        if (useCaseCombination == 0) {
            return;
        }

        mPreviewToggle.setChecked((useCaseCombination & BIND_PREVIEW) != 0L);
        mPhotoToggle.setChecked((useCaseCombination & BIND_IMAGE_CAPTURE) != 0L);
        mVideoToggle.setChecked((useCaseCombination & BIND_VIDEO_CAPTURE) != 0L);
        mAnalysisToggle.setChecked((useCaseCombination & BIND_IMAGE_ANALYSIS) != 0L);
    }

    private void updateVideoQualityByIntent(@NonNull Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }

        Quality quality = itemIdToQuality(bundle.getInt(INTENT_EXTRA_VIDEO_QUALITY, 0));
        if (quality == QUALITY_AUTO || !mVideoToggle.isChecked()) {
            return;
        }

        if (mCameraProvider == null) {
            throw new IllegalStateException("Need to obtain mCameraProvider first!");
        }

        // Check and set specific quality.
        Camera targetCamera = mCameraProvider.bindToLifecycle(this, mCurrentCameraSelector);
        VideoCapabilities videoCapabilities = Recorder.getVideoCapabilities(
                targetCamera.getCameraInfo());
        List<Quality> supportedQualities = videoCapabilities.getSupportedQualities(mDynamicRange);
        if (supportedQualities.contains(quality)) {
            mVideoQuality = quality;
            mRecordUi.getButtonQuality().setText(getQualityIconName(mVideoQuality));
        }
    }

    @SuppressLint("NullAnnotationGroup")
    @OptIn(markerClass = ExperimentalLensFacing.class)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //if different Camera Provider (CameraPipe vs Camera2 was initialized in previous session,
        //then close this application.
        closeAppIfCameraProviderMismatch(this.getIntent());

        setContentView(R.layout.activity_camera_xmain);
        mImageCaptureExecutorService = Executors.newSingleThreadExecutor();
        OpenGLRenderer previewRenderer = mPreviewRenderer = new OpenGLRenderer();
        ViewStub viewFinderStub = findViewById(R.id.viewFinderStub);
        updatePreviewRatioAndScaleTypeByIntent(viewFinderStub);

        mViewFinder = OpenGLActivity.chooseViewFinder(getIntent().getExtras(), viewFinderStub,
                previewRenderer);
        mViewFinder.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)
                        -> tryBindUseCases());

        mVideoToggle = findViewById(R.id.VideoToggle);
        mPhotoToggle = findViewById(R.id.PhotoToggle);
        mAnalysisToggle = findViewById(R.id.AnalysisToggle);
        mPreviewToggle = findViewById(R.id.PreviewToggle);

        updateUseCaseCombinationByIntent(getIntent());

        mTakePicture = findViewById(R.id.Picture);
        mFlashButton = findViewById(R.id.flash_toggle);
        mCameraDirectionButton = findViewById(R.id.direction_toggle);
        mTorchButton = findViewById(R.id.torch_toggle);
        mCaptureQualityToggle = findViewById(R.id.capture_quality);
        mPlusEV = findViewById(R.id.plus_ev_toggle);
        mDecEV = findViewById(R.id.dec_ev_toggle);
        mZslToggle = findViewById(R.id.zsl_toggle);
        mZoomSeekBar = findViewById(R.id.seekBar);
        mZoomRatioLabel = findViewById(R.id.zoomRatio);
        mZoomIn2XToggle = findViewById(R.id.zoom_in_2x_toggle);
        mZoomResetToggle = findViewById(R.id.zoom_reset_toggle);

        mTextView = findViewById(R.id.textView);
        mRecordUi = new RecordUi(
                findViewById(R.id.Video),
                findViewById(R.id.video_pause),
                findViewById(R.id.video_stats),
                findViewById(R.id.video_quality)
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
            String launchingCameraId = bundle.getString(INTENT_EXTRA_CAMERA_ID, null);

            if (launchingCameraId != null) {
                mLaunchingCameraIdSelector = createCameraSelectorById(launchingCameraId);
                mCurrentCameraSelector = mLaunchingCameraIdSelector;
            } else {
                String newCameraDirection = bundle.getString(INTENT_EXTRA_CAMERA_DIRECTION);
                if (newCameraDirection != null) {
                    if (newCameraDirection.equals(BACKWARD)) {
                        mCurrentCameraSelector = BACK_SELECTOR;
                    } else {
                        mCurrentCameraSelector = FRONT_SELECTOR;
                    }
                }
            }

            String cameraImplementation = bundle.getString(INTENT_EXTRA_CAMERA_IMPLEMENTATION);
            boolean cameraImplementationNoHistory =
                    bundle.getBoolean(INTENT_EXTRA_CAMERA_IMPLEMENTATION_NO_HISTORY, false);
            if (cameraImplementationNoHistory) {
                Intent newIntent = new Intent(getIntent());
                newIntent.removeExtra(INTENT_EXTRA_CAMERA_IMPLEMENTATION);
                newIntent.removeExtra(INTENT_EXTRA_CAMERA_IMPLEMENTATION_NO_HISTORY);
                setIntent(newIntent);
            }

            if (cameraImplementation != null) {
                if (cameraImplementation.equalsIgnoreCase(
                        CameraXViewModel.CAMERA_PIPE_IMPLEMENTATION_OPTION)) {
                    setTitle(APP_TITLE_FOR_CAMERA_PIPE);
                }
                CameraXViewModel.configureCameraProvider(
                        cameraImplementation, cameraImplementationNoHistory);
            }

            // Update the app UI according to the e2e test case.
            String testCase = bundle.getString(INTENT_EXTRA_E2E_TEST_CASE);
            if (testCase != null) {
                updateAppUIForE2ETest(testCase);
            }
        }

        mInitializationIdlingResource.increment();
        CameraXViewModel viewModel = new ViewModelProvider(this).get(CameraXViewModel.class);
        viewModel.getCameraProvider().observe(this, cameraProviderResult -> {
            mCameraProviderResult = cameraProviderResult;
            mInitializationIdlingResource.decrement();
            if (cameraProviderResult.hasProvider()) {
                mCameraProvider = cameraProviderResult.getProvider();

                //initialize mExternalCameraSelector
                CameraSelector externalCameraSelectorLocal = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_EXTERNAL).build();
                List<CameraInfo> cameraInfos = externalCameraSelectorLocal.filter(
                        mCameraProvider.getAvailableCameraInfos());
                if (cameraInfos.size() > 0) {
                    mExternalCameraSelector = externalCameraSelectorLocal;
                }

                updateVideoQualityByIntent(getIntent());
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

    /**
     * Close current app if CameraProvider from intent of current activity doesn't match with
     * CameraProvider stored in the CameraXViewModel, because CameraProvider can't be changed
     * between Camera2 and Camera Pipe while app is running.
     */
    private void closeAppIfCameraProviderMismatch(Intent mIntent) {
        String cameraImplementation = null;
        boolean cameraImplementationNoHistory = false;
        Bundle bundle = mIntent.getExtras();
        if (bundle != null) {
            cameraImplementation = bundle.getString(INTENT_EXTRA_CAMERA_IMPLEMENTATION);
            cameraImplementationNoHistory =
                    bundle.getBoolean(INTENT_EXTRA_CAMERA_IMPLEMENTATION_NO_HISTORY, false);
        }

        if (!cameraImplementationNoHistory) {
            if (!CameraXViewModel.isCameraProviderUnInitializedOrSameAsParameter(
                    cameraImplementation)) {
                Toast.makeText(CameraXActivity.this, "Please relaunch "
                                + "the app to apply new CameraX configuration.",
                        Toast.LENGTH_LONG).show();
                finish();
                System.exit(0);
            }
        }
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

        // Remove ZoomState observer from old CameraInfo to prevent from receiving event from old
        // CameraInfo
        if (mCamera != null) {
            mCamera.getCameraInfo().getZoomState().removeObservers(this);
        }

        // Stop video recording if exists.
        if (mRecordUi.getState() == RecordUi.State.RECORDING
                || mRecordUi.getState() == RecordUi.State.PAUSED) {
            mActiveRecording.stop();
            mActiveRecording = null;
            mRecordUi.setState(RecordUi.State.STOPPING);
        }

        mCameraProvider.unbindAll();
        try {
            // Binds to lifecycle without use cases to make sure mCamera can be retrieved for
            // tests to do necessary checks.
            mCamera = mCameraProvider.bindToLifecycle(this, mCurrentCameraSelector);

            // Retrieves the lens facing info when the activity is launched with a specified
            // camera id.
            if (mCurrentCameraSelector == mLaunchingCameraIdSelector
                    && mLaunchingCameraLensFacing == CameraSelector.LENS_FACING_UNKNOWN) {
                mLaunchingCameraLensFacing = getLensFacing(mCamera.getCameraInfo());
            }
            List<UseCase> useCases = buildUseCases();
            mCamera = bindToLifecycleSafely(useCases);

            // Set the use cases after a successful binding.
            mUseCases = useCases;
        } catch (IllegalArgumentException ex) {
            String msg;
            if (mVideoQuality != QUALITY_AUTO) {
                msg = "Bind too many use cases or video quality is too large.";
            } else {
                msg = "Bind too many use cases.";
            }
            Log.e(TAG, "bindToLifecycle() failed. " + msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

            // Restore toggle buttons to the previous state if the bind failed.
            if (mUseCases != null) {
                mPreviewToggle.setChecked(getPreview() != null);
                mPhotoToggle.setChecked(getImageCapture() != null);
                mAnalysisToggle.setChecked(getImageAnalysis() != null);
                mVideoToggle.setChecked(getVideoCapture() != null);
            }
            // Reset video quality to avoid always fail by quality too large.
            mRecordUi.getButtonQuality().setText(getQualityIconName(mVideoQuality = QUALITY_AUTO));

            reduceUseCaseToFindSupportedCombination();

            if (!calledBySelf) {
                // Only call self if not already calling self to avoid an infinite loop.
                tryBindUseCases(true);
            }
        }
        updateButtonsUi();
    }

    /**
     * Checks whether currently checked use cases combination can be supported or not.
     */
    private boolean isCheckedUseCasesCombinationSupported() {
        return mCamera.isUseCasesCombinationSupported(buildUseCases().toArray(new UseCase[0]));
    }

    /**
     * Unchecks use case to find a supported use cases combination.
     *
     * <p>Only VideoCapture or ImageAnalysis will be tried to uncheck. If only Preview and
     * ImageCapture are remained, the combination should always be supported.
     */
    private void reduceUseCaseToFindSupportedCombination() {
        // Checks whether current combination can be supported
        if (isCheckedUseCasesCombinationSupported()) {
            return;
        }

        // Remove VideoCapture to check whether the new use cases combination can be supported.
        if (mVideoToggle.isChecked()) {
            mVideoToggle.setChecked(false);
            if (isCheckedUseCasesCombinationSupported()) {
                return;
            }
        }

        // Remove ImageAnalysis to check whether the new use cases combination can be supported.
        if (mAnalysisToggle.isChecked()) {
            mAnalysisToggle.setChecked(false);
            if (isCheckedUseCasesCombinationSupported()) {
                return;
            }
        }

        // Preview + ImageCapture should be always supported.
    }

    /**
     * Builds all use cases based on current settings and return as an array.
     */
    private List<UseCase> buildUseCases() {
        List<UseCase> useCases = new ArrayList<>();
        if (mPreviewToggle.isChecked()) {
            Preview preview = new Preview.Builder()
                    .setTargetName("Preview")
                    .setTargetAspectRatio(mTargetAspectRatio)
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
                    .setTargetAspectRatio(mTargetAspectRatio)
                    .setTargetName("ImageCapture")
                    .build();
            useCases.add(imageCapture);
        }

        if (mAnalysisToggle.isChecked()) {
            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setTargetName("ImageAnalysis")
                    .build();
            useCases.add(imageAnalysis);
            // Make the analysis idling resource non-idle, until the required frames received.
            resetAnalysisIdlingResource();
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), mAnalyzer);
        }

        if (mVideoToggle.isChecked()) {
            Recorder.Builder builder = new Recorder.Builder();
            if (mVideoQuality != QUALITY_AUTO) {
                builder.setQualitySelector(QualitySelector.from(mVideoQuality));
            }
            VideoCapture<Recorder> videoCapture = new VideoCapture.Builder<>(builder.build())
                    .setMirrorMode(MIRROR_MODE_ON_FRONT_ONLY)
                    .build();
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

    void createDefaultPictureFolderIfNotExist() {
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
                    float newZoom = cameraInfo.getZoomState().getValue().getZoomRatio()
                            * detector.getScaleFactor();
                    setZoomRatio(newZoom);
                    return true;
                }
            };

    GestureDetector.OnGestureListener onTapGestureListener =
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    if (mCamera == null) {
                        return false;
                    }
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
                                public void onFailure(@NonNull Throwable t) {
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
                    public void onFailure(@NonNull Throwable t) {
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

    private boolean is2XZoomSupported() {
        CameraInfo cameraInfo = getCameraInfo();
        return cameraInfo != null
                && cameraInfo.getZoomState().getValue().getMaxZoomRatio() >= 2.0f;
    }

    private void setUpZoomButton() {
        mZoomIn2XToggle.setOnClickListener(v -> setZoomRatio(2.0f));

        mZoomResetToggle.setOnClickListener(v -> setZoomRatio(1.0f));
    }

    void setZoomRatio(float newZoom) {
        if (mCamera == null) {
            return;
        }

        CameraInfo cameraInfo = mCamera.getCameraInfo();
        CameraControl cameraControl = mCamera.getCameraControl();
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
            public void onFailure(@NonNull Throwable t) {
                Log.d(TAG, "setZoomRatio failed, " + t);
            }
        }, ContextCompat.getMainExecutor(CameraXActivity.this));
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
                    contentUri, e));
            return "";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private class SessionMediaUriSet {
        private final Set<Uri> mSessionMediaUris;

        SessionMediaUriSet() {
            mSessionMediaUris = Collections.synchronizedSet(new HashSet<>());
        }

        public void add(@NonNull Uri uri) {
            mSessionMediaUris.add(uri);
        }

        public void deleteAllUris() {
            synchronized (mSessionMediaUris) {
                Iterator<Uri> it = mSessionMediaUris.iterator();
                while (it.hasNext()) {
                    try {
                        getContentResolver().delete(it.next(), null, null);
                    } catch (SecurityException e) {
                        Log.w(TAG, "Cannot delete the content.", e);
                    }
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
        private final Button mButtonQuality;
        private boolean mEnabled = false;
        private State mState = State.IDLE;

        RecordUi(@NonNull Button buttonRecord, @NonNull Button buttonPause,
                @NonNull TextView textStats, @NonNull Button buttonQuality) {
            mButtonRecord = buttonRecord;
            mButtonPause = buttonPause;
            mTextStats = textStats;
            mButtonQuality = buttonQuality;
        }

        void setEnabled(boolean enabled) {
            mEnabled = enabled;
            if (enabled) {
                mTextStats.setText("");
                mTextStats.setVisibility(View.VISIBLE);
                mButtonQuality.setVisibility(View.VISIBLE);
                updateUi();
            } else {
                mButtonRecord.setText("Record");
                mButtonRecord.setEnabled(false);
                mButtonPause.setVisibility(View.INVISIBLE);
                mButtonQuality.setVisibility(View.INVISIBLE);
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

        void hideUi() {
            mButtonRecord.setVisibility(View.GONE);
            mButtonPause.setVisibility(View.GONE);
            mTextStats.setVisibility(View.GONE);
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

        @NonNull
        Button getButtonQuality() {
            return mButtonQuality;
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

    /**
     * Returns the error message of the last take picture action if any error occurs. Returns
     * null if no error occurs.
     */
    @VisibleForTesting
    @Nullable
    String getLastTakePictureErrorMessage() {
        return mLastTakePictureErrorMessage;
    }

    @VisibleForTesting
    void cleanTakePictureErrorMessage() {
        mLastTakePictureErrorMessage = null;
    }

    @SuppressWarnings("unchecked")
    VideoCapture<Recorder> getVideoCapture() {
        return findUseCase(VideoCapture.class);
    }

    @VisibleForTesting
    void setVideoCaptureAutoStopLength(long autoStopLengthInMs) {
        mVideoCaptureAutoStopLength = autoStopLengthInMs;
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
    public Camera getCamera() {
        return mCamera;
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

    @NonNull
    private static String getQualityIconName(@Nullable Quality quality) {
        if (quality == QUALITY_AUTO) {
            return "Auto";
        } else if (quality == Quality.UHD) {
            return "UHD";
        } else if (quality == Quality.FHD) {
            return "FHD";
        } else if (quality == Quality.HD) {
            return "HD";
        } else if (quality == Quality.SD) {
            return "SD";
        }
        return "?";
    }

    @NonNull
    private static String getQualityMenuItemName(@Nullable Quality quality) {
        if (quality == QUALITY_AUTO) {
            return "Auto";
        } else if (quality == Quality.UHD) {
            return "UHD (2160P)";
        } else if (quality == Quality.FHD) {
            return "FHD (1080P)";
        } else if (quality == Quality.HD) {
            return "HD (720P)";
        } else if (quality == Quality.SD) {
            return "SD (480P)";
        }
        return "Unknown quality";
    }

    private static int qualityToItemId(@Nullable Quality quality) {
        if (quality == QUALITY_AUTO) {
            return 0;
        } else if (quality == Quality.UHD) {
            return 1;
        } else if (quality == Quality.FHD) {
            return 2;
        } else if (quality == Quality.HD) {
            return 3;
        } else if (quality == Quality.SD) {
            return 4;
        } else {
            throw new IllegalArgumentException("Undefined quality: " + quality);
        }
    }

    @Nullable
    private static Quality itemIdToQuality(int itemId) {
        switch (itemId) {
            case 0:
                return QUALITY_AUTO;
            case 1:
                return Quality.UHD;
            case 2:
                return Quality.FHD;
            case 3:
                return Quality.HD;
            case 4:
                return Quality.SD;
            default:
                throw new IllegalArgumentException("Undefined item id: " + itemId);
        }
    }

    private static CameraSelector createCameraSelectorById(@Nullable String cameraId) {
        return new CameraSelector.Builder().addCameraFilter(cameraInfos -> {
            for (CameraInfo cameraInfo : cameraInfos) {
                if (Objects.equals(cameraId, getCameraId(cameraInfo))) {
                    return Collections.singletonList(cameraInfo);
                }
            }

            throw new IllegalArgumentException("No camera can be find for id: " + cameraId);
        }).build();
    }

    private static int getLensFacing(@NonNull CameraInfo cameraInfo) {
        try {
            return getCamera2LensFacing(cameraInfo);
        } catch (IllegalArgumentException e) {
            return getCamera2PipeLensFacing(cameraInfo);
        }
    }

    @SuppressLint("NullAnnotationGroup")
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private static int getCamera2LensFacing(@NonNull CameraInfo cameraInfo) {
        Integer lensFacing = Camera2CameraInfo.from(cameraInfo).getCameraCharacteristic(
                    CameraCharacteristics.LENS_FACING);

        return lensFacing == null ? CameraCharacteristics.LENS_FACING_BACK : lensFacing;
    }

    @SuppressLint("NullAnnotationGroup")
    @OptIn(markerClass =
            androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop.class)
    private static int getCamera2PipeLensFacing(@NonNull CameraInfo cameraInfo) {
        Integer lensFacing =
                androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo.from(
                        cameraInfo).getCameraCharacteristic(CameraCharacteristics.LENS_FACING);

        return lensFacing == null ? CameraCharacteristics.LENS_FACING_BACK : lensFacing;
    }

    @NonNull
    private static String getCameraId(@NonNull CameraInfo cameraInfo) {
        try {
            return getCamera2CameraId(cameraInfo);
        } catch (IllegalArgumentException e) {
            return getCameraPipeCameraId(cameraInfo);
        }
    }

    @SuppressLint("NullAnnotationGroup")
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @NonNull
    private static String getCamera2CameraId(@NonNull CameraInfo cameraInfo) {
        return Camera2CameraInfo.from(cameraInfo).getCameraId();
    }

    @SuppressLint("NullAnnotationGroup")
    @OptIn(markerClass =
            androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop.class)
    @NonNull
    private static String getCameraPipeCameraId(@NonNull CameraInfo cameraInfo) {
        return androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo.from(
                cameraInfo).getCameraId();
    }
}
