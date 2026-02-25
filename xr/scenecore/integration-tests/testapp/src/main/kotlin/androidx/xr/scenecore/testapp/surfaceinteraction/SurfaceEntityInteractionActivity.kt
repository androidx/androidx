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

package androidx.xr.scenecore.testapp.surfaceinteraction

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.Hand
import androidx.xr.arcore.HandJointType
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GroupEntity
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.isMvHevcSupported
import java.io.File

internal const val TAG = "JXR-SurfaceEntityInteractionActivity"

class SurfaceEntityInteractionActivity : AppCompatActivity() {
    private val activity = this
    private var hasPermission: Boolean = false
    private var surfaceParent: GroupEntity? = null
    private var surfaceEntity: SurfaceEntity? = null
    private var exoPlayer: ExoPlayer? = null
    private val requestReadMediaVideo: Int = 1

    private var attachedInteractable: InteractableComponent? = null
    private var videoSelected: VideoEnums? = null
    private val videoAttrSelected: VideoAttributes?
        get() {
            return if (videoSelected == null) null else videoAttributesMap[videoSelected!!.ordinal]
        }

    private lateinit var view: View
    private lateinit var session: Session

    private lateinit var scene: Scene
    private lateinit var device: ArDevice

    private lateinit var videoInputManager: VideoInputManager
    private lateinit var pointerLogManager: PointerLogManager

