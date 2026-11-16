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

package androidx.xr.compose.testapp.navigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme

/** Secondary test activity with a Subspace launched from [SpatialNavigationActivity]. */
class ActivityB : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            IntegrationTestsAppTheme {
                Subspace {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(500.dp).height(400.dp).movable().resizable()
                    ) {
                        CommonTestScaffold(
                            title = "Navigation: Activity B",
                            showBottomBar = true,
                            bottomBarText = "Active: Activity B (Subspace)",
                            onClickBackArrow = { finish() },
                            onClickRecreate = { recreate() },
                        ) { padding ->
                            Column(
                                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement =
                                    Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                            ) {
                                Text(
                                    "Activity B Subspace Panel",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Button(onClick = { finish() }) { Text("← Back") }
                            }
                        }
                    }
                }
            }
        }
    }
}
