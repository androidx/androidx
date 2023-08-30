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

import static androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA;
import static androidx.camera.testing.impl.FileUtil.createParentFolder;

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.UseCase;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.Recorder;
import androidx.camera.video.VideoCapture;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleService;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/**
 * A service used to test background UseCases binding and camera operations.
 */
public class CameraXService extends LifecycleService {
    private static final String TAG = "CameraXService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID_SERVICE_INFO = "channel_service_info";

    // Actions
    public static final String ACTION_BIND_USE_CASES =
            "androidx.camera.integration.core.intent.action.BIND_USE_CASES";
    public static final String ACTION_TAKE_PICTURE =
            "androidx.camera.integration.core.intent.action.TAKE_PICTURE";

    // Extras
    public static final String EXTRA_VIDEO_CAPTURE_ENABLED = "EXTRA_VIDEO_CAPTURE_ENABLED";
    public static final String EXTRA_IMAGE_CAPTURE_ENABLED = "EXTRA_IMAGE_CAPTURE_ENABLED";
    public static final String EXTRA_IMAGE_ANALYSIS_ENABLED = "EXTRA_IMAGE_ANALYSIS_ENABLED";

    private final IBinder mBinder = new CameraXServiceBinder();

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                          Members only accessed on main thread                              //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final Map<Class<?>, UseCase> mBoundUseCases = new HashMap<>();
    //--------------------------------------------------------------------------------------------//

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                   Members for testing                                      //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final Set<Uri> mSavedImageUri = new HashSet<>();

    @Nullable
    private Consumer<Collection<UseCase>> mOnUseCaseBoundCallback;
    @Nullable
    private CountDownLatch mAnalysisFrameLatch;
    @Nullable
    private CountDownLatch mTakePictureLatch;
    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreate() {
        super.onCreate();
        makeForeground();
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        super.onBind(intent);
        return mBinder;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "onStartCommand: action = " + action + ", extras = " + intent.getExtras());
            if (ACTION_BIND_USE_CASES.equals(action)) {
                bindToLifecycle(intent);
            } else if (ACTION_TAKE_PICTURE.equals(action)) {
                takePicture();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void makeForeground() {
        createNotificationChannel();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,
                CHANNEL_ID_SERVICE_INFO)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void bindToLifecycle(@NonNull Intent intent) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        ProcessCameraProvider cameraProvider;
        try {
            cameraProvider = cameraProviderFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
        cameraProvider.unbindAll();
        mBoundUseCases.clear();
        UseCaseGroup useCaseGroup = resolveUseCaseGroup(intent);
        List<UseCase> boundUseCases = Collections.emptyList();
        if (useCaseGroup != null) {
            try {
                cameraProvider.bindToLifecycle(this, DEFAULT_BACK_CAMERA, useCaseGroup);
                boundUseCases = useCaseGroup.getUseCases();
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Failed to bind by " + e, e);
            }
        }
        Log.d(TAG, "Bound UseCases: " + boundUseCases);
        for (UseCase boundUseCase : boundUseCases) {
            mBoundUseCases.put(boundUseCase.getClass(), boundUseCase);
        }
        if (mOnUseCaseBoundCallback != null) {
            mOnUseCaseBoundCallback.accept(boundUseCases);
        }
    }

    @Nullable
    private UseCaseGroup resolveUseCaseGroup(@NonNull Intent intent) {
        boolean hasUseCase = false;
        UseCaseGroup.Builder useCaseGroupBuilder = new UseCaseGroup.Builder();

        if (intent.getBooleanExtra(EXTRA_VIDEO_CAPTURE_ENABLED, false)) {
            Recorder recorder = new Recorder.Builder().build();
            VideoCapture<?> videoCapture = new VideoCapture.Builder<>(recorder).build();
            useCaseGroupBuilder.addUseCase(videoCapture);
            hasUseCase = true;
        }
        if (intent.getBooleanExtra(EXTRA_IMAGE_CAPTURE_ENABLED, false)) {
            ImageCapture imageCapture = new ImageCapture.Builder().build();
            useCaseGroupBuilder.addUseCase(imageCapture);
            hasUseCase = true;
        }
        if (intent.getBooleanExtra(EXTRA_IMAGE_ANALYSIS_ENABLED, false)) {
            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), mAnalyzer);
            useCaseGroupBuilder.addUseCase(imageAnalysis);
            hasUseCase = true;
        }

