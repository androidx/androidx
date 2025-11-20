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
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.ExrImage
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.ResizableComponent
import androidx.xr.scenecore.ResizeEvent
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.SpatialWindow
import androidx.xr.scenecore.createBundleForFullSpaceModeLaunch
import androidx.xr.scenecore.createBundleForFullSpaceModeLaunchWithEnvironmentInherited
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.createSession
import androidx.xr.scenecore.testapp.common.format
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.function.Consumer
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
    private lateinit var defaultPanelSize: FloatSize2d

    private fun mainPanelPixelDimensionsString(): String {
        val width = session!!.scene.mainPanelEntity.size.width.format(2)
        val height = session!!.scene.mainPanelEntity.size.height.format(2)
        return "{w:$width, h:$height}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fsm_hsm_transition)

        session = createSession(this)
        if (session == null) {
            this.finish()
        } else {
            if (savedInstanceState != null) {
                val width = savedInstanceState.getFloat("defaultPanelSizeWidth")
                val height = savedInstanceState.getFloat("defaultPanelSizeHeight")
                defaultPanelSize = FloatSize2d(width, height)
            } else {
                defaultPanelSize = session!!.scene.mainPanelEntity.size
            }
            Log.d(
                TAG,
                "defaultPanelSize: " +
                    "w ${defaultPanelSize.width.format(2)} x " +
                    "h ${defaultPanelSize.height.format(2)}",
            )
        }

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
            it.setNavigationOnClickListener {
                val resultIntent = Intent()
                resultIntent.putExtra("defaultPanelSizeWidth", defaultPanelSize.width)
                resultIntent.putExtra("defaultPanelSizeHeight", defaultPanelSize.height)
                setResult(RESULT_OK, resultIntent)

                this.finish()
            }
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
                ResizableComponent.create(
                    session!!,
                    executor = Executors.newSingleThreadExecutor(),
                    resizeEventListener =
                        Consumer<ResizeEvent> { resizeEvent: ResizeEvent ->
                            if (resizeEvent.resizeState == ResizeEvent.ResizeState.END) {
                                Log.i(TAG, "resize event ${resizeEvent.newSize}")
                                (resizeEvent.entity as PanelEntity).size =
                                    resizeEvent.newSize.to2d()
                                findViewById<TextView>(R.id.text_main_panel_dimensions_value).text =
                                    mainPanelPixelDimensionsString()
                            }
                        },
                )
            it.setOnCheckedChangeListener { _, isOn ->
                resizableComponent.affordanceSize = session!!.scene.mainPanelEntity.size.to3d()
                when (isOn) {
                    true ->
                        resizableActive =
                            session!!.scene.mainPanelEntity.addComponent(resizableComponent)
                    false -> {
                        if (resizableActive) {
                            session!!.scene.mainPanelEntity.removeComponent(resizableComponent)
                            resizableActive = false
                        }
                    }
                }
            }
        }

        // Resize to portrait in fsm
        findViewById<Button>(R.id.button_resize_in_fsm_portrait).also {
            it.setOnClickListener {
                if (resizableActive) {
                    session!!.scene.mainPanelEntity.sizeInPixels = IntSize2d(1200, 1600)
                }
            }
        }

        // Resize to landscape in fsm
        findViewById<Button>(R.id.button_resize_in_fsm_landscape).also {
            it.setOnClickListener {
                if (resizableActive) {
                    session!!.scene.mainPanelEntity.sizeInPixels = IntSize2d(1600, 1200)
                }
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
        SpatialWindow.setPreferredAspectRatio(
            session!!,
            this,
            SpatialWindow.NO_PREFERRED_ASPECT_RATIO,
        )

        // Make components visible per mode
        findViewById<RadioButton>(R.id.choice_any_aspect_ratio_in_hsm).isChecked = true
        findViewById<RadioGroup>(R.id.radio_group_hsm_aspect_ratio).also {
            it.setOnCheckedChangeListener { _, checkedId ->
                val ratio =
                    when (checkedId) {
                        R.id.choice_portrait_in_hsm -> 0.7f
                        R.id.choice_landscape_in_hsm -> 1.4f
                        else -> SpatialWindow.NO_PREFERRED_ASPECT_RATIO
                    }
                // Note: If currently in FSM, the ratio will be applied
                // when the mode switches back to HSM.
                SpatialWindow.setPreferredAspectRatio(session!!, this, ratio)
            }
        }

        // Launch settings app
        findViewById<Button>(R.id.button_launch_settings_app).also {
            it.setOnClickListener {
                var (intent, bundle) = createIntent()
                bundle = createBundleForFullSpaceModeLaunch(session!!, bundle)
                startActivity(intent, bundle)
            }
        }

        // Launch settings app with environment inherited
        findViewById<Button>(R.id.button_launch_settings_app_with_env_inherited).also {
            it.setOnClickListener {
                var (intent, bundle) = createIntent()
                bundle =
                    createBundleForFullSpaceModeLaunchWithEnvironmentInherited(session!!, bundle)
                startActivity(intent, bundle)
            }
        }

        // Add bounds check listener for activity space bounds
        session!!.scene.activitySpace.addOnBoundsChangedListener { dimensions ->
            val dimsString =
                "{w:${dimensions.width.format(2)}, " +
                    "h:${dimensions.height.format(2)}, " +
                    "d:${dimensions.depth.format(2)}"
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putFloat("defaultPanelSizeWidth", defaultPanelSize.width)
        outState.putFloat("defaultPanelSizeHeight", defaultPanelSize.height)
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
