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

package androidx.xr.scenecore.testapp.surfaceimage

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.arcore.ArDevice
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.ExperimentalSurfaceEntityPixelDimensionsApi
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.scene
import java.io.File
import java.nio.file.Paths
import kotlinx.coroutines.launch

private const val TAG = "JXR-SurfaceEntity-SurfaceEntityImageActivity"
private const val MAX_CORNER_RADIUS = 0.5f

object VideoButtonColors {
    val StandardPlayback = Color(0xFF42A5F5) // Blue 400
    val Multiview = Color(0xFF26A69A) // Teal 400
    val VR = Color(0xFF7E57C2) // Deep Purple 400
    val DRM = Color(0xFF78909C) // Blue Grey 400
    val HDR = Color(0xFF66BB6A) // Green 400
    val Transformations = Color(0xFF757575) // Grey 600
    val DefaultButton = Color(0xFF42A5F5) // Blue 400
}

class SurfaceEntityImageActivity : ComponentActivity() {
    private val activity = this

    private val REQUEST_READ_MEDIA: Int = 1
    private var hasPermission: Boolean = false

    // TODO: b/393150833 - Refactor these vars into a common UI struct to reduce nullability.
    private var surfaceEntity: SurfaceEntity? = null
    private var movableComponent: MovableComponent? = null // movable component for surfaceEntity
    private var movableComponentMP: MovableComponent? = null // movable component for mainPanel
    private var imageShowing by mutableStateOf<Boolean>(false)
    private var controlPanelEntity: PanelEntity? = null
    private var alphaMaskTexture: Texture? = null
    private var movieParent: Entity? = null

    // This is a custom move listener which moves the movieParent instead of the surfaceEntity
    // directly. This allows for the SurfaceEntity to be independently rotated without impacting
    // the player controls which are attached to the movieParent.
    private var moveListener =
        object : EntityMoveListener {
            override fun onMoveUpdate(
                entity: Entity,
                currentInputRay: Ray,
                currentPose: Pose,
                currentScale: Float,
            ) {
                check(entity == surfaceEntity) {
                    "Listener should only be attached to surfaceEntity."
                }
                var curParentPose = movieParent!!.getPose()
                // Apply the currentPose to the movieParent to move the surfaceEntity.
                movieParent?.setPose(curParentPose.compose(currentPose))
            }
        }

    enum class ColorCorrectionMode {
        BEST_EFFORT,
        USER_MANAGED,
    }

    var colorCorrectionMode by mutableStateOf(ColorCorrectionMode.BEST_EFFORT)
        private set

    fun toggleColorCorrectionMode() {
        colorCorrectionMode =
            if (colorCorrectionMode == ColorCorrectionMode.BEST_EFFORT) {
                ColorCorrectionMode.USER_MANAGED
            } else {
                ColorCorrectionMode.BEST_EFFORT
            }
        Log.d(TAG, "ColorCorrectionMode toggled to: $colorCorrectionMode")
    }

    enum class SuperSamplingMode {
        NONE,
        DEFAULT,
    }

    enum class ShapeMode {
        QUAD,
        HEMISPHERE,
        SPHERE,
    }

    var superSamplingMode by mutableStateOf(SuperSamplingMode.DEFAULT)
        private set

    fun toggleSuperSamplingMode() {
        superSamplingMode =
            if (superSamplingMode == SuperSamplingMode.NONE) {
                SuperSamplingMode.DEFAULT
            } else {
                SuperSamplingMode.NONE
            }
        Log.d(TAG, "SuperSamplingMode toggled to: $superSamplingMode")
    }

    private var currentImageSize: IntSize2d? = null

    // When an image is captured using a rotated phone, the encoded image
    // bitstream may have a different orientation than the device's display.
    // To correct the orientation, we need to rotate the image content by
    // the same amount as the device's display.
    private var currentVideoRotationDegrees: Int = 0
    private var currentPixelAspectRatio: Float = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = (Session.create(this) as SessionCreateSuccess).session
        session.configure(Config(deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN))
        val arDevice = ArDevice.getInstance(session)
        session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
        session.scene.keyEntity = session.scene.mainPanelEntity

