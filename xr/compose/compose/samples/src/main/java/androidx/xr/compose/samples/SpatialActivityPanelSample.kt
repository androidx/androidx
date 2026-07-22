/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.compose.samples

import android.app.Activity
import android.content.Intent
import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialActivityPanel
import androidx.xr.compose.subspace.rememberSpatialActivityPanelController

private class DashboardActivity : Activity()

private class DetailActivity : Activity()

private class NewEmailActivity : Activity()

@Sampled
@Composable
public fun SpatialActivityPanelSample(intent: Intent) {
    // Because the controller is remembered, passing a different intent parameter
    // on recomposition will not update the controller or launch a new activity.
    val controller = rememberSpatialActivityPanelController(intent)

    Subspace { SpatialActivityPanel(controller = controller) }
}

@Sampled
@Composable
public fun SpatialActivityPanelLaunchedEffectSample(pendingItemId: String?) {
    val context = LocalContext.current
    val controller =
        rememberSpatialActivityPanelController(Intent(context, DashboardActivity::class.java))

    Subspace {
        SpatialActivityPanel(controller = controller)

        LaunchedEffect(pendingItemId) {
            val itemId = pendingItemId ?: return@LaunchedEffect
            val detailIntent =
                Intent(context, DetailActivity::class.java).apply { putExtra("ITEM_ID", itemId) }

            controller.startActivity(detailIntent)
        }
    }
}

@Sampled
@Composable
public fun SpatialActivityPanelRecreateSample() {
    val context = LocalContext.current
    var selectedEmailId by remember { mutableStateOf<String?>(null) }

    Column {
        Button(onClick = { selectedEmailId = "email_1" }) { Text("Open Email 1") }

        Button(onClick = { selectedEmailId = "email_2" }) { Text("Open Email 2") }

        if (selectedEmailId != null) {
            Subspace {
                // Using Compose key to force recreation of the controller and
                // the panel whenever the selected email changes. This tears down
                // the old activity stack and starts a new one for the new email.
                key(selectedEmailId) {
                    val intent =
                        Intent(context, NewEmailActivity::class.java).apply {
                            putExtra("EMAIL_ID", selectedEmailId)
                        }
                    SpatialActivityPanel(
                        controller = rememberSpatialActivityPanelController(intent)
                    )
                }
            }
        }
    }
}