    private lateinit var buttonSelectQuad: Button
    private lateinit var buttonSelectSphere: Button
    private lateinit var buttonSelectHemisphere: Button
    private lateinit var buttonDeselectVideo: Button
    private lateinit var switchInteractableAttached: Switch
    private lateinit var switchSingleClickEnabled: Switch
    private lateinit var switchDoubleClickEnabled: Switch
    private lateinit var switchDragEnabled: Switch
    private lateinit var textViewPointerLogs: TextView
    private lateinit var mvHevcNotSupportedText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_surface_interaction)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        session = (Session.create(this) as SessionCreateSuccess).session
        scene = session.scene
        scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
        session.configure(
            Config(
                deviceTracking = DeviceTrackingMode.LAST_KNOWN,
                handTracking = HandTrackingMode.BOTH,
            )
        )
        session.scene.keyEntity = session.scene.mainPanelEntity
        device = ArDevice.getInstance(session)

        surfaceParent = GroupEntity.create(session, "SurfaceParent", Pose.Identity)
        videoInputManager = VideoInputManager()
        pointerLogManager = PointerLogManager(this, session)

        // Set up the MoveableComponent so the user can move the Main Panel out of the way of
        // video canvases which appear behind it.
        if (scene.mainPanelEntity.getComponentsOfType(MovableComponent::class.java).isEmpty()) {
            val comp = MovableComponent.createSystemMovable(session)
            scene.mainPanelEntity.addComponent(comp)
            comp.size = scene.mainPanelEntity.size.to3d()
        }

        checkExternalStoragePermission()

        // Setup View Buttons
        val toolBar = findViewById<Toolbar>(R.id.surface_interaction_topAppBar)
        setSupportActionBar(toolBar)
        toolBar.setNavigationOnClickListener { activity.finish() }

        buttonSelectQuad = findViewById(R.id.button_select_quad_video)
        buttonSelectQuad.setOnClickListener { view ->
            onSelectedVideoClicked(VideoEnums.BIG_BUCK_BUNNY_BUTTON)
        }

        buttonSelectSphere = findViewById(R.id.button_select_360_video)
        buttonSelectSphere.setOnClickListener { view ->
            onSelectedVideoClicked(VideoEnums.GALAXY_360_MVHEVC_BUTTON)
        }

        buttonSelectHemisphere = findViewById(R.id.button_select_180_video)
        buttonSelectHemisphere.setOnClickListener { view ->
            onSelectedVideoClicked(VideoEnums.NAVER_180_MVHEVC_BUTTON)
        }

        buttonDeselectVideo = findViewById(R.id.button_deselect_video)
        buttonDeselectVideo.setOnClickListener {
            deselectVideo()
            updateButtonsEnabled(videoSelected, attachedInteractable != null)
        }

        switchInteractableAttached = findViewById(R.id.switch_interactable_attached)
        switchInteractableAttached.setOnClickListener(this::onSwitchInteractableClicked)

        switchSingleClickEnabled = findViewById(R.id.switch_single_click_enabled)
        switchSingleClickEnabled.setOnClickListener {
            videoInputManager.singleClickEnabled = !videoInputManager.singleClickEnabled
            switchSingleClickEnabled.isChecked = videoInputManager.singleClickEnabled
        }

        switchDoubleClickEnabled = findViewById(R.id.switch_double_click_enabled)
        switchDoubleClickEnabled.setOnClickListener {
            videoInputManager.doubleClickEnabled = !videoInputManager.doubleClickEnabled
            switchDoubleClickEnabled.isChecked = videoInputManager.doubleClickEnabled
        }

        switchDragEnabled = findViewById(R.id.switch_drag_enabled)
        switchDragEnabled.setOnClickListener {
            videoInputManager.dragEnabled = !videoInputManager.dragEnabled
            switchDragEnabled.isChecked = videoInputManager.dragEnabled
        }

        textViewPointerLogs = findViewById(R.id.textView_pointer_log)
        textViewPointerLogs.text = ""

        mvHevcNotSupportedText = findViewById(R.id.mv_hevc_not_supported_text)
        if (!isMvHevcSupported()) {
            mvHevcNotSupportedText.visibility = View.VISIBLE
        }

        updateButtonsEnabled(null, false)

        view = window.decorView
        view.postOnAnimation(this::onAnimation)
    }

    private fun onAnimation() {
        if (surfaceParent == null) return

        val headPose = device.state.value.devicePose

        val rightState = Hand.right(session)?.state?.value
        val rightPose =
            if (rightState?.trackingState == TrackingState.TRACKING)
                scene.perceptionSpace.transformPoseTo(
                    rightState.handJoints[HandJointType.HAND_JOINT_TYPE_PALM]!!,
                    scene.activitySpace,
                )
            else null

        val leftState = Hand.left(session)?.state?.value
        val leftPose =
            if (leftState?.trackingState == TrackingState.TRACKING)
                scene.perceptionSpace.transformPoseTo(
                    leftState.handJoints[HandJointType.HAND_JOINT_TYPE_PALM]!!,
                    scene.activitySpace,
                )
            else null

        // Place video canvas on the head (gravity aligned)
        val videoAttr = videoAttrSelected
        if (videoAttr != null && videoAttr.stickToHead) {
            surfaceParent!!.setPose(Pose(headPose.translation, surfaceParent!!.getPose().rotation))
        }

        val followingPortion = 0.3f
        // Place default pointer debug panel
        val debugPanelDefault = pointerLogManager.default.validPanel
        if (debugPanelDefault != null) {
            val oldPose = debugPanelDefault.getPose()
            val newPose = headPose.compose(Pose(Vector3(0f, 0f, -1f), Quaternion.Identity))
            debugPanelDefault.setPose(Pose.lerp(oldPose, newPose, followingPortion))
        }

        // Place left pointer debug panel
        val debugPanelLeft = pointerLogManager.left.validPanel
        if (leftPose != null && debugPanelLeft != null) {
            val oldPose = debugPanelLeft.getPose()
            val newPos = leftPose.translation + Vector3(0f, 0.05f, 0f)
            val newRot = headPose.rotation
            debugPanelLeft.setPose(Pose.lerp(oldPose, Pose(newPos, newRot), followingPortion))
        }

        // Place right pointer debug panel
        val debugPanelRight = pointerLogManager.right.validPanel
        if (rightPose != null && debugPanelRight != null) {
            val oldPose = debugPanelRight.getPose()
            val newPos = rightPose.translation + Vector3(0f, 0.05f, 0f)
            val newRot = headPose.rotation
            debugPanelRight.setPose(Pose.lerp(oldPose, Pose(newPos, newRot), followingPortion))
        }

        view.postOnAnimation(this::onAnimation)
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.stop()
        exoPlayer?.release()
    }

    // Request the external storage permission so that we can read large files from the SDCard
    private fun checkExternalStoragePermission() {
        if (
            checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            hasPermission = false
            requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_VIDEO), requestReadMediaVideo)
        } else {
            hasPermission = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if (requestCode == requestReadMediaVideo) {
            hasPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun updateButtonsEnabled(selectedVideo: VideoEnums?, interactableAttached: Boolean) {
        val isVideoSelected = selectedVideo != null
        buttonSelectQuad.isEnabled = selectedVideo != VideoEnums.BIG_BUCK_BUNNY_BUTTON
        buttonSelectSphere.isEnabled =
            isMvHevcSupported() && (selectedVideo != VideoEnums.GALAXY_360_MVHEVC_BUTTON)
        buttonSelectHemisphere.isEnabled =
            isMvHevcSupported() && (selectedVideo != VideoEnums.NAVER_180_MVHEVC_BUTTON)
        buttonDeselectVideo.isEnabled = isVideoSelected

        switchInteractableAttached.isEnabled = selectedVideo != null
        switchInteractableAttached.isChecked = interactableAttached

        switchSingleClickEnabled.isEnabled = interactableAttached && isVideoSelected
        switchSingleClickEnabled.isChecked =
            interactableAttached && videoInputManager.singleClickEnabled

        switchDoubleClickEnabled.isEnabled = interactableAttached && isVideoSelected
        switchDoubleClickEnabled.isChecked =
            interactableAttached && videoInputManager.doubleClickEnabled

        switchDragEnabled.isEnabled = interactableAttached && videoInputManager.canDrag
        switchDragEnabled.isChecked = videoInputManager.canDrag && videoInputManager.dragEnabled
    }

    private fun onSelectedVideoClicked(videoEnum: VideoEnums) {
        val index = videoEnum.ordinal
        val videoAttr = videoAttributesMap[index]!!

        if (videoSelected != null && videoSelected!! != videoEnum) {
            deselectVideo()
        }

        val videoPath = Environment.getExternalStorageDirectory().path + videoAttr.videoPath
        // Check if file exists
        val file = File(videoPath)
        if (!file.exists()) {
            Toast.makeText(
                    activity,
                    "File does not exist. Did you download all the assets?",
                    Toast.LENGTH_LONG,
                )
                .show()
            return
        }

        surfaceEntity =
            createSurfaceEntity(
                session = session,
                parent = surfaceParent!!,
                initPose = videoAttr.shapeOffset,
                shape = videoAttr.canvasShape,
                stereoMode = videoAttr.stereoMode,
                surfaceProtection = videoAttr.protection,
                movable = videoAttr.movable,
            )

        alignPoseToPlayerHead(scene, device, surfaceParent!!)

        exoPlayer =
            createExoPlayer(
                activity = activity,
                surfaceEntity = surfaceEntity!!,
                videoUri = videoPath,
                stereoMode = videoAttr.stereoMode,
                canvasShape = videoAttr.canvasShape,
                canvasMovable = videoAttr.movable,
                protected = videoAttr.isProtected,
            )

        videoSelected = videoEnum
        updateButtonsEnabled(videoSelected, attachedInteractable != null)
    }

    private fun onSwitchInteractableClicked(view: View) {
        if (attachedInteractable == null) {
            // Create new input handler
            videoInputManager.handler =
                videoAttrSelected?.createInputHandler(surfaceParent!!, exoPlayer!!)

            attachedInteractable =
                InteractableComponent.create(session, mainExecutor) {
                    // Update logs, will also assign new input event to trigger
                    // refreshing the panel content
                    pointerLogManager.update(it)
                    textViewPointerLogs.text = pointerLogManager.getLog()

                    // Update video input handler to perform interactions
                    videoInputManager.update(it)
                }

            surfaceEntity!!.addComponent(attachedInteractable!!)
            switchInteractableAttached.isChecked = true
        } else {
            surfaceEntity!!.removeComponent(attachedInteractable!!)
            attachedInteractable = null
            // Clear video input handler
            videoInputManager.handler = null
            // Clear logs
            pointerLogManager.reset()
            switchInteractableAttached.isChecked = false
        }

        updateButtonsEnabled(videoSelected, attachedInteractable != null)
    }

    private fun deselectVideo() {
        if (attachedInteractable != null) {
            surfaceEntity?.removeComponent(attachedInteractable!!)
            attachedInteractable = null
            videoInputManager.handler = null
            pointerLogManager.reset()
            textViewPointerLogs.text = ""
        }

        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
        surfaceEntity?.dispose()
        surfaceEntity = null
        videoSelected = null

        updateButtonsEnabled(null, false)
    }

    enum class VideoEnums {
        BIG_BUCK_BUNNY_BUTTON,
        GALAXY_360_MVHEVC_BUTTON,
        NAVER_180_MVHEVC_BUTTON,
    }

    data class VideoAttributes(
        val buttonText: String,
        val videoPath: String,
        val stereoMode: SurfaceEntity.StereoMode,
        val protection: SurfaceEntity.SurfaceProtection,
        val movable: Boolean,
        val stickToHead: Boolean,
        val shapeOffset: Pose,
        val canvasShape: SurfaceEntity.Shape,
        val inputHandlerProvider:
            VideoAttributes.(GroupEntity, ExoPlayer) -> VideoInputManager.InputHandler,
    ) {
        val isProtected
            get() = protection == SurfaceEntity.SurfaceProtection.PROTECTED

        fun createInputHandler(parent: GroupEntity, player: ExoPlayer) =
            inputHandlerProvider(parent, player)
    }

    companion object {
        private fun alignPoseToPlayerHead(scene: Scene, device: ArDevice, entity: GroupEntity) {
            val pose =
                scene.perceptionSpace.transformPoseTo(
                    device.state.value.devicePose,
                    scene.activitySpace,
                )
            val rotation = Quaternion.fromEulerAngles(0.0f, pose.rotation.eulerAngles.y, 0.0f)
            entity.setPose(Pose(pose.translation, rotation))
        }

        private fun createSurfaceEntity(
            session: Session,
            parent: GroupEntity,
            initPose: Pose,
            shape: SurfaceEntity.Shape,
            stereoMode: SurfaceEntity.StereoMode,
            surfaceProtection: SurfaceEntity.SurfaceProtection,
            movable: Boolean,
        ): SurfaceEntity {
            // Create SurfaceEntity
            val surfaceEntity =
                SurfaceEntity.create(
                    session = session,
                    pose = initPose,
                    shape = shape,
                    stereoMode = stereoMode,
                    surfaceProtection = surfaceProtection,
                )
            surfaceEntity.parent = parent

            if (movable) {
                surfaceEntity.addComponent(MovableComponent.createSystemMovable(session))
            } else {
                for (comp in surfaceEntity.getComponentsOfType(MovableComponent::class.java)) {
                    surfaceEntity.removeComponent(comp)
                }
            }

            return surfaceEntity
        }

        private fun createExoPlayer(
            activity: ComponentActivity,
            surfaceEntity: SurfaceEntity,
            videoUri: String,
            stereoMode: SurfaceEntity.StereoMode,
            canvasShape: SurfaceEntity.Shape,
            canvasMovable: Boolean,
            protected: Boolean,
            pixelAspectRatio: Float = 1.0f,
        ): ExoPlayer {
            val drmLicenseUrl = "https://proxy.uat.widevine.com/proxy?provider=widevine_test"

            // Get or initialize the ExoPlayer.
            val player = ExoPlayer.Builder(activity).build()

            // Set the video surface.
            player.setVideoSurface(surfaceEntity.getSurface())
            // Clear previous media items.
            player.clearMediaItems()

            val mediaItem =
                if (protected) {
                    MediaItem.Builder()
                        .setUri(videoUri)
                        .setDrmConfiguration(
                            MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
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

                        // Resize the canvas to match the video aspect ratio - accounting for the
                        // stereo mode.
                        val dimensions =
                            getCanvasAspectRatio(stereoMode, width, height, pixelAspectRatio)
                        // Set the dimensions of the Quad canvas to the video dimensions and attach
                        // the a MovableComponent.
                        if (canvasShape is SurfaceEntity.Shape.Quad) {
                            val size2d = FloatSize2d(dimensions.width, dimensions.height)
                            surfaceEntity.shape = SurfaceEntity.Shape.Quad(size2d)
                        }
                        if (canvasMovable) {
                            for (comp in
                                surfaceEntity.getComponentsOfType(MovableComponent::class.java)) {
                                comp.size = dimensions
                            }
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.i(TAG, "onPlaybackStateChanged: ${playbackState.toPlayStateString()}")
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Player error: $error")
                    }
                }
            )
            player.playWhenReady = true
            player.prepare()

            return player
        }

        fun getCanvasAspectRatio(
            stereoMode: SurfaceEntity.StereoMode,
            videoWidth: Int,
            videoHeight: Int,
            pixelAspectRatio: Float,
        ): FloatSize3d {
            check(videoWidth >= 0 && videoHeight >= 0) { "Video dimensions must be positive." }
            check(pixelAspectRatio > 0f) { "Pixel aspect ratio must be positive." }
            val effectiveDisplayWidth = videoWidth.toFloat() * pixelAspectRatio

            return when (stereoMode) {
                SurfaceEntity.StereoMode.MONO,
                SurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY,
                SurfaceEntity.StereoMode.MULTIVIEW_RIGHT_PRIMARY ->
                    FloatSize3d(1.0f, videoHeight.toFloat() / effectiveDisplayWidth, 0.0f)

                SurfaceEntity.StereoMode.TOP_BOTTOM ->
                    FloatSize3d(1.0f, 0.5f * videoHeight.toFloat() / effectiveDisplayWidth, 0.0f)

                SurfaceEntity.StereoMode.SIDE_BY_SIDE ->
                    FloatSize3d(1.0f, 2.0f * videoHeight.toFloat() / effectiveDisplayWidth, 0.0f)

                else -> throw IllegalArgumentException("Unsupported stereo mode: $stereoMode")
            }
        }

        var videoAttributesMap =
            mapOf<Int, VideoAttributes>(
                VideoEnums.BIG_BUCK_BUNNY_BUTTON.ordinal to
                    VideoAttributes(
                        buttonText = "Play Quad Surface Video",
                        videoPath = "/Download/vid_bigbuckbunny.mp4",
                        stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                        protection = SurfaceEntity.SurfaceProtection.NONE,
                        movable = true,
                        stickToHead = false,
                        shapeOffset = Pose(Vector3(0.0f, 0.0f, -1.0f), Quaternion.Identity),
                        canvasShape = SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
                        inputHandlerProvider = { parent, player -> ClickVideoInputHandler(player) },
                    ),
                VideoEnums.GALAXY_360_MVHEVC_BUTTON.ordinal to
                    VideoAttributes(
                        buttonText = "Play 360 Surface Video",
                        videoPath = "/Download/Galaxy11_VR_3D360.mp4",
                        stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                        protection = SurfaceEntity.SurfaceProtection.NONE,
                        movable = false,
                        stickToHead = true,
                        shapeOffset = Pose.Identity,
                        canvasShape = SurfaceEntity.Shape.Sphere(15.0f),
                        inputHandlerProvider = { parent, player ->
                            SphericalVideoInputHandler(parent, 15.0f, player)
                        },
                    ),
                VideoEnums.NAVER_180_MVHEVC_BUTTON.ordinal to
                    VideoAttributes(
                        buttonText = "Play 180 Surface Video",
                        videoPath = "/Download/Galaxy11_VR_3D360.mp4",
                        stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM,
                        protection = SurfaceEntity.SurfaceProtection.NONE,
                        movable = false,
                        stickToHead = true,
                        shapeOffset = Pose.Identity,
                        canvasShape = SurfaceEntity.Shape.Hemisphere(15.0f),
                        inputHandlerProvider = { parent, player ->
                            SphericalVideoInputHandler(parent, 15.0f, player)
                        },
                    ),
            )

        fun Int.toPlayStateString(): String =
            when (this) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "UNKNOWN_PLAY_STATE"
            }
    }
}
