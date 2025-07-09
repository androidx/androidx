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

package androidx.xr.scenecore.testapp.fsmhsmtransition

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.ExrImage
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeListener
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.createSession
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.nio.file.Paths
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n", "RestrictedApi")
class FsmHsmTransitionActivity : AppCompatActivity() {

    private var session: Session? = null
    private var inFsm: Boolean = true
    private var resizableActive: Boolean = false
    private var movableActive: Boolean = false
    private var skyboxActive: Boolean = false
    private var skybox: ExrImage? = null
    private var spatialEnvironmentPreference: SpatialEnvironment.SpatialEnvironmentPreference? =
        null

    private fun mainPanelPixelDimensionsString(): String {
        val width = session!!.scene.mainPanelEntity.size.width
        val height = session!!.scene.mainPanelEntity.size.height
        return "{w:$width, h:$height}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fsm_hsm_transition)

        session = createSession(this)
        if (session == null) this.finish()

        // Set visibility of components per mode
        componentVisibility()

        // Set and get initial spatial environment preference
        session!!.scene.spatialEnvironment.preferredSpatialEnvironment = null
        spatialEnvironmentPreference =
            session!!.scene.spatialEnvironment.preferredSpatialEnvironment

        // Set initial main panel dimensions in the text view
        findViewById<TextView>(R.id.text_main_panel_dimensions_value).text =
            mainPanelPixelDimensionsString()

        // Toolbar action
        findViewById<Toolbar>(R.id.top_app_bar_activity_panel).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { this.finish() }
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@FsmHsmTransitionActivity) }
        }

        // Request FSM
        findViewById<Button>(R.id.button_request_fsm).also {
            it.setOnClickListener {
                session!!.scene.requestFullSpaceMode()
                inFsm = true
                componentVisibility()
            }
        }

        // Request HSM
        findViewById<Button>(R.id.button_request_hsm).also {
            it.setOnClickListener {
                session!!.scene.requestHomeSpaceMode()
                inFsm = false
                componentVisibility()
            }
        }

        // Movable switch
        findViewById<SwitchMaterial>(R.id.switch_movable_in_fsm).also {
            val movableComponent = MovableComponent.createSystemMovable(session!!)
            it.setOnCheckedChangeListener { _, isOn ->
                movableComponent.size = session!!.scene.mainPanelEntity.size.to3d()
                when (isOn) {
                    true ->
                        movableActive =
                            session!!.scene.mainPanelEntity.addComponent(movableComponent)
                    false ->
                        movableActive.let {
                            session!!.scene.mainPanelEntity.removeComponent(movableComponent)
                        }
                }
            }
        }

        // Resizeable switch
        findViewById<SwitchMaterial>(R.id.switch_resizeable_in_fsm).also {
            val resizableComponent =
                ResizableComponent.create(session!!).also { component ->
                    component.addResizeListener(
                        Executors.newSingleThreadExecutor(),
                        object : ResizeListener {
                            override fun onResizeEnd(entity: Entity, finalSize: FloatSize3d) {
                                Log.i(TAG, "resize event $finalSize")
                                (entity as PanelEntity).size = finalSize.to2d()
                                findViewById<TextView>(R.id.text_main_panel_dimensions_value).text =
                                    mainPanelPixelDimensionsString()
                            }
                        },
                    )
                }
            it.setOnCheckedChangeListener { _, isOn ->
                resizableComponent.size = session!!.scene.mainPanelEntity.size.to3d()
                when (isOn) {
                    true ->
                        resizableActive =
                            session!!.scene.mainPanelEntity.addComponent(resizableComponent)
                    false ->
                        resizableActive.let {
                            session!!.scene.mainPanelEntity.removeComponent(resizableComponent)
                        }
                }
            }
        }

        // Resize to portrait in fsm
        findViewById<Button>(R.id.button_resize_in_fsm_portrait).also {
            it.setOnClickListener {
                session!!.scene.mainPanelEntity.sizeInPixels = IntSize2d(1200, 1600)
            }
        }

        // Resize to landscape in fsm
        findViewById<Button>(R.id.button_resize_in_fsm_landscape).also {
            it.setOnClickListener {
                session!!.scene.mainPanelEntity.sizeInPixels = IntSize2d(1600, 1200)
            }
        }

        // Load skybox
        findViewById<Button>(R.id.button_load_skybox).also {
            it.setOnClickListener {
                session!!.scene.spatialEnvironment.preferredSpatialEnvironment =
                    SpatialEnvironment.SpatialEnvironmentPreference(
                        skybox,
                        spatialEnvironmentPreference?.geometry,
                    )

                skyboxActive = true
            }
        }

        // Remove skybox
        findViewById<Button>(R.id.button_remove_skybox).also {
            it.setOnClickListener {
                session!!.scene.spatialEnvironment.preferredSpatialEnvironment = null
                skyboxActive = false
            }
        }

        // No aspect ratio preferences initially
        session!!.scene.setPreferredAspectRatio(this, 0.0f)

        // Make components visible per mode
        findViewById<RadioButton>(R.id.choice_any_aspect_ratio_in_hsm).isChecked = true
        findViewById<RadioGroup>(R.id.radio_group_hsm_aspect_ratio).also {
            it.setOnCheckedChangeListener { _, checkedId ->
                val ratio =
                    when (checkedId) {
                        R.id.choice_portrait_in_hsm -> 0.7f
                        R.id.choice_landscape_in_hsm -> 1.4f
                        else -> -12.345f // A negative ratio means "no preferences."
                    }
                // Note: If currently in FSM, the ratio will be applied
                // when the mode switches back to HSM.
                session!!.scene.setPreferredAspectRatio(this, ratio)
            }
        }

        // Launch settings app
        findViewById<Button>(R.id.button_launch_settings_app).also {
            it.setOnClickListener {
                var (intent, bundle) = createIntent()
                bundle = session!!.scene.configureBundleForFullSpaceModeLaunch(bundle)
                startActivity(intent, bundle)
            }
        }

        // Launch settings app with environment inherited
        findViewById<Button>(R.id.button_launch_settings_app_with_env_inherited).also {
            it.setOnClickListener {
                var (intent, bundle) = createIntent()
                bundle =
                    session!!
                        .scene
                        .configureBundleForFullSpaceModeLaunchWithEnvironmentInherited(bundle)
                startActivity(intent, bundle)
            }
        }

        // Add bounds check listener for activity space bounds
        session!!.scene.activitySpace.addOnBoundsChangedListener { dimensions ->
            val dimsString =
                "{w:${dimensions.width}, h:${dimensions.height}, d:${dimensions.depth}}"
            // Set activity space dimensions
            findViewById<TextView>(R.id.text_activity_space_dimensions_value).text = dimsString
            // Set main panel dimensions
            findViewById<TextView>(R.id.text_main_panel_dimensions_value).text =
                mainPanelPixelDimensionsString()

            // Set FSM flag
            inFsm = dimensions.width == Float.POSITIVE_INFINITY

            // Set visibility of components per mode
            componentVisibility()
        }

        lifecycleScope.launch {
            skybox = ExrImage.createFromZip(session!!, Paths.get("skyboxes", "BlueSkybox.zip"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun componentVisibility() {
        val card1 = findViewById<CardView>(R.id.card1)
        val card2 = findViewById<CardView>(R.id.card2)
        if (inFsm) {
            card1.visibility = View.VISIBLE
            card2.visibility = View.GONE
        } else {
            card1.visibility = View.GONE
            card2.visibility = View.VISIBLE
        }
    }

    private fun createIntent(): Pair<Intent, Bundle> {
        val intent = Intent()
        intent.setComponent(ComponentName("com.android.settings", "com.android.settings.Settings"))
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return Pair(intent, ActivityOptions.makeBasic().toBundle())
    }

    private fun updateLaunchInFSMWithEnvVisibility(buttonLaunchInFSMWithEnv: Button) {
        // Unless you're in FSM and skybox is loaded, the "launch in FSM with env" button behaves
        // the
        // same as "launch in FSM" button.
        buttonLaunchInFSMWithEnv.visibility = if (inFsm && skyboxActive) View.VISIBLE else View.GONE
    }

    companion object {
        private val TAG = FsmHsmTransitionActivity::class.java.simpleName
    }
}
