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
import android.content.res.Resources
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION
import android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
import android.media.AudioFormat
import android.media.AudioFormat.CHANNEL_OUT_MONO
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.SoundFieldAttributes
import androidx.xr.scenecore.SpatialAudioTrack
import androidx.xr.scenecore.SpatialAudioTrackBuilder
import androidx.xr.scenecore.SpatialMediaPlayer
import androidx.xr.scenecore.SpatialSoundPool
import androidx.xr.scenecore.SpatializerConstants
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileInputStream

class SpatialAudioActivity : AppCompatActivity() {

    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }

    private val mediaplayer = MediaPlayer()

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spatialaudio)

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
            it.setOnClickListener { ActivityCompat.recreate(this@SpatialAudioActivity) }
        }
        // Sound panel
        val soundPanelView = layoutInflater.inflate(R.layout.sound_panel, null)
        val soundEntity =
            PanelEntity.create(
                session,
                soundPanelView,
                IntSize2d(640, 480),
                "sound panel",
                Pose(Vector3(0F, 0F, 0.5F)),
            )

        val movableComponent = MovableComponent.createSystemMovable(session, scaleInZ = false)
        val unused = soundEntity.addComponent(movableComponent)

        val pointSourceParams = PointSourceParams(soundEntity)
        val soundFieldAttributes =
            SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER)

        val soundPool =
            SoundPool.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(CONTENT_TYPE_SONIFICATION)
                        .setUsage(USAGE_ASSISTANCE_SONIFICATION)
                        .build()
                )
                .build()

        val tigerPath =
            Environment.getExternalStorageDirectory().getPath() + "/Download/tiger_16db.mp3"
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

        val pointSoundId =
            soundPool.load(
                // For Testers: Note that this translates to "/sdcard/Download/tiger_16db.mp3"
                tigerPath,
                /* priority= */ 1,
            )
        val soundFieldSoundId =
            soundPool.load(
                // For Testers: Note that this translates to
                // "/sdcard/Download/foa_basketball_16bit.wav"
                Environment.getExternalStorageDirectory().getPath() +
                    "/Download/foa_basketball_16bit.wav",
                /* priority= */ 1,
            )

        val soundPoolPointButton = findViewById<Button>(R.id.button_soundpool_play_point_sound)
        soundPoolPointButton.setOnClickListener {
            val unused =
                SpatialSoundPool.play(
                    session,
                    soundPool,
                    pointSoundId,
                    pointSourceParams,
                    DEFAULT_VOLUME,
                    DEFAULT_PRIORITY,
                    DEFAULT_LOOP,
                    DEFAULT_RATE,
                )
        }

        val soundPoolSoundFieldButton = findViewById<Button>(R.id.button_soundpool_play_sound_field)
        soundPoolSoundFieldButton.setOnClickListener {
            val unused =
                SpatialSoundPool.play(
                    session,
                    soundPool,
                    soundFieldSoundId,
                    soundFieldAttributes,
                    DEFAULT_VOLUME,
                    DEFAULT_PRIORITY,
                    DEFAULT_LOOP,
                    DEFAULT_RATE,
                )
        }

        val audioTrackDefaultPlayer =
            AudioTrackPlayer(
                resources,
                // For Testers: Note that this translates to "/sdcard/Download/tiger_16db_raw.wav"
                Environment.getExternalStorageDirectory().getPath() +
                    "/Download/tiger_16db_raw.wav",
                sampleRate = 48000,
                session,
            )
        audioTrackDefaultPlayer.configureTrack()
        val audioTrackDefaultButton = findViewById<Button>(R.id.button_audiotrack_play_default)
        audioTrackDefaultButton.setOnClickListener { audioTrackDefaultPlayer.play() }

        val audioTrackDefaultSetParams =
            findViewById<Button>(R.id.button_audiotrack_default_set_params)
        audioTrackDefaultSetParams.setOnClickListener {
            audioTrackDefaultPlayer.setPointSourceParams(session, PointSourceParams(soundEntity))
        }

        val audioTrackPointPlayer =
            PointSourceTrackPlayer(
                resources,
                // For Testers: Note that this translates to "/sdcard/Download/tiger_16db_raw.wav"
                Environment.getExternalStorageDirectory().getPath() +
                    "/Download/tiger_16db_raw.wav",
                sampleRate = 48000,
                session,
                PointSourceParams(session.scene.mainPanelEntity),
            )
        audioTrackPointPlayer.configureTrack()
        val audioTrackPointButton = findViewById<Button>(R.id.button_audiotrack_play_point_sound)
        audioTrackPointButton.setOnClickListener { audioTrackPointPlayer.play() }

        val audioTrackParamsButton = findViewById<Button>(R.id.button_audiotrack_point_set_params)
        audioTrackParamsButton.setOnClickListener {
            audioTrackPointPlayer.setPointSourceParams(session, PointSourceParams(soundEntity))
        }

        val audioTrackSoundFieldPlayer =
            SoundFieldTrackPlayer(
                resources,
                // For Testers: Note that this translates to
                // "/sdcard/Download/foa_basketball_raw.wav"
                Environment.getExternalStorageDirectory().getPath() +
                    "/Download/foa_basketball_raw.wav",
                sampleRate = 48000,
                session,
                soundFieldAttributes,
            )
        audioTrackSoundFieldPlayer.configureTrack()

        val audioTrackSoundFieldButton =
            findViewById<Button>(R.id.button_audiotrack_play_sound_field)
        audioTrackSoundFieldButton.setOnClickListener { audioTrackSoundFieldPlayer.play() }

        val audioTrackSoundFieldParamsButton =
            findViewById<Button>(R.id.button_audiotrack_set_params_log_error)
        audioTrackSoundFieldParamsButton.setOnClickListener {
            audioTrackSoundFieldPlayer.setPointSourceParams(session, PointSourceParams(soundEntity))
        }

        // Init MediaPlayer
        // For Testers: Note that this translates to "/sdcard/Download/tiger_16db.mp3"
        val pointSourcePath =
            Environment.getExternalStorageDirectory().getPath() + "/Download/tiger_16db.mp3"
        // For Testers: Note that this translates to "/sdcard/Download/foa_basketball_16bit.wav"
        val soundFieldPath =
            Environment.getExternalStorageDirectory().getPath() +
                "/Download/foa_basketball_16bit.wav"
        // For Testers: Note that this translates to "/sdcard/Download/dunes_test_opus.ogg"
        val soundFieldOpusPath =
            Environment.getExternalStorageDirectory().getPath() + "/Download/dunes_test_opus.ogg"

        val mediaPlayerPointButton = findViewById<Button>(R.id.button_mediaplayer_play_point_sound)
        mediaPlayerPointButton.setOnClickListener {
            mediaplayer.reset()
            mediaplayer.setDataSource(pointSourcePath)

            val audioAttributes =
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()

            SpatialMediaPlayer.setPointSourceParams(session, mediaplayer, pointSourceParams)

            mediaplayer.setAudioAttributes(audioAttributes)
            mediaplayer.prepare()
            mediaplayer.start()
        }

        val mediaPlayerSoundFieldButton =
            findViewById<Button>(R.id.button_mediaplayer_play_sound_field_wav)
        mediaPlayerSoundFieldButton.setOnClickListener {
            mediaplayer.reset()
            mediaplayer.setDataSource(soundFieldPath)

            val audioAttributes =
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()

            SpatialMediaPlayer.setSoundFieldAttributes(session, mediaplayer, soundFieldAttributes)

            mediaplayer.setAudioAttributes(audioAttributes)
            mediaplayer.prepare()
            mediaplayer.start()
        }

        val mediaPlayerSoundFieldOpusButton =
            findViewById<Button>(R.id.button_mediaplayer_play_sound_field_opus)
        mediaPlayerSoundFieldOpusButton.setOnClickListener {
            mediaplayer.reset()
            mediaplayer.setDataSource(soundFieldOpusPath)

            val audioAttributes =
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()

            val thirdOrderAttributes =
                SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER)
            SpatialMediaPlayer.setSoundFieldAttributes(session, mediaplayer, thirdOrderAttributes)

            mediaplayer.setAudioAttributes(audioAttributes)
            mediaplayer.prepare()
            mediaplayer.start()
        }
    }

    private class PointSourceTrackPlayer(
        resources: Resources,
        filePath: String,
        val sampleRate: Int,
        session: Session,
        val pointSourceParams: PointSourceParams,
    ) : AudioTrackPlayer(resources, filePath, sampleRate, session) {

        override fun configureBuilder(session: Session, builder: AudioTrack.Builder) {
            super.configureBuilder(session, builder)
            val unused =
                SpatialAudioTrackBuilder.setPointSourceParams(session, builder, pointSourceParams)
        }
    }

    private class SoundFieldTrackPlayer(
        resources: Resources,
        filePath: String,
        sampleRate: Int,
        session: Session,
        val soundFieldAttributes: SoundFieldAttributes,
    ) : AudioTrackPlayer(resources, filePath, sampleRate, session) {

        override fun configureBuilder(session: Session, builder: AudioTrack.Builder) {
            val unused =
                SpatialAudioTrackBuilder.setSoundFieldAttributes(
                    session,
                    builder,
                    soundFieldAttributes,
                )
        }
    }

    private open class AudioTrackPlayer(
        private val resources: Resources,
        private val filePath: String,
        private val sampleRate: Int = 48000,
        private val session: Session,
    ) {
        lateinit var audioTrack: AudioTrack

        fun configureTrack() {
            FileInputStream(filePath).use {
                val audioData = it.readAllBytes()
                val audioSize = audioData.size

                val builder =
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .build()
                        )
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .setBufferSizeInBytes(audioSize)

                configureBuilder(session, builder)

                audioTrack = builder.build()

                audioTrack.write(audioData, 0, audioSize)
            }
        }

        open fun configureBuilder(session: Session, builder: AudioTrack.Builder) {
            builder.setAudioFormat(
                AudioFormat.Builder()
                    .setChannelMask(CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .build()
            )
        }

        fun play() {
            if (audioTrack.playbackHeadPosition != 0) {
                audioTrack.stop()
                audioTrack.reloadStaticData()
            }

            audioTrack.play()

            when (SpatialAudioTrack.getSpatialSourceType(session, audioTrack)) {
                SpatializerConstants.SOURCE_TYPE_BYPASS -> {
                    Log.d(TAG, "Source type is bypass")
                }
                SpatializerConstants.SOURCE_TYPE_POINT_SOURCE -> {
                    Log.d(
                        TAG,
                        "Point Source: ${SpatialAudioTrack.getPointSourceParams(session, audioTrack)}",
                    )
                }
                SpatializerConstants.SOURCE_TYPE_SOUND_FIELD -> {
                    Log.d(
                        TAG,
                        "Sound Field: ${SpatialAudioTrack.getSoundFieldAttributes(session, audioTrack)}",
                    )
                }
            }
        }

        fun setPointSourceParams(session: Session, params: PointSourceParams) {
            try {
                SpatialAudioTrack.setPointSourceParams(session, audioTrack, params)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Failed to set point source params", e)
            }
        }
    }

    private companion object {
        private const val DEFAULT_VOLUME = 1F
        private const val DEFAULT_PRIORITY = 0
        private const val DEFAULT_LOOP = 0
        private const val DEFAULT_RATE = 1F

        private const val TAG = "SpatialAudioTestActivity"
    }
}
