/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.scenecore.testapp.model

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.GltfAnimation
import androidx.xr.scenecore.GltfAnimationStartOptions
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.GltfModelNode
import androidx.xr.scenecore.KhronosPbrMaterial
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.TextureSampler
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.SpatialMode
import androidx.xr.scenecore.testapp.common.managers.SessionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import java.nio.file.Paths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n")
class GltfModelMaterialTextureActivity : AppCompatActivity() {
    private val TAG = "GltfModelMaterialTextureActivity"
    private val ANIMATION_NAME = "Fast_Flying"
    private val NODE_NAME = "Dragon"
    private val DRAGON_SCALE = 0.2f
    private val DRAGON_TRANSLATION = Vector3(0f, 0.3f, 0f)
    private var session: Session? = null
    private var spatialMode = SpatialMode.FSM

    private lateinit var dragonModel: GltfModel
    private var dragonModelEntity: GltfModelEntity? = null
    private var khronosPbrMaterial: KhronosPbrMaterial? = null
    private var patternTexture: Texture? = null
    private var selectedNode: GltfModelNode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_gltf_model_material_texture)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        session = SessionManager(this).createSession()
        if (session == null) this.finish()
        session!!.configure(Config(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        session?.scene?.keyEntity = session?.scene?.mainPanelEntity

        findViewById<Toolbar>(R.id.gltf_model_topAppBar).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { this@GltfModelMaterialTextureActivity.finish() }
            it.setTitle(getString(R.string.cuj_gltf_model_material_texture_test))
        }

        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@GltfModelMaterialTextureActivity) }
        }

        findViewById<Button>(R.id.gltf_model_toggle_hsm_fsm).also { button ->
            button.text = getString(R.string.switch_to_hsm_button_text)
            button.setOnClickListener { button.text = toggleMode() }
        }

        lifecycleScope.launch {
            loadResources()
            setupButtons()
        }
    }

    private fun setupButtons() {
        // Pose and scale sliders
        val dropdownRow = findViewById<View>(R.id.gltf_model_cardRow_dropdown)
        val matRow = findViewById<View>(R.id.gltf_model_cardRow5)
        val slidersRow = findViewById<View>(R.id.gltf_model_cardRow_sliders)
        val nodeDropdown = findViewById<AutoCompleteTextView>(R.id.node_dropdown)

        val locX = findViewById<Slider>(R.id.slider_loc_x)
        val locY = findViewById<Slider>(R.id.slider_loc_y)
        val locZ = findViewById<Slider>(R.id.slider_loc_z)
        val rotX = findViewById<Slider>(R.id.slider_rot_x)
        val rotY = findViewById<Slider>(R.id.slider_rot_y)
        val rotZ = findViewById<Slider>(R.id.slider_rot_z)
        val rotW = findViewById<Slider>(R.id.slider_rot_w)
        val scaleX = findViewById<Slider>(R.id.slider_scale_x)
        val scaleY = findViewById<Slider>(R.id.slider_scale_y)
        val scaleZ = findViewById<Slider>(R.id.slider_scale_z)

        fun updatePoseScaleFromSliders() {
            selectedNode?.let { node ->
                node.modelPose =
                    Pose(
                        Vector3(locX.value, locY.value, locZ.value),
                        Quaternion(rotX.value, rotY.value, rotZ.value, rotW.value),
                    )
                node.modelScale = Vector3(scaleX.value, scaleY.value, scaleZ.value)
            }
        }

        val sliderListener =
            Slider.OnChangeListener { _, _, fromUser -> if (fromUser) updatePoseScaleFromSliders() }

        listOf(locX, locY, locZ, rotX, rotY, rotZ, rotW, scaleX, scaleY, scaleZ).forEach {
            it.addOnChangeListener(sliderListener)
        }

        // Create Texture
        findViewById<Button>(R.id.gltf_model_button1_1).setOnClickListener {
            lifecycleScope.launch {
                patternTexture = Texture.create(session!!, Paths.get("textures", "pattern.png"))
            }
        }
        // Dispose Texture via GC
        findViewById<Button>(R.id.gltf_model_button1_2).setOnClickListener {
            patternTexture = null
            // Make GC likely to run
            requestGargabeCollection()
        }
        // Dispose Texture explicitly
        findViewById<Button>(R.id.gltf_model_button1_3).setOnClickListener {
            patternTexture?.close()
        }
        // Create Khronos PBR Material
        findViewById<Button>(R.id.gltf_model_button2_1).setOnClickListener {
            lifecycleScope.launch {
                khronosPbrMaterial = KhronosPbrMaterial.create(session!!, AlphaMode.BLEND)
            }
        }
        // Dispose Khronos PBR Material via GC
        findViewById<Button>(R.id.gltf_model_button2_2).setOnClickListener {
            khronosPbrMaterial = null
            requestGargabeCollection()
        }
        // Dispose Khronos PBR Material explicitly
        findViewById<Button>(R.id.gltf_model_button2_3).setOnClickListener {
            khronosPbrMaterial?.close()
        }
        // Set Base Color Texture
        findViewById<Button>(R.id.gltf_model_button3_1).setOnClickListener {
            val mat = khronosPbrMaterial
            val tex = patternTexture
            if (mat != null && tex != null) {
                mat.setBaseColorTexture(tex, TextureSampler())
            } else {
                Log.w(TAG, "Material or Texture not created yet")
            }
        }

        findViewById<Slider>(R.id.gltf_model_metallic_slider).addOnChangeListener {
            slider,
            value,
            fromUser ->
            khronosPbrMaterial?.setMetallicFactor(value)
        }

        // Create GLTF Model Entity
        findViewById<Button>(R.id.gltf_model_button4_1).setOnClickListener {
            dragonModelEntity
                ?: lifecycleScope.launch {
                    dragonModelEntity =
                        GltfModelEntity.create(
                            session!!,
                            dragonModel,
                            Pose(translation = DRAGON_TRANSLATION),
                        )
                    dragonModelEntity?.setScale(DRAGON_SCALE)

                    val nodeNames = dragonModelEntity!!.nodes.map { it.name ?: "NO NAME" }
                    val adapter =
                        ArrayAdapter(
                            this@GltfModelMaterialTextureActivity,
                            android.R.layout.simple_spinner_item,
                            nodeNames,
                        )
                    nodeDropdown.setAdapter(adapter)
                    dropdownRow.visibility = View.VISIBLE
                }
        }
        // Dispose GLTF Model Entity
        findViewById<Button>(R.id.gltf_model_button4_2).setOnClickListener {
            dragonModelEntity?.let {
                it.dispose()
                dragonModelEntity = null
            }
            dropdownRow.visibility = View.GONE
            matRow.visibility = View.GONE
            slidersRow.visibility = View.GONE
            selectedNode = null
            nodeDropdown.setText("")
        }
        // Toggle Animation
        findViewById<Button>(R.id.gltf_model_button4_3).setOnClickListener {
            val entity = dragonModelEntity
            if (entity != null) {
                val animation = entity.animations.find { it.name == ANIMATION_NAME }
                if (animation?.animationState == GltfAnimation.AnimationState.PLAYING) {
                    animation.stop()
                } else {
                    animation?.start(GltfAnimationStartOptions(shouldLoop = true))
                }
            }
        }
        // Node dropdown
        nodeDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedNode = dragonModelEntity?.nodes?.get(position)
            matRow.visibility = View.VISIBLE
            slidersRow.visibility = View.VISIBLE

            selectedNode?.let { node ->
                val p = node.modelPose
                val s = node.modelScale

                fun Float.snap() = Math.round(this * 100f) / 100f

                locX.value = p.translation.x.snap().coerceIn(-10f, 10f)
                locY.value = p.translation.y.snap().coerceIn(-10f, 10f)
                locZ.value = p.translation.z.snap().coerceIn(-10f, 10f)
                rotX.value = p.rotation.x.snap().coerceIn(-1f, 1f)
                rotY.value = p.rotation.y.snap().coerceIn(-1f, 1f)
                rotZ.value = p.rotation.z.snap().coerceIn(-1f, 1f)
                rotW.value = p.rotation.w.snap().coerceIn(-1f, 1f)
                scaleX.value = s.x.snap().coerceIn(0f, 10f)
                scaleY.value = s.y.snap().coerceIn(0f, 10f)
                scaleZ.value = s.z.snap().coerceIn(0f, 10f)
            }
        }
        findViewById<Button>(R.id.gltf_model_button5_1).setOnClickListener {
            val node = selectedNode
            val mat = khronosPbrMaterial
            if (node != null && mat != null) {
                try {
                    node.setMaterialOverride(mat)
                } catch (e: Exception) { // Broaden to Exception
                    android.widget.Toast.makeText(
                            this@GltfModelMaterialTextureActivity,
                            "Node does not have a mesh",
                            android.widget.Toast.LENGTH_SHORT,
                        )
                        .show()
                }
            } else {
                Log.w(TAG, "Node or Material not selected/created yet")
            }
        }
        // Clear Material Override
        findViewById<Button>(R.id.gltf_model_button5_2).setOnClickListener {
            try {
                selectedNode?.clearMaterialOverride()
            } catch (e: Exception) { // Broaden to Exception
                android.widget.Toast.makeText(
                        this@GltfModelMaterialTextureActivity,
                        "Node does not have a mesh",
                        android.widget.Toast.LENGTH_SHORT,
                    )
                    .show()
            }
        }
    }

    private suspend fun loadResources() {
        this.dragonModel = GltfModel.create(session!!, Paths.get("models", "Dragon_Evolved.gltf"))
    }

    private fun toggleMode(): String {
        when (spatialMode) {
            SpatialMode.FSM -> {
                session!!.scene.requestHomeSpaceMode()
                spatialMode = SpatialMode.HSM
                return getString(R.string.switch_to_fsm_button_text)
            }

            SpatialMode.HSM -> {
                session!!.scene.requestFullSpaceMode()
                spatialMode = SpatialMode.FSM
                return getString(R.string.switch_to_hsm_button_text)
            }
        }
    }

    private fun requestGargabeCollection() {
        // Make GC likely to run
        lifecycleScope.launch(Dispatchers.Default) {
            repeat(10) {
                System.gc()
                System.runFinalization()
                delay(300)
            }
        }
    }
}
