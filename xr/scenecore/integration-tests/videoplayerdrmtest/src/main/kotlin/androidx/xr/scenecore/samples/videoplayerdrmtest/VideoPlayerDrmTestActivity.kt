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

package androidx.xr.scenecore.samples.videoplayerdrmtest

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.DrmConfiguration
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.HeadTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Test app for integrated Drm functionality. */
class VideoPlayerDrmTestActivity : ComponentActivity() {
    private val TAG = "VideoPlayerDrmTestActivity"

    private var exoPlayer: ExoPlayer? = null
    private val activity = this

    private val drmLicenseUrl = "https://proxy.uat.widevine.com/proxy?provider=widevine_test"
    private val REQUEST_READ_MEDIA_VIDEO: Int = 1
    private var hasPermission: Boolean = false

    private var surfaceEntity: SurfaceEntity? = null
    private var movableComponent: MovableComponent? = null // movable component for surfaceEntity
    private var movableComponentMp: MovableComponent? = null // movable component for MainPanel

    private var videoPlaying by mutableStateOf<Boolean>(false)
    private var queueWithDelay by mutableStateOf<Boolean>(true)
    private var controlPanelEntity: PanelEntity? = null

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.i(TAG, "Media Selected")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")

        val session = (Session.create(this) as SessionCreateSuccess).session
        session.configure(Config(headTracking = HeadTrackingMode.LAST_KNOWN))
        session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f

        if (movableComponentMp == null) {
            movableComponentMp = MovableComponent.createSystemMovable(session)
            val unused = session.scene.mainPanelEntity.addComponent(movableComponentMp!!)
        }

        setContent { BootstrapUi(session, activity) }

        checkExternalStoragePermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }

    private fun togglePassthrough(session: Session) {
        val passthroughOpacity: Float = session.scene.spatialEnvironment.currentPassthroughOpacity
        Log.i(TAG, "TogglePassthrough!")
        when (passthroughOpacity) {
            0.0f -> session.scene.spatialEnvironment.preferredPassthroughOpacity = 1.0f
            1.0f -> session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
        }
    }

    // Request the external storage permission so that we can read large files from the SDCard
    private fun checkExternalStoragePermission() {
        if (
            checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            hasPermission = false
            requestPermissions(
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
                REQUEST_READ_MEDIA_VIDEO,
            )
        } else {
            hasPermission = true
        }
    }

    private fun setupControlPanel(session: Session) {
        // Technically this leaks, but it's a sample / test app.
        val panelContentView =
            ComposeView(this).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { VideoPlayerControls(session) }
            }

        controlPanelEntity =
            PanelEntity.create(
                session,
                panelContentView,
                // These are about the right size to fit the buttons without a lot of invisible
                // panel edges
                IntSize2d(640, 480),
                "playerControls",
                Pose(Vector3(0.0f, -0.25f, 0.25f)), // below and slightly in front of the canvas
            )
        controlPanelEntity!!.parent = surfaceEntity!!

        // TODO: b/413478924 - Use controlPanelEntity.view when the api is available.
        val parentView: View =
            if (panelContentView.parent != null && panelContentView.parent is View)
                panelContentView.parent as View
            else panelContentView

        parentView.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        parentView.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        parentView.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
    }

    fun initializeExoPlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build()
        }
        return exoPlayer!!
    }

    fun fastRewindOneSec() {
        if (videoPlaying) {
            exoPlayer?.seekTo(exoPlayer!!.currentPosition - 1000)
        }
    }

    fun fastForwardOneSec() {
        if (videoPlaying) {
            exoPlayer?.seekTo(exoPlayer!!.currentPosition + 1000)
        }
    }

    fun destroySurfaceEntity() {
        Log.i(TAG, "destroySurfaceEntity")
        videoPlaying = false
        exoPlayer?.release()
        exoPlayer = null
        if (surfaceEntity != null) {
            surfaceEntity!!.dispose()
            surfaceEntity = null
        }
    }

    fun getCanvasAspectRatio(stereoMode: Int, videoWidth: Int, videoHeight: Int): FloatSize3d {
        when (stereoMode) {
            SurfaceEntity.StereoMode.MONO,
            SurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY,
            SurfaceEntity.StereoMode.MULTIVIEW_RIGHT_PRIMARY ->
                return FloatSize3d(1.0f, videoHeight.toFloat() / videoWidth, 0.0f)
            SurfaceEntity.StereoMode.TOP_BOTTOM ->
                return FloatSize3d(1.0f, 0.5f * videoHeight.toFloat() / videoWidth, 0.0f)
            SurfaceEntity.StereoMode.SIDE_BY_SIDE ->
                return FloatSize3d(1.0f, 2.0f * videoHeight.toFloat() / videoWidth, 0.0f)
            else -> throw IllegalArgumentException("Unsupported stereo mode: $stereoMode")
        }
    }

    fun playVideo(
        session: Session,
        videoUri: String,
        stereoMode: Int,
        pose: Pose,
        canvasShape: SurfaceEntity.CanvasShape,
        loop: Boolean = true,
        protected: Boolean = false,
    ) {
        Log.i(TAG, "playVideo: $videoUri $protected")
        if (surfaceEntity == null) {
            val surfaceContentLevel =
                if (protected) {
                    SurfaceEntity.ContentSecurityLevel.PROTECTED
                } else {
                    SurfaceEntity.ContentSecurityLevel.NONE
                }

            surfaceEntity =
                SurfaceEntity.create(session, stereoMode, pose, canvasShape, surfaceContentLevel)
            // Make the video player movable (to make it easier to look at it from different
            // angles and distances) (only on quad canvas)
            movableComponent = MovableComponent.createSystemMovable(session)
            // The quad has a radius of 1.0 meters
            movableComponent!!.size = FloatSize3d(1.0f, 1.0f, 1.0f)

            if (canvasShape is SurfaceEntity.CanvasShape.Quad) {
                val unused = surfaceEntity!!.addComponent(movableComponent!!)
            }
        }

        // Get or initialize the ExoPlayer.
        val player = initializeExoPlayer(activity)
        // Update the Composable's state.
        exoPlayer = player
        // Set the video surface.
        player.setVideoSurface(surfaceEntity!!.getSurface())
        // Clear previous media items.
        player.clearMediaItems()

        val mediaItem =
            if (protected) {
                MediaItem.Builder()
                    .setUri(videoUri)
                    .setDrmConfiguration(
                        DrmConfiguration.Builder(C.WIDEVINE_UUID)
                            .setLicenseUri(drmLicenseUrl)
                            .build()
                    )
                    .build()
            } else {
                MediaItem.fromUri(videoUri)
            }
        player.setMediaItem(mediaItem)

        player.addListener(
            object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    val width = videoSize.width
                    val height = videoSize.height
                    check(width >= 0 && height >= 0) { "Canvas size must be larger than 0" }

                    // Resize the canvas to match the video aspect ratio - accounting for
                    // the stereo
                    // mode.
                    val dimensions = getCanvasAspectRatio(stereoMode, width, height)
                    // Set the dimensions of the Quad canvas to the video dimensions and
                    // attach the
                    // a MovableComponent.
                    if (canvasShape is SurfaceEntity.CanvasShape.Quad) {
                        surfaceEntity?.canvasShape =
                            SurfaceEntity.CanvasShape.Quad(dimensions.width, dimensions.height)
                        movableComponent?.size =
                            surfaceEntity?.dimensions ?: FloatSize3d(1.0f, 1.0f, 1.0f)
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    Log.i(TAG, "onPlaybackStateChanged: $playbackState")
                    // Update videoPlaying based on ExoPlayer's isPlaying property.
                    videoPlaying = exoPlayer?.isPlaying ?: false // Use safe call and elvis operator
                    if (playbackState == Player.STATE_ENDED) {
                        destroySurfaceEntity()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Player error: $error")
                }
            }
        )
        if (loop) {
            player.setRepeatMode(Player.REPEAT_MODE_ALL)
        }
        player.playWhenReady = true
        player.prepare()
        setupControlPanel(session)
    }

    /** A simple composable that toggles the passthrough on and off to test environment changes. */
    // TODO: b/324947709 - Refactor common @Composable code into a utility library for common usage
    // across sample apps.
    @Composable
    fun BootstrapUi(session: Session, activity: Activity) {
        // Add a panel to the main activity with a button to toggle passthrough
        LaunchedEffect(Unit) {
            activity.setContentView(
                createButtonViewUsingCompose(activity = activity, session = session)
            )
        }
    }

    private fun createButtonViewUsingCompose(activity: Activity, session: Session): View {
        val view =
            ComposeView(activity.applicationContext).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { VideoPlayerTestActivityUI(session, activity) }
            }
        view.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        view.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        view.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
        return view
    }

    @Composable
    fun VideoPlayerControls(session: Session) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { fastRewindOneSec() }) { Text(text = "1s-", fontSize = 10.sp) }
                Button(onClick = { fastForwardOneSec() }) { Text(text = "1s+", fontSize = 10.sp) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { destroySurfaceEntity() }) {
                    Text(text = "End Video", fontSize = 10.sp)
                }
            }
        } // end column
    }

    @Composable
    fun PlayVideoButton(
        session: Session,
        activity: Activity,
        videoUri: String,
        stereoMode: Int,
        pose: Pose,
        canvasShape: SurfaceEntity.CanvasShape,
        buttonText: String,
        enabled: Boolean = true,
        loop: Boolean = true,
        protected: Boolean = false,
    ) {
        val file = File(videoUri)
        if (!file.exists()) {
            Log.e(TAG, "File ($videoUri) does not exist. Did you download all the assets?")
            Toast.makeText(
                    activity,
                    "File ($videoUri) does not exist. Did you download all the assets?",
                    Toast.LENGTH_LONG,
                )
                .show()
            return
        }

        Button(
            enabled = enabled,
            onClick = {
                // Create SurfaceEntity and MovableComponent if they don't exist.
                playVideo(session, videoUri, stereoMode, pose, canvasShape, loop, protected)
            },
        ) {
            Text(text = buttonText, fontSize = 20.sp)
        }
    }

    @Composable
    fun PlayVideoButton(
        activity: Activity,
        videoUri: String,
        buttonText: String,
        onClick: () -> Unit,
    ) {
        val file = File(videoUri)
        var enabled = true
        if (!file.exists()) {
            Log.e(TAG, "File ($videoUri) does not exist. Did you download all the assets?")
            Toast.makeText(
                    activity,
                    "File ($videoUri) does not exist. Did you download all the assets?",
                    Toast.LENGTH_LONG,
                )
                .show()
            enabled = false
        }

        Button(enabled = enabled, onClick = onClick) { Text(text = buttonText, fontSize = 20.sp) }
    }

    @Composable
    fun NonDrmVideoButton(session: Session, activity: Activity) {
        val videoUri =
            Environment.getExternalStorageDirectory().getPath() + "/Download/vid_bigbuckbunny.mp4"
        val playVideoAction = {
            playVideo(
                session = session,
                videoUri = videoUri,
                stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                pose = Pose(Vector3(0.0f, 0.0f, -0.25f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
                canvasShape = SurfaceEntity.CanvasShape.Quad(1.0f, 1.0f),
                loop = true,
                protected = false,
            )
        }

        PlayVideoButton(
            activity = activity,
            videoUri = videoUri,
            buttonText =
                if (!videoPlaying) {
                    "Play Non-Drm Video"
                } else {
                    "Queue Non-Drm Video"
                },
            onClick = {
                if (!videoPlaying) {
                    Log.i(TAG, "(No Video Playing) Play Non-Drm Video")
                    playVideoAction()
                } else {
                    destroySurfaceEntity()
                    if (queueWithDelay) {
                        Log.i(TAG, "(Video Playing) Queue Non-Drm Video")
                        lifecycleScope.launch {
                            delay(300)
                            playVideoAction()
                        }
                    } else {
                        Log.i(TAG, "Immediate Queue Non-Drm Video")
                        playVideoAction()
                    }
                }
            },
        )
    }

    @Composable
    fun DrmVideoButton(session: Session, activity: Activity) {
        val videoUri =
            Environment.getExternalStorageDirectory().getPath() +
                "/Download/sdr_singleview_protected.mp4"
        val playVideoAction = {
            playVideo(
                session = session,
                videoUri = videoUri,
                stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE,
                pose = Pose(Vector3(0.0f, 0.0f, -0.25f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
                canvasShape = SurfaceEntity.CanvasShape.Quad(1.0f, 1.0f),
                loop = true,
                protected = true,
            )
        }

        PlayVideoButton(
            activity = activity,
            videoUri = videoUri,
            buttonText =
                if (!videoPlaying) {
                    "Play Drm Video"
                } else {
                    "Queue Drm Video"
                },
            onClick = {
                if (!videoPlaying) {
                    Log.i(TAG, "(No Video Playing) Play Drm Video")
                    playVideoAction()
                } else {
                    destroySurfaceEntity()
                    if (queueWithDelay) {
                        Log.i(TAG, "Queue Drm Video")
                        lifecycleScope.launch {
                            delay(300)
                            playVideoAction()
                        }
                    } else {
                        Log.i(TAG, "Immediate Queue Drm Video")
                        playVideoAction()
                    }
                }
            },
        )
    }

    @Composable
    fun VideoPlayerTestActivityUI(session: Session, activity: Activity) {
        val videoPaused = remember { mutableStateOf(false) }

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f).padding(8.dp),
            ) {
                Text(text = "System APIs", fontSize = 50.sp)
                Button(onClick = { togglePassthrough(session) }) {
                    Text(text = "Toggle Passthrough", fontSize = 30.sp)
                }
                Button(onClick = { session.scene.requestFullSpaceMode() }) {
                    Text(text = "Request FSM", fontSize = 30.sp)
                }
                Button(onClick = { session.scene.requestHomeSpaceMode() }) {
                    Text(text = "Request HSM", fontSize = 30.sp)
                }
                Button(onClick = { ActivityCompat.recreate(activity) }) {
                    Text(text = "Recreate Activity", fontSize = 30.sp)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "Queue with delay:")
                    Switch(
                        checked = queueWithDelay,
                        onCheckedChange = { newValue -> queueWithDelay = newValue },
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f).padding(8.dp),
            ) {
                Text(text = "SurfaceEntity Video Drm Test", fontSize = 30.sp)

                Button(
                    onClick = {
                        startActivity(
                            Intent(this@VideoPlayerDrmTestActivity, SecondActivity::class.java)
                        )
                    }
                ) {
                    Text(text = "Launch 2nd App Activity")
                }
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_PICK).apply { type = "video/*" }
                        pickMedia.launch(intent)
                    }
                ) {
                    Text(text = "Launch System Activity")
                }

                NonDrmVideoButton(session, activity)
                DrmVideoButton(session, activity)

                if (videoPlaying) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f).padding(8.dp),
                    ) {
                        VideoPlayerControls(session)
                        Button(
                            onClick = {
                                videoPaused.value = !videoPaused.value
                                if (videoPaused.value) {
                                    exoPlayer?.pause()
                                } else {
                                    exoPlayer?.play()
                                }
                            }
                        ) {
                            Text(text = "Toggle Pause Stereo video", fontSize = 30.sp)
                        }
                    }
                }
            }
        }
    }
}
