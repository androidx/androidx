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

package androidx.camera.app.camera2interopburst;

import android.Manifest;
import android.app.Activity;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.GuardedBy;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.camera.camera2.Camera2Configuration;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysisUseCase;
import androidx.camera.core.ImageAnalysisUseCaseConfiguration;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ViewFinderUseCase;
import androidx.camera.core.ViewFinderUseCaseConfiguration;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * An activity using CameraX-Camera2 interop to capture a burst.
 *
 * <p>First, the activity uses CameraX to set up a ViewFinderUseCase and ImageAnalysisUseCase. The
 * ImageAnalysisUseCase converts Image instances into Bitmap instances. During the setup, custom
 * CameraCaptureSession.StateCallback and CameraCaptureSession.CaptureCallback instances are passed
 * to CameraX. These callbacks enable the activity to get references to the CameraCaptureSession and
 * repeating CaptureRequest created internally by CameraX.
 *
 * <p>Then, when the user clicks on the viewfinder, CameraX's repeating request is stopped and a new
 * burst capture request is issued, using the active CameraCaptureSession still owned by CameraX.
 * The images captured during the burst are shown in an overlay mosaic for visualization.
 *
 * <p>Finally, after the burst capture concludes, CameraX's previous repeating request is resumed,
 * until the next time the user starts a burst.
 */
