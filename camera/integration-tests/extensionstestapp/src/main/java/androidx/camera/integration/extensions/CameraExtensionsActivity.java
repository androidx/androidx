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

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.camera2.CameraCaptureSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.camera.extensions.AutoImageCaptureExtender;
import androidx.camera.extensions.AutoPreviewExtender;
import androidx.camera.extensions.BeautyImageCaptureExtender;
import androidx.camera.extensions.BeautyPreviewExtender;
import androidx.camera.extensions.BokehImageCaptureExtender;
import androidx.camera.extensions.BokehPreviewExtender;
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.extensions.HdrPreviewExtender;
import androidx.camera.extensions.NightImageCaptureExtender;
import androidx.camera.extensions.NightPreviewExtender;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** An activity that shows off how extensions can be applied */
public class CameraExtensionsActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "CameraExtensionActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 42;

    private static final CameraSelector CAMERA_SELECTOR =
            new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

    boolean mPermissionsGranted = false;
    private CallbackToFutureAdapter.Completer<Boolean> mPermissionCompleter;

    @Nullable
    private Preview mPreview;

    @Nullable
    private ImageCapture mImageCapture;

    @NonNull
    private ImageCaptureType mCurrentImageCaptureType = ImageCaptureType.IMAGE_CAPTURE_TYPE_HDR;

    // Espresso testing variables
    @VisibleForTesting
    CountingIdlingResource mTakePictureIdlingResource = new CountingIdlingResource("TakePicture");
    @Nullable
    private CountDownLatch mPreviewCaptureSessionConfigured = new CountDownLatch(1);

    private PreviewView mPreviewView;

    ProcessCameraProvider mCameraProvider;

    /**
     * Creates a preview use case.
     *
     * <p>This use case uses a {@link PreviewView} to display a camera preview.
     */
    private void createPreview() {
        enablePreview();
        Log.i(TAG, "Got UseCase: " + mPreview);
    }

    void enablePreview() {
        if (mPreview != null) {
            mCameraProvider.unbind(mPreview);
        }

        Preview.Builder builder = new Preview.Builder()
                .setTargetName("Preview");

        Log.d(TAG, "Enabling the extended preview");
        if (mCurrentImageCaptureType == ImageCaptureType.IMAGE_CAPTURE_TYPE_BOKEH) {
            Log.d(TAG, "Enabling the extended preview in bokeh mode.");

            BokehPreviewExtender extender = BokehPreviewExtender.create(builder);
            if (extender.isExtensionAvailable(CAMERA_SELECTOR)) {
                extender.enableExtension(CAMERA_SELECTOR);
            }
        } else if (mCurrentImageCaptureType == ImageCaptureType.IMAGE_CAPTURE_TYPE_HDR) {
            Log.d(TAG, "Enabling the extended preview in HDR mode.");

            HdrPreviewExtender extender = HdrPreviewExtender.create(builder);
            if (extender.isExtensionAvailable(CAMERA_SELECTOR)) {
                extender.enableExtension(CAMERA_SELECTOR);
            }
        } else if (mCurrentImageCaptureType == ImageCaptureType.IMAGE_CAPTURE_TYPE_NIGHT) {
            Log.d(TAG, "Enabling the extended preview in night mode.");

            NightPreviewExtender extender = NightPreviewExtender.create(builder);
            if (extender.isExtensionAvailable(CAMERA_SELECTOR)) {
                extender.enableExtension(CAMERA_SELECTOR);
            }
        } else if (mCurrentImageCaptureType == ImageCaptureType.IMAGE_CAPTURE_TYPE_BEAUTY) {
            Log.d(TAG, "Enabling the extended preview in beauty mode.");

            BeautyPreviewExtender extender = BeautyPreviewExtender.create(builder);
            if (extender.isExtensionAvailable(CAMERA_SELECTOR)) {
                extender.enableExtension(CAMERA_SELECTOR);
            }
        } else if (mCurrentImageCaptureType == ImageCaptureType.IMAGE_CAPTURE_TYPE_AUTO) {
            Log.d(TAG, "Enabling the extended preview in auto mode.");

            AutoPreviewExtender extender = AutoPreviewExtender.create(builder);
            if (extender.isExtensionAvailable(CAMERA_SELECTOR)) {
                extender.enableExtension(CAMERA_SELECTOR);
            }
        }
        // Recreate the CountDown before create the Preview.
        mPreviewCaptureSessionConfigured = new CountDownLatch(1);
        new Camera2Interop.Extender<>(builder).setSessionStateCallback(
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        mPreviewCaptureSessionConfigured.countDown();
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                    }
                });
        mPreview = builder.build();
        mPreview.setSurfaceProvider(mPreviewView.createSurfaceProvider());
    }

    enum ImageCaptureType {
        IMAGE_CAPTURE_TYPE_HDR,
        IMAGE_CAPTURE_TYPE_BOKEH,
        IMAGE_CAPTURE_TYPE_NIGHT,
        IMAGE_CAPTURE_TYPE_BEAUTY,
        IMAGE_CAPTURE_TYPE_AUTO,
        IMAGE_CAPTURE_TYPE_DEFAULT,
        IMAGE_CAPTURE_TYPE_NONE,
    }

    /**
     * Creates an image capture use case.
     *
     * <p>This use case takes a picture and saves it to a file, whenever the user clicks a button.
     */
    private void createImageCapture() {
        Button button = findViewById(R.id.PhotoToggle);
        enableImageCapture(mCurrentImageCaptureType);
        button.setOnClickListener(
                (view) -> {
                    disableImageCapture();
                    // Toggle to next capture type and enable it and set it as current
                    switch (mCurrentImageCaptureType) {
                        case IMAGE_CAPTURE_TYPE_HDR:
                            enableImageCapture(ImageCaptureType.IMAGE_CAPTURE_TYPE_BOKEH);
                            enablePreview();
                            break;
                        case IMAGE_CAPTURE_TYPE_BOKEH:
                            enableImageCapture(ImageCaptureType.IMAGE_CAPTURE_TYPE_NIGHT);
                            enablePreview();
                            break;
                        case IMAGE_CAPTURE_TYPE_NIGHT:
                            enableImageCapture(ImageCaptureType.IMAGE_CAPTURE_TYPE_BEAUTY);
                            enablePreview();
                            break;
                        case IMAGE_CAPTURE_TYPE_BEAUTY:
                            enableImageCapture(ImageCaptureType.IMAGE_CAPTURE_TYPE_AUTO);
                            enablePreview();
                            break;
                        case IMAGE_CAPTURE_TYPE_AUTO:
                            enableImageCapture(ImageCaptureType.IMAGE_CAPTURE_TYPE_DEFAULT);
                            enablePreview();
                            break;
                        case IMAGE_CAPTURE_TYPE_DEFAULT:
                            enableImageCapture(ImageCaptureType.IMAGE_CAPTURE_TYPE_NONE);
                            enablePreview();
                            break;
                        case IMAGE_CAPTURE_TYPE_NONE:
                            enableImageCapture(ImageCaptureType.IMAGE_CAPTURE_TYPE_HDR);
                            enablePreview();
                            break;
                    }
                    bindUseCases();
                    showTakePictureButton();
                });

        Log.i(TAG, "Got UseCase: " + mImageCapture);
    }

    void enableImageCapture(ImageCaptureType imageCaptureType) {
        mCurrentImageCaptureType = imageCaptureType;
        ImageCapture.Builder builder = new ImageCapture.Builder().setTargetName("ImageCapture");
        Button toggleButton = findViewById(R.id.PhotoToggle);
        toggleButton.setText(mCurrentImageCaptureType.toString());

        switch (imageCaptureType) {
            case IMAGE_CAPTURE_TYPE_HDR:
                HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(
                        builder);
                if (hdrImageCaptureExtender.isExtensionAvailable(CAMERA_SELECTOR)) {
                    hdrImageCaptureExtender.enableExtension(CAMERA_SELECTOR);
                }
                break;
            case IMAGE_CAPTURE_TYPE_BOKEH:
                BokehImageCaptureExtender bokehImageCapture = BokehImageCaptureExtender.create(
                        builder);
                if (bokehImageCapture.isExtensionAvailable(CAMERA_SELECTOR)) {
                    bokehImageCapture.enableExtension(CAMERA_SELECTOR);
                }
                break;
            case IMAGE_CAPTURE_TYPE_NIGHT:
                NightImageCaptureExtender nightImageCapture = NightImageCaptureExtender.create(
                        builder);
                if (nightImageCapture.isExtensionAvailable(CAMERA_SELECTOR)) {
                    nightImageCapture.enableExtension(CAMERA_SELECTOR);
                }
                break;
            case IMAGE_CAPTURE_TYPE_BEAUTY:
                BeautyImageCaptureExtender beautyImageCapture = BeautyImageCaptureExtender.create(
                        builder);
                if (beautyImageCapture.isExtensionAvailable(CAMERA_SELECTOR)) {
                    beautyImageCapture.enableExtension(CAMERA_SELECTOR);
                }
                break;
            case IMAGE_CAPTURE_TYPE_AUTO:
                AutoImageCaptureExtender autoImageCapture = AutoImageCaptureExtender.create(
                        builder);
                if (autoImageCapture.isExtensionAvailable(CAMERA_SELECTOR)) {
                    autoImageCapture.enableExtension(CAMERA_SELECTOR);
                }
                break;
            case IMAGE_CAPTURE_TYPE_DEFAULT:
                break;
            case IMAGE_CAPTURE_TYPE_NONE:
                return;
        }

        mImageCapture = builder.build();

        Button captureButton = findViewById(R.id.Picture);

        Format formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US);
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "ExtensionsPictures");

        captureButton.setOnClickListener((view) -> {
            mTakePictureIdlingResource.increment();

            String fileName = formatter.format(Calendar.getInstance().getTime())
                    + mCurrentImageCaptureType.name() + ".jpg";
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

                            if (!mTakePictureIdlingResource.isIdleNow()) {
                                mTakePictureIdlingResource.decrement();
                            }

                            // Trigger MediaScanner to scan the file
                            if (outputFileResults.getSavedUri() == null) {
                                Intent intent = new Intent(
                                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                intent.setData(Uri.fromFile(saveFile));
                                sendBroadcast(intent);
                            }

                            Toast.makeText(getApplicationContext(),
                                    "Saved image to " + saveFile,
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "Failed to save image - " + exception.getMessage(),
                                    exception.getCause());
                        }
                    });
        });
    }

    void disableImageCapture() {
        if (mImageCapture != null) {
            mCameraProvider.unbind(mImageCapture);
            mImageCapture = null;
        }

        Button button = findViewById(R.id.Picture);
        button.setVisibility(View.INVISIBLE);
        button.setOnClickListener(null);
    }

    /** Creates all the use cases. */
    private void createUseCases() {
        ExtensionsManager.setExtensionsErrorListener((errorCode) ->
                Log.d(TAG, "Extensions error in error code: " + errorCode));
        createImageCapture();
        createPreview();
        bindUseCases();
        showTakePictureButton();
    }

    private void bindUseCases() {
        List<UseCase> useCases = new ArrayList<>();
        // When it is not IMAGE_CAPTURE_TYPE_NONE, mImageCapture won't be null.
        if (mImageCapture != null) {
            useCases.add(mImageCapture);
        }
        useCases.add(mPreview);
        mCameraProvider.bindToLifecycle(this, CAMERA_SELECTOR,
                useCases.toArray(new UseCase[useCases.size()]));
    }

    private void showTakePictureButton() {
        if (mImageCapture != null) {
            // Set the TakePicture button visible after bindToLifeCycle.
            Button captureButton = findViewById(R.id.Picture);
            captureButton.setVisibility(View.VISIBLE);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_extensions);

        StrictMode.VmPolicy policy =
                new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setVmPolicy(policy);
        mPreviewView = findViewById(R.id.previewView);

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        Futures.addCallback(setupPermissions(), new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@Nullable Boolean result) {
                mPermissionsGranted = Preconditions.checkNotNull(result);
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

    void setupCamera() {
        if (!mPermissionsGranted) {
            Log.d(TAG, "Permissions denied.");
            return;
        }

        ListenableFuture<ExtensionsManager.ExtensionsAvailability> availability =
                ExtensionsManager.init();

        Futures.addCallback(availability,
                new FutureCallback<ExtensionsManager.ExtensionsAvailability>() {
                    @Override
                    public void onSuccess(
                            @Nullable ExtensionsManager.ExtensionsAvailability availability) {
                        // Run this on the UI thread to manipulate the Textures & Views.
                        createUseCases();
                    }

                    @Override
                    public void onFailure(@NonNull Throwable throwable) {
                    }
                },
                ContextCompat.getMainExecutor(CameraExtensionsActivity.this)
        );
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
    private String[] getRequiredPermissions() {
        PackageInfo info;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException exception) {
            Log.e(TAG, "Failed to obtain all required permissions.", exception);
            return new String[0];
        }
        String[] permissions = info.requestedPermissions;
        if (permissions != null && permissions.length > 0) {
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

    @NonNull
    public ImageCaptureType getCurrentImageCaptureType() {
        return mCurrentImageCaptureType;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permissions Granted.");
                    mPermissionCompleter.set(true);
                } else {
                    Log.d(TAG, "Permissions Denied.");
                    mPermissionCompleter.set(false);
                }
                return;
            }
            default:
                // No-op
        }
    }

    /**
     * Waiting for preview capture session configured. Returns true if the capture session of
     * the preview is configured successfully, otherwise return false after timeout.
     */
    @VisibleForTesting
    public boolean waitForPreviewConfigured(long timeOutInMs) {
        try {
            return mPreviewCaptureSessionConfigured.await(timeOutInMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }
}
