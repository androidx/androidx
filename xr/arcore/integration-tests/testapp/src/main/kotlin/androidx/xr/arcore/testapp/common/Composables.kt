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
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package androidx.xr.arcore.testapp.common

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.xr.arcore.AugmentedImage
import androidx.xr.arcore.AugmentedObject
import androidx.xr.arcore.Plane
import androidx.xr.arcore.PlaneLabel
import androidx.xr.arcore.Trackable
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.testapp.ui.theme.GoogleYellow
import androidx.xr.arcore.testapp.ui.theme.PurpleGrey80
import androidx.xr.runtime.AugmentedObjectCategory

@Composable
fun BackToMainActivityButton() {
    val context = LocalContext.current
    Button(
        onClick = { (context as Activity).finish() },
        colors =
            ButtonColors(
                containerColor = GoogleYellow,
                contentColor = Color.White,
                disabledContainerColor = Color.LightGray,
                disabledContentColor = Color.Gray,
            ),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            "backIcon",
            Modifier.size(36.dp),
            tint = Color.Black,
        )
    }
}

@Composable
fun TrackablesList(trackables: List<Trackable<Trackable.State>>) {
    LazyColumn { items(trackables) { trackable -> TrackableCard(trackable) } }
}

@Composable
fun TrackableCard(trackable: Trackable<Trackable.State>) {
    val state = trackable.state.collectAsStateWithLifecycle()
    OutlinedCard(
        colors = CardDefaults.cardColors(),
        modifier = Modifier.padding(8.dp).fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Trackable ID: $trackable")
            Text(text = "Tracking State: ${state.value.trackingState}")
            when (trackable) {
                is AugmentedObject -> {
                    AugmentedObjectStateInfo(state.value as AugmentedObject.State)
                }
                is Plane -> {
                    Text("Plane Type: ${trackable.type}")
                    PlaneStateInfo(state.value as Plane.State)
                }
                is AugmentedImage -> {
                    AugmentedImageStateInfo(state.value as AugmentedImage.State)
                }
            }
        }
    }
}

@Composable
fun PlaneStateInfo(state: Plane.State) {
    Text(text = "Plane Label: ${state.label}", color = convertPlaneLabelToColor(state.label))
    Text(text = "Plane Center Pose: ${state.centerPose}")
    Text(text = "Plane Extents: ${state.extents}")
    Text(text = "Subsumed by Plane: ${state.subsumedBy}")
    Text(text = "Plane Vertices: ${state.vertices}")
}

@Composable
fun AugmentedObjectStateInfo(state: AugmentedObject.State) {
    Text(
        text = "Object Category: ${state.category.getDescription()}",
        color = convertAugmentedObjectCategoryToColor(state.category),
    )
    if (state.trackingState == TrackingState.TRACKING) {
        Text(text = "Object Center Pose: ${state.centerPose}")
        Text(text = "Object Extents: ${state.extents}")
    }
}

private fun AugmentedObjectCategory.getDescription(): String =
    when (this) {
        AugmentedObjectCategory.KEYBOARD -> "Keyboard"
        AugmentedObjectCategory.MOUSE -> "Mouse"
        AugmentedObjectCategory.LAPTOP -> "Laptop"
        else -> "Unknown"
    }

@Suppress("DEPRECATION")
private fun convertPlaneLabelToColor(label: PlaneLabel): Color =
    when (label) {
        PlaneLabel.WALL -> Color.Green
        PlaneLabel.FLOOR -> Color.Blue
        PlaneLabel.CEILING -> Color.Yellow
        PlaneLabel.TABLE -> Color.Magenta
        else -> Color.Red
    }

private fun convertAugmentedObjectCategoryToColor(category: AugmentedObjectCategory): Color =
    when (category) {
        AugmentedObjectCategory.KEYBOARD -> Color.Green
        AugmentedObjectCategory.LAPTOP -> Color.Yellow
        AugmentedObjectCategory.MOUSE -> Color.Blue
        else -> Color.Magenta
    }

@Composable
fun AugmentedImageStateInfo(state: AugmentedImage.State) {
    Text(text = "Augmented Image Center Pose: ${state.centerPose}")
    Text(text = "Augmented Image Extents: ${state.extents}")
}

@Composable
fun TestCaseButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(2.dp).fillMaxWidth(),
        shape = RoundedCornerShape(3.dp),
        colors =
            ButtonColors(
                containerColor = PurpleGrey80,
                contentColor = Color.Black,
                disabledContentColor = Color.Gray,
                disabledContainerColor = Color.DarkGray,
            ),
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