        checkExternalStoragePermission()

        // Set up the MoveableComponent so the user can move the Main Panel out of the way of
        // video canvases which appear behind it.
        if (movableComponentMP == null) {
            movableComponentMP = MovableComponent.createSystemMovable(session)
            session.scene.mainPanelEntity.addComponent(movableComponentMP!!)
        }

        // This will be re-used throughout the life of the Activity.
        movieParent = Entity.create(session, "movieParent")

        lifecycleScope.launch {
            alphaMaskTexture = Texture.create(session, Paths.get("textures", "alpha_mask.png"))
        }
        setContent { HelloWorld(session, arDevice, activity) }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (alphaMaskTexture != null) {
            // TODO: b/450175596 - Restore alpha mask closure.
            alphaMaskTexture = null
        }
    }

    /** A simple composable that toggles the passthrough on and off to test environment changes. */
    // TODO: b/324947709 - Refactor common @Composable code into a utility library for common usage
    // across sample apps.
    @Composable
    fun HelloWorld(session: Session, arDevice: ArDevice, activity: SurfaceEntityImageActivity) {
        // Add a panel to the main activity with a button to toggle passthrough
        LaunchedEffect(Unit) {
            activity.setContentView(
                createButtonViewUsingCompose(
                    activity = activity,
                    session = session,
                    arDevice = arDevice,
                )
            )
        }
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
            checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) !=
                PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) !=
                    PackageManager.PERMISSION_GRANTED
        ) {
            hasPermission = false
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                ),
                REQUEST_READ_MEDIA,
            )
        } else {
            hasPermission = true
        }
    }

    private fun setupControlPanel(session: Session, arDevice: ArDevice) {
        // Dispose previous control panel if it exists
        controlPanelEntity?.dispose()
        controlPanelEntity = null

        // Technically this leaks, but it's a sample / test app.
        val panelContentView =
            ComposeView(this).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { VideoPlayerControls(session, arDevice) }
            }

        controlPanelEntity =
            PanelEntity.create(
                session,
                panelContentView,
                // These are about the right size to fit the buttons without a lot of invisible
                // panel edges
                IntSize2d(640, 480),
                "playerControls",
                Pose.Identity,
            )

        // TODO: b/413478924 - Use controlPanelEntity.view when the api is available.
        val parentView: View =
            if (panelContentView.parent != null && panelContentView.parent is View)
                panelContentView.parent as View
            else panelContentView

        parentView.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        parentView.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        parentView.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
    }

    fun destroySurfaceEntity() {
        imageShowing = false

        surfaceEntity?.dispose()
        surfaceEntity = null

        controlPanelEntity?.dispose()
        controlPanelEntity = null

        currentImageSize = null
        currentVideoRotationDegrees = 0
    }

    fun getCanvasAspectRatio(
        stereoMode: SurfaceEntity.StereoMode,
        videoWidth: Int,
        videoHeight: Int,
        pixelAspectRatio: Float,
    ): FloatSize3d {
        check(videoWidth > 0 && videoHeight > 0) { "Video dimensions must be positive." }
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

    private fun createButtonViewUsingCompose(
        activity: SurfaceEntityImageActivity,
        session: Session,
        arDevice: ArDevice,
    ): View {
        val view =
            ComposeView(activity.applicationContext).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { SurfaceEntityImageActivityUI(session, arDevice, activity) }
            }
        view.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        view.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        view.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
        return view
    }

    @Composable
    fun VideoPlayerControls(session: Session, arDevice: ArDevice) {
        var featherRadiusX by remember { mutableFloatStateOf(0.0f) }
        var featherRadiusY by remember { mutableFloatStateOf(0.0f) }
        var isQuadShape by remember {
            mutableStateOf(surfaceEntity?.shape is SurfaceEntity.Shape.Quad)
        }
        var cornerRadius by remember { mutableFloatStateOf(0.0f) }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { destroySurfaceEntity() }) {
                    Text(text = "Dismiss Image", fontSize = 10.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Feather Radius", fontSize = 10.sp, color = Color.White)
                Column {
                    Slider(
                        value = featherRadiusX,
                        onValueChange = {
                            featherRadiusX = it
                            surfaceEntity!!.edgeFeatheringParams =
                                SurfaceEntity.EdgeFeatheringParams.RectangleFeather(
                                    featherRadiusX,
                                    featherRadiusY,
                                )
                        },
                        valueRange = 0.0f..0.5f,
                    )
                    Slider(
                        value = featherRadiusY,
                        onValueChange = {
                            featherRadiusY = it
                            surfaceEntity!!.edgeFeatheringParams =
                                SurfaceEntity.EdgeFeatheringParams.RectangleFeather(
                                    featherRadiusX,
                                    featherRadiusY,
                                )
                        },
                        valueRange = 0.0f..0.5f,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Corner Radius", fontSize = 10.sp, color = Color.White)
                Slider(
                    value = cornerRadius,
                    onValueChange = {
                        cornerRadius = it
                        val currentShape = surfaceEntity!!.shape
                        if (currentShape is SurfaceEntity.Shape.Quad) {
                            surfaceEntity!!.shape =
                                SurfaceEntity.Shape.Quad(currentShape.extents, cornerRadius)
                        }
                    },
                    valueRange = 0.0f..MAX_CORNER_RADIUS,
                    enabled = isQuadShape,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        surfaceEntity!!.shape =
                            SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f), cornerRadius)
                        isQuadShape = true
                        // Move the Quad-shaped canvas to a spot in front of the User.
                        surfaceEntity!!.setPose(
                            session.scene.perceptionSpace
                                .getScenePoseFromPerceptionPose(arDevice.state.value.devicePose)
                                .transformPoseTo(
                                    Pose(
                                        Vector3(0.0f, 0.0f, -1.5f),
                                        Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                                    ),
                                    session.scene.activitySpace,
                                )
                        )
                    }
                ) {
                    Text(text = "Set Quad", fontSize = 10.sp)
                }
                Button(
                    onClick = {
                        surfaceEntity!!.shape = SurfaceEntity.Shape.Sphere(1.0f)
                        isQuadShape = false
                    }
                ) {
                    Text(text = "Set Vr360", fontSize = 10.sp)
                }
                Button(
                    onClick = {
                        surfaceEntity!!.shape = SurfaceEntity.Shape.Hemisphere(1.0f)
                        isQuadShape = false
                    }
                ) {
                    Text(text = "Set Vr180", fontSize = 10.sp)
                }
            } // end row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { surfaceEntity!!.stereoMode = SurfaceEntity.StereoMode.MONO }) {
                    Text(text = "Mono", fontSize = 10.sp)
                }
                Button(
                    onClick = { surfaceEntity!!.stereoMode = SurfaceEntity.StereoMode.TOP_BOTTOM }
                ) {
                    Text(text = "Top-Bottom", fontSize = 10.sp)
                }
                Button(
                    onClick = { surfaceEntity!!.stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE }
                ) {
                    Text(text = "Side-by-Side", fontSize = 10.sp)
                }
            } // end row
        } // end column
    }

    private fun updateSurfaceEntityVisuals() {
        val newShapeDimensions =
            getCanvasAspectRatio(
                surfaceEntity!!.stereoMode,
                currentImageSize!!.width,
                currentImageSize!!.height,
                currentPixelAspectRatio,
            )
        if (surfaceEntity!!.shape is SurfaceEntity.Shape.Quad) {
            surfaceEntity!!.shape =
                SurfaceEntity.Shape.Quad(
                    FloatSize2d(newShapeDimensions.width, newShapeDimensions.height)
                )
            movableComponent?.size = surfaceEntity!!.dimensions
        }

        var controlOffsetY: Float = 0.0f
        var controlOffsetZ: Float = 0.0f

        var rotation =
            Quaternion.fromAxisAngle(Vector3.Forward, currentVideoRotationDegrees.toFloat())
        var newPose = surfaceEntity!!.getPose().compose(Pose(Vector3.Zero, rotation))
        surfaceEntity!!.setPose(newPose)
        controlPanelEntity!!.parent = movieParent!!

        // Position the control panel below and slightly in front of the video panel.
        if (surfaceEntity!!.shape is SurfaceEntity.Shape.Quad) {
            controlOffsetY = -(newShapeDimensions.height / 2f) - 0.15f
            controlOffsetZ = 0.15f
        } else if (surfaceEntity!!.shape is SurfaceEntity.Shape.Hemisphere) {
            // A rough approximation of a 30/60/90 triangle with radius 1.0f
            controlOffsetY = -0.50f
            controlOffsetZ = -0.85f
        } else if (surfaceEntity!!.shape is SurfaceEntity.Shape.Sphere) {
            // A rough approximation of a 30/60/90 triangle with radius 1.0f
            controlOffsetY = -0.50f
            controlOffsetZ = -0.85f
        }
        controlPanelEntity!!.setPose(
            Pose(Vector3(0f, controlOffsetY, controlOffsetZ), Quaternion.Identity)
        )
    }

    // Note that pose here will be ignored if the canvasShape is not a Quad
    // TODO: Update this to take a Pose for the controlPanel
    @Suppress("UnsafeOptInUsageError")
    @Composable
    @OptIn(ExperimentalSurfaceEntityPixelDimensionsApi::class)
    fun ShowBitmapButton(
        session: Session,
        arDevice: ArDevice,
        activity: Activity,
        bitmapUri: String,
        stereoMode: SurfaceEntity.StereoMode,
        pose: Pose,
        shapeMode: ShapeMode,
        buttonText: String,
        buttonColor: Color = VideoButtonColors.DefaultButton,
        enabled: Boolean = true,
        loop: Boolean = false,
        protected: Boolean = false,
    ) {
        val file = File(bitmapUri)
        if (!file.exists()) {
            Toast.makeText(
                    activity,
                    "File (bitmap) ($bitmapUri) does not exist. Did you download all the assets?",
                    Toast.LENGTH_LONG,
                )
                .show()
            return
        }

        Button(
            enabled = enabled,
            onClick = {
                val bitmap: Bitmap = BitmapFactory.decodeFile(bitmapUri)!!
                currentImageSize = IntSize2d(bitmap.width, bitmap.height)
                val shapeDimensions =
                    getCanvasAspectRatio(stereoMode, bitmap.width, bitmap.height, 1.0f)

                var actualPose = pose

                val canvasShape: SurfaceEntity.Shape =
                    when (shapeMode) {
                        ShapeMode.QUAD ->
                            SurfaceEntity.Shape.Quad(
                                FloatSize2d(shapeDimensions.width, shapeDimensions.height)
                            )

                        ShapeMode.HEMISPHERE -> SurfaceEntity.Shape.Hemisphere(1.0f)
                        ShapeMode.SPHERE -> SurfaceEntity.Shape.Sphere(1.0f)
                    }

                if (!(canvasShape is SurfaceEntity.Shape.Quad)) {
                    actualPose =
                        session.scene.perceptionSpace.transformPoseTo(
                            arDevice.state.value.devicePose,
                            session.scene.activitySpace,
                        )
                }

                // Create SurfaceEntity and MovableComponent if they don't exist.
                if (surfaceEntity == null) {
                    val surfaceContentLevel =
                        if (protected) {
                            SurfaceEntity.SurfaceProtection.PROTECTED
                        } else {
                            SurfaceEntity.SurfaceProtection.NONE
                        }

                    val superSamplingMode =
                        if (
                            this@SurfaceEntityImageActivity.superSamplingMode ==
                                SuperSamplingMode.DEFAULT
                        ) {
                            SurfaceEntity.SuperSampling.PENTAGON
                        } else {
                            SurfaceEntity.SuperSampling.NONE
                        }

                    movieParent!!.parent = session.scene.activitySpace
                    movieParent!!.setPose(actualPose)

                    surfaceEntity =
                        SurfaceEntity.create(
                            session = session,
                            pose = Pose.Identity,
                            shape = canvasShape,
                            stereoMode = stereoMode,
                            mediaBlendingMode = SurfaceEntity.MediaBlendingMode.TRANSPARENT,
                            superSampling = superSamplingMode,
                            surfaceProtection = surfaceContentLevel,
                        )

                    surfaceEntity?.parent = movieParent!!
                    surfaceEntity?.setPose(Pose.Identity)

                    // Make the video player movable (to make it easier to look at it from different
                    // angles and distances) (only on quad canvas)
                    movableComponent =
                        MovableComponent.createCustomMovable(
                            session,
                            scaleInZ = false,
                            executor = Runnable::run,
                            entityMoveListener = moveListener,
                        )

                    // The quad has a radius of 1.0 meters
                    movableComponent!!.size = FloatSize3d(1.0f, 1.0f, 1.0f)

                    if (canvasShape is SurfaceEntity.Shape.Quad) {
                        surfaceEntity!!.addComponent(movableComponent!!)
                    }

                    imageShowing = true
                }

                // Blit the image into the SurfaceEntity
                surfaceEntity!!.setSurfacePixelDimensions(IntSize2d(bitmap.width, bitmap.height))
                val canvas = surfaceEntity!!.getSurface().lockHardwareCanvas()
                canvas.drawBitmap(bitmap, 0.0f, 0.0f, null)
                surfaceEntity!!.getSurface().unlockCanvasAndPost(canvas)
                setupControlPanel(session, arDevice)
                updateSurfaceEntityVisuals()
            },
            modifier = Modifier.fillMaxWidth().height(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = buttonText,
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }

    @Composable
    fun StereoBitmapQuadButton(
        session: Session,
        arDevice: ArDevice,
        activity: Activity,
        enabled: Boolean = true,
    ) {
        ShowBitmapButton(
            session = session,
            arDevice = arDevice,
            activity = activity,
            // For Testers: Note that this translates to
            // "/sdcard/Download/2d_Flats_sbs_downsampled.jpg".
            bitmapUri =
                Environment.getExternalStorageDirectory().path +
                    "/Download/2d_Flats_sbs_downsampled.jpg",
            shapeMode = ShapeMode.QUAD,
            stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE,
            pose = Pose(Vector3(0.0f, 0.0f, -1.5f), Quaternion(0.0f, 0.0f, 0.0f, 1.0f)),
            buttonText = "[Stereo JPG] Show stereoscopic static Quad",
            buttonColor = VideoButtonColors.StandardPlayback,
            enabled = enabled,
            protected = false,
        )
    }

    @Composable
    fun StereoBitmap180Button(
        session: Session,
        arDevice: ArDevice,
        activity: Activity,
        enabled: Boolean = true,
    ) {
        ShowBitmapButton(
            session = session,
            arDevice = arDevice,
            activity = activity,
            // For Testers: Note that this translates to
            // "/sdcard/Download/VR180_CANON_downsampled.png".
            bitmapUri =
                Environment.getExternalStorageDirectory().path +
                    "/Download/VR180_CANON_downsampled.png",
            shapeMode = ShapeMode.HEMISPHERE,
            stereoMode = SurfaceEntity.StereoMode.SIDE_BY_SIDE,
            pose = Pose.Identity,
            buttonText = "[Stereo PNG] Show sereoscopic static 180",
            buttonColor = VideoButtonColors.VR,
            enabled = enabled,
            protected = false,
        )
    }

    @Composable
    fun MonoBitmap360Button(
        session: Session,
        arDevice: ArDevice,
        activity: Activity,
        enabled: Boolean = true,
    ) {
        ShowBitmapButton(
            session = session,
            arDevice = arDevice,
            activity = activity,
            // For Testers: Note that this translates to
            // "/sdcard/Download/360Flats_downsampled.jpg".
            bitmapUri =
                Environment.getExternalStorageDirectory().path +
                    "/Download/360Flats_downsampled.jpg",
            shapeMode = ShapeMode.SPHERE,
            stereoMode = SurfaceEntity.StereoMode.MONO,
            pose = Pose.Identity,
            buttonText = "[Stereo JPG] Show Mono static 360",
            buttonColor = VideoButtonColors.StandardPlayback,
            enabled = enabled,
            protected = false,
        )
    }

    @Composable
    fun SurfaceEntityImageActivityUI(
        session: Session,
        arDevice: ArDevice,
        activity: SurfaceEntityImageActivity,
    ) {
        remember { mutableStateOf(false) }
        val alphaMaskEnabled = remember { mutableStateOf(false) }

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f).padding(8.dp),
            ) {
                Text(text = "System APIs", fontSize = 30.sp)
                Button(onClick = { togglePassthrough(session) }) {
                    Text(text = "Toggle Passthrough", fontSize = 18.sp)
                }
                Button(onClick = { ActivityCompat.recreate(activity) }) {
                    Text(text = "Recreate Activity", fontSize = 18.sp)
                }
                Button(onClick = { activity.toggleColorCorrectionMode() }) {
                    val buttonTextToDisplay =
                        if (activity.colorCorrectionMode == ColorCorrectionMode.BEST_EFFORT) {
                            "CC: Best Effort (Tap to User Managed)"
                        } else {
                            "CC: User Managed (Tap to Best Effort)"
                        }
                    Text(text = buttonTextToDisplay, fontSize = 18.sp)
                }
                Button(onClick = { activity.toggleSuperSamplingMode() }) {
                    val buttonTextToDisplay =
                        if (activity.superSamplingMode == SuperSamplingMode.DEFAULT) {
                            "SuperSampling: Enabled (Tap to disable)"
                        } else {
                            "SuperSampling: Disabled (Tap to enable)"
                        }
                    Text(text = buttonTextToDisplay, fontSize = 18.sp)
                }
            }
            Column(
                modifier = Modifier.weight(1f).padding(4.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "SurfaceEntity", fontSize = 30.sp)
                if (imageShowing == false) {
                    // High level testcases
                    StereoBitmapQuadButton(session, arDevice, activity)
                    StereoBitmap180Button(session, arDevice, activity)
                    MonoBitmap360Button(session, arDevice, activity)
                } else {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f).padding(8.dp),
                    ) {
                        VideoPlayerControls(session, arDevice)
                        Button(
                            onClick = {
                                alphaMaskEnabled.value = !alphaMaskEnabled.value
                                if (alphaMaskEnabled.value) {
                                    surfaceEntity!!.primaryAlphaMaskTexture = alphaMaskTexture
                                } else {
                                    // Clear the alpha mask texture.
                                    surfaceEntity!!.primaryAlphaMaskTexture = null
                                }
                            }
                        ) {
                            Text(text = "Toggle Alpha Mask", fontSize = 18.sp)
                        }
                        val isTransparent = remember { mutableStateOf(true) }
                        Button(
                            onClick = {
                                @Suppress("RestrictedApi")
                                if (isTransparent.value) {
                                    surfaceEntity!!.mediaBlendingMode =
                                        SurfaceEntity.MediaBlendingMode.OPAQUE
                                    isTransparent.value = false
                                } else {
                                    surfaceEntity!!.mediaBlendingMode =
                                        SurfaceEntity.MediaBlendingMode.TRANSPARENT
                                    isTransparent.value = true
                                }
                            }
                        ) {
                            Text(
                                text =
                                    if (isTransparent.value) "Make Opaque" else "Make Transparent",
                                fontSize = 18.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
