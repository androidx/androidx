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

import static androidx.camera.integration.core.CameraXService.ACTION_BIND_USE_CASES;
import static androidx.camera.integration.core.CameraXService.EXTRA_IMAGE_ANALYSIS_ENABLED;
import static androidx.camera.integration.core.CameraXService.EXTRA_IMAGE_CAPTURE_ENABLED;
import static androidx.camera.integration.core.CameraXService.EXTRA_VIDEO_CAPTURE_ENABLED;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An activity to configure CameraXService.
 *
 * <p>It will:
 * <ul>
 *   <li>Grant runtime permissions.</li>
 *   <li>Start the CameraXService automatically.</li>
 *   <li>Control which use cases are bound to the CameraXService lifecycle.</li>
 * </ul>
 */
public class ServiceSettingActivity extends AppCompatActivity {

    private static final String[] REQUIRED_PERMISSIONS;

    static {
        List<String> permissions = new ArrayList<>();
        permissions.add(android.Manifest.permission.CAMERA);
        permissions.add(android.Manifest.permission.RECORD_AUDIO);
        // From Android T, skips the permission check of WRITE_EXTERNAL_STORAGE since it won't be
        // granted any more.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            // From Android T, POST_NOTIFICATIONS is required for foreground service to post
            // notification.
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        REQUIRED_PERMISSIONS = permissions.toArray(new String[0]);
    }

    private ToggleButton mButtonVideo;
    private ToggleButton mButtonImage;
    private ToggleButton mButtonAnalysis;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_service_setting);
        mButtonVideo = findViewById(R.id.video_toggle);
        mButtonImage = findViewById(R.id.image_toggle);
        mButtonAnalysis = findViewById(R.id.analysis_toggle);

        mButtonVideo.setOnClickListener(v -> bindUseCases());
        mButtonImage.setOnClickListener(v -> bindUseCases());
        mButtonAnalysis.setOnClickListener(v -> bindUseCases());

        grantPermissions();
    }

    private void onPermissionGranted() {
        launchService();
        bindUseCases();
    }

    private void launchService() {
        Intent intent = createServiceIntent(null);
        ContextCompat.startForegroundService(this, intent);
    }

    private void bindUseCases() {
        Intent intent = createServiceIntent(ACTION_BIND_USE_CASES);
        intent.putExtra(EXTRA_VIDEO_CAPTURE_ENABLED, mButtonVideo.isChecked());
        intent.putExtra(EXTRA_IMAGE_CAPTURE_ENABLED, mButtonImage.isChecked());
        intent.putExtra(EXTRA_IMAGE_ANALYSIS_ENABLED, mButtonAnalysis.isChecked());
        ContextCompat.startForegroundService(this, intent);
    }

    @NonNull
    private Intent createServiceIntent(@Nullable String action) {
        Intent intent = new Intent(this, CameraXService.class);
        intent.setAction(action);
        return intent;
    }

    private boolean isPermissionMissing() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    private void grantPermissions() {
        if (isPermissionMissing()) {
            ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        for (String permission : REQUIRED_PERMISSIONS) {
                            if (!Objects.requireNonNull(result.get(permission))) {
                                Toast.makeText(this, "Camera permission denied.",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                                return;
                            }
                        }
                        onPermissionGranted();
                    });

            permissionLauncher.launch(REQUIRED_PERMISSIONS);
        } else {
            onPermissionGranted();
        }
    }
}
