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

import static android.os.Environment.getExternalStoragePublicDirectory;

import static androidx.camera.core.ImageCapture.ERROR_CAMERA_CLOSED;
import static androidx.camera.core.ImageCapture.ERROR_CAPTURE_FAILED;
import static androidx.camera.core.ImageCapture.ERROR_FILE_IO;
import static androidx.camera.core.ImageCapture.ERROR_INVALID_CAMERA;
import static androidx.camera.core.ImageCapture.ERROR_UNKNOWN;
import static androidx.camera.core.ImageCapture.FLASH_MODE_AUTO;
import static androidx.camera.core.ImageCapture.FLASH_MODE_OFF;
import static androidx.camera.core.ImageCapture.FLASH_MODE_ON;
import static androidx.camera.core.ImageCapture.FLASH_MODE_SCREEN;
import static androidx.camera.core.ImageCapture.OUTPUT_FORMAT_JPEG;
import static androidx.camera.core.ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR;
import static androidx.camera.core.ImageCapture.getImageCaptureCapabilities;
import static androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY;
import static androidx.camera.integration.core.CameraXViewModel.getConfiguredCameraXCameraImplementation;
import static androidx.camera.testing.impl.FileUtil.canDeviceWriteToMediaStore;
import static androidx.camera.testing.impl.FileUtil.createFolder;
import static androidx.camera.testing.impl.FileUtil.createParentFolder;
import static androidx.camera.testing.impl.FileUtil.generateVideoFileOutputOptions;
import static androidx.camera.testing.impl.FileUtil.generateVideoMediaStoreOptions;
import static androidx.camera.testing.impl.FileUtil.getAbsolutePathFromUri;
import static androidx.camera.testing.impl.FileUtil.writeTextToExternalFile;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE;

import static java.util.Objects.requireNonNull;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
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
import androidx.annotation.RequiresApi;
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
import androidx.camera.core.ExperimentalImageCaptureOutputFormat;
import androidx.camera.core.ExperimentalLensFacing;
import androidx.camera.core.ExposureState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureCapabilities;
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
import androidx.camera.video.ExperimentalPersistentRecording;
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
import androidx.camera.view.ScreenFlashView;
import androidx.camera.view.impl.ZoomGestureDetector;
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
@SuppressLint("NullAnnotationGroup")
@OptIn(markerClass = ExperimentalImageCaptureOutputFormat.class)
public class CameraXActivity extends AppCompatActivity {
    private static final String TAG = "CameraXActivity";
    private static final String[] REQUIRED_PERMISSIONS;
    private static final List<DynamicRangeUiData> DYNAMIC_RANGE_UI_DATA = new ArrayList<>();

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

        DYNAMIC_RANGE_UI_DATA.add(new DynamicRangeUiData(
                DynamicRange.SDR,
                "SDR",
                R.string.toggle_video_dyn_rng_sdr));
        DYNAMIC_RANGE_UI_DATA.add(new DynamicRangeUiData(
                DynamicRange.HDR_UNSPECIFIED_10_BIT,
                "HDR (Auto, 10-bit)",
                R.string.toggle_video_dyn_rng_hdr_auto));
        DYNAMIC_RANGE_UI_DATA.add(new DynamicRangeUiData(
                DynamicRange.HLG_10_BIT,
                "HDR (HLG, 10-bit)",
                R.string.toggle_video_dyn_rng_hlg));
        DYNAMIC_RANGE_UI_DATA.add(new DynamicRangeUiData(
                DynamicRange.HDR10_10_BIT,
                "HDR (HDR10, 10-bit)",
                R.string.toggle_video_dyn_rng_hdr_ten));
        DYNAMIC_RANGE_UI_DATA.add(new DynamicRangeUiData(
                DynamicRange.HDR10_PLUS_10_BIT,
                "HDR (HDR10+, 10-bit)",
                R.string.toggle_video_dyn_rng_hdr_ten_plus));
        DYNAMIC_RANGE_UI_DATA.add(new DynamicRangeUiData(
                DynamicRange.DOLBY_VISION_8_BIT,
                "HDR (Dolby Vision, 8-bit)",
                R.string.toggle_video_dyn_rng_hdr_dolby_vision_8));
        DYNAMIC_RANGE_UI_DATA.add(new DynamicRangeUiData(
                DynamicRange.DOLBY_VISION_10_BIT,
                "HDR (Dolby Vision, 10-bit)",
                R.string.toggle_video_dyn_rng_hdr_dolby_vision_10));
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
    // Launch the activity with the view finder position log into a text file.
    private static final String INTENT_EXTRA_LOG_VIEWFINDER_POSITION = "log_view_finder_position";
    // Launch the activity with the specified video mirror mode.
    private static final String INTENT_EXTRA_VIDEO_MIRROR_MODE = "video_mirror_mode";
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

    /** Represents screen flash is set and fully supported (non-legacy device) */
    private static final String DESCRIPTION_FLASH_MODE_SCREEN = "FLASH_MODE_SCREEN";
    /** Represents screen flash is set, but not supported due to being legacy device */
    private static final String DESCRIPTION_SCREEN_FLASH_NOT_SUPPORTED_LEGACY =
            "SCREEN_FLASH_NOT_SUPPORTED_LEGACY";
    /** Represents the lack of physical flash unit for current camera */
    private static final String DESCRIPTION_FLASH_UNIT_NOT_AVAILABLE = "FLASH_UNIT_NOT_AVAILABLE";
    /** Represents current (if any) flash mode not being supported */
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
    ExecutorService mFileWriterExecutorService;
    ExecutorService mImageCaptureExecutorService;
    private VideoCapture<Recorder> mVideoCapture;
    private Recorder mRecorder;
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
    private ScreenFlashView mScreenFlashView;
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
    private Button mButtonImageOutputFormat;
    private Toast mEvToast = null;
    private Toast mPSToast = null;
    private ToggleButton mPreviewStabilizationToggle;

