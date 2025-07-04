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

package androidx.xr.scenecore.samples.videoplayertest

import android.app.Activity
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.exoplayer.ExoPlayer
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * VideoPlayerSubtitles is responsible for cyclically displaying head-locked simulated subtitles. It
 * also supports pausing and resuming subtitle playback.
 */
class VideoPlayerSubtitles(activity: Activity, session: Session, exoPlayer: ExoPlayer) {

    private var panelEntity: PanelEntity? = null
    private var coroutineScope: CoroutineScope? = null
    private var poseUpdater: PoseUpdater? = null
    private var textUpdater: TextUpdater? = null

    enum class SubtitleState {
        RUNNING,
        PAUSED,
    }

    init {
        setupPanel(activity, session, exoPlayer)
    }

    fun pause() {
        textUpdater?.pause()
    }

    fun resume() {
        textUpdater?.resume()
    }

    fun destroy() {
        if (textUpdater != null) {
            textUpdater!!.dispose()
            textUpdater = null
        }
        if (poseUpdater != null) {
            poseUpdater!!.dispose()
            poseUpdater = null
        }
        coroutineScope?.cancel()
        coroutineScope = null
        if (panelEntity != null) {
            panelEntity!!.dispose()
            panelEntity = null
        }
    }

    /** Cycles through fake subtitles on a delay. Able to pause and resume. */
    class TextUpdater(private val mainScope: CoroutineScope, private val textView: TextView) {
        val subtitleTexts: List<String> =
            listOf(
                "[Fake Subtitle] Hey folks. Let's call it a day. It's getting a bit late.",
                "Agree.",
                "Alright, let's continue our research tomorrow.",
            )
        private var subtitleTextUpdateJob: Job? = null
        private val stateFlow = MutableStateFlow(SubtitleState.RUNNING)

        fun start() {
            subtitleTextUpdateJob =
                mainScope.launch {
                    var counter = 0
                    stateFlow.collectLatest { state ->
                        when (state) {
                            SubtitleState.RUNNING -> {
                                flow {
                                        while (isActive) {
                                            emit(counter++)
                                            delay(3000)
                                        }
                                    }
                                    .collect { currentCounter ->
                                        textView.text =
                                            subtitleTexts[currentCounter % subtitleTexts.size]
                                    }
                            }
                            SubtitleState.PAUSED -> {}
                        }
                    }
                }
        }

        fun pause() {
            stateFlow.value = SubtitleState.PAUSED
        }

        fun resume() {
            stateFlow.value = SubtitleState.RUNNING
        }

        fun dispose() {
            subtitleTextUpdateJob?.cancel()
            subtitleTextUpdateJob = null
        }
    }

    /** Update head-locked pose per frame. */
    class PoseUpdater(
        private val session: Session,
        private val subtitlePanelEntity: PanelEntity,
        private val mainScope: CoroutineScope,
    ) : Choreographer.FrameCallback {

        private var choreographer: Choreographer? = null
        private var isRunning = false

        fun start() {
            isRunning = true
            mainScope.launch {
                choreographer = Choreographer.getInstance()
                choreographer?.postFrameCallback(this@PoseUpdater)
            }
        }

        fun stop() {
            isRunning = false
            mainScope.launch { choreographer?.removeFrameCallback(this@PoseUpdater) }
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning) return
            updateSubtitlePose()
            choreographer?.postFrameCallback(this)
        }

        private fun updateSubtitlePose() {
            subtitlePanelEntity.setPose(
                session.scene.spatialUser.head?.transformPoseTo(
                    Pose(Vector3(0.0f, -0.3f, -1.0f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
                    session.scene.activitySpace,
                )!!
            )
        }

        fun dispose() {
            stop()
        }
    }

    private fun setupPanel(activity: Activity, session: Session, exoPlayer: ExoPlayer) {
        require(panelEntity == null) { "subtitlePanelEntity should be null!" }
        require(coroutineScope == null) { "subtitleCoroutineScope should be null!" }
        require(poseUpdater == null) { "subtitlePoseUpdater should be null!" }
        require(textUpdater == null) { "subtitleTextUpdater should be null!" }

        val subtitleTextView =
            TextView(activity).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                setTextColor(android.graphics.Color.YELLOW)
                paint.setShadowLayer(
                    3f, // radius
                    2f, // dx
                    2f, // dy
                    android.graphics.Color.BLACK, // shadow color
                )
                textSize = 20f
            }

        panelEntity =
            PanelEntity.create(
                session,
                subtitleTextView,
                IntSize2d(1000, 40),
                "subtitlePanel",
                Pose.Identity,
            )

        val parentView: View =
            if (subtitleTextView.parent != null && subtitleTextView.parent is View)
                subtitleTextView.parent as View
            else subtitleTextView

        parentView.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        parentView.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        parentView.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)

        coroutineScope = CoroutineScope(Dispatchers.Main)

        poseUpdater = PoseUpdater(session, panelEntity!!, coroutineScope!!).apply { start() }
        textUpdater =
            TextUpdater(coroutineScope!!, subtitleTextView).apply {
                start()
                val isPlaying = exoPlayer.isPlaying
                if (!isPlaying) pause()
            }
    }
}
