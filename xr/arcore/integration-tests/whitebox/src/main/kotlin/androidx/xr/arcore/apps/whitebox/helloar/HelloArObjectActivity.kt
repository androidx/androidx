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

package androidx.xr.arcore.apps.whitebox.helloar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.apps.whitebox.common.BackToMainActivityButton
import androidx.xr.arcore.apps.whitebox.common.SessionLifecycleHelper
import androidx.xr.arcore.apps.whitebox.common.TrackablesList
import androidx.xr.arcore.apps.whitebox.helloar.rendering.AugmentedObjectRenderer
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session

/** Sample that demonstrates fundamental ARCore for Android XR usage. */
class HelloArObjectActivity : ComponentActivity() {

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private lateinit var augmentedObjectRenderer: AugmentedObjectRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create session and renderers.
        sessionHelper =
            SessionLifecycleHelper(
                this,
                Config(
                    deviceTracking = Config.DeviceTrackingMode.LAST_KNOWN,
                    augmentedObjectCategories =
                        listOf(
                            AugmentedObjectCategory.KEYBOARD,
                            AugmentedObjectCategory.MOUSE,
                            AugmentedObjectCategory.LAPTOP,
                        ),
                ),
                onSessionAvailable = { session ->
                    this.session = session

                    augmentedObjectRenderer = AugmentedObjectRenderer(session, lifecycleScope)
                    lifecycle.addObserver(augmentedObjectRenderer)

                    setContent { HelloObjects(session) }
                },
            )
        sessionHelper.tryCreateSession()
    }
}

@Composable
fun HelloObjects(session: Session) {
    val state by session.state.collectAsStateWithLifecycle()
    val perceptionState = state.perceptionState

    Column(modifier = Modifier.background(color = Color.White)) {
        BackToMainActivityButton()
        Text(text = "CoreState: ${state.timeMark}")
        if (perceptionState != null) {
            TrackablesList(perceptionState.trackables.toList())
        } else {
            Text("PerceptionState is null.")
        }
    }
}
