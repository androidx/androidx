/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.scenecore.samples.splitenginetest

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ExrImage
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.KhronosPbrMaterial
import androidx.xr.scenecore.KhronosPbrMaterialSpec
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.Texture
import androidx.xr.scenecore.TextureSampler
import androidx.xr.scenecore.scene
import com.google.common.util.concurrent.ListenableFuture
import java.nio.file.Paths
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplitEngineTestActivity : ComponentActivity() {

    private val activity = this

    private val session by lazy {
        // SplitEngine is enabled by default.
        (Session.create(this) as SessionCreateSuccess).session
    }

    private var spatialEnvironmentPreference: SpatialEnvironmentPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f

        setContent { ComposeEntry(session, activity) }
    }

    private fun togglePassthrough(session: Session) {
        val passthroughOpacity: Float = session.scene.spatialEnvironment.currentPassthroughOpacity
        Log.i("TogglePassthrough", "TogglePassthrough!")
        when (passthroughOpacity) {
            0.0f -> session.scene.spatialEnvironment.preferredPassthroughOpacity = 1.0f
            1.0f -> session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f
        }
    }

    private fun setSkyboxAndGeometry(skybox: ExrImage?, geometry: GltfModel?) {
        spatialEnvironmentPreference = SpatialEnvironmentPreference(skybox, geometry)
        session.scene.spatialEnvironment.preferredSpatialEnvironment = spatialEnvironmentPreference
    }

    // TODO: b/324947709 - Refactor common @Composable code into a utility library for common usage
    // across sample apps.
    /** A boilerplate entrypoint to wire up ComposeUI into the main Panel. */
    @Composable
    fun ComposeEntry(session: Session, activity: Activity) {
        val composeView =
            ComposeView(activity.applicationContext).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { SplitEngineTestActivityUI(session) }
            }
        composeView.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        composeView.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)

        // Add a panel to the main activity with a button to toggle passthrough
        LaunchedEffect(Unit) { activity.setContentView(composeView) }
    }

    /*
     * Panel UI which exercises split engine pathways integrated with .
     */
    @Composable
    fun SplitEngineTestActivityUI(session: Session) {
        val movableComponentMP = remember { mutableStateOf<MovableComponent?>(null) }

        val blueSkybox = remember { mutableStateOf<ExrImage?>(null) }
        val groundGeometry = remember { mutableStateOf<GltfModel?>(null) }
        val dragonModel = remember { mutableStateOf<GltfModel?>(null) }
        val dragonEntity = remember { mutableStateOf<GltfModelEntity?>(null) }
        val dragonAnimationState = remember {
            androidx.compose.runtime.mutableIntStateOf(GltfModelEntity.AnimationState.STOPPED)
        }
        val glimmerModel = remember { mutableStateOf<GltfModel?>(null) }
        val glimmerEntity = remember { mutableStateOf<GltfModelEntity?>(null) }
        var interactableAttached by remember { mutableStateOf(false) }
        val patternTexture = remember { mutableStateOf<Texture?>(null) }
        val khronosPbrMaterial = remember { mutableStateOf<KhronosPbrMaterial?>(null) }

        var khronosPbrMaterialEmissiveR by remember { mutableFloatStateOf(0f) }
        var khronosPbrMaterialEmissiveG by remember { mutableFloatStateOf(0f) }
        var khronosPbrMaterialEmissiveB by remember { mutableFloatStateOf(0f) }

        val updateEmissiveFactor = {
            khronosPbrMaterial.value?.let { material ->
                val factorVec =
                    Vector3(
                        x = khronosPbrMaterialEmissiveR / 100f,
                        y = khronosPbrMaterialEmissiveG / 100f,
                        z = khronosPbrMaterialEmissiveB / 100f,
                    )
                material.setEmissiveFactors(factorVec)
            }
        }
        var khronosPbrMaterialMetallic by remember { mutableFloatStateOf(0.0f) }
        var khronosPbrMaterialRoughness by remember { mutableFloatStateOf(50f) }

        // This will continuously poll the animation state to verify that the state is updated to
        // "STOPPED" after a single run is done. Ideally we would register a listener for this, but
        // the
        // current API doesn't support it.
        LaunchedEffect(Unit) {
            while (true) {
                if (dragonEntity.value != null) {
                    dragonAnimationState.intValue = dragonEntity.value!!.animationState
                }
                delay(16)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f).padding(8.dp),
            ) {
                Text(text = "System APIs", fontSize = 50.sp)
                Button(onClick = { togglePassthrough(session) }) {
                    Text(text = "Toggle Passthrough", fontSize = 20.sp)
                }
                Button(onClick = { session.scene.requestFullSpaceMode() }) {
                    // Set up the MoveableComponent on the first jump into FSM to allow the user to
                    // move the
                    // main panel around.
                    if (movableComponentMP.value == null) {
                        movableComponentMP.value = MovableComponent.createSystemMovable(session)
                        val unused =
                            session.scene.mainPanelEntity.addComponent(movableComponentMP.value!!)
                    }
                    Text(text = "Request FSM", fontSize = 20.sp)
                }
                Button(onClick = { session.scene.requestHomeSpaceMode() }) {
                    Text(text = "Request HSM", fontSize = 20.sp)
                }
                Button(onClick = { ActivityCompat.recreate(activity) }) {
                    Text(text = "Recreate Activity", fontSize = 20.sp)
                }
            }
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f).padding(8.dp).verticalScroll(rememberScrollState()),
            ) {
                Text(text = "Split Engine APIs", fontSize = 50.sp)
                Button(
                    onClick = {
                        lifecycleScope.launch {
                            val skyboxToken: ExrImage =
                                ExrImage.createFromZip(
                                    session,
                                    Paths.get("skyboxes", "BlueSkybox.zip"),
                                )
                            try {
                                blueSkybox.value = skyboxToken
                            } catch (e: Exception) {
                                Log.e(
                                    "SplitEngineTestActivity",
                                    "Failed to load BlueSkybox: " + e.message,
                                )
                            }
                        }
                    }
                ) {
                    Text(text = "Load Skybox Blue", fontSize = 20.sp)
                }
                Button(
                    onClick = {
                        setSkyboxAndGeometry(
                            blueSkybox.value,
                            spatialEnvironmentPreference?.geometry,
                        )
                    }
                ) {
                    Text(text = "Set Skybox Blue", fontSize = 20.sp)
                }
                Button(
                    onClick = { setSkyboxAndGeometry(null, spatialEnvironmentPreference?.geometry) }
                ) {
                    Text(text = "Remove Skybox Blue", fontSize = 20.sp)
                }
                Button(
                    onClick = {
                        lifecycleScope.launch {
                            try {
                                groundGeometry.value =
                                    GltfModel.create(
                                        session,
                                        Paths.get("models", "GroundGeometry.glb"),
                                    )
                            } catch (e: Exception) {
                                Log.e(
                                    "SplitEngineTestActivity",
                                    "Failed to load GroundGeometry: " + e.message,
                                )
                            }
                        }
                    }
                ) {
                    Text(text = "Load Ground Geometry", fontSize = 20.sp)
                }
                Button(
                    onClick = {
                        setSkyboxAndGeometry(
                            spatialEnvironmentPreference?.skybox,
                            groundGeometry.value,
                        )
                    }
                ) {
                    Text(text = "Set Ground Geometry", fontSize = 20.sp)
                }
                Button(
                    onClick = { setSkyboxAndGeometry(spatialEnvironmentPreference?.skybox, null) }
                ) {
                    Text(text = "Remove Ground Geometry", fontSize = 20.sp)
                }
                Button(
                    onClick = {
                        lifecycleScope.launch {
                            try {
                                glimmerModel.value =
                                    GltfModel.create(session, Paths.get("models", "l2a_pulse.glb"))
                            } catch (e: Exception) {
                                Log.e(
                                    "SplitEngineTestActivity",
                                    "Failed to load Glimmer Model: " + e.message,
                                )
                            }
                        }
                    }
                ) {
                    Text(text = "Load Glimmer Model", fontSize = 20.sp)
                }
                Button(
                    onClick = {
                        lifecycleScope.launch {
                            try {
                                dragonModel.value =
                                    GltfModel.create(
                                        session,
                                        Paths.get("models", "Dragon_Evolved.gltf"),
                                    )
                            } catch (e: Exception) {
                                Log.e(
                                    "SplitEngineTestActivity",
                                    "Failed to load Dragon Model: " + e.message,
                                )
                            }
                        }
                    }
                ) {
                    Text(text = "Load Dragon Model Split Engine", fontSize = 20.sp)
                }
                if (dragonModel.value != null) {
                    Button(
                        onClick = {
                            dragonEntity.value =
                                GltfModelEntity.create(
                                    session,
                                    dragonModel.value!!,
                                    Pose(
                                        Vector3(2.0f, 0.0f, -1.0f),
                                        Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                                    ),
                                )
                        }
                    ) {
                        Text(text = "Create Dragon Entity Split Engine", fontSize = 20.sp)
                    }
                }
                if (dragonEntity.value != null) {
                    val interactableComponent =
                        InteractableComponent.create(session, mainExecutor) {
                            if (it.action == InputEvent.Action.ACTION_DOWN) {
                                dragonEntity.value!!.setScale(
                                    dragonEntity.value!!.getScale() * 1.1f
                                )
                            }
                        }
                    Text(text = "Attach Interactable Component", fontSize = 16.sp)
                    val dragonScale = dragonEntity.value!!.getScale()
                    Switch(
                        checked = interactableAttached,
                        onCheckedChange = {
                            interactableAttached = it
                            if (interactableAttached) {
                                val unused =
                                    dragonEntity.value!!.addComponent(interactableComponent)
                            } else {
                                dragonEntity.value!!
                                    .getComponentsOfType(InteractableComponent::class.java)
                                    .forEach { component ->
                                        dragonEntity.value!!.removeComponent(component)
                                    }
                                dragonEntity.value!!.setScale(dragonScale)
                            }
                        },
                    )
                    Button(
                        onClick = {
                            dragonEntity.value!!.dispose()
                            dragonEntity.value = null
                        }
                    ) {
                        Text(text = "Destroy Dragon Entity Split Engine", fontSize = 20.sp)
                    }
                }
            }
            if (dragonEntity.value != null) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f).padding(8.dp),
                ) {
                    Text(text = "Animation APIs", fontSize = 50.sp)
                    Button(
                        onClick = {
                            dragonEntity.value!!.startAnimation(false, "Fast_Flying")
                            dragonAnimationState.intValue = dragonEntity.value!!.animationState
                        }
                    ) {
                        Text(text = "Animate Dragon Entity", fontSize = 20.sp)
                    }
                    Button(
                        onClick = {
                            dragonEntity.value!!.startAnimation(true, "Fast_Flying")
                            dragonAnimationState.intValue = dragonEntity.value!!.animationState
                        }
                    ) {
                        Text(text = "Loop Animate Dragon Entity", fontSize = 20.sp)
                    }
                    Button(
                        onClick = {
                            dragonEntity.value!!.stopAnimation()
                            dragonAnimationState.intValue = dragonEntity.value!!.animationState
                        }
                    ) {
                        Text(text = "Stop Animate Dragon Entity", fontSize = 20.sp)
                    }
                    Text(
                        text = "Dragon Entity Animation State: ${dragonAnimationState.intValue}",
                        fontSize = 20.sp,
                    )
                    Button(
                        onClick = {
                            val spec =
                                KhronosPbrMaterialSpec.create(
                                    lightingModel = KhronosPbrMaterialSpec.LightingModel.LIT,
                                    blendMode = KhronosPbrMaterialSpec.BlendMode.OPAQUE,
                                    doubleSidedMode =
                                        KhronosPbrMaterialSpec.DoubleSidedMode.SINGLE_SIDED,
                                )
                            val khronosPbrMaterialFuture: ListenableFuture<KhronosPbrMaterial> =
                                KhronosPbrMaterial.create(session, spec)
                            khronosPbrMaterialFuture.addListener(
                                {
                                    try {
                                        khronosPbrMaterial.value = khronosPbrMaterialFuture.get()
                                    } catch (e: Exception) {
                                        Log.e(
                                            "SplitEngineTestActivity",
                                            "Failed to Khronos PBR Material: " + e.message,
                                        )
                                    }
                                },
                                Runnable::run,
                            )
                        }
                    ) {
                        Text(text = "Create Khronos PBR Material Split Engine", fontSize = 20.sp)
                    }
                    Button(
                        onClick = {
                            val textureFuture: ListenableFuture<Texture> =
                                Texture.create(
                                    session,
                                    "textures/pattern.png",
                                    TextureSampler.create(),
                                )
                            textureFuture.addListener(
                                {
                                    try {
                                        patternTexture.value = textureFuture.get()
                                    } catch (e: Exception) {
                                        Log.e(
                                            "SplitEngineTestActivity",
                                            "Failed to load Pattern Texture: " + e.message,
                                        )
                                    }
                                },
                                Runnable::run,
                            )
                        }
                    ) {
                        Text(text = "Load Pattern Texture Split Engine", fontSize = 20.sp)
                    }
                    Button(
                        onClick = {
                            khronosPbrMaterial.value?.let { material ->
                                patternTexture.value?.let { texture ->
                                    material.setBaseColorTexture(texture)
                                }
                            }
                        }
                    ) {
                        Text("Set BaseColor Pattern Texture", fontSize = 20.sp)
                    }
                    Button(
                        onClick = {
                            dragonEntity.value?.let { entity ->
                                khronosPbrMaterial.value?.let { material ->
                                    try {
                                        entity.setMaterialOverride(material, "Dragon")
                                    } catch (e: IllegalStateException) {
                                        Log.e("Override Mesh", "Failed to set material override", e)
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Override Dragon main mesh", fontSize = 20.sp)
                    }
                    Text("Emissive R: ${"%.2f".format(khronosPbrMaterialEmissiveR / 100f)}")
                    Slider(
                        value = khronosPbrMaterialEmissiveR,
                        valueRange = 0f..100f,
                        steps = 99,
                        onValueChangeFinished = { updateEmissiveFactor() },
                        onValueChange = { it ->
                            khronosPbrMaterialEmissiveR = it
                            updateEmissiveFactor()
                        },
                    )
                    Text("Emissive G: ${"%.2f".format(khronosPbrMaterialEmissiveG / 100f)}")
                    Slider(
                        value = khronosPbrMaterialEmissiveG,
                        valueRange = 0f..100f,
                        steps = 99,
                        onValueChangeFinished = { updateEmissiveFactor() },
                        onValueChange = { it ->
                            khronosPbrMaterialEmissiveG = it
                            updateEmissiveFactor()
                        },
                    )
                    Text("Emissive B: ${"%.2f".format(khronosPbrMaterialEmissiveB / 100f)}")
                    Slider(
                        value = khronosPbrMaterialEmissiveB,
                        valueRange = 0f..100f,
                        steps = 99,
                        onValueChangeFinished = { updateEmissiveFactor() },
                        onValueChange = { it ->
                            khronosPbrMaterialEmissiveB = it
                            updateEmissiveFactor()
                        },
                    )
                    Text("Metallic: ${"%.2f".format(khronosPbrMaterialMetallic / 100f)}")
                    Slider(
                        value = khronosPbrMaterialMetallic,
                        onValueChange = {
                            khronosPbrMaterialMetallic = it
                            khronosPbrMaterial.value?.let { material ->
                                val factor = khronosPbrMaterialMetallic / 100f
                                material.setMetallicFactor(factor)
                            }
                        },
                        valueRange = 0f..100f,
                        steps = 99,
                        onValueChangeFinished = {
                            khronosPbrMaterial.value?.let { material ->
                                val factor = khronosPbrMaterialMetallic / 100f
                                material.setMetallicFactor(factor)
                            }
                        },
                    )
                    Text("Roughness: ${"%.2f".format(khronosPbrMaterialRoughness / 100f)}")
                    Slider(
                        value = khronosPbrMaterialRoughness,
                        onValueChange = {
                            khronosPbrMaterialRoughness = it
                            khronosPbrMaterial.value?.let { material ->
                                val factor = khronosPbrMaterialRoughness / 100f
                                material.setRoughnessFactor(factor)
                            }
                        },
                        valueRange = 0f..100f,
                        steps = 99,
                        onValueChangeFinished = {
                            khronosPbrMaterial.value?.let { material ->
                                val factor = khronosPbrMaterialRoughness / 100f
                                material.setRoughnessFactor(factor)
                            }
                        },
                    )
                }
            }
            if (glimmerModel.value != null) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f).padding(8.dp),
                ) {
                    Text(text = "Glimmer", fontSize = 50.sp)
                    Button(
                        onClick = {
                            if (glimmerEntity.value == null) {
                                glimmerEntity.value =
                                    GltfModelEntity.create(
                                        session,
                                        glimmerModel.value!!,
                                        Pose(
                                            Vector3(0.0f, 0.0f, 0.0f),
                                            Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                                        ),
                                    )
                            }
                            glimmerEntity.value!!.startAnimation(false)
                        }
                    ) {
                        Text(text = "Play Glimmer", fontSize = 20.sp)
                    }
                }
            }
        }
    }
}
