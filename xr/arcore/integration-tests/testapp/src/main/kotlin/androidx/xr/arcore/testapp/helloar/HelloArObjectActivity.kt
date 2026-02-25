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

package androidx.xr.arcore.testapp.helloar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.testapp.common.BackToMainActivityButton
import androidx.xr.arcore.testapp.common.SessionLifecycleHelper
import androidx.xr.arcore.testapp.common.TrackablesList
import androidx.xr.arcore.testapp.helloar.rendering.AugmentedObjectRenderer
import androidx.xr.arcore.testapp.ui.theme.GoogleYellow
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session

class HelloArObjectActivity : ComponentActivity() {

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private val augmentedObjectRenderer = AugmentedObjectRenderer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create session and renderers.
        sessionHelper =
            SessionLifecycleHelper(
                this,
                Config(
                    deviceTracking = DeviceTrackingMode.LAST_KNOWN,
                    augmentedObjectCategories =
                        listOf(
                            AugmentedObjectCategory.KEYBOARD,
                            AugmentedObjectCategory.MOUSE,
                            AugmentedObjectCategory.LAPTOP,
                        ),
                ),
                onSessionAvailable = { session ->
                    this.session = session

                    setContent {
                        Subspace {
                            SpatialPanel(
                                modifier =
                                    SubspaceModifier.size(DpVolumeSize(640.dp, 480.dp, 0.dp)),
                                dragPolicy = MovePolicy(),
                                resizePolicy = ResizePolicy(),
                            ) {
                                HelloObjects(session)
                            }
                        }
                    }
                },
            )
        sessionHelper.tryCreateSession()
    }

    override fun onPause() {
        super.onPause()
        augmentedObjectRenderer.stopRendering()
    }

    override fun onResume() {
        super.onResume()
        if (::session.isInitialized) augmentedObjectRenderer.startRendering(session, lifecycleScope)
    }

    @Composable
    fun HelloObjects(session: Session) {
        val state by session.state.collectAsStateWithLifecycle()
        val objects by
            augmentedObjectRenderer.renderedObjects.collectAsStateWithLifecycle(emptyList())

        var title = intent.getStringExtra("TITLE")
        if (title == null) title = "Hello AR Object"
        Scaffold(
            modifier = Modifier.fillMaxSize().padding(0.dp),
            topBar = {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(0.dp).background(color = GoogleYellow),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackToMainActivityButton()
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = title,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    )
                }
            },
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).background(color = Color.White)) {
                Text(text = "CoreState: ${state.timeMark}")
                TrackablesList(objects.toList())
            }
        }
    }

    companion object {
        const val ACTIVITY_NAME = "AugmentedObjectActivity"
    }
}
