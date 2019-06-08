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

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.camera.extensions.AutoImageCaptureExtender;
import androidx.camera.extensions.AutoPreviewExtender;
import androidx.camera.extensions.BeautyImageCaptureExtender;
import androidx.camera.extensions.BeautyPreviewExtender;
import androidx.camera.extensions.BokehImageCaptureExtender;
import androidx.camera.extensions.BokehPreviewExtender;
import androidx.camera.extensions.HdrImageCaptureExtender;
import androidx.camera.extensions.HdrPreviewExtender;
import androidx.camera.extensions.NightImageCaptureExtender;
import androidx.camera.extensions.NightPreviewExtender;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

/** An activity that shows off how extensions can be applied */
public class CameraExtensionsActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "CameraExtensionActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 42;

    private final SettableCallable<Boolean> mSettableResult = new SettableCallable<>();
    private final FutureTask<Boolean> mCompletableFuture = new FutureTask<>(mSettableResult);

    /** The cameraId to use. Assume that 0 is the typical back facing camera. */
    private String mCurrentCameraId = "0";

    private String mCurrentCameraFacing = "BACK";

    private Preview mPreview;
    private ImageCapture mImageCapture;
    private ImageCaptureType mCurrentImageCaptureType = ImageCaptureType.IMAGE_CAPTURE_TYPE_HDR;

    private HandlerThread mHandlerThread = new HandlerThread("CameraExtensionsActivityHandler");

    /**
     * Creates a preview use case.
     *
     * <p>This use case observes a {@link SurfaceTexture}. The texture is connected to a {@link
     * TextureView} to display a camera preview.
     */
    private void createPreview() {
        enablePreview();
        Log.i(TAG, "Got UseCase: " + mPreview);
    }

    void enablePreview() {
        if (mPreview != null) {
            CameraX.unbind(mPreview);
        }

        PreviewConfig.Builder builder =
                new PreviewConfig.Builder()
                        .setLensFacing(LensFacing.BACK)
                        .setTargetName("Preview");

        Log.d(TAG, "Enabling the extended preview");
        if (mCurrentImageCaptureType == ImageCaptureType.IMAGE_CAPTURE_TYPE_BOKEH) {
            Log.d(TAG, "Enabling the extended preview in bokeh mode.");

            BokehPreviewExtender extender = BokehPreviewExtender.create(builder);
            if (extender.isExtensionAvailable()) {
                extender.enableExtension();
            }
        } else if (mCurrentImageCaptureType == ImageCaptureType.IMAGE_CAPTURE_TYPE_HDR) {
            Log.d(TAG, "Enabling the extended preview in HDR mode.");

            HdrPreviewExtender extender = HdrPreviewExtender.create(builder);
            if (extender.isExtensionAvailable()) {
                extender.enableExtension();
            }
        } else if (mCurrentImageCaptureType == ImageCaptureType.IMAGE_CAPTURE_TYPE_NIGHT) {
            Log.d(TAG, "Enabling the extended preview in night mode.");

            NightPreviewExtender extender = NightPreviewExtender.create(builder);
            if (extender.isExtensionAvailable()) {
                extender.enableExtension();
            }
        } else if (mCurrentImageCaptureType == ImageCaptureType.IMAGE_CAPTURE_TYPE_BEAUTY) {
            Log.d(TAG, "Enabling the extended preview in beauty mode.");

            BeautyPreviewExtender extender = BeautyPreviewExtender.create(builder);
            if (extender.isExtensionAvailable()) {
                extender.enableExtension();
            }
        } else if (mCurrentImageCaptureType == ImageCaptureType.IMAGE_CAPTURE_TYPE_AUTO) {
            Log.d(TAG, "Enabling the extended preview in auto mode.");

            AutoPreviewExtender extender = AutoPreviewExtender.create(builder);
            if (extender.isExtensionAvailable()) {
                extender.enableExtension();
            }
        }

        mPreview = new Preview(builder.build());

        TextureView textureView = findViewById(R.id.textureView);

        mPreview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        // If TextureView was already created, need to re-add it to change the
                        // SurfaceTexture.
                        ViewGroup viewGroup = (ViewGroup) textureView.getParent();
                        viewGroup.removeView(textureView);
                        viewGroup.addView(textureView);
                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                    }
                });
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
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
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
                    }
                });

        Log.i(TAG, "Got UseCase: " + mImageCapture);
    }

    void enableImageCapture(ImageCaptureType imageCaptureType) {
        mCurrentImageCaptureType = imageCaptureType;
        ImageCaptureConfig.Builder builder =
                new ImageCaptureConfig.Builder()
                        .setLensFacing(LensFacing.BACK)
                        .setTargetName("ImageCapture");
        Button toggleButton = findViewById(R.id.PhotoToggle);
        toggleButton.setText(mCurrentImageCaptureType.toString());

        switch (imageCaptureType) {
            case IMAGE_CAPTURE_TYPE_HDR:
                HdrImageCaptureExtender hdrImageCaptureExtender = HdrImageCaptureExtender.create(
                        builder);
                if (hdrImageCaptureExtender.isExtensionAvailable()) {
                    hdrImageCaptureExtender.enableExtension();
                    builder.setCallbackHandler(new Handler(mHandlerThread.getLooper()));
                }
                break;
            case IMAGE_CAPTURE_TYPE_BOKEH:
                BokehImageCaptureExtender bokehImageCapture = BokehImageCaptureExtender.create(
                        builder);
                if (bokehImageCapture.isExtensionAvailable()) {
                    bokehImageCapture.enableExtension();
                }
                break;
            case IMAGE_CAPTURE_TYPE_NIGHT:
                NightImageCaptureExtender nightImageCapture = NightImageCaptureExtender.create(
                        builder);
                if (nightImageCapture.isExtensionAvailable()) {
                    nightImageCapture.enableExtension();
                }
                break;
            case IMAGE_CAPTURE_TYPE_BEAUTY:
                BeautyImageCaptureExtender beautyImageCapture = BeautyImageCaptureExtender.create(
                        builder);
                if (beautyImageCapture.isExtensionAvailable()) {
                    beautyImageCapture.enableExtension();
                }
                break;
            case IMAGE_CAPTURE_TYPE_AUTO:
                AutoImageCaptureExtender autoImageCapture = AutoImageCaptureExtender.create(
                        builder);
                if (autoImageCapture.isExtensionAvailable()) {
                    autoImageCapture.enableExtension();
                }
                break;
            case IMAGE_CAPTURE_TYPE_DEFAULT:
                break;
            case IMAGE_CAPTURE_TYPE_NONE:
                return;
        }

        mImageCapture = new ImageCapture(builder.build());

        Button captureButton = findViewById(R.id.Picture);

        final Format formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US);

        final File dir =
                new File(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES),
                        "ExtensionsPictures");
        dir.mkdirs();
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mImageCapture.takePicture(
                                new File(
                                        dir,
                                        formatter.format(Calendar.getInstance().getTime())
                                                + mCurrentImageCaptureType.name()
                                                + ".jpg"),
                                new ImageCapture.OnImageSavedListener() {
                                    @Override
                                    public void onImageSaved(File file) {
                                        Log.d(TAG, "Saved image to " + file);

                                        // Trigger MediaScanner to scan the file
                                        Intent intent = new Intent(
                                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                        intent.setData(Uri.fromFile(file));
                                        sendBroadcast(intent);

                                        Toast.makeText(getApplicationContext(),
                                                "Saved image to " + file,
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(
                                            ImageCapture.UseCaseError useCaseError,
                                            String message,
                                            Throwable cause) {
                                        Log.e(TAG, "Failed to save image - " + message, cause);
                                    }
                                });
                    }
                });
    }

    void disableImageCapture() {
        if (mImageCapture != null) {
            CameraX.unbind(mImageCapture);
            mImageCapture = null;
        }

        Button button = findViewById(R.id.Picture);
        button.setOnClickListener(null);
    }

    /** Creates all the use cases. */
    private void createUseCases() {
        createImageCapture();
        createPreview();
        bindUseCases();
    }

    private void bindUseCases() {
        List<UseCase> useCases = new ArrayList();
        // When it is not IMAGE_CAPTURE_TYPE_NONE, mImageCapture won't be null.
        if (mImageCapture != null) {
            useCases.add(mImageCapture);
        }
        useCases.add(mPreview);
        CameraX.bindToLifecycle(this, useCases.toArray(new UseCase[useCases.size()]));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_extensions);

        StrictMode.VmPolicy policy =
                new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setVmPolicy(policy);

        mHandlerThread.start();

        // Get params from adb extra string
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String newCameraFacing = bundle.getString("cameraFacing");
            if (newCameraFacing != null) {
                mCurrentCameraFacing = newCameraFacing;
            }
        }

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        setupCamera();
                    }
                })
                .start();
        setupPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandlerThread.quitSafely();
    }

    private void setupCamera() {
        try {
            // Wait for permissions before proceeding.
            if (!mCompletableFuture.get()) {
                Log.d(TAG, "Permissions denied.");
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred getting permission future: " + e);
        }

        try {
            Log.d(TAG, "Camera Facing: " + mCurrentCameraFacing);
            LensFacing facing = LensFacing.BACK;
            if (mCurrentCameraFacing.equalsIgnoreCase("BACK")) {
                facing = LensFacing.BACK;
            } else if (mCurrentCameraFacing.equalsIgnoreCase("FRONT")) {
                facing = LensFacing.FRONT;
            } else {
                throw new RuntimeException("Invalid lens facing: " + mCurrentCameraFacing);
            }
            mCurrentCameraId = CameraX.getCameraWithLensFacing(facing);
        } catch (Exception e) {
            Log.e(TAG, "Unable to obtain camera with specified facing. " + e.getMessage());
        }

        Log.d(TAG, "Using cameraId: " + mCurrentCameraId);

        // Run this on the UI thread to manipulate the Textures & Views.
        CameraExtensionsActivity.this.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        createUseCases();
                    }
                });
    }

    private void setupPermissions() {
        if (!allPermissionsGranted()) {
            makePermissionRequest();
        } else {
            mSettableResult.set(true);
            mCompletableFuture.run();
        }
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

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permissions Granted.");
                    mSettableResult.set(true);
                    mCompletableFuture.run();
                } else {
                    Log.d(TAG, "Permissions Denied.");
                    mSettableResult.set(false);
                    mCompletableFuture.run();
                }
                return;
            }
            default:
                // No-op
        }
    }

    /** A {@link Callable} whose return value can be set. */
    private static final class SettableCallable<V> implements Callable<V> {
        private final AtomicReference<V> mValue = new AtomicReference<>();

        public void set(V value) {
            mValue.set(value);
        }

        @Override
        public V call() {
            return mValue.get();
        }
    }
}
