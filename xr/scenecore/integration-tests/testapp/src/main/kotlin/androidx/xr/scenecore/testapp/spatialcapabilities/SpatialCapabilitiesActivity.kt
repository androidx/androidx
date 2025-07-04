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

package androidx.xr.scenecore.testapp.spatialcapabilities

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.EventType
import androidx.xr.scenecore.testapp.common.SpatialEventLog
import androidx.xr.scenecore.testapp.common.SpatialMode
import androidx.xr.scenecore.testapp.common.currentTimestamp
import androidx.xr.scenecore.testapp.common.logCapabilities
import androidx.xr.scenecore.testapp.ui.EventLogRecyclerViewAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton

@SuppressLint("SetTextI18n", "RestrictedApi")
class SpatialCapabilitiesActivity : AppCompatActivity() {
    private val renderingSession: Session by lazy {
        (Session.create(this) as SessionCreateSuccess).session
    }
    private var spatialMode = SpatialMode.FSM
    private var spatialEventLogList = mutableListOf<SpatialEventLog>()
    private lateinit var eventLogView: RecyclerView
    private lateinit var eventLogRecyclerViewAdapter: EventLogRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View
        setContentView(R.layout.spatial_capabilities_panel)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // toolbar
        findViewById<Toolbar>(R.id.top_app_bar_activity_panel).also {
            setSupportActionBar(it)
            it.setTitle(R.string.cuj_spatial_capabilities_test)
            it.setNavigationOnClickListener { this.finish() }
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@SpatialCapabilitiesActivity) }
        }

        // fsm/hsm toggle
        val toggleButton = findViewById<Button>(R.id.spawn_activity_panel_button)
        toggleButton.text = getString(R.string.switch_to_hsm_button_text)
        toggleButton.setOnClickListener { toggleButton.text = toggleMode(renderingSession) }

        // Log current spatial capabilities
        val logButton = findViewById<Button>(R.id.current_spatial_capabilities_button)
        logButton.setOnClickListener {
            addNewSpatialLogEvent(
                SpatialEventLog(
                    currentTimestamp(),
                    EventType.LOG_CAPABILITIES_CLICKED.text,
                    logCapabilities(renderingSession),
                )
            )
        }

        // Listen events
        addEventListeners()

        // Create event log view
        createEventLogRecyclerView()
    }

    private fun addEventListeners() {
        renderingSession.scene.addSpatialCapabilitiesChangedListener { _ ->
            addNewSpatialLogEvent(
                SpatialEventLog(
                    currentTimestamp(),
                    EventType.CAPABILITIES_CHANGED.text,
                    logCapabilities(renderingSession),
                )
            )
        }

        renderingSession.scene.activitySpace.addOnBoundsChangedListener { bounds ->
            addNewSpatialLogEvent(
                SpatialEventLog(
                    currentTimestamp(),
                    EventType.BOUNDS_CHANGED.text,
                    "w=${bounds.width}, h=${bounds.height}, d=${bounds.depth}",
                )
            )
        }
    }

    private fun createEventLogRecyclerView() {
        eventLogRecyclerViewAdapter = EventLogRecyclerViewAdapter(spatialEventLogList)
        eventLogView = findViewById(R.id.event_log_table_view)
        eventLogView.layoutManager = LinearLayoutManager(this)
        eventLogView.adapter = eventLogRecyclerViewAdapter
    }

    private fun addNewSpatialLogEvent(spatialEventLog: SpatialEventLog) {
        spatialEventLogList.add(spatialEventLog)
        val newPosition = spatialEventLogList.size
        eventLogRecyclerViewAdapter.notifyItemInserted(newPosition)
        eventLogView.smoothScrollToPosition(newPosition)
    }

    private fun toggleMode(session: Session): String {
        when (spatialMode) {
            SpatialMode.FSM -> {
                session.scene.requestHomeSpaceMode()
                spatialMode = SpatialMode.HSM
                addNewSpatialLogEvent(
                    SpatialEventLog(currentTimestamp(), EventType.MODE_CHANGED_TO_HSM.text, "")
                )
                return getString(R.string.switch_to_fsm_button_text)
            }
            SpatialMode.HSM -> {
                session.scene.requestFullSpaceMode()
                spatialMode = SpatialMode.FSM
                addNewSpatialLogEvent(
                    SpatialEventLog(currentTimestamp(), EventType.MODE_CHANGED_TO_FSM.text, "")
                )
                return getString(R.string.switch_to_hsm_button_text)
            }
        }
    }

    companion object {
        const val ACTIVITY_NAME = "SpatialCapabilities"
    }
}
