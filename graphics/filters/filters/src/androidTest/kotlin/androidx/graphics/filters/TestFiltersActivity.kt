/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.graphics.filters

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaLibraryInfo.TAG
import androidx.media3.common.util.Log
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.TransformationException
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.TransformationResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import com.google.common.collect.ImmutableList
import java.io.File
import java.io.IOException

private val PRESET_FILE_URIS =
    arrayOf(
        "https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-screens-10s.mp4",
        "https://storage.googleapis.com/exoplayer-test-media-0/android-block-1080-hevc.mp4",
        "https://html5demos.com/assets/dizzy.mp4",
        "https://html5demos.com/assets/dizzy.webm",
        "https://storage.googleapis.com/exoplayer-test-media-1/mp4/portrait_4k60.mp4",
        "https://storage.googleapis.com/exoplayer-test-media-1/mp4/8k24fps_4s.mp4",
        "https://storage.googleapis.com/exoplayer-test-media-1/mp4/1920w_1080h_4s.mp4",
        "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4",
        "https://storage.googleapis.com/exoplayer-test-media-1/mp4/portrait_avc_aac.mp4",
        "https://storage.googleapis.com/exoplayer-test-media-1/mp4/portrait_rotated_avc_aac.mp4",
        "https://storage.googleapis.com/exoplayer-test-media-internal-63834241aced7884c2544af1a" +
            "3452e01/mp4/slow%20motion/slowMotion_countdown_120fps.mp4",
        "https://storage.googleapis.com/exoplayer-test-media-1/mp4/slow-motion/" +
            "slowMotion_stopwatch_240fps_long.mp4",
        "https://storage.googleapis.com/exoplayer-test-media-1/gen/screens/dash-vod-single-segment/" +
            "manifest-baseline.mpd",
        "https://storage.googleapis.com/exoplayer-test-media-1/mp4/samsung-s21-hdr-hdr10.mp4",
        "https://storage.googleapis.com/exoplayer-test-media-1/mp4/Pixel7Pro_HLG_1080P.mp4",
        "https://storage.googleapis.com/exoplayer-test-media-internal-63834241aced7884c2544af1a3452" +
            "e01/mp4/sony-hdr-hlg-full-range.mp4"
    )

class TestFiltersActivity : Activity() {

