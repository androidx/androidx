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

package androidx.xr.scenecore.samples.environmenttest

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.ExrImage
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.SpatialCapabilities
import androidx.xr.scenecore.SpatialEnvironment
import androidx.xr.scenecore.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.scene
import java.nio.file.Paths
import kotlin.math.roundToInt

class EnvironmentTestActivity : ComponentActivity() {

    private val TAG = "EnvironmentTestActivity"

    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }
    private var spatialEnvironmentPreference: SpatialEnvironment.SpatialEnvironmentPreference? =
        null
    private var lastApiCall: String by mutableStateOf("Nothing")
    private var onSpatialEnvironmentChangedCount: Int by mutableIntStateOf(0)
    private var onPassthroughOpacityChangedCount: Int by mutableIntStateOf(0)

    // The current opacity of the passthrough as returned by the system. NOT the preference.
    private var currentPassthroughOpacity = mutableFloatStateOf(0.0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mActivity = this

        session.scene.mainPanelEntity.sizeInPixels = IntSize2d(width = 1500, height = 2000)

        session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
        session.scene.spatialEnvironment.addOnPassthroughOpacityChangedListener { newOpacity ->
            currentPassthroughOpacity.floatValue = newOpacity
        }
        currentPassthroughOpacity.floatValue =
            session.scene.spatialEnvironment.currentPassthroughOpacity

        session.scene.spatialEnvironment.addOnSpatialEnvironmentChangedListener {
            onSpatialEnvironmentChangedCount++
            Log.i(TAG, "onSpatialEnvironmentChangedCount: $onSpatialEnvironmentChangedCount")
        }
        session.scene.spatialEnvironment.addOnPassthroughOpacityChangedListener {
            onPassthroughOpacityChangedCount++
            Log.i(
                TAG,
                "PassthroughOpacity changed to: $it, count: $onPassthroughOpacityChangedCount",
            )
        }

        setPassthroughOpacity(0.0f)

        setContent { HelloWorld(session, mActivity) }
    }

    private fun setPassthroughOpacity(opacity: Float) {
        session.scene.spatialEnvironment.preferredPassthroughOpacity = opacity
        lastApiCall =
            "set opacity preference to ${session.scene.spatialEnvironment.preferredPassthroughOpacity}, but current actual opacity is ${session.scene.spatialEnvironment.currentPassthroughOpacity}"
        Log.i(TAG, lastApiCall)
    }

    /**
     * Sets the skybox and geometry for the environment.
     *
     * If either is null, it will be removed from the environment, but a preference will still be
     * set so the default system environment will not be used.
     */
    private fun setGeoAndSkybox(skybox: ExrImage?, geometry: GltfModel?) {
        spatialEnvironmentPreference = SpatialEnvironmentPreference(skybox, geometry)
        session.scene.spatialEnvironment.preferredSpatialEnvironment = spatialEnvironmentPreference
        lastApiCall =
            "set spatial environment preference to ${session.scene.spatialEnvironment.preferredSpatialEnvironment?.info()}, but current actual mode shown is ${session.scene.spatialEnvironment.isPreferredSpatialEnvironmentActive}"

        Log.i(TAG, lastApiCall)
    }

    /** A simple composable that toggles the passthrough on and off to test environment changes. */
    // TODO: b/324947709 - Refactor common @Composable code into a utility library for common usage
    // across sample apps.
    @Composable
    fun HelloWorld(session: Session, activity: Activity) {

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
                setContent { TestPanelContent(session) }
            }
        view.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        view.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        view.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
        return view
    }

    @Composable
    fun TestPanelContent(session: Session) {

        Column(verticalArrangement = Arrangement.Top) {
            OpacityControls(session)

            Row {
                Button(
                    onClick = {
                        lastApiCall = "requestFullSpaceMode"
                        session.scene.requestFullSpaceMode()
                    }
                ) {
                    Text(text = "Request FSM", fontSize = 30.sp)
                }
                Button(
                    onClick = {
                        lastApiCall = "requestHomeSpaceMode"
                        session.scene.requestHomeSpaceMode()
                    }
                ) {
                    Text(text = "Request HSM", fontSize = 30.sp)
                }
            }

            SkyboxAndGeoControls(session)

            Text(
                text =
                    "Is Spatial Environment Preference Active? ${session.scene.spatialEnvironment.isPreferredSpatialEnvironmentActive}",
                fontSize = 30.sp,
            )
            Text(text = "Last API Call: ${lastApiCall}", fontSize = 20.sp)
            Text(
                text = "onPassthroughOpacityChangedCount: ${onPassthroughOpacityChangedCount}",
                fontSize = 20.sp,
            )
            Text(
                text = "onSpatialEnvironmentChangedCount: ${onSpatialEnvironmentChangedCount}",
                fontSize = 20.sp,
            )
            Text(
                text = "SpatialCapabilities: ${session.scene.spatialCapabilities.getAsStrings()}",
                fontSize = 20.sp,
            )
        }
    }

    @Composable
    fun OpacityControls(session: Session) {
        var sliderPosition by remember {
            mutableFloatStateOf(currentPassthroughOpacity.floatValue * 100.0f)
        }
        val opacityPreferenceStr =
            session.scene.spatialEnvironment.preferredPassthroughOpacity ?: "null"

        Text(text = "Passthrough Opacity Preference: $opacityPreferenceStr", fontSize = 30.sp)
        Row {
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
                valueRange = 0f..100f,
                steps = 19,
                onValueChangeFinished = {
                    setPassthroughOpacity(sliderPosition.roundToInt() / 100.0f)
                },
                modifier = Modifier.fillMaxWidth(0.5f),
            )
            Button(
                onClick = {
                    setPassthroughOpacity(SpatialEnvironment.NO_PASSTHROUGH_OPACITY_PREFERENCE)
                }
            ) {
                Text(text = "Unset passthrough preference", fontSize = 30.sp)
            }
        }
        Text(
            text = "Current Actual Passthrough Opacity: ${currentPassthroughOpacity.floatValue}",
            fontSize = 30.sp,
        )
    }

    @Composable
    @Suppress("DEPRECATION")
    fun SkyboxAndGeoControls(session: Session) {
        var groundGeo by remember { mutableStateOf<GltfModel?>(null) }
        var rocksGeo by remember { mutableStateOf<GltfModel?>(null) }
        var greySkybox by remember { mutableStateOf<ExrImage?>(null) }
        var blueSkybox by remember { mutableStateOf<ExrImage?>(null) }

        LaunchedEffect(Unit) {
            groundGeo = GltfModel.create(session, Paths.get("models", "GroundGeometry.glb"))
        }
        LaunchedEffect(Unit) {
            rocksGeo = GltfModel.create(session, Paths.get("models", "RocksGeometry.glb"))
        }
        LaunchedEffect(Unit) {
            greySkybox = ExrImage.createFromZip(session, Paths.get("skyboxes", "GreySkybox.zip"))
        }
        LaunchedEffect(Unit) {
            blueSkybox = ExrImage.createFromZip(session, Paths.get("skyboxes", "BlueSkybox.zip"))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { setGeoAndSkybox(greySkybox, spatialEnvironmentPreference?.geometry) }
            ) {
                Text(text = "Set Skybox Grey", fontSize = 30.sp)
            }
            Button(
                onClick = { setGeoAndSkybox(blueSkybox, spatialEnvironmentPreference?.geometry) }
            ) {
                Text(text = "Set Skybox Blue", fontSize = 30.sp)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { setGeoAndSkybox(spatialEnvironmentPreference?.skybox, rocksGeo) }) {
                Text(text = "Set Geometry Rocks", fontSize = 30.sp)
            }
            Button(onClick = { setGeoAndSkybox(spatialEnvironmentPreference?.skybox, groundGeo) }) {
                Text(text = "Set Geometry Ground", fontSize = 30.sp)
            }
        }
        Button(
            onClick = {
                spatialEnvironmentPreference = SpatialEnvironmentPreference(blueSkybox, groundGeo)
                lastApiCall = "setBothGeometryAndSkybox"
                session.scene.spatialEnvironment.preferredSpatialEnvironment =
                    spatialEnvironmentPreference
            }
        ) {
            Text(text = "Set both Geometry and Skybox (Ground, Blue)", fontSize = 30.sp)
        }
        Button(onClick = { setGeoAndSkybox(null, spatialEnvironmentPreference?.geometry) }) {
            Text(text = "Unset Skybox preference", fontSize = 30.sp)
        }
        Button(onClick = { setGeoAndSkybox(spatialEnvironmentPreference?.skybox, null) }) {
            Text(text = "Unset Geometry preference", fontSize = 30.sp)
        }
        Button(
            onClick = {
                spatialEnvironmentPreference = null
                lastApiCall = "revertToSystemDefaultGeometryAndSkybox"
                session.scene.spatialEnvironment.preferredSpatialEnvironment = null
            }
        ) {
            Text(text = "Unset both Geometry and Skybox preference", fontSize = 30.sp)
        }
    }
}

fun SpatialCapabilities.getAsStrings(): String {
    val msg =
        """
  SPATIAL_CAPABILITY_UI: ${hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_UI)}
  SPATIAL_CAPABILITY_3D_CONTENT: ${hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT)}
  SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL: ${hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL)}
  SPATIAL_CAPABILITY_APP_ENVIRONMENT: ${hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT)}
  SPATIAL_CAPABILITY_SPATIAL_AUDIO: ${hasCapability(SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO)}
  """
            .trimIndent()
    return msg
}

fun SpatialEnvironmentPreference.info(): String {
    return "{skybox: ${this.skybox}, geometry: ${this.geometry}}"
}
