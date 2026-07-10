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

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.testing.impl.util.EdgeToEdgeUtil;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;

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

    private static final String KEY_FRAGMENT_TYPE = "fragment_type";

    private boolean mCheckedPermissions = false;
    private FragmentType mFragmentType = FragmentType.CAMERA_CONTROLLER;

    @OptIn(markerClass = androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration.class)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdgeUtil.enableEdgeToEdge(this, R.id.content, Collections.emptyList());
        // Get extra option for checking whether it needs to be implemented with PreviewView
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            parseFragmentType(bundle);
            // Update the app UI according to the e2e test case.
            String testItem = bundle.getString(INTENT_EXTRA_E2E_TEST_CASE);
            if (testItem != null) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().hide();
                }
            }
        }

        if (savedInstanceState != null) {
            mFragmentType = FragmentType.values()[savedInstanceState.getInt(KEY_FRAGMENT_TYPE)];
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getTitleRes(mFragmentType));
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_FRAGMENT_TYPE, mFragmentType.ordinal());
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String @NonNull [] permissions, int @NonNull [] grantResults) {
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
        int itemId = item.getItemId();
        if (itemId == R.id.preview_view) {
            mFragmentType = FragmentType.PREVIEW_VIEW;
        } else if (itemId == R.id.camera_controller) {
            mFragmentType = FragmentType.CAMERA_CONTROLLER;
        } else if (itemId == R.id.transform) {
            mFragmentType = FragmentType.TRANSFORM;
        } else if (itemId == R.id.compose_ui) {
            mFragmentType = FragmentType.COMPOSE_UI;
        } else if (itemId == R.id.mlkit) {
            mFragmentType = FragmentType.MLKIT;
        } else if (itemId == R.id.effects) {
            mFragmentType = FragmentType.EFFECTS;
        } else if (itemId == R.id.overlay_effect) {
            mFragmentType = FragmentType.OVERLAY_EFFECTS;
        } else if (itemId == R.id.media3effect) {
            mFragmentType = FragmentType.MEDIA3_EFFECT;
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

    private void parseFragmentType(@NonNull Bundle bundle) {
        final String viewTypeString = bundle.getString(INTENT_FRAGMENT_TYPE);
        if (PREVIEW_VIEW_FRAGMENT.equalsIgnoreCase(viewTypeString)) {
            mFragmentType = FragmentType.PREVIEW_VIEW;
        } else if (COMPOSE_UI_FRAGMENT.equalsIgnoreCase(viewTypeString)) {
            mFragmentType = FragmentType.COMPOSE_UI;
        }
    }

    private void startFragment() {
        Fragment fragment;
        switch (mFragmentType) {
            case PREVIEW_VIEW:
                fragment = new PreviewViewFragment();
                break;
            case CAMERA_CONTROLLER:
                fragment = new CameraControllerFragment();
                break;
            case TRANSFORM:
                fragment = new TransformFragment();
                break;
            case COMPOSE_UI:
                fragment = new ComposeUiFragment();
                break;
            case MLKIT:
                fragment = new MlKitFragment();
                break;
            case EFFECTS:
                fragment = new EffectsFragment();
                break;
            case OVERLAY_EFFECTS:
                fragment = new OverlayEffectFragment();
                break;
            case MEDIA3_EFFECT:
                fragment = new Media3EffectsFragment();
                break;
            default:
                throw new IllegalArgumentException("Unknown fragment type: " + mFragmentType);
        }
        startFragment(getTitleRes(mFragmentType), fragment);
    }

    private int getTitleRes(FragmentType type) {
        switch (type) {
            case PREVIEW_VIEW:
                return R.string.preview_view;
            case CAMERA_CONTROLLER:
                return R.string.camera_controller;
            case TRANSFORM:
                return R.string.transform;
            case COMPOSE_UI:
                return R.string.compose_ui;
            case MLKIT:
                return R.string.mlkit;
            case EFFECTS:
                return R.string.effects;
            case OVERLAY_EFFECTS:
                return R.string.overlay_effect;
            case MEDIA3_EFFECT:
                return R.string.media3_effect;
            default:
                throw new IllegalArgumentException("Unknown fragment type: " + type);
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
        PREVIEW_VIEW, CAMERA_CONTROLLER, TRANSFORM, COMPOSE_UI, MLKIT, EFFECTS, OVERLAY_EFFECTS,
        MEDIA3_EFFECT
    }
}
