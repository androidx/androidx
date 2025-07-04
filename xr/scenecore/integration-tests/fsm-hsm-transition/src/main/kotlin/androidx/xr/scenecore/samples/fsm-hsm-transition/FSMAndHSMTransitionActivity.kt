/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.samples.fsm_hsm_transition

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
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
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.function.Consumer
import kotlinx.coroutines.launch

class FSMAndHSMTransitionActivity : AppCompatActivity() {

    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }
    private var inFsm: Boolean = false
    private var resizableActive: Boolean = false
    private var movableActive: Boolean = false
    private var skyboxActive: Boolean = false
    private var boundsListener = Consumer<FloatSize3d> {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val textMainPanelPixelDimensions = findViewById<TextView>(R.id.mainPanelPixelDimensions)
        fun mainPanelPixelDimensionsString() =
            "{w:${session.scene.mainPanelEntity.sizeInPixels.width}, h:${session.scene.mainPanelEntity.sizeInPixels.height}}"
        textMainPanelPixelDimensions.text = mainPanelPixelDimensionsString()

        val buttonRequestFSM: Button = findViewById(R.id.buttonFsm)
        buttonRequestFSM.setOnClickListener {
            session.scene.requestFullSpaceMode()
            Log.i(TAG, "Requesting Full Space Mode.")
        }

        val buttonRequestHSM: Button = findViewById(R.id.buttonHsm)
        buttonRequestHSM.setOnClickListener {
            session.scene.requestHomeSpaceMode()
            Log.i(TAG, "Requesting Home Space Mode.")
        }

        val buttonLaunchInFSM: Button = findViewById(R.id.buttonLaunchInFsm)
        buttonLaunchInFSM.setOnClickListener {
            var (intent, bundle) = createIntent()
            bundle = session.scene.setFullSpaceMode(bundle)
            startActivity(intent, bundle)
            Log.i(TAG, "Launching Settings app in a new task in FSM.")
        }

        val buttonLaunchInFSMWithEnv: Button =
            findViewById(R.id.buttonLaunchInFsmWithEnvironmentInherited)
        buttonLaunchInFSMWithEnv.setOnClickListener {
            var (intent, bundle) = createIntent()
            bundle = session.scene.setFullSpaceModeWithEnvironmentInherited(bundle)
            startActivity(intent, bundle)
            Log.i(TAG, "Launching Settings app in a new task in FSM with environment inherited.")
        }

        val buttonLoadSkybox: Button = findViewById(R.id.buttonLoadSkybox)

        lifecycleScope.launch {
            val skybox = ExrImage.createFromZip(session, Paths.get("skyboxes", "BlueSkybox.zip"))
            buttonLoadSkybox.setOnClickListener {
                session.scene.spatialEnvironment.preferredSpatialEnvironment =
                    SpatialEnvironment.SpatialEnvironmentPreference(skybox, null)
                skyboxActive = true
                updateLaunchInFSMWithEnvVisibility(buttonLaunchInFSMWithEnv)
                Log.i(TAG, "Loading skybox.")
            }
        }

        val buttonRemoveSkybox: Button = findViewById(R.id.buttonRemoveSkybox)
        buttonRemoveSkybox.setOnClickListener {
            // set env preference to null to revert to the default skybox.
            session.scene.spatialEnvironment.preferredSpatialEnvironment = null
            skyboxActive = false
            updateLaunchInFSMWithEnvVisibility(buttonLaunchInFSMWithEnv)
            Log.i(TAG, "Removing skybox.")
        }

        val skyboxButtons: LinearLayout = findViewById(R.id.skyboxButtons)

        // For aspect ratio settings in HSM.
        val radioGroup: RadioGroup = findViewById(R.id.radioGroup1)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val ratio =
                when (checkedId) {
                    R.id.radioButton2 -> 0.7f // portait
                    R.id.radioButton3 -> 1.4f // landscape
                    // A negative ratio means "no preferences."
                    else -> -12.345f
                }
            // Note: If currently in FSM, the ratio will be applied when the mode switches back to
            // HSM.
            session.scene.setPreferredAspectRatio(this, ratio)
        }
        session.scene.setPreferredAspectRatio(this, 0.0f) // no preferences initially

        val resizePortraitFsm: Button = findViewById(R.id.resizePortraitFsm)
        resizePortraitFsm.setOnClickListener {
            session.scene.mainPanelEntity.sizeInPixels = IntSize2d(1200, 1600)
        }
        val resizeLandscapeFsm: Button = findViewById(R.id.resizeLandscapeFsm)
        resizeLandscapeFsm.setOnClickListener {
            session.scene.mainPanelEntity.sizeInPixels = IntSize2d(1600, 1200)
        }

        val textRadio: TextView = findViewById(R.id.radioLabel)
        val textDimensions: TextView = findViewById(R.id.textDimensions)
        boundsListener =
            Consumer<FloatSize3d> { dimensions ->
                val dimensionsString =
                    "{w:${dimensions.width}, h:${dimensions.height}, d:${dimensions.depth}}"
                textDimensions.text = dimensionsString
                textMainPanelPixelDimensions.text = mainPanelPixelDimensionsString()
                inFsm = dimensions.width == Float.POSITIVE_INFINITY

                updateLaunchInFSMWithEnvVisibility(buttonLaunchInFSMWithEnv)

                val visibility = if (inFsm) View.VISIBLE else View.GONE
                skyboxButtons.visibility = visibility
                textRadio.visibility = visibility
                resizePortraitFsm.visibility = visibility
                resizeLandscapeFsm.visibility = visibility
                Log.i(
                    TAG,
                    "OnBoundsChanged called on Activity Space with dimensions: $dimensionsString",
                )
            }
        session.scene.activitySpace.addOnBoundsChangedListener(boundsListener)

        val resizableComponent = ResizableComponent.create(session)
        resizableComponent.addResizeListener(
            Executors.newSingleThreadExecutor(),
            object : ResizeListener {
                override fun onResizeEnd(entity: Entity, finalSize: FloatSize3d) {
                    Log.i(TAG, "resize event $finalSize")
                    (entity as PanelEntity).size = finalSize.to2d()
                    textMainPanelPixelDimensions.text = mainPanelPixelDimensionsString()
                }
            },
        )
        val movableComponent = MovableComponent.createSystemMovable(session)

        val switchMovable: Switch = findViewById(R.id.movableSwitch)
        switchMovable.setOnCheckedChangeListener { _, isChecked ->
            movableComponent.size = session.scene.mainPanelEntity.size.to3d()
            when (isChecked) {
                true -> movableActive = session.scene.mainPanelEntity.addComponent(movableComponent)
                false ->
                    movableActive.let {
                        session.scene.mainPanelEntity.removeComponent(movableComponent)
                    }
            }
        }

        val switchResizable: Switch = findViewById(R.id.resizableSwitch)
        switchResizable.setOnCheckedChangeListener { _, isChecked ->
            resizableComponent.size = session.scene.mainPanelEntity.size.to3d()
            when (isChecked) {
                true ->
                    resizableActive = session.scene.mainPanelEntity.addComponent(resizableComponent)
                false ->
                    resizableActive.let {
                        session.scene.mainPanelEntity.removeComponent(resizableComponent)
                    }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        session.scene.activitySpace.removeOnBoundsChangedListener(boundsListener)
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
        private val TAG = FSMAndHSMTransitionActivity::class.java.simpleName
    }
}
