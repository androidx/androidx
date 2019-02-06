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

package androidx.camera.app.camera2interoperror;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Rational;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ViewFinderUseCase;
import androidx.camera.core.ViewFinderUseCaseConfiguration;
import androidx.legacy.app.ActivityCompat;
import java.util.concurrent.CompletableFuture;

/** for testing interop */
@SuppressWarnings("AndroidJdkLibsChecker") // CompletableFuture not generally available yet.
public class CameraXInteropErrorActivity extends AppCompatActivity
    implements ActivityCompat.OnRequestPermissionsResultCallback {
  private static final String TAG = "CameraXInteropErrorActivity";
  private static final int PERMISSIONS_REQUEST_CODE = 42;
  private static final Rational ASPECT_RATIO = new Rational(4, 3);

  private final CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

  /** The LensFacing to use. */
  private LensFacing currentCameraLensFacing = LensFacing.BACK;

  private String currentCameraFacingString = "BACK";

  /** The types of errors that can be induced on CameraX. */
  enum ErrorType {
    /** Attempt to reopen the currently opened {@link CameraDevice}. */
    REOPEN_CAMERA,
    /** Close the {@link CameraDevice} without going through CameraX. */
    CLOSE_DEVICE,
    /**
     * Open up a {@link android.hardware.camera2.CameraCaptureSession} using the {@link
     * CameraDevice} obtained from {@link CameraDevice.StateCallback}.
     */
    OPEN_CAPTURE_SESSION
  }

  private ErrorType currentError = ErrorType.REOPEN_CAMERA;

  /**
   * Creates a view finder use case.
   *
   * <p>This use case observes a {@link SurfaceTexture}. The texture is connected to a {@link
   * TextureView} to display a camera preview.
   */
  private void createViewFinderUseCase() {
    ViewFinderUseCaseConfiguration configuration =
        new ViewFinderUseCaseConfiguration.Builder()
            .setLensFacing(currentCameraLensFacing)
            .setTargetName("ViewFinder")
            .setTargetAspectRatio(ASPECT_RATIO)
            .build();
    ViewFinderUseCase viewFinderUseCase = new ViewFinderUseCase(configuration);

    viewFinderUseCase.setOnViewFinderOutputUpdateListener(
        viewFinderOutput -> {
          // If TextureView was already created, need to re-add it to change the SurfaceTexture.
          TextureView textureView = findViewById(R.id.textureView);
          ViewGroup viewGroup = (ViewGroup) textureView.getParent();
          viewGroup.removeView(textureView);
          viewGroup.addView(textureView);
          textureView.setSurfaceTexture(viewFinderOutput.getSurfaceTexture());
        });

    CameraX.bindToLifecycle(/* lifecycleOwner= */this, viewFinderUseCase);
    Log.i(TAG, "Got UseCase: " + viewFinderUseCase);
  }

  void createBadUseCase() {
    Camera2InteropErrorUseCaseConfiguration configuration =
        new Camera2InteropErrorUseCaseConfiguration.Builder()
            .setLensFacing(currentCameraLensFacing)
            .setTargetName("Camera2InteropErrorUseCase")
            .setTargetAspectRatio(ASPECT_RATIO)
            .build();
    Camera2InteropErrorUseCase camera2InteropErrorUseCase =
        new Camera2InteropErrorUseCase(configuration);
    CameraX.bindToLifecycle(this, camera2InteropErrorUseCase);

    Button button = this.findViewById(R.id.CauseError);

    button.setOnClickListener(
        view -> {
          switch (currentError) {
            case CLOSE_DEVICE:
              camera2InteropErrorUseCase.closeCamera();
              break;
            case REOPEN_CAMERA:
              CameraManager manager =
                  (CameraManager) getApplicationContext().getSystemService(CAMERA_SERVICE);
              Log.d(TAG, "Attempting to reopen camera");
              try {
                String cameraId = CameraX.getCameraWithLensFacing(currentCameraLensFacing);
                manager.openCamera(
                    cameraId,
                    new StateCallback() {
                      @Override
                      public void onOpened(@NonNull CameraDevice camera) {}

                      @Override
                      public void onDisconnected(@NonNull CameraDevice camera) {}

                      @Override
                      public void onError(@NonNull CameraDevice camera, int error) {}
                    },
                    null);
                Log.d(TAG, "Looks like nothing overtly bad occurred");
              } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Log.e(TAG, "Should we do something here?");
              }
              break;
            case OPEN_CAPTURE_SESSION:
              camera2InteropErrorUseCase.reopenCaptureSession();
              break;
          }
        });

    TextView textView = this.findViewById(R.id.textView);
    textView.setText(currentError.toString());

    Button button1 = this.findViewById(R.id.SelectError);
    button1.setOnClickListener(
        view -> {
          switch (currentError) {
            case CLOSE_DEVICE:
              currentError = ErrorType.REOPEN_CAMERA;
              break;
            case REOPEN_CAMERA:
              currentError = ErrorType.OPEN_CAPTURE_SESSION;
              break;
            case OPEN_CAPTURE_SESSION:
              currentError = ErrorType.CLOSE_DEVICE;
              break;
          }
          textView.setText(currentError.toString());
        });
  }

  /** Creates all the use cases. */
  private void createUseCases() {
    createViewFinderUseCase();
    createBadUseCase();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_camera_2main);

    // Get params from adb extra string
    Bundle bundle = this.getIntent().getExtras();
    if (bundle != null) {
      currentCameraFacingString = bundle.getString("cameraFacing");
    }

    new Thread(
            () -> {
              setupCamera();
            })
        .start();
    setupPermissions();
  }

  private void setupCamera() {
    try {
      // Wait for permissions before proceeding.
      if (!completableFuture.get()) {
        Log.d(TAG, "Permissions denied.");
        return;
      }
    } catch (Exception e) {
      Log.e(TAG, "Exception occurred getting permission future: " + e);
    }

    Log.d(TAG, "Camera Facing: " + currentCameraFacingString);
    if (currentCameraFacingString.equalsIgnoreCase("BACK")) {
      currentCameraLensFacing = LensFacing.BACK;
    } else if (currentCameraFacingString.equalsIgnoreCase("FRONT")) {
      currentCameraLensFacing = LensFacing.FRONT;
    } else {
      throw new RuntimeException("Invalid lens facing: " + currentCameraFacingString);
    }

    Log.d(TAG, "Using camera lens facing: " + currentCameraLensFacing);

    // Run this on the UI thread to manipulate the Textures & Views.
    CameraXInteropErrorActivity.this.runOnUiThread(
        () -> {
          createUseCases();
        });
  }

  private void setupPermissions() {
    if (!allPermissionsGranted()) {
      makePermissionRequest();
    } else {
      completableFuture.complete(true);
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
      info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
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
      case PERMISSIONS_REQUEST_CODE:
        {
          // If request is cancelled, the result arrays are empty.
          if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permissions Granted.");
            completableFuture.complete(true);
          } else {
            Log.d(TAG, "Permissions Denied.");
            completableFuture.complete(false);
          }
          return;
      }
      default:
        // No-op
    }
  }
}