    private OpenGLRenderer mPreviewRenderer;
    private DisplayManager.DisplayListener mDisplayListener;
    private RecordUi mRecordUi;
    private DynamicRangeUi mDynamicRangeUi;
    private Quality mVideoQuality;
    private DynamicRange mDynamicRange = DynamicRange.SDR;
    private @ImageCapture.OutputFormat int mImageOutputFormat = OUTPUT_FORMAT_JPEG;
    private Set<DynamicRange> mDisplaySupportedHighDynamicRanges = Collections.emptySet();
    private final Set<DynamicRange> mSelectableDynamicRanges = new HashSet<>();
    private int mVideoMirrorMode = MIRROR_MODE_ON_FRONT_ONLY;
    private boolean mIsPreviewStabilizationOn = false;

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
            if (result == null) {
                return;
            }
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
        // Make the view idling resource non-idle, until required frame count achieved.
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

    /** Returns whether any kind of flash (physical/screen) is available */
    private boolean isFlashAvailable() {
        return isFlashUnitAvailable() || isScreenFlashAvailable();
    }

    /** Returns whether physical flash unit is available */
    private boolean isFlashUnitAvailable() {
        CameraInfo cameraInfo = getCameraInfo();
        return mPhotoToggle.isChecked() && cameraInfo != null && cameraInfo.hasFlashUnit();
    }

    /** Returns whether screen flash is available */
    private boolean isScreenFlashAvailable() {
        return mPhotoToggle.isChecked() && isFrontCamera();
    }

    @SuppressLint("RestrictedApiAndroidX")
    private boolean isFlashTestSupported(@ImageCapture.FlashMode int flashMode) {
        switch (flashMode) {
            case FLASH_MODE_OFF:
                return false;
            case FLASH_MODE_AUTO:
                CameraInfo cameraInfo = getCameraInfo();
                if (cameraInfo instanceof CameraInfoInternal) {
                    Quirks deviceQuirks =
                            androidx.camera.camera2.internal.compat.quirk.DeviceQuirks.getAll();
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

            if (flashMode == FLASH_MODE_OFF) {
                if (isFlashUnitAvailable()) {
                    getImageCapture().setFlashMode(FLASH_MODE_AUTO);
                } else if (isScreenFlashAvailable()) {
                    setUpScreenFlash();
                } else {
                    Log.e(TAG,
                            "Flash button clicked despite lack of both physical and screen flash");
                }
            } else if (flashMode == FLASH_MODE_AUTO) {
                getImageCapture().setFlashMode(FLASH_MODE_ON);
            } else if (flashMode == FLASH_MODE_ON) {
                if (!isScreenFlashAvailable()) {
                    getImageCapture().setFlashMode(FLASH_MODE_OFF);
                } else {
                    setUpScreenFlash();
                }
            } else if (flashMode == FLASH_MODE_SCREEN) {
                getImageCapture().setFlashMode(FLASH_MODE_OFF);
            }
            updateButtonsUi();
        });
    }

    private void setUpScreenFlash() {
        if (!isFrontCamera()) {
            return;
        }

        mScreenFlashView.setScreenFlashWindow(getWindow());
        getImageCapture().setScreenFlash(
                mScreenFlashView.getScreenFlash());
        getImageCapture().setFlashMode(FLASH_MODE_SCREEN);
    }

