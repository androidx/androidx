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

package androidx.xr.scenecore.testapp.environment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.ExrImage
import androidx.xr.scenecore.GltfAnimationStartOptions
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.KhronosPbrMaterial
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.TextureSampler
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.EventType
import androidx.xr.scenecore.testapp.common.SpatialEventLog
import androidx.xr.scenecore.testapp.common.SpatialMode
import androidx.xr.scenecore.testapp.common.currentTimestamp
import androidx.xr.scenecore.testapp.common.logCapabilities
import androidx.xr.scenecore.testapp.common.managers.SessionManager
import androidx.xr.scenecore.testapp.ui.EventLogRecyclerViewAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import java.nio.file.Paths
import java.text.DecimalFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n", "RestrictedApi")
class EnvironmentActivity : AppCompatActivity() {
    private val TAG = "EnvironmentTestActivity"
    private var session: Session? = null
    private var spatialMode = SpatialMode.FSM
    private var spatialEventLogList = mutableListOf<SpatialEventLog>()
    private lateinit var eventLogView: RecyclerView
    private lateinit var eventLogRecyclerViewAdapter: EventLogRecyclerViewAdapter
    private var currentPassthroughOpacity = MutableStateFlow(0.0f)
    private var passthroughOpacityPreference = MutableStateFlow(0.0f)
    private var geometryEntity: GltfModelEntity? = null
    private lateinit var greySkybox: ExrImage
    private lateinit var blueSkybox: ExrImage
    private lateinit var groundGeometry: GltfModel
    private lateinit var rockGeometry: GltfModel
    private lateinit var dragonGeometry: GltfModel
    private lateinit var khronosPbrMaterial: KhronosPbrMaterial
    private lateinit var patternTexture: Texture
    private var spatialEnvironmentPreference: SpatialEnvironment.SpatialEnvironmentPreference? =
        null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_environment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        session = SessionManager(this).createSession()
        if (session == null) this.finish()
        session!!.configure(Config(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        session?.scene?.keyEntity = session?.scene?.mainPanelEntity

        // toolbar
        findViewById<Toolbar>(R.id.environment_topAppBar).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { this@EnvironmentActivity.finish() }
            it.setTitle(getString(R.string.cuj_environment_test))
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@EnvironmentActivity) }
        }

        // fsm/hsm toggle
        findViewById<Button>(R.id.environment_toggle_hsm_fsm).also { button ->
            button.text = getString(R.string.switch_to_hsm_button_text)
            button.setOnClickListener { button.text = toggleMode() }
        }

        // Create event log view
        createEventLogRecyclerView()

        // Toggle passthrough
        val togglePassthroughButton = findViewById<Button>(R.id.environment_toggle_passthrough)
        togglePassthroughButton.setOnClickListener { togglePassthrough() }
        togglePassthroughButton.visibility = View.GONE

        // Event listeners
        addSpatialEventListeners()

        // Opacity
        manageOpacity()

        // Set initial environment preference
        session!!.scene.spatialEnvironment.preferredSpatialEnvironment = null
        spatialEnvironmentPreference =
            session!!.scene.spatialEnvironment.preferredSpatialEnvironment

        // handle Log capabilities
        findViewById<Button>(R.id.environment_log_spatial_capabilities).setOnClickListener {
            addEvent(EventType.CAPABILITIES_CHANGED, logCapabilities(session!!))
        }

        // Add other handlers
        lifecycleScope.launch {
            // load environment resources
            loadResources()

            // add skybox handlers
            skyBoxButtonHandlers()

            // add geometry handlers
            geometryHandlers()

            // add geometry and skybox handlers
            skyboxAndGeometryHandlers()
        }
    }

    private fun skyBoxButtonHandlers() {
        // Load skybox from a Path
        val loadPathButton = findViewById<Button>(R.id.environment_load_path)
        loadPathButton.setOnClickListener {
            lifecycleScope.launch {
                greySkybox =
                    ExrImage.createFromZip(session!!, Paths.get("skyboxes", "GreySkybox.zip"))
                addEvent(EventType.SKYBOX_CHANGED, "Grey Skybox loaded from Path")
                findViewById<Button>(R.id.environment_button2_1).isEnabled = true
            }
        }
        loadPathButton.isEnabled = true

        // Load skybox from a bytes
        val loadBytesButton = findViewById<Button>(R.id.environment_load_bytes)
        loadBytesButton.setOnClickListener {
            lifecycleScope.launch {
                val bytes = assets.open("skyboxes/BlueSkybox.zip").readBytes()
                blueSkybox = ExrImage.createFromZip(session!!, bytes, "BlueSkybox.zip")
                addEvent(EventType.SKYBOX_CHANGED, "Blue Skybox loaded from Bytes")
                findViewById<Button>(R.id.environment_button2_2).isEnabled = true
                findViewById<Button>(R.id.environment_button4_1).isEnabled = true
            }
        }
        loadBytesButton.isEnabled = true

        // handle grey skybox
        findViewById<Button>(R.id.environment_button2_1).setOnClickListener {
            val currentGeometry = spatialEnvironmentPreference?.geometry
            val currentEntity = if (currentGeometry == null) geometryEntity else null

            setGeoAndSkybox(greySkybox, currentGeometry, currentEntity)
            addEvent(EventType.SKYBOX_CHANGED, "Skybox set to BAR")
        }

        // handle blue skybox
        findViewById<Button>(R.id.environment_button2_2).setOnClickListener {
            val currentGeometry = spatialEnvironmentPreference?.geometry
            val currentEntity = if (currentGeometry == null) geometryEntity else null

            setGeoAndSkybox(blueSkybox, currentGeometry, currentEntity)
            addEvent(EventType.SKYBOX_CHANGED, "Skybox set to BLUE")
        }

        // handle unset skybox
        findViewById<Button>(R.id.environment_button2_3).setOnClickListener {
            val currentGeometry = spatialEnvironmentPreference?.geometry
            val currentEntity = if (currentGeometry == null) geometryEntity else null

            setGeoAndSkybox(null, currentGeometry, currentEntity)
            addEvent(EventType.SKYBOX_CHANGED, "Skybox unset (set to black)")
        }
    }

    private fun geometryHandlers() {
        // handle ground geometry
        findViewById<Button>(R.id.environment_button3_1).setOnClickListener {
            setGeoAndSkybox(spatialEnvironmentPreference?.skybox, groundGeometry)
            addEvent(EventType.GEOMETRY_CHANGED, "Geometry set to GROUND")
        }

        // handle rock geometry
        findViewById<Button>(R.id.environment_button3_2).setOnClickListener {
            setGeoAndSkybox(spatialEnvironmentPreference?.skybox, rockGeometry)
            addEvent(EventType.GEOMETRY_CHANGED, "Geometry set to ROCKS")
        }

        // handle animated with mesh override geometry
        findViewById<Button>(R.id.environment_button3_3).setOnClickListener {
            val dragonEntity = GltfModelEntity.create(session!!, dragonGeometry)
            geometryEntity = dragonEntity
            dragonEntity.setEnabled(false)
            dragonEntity.nodes.find { it.name == "Dragon" }?.setMaterialOverride(khronosPbrMaterial)
            dragonEntity.animations
                .find { it.name == "Fast_Flying" }
                ?.start(GltfAnimationStartOptions(shouldLoop = true))

            setGeoAndSkybox(spatialEnvironmentPreference?.skybox, dragonGeometry, dragonEntity)
            addEvent(EventType.GEOMETRY_CHANGED, "Geometry set to DRAGON")
        }

        // handle unset geometry
        findViewById<Button>(R.id.environment_button3_4).setOnClickListener {
            setGeoAndSkybox(spatialEnvironmentPreference?.skybox, null, null)
            geometryEntity = null
            addEvent(EventType.GEOMETRY_CHANGED, "Geometry unset (no Geometry visible)")
        }
    }

    private fun skyboxAndGeometryHandlers() {
        // handle set geometry and skybox
        findViewById<Button>(R.id.environment_button4_1).setOnClickListener {
            setGeoAndSkybox(blueSkybox, groundGeometry)
            geometryEntity = null
            addEvent(
                EventType.SKYBOX_AND_GEOMETRY_CHANGED,
                "Skybox set to BLUE and geometry to GROUND",
            )
        }

        // handle unset geometry and skybox
        findViewById<Button>(R.id.environment_button4_2).setOnClickListener {
            session!!.scene.spatialEnvironment.preferredSpatialEnvironment = null
            geometryEntity = null
            addEvent(
                EventType.SKYBOX_AND_GEOMETRY_CHANGED,
                "Skybox and Geometry reverted to Home Environment",
            )
        }
    }

    private fun addSpatialEventListeners() {
        // Listener for spatial capabilities
        session!!.scene.addSpatialCapabilitiesChangedListener { _ ->
            addEvent(EventType.CAPABILITIES_CHANGED, logCapabilities(session!!))
        }
        // Listener for bounds change
        session!!.scene.activitySpace.addOnBoundsChangedListener { bounds ->
            addEvent(
                EventType.BOUNDS_CHANGED,
                "w=${bounds.width}, h=${bounds.height}, d=${bounds.depth}",
            )
            if (bounds.width == Float.POSITIVE_INFINITY) {
                spatialMode = SpatialMode.FSM
            }
        }
    }

    private suspend fun loadResources() {
        this.groundGeometry = GltfModel.create(session!!, Paths.get("models", "GroundGeometry.glb"))
        this.rockGeometry = GltfModel.create(session!!, Paths.get("models", "RocksGeometry.glb"))
        this.dragonGeometry =
            GltfModel.create(session!!, Paths.get("models", "Dragon_Evolved.gltf"))
        this.patternTexture = Texture.create(session!!, Paths.get("textures", "pattern.png"))
        this.khronosPbrMaterial = KhronosPbrMaterial.create(session!!, AlphaMode.OPAQUE)
        this.khronosPbrMaterial.setBaseColorTexture(patternTexture, TextureSampler())
    }

    private fun setGeoAndSkybox(
        skybox: ExrImage?,
        geometry: GltfModel?,
        geometryEntity: GltfModelEntity? = null,
    ) {
        if (geometryEntity == null) {
            spatialEnvironmentPreference =
                SpatialEnvironment.SpatialEnvironmentPreference(skybox, geometry)
        } else {
            spatialEnvironmentPreference =
                SpatialEnvironment.SpatialEnvironmentPreference(skybox, null, geometryEntity)
        }
        session!!.scene.spatialEnvironment.preferredSpatialEnvironment =
            spatialEnvironmentPreference
    }

    private fun toggleMode(): String {
        when (spatialMode) {
            SpatialMode.FSM -> {
                session!!.scene.requestHomeSpaceMode()
                spatialMode = SpatialMode.HSM
                addEvent(EventType.MODE_CHANGED_TO_HSM, "")
                return getString(R.string.switch_to_fsm_button_text)
            }

            SpatialMode.HSM -> {
                session!!.scene.requestFullSpaceMode()
                spatialMode = SpatialMode.FSM
                addEvent(EventType.MODE_CHANGED_TO_FSM, "")
                return getString(R.string.switch_to_hsm_button_text)
            }
        }
    }

    @SuppressLint("SetTextI18n", "RestrictedApi")
    private fun manageOpacity() {
        val opacityTextView = findViewById<TextView>(R.id.sliderValueTextView)
        passthroughOpacityPreference.value = 0.0f
        session!!.scene.spatialEnvironment.preferredPassthroughOpacity =
            passthroughOpacityPreference.value
        currentPassthroughOpacity.value =
            session!!.scene.spatialEnvironment.currentPassthroughOpacity
        opacityTextView.text =
            opacityValueText(passthroughOpacityPreference.value, currentPassthroughOpacity.value)

        val opacitySlider = findViewById<Slider>(R.id.environment_mySlider)
        opacitySlider.addOnSliderTouchListener(
            object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {}

                override fun onStopTrackingTouch(slider: Slider) {
                    Log.i(TAG, "Passthrough opacity slider set to value: ${slider.value}")
                    session!!.scene.spatialEnvironment.preferredPassthroughOpacity = slider.value
                    passthroughOpacityPreference.value = slider.value
                    currentPassthroughOpacity.value =
                        session!!.scene.spatialEnvironment.currentPassthroughOpacity
                    opacityTextView.text =
                        opacityValueText(
                            passthroughOpacityPreference.value,
                            currentPassthroughOpacity.value,
                        )
                }
            }
        )

        session!!.scene.spatialEnvironment.addOnPassthroughOpacityChangedListener { newOpacity ->
            currentPassthroughOpacity.value = newOpacity
            opacityTextView.text =
                opacityValueText(
                    passthroughOpacityPreference.value,
                    currentPassthroughOpacity.value,
                )
            addEvent(
                EventType.OPACITY_CHANGED,
                opacityValueText(
                    passthroughOpacityPreference.value,
                    currentPassthroughOpacity.value,
                    ", ",
                ),
            )
        }

        // Unset opacity preference
        val unsetOpacityPrefButton = findViewById<Button>(R.id.environment_sliderButton)
        unsetOpacityPrefButton.setOnClickListener {
            session!!.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
            opacitySlider.value = 0f
            passthroughOpacityPreference.value =
                session!!.scene.spatialEnvironment.preferredPassthroughOpacity
            currentPassthroughOpacity.value =
                session!!.scene.spatialEnvironment.currentPassthroughOpacity
            opacityTextView.text =
                opacityValueText(
                    passthroughOpacityPreference.value,
                    currentPassthroughOpacity.value,
                )
            addEvent(
                EventType.OPACITY_CHANGED,
                opacityValueText(
                    passthroughOpacityPreference.value,
                    currentPassthroughOpacity.value,
                    ", ",
                ),
            )
        }
    }

    private fun createEventLogRecyclerView() {
        eventLogRecyclerViewAdapter = EventLogRecyclerViewAdapter(spatialEventLogList)
        eventLogView = findViewById(R.id.environment_event_log_table_view)
        eventLogView.layoutManager = LinearLayoutManager(this)
        eventLogView.adapter = eventLogRecyclerViewAdapter
    }

    private fun addNewSpatialLogEvent(spatialEventLog: SpatialEventLog) {
        spatialEventLogList.add(spatialEventLog)
        val newPosition = spatialEventLogList.size
        eventLogRecyclerViewAdapter.notifyItemInserted(newPosition)
        eventLogView.smoothScrollToPosition(newPosition)
    }

    private fun addEvent(eventType: EventType, text: String) {
        addNewSpatialLogEvent(SpatialEventLog(currentTimestamp(), eventType.text, text))
    }

    private fun togglePassthrough() {
        val lastApiCall = "togglePassthrough"
        Log.i(TAG, lastApiCall)
        if (session!!.scene.spatialEnvironment.currentPassthroughOpacity > 0) {
            session!!.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
        } else {
            session!!.scene.spatialEnvironment.preferredPassthroughOpacity = 1.0f
        }
    }

    @SuppressLint("SetTextI18n")
    private fun opacityValueText(
        preference: Float,
        actual: Float,
        separator: String = "\n",
    ): String {
        val decimalFormat = DecimalFormat("#.##")
        val p = decimalFormat.format(preference)
        val a = decimalFormat.format(actual)
        return "Opacity Preference: $p" + separator + "Current Actual Opacity: $a"
    }
}
