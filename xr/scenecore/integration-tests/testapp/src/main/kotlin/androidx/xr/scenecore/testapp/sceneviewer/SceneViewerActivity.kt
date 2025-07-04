/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.testapp.sceneviewer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.xr.scenecore.testapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

@Suppress("GlobalCoroutineDispatchers")
class SceneViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.common_test_panel)
        Log.i("CREATE_ACTIVITY", "created")

        // Toolbar
        findViewById<Toolbar>(R.id.top_app_bar_activity_panel).also {
            setSupportActionBar(it)
            it.setTitle(getString(R.string.cuj_scene_viewer_test))
            it.setNavigationOnClickListener { this.finish() }
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@SceneViewerActivity) }
        }

        // Spawn Scene Viewer
        findViewById<Button>(R.id.spawn_activity_panel_button).also {
            it.text = getString(R.string.intent_into_scene_viewer)
            it.setOnClickListener {
                val sceneViewerIntent = Intent(Intent.ACTION_VIEW)
                val intentUri =
                    Uri.parse("https://arvr.google.com/scene-viewer/1.2")
                        .buildUpon()
                        .appendQueryParameter("file", ALT_THREE_D_MODEL_URL)
                        .build()

                Log.i("SCENE_VIEWER_INTENT", intentUri.toString())
                sceneViewerIntent.setData(intentUri)
                try {
                    startActivity(sceneViewerIntent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(
                            this,
                            "Failed to load the 3D model. No activity could handle the intent.",
                            Toast.LENGTH_LONG,
                        )
                        .show()
                }
            }
        }
    }

    private companion object {
        const val ALT_THREE_D_MODEL_URL =
            "https://assets.science.nasa.gov/content/dam/science/psd/mars/resources" +
                "/gltf_files/25042_Perseverance.glb?emrc=67ddb74ba1d27"
        const val THREE_D_MODEL_URL =
            "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master" +
                "/2.0/FlightHelmet/glTF/FlightHelmet.gltf"
        const val MIME_TYPE = "model/gltf-binary"
    }
}
