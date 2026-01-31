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

package androidx.xr.compose.testapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height as spatialHeight
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.width as spatialWidth
import androidx.xr.compose.testapp.ui.components.TestDialog

class SimpleSpatialFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                Subspace {
                    val xOffset = arguments?.getFloat("x_offset") ?: 0f

                    SpatialColumn(modifier = SubspaceModifier.offset(x = xOffset.dp)) {
                        SpatialRow {
                            SpatialPanel(
                                modifier =
                                    SubspaceModifier.spatialWidth(300.dp).spatialHeight(300.dp)
                            ) {
                                Surface(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        val context = LocalContext.current
                                        val lifecycleOwner = LocalLifecycleOwner.current
                                        Text(
                                            "Context: ${context.javaClass.simpleName}",
                                            color = Color.Blue,
                                        )
                                        val currentState =
                                            lifecycleOwner.lifecycle.currentStateAsState().value
                                        Text("State: $currentState", color = Color.Blue)

                                        var text by remember { mutableStateOf("") }
                                        TextField(
                                            value = text,
                                            onValueChange = { text = it },
                                            label = { Text("Enter text") },
                                        )

                                        Button(
                                            onClick = {
                                                (requireActivity()
                                                        as? FragmentCompatibilityActivity)
                                                    ?.showVideoPlayerFragment()
                                            }
                                        ) {
                                            Text("Go to Video player fragment")
                                        }

                                        Button(
                                            onClick = {
                                                (requireActivity()
                                                        as? FragmentCompatibilityActivity)
                                                    ?.showMainPanelFragment()
                                            }
                                        ) {
                                            Text("Go to MainPanel fragment")
                                        }

                                        TestDialog {
                                            Surface(
                                                color = Color.White,
                                                modifier = Modifier.clip(RoundedCornerShape(5.dp)),
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(20.dp),
                                                    horizontalAlignment =
                                                        Alignment.CenterHorizontally,
                                                ) {
                                                    Text(
                                                        "This is a SpatialDialog",
                                                        modifier = Modifier.padding(10.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            SpatialPanel(
                                modifier =
                                    SubspaceModifier.spatialWidth(300.dp).spatialHeight(300.dp)
                            ) {
                                Surface(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text("Orbiter Host")
                                        Orbiter(position = ContentEdge.Top, offset = 10.dp) {
                                            Surface(color = Color.Gray) {
                                                Text(
                                                    "Orbiter Content",
                                                    modifier = Modifier.padding(8.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        SpatialBox {
                            SpatialPanel(
                                modifier =
                                    SubspaceModifier.spatialWidth(600.dp).spatialHeight(200.dp)
                            ) {
                                Surface(modifier = Modifier.fillMaxSize()) {
                                    Column {
                                        Text(
                                            "Fragment inside Panel",
                                            modifier = Modifier.padding(8.dp),
                                        )
                                        // Fragment inside SpatialPanel is giving an
                                        // IllegalArgumentException leading to a crash. Need to be
                                        // further investigated as part of b/455674712
                                        /* val containerId = remember { View.generateViewId() }
                                        AndroidView(
                                            factory = { context ->
                                                FragmentContainerView(context).apply {
                                                    id = containerId
                                                }
                                            },
                                            update = { view ->
                                                view.post {
                                                    if (childFragmentManager.findFragmentById(view.id) == null) {
                                                        val simpleTextFragmentBundle = Bundle().apply {
                                                            putString("text", "This is SecondFragment to see multi fragment view")
                                                        }
                                                        val simpleTextFragment = SimpleTextFragment().apply { arguments = simpleTextFragmentBundle }
                                                        childFragmentManager.beginTransaction()
                                                            .replace(view.id, simpleTextFragment)
                                                            .commit()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        ) */
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
