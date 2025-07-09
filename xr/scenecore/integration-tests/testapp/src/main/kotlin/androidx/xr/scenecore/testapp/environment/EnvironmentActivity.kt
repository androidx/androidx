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
import androidx.xr.runtime.Session
import androidx.xr.scenecore.ExrImage
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.KhronosPbrMaterial
import androidx.xr.scenecore.KhronosPbrMaterialSpec
import androidx.xr.scenecore.Material
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.TextureSampler
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.EventType
import androidx.xr.scenecore.testapp.common.SpatialEventLog
import androidx.xr.scenecore.testapp.common.SpatialMode
import androidx.xr.scenecore.testapp.common.createSession
import androidx.xr.scenecore.testapp.common.currentTimestamp
import androidx.xr.scenecore.testapp.common.logCapabilities
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

        session = createSession(this)
        if (session == null) this.finish()
        session!!.configure(Config(Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))

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
        // handle grey skybox
        findViewById<Button>(R.id.environment_button2_1).setOnClickListener {
            setGeoAndSkybox(greySkybox, spatialEnvironmentPreference?.geometry)
            addEvent(EventType.SKYBOX_CHANGED, "Skybox set to GREY")
        }

        // handle blue skybox
        findViewById<Button>(R.id.environment_button2_2).setOnClickListener {
            setGeoAndSkybox(blueSkybox, spatialEnvironmentPreference?.geometry)
            addEvent(EventType.SKYBOX_CHANGED, "Skybox set to BLUE")
        }

        // handle unset skybox
        findViewById<Button>(R.id.environment_button2_3).setOnClickListener {
            setGeoAndSkybox(null, spatialEnvironmentPreference?.geometry)
            addEvent(EventType.SKYBOX_CHANGED, "Skybox unset")
        }
    }

    private fun geometryHandlers() {
        // handle ground geometry
        findViewById<Button>(R.id.environment_button3_1).setOnClickListener {
            setGeoAndSkybox(spatialEnvironmentPreference?.skybox, groundGeometry)
            addEvent(EventType.GEOMETRY_CHANGED, "Geometry set to GROUND")
        }

        // handle night geometry
        findViewById<Button>(R.id.environment_button3_2).setOnClickListener {
            setGeoAndSkybox(spatialEnvironmentPreference?.skybox, rockGeometry)
            addEvent(EventType.GEOMETRY_CHANGED, "Geometry set to NIGHT")
        }

        // handle animated with mesh override geometry
        findViewById<Button>(R.id.environment_button3_3).setOnClickListener {
            setGeoAndSkybox(
                spatialEnvironmentPreference?.skybox,
                dragonGeometry,
                khronosPbrMaterial,
                "Dragon",
                "Fast_Flying",
            )
            addEvent(EventType.GEOMETRY_CHANGED, "Geometry set to DRAGON")
        }

        // handle unset geometry
        findViewById<Button>(R.id.environment_button3_4).setOnClickListener {
            setGeoAndSkybox(spatialEnvironmentPreference?.skybox, null)
            addEvent(EventType.GEOMETRY_CHANGED, "Geometry unset")
        }
    }

    private fun skyboxAndGeometryHandlers() {
        // handle set geometry and skybox
        findViewById<Button>(R.id.environment_button4_1).setOnClickListener {
            setGeoAndSkybox(blueSkybox, groundGeometry)
            addEvent(
                EventType.SKYBOX_AND_GEOMETRY_CHANGED,
                "Skybox set to BLUE and geometry to GROUND",
            )
        }

        // handle unset geometry and skybox
        findViewById<Button>(R.id.environment_button4_2).setOnClickListener {
            session!!.scene.spatialEnvironment.preferredSpatialEnvironment = null
            addEvent(EventType.SKYBOX_AND_GEOMETRY_CHANGED, "Skybox and geometry unset")
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
        this.greySkybox = ExrImage.createFromZip(session!!, Paths.get("skyboxes", "GreySkybox.zip"))
        this.blueSkybox = ExrImage.createFromZip(session!!, Paths.get("skyboxes", "BlueSkybox.zip"))
        this.groundGeometry = GltfModel.create(session!!, Paths.get("models", "GroundGeometry.glb"))
        this.rockGeometry = GltfModel.create(session!!, Paths.get("models", "RocksGeometry.glb"))
        this.dragonGeometry =
            GltfModel.create(session!!, Paths.get("models", "Dragon_Evolved.gltf"))
        this.patternTexture =
            Texture.create(session!!, Paths.get("textures", "pattern.png"), TextureSampler.create())
        val spec =
            KhronosPbrMaterialSpec.create(
                lightingModel = KhronosPbrMaterialSpec.LightingModel.LIT,
                blendMode = KhronosPbrMaterialSpec.BlendMode.OPAQUE,
                doubleSidedMode = KhronosPbrMaterialSpec.DoubleSidedMode.SINGLE_SIDED,
            )
        this.khronosPbrMaterial = KhronosPbrMaterial.create(session!!, spec)
        this.khronosPbrMaterial.setBaseColorTexture(patternTexture)
    }

    private fun setGeoAndSkybox(
        skybox: ExrImage?,
        geometry: GltfModel?,
        material: Material? = null,
        meshName: String? = null,
        animationName: String? = null,
    ) {
        if (material == null && meshName == null && animationName == null) {
            spatialEnvironmentPreference =
                SpatialEnvironment.SpatialEnvironmentPreference(skybox, geometry)
        } else {
            spatialEnvironmentPreference =
                SpatialEnvironment.SpatialEnvironmentPreference(
                    skybox,
                    geometry,
                    material,
                    meshName,
                    animationName,
                )
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
        opacitySlider.addOnChangeListener { _, value, _ ->
            session!!.scene.spatialEnvironment.preferredPassthroughOpacity = value
            passthroughOpacityPreference.value = value
            currentPassthroughOpacity.value =
                session!!.scene.spatialEnvironment.currentPassthroughOpacity
            opacityTextView.text =
                opacityValueText(
                    passthroughOpacityPreference.value,
                    currentPassthroughOpacity.value,
                )
        }

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