    @SuppressLint({"MissingPermission", "NullAnnotationGroup"})
    @OptIn(markerClass = ExperimentalPersistentRecording.class)
    private void setUpRecordButton() {
        mRecordUi.getButtonRecord().setOnClickListener((view) -> {
            RecordUi.State state = mRecordUi.getState();
            switch (state) {
                case IDLE:
                    createDefaultVideoFolderIfNotExist();
                    final PendingRecording pendingRecording;
                    String fileName = "video_" + System.currentTimeMillis();
                    String extension = "mp4";
                    if (canDeviceWriteToMediaStore()) {
                        // Use MediaStoreOutputOptions for public share media storage.
                        pendingRecording = getVideoCapture().getOutput().prepareRecording(
                                this,
                                generateVideoMediaStoreOptions(getContentResolver(), fileName));
                    } else {
                        // Use FileOutputOption for devices in MediaStoreVideoCannotWrite Quirk.
                        pendingRecording = getVideoCapture().getOutput().prepareRecording(
                                this, generateVideoFileOutputOptions(fileName, extension));
                    }

                    resetVideoSavedIdlingResource();

                    if (isPersistentRecordingEnabled()) {
                        pendingRecording.asPersistentRecording();
                    }
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

        // Final reference to this record UI
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

    private void setUpDynamicRangeButton() {
        mDynamicRangeUi.setDisplayedDynamicRange(mDynamicRange);
        mDynamicRangeUi.getButton().setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(this, view);
            Menu menu = popup.getMenu();

            final int groupId = Menu.NONE;
            for (DynamicRange dynamicRange : mSelectableDynamicRanges) {
                int itemId = dynamicRangeToItemId(dynamicRange);
                menu.add(groupId, itemId, itemId, getDynamicRangeMenuItemName(dynamicRange));
                if (Objects.equals(dynamicRange, mDynamicRange)) {
                    // Apply the checked item for the selected dynamic range to the menu.
                    menu.findItem(itemId).setChecked(true);
                }
            }

            // Make menu single checkable
            menu.setGroupCheckable(groupId, true, true);

            popup.setOnMenuItemClickListener(item -> {
                DynamicRange dynamicRange = itemIdToDynamicRange(item.getItemId());
                if (!Objects.equals(dynamicRange, mDynamicRange)) {
                    setSelectedDynamicRange(dynamicRange);
                    // Dynamic range changed, rebind UseCases
                    tryBindUseCases();
                }
                return true;
            });

            popup.show();
        });
    }

    private void setUpImageOutputFormatButton() {
        mButtonImageOutputFormat.setText(getImageOutputFormatIconName(mImageOutputFormat));
        mButtonImageOutputFormat.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(this, view);
            Menu menu = popup.getMenu();
            final int groupId = Menu.NONE;

            // Add device supported output formats.
            ImageCaptureCapabilities capabilities = getImageCaptureCapabilities(
                    mCamera.getCameraInfo());
            Set<Integer> supportedOutputFormats = capabilities.getSupportedOutputFormats();
            for (int supportedOutputFormat : supportedOutputFormats) {
                // Add output format item to menu.
                final int menuItemId = imageOutputFormatToItemId(supportedOutputFormat);
                final int order = menu.size();
                final String menuItemName = getImageOutputFormatMenuItemName(supportedOutputFormat);

                menu.add(groupId, menuItemId, order, menuItemName);
                if (mImageOutputFormat == supportedOutputFormat) {
                    menu.findItem(menuItemId).setChecked(true);
                }
            }

            // Make menu single checkable.
            menu.setGroupCheckable(groupId, true, true);

            // Set item click listener.
            popup.setOnMenuItemClickListener(item -> {
                int outputFormat = itemIdToImageOutputFormat(item.getItemId());
                if (outputFormat != mImageOutputFormat) {
                    mImageOutputFormat = outputFormat;
                    final String newIconName = getImageOutputFormatIconName(mImageOutputFormat);
                    mButtonImageOutputFormat.setText(newIconName);

                    // Output format changed, rebind UseCases.
                    tryBindUseCases();
                }
                return true;
            });

            popup.show();
        });
    }

    private void setSelectedDynamicRange(@NonNull DynamicRange dynamicRange) {
        mDynamicRange = dynamicRange;
        if (Build.VERSION.SDK_INT >= 26) {
            updateWindowColorMode();
        }
        mDynamicRangeUi.setDisplayedDynamicRange(mDynamicRange);
    }

    @RequiresApi(26)
    private void updateWindowColorMode() {
        int colorMode = ActivityInfo.COLOR_MODE_DEFAULT;
        if (!Objects.equals(mDynamicRange, DynamicRange.SDR)) {
            colorMode = ActivityInfo.COLOR_MODE_HDR;
        }
        Api26Impl.setColorMode(requireNonNull(getWindow()), colorMode);
    }

