/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.camera.integration.extensions;

import static androidx.camera.core.ImageCapture.ERROR_CAMERA_CLOSED;
import static androidx.camera.core.ImageCapture.ERROR_CAPTURE_FAILED;
import static androidx.camera.core.ImageCapture.ERROR_FILE_IO;
import static androidx.camera.core.ImageCapture.ERROR_INVALID_CAMERA;
import static androidx.camera.core.ImageCapture.ERROR_UNKNOWN;
import static androidx.camera.integration.extensions.CameraDirection.BACKWARD;
import static androidx.camera.integration.extensions.CameraDirection.FORWARD;
import static androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_CAMERA_DIRECTION;
import static androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_CAMERA_ID;
import static androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_DELETE_CAPTURED_IMAGE;
import static androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_EXTENSION_MODE;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.FocusMeteringResult;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.integration.extensions.utils.CameraSelectorUtil;
import androidx.camera.integration.extensions.utils.ExtensionModeUtil;
import androidx.camera.integration.extensions.utils.FpsRecorder;
import androidx.camera.integration.extensions.validation.CameraValidationResultActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.math.MathUtils;
import androidx.lifecycle.Lifecycle;
import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** An activity that shows off how extensions can be applied */
public class CameraExtensionsActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "CameraExtensionActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 42;

    private CameraSelector mCurrentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    boolean mPermissionsGranted = false;
    private CallbackToFutureAdapter.Completer<Boolean> mPermissionCompleter;

    @Nullable
    private Preview mPreview;

    @Nullable
    private ImageCapture mImageCapture;

    @ExtensionMode.Mode
    private int mCurrentExtensionMode = ExtensionMode.BOKEH;

    // Espresso testing variables
    private final CountingIdlingResource mInitializationIdlingResource = new CountingIdlingResource(
            "Initialization");

    private final CountingIdlingResource mTakePictureIdlingResource = new CountingIdlingResource(
            "TakePicture");

    private final CountingIdlingResource mPreviewViewStreamingStateIdlingResource =
            new CountingIdlingResource("PreviewView-Streaming");

    private final CountingIdlingResource mPreviewViewIdleStateIdlingResource =
            new CountingIdlingResource("PreviewView-Idle");

    private PreviewView mPreviewView;

    ProcessCameraProvider mCameraProvider;

    Camera mCamera;

    ExtensionsManager mExtensionsManager;

    boolean mDeleteCapturedImage = false;

    // < Sensor timestamp,  current timestamp >
    Map<Long, Long> mFrameTimestampMap = new HashMap<>();
    TextView mFrameInfo;

    String mCurrentCameraId = null;

    /**
     * Saves the error message of the last take picture action if any error occurs. This will be
     * null which means no error occurs.
     */
    @Nullable
    private String mLastTakePictureErrorMessage = null;

    private PreviewView.StreamState mCurrentStreamState = null;

    void setupButtons() {
        Button btnToggleMode = findViewById(R.id.PhotoToggle);
        Button btnSwitchCamera = findViewById(R.id.Switch);
        btnToggleMode.setOnClickListener(view -> bindUseCasesWithNextExtensionMode());
        btnSwitchCamera.setOnClickListener(view -> switchCameras());
    }

    void switchCameras() {
        mCameraProvider.unbindAll();
        if (mCurrentCameraId != null) {
            String nextCameraId = CameraSelectorUtil.findNextSupportedCameraId(
                    this, mExtensionsManager, mCurrentCameraId, mCurrentExtensionMode);
            if (nextCameraId == null) {
                Log.e(TAG, "Cannot find next camera id that supports the extensions mode");
                return;
            }
            mCurrentCameraSelector = CameraSelectorUtil.createCameraSelectorById(mCurrentCameraId);
        } else {
            mCurrentCameraSelector = (mCurrentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                    ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
        }
        if (!bindUseCasesWithCurrentExtensionMode()) {
            bindUseCasesWithNextExtensionMode();
        }
    }

    @VisibleForTesting
    boolean switchToExtensionMode(@ExtensionMode.Mode int extensionMode) {
        if (mCamera == null || mExtensionsManager == null) {
            return false;
        }

        if (!mExtensionsManager.isExtensionAvailable(mCurrentCameraSelector, extensionMode)) {
            return false;
        }

        mCurrentExtensionMode = extensionMode;
        bindUseCasesWithCurrentExtensionMode();

        return true;
    }

    void bindUseCasesWithNextExtensionMode() {
        do {
            mCurrentExtensionMode = getNextExtensionMode(mCurrentExtensionMode);
        } while (!bindUseCasesWithCurrentExtensionMode());
    }

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    boolean bindUseCasesWithCurrentExtensionMode() {
        if (!mExtensionsManager.isExtensionAvailable(mCurrentCameraSelector,
                mCurrentExtensionMode)) {
            return false;
        }

        resetPreviewViewStreamingStateIdlingResource();
        resetPreviewViewIdleStateIdlingResource();

        ImageCapture.Builder imageCaptureBuilder = new ImageCapture.Builder().setTargetName(
                "ImageCapture");
        mImageCapture = imageCaptureBuilder.build();

        mFrameTimestampMap.clear();
        Preview.Builder previewBuilder = new Preview.Builder().setTargetName("Preview");

        new Camera2Interop.Extender<>(previewBuilder)
                .setSessionCaptureCallback(new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureStarted(@NonNull CameraCaptureSession session,
                            @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                        mFrameTimestampMap.put(timestamp, SystemClock.elapsedRealtimeNanos());
                    }
                });
        mPreview = previewBuilder.build();
        mCurrentStreamState = null;
        mPreview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        // Observes the stream state for the unit tests to know the preview status.
        mPreviewView.getPreviewStreamState().removeObservers(this);
        mPreviewView.getPreviewStreamState().observeForever(streamState -> {
            mCurrentStreamState = streamState;
            if (streamState == PreviewView.StreamState.STREAMING
                    && !mPreviewViewStreamingStateIdlingResource.isIdleNow()) {
                mPreviewViewStreamingStateIdlingResource.decrement();
            } else if (streamState == PreviewView.StreamState.IDLE
                    && !mPreviewViewIdleStateIdlingResource.isIdleNow()) {
                mPreviewViewIdleStateIdlingResource.decrement();
            }
        });

        FpsRecorder fpsRecorder = new FpsRecorder(10 /* sample count */);
        // Calls internal API PreviewView#setFrameUpdateListener to calculate the frame latency.
        // Remove this if you copy this sample app to your project.
        mPreviewView.setFrameUpdateListener(CameraXExecutors.directExecutor(), (timestamp) -> {
            Long frameCapturedTime = mFrameTimestampMap.remove(timestamp);
            if (frameCapturedTime == null) {
                Log.e(TAG, "Cannot find frame with timestamp: " + timestamp);
                return;
            }

            long latency = (SystemClock.elapsedRealtimeNanos() - frameCapturedTime) / 1000000L;
            double fps = fpsRecorder.recordTimestamp(SystemClock.elapsedRealtimeNanos());
            String fpsText = String.format("%1$s",
                    (Double.isNaN(fps) || Double.isInfinite(fps)) ? "---" :
                            String.format(Locale.US, "%.0f", fps));
            mFrameInfo.setText("Latency:" + latency + " ms\n" + "FPS: " + fpsText);
        });

        CameraSelector cameraSelector = mExtensionsManager.getExtensionEnabledCameraSelector(
                mCurrentCameraSelector, mCurrentExtensionMode);

        mCameraProvider.unbindAll();

        UseCaseGroup.Builder useCaseGroupBuilder =
                new UseCaseGroup.Builder()
                        .addUseCase(mPreview)
                        .addUseCase(mImageCapture);

        if (mExtensionsManager.isImageAnalysisSupported(cameraSelector,
                mCurrentExtensionMode)) {
            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
            imageAnalysis.setAnalyzer(CameraXExecutors.ioExecutor(),  img -> {
                img.close();
            });
            useCaseGroupBuilder.addUseCase(imageAnalysis);
        }

        mCamera = mCameraProvider.bindToLifecycle(this, cameraSelector,
                useCaseGroupBuilder.build());

        // Update the UI and save location for ImageCapture
        Button toggleButton = findViewById(R.id.PhotoToggle);
        String extensionModeString =
                ExtensionModeUtil.getExtensionModeStringFromId(mCurrentExtensionMode);
        toggleButton.setText(extensionModeString);

        Button captureButton = findViewById(R.id.Picture);

        Format formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US);
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "ExtensionsPictures");

        captureButton.setOnClickListener((view) -> {
            resetTakePictureIdlingResource();

            String fileName = "[" + formatter.format(Calendar.getInstance().getTime())
                    + "][CameraX]" + extensionModeString + ".jpg";
            File saveFile = new File(dir, fileName);
            ImageCapture.OutputFileOptions outputFileOptions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        "Pictures/ExtensionsPictures");
                outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues).build();
            } else {
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                outputFileOptions = new ImageCapture.OutputFileOptions.Builder(saveFile).build();
            }

            mImageCapture.takePicture(
                    outputFileOptions,
                    ContextCompat.getMainExecutor(CameraExtensionsActivity.this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(
                                @NonNull ImageCapture.OutputFileResults outputFileResults) {
                            Log.d(TAG, "Saved image to " + saveFile);

                            mLastTakePictureErrorMessage = null;

                            if (!mTakePictureIdlingResource.isIdleNow()) {
                                mTakePictureIdlingResource.decrement();
                            }

                            Uri outputUri = outputFileResults.getSavedUri();

                            if (mDeleteCapturedImage) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    try {
                                        getContentResolver().delete(outputUri, null, null);
                                    } catch (RuntimeException e) {
                                        Log.w(TAG, "Failed to delete uri: " + outputUri);
                                    }
                                } else {
                                    if (!saveFile.delete()) {
                                        Log.w(TAG, "Failed to delete file: " + saveFile);
                                    }
                                }
                            } else {
                                // Trigger MediaScanner to scan the file
                                // The output Uri is already inserted into media store if the
                                // device API level is equal to or larger than Android Q(29)."
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                    Intent intent = new Intent(
                                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                    intent.setData(Uri.fromFile(saveFile));
                                    sendBroadcast(intent);
                                }

                                Toast.makeText(CameraExtensionsActivity.this,
                                        "Saved image to " + fileName,
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "Failed to save image - " + exception.getMessage(),
                                    exception.getCause());

                            mLastTakePictureErrorMessage = getImageCaptureErrorMessage(exception);
                            if (!mTakePictureIdlingResource.isIdleNow()) {
                                mTakePictureIdlingResource.decrement();
                            }
                        }
                    });
        });

        return true;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_extensions);

        mInitializationIdlingResource.increment();

        mCurrentCameraId = getIntent().getStringExtra(INTENT_EXTRA_KEY_CAMERA_ID);
        if (mCurrentCameraId != null) {
            mCurrentCameraSelector = CameraSelectorUtil.createCameraSelectorById(mCurrentCameraId);
        }

        // Get params from adb extra string for the e2e test cases.
        String cameraDirection = getIntent().getStringExtra(INTENT_EXTRA_KEY_CAMERA_DIRECTION);
        if (cameraDirection != null) {
            if (cameraDirection.equals(BACKWARD)) {
                mCurrentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            } else if (cameraDirection.equals(FORWARD)) {
                mCurrentCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
            } else {
                throw new IllegalArgumentException(
                        "The camera " + cameraDirection + " is unavailable.");
            }
        }
        mCurrentExtensionMode = getIntent().getIntExtra(INTENT_EXTRA_KEY_EXTENSION_MODE,
                mCurrentExtensionMode);

        mDeleteCapturedImage = getIntent().getBooleanExtra(INTENT_EXTRA_KEY_DELETE_CAPTURED_IMAGE,
                mDeleteCapturedImage);

        StrictMode.VmPolicy policy =
                new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setVmPolicy(policy);
        ViewStub viewFinderStub = findViewById(R.id.viewFinderStub);
        viewFinderStub.setLayoutResource(R.layout.full_previewview);
        mPreviewView = (PreviewView) viewFinderStub.inflate();
        mFrameInfo = findViewById(R.id.frameInfo);
        mPreviewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        setupPinchToZoomAndTapToFocus(mPreviewView);
        Futures.addCallback(setupPermissions(), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@Nullable Boolean result) {
                mPermissionsGranted = Preconditions.checkNotNull(result);

                if (!mPermissionsGranted) {
                    Log.d(TAG, "Required permissions are not all granted!");
                    Toast.makeText(CameraExtensionsActivity.this, "Required permissions are not "
                            + "all granted!", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                        ProcessCameraProvider.getInstance(CameraExtensionsActivity.this);

                Futures.addCallback(cameraProviderFuture,
                        new FutureCallback<ProcessCameraProvider>() {
                            @Override
                            public void onSuccess(@Nullable ProcessCameraProvider result) {
                                mCameraProvider = result;
                                setupCamera();
                            }

                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                throw new RuntimeException("Failed to get camera provider", t);
                            }
                        }, ContextCompat.getMainExecutor(CameraExtensionsActivity.this));
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                throw new RuntimeException("Failed to get permissions", t);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public boolean onCreateOptionsMenu(@Nullable Menu menu) {
        if (menu != null) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_menu, menu);

            // Remove Camera2Extensions implementation entry if the device API level is less than 32
            if (Build.VERSION.SDK_INT < 31) {
                menu.removeItem(R.id.menu_camera2_extensions);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        switch (item.getItemId()) {
            case R.id.menu_camera2_extensions:
                if (Build.VERSION.SDK_INT >= 31) {
                    mCameraProvider.unbindAll();
                    intent.setClassName(this, Camera2ExtensionsActivity.class.getName());
                    startActivity(intent);
                    finish();
                }
                return true;
            case R.id.menu_validation_tool:
                intent.setClassName(this, CameraValidationResultActivity.class.getName());
                startActivity(intent);
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void setupCamera() {
        if (!mPermissionsGranted) {
            Log.d(TAG, "Permissions denied.");
            return;
        }

        mCamera = mCameraProvider.bindToLifecycle(this, mCurrentCameraSelector);
        ListenableFuture<ExtensionsManager> extensionsManagerFuture =
                ExtensionsManager.getInstanceAsync(getApplicationContext(), mCameraProvider);

        Futures.addCallback(extensionsManagerFuture,
                new FutureCallback<ExtensionsManager>() {
                    @Override
                    public void onSuccess(@Nullable ExtensionsManager extensionsManager) {
                        // There might be timing issue that the activity has been destroyed when
                        // the onSuccess callback is received. Skips the afterward flow when the
                        // situation happens.
                        if (CameraExtensionsActivity.this.getLifecycle().getCurrentState()
                                == Lifecycle.State.DESTROYED) {
                            return;
                        }
                        mExtensionsManager = extensionsManager;
                        if (!bindUseCasesWithCurrentExtensionMode()) {
                            bindUseCasesWithNextExtensionMode();
                        }
                        setupButtons();
                        mInitializationIdlingResource.decrement();
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {
                    }
                },
                ContextCompat.getMainExecutor(CameraExtensionsActivity.this)
        );
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

                    ListenableFuture<Void> listenableFuture = cameraControl.setZoomRatio(
                            clampedNewZoom);
                    Futures.addCallback(listenableFuture, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                            Log.d(TAG, "setZoomRatio onSuccess: " + clampedNewZoom);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Log.d(TAG, "setZoomRatio failed, " + t);
                        }
                    }, ContextCompat.getMainExecutor(CameraExtensionsActivity.this));
                    return true;
                }
            };

    private void setupPinchToZoomAndTapToFocus(PreviewView previewView) {
        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this, mScaleGestureListener);

        previewView.setOnTouchListener((view, motionEvent) -> {
            scaleDetector.onTouchEvent(motionEvent);

            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                if (mCamera == null) {
                    return true;
                }
                MeteringPoint point =
                        previewView.getMeteringPointFactory().createPoint(
                                motionEvent.getX(), motionEvent.getY());

                Futures.addCallback(
                        mCamera.getCameraControl().startFocusAndMetering(
                                new FocusMeteringAction.Builder(point).build()),
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
                        ContextCompat.getMainExecutor(CameraExtensionsActivity.this));
            }
            return true;
        });
    }

    private ListenableFuture<Boolean> setupPermissions() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            mPermissionCompleter = completer;
            if (!allPermissionsGranted()) {
                makePermissionRequest();
            } else {
                mPermissionCompleter.set(true);
            }

            return "get_permissions";
        });
    }

    private void makePermissionRequest() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERMISSIONS_REQUEST_CODE);
    }

    /** Returns true if all the necessary permissions have been granted already. */
    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /** Tries to acquire all the necessary permissions through a dialog. */
    @SuppressWarnings("deprecation")
    private String[] getRequiredPermissions() {
        PackageInfo info;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException exception) {
            Log.e(TAG, "Failed to obtain all required permissions.", exception);
            return new String[0];
        }

        if (info.requestedPermissions == null || info.requestedPermissions.length == 0) {
            return new String[0];
        }

        List<String> requiredPermissions = new ArrayList<>();

        // From Android T, skips the permission check of WRITE_EXTERNAL_STORAGE since it won't be
        // granted any more. When querying the requested permissions from PackageManager,
        // READ_EXTERNAL_STORAGE will also be included if we specify WRITE_EXTERNAL_STORAGE
        // requirement in AndroidManifest.xml. Therefore, also need to skip the permission check
        // of READ_EXTERNAL_STORAGE.
        for (String permission : info.requestedPermissions) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (
                    Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)
                            || Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission))) {
                continue;
            }

            requiredPermissions.add(permission);
        }

        String[] permissions = requiredPermissions.toArray(new String[0]);

        if (permissions.length > 0) {
            return permissions;
        } else {
            return new String[0];
        }
    }

    @Nullable
    public Preview getPreview() {
        return mPreview;
    }

    @Nullable
    public ImageCapture getImageCapture() {
        return mImageCapture;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != PERMISSIONS_REQUEST_CODE) {
            return;
        }

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length == 0) {
            mPermissionCompleter.set(false);
            return;
        }

        boolean allPermissionGranted = true;

        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                allPermissionGranted = false;
                break;
            }
        }

        Log.d(TAG, "All permissions granted: " + allPermissionGranted);
        mPermissionCompleter.set(allPermissionGranted);
    }

    @ExtensionMode.Mode
    private int getNextExtensionMode(@ExtensionMode.Mode int extensionMode) {
        switch (extensionMode) {
            case ExtensionMode.NONE:
                return ExtensionMode.BOKEH;
            case ExtensionMode.BOKEH:
                return ExtensionMode.HDR;
            case ExtensionMode.HDR:
                return ExtensionMode.NIGHT;
            case ExtensionMode.NIGHT:
                return ExtensionMode.FACE_RETOUCH;
            case ExtensionMode.FACE_RETOUCH:
                return ExtensionMode.AUTO;
            case ExtensionMode.AUTO:
                return ExtensionMode.NONE;
            default:
                throw new IllegalStateException("Unexpected value: " + extensionMode);
        }
    }

    @VisibleForTesting
    boolean isExtensionModeSupported(@NonNull String cameraId, @ExtensionMode.Mode int mode) {
        CameraSelector cameraSelector = CameraSelectorUtil.createCameraSelectorById(cameraId);

        return mExtensionsManager.isExtensionAvailable(cameraSelector, mode);
    }

    @VisibleForTesting
    boolean isExtensionModeSupported(@NonNull CameraSelector cameraSelector,
            @ExtensionMode.Mode int mode) {
        return mExtensionsManager.isExtensionAvailable(cameraSelector, mode);
    }

    @VisibleForTesting
    @ExtensionMode.Mode
    public int getCurrentExtensionMode() {
        return mCurrentExtensionMode;
    }

    @VisibleForTesting
    @NonNull
    public CountingIdlingResource getInitializationIdlingResource() {
        return mInitializationIdlingResource;
    }

    @VisibleForTesting
    @NonNull
    public CountingIdlingResource getPreviewViewStreamingStateIdlingResource() {
        return mPreviewViewStreamingStateIdlingResource;
    }

    @VisibleForTesting
    @NonNull
    public CountingIdlingResource getPreviewViewIdleStateIdlingResource() {
        return mPreviewViewIdleStateIdlingResource;
    }

    @VisibleForTesting
    @NonNull
    public CountingIdlingResource getTakePictureIdlingResource() {
        return mTakePictureIdlingResource;
    }

    @VisibleForTesting
    public void resetPreviewViewStreamingStateIdlingResource() {
        if (mPreviewViewStreamingStateIdlingResource.isIdleNow()) {
            mPreviewViewStreamingStateIdlingResource.increment();
        }
    }

    @VisibleForTesting
    public void resetPreviewViewIdleStateIdlingResource() {
        if (mPreviewViewIdleStateIdlingResource.isIdleNow()) {
            mPreviewViewIdleStateIdlingResource.increment();
        }
    }

    @VisibleForTesting
    void resetTakePictureIdlingResource() {
        if (mTakePictureIdlingResource.isIdleNow()) {
            mTakePictureIdlingResource.increment();
        }
    }

    /**
     * Returns the error message of the last take picture action if any error occurs. Returns
     * null if no error occurs.
     */
    @VisibleForTesting
    @Nullable
    public String getLastTakePictureErrorMessage() {
        return mLastTakePictureErrorMessage;
    }

    /**
     * Returns current stream state value.
     */
    @VisibleForTesting
    @Nullable
    public PreviewView.StreamState getCurrentStreamState() {
        return mCurrentStreamState;
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
}
