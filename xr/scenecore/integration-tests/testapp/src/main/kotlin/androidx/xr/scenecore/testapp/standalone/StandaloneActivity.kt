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

package androidx.xr.scenecore.testapp.standalone

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.createSession
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.nio.file.Paths
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n", "RestrictedApi")
class StandaloneActivity : AppCompatActivity() {

    private var session: Session? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.common_test_panel)

        session = createSession(this)
        if (session == null) this.finish()

        // Set toolbar
        val toolbar: Toolbar = findViewById(R.id.top_app_bar_activity_panel)
        setSupportActionBar(toolbar)
        toolbar.setTitle(getString(R.string.cuj_standalone_test))
        toolbar.setNavigationOnClickListener { this.finish() }

        // Hide center button
        findViewById<Button>(R.id.spawn_activity_panel_button).visibility = View.GONE

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@StandaloneActivity) }
        }

        // Create a single panel with text
        @SuppressLint("InflateParams")
        val panelEntityView = layoutInflater.inflate(R.layout.standalone_panel, null)
        val panelEntity =
            PanelEntity.create(
                session!!,
                panelEntityView,
                IntSize2d(720, 480),
                "panel_entity",
                Pose(Vector3(0f, -0.25f, 0.5f)),
            )
        panelEntity.parent = session!!.scene.activitySpace

        lifecycleScope.launch {
            // load 3D Model
            val model = load3DModel()
            createModelSolarSystem(session!!, model)
        }
    }

    private suspend fun load3DModel(): GltfModel {
        return GltfModel.create(session!!, Paths.get("models", "Dragon_Evolved.gltf"))
    }

    private fun createModelSolarSystem(session: Session, model: GltfModel) {
        val sunEntity = GltfModelEntity.create(session, model, Pose(Vector3(-0.5f, 0.5f, -0.5f)))
        sunEntity.parent = session.scene.activitySpace
        // Each child is scaled down relative to the parent to make it more visually clear which
        // entities are the "sun", "planet", and "moon".
        sunEntity.setScale(0.50f) // Scale down the sun entity so everything fits in the FOV better
        val planetEntity = GltfModelEntity.create(session, model, Pose(Vector3(-1f, 2f, -0.5f)))
        planetEntity.parent = sunEntity
        planetEntity.setScale(0.5f)
        val moonEntity = GltfModelEntity.create(session, model, Pose(Vector3(-1.5f, 2f, -0.5f)))
        moonEntity.parent = planetEntity
        moonEntity.setScale(0.5f)

        orbitModelAroundParent(planetEntity, 3f, 0f, 20000f)
        orbitModelAroundParent(moonEntity, 2f, 1.67f, 5000f)
    }

    // TODO: b/339450306 - Simply update parent's rotation once math library is added to SceneCore
    private fun orbitModelAroundParent(
        modelEntity: GltfModelEntity,
        radius: Float,
        startAngle: Float,
        rotateTimeMs: Float,
    ) {
        lifecycleScope.launch {
            val pi = 3.14159F
            val timeSource = TimeSource.Monotonic
            val startTime = timeSource.markNow()

            while (true) {
                delay(16L)
                val deltaAngle =
                    (2 * pi) * ((timeSource.markNow() - startTime).inWholeMilliseconds) /
                        rotateTimeMs

                val angle = startAngle + deltaAngle
                val pos = Vector3(radius * cos(angle), 0F, radius * sin(angle))
                modelEntity.setPose(Pose(pos, Quaternion.Identity))
            }
        }
    }
}
