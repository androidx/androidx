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

package androidx.camera.integration.diagnose

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView

class MainActivity : AppCompatActivity() {

    private lateinit var cameraController: LifecycleCameraController
    private lateinit var previewView: PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // TODO: request CAMERA permission and fail gracefully if not granted.

        // Setup CameraX
        previewView = findViewById(R.id.preview_view)
        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        previewView.controller = cameraController

        // Setup UI events
        findViewById<Button>(R.id.capture).setOnClickListener {
            // TODO: handle capture button click event following examples
            //  in CameraControllerFragment.
        }
    }
}