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

package androidx.camera.integration.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.pipe.integration.CameraPipeConfig;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

/** The main activity. */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // Possible values for this intent key (case-insensitive): "PreviewView", "ComposeUi".
    private static final String INTENT_FRAGMENT_TYPE = "fragment_type";
    private static final String PREVIEW_VIEW_FRAGMENT = "PreviewView";
    private static final String COMPOSE_UI_FRAGMENT = "ComposeUi";

    private static final String[] REQUIRED_PERMISSIONS;
    static {
        // From Android T, skips the permission check of WRITE_EXTERNAL_STORAGE since it won't be
        // granted any more.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }
    }
    private static final int REQUEST_CODE_PERMISSIONS = 10;

    // Possible values for this intent key are the name values of LensFacing encoded as
    // strings (case-insensitive): "back", "front".
    public static final String INTENT_EXTRA_CAMERA_DIRECTION = "camera_direction";
    public static final String CAMERA_DIRECTION_BACK = "back";
    public static final String CAMERA_DIRECTION_FRONT = "front";
    // Possible values for this intent key: "preview_test_case" or "default_test_case".
    public static final String INTENT_EXTRA_E2E_TEST_CASE = "e2e_test_case";
    public static final String PREVIEW_TEST_CASE = "preview_test_case";

    // Launch the activity with the specified scale type.
    public static final String INTENT_EXTRA_SCALE_TYPE = "scale_type";
    // The default scale type is FILL_CENTER.
    public static final int DEFAULT_SCALE_TYPE_ID = 1;

    /** Intent extra representing type of camera implementation. */
    public static final String INTENT_EXTRA_CAMERA_IMPLEMENTATION = "camera_implementation";

    // Camera2 implementation.
    public static final String CAMERA2_IMPLEMENTATION_OPTION = "camera2";
    // Camera-pipe implementation.
    public static final String CAMERA_PIPE_IMPLEMENTATION_OPTION = "camera_pipe";

    private static String sCameraImplementationType;

    private boolean mCheckedPermissions = false;
    private FragmentType mFragmentType = FragmentType.CAMERA_CONTROLLER;

    @OptIn(markerClass = androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration.class)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Get extra option for checking whether it needs to be implemented with PreviewView
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            final String viewTypeString = bundle.getString(INTENT_FRAGMENT_TYPE);
            if (PREVIEW_VIEW_FRAGMENT.equalsIgnoreCase(viewTypeString)) {
                mFragmentType = FragmentType.PREVIEW_VIEW;
            } else if (COMPOSE_UI_FRAGMENT.equalsIgnoreCase(viewTypeString)) {
                mFragmentType = FragmentType.COMPOSE_UI;
            }
            // Update the app UI according to the e2e test case.
            String testItem = bundle.getString(INTENT_EXTRA_E2E_TEST_CASE);
            if (testItem != null) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().hide();
                }
            }
            String cameraImplementation = bundle.getString(INTENT_EXTRA_CAMERA_IMPLEMENTATION);
            if (cameraImplementation != null && sCameraImplementationType == null) {
                if (cameraImplementation.equals(CAMERA2_IMPLEMENTATION_OPTION)) {
                    ProcessCameraProvider.configureInstance(Camera2Config.defaultConfig());
                    sCameraImplementationType = cameraImplementation;
                } else if (cameraImplementation.equals(CAMERA_PIPE_IMPLEMENTATION_OPTION)) {
                    ProcessCameraProvider.configureInstance(
                            CameraPipeConfig.defaultConfig());
                    sCameraImplementationType = cameraImplementation;
                } else {
                    throw new IllegalArgumentException("Failed to configure the CameraProvider "
                            + "using unknown " + cameraImplementation
                            + " implementation option in " + TAG + ".");
                }
            }
        }
        // TODO(b/173019455): make this penaltyDeath after we fix the IO in test apps.
        StrictMode.ThreadPolicy threadPolicy =
                new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build();
        StrictMode.setThreadPolicy(threadPolicy);
        if (null == savedInstanceState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (allPermissionsGranted()) {
                    startFragment();
                } else if (!mCheckedPermissions) {
                    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                            REQUEST_CODE_PERMISSIONS);
                    mCheckedPermissions = true;
                }
            } else {
                startFragment();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startFragment();
            } else {
                report("Permissions not granted by the user.");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.preview_view:
                mFragmentType = FragmentType.PREVIEW_VIEW;
                break;
            case R.id.camera_controller:
                mFragmentType = FragmentType.CAMERA_CONTROLLER;
                break;
            case R.id.transform:
                mFragmentType = FragmentType.TRANSFORM;
                break;
            case R.id.compose_ui:
                mFragmentType = FragmentType.COMPOSE_UI;
                break;
            case R.id.mlkit:
                mFragmentType = FragmentType.MLKIT;
                break;
        }
        startFragment();
        return true;
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startFragment() {
        switch (mFragmentType) {
            case PREVIEW_VIEW:
                startFragment(R.string.preview_view, new PreviewViewFragment());
                break;
            case CAMERA_CONTROLLER:
                startFragment(R.string.camera_controller, new CameraControllerFragment());
                break;
            case TRANSFORM:
                startFragment(R.string.transform, new TransformFragment());
                break;
            case COMPOSE_UI:
                startFragment(R.string.compose_ui, new ComposeUiFragment());
                break;
            case MLKIT:
                startFragment(R.string.mlkit, new MlKitFragment());
                break;
        }
    }

    private void startFragment(int titleRes, Fragment fragment) {
        getSupportActionBar().setTitle(titleRes);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.content, fragment);
        if (mCheckedPermissions && Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            // For the codes, check the b/182981155 for the detail.
            fragmentTransaction.commitAllowingStateLoss();
        } else {
            fragmentTransaction.commit();
        }
    }

    private void report(String msg) {
        Log.d(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private enum FragmentType {
        PREVIEW_VIEW, CAMERA_CONTROLLER, TRANSFORM, COMPOSE_UI, MLKIT
    }
}
