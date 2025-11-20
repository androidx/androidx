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

package androidx.xr.arcore.apps.whitebox.mobile.common

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.xr.arcore.AugmentedObject
import androidx.xr.arcore.Plane
import androidx.xr.arcore.Trackable

@Composable
fun BackToMainActivityButton() {
    val context = LocalContext.current
    FilledTonalButton(onClick = { (context as Activity).finish() }) { Text("Go Back") }
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
            Text(text = "Trackable ID: ${trackable}")
            Text(text = "Tracking State: ${state.value.trackingState}")
            if (trackable is Plane) {
                Text("Plane Type: ${trackable.type}")
                PlaneStateInfo(state.value as Plane.State)
            } else if (trackable is AugmentedObject) {
                AugmentedObjectStateInfo(state.value as AugmentedObject.State)
            }
        }
    }
}

@Composable
fun AugmentedObjectStateInfo(state: AugmentedObject.State) {
    Text(text = "Augmented Object Category: ${state.category}")
    Text(text = "Augmented Object Center Pose: ${state.centerPose}")
    Text(text = "Augmented Object Extents: ${state.extents}")
}

@Composable
fun PlaneStateInfo(state: Plane.State) {
    Text(text = "Plane Label: ${state.label}", color = convertPlaneLabelToColor(state.label))
    Text(text = "Plane Center Pose: ${state.centerPose}")
    Text(text = "Plane Extents: ${state.extents}")
    Text(text = "Subsumed by Plane: ${state.subsumedBy}")
    Text(text = "Plane Vertices: ${state.vertices}")
}

private fun convertPlaneLabelToColor(label: Plane.Label): Color =
    when (label) {
        Plane.Label.WALL -> Color.Green
        Plane.Label.FLOOR -> Color.Blue
        Plane.Label.CEILING -> Color.Yellow
        Plane.Label.TABLE -> Color.Magenta
        else -> Color.Red
    }
