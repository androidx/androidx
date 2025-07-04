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

package androidx.xr.scenecore.testapp.activitypanel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.SpatialCapabilities
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.createSession
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ActivityPanelActivity : AppCompatActivity() {
    private lateinit var activityPanelEntity: ActivityPanelEntity
    private var session: Session? = null
    private var secondaryPanelLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.common_test_panel)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        session = createSession(this)
        if (session == null) this.finish()

        // Set toolbar
        findViewById<Toolbar>(R.id.top_app_bar_activity_panel).also {
            setSupportActionBar(it)
            it.setTitle(getString(R.string.cuj_activity_panel_test))
            it.setNavigationOnClickListener { this.finish() }
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@ActivityPanelActivity) }
        }

        // Create activity panel entity
        activityPanelEntity =
            ActivityPanelEntity.create(session!!, IntSize2d(640, 480), ACTIVITY_NAME)

        // Set button listener
        val button: Button = findViewById(R.id.spawn_activity_panel_button)
        button.setOnClickListener {
            // Check spatial capabilities of the session
            if (
                session!!
                    .scene
                    .spatialCapabilities
                    .hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)
            ) {

                if (!secondaryPanelLaunched) {
                    // Set the pose for the activity panel
                    activityPanelEntity.setPose(Pose(Vector3(0f, 0.6f, 0f)))
                    // Create intent to launch a new activity in the panel
                    val intent = Intent(this, ActivityPanel::class.java)
                    intent.putExtra("NAV_ICON", false)
                    // Launch an activity in the panel
                    activityPanelEntity.launchActivity(intent, savedInstanceState)
                    // Add movable component
                    val movableComponent = MovableComponent.createSystemMovable(session!!)
                    activityPanelEntity.addComponent(movableComponent)
                    movableComponent.size = getSizeInLocalSpace(activityPanelEntity)
                    // Add resizeable component
                    val resizeableComponent = ResizableComponent.create(session!!)
                    activityPanelEntity.addComponent(resizeableComponent)

                    secondaryPanelLaunched = true
                }
            } else {
                Log.e(ACTIVITY_NAME, "permission denied")
            }
        }
    }

    private fun getSizeInLocalSpace(activityPanelEntity: ActivityPanelEntity): FloatSize3d {
        val scaledSize = activityPanelEntity.size
        val spaceScale = activityPanelEntity.getScale()
        return FloatSize3d(scaledSize.width / spaceScale, scaledSize.height / spaceScale)
    }

    companion object {
        const val ACTIVITY_NAME = "ActivityPanelTest"
    }
}
