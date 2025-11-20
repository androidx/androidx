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

package androidx.xr.scenecore.testapp.memoryleak

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.HeadTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.createSession
import androidx.xr.scenecore.testapp.common.managers.GltfManager
import androidx.xr.scenecore.testapp.common.managers.PanelEntityManager
import androidx.xr.scenecore.testapp.common.managers.SpatialEnvironmentManager
import androidx.xr.scenecore.testapp.common.managers.SurfaceEntityManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.ref.WeakReference

class MemoryLeakActivity : AppCompatActivity() {
    private var session: Session? = null
    private lateinit var mGltfManager: GltfManager
    private lateinit var mSurfaceEntityManager: SurfaceEntityManager
    private lateinit var mSpatialEnvironmentManager: SpatialEnvironmentManager
    private lateinit var mPanelEntityManager: PanelEntityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_leak)

        session = createSession(this)
        if (session == null) this.finish()
        session!!.configure(Config(headTracking = HeadTrackingMode.LAST_KNOWN))

        val weakActivity = WeakReference(this@MemoryLeakActivity)
        // toolbar
        findViewById<Toolbar>(R.id.top_app_bar).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { weakActivity.get()?.finish() }
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(weakActivity.get()!!) }
        }

        setupMainPanel()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w("MemoryLeakActivity", "onDestroy called")
        mGltfManager.ClearListeners()
        mSurfaceEntityManager.ClearListeners()
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).setOnClickListener(null)
        findViewById<Toolbar>(R.id.top_app_bar).setNavigationOnClickListener(null)
    }

    private fun setupMainPanel() {
        // Request FSM
        findViewById<Button>(R.id.button_request_fsm).also {
            it.setOnClickListener { session!!.scene.requestFullSpaceMode() }
        }

        // Request HSM
        findViewById<Button>(R.id.button_request_hsm).also {
            it.setOnClickListener { session!!.scene.requestHomeSpaceMode() }
        }

        // Make the main panel movable.
        val movableComponent = MovableComponent.createSystemMovable(session!!, scaleInZ = false)
        session!!.scene.mainPanelEntity.addComponent(movableComponent)

        // Create the UI component managers.
        mSpatialEnvironmentManager = SpatialEnvironmentManager(session!!, this)
        mSurfaceEntityManager = SurfaceEntityManager(session!!, this)
        mGltfManager = GltfManager(session!!, this, 100, 5)
        mPanelEntityManager = PanelEntityManager(session!!, this, 1000, 100)

        // Update contentDescription for models
        mGltfManager.AddOnEntityChangedListener {
            it?.contentDescription = "Showing Gltf Model Entity"
        }
        mSurfaceEntityManager.AddOnEntityChangedListener {
            it?.contentDescription = "Showing Surface Entity"
        }
    }
}