    private static boolean hasTenBitDynamicRange(@NonNull Set<DynamicRange> dynamicRanges) {
        for (DynamicRange dynamicRange : dynamicRanges) {
            if (dynamicRange.getBitDepth() == DynamicRange.BIT_DEPTH_10_BIT) {
                return true;
            }
        }
        return false;
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
                                                    requireNonNull(
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
                Toast.makeText(this, "Failed to switch Camera. Error:" + e.getMessage(),
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
            requireNonNull(getCameraInfo());
            requireNonNull(getCameraControl());
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
            requireNonNull(getCameraInfo());
            requireNonNull(getCameraControl());

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
            requireNonNull(getCameraInfo());
            requireNonNull(getCameraControl());

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

    void showPreviewStabilizationToast(String message) {
        if (mPSToast != null) {
            mPSToast.cancel();
        }
        mPSToast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        mPSToast.show();
    }

    private void updateAppUIForE2ETest() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            return;
        }

        String testCase = bundle.getString(INTENT_EXTRA_E2E_TEST_CASE);
        if (testCase == null) {
            return;
        }

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
            mDynamicRangeUi.getButton().setVisibility(View.GONE);
            mButtonImageOutputFormat.setVisibility(View.GONE);
            mRecordUi.hideUi();
            mPreviewStabilizationToggle.setVisibility(View.GONE);
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
                    lp.height = displayMetrics.widthPixels / ratio.getDenominator()
                            * ratio.getNumerator();
                } else {
                    lp.height = displayMetrics.heightPixels;
                    lp.width = displayMetrics.heightPixels / ratio.getDenominator()
                            * ratio.getNumerator();
                }
                viewFinderStub.setLayoutParams(lp);
            }
        }
    }

    private void updateDynamicRangeUiState() {
        // Only show dynamic range if video or preview are enabled
        boolean visible = (mVideoToggle.isChecked() || mPreviewToggle.isChecked());
        // Dynamic range is configurable if it's visible, there's more than 1 choice, and there
        // isn't a recording in progress
        boolean configurable = visible
                && mSelectableDynamicRanges.size() > 1
                && mRecordUi.getState() != RecordUi.State.RECORDING;

        if (configurable) {
            mDynamicRangeUi.setState(DynamicRangeUi.State.CONFIGURABLE);
        } else if (visible) {
            mDynamicRangeUi.setState(DynamicRangeUi.State.VISIBLE);
        } else {
            mDynamicRangeUi.setState(DynamicRangeUi.State.HIDDEN);
        }
    }

    private void updateImageOutputFormatUiState() {
        int visible = mPhotoToggle.isChecked() ? View.VISIBLE : View.GONE;
        mButtonImageOutputFormat.setVisibility(visible);
    }

    @SuppressLint({"NullAnnotationGroup", "RestrictedApiAndroidX"})
    @OptIn(markerClass = androidx.camera.core.ExperimentalZeroShutterLag.class)
    private void updateButtonsUi() {
        mRecordUi.setEnabled(mVideoToggle.isChecked());
        updateDynamicRangeUiState();
        updateImageOutputFormatUiState();

        mTakePicture.setEnabled(mPhotoToggle.isChecked());
        mCaptureQualityToggle.setEnabled(mPhotoToggle.isChecked());
        mZslToggle.setVisibility(getCameraInfo() != null
                && getCameraInfo().isZslSupported() ? View.VISIBLE : View.GONE);
        mZslToggle.setEnabled(mPhotoToggle.isChecked());
        mCameraDirectionButton.setEnabled(getCameraInfo() != null);
        mPreviewStabilizationToggle.setEnabled(mCamera != null
                && Preview.getPreviewCapabilities(getCameraInfo()).isStabilizationSupported());
        mTorchButton.setEnabled(isFlashUnitAvailable());
        // Flash button
        mFlashButton.setEnabled(isFlashAvailable());
        if (mPhotoToggle.isChecked()) {
            int flashMode = getImageCapture().getFlashMode();
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
                case FLASH_MODE_SCREEN:
                    mFlashButton.setImageResource(R.drawable.ic_flash_screen);
                    break;
            }
        }
        setFlashButtonContentDescription();

        mPlusEV.setEnabled(isExposureCompensationSupported());
        mDecEV.setEnabled(isExposureCompensationSupported());
        mZoomIn2XToggle.setEnabled(is2XZoomSupported());

        // this function may make some view visible again, so need to update for E2E tests again
        updateAppUIForE2ETest();
    }

    // Set or reset content description for e2e testing.
    private void setFlashButtonContentDescription() {
        // This is set even if button is not enabled, to better represent why it is not enabled.
        if (!isFlashUnitAvailable()) {
            mFlashButton.setContentDescription(DESCRIPTION_FLASH_UNIT_NOT_AVAILABLE);
        }

        if (!mPhotoToggle.isChecked()) {
            return;
        }

        int flashMode = getImageCapture().getFlashMode();

        // Button may be enabled even when flash unit is not available, due to screen flash.
        if (isFlashUnitAvailable()) {
            // Even if flash unit is available, some flash modes still may not be suitable for tests
            if (isFlashTestSupported(flashMode)) {
                // Reset content description if flash is ready for test.
                // TODO: Set content description specific to flash mode, may need to check the
                //  E2E tests first if that will be okay.
                mFlashButton.setContentDescription("");
            } else {
                mFlashButton.setContentDescription(DESCRIPTION_FLASH_MODE_NOT_SUPPORTED);
            }
        }

        // Screen flash does not depend on flash unit or the quirks in isFlashTestSupported, so
        // will override the previously set descriptions without any concern to those.
        if (flashMode == FLASH_MODE_SCREEN) {
            if (isLegacyDevice(requireNonNull(getCameraInfo()))) {
                mFlashButton.setContentDescription(
                        DESCRIPTION_SCREEN_FLASH_NOT_SUPPORTED_LEGACY);
            } else {
                mFlashButton.setContentDescription(DESCRIPTION_FLASH_MODE_SCREEN);
            }
        }

        Log.d(TAG, "Flash Button content description = " + mFlashButton.getContentDescription());
    }

    private void setUpButtonEvents() {
        mVideoToggle.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mPhotoToggle.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mAnalysisToggle.setOnCheckedChangeListener(mOnCheckedChangeListener);
        mPreviewToggle.setOnCheckedChangeListener(mOnCheckedChangeListener);

        setUpRecordButton();
        setUpDynamicRangeButton();
        setUpImageOutputFormatButton();
        setUpFlashButton();
        setUpTakePictureButton();
        setUpCameraDirectionButton();
        setUpTorchButton();
        setUpEVButton();
        setUpZoomButton();
        setUpPreviewStabilizationButton();
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

    private void updateVideoMirrorModeByIntent(@NonNull Intent intent) {
        int mirrorMode = intent.getIntExtra(INTENT_EXTRA_VIDEO_MIRROR_MODE, -1);
        if (mirrorMode != -1) {
            Log.d(TAG, "updateVideoMirrorModeByIntent: mirrorMode = " + mirrorMode);
            mVideoMirrorMode = mirrorMode;
        }
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
        mFileWriterExecutorService = Executors.newSingleThreadExecutor();
        mImageCaptureExecutorService = Executors.newSingleThreadExecutor();
        mDisplaySupportedHighDynamicRanges = Collections.emptySet();
        if (Build.VERSION.SDK_INT >= 30) {
            Display display = OpenGLActivity.Api30Impl.getDisplay(this);
            mDisplaySupportedHighDynamicRanges =
                    OpenGLActivity.getHighDynamicRangesSupportedByDisplay(display);
        }
        OpenGLRenderer previewRenderer = mPreviewRenderer =
                new OpenGLRenderer(mDisplaySupportedHighDynamicRanges);
        ViewStub viewFinderStub = findViewById(R.id.viewFinderStub);
        updatePreviewRatioAndScaleTypeByIntent(viewFinderStub);
        updateVideoMirrorModeByIntent(getIntent());

        Bundle bundle = this.getIntent().getExtras();

        mViewFinder = OpenGLActivity.chooseViewFinder(getIntent().getExtras(), viewFinderStub,
                previewRenderer);
        mViewFinder.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom)
                        -> {
                    tryBindUseCases();

                    if (bundle == null) {
                        return;
                    }

                    if (!bundle.getBoolean(INTENT_EXTRA_LOG_VIEWFINDER_POSITION)) {
                        return;
                    }

                    Rect rect = new Rect();
                    v.getGlobalVisibleRect(rect);

                    String viewFinderPositionText =
                            rect.left + "," + rect.top + "," + rect.right + "," + rect.bottom;
                    String fileName = "camerax_view_finder_position_" + System.currentTimeMillis();
                    writeTextToExternalStorage(viewFinderPositionText, fileName, "txt");
                });

        mVideoToggle = findViewById(R.id.VideoToggle);
        mPhotoToggle = findViewById(R.id.PhotoToggle);
        mAnalysisToggle = findViewById(R.id.AnalysisToggle);
        mPreviewToggle = findViewById(R.id.PreviewToggle);

        updateUseCaseCombinationByIntent(getIntent());

        mTakePicture = findViewById(R.id.Picture);
        mFlashButton = findViewById(R.id.flash_toggle);
        mScreenFlashView = findViewById(R.id.screen_flash_view);
        mCameraDirectionButton = findViewById(R.id.direction_toggle);
        mTorchButton = findViewById(R.id.torch_toggle);
        mCaptureQualityToggle = findViewById(R.id.capture_quality);
        mPlusEV = findViewById(R.id.plus_ev_toggle);
        mDecEV = findViewById(R.id.dec_ev_toggle);
        mZslToggle = findViewById(R.id.zsl_toggle);
        mPreviewStabilizationToggle = findViewById(R.id.preview_stabilization);
        mZoomSeekBar = findViewById(R.id.seekBar);
        mZoomRatioLabel = findViewById(R.id.zoomRatio);
        mZoomIn2XToggle = findViewById(R.id.zoom_in_2x_toggle);
        mZoomResetToggle = findViewById(R.id.zoom_reset_toggle);

        mTextView = findViewById(R.id.textView);
        mDynamicRangeUi = new DynamicRangeUi(findViewById(R.id.dynamic_range));
        mButtonImageOutputFormat = findViewById(R.id.image_output_format);
        mRecordUi = new RecordUi(
                findViewById(R.id.Video),
                findViewById(R.id.video_pause),
                findViewById(R.id.video_stats),
                findViewById(R.id.video_quality),
                findViewById(R.id.video_persistent),
                (newState) -> updateDynamicRangeUiState()
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
                requireNonNull(ContextCompat.getSystemService(this, DisplayManager.class));
        dpyMgr.registerDisplayListener(mDisplayListener, new Handler(Looper.getMainLooper()));

        StrictMode.VmPolicy vmPolicy =
                new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setVmPolicy(vmPolicy);
        StrictMode.ThreadPolicy threadPolicy =
                new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setThreadPolicy(threadPolicy);

        // Get params from adb extra string
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
            updateAppUIForE2ETest();
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
     * Writes text data to a file in public external directory for reading during tests.
     */
    private void writeTextToExternalStorage(@NonNull String text, @NonNull String filename,
            @NonNull String extension) {
        mFileWriterExecutorService.execute(() -> {
            writeTextToExternalFile(text, filename, extension);
        });
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
                requireNonNull(ContextCompat.getSystemService(this, DisplayManager.class));
        dpyMgr.unregisterDisplayListener(mDisplayListener);
        mPreviewRenderer.shutdown();
        mFileWriterExecutorService.shutdown();
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

        // Stop in-progress video recording if it's not a persistent recording.
        if (hasRunningRecording() && !isPersistentRecordingEnabled()) {
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
            String msg = getBindFailedErrorMessage();
            Log.e(TAG, "bindToLifecycle() failed. " + msg, ex);
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
            // Reset video dynamic range to avoid failure
            setSelectedDynamicRange(DynamicRange.SDR);

            reduceUseCaseToFindSupportedCombination();

            if (!calledBySelf) {
                // Only call self if not already calling self to avoid an infinite loop.
                tryBindUseCases(true);
            }
        }
        updateButtonsUi();
    }

    @NonNull
    private String getBindFailedErrorMessage() {
        if (mVideoQuality != QUALITY_AUTO) {
            return "Bind too many use cases or video quality is too large.";
        } else if (mImageOutputFormat == OUTPUT_FORMAT_JPEG_ULTRA_HDR
                && Objects.equals(mDynamicRange, DynamicRange.SDR)) {
            return "Bind too many use cases or device does not support concurrent SDR and HDR.";
        } else if (!Objects.equals(mDynamicRange, DynamicRange.SDR)) {
            return "Bind too many use cases or unsupported dynamic range combination.";
        }
        return "Bind too many use cases.";
    }

    private boolean hasRunningRecording() {
        RecordUi.State recordState = mRecordUi.getState();
        return recordState == RecordUi.State.RECORDING || recordState == RecordUi.State.PAUSED;
    }

    private boolean isPersistentRecordingEnabled() {
        return mRecordUi.getButtonPersistent().isChecked();
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
            // No need to do further use case combination check since Preview + ImageCapture
            // should be always supported.
        }
    }

    /**
     * Builds all use cases based on current settings and return as an array.
     */
    @SuppressLint("RestrictedApiAndroidX")
    private List<UseCase> buildUseCases() {
        List<UseCase> useCases = new ArrayList<>();
        if (mVideoToggle.isChecked() || mPreviewToggle.isChecked()) {
            // Update possible dynamic ranges for current camera
            updateDynamicRangeConfiguration();
        }

        if (mPreviewToggle.isChecked()) {
            Preview preview = new Preview.Builder()
                    .setTargetName("Preview")
                    .setTargetAspectRatio(mTargetAspectRatio)
                    .setPreviewStabilizationEnabled(mIsPreviewStabilizationOn)
                    .setDynamicRange(
                            mVideoToggle.isChecked() ? DynamicRange.UNSPECIFIED : mDynamicRange)
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
                    .setOutputFormat(mImageOutputFormat)
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
            // Recreate the Recorder except there's a running persistent recording, existing
            // Recorder. We may later consider reuse the Recorder everytime if the quality didn't
            // change.
            if (mVideoCapture == null
                    || mRecorder == null
                    || !(hasRunningRecording() && isPersistentRecordingEnabled())) {
                Recorder.Builder builder = new Recorder.Builder();
                if (mVideoQuality != QUALITY_AUTO) {
                    builder.setQualitySelector(QualitySelector.from(mVideoQuality));
                }
                mRecorder = builder.build();
                mVideoCapture = new VideoCapture.Builder<>(mRecorder)
                        .setMirrorMode(mVideoMirrorMode)
                        .setDynamicRange(mDynamicRange)
                        .build();
            }
            useCases.add(mVideoCapture);
        }
        return useCases;
    }

    private void updateDynamicRangeConfiguration() {
        mSelectableDynamicRanges.clear();

        Set<DynamicRange> supportedDynamicRanges = Collections.singleton(DynamicRange.SDR);
        // The dynamic range here (mDynamicRange) is considered the dynamic range for
        // Preview/VideoCapture for following reasons:
        // 1. ImageAnalysis currently only support SDR, so only update supported ranges if
        // ImageAnalysis is not enabled.
        // 2. ImageCapture's dynamic range is determined by its output format (JPEG -> SDR,
        // Ultra HDR -> HDR unspecified), so mDynamicRange can be updated but does not affect
        // ImageCapture's configuration.
        if (!mAnalysisToggle.isChecked()) {
            if (mVideoToggle.isChecked()) {
                // Get the list of available dynamic ranges for the current quality
                VideoCapabilities videoCapabilities = Recorder.getVideoCapabilities(
                        mCamera.getCameraInfo());
                supportedDynamicRanges = videoCapabilities.getSupportedDynamicRanges();
            } else if (mPreviewToggle.isChecked()) {
                supportedDynamicRanges = new HashSet<>();
                // Add SDR as its always available
                supportedDynamicRanges.add(DynamicRange.SDR);

                // Add all HDR dynamic ranges supported by the display
                Set<DynamicRange> queryResult = mCamera.getCameraInfo()
                        .querySupportedDynamicRanges(
                                Collections.singleton(DynamicRange.UNSPECIFIED));
                supportedDynamicRanges.addAll(queryResult);
            }
        }

        if (supportedDynamicRanges.size() > 1) {
            if (hasTenBitDynamicRange(supportedDynamicRanges)) {
                mSelectableDynamicRanges.add(DynamicRange.HDR_UNSPECIFIED_10_BIT);
            }
        }
        mSelectableDynamicRanges.addAll(supportedDynamicRanges);

        // In case the previous dynamic range held in mDynamicRange isn't supported, reset
        // to SDR.
        if (!mSelectableDynamicRanges.contains(mDynamicRange)) {
            setSelectedDynamicRange(DynamicRange.SDR);
        }
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
                                    if (!requireNonNull(result.get(permission))) {
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
        File pictureFolder = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (createFolder(pictureFolder)) {
            Log.e(TAG, "Failed to create directory: " + pictureFolder);
        }
    }

    /** Checks the folder existence by how the video file be created. */
    private void createDefaultVideoFolderIfNotExist() {
        String videoFilePath =
                getAbsolutePathFromUri(getApplicationContext().getContentResolver(),
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        if (videoFilePath == null || !createParentFolder(videoFilePath)) {
            Log.e(TAG, "Failed to create parent directory for: " + videoFilePath);
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

    @SuppressLint("RestrictedApiAndroidX")
    ZoomGestureDetector.OnZoomGestureListener mZoomGestureListener = zoomEvent -> {
        if (mCamera != null && zoomEvent instanceof ZoomGestureDetector.ZoomEvent.Move) {
            CameraInfo cameraInfo = mCamera.getCameraInfo();
            float newZoom =
                    requireNonNull(cameraInfo.getZoomState().getValue()).getZoomRatio()
                            * ((ZoomGestureDetector.ZoomEvent.Move) zoomEvent)
                            .getIncrementalScaleFactor();
            setZoomRatio(newZoom);
        }
        return true;
    };

    GestureDetector.OnGestureListener onTapGestureListener =
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(@NonNull MotionEvent e) {
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
                (int) (requireNonNull(cameraInfo.getZoomState().getValue()).getLinearZoom()
                        * MAX_SEEKBAR_VALUE));
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
                && requireNonNull(cameraInfo.getZoomState().getValue()).getMaxZoomRatio() >= 2.0f;
    }

    private void setUpZoomButton() {
        mZoomIn2XToggle.setOnClickListener(v -> setZoomRatio(2.0f));

        mZoomResetToggle.setOnClickListener(v -> setZoomRatio(1.0f));
    }

    private void setUpPreviewStabilizationButton() {
        mPreviewStabilizationToggle.setOnClickListener(v -> {
            mIsPreviewStabilizationOn = !mIsPreviewStabilizationOn;
            if (mIsPreviewStabilizationOn) {
                showPreviewStabilizationToast("Preview Stabilization On, FOV changes");
            }
            tryBindUseCases();
        });
    }

    void setZoomRatio(float newZoom) {
        if (mCamera == null) {
            return;
        }

        CameraInfo cameraInfo = mCamera.getCameraInfo();
        CameraControl cameraControl = mCamera.getCameraControl();
        float clampedNewZoom = MathUtils.clamp(newZoom,
                requireNonNull(cameraInfo.getZoomState().getValue()).getMinZoomRatio(),
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

    @SuppressLint("RestrictedApiAndroidX")
    private void setupViewFinderGestureControls() {
        GestureDetector tapGestureDetector = new GestureDetector(this, onTapGestureListener);
        ZoomGestureDetector scaleDetector = new ZoomGestureDetector(this, mZoomGestureListener);
        mViewFinder.setOnTouchListener((view, e) -> {
            boolean tapEventProcessed = tapGestureDetector.onTouchEvent(e);
            boolean scaleEventProcessed = scaleDetector.onTouchEvent(e);
            return tapEventProcessed || scaleEventProcessed;
        });
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

    private static class DynamicRangeUi {

        enum State {
            // Button can be selected to choose dynamic range
            CONFIGURABLE,
            // Button is visible, but cannot be selected
            VISIBLE,
            // Button is not visible, cannot be selected
            HIDDEN
        }

        private State mState = State.HIDDEN;

        private final Button mButtonDynamicRange;

        DynamicRangeUi(@NonNull Button buttonDynamicRange) {
            mButtonDynamicRange = buttonDynamicRange;
        }

        void setState(@NonNull State newState) {
            if (newState != mState) {
                mState = newState;
                switch (newState) {
                    case HIDDEN: {
                        mButtonDynamicRange.setEnabled(false);
                        mButtonDynamicRange.setVisibility(View.INVISIBLE);
                        break;
                    }
                    case VISIBLE: {
                        mButtonDynamicRange.setEnabled(false);
                        mButtonDynamicRange.setVisibility(View.VISIBLE);
                        break;
                    }
                    case CONFIGURABLE: {
                        mButtonDynamicRange.setEnabled(true);
                        mButtonDynamicRange.setVisibility(View.VISIBLE);
                        break;
                    }
                }
            }
        }

        @NonNull
        Button getButton() {
            return mButtonDynamicRange;
        }

        void setDisplayedDynamicRange(@NonNull DynamicRange dynamicRange) {
            int resId = R.string.toggle_video_dyn_rng_unknown;
            for (DynamicRangeUiData uiData : DYNAMIC_RANGE_UI_DATA) {
                if (Objects.equals(dynamicRange, uiData.mDynamicRange)) {
                    resId = uiData.mToggleLabelRes;
                    break;
                }
            }
            mButtonDynamicRange.setText(resId);
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
        private final ToggleButton mButtonPersistent;
        private boolean mEnabled = false;
        private State mState = State.IDLE;
        private final Consumer<State> mNewStateConsumer;

        RecordUi(@NonNull Button buttonRecord, @NonNull Button buttonPause,
                @NonNull TextView textStats, @NonNull Button buttonQuality,
                @NonNull ToggleButton buttonPersistent,
                @NonNull Consumer<State> onNewState) {
            mButtonRecord = buttonRecord;
            mButtonPause = buttonPause;
            mTextStats = textStats;
            mButtonQuality = buttonQuality;
            mButtonPersistent = buttonPersistent;
            mNewStateConsumer = onNewState;
        }

        void setEnabled(boolean enabled) {
            mEnabled = enabled;
            if (enabled) {
                mTextStats.setText("");
                mTextStats.setVisibility(View.VISIBLE);
                mButtonQuality.setVisibility(View.VISIBLE);
                mButtonPersistent.setVisibility(View.VISIBLE);
                updateUi();
            } else {
                mButtonRecord.setText("Record");
                mButtonRecord.setEnabled(false);
                mButtonPause.setVisibility(View.INVISIBLE);
                mButtonQuality.setVisibility(View.INVISIBLE);
                mTextStats.setVisibility(View.GONE);
                mButtonPersistent.setVisibility(View.INVISIBLE);
            }
        }

        void setState(@NonNull State state) {
            if (state != mState) {
                mState = state;
                updateUi();
                mNewStateConsumer.accept(state);
            }
        }

        @NonNull
        State getState() {
            return mState;
        }

        void hideUi() {
            mButtonRecord.setVisibility(View.GONE);
            mButtonPause.setVisibility(View.GONE);
            mTextStats.setVisibility(View.GONE);
            mButtonPersistent.setVisibility(View.GONE);
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
                    mButtonPersistent.setEnabled(true);
                    mButtonQuality.setEnabled(true);
                    break;
                case RECORDING:
                    mButtonRecord.setText("Stop");
                    mButtonRecord.setEnabled(true);
                    mButtonPause.setText("Pause");
                    mButtonPause.setVisibility(View.VISIBLE);
                    mButtonPersistent.setEnabled(false);
                    mButtonQuality.setEnabled(false);
                    break;
                case STOPPING:
                    mButtonRecord.setText("Saving");
                    mButtonRecord.setEnabled(false);
                    mButtonPause.setText("Pause");
                    mButtonPause.setVisibility(View.INVISIBLE);
                    mButtonPersistent.setEnabled(false);
                    mButtonQuality.setEnabled(true);
                    break;
                case PAUSED:
                    mButtonRecord.setText("Stop");
                    mButtonRecord.setEnabled(true);
                    mButtonPause.setText("Resume");
                    mButtonPause.setVisibility(View.VISIBLE);
                    mButtonPersistent.setEnabled(false);
                    mButtonQuality.setEnabled(true);
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

        ToggleButton getButtonPersistent() {
            return mButtonPersistent;
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

    @Nullable
    View getViewFinder() {
        return mViewFinder;
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

    @NonNull
    private static String getDynamicRangeMenuItemName(@NonNull DynamicRange dynamicRange) {
        String menuItemName = dynamicRange.toString();
        for (DynamicRangeUiData uiData : DYNAMIC_RANGE_UI_DATA) {
            if (Objects.equals(dynamicRange, uiData.mDynamicRange)) {
                menuItemName = uiData.mMenuItemName;
                break;
            }
        }
        return menuItemName;
    }

    private static int dynamicRangeToItemId(@NonNull DynamicRange dynamicRange) {
        int itemId = -1;
        for (int i = 0; i < DYNAMIC_RANGE_UI_DATA.size(); i++) {
            DynamicRangeUiData uiData = DYNAMIC_RANGE_UI_DATA.get(i);
            if (Objects.equals(dynamicRange, uiData.mDynamicRange)) {
                itemId = i;
                break;
            }
        }
        if (itemId == -1) {
            throw new IllegalArgumentException("Unsupported dynamic range: " + dynamicRange);
        }
        return itemId;
    }

    @NonNull
    private static DynamicRange itemIdToDynamicRange(int itemId) {
        if (itemId < 0 || itemId >= DYNAMIC_RANGE_UI_DATA.size()) {
            throw new IllegalArgumentException("Undefined item id: " + itemId);
        }
        return DYNAMIC_RANGE_UI_DATA.get(itemId).mDynamicRange;
    }

    @NonNull
    private static String getImageOutputFormatIconName(@ImageCapture.OutputFormat int format) {
        if (format == OUTPUT_FORMAT_JPEG) {
            return "Jpeg";
        } else if (format == OUTPUT_FORMAT_JPEG_ULTRA_HDR) {
            return "Ultra HDR";
        }
        return "?";
    }

    @NonNull
    private static String getImageOutputFormatMenuItemName(@ImageCapture.OutputFormat int format) {
        if (format == OUTPUT_FORMAT_JPEG) {
            return "Jpeg";
        } else if (format == OUTPUT_FORMAT_JPEG_ULTRA_HDR) {
            return "Ultra HDR";
        }
        return "Unknown format";
    }

    private static int imageOutputFormatToItemId(@ImageCapture.OutputFormat int format) {
        if (format == OUTPUT_FORMAT_JPEG) {
            return 0;
        } else if (format == OUTPUT_FORMAT_JPEG_ULTRA_HDR) {
            return 1;
        } else {
            throw new IllegalArgumentException("Undefined output format: " + format);
        }
    }

    @ImageCapture.OutputFormat
    private static int itemIdToImageOutputFormat(int itemId) {
        switch (itemId) {
            case 0:
                return OUTPUT_FORMAT_JPEG;
            case 1:
                return OUTPUT_FORMAT_JPEG_ULTRA_HDR;
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

    private boolean isFrontCamera() {
        return getLensFacing(Objects.requireNonNull(getCameraInfo()))
                == CameraSelector.LENS_FACING_FRONT;
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

    private static boolean isLegacyDevice(@NonNull CameraInfo cameraInfo) {
        if (CameraXViewModel.CAMERA_PIPE_IMPLEMENTATION_OPTION.equals(
                getConfiguredCameraXCameraImplementation())) {
            return isCameraPipeLegacyDevice(cameraInfo);
        }
        return isCamera2LegacyDevice(cameraInfo);
    }

    @SuppressLint("NullAnnotationGroup")
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    private static boolean isCamera2LegacyDevice(@NonNull CameraInfo cameraInfo) {
        return Camera2CameraInfo.from(cameraInfo).getCameraCharacteristic(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
        ) == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }

    @SuppressLint("NullAnnotationGroup")
    @OptIn(markerClass =
            androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop.class)
    private static boolean isCameraPipeLegacyDevice(@NonNull CameraInfo cameraInfo) {
        return androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo.from(cameraInfo)
                .getCameraCharacteristic(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
                ) == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
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

    private static final class DynamicRangeUiData {
        private DynamicRangeUiData(
                @NonNull DynamicRange dynamicRange,
                @NonNull String menuItemName,
                int toggleLabelRes) {
            mDynamicRange = dynamicRange;
            mMenuItemName = menuItemName;
            mToggleLabelRes = toggleLabelRes;
        }

        DynamicRange mDynamicRange;
        String mMenuItemName;
        int mToggleLabelRes;
    }

    @RequiresApi(26)
    static class Api26Impl {
        private Api26Impl() {
            // This class is not instantiable.
        }

        static void setColorMode(@NonNull Window window, int colorMode) {
            window.setColorMode(colorMode);
        }

    }
}
