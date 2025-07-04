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

package androidx.xr.scenecore.testapp.anchorentity

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.PlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.createSession
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.launch

class AnchorEntityActivity : AppCompatActivity() {
    private var session: Session? = null
    private lateinit var anchorEntity: AnchorEntity
    private lateinit var xyzModel: GltfModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = createSession(this)
        if (session == null) this.finish()

        // View
        setContentView(R.layout.common_test_panel)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // toolbar
        findViewById<Toolbar>(R.id.top_app_bar_activity_panel).also {
            setSupportActionBar(it)
            it.setTitle(resources.getString(R.string.cuj_anchor_test))
            it.setNavigationOnClickListener { this.finish() }
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@AnchorEntityActivity) }
        }

        // Spawn button
        val button: Button = findViewById(R.id.spawn_activity_panel_button)
        button.text = getString(R.string.spawn_anchor_entity_button_text)
        button.setOnClickListener { createAnchorEntity() }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun createAnchorEntity() {
        lifecycleScope.launch {
            session!!.configure(
                Config(planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
            )

            xyzModel = GltfModel.create(session!!, XYZ_ARROWS_MODEL)

            // Create anchored gltf entity
            anchorEntity =
                AnchorEntity.create(
                    session!!,
                    FloatSize2d(0.1f, 0.1f),
                    PlaneOrientation.ANY,
                    PlaneSemanticType.ANY,
                )
            GltfModelEntity.create(session!!, xyzModel, Pose.Identity).also {
                it.setScale(1f)
                anchorEntity.addChild(it)
            }
        }
    }

    companion object {
        val XYZ_ARROWS_MODEL: Path = Paths.get("models", "xyzArrows.glb")
    }
}
