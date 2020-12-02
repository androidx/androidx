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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/** The main activity. */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // Possible values for this intent key (case-insensitive): "PreviewView", "CameraView".
    private static final String INTENT_EXTRA_VIEW_TYPE = "view_type";
    private static final String VIEW_TYPE_PREVIEW_VIEW = "PreviewView";
    private static final String VIEW_TYPE_CAMERA_VIEW = "CameraView";

    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
    private static final int REQUEST_CODE_PERMISSIONS = 10;

    private boolean mCheckedPermissions = false;
    private Mode mMode = Mode.CAMERA_VIEW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Get extra option for checking whether it need to be implemented with PreviewView
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            final String viewTypeString = bundle.getString(INTENT_EXTRA_VIEW_TYPE);
            final boolean isViewTypeValid =
                    viewTypeString != null && (viewTypeString.equalsIgnoreCase(
                            VIEW_TYPE_PREVIEW_VIEW) || viewTypeString.equalsIgnoreCase(
                            VIEW_TYPE_CAMERA_VIEW));
            if (isViewTypeValid && viewTypeString.equalsIgnoreCase(VIEW_TYPE_PREVIEW_VIEW)) {
                mMode = Mode.PREVIEW_VIEW;
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
                    requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.camera_view:
                mMode = Mode.CAMERA_VIEW;
                break;
            case R.id.preview_view:
                mMode = Mode.PREVIEW_VIEW;
                break;
            case R.id.camera_controller:
                mMode = Mode.CAMERA_CONTROLLER;
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
        switch (mMode) {
            case CAMERA_VIEW:
                startFragment(R.string.camera_view, new CameraViewFragment());
                break;
            case PREVIEW_VIEW:
                startFragment(R.string.preview_view, new PreviewViewFragment());
                break;
            case CAMERA_CONTROLLER:
                startFragment(R.string.camera_controller, new CameraControllerFragment());
                break;
        }
    }

    private void startFragment(int titleRes, Fragment fragment) {
        getSupportActionBar().setTitle(titleRes);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .commit();
    }

    private void report(String msg) {
        Log.d(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private enum Mode {
        CAMERA_VIEW, PREVIEW_VIEW, CAMERA_CONTROLLER
    }
}
