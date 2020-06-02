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
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
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
import android.view.Display;
import android.view.TextureView;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.TorchState;
import androidx.camera.core.UseCase;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.impl.VideoCaptureConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    static final CameraSelector BACK_SELECTOR =
            new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
    static final CameraSelector FRONT_SELECTOR =
            new CameraSelector.Builder().requireLensFacing(
                    CameraSelector.LENS_FACING_FRONT).build();

    private final AtomicLong mImageAnalysisFrameCount = new AtomicLong(0);
    private final AtomicLong mPreviewFrameCount = new AtomicLong(0);
    private final MutableLiveData<String> mImageAnalysisResult = new MutableLiveData<>();
    private VideoFileSaver mVideoFileSaver;
    /** The camera to use */
    CameraSelector mCurrentCameraSelector = BACK_SELECTOR;
    @CameraSelector.LensFacing
    int mCurrentCameraLensFacing = CameraSelector.LENS_FACING_BACK;
    ProcessCameraProvider mCameraProvider;
    private CameraXViewModel.CameraProviderResult mCameraProviderResult;

    // TODO: Move the analysis processing, capture processing to separate threads, so
    // there is smaller impact on the preview.
    private String mCurrentCameraDirection = "BACKWARD";
    private Preview mPreview;
    private ImageAnalysis mImageAnalysis;
    private ImageCapture mImageCapture;
    private ExecutorService mImageCaptureExecutorService;
    private VideoCapture mVideoCapture;
    private Camera mCamera;
    @ImageCapture.CaptureMode
    private int mCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY;

    /**
     * Intent Extra string for choosing which type of render surface to use to display Preview.
     */
    public static final String INTENT_EXTRA_RENDER_SURFACE_TYPE = "render_surface_type";
    /**
     * TextureView render surface for {@link #INTENT_EXTRA_RENDER_SURFACE_TYPE}. This is the
     * default render surface.
     */
    public static final String RENDER_SURFACE_TYPE_TEXTUREVIEW = "textureview";
    /**
     * SurfaceView render surface for {@link #INTENT_EXTRA_RENDER_SURFACE_TYPE}. This type will
     * block the main thread while detaching it's {@link android.view.Surface} from the OpenGL
     * renderer to avoid compatibility issues on some devices.
     */
    public static final String RENDER_SURFACE_TYPE_SURFACEVIEW = "surfaceview";
    /**
     * SurfaceView render surface (in non-blocking mode) for
     * {@link #INTENT_EXTRA_RENDER_SURFACE_TYPE}. This type will NOT
     * block the main thread while detaching it's {@link android.view.Surface} from the OpenGL
     * renderer, but some devices may crash due to their OpenGL/EGL implementation not being
     * thread-safe.
     */
    public static final String RENDER_SURFACE_TYPE_SURFACEVIEW_NONBLOCKING =
            "surfaceview_nonblocking";

    private OpenGLRenderer mPreviewRenderer;
    private DisplayManager.DisplayListener mDisplayListener;

    SessionImagesUriSet mSessionImagesUriSet = new SessionImagesUriSet();

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

    /**
     * Creates a view finder use case.
     *
     * <p>This use case observes a {@link SurfaceTexture}. The texture is connected to a {@link
     * TextureView} to display a camera preview.
     */
    private void createPreview() {
        Button button = this.findViewById(R.id.PreviewToggle);
        button.setBackgroundColor(Color.LTGRAY);
        enablePreview();

        button.setOnClickListener(
                view -> {
                    Button buttonView = (Button) view;
                    if (mPreview != null) {
                        // Remove the use case
                        buttonView.setBackgroundColor(Color.RED);
                        mCameraProvider.unbind(mPreview);
                        mPreview = null;
                    } else {
                        // Add the use case
                        buttonView.setBackgroundColor(Color.LTGRAY);
                        enablePreview();
                    }
                });

        Log.i(TAG, "Got UseCase: " + mPreview);
    }

    void enablePreview() {
        mPreview = new Preview.Builder()
                .setTargetName("Preview")
                .build();
        Log.d(TAG, "enablePreview");

        resetViewIdlingResource();

        mPreviewRenderer.attachInputPreview(mPreview);

        if (bindToLifecycleSafely(mPreview, R.id.PreviewToggle) == null) {
            mPreview = null;
        }
    }

    /**
     * Creates an image analysis use case.
     *
     * <p>This use case observes a stream of analysis results computed from the frames.
     */
    private void createImageAnalysis() {
        Button button = this.findViewById(R.id.AnalysisToggle);
        button.setBackgroundColor(Color.LTGRAY);
        enableImageAnalysis();

        button.setOnClickListener(
                view -> {
                    Button buttonView = (Button) view;
                    if (mImageAnalysis != null) {
                        // Remove the use case
                        buttonView.setBackgroundColor(Color.RED);
                        mCameraProvider.unbind(mImageAnalysis);
                        mImageAnalysis = null;
                    } else {
                        // Add the use case
                        buttonView.setBackgroundColor(Color.LTGRAY);
                        CameraXActivity.this.enableImageAnalysis();
                    }
                });

        Log.i(TAG, "Got UseCase: " + mImageAnalysis);
    }

    void enableImageAnalysis() {
        mImageAnalysis = new ImageAnalysis.Builder()
                .setTargetName("ImageAnalysis")
                .build();
        TextView textView = this.findViewById(R.id.textView);

        // Make the analysis idling resource non-idle, until a frame received.
        mAnalysisIdlingResource.increment();

        if (bindToLifecycleSafely(mImageAnalysis, R.id.AnalysisToggle) == null) {
            mImageAnalysis = null;
            return;
        }

        mImageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(this),
                (image) -> {
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
        );
        mImageAnalysisResult.observe(
                this,
                text -> {
                    if (mImageAnalysisFrameCount.getAndIncrement() % 30 == 0) {
                        textView.setText(
                                "ImgCount: " + mImageAnalysisFrameCount.get() + " @ts: "
                                        + text);
                    }
                });
    }

    /**
     * Creates an image capture use case.
     *
     * <p>This use case takes a picture and saves it to a file, whenever the user clicks a button.
     */
    private void createImageCapture() {

        Button button = this.findViewById(R.id.PhotoToggle);
        button.setBackgroundColor(Color.LTGRAY);
        enableImageCapture();

        button.setOnClickListener(
                view -> {
                    Button buttonView = (Button) view;
                    if (mImageCapture != null) {
                        // Remove the use case
                        buttonView.setBackgroundColor(Color.RED);
                        CameraXActivity.this.disableImageCapture();
                    } else {
                        // Add the use case
                        buttonView.setBackgroundColor(Color.LTGRAY);
                        CameraXActivity.this.enableImageCapture();
                    }
                });

        Log.i(TAG, "Got UseCase: " + mImageCapture);
    }

    void enableImageCapture() {
        mImageCaptureExecutorService = Executors.newSingleThreadExecutor();
        mImageCapture = new ImageCapture.Builder()
                .setCaptureMode(mCaptureMode)
                .setTargetName("ImageCapture")
                .build();

        Camera camera = bindToLifecycleSafely(mImageCapture, R.id.PhotoToggle);
        if (camera == null) {
            Button button = this.findViewById(R.id.Picture);
            button.setOnClickListener(null);
            mImageCapture = null;
            return;
        }

        Button button = this.findViewById(R.id.Picture);
        button.setOnClickListener(
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
                        mImageCapture.takePicture(outputFileOptions,
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

        refreshFlashButton();


        Button btnCaptureQuality = this.findViewById(R.id.capture_quality);
        btnCaptureQuality.setVisibility(View.VISIBLE);
        btnCaptureQuality.setText(
                mCaptureMode == ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY ? "MAX" : "MIN");
        btnCaptureQuality.setOnClickListener(view -> {
            mCaptureMode = (mCaptureMode == ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                    ? ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                    : ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY);
            rebindUseCases();
        });
    }

    void disableImageCapture() {
        mCameraProvider.unbind(mImageCapture);
        // Shutdown capture executor, callbacks rejected on executor will be caught by camerax-core.
        mImageCaptureExecutorService.shutdown();

        mImageCapture = null;
        Button button = this.findViewById(R.id.Picture);
        button.setOnClickListener(null);

        Button btnCaptureQuality = this.findViewById(R.id.capture_quality);
        btnCaptureQuality.setVisibility(View.GONE);

        refreshFlashButton();
    }

    private void refreshFlashButton() {
        ImageButton flashToggle = findViewById(R.id.flash_toggle);
        if (mImageCapture != null) {
            CameraInfo cameraInfo = getCameraInfo();
            if (cameraInfo != null && cameraInfo.hasFlashUnit()) {
                flashToggle.setVisibility(View.VISIBLE);
                flashToggle.setOnClickListener(v -> {
                    @ImageCapture.FlashMode int flashMode = mImageCapture.getFlashMode();
                    if (flashMode == FLASH_MODE_ON) {
                        mImageCapture.setFlashMode(FLASH_MODE_OFF);
                    } else if (flashMode == FLASH_MODE_OFF) {
                        mImageCapture.setFlashMode(FLASH_MODE_AUTO);
                    } else if (flashMode == FLASH_MODE_AUTO) {
                        mImageCapture.setFlashMode(FLASH_MODE_ON);
                    }
                    refreshFlashButtonIcon();
                });
                refreshFlashButtonIcon();
            } else {
                flashToggle.setVisibility(View.INVISIBLE);
                flashToggle.setOnClickListener(null);
            }
        } else {
            flashToggle.setVisibility(View.GONE);
            flashToggle.setOnClickListener(null);
        }
    }

    private void refreshFlashButtonIcon() {
        ImageButton flashToggle = findViewById(R.id.flash_toggle);
        @ImageCapture.FlashMode int flashMode = mImageCapture.getFlashMode();
        switch (flashMode) {
            case FLASH_MODE_ON:
                flashToggle.setImageResource(R.drawable.ic_flash_on);
                break;
            case FLASH_MODE_OFF:
                flashToggle.setImageResource(R.drawable.ic_flash_off);
                break;
            case FLASH_MODE_AUTO:
                flashToggle.setImageResource(R.drawable.ic_flash_auto);
                break;
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private void refreshTorchButton() {
        ImageButton torchToggle = findViewById(R.id.torch_toggle);
        CameraInfo cameraInfo = getCameraInfo();
        if (cameraInfo != null && cameraInfo.hasFlashUnit()) {
            torchToggle.setVisibility(View.VISIBLE);
            torchToggle.setOnClickListener(v -> {
                Integer torchState = cameraInfo.getTorchState().getValue();
                boolean toggledState = !Objects.equals(torchState, TorchState.ON);
                CameraControl cameraControl = getCameraControl();
                if (cameraControl != null) {
                    Log.d(TAG, "Set camera torch: " + toggledState);
                    ListenableFuture<Void> future = cameraControl.enableTorch(toggledState);
                    Futures.addCallback(future, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void result) {
                        }

                        @Override
                        public void onFailure(@NonNull Throwable t) {
                            // Throw the unexpected error.
                            throw new RuntimeException(t);
                        }
                    }, CameraXExecutors.directExecutor());
                }
            });
        } else {
            Log.d(TAG, "No flash unit");
            torchToggle.setVisibility(View.INVISIBLE);
            torchToggle.setOnClickListener(null);
        }
    }

    /**
     * Creates a video capture use case.
     *
     * <p>This use case records a video segment and saves it to a file, in response to user button
     * clicks.
     */
    private void createVideoCapture() {
        Button button = this.findViewById(R.id.VideoToggle);
        button.setBackgroundColor(Color.LTGRAY);
        enableVideoCapture();

        mVideoFileSaver = new VideoFileSaver();
        mVideoFileSaver.setRootDirectory(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM));

        button.setOnClickListener(
                view -> {
                    Button buttonView = (Button) view;
                    if (mVideoCapture != null) {
                        // Remove the use case
                        buttonView.setBackgroundColor(Color.RED);
                        CameraXActivity.this.disableVideoCapture();
                    } else {
                        // Add the use case
                        buttonView.setBackgroundColor(Color.LTGRAY);
                        CameraXActivity.this.enableVideoCapture();
                    }
                });

        Log.i(TAG, "Got UseCase: " + mVideoCapture);
    }

    void enableVideoCapture() {
        mVideoCapture = new VideoCaptureConfig.Builder()
                .setTargetName("VideoCapture")
                .build();

        if (bindToLifecycleSafely(mVideoCapture, R.id.VideoToggle) == null) {
            Button button = this.findViewById(R.id.Video);
            button.setOnClickListener(null);
            mVideoCapture = null;
            return;
        }

        Button button = this.findViewById(R.id.Video);
        button.setOnClickListener((view) -> {
            Button buttonView = (Button) view;
            String text = button.getText().toString();
            if (text.equals("Record") && !mVideoFileSaver.isSaving()) {
                mVideoCapture.startRecording(
                        mVideoFileSaver.getNewVideoFile(),
                        ContextCompat.getMainExecutor(CameraXActivity.this),
                        mVideoFileSaver);
                mVideoFileSaver.setSaving();
                buttonView.setText("Stop");
            } else if (text.equals("Stop") && mVideoFileSaver.isSaving()) {
                buttonView.setText("Record");
                mVideoCapture.stopRecording();
            } else if (text.equals("Record") && mVideoFileSaver.isSaving()) {
                buttonView.setText("Stop");
                mVideoFileSaver.setSaving();
            } else if (text.equals("Stop") && !mVideoFileSaver.isSaving()) {
                buttonView.setText("Record");
            }
        });
    }

    void disableVideoCapture() {
        Button button = this.findViewById(R.id.Video);
        button.setOnClickListener(null);
        mCameraProvider.unbind(mVideoCapture);

        mVideoCapture = null;
    }

    /** Creates all the use cases. */
    private void createUseCases() {
        createImageCapture();
        createPreview();
        createImageAnalysis();
        createVideoCapture();
    }

    private View chooseViewFinder(@NonNull ViewStub viewFinderStub,
            @NonNull OpenGLRenderer renderer) {
        Bundle bundle = getIntent().getExtras();
        // By default we choose TextureView to maximize compatibility.
        String renderSurfaceType = RENDER_SURFACE_TYPE_TEXTUREVIEW;
        if (bundle != null) {
            renderSurfaceType = bundle.getString(INTENT_EXTRA_RENDER_SURFACE_TYPE,
                    RENDER_SURFACE_TYPE_TEXTUREVIEW);
        }

        switch (renderSurfaceType) {
            case RENDER_SURFACE_TYPE_TEXTUREVIEW:
                Log.d(TAG, "Using TextureView render surface.");
                return TextureViewRenderSurface.inflateWith(viewFinderStub, renderer);
            case RENDER_SURFACE_TYPE_SURFACEVIEW:
                Log.d(TAG, "Using SurfaceView render surface.");
                return SurfaceViewRenderSurface.inflateWith(viewFinderStub, renderer);
            case RENDER_SURFACE_TYPE_SURFACEVIEW_NONBLOCKING:
                Log.d(TAG, "Using SurfaceView (non-blocking) render surface.");
                return SurfaceViewRenderSurface.inflateNonBlockingWith(viewFinderStub, renderer);
            default:
                throw new IllegalArgumentException(String.format(Locale.US, "Unknown render "
                        + "surface type: %s. Supported surface types include: [%s, %s, %s]",
                        renderSurfaceType, RENDER_SURFACE_TYPE_TEXTUREVIEW,
                        RENDER_SURFACE_TYPE_SURFACEVIEW,
                        RENDER_SURFACE_TYPE_SURFACEVIEW_NONBLOCKING));
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_xmain);
        OpenGLRenderer previewRenderer = mPreviewRenderer = new OpenGLRenderer();
        ViewStub viewFinderStub = findViewById(R.id.viewFinderStub);
        View viewFinder = chooseViewFinder(viewFinderStub, previewRenderer);

        mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {

            }

            @Override
            public void onDisplayRemoved(int displayId) {

            }

            @Override
            public void onDisplayChanged(int displayId) {
                Display viewFinderDisplay = viewFinder.getDisplay();
                if (viewFinderDisplay != null && viewFinderDisplay.getDisplayId() == displayId) {
                    previewRenderer.invalidateSurface(
                            Surfaces.toSurfaceRotationDegrees(viewFinderDisplay.getRotation()));
                }
            }
        };

        DisplayManager dpyMgr =
                Objects.requireNonNull((DisplayManager) getSystemService(Context.DISPLAY_SERVICE));
        dpyMgr.registerDisplayListener(mDisplayListener, new Handler(Looper.getMainLooper()));

        previewRenderer.setFrameUpdateListener(ContextCompat.getMainExecutor(this), timestamp -> {
            // Wait until surface texture receives enough updates. This is for testing.
            if (mPreviewFrameCount.getAndIncrement() >= FRAMES_UNTIL_VIEW_IS_READY) {
                try {
                    if (!mViewIdlingResource.isIdleNow()) {
                        Log.d(TAG, FRAMES_UNTIL_VIEW_IS_READY + " or more counted on preview."
                                + " Make IdlingResource idle.");
                        mViewIdlingResource.decrement();
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Unexpected decrement. Continuing");
                }
            }
        });

        StrictMode.VmPolicy policy =
                new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setVmPolicy(policy);

        // Get params from adb extra string
        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            String newCameraDirection = bundle.getString(INTENT_EXTRA_CAMERA_DIRECTION);
            if (newCameraDirection != null) {
                mCurrentCameraDirection = newCameraDirection;
            }
        }

        mInitializationIdlingResource.increment();
        CameraXViewModel viewModel = new ViewModelProvider(this).get(CameraXViewModel.class);
        viewModel.getCameraProvider().observe(this, cameraProviderResult -> {
            mCameraProviderResult = cameraProviderResult;
            mInitializationIdlingResource.decrement();
            if (cameraProviderResult.hasProvider()) {
                mCameraProvider = cameraProviderResult.getProvider();
                if (allPermissionsGranted()) {
                    setupCamera();
                }
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
    }

    void setupCamera() {
        // Only call setupCamera if permissions are granted
        Preconditions.checkState(allPermissionsGranted());

        Log.d(TAG, "Camera direction: " + mCurrentCameraDirection);
        if (mCurrentCameraDirection.equalsIgnoreCase("BACKWARD")) {
            mCurrentCameraSelector = BACK_SELECTOR;
            mCurrentCameraLensFacing = CameraSelector.LENS_FACING_BACK;
        } else if (mCurrentCameraDirection.equalsIgnoreCase("FORWARD")) {
            mCurrentCameraSelector = FRONT_SELECTOR;
            mCurrentCameraLensFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            throw new RuntimeException("Invalid camera direction: " + mCurrentCameraDirection);
        }
        Log.d(TAG, "Using camera lens facing: " + mCurrentCameraSelector);

        CameraXActivity.this.createUseCases();
        refreshTorchButton();

        ImageButton directionToggle = findViewById(R.id.direction_toggle);
        directionToggle.setVisibility(View.VISIBLE);
        directionToggle.setOnClickListener(v -> {
            if (mCurrentCameraLensFacing == CameraSelector.LENS_FACING_BACK) {
                mCurrentCameraSelector = FRONT_SELECTOR;
                mCurrentCameraLensFacing = CameraSelector.LENS_FACING_FRONT;
            } else if (mCurrentCameraLensFacing == CameraSelector.LENS_FACING_FRONT) {
                mCurrentCameraSelector = BACK_SELECTOR;
                mCurrentCameraLensFacing = CameraSelector.LENS_FACING_BACK;
            }

            Log.d(TAG, "Change camera direction: " + mCurrentCameraSelector);
            rebindUseCases();
            refreshTorchButton();

        });
    }

    private void rebindUseCases() {
        // Rebind all use cases.
        mCameraProvider.unbindAll();
        if (mImageCapture != null) {
            enableImageCapture();
        }
        if (mPreview != null) {
            enablePreview();
        }
        if (mImageAnalysis != null) {
            enableImageAnalysis();
        }
        if (mVideoCapture != null) {
            enableVideoCapture();
        }
    }

    private void setupPermissions() {
        if (!allPermissionsGranted()) {
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

                                // All permissions granted.
                                if (mCameraProvider != null) {
                                    setupCamera();
                                }
                            });

            permissionLauncher.launch(REQUIRED_PERMISSIONS);
        } else if (mCameraProvider != null) {
            // Permissions already granted. Start camera.
            setupCamera();
        }
    }

    /** Returns true if all the necessary permissions have been granted already. */
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
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

    @Nullable
    private Camera bindToLifecycleSafely(UseCase useCase, int buttonViewId) {
        try {
            mCamera = mCameraProvider.bindToLifecycle(this, mCurrentCameraSelector,
                    useCase);
            return mCamera;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "bindToLifecycle() failed.", e);
            Toast.makeText(getApplicationContext(), "Bind too many use cases.", Toast.LENGTH_SHORT)
                    .show();
            Button button = this.findViewById(buttonViewId);
            button.setBackgroundColor(Color.RED);
        }
        return null;
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

    Preview getPreview() {
        return mPreview;
    }

    ImageAnalysis getImageAnalysis() {
        return mImageAnalysis;
    }

    ImageCapture getImageCapture() {
        return mImageCapture;
    }

    VideoCapture getVideoCapture() {
        return mVideoCapture;
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
