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

package androidx.xr.compose.testapp.splitengine

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ExrImage
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.GltfModelEntity.AnimationState
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.SpatialEnvironment.SpatialEnvironmentPreference
import androidx.xr.scenecore.scene
import java.nio.file.Paths
import kotlinx.coroutines.launch

class SplitEngine : ComponentActivity() {

    private val activity = this

    private val session by lazy {
        // SplitEngine is enabled by default.
        (Session.create(this) as SessionCreateSuccess).session
    }

    private var spatialEnvironmentPreference: SpatialEnvironmentPreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session.scene.spatialEnvironment.preferredPassthroughOpacity = 0.0f

        setContent {
            var title = intent.getStringExtra("TITLE")
            if (title == null) title = "Split Engine Test"
            ComposeEntry(activity, title)
        }
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
    fun ComposeEntry(activity: Activity, title: String) {
        val composeView =
            ComposeView(activity.applicationContext).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { SplitEngineTestActivityUI(title, true) }
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
    @SuppressLint("RestrictedApi")
    @Composable
    fun SplitEngineTestActivityUI(title: String, backButton: Boolean) {
        val lambda = { this@SplitEngine.finish() }
        val recreateLambda = { this@SplitEngine.recreate() }
        CommonTestScaffold(
            title = title,
            showBottomBar = backButton,
            onClickBackArrow = if (backButton) lambda else null,
            onClickRecreate = if (backButton) recreateLambda else null,
        ) { padding ->
            Row(modifier = Modifier.fillMaxWidth().padding(padding)) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f).padding(8.dp),
                ) {
                    SystemApisCard()
                    SplitEngineSkyboxApisCard()
                    SplitEngineGeometryApisCard()
                }
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier =
                        Modifier.weight(1f).padding(8.dp).verticalScroll(rememberScrollState()),
                ) {
                    SplitEngineGlimmerApisCard()
                    SplitEngineEntityApisCard()
                }
            }
        }
    }

    @Composable
    fun SystemApisCard() {
        val movableComponentMP = remember { mutableStateOf<MovableComponent?>(null) }
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                ApiText(text = "System APIs")

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val modifier = Modifier.weight(1F)
                    ApiButton("Toggle Passthrough", modifier) { togglePassthrough(session) }
                    ApiButton("Switch to FSM", modifier) {
                        session.scene.requestFullSpaceMode()
                        if (movableComponentMP.value == null) {
                            movableComponentMP.value = MovableComponent.createSystemMovable(session)
                            session.scene.mainPanelEntity.addComponent(movableComponentMP.value!!)
                        }
                    }
                    ApiButton("Switch to HSM", modifier) { session.scene.requestHomeSpaceMode() }
                }
            }
        }
    }

    @Composable
    fun SplitEngineSkyboxApisCard() {
        val blueSkybox = remember { mutableStateOf<ExrImage?>(null) }

        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                ApiText(text = "Split-Engine APIs - Skybox")

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val modifier = Modifier.weight(1F)
                    val coroutineScope = rememberCoroutineScope()

                    ApiButton("Load Skybox Blue", modifier) {
                        coroutineScope.launch {
                            blueSkybox.value =
                                ExrImage.createFromZip(
                                    session,
                                    Paths.get("skyboxes", "BlueSkybox.zip"),
                                )
                        }
                    }
                    if (blueSkybox.value != null) {
                        ApiButton("Set Skybox Blue", modifier) {
                            if (blueSkybox.value != null) {
                                setSkyboxAndGeometry(
                                    blueSkybox.value,
                                    spatialEnvironmentPreference?.geometry,
                                )
                            }
                        }
                        ApiButton("Remove Skybox Blue", modifier) {
                            setSkyboxAndGeometry(null, spatialEnvironmentPreference?.geometry)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SplitEngineGeometryApisCard() {
        val rocksGeometry = remember { mutableStateOf<GltfModel?>(null) }
        val scope = rememberCoroutineScope()

        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                ApiText(text = "Split-Engine APIs - Geometry")

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val modifier = Modifier.weight(1F)
                    ApiButton("Load Geometry Rocks", modifier) {
                        scope.launch {
                            rocksGeometry.value =
                                GltfModel.create(session, Paths.get("models", "GroundGeometry.glb"))
                        }
                    }

                    if (rocksGeometry.value != null) {
                        ApiButton("Set Geometry Rocks", modifier) {
                            if (rocksGeometry.value != null) {
                                setSkyboxAndGeometry(
                                    spatialEnvironmentPreference?.skybox,
                                    rocksGeometry.value,
                                )
                            }
                        }

                        ApiButton("Remove Geometry Rocks", modifier) {
                            setSkyboxAndGeometry(spatialEnvironmentPreference?.skybox, null)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SplitEngineGlimmerApisCard() {
        val glimmerModel = remember { mutableStateOf<GltfModel?>(null) }
        val glimmerEntity = remember { mutableStateOf<GltfModelEntity?>(null) }
        val scope = rememberCoroutineScope()

        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                ApiText(text = "Split-Engine APIs - Glimmer")

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val modifier = Modifier.weight(1F)
                    ApiButton("Load\nGlimmer", modifier) {
                        scope.launch {
                            glimmerModel.value =
                                GltfModel.create(session, Paths.get("models", "l2a_pulse.glb"))
                        }
                    }

                    if (glimmerModel.value != null) {
                        ApiButton("Play\nGlimmer", modifier) {
                            if (glimmerEntity.value == null) {
                                glimmerEntity.value =
                                    GltfModelEntity.create(
                                        session,
                                        glimmerModel.value!!,
                                        Pose.Identity,
                                    )
                            }
                            glimmerEntity.value!!.startAnimation(false)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SplitEngineEntityApisCard() {
        val dragonModel = remember { mutableStateOf<GltfModel?>(null) }
        val dragonEntity = remember { mutableStateOf<GltfModelEntity?>(null) }
        var isChecked by remember { mutableStateOf(false) } // State for the switch
        val dragonAnimationState = remember {
            androidx.compose.runtime.mutableIntStateOf(GltfModelEntity.AnimationState.STOPPED)
        }
        val scope = rememberCoroutineScope()

        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                ApiText(text = "Split-Engine APIs - Entity")

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val modifier = Modifier.weight(1F)
                    ApiButton("Load Dragon Model", modifier) {
                        scope.launch {
                            dragonModel.value =
                                GltfModel.create(
                                    session,
                                    Paths.get("models", "Dragon_Evolved.gltf"),
                                )
                        }
                    }
                    if (dragonModel.value != null) {
                        ApiButton("Create Dragon Entity", modifier) {
                            if (dragonModel.value != null && dragonEntity.value == null) {
                                dragonEntity.value =
                                    GltfModelEntity.create(
                                        session,
                                        dragonModel.value!!,
                                        Pose(
                                            Vector3(2.0f, 0.0f, 0.0f),
                                            Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                                        ),
                                    )
                            }
                        }

                        ApiButton("Destroy Dragon Entity", modifier) {
                            if (dragonEntity.value != null) {
                                dragonEntity.value!!.dispose()
                                dragonEntity.value = null
                            }
                        }
                    }
                }
            }
        }

        if (dragonEntity.value != null) {
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ApiText(text = "Split-Engine APIs - Animation")

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val modifier = Modifier.weight(1F)
                        ApiButton("Animate Dragon Entity", modifier) {
                            dragonEntity.value!!.startAnimation(false, "Animation")
                        }
                        ApiButton("Loop Animate Dragon Entity", modifier) {
                            dragonEntity.value!!.startAnimation(true, "Animation")
                        }
                        ApiButton("Stop Animate Dragon Entity", modifier) {
                            if (dragonEntity.value!!.animationState == AnimationState.PLAYING) {
                                dragonEntity.value!!.stopAnimation()
                            }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ApiText(text = "Split Engine APIs - Setting & Animation State")

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Attach Interactable Component to Dragon Entity:",
                            modifier = Modifier.weight(3f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        val interactableComponent =
                            InteractableComponent.create(session, mainExecutor) {
                                if (it.action == InputEvent.Action.ACTION_DOWN) {
                                    dragonEntity.value!!.setScale(
                                        dragonEntity.value!!.getScale() * 1.1f
                                    )
                                }
                            }

                        Switch(
                            checked = isChecked,
                            onCheckedChange = {
                                isChecked = it
                                if (isChecked) {
                                    dragonEntity.value!!.addComponent(interactableComponent)
                                } else {
                                    dragonEntity.value!!
                                        .getComponentsOfType(InteractableComponent::class.java)
                                        .forEach { component ->
                                            dragonEntity.value!!.removeComponent(component)
                                        }
                                    dragonEntity.value!!.setScale(dragonEntity.value!!.getScale())
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val stateValue =
                            if (dragonAnimationState.intValue == 1) "STOPPED" else "STARTED"
                        Text(
                            text = "Animation State: $stateValue",
                            modifier = Modifier.weight(1f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ApiText(text: String) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }

    @Composable
    private fun ApiButton(text: String, modifier: Modifier, onClick: () -> Unit) {
        Button(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(10.dp)) {
            Text(text, textAlign = TextAlign.Center)
        }
    }
}
