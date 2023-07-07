/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.viewfinder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Activity for {@link CameraViewfinder}.
 */
class CameraViewfinderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_camera)

        if (shouldRequestPermissionsAtRuntime() && !hasPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        if (null == savedInstanceState) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, CameraViewfinderFoldableFragment())
                .commit()
        }
    }

    private fun shouldRequestPermissionsAtRuntime(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private fun hasPermissions(): Boolean {
        return PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 20
        val PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        // in android 10 or later, we don't actually need WRITE_EXTERNAL_STORAGE to write to
        // the external storage.
            arrayOf(Manifest.permission.CAMERA)
        else
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
}
