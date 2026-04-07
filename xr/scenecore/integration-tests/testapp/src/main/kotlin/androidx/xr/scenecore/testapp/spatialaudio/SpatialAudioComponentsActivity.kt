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

package androidx.xr.scenecore.testapp.spatialaudio

import android.annotation.SuppressLint
import android.content.res.AssetFileDescriptor
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.PositionalAudioComponent
import androidx.xr.scenecore.SoundEffectPool
import androidx.xr.scenecore.SoundEffectPoolComponent
import androidx.xr.scenecore.SoundFieldAttributes
import androidx.xr.scenecore.SoundFieldAudioComponent
import androidx.xr.scenecore.SpatializerConstants.AmbisonicsOrder
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class SpatialAudioComponentsActivity : AppCompatActivity() {

    private enum class AttachmentState {
        NONE,
        SOUND_PANEL,
        MAIN_PANEL,
    }

    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }

    private lateinit var exoPlayerPoint: ExoPlayer
    private lateinit var exoPlayerFirstOrder: ExoPlayer
    private lateinit var exoPlayerThirdOrder: ExoPlayer

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spatial_audio_components)
        session.scene.keyEntity = session.scene.mainPanelEntity

        // Toolbar action
        val toolbar: Toolbar = findViewById(R.id.toolbar_spatial_audio_test)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { this.finish() }
        if (intent.extras != null) {
            val toolbarTitle = intent.extras!!.getString("MAIN_PANEL_TITLE", "")
            if (toolbarTitle != "") toolbar.setTitle(toolbarTitle)
        }

        // Recreate button
        findViewById<FloatingActionButton>(R.id.bottomCenterFab).also {
            it.tooltipText = getString(R.string.fab_recreate_activity_tooltip)
            it.setOnClickListener { ActivityCompat.recreate(this@SpatialAudioComponentsActivity) }
        }
        // Sound panel
        val soundPanelView = layoutInflater.inflate(R.layout.sound_panel, null)
        val soundEntity =
            PanelEntity.create(
                session,
                soundPanelView,
                IntSize2d(640, 480),
                "sound panel",
                Pose(Vector3(1F, 0F, 0.5F)),
            )

        val movableComponent = MovableComponent.createSystemMovable(session, scaleInZ = false)
        soundEntity.addComponent(movableComponent)

        val pointSourceParams = PointSourceParams()
        val firstOrderAttributes = SoundFieldAttributes(AmbisonicsOrder.FIRST_ORDER)
        val thirdOrderAttributes = SoundFieldAttributes(AmbisonicsOrder.THIRD_ORDER)

        // Components
        val positionalAudioComponent = PositionalAudioComponent.create(session, pointSourceParams)

        val firstOrderComponent = SoundFieldAudioComponent.create(session, firstOrderAttributes)
        soundEntity.addComponent(firstOrderComponent)

        val thirdOrderComponent = SoundFieldAudioComponent.create(session, thirdOrderAttributes)
        soundEntity.addComponent(thirdOrderComponent)

        // ExoPlayers
        exoPlayerPoint =
            ExoPlayer.Builder(this)
                .setAudioOutputProvider(positionalAudioComponent.audioOutputProvider)
                .build()

        exoPlayerFirstOrder =
            ExoPlayer.Builder(this)
                .setAudioOutputProvider(firstOrderComponent.audioOutputProvider)
                .build()

        exoPlayerThirdOrder =
            ExoPlayer.Builder(this)
                .setAudioOutputProvider(thirdOrderComponent.audioOutputProvider)
                .build()

        // File Paths
        val tigerPath = Environment.getExternalStorageDirectory().path + "/Download/tiger_16db.mp3"
        val tigerFile = File(tigerPath)
        if (!tigerFile.exists()) {
            Toast.makeText(
                    this,
                    "Audio files not found. Did you download all the assets?",
                    Toast.LENGTH_LONG,
                )
                .show()
            return
        }

        val basketballPath =
            Environment.getExternalStorageDirectory().path + "/Download/foa_basketball_16bit.wav"
        val opusPath =
            Environment.getExternalStorageDirectory().path + "/Download/dunes_test_opus.ogg"

        // SoundEffectPool
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val afd =
            AssetFileDescriptor(
                ParcelFileDescriptor.open(tigerFile, ParcelFileDescriptor.MODE_READ_ONLY),
                0L,
                tigerFile.length(),
            )

        soundEffectPool.setOnLoadCompleteListener { effect, bool ->
            Log.i(TAG, "Loaded $effect and $bool")
        }
        val soundEffect = soundEffectPool.load(afd)
        val corruptSoundEffect = soundEffectPool.load(this, R.raw.corrupt_sound)

        val soundPoolComponent =
            SoundEffectPoolComponent.create(session, soundEffectPool, pointSourceParams)
        soundEntity.addComponent(soundPoolComponent)

        // --- SoundEffectPoolComponent Card ---
        val soundEffectPoolPlayButton = findViewById<Button>(R.id.button_sound_effect_pool_play)
        soundEffectPoolPlayButton.setOnClickListener {
            soundPoolComponent.play(soundEffect, 1.0f, 1, false)
        }

        val soundEffectPoolPlayCorruptButton =
            findViewById<Button>(R.id.button_sound_effect_pool_play_corrupt)
        soundEffectPoolPlayCorruptButton.setOnClickListener {
            try {
                soundPoolComponent.play(corruptSoundEffect, 1.0f, 1, false)
                Toast.makeText(this, "Unexpected: No exception thrown", Toast.LENGTH_SHORT).show()
            } catch (e: RuntimeException) {
                Toast.makeText(this, "Confirmed: RuntimeException thrown", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // --- PositionalAudioComponent Card ---
        val positionalAudioPlayTigerButton =
            findViewById<Button>(R.id.button_positional_audio_play_tiger)
        positionalAudioPlayTigerButton.setOnClickListener {
            exoPlayerPoint.setMediaItem(MediaItem.fromUri(tigerPath))
            exoPlayerPoint.prepare()
            exoPlayerPoint.play()
        }

        var nextAttachment = AttachmentState.SOUND_PANEL
        val toggleButton = findViewById<Button>(R.id.button_positional_audio_toggle_attachment)
        val statusText = findViewById<android.widget.TextView>(R.id.text_positional_audio_status)
        toggleButton.setOnClickListener {
            when (nextAttachment) {
                AttachmentState.SOUND_PANEL -> {
                    soundEntity.addComponent(positionalAudioComponent)
                    toggleButton.text = "Attach to main panel"
                    statusText.text = "Status: Attached to sound panel"
                    nextAttachment = AttachmentState.MAIN_PANEL
                }

                AttachmentState.MAIN_PANEL -> {
                    soundEntity.removeComponent(positionalAudioComponent)
                    session.scene.mainPanelEntity.addComponent(positionalAudioComponent)
                    toggleButton.text = "Detach"
                    statusText.text = "Status: Attached to main panel"
                    nextAttachment = AttachmentState.NONE
                }

                AttachmentState.NONE -> {
                    session.scene.mainPanelEntity.removeComponent(positionalAudioComponent)
                    toggleButton.text = "Attach to sound panel"
                    statusText.text = "Status: Detached"
                    nextAttachment = AttachmentState.SOUND_PANEL
                }
            }
        }

        // --- SoundFieldAudioComponent (First Order) Card ---
        val soundFieldPlayWavButton = findViewById<Button>(R.id.button_sound_field_play_wav)
        soundFieldPlayWavButton.setOnClickListener {
            exoPlayerFirstOrder.setMediaItem(MediaItem.fromUri(basketballPath))
            exoPlayerFirstOrder.prepare()
            exoPlayerFirstOrder.play()
        }

        // --- SoundFieldAudioComponent (Third Order) Card ---
        val soundFieldPlayOpusButton = findViewById<Button>(R.id.button_sound_field_play_opus)
        soundFieldPlayOpusButton.setOnClickListener {
            exoPlayerThirdOrder.setMediaItem(MediaItem.fromUri(opusPath))
            exoPlayerThirdOrder.prepare()
            exoPlayerThirdOrder.play()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::exoPlayerPoint.isInitialized) {
            exoPlayerPoint.release()
        }
        if (::exoPlayerFirstOrder.isInitialized) {
            exoPlayerFirstOrder.release()
        }
        if (::exoPlayerThirdOrder.isInitialized) {
            exoPlayerThirdOrder.release()
        }
    }

    private companion object {
        private const val TAG = "SpatialAudioComponentsActivity"
    }
}
