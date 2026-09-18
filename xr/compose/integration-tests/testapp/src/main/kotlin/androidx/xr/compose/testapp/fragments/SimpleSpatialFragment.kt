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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.fragment.compose.content
import androidx.xr.compose.spatial.Subspace
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
    ) = content {
        Subspace {
            val xOffset = arguments?.getFloat("x_offset") ?: 0f
            SpatialColumn(modifier = SubspaceModifier.offset(x = xOffset.dp)) {
                SpatialRow {
                    SpatialPanel(
                        modifier = SubspaceModifier.spatialWidth(300.dp).spatialHeight(300.dp)
                    ) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Simple Spatial Fragment", color = Color.Blue)

                                Button(
                                    onClick = {
                                        (requireActivity() as? FragmentCompatibilityActivity)
                                            ?.showVideoPlayerFragment()
                                    }
                                ) {
                                    Text("Go to Video player fragment")
                                }

                                Button(
                                    onClick = {
                                        (requireActivity() as? FragmentCompatibilityActivity)
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
                                            horizontalAlignment = Alignment.CenterHorizontally,
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
                }

                SpatialPanel {
                    Column(Modifier.size(500.dp).background(Color.White)) {
                        Text("Fragment inside Panel", modifier = Modifier.padding(24.dp))

                        val containerId = remember { View.generateViewId() }

                        // We manually embed a FragmentContainerView via AndroidView instead
                        // of using the AndroidFragment composable because AndroidFragment relies
                        // on ViewTree resolution (via LocalView.current and
                        // FragmentManager.findFragmentManager) to discover the parent Fragment,
                        // which resolves to the Activity's FragmentManager instead of this
                        // parent Fragment's childFragmentManager.
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                FragmentContainerView(context).apply {
                                    id = containerId

                                    // Manually establish fragment parenting relationship on the
                                    // View
                                    setTag(
                                        androidx.fragment.R.id.fragment_container_view_tag,
                                        this@SimpleSpatialFragment,
                                    )
                                }
                            },
                            update = { containerView ->
                                val existingFragment =
                                    childFragmentManager.findFragmentById(containerView.id)
                                if (existingFragment == null) {
                                    val simpleTextFragment =
                                        SimpleTextFragment().apply {
                                            arguments =
                                                Bundle().apply {
                                                    putString(
                                                        "text",
                                                        "This is a 2D Embedded Fragment",
                                                    )
                                                }
                                        }
                                    if (childFragmentManager.isStateSaved) {
                                        childFragmentManager.commit(allowStateLoss = true) {
                                            setReorderingAllowed(true)
                                            add(
                                                containerView,
                                                simpleTextFragment,
                                                "simple_text_fragment",
                                            )
                                        }
                                    } else {
                                        childFragmentManager.commitNow(allowStateLoss = true) {
                                            setReorderingAllowed(true)
                                            add(
                                                containerView,
                                                simpleTextFragment,
                                                "simple_text_fragment",
                                            )
                                        }
                                    }
                                }
                                childFragmentManager.onContainerAvailable(containerView)
                            },
                            onRelease = { containerView ->
                                val fragment =
                                    childFragmentManager.findFragmentById(containerView.id)
                                if (
                                    fragment != null &&
                                        !childFragmentManager.isStateSaved &&
                                        !childFragmentManager.isDestroyed
                                ) {
                                    childFragmentManager.commit(allowStateLoss = true) {
                                        remove(fragment)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
