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

package androidx.xr.compose.testapp.spatialcompose

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.LocalSpatialConfiguration
import androidx.xr.compose.spatial.ApplicationSubspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.unit.VolumeConstraints

class SpatialComposeWindowManager : ComponentActivity() {
    private val mediaUriState: MutableState<Uri?> = mutableStateOf(null)

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                mediaUriState.value = result.data?.data
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // 2D Content rendered to the MainPanel
            MainPanelContent()

            ApplicationSubspace(constraints = VolumeConstraints()) { SpatialLayout() }
        }
    }

    @Composable
    fun MainPanelContent() {
        var title = intent.getStringExtra("TITLE")
        if (title == null) title = "Window Manager Test"
        CommonTestScaffold(
            title = title,
            showBottomBar = true,
            onClickBackArrow = { this@SpatialComposeWindowManager.finish() },
            onClickRecreate = { this@SpatialComposeWindowManager.recreate() },
        ) {
            Column(
                modifier = Modifier.background(Color.LightGray).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Panel Center - main task window")
                PanelContent()
            }
        }
    }

    @Composable
    @SubspaceComposable
    fun SpatialLayout() {
        var showMainPanel by remember { mutableStateOf(true) }

        SpatialRow(modifier = SubspaceModifier.width(4000.dp).height(2000.dp)) {
            SpatialColumn(modifier = SubspaceModifier.height(500.dp).width(400.dp)) {
                SpatialPanel(modifier = SubspaceModifier.height(500.dp).width(300.dp)) {
                    Column(
                        modifier = Modifier.background(Color.LightGray).fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Button(onClick = { showMainPanel = !showMainPanel }) {
                            Text("Show Main Panel")
                        }
                        Spacer(modifier = Modifier.size(20.dp))
                        PanelContent()
                    }
                }
            }
            PanelGrid()
            if (showMainPanel) {
                SpatialMainPanel(modifier = SubspaceModifier.height(800.dp).width(680.dp))
            }
        }
    }

    @Composable
    fun PanelContent() {
        var showDialog = remember { mutableStateOf(false) }
        val context = LocalContext.current
        val textToShare = "https://www.google.com/"
        val isSpatialUiEnabled = LocalSpatialCapabilities.current.isSpatialUiEnabled
        val config = LocalSpatialConfiguration.current

        Button(onClick = { showDialog.value = !showDialog.value }) { Text("Show Modal Dialog") }
        ModalDialog(showDialog)
        Spacer(modifier = Modifier.size(20.dp))
        Button(
            onClick = {
                val sendIntent: Intent =
                    Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, textToShare)
                        type = "text/plain"
                    }

                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }
        ) {
            Text("Share This Text")
        }
        Spacer(modifier = Modifier.size(20.dp))
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
                pickMedia.launch(intent)
            }
        ) {
            Text("Select photo")
        }
        Spacer(modifier = Modifier.size(20.dp))
        Button(
            onClick = {
                if (isSpatialUiEnabled) {
                    config.requestHomeSpaceMode()
                } else {
                    config.requestFullSpaceMode()
                }
            }
        ) {
            Text("Switch Space Mode")
        }
    }

    @Composable
    fun PanelGrid() {
        SpatialRow(modifier = SubspaceModifier.height(700.dp).width(500.dp)) {
            SpatialColumn(modifier = SubspaceModifier.height(700.dp).width(300.dp)) {
                for (j in 0..<3) {
                    SpatialPanel(modifier = SubspaceModifier.height(200.dp).width(200.dp)) {
                        var backgroundColor by remember { mutableStateOf(Color.LightGray) }
                        Column(
                            modifier =
                                Modifier.fillMaxSize()
                                    .clickable {
                                        backgroundColor =
                                            if (backgroundColor == Color.LightGray) {
                                                Color.Cyan
                                            } else {
                                                Color.LightGray
                                            }
                                    }
                                    .background(backgroundColor),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("click me")
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ModalDialog(showDialog: MutableState<Boolean>) {
        if (showDialog.value) {
            Dialog(onDismissRequest = { showDialog.value = false }) {
                Surface(color = Color.White, modifier = Modifier.clip(RoundedCornerShape(5.dp))) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("This is a SpatialDialog", modifier = Modifier.padding(10.dp))
                        Button(onClick = { showDialog.value = false }) { Text("Dismiss") }
                    }
                }
            }
        }
    }
}