    private var outputFile: File? = null
    private var transformer: Transformer? = null
    private var sourcePlayer: ExoPlayer? = null
    private var filteredPlayer: ExoPlayer? = null
    private var sourcePlayerView: PlayerView? = null
    private var filteredPlayerView: PlayerView? = null
    private var filterButton: Button? = null
    private var statusBar: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sourcePlayerView = PlayerView(this@TestFiltersActivity)
        sourcePlayerView!!.minimumHeight = 480
        sourcePlayerView!!.minimumWidth = 640
        sourcePlayerView!!.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        filteredPlayerView = PlayerView(this@TestFiltersActivity)
        filteredPlayerView!!.minimumHeight = 480
        filteredPlayerView!!.minimumWidth = 640
        filteredPlayerView!!.layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)

        statusBar = TextView(this)

        setContentView(
            FrameLayout(this).apply {
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)

                addView(
                    ScrollView(this@TestFiltersActivity).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                        addView(
                            LinearLayout(this@TestFiltersActivity).apply {
                                layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                                orientation = LinearLayout.VERTICAL
                                addView(
                                    TextView(this@TestFiltersActivity).apply {
                                        layoutParams =
                                            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                                                .apply { gravity = Gravity.LEFT }
                                        text = "Source Video"
                                    }
                                )
                                addView(sourcePlayerView)
                                addView(
                                    TextView(this@TestFiltersActivity).apply {
                                        layoutParams =
                                            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                                                .apply { gravity = Gravity.LEFT }
                                        text = "Filtered Video"
                                    }
                                )
                                addView(filteredPlayerView)
                                addView(statusBar)
                                addView(createControls())
                            }
                        )
                    }
                )
            }
        )
    }

    private fun createControls(): View {
        this.filterButton = Button(this)
        this.filterButton!!.text = "Run Filter"
        this.filterButton!!.setOnClickListener(
            View.OnClickListener { this@TestFiltersActivity.startTransformation() }
        )

        val controls =
            LinearLayout(this).apply {
                layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                orientation = LinearLayout.HORIZONTAL

                addView(this@TestFiltersActivity.filterButton)
            }

        return controls
    }

    private fun startTransformation() {
        statusBar!!.text = "Request permissions"
        requestPermission()
        statusBar!!.text = "Setup transformation"

        val mediaUri = Uri.parse(PRESET_FILE_URIS[0])
        try {
            outputFile = createExternalCacheFile("filters-output.mp4")
            val outputFilePath: String = outputFile!!.getAbsolutePath()
            val mediaItem: MediaItem = createMediaItem(mediaUri)
            var transformer: Transformer = createTransformer(outputFilePath)
            transformer.startTransformation(mediaItem, outputFilePath)
            this.transformer = transformer
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
        val mainHandler = Handler(mainLooper)
        val progressHolder = ProgressHolder()
        mainHandler.post(
            object : Runnable {
                override fun run() {
                    if (
                        transformer?.getProgress(progressHolder) !=
                            Transformer.PROGRESS_STATE_NO_TRANSFORMATION
                    ) {
                        mainHandler.postDelayed(/* r= */ this, /* delayMillis= */ 500)
                    }
                }
            }
        )
    }

    private fun createTransformer(filePath: String): Transformer {
        val transformerBuilder = Transformer.Builder(/* context= */ this)
        val effects: List<Effect> = createEffectsList()

        val requestBuilder = TransformationRequest.Builder()
        transformerBuilder
            .setTransformationRequest(requestBuilder.build())
            .setEncoderFactory(
                DefaultEncoderFactory.Builder(this.applicationContext)
                    .setEnableFallback(false)
                    .build()
            )
        transformerBuilder.setVideoEffects(effects)

        return transformerBuilder
            .addListener(
                object : Transformer.Listener {
                    override fun onTransformationCompleted(
                        mediaItem: MediaItem,
                        transformationResult: TransformationResult
                    ) {
                        this@TestFiltersActivity.onTransformationCompleted(filePath, mediaItem)
                    }

                    override fun onTransformationError(
                        mediaItem: MediaItem,
                        exception: TransformationException
                    ) {
                        this@TestFiltersActivity.onTransformationError(exception)
                    }
                }
            )
            .build()
    }

    private fun onTransformationError(exception: TransformationException) {
        statusBar!!.text = "Transformation error: " + exception.message
        Log.e(TAG, "Transformation error", exception)
    }

    private fun onTransformationCompleted(filePath: String, inputMediaItem: MediaItem?) {
        statusBar!!.text = "Transformation success!"
        Log.d(TAG, "Output file path: file://$filePath")
        playMediaItems(inputMediaItem, MediaItem.fromUri("file://" + filePath))
    }

    private fun playMediaItems(inputMediaItem: MediaItem?, outputMediaItem: MediaItem) {
        sourcePlayerView!!.player = null
        filteredPlayerView!!.player = null

        releasePlayer()
        var sourcePlayer = ExoPlayer.Builder(/* context= */ this).build()
        sourcePlayerView!!.player = sourcePlayer
        sourcePlayerView!!.controllerAutoShow = false
        if (inputMediaItem != null) {
            sourcePlayer.setMediaItem(inputMediaItem)
        }
        sourcePlayer.prepare()
        this.sourcePlayer = sourcePlayer
        sourcePlayer.volume = 0f
        var filteredPlayer = ExoPlayer.Builder(/* context= */ this).build()
        filteredPlayerView!!.player = filteredPlayer
        filteredPlayerView!!.controllerAutoShow = false
        filteredPlayer.setMediaItem(outputMediaItem)
        filteredPlayer.prepare()
        this.filteredPlayer = filteredPlayer
        sourcePlayer.play()
        filteredPlayer.play()
    }

    private fun releasePlayer() {
        if (sourcePlayer != null) {
            sourcePlayer!!.release()
            sourcePlayer = null
        }
        if (filteredPlayer != null) {
            filteredPlayer!!.release()
            filteredPlayer = null
        }
    }

    private fun createEffectsList(): List<Effect> {
        val effects = ImmutableList.Builder<Effect>()

        effects.add(Vignette(0.5f, 0.75f))

        return effects.build()
    }

    private fun requestPermission() {
        if (Util.SDK_INT < 23) {
            return
        }

        if (checkSelfPermission(READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(Array<String>(1) { READ_EXTERNAL_STORAGE }, /* requestCode= */ 0)
        }
    }

    @Throws(IOException::class)
    private fun createExternalCacheFile(fileName: String): File? {
        val file = File(externalCacheDir, fileName)
        check(!(file.exists() && !file.delete())) {
            "Could not delete the previous transformer output file"
        }
        check(file.createNewFile()) { "Could not create the transformer output file" }
        return file
    }

    private fun createMediaItem(uri: Uri): MediaItem {
        val mediaItemBuilder = MediaItem.Builder().setUri(uri)
        return mediaItemBuilder.build()
    }
}
