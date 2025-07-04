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

package androidx.xr.compose.integration.layout.modifierorderapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.alpha
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.onGloballyPositioned
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.scale
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width

class ModifierOrderApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Subspace { SpatialContent() } }
    }

    @Composable
    private fun SpatialContent() {
        val panelSize =
            SubspaceModifier.movable()
                .offset(x = 10.dp)
                .width(200.dp)
                .alpha(1f)
                .height(200.dp)
                .resizable()

        SpatialPanel(
            modifier =
                panelSize
                    .offset(z = (-50).dp)
                    .scale(1.2f)
                    .onGloballyPositioned {
                        Log.i("ModifierOrderApp", "BackPanel position: ${it.poseInRoot}")
                    }
                    .offset(y = (-100).dp)
                    .testTag("Back Panel")
        ) {
            PanelContent(Color.Red, "Back Panel")
        }
        SpatialPanel(
            modifier =
                panelSize
                    .onGloballyPositioned {
                        Log.i("ModifierOrderApp", "MiddlePanel position: ${it.poseInRoot}")
                    }
                    .scale(0.9f)
                    .testTag("Middle Panel")
        ) {
            PanelContent(Color.White, "Middle Panel")
        }
        SpatialPanel(
            modifier =
                panelSize
                    .scale(0.8f)
                    .offset(z = 50.dp)
                    .onGloballyPositioned {
                        Log.i("ModifierOrderApp", "FrontPanel position: ${it.poseInRoot}")
                    }
                    .offset(y = 100.dp)
                    .testTag("Front Panel")
        ) {
            PanelContent(Color.Blue, "Front Panel")
        }
    }

    @UiComposable
    @Composable
    private fun PanelContent(color: Color, text: String) {
        Box(
            modifier = Modifier.background(color).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    color = Color.Black,
                )
            }
        }
    }
}