@SuppressWarnings("AndroidJdkLibsChecker") // CompletableFuture not generally available yet.
public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getName();
  private static final int CAMERA_REQUEST_CODE = 101;
  private static final int BURST_FRAME_COUNT = 30;
  private static final int MOSAIC_ROWS = 3;
  private static final int MOSAIC_COLS = BURST_FRAME_COUNT / MOSAIC_ROWS;
  // TODO: Figure out dynamically to fill the screen, instead of hard-coding.
  private static final int TILE_WIDTH = 102;
  private static final int TILE_HEIGHT = 72;;

  // Waiting for the permissions approval.
  private final CompletableFuture<Integer> completableFuture = new CompletableFuture<>();

  // For handling touch events on the TextureView.
  private final View.OnTouchListener onTouchListener = new OnTouchListener();

  // Tracks the burst state.
  private final Object burstLock = new Object();

  @GuardedBy("burstLock")
  private boolean burstInProgress = false;

  @GuardedBy("burstLock")
  private int burstFrameCount = 0;

  // Camera2 interop objects.
  private final SessionUpdatingSessionStateCallback sessionStateCallback =
      new SessionUpdatingSessionStateCallback();
  private final RequestUpdatingSessionCaptureCallback sessionCaptureCallback =
      new RequestUpdatingSessionCaptureCallback();

  // For visualizing the images captured in the burst.
  private ImageView imageView;
  private final Object mosaicLock = new Object();

  @GuardedBy("mosaicLock")
  private final Bitmap mosaic =
      Bitmap.createBitmap(
          MOSAIC_COLS * TILE_WIDTH, MOSAIC_ROWS * TILE_HEIGHT, Bitmap.Config.ARGB_8888);

  private final MutableLiveData<Bitmap> analysisResult = new MutableLiveData<>();

  // For running ops on a background thread.
  private Handler backgroundHandler;
  private HandlerThread backgroundHandlerThread;

  @Override
  protected void onCreate(@Nullable Bundle bundle) {
    super.onCreate(bundle);
    setContentView(R.layout.activity_main);

    backgroundHandlerThread = new HandlerThread("Background");
    backgroundHandlerThread.start();
    backgroundHandler = new Handler(backgroundHandlerThread.getLooper());

    new Thread(() -> {
      setupCamera();
    }).start();
    setupPermissions(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    backgroundHandler.removeCallbacksAndMessages(null);
    backgroundHandlerThread.quitSafely();
  }

  @Override
  protected void onPause() {
    synchronized (burstLock) {
      burstInProgress = false;
    }
    super.onPause();
  }

  private void setupCamera() {
    try {
      // Wait for permissions before proceeding.
      if (completableFuture.get() == PackageManager.PERMISSION_DENIED) {
        Log.e(TAG, "Permission to open camera denied.");
        return;
      }
    } catch (InterruptedException | ExecutionException e) {
      Log.e(TAG, "Exception occurred getting permission future: " + e);
    }
    LifecycleOwner lifecycleOwner = this;

    // Run this on the UI thread to manipulate the Textures & Views.
    MainActivity.this.runOnUiThread(
        () -> {
          imageView = findViewById(R.id.imageView);

          ViewFinderUseCaseConfiguration.Builder viewFinderConfigBuilder =
              new ViewFinderUseCaseConfiguration.Builder().setTargetName("ViewFinder");

          new Camera2Configuration.Extender(viewFinderConfigBuilder)
              .setSessionStateCallback(sessionStateCallback)
              .setSessionCaptureCallback(sessionCaptureCallback);

          ViewFinderUseCaseConfiguration viewFinderConfig = viewFinderConfigBuilder.build();
          TextureView textureView = findViewById(R.id.textureView);
          textureView.setOnTouchListener(onTouchListener);
          ViewFinderUseCase viewFinderUseCase = new ViewFinderUseCase(viewFinderConfig);

          viewFinderUseCase.setOnViewFinderOutputUpdateListener(
              output -> {
                // If TextureView was already created, need to re-add it to change
                // the SurfaceTexture.
                ViewGroup v = (ViewGroup) textureView.getParent();
                v.removeView(textureView);
                v.addView(textureView);
                textureView.setSurfaceTexture(output.getSurfaceTexture());
              });

          CameraX.bindToLifecycle(lifecycleOwner, viewFinderUseCase);

          ImageAnalysisUseCaseConfiguration analysisConfig =
              new ImageAnalysisUseCaseConfiguration.Builder()
                  .setTargetName("ImageAnalysis")
                  .setCallbackHandler(backgroundHandler)
                  .build();
          ImageAnalysisUseCase analysisUseCase = new ImageAnalysisUseCase(analysisConfig);
          CameraX.bindToLifecycle(lifecycleOwner, analysisUseCase);
          analysisUseCase.setAnalyzer(
              (image, rotationDegrees) -> {
                analysisResult.postValue(convertYuv420ImageToBitmap(image));
              });
          analysisResult.observe(
              lifecycleOwner,
              bitmap -> {
                synchronized (burstLock) {
                  if (burstInProgress) {
                    // Update the mosaic.
                    insertIntoMosaic(bitmap, burstFrameCount++);
                    MainActivity.this.runOnUiThread(
                        () -> {
                          synchronized (mosaicLock) {
                            imageView.setImageBitmap(mosaic);
                          }
                        });

                    // Detect the end of the burst.
                    if (burstFrameCount == BURST_FRAME_COUNT) {
                      burstInProgress = false;
                      submitRepeatingRequest();
                    }
                  }
                }
              });
        });
  }

  private void setupPermissions(Activity context) {
    int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
    if (permission != PackageManager.PERMISSION_GRANTED) {
      makePermissionRequest(context);
    } else {
      completableFuture.complete(permission);
    }
  }

  private static void makePermissionRequest(Activity context) {
    ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.CAMERA},
        CAMERA_REQUEST_CODE);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
      String[] permissions, int[] grantResults) {
    switch (requestCode) {
      case CAMERA_REQUEST_CODE: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Log.i(TAG, "Camera Permission Granted.");
        } else {
          Log.i(TAG, "Camera Permission Denied.");
        }
          completableFuture.complete(grantResults[0]);
        return;
      }
      default: {}
    }
  }

  /** An on-touch listener which submits a capture burst request when the view is touched. */
  private class OnTouchListener implements View.OnTouchListener {
    @Override
    public boolean onTouch(View view, MotionEvent event) {
      if (event.getActionMasked() == MotionEvent.ACTION_UP) {
        synchronized (burstLock) {
          if (!burstInProgress) {
            burstFrameCount = 0;
            synchronized (mosaicLock) {
              mosaic.eraseColor(0);
            }
            try {
              sessionStateCallback.getSession().stopRepeating();
            } catch (CameraAccessException e) {
              throw new RuntimeException("Could not stop the repeating request.", e);
            }
            submitCaptureBurstRequest();
            burstInProgress = true;
          }
        }
      }
      return true;
    }
  }

  private void submitCaptureBurstRequest() {
    try {
      // Use the existing session created by CameraX.
      CameraCaptureSession session = sessionStateCallback.getSession();
      // Use the previous request created by CameraX.
      CaptureRequest request = sessionCaptureCallback.getRequest();
      List<CaptureRequest> requests = new ArrayList<>(BURST_FRAME_COUNT);
      for (int i = 0; i < BURST_FRAME_COUNT; ++i) {
        requests.add(request);
      }
      session.captureBurst(requests, /*callback=*/ null, backgroundHandler);
    } catch (CameraAccessException e) {
      throw new RuntimeException("Could not submit the burst capture request.", e);
    }
  }

  private void submitRepeatingRequest() {
    try {
      // Use the existing session created by CameraX.
      CameraCaptureSession session = sessionStateCallback.getSession();
      // Use the previous request created by CameraX.
      CaptureRequest request = sessionCaptureCallback.getRequest();
      // TODO: This capture callback is not the same as that used by CameraX internally.
      // Find a way to use exactly that same callback.
      session.setRepeatingRequest(request, sessionCaptureCallback, backgroundHandler);
    } catch (CameraAccessException e) {
      throw new RuntimeException("Could not submit the repeating request.", e);
    }
  }

  // TODO: Do proper YUV420-to-RGB conversion, instead of just taking the Y channel and
  // propagating it to all 3 channels.
  private Bitmap convertYuv420ImageToBitmap(ImageProxy image) {
    ImageProxy.PlaneProxy plane = image.getPlanes()[0];
    ByteBuffer buffer = plane.getBuffer();
    final int bytesCount = buffer.remaining();
    byte[] imageBytes = new byte[bytesCount];
    buffer.get(imageBytes);

    // TODO: Reuse a bitmap from a pool.
    Bitmap bitmap =
        Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);

    int[] bitmapPixels = new int[bitmap.getWidth() * bitmap.getHeight()];
    for (int row = 0; row < bitmap.getHeight(); ++row) {
      int imageBytesPosition = row * plane.getRowStride();
      int bitmapPixelsPosition = row * bitmap.getWidth();
      for (int col = 0; col < bitmap.getWidth(); ++col) {
        int channelValue = (imageBytes[imageBytesPosition++] & 0xFF);
        bitmapPixels[bitmapPixelsPosition++] = Color.rgb(channelValue, channelValue, channelValue);
      }
    }
    bitmap.setPixels(
        bitmapPixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    return bitmap;
  }

  private void insertIntoMosaic(Bitmap bitmap, int position) {
    // TODO: Reuse a bitmap from a pool.
    Bitmap rescaledBitmap =
        Bitmap.createScaledBitmap(bitmap, TILE_WIDTH, TILE_HEIGHT, /*filter=*/ false);

    int tileRowOffset = (position / MOSAIC_COLS) * TILE_HEIGHT;
    int tileColOffset = (position % MOSAIC_COLS) * TILE_WIDTH;
    for (int row = 0; row < rescaledBitmap.getHeight(); ++row) {
      for (int col = 0; col < rescaledBitmap.getWidth(); ++col) {
        int color = rescaledBitmap.getPixel(col, row);
        synchronized (mosaicLock) {
          mosaic.setPixel(col + tileColOffset, row + tileRowOffset, color);
        }
      }
    }
  }
}