        return hasUseCase ? useCaseGroupBuilder.build() : null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = Api26Impl.newNotificationChannel(
                    CHANNEL_ID_SERVICE_INFO,
                    getString(R.string.camerax_service),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            Api26Impl.createNotificationChannel(getNotificationManager(), serviceChannel);
        }
    }

    @NonNull
    private NotificationManager getNotificationManager() {
        return checkNotNull(ContextCompat.getSystemService(this, NotificationManager.class));
    }

    @Nullable
    private ImageCapture getImageCapture() {
        return (ImageCapture) mBoundUseCases.get(ImageCapture.class);
    }

    private void takePicture() {
        ImageCapture imageCapture = getImageCapture();
        if (imageCapture == null) {
            Log.w(TAG, "ImageCapture is not bound.");
            return;
        }
        createDefaultPictureFolderIfNotExist();
        Format formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US);
        String fileName = "ServiceTestApp-" + formatter.format(Calendar.getInstance().getTime())
                + ".jpg";
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues).build();
        long startTimeMs = SystemClock.elapsedRealtime();
        imageCapture.takePicture(outputFileOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(
                            @NonNull ImageCapture.OutputFileResults outputFileResults) {
                        long durationMs = SystemClock.elapsedRealtime() - startTimeMs;
                        Log.d(TAG, "Saved image " + outputFileResults.getSavedUri()
                                + "  (" + durationMs + " ms)");
                        mSavedImageUri.add(outputFileResults.getSavedUri());
                        if (mTakePictureLatch != null) {
                            mTakePictureLatch.countDown();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Failed to save image by " + exception.getImageCaptureError(),
                                exception);
                    }
                });
    }

    private void createDefaultPictureFolderIfNotExist() {
        File pictureFolder = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        if (!createParentFolder(pictureFolder)) {
            Log.e(TAG, "Failed to create directory: " + pictureFolder);
        }
    }

    private final ImageAnalysis.Analyzer mAnalyzer = image -> {
        if (mAnalysisFrameLatch != null) {
            mAnalysisFrameLatch.countDown();
        }
        image.close();
    };

    @RequiresApi(26)
    static class Api26Impl {

        private Api26Impl() {
        }

        /** @noinspection SameParameterValue */
        @DoNotInline
        @NonNull
        static NotificationChannel newNotificationChannel(@NonNull String id,
                @NonNull CharSequence name, int importance) {
            return new NotificationChannel(id, name, importance);
        }

        @DoNotInline
        static void createNotificationChannel(@NonNull NotificationManager manager,
                @NonNull NotificationChannel channel) {
            manager.createNotificationChannel(channel);
        }
    }

    @VisibleForTesting
    void setOnUseCaseBoundCallback(@NonNull Consumer<Collection<UseCase>> callback) {
        mOnUseCaseBoundCallback = callback;
    }

    @VisibleForTesting
    @NonNull
    CountDownLatch acquireAnalysisFrameCountDownLatch() {
        mAnalysisFrameLatch = new CountDownLatch(3);
        return mAnalysisFrameLatch;
    }

    @VisibleForTesting
    @NonNull
    CountDownLatch acquireTakePictureCountDownLatch() {
        mTakePictureLatch = new CountDownLatch(1);
        return mTakePictureLatch;
    }

    @VisibleForTesting
    void deleteSavedMediaFiles() {
        deleteUriSet(mSavedImageUri);
    }

    private void deleteUriSet(@NonNull Set<Uri> uriSet) {
        for (Uri uri : uriSet) {
            try {
                getContentResolver().delete(uri, null, null);
            } catch (RuntimeException e) {
                Log.w(TAG, "Unable to delete uri: " + uri, e);
            }
        }
    }

    class CameraXServiceBinder extends Binder {
        @NonNull
        CameraXService getService() {
            return CameraXService.this;
        }
    }
}
