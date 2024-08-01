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

import static android.hardware.camera2.CameraCharacteristics.LENS_POSE_REFERENCE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static androidx.camera.testing.impl.FileUtil.canDeviceWriteToMediaStore;
import static androidx.camera.testing.impl.FileUtil.createParentFolder;
import static androidx.camera.testing.impl.FileUtil.generateVideoFileOutputOptions;
import static androidx.camera.testing.impl.FileUtil.generateVideoMediaStoreOptions;
import static androidx.camera.testing.impl.FileUtil.getAbsolutePathFromUri;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.camera2.pipe.integration.CameraPipeConfig;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CompositionSettings;
import androidx.camera.core.ConcurrentCamera;
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.ExperimentalCameraInfo;
import androidx.camera.core.ExperimentalMirrorMode;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MirrorMode;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration;
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
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.math.MathUtils;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Concurrent camera activity.
 */
public class ConcurrentCameraActivity extends AppCompatActivity {
    private static final String TAG = "ConcurrentCamera";
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            "android.permission.CAMERA"
    };

    // For Video Capture
    private RecordUi mRecordUi;
    private VideoCapture<Recorder> mVideoCapture;
    private final CountingIdlingResource mVideoSavedIdlingResource =
            new CountingIdlingResource("videosaved");
    private Recording mActiveRecording;
    private long mVideoCaptureAutoStopLength = 0;
    private SessionMediaUriSet
            mSessionVideosUriSet = new SessionMediaUriSet();
    private static final Quality QUALITY_AUTO = null;
    private Quality mVideoQuality;

    @NonNull private PreviewView mSinglePreviewView;
    @NonNull private PreviewView mFrontPreviewView;
    @NonNull private PreviewView mBackPreviewView;
    @NonNull private FrameLayout mFrontPreviewViewForPip;
    @NonNull private FrameLayout mBackPreviewViewForPip;
    @NonNull private FrameLayout mFrontPreviewViewForSideBySide;
    @NonNull private FrameLayout mBackPreviewViewForSideBySide;
    @NonNull private ToggleButton mModeButton;
    @NonNull private ToggleButton mLayoutButton;
    @NonNull private ToggleButton mToggleButton;
    @NonNull private ToggleButton mDualSelfieButton;
    @NonNull private ToggleButton mDualRecordButton;
    @NonNull private LinearLayout mSideBySideLayout;
    @NonNull private FrameLayout mPiPLayout;
    @Nullable private ProcessCameraProvider mCameraProvider;
    private boolean mIsConcurrentModeOn = false;
    private boolean mIsLayoutPiP = true;
    private boolean mIsFrontPrimary = true;
    private boolean mIsDualSelfieEnabled = false;
    private boolean mIsDualRecordEnabled = false;
    private boolean mIsCameraPipeEnabled = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_concurrent_camera);

        mFrontPreviewViewForPip = findViewById(R.id.camera_front_pip);
        mBackPreviewViewForPip = findViewById(R.id.camera_back_pip);
        mBackPreviewViewForSideBySide = findViewById(R.id.camera_back_side_by_side);
        mFrontPreviewViewForSideBySide = findViewById(R.id.camera_front_side_by_side);
        mSideBySideLayout = findViewById(R.id.layout_side_by_side);
        mPiPLayout = findViewById(R.id.layout_pip);
        mModeButton = findViewById(R.id.mode_button);
        mLayoutButton = findViewById(R.id.layout_button);
        mToggleButton = findViewById(R.id.toggle_button);
        mDualSelfieButton = findViewById(R.id.dual_selfie);
        mDualRecordButton = findViewById(R.id.dual_record);

        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD))
                .build();
        mVideoCapture = new VideoCapture.Builder<>(recorder)
                .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
                .build();
        mRecordUi = new RecordUi(
                findViewById(R.id.Video),
                findViewById(R.id.video_pause),
                findViewById(R.id.video_stats),
                findViewById(R.id.video_quality),
                findViewById(R.id.video_persistent),
                (newState) -> {});
        setUpRecordButton();

        boolean isConcurrentCameraSupported =
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_CONCURRENT);
        mModeButton.setEnabled(isConcurrentCameraSupported);
        mLayoutButton.setEnabled(false);
        if (!isConcurrentCameraSupported) {
            Toast.makeText(this, getString(R.string.concurrent_not_supported_warning),
                    Toast.LENGTH_SHORT).show();
        }
        mModeButton.setOnClickListener(view -> {
            if (mCameraProvider == null) {
                return;
            }
            mFrontPreviewView = null;
            mBackPreviewView = null;
            // Switch the concurrent mode
            if (mCameraProvider != null && mIsConcurrentModeOn) {
                mIsFrontPrimary = true;
                mIsLayoutPiP = true;
                bindPreviewForSingle(mCameraProvider);
                mIsConcurrentModeOn = false;
                mIsDualSelfieEnabled = false;
                mDualSelfieButton.setChecked(false);
                mIsDualRecordEnabled = false;
                mDualRecordButton.setChecked(false);
            } else {
                mIsLayoutPiP = true;
                bindPreviewForPiP(mCameraProvider);
                mIsConcurrentModeOn = true;
            }
            mLayoutButton.setEnabled(mCameraProvider != null && mIsConcurrentModeOn);
        });
        mLayoutButton.setOnClickListener(view -> {
            if (mIsLayoutPiP) {
                bindPreviewForSideBySide();
            } else {
                bindPreviewForPiP(mCameraProvider);
            }
            mIsLayoutPiP = !mIsLayoutPiP;
        });
        mToggleButton.setOnClickListener(view -> {
            mIsFrontPrimary = !mIsFrontPrimary;
            if (mIsConcurrentModeOn) {
                if (mIsLayoutPiP) {
                    bindPreviewForPiP(mCameraProvider);
                } else {
                    bindPreviewForSideBySide();
                }
            } else {
                bindPreviewForSingle(mCameraProvider);
            }
        });
        mDualSelfieButton.setOnClickListener(view -> {
            mIsDualSelfieEnabled = mDualSelfieButton.isChecked();
            mDualSelfieButton.setChecked(mIsDualSelfieEnabled);
        });
        mDualRecordButton.setOnClickListener(view -> {
            mIsDualRecordEnabled = mDualRecordButton.isChecked();
            mDualRecordButton.setChecked(mIsDualRecordEnabled);
        });

        setupPermissions();
    }

    @SuppressLint("NullAnnotationGroup")
    @OptIn(markerClass = ExperimentalCameraProviderConfiguration.class)
    private void startCamera() {
        if (mIsCameraPipeEnabled) {
            ProcessCameraProvider.configureInstance(CameraPipeConfig.defaultConfig());
        }

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                mCameraProvider = cameraProviderFuture.get();
                bindPreviewForSingle(mCameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreviewForSingle(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        mSideBySideLayout.setVisibility(GONE);
        mFrontPreviewViewForPip.setVisibility(VISIBLE);
        mBackPreviewViewForPip.setVisibility(GONE);
        mPiPLayout.setVisibility(VISIBLE);
        mToggleButton.setVisibility(VISIBLE);
        mLayoutButton.setVisibility(VISIBLE);
        mRecordUi.hideUi();
        // Front
        mSinglePreviewView = new PreviewView(this);
        mSinglePreviewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        mFrontPreviewViewForPip.addView(mSinglePreviewView);
        Preview previewFront = new Preview.Builder()
                .build();
        CameraSelector cameraSelectorFront = new CameraSelector.Builder()
                .requireLensFacing(mIsFrontPrimary
                        ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK)
                .build();
        previewFront.setSurfaceProvider(mSinglePreviewView.getSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle(
                this, cameraSelectorFront, previewFront);
        mDualSelfieButton.setVisibility(camera.getCameraInfo().isLogicalMultiCameraSupported()
                ? VISIBLE : GONE);
        mDualRecordButton.setVisibility(VISIBLE);
        mIsDualSelfieEnabled = false;
        mIsDualRecordEnabled = false;
        setupZoomAndTapToFocus(camera, mSinglePreviewView);
    }

    void bindPreviewForPiP(@NonNull ProcessCameraProvider cameraProvider) {
        mSideBySideLayout.setVisibility(GONE);
        mFrontPreviewViewForPip.setVisibility(VISIBLE);
        mBackPreviewViewForPip.setVisibility(VISIBLE);
        mPiPLayout.setVisibility(VISIBLE);
        mDualSelfieButton.setVisibility(GONE);
        mDualRecordButton.setVisibility(GONE);
        if (mIsDualRecordEnabled) {
            mRecordUi.showUi();
        } else {
            mRecordUi.hideUi();
        }
        mToggleButton.setVisibility(mIsDualRecordEnabled ? GONE : VISIBLE);
        mLayoutButton.setVisibility(mIsDualRecordEnabled ? GONE : VISIBLE);
        if (mFrontPreviewView == null && mBackPreviewView == null) {
            // Front
            mFrontPreviewView = new PreviewView(this);
            mFrontPreviewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
            mFrontPreviewViewForPip.removeAllViews();
            mFrontPreviewViewForPip.addView(mFrontPreviewView,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
            // Back
            mBackPreviewView = new PreviewView(this);
            mBackPreviewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
            mBackPreviewViewForPip.removeAllViews();
            mBackPreviewViewForPip.addView(mBackPreviewView,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
            cameraProvider.unbindAll();
            bindToLifecycleForConcurrentCamera(
                    cameraProvider,
                    this,
                    mFrontPreviewView,
                    mBackPreviewView);
        } else {
            updateFrontAndBackView(
                    mIsFrontPrimary,
                    mFrontPreviewViewForPip,
                    mBackPreviewViewForPip,
                    mFrontPreviewView,
                    mBackPreviewView);
        }
    }

    void bindPreviewForSideBySide() {
        mSideBySideLayout.setVisibility(VISIBLE);
        mPiPLayout.setVisibility(GONE);
        mDualSelfieButton.setVisibility(GONE);
        if (mFrontPreviewView == null && mBackPreviewView == null) {
            mFrontPreviewView = new PreviewView(this);
            mFrontPreviewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
            mBackPreviewView = new PreviewView(this);
            mBackPreviewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        }
        updateFrontAndBackView(
                mIsFrontPrimary,
                mFrontPreviewViewForSideBySide,
                mBackPreviewViewForSideBySide,
                mFrontPreviewView,
                mBackPreviewView);
    }

    @SuppressLint({"NullAnnotationGroup", "RestrictedApiAndroidX"})
    @OptIn(markerClass = {ExperimentalCamera2Interop.class, ExperimentalMirrorMode.class,
            androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop.class})
    private void bindToLifecycleForConcurrentCamera(
            @NonNull ProcessCameraProvider cameraProvider,
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull PreviewView frontPreviewView,
            @NonNull PreviewView backPreviewView) {
        if (mIsDualSelfieEnabled) {
            CameraInfo cameraInfoPrimary = null;
            for (CameraInfo cameraInfo : cameraProvider.getAvailableCameraInfos()) {
                if (cameraInfo.getLensFacing() == CameraSelector.LENS_FACING_FRONT) {
                    cameraInfoPrimary = cameraInfo;
                    break;
                }
            }
            if (cameraInfoPrimary == null
                    || cameraInfoPrimary.getPhysicalCameraInfos().size() != 2) {
                return;
            }

            String innerPhysicalCameraId = null;
            String outerPhysicalCameraId = null;
            for (CameraInfo info : cameraInfoPrimary.getPhysicalCameraInfos()) {
                if (isPrimaryCamera(info)) {
                    innerPhysicalCameraId = mIsCameraPipeEnabled
                            ? androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo
                                    .from(info).getCameraId()
                            : androidx.camera.camera2.interop.Camera2CameraInfo
                                    .from(info).getCameraId();
                } else {
                    outerPhysicalCameraId = mIsCameraPipeEnabled
                            ? androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo
                                    .from(info).getCameraId()
                            : androidx.camera.camera2.interop.Camera2CameraInfo
                                    .from(info).getCameraId();
                }
            }

            if (Objects.equal(innerPhysicalCameraId, outerPhysicalCameraId)) {
                return;
            }

            Preview previewFront = new Preview.Builder()
                    .build();
            previewFront.setSurfaceProvider(frontPreviewView.getSurfaceProvider());
            SingleCameraConfig primary = new SingleCameraConfig(
                    new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .setPhysicalCameraId(innerPhysicalCameraId)
                            .build(),
                    new UseCaseGroup.Builder()
                            .addUseCase(previewFront)
                            .build(),
                    lifecycleOwner);
            Preview previewBack = new Preview.Builder()
                    .setMirrorMode(MirrorMode.MIRROR_MODE_OFF)
                    .build();
            previewBack.setSurfaceProvider(backPreviewView.getSurfaceProvider());
            SingleCameraConfig secondary = new SingleCameraConfig(
                    new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .setPhysicalCameraId(outerPhysicalCameraId)
                            .build(),
                    new UseCaseGroup.Builder()
                            .addUseCase(previewBack)
                            .build(),
                    lifecycleOwner);
            cameraProvider.bindToLifecycle(ImmutableList.of(primary, secondary));
        } else {
            CameraSelector cameraSelectorPrimary = null;
            CameraSelector cameraSelectorSecondary = null;
            for (List<CameraInfo> cameraInfoList : cameraProvider
                    .getAvailableConcurrentCameraInfos()) {
                for (CameraInfo cameraInfo : cameraInfoList) {
                    if (cameraInfo.getLensFacing() == CameraSelector.LENS_FACING_FRONT) {
                        cameraSelectorPrimary = cameraInfo.getCameraSelector();
                    } else if (cameraInfo.getLensFacing() == CameraSelector.LENS_FACING_BACK) {
                        cameraSelectorSecondary = cameraInfo.getCameraSelector();
                    }
                }

                if (cameraSelectorPrimary == null || cameraSelectorSecondary == null) {
                    // If either a primary or secondary selector wasn't found, reset both
                    // to move on to the next list of CameraInfos.
                    cameraSelectorPrimary = null;
                    cameraSelectorSecondary = null;
                } else {
                    // If both primary and secondary camera selectors were found, we can
                    // conclude the search.
                    break;
                }
            }
            if (cameraSelectorPrimary == null || cameraSelectorSecondary == null) {
                return;
            }
            if (mIsDualRecordEnabled) {
                mFrontPreviewViewForPip.removeAllViews();
                mFrontPreviewViewForPip.addView(mSinglePreviewView);
                mBackPreviewViewForPip.setVisibility(GONE);

                ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(
                                AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .build();
                Preview preview = new Preview.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .build();
                preview.setSurfaceProvider(mSinglePreviewView.getSurfaceProvider());
                UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                        .addUseCase(preview)
                        .addUseCase(mVideoCapture)
                        .build();
                // PiP
                SingleCameraConfig primary = new SingleCameraConfig(
                        cameraSelectorPrimary,
                        useCaseGroup,
                        new CompositionSettings.Builder()
                                .setAlpha(1.0f)
                                .setOffset(0.0f, 0.0f)
                                .setScale(1.0f, 1.0f)
                                .build(),
                        lifecycleOwner);
                SingleCameraConfig secondary = new SingleCameraConfig(
                        cameraSelectorSecondary,
                        useCaseGroup,
                        new CompositionSettings.Builder()
                                .setAlpha(1.0f)
                                .setOffset(-0.3f, -0.4f)
                                .setScale(0.3f, 0.3f)
                                .build(),
                        lifecycleOwner);
                cameraProvider.bindToLifecycle(ImmutableList.of(primary, secondary));
            } else {
                Preview previewFront = new Preview.Builder()
                        .build();
                previewFront.setSurfaceProvider(frontPreviewView.getSurfaceProvider());
                SingleCameraConfig primary = new SingleCameraConfig(
                        cameraSelectorPrimary,
                        new UseCaseGroup.Builder()
                                .addUseCase(previewFront)
                                .build(),
                        lifecycleOwner);
                Preview previewBack = new Preview.Builder()
                        .build();
                previewBack.setSurfaceProvider(backPreviewView.getSurfaceProvider());
                SingleCameraConfig secondary = new SingleCameraConfig(
                        cameraSelectorSecondary,
                        new UseCaseGroup.Builder()
                                .addUseCase(previewBack)
                                .build(),
                        lifecycleOwner);
                ConcurrentCamera concurrentCamera =
                        cameraProvider.bindToLifecycle(ImmutableList.of(primary, secondary));

                setupZoomAndTapToFocus(concurrentCamera.getCameras().get(0), frontPreviewView);
                setupZoomAndTapToFocus(concurrentCamera.getCameras().get(1), backPreviewView);
            }
        }
    }

    @SuppressLint("NullAnnotationGroup")
    @OptIn(markerClass = { ExperimentalCamera2Interop.class,
            androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop.class })
    private boolean isPrimaryCamera(@NonNull CameraInfo info) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return true;
        }
        if (mIsCameraPipeEnabled) {
            return androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo.from(info)
                    .getCameraCharacteristic(LENS_POSE_REFERENCE)
                    == CameraCharacteristics.LENS_POSE_REFERENCE_PRIMARY_CAMERA;
        } else {
            return androidx.camera.camera2.interop.Camera2CameraInfo.from(info)
                    .getCameraCharacteristic(LENS_POSE_REFERENCE)
                    == CameraCharacteristics.LENS_POSE_REFERENCE_PRIMARY_CAMERA;
        }
    }

    private void setupZoomAndTapToFocus(Camera camera, PreviewView previewView) {
        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(@NonNull ScaleGestureDetector detector) {
                        CameraInfo cameraInfo = camera.getCameraInfo();
                        CameraControl cameraControl = camera.getCameraControl();
                        float newZoom =
                                cameraInfo.getZoomState().getValue().getZoomRatio()
                                        * detector.getScaleFactor();
                        float clampedNewZoom = MathUtils.clamp(newZoom,
                                cameraInfo.getZoomState().getValue().getMinZoomRatio(),
                                cameraInfo.getZoomState().getValue().getMaxZoomRatio());
                        cameraControl.setZoomRatio(clampedNewZoom)
                                .addListener(() -> {}, cmd -> cmd.run());
                        return true;
                    }
                });


        previewView.setOnTouchListener((view, motionEvent) -> {
            scaleDetector.onTouchEvent(motionEvent);

            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                MeteringPoint point =
                        previewView.getMeteringPointFactory().createPoint(
                                motionEvent.getX(), motionEvent.getY());

                camera.getCameraControl().startFocusAndMetering(
                        new FocusMeteringAction.Builder(point).build()).addListener(() -> {},
                        ContextCompat.getMainExecutor(ConcurrentCameraActivity.this));
            }
            return true;
        });
    }

    private static void updateFrontAndBackView(
            boolean isFrontPrimary,
            @NonNull ViewGroup frontParent,
            @NonNull ViewGroup backParent,
            @NonNull View frontChild,
            @NonNull View backChild) {
        frontParent.removeAllViews();
        if (frontChild.getParent() != null) {
            ((ViewGroup) frontChild.getParent()).removeView(frontChild);
        }
        backParent.removeAllViews();
        if (backChild.getParent() != null) {
            ((ViewGroup) backChild.getParent()).removeView(backChild);
        }
        if (isFrontPrimary) {
            frontParent.addView(frontChild,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
            backParent.addView(backChild,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            frontParent.addView(backChild,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
            backParent.addView(frontChild,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
        }
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
                startCamera();
            } else {
                Toast.makeText(this, getString(R.string.permission_warning),
                        Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    private boolean isPermissionMissing() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

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
                                startCamera();
                            });

            permissionLauncher.launch(REQUIRED_PERMISSIONS);
        } else {
            // Permissions already granted. Start camera.
            startCamera();
        }
    }

    private void createDefaultVideoFolderIfNotExist() {
        String videoFilePath =
                getAbsolutePathFromUri(getApplicationContext().getContentResolver(),
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        if (videoFilePath == null || !createParentFolder(videoFilePath)) {
            Log.e(TAG, "Failed to create parent directory for: " + videoFilePath);
        }
    }

    private void resetVideoSavedIdlingResource() {
        // Make the video saved idling resource non-idle, until required video length recorded.
        if (mVideoSavedIdlingResource.isIdleNow()) {
            mVideoSavedIdlingResource.increment();
        }
    }

    private boolean isPersistentRecordingEnabled() {
        return mRecordUi.getButtonPersistent().isChecked();
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

    private void updateVideoSavedSessionData(@NonNull Uri uri) {
        if (mSessionVideosUriSet != null) {
            mSessionVideosUriSet.add(uri);
        }

        if (!mVideoSavedIdlingResource.isIdleNow()) {
            mVideoSavedIdlingResource.decrement();
        }
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
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    break;
                default:
                    String errMsg = "Video capture failed by (" + finalize.getError() + "): "
                            + finalize.getCause();
                    Log.e(TAG, errMsg, finalize.getCause());
                    Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
            }
            mRecordUi.setState(RecordUi.State.IDLE);
        }
    };

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

    @SuppressLint({"MissingPermission", "NullAnnotationGroup"})
    @OptIn(markerClass = { ExperimentalCameraInfo.class, ExperimentalPersistentRecording.class})
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
                        pendingRecording = mVideoCapture.getOutput().prepareRecording(
                                this,
                                generateVideoMediaStoreOptions(getContentResolver(), fileName));
                    } else {
                        // Use FileOutputOption for devices in MediaStoreVideoCannotWrite Quirk.
                        pendingRecording = mVideoCapture.getOutput().prepareRecording(
                                this, generateVideoFileOutputOptions(fileName, extension));
                    }

                    resetVideoSavedIdlingResource();

                    if (isPersistentRecordingEnabled()) {
                        pendingRecording.asPersistentRecording();
                    }
                    mActiveRecording = pendingRecording
                            .withAudioEnabled()
                            .start(ContextCompat.getMainExecutor(this),
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
                    mCameraProvider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA));
            List<Quality> supportedQualities = videoCapabilities.getSupportedQualities(
                    DynamicRange.SDR);
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
                    startCamera();
                }
                return true;
            });

            popup.show();
        });
    }

    private static class SessionMediaUriSet {
        private final Set<Uri> mSessionMediaUris;

        SessionMediaUriSet() {
            mSessionMediaUris = Collections.synchronizedSet(new HashSet<>());
        }

        public void add(@NonNull Uri uri) {
            mSessionMediaUris.add(uri);
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
        private RecordUi.State mState = RecordUi.State.IDLE;
        private final Consumer<RecordUi.State> mNewStateConsumer;

        RecordUi(@NonNull Button buttonRecord, @NonNull Button buttonPause,
                @NonNull TextView textStats, @NonNull Button buttonQuality,
                @NonNull ToggleButton buttonPersistent,
                @NonNull Consumer<RecordUi.State> onNewState) {
            mButtonRecord = buttonRecord;
            mButtonPause = buttonPause;
            mTextStats = textStats;
            mButtonQuality = buttonQuality;
            mButtonPersistent = buttonPersistent;
            mNewStateConsumer = onNewState;
        }

        void setState(@NonNull RecordUi.State state) {
            if (state != mState) {
                mState = state;
                updateUi();
                mNewStateConsumer.accept(state);
            }
        }

        @NonNull
        RecordUi.State getState() {
            return mState;
        }

        void showUi() {
            mButtonRecord.setVisibility(VISIBLE);
            mButtonPause.setVisibility(VISIBLE);
            mTextStats.setVisibility(VISIBLE);
            mButtonPersistent.setVisibility(VISIBLE);
            mButtonQuality.setVisibility(VISIBLE);
        }

        void hideUi() {
            mButtonRecord.setVisibility(GONE);
            mButtonPause.setVisibility(GONE);
            mTextStats.setVisibility(GONE);
            mButtonPersistent.setVisibility(GONE);
            mButtonQuality.setVisibility(GONE);
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
}
