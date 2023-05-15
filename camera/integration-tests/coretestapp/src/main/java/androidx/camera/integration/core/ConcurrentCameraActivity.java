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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ConcurrentCamera;
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.math.MathUtils;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;
/**
 * Concurrent camera activity.
 */
public class ConcurrentCameraActivity extends AppCompatActivity {
    private static final String TAG = "ConcurrentCameraActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            "android.permission.CAMERA"
    };

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
    @NonNull private LinearLayout mSideBySideLayout;
    @NonNull private FrameLayout mPiPLayout;
    @Nullable private ProcessCameraProvider mCameraProvider;
    private boolean mIsConcurrentModeOn = false;
    private boolean mIsLayoutPiP = true;
    private boolean mIsFrontPrimary = true;

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
            } else {
                mIsLayoutPiP = true;
                bindPreviewForPiP(mCameraProvider);
                mIsConcurrentModeOn = true;
            }
            mLayoutButton.setEnabled(mCameraProvider != null && mIsConcurrentModeOn);
            mToggleButton.setEnabled(mCameraProvider != null && mIsConcurrentModeOn);
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
        if (allPermissionsGranted()) {
            if (mCameraProvider != null) {
                mCameraProvider.unbindAll();
            }
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
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
        setupZoomAndTapToFocus(camera, mSinglePreviewView);
    }
    void bindPreviewForPiP(@NonNull ProcessCameraProvider cameraProvider) {
        mSideBySideLayout.setVisibility(GONE);
        mFrontPreviewViewForPip.setVisibility(VISIBLE);
        mBackPreviewViewForPip.setVisibility(VISIBLE);
        mPiPLayout.setVisibility(VISIBLE);
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
    private void bindToLifecycleForConcurrentCamera(
            @NonNull ProcessCameraProvider cameraProvider,
            @NonNull LifecycleOwner lifecycleOwner,
            @NonNull PreviewView frontPreviewView,
            @NonNull PreviewView backPreviewView) {
        Preview previewFront = new Preview.Builder()
                .build();
        CameraSelector cameraSelectorPrimary = null;
        CameraSelector cameraSelectorSecondary = null;
        for (List<CameraInfo> cameraInfoList : cameraProvider.getAvailableConcurrentCameraInfos()) {
            for (CameraInfo cameraInfo : cameraInfoList) {
                if (cameraInfo.getLensFacing() == CameraSelector.LENS_FACING_FRONT) {
                    cameraSelectorPrimary = cameraInfo.getCameraSelector();
                } else if (cameraInfo.getLensFacing() == CameraSelector.LENS_FACING_BACK) {
                    cameraSelectorSecondary = cameraInfo.getCameraSelector();
                }
            }
        }
        if (cameraSelectorPrimary == null || cameraSelectorSecondary == null) {
            return;
        }
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
}
