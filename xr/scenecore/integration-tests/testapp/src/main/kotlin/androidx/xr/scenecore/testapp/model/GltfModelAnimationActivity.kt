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
import android.widget.Button
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
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.nio.file.Paths
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n", "RestrictedApi")
class GltfModelAnimationActivity : AppCompatActivity() {

    enum class AnimationState(val value: String) {
        PLAYING("PLAYING"),
        STOPPED("STOPPED"),
        PAUSED("PAUSED");

        companion object {
            private val map = entries.associateBy(AnimationState::value)

            fun fromInt(type: String): AnimationState? {
                return map[type]
            }
        }
    }

    private lateinit var createGltfModelButton: Button
    private lateinit var createGltfEntityButton: Button
    private lateinit var destroyGltfModelButton: Button
    private lateinit var startPlayGltfButton: Button
    private lateinit var startPlayGltfButton2: Button
    private lateinit var pausePlayGltfButton: Button
    private lateinit var resumePlayGltfButton: Button
    private lateinit var stopPlayGltfButton: Button
    private lateinit var loopToggleButton: ToggleButton

    private var gltfModel: GltfModel? = null
    private var gltfModelEntity: GltfModelEntity? = null
    private var isGltfModelLoopAnimation: Boolean = false
    private var animationState: AnimationState = AnimationState.STOPPED

    @Suppress("DEPRECATION")
    private val session by lazy {
        (Session.create(this, unscaledGravityAlignedActivitySpace = true) as SessionCreateSuccess)
            .session
    }

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
            it.setOnClickListener { ActivityCompat.recreate(this@GltfModelAnimationActivity) }
        }

        createGltfModelButton = findViewById(R.id.gltf_model_create)
        createGltfEntityButton = findViewById(R.id.gltf_entity_create)
        destroyGltfModelButton = findViewById(R.id.gltf_entity_destroy)

        startPlayGltfButton = findViewById(R.id.start_play)
        startPlayGltfButton2 = findViewById(R.id.start_play_2)
        pausePlayGltfButton = findViewById(R.id.pause_play)
        resumePlayGltfButton = findViewById(R.id.resume_play)
        stopPlayGltfButton = findViewById(R.id.stop_play)
        loopToggleButton = findViewById(R.id.loop_toggle_button)
        loopToggleButton.isChecked = false

        loopToggleButton.setOnClickListener {
            isGltfModelLoopAnimation = loopToggleButton.isChecked
        }

        createGltfModelButton.setOnClickListener { lifecycleScope.launch { createGltfModel() } }

        createGltfEntityButton.setOnClickListener { createGltfEntity() }

        destroyGltfModelButton.setOnClickListener { destroyGltfEntity() }

        startPlayGltfButton.setOnClickListener { playGltfModel() }

        pausePlayGltfButton.setOnClickListener { pauseGltfModel() }

        resumePlayGltfButton.setOnClickListener { resumeGltfModel() }

        stopPlayGltfButton.setOnClickListener { stopGltfModel() }

        startPlayGltfButton2.setOnClickListener {
            gltfModelEntity?.startAnimation(isGltfModelLoopAnimation, "Linear Scale")
        }

        setAllButtonEnabled(false)
        createGltfModelButton.isEnabled = true
    }

    override fun onDestroy() {
        destroyGltfEntity()
        super.onDestroy()
    }

    fun pauseGltfModel() {
        if (gltfModelEntity?.animationState == GltfModelEntity.AnimationState.PLAYING) {
            gltfModelEntity!!.pauseAnimation()
            setAnimationState(AnimationState.PAUSED)
        }
    }

    fun resumeGltfModel() {
        if (gltfModelEntity?.animationState == GltfModelEntity.AnimationState.PAUSED) {
            gltfModelEntity!!.resumeAnimation()
            setAnimationState(AnimationState.PLAYING)
        }
    }

    fun playGltfModel() {
        gltfModelEntity?.startAnimation(isGltfModelLoopAnimation, "Step Scale")

        if (isGltfModelLoopAnimation) {
            setAnimationState(AnimationState.PLAYING)
        }
    }

    fun stopGltfModel() {
        if (
            gltfModelEntity?.animationState == GltfModelEntity.AnimationState.PLAYING ||
                gltfModelEntity?.animationState == GltfModelEntity.AnimationState.PAUSED
        ) {
            gltfModelEntity!!.stopAnimation()
        }

        setAnimationState(AnimationState.STOPPED)
    }

    fun destroyGltfEntity() {
        if (gltfModelEntity != null) {
            gltfModelEntity!!.dispose()
            gltfModelEntity = null
        }

        setAnimationState(AnimationState.STOPPED)
        setAllButtonEnabled(false)
        createGltfModelButton.isEnabled = true
    }

    fun createGltfEntity() {

        fun initGltfEntity(@NonNull gltfModel: GltfModel) {
            gltfModelEntity =
                GltfModelEntity.create(
                    session,
                    gltfModel,
                    Pose(Vector3(3.0f, 0.0f, -2.0f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
                )
            gltfModelEntity?.setScale(.3f)
        }

        if (gltfModel != null) {
            initGltfEntity(gltfModel!!)
        } else {
            lifecycleScope.launch {
                createGltfModel()
                gltfModel?.let { initGltfEntity(it) }
            }
        }

        setAnimationState(AnimationState.STOPPED)

        setAllButtonEnabled(true)
        createGltfModelButton.isEnabled = false
        createGltfEntityButton.isEnabled = false
    }

    suspend fun createGltfModel() {
        gltfModel = GltfModel.create(session, Paths.get("models", "InterpolationTest.glb"))

        setAnimationState(AnimationState.STOPPED)

        createGltfModelButton.isEnabled = false
        createGltfEntityButton.isEnabled = true
    }

    fun setAnimationState(@NonNull animationState: AnimationState) {
        this.animationState = animationState
    }

    fun setAllButtonEnabled(isEnabled: Boolean) {
        createGltfModelButton.isEnabled = isEnabled
        createGltfEntityButton.isEnabled = isEnabled
        destroyGltfModelButton.isEnabled = isEnabled
        startPlayGltfButton.isEnabled = isEnabled
        startPlayGltfButton2.isEnabled = isEnabled
        pausePlayGltfButton.isEnabled = isEnabled
        resumePlayGltfButton.isEnabled = isEnabled
        stopPlayGltfButton.isEnabled = isEnabled
        loopToggleButton.isEnabled = isEnabled
    }
}
