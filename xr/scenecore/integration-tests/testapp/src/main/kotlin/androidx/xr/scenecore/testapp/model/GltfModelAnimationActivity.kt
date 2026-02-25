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

package androidx.xr.scenecore.testapp.model

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfAnimation
import androidx.xr.scenecore.GltfAnimationStartOptions
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider
import java.nio.file.Paths
import java.util.Collections
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.launch

const val TAG = "GltfModelAnimationActivity"

@SuppressLint("SetTextI18n", "RestrictedApi")
class GltfModelAnimationActivity : AppCompatActivity() {

    // Button for glTF loading, entity creation, and entity destruction
    private lateinit var createGltfModelButton: Button
    private lateinit var createGltfEntityButton: Button

    // Start, Stop, Pause, Resume Button
    private lateinit var startPlayGltfButton: Button
    private lateinit var stopPlayGltfButton: Button
    private lateinit var pausePlayGltfButton: Button
    private lateinit var resumePlayGltfButton: Button

    // UI and variable related to 'Loop' animation setup
    private lateinit var loopToggleButton: ToggleButton

    // UI related to the animation speed controlling
    private lateinit var speedSlider: Slider
    private lateinit var speedText: TextView

    // UI related to the animation seek to time (in seconds) controlling
    private lateinit var seekCurrentTimeText: TextView
    private lateinit var seekPlaySlider: Slider
    private lateinit var seekEndText: TextView

    // Text UI and map about animation state
    private lateinit var animationStateText: TextView
    private val animationStateMap: MutableMap<Int, GltfAnimation.AnimationState> =
        Collections.synchronizedMap(mutableMapOf())

    private lateinit var animationList: AutoCompleteTextView
    private lateinit var animations: List<GltfAnimation>
    private var selectedIndexAtAnimationList = -1
    private val modelInitTranslation = Vector3(3.0f, 0.0f, -2.0f)
    private val modelInitQuaternion = Quaternion.Identity
    private val modelInitPose = Pose(modelInitTranslation, modelInitQuaternion)
    private val modelScale = 0.3f

