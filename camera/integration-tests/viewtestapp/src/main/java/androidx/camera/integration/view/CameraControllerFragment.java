/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.view;

import static androidx.camera.core.impl.utils.TransformUtils.getRectToRect;
import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Logger;
import androidx.camera.core.ZoomState;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.camera.view.RotationProvider;
import androidx.camera.view.video.AudioConfig;
import androidx.camera.view.video.ExperimentalVideo;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link Fragment} for testing {@link LifecycleCameraController}.
 */
@SuppressLint("RestrictedAPI")
public class CameraControllerFragment extends Fragment {

    private static final String TAG = "CameraCtrlFragment";

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    LifecycleCameraController mCameraController;

    @VisibleForTesting
    PreviewView mPreviewView;
    private FrameLayout mContainer;
    private Button mFlashMode;
    private ToggleButton mCameraToggle;
    private ExecutorService mExecutorService;
    private ToggleButton mCaptureEnabledToggle;
    private ToggleButton mAnalysisEnabledToggle;
    private ToggleButton mVideoEnabledToggle;
    private ToggleButton mPinchToZoomToggle;
    private ToggleButton mTapToFocusToggle;
    private TextView mZoomStateText;
    private TextView mFocusResultText;
    private TextView mTorchStateText;
    private TextView mLuminance;
    private CheckBox mOnDisk;
    private boolean mIsAnalyzerSet = true;
    // Listen to accelerometer rotation change and pass it to tests.
    private RotationProvider mRotationProvider;
    private int mRotation;
    private final RotationProvider.Listener mRotationListener = rotation -> mRotation = rotation;
    @Nullable
    private Recording mActiveRecording = null;
    private final Consumer<VideoRecordEvent> mVideoRecordEventListener = videoRecordEvent -> {
        if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
            VideoRecordEvent.Finalize finalize = (VideoRecordEvent.Finalize) videoRecordEvent;
            Uri uri = finalize.getOutputResults().getOutputUri();

            if (finalize.getError() == ERROR_NONE) {
                toast("Video saved to: " + uri);
            } else {
                String msg = "Saved uri " + uri;
                msg += " with code (" + finalize.getError() + ")";
                toast("Failed to save video: " + msg);
            }
        }
    };

    // Wrapped analyzer for tests to receive callbacks.
    @Nullable
    private ImageAnalysis.Analyzer mWrappedAnalyzer;

    private final ImageAnalysis.Analyzer mAnalyzer = image -> {
        byte[] bytes = new byte[image.getPlanes()[0].getBuffer().remaining()];
        image.getPlanes()[0].getBuffer().get(bytes);
        int total = 0;
        for (byte value : bytes) {
            total += value & 0xFF;
        }
        if (bytes.length != 0) {
            final int luminance = total / bytes.length;
            mLuminance.post(() -> mLuminance.setText(String.valueOf(luminance)));
        }
        // Forward the call to wrapped analyzer if set.
        if (mWrappedAnalyzer != null) {
            mWrappedAnalyzer.analyze(image);
        }
        image.close();
    };

    @NonNull
    private MediaStoreOutputOptions getNewVideoOutputMediaStoreOptions() {
        String videoFileName = "video_" + System.currentTimeMillis();
        ContentResolver resolver = requireContext().getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.TITLE, videoFileName);
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName);
        return new MediaStoreOutputOptions
                .Builder(resolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build();
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @NonNull
    @Override
    @OptIn(markerClass = ExperimentalVideo.class)
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mExecutorService = Executors.newSingleThreadExecutor();
        mRotationProvider = new RotationProvider(requireContext());
        boolean canDetectRotation = mRotationProvider.addListener(
                mainThreadExecutor(), mRotationListener);
        if (!canDetectRotation) {
            Logger.e(TAG, "The device cannot detect rotation with motion sensor.");
        }
        mCameraController = new LifecycleCameraController(requireContext());
        checkFailedFuture(mCameraController.getInitializationFuture());
        runSafely(() -> mCameraController.bindToLifecycle(getViewLifecycleOwner()));


        View view = inflater.inflate(R.layout.camera_controller_view, container, false);
        mPreviewView = view.findViewById(R.id.preview_view);
        // Use compatible mode so StreamState is accurate.
        mPreviewView.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        mPreviewView.setController(mCameraController);

        // Set up the button to add and remove the PreviewView
        mContainer = view.findViewById(R.id.container);
        view.findViewById(R.id.remove_or_add).setOnClickListener(v -> {
            if (mContainer.getChildCount() == 0) {
                mContainer.addView(mPreviewView);
            } else {
                mContainer.removeView(mPreviewView);
            }
        });

        // Set up the button to change the PreviewView's size.
        view.findViewById(R.id.shrink).setOnClickListener(v -> {
            // Shrinks PreviewView by 10% each time it's clicked.
            mPreviewView.setLayoutParams(new FrameLayout.LayoutParams(mPreviewView.getWidth(),
                    (int) (mPreviewView.getHeight() * 0.9)));
        });

        // Set up the front/back camera toggle.
        mCameraToggle = view.findViewById(R.id.camera_toggle);
        mCameraToggle.setOnCheckedChangeListener(
                (compoundButton, value) ->
                        runSafely(() -> mCameraController.setCameraSelector(value
                                ? CameraSelector.DEFAULT_BACK_CAMERA
                                : CameraSelector.DEFAULT_FRONT_CAMERA)));

        // Image Capture enable switch.
        mCaptureEnabledToggle = view.findViewById(R.id.capture_enabled);
        mCaptureEnabledToggle.setOnCheckedChangeListener(this::onUseCaseToggled);
        mCaptureEnabledToggle.setChecked(mCameraController.isImageCaptureEnabled());

        // Flash mode for image capture.
        mFlashMode = view.findViewById(R.id.flash_mode);
        mFlashMode.setOnClickListener(v -> {
            switch (mCameraController.getImageCaptureFlashMode()) {
                case ImageCapture.FLASH_MODE_AUTO:
                    mCameraController.setImageCaptureFlashMode(ImageCapture.FLASH_MODE_ON);
                    break;
                case ImageCapture.FLASH_MODE_ON:
                    mCameraController.setImageCaptureFlashMode(ImageCapture.FLASH_MODE_OFF);
                    break;
                case ImageCapture.FLASH_MODE_OFF:
                    mCameraController.setImageCaptureFlashMode(ImageCapture.FLASH_MODE_AUTO);
                    break;
                default:
                    throw new IllegalStateException("Invalid flash mode: "
                            + mCameraController.getImageCaptureFlashMode());
            }
            updateUiText();
        });

        mOnDisk = view.findViewById(R.id.on_disk);
        // Take picture button.
        view.findViewById(R.id.capture).setOnClickListener(v -> takePicture());

        // Set up analysis UI.
        mAnalysisEnabledToggle = view.findViewById(R.id.analysis_enabled);
        mAnalysisEnabledToggle.setOnCheckedChangeListener(
                this::onUseCaseToggled);
        mAnalysisEnabledToggle.setChecked(mCameraController.isImageAnalysisEnabled());

        ToggleButton analyzerSet = view.findViewById(R.id.analyzer_set);
        analyzerSet.setOnCheckedChangeListener(
                (compoundButton, value) -> {
                    mIsAnalyzerSet = value;
                    updateControllerAnalyzer();
                });
        analyzerSet.setChecked(mIsAnalyzerSet);
        updateControllerAnalyzer();

        mLuminance = view.findViewById(R.id.luminance);

        // Set up video UI.
        mVideoEnabledToggle = view.findViewById(R.id.video_enabled);
        mVideoEnabledToggle.setOnCheckedChangeListener(
                (compoundButton, checked) -> {
                    onUseCaseToggled(compoundButton, checked);
                    updateUiText();
                });

        view.findViewById(R.id.video_record).setOnClickListener(v -> {
            try {
                startRecording(mVideoRecordEventListener);
            } catch (RuntimeException exception) {
                toast("Failed to record video: " + exception.getMessage());
            }
            updateUiText();
        });
        view.findViewById(R.id.video_stop_recording).setOnClickListener(
                v -> {
                    stopRecording();
                    updateUiText();
                });

        mPinchToZoomToggle = view.findViewById(R.id.pinch_to_zoom_toggle);
        mPinchToZoomToggle.setOnCheckedChangeListener(
                (compoundButton, checked) -> mCameraController.setPinchToZoomEnabled(checked));

        mTapToFocusToggle = view.findViewById(R.id.tap_to_focus_toggle);
        mTapToFocusToggle.setOnCheckedChangeListener(
                (compoundButton, checked) -> mCameraController.setTapToFocusEnabled(checked));

        ((ToggleButton) view.findViewById(R.id.torch_toggle)).setOnCheckedChangeListener(
                (compoundButton, checked) -> checkFailedFuture(
                        mCameraController.enableTorch(checked)));

        ((SeekBar) view.findViewById(R.id.linear_zoom_slider)).setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                        checkFailedFuture(mCameraController.setLinearZoom(
                                (float) progress / seekBar.getMax()));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

        mZoomStateText = view.findViewById(R.id.zoom_state_text);
        updateZoomStateText(mCameraController.getZoomState().getValue());
        mCameraController.getZoomState().observe(getViewLifecycleOwner(),
                this::updateZoomStateText);

        mFocusResultText = view.findViewById(R.id.focus_result_text);
        LiveData<Integer> focusMeteringResult =
                mCameraController.getTapToFocusState();
        updateFocusStateText(Objects.requireNonNull(focusMeteringResult.getValue()));
        focusMeteringResult.observe(getViewLifecycleOwner(),
                this::updateFocusStateText);

        mTorchStateText = view.findViewById(R.id.torch_state_text);
        updateTorchStateText(mCameraController.getTorchState().getValue());
        mCameraController.getTorchState().observe(getViewLifecycleOwner(),
                this::updateTorchStateText);

        updateUiText();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mExecutorService != null) {
            mExecutorService.shutdown();
        }
        mRotationProvider.removeListener(mRotationListener);
    }

    void checkFailedFuture(ListenableFuture<Void> voidFuture) {
        Futures.addCallback(voidFuture, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {

            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                toast(t.getMessage());
            }
        }, mainThreadExecutor());
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    void toast(String message) {
        requireActivity().runOnUiThread(
                () -> Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
    }

    private void updateZoomStateText(@Nullable ZoomState zoomState) {
        if (zoomState == null) {
            mZoomStateText.setText("Null");
        } else {
            mZoomStateText.setText(zoomState.toString());
        }
    }

    private void updateFocusStateText(@NonNull Integer tapToFocusState) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String text = "";
        switch (tapToFocusState) {
            case CameraController.TAP_TO_FOCUS_NOT_STARTED:
                text = "not started";
                break;
            case CameraController.TAP_TO_FOCUS_STARTED:
                text = "started";
                break;
            case CameraController.TAP_TO_FOCUS_FOCUSED:
                text = "successful";
                break;
            case CameraController.TAP_TO_FOCUS_NOT_FOCUSED:
                text = "unsuccessful";
                break;
            case CameraController.TAP_TO_FOCUS_FAILED:
                text = "failed";
                break;
        }
        mFocusResultText.setText(
                "Focus state: " + text + " time: " + dateFormat.format(new Date()));
    }

    private void updateTorchStateText(@Nullable Integer torchState) {
        if (torchState == null) {
            mTorchStateText.setText("Torch state null");
        } else {
            mTorchStateText.setText("Torch state: " + torchState);
        }
    }

    /**
     * Updates UI text based on the state of {@link #mCameraController}.
     */
    @OptIn(markerClass = ExperimentalVideo.class)
    private void updateUiText() {
        mFlashMode.setText(getFlashModeTextResId());
        final Integer lensFacing = mCameraController.getCameraSelector().getLensFacing();
        mCameraToggle.setChecked(
                lensFacing != null && lensFacing == CameraSelector.LENS_FACING_BACK);
        mVideoEnabledToggle.setChecked(mCameraController.isVideoCaptureEnabled());
        mPinchToZoomToggle.setChecked(mCameraController.isPinchToZoomEnabled());
        mTapToFocusToggle.setChecked(mCameraController.isTapToFocusEnabled());
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

    private int getFlashModeTextResId() {
        switch (mCameraController.getImageCaptureFlashMode()) {
            case ImageCapture.FLASH_MODE_AUTO:
                return R.string.flash_mode_auto;
            case ImageCapture.FLASH_MODE_ON:
                return R.string.flash_mode_on;
            case ImageCapture.FLASH_MODE_OFF:
                return R.string.flash_mode_off;
            default:
                throw new IllegalStateException("Invalid flash mode: "
                        + mCameraController.getImageCaptureFlashMode());
        }
    }

    /**
     * Sets or clears analyzer based on current state.
     */
    private void updateControllerAnalyzer() {
        if (mIsAnalyzerSet) {
            mCameraController.setImageAnalysisAnalyzer(mExecutorService, mAnalyzer);
        } else {
            mCameraController.clearImageAnalysisAnalyzer();
        }
    }

    /**
     * Executes the runnable and catches {@link IllegalStateException}.
     */
    private void runSafely(@NonNull Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalStateException ex) {
            toast("Failed to bind use cases.");
        }
    }

    @OptIn(markerClass = ExperimentalVideo.class)
    private void onUseCaseToggled(CompoundButton compoundButton, boolean value) {
        if (mCaptureEnabledToggle == null || mAnalysisEnabledToggle == null
                || mVideoEnabledToggle == null) {
            return;
        }
        int useCaseEnabledFlags = 0;
        if (mCaptureEnabledToggle.isChecked()) {
            useCaseEnabledFlags = useCaseEnabledFlags | CameraController.IMAGE_CAPTURE;
        }
        if (mAnalysisEnabledToggle.isChecked()) {
            useCaseEnabledFlags = useCaseEnabledFlags | CameraController.IMAGE_ANALYSIS;
        }
        if (mVideoEnabledToggle.isChecked()) {
            useCaseEnabledFlags = useCaseEnabledFlags | CameraController.VIDEO_CAPTURE;
        }
        final int finalUseCaseEnabledFlags = useCaseEnabledFlags;
        runSafely(() -> mCameraController.setEnabledUseCases(finalUseCaseEnabledFlags));
    }

    /**
     * Take a picture based on the current configuration.
     */
    private void takePicture() {
        try {
            if (mOnDisk.isChecked()) {
                takePicture(new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults outputFileResults) {
                        toast("Image saved to: " + outputFileResults.getSavedUri());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        toast("Failed to save picture: " + exception.getMessage());
                    }
                });
            } else {
                mCameraController.takePicture(mExecutorService,
                        new ImageCapture.OnImageCapturedCallback() {
                            @Override
                            public void onCaptureSuccess(@NonNull ImageProxy image) {
                                displayImage(image);
                            }

                            @Override
                            public void onError(@NonNull ImageCaptureException exception) {
                                toast("Failed to capture in-memory picture: "
                                        + exception.getMessage());
                            }
                        });
            }
        } catch (RuntimeException exception) {
            toast("Failed to take picture: " + exception.getMessage());
        }
    }

    /**
     * Displays a {@link ImageProxy} in a pop-up dialog.
     */
    private void displayImage(@NonNull ImageProxy image) {
        int rotationDegrees = image.getImageInfo().getRotationDegrees();
        Bitmap cropped = getCroppedBitmap(image);
        image.close();

        mainThreadExecutor().execute(() -> {
            Dialog dialog = new Dialog(requireContext());
            dialog.setContentView(R.layout.image_dialog);
            ImageView imageView = (ImageView) dialog.findViewById(R.id.dialog_image);
            imageView.setImageBitmap(cropped);
            imageView.setRotation(rotationDegrees);
            dialog.findViewById(R.id.dialog_button).setOnClickListener(view -> dialog.dismiss());
            dialog.show();
        });
    }

    /**
     * Converts the {@link ImageProxy} to {@link Bitmap} with crop rect applied.
     */
    private Bitmap getCroppedBitmap(@NonNull ImageProxy image) {
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        Rect cropRect = image.getCropRect();
        Size newSize = new Size(cropRect.width(), cropRect.height());
        Bitmap cropped = Bitmap.createBitmap(newSize.getWidth(), newSize.getHeight(),
                Bitmap.Config.ARGB_8888);

        Matrix croppingTransform = getRectToRect(new RectF(cropRect),
                new RectF(0, 0, cropRect.width(), cropRect.height()), 0);

        Canvas canvas = new Canvas(cropped);
        canvas.drawBitmap(bitmap, croppingTransform, new Paint());
        canvas.save();

        bitmap.recycle();
        return cropped;
    }

    // -----------------
    // For testing
    // -----------------

    @VisibleForTesting
    LifecycleCameraController getCameraController() {
        return mCameraController;
    }

    @VisibleForTesting
    void setWrappedAnalyzer(@Nullable ImageAnalysis.Analyzer analyzer) {
        mWrappedAnalyzer = analyzer;
    }

    @VisibleForTesting
    PreviewView getPreviewView() {
        return mPreviewView;
    }

    @VisibleForTesting
    int getSensorRotation() {
        return mRotation;
    }

    @VisibleForTesting
    void takePicture(ImageCapture.OnImageSavedCallback callback) {
        createDefaultPictureFolderIfNotExist();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(
                        requireContext().getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues).build();
        mCameraController.takePicture(outputFileOptions, mExecutorService, callback);
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @VisibleForTesting
    @MainThread
    @OptIn(markerClass = ExperimentalVideo.class)
    void startRecording(Consumer<VideoRecordEvent> listener) {
        MediaStoreOutputOptions outputOptions = getNewVideoOutputMediaStoreOptions();
        AudioConfig audioConfig = AudioConfig.create(true);
        mActiveRecording = mCameraController.startRecording(outputOptions, audioConfig,
                mExecutorService, listener);
    }

    @VisibleForTesting
    @MainThread
    @OptIn(markerClass = ExperimentalVideo.class)
    void stopRecording() {
        if (mActiveRecording != null) {
            mActiveRecording.stop();
        }
    }

}
