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

package androidx.camera.testapp.javacameraxpermissions;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewGroup;

import androidx.camera.core.CameraX;
import androidx.camera.core.ViewFinderUseCase;
import androidx.camera.core.ViewFinderUseCaseConfiguration;

import java.util.concurrent.CompletableFuture;

/**
 * The activity verifies the perssions used in CameraX
 *
 */
@SuppressWarnings("AndroidJdkLibsChecker")
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    private static final int CAMERA_REQUEST_CODE = 101;
    private final CompletableFuture<Integer> mCF = new CompletableFuture<>();

    private static void makePermissionRequest(Activity context) {
        ActivityCompat.requestPermissions(
                context, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        new Thread(
                () -> {
                    setupCamera();
                })
                .start();
        setupPermissions(this);
    }

    private void setupCamera() {
        try {
            // Wait for permissions before proceeding.
            if (mCF.get() == PackageManager.PERMISSION_DENIED) {
                Log.i(TAG, "Permission to open camera denied.");
                return;
            }
        } catch (Exception e) {
            Log.i(TAG, "Exception occurred getting permission future: " + e);
        }

        // Run this on the UI thread to manipulate the Textures & Views.
        MainActivity.this.runOnUiThread(
                () -> {
                    ViewFinderUseCaseConfiguration vfConfig =
                            new ViewFinderUseCaseConfiguration.Builder()
                                    .setTargetName("vf0")
                                    .build();
                    TextureView textureView = findViewById(R.id.textureView);
                    ViewFinderUseCase vfUseCase = new ViewFinderUseCase(vfConfig);

                    vfUseCase.setOnViewFinderOutputUpdateListener(
                            viewFinderOutput -> {
                                // If TextureView was already created, need to re-add it to change
                                // the SurfaceTexture.
                                ViewGroup v = (ViewGroup) textureView.getParent();
                                v.removeView(textureView);
                                v.addView(textureView);
                                textureView.setSurfaceTexture(viewFinderOutput.getSurfaceTexture());
                            });
                    CameraX.bindToLifecycle(/* lifecycleOwner= */ this, vfUseCase);
                });
    }

    private void setupPermissions(Activity context) {
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            makePermissionRequest(context);
        } else {
            mCF.complete(permission);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Camera Permission Granted.");
                } else {
                    Log.i(TAG, "Camera Permission Denied.");
                }
                mCF.complete(grantResults[0]);
                return;
            }
            default: {
            }
        }
    }
}