    private var gltfModel: GltfModel? = null
    private var gltfModelEntity: GltfModelEntity? = null

    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_gltf_model_animation)

        session.scene.keyEntity = session.scene.mainPanelEntity

        findViewById<Toolbar>(R.id.gltf_model_animation_topAppBar).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { this@GltfModelAnimationActivity.finish() }
            it.setTitle(getString(R.string.cuj_gltf_model_animation_test))
        }

        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener {
                resetUi()
                ActivityCompat.recreate(this@GltfModelAnimationActivity)
            }
        }

        createGltfModelButton = findViewById(R.id.gltf_model_create)
        createGltfEntityButton = findViewById(R.id.gltf_entity_create)

        startPlayGltfButton = findViewById(R.id.start_play)
        stopPlayGltfButton = findViewById(R.id.stop_play)
        pausePlayGltfButton = findViewById(R.id.pause_play)
        resumePlayGltfButton = findViewById(R.id.resume_play)

        loopToggleButton = findViewById(R.id.loop_toggle_button)
        loopToggleButton.isChecked = false

        speedText = findViewById(R.id.speed_textview)
        speedSlider = findViewById(R.id.speed_slider)

        seekCurrentTimeText = findViewById(R.id.seek_current_time_text)
        seekPlaySlider = findViewById(R.id.seek_time_second_slider)
        seekEndText = findViewById(R.id.seek_end_text)

        animationStateText = findViewById(R.id.animation_current_state_text)
        animationList = findViewById(R.id.autoCompleteTextView)

        createGltfModelButton.setOnClickListener { lifecycleScope.launch { createGltfModel() } }

        createGltfEntityButton.setOnClickListener { createGltfEntity() }

        startPlayGltfButton.setOnClickListener {
            if (selectedIndexAtAnimationList < 0 || animations.isEmpty()) {
                return@setOnClickListener
            }

            val animationOptions =
                GltfAnimationStartOptions(
                    shouldLoop = loopToggleButton.isChecked,
                    speed = speedSlider.value,
                    seekStartTime = seekPlaySlider.value.toDouble().seconds.toJavaDuration(),
                )

            val animationOptions2 = animationOptions.copy()
            Log.d(TAG, "animationOptions2 is ${animationOptions2.toString()}")

            val animationOptions3 = animationOptions.copy(true)
            Log.d(TAG, "animationOptions3 is ${animationOptions3.toString()}")

            val animationOptions4 = animationOptions.copy(true, 2f)
            Log.d(TAG, "animationOptions4 is ${animationOptions4.toString()}")

            val animationOptions5 = animationOptions.copy(true, 2f, 1.seconds.toJavaDuration())
            Log.d(TAG, "animationOptions5 is ${animationOptions5.toString()}")

            val animationOptions6 =
                animationOptions.copy(true, seekStartTime = (.5).seconds.toJavaDuration())
            Log.d(TAG, "animationOptions6 is ${animationOptions6.toString()}")

            animations[selectedIndexAtAnimationList].start(animationOptions)
        }

        stopPlayGltfButton.setOnClickListener {
            if (selectedIndexAtAnimationList < 0 || animations.isEmpty()) {
                return@setOnClickListener
            }

            animations[selectedIndexAtAnimationList].stop()
        }

        pausePlayGltfButton.setOnClickListener {
            if (selectedIndexAtAnimationList < 0 || animations.isEmpty()) {
                return@setOnClickListener
            }

            animations[selectedIndexAtAnimationList].pause()
        }

        resumePlayGltfButton.setOnClickListener {
            if (selectedIndexAtAnimationList < 0 || animations.isEmpty()) {
                return@setOnClickListener
            }

            animations[selectedIndexAtAnimationList].resume()
        }

        seekPlaySlider.addOnChangeListener { _, value, fromUser ->
            seekCurrentTimeText.text = "Start time=$value"

            if (fromUser) {
                if (selectedIndexAtAnimationList < 0 || animations.isEmpty()) {
                    return@addOnChangeListener
                }

                if (
                    animations[selectedIndexAtAnimationList].animationState ==
                        GltfAnimation.AnimationState.STOPPED
                ) {
                    return@addOnChangeListener
                }

                animations[selectedIndexAtAnimationList].seekTo(
                    value.toDouble().seconds.toJavaDuration()
                )
            }
        }

        speedSlider.addOnChangeListener { _, value, _ ->
            speedText.text = "Speed=$value"

            if (selectedIndexAtAnimationList < 0 || animations.isEmpty()) {
                return@addOnChangeListener
            }

            if (
                animations[selectedIndexAtAnimationList].animationState ==
                    GltfAnimation.AnimationState.STOPPED
            ) {
                return@addOnChangeListener
            }

            animations[selectedIndexAtAnimationList].setSpeed(value)
        }

        setAllUiEnabled(false)
        createGltfModelButton.isEnabled = true
    }

    suspend fun createGltfModel() {
        gltfModel = GltfModel.create(session, Paths.get("models", "RobotExpressive.glb"))

        createGltfModelButton.isEnabled = false
        createGltfEntityButton.isEnabled = true
    }

    fun createGltfEntity() {

        fun initGltfEntity(@NonNull gltfModel: GltfModel) {
            gltfModelEntity = GltfModelEntity.create(session, gltfModel, modelInitPose)
            gltfModelEntity?.setScale(modelScale)
        }

        if (gltfModel != null) {
            initGltfEntity(gltfModel!!)
        } else {
            lifecycleScope.launch {
                createGltfModel()
                gltfModel?.let { initGltfEntity(it) }
            }
        }

        setAllUiEnabled(true)
        createGltfModelButton.isEnabled = false
        createGltfEntityButton.isEnabled = false

        if (gltfModelEntity != null) {

            animations = gltfModelEntity!!.animations
            Log.w(TAG, "Animation total count is ${animations.size - 1}")

            // setup spinner item to show options in spinner
            val options = ArrayList<String>()

            for (i in 0..<animations.size) {

                val name = animations[i].name ?: ""
                val state = animations[i].animationState

                // Add item in animationStateMap
                animationStateMap[i] = state

                // Add item in options
                options.add(name)

                printAnimationInfo(animations[i])

                setupCallback(animations[i])
            }

            val adapter = ArrayAdapter<String?>(this, android.R.layout.simple_spinner_item, options)
            animationList.setAdapter(adapter)
            animationList.setOnItemClickListener { parent, view, position, id ->
                selectedIndexAtAnimationList = position

                animationStateText.text = animationStateMap[position].toString()

                loopToggleButton.isChecked = false

                seekPlaySlider.value = 0f
                seekPlaySlider.valueFrom = 0f
                seekPlaySlider.valueTo = animations[position].duration.toMillis() / 1000f
                seekPlaySlider.stepSize = (seekPlaySlider.valueTo - seekPlaySlider.valueFrom) / 20f

                seekEndText.text = String.format("%.1fs", seekPlaySlider.valueTo)

                speedSlider.value = 1f
            }

            animationStateText.text = "STOPPED"
        }
    }

    fun printAnimationInfo(animation: GltfAnimation) {
        Log.w(TAG, "Animation index is ${animation.index}")
        Log.w(TAG, "Animation name is ${animation.name}")
        Log.w(TAG, "Animation duration is ${animation.duration.toMillis() / 1000f} seconds")
    }

    fun setupCallback(animation: GltfAnimation) {
        animation.addAnimationStateListener { state ->
            when (state) {
                GltfAnimation.AnimationState.PLAYING -> {
                    Log.d(TAG, "${animation.name} animation is now playing!!")
                }

                GltfAnimation.AnimationState.STOPPED -> {
                    Log.d(TAG, "${animation.name} animation is now stopped!!")
                }

                GltfAnimation.AnimationState.PAUSED -> {
                    Log.d(TAG, "${animation.name} animation is now paused!!")
                }
            }

            animationStateMap[animation.index] = state

            if (animation.index == selectedIndexAtAnimationList) {
                animationStateText.text = state.toString()
            }
        }
    }

    fun resetUi() {
        setAllUiEnabled(false)
        createGltfModelButton.isEnabled = true

        // Clean up
        animations = emptyList()
        animationStateMap.clear()
        animationList.setText("Choose glTF Animations")
        animationStateText.text = "STOPPED"

        selectedIndexAtAnimationList = -1
    }

    fun setAllUiEnabled(isEnabled: Boolean) {
        createGltfModelButton.isEnabled = isEnabled
        createGltfEntityButton.isEnabled = isEnabled

        startPlayGltfButton.isEnabled = isEnabled
        stopPlayGltfButton.isEnabled = isEnabled
        pausePlayGltfButton.isEnabled = isEnabled
        resumePlayGltfButton.isEnabled = isEnabled
        loopToggleButton.isEnabled = isEnabled

        speedSlider.isEnabled = isEnabled

        seekCurrentTimeText.isEnabled = isEnabled
        seekPlaySlider.isEnabled = isEnabled
    }
}
